package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryExecutionJournalTest {
    @Test fun beginRequiresTheCurrentUnexpiredLeaseAndPersistsAttemptIdentity() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(41, identity("worker-a"), 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)

        val entry = journal.begin(lease, "attempt-1", 1_100, "factory.sync")

        assertEquals(41, entry.issueNumber)
        assertEquals("attempt-1", entry.attemptId)
        assertEquals("factory.sync", entry.operationKey)
        assertEquals(lease.leaseId, entry.leaseId)
        assertEquals(lease.workerId, entry.workerId)
        assertEquals(lease.lane, entry.lane)
        assertEquals(lease.workerSlot, entry.workerSlot)
        assertEquals(VyomFactoryExecutionJournal.State.CLAIMED, entry.state)
        assertEquals(entry, VyomFactoryExecutionJournal(prefs, leases).get("attempt-1"))
    }

    @Test fun legacyOwnerIdJournalRecordsRemainReadableAsWorkerIdentity() {
        val prefs = MemoryPrefs()
        prefs.edit().putString(
            "attempts_v1",
            """[{"issue_number":46,"attempt_id":"legacy-attempt-46","lease_id":"legacy-lease-46","owner_id":"worker-legacy","state":"INTERRUPTED","started_at":1000,"updated_at":1100,"operation_key":"legacy:unknown"}]"""
        ).commit()

        val entry = VyomFactoryExecutionJournal(prefs, VyomFactoryWorkerLeaseStore(MemoryPrefs()))
            .get("legacy-attempt-46")

        assertNotNull(entry)
        assertEquals("worker-legacy", entry!!.workerId)
        assertNull(entry.lane)
        assertNull(entry.workerSlot)
    }

    @Test fun beginRejectsSecondNonterminalAttemptForSameIssueAndLease() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(48, identity("worker-a"), 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-active-1", 1_100)

        assertThrows(IllegalStateException::class.java) {
            journal.begin(lease, "attempt-active-2", 1_200)
        }
        assertEquals(1, journal.all().size)
    }

    @Test fun newAttemptIsAllowedAfterPreviousAttemptIsTerminal() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(49, identity("worker-a"), 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-terminal-1", 1_100)
        journal.markRunning(lease, "attempt-terminal-1", 1_150)
        assertTrue(journal.succeed(lease, "attempt-terminal-1", 1_200, "done"))

        assertEquals(
            "attempt-terminal-2",
            journal.begin(lease, "attempt-terminal-2", 1_300).attemptId
        )
    }

    @Test fun onlyCurrentLeaseCanWriteRunningAndTerminalResults() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(42, identity("worker-a"), 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-2", 1_100)

        assertTrue(journal.markRunning(lease, "attempt-2", 1_200))
        assertTrue(journal.succeed(lease, "attempt-2", 1_300, "done"))
        val saved = VyomFactoryExecutionJournal(prefs, leases).get("attempt-2")!!
        assertEquals(VyomFactoryExecutionJournal.State.SUCCEEDED, saved.state)
        assertEquals("done", saved.result)
    }

    @Test fun expiredLeaseCannotWriteAResultAfterSuccessorClaimsTheIssue() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val oldLease = leases.claim(43, identity("worker-a"), 1_000, 5_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(oldLease, "attempt-old", 1_100)
        journal.markRunning(oldLease, "attempt-old", 1_200)
        val successor = leases.claim(43, identity("worker-b", 1), 6_000, 5_000)!!
        journal.begin(successor, "attempt-new", 6_100)

        assertFalse(journal.succeed(oldLease, "attempt-old", 6_200, "stale result"))
        assertEquals(VyomFactoryExecutionJournal.State.INTERRUPTED, journal.get("attempt-old")!!.state)
        assertTrue(journal.markRunning(successor, "attempt-new", 6_200))
    }

    @Test fun terminalAttemptRejectsDuplicateOrConflictingWrites() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(44, identity("worker-a"), 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-3", 1_100)
        assertTrue(journal.markRunning(lease, "attempt-3", 1_150))

        assertTrue(journal.succeed(lease, "attempt-3", 1_200, "first"))
        assertFalse(journal.fail(lease, "attempt-3", 1_300, "execution", "late failure"))
        assertFalse(journal.succeed(lease, "attempt-3", 1_400, "second"))
        assertEquals("first", journal.get("attempt-3")!!.result)
    }

    @Test fun stopRequestIsDurableAndDoesNotPretendTheWorkHasStopped() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(45, identity("worker-a"), 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-4", 1_100)
        journal.markRunning(lease, "attempt-4", 1_200)

        assertTrue(journal.requestStop(lease, "attempt-4", 1_300))
        val saved = VyomFactoryExecutionJournal(prefs, leases).get("attempt-4")!!
        assertEquals(VyomFactoryExecutionJournal.State.STOP_REQUESTED, saved.state)
        assertEquals(null, saved.result)
    }

    @Test fun stopRequestRequiresExactCurrentLeaseAndRunningAttempt() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val oldLease = leases.claim(50, identity("worker-a"), 1_000, 5_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(oldLease, "attempt-stop-fenced", 1_100)

        assertFalse(journal.requestStop(oldLease, "attempt-stop-fenced", 1_200))
        assertTrue(journal.markRunning(oldLease, "attempt-stop-fenced", 1_300))
        val successor = leases.claim(50, identity("worker-b", 1), 6_000, 5_000)!!

        assertFalse(journal.requestStop(oldLease, "attempt-stop-fenced", 6_100))
        assertEquals(VyomFactoryExecutionJournal.State.RUNNING, journal.get("attempt-stop-fenced")!!.state)
        assertTrue(journal.begin(successor, "attempt-stop-successor", 6_200).state ==
            VyomFactoryExecutionJournal.State.CLAIMED)
    }

    @Test fun expiredNonterminalAttemptsBecomeInterruptedButAreNeverAutoReplayed() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(46, identity("worker-a"), 1_000, 5_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-5", 1_100)
        journal.markRunning(lease, "attempt-5", 1_200)

        val recovered = journal.interruptExpired(6_000)

        assertEquals(1, recovered)
        val saved = journal.get("attempt-5")!!
        assertEquals(VyomFactoryExecutionJournal.State.INTERRUPTED, saved.state)
        assertEquals(emptyList<VyomFactoryExecutionJournal.Entry>(), journal.replayable())
    }


    @Test fun reopenedJournalRecoversExpiredStopRequestedAttemptWithoutReplay() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(52, identity("worker-a"), 1_000, 5_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-after-process-restart", 1_100, "factory.sync")
        assertTrue(journal.markRunning(lease, "attempt-after-process-restart", 1_200))
        assertTrue(journal.requestStop(lease, "attempt-after-process-restart", 1_300))

        // Re-open the durable journal as a new process would after a crash.
        val reopened = VyomFactoryExecutionJournal(prefs, leases)
        assertEquals(1, reopened.interruptExpired(6_000))

        val recovered = reopened.get("attempt-after-process-restart")!!
        assertEquals(VyomFactoryExecutionJournal.State.INTERRUPTED, recovered.state)
        assertEquals(1_300L, recovered.stopRequestedAt)
        assertTrue(reopened.replayable().isEmpty())
    }

    @Test fun recoveryInterruptsOnlyExpiredAttemptsAndPreservesLiveAndTerminalHistory() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val expired = leases.claim(54, identity("worker-a"), 1_000, 5_000)!!
        val live = leases.claim(55, identity("worker-b", 1), 1_000, 20_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(expired, "expired-running", 1_100, "factory.expired")
        assertTrue(journal.markRunning(expired, "expired-running", 1_200))
        journal.begin(live, "still-running", 1_100, "factory.live")
        assertTrue(journal.markRunning(live, "still-running", 1_200))

        assertEquals(1, journal.interruptExpired(6_000))

        assertEquals(VyomFactoryExecutionJournal.State.INTERRUPTED, journal.get("expired-running")!!.state)
        assertEquals(VyomFactoryExecutionJournal.State.RUNNING, journal.get("still-running")!!.state)
    }

    @Test fun failedRecoveryCommitLeavesExpiredAttemptUntouchedAndReadable() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(56, identity("worker-a"), 1_000, 5_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-recovery-commit", 1_100)
        assertTrue(journal.markRunning(lease, "attempt-recovery-commit", 1_200))
        val before = prefs.getString("attempts_v1", null)

        prefs.failCommit = true
        assertThrows(IllegalStateException::class.java) {
            VyomFactoryExecutionJournal(prefs, leases).interruptExpired(6_000)
        }
        prefs.failCommit = false

        assertEquals(before, prefs.getString("attempts_v1", null))
        assertEquals(
            VyomFactoryExecutionJournal.State.RUNNING,
            VyomFactoryExecutionJournal(prefs, leases).get("attempt-recovery-commit")!!.state
        )
    }

    @Test fun corruptPersistenceAndFailedCommitFailClosed() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(47, identity("worker-a"), 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        prefs.failCommit = true
        assertThrows(IllegalStateException::class.java) { journal.begin(lease, "attempt-6", 1_100) }
        prefs.failCommit = false
        prefs.edit().putString("attempts_v1", "{broken").commit()
        assertThrows(IllegalStateException::class.java) { journal.all() }
        assertEquals("{broken", prefs.getString("attempts_v1", null))
    }

    @Test fun startupRecoveryInterruptsExpiredAttemptsWithoutReplayingOrTouchingLiveLeases() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val expired = leases.claim(57, identity("worker-expired"), 1_000, 5_000)!!
        val live = leases.claim(58, identity("worker-live", 1), 1_000, 20_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(expired, "startup-expired", 1_100, "factory.startup")
        assertTrue(journal.markRunning(expired, "startup-expired", 1_200))
        journal.begin(live, "startup-live", 1_100, "factory.live")
        assertTrue(journal.markRunning(live, "startup-live", 1_200))

        val reopenedAfterProcessRestart = VyomFactoryExecutionJournal(prefs, leases)
        assertEquals(1, reopenedAfterProcessRestart.recoverAtStartup(6_000))
        assertEquals(0, reopenedAfterProcessRestart.recoverAtStartup(6_001))

        assertEquals(VyomFactoryExecutionJournal.State.INTERRUPTED,
            reopenedAfterProcessRestart.get("startup-expired")!!.state)
        assertEquals(VyomFactoryExecutionJournal.State.RUNNING,
            reopenedAfterProcessRestart.get("startup-live")!!.state)
        assertTrue(reopenedAfterProcessRestart.replayable().isEmpty())
    }

    private fun identity(workerId: String, slot: Int = 0) =
        VyomFactoryWorkerIdentity(workerId, VyomFactoryLaneRouter.Lane.INSTANT, slot)

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        var failCommit = false
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = defValue
        override fun getLong(key: String, defValue: Long): Long = defValue
        override fun getFloat(key: String, defValue: Float): Float = defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = MemoryEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        private inner class MemoryEditor : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String): SharedPreferences.Editor = apply { removals += key }
            override fun clear(): SharedPreferences.Editor = this
            override fun commit(): Boolean {
                if (failCommit) return false
                removals.forEach(values::remove)
                updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                return true
            }
            override fun apply() { commit() }
        }
    }
}
