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
}
