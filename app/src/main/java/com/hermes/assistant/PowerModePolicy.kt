package com.hermes.assistant

/**
 * Pure policy for the owner-operated Wireless ADB setup.
 * This code formats instructions only; it never starts ADB or grants permissions itself.
 */
internal object PowerModePolicy {
    enum class Blocker { WIFI_DISABLED, NETWORK_MISMATCH }
    enum class CapabilityLevel { LEVEL_0, LEVEL_1, LEVEL_2 }

    data class TermuxCommands(
        val pair: String,
        val connect: String,
        val grantSecureSettings: String,
        val verifySecureSettings: String,
        val grantOverlay: String,
        val verifyOverlay: String,
        val verifyAccessibilityEnabled: String,
        val verifyAccessibilityServices: String
    )

    fun wirelessAdbBlockers(wifiEnabled: Boolean, sameNetwork: Boolean): Set<Blocker> =
        buildSet {
            if (!wifiEnabled) add(Blocker.WIFI_DISABLED)
            if (!sameNetwork) add(Blocker.NETWORK_MISMATCH)
        }

    fun isValidEndpoint(value: String): Boolean {
        val pieces = value.trim().split(':')
        if (pieces.size != 2) return false
        val host = pieces[0]
        val port = pieces[1].toIntOrNull() ?: return false
        val validHost = host.length in 1..253 &&
            host.matches(Regex("^[A-Za-z0-9.-]+$")) &&
            !host.startsWith('.') && !host.endsWith('.') &&
            !host.contains("..")
        return validHost && port in 1..65535
    }

    fun termuxCommands(
        packageName: String,
        pairingEndpoint: String,
        debugEndpoint: String
    ): TermuxCommands? {
        val validPackage = packageName.matches(Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$"))
        if (!validPackage || !isValidEndpoint(pairingEndpoint) || !isValidEndpoint(debugEndpoint)) return null
        return TermuxCommands(
            pair = "adb pair $pairingEndpoint",
            connect = "adb connect $debugEndpoint",
            grantSecureSettings = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS",
            verifySecureSettings = "adb shell pm check-permission android.permission.WRITE_SECURE_SETTINGS $packageName",
            grantOverlay = "adb shell appops set $packageName SYSTEM_ALERT_WINDOW allow",
            verifyOverlay = "adb shell appops get $packageName SYSTEM_ALERT_WINDOW",
            verifyAccessibilityEnabled = "adb shell settings get secure accessibility_enabled",
            verifyAccessibilityServices = "adb shell settings get secure enabled_accessibility_services"
        )
    }

    fun mayAttemptAccessibilityReconnect(secureSettingsGranted: Boolean): Boolean = secureSettingsGranted

    fun requiredAppOps(overlayFeatureEnabled: Boolean, allFilesFeatureDeclared: Boolean): List<String> =
        buildList {
            if (overlayFeatureEnabled) add("SYSTEM_ALERT_WINDOW")
            if (allFilesFeatureDeclared) add("MANAGE_EXTERNAL_STORAGE")
        }

    fun capabilityLevel(
        shizukuRunning: Boolean,
        shizukuAuthorized: Boolean,
        shizukuUid: Int,
        accessibilityEnabled: Boolean
    ): CapabilityLevel = when {
        shizukuRunning && shizukuAuthorized && shizukuUid == 2000 -> CapabilityLevel.LEVEL_2
        accessibilityEnabled -> CapabilityLevel.LEVEL_1
        else -> CapabilityLevel.LEVEL_0
    }

    /** Even a shell-identity Shizuku session is not root and cannot bypass protected OS surfaces. */
    fun levelTwoAllowsProtectedSurfaces(): Boolean = false

    fun autoBlockerGuidance(blocksUsbCommands: Boolean, blocksUnknownInstalls: Boolean): String {
        if (!blocksUsbCommands && !blocksUnknownInstalls) {
            return "Wireless ADB does not require Auto Blocker to be disabled. Keep it enabled."
        }
        val affected = buildList {
            if (blocksUsbCommands) add("USB command blocking")
            if (blocksUnknownInstalls) add("unauthorized-source APK installation blocking")
        }.joinToString(" and ")
        return "Auto Blocker is affecting $affected. Only if you intentionally use that blocked path, open Settings > Security and privacy > Auto Blocker and temporarily disable the specific blocking feature. Re-enable it immediately afterward. Wireless ADB should be preferred."
    }
}
