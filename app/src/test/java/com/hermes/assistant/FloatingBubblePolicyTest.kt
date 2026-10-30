package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingBubblePolicyTest {
    @Test fun requiresBothExplicitOptInAndOverlayPermission() {
        assertFalse(FloatingBubblePolicy.shouldStart(false, false))
        assertFalse(FloatingBubblePolicy.shouldStart(false, true))
        assertFalse(FloatingBubblePolicy.shouldStart(true, false))
        assertTrue(FloatingBubblePolicy.shouldStart(true, true))
    }
}