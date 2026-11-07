package com.hermes.assistant

import android.content.SharedPreferences
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
        assertEquals(listOf("IDEA: Add export support"), events.first().findingSummaries)
        assertEquals(listOf("BUG: Null output on empty input"), events[1].findingSummaries)
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

        val result = detector.onVerificationResult(parent, passed = true, evidence = "Expected result observed")

        assertFalse(result.detectorFailed)
        assertFalse(result.journaled)
        assertEquals(VyomFactoryStageDetector.Outcome.NOTHING_CREATED, result.outcome)
    }
    @Test fun stageDetectorJournalPersistsEachLifecycleOutcome() {
        val prefs = MemoryPrefs()
        val journal = VyomFactoryStageDetectorJournal(prefs)
        val event = VyomFactoryStageDetector.Event(
            parentIssueNumber = 26,
            stage = VyomFactoryTaskIntelligence.Stage.IMPLEMENTATION,
            checkpoint = "before_adapter_dispatch",
            outcome = VyomFactoryStageDetector.Outcome.NOTHING_CREATED,
            findingCount = 0,
            createdIssueNumbers = emptyList(),
            duplicateCount = 0,
            detail = "nothing found",
            recordedAt = 123L
        )

        journal.record(event)

        assertEquals(listOf(event), VyomFactoryStageDetectorJournal(prefs).all())
    }

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int) = defValue
        override fun getLong(key: String, defValue: Long) = defValue
        override fun getFloat(key: String, defValue: Float) = defValue
        override fun getBoolean(key: String, defValue: Boolean) = defValue
        override fun contains(key: String) = key in values
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        private inner class Editor : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            override fun putString(key: String, value: String?) = apply { changes[key] = value } as SharedPreferences.Editor
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = this
            override fun putLong(key: String, value: Long) = this
            override fun putFloat(key: String, value: Float) = this
            override fun putBoolean(key: String, value: Boolean) = this
            override fun remove(key: String) = apply { removals += key } as SharedPreferences.Editor
            override fun clear() = this
            override fun commit(): Boolean {
                removals.forEach(values::remove)
                changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                return true
            }
            override fun apply() { commit() }
        }
    }

}
