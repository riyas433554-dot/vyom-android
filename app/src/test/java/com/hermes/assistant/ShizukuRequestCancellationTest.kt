package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class ShizukuRequestCancellationTest {
    @Test fun authorizationLossAttemptsCancellationForEveryActiveRequestWhileRemoteBinderIsAlive() {
        val attempted = mutableListOf<String>()
        val result = ShizukuRequestCancellation.cancelOutstanding(
            remoteBinderAlive = true,
            requestIds = listOf("req-1", "req-2")
        ) { attempted += it }

        assertEquals(listOf("req-1", "req-2"), attempted)
        assertEquals(setOf("req-1", "req-2"), result)
    }

    @Test fun deadRemoteBinderDoesNotAttemptCancellation() {
        val attempted = mutableListOf<String>()
        val result = ShizukuRequestCancellation.cancelOutstanding(
            remoteBinderAlive = false,
            requestIds = listOf("req-1")
        ) { attempted += it }

        assertTrue(attempted.isEmpty())
        assertTrue(result.isEmpty())
    }

    @Test fun invalidRequestIdsAreNeverSentAndOneFailedCancelDoesNotSkipOthers() {
        val attempted = mutableListOf<String>()
        val result = ShizukuRequestCancellation.cancelOutstanding(
            remoteBinderAlive = true,
            requestIds = listOf("req-1", "bad;command", "req-2")
        ) {
            attempted += it
            if (it == "req-1") error("binder transaction failed")
        }

        assertEquals(listOf("req-1", "req-2"), attempted)
        assertEquals(setOf("req-1", "req-2"), result)
    }
}
