package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryQueueRefreshTest {
    @Test fun refreshPersistsOnlyOpenEligibleRemoteTasksWithoutClaimingThem() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryTaskStore(prefs)
        var fetches = 0
        val refresh = VyomFactoryQueueRefresh(
            taskStore = store,
            fetchRemoteTasks = {
                fetches++
                listOf(
                    remote(71, "open", "open"),
                    remote(72, "open", "in_progress"),
                    remote(73, "closed", "open"),
                    remote(74, "open", "paused")
                )
            },
            now = { 9_000L }
        )

        assertEquals(2, refresh.refresh())
        assertEquals(1, fetches)
        val persisted = VyomFactoryTaskStore(prefs).pending()
        assertEquals(setOf(71, 72), persisted.map { it.issueNumber }.toSet())
        assertTrue(persisted.all { it.enqueuedAt == 9_000L })
        assertTrue(persisted.all { it.lane == VyomFactoryLaneRouter.Lane.INSTANT })
        assertTrue(persisted.all { it.lanePlanningAllowed })
    }

    @Test fun refreshIsIdempotentAndDoesNotReplaceExistingEnqueueTime() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryTaskStore(prefs)
        assertTrue(store.enqueue(VyomFactoryTaskStore.QueuedTask(
            71, "Task 71", "P2", "source-71", 123L
        )))
        val refresh = VyomFactoryQueueRefresh(
            taskStore = store,
            fetchRemoteTasks = { listOf(remote(71, "open", "open")) },
            now = { 9_000L }
        )

        assertEquals(0, refresh.refresh())
        val queued = VyomFactoryTaskStore(prefs).pending().single()
        assertEquals(123L, queued.enqueuedAt)
    }

    @Test fun failedRemoteRefreshDoesNotMutateOrEraseLocalQueue() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryTaskStore(prefs)
        assertTrue(store.enqueue(VyomFactoryTaskStore.QueuedTask(
            71, "Task 71", "P2", "source-71", 123L
        )))
        val refresh = VyomFactoryQueueRefresh(
            taskStore = store,
            fetchRemoteTasks = { throw java.io.IOException("offline") },
            now = { 9_000L }
        )

        assertThrows(java.io.IOException::class.java) { refresh.refresh() }
        assertEquals(listOf(71), VyomFactoryTaskStore(prefs).pending().map { it.issueNumber })
    }

    private fun remote(
        issue: Int,
        issueState: String,
        status: String
    ): VyomFactoryGitHubClient.Task {
        val sourceId = "source-$issue"
        val metadata = VyomFactoryStatus.Metadata(
            "P1", status, "factory", sourceId,
            VyomFactoryStatus.idempotencyKey("factory", sourceId)!!,
            emptyList(), VyomFactoryLaneRouter.Lane.INSTANT, true
        )
        return VyomFactoryGitHubClient.Task(
            issue, "Task $issue", "Description", issueState,
            "https://github.com/riyas433554-dot/vyom-android/issues/$issue",
            "2026-11-11T00:00:00Z", metadata
        )
    }

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
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
