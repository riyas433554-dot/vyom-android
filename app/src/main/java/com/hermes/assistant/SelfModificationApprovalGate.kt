package com.hermes.assistant

import java.security.MessageDigest
import java.util.UUID

/** Process-memory, single-use approval bound to the exact diff shown in the local review UI. */
internal class SelfModificationApprovalGate(
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val newNonce: () -> String = { UUID.randomUUID().toString() },
    private val ttlMillis: Long = TOKEN_TTL_MS
) {
    data class Token internal constructor(
        val nonce: String,
        val draftHash: String,
        val expiresAtMillis: Long
    )

    private val issued = linkedMapOf<String, Token>()

    init { require(ttlMillis in 1_000L..TOKEN_TTL_MS) }

    @Synchronized
    fun issueFromOwnerReview(draft: SelfModificationClient.Draft): Token {
        val now = nowMillis()
        require(now >= 0) { "Approval clock is invalid" }
        val nonce = newNonce()
        require(nonce.matches(Regex("[A-Za-z0-9-]{16,80}"))) { "Approval nonce is invalid" }
        check(nonce !in issued) { "Approval nonce collision" }
        val token = Token(nonce, hashDraft(draft), Math.addExact(now, ttlMillis))
        issued[nonce] = token
        while (issued.size > MAX_PENDING) issued.remove(issued.keys.first())
        return token
    }

    @Synchronized
    fun consume(draft: SelfModificationClient.Draft, token: Token) {
        val stored = issued.remove(token.nonce)
            ?: throw SecurityException("Local self-modification approval is missing or already consumed")
        val now = nowMillis()
        if (now < 0 || token.expiresAtMillis <= now ||
            token.expiresAtMillis - now > TOKEN_TTL_MS ||
            stored != token ||
            !MessageDigest.isEqual(
                hashDraft(draft).toByteArray(Charsets.US_ASCII),
                token.draftHash.toByteArray(Charsets.US_ASCII)
            )
        ) {
            throw SecurityException("Local approval expired or does not match the exact reviewed diff")
        }
    }

    @Synchronized
    fun clear() { issued.clear() }

    private fun hashDraft(draft: SelfModificationClient.Draft): String {
        val canonical = listOf(
            draft.title, draft.path, draft.baseSha, draft.proposedContent, draft.preview, draft.requestSummary
        ).joinToString("\n") { value -> "${value.length}:$value" }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val TOKEN_TTL_MS = 120_000L
        private const val MAX_PENDING = 8
    }
}
