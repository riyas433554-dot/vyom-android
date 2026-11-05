package com.hermes.assistant

/**
 * Typed complaint envelope shared by Factory intake adapters.
 * Only defects explicitly about Vyom Factory's own workflow enter its canonical register.
 */
internal data class VyomFactoryComplaintRecord(
    val source: String,
    val originalText: String,
    val area: String,
    val severity: String,
    val priority: String,
    val evidence: String,
    val reproSteps: String,
    val expectedResult: String,
    val verificationProbe: String,
    val fixType: String
) {
    fun toIssueDescription(): String = buildString {
        appendLine("## Complaint register record")
        appendLine("- **Source:** $source")
        appendLine("- **Area:** $area")
        appendLine("- **Severity:** $severity")
        appendLine("- **Priority:** $priority")
        appendLine("- **Evidence:** $evidence")
        appendLine("- **Repro steps:** $reproSteps")
        appendLine("- **Expected result:** $expectedResult")
        appendLine("- **Verification probe:** $verificationProbe")
        appendLine("- **Fix type:** $fixType")
        appendLine()
        appendLine("### Original complaint (sensitive text redacted when detected)")
        appendLine(originalText)
        appendLine()
        append("### Closure rule\nClose only after the named verification probe passes on the device; otherwise keep/reopen with the observed result.")
    }

    companion object {
        private val SOURCES = setOf(
            "factory", "vyom_chat", "vyom_voice", "diagnostics", "task_failure",
            "termux", "chatgpt", "claude", "codex", "share_sheet"
        )

        private fun classifyFactoryArea(text: String): String? {
            val lower = text.lowercase()
            val mentionsFactory = Regex("\\bvyom\\s+factory\\b").containsMatchIn(lower) || listOf("factory worker", "factory queue", "factory complaint", "factory triage", "factory registry", "factory proposal", "factory executor", "factory update", "factory issue", "factory dialog", "factory timeline", "factory audit", "factory intake", "factory verification", "factory runner", "factory task", "factory content", "factory token").any { lower.contains(it) }
            if (!mentionsFactory) return null
            return when {
                listOf("a18", "content update", "signed update", "content package", "rollback", "update channel")
                    .any { lower.contains(it) } -> "update"
                listOf("worker", "executor", "execution", "task queue", "queue", "lease", "retry",
                    "runner", "claim", "stop", "pause", "task failure", "postcondition action")
                    .any { lower.contains(it) } -> "execution"
                listOf("triage", "priority", "classif", "routing", "route", "assignment", "intake", "register complaint")
                    .any { lower.contains(it) } -> "triage"
                listOf("verification", "verify", "probe", "postcondition", "evidence", "closure", "close complaint", "reopen")
                    .any { lower.contains(it) } -> "verification"
                listOf("audit", "timeline", "activity log", "history").any { lower.contains(it) } -> "audit"
                listOf("proposal", "self-change", "pull request", " pr ", "exact sha", "signed artifact", "code fix")
                    .any { lower.contains(it) } -> "proposal"
                listOf("github token", "signing secret", "encrypted", "credential", "permission", "protected scope", "project rule")
                    .any { lower.contains(it) } -> "security"
                listOf("factory ui", "factory screen", "complaint card", "automations screen", "factory dialog")
                    .any { lower.contains(it) } -> "ui"
                listOf("complaint", "factory").any { lower.contains(it) } -> "intake"
                else -> "other"
            }
        }

        fun fromFreeText(source: String, text: String): VyomFactoryComplaintRecord {
            require(source in SOURCES) { "Unsupported complaint source" }
            val original = text.trim()
            require(original.isNotEmpty() && original.length <= 12_000) { "Complaint must contain 1–12,000 characters" }
            val area = classifyFactoryArea(original)
                ?: throw IllegalArgumentException("Out of scope: only Vyom Factory workflow complaints can enter the Factory register")
            val lower = original.lowercase()
            val severity = when {
                listOf("security", "unsafe", "data loss", "crash", "credential leak").any { lower.contains(it) } -> "critical"
                listOf(
                    "fail", "failed", "broken", "error", "not working", "does not work", "doesn't work",
                    "nahi ho raha", "nahi ho rahi", "nahi hota", "nahi hoti",
                    "nahi chal raha", "nahi chal rahi", "kaam nahi kar"
                ).any { lower.contains(it) } -> "high"
                else -> "medium"
            }
            val priority = when (area) {
                "update", "execution", "security" -> "P0"
                "triage", "verification", "proposal", "intake" -> "P1"
                "audit", "ui" -> "P2"
                else -> "P3"
            }
            val fixType = if (area == "update" && listOf("content", "a18", "no-apk").any { lower.contains(it) }) {
                "content update with no APK"
            } else {
                "native change needing an APK"
            }
            val sensitive = MemoryCapturePolicy.isSensitive(original)
            val safeOriginal = if (sensitive) "[REDACTED: sensitive complaint text withheld from the shared issue]" else original
            val evidence = when {
                sensitive -> "Sensitive text was redacted before shared issue registration; review the original locally in Vyom."
                Regex("(?im)^\\s*(log|screenshot|diagnostics result):").containsMatchIn(original) ->
                    "Owner-supplied evidence is included in the original complaint; review and attach it to this issue."
                else -> "No log, screenshot or Diagnostics result supplied at intake; collect during reproduction."
            }
            return VyomFactoryComplaintRecord(
                source = source,
                originalText = safeOriginal,
                area = area,
                severity = severity,
                priority = priority,
                evidence = evidence,
                reproSteps = "Use the original complaint as the reproduction context. Detailed step-by-step reproduction was not supplied; obtain it during triage.",
                expectedResult = "The reported Vyom Factory workflow behavior should match the owner's stated intent. A measurable post-condition must be confirmed during triage.",
                verificationProbe = "UNASSIGNED at intake: triage must define a named automated test and observable post-condition probe before implementation or closure.",
                fixType = fixType
            )
        }
    }
}
