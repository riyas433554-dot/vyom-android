package com.hermes.assistant

/** Read-only view of persisted retry eligibility; it never triggers execution. */
internal object VyomFactoryRetryOverview {
    data class Summary(
        val total: Int,
        val due: Int,
        val waiting: Int,
        val next: VyomFactoryRetryScheduleStore.Schedule?,
        val label: String
    )

    fun summarize(
        schedules: List<VyomFactoryRetryScheduleStore.Schedule>,
        now: Long
    ): Summary {
        require(now >= 0)
        val ordered = schedules.sortedWith(
            compareBy<VyomFactoryRetryScheduleStore.Schedule> { it.nextAttemptAt }
                .thenBy { it.issueNumber }
                .thenBy { it.operationKey }
        )
        val due = ordered.count { it.nextAttemptAt <= now }
        val next = ordered.firstOrNull()
        val waiting = ordered.size - due
        val label = when {
            next == null -> "RETRIES · NONE"
            due > 0 && waiting == 0 -> "$due DUE · 0 SCHEDULED · NEXT #${next.issueNumber} DUE NOW"
            due > 0 -> "$due DUE · $waiting SCHEDULED · NEXT #${next.issueNumber} DUE NOW"
            else -> "$due DUE · $waiting SCHEDULED · NEXT #${next.issueNumber} IN ${remaining(next.nextAttemptAt - now)}"
        }
        return Summary(ordered.size, due, waiting, next, label)
    }

    private fun remaining(millis: Long): String {
        val seconds = ((millis.coerceAtLeast(0) + 999) / 1_000)
        val minutes = seconds / 60
        val remainder = seconds % 60
        return if (minutes > 0) "${minutes}m ${remainder}s" else "${seconds}s"
    }
}
