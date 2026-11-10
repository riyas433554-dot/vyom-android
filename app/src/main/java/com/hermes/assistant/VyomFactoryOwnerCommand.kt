package com.hermes.assistant

/** Parses explicit owner commands without guessing which complaint "this" refers to. */
internal object VyomFactoryOwnerCommand {
    private val patterns = listOf(
        Regex("^ye\\s+complaint\\s+pehle\\s+#?(\\d+)$", RegexOption.IGNORE_CASE),
        Regex("^(?:complaint|issue)\\s+#?(\\d+)\\s+(?:pehle|first)$", RegexOption.IGNORE_CASE)
    )

    fun promoteFirstIssueNumber(message: String): Int? {
        val normalized = message.trim()
        val match = patterns.firstNotNullOfOrNull { it.find(normalized) } ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }
    }
}
