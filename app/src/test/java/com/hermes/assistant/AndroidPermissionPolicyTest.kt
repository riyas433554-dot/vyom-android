package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidPermissionPolicyTest {
    @Test fun fullControlRequiresAccessibilityAndOwnerAppAccess() {
        val missingAccessibility = AndroidPermissionPolicy.readiness(
            accessibilityEnabled = false, masterAppAccess = true, allowedTargetApps = 3
        )
        assertFalse(missingAccessibility.ready)
        assertTrue(missingAccessibility.missing.contains(AndroidPermissionPolicy.Requirement.ACCESSIBILITY))

        val missingMaster = AndroidPermissionPolicy.readiness(
            accessibilityEnabled = true, masterAppAccess = false, allowedTargetApps = 3
        )
        assertFalse(missingMaster.ready)
        assertTrue(missingMaster.missing.contains(AndroidPermissionPolicy.Requirement.APP_ACCESS_MASTER))

        val noApps = AndroidPermissionPolicy.readiness(
            accessibilityEnabled = true, masterAppAccess = true, allowedTargetApps = 0
        )
        assertFalse(noApps.ready)
        assertTrue(noApps.missing.contains(AndroidPermissionPolicy.Requirement.ALLOWED_TARGET_APP))
    }

    @Test fun optionalPermissionsAreNotGlobalBlockers() {
        val ready = AndroidPermissionPolicy.readiness(
            accessibilityEnabled = true, masterAppAccess = true, allowedTargetApps = 2
        )
        assertTrue(ready.ready)
        assertEquals(emptySet<AndroidPermissionPolicy.Requirement>(), ready.missing)
    }

    @Test fun capabilitiesAreTaskScopedRatherThanDemandingEveryPermission() {
        assertTrue(AndroidPermissionPolicy.missingForTask(
            AndroidPermissionPolicy.TaskCapability.SPEECH_INPUT,
            microphoneGranted = false, cameraGranted = false, notificationsGranted = false,
            overlayGranted = false, writeSettingsGranted = false
        ).contains(AndroidPermissionPolicy.Requirement.MICROPHONE))
        assertFalse(AndroidPermissionPolicy.missingForTask(
            AndroidPermissionPolicy.TaskCapability.APP_SWITCH,
            microphoneGranted = false, cameraGranted = false, notificationsGranted = false,
            overlayGranted = false, writeSettingsGranted = false
        ).contains(AndroidPermissionPolicy.Requirement.MICROPHONE))
        assertTrue(AndroidPermissionPolicy.missingForTask(
            AndroidPermissionPolicy.TaskCapability.FLOATING_PROGRESS,
            microphoneGranted = true, cameraGranted = true, notificationsGranted = true,
            writeSettingsGranted = true
        ).isEmpty())
    }

    @Test fun specialPermissionsAreNeverAssumedGranted() {
        assertEquals(AndroidPermissionPolicy.GrantState.NEEDS_SYSTEM_APPROVAL,
            AndroidPermissionPolicy.specialPermissionState(false))
        assertEquals(AndroidPermissionPolicy.GrantState.GRANTED,
            AndroidPermissionPolicy.specialPermissionState(true))
    }
}
