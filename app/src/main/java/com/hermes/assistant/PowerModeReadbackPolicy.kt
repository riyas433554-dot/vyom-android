package com.hermes.assistant

/** Parses only successful, exact Shizuku read-back responses; missing/ambiguous output stays UNKNOWN. */
internal object PowerModeReadbackPolicy {
    enum class Status { GRANTED, DENIED, ALLOWED, BLOCKED, ENABLED, DISABLED, UNKNOWN }

    fun secureSettings(result: String): Status = when (successfulOutput(result)) {
        "granted" -> Status.GRANTED
        "denied" -> Status.DENIED
        else -> Status.UNKNOWN
    }

    fun overlay(result: String): Status {
        val output = successfulOutput(result) ?: return Status.UNKNOWN
        val match = Regex("^SYSTEM_ALERT_WINDOW:\\s*(allow|ignore|deny|errored|default)(?:$|[;\\s])", RegexOption.IGNORE_CASE)
            .find(output) ?: return Status.UNKNOWN
        return when (match.groupValues[1].lowercase()) {
            "allow" -> Status.ALLOWED
            "ignore", "deny", "errored" -> Status.BLOCKED
            else -> Status.UNKNOWN
        }
    }

    fun accessibilityGlobalEnabled(result: String): Boolean? = when (successfulOutput(result)) {
        "1" -> true
        "0" -> false
        else -> null
    }

    fun accessibilityServiceEnabled(result: String, component: String): Boolean? {
        val output = successfulOutput(result) ?: return null
        if (!AccessibilityReconnectPolicy.canReconnect(true, component)) return null
        if (output == "null" || output.isBlank()) return false
        return AccessibilityReconnectPolicy.isEnabled(output, component)
    }

    private fun successfulOutput(result: String): String? {
        val prefix = "exit=0; output="
        if (!result.startsWith(prefix)) return null
        val output = result.removePrefix(prefix).trim()
        if (output.contains('\n') || output.contains('\r')) return null
        return output
    }
}
