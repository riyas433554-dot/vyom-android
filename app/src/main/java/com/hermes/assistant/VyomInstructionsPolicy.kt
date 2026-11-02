package com.hermes.assistant

/** Validates user-authored response preferences before storing or adding them to prompts. */
internal object VyomInstructionsPolicy {
    const val MAX_RULE_CHARS = 1_000

    fun normalizeRules(raw: String): String =
        raw.replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim().replace(Regex("[\\t ]+"), " ") }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .take(MAX_RULE_CHARS)

    fun isValidRules(raw: String): Boolean =
        raw.length <= MAX_RULE_CHARS && !MemoryCapturePolicy.isSensitive(raw)

    fun personaInstruction(persona: String): String = when (persona.trim().lowercase()) {
        "english" -> "Reply in clear English. Keep code and technical identifiers unchanged."
        "hindi" -> "Reply in natural Hindi using Devanagari script. Keep code and technical identifiers unchanged."
        "formal" -> "Use a professional, respectful and concise tone in clear English. Keep code and technical identifiers unchanged."
        "fun" -> "Use natural Hinglish in Latin script with a light, playful tone. Stay clear and respectful; keep code and technical identifiers unchanged."
        else -> "Use natural Hinglish in Latin script, mixing Hindi and English naturally. Keep code and technical identifiers unchanged."
    }
}
