package com.hermes.assistant

internal object VyomFactoryComplaintPrefixPolicy {
    private val PREFIXES = listOf("factory complaint:", "register complaint:", "complaint:", "report bug:", "factory:", "issue:")
    fun extract(message: String): String? {
        val trimmed = message.trim()
        val normalized = trimmed.lowercase()
        val prefix = PREFIXES.firstOrNull { normalized.startsWith(it) } ?: return null
        val body = trimmed.substring(prefix.length).trim().takeIf { it.isNotBlank() } ?: return null
        return if (prefix == "factory complaint:" || prefix == "factory:") "Vyom Factory complaint: $body" else body
    }
}

/**
 * Typed complaint envelope shared by Factory intake adapters.
 * The Factory register accepts defects from every Vyom product area; implementation remains backlog work after APK 1.
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
            "termux", "chatgpt", "claude", "codex", "vyom", "share_sheet"
        )

        private fun classifyFactoryArea(text: String): String? {
            val lower = text.lowercase()
            val mentionsFactory = Regex("\\bvyom\\s+factory\\b").containsMatchIn(lower) || listOf("factory worker", "factory queue", "factory complaint", "factory triage", "factory voice intake", "factory ui", "factory screen", "complaint card", "automations screen", "factory registry", "factory proposal", "factory executor", "factory update", "factory issue", "factory dialog", "factory timeline", "factory audit", "factory intake", "factory verification", "factory runner", "factory task", "factory content", "factory token").any { lower.contains(it) }
            if (!mentionsFactory) return null
            return when {
                listOf("a18", "content update", "signed update", "content package", "rollback", "update channel").any { lower.contains(it) } -> "update"
                listOf("worker", "executor", "execution", "task queue", "queue", "lease", "retry", "runner", "claim", "stop", "pause", "task failure", "postcondition action").any { lower.contains(it) } -> "execution"
                listOf("triage", "priority", "classif", "routing", "route", "assignment", "intake", "register complaint", "complaint register").any { lower.contains(it) } -> "triage"
                listOf("verification", "verify", "probe", "postcondition", "evidence", "closure", "close complaint", "reopen", "package compatibility", "phone test").any { lower.contains(it) } -> "verification"
                listOf("audit", "timeline", "activity log", "history", "handoff").any { lower.contains(it) } -> "audit"
                listOf("proposal", "self-change", "pull request", " pr ", "exact sha", "signed artifact", "code fix").any { lower.contains(it) } -> "proposal"
                listOf("github token", "signing secret", "encrypted", "credential", "permission", "protected scope", "project rule").any { lower.contains(it) } -> "security"
                listOf("factory ui", "factory screen", "complaint card", "automations screen", "factory dialog").any { lower.contains(it) } -> "ui"
                listOf("complaint intake", "complaint register", "intake parser", "intake flow").any { lower.contains(it) } -> "intake"
                else -> null
            }
        }

        private fun classifyProductArea(text: String): String {
            val lower = text.lowercase()
            return when {
                listOf("whatsapp", "wa message", "whats app").any { lower.contains(it) } -> "whatsapp"
                listOf("play store", "playstore", "install app", "app install").any { lower.contains(it) } -> "play_store"
                listOf("floating orb", "assistant bubble", "overlay orb", "system-wide orb").any { lower.contains(it) } -> "floating_orb"
                listOf("memory", "remember", "forgot", "saved fact", "episodic", "semantic retrieval").any { lower.contains(it) } -> "memory"
                listOf("design", "layout", "spacing", "theme", "typography", "screen ui", "composer").any { lower.contains(it) } -> "design"
                listOf("personality", "persona", "tone", "speaking style").any { lower.contains(it) } -> "personality"
                listOf("github repo", "repository", "repo ", "repo:", "git clone", "pull request").any { lower.contains(it) } -> "repo"
                listOf("skill", "skills", "learn anywhere").any { lower.contains(it) } -> "skill"
                listOf("schedule", "scheduled", "recurring task", "automation timer", "alarm").any { lower.contains(it) } -> "schedule"
                listOf("voice", "speech", "stt", "tts", "microphone", "transcription", "playback").any { lower.contains(it) } -> "voice"
                listOf("update", "upgrade", "apk install", "apk update", "release channel", "version install").any { lower.contains(it) } -> "update"
                listOf("android control", "accessibility", "tap ", "swipe", "read screen", "open app", "power mode", "shizuku", "adb", "control engine", "gesture", "factory reset").any { lower.contains(it) } -> "android_control"
                listOf("control", "action", "button").any { lower.contains(it) } -> "control"
                else -> "other"
            }
        }

        fun fromFreeText(source: String, text: String): VyomFactoryComplaintRecord {
            require(source in SOURCES) { "Unsupported complaint source" }
            val original = text.trim()
            require(original.isNotEmpty() && original.length <= 12_000) { "Complaint must contain 1–12,000 characters" }
            val lower = original.lowercase()
            val area = classifyFactoryArea(original) ?: classifyProductArea(original)
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
                "update", "control", "android_control", "execution", "security" -> "P0"
                "memory", "design", "triage", "verification", "proposal", "intake" -> "P1"
                "voice", "audit", "ui", "personality", "repo", "skill", "schedule", "whatsapp", "play_store", "floating_orb" -> "P2"
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
