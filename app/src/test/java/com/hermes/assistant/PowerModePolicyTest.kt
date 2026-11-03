package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class PowerModePolicyTest {
    @Test fun wirelessAdbRequiresWifiAndSameLocalNetwork() {
        assertEquals(
            setOf(PowerModePolicy.Blocker.WIFI_DISABLED, PowerModePolicy.Blocker.NETWORK_MISMATCH),
            PowerModePolicy.wirelessAdbBlockers(wifiEnabled = false, sameNetwork = false)
        )
        assertTrue(PowerModePolicy.wirelessAdbBlockers(wifiEnabled = true, sameNetwork = true).isEmpty())
    }

    @Test fun pairingAndDebugPortsMustBeValidHostPortPairs() {
        assertTrue(PowerModePolicy.isValidEndpoint("192.168.1.12:37123"))
        assertFalse(PowerModePolicy.isValidEndpoint("192.168.1.12"))
        assertFalse(PowerModePolicy.isValidEndpoint("192.168.1.12:0"))
        assertFalse(PowerModePolicy.isValidEndpoint("evil;pm grant"))
    }

    @Test fun generatedTermuxCommandsUseTheRealApplicationIdAndNeverRunAutomatically() {
        val commands = PowerModePolicy.termuxCommands(
            packageName = "com.vyom.assistant",
            pairingEndpoint = "192.168.1.12:37123",
            debugEndpoint = "192.168.1.12:41231"
        )
        assertEquals("adb pair 192.168.1.12:37123", commands.pair)
        assertEquals("adb connect 192.168.1.12:41231", commands.connect)
        assertEquals(
            "adb shell pm grant com.vyom.assistant android.permission.WRITE_SECURE_SETTINGS",
            commands.grantSecureSettings
        )
        assertEquals(
            "adb shell pm check-permission android.permission.WRITE_SECURE_SETTINGS com.vyom.assistant",
            commands.verifySecureSettings
        )
    }

    @Test fun malformedPackageOrEndpointIsRejected() {
        assertNull(PowerModePolicy.termuxCommands("com.bad;pm", "192.168.1.2:1234", "192.168.1.2:5678"))
        assertNull(PowerModePolicy.termuxCommands("com.vyom.assistant", "192.168.1.2:1234", "bad"))
    }

    @Test fun secureSettingsGrantIsRequiredBeforeAccessibilityReconnect() {
        assertFalse(PowerModePolicy.mayAttemptAccessibilityReconnect(secureSettingsGranted = false))
        assertTrue(PowerModePolicy.mayAttemptAccessibilityReconnect(secureSettingsGranted = true))
    }

    @Test fun appopsAreScopedAndAllFilesIsNotRequestedByDefault() {
        val ops = PowerModePolicy.requiredAppOps(
            overlayFeatureEnabled = true,
            allFilesFeatureDeclared = false
        )
        assertEquals(listOf("SYSTEM_ALERT_WINDOW"), ops)
        assertFalse(ops.contains("MANAGE_EXTERNAL_STORAGE"))
    }

    @Test fun levelTwoDoesNotMeanRootOrProtectedSurfaceBypass() {
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_2,
            PowerModePolicy.capabilityLevel(shizukuRunning = true, accessibilityEnabled = true)
        )
        assertFalse(PowerModePolicy.levelTwoAllowsProtectedSurfaces())
    }
}
