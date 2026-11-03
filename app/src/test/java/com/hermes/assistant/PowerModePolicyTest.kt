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
        val generated = PowerModePolicy.termuxCommands(
            packageName = "com.vyom.assistant",
            pairingEndpoint = "192.168.1.12:37123",
            debugEndpoint = "192.168.1.12:41231"
        )
        assertNotNull(generated)
        val commands = requireNotNull(generated)
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

    @Test fun shizukuBinderWithoutOwnerAuthorizationDoesNotGrantLevelTwo() {
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_1,
            PowerModePolicy.capabilityLevel(
                shizukuRunning = true,
                shizukuAuthorized = false,
                shizukuUid = 2000,
                accessibilityEnabled = true
            )
        )
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_0,
            PowerModePolicy.capabilityLevel(
                shizukuRunning = true,
                shizukuAuthorized = false,
                shizukuUid = 2000,
                accessibilityEnabled = false
            )
        )
    }

    @Test fun rootOrUnknownShizukuIdentityNeverGetsLevelTwo() {
        for (uid in listOf(0, 1000, -1)) {
            assertEquals(
                PowerModePolicy.CapabilityLevel.LEVEL_1,
                PowerModePolicy.capabilityLevel(
                    shizukuRunning = true,
                    shizukuAuthorized = true,
                    shizukuUid = uid,
                    accessibilityEnabled = true
                )
            )
        }
    }

    @Test fun capabilityMatrixReportsOnlyVerifiedHighestLevel() {
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_0,
            PowerModePolicy.capabilityLevel(false, false, -1, false)
        )
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_1,
            PowerModePolicy.capabilityLevel(false, false, -1, true)
        )
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_2,
            PowerModePolicy.capabilityLevel(true, true, 2000, false)
        )
    }

    @Test fun levelTwoRequiresLiveBinderExplicitAuthorizationAndAdbShellUid() {
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_2,
            PowerModePolicy.capabilityLevel(
                shizukuRunning = true,
                shizukuAuthorized = true,
                shizukuUid = 2000,
                accessibilityEnabled = true
            )
        )
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_2,
            PowerModePolicy.capabilityLevel(
                shizukuRunning = true,
                shizukuAuthorized = true,
                shizukuUid = 2000,
                accessibilityEnabled = false
            )
        )
        assertEquals(
            PowerModePolicy.CapabilityLevel.LEVEL_1,
            PowerModePolicy.capabilityLevel(
                shizukuRunning = false,
                shizukuAuthorized = true,
                shizukuUid = 2000,
                accessibilityEnabled = true
            )
        )
        assertFalse(PowerModePolicy.levelTwoAllowsProtectedSurfaces())
    }
    @Test fun userServiceBindingRequiresLiveAuthorizedAdbShellIdentity() {
        assertTrue(PowerModePolicy.mayBindShizukuUserService(true, true, 2000))
        assertFalse(PowerModePolicy.mayBindShizukuUserService(false, true, 2000))
        assertFalse(PowerModePolicy.mayBindShizukuUserService(true, false, 2000))
        assertFalse(PowerModePolicy.mayBindShizukuUserService(true, true, 0))
        assertFalse(PowerModePolicy.mayBindShizukuUserService(true, true, 1000))
        assertFalse(PowerModePolicy.mayBindShizukuUserService(true, true, -1))
    }

    @Test fun boundUserServiceIsUnusableAfterAuthorizationOrBinderIsLost() {
        assertTrue(PowerModePolicy.mayUseBoundShizukuUserService(true, true, 2000, true, true))
        assertFalse(PowerModePolicy.mayUseBoundShizukuUserService(false, true, 2000, true, true))
        assertFalse(PowerModePolicy.mayUseBoundShizukuUserService(true, false, 2000, true, true))
        assertFalse(PowerModePolicy.mayUseBoundShizukuUserService(true, true, 0, true, true))
        assertFalse(PowerModePolicy.mayUseBoundShizukuUserService(true, true, 2000, false, true))
        assertFalse(PowerModePolicy.mayUseBoundShizukuUserService(true, true, 2000, true, false))
    }
}
