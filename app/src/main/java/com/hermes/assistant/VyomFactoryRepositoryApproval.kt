package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.security.MessageDigest
import java.util.UUID

/** Device-local, plan-bound approval. Only the local owner-review UI issues tokens. */
internal class VyomFactoryRepositoryApprovalStore(
    private val prefs: SharedPreferences,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val nonceFactory: () -> String = { UUID.randomUUID().toString() }
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    )

    data class Token internal constructor(val planHash: String, val nonce: String, val expiresAtMillis: Long)

    @Synchronized
    fun issueFromOwnerReview(
        plan: VyomFactoryRepositoryChangePolicy.Plan,
        binding: VyomFactorySystemRegistry.Binding
    ): Token {
        require(binding.systemId == plan.systemId && binding.commitSha == plan.baseCommitSha)
        val nonce = nonceFactory()
        require(nonce.matches(Regex("[A-Za-z0-9-]{16,80}")))
        check(!isConsumed(nonce)) { "Approval nonce was already consumed" }
        return Token(hash(plan, binding.repositoryUrl), nonce, nowMillis() + TOKEN_TTL_MS)
    }

    @Synchronized
    fun verifyAndConsume(
        plan: VyomFactoryRepositoryChangePolicy.Plan,
        binding: VyomFactorySystemRegistry.Binding,
        token: Token,
        now: Long = nowMillis()
    ) {
        require(binding.systemId == plan.systemId && binding.commitSha == plan.baseCommitSha)
        if (now < 0 || token.expiresAtMillis <= now || token.expiresAtMillis - now > TOKEN_TTL_MS) {
            throw SecurityException("Repository approval token expired or has an invalid lifetime")
        }
        val expected = hash(plan, binding.repositoryUrl)
        if (!MessageDigest.isEqual(expected.toByteArray(Charsets.US_ASCII), token.planHash.toByteArray(Charsets.US_ASCII))) {
            throw SecurityException("Repository approval does not match this exact plan")
        }
        if (!token.nonce.matches(Regex("[A-Za-z0-9-]{16,80}"))) throw SecurityException("Invalid approval nonce")
        if (isConsumed(token.nonce)) throw SecurityException("Repository approval token was already used")
        val consumed = readConsumed().toMutableSet()
        consumed += token.nonce
        val encoded = JSONArray()
        consumed.sorted().forEach(encoded::put)
        check(prefs.edit().putString(KEY_CONSUMED, encoded.toString()).commit()) {
            "Repository approval nonce could not be persisted"
        }
    }

    private fun isConsumed(nonce: String): Boolean = nonce in readConsumed()

    private fun readConsumed(): Set<String> {
        val raw = prefs.getString(KEY_CONSUMED, null) ?: return emptySet()
        return try {
            val array = JSONArray(raw)
            require(array.length() <= MAX_CONSUMED_NONCES)
            buildSet {
                for (i in 0 until array.length()) {
                    val value = array.getString(i)
                    require(value.matches(Regex("[A-Za-z0-9-]{16,80}")))
                    require(add(value)) { "Duplicate consumed approval nonce" }
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("Approval nonce journal is unreadable; refusing approval", error)
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
        private const val PREFS = "vyom_factory_repository_approvals"
        private const val KEY_CONSUMED = "consumed_nonces_v1"
        const val TOKEN_TTL_MS = 5 * 60 * 1_000L
        private const val MAX_CONSUMED_NONCES = 10_000
    }
}
