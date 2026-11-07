package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryRetryPolicyTest {
    @Test fun retriesOnlyIdempotentWorkAfterTransientFailure() {
        val decision = VyomFactoryRetryPolicy.afterFailure(
            completedAttempts = 1,
            errorCategory = "network_timeout",
            now = 10_000,
            adapterIdempotent = true
        )

        assertTrue(decision.retry)
        assertEquals(40_000L, decision.nextAttemptAt)
    }

    @Test fun refusesToReplayNonIdempotentSideEffects() {
        val decision = VyomFactoryRetryPolicy.afterFailure(
            completedAttempts = 1,
            errorCategory = "network_timeout",
            now = 10_000,
            adapterIdempotent = false
        )

        assertFalse(decision.retry)
        assertNull(decision.nextAttemptAt)
    }

    @Test fun usesBoundedExponentialBackoffAndStopsAtAttemptLimit() {
        val first = VyomFactoryRetryPolicy.afterFailure(1, "server_unavailable", 1_000, true)
        val second = VyomFactoryRetryPolicy.afterFailure(2, "server_unavailable", 1_000, true)
        val exhausted = VyomFactoryRetryPolicy.afterFailure(3, "server_unavailable", 1_000, true)

        assertEquals(31_000L, first.nextAttemptAt)
        assertEquals(121_000L, second.nextAttemptAt)
        assertFalse(exhausted.retry)
        assertNull(exhausted.nextAttemptAt)
    }

    @Test fun doesNotRetryPermanentOrUnknownFailures() {
        listOf("permission_denied", "invalid_request", "unsupported", "unknown")
            .forEach { category ->
                val decision = VyomFactoryRetryPolicy.afterFailure(1, category, 5_000, true)
                assertFalse("category=$category", decision.retry)
                assertNull(decision.nextAttemptAt)
            }
    }

    @Test fun saturatesTimestampInsteadOfOverflowing() {
        val decision = VyomFactoryRetryPolicy.afterFailure(
            completedAttempts = 1,
            errorCategory = "network_timeout",
            now = Long.MAX_VALUE - 10,
            adapterIdempotent = true
        )

        assertEquals(Long.MAX_VALUE, decision.nextAttemptAt)
    }
}
