package com.hermes.assistant

internal object ShizukuCommandPolicy {
    data class Command(val argv: List<String>, val timeoutMs: Long, val mutating: Boolean)

    private val packagePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun command(
        commandId: String,
        args: List<String>,
        ownerConfirmed: Boolean,
        packageName: String
    ): Command? {
        if (!packageName.matches(packagePattern) || packageName != "com.vyom.assistant") return null
        return when (commandId) {
            "verify_secure_settings" -> if (args.isEmpty()) Command(
                listOf("pm", "check-permission", "android.permission.WRITE_SECURE_SETTINGS", packageName),
                4_000L, false
            ) else null
            "verify_overlay" -> if (args.isEmpty()) Command(
                listOf("appops", "get", packageName, "SYSTEM_ALERT_WINDOW"),
                4_000L, false
            ) else null
            "verify_accessibility_enabled" -> if (args.isEmpty()) Command(
                listOf("settings", "get", "secure", "accessibility_enabled"),
                4_000L, false
            ) else null
            "verify_accessibility_services" -> if (args.isEmpty()) Command(
                listOf("settings", "get", "secure", "enabled_accessibility_services"),
                4_000L, false
            ) else null
            "set_overlay" -> if (ownerConfirmed && args.size == 1 && args[0] in setOf("allow", "default")) Command(
                listOf("appops", "set", packageName, "SYSTEM_ALERT_WINDOW", args[0]),
                4_000L, true
            ) else null
            "install_verified_apk" -> if (
                ownerConfirmed && args.size == 1 &&
                args[0].matches(Regex("^/storage/emulated/0/Download/VyomUpdates/vyom-update-[a-f0-9]{12}\\.apk$"))
            ) Command(listOf("pm", "install", "-r", args[0]), 120_000L, true) else null
            else -> null
        }
    }
}
