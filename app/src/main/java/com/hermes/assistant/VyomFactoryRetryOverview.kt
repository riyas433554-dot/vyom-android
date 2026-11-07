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
        val waiting = ordered.size - due
        // Highlight the next future retry even when another task is already due.
        val next = ordered.firstOrNull { it.nextAttemptAt > now }
            ?: ordered.filter { it.nextAttemptAt <= now }.minByOrNull { it.issueNumber }
        val label = if (next == null) {
            "RETRIES · NONE"
        } else {
            val nextLabel = if (next.nextAttemptAt <= now) "DUE NOW"
                else "IN ${remaining(next.nextAttemptAt - now)}"
            "$due DUE · $waiting SCHEDULED · NEXT #${next.issueNumber} $nextLabel"
        }
        return Summary(ordered.size, due, waiting, next, label)
    }

    private fun remaining(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0)
        val seconds = safeMillis / 1_000 + if (safeMillis % 1_000 == 0L) 0 else 1
        val minutes = seconds / 60
        val remainder = seconds % 60
        return if (minutes > 0) "${minutes}m ${remainder}s" else "${seconds}s"
    }
}
