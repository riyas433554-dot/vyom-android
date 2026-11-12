package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryLaneMetricsTest {
    @Test fun summarizesWorkersRunsAndFailuresIndependentlyForEachLane() {
        val instant = VyomFactoryLaneRouter.Lane.INSTANT
        val scheduled = VyomFactoryLaneRouter.Lane.SCHEDULED
        val entries = listOf(
            entry("i-1", "worker-a", instant, VyomFactoryExecutionJournal.State.RUNNING),
            entry("i-2", "worker-a", instant, VyomFactoryExecutionJournal.State.FAILED),
            entry("i-3", "worker-b", instant, VyomFactoryExecutionJournal.State.SUCCEEDED),
            entry("s-1", "worker-c", scheduled, VyomFactoryExecutionJournal.State.STOP_REQUESTED),
            entry("s-2", "worker-d", scheduled, VyomFactoryExecutionJournal.State.FAILED)
        )

        val summary = VyomFactoryLaneMetrics.summarize(entries)

        assertEquals(VyomFactoryLaneMetrics.Summary(1, 3, 1), summary.getValue(instant))
        assertEquals(VyomFactoryLaneMetrics.Summary(1, 2, 1), summary.getValue(scheduled))
    }

    @Test fun ignoresLegacyEntriesWithoutLaneAndCountsOnlyNonterminalWorkersAsActive() {
        val entries = listOf(
            entry("a-1", "worker-a", VyomFactoryLaneRouter.Lane.INSTANT, VyomFactoryExecutionJournal.State.CLAIMED),
            entry("a-2", "worker-a", VyomFactoryLaneRouter.Lane.INSTANT, VyomFactoryExecutionJournal.State.INTERRUPTED),
            VyomFactoryExecutionJournal.Entry(3, "legacy", "lease", "legacy-worker", null, null,
                VyomFactoryExecutionJournal.State.FAILED, 1L, 2L)
        )

        val summary = VyomFactoryLaneMetrics.summarize(entries)

        assertEquals(VyomFactoryLaneMetrics.Summary(1, 2, 0),
            summary.getValue(VyomFactoryLaneRouter.Lane.INSTANT))
        assertEquals(VyomFactoryLaneMetrics.Summary(0, 0, 0),
            summary.getValue(VyomFactoryLaneRouter.Lane.SCHEDULED))
    }

    private fun entry(
        attempt: String,
        worker: String,
        lane: VyomFactoryLaneRouter.Lane,
        state: VyomFactoryExecutionJournal.State
    ) = VyomFactoryExecutionJournal.Entry(
        1, attempt, "lease-$attempt", worker, lane, 0, state, 1L, 2L
    )
}