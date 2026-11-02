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

        val memoryHeader = "On-device memory (newest preferences first):"
        val historyHeader = "Recent conversation:"
        if (memory.isEmpty()) return sectionTail(historyHeader, recent, maxChars)
        if (recent.isEmpty()) return sectionTail(memoryHeader, memory, maxChars)

        val fixedChars = memoryHeader.length + historyHeader.length + 3
        val contentBudget = (maxChars - fixedChars).coerceAtLeast(0)
        val memoryBudget = minOf(memory.length, contentBudget / 2)
        val historyBudget = minOf(recent.length, contentBudget - memoryBudget)
        val remaining = contentBudget - memoryBudget - historyBudget
        val finalMemoryBudget = minOf(memory.length, memoryBudget + remaining)
        val finalHistoryBudget = minOf(recent.length, historyBudget + (remaining - (finalMemoryBudget - memoryBudget)))
        return buildString(maxChars) {
            append(memoryHeader).append('\n').append(tailLines(memory, finalMemoryBudget))
            append("\n\n").append(historyHeader).append('\n').append(tailLines(recent, finalHistoryBudget))
        }.take(maxChars)
    }

    private fun sectionTail(header: String, value: String, maxChars: Int): String {
        if (maxChars <= header.length) return header.take(maxChars)
        val bodyBudget = maxChars - header.length - 1
        return header + "\n" + tailLines(value, bodyBudget)
    }

    /** Keep complete newest lines; if the newest line alone is too long, keep its suffix. */
    private fun tailLines(value: String, budget: Int): String {
        if (budget <= 0) return ""
        if (value.length <= budget) return value
        val lines = value.split('\n')
        val kept = ArrayDeque<String>()
        var used = 0
        for (line in lines.asReversed()) {
            val cost = line.length + if (kept.isEmpty()) 0 else 1
            if (used + cost <= budget) {
                kept.addFirst(line)
                used += cost
            } else if (kept.isEmpty()) {
                return line.takeLast(budget)
            } else {
                break
            }
        }
        return kept.joinToString("\n")
    }

    fun shouldRetryWithRaw(statusCode: Int, contextualMessage: String, rawMessage: String): Boolean =
        statusCode == 400 && rawMessage.isNotBlank() && rawMessage != contextualMessage

    fun connectionRetryDelayMillis(attempt: Int): Long =
        500L * (1L shl attempt.coerceIn(0, 3))
}
