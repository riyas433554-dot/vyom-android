package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryWorkerPoolTest {
    @Test fun provisionsTwentyStableLaneBoundWorkerSessions() {
        val prefs = MemoryPrefs()
        val first = createPool(prefs)
        val second = createPool(prefs)

        assertEquals(20, first.workerCount)
        assertEquals(first.workerCount, first.concurrencyLimit)
        assertEquals(
            first.sessions.map { it.workerIdentity }.toSet(),
            second.sessions.map { it.workerIdentity }.toSet()
        )
        assertEquals(first.workerCount, first.sessions.map { it.workerIdentity.workerId }.toSet().size)
        VyomFactoryLaneRouter.Lane.values().forEach { lane ->
            val workers = first.sessionsForLane(lane)
            assertEquals(5, workers.size)
            assertTrue(workers.all { it.workerIdentity.lane == lane })
            assertEquals(5, workers.map { it.workerIdentity.slotIndex }.toSet().size)
        }
    }

    @Test fun instantWorkerCannotClaimCustomOrUnapprovedInstantTasks() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))
        val session = VyomFactoryWorkerSession(
            queue, coordinator, identity("instant-worker-01", VyomFactoryLaneRouter.Lane.INSTANT),
            fetchRemoteTasks = {
                listOf(
                    remote(1, VyomFactoryLaneRouter.Lane.CUSTOM, lanePlanningAllowed = true),
                    remote(2, VyomFactoryLaneRouter.Lane.INSTANT, lanePlanningAllowed = false),
                    remote(3, VyomFactoryLaneRouter.Lane.INSTANT, lanePlanningAllowed = true)
                )
            },
            now = { 1_000L },
            leaseDurationMs = 10_000L
        )

        val claim = session.claimNext()

        assertEquals(3, claim?.task?.issueNumber)
        assertTrue(queue.contains(1))
        assertTrue(queue.contains(2))
        assertTrue(queue.contains(3))
    }

    @Test fun customWorkerClaimsOnlyOwnerApprovedCustomTasks() {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))
        val session = VyomFactoryWorkerSession(
            queue, coordinator, identity("custom-worker-01", VyomFactoryLaneRouter.Lane.CUSTOM),
            fetchRemoteTasks = {
                listOf(
                    remote(4, VyomFactoryLaneRouter.Lane.INSTANT, lanePlanningAllowed = true),
                    remote(5, VyomFactoryLaneRouter.Lane.CUSTOM, lanePlanningAllowed = false),
                    remote(6, VyomFactoryLaneRouter.Lane.CUSTOM, lanePlanningAllowed = true)
                )
            },
            now = { 1_000L },
            leaseDurationMs = 10_000L
        )

        val claim = session.claimNext()

        assertEquals(6, claim?.task?.issueNumber)
    }

    private fun createPool(prefs: SharedPreferences): VyomFactoryWorkerPool {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))
        var sequence = 0
        return VyomFactoryWorkerPool(
            identityStore = VyomFactoryWorkerIdentityStore(prefs) { "worker-${++sequence}-stable-id" },
            taskStore = queue,
            coordinator = coordinator,
            fetchRemoteTasks = { emptyList() },
            now = { 1_000L },
            leaseDurationMs = 10_000L
        )
    }

    private fun identity(workerId: String, lane: VyomFactoryLaneRouter.Lane) =
        VyomFactoryWorkerIdentity(workerId, lane, 0)

    private fun remote(
        issue: Int,
        lane: VyomFactoryLaneRouter.Lane,
        lanePlanningAllowed: Boolean
    ): VyomFactoryGitHubClient.Task {
        val sourceId = "worker-pool-$issue"
        val key = VyomFactoryStatus.idempotencyKey("factory", sourceId)!!
        val metadata = VyomFactoryStatus.Metadata(
            "P1", "open", "factory", sourceId, key, emptyList(),
            lane = lane,
            lanePlanningAllowed = lanePlanningAllowed
        )
        return VyomFactoryGitHubClient.Task(
            issue, "Task $issue", "Description", "open",
            "https://github.com/riyas433554-dot/vyom-android/issues/$issue",
            "2026-11-10T00:00:00Z", metadata
        )
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
