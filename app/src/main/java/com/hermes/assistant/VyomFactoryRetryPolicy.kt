package com.hermes.assistant

/**
 * Pure retry decision policy. This does not schedule or execute work.
 *
 * Callers must persist the returned timestamp and must pass true only when the
 * concrete adapter explicitly guarantees idempotency for the operation.
 */
internal object VyomFactoryRetryPolicy {
    data class Decision(
        val retry: Boolean,
        val nextAttemptAt: Long?
    )

    private const val MAX_ATTEMPTS = 3
    private const val FIRST_DELAY_MS = 30_000L
    private const val SECOND_DELAY_MS = 120_000L

    private val transientCategories = setOf(
        "network_timeout",
        "rate_limited",
        "server_unavailable",
        "io_error"
    )

    /**
     * completedAttempts is the number of attempts that have already failed.
     * Unknown and permanent errors fail closed; non-idempotent work is never replayed.
     */
    fun afterFailure(
        completedAttempts: Int,
        errorCategory: String,
        now: Long,
        adapterIdempotent: Boolean
    ): Decision {
        require(completedAttempts >= 1) { "At least one attempt must have failed" }
        require(now >= 0) { "Timestamp must not be negative" }

        if (!adapterIdempotent ||
            completedAttempts >= MAX_ATTEMPTS ||
            errorCategory !in transientCategories
        ) {
            return Decision(retry = false, nextAttemptAt = null)
        }

        val delay = when (completedAttempts) {
            1 -> FIRST_DELAY_MS
            2 -> SECOND_DELAY_MS
            else -> return Decision(retry = false, nextAttemptAt = null)
        }
        val nextAt = if (Long.MAX_VALUE - now < delay) Long.MAX_VALUE else now + delay
        return Decision(retry = true, nextAttemptAt = nextAt)
    }
}
