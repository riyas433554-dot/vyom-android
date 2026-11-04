package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class VyomFactoryExecutionRunnerTest {
    @Test fun executesOnlyFreshOpenTaskAndPersistsTerminalSuccess() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, idempotent = true)
        val runner = fixture.runner(adapter)
        val outcome = runner.runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals(1, adapter.calls)
        assertEquals(VyomFactoryExecutionJournal.State.SUCCEEDED, fixture.journal.get("attempt-1")?.state)
    }

    @Test fun skipsTaskThatWasClosedOrChangedBeforeExecution() {
        val fixture = Fixture(remoteState = "closed")
        val adapter = RecordingAdapter(fail = false, idempotent = true)
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
        val adapter = RecordingAdapter(fail = true, idempotent = true)
        val outcome = fixture.runner(adapter).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Failed)
        assertEquals(1, fixture.retryStore.all().size)
        assertEquals(1, fixture.retryStore.all().single().failedAttempts)
    }

    @Test fun nonIdempotentAdapterFailureNeverSchedulesReplay() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = true, idempotent = false)
        fixture.runner(adapter).runNext()

        assertTrue(fixture.retryStore.all().isEmpty())
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
        private val lease = leaseStore.claim(71, "runner-test", 1_000, 100_000)!!
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
        private var claimed = false

        fun requestStop() {
            assertTrue(journal.requestStop(lease, "attempt-1", 2_001))
        }

        init {
            assertEquals("open", metadata.status)
        }

        fun runner(adapter: VyomFactoryExecutionAdapter): VyomFactoryExecutionRunner {
            val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))
            return VyomFactoryExecutionRunner(
                claimNext = {
                    if (claimed) null else {
                        claimed = true
                        VyomFactoryWorkerClaimCoordinator.Claim(task, remote, lease)
                    }
                },
                isClaimCurrent = { leaseStore.active(71, 2_000) == lease },
                releaseClaim = { leaseStore.release(it.lease) },
                fetchFreshTask = { if (it == 71) remote else null },
                journal = journal,
                adapters = registry,
                retrySchedules = retryStore,
                now = { 2_000 },
                newAttemptId = { "attempt-1" }
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
