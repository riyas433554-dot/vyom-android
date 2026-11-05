package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryTaskOrderingTest {
    @Test fun priorityIsPrimaryAndOldestCreatedIssueLeadsItsTier() {
        val tasks = listOf(
            task(10, "P0", "2026-11-04T12:00:00Z", "2026-11-01T09:00:00Z"),
            task(8, "P1", "2026-11-04T13:00:00Z", "2026-11-01T08:00:00Z"),
            task(12, "P0", "2026-11-04T14:00:00Z", "2026-11-02T09:00:00Z"),
            task(7, "P2", "2026-11-04T15:00:00Z", "2026-11-01T07:00:00Z")
        )

        assertEquals(
            listOf(10, 12, 8, 7),
            VyomFactoryTaskOrdering.sort(tasks).map { it.number }
        )
    }

    @Test fun updatesAndOwnerPromotionsDoNotJumpOlderIssueWithinSamePriority() {
        val tasks = listOf(
            task(20, "P1", "2026-11-05T15:00:00Z", "2026-11-01T10:00:00Z"),
            task(21, "P1", "2026-11-05T16:00:00Z", "2026-11-02T10:00:00Z")
        )

        assertEquals(listOf(20, 21), VyomFactoryTaskOrdering.sort(tasks).map { it.number })
    }

    @Test fun equalCreationTimestampUsesStableIssueNumberTieBreak() {
        val tasks = listOf(
            task(15, "P1", "2026-11-04T12:00:00Z", "2026-11-01T12:00:00Z"),
            task(11, "P1", "2026-11-04T13:00:00Z", "2026-11-01T12:00:00Z")
        )

        assertEquals(listOf(11, 15), VyomFactoryTaskOrdering.sort(tasks).map { it.number })
    }

    private fun task(number: Int, priority: String, updatedAt: String, createdAt: String) =
        VyomFactoryGitHubClient.Task(
            number = number,
            title = "Complaint $number",
            body = "Body",
            state = "open",
            url = "https://github.com/example/repo/issues/$number",
            updatedAt = updatedAt,
            metadata = VyomFactoryStatus.Metadata(
                priority = priority,
                status = "open",
                source = "factory",
                sourceId = "task-$number",
                idempotencyKey = "factory:task-$number",
                events = emptyList()
            ),
            createdAt = createdAt
        )
}
