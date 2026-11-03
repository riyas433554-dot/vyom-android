package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCapabilityManifestTest {
    private fun snapshot(
        shellUiReady: Boolean = false,
        accessibilityEnabled: Boolean = true,
        accessibilityConnected: Boolean = true,
        appAccessEnabled: Boolean = true,
        plannerConfigured: Boolean = true,
        stopEnabled: Boolean = false,
        powerModeLevel2: Boolean = false
    ) = ControlCapabilityManifest.Snapshot(
        shellExecutorUiReady = shellUiReady,
        accessibilityEnabled = accessibilityEnabled,
        accessibilityConnected = accessibilityConnected,
        appAccessMasterEnabled = appAccessEnabled,
        enabledAppCount = 3,
        plannerConfigured = plannerConfigured,
        stopControlEnabled = stopEnabled,
        powerModeLevel2 = powerModeLevel2
    )

    @Test fun prefersAuthorizedShellUiAdapterWhenItIsActuallyAvailable() {
        assertEquals(
            ControlCapabilityManifest.Adapter.SHELL_EXECUTOR,
            ControlCapabilityManifest.selectAdapter(snapshot(shellUiReady = true))
        )
    }

    @Test fun fallsBackToAccessibilityWhenShellUiAdapterIsUnavailable() {
        assertEquals(
            ControlCapabilityManifest.Adapter.ACCESSIBILITY,
            ControlCapabilityManifest.selectAdapter(snapshot(shellUiReady = false))
        )
    }

    @Test fun ShizukuLevelTwoAdvertisesOnlyTheImplementedCoordinateGestureSubset() {
        val state = snapshot().copy(powerModeLevel2 = true, shellExecutorUiReady = false)
        assertEquals(ControlCapabilityManifest.Adapter.ACCESSIBILITY, ControlCapabilityManifest.selectAdapter(state))
        val context = ControlCapabilityManifest.promptContext(state)
        assertTrue(context.contains("allowlisted coordinate gestures"))
        assertTrue(context.contains("Accessibility remains the fallback for tree/OCR actions"))
    }

    @Test fun refusesControlWhenAccessibilityOrAppAccessIsMissing() {
        assertEquals(
            ControlCapabilityManifest.Adapter.UNAVAILABLE,
            ControlCapabilityManifest.selectAdapter(snapshot(accessibilityEnabled = false))
        )
        assertEquals(
            ControlCapabilityManifest.Adapter.UNAVAILABLE,
            ControlCapabilityManifest.selectAdapter(snapshot(accessibilityConnected = false))
        )
        assertEquals(
            ControlCapabilityManifest.Adapter.UNAVAILABLE,
            ControlCapabilityManifest.selectAdapter(snapshot(appAccessEnabled = false))
        )
    }

    @Test fun plannerConfigurationIsRequiredBeforeAnyAdapterCanRun() {
        assertEquals(
            ControlCapabilityManifest.Adapter.UNAVAILABLE,
            ControlCapabilityManifest.selectAdapter(snapshot(plannerConfigured = false))
        )
    }

    @Test fun shellExecutorMustNotBypassMasterAppAccessGate() {
        assertEquals(
            ControlCapabilityManifest.Adapter.UNAVAILABLE,
            ControlCapabilityManifest.selectAdapter(snapshot(shellUiReady = true, appAccessEnabled = false))
        )
    }

    @Test fun stopControlAlwaysOverridesAvailableAdapters() {
        assertEquals(
            ControlCapabilityManifest.Adapter.UNAVAILABLE,
            ControlCapabilityManifest.selectAdapter(snapshot(shellUiReady = true, stopEnabled = true))
        )
    }

    @Test fun manifestNamesExactOneTapFixesForMissingAccessibilityAndAppAccess() {
        val guidance = ControlCapabilityManifest.missingGuidance(
            snapshot(accessibilityEnabled = false, accessibilityConnected = false, appAccessEnabled = false)
        )
        assertTrue(guidance.any { it.contains("Settings > Accessibility") && it.contains("Vyom") })
        assertTrue(guidance.any { it.contains("Profile > App access") })
    }
}
