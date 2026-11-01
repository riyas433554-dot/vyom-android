package com.hermes.assistant

/**
 * Keeps optional on-device context small enough for the Hermes chat endpoint.
 * The current user message is deliberately sent separately and is never folded
 * into this context budget.
 */
internal object ChatContextPolicy {
    fun buildContext(memoryFacts: String, history: String, maxChars: Int = 2_000): String {
        require(maxChars >= 0) { "maxChars must not be negative" }
        val memory = memoryFacts.trim()
        val recent = history.trim()
        if (memory.isEmpty() && recent.isEmpty() || maxChars == 0) return ""

        val memoryLabel = if (memory.isEmpty()) "" else "On-device memory facts (user-controlled):\n$memory"
        val historyLabel = if (recent.isEmpty()) "" else "Recent conversation context:\n$recent"
        val complete = listOf(memoryLabel, historyLabel).filter { it.isNotEmpty() }.joinToString("\n\n")
        if (complete.length <= maxChars) return complete
        if (memoryLabel.isEmpty()) return historyLabel.takeLast(maxChars)
        if (historyLabel.isEmpty()) return memoryLabel.takeLast(maxChars)

        val memoryBudget = (maxChars / 2).coerceAtMost(memoryLabel.length)
        val historyBudget = maxChars - memoryBudget
        return buildString(maxChars) {
            append(memoryLabel.takeLast(memoryBudget))
            append("\n")
            append(historyLabel.takeLast(historyBudget - 1))
        }
    }

    fun shouldRetryWithRaw(statusCode: Int, contextualMessage: String, rawMessage: String): Boolean =
        statusCode == 400 && rawMessage.isNotBlank() && rawMessage != contextualMessage

    fun connectionRetryDelayMillis(attempt: Int): Long =
        500L * (1L shl attempt.coerceIn(0, 3))
}
