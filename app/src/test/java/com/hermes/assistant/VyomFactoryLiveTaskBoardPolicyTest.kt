package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryLiveTaskBoardPolicyTest {
    @Test fun activeFilterIncludesOpenAndInProgressButNotPausedOrBlocked() {
        val tasks = listOf(task(1, "open"), task(2, "in_progress"), task(3, "paused"), task(4, "blocked"))

        assertEquals(listOf(1, 2), VyomFactoryLiveTaskBoardPolicy.filter(
            tasks, VyomFactoryLiveTaskBoardPolicy.Filter.ACTIVE, ""
        ).map { it.number })
    }

    @Test fun searchMatchesIssueNumberTitleDescriptionPriorityLaneAndStatus() {
        val tasks = listOf(
            task(12, "blocked", title = "Fix scrolling", body = "Board clips long task detail", priority = "P0"),
            task(13, "open", title = "Improve queue", lane = VyomFactoryLaneRouter.Lane.INSTANT)
        )

        assertEquals(listOf(12), VyomFactoryLiveTaskBoardPolicy.filter(
            tasks, VyomFactoryLiveTaskBoardPolicy.Filter.ALL, "clips long"
        ).map { it.number })
        assertEquals(listOf(12), VyomFactoryLiveTaskBoardPolicy.filter(
            tasks, VyomFactoryLiveTaskBoardPolicy.Filter.ALL, "p0"
        ).map { it.number })
        assertEquals(listOf(13), VyomFactoryLiveTaskBoardPolicy.filter(
            tasks, VyomFactoryLiveTaskBoardPolicy.Filter.ALL, "instant"
        ).map { it.number })
    }

    @Test fun countsUseCanonicalRepositoryStatuses() {
        val counts = VyomFactoryLiveTaskBoardPolicy.counts(listOf(
            task(1, "open"), task(2, "in_progress"), task(3, "blocked"),
            task(4, "completed"), task(5, "paused")
        ))

        assertEquals(VyomFactoryLiveTaskBoardPolicy.Counts(5, 2, 1, 1), counts)
    }

    @Test fun blankSearchKeepsAllTasksAndIsCaseInsensitive() {
        val tasks = listOf(task(1, "open", title = "Fix Android UI"))

        assertEquals(1, VyomFactoryLiveTaskBoardPolicy.filter(
            tasks, VyomFactoryLiveTaskBoardPolicy.Filter.ALL, "  "
        ).size)
        assertEquals(1, VyomFactoryLiveTaskBoardPolicy.filter(
            tasks, VyomFactoryLiveTaskBoardPolicy.Filter.ALL, "ANDROID"
        ).size)
    }

    private fun task(
        number: Int,
        status: String,
        title: String = "Task $number",
        body: String = "Description for task $number",
        priority: String = "P2",
        lane: VyomFactoryLaneRouter.Lane = VyomFactoryLaneRouter.Lane.CUSTOM
    ) = VyomFactoryGitHubClient.Task(
        number = number,
        title = title,
        body = body,
        state = if (status == "completed" || status == "cancelled") "closed" else "open",
        url = "https://github.com/example/repo/issues/$number",
        updatedAt = "2026-11-14T06:00:00Z",
        metadata = VyomFactoryStatus.Metadata(
            priority = priority,
            status = status,
            source = "factory",
            sourceId = "task-$number",
            idempotencyKey = "factory:task-$number",
            events = emptyList(),
            lane = lane
        ),
        createdAt = "2026-11-13T06:00:00Z"
    )
}
