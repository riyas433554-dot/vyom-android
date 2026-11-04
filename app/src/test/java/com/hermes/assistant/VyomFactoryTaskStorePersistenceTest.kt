package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryTaskStorePersistenceTest {
    @Test fun enqueuedTasksSurviveStoreRecreationAndRemainOrdered() {
        val prefs = InMemorySharedPreferences()
        val firstStore = VyomFactoryTaskStore(prefs)
        assertTrue(firstStore.enqueue(task(41, "P2", "chat-41", 10)))
        assertTrue(firstStore.enqueue(task(42, "P0", "chat-42", 20)))

        val recreatedStore = VyomFactoryTaskStore(prefs)
        assertEquals(listOf(42, 41), recreatedStore.pending().map { it.issueNumber })
        assertTrue(recreatedStore.contains(41))
    }

    @Test fun duplicateEnqueueDoesNotOverwriteExistingTask() {
        val prefs = InMemorySharedPreferences()
        val store = VyomFactoryTaskStore(prefs)
        assertTrue(store.enqueue(task(41, "P2", "chat-41", 10)))
        assertFalse(store.enqueue(task(41, "P0", "chat-41-new", 11)))
        assertEquals("P2", store.pending().single().priority)
    }

    @Test fun removePersistsAcrossStoreRecreation() {
        val prefs = InMemorySharedPreferences()
        val store = VyomFactoryTaskStore(prefs)
        store.enqueue(task(41, "P2", "chat-41", 10))
        assertTrue(store.remove(41))
        assertFalse(VyomFactoryTaskStore(prefs).contains(41))
    }

    @Test fun malformedPersistedJsonFailsClosedWithoutOverwritingData() {
        val prefs = InMemorySharedPreferences().apply {
            edit().putString("pending_tasks_v1", "{broken-json").commit()
        }
        val store = VyomFactoryTaskStore(prefs)
        assertThrows(IllegalStateException::class.java) { store.pending() }
        assertEquals("{broken-json", prefs.getString("pending_tasks_v1", null))
    }

    @Test fun failedCommitIsReportedAndNotReportedAsSuccessfulEnqueue() {
        val prefs = InMemorySharedPreferences().apply { failCommits = true }
        val store = VyomFactoryTaskStore(prefs)
        assertThrows(IllegalStateException::class.java) {
            store.enqueue(task(41, "P2", "chat-41", 10))
        }
    }

    private fun task(issue: Int, priority: String, source: String, time: Long) =
        VyomFactoryTaskStore.QueuedTask(issue, "Task $issue", priority, source, time)

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        var failCommits = false

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
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
            private var clear = false
            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply { updates[key] = values?.toSet() }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { updates[key] = value }
            override fun remove(key: String): SharedPreferences.Editor = apply { removals += key }
            override fun clear(): SharedPreferences.Editor = apply { clear = true }
            override fun commit(): Boolean {
                if (failCommits) return false
                if (clear) values.clear()
                removals.forEach(values::remove)
                updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                return true
            }
            override fun apply() { commit() }
        }
    }
}
