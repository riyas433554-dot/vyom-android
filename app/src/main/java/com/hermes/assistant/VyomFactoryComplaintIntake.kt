package com.hermes.assistant

import java.security.MessageDigest
import java.util.Locale

/** Shared, source-attributed intake boundary for external agent and Termux complaints. */
internal object VyomFactoryComplaintIntake {
    data class Intake(
        val source: String,
        val sourceId: String,
        val originalText: String,
        val record: VyomFactoryComplaintRecord
    )

    private val SOURCES = setOf("chatgpt", "claude", "codex", "termux", "vyom", "share_sheet")
    private val PACKAGE_SOURCES = mapOf(
        "com.openai.chatgpt" to "chatgpt",
        "com.anthropic.claude" to "claude",
        "com.openai.codex" to "codex",
        "com.termux" to "termux",
        "com.vyom.assistant" to "vyom",
        "com.hermes.assistant" to "vyom"
    )
    private val SOURCE_TAG = Regex("(?is)^\\s*\\[source:(chatgpt|claude|codex|termux|vyom)\\]\\s*")

    fun resolveSource(declaredSource: String?, callerPackage: String?): String {
        val declared = declaredSource?.trim()?.lowercase(Locale.ROOT)
        if (declared in SOURCES) return declared!!
        val caller = callerPackage?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return PACKAGE_SOURCES[caller] ?: "share_sheet"
    }

    fun fromExternalShare(
        source: String,
        text: String
    ): Intake {
        val declared = source.trim().lowercase(Locale.ROOT)
        require(declared in SOURCES) { "Unsupported complaint source" }
        val raw = text.trim()
        require(raw.isNotEmpty() && raw.length <= 12_000) {
            "Complaint must contain 1–12,000 characters"
        }
        val tag = SOURCE_TAG.find(raw)
        val resolvedSource = tag?.groupValues?.getOrNull(1)?.lowercase(Locale.ROOT)
            ?.takeIf { declared == "share_sheet" } ?: declared
        val complaint = if (tag != null) raw.removeRange(tag.range).trim() else raw
        require(complaint.isNotEmpty() && complaint.length <= 12_000) {
            "Complaint must contain 1–12,000 characters"
        }
        val record = VyomFactoryComplaintRecord.fromFreeText(resolvedSource, complaint)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((resolvedSource + "\n" + complaint.trim().replace(Regex("\\s+"), " ")).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return Intake(
            source = resolvedSource,
            sourceId = "external-$digest",
            originalText = complaint,
            record = record
        )
    }
}
