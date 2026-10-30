package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRequestGuardTest {
    @Test fun sameClientMessageCannotBeScheduledTwiceWhileInFlight() {
        val guard = ChatRequestGuard()
        assertTrue(guard.tryStart("client-1"))
        assertFalse(guard.tryStart("client-1"))
        guard.finish("client-1")
        assertTrue(guard.tryStart("client-1"))
    }
    @Test fun blankIdsAreRejected() {
        val guard = ChatRequestGuard()
        assertFalse(guard.tryStart(""))
        assertFalse(guard.tryStart("   "))
    }
}
