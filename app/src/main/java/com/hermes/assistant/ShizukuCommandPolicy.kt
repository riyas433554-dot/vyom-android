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
                ownerConfirmed && args.size == 2 &&
                args[1].matches(Regex("^[a-f0-9]{64}$")) &&
                args[0] == "/storage/emulated/0/Download/VyomUpdates/vyom-update-${args[1].take(12)}.apk"
            ) {
                val source = args[0]
                val hash = args[1]
                val destination = "/data/local/tmp/vyom-update-$hash.apk"
                val script = "set -e; trap 'rm -f \"$destination\"' EXIT; cp '$source' '$destination'; actual=\$(sha256sum '$destination' | cut -d ' ' -f 1); [ \"\$actual\" = '$hash' ]; pm install -r '$destination'"
                Command(listOf("sh", "-c", script), 120_000L, true)
            } else null
            else -> null
        }
    }
}
