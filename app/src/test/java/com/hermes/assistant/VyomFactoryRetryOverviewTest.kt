package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryRetryOverviewTest {
    @Test fun emptyQueueIsExplicit() {
        assertEquals("RETRIES · NONE", VyomFactoryRetryOverview.summarize(emptyList(), 10_000).label)
    }

    @Test fun separatesDueFromScheduledAndShowsEarliestIssue() {
        val schedules = listOf(
            schedule(12, 50_000),
            schedule(7, 20_000),
            schedule(9, 30_000)
        )
        val overview = VyomFactoryRetryOverview.summarize(schedules, 25_000)
        assertEquals(3, overview.total)
        assertEquals(1, overview.due)
        assertEquals(2, overview.waiting)
        assertEquals("1 DUE · 2 SCHEDULED · NEXT #9 IN 5s", overview.label)
    }

    @Test fun dueOnlyQueueDoesNotShowMisleadingFutureCountdown() {
        val overview = VyomFactoryRetryOverview.summarize(
            listOf(schedule(8, 10_000), schedule(3, 20_000)),
            25_000
        )
        assertEquals("2 DUE · 0 SCHEDULED · NEXT #3 DUE NOW", overview.label)
    }

    @Test fun countdownRoundsUpAndSwitchesToMinutes() {
        assertEquals("1 DUE · 0 SCHEDULED · NEXT #4 IN 1s",
            VyomFactoryRetryOverview.summarize(listOf(schedule(4, 10_001)), 10_000).label)
        assertEquals("0 DUE · 1 SCHEDULED · NEXT #4 IN 2m 1s",
            VyomFactoryRetryOverview.summarize(listOf(schedule(4, 130_001)), 10_000).label)
    }

    private fun schedule(issue: Int, next: Long) = VyomFactoryRetryScheduleStore.Schedule(
        issueNumber = issue,
        operationKey = "android_action:set_media_volume",
        failedAttempts = 1,
        errorCategory = "network_timeout",
        scheduledAt = 1_000,
        nextAttemptAt = next
    )
}
