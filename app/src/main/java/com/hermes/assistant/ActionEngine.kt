package com.hermes.assistant

/** Rule-first device action classification. Blocked rules always win over fallback reasoning. */
object ActionEngine {
    enum class Risk { SAFE, CONFIRM, BLOCKED }
    data class Intent(val name: String, val risk: Risk, val settingsAction: String? = null)

    fun classify(text: String): Intent? {
        val s = text.trim()
        if (s.isEmpty()) return null
        if (Regex("\\b(bank|banking|payment|pay|upi|otp|password|passcode|uninstall|disable security)\\b", RegexOption.IGNORE_CASE).containsMatchIn(s))
            return Intent("Blocked sensitive action", Risk.BLOCKED)
        val safe = listOf(
            Regex("\\b(open|show)\\s+(wi-?fi|wifi)\\s+settings\\b", RegexOption.IGNORE_CASE) to Intent("Open Wi-Fi settings", Risk.SAFE, android.provider.Settings.ACTION_WIFI_SETTINGS),
            Regex("\\b(open|show)\\s+bluetooth\\s+settings\\b", RegexOption.IGNORE_CASE) to Intent("Open Bluetooth settings", Risk.SAFE, android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),
            Regex("\\b(open|show)\\s+(display|brightness)\\s+settings\\b", RegexOption.IGNORE_CASE) to Intent("Open display settings", Risk.SAFE, android.provider.Settings.ACTION_DISPLAY_SETTINGS),
            Regex("\\b(open|show)\\s+(sound|volume)\\s+settings\\b", RegexOption.IGNORE_CASE) to Intent("Open sound settings", Risk.SAFE, android.provider.Settings.ACTION_SOUND_SETTINGS),
            Regex("\\b(open|show)\\s+(app|application) settings\\b", RegexOption.IGNORE_CASE) to Intent("Open app settings", Risk.SAFE, android.provider.Settings.ACTION_APPLICATION_SETTINGS),
            Regex("\\b(open|show)\\s+settings\\b", RegexOption.IGNORE_CASE) to Intent("Open Android settings", Risk.SAFE, android.provider.Settings.ACTION_SETTINGS)
        )
        safe.firstOrNull { it.first.containsMatchIn(s) }?.let { return it.second }
        if (Regex("\\b(call|dial|sms|text someone|send message|delete|install|send)\\b", RegexOption.IGNORE_CASE).containsMatchIn(s))
            return Intent("Potentially consequential action", Risk.CONFIRM)
        return null
    }
}