package com.hermes.assistant

import android.provider.Settings

object ActionEngine {
    enum class Risk { SAFE, CONFIRM, BLOCKED }
    enum class Operation { OPEN_SETTINGS, OPEN_APP, VOLUME_DELTA, VOLUME_SET, BRIGHTNESS_SET, TORCH, SET_ALARM, SET_TIMER, DIAL, SMS, SHARE, DELETE, INSTALL, BLOCK }
    data class Intent(val name: String, val risk: Risk, val operation: Operation, val target: String = "", val value: Int = 0, val settingsAction: String? = null)
    private val financeTargets = Regex("""\b(bank|banking|payment|pay|upi)\b""", RegexOption.IGNORE_CASE)
    private val credentialTargets = Regex("""\b(otp|password|passcode|pin code)\b""", RegexOption.IGNORE_CASE)
    private val blockedSecurity = Regex("""\b(uninstall|disable security|security settings|lock screen security)\b""", RegexOption.IGNORE_CASE)
    private val sendTarget = Regex("""\b(send|share)\b.*\b(to|via|with)\b|\bshare\s+(this|it|file|photo|link)\b""", RegexOption.IGNORE_CASE)
    fun looksLikeDeviceAction(text: String): Boolean {
        val direct = Regex("""\b(open|launch|start|set|turn on|turn off|switch on|switch off|enable|disable|increase|decrease|raise|lower|volume|brightness|torch|flashlight|alarm|timer|call|dial|sms|text|delete|remove|install|uninstall|wifi|wi-fi|bluetooth|settings)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        return direct || sendTarget.containsMatchIn(text)
    }
    fun classify(text: String): Intent? {
        val s = text.trim()
        if (s.isEmpty()) return null
        if (Regex("""^(how|what|why|when|where|who|explain|can you explain|tell me about)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)) return null
        val securityRequest = blockedSecurity.containsMatchIn(s) &&
            (Regex("""\b(open|launch|disable|turn off|change|access)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s) || Regex("""\buninstall\b""", RegexOption.IGNORE_CASE).containsMatchIn(s))
        val financeRequest = financeTargets.containsMatchIn(s) &&
            Regex("""\b(open|launch|start|use|access|pay|transfer|send)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)
        val credentialRequest = credentialTargets.containsMatchIn(s) &&
            Regex("""\b(read|show|reveal|tell|fetch|get|access|copy|display|enter)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)
        if (securityRequest || financeRequest || credentialRequest) return Intent("Blocked sensitive action", Risk.BLOCKED, Operation.BLOCK)
        val settings = listOf(
            Regex("""\b(open|show)\s+(wi-?fi|wifi)\s+settings\b""", RegexOption.IGNORE_CASE) to Intent("Open Wi-Fi settings", Risk.SAFE, Operation.OPEN_SETTINGS, settingsAction = Settings.ACTION_WIFI_SETTINGS),
            Regex("""\b(open|show)\s+bluetooth\s+settings\b""", RegexOption.IGNORE_CASE) to Intent("Open Bluetooth settings", Risk.SAFE, Operation.OPEN_SETTINGS, settingsAction = Settings.ACTION_BLUETOOTH_SETTINGS),
            Regex("""\b(open|show)\s+(display|brightness)\s+settings\b""", RegexOption.IGNORE_CASE) to Intent("Open display settings", Risk.SAFE, Operation.OPEN_SETTINGS, settingsAction = Settings.ACTION_DISPLAY_SETTINGS),
            Regex("""\b(open|show)\s+(sound|volume)\s+settings\b""", RegexOption.IGNORE_CASE) to Intent("Open sound settings", Risk.SAFE, Operation.OPEN_SETTINGS, settingsAction = Settings.ACTION_SOUND_SETTINGS),
            Regex("""\b(open|show)\s+app(lication)?\s+settings\b""", RegexOption.IGNORE_CASE) to Intent("Open app settings", Risk.SAFE, Operation.OPEN_SETTINGS, settingsAction = Settings.ACTION_APPLICATION_SETTINGS),
            Regex("""\b(open|show)\s+settings\b""", RegexOption.IGNORE_CASE) to Intent("Open Android settings", Risk.SAFE, Operation.OPEN_SETTINGS, settingsAction = Settings.ACTION_SETTINGS)
        )
        settings.firstOrNull { it.first.containsMatchIn(s) }?.let { return it.second }
        Regex("""\bset\s+(?:the\s+)?volume\s+to\s+(\d{1,3})\s*%?\b""", RegexOption.IGNORE_CASE).find(s)?.let { return Intent("Set media volume", Risk.SAFE, Operation.VOLUME_SET, value = it.groupValues[1].toInt().coerceIn(0,100)) }
        if (Regex("""\b(volume up|increase (?:the )?volume|raise (?:the )?volume|turn (?:the )?volume up)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)) return Intent("Increase media volume", Risk.SAFE, Operation.VOLUME_DELTA, value = 1)
        if (Regex("""\b(volume down|decrease (?:the )?volume|lower (?:the )?volume|turn (?:the )?volume down)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)) return Intent("Decrease media volume", Risk.SAFE, Operation.VOLUME_DELTA, value = -1)
        Regex("""\b(?:set|change)\s+(?:the\s+)?brightness\s+(?:to\s+)?(\d{1,3})\s*%?\b""", RegexOption.IGNORE_CASE).find(s)?.let { return Intent("Set screen brightness", Risk.SAFE, Operation.BRIGHTNESS_SET, value = it.groupValues[1].toInt().coerceIn(0,100)) }
        if (Regex("""\b(turn on|switch on|enable)\s+(?:the\s+)?(torch|flashlight)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)) return Intent("Turn torch on", Risk.SAFE, Operation.TORCH, value = 1)
        if (Regex("""\b(turn off|switch off|disable)\s+(?:the\s+)?(torch|flashlight)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)) return Intent("Turn torch off", Risk.SAFE, Operation.TORCH, value = 0)
        Regex("""\b(?:set|create)\s+(?:an?\s+)?alarm\s+(?:for\s+)?(\d{1,2})(?::(\d{2}))?\b""", RegexOption.IGNORE_CASE).find(s)?.let {
            val h = it.groupValues[1].toIntOrNull() ?: -1
            val m = it.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: -1
            if (h !in 0..23 || m !in 0..59) return Intent("Alarm time invalid", Risk.BLOCKED, Operation.BLOCK)
            return Intent("Set alarm", Risk.SAFE, Operation.SET_ALARM, target = "%02d:%02d".format(h,m))
        }
        Regex("""\b(?:set|start)\s+(?:a\s+)?timer\s+(?:for\s+)?(\d+)\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)\b""", RegexOption.IGNORE_CASE).find(s)?.let {
            val n = it.groupValues[1].toIntOrNull()?.coerceIn(1,86400) ?: return Intent("Timer invalid", Risk.BLOCKED, Operation.BLOCK)
            val u = it.groupValues[2].lowercase()
            val sec = when { u.startsWith("hour") || u.startsWith("hr") -> (n*3600).coerceAtMost(86400); u.startsWith("min") -> n*60; else -> n }
            return Intent("Set timer", Risk.SAFE, Operation.SET_TIMER, value = sec.coerceAtMost(86400))
        }
        Regex("""\b(?:call|dial)\s+(\+?[0-9][0-9 .()-]{4,}[0-9])\b""", RegexOption.IGNORE_CASE).find(s)?.let { return Intent("Call " + it.groupValues[1].trim(), Risk.CONFIRM, Operation.DIAL, target = it.groupValues[1].filter { c -> c.isDigit() || c=='+' }) }
        Regex("""\b(?:sms|text|send (?:a )?message to)\s+(\+?[0-9][0-9 .()-]{4,}[0-9])(?:\s+(?:saying|that|message)\s+(.+))?$""", RegexOption.IGNORE_CASE).find(s)?.let { return Intent("Compose SMS", Risk.CONFIRM, Operation.SMS, target = it.groupValues[1].filter { c -> c.isDigit() || c=='+' } + "\u0000" + it.groupValues[2].trim()) }
        if (Regex("""\b(delete|remove)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)) return Intent("Delete request", Risk.CONFIRM, Operation.DELETE, target=s)
        if (Regex("""\binstall\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)) return Intent("Install request", Risk.CONFIRM, Operation.INSTALL, target=s)
        if (sendTarget.containsMatchIn(s)) return Intent("Open share sheet", Risk.CONFIRM, Operation.SHARE, target=s)
        val app = Regex("""\b(?:open|launch|start)\s+(?:the\s+)?(.+?)\s+(?:app|application)\s*$""", RegexOption.IGNORE_CASE).find(s) ?: Regex("""\b(?:open|launch|start)\s+(?:the\s+)?([a-z][a-z0-9 ._-]{1,40})\s*$""", RegexOption.IGNORE_CASE).find(s)
        if (app != null) { val n=app.groupValues[1].trim().removeSuffix(" app").removeSuffix(" application").trim(); if(n.isNotBlank()) return Intent("Open $n", Risk.SAFE, Operation.OPEN_APP, target=n) }
        if (Regex("""\b(call|dial|sms|text|delete|remove|install|uninstall)\b""", RegexOption.IGNORE_CASE).containsMatchIn(s)) return Intent("Unsupported consequential action", Risk.BLOCKED, Operation.BLOCK)
        return null
    }
}