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
            "ui_tap" -> if (
                args.size == 5 && validUiTarget(args[0]) &&
                validDisplayPoint(args[1], args[2], args[3], args[4])
            ) Command(listOf("input", "tap", args[1], args[2]), 4_000L, true) else null
            "ui_long_press" -> if (
                ownerConfirmed && args.size == 6 && validUiTarget(args[0]) &&
                validDisplayPoint(args[1], args[2], args[4], args[5]) &&
                args[3].toLongOrNull() in 500L..2_000L
            ) Command(listOf("input", "swipe", args[1], args[2], args[1], args[2], args[3]), 4_000L, true) else null
            "ui_swipe" -> if (
                ownerConfirmed && args.size == 8 && validUiTarget(args[0]) &&
                validDisplayPoint(args[1], args[2], args[6], args[7]) &&
                validDisplayPoint(args[3], args[4], args[6], args[7]) &&
                args[5].toLongOrNull() in 100L..2_000L
            ) Command(listOf("input", "swipe", args[1], args[2], args[3], args[4], args[5]), 5_000L, true) else null
            "ui_keyevent" -> if (
                args.size == 2 && validUiTarget(args[0]) &&
                args[1].toIntOrNull() in setOf(3, 4, 19, 20, 21, 22, 23, 61, 62, 66, 111, 187)
            ) Command(listOf("input", "keyevent", args[1]), 4_000L, true) else null
            "ui_type_text" -> if (
                args.size == 2 && validUiTarget(args[0]) && isSafeUiText(args[1])
            ) {
                val encoded = args[1].replace("%", "%25").replace(" ", "%s")
                Command(listOf("input", "text", encoded), 5_000L, true)
            } else null
            "install_verified_apk" -> if (
                ownerConfirmed && args.size == 2 &&
                args[1].matches(Regex("^[a-f0-9]{64}$")) &&
                args[0] == "/storage/emulated/0/Download/VyomUpdates/vyom-update-${args[1].take(12)}.apk"
            ) {
                val source = args[0]
                val hash = args[1]
                val destination = "/data/local/tmp/vyom-update-$hash.apk"
                val script = "set -e; destination='$destination'; trap 'rm -f \"\$destination\"' EXIT; cp '$source' \"\$destination\"; actual=\$(sha256sum \"\$destination\" | cut -d ' ' -f 1); [ \"\$actual\" = '$hash' ]; pm install -r \"\$destination\""
                Command(listOf("sh", "-c", script), 120_000L, true)
            } else null
            else -> null
        }
    }

    private fun validUiTarget(value: String): Boolean =
        value.matches(packagePattern) && value !in setOf("com.vyom.assistant", "com.android.systemui")

    private fun validDisplayPoint(xValue: String, yValue: String, widthValue: String, heightValue: String): Boolean {
        val x = xValue.toIntOrNull() ?: return false
        val y = yValue.toIntOrNull() ?: return false
        val width = widthValue.toIntOrNull() ?: return false
        val height = heightValue.toIntOrNull() ?: return false
        return width in 1..10_000 && height in 1..10_000 && x in 0 until width && y in 0 until height
    }

    private fun isSafeUiText(value: String): Boolean =
        value.isNotBlank() && value.length <= 500 && value.none(Char::isISOControl) &&
            !Regex("(?i)(password|passcode|otp|cvv|api[_ -]?key|access[_ -]?token)").containsMatchIn(value) &&
            !Regex("""\\b(?:\\d[ -]*?){13,19}\\b""").containsMatchIn(value) &&
            !Regex("""\\b\\d{4,12}\\b""").containsMatchIn(value)
}
