package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryStageDetectorTest {
    private fun task(number: Int, title: String, body: String) =
        VyomFactoryGitHubClient.Task(
            number, title, body, "open",
            "https://github.com/example/repo/issues/$number",
            "2026-11-07T00:00:00Z",
            VyomFactoryStatus.Metadata("P2", "in_progress", "factory", "task-$number", "factory:task-$number", emptyList())
        )

    @Test fun planningImplementationAndVerificationHooksInvokeDetectorAndJournalEachResult() {
        val parent = task(21, "Parent task", "Current scope stays fixed")
        val stages = mutableListOf<VyomFactoryTaskIntelligence.Stage>()
        val events = mutableListOf<VyomFactoryStageDetector.Event>()
        val detector = VyomFactoryStageDetector(
            detect = { stage, output ->
                stages += stage
                VyomFactoryTaskIntelligence.detect(stage, output)
            },
            loadTasks = { listOf(parent) },
            createTask = { _, finding, _ -> task(30 + stages.size, finding.title, finding.detail) },
            recordEvent = { events += it }
        )

        detector.onPlanning(parent, "[IDEA] Add export support")
        detector.onImplementationCheckpoint(parent, "checkpoint_1", "[BUG] Null output on empty input")
        detector.onVerificationResult(parent, passed = true, evidence = "Expected result observed")

        assertEquals(
            listOf(
                VyomFactoryTaskIntelligence.Stage.PLANNING,
                VyomFactoryTaskIntelligence.Stage.IMPLEMENTATION,
                VyomFactoryTaskIntelligence.Stage.TEST
            ),
            stages
        )
        assertEquals(3, events.size)
        assertEquals(VyomFactoryStageDetector.Outcome.NOTHING_CREATED, events.last().outcome)
        assertEquals("Current scope stays fixed", parent.body)
    }

    @Test fun duplicateFindingIsJournaledButNeverCreatesAnotherTask() {
        val parent = task(22, "Parent task", "Current scope")
        val existing = task(23, "Add export support", "Add export support for reports")
        var creates = 0
        val events = mutableListOf<VyomFactoryStageDetector.Event>()
        val detector = VyomFactoryStageDetector(
            detect = { stage, output -> VyomFactoryTaskIntelligence.detect(stage, output) },
            loadTasks = { listOf(parent, existing) },
            createTask = { _, finding, _ -> creates++; task(40, finding.title, finding.detail) },
            recordEvent = { events += it }
        )

        detector.onPlanning(parent, "[IDEA] Add export support")

        assertEquals(0, creates)
        assertEquals(VyomFactoryStageDetector.Outcome.DUPLICATES_ONLY, events.single().outcome)
        assertEquals(1, events.single().duplicateCount)
    }

    @Test fun detectorFailureIsJournaledAndDoesNotEscapeIntoTaskExecution() {
        val parent = task(24, "Parent task", "Current scope")
        val events = mutableListOf<VyomFactoryStageDetector.Event>()
        val detector = VyomFactoryStageDetector(
            detect = { _, _ -> error("detector unavailable") },
            loadTasks = { listOf(parent) },
            createTask = { _, finding, _ -> task(50, finding.title, finding.detail) },
            recordEvent = { events += it }
        )

        val result = detector.onImplementationCheckpoint(parent, "checkpoint_2", "some output")

        assertTrue(result.detectorFailed)
        assertEquals(VyomFactoryStageDetector.Outcome.DETECTOR_FAILED, events.single().outcome)
        assertEquals("Current scope", parent.body)
    }

    @Test fun journalFailureDoesNotCrashTaskOrHideDetectorResult() {
        val parent = task(25, "Parent task", "Current scope")
        val detector = VyomFactoryStageDetector(
            detect = { _, _ -> emptyList() },
            loadTasks = { listOf(parent) },
            createTask = { _, finding, _ -> task(60, finding.title, finding.detail) },
            recordEvent = { error("journal unavailable") }
        )

        val result = detector.onVerificationResult(parent, passed = false, evidence = "Observed failure")

        assertFalse(result.detectorFailed)
        assertFalse(result.journaled)
        assertEquals(VyomFactoryStageDetector.Outcome.NOTHING_CREATED, result.outcome)
    }
}
