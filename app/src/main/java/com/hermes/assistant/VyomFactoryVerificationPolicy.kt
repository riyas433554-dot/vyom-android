package com.hermes.assistant

import java.security.MessageDigest

/** Closure gate shared by Factory UI and GitHub issue updates. */
internal object VyomFactoryVerificationPolicy {
    fun assignedProbe(description: String): String? {
        val value = Regex("(?im)^\\s*-\\s*\\*\\*Verification probe:\\*\\*\\s*(.+)$")
            .find(description)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (value.isBlank() || value.startsWith("UNASSIGNED", ignoreCase = true)) return null
        return value.takeIf { it.length <= 500 }
    }

    fun resultEvent(probe: String, evidence: String, passed: Boolean): String {
        require(probe.isNotBlank() && probe.length <= 500)
        require(evidence.isNotBlank() && evidence.length <= 1_000)
        return "probe_${if (passed) "pass" else "fail"}:${sha256(probe)}:${sha256(evidence)}"
    }

    fun canComplete(description: String, events: List<String>): Boolean {
        val probe = assignedProbe(description) ?: return false
        val prefix = "probe_pass:${sha256(probe)}:"
        return events.any { it.startsWith(prefix) }
    }

    fun blocksCompletion(
        currentStatus: String,
        newStatus: String,
        description: String,
        events: List<String>
    ): Boolean = newStatus == "completed" && currentStatus != "completed" &&
        !canComplete(description, events)

    fun appendEvidence(description: String, probe: String, evidence: String, passed: Boolean): String =
        description.trimEnd() + "\n\n## Owner-reported device verification\n" +
            "- **Probe:** $probe\n" +
            "- **Result:** ${if (passed) "PASS" else "FAIL"}\n" +
            "- **Evidence:** $evidence"

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
