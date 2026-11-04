package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class AccessibilityReconnectPolicyTest {
    private val vyom = "com.vyom.assistant/com.hermes.assistant.VyomAccessibilityService"
    private val other = "com.example/.OtherAccessibilityService"

    @Test fun ownerGrantIsRequiredBeforeReconnect() {
        assertFalse(AccessibilityReconnectPolicy.canReconnect(false, vyom))
        assertTrue(AccessibilityReconnectPolicy.canReconnect(true, vyom))
        assertFalse(AccessibilityReconnectPolicy.canReconnect(true, ""))
    }

    @Test fun reconnectAddsVyomWithoutRemovingOtherEnabledServices() {
        assertEquals("$other:$vyom", AccessibilityReconnectPolicy.mergeEnabledServices(other, vyom))
    }

    @Test fun reconnectIsIdempotentAndDoesNotDuplicateComponent() {
        assertEquals("$other:$vyom", AccessibilityReconnectPolicy.mergeEnabledServices("$other:$vyom", vyom))
        assertTrue(AccessibilityReconnectPolicy.isEnabled("$other:$vyom", vyom))
        assertFalse(AccessibilityReconnectPolicy.isEnabled(other, vyom))
    }

    @Test fun effectiveStateRequiresGlobalAccessibilityFlagAndVyomComponent() {
        assertFalse(AccessibilityReconnectPolicy.isEffectivelyEnabled(false, "$vyom", vyom))
        assertFalse(AccessibilityReconnectPolicy.isEffectivelyEnabled(true, other, vyom))
        assertFalse(AccessibilityReconnectPolicy.isEffectivelyEnabled(false, other, vyom))
        assertTrue(AccessibilityReconnectPolicy.isEffectivelyEnabled(true, "$other:$vyom", vyom))
    }

    @Test fun effectiveStateRejectsNullAndMalformedComponents() {
        assertFalse(AccessibilityReconnectPolicy.isEffectivelyEnabled(true, null, vyom))
        assertFalse(AccessibilityReconnectPolicy.isEffectivelyEnabled(true, "$vyom", "invalid"))
    }

    @Test fun malformedComponentIsRejectedWithoutChangingExistingServices() {
        assertNull(AccessibilityReconnectPolicy.mergeEnabledServices(other, "bad:component"))
        assertNull(AccessibilityReconnectPolicy.mergeEnabledServices(other, ""))
    }
}
