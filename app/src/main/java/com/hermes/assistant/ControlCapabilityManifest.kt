package com.hermes.assistant

/** Live capability truth for the generic control planner. Missing access is never silently guessed. */
internal object ControlCapabilityManifest {
    enum class Adapter { SHELL_EXECUTOR, ACCESSIBILITY, UNAVAILABLE }

    data class Snapshot(
        val shellExecutorUiReady: Boolean,
        val accessibilityEnabled: Boolean,
        val accessibilityConnected: Boolean,
        val appAccessMasterEnabled: Boolean,
        val enabledAppCount: Int,
        val plannerConfigured: Boolean,
        val stopControlEnabled: Boolean,
        val powerModeLevel2: Boolean
    )

    fun selectAdapter(snapshot: Snapshot): Adapter = when {
        snapshot.stopControlEnabled || !snapshot.plannerConfigured || !snapshot.appAccessMasterEnabled -> Adapter.UNAVAILABLE
        snapshot.shellExecutorUiReady -> Adapter.SHELL_EXECUTOR
        snapshot.accessibilityEnabled && snapshot.accessibilityConnected -> Adapter.ACCESSIBILITY
        else -> Adapter.UNAVAILABLE
    }

    fun missingGuidance(snapshot: Snapshot): List<String> = buildList {
        if (snapshot.stopControlEnabled) {
            add("STOP CONTROL is ON. Turn it off from the Control panel before starting another task.")
        }
        if (!snapshot.plannerConfigured) {
            add("UI planner is not configured. One-tap fix: Profile > Settings > enter and save the NVIDIA API key.")
        }
        if (!snapshot.accessibilityEnabled || !snapshot.accessibilityConnected) {
            add("Missing Android Accessibility service. One-tap fix: Settings > Accessibility > Installed apps > Vyom > turn on Accessibility, then return to Vyom.")
        }
        if (!snapshot.appAccessMasterEnabled) {
            add("Missing app-control grant. One-tap fix: Profile > App access > Enable Master App Control, then enable the target app.")
        }
    }

    fun promptContext(snapshot: Snapshot): String = buildString {
        append("adapter=").append(selectAdapter(snapshot).name)
        append("; accessibilityEnabled=").append(snapshot.accessibilityEnabled)
        append("; accessibilityConnected=").append(snapshot.accessibilityConnected)
        append("; appAccessMasterEnabled=").append(snapshot.appAccessMasterEnabled)
        append("; enabledApps=").append(snapshot.enabledAppCount.coerceAtLeast(0))
        append("; plannerConfigured=").append(snapshot.plannerConfigured)
        append("; stopControlEnabled=").append(snapshot.stopControlEnabled)
        append("; powerModeLevel2=").append(snapshot.powerModeLevel2)
        if (snapshot.powerModeLevel2 && !snapshot.shellExecutorUiReady) {
            append("; Power Mode ShellExecutor is available for allowlisted coordinate gestures; Accessibility remains the fallback for tree/OCR actions and any rejected shell action.")
        }
        val guidance = missingGuidance(snapshot)
        if (guidance.isNotEmpty()) append("; missing=").append(guidance.joinToString(" | "))
        append("; hardLimits=FLAG_SECURE, biometrics/lock-screen credentials, Play Protect, payments/banking/OTP/Authenticator apps, Vyom kill-switch and protected files")
    }
}
