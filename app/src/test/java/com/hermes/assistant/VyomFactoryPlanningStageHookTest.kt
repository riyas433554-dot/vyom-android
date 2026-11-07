package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryPlanningStageHookTest {
    private fun task(number: Int, state: String, status: String) =
        VyomFactoryGitHubClient.Task(
            number, "Task $number", "planning output #$number", state,
            "https://github.com/example/repo/issues/$number", "2026-11-07T00:00:00Z",
            VyomFactoryStatus.Metadata("P2", status, "factory", "task-$number", "factory:task-$number", emptyList())
        )

    @Test fun planningHookInvokesDetectorOnlyForOpenClaimableTasks() {
        val calls = mutableListOf<Pair<Int, String>>()
        val hook = VyomFactoryPlanningStageHook { task, output ->
            calls += task.number to output
        }

        hook.inspect(
            listOf(
                task(1, "open", "open"),
                task(2, "open", "in_progress"),
                task(3, "closed", "completed"),
                task(4, "open", "completed")
            )
        )

        assertEquals(
            listOf(1 to "planning output #1", 2 to "planning output #2"),
            calls
        )
    }
}
