package com.hermes.assistant

internal object VyomAuditPolicy {
    const val MAX_AUDIT_TEXT = 160

    fun sanitize(value: String?): String {
        val clean = value.orEmpty()
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (clean.isBlank()) return "—"
        if (MemoryCapturePolicy.isSensitive(clean)) return "[REDACTED]"
        return clean.take(MAX_AUDIT_TEXT)
    }
}
