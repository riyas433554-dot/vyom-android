package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VyomFactoryLivePlanningStageIntegrationTest {
    private fun task(number: Int, state: String, status: String, body: String = "planning #$number") =
        VyomFactoryGitHubClient.Task(
            number = number,
            title = "Task $number",
            body = body,
            state = state,
            url = "https://github.com/example/repo/issues/$number",
            updatedAt = "2026-11-08T00:00:00Z",
            metadata = VyomFactoryStatus.Metadata(
                "P2", status, "factory", "task-$number", "factory:task-$number", emptyList()
            )
        )

    @Test fun livePlanningInvokesDetectorOnceAndSkipsClosedAndLocallyClaimedTasks() {
        val journal = mutableListOf<VyomFactoryStageDetector.Event>()
        val detector = VyomFactoryStageDetector(
            detect = { _, _ -> emptyList() },
            loadTasks = { emptyList() },
            createTask = { _, _, _ -> error("No task should be created") },
            recordEvent = { journal.add(it) }
        )
        val livePlanning = VyomFactoryLivePlanningStage(detector::onPlanning)

        livePlanning.inspect(
            tasks = listOf(
                task(1, "open", "open"),
                task(2, "closed", "completed"),
                task(3, "open", "in_progress")
            ),
            claimedIssueNumbers = setOf(3)
        )

        assertEquals(listOf(1), journal.map { it.parentIssueNumber })
        assertEquals(listOf(VyomFactoryStageDetector.Outcome.NOTHING_CREATED), journal.map { it.outcome })
        assertFalse(journal.any { it.parentIssueNumber == 2 || it.parentIssueNumber == 3 })
    }

    @Test fun livePlanningContainsDetectorFailureAndContinuesToNextTask() {
        val journal = mutableListOf<VyomFactoryStageDetector.Event>()
        val detector = VyomFactoryStageDetector(
            detect = { _, output ->
                if (output.contains("explode")) error("synthetic detector failure")
                emptyList()
            },
            loadTasks = { emptyList() },
            createTask = { _, _, _ -> error("No task should be created") },
            recordEvent = journal::add
        )
        val livePlanning = VyomFactoryLivePlanningStage(detector::onPlanning)

        livePlanning.inspect(
            tasks = listOf(
                task(10, "open", "open", "explode"),
                task(11, "open", "open", "normal planning")
            ),
            claimedIssueNumbers = emptySet()
        )

        assertEquals(listOf(10, 11), journal.map { it.parentIssueNumber })
        assertEquals(
            listOf(
                VyomFactoryStageDetector.Outcome.DETECTOR_FAILED,
                VyomFactoryStageDetector.Outcome.NOTHING_CREATED
            ),
            journal.map { it.outcome }
        )
    }
}
