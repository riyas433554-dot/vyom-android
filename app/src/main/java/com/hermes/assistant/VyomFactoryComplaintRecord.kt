package com.hermes.assistant

/**
 * Typed complaint envelope shared by every Factory intake adapter.
 * Classification is deliberately conservative: unknown language/domain falls back to "other".
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

        fun fromFreeText(source: String, text: String): VyomFactoryComplaintRecord {
            require(source in SOURCES) { "Unsupported complaint source" }
            val original = text.trim()
            require(original.isNotEmpty() && original.length <= 12_000) { "Complaint must contain 1–12,000 characters" }
            val lower = original.lowercase()
            val area = when {
                listOf("open_app", "tap", "swipe", "scroll", "type", "read_screen", "screenshot",
                    "clipboard", "intent", "force_stop", "control", "accessibility", "android control",
                    "play store", "playstore", "install app", "app install", "whatsapp",
                    "send message", "message send")
                    .any { lower.contains(it) } -> "control"
                listOf("update", "apk", "rollback", "content package", "a18").any { lower.contains(it) } -> "update"
                listOf("memory", "remember", "forgot", "saved fact", "recall").any { lower.contains(it) } -> "memory"
                listOf("design", "layout", "theme", "spacing", "screen ui", "composer").any { lower.contains(it) } -> "design"
                listOf("personality", "persona", "tone").any { lower.contains(it) } -> "personality"
                listOf("github", "repository", "repo", "commit", "pull request", "code").any { lower.contains(it) } -> "repo"
                listOf("skill", "teach", "learned procedure").any { lower.contains(it) } -> "skill"
                listOf("schedule", "automation", "reminder", "timer", "recurring").any { lower.contains(it) } -> "schedule"
                listOf("voice", "speech", "microphone", "stt", "tts", "transcription").any { lower.contains(it) } -> "voice"
                else -> "other"
            }
            val severity = when {
                listOf("security", "unsafe", "data loss", "crash", "credential leak").any { lower.contains(it) } -> "critical"
                listOf("fail", "failed", "broken", "error", "not working", "does not work", "doesn't work").any { lower.contains(it) } -> "high"
                else -> "medium"
            }
            val priority = when (area) {
                "control", "update" -> "P0"
                "memory" -> "P1"
                "design" -> "P2"
                else -> "P3"
            }
            val fixType = if (area in setOf("skill", "personality") &&
                listOf("prompt", "wording", "persona", "skill content", "routing rule").any { lower.contains(it) }
            ) "content update with no APK" else "native change needing an APK"
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
                expectedResult = "The reported behavior should match the owner's stated intent. A measurable post-condition must be confirmed during triage.",
                verificationProbe = "UNASSIGNED at intake: triage must define a named automated test and on-device post-condition probe before implementation or closure.",
                fixType = fixType
            )
        }
    }
}
