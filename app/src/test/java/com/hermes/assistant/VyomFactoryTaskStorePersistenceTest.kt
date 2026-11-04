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

    @Test fun reconciliationRestoresMissingTasksAndIsIdempotent() {
        val prefs = InMemorySharedPreferences()
        val store = VyomFactoryTaskStore(prefs)
        assertTrue(store.enqueue(task(41, "P2", "existing", 10)))

        val restored = listOf(task(42, "P1", "remote-42", 20), task(43, "P3", "remote-43", 30))
        assertEquals(2, store.reconcileOpen(restored))
        assertEquals(0, VyomFactoryTaskStore(prefs).reconcileOpen(restored))
        assertEquals(listOf(42, 41, 43), VyomFactoryTaskStore(prefs).pending().map { it.issueNumber })
    }

    @Test fun reconciliationSkipsIssueAndSourceIdentityConflictsWithoutOverwriting() {
        val prefs = InMemorySharedPreferences()
        val store = VyomFactoryTaskStore(prefs)
        assertTrue(store.enqueue(task(41, "P2", "source-41", 10)))

        val added = store.reconcileOpen(listOf(
            task(41, "P0", "different-source", 20),
            task(42, "P0", "source-41", 21),
            task(43, "P1", "new-source", 22)
        ))
        assertEquals(1, added)
        assertEquals(listOf(41, 43), store.pending().map { it.issueNumber })
        assertEquals("P2", store.pending().first { it.issueNumber == 41 }.priority)
    }

    @Test fun reconciliationCommitFailureLeavesPreviousQueueIntact() {
        val prefs = InMemorySharedPreferences()
        val store = VyomFactoryTaskStore(prefs)
        assertTrue(store.enqueue(task(41, "P2", "existing", 10)))
        val before = prefs.getString("pending_tasks_v1", null)
        prefs.failCommits = true

        assertThrows(IllegalStateException::class.java) {
            store.reconcileOpen(listOf(task(42, "P1", "remote-42", 20)))
        }
        assertEquals(before, prefs.getString("pending_tasks_v1", null))
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

    @Test fun failedEnqueueCanBeRetriedWithSameIssueAfterStorageRecovers() {
        val prefs = InMemorySharedPreferences().apply { failCommits = true }
        val store = VyomFactoryTaskStore(prefs)
        val original = task(41, "P2", "stable-chat-message-id", 10)

        assertThrows(IllegalStateException::class.java) { store.enqueue(original) }
        prefs.failCommits = false

        assertTrue(store.enqueue(original))
        assertFalse(store.enqueue(original))
        assertEquals(listOf(41), VyomFactoryTaskStore(prefs).pending().map { it.issueNumber })
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
