package com.hermes.assistant

/** Read-only, redacted summary of persisted Factory execution attempts. */
internal object VyomFactoryExecutionHistory {
    data class Row(
        val issueNumber: Int,
        val state: VyomFactoryExecutionJournal.State,
        val attemptId: String,
        val updatedAt: Long,
        val errorCategory: String?
    ) {
        val label: String
            get() = buildString {
                append("#").append(issueNumber)
                append(" · ").append(state.name)
                append(" · ").append(attemptId.take(8))
                if (state == VyomFactoryExecutionJournal.State.FAILED && !errorCategory.isNullOrBlank()) {
                    append(" · ").append(errorCategory)
                }
            }
    }

    data class Summary(
        val total: Int,
        val active: Int,
        val succeeded: Int,
        val failed: Int,
        val interrupted: Int,
        val latest: List<Row>,
        val label: String
    )

    fun summarize(entries: List<VyomFactoryExecutionJournal.Entry>, limit: Int = 5): Summary {
        require(limit in 1..20)
        val ordered = entries.sortedWith(
            compareByDescending<VyomFactoryExecutionJournal.Entry> { it.updatedAt }
                .thenByDescending { it.issueNumber }
                .thenByDescending { it.attemptId }
        )
        val active = ordered.count {
            it.state == VyomFactoryExecutionJournal.State.CLAIMED ||
                it.state == VyomFactoryExecutionJournal.State.RUNNING ||
                it.state == VyomFactoryExecutionJournal.State.STOP_REQUESTED
        }
        val succeeded = ordered.count { it.state == VyomFactoryExecutionJournal.State.SUCCEEDED }
        val failed = ordered.count { it.state == VyomFactoryExecutionJournal.State.FAILED }
        val interrupted = ordered.count { it.state == VyomFactoryExecutionJournal.State.INTERRUPTED }
        val label = if (ordered.isEmpty()) {
            "RUNS · NONE"
        } else {
            "${ordered.size} TOTAL · $active ACTIVE · $succeeded VERIFIED · $failed FAILED · $interrupted INTERRUPTED"
        }
        return Summary(
            total = ordered.size,
            active = active,
            succeeded = succeeded,
            failed = failed,
            interrupted = interrupted,
            latest = ordered.take(limit).map {
                Row(it.issueNumber, it.state, it.attemptId, it.updatedAt, it.errorCategory)
            },
            label = label
        )
    }
}
