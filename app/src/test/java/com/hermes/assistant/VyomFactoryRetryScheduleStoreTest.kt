package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryRetryScheduleStoreTest {
    @Test fun persistsBoundedRetryScheduleAndReturnsOnlyDueEntries() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryRetryScheduleStore(prefs)
        val first = store.recordFailure(21, "repo-change:abc", "network_timeout", 10_000, true)
        assertEquals(1, first?.failedAttempts)
        assertTrue(first!!.nextAttemptAt in 40_000L..47_500L)
        assertTrue(store.due(39_999).isEmpty())
        assertEquals(first, store.due(40_000).single())
        assertEquals(first, VyomFactoryRetryScheduleStore(prefs).all().single())
    }

    @Test fun incrementsAttemptsAndStopsAfterPolicyLimit() {
        val store = VyomFactoryRetryScheduleStore(MemoryPrefs())
        store.recordFailure(21, "repo-change:abc", "server_unavailable", 1_000, true)
        val second = store.recordFailure(21, "repo-change:abc", "server_unavailable", 31_000, true)
        assertEquals(2, second?.failedAttempts)
        assertTrue(second!!.nextAttemptAt in 151_000L..181_000L)
        assertNull(store.recordFailure(21, "repo-change:abc", "server_unavailable", 151_000, true))
        assertTrue(store.all().isEmpty())
    }

    @Test fun neverSchedulesNonIdempotentOrPermanentFailures() {
        val store = VyomFactoryRetryScheduleStore(MemoryPrefs())
        assertNull(store.recordFailure(1, "op:1", "network_timeout", 1_000, false))
        assertNull(store.recordFailure(2, "op:2", "permission_denied", 1_000, true))
        assertTrue(store.all().isEmpty())
    }

    @Test fun differentOperationsForSameIssueHaveIndependentSchedulesAndCanBeCleared() {
        val store = VyomFactoryRetryScheduleStore(MemoryPrefs())
        store.recordFailure(7, "op:a", "network_timeout", 1_000, true)
        store.recordFailure(7, "op:b", "network_timeout", 2_000, true)
        assertEquals(2, store.all().size)
        assertTrue(store.clear(7, "op:a"))
        assertFalse(store.clear(7, "op:a"))
        assertEquals("op:b", store.all().single().operationKey)
    }

    @Test fun corruptDataAndCommitFailureFailClosed() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryRetryScheduleStore(prefs)
        prefs.edit().putString("retry_schedules_v1", "{broken").commit()
        assertThrows(IllegalStateException::class.java) { store.recordFailure(1, "op:1", "network_timeout", 1_000, true) }
        assertEquals("{broken", prefs.getString("retry_schedules_v1", null))

        val failing = VyomFactoryRetryScheduleStore(MemoryPrefs().apply { failCommit = true })
        assertThrows(IllegalStateException::class.java) {
            failing.recordFailure(2, "op:2", "network_timeout", 1_000, true)
        }
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
