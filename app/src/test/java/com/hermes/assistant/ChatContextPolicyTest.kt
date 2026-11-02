package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContextPolicyTest {
    @Test fun contextIsBounded() {
        val context = ChatContextPolicy.buildContext("memory ".repeat(500), "history ".repeat(500))
        assertTrue(context.length <= 2_000)
    }

    @Test fun keepsBothMemoryAndRecentHistoryWhenTheyFit() {
        val context = ChatContextPolicy.buildContext("USER_PREF=Hinglish", "LATEST_MESSAGE=remember this")
        assertTrue(context.contains("USER_PREF=Hinglish"))
        assertTrue(context.contains("LATEST_MESSAGE=remember this"))
    }

    @Test fun rawRetryOnlyHappensForBadRequestWithDifferentRawMessage() {
        assertTrue(ChatContextPolicy.shouldRetryWithRaw(400, "contextual prompt", "hi"))
        assertFalse(ChatContextPolicy.shouldRetryWithRaw(500, "contextual prompt", "hi"))
        assertFalse(ChatContextPolicy.shouldRetryWithRaw(400, "hi", "hi"))
        assertFalse(ChatContextPolicy.shouldRetryWithRaw(400, "contextual prompt", ""))
    }

    @Test fun connectionRetryUsesCappedExponentialBackoff() {
        assertEquals(500L, ChatContextPolicy.connectionRetryDelayMillis(0))
        assertEquals(1_000L, ChatContextPolicy.connectionRetryDelayMillis(1))
        assertEquals(2_000L, ChatContextPolicy.connectionRetryDelayMillis(2))
        assertEquals(4_000L, ChatContextPolicy.connectionRetryDelayMillis(3))
        assertEquals(4_000L, ChatContextPolicy.connectionRetryDelayMillis(9))
    }

    @Test fun truncationPreservesSectionLabelsAndNewestMemoryAndHistoryLines() {
        val context = ChatContextPolicy.buildContext(
            "old-memory-line\nnewest-memory-line",
            "old-history-line\nnewest-history-line",
            110
        )
        assertTrue(context.length <= 110)
        assertTrue(context.contains("On-device memory (newest preferences first):"))
        assertTrue(context.contains("Recent conversation:"))
        assertTrue(context.contains("newest-memory-line"))
        assertTrue(context.contains("newest-history-line"))
        assertFalse(context.contains("old-memory-line"))
        assertFalse(context.contains("old-history-line"))
    }

    @Test fun contextRejectsNegativeBudgetAndAllowsZeroBudget() {
        assertEquals("", ChatContextPolicy.buildContext("memory", "history", 0))
        try {
            ChatContextPolicy.buildContext("memory", "history", -1)
            throw AssertionError("negative budget should be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

}
