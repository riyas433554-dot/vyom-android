package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class VyomFactoryExecutionRunnerTest {
    @Test fun executesOnlyFreshOpenTaskAndPersistsTerminalSuccess() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val runner = fixture.runner(adapter)
        val outcome = runner.runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals(1, adapter.calls)
        assertEquals(VyomFactoryExecutionJournal.State.SUCCEEDED, fixture.journal.get("attempt-1")?.state)
    }

    @Test fun skipsTaskThatWasClosedOrChangedBeforeExecution() {
        val fixture = Fixture(remoteState = "closed")
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val outcome = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "task_no_longer_open_or_identity_changed"),
            outcome
        )
        assertEquals(0, adapter.calls)
        assertNull(fixture.journal.get("attempt-1"))
    }

    @Test fun transientFailureSchedulesRetryOnlyForIdempotentAdapter() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = true, isIdempotent = true)
        val outcome = fixture.runner(adapter).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Failed)
        assertEquals(1, fixture.retryStore.all().size)
        assertEquals(1, fixture.retryStore.all().single().failedAttempts)
    }

    @Test fun nonIdempotentAdapterFailureNeverSchedulesReplay() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = true, isIdempotent = false)
        fixture.runner(adapter).runNext()

        assertTrue(fixture.retryStore.all().isEmpty())
    }

    @Test fun idempotentFailureWaitsUntilPersistedRetryIsDue() {
        val fixture = Fixture()
        val firstAdapter = RecordingAdapter(fail = true, isIdempotent = true)
        val first = fixture.runner(firstAdapter)
        assertTrue(first.runNext() is VyomFactoryExecutionRunner.Outcome.Failed)

        val earlyAdapter = RecordingAdapter(fail = false, isIdempotent = true)
        val early = fixture.runner(earlyAdapter).runNext()
        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "retry_not_due_or_manual_review_required"),
            early
        )
        assertEquals(0, earlyAdapter.calls)

        fixture.advanceTo(fixture.retryStore.all().single().nextAttemptAt)
        val retryAdapter = RecordingAdapter(fail = false, isIdempotent = true)
        val retried = fixture.runner(retryAdapter).runNext()
        assertTrue(retried is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals(1, retryAdapter.calls)
    }

    @Test fun failedNonIdempotentOperationIsNeverAutomaticallyReplayed() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = true, isIdempotent = false)
        fixture.runner(adapter).runNext()

        val second = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "retry_not_due_or_manual_review_required"),
            second
        )
        assertEquals(1, adapter.calls)
    }

    @Test fun successfulExecutionClearsOnlyItsMatchingRetrySchedule() {
        val fixture = Fixture()
        fixture.retryStore.recordFailure(71, "android_action:safe_action", "network_timeout", 1_000, true)

        val outcome = fixture.runner(RecordingAdapter(fail = false, isIdempotent = true)).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertTrue((outcome as VyomFactoryExecutionRunner.Outcome.Succeeded).retryScheduleCleared)
        assertTrue(fixture.retryStore.all().isEmpty())
    }

    @Test fun legacyJournalEntryBlocksReplayWhenOperationIdentityIsUnknown() {
        val fixture = Fixture()
        fixture.seedLegacySuccess()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)

        val outcome = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "legacy_attempt_requires_review"),
            outcome
        )
        assertEquals(0, adapter.calls)
    }

    @Test fun expiredOrReplacedLeasePreventsAdapterInvocation() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        fixture.releaseLease()

        val outcome = fixture.runner(adapter).runNext()

        assertEquals(VyomFactoryExecutionRunner.Outcome.Fenced(71), outcome)
        assertEquals(0, adapter.calls)
        assertNull(fixture.journal.get("attempt-1"))
    }

    @Test fun terminalSuccessReleasesExactWorkerClaim() {
        val fixture = Fixture()
        val outcome = fixture.runner(RecordingAdapter(fail = false, isIdempotent = true)).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertNull(fixture.activeLease())
    }

    @Test fun terminalFailureReleasesExactWorkerClaimAfterRecordingFailure() {
        val fixture = Fixture()
        val outcome = fixture.runner(RecordingAdapter(fail = true, isIdempotent = true)).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Failed)
        assertEquals(VyomFactoryExecutionJournal.State.FAILED, fixture.journal.get("attempt-1")?.state)
        assertNull(fixture.activeLease())
        assertEquals(1, fixture.retryStore.all().single().failedAttempts)
    }

    @Test fun adapterCanObserveStopSignalBeforeSideEffect() {
        val fixture = Fixture()
        var sideEffects = 0
        val adapter = object : VyomFactoryExecutionAdapter {
            override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
            override val isIdempotent = true
            override fun run(
                envelope: VyomFactoryExecutionEnvelope,
                control: VyomFactoryExecutionControl
            ): String {
                fixture.requestStop()
                if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
                sideEffects++
                return "done"
            }
        }
        val runner = fixture.runner(adapter)
        val outcome = runner.runNext()
        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Failed)
        assertEquals(0, sideEffects)
        assertTrue(fixture.retryStore.all().isEmpty())
    }

    private class Fixture(remoteState: String = "open") {
        private val prefs = MemoryPrefs()
        private val leaseStore = VyomFactoryWorkerLeaseStore(prefs)
        private var lease = leaseStore.claim(71, "runner-test", 1_000, 100_000)!!
        private var preserveReleasedLeaseForNextClaim = false
        private val task = VyomFactoryTaskStore.QueuedTask(71, "Test execution", "P2", "src-71", 900)
        private val metadata = VyomFactoryStatus.Metadata(
            "P2", "open", "factory", "src-71", "factory:src-71", listOf("created")
        )
        val remote = VyomFactoryGitHubClient.Task(
            71, "Test execution",
            VyomFactoryStatus.withMetadata(
                "Execute a bounded action",
                metadata
            ) + "\n<!-- VYOM_FACTORY_EXECUTION:{\"schema\":1,\"target\":\"android_action\",\"action\":\"safe_action\",\"request\":\"run safe action\",\"arguments\":{}} -->",
            remoteState, "https://github.com/example/repo/issues/71", "2026-11-04", metadata
        )
        val journal = VyomFactoryExecutionJournal(prefs, leaseStore)
        val retryStore = VyomFactoryRetryScheduleStore(MemoryPrefs())
        private var attemptSequence = 0
        private var clock = 2_000L

        fun advanceTo(value: Long) { clock = value }

        fun activeLease() = leaseStore.active(71, clock)

        fun requestStop() {
            assertTrue(journal.requestStop(lease, "attempt-1", 2_001))
        }

        fun seedLegacySuccess() {
            journal.begin(lease, "legacy-attempt", 1_500)
            assertTrue(journal.markRunning(lease, "legacy-attempt", 1_600))
            assertTrue(journal.succeed(lease, "legacy-attempt", 1_700, "old result"))
        }

        fun releaseLease() {
            assertTrue(leaseStore.release(lease))
            preserveReleasedLeaseForNextClaim = true
        }

        init {
            assertEquals("open", metadata.status)
        }

        fun runner(adapter: VyomFactoryExecutionAdapter): VyomFactoryExecutionRunner {
            val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))
            return VyomFactoryExecutionRunner(
                claimNext = {
                    val current = if (preserveReleasedLeaseForNextClaim) lease else
                        leaseStore.claim(71, "runner-test", clock, 100_000)!!
                    lease = current
                    VyomFactoryWorkerClaimCoordinator.Claim(task, remote, current)
                },
                isClaimCurrent = { claim -> leaseStore.active(71, clock) == claim.lease },
                releaseClaim = { leaseStore.release(it.lease) },
                fetchFreshTask = { if (it == 71) remote else null },
                journal = journal,
                adapters = registry,
                retrySchedules = retryStore,
                now = { clock },
                newAttemptId = { "attempt-${++attemptSequence}" }
            )
        }
    }

    private class RecordingAdapter(
        private val fail: Boolean,
        override val isIdempotent: Boolean
    ) : VyomFactoryExecutionAdapter {
        override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
        var calls = 0
        override fun run(envelope: VyomFactoryExecutionEnvelope, control: VyomFactoryExecutionControl): String {
            calls++
            if (fail) throw IOException("temporary network timeout")
            return "completed"
        }
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
