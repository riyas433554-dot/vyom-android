package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryWorkerSessionTest {
    @Test fun refreshesRegistryImportsOpenTasksAndClaimsHighestPriority() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val session = session(queue, listOf(remote(21, "P2", "source-21"), remote(22, "P0", "source-22")))

        val claim = session.claimNext()

        assertNotNull(claim)
        assertEquals(22, claim!!.task.issueNumber)
        assertTrue(queue.contains(21))
        assertTrue(queue.contains(22))
        assertTrue(session.isCurrent(claim))
    }

    @Test fun startedInProgressComplaintsRemainClaimableByFactoryWorker() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val session = session(queue, listOf(
            remote(29, "P0", "source-29", metadataStatus = "in_progress")
        ))

        val claim = session.claimNext()

        assertNotNull(claim)
        assertEquals(29, claim!!.task.issueNumber)
        assertTrue(queue.contains(29))
    }

    @Test fun doesNotImportOrClaimPausedOrClosedTasks() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val session = session(queue, listOf(
            remote(23, "P0", "source-23", metadataStatus = "paused"),
            remote(24, "P1", "source-24", issueState = "closed")
        ))

        assertNull(session.claimNext())
        assertTrue(queue.pending().isEmpty())
    }

    @Test fun filteredClaimSkipsHigherPriorityTasksWithoutRemovingThemFromInbox() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val session = session(queue, listOf(
            remote(27, "P0", "source-27"),
            remote(28, "P1", "source-28")
        ))

        val claim = session.claimNext { it.number == 28 }

        assertNotNull(claim)
        assertEquals(28, claim!!.task.issueNumber)
        assertTrue(queue.contains(27))
        assertTrue(queue.contains(28))
    }

    @Test fun registryReadFailureDoesNotCreateAClaimOrChangeTheInbox() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        queue.enqueue(VyomFactoryTaskStore.QueuedTask(25, "Existing", "P1", "source-25", 100))
        val session = VyomFactoryWorkerSession(
            queue,
            VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs())),
            "worker-a",
            fetchRemoteTasks = { throw IllegalStateException("network unavailable") },
            now = { 1_000L },
            leaseDurationMs = 10_000L
        )

        try {
            session.claimNext()
            fail("Expected registry read failure")
        } catch (expected: IllegalStateException) {
            assertEquals("network unavailable", expected.message)
        }
        assertEquals(listOf(25), queue.pending().map { it.issueNumber })
    }

    @Test fun releaseFencesClaimAndMakesTaskClaimableAgain() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val leases = VyomFactoryWorkerLeaseStore(MemoryPrefs())
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, leases)
        val remoteTasks = listOf(remote(26, "P1", "source-26"))
        val firstSession = VyomFactoryWorkerSession(
            queue, coordinator, "worker-a", { remoteTasks }, { 1_000L }, 10_000L
        )
        val claim = firstSession.claimNext()!!

        assertTrue(firstSession.release(claim))
        assertFalse(firstSession.isCurrent(claim))
        val secondSession = VyomFactoryWorkerSession(
            queue, coordinator, "worker-b", { remoteTasks }, { 1_002L }, 10_000L
        )
        val next = secondSession.claimNext()
        assertNotNull(next)
        assertEquals(26, next!!.task.issueNumber)
    }

    private fun session(queue: VyomFactoryTaskStore, tasks: List<VyomFactoryGitHubClient.Task>) =
        session(queue, tasks, "worker-a")

    private fun session(queue: VyomFactoryTaskStore, tasks: List<VyomFactoryGitHubClient.Task>, owner: String) =
        VyomFactoryWorkerSession(
            queue,
            VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs())),
            owner,
            fetchRemoteTasks = { tasks },
            now = { 1_000L },
            leaseDurationMs = 10_000L
        )

    private fun remote(
        issue: Int,
        priority: String,
        sourceId: String,
        metadataStatus: String = "open",
        issueState: String = "open"
    ): VyomFactoryGitHubClient.Task {
        val key = VyomFactoryStatus.idempotencyKey("factory", sourceId)!!
        val metadata = VyomFactoryStatus.Metadata(priority, metadataStatus, "factory", sourceId, key, emptyList())
        return VyomFactoryGitHubClient.Task(issue, "Task $issue", "Description", issueState,
            "https://github.com/riyas433554-dot/vyom-android/issues/$issue", "2026-11-04T00:00:00Z", metadata)
    }

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = defValue
        override fun getLong(key: String, defValue: Long): Long = defValue
        override fun getFloat(key: String, defValue: Float): Float = defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        private inner class Editor : SharedPreferences.Editor {
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
                removals.forEach(values::remove)
                updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                return true
            }
            override fun apply() { commit() }
        }
    }
}
