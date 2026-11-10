package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryExecutionHistoryTest {
    @Test fun summarizesRealAttemptStatesAndOrdersNewestFirst() {
        val summary = VyomFactoryExecutionHistory.summarize(listOf(
            entry(1, "attempt-old", VyomFactoryExecutionJournal.State.SUCCEEDED, 10),
            entry(2, "attempt-fail", VyomFactoryExecutionJournal.State.FAILED, 30, "network_timeout"),
            entry(3, "attempt-live", VyomFactoryExecutionJournal.State.RUNNING, 20)
        ))

        assertEquals(3, summary.total)
        assertEquals(1, summary.active)
        assertEquals(1, summary.succeeded)
        assertEquals(1, summary.failed)
        assertEquals(0, summary.interrupted)
        assertEquals("#2 · FAILED · attempt- · network_timeout", summary.latest[0].label)
        assertEquals("#3 · RUNNING · attempt-", summary.latest[1].label)
    }

    @Test fun limitsRowsAndNeverSurfacesRawResultOrErrorMessage() {
        val entries = (1..8).map {
            entry(it, "attempt-$it", VyomFactoryExecutionJournal.State.FAILED, it.toLong(),
                "execution_error").copy(
                    result = "private result",
                    errorMessage = "private exception text"
                )
        }
        val summary = VyomFactoryExecutionHistory.summarize(entries)
        assertEquals(5, summary.latest.size)
        assertTrue(summary.latest.first().label.startsWith("#8"))
        assertFalse(summary.latest.joinToString().contains("private"))
    }

    @Test fun emptyJournalIsExplicit() {
        val summary = VyomFactoryExecutionHistory.summarize(emptyList())
        assertEquals("RUNS · NONE", summary.label)
        assertTrue(summary.latest.isEmpty())
    }

    private fun entry(
        issue: Int,
        attempt: String,
        state: VyomFactoryExecutionJournal.State,
        updated: Long,
        category: String? = null
    ) = VyomFactoryExecutionJournal.Entry(
        issueNumber = issue,
        attemptId = attempt,
        leaseId = "lease-$issue",
        ownerId = "owner-$issue",
        state = state,
        startedAt = updated,
        updatedAt = updated,
        errorCategory = category,
        operationKey = "android_action:set_media_volume"
    )
}
