package com.hermes.assistant

/** Structured, redacted audit event. Never accepts or records argv, output, or secrets. */
internal object ShizukuAudit {
    private val requestPattern = Regex("^[A-Za-z0-9-]{1,64}$")
    private val commands = setOf(
        "verify_secure_settings",
        "verify_overlay",
        "verify_accessibility_enabled",
        "verify_accessibility_services",
        "set_overlay"
    )
    private val outcomes = setOf(
        "success", "nonzero_exit", "timeout", "cancelled", "interrupted",
        "failed", "cancelled_startup", "rejected_duplicate"
    )

    fun format(requestId: String, commandId: String, outcome: String, exitCode: Int?): String {
        val safeRequest = if (requestId.matches(requestPattern)) requestId else "redacted"
        val safeCommand = if (commandId in commands) commandId else "unknown"
        val safeOutcome = if (outcome in outcomes) outcome else "failed"
        return buildString {
            append("request=$safeRequest command=$safeCommand outcome=$safeOutcome")
            if (exitCode != null) append(" exit=$exitCode")
        }
    }
}
