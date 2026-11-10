package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryVerifiedProgressTest {
    private val probe = "Open the app and confirm the approved change is visible"
    private val commit = "a".repeat(40)
    private val verifiedResult =
        "repository_proposal_verified: issue=77; commit=$commit; " +
            "pr=https://github.com/riyas433554-dot/vyom-android/pull/9; " +
            "ci=Both required workflows succeeded for exact commit $commit"

    @Test fun noJournalCommitMeansZeroNotStarted() {
        val progress = calculate(task(), emptyList())
        assertEquals(0, progress.percent)
        assertEquals(VyomFactoryVerifiedProgress.Stage.NOT_STARTED, progress.stage)
        assertEquals("NOT_STARTED", progress.label)
    }

    @Test fun commitShaInLatestSuccessfulJournalMeansImplementationDoneAtTwentyFive() {
        val progress = calculate(task(), listOf(entry(result = "repository_proposal_verified: commit=$commit")))
        assertEquals(25, progress.percent)
        assertEquals(VyomFactoryVerifiedProgress.Stage.IMPLEMENTATION_DONE, progress.stage)
        assertEquals(commit, progress.commitSha)
        assertTrue(progress.implementationVerified)
        assertFalse(progress.ciVerified)
    }

    @Test fun missingOrMalformedCommitShaCannotProveImplementation() {
        listOf("repository_proposal_verified: commit=short", "proposal completed").forEach { result ->
            val progress = calculate(task(), listOf(entry(result = result)))
            assertEquals(0, progress.percent)
            assertFalse(progress.implementationVerified)
        }
    }

    @Test fun exactShaFullAndDebugPassMovesToCiVerifiedAtFifty() {
        val incompleteCriteria = task(
            description = description().replace("[x] Verify the required CI checks", "[ ] Verify the required CI checks")
        )
        val progress = calculate(incompleteCriteria, listOf(entry()))
        assertEquals(50, progress.percent)
        assertEquals(VyomFactoryVerifiedProgress.Stage.CI_VERIFIED, progress.stage)
        assertTrue(progress.ciVerified)
        assertFalse(progress.criteriaVerified)
    }

    @Test fun ciFailureOrPendingDoesNotPassCiGate() {
        listOf(
            "Full workflow failed for exact commit $commit",
            "Full workflow is waiting",
            "Both required workflows succeeded for exact commit ${"b".repeat(40)}"
        ).forEach { ci ->
            val result = "repository_proposal_verified: commit=$commit; pr=https://github.com/x/y/pull/1; ci=$ci"
            val progress = calculate(task(), listOf(entry(result = result)))
            assertEquals(25, progress.percent)
            assertFalse(progress.ciVerified)
        }
    }

    @Test fun checkedAcceptanceCriteriaMovesToNinetyNineWhenDeviceProbeIsRequired() {
        val progress = calculate(task(), listOf(entry()), events = emptyList())
        assertEquals(99, progress.percent)
        assertEquals(VyomFactoryVerifiedProgress.Stage.CRITERIA_VERIFIED, progress.stage)
        assertTrue(progress.criteriaVerified)
        assertTrue(progress.probeRequired)
        assertFalse(progress.probeVerified)
        assertEquals("PHONE-PENDING", progress.label)
    }

    @Test fun uncheckedAcceptanceCriteriaStopsAtFiftyEvenWhenCiPassed() {
        val incomplete = task(description = description().replace("[x] Add the reviewed implementation", "[ ] Add the reviewed implementation"))
        val progress = calculate(incomplete, listOf(entry()), events = listOf(passEvent()))
        assertEquals(50, progress.percent)
        assertFalse(progress.criteriaVerified)
        assertFalse(progress.probeVerified)
    }

    @Test fun exactNamedProbePassIsTheOnlyPathToOneHundred() {
        val progress = calculate(task(), listOf(entry()), events = listOf(passEvent()))
        assertEquals(100, progress.percent)
        assertEquals(VyomFactoryVerifiedProgress.Stage.PROBE_VERIFIED, progress.stage)
        assertTrue(progress.probeVerified)
    }

    @Test fun changedProbeInvalidatesEarlierPassEvidence() {
        val changed = task(description = description().replace(probe, "Open settings and confirm the new preference is visible"))
        val progress = calculate(changed, listOf(entry()), events = listOf(passEvent()))
        assertEquals(99, progress.percent)
        assertEquals(VyomFactoryVerifiedProgress.Stage.CRITERIA_VERIFIED, progress.stage)
        assertFalse(progress.probeVerified)
    }

    @Test fun noProbeRequiredAllowsCompletionAfterCriteriaVerification() {
        val noProbe = task(description = description().replace("- **Verification probe:** $probe\n\n", ""))
        val progress = calculate(noProbe, listOf(entry()))
        assertEquals(100, progress.percent)
        assertEquals(VyomFactoryVerifiedProgress.Stage.PROBE_VERIFIED, progress.stage)
        assertFalse(progress.probeRequired)
    }

    @Test fun newerStopFailureOrCrashInvalidatesAnOlderSuccess() {
        val olderSuccess = entry()
        val stopped = entry(attemptId = "attempt-2").copy(
            state = VyomFactoryExecutionJournal.State.STOP_REQUESTED,
            updatedAt = 3_000L,
            result = null
        )
        val crashed = stopped.copy(
            state = VyomFactoryExecutionJournal.State.INTERRUPTED,
            updatedAt = 4_000L
        )
        listOf(stopped, crashed, stopped.copy(state = VyomFactoryExecutionJournal.State.FAILED))
            .forEach { latest ->
                val progress = calculate(task(), listOf(olderSuccess, latest))
                assertEquals(0, progress.percent)
                assertFalse(progress.implementationVerified)
            }
    }

    @Test fun activeAttemptNeverPretendsToBeCommitted() {
        val running = entry().copy(state = VyomFactoryExecutionJournal.State.RUNNING, result = null)
        val progress = calculate(task(), listOf(running))
        assertEquals(0, progress.percent)
        assertTrue(progress.executionInProgress)
        assertEquals("IMPLEMENTATION RUNNING", progress.label)
    }

    @Test fun journalRestartPreservesTheSameProgressAndCommitReceipt() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(77, identity("worker"), 1_000, 20_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "persisted-attempt", 1_100, "repository_change:propose_change")
        assertTrue(journal.markRunning(lease, "persisted-attempt", 1_200))
        assertTrue(journal.succeed(lease, "persisted-attempt", 1_300, verifiedResult))

        val beforeRestart = VyomFactoryVerifiedProgress.calculate(task(), journal.all())
        val reopened = VyomFactoryExecutionJournal(prefs, leases)
        val afterRestart = VyomFactoryVerifiedProgress.calculate(task(), reopened.all())

        assertEquals(beforeRestart, afterRestart)
        assertEquals(99, afterRestart.percent)
        assertEquals(commit, afterRestart.commitSha)
    }

    @Test fun failedLatestAttemptAfterRestartCannotReuseOldSuccessfulReceipt() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(77, identity("worker"), 1_000, 20_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "success", 1_100, "repository_change:propose_change")
        assertTrue(journal.markRunning(lease, "success", 1_200))
        assertTrue(journal.succeed(lease, "success", 1_300, verifiedResult))
        journal.begin(lease, "stopped", 1_400, "repository_change:propose_change")
        assertTrue(journal.markRunning(lease, "stopped", 1_500))
        assertTrue(journal.requestStop(lease, "stopped", 1_600))

        val reopened = VyomFactoryExecutionJournal(prefs, leases)
        val progress = VyomFactoryVerifiedProgress.calculate(task(), reopened.all())

        assertEquals(0, progress.percent)
        assertFalse(progress.implementationVerified)
    }

    @Test fun completedIssueWithoutCommitProofNeverGetsOneHundred() {
        val progress = calculate(task(status = "completed", events = listOf(passEvent())), emptyList())
        assertEquals(0, progress.percent)
        assertFalse(progress.implementationVerified)
    }

    private fun calculate(
        task: VyomFactoryGitHubClient.Task,
        entries: List<VyomFactoryExecutionJournal.Entry>,
        events: List<String> = emptyList()
    ): VyomFactoryVerifiedProgress.Snapshot =
        VyomFactoryVerifiedProgress.calculate(task.copy(metadata = task.metadata.copy(events = events)), entries)

    private fun task(
        status: String = "in_progress",
        description: String = description(),
        events: List<String> = emptyList()
    ): VyomFactoryGitHubClient.Task {
        val metadata = VyomFactoryStatus.Metadata(
            priority = "P2", status = status, source = "factory",
            sourceId = "progress-77", idempotencyKey = "factory:progress-77",
            events = events,
            lane = VyomFactoryLaneRouter.Lane.INSTANT,
            lanePlanningAllowed = true
        )
        return VyomFactoryGitHubClient.Task(
            number = 77, title = "[VYOM FACTORY][P2] Progress verification",
            body = VyomFactoryStatus.withMetadata(description, metadata),
            state = if (status == "completed") "closed" else "open",
            url = "https://github.com/riyas433554-dot/vyom-android/issues/77",
            updatedAt = "2026-11-08T00:00:00Z", metadata = metadata
        )
    }

    private fun description() =
        "- **Verification probe:** $probe\n\n" +
            "Acceptance criteria:\n" +
            "- [x] Add the reviewed implementation\n" +
            "- [x] Verify the required CI checks\n\n" +
            """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"repository_change","operation":"propose_change","request":"Apply reviewed code change","arguments":{}} -->"""

    private fun passEvent() =
        VyomFactoryVerificationPolicy.resultEvent(probe, "The exact approved change is visible on the device", true)

    private fun identity(workerId: String) =
        VyomFactoryWorkerIdentity(workerId, VyomFactoryLaneRouter.Lane.INSTANT, 0)

    private fun entry(
        attemptId: String = "attempt-1",
        result: String = verifiedResult
    ) = VyomFactoryExecutionJournal.Entry(
        issueNumber = 77, attemptId = attemptId, leaseId = "lease-progress",
        workerId = "worker-progress", lane = VyomFactoryLaneRouter.Lane.INSTANT, workerSlot = 0,
        state = VyomFactoryExecutionJournal.State.SUCCEEDED,
        startedAt = 1_000L, updatedAt = 2_000L, result = result,
        operationKey = "repository_change:propose_change"
    )

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        var failCommit = false
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
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
                if (failCommit) return false
                removals.forEach(values::remove)
                changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                return true
            }
            override fun apply() { commit() }
        }
    }
}
