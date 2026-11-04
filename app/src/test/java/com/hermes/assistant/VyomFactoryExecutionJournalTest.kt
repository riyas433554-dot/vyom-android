package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryExecutionJournalTest {
    @Test fun beginRequiresTheCurrentUnexpiredLeaseAndPersistsAttemptIdentity() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(41, "worker-a", 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)

        val entry = journal.begin(lease, "attempt-1", 1_100)

        assertEquals(41, entry.issueNumber)
        assertEquals("attempt-1", entry.attemptId)
        assertEquals(lease.leaseId, entry.leaseId)
        assertEquals(VyomFactoryExecutionJournal.State.CLAIMED, entry.state)
        assertEquals(entry, VyomFactoryExecutionJournal(prefs, leases).get("attempt-1"))
    }

    @Test fun onlyCurrentLeaseCanWriteRunningAndTerminalResults() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(42, "worker-a", 1_000, 10_000)!!
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
        val oldLease = leases.claim(43, "worker-a", 1_000, 5_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(oldLease, "attempt-old", 1_100)
        journal.markRunning(oldLease, "attempt-old", 1_200)
        val successor = leases.claim(43, "worker-b", 6_000, 5_000)!!
        journal.begin(successor, "attempt-new", 6_100)

        assertFalse(journal.succeed(oldLease, "attempt-old", 6_200, "stale result"))
        assertEquals(VyomFactoryExecutionJournal.State.RUNNING, journal.get("attempt-old")!!.state)
        assertTrue(journal.markRunning(successor, "attempt-new", 6_200))
    }

    @Test fun terminalAttemptRejectsDuplicateOrConflictingWrites() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(44, "worker-a", 1_000, 10_000)!!
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
        val lease = leases.claim(45, "worker-a", 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-4", 1_100)
        journal.markRunning(lease, "attempt-4", 1_200)

        assertTrue(journal.requestStop(45, 1_300))
        val saved = VyomFactoryExecutionJournal(prefs, leases).get("attempt-4")!!
        assertEquals(VyomFactoryExecutionJournal.State.STOP_REQUESTED, saved.state)
        assertEquals(null, saved.result)
    }

    @Test fun expiredNonterminalAttemptsBecomeInterruptedButAreNeverAutoReplayed() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(46, "worker-a", 1_000, 5_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        journal.begin(lease, "attempt-5", 1_100)
        journal.markRunning(lease, "attempt-5", 1_200)

        val recovered = journal.interruptExpired(6_000)

        assertEquals(1, recovered)
        val saved = journal.get("attempt-5")!!
        assertEquals(VyomFactoryExecutionJournal.State.INTERRUPTED, saved.state)
        assertEquals(emptyList<VyomFactoryExecutionJournal.Entry>(), journal.replayable())
    }

    @Test fun corruptPersistenceAndFailedCommitFailClosed() {
        val prefs = MemoryPrefs()
        val leases = VyomFactoryWorkerLeaseStore(prefs)
        val lease = leases.claim(47, "worker-a", 1_000, 10_000)!!
        val journal = VyomFactoryExecutionJournal(prefs, leases)
        prefs.failCommit = true
        assertThrows(IllegalStateException::class.java) { journal.begin(lease, "attempt-6", 1_100) }
        prefs.failCommit = false
        prefs.edit().putString("attempts_v1", "{broken").commit()
        assertThrows(IllegalStateException::class.java) { journal.all() }
        assertEquals("{broken", prefs.getString("attempts_v1", null))
    }

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
