package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryWorkerClaimCoordinatorTest {
    @Test fun claimsHighestPriorityEligibleTaskWithoutRemovingInboxEntry() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        queue.enqueue(task(10, "P2", "source-10"))
        queue.enqueue(task(11, "P0", "source-11"))
        val leases = VyomFactoryWorkerLeaseStore(MemoryPrefs())
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, leases)

        val claim = coordinator.claimNext(
            listOf(remote(10, "P2", "source-10"), remote(11, "P0", "source-11")),
            "device-worker", 1_000, 10_000
        )

        assertNotNull(claim)
        assertEquals(11, claim!!.task.issueNumber)
        assertTrue(queue.contains(11))
        assertEquals(claim.lease, leases.active(11, 1_001))
    }

    @Test fun oldestCreatedIssueWinsWithinPriorityEvenWhenNewerIssueWasUpdatedMoreRecently() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        queue.enqueue(VyomFactoryTaskStore.QueuedTask(21, "Older P0", "P0", "source-21", 100))
        queue.enqueue(VyomFactoryTaskStore.QueuedTask(22, "Newer P0", "P0", "source-22", 200))
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))

        val claim = coordinator.claimNext(
            listOf(
                remote(21, "P0", "source-21", updatedAt = "2026-11-04T15:00:00Z", createdAt = "2026-11-01T00:00:00Z"),
                remote(22, "P0", "source-22", updatedAt = "2026-11-04T16:00:00Z", createdAt = "2026-11-02T00:00:00Z")
            ),
            "device-worker", 1_000, 10_000
        )

        assertEquals(21, claim?.task?.issueNumber)
    }

    @Test fun claimsInProgressIssueAfterOwnerStartsIt() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        queue.enqueue(task(16, "P0", "source-16"))
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))

        val claim = coordinator.claimNext(
            listOf(remote(16, "P0", "source-16", status = "in_progress")),
            "device-worker", 1_000, 10_000
        )

        assertNotNull(claim)
        assertEquals(16, claim!!.task.issueNumber)
    }

    @Test fun skipsRemoteTasksThatAreNotOpenOrDoNotMatchLocalIdentity() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        queue.enqueue(task(10, "P0", "local-source"))
        queue.enqueue(task(11, "P1", "source-11"))
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))

        val claim = coordinator.claimNext(
            listOf(
                remote(10, "P0", "local-source", status = "paused"),
                remote(11, "P1", "different-source")
            ),
            "device-worker", 1_000, 10_000
        )

        assertNull(claim)
    }

    @Test fun anotherLocalWorkerCannotClaimAnUnexpiredLease() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        queue.enqueue(task(12, "P1", "source-12"))
        val leases = VyomFactoryWorkerLeaseStore(MemoryPrefs())
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, leases)

        val first = coordinator.claimNext(listOf(remote(12, "P1", "source-12")), identity("worker-a"), 1_000, 10_000)
        val second = coordinator.claimNext(listOf(remote(12, "P1", "source-12")), identity("worker-b"), 1_001, 10_000)

        assertNotNull(first)
        assertNull(second)
        assertTrue(coordinator.isCurrent(first!!, 1_002))
    }

    @Test fun sameWorkerClaimIsIdempotentAndExpiredClaimIsNotCurrent() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        queue.enqueue(task(13, "P2", "source-13"))
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))
        val remote = listOf(remote(13, "P2", "source-13"))

        val first = coordinator.claimNext(remote, identity("worker-a"), 1_000, 5_000)!!
        val repeated = coordinator.claimNext(remote, identity("worker-a"), 2_000, 5_000)!!

        assertEquals(first.lease, repeated.lease)
        assertFalse(coordinator.isCurrent(first, 6_000))
    }

    @Test fun releaseRequiresExactLeaseAndLeavesTaskAvailableForAnotherWorker() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        queue.enqueue(task(14, "P1", "source-14"))
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))
        val remote = listOf(remote(14, "P1", "source-14"))
        val first = coordinator.claimNext(remote, identity("worker-a"), 1_000, 5_000)!!

        assertFalse(coordinator.release(first.copy(lease = first.lease.copy(workerId = "worker-b"))))
        assertTrue(coordinator.release(first))
        assertFalse(coordinator.isCurrent(first, 1_001))
        val next = coordinator.claimNext(remote, identity("worker-b"), 1_002, 5_000)
        assertNotNull(next)
        assertEquals(14, next!!.task.issueNumber)
    }

    private fun identity(workerId: String) = VyomFactoryWorkerIdentity(
        workerId, VyomFactoryLaneRouter.Lane.INSTANT, 0
    )

    private fun task(issue: Int, priority: String, sourceId: String) =
        VyomFactoryTaskStore.QueuedTask(issue, "Task $issue", priority, sourceId, 100)

    private fun remote(issue: Int, priority: String, sourceId: String, status: String = "open", updatedAt: String = "2026-11-04T00:00:00Z", createdAt: String = updatedAt): VyomFactoryGitHubClient.Task {
        val key = VyomFactoryStatus.idempotencyKey("factory", sourceId)!!
        val metadata = VyomFactoryStatus.Metadata(priority, status, "factory", sourceId, key, emptyList())
        return VyomFactoryGitHubClient.Task(issue, "Task $issue", "Description", "open",
            "https://github.com/riyas433554-dot/vyom-android/issues/$issue", updatedAt, metadata, createdAt)
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
