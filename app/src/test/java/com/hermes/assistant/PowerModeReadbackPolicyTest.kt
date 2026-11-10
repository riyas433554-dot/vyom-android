package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class PowerModeReadbackPolicyTest {
    @Test fun secureSettingsGrantRequiresSuccessfulExactPermissionOutput() {
        assertEquals(PowerModeReadbackPolicy.Status.GRANTED, PowerModeReadbackPolicy.secureSettings("exit=0; output=granted"))
        assertEquals(PowerModeReadbackPolicy.Status.DENIED, PowerModeReadbackPolicy.secureSettings("exit=0; output=denied"))
        assertEquals(PowerModeReadbackPolicy.Status.UNKNOWN, PowerModeReadbackPolicy.secureSettings("exit=1; output=granted"))
        assertEquals(PowerModeReadbackPolicy.Status.UNKNOWN, PowerModeReadbackPolicy.secureSettings("failed: SecurityException"))
        assertEquals(PowerModeReadbackPolicy.Status.UNKNOWN, PowerModeReadbackPolicy.secureSettings("exit=0; output=granted\nextra"))
    }

    @Test fun overlayAppOpAllowsOnlyExplicitAllowAndDoesNotTreatDefaultAsGranted() {
        assertEquals(PowerModeReadbackPolicy.Status.ALLOWED, PowerModeReadbackPolicy.overlay("exit=0; output=SYSTEM_ALERT_WINDOW: allow"))
        assertEquals(PowerModeReadbackPolicy.Status.BLOCKED, PowerModeReadbackPolicy.overlay("exit=0; output=SYSTEM_ALERT_WINDOW: ignore"))
        assertEquals(PowerModeReadbackPolicy.Status.BLOCKED, PowerModeReadbackPolicy.overlay("exit=0; output=SYSTEM_ALERT_WINDOW: deny"))
        assertEquals(PowerModeReadbackPolicy.Status.UNKNOWN, PowerModeReadbackPolicy.overlay("exit=0; output=SYSTEM_ALERT_WINDOW: default"))
        assertEquals(PowerModeReadbackPolicy.Status.UNKNOWN, PowerModeReadbackPolicy.overlay("exit=0; output=No operations."))
    }

    @Test fun accessibilityGlobalFlagRequiresExactZeroOrOne() {
        assertEquals(true, PowerModeReadbackPolicy.accessibilityGlobalEnabled("exit=0; output=1"))
        assertEquals(false, PowerModeReadbackPolicy.accessibilityGlobalEnabled("exit=0; output=0"))
        assertNull(PowerModeReadbackPolicy.accessibilityGlobalEnabled("exit=0; output=2"))
        assertNull(PowerModeReadbackPolicy.accessibilityGlobalEnabled("exit=1; output=1"))
    }

    @Test fun accessibilityServiceReadbackChecksExactComponentMembership() {
        val component = "com.vyom.assistant/com.hermes.assistant.VyomAccessibilityService"
        assertEquals(true, PowerModeReadbackPolicy.accessibilityServiceEnabled("exit=0; output=com.example/.Other:$component", component))
        assertEquals(false, PowerModeReadbackPolicy.accessibilityServiceEnabled("exit=0; output=com.example/.Other", component))
        assertNull(PowerModeReadbackPolicy.accessibilityServiceEnabled("exit=1; output=$component", component))
        assertNull(PowerModeReadbackPolicy.accessibilityServiceEnabled("exit=0; output=$component", "bad"))
    }
}
