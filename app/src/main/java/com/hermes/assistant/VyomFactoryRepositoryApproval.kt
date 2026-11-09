package com.hermes.assistant

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

/** Device-local, plan-bound approval. Only the local owner-review UI issues tokens. */
internal class VyomFactoryRepositoryApprovalStore(
    private val journal: VyomFactoryApprovalNonceJournal,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val nonceFactory: () -> String = { UUID.randomUUID().toString() }
) {
    constructor(context: Context) : this(VyomFactoryExecutionJournal(context.applicationContext))

    data class Token internal constructor(val planHash: String, val nonce: String, val expiresAtMillis: Long)

    @Synchronized
    fun issueFromOwnerReview(
        plan: VyomFactoryRepositoryChangePolicy.Plan,
        binding: VyomFactorySystemRegistry.Binding
    ): Token {
        check(!plan.ownerApproved) { "A repository plan must not carry pre-existing approval" }
        require(binding.systemId == plan.systemId && binding.commitSha == plan.baseCommitSha)
        val now = nowMillis()
        require(now >= 0)
        val nonce = nonceFactory()
        require(nonce.matches(Regex("[A-Za-z0-9-]{16,80}")))
        check(!journal.isApprovalNonceConsumed(nonce)) { "Approval nonce was already consumed" }
        val planHash = hash(plan, binding.repositoryUrl)
        val expiresAt = now + TOKEN_TTL_MS
        journal.issueApprovalNonce(nonce, planHash, expiresAt)
        return Token(planHash, nonce, expiresAt)
    }

    @Synchronized
    fun verifyAndConsume(
        plan: VyomFactoryRepositoryChangePolicy.Plan,
        binding: VyomFactorySystemRegistry.Binding,
        token: Token,
        now: Long = nowMillis()
    ) {
        check(!plan.ownerApproved) { "Issue-controlled approval flags are not accepted" }
        require(binding.systemId == plan.systemId && binding.commitSha == plan.baseCommitSha)
        if (now < 0 || token.expiresAtMillis <= now || token.expiresAtMillis - now > TOKEN_TTL_MS) {
            throw SecurityException("Repository approval token expired or has an invalid lifetime")
        }
        val expected = hash(plan, binding.repositoryUrl)
        if (!MessageDigest.isEqual(expected.toByteArray(Charsets.US_ASCII), token.planHash.toByteArray(Charsets.US_ASCII))) {
            throw SecurityException("Repository approval does not match this exact plan")
        }
        if (!token.nonce.matches(Regex("[A-Za-z0-9-]{16,80}"))) throw SecurityException("Invalid approval nonce")
        if (!journal.consumeApprovalNonce(token.nonce, expected, token.expiresAtMillis, now)) {
            throw SecurityException("Repository approval nonce is missing, expired, or already consumed")
        }
    }

    internal fun hash(plan: VyomFactoryRepositoryChangePolicy.Plan, repositoryUrl: String): String {
        val canonical = buildString {
            append(repositoryUrl).append('\n')
            append(plan.systemId).append('\n')
            append(plan.issueNumber).append('\n')
            append(plan.attemptId).append('\n')
            append(plan.title).append('\n')
            append(plan.baseBranch).append('\n')
            append(plan.baseCommitSha).append('\n')
            plan.changes.sortedBy { it.path }.forEach {
                append(it.path).append('\n')
                append(sha256(it.content.toByteArray(Charsets.UTF_8))).append('\n')
            }
        }
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        const val TOKEN_TTL_MS = 5 * 60 * 1_000L
    }
}
