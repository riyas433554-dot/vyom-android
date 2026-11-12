package com.hermes.assistant

/** Read-only lane aggregates derived from the persisted execution journal. */
internal object VyomFactoryLaneMetrics {
    data class Summary(
        val activeWorkers: Int,
        val runs: Int,
        val failures: Int
    )

    fun summarize(
        entries: List<VyomFactoryExecutionJournal.Entry>
    ): Map<VyomFactoryLaneRouter.Lane, Summary> =
        VyomFactoryLaneRouter.Lane.values().associateWith { lane ->
            val laneEntries = entries.filter { it.lane == lane }
            Summary(
                activeWorkers = laneEntries
                    .filter { it.state in ACTIVE_STATES }
                    .map { it.workerId }
                    .distinct()
                    .size,
                runs = laneEntries.size,
                failures = laneEntries.count {
                    it.state == VyomFactoryExecutionJournal.State.FAILED
                }
            )
        }

    private val ACTIVE_STATES = setOf(
        VyomFactoryExecutionJournal.State.CLAIMED,
        VyomFactoryExecutionJournal.State.RUNNING,
        VyomFactoryExecutionJournal.State.STOP_REQUESTED
    )
}