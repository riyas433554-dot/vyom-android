package com.hermes.assistant

/**
 * Models Android grants as observable capabilities. A missing special permission always
 * requires a system-owned settings flow; an app must never pretend it can grant itself.
 */
internal object AndroidPermissionPolicy {
    enum class Requirement {
        ACCESSIBILITY,
        APP_ACCESS_MASTER,
        ALLOWED_TARGET_APP,
        MICROPHONE,
        CAMERA,
        NOTIFICATIONS,
        OVERLAY,
        WRITE_SYSTEM_SETTINGS
    }

    enum class GrantState { GRANTED, NEEDS_SYSTEM_APPROVAL }

    enum class TaskCapability {
        APP_SWITCH,
        UI_AUTOMATION,
        SPEECH_INPUT,
        CAMERA_CAPTURE,
        FLOATING_PROGRESS,
        CHANGE_SYSTEM_SETTINGS,
        BACKGROUND_NOTIFICATIONS
    }

    data class Readiness(val ready: Boolean, val missing: Set<Requirement>)

    fun readiness(
        accessibilityEnabled: Boolean,
        masterAppAccess: Boolean,
        allowedTargetApps: Int
    ): Readiness {
        val missing = linkedSetOf<Requirement>()
        if (!accessibilityEnabled) missing += Requirement.ACCESSIBILITY
        if (!masterAppAccess) missing += Requirement.APP_ACCESS_MASTER
        if (allowedTargetApps <= 0) missing += Requirement.ALLOWED_TARGET_APP
        return Readiness(missing.isEmpty(), missing)
    }

    fun missingForTask(
        task: TaskCapability,
        microphoneGranted: Boolean,
        cameraGranted: Boolean,
        notificationsGranted: Boolean,
        overlayGranted: Boolean,
        writeSettingsGranted: Boolean
    ): Set<Requirement> = when (task) {
        TaskCapability.SPEECH_INPUT -> if (microphoneGranted) emptySet() else setOf(Requirement.MICROPHONE)
        TaskCapability.CAMERA_CAPTURE -> if (cameraGranted) emptySet() else setOf(Requirement.CAMERA)
        TaskCapability.FLOATING_PROGRESS -> if (overlayGranted) emptySet() else setOf(Requirement.OVERLAY)
        TaskCapability.CHANGE_SYSTEM_SETTINGS -> if (writeSettingsGranted) emptySet() else setOf(Requirement.WRITE_SYSTEM_SETTINGS)
        TaskCapability.BACKGROUND_NOTIFICATIONS -> if (notificationsGranted) emptySet() else setOf(Requirement.NOTIFICATIONS)
        TaskCapability.APP_SWITCH, TaskCapability.UI_AUTOMATION -> emptySet()
    }

    fun specialPermissionState(granted: Boolean): GrantState =
        if (granted) GrantState.GRANTED else GrantState.NEEDS_SYSTEM_APPROVAL
}
