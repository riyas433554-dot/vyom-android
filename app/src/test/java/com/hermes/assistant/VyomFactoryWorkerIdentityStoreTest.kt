package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryWorkerIdentityStoreTest {
    @Test fun provisionsFiveStableUniqueWorkersForEveryLane() {
        val workers = VyomFactoryWorkerIdentityStore(MemoryPrefs()).ensurePool()

        assertEquals(VyomFactoryWorkerPoolConfig.totalWorkerCount, workers.size)
        VyomFactoryLaneRouter.Lane.values().forEach { lane ->
            val laneWorkers = workers.filter { it.lane == lane }
            assertEquals(VyomFactoryWorkerPoolConfig.MINIMUM_WORKERS_PER_LANE, laneWorkers.size)
            assertEquals(
                (0 until VyomFactoryWorkerPoolConfig.MINIMUM_WORKERS_PER_LANE).toSet(),
                laneWorkers.map { it.slotIndex }.toSet()
            )
        }
        assertEquals(workers.size, workers.map { it.workerId }.toSet().size)
        assertEquals(workers.size, VyomFactoryWorkerPoolConfig.concurrencyLimit(workers))
    }

    @Test fun workerIdentityAssignmentsPersistAcrossStoreRecreation() {
        val prefs = MemoryPrefs()
        var generated = 0
        val first = VyomFactoryWorkerIdentityStore(prefs) { "worker-" + (++generated) + "-identity" }
            .ensurePool()
        val generationCount = generated
        val second = VyomFactoryWorkerIdentityStore(prefs) { "replacement-" + (++generated) + "-identity" }
            .ensurePool()

        assertEquals(first, second)
        assertEquals(generationCount, generated)
    }

    @Test fun duplicateGeneratedWorkerIdsFailClosedWithoutPersistingPartialPool() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryWorkerIdentityStore(prefs) { "duplicate-worker-id" }

        assertThrows(IllegalStateException::class.java) { store.ensurePool() }
        assertNull(prefs.getString("workers_v1", null))
    }

    @Test fun corruptIdentityRegistryFailsClosedWithoutOverwritingIt() {
        val prefs = MemoryPrefs()
        prefs.edit().putString("workers_v1", "{broken").commit()

        assertThrows(IllegalStateException::class.java) {
            VyomFactoryWorkerIdentityStore(prefs).ensurePool()
        }
        assertEquals("{broken", prefs.getString("workers_v1", null))
    }

    @Test fun partialPoolAddsOnlyMissingSlotsAndPreservesExistingIdentities() {
        val prefs = MemoryPrefs()
        val initial = VyomFactoryWorkerIdentityStore(prefs).ensurePool()
        val removed = initial.last()
        val array = org.json.JSONArray(prefs.getString("workers_v1", "[]"))
        val retained = org.json.JSONArray()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            if (item.getString("worker_id") != removed.workerId) retained.put(item)
        }
        prefs.edit().putString("workers_v1", retained.toString()).commit()

        val repaired = VyomFactoryWorkerIdentityStore(prefs).ensurePool()

        assertEquals(VyomFactoryWorkerPoolConfig.totalWorkerCount, repaired.size)
        initial.filterNot { it == removed }.forEach { prior ->
            assertEquals(prior, repaired.single { it.lane == prior.lane && it.slotIndex == prior.slotIndex })
        }
        assertNotEquals(removed.workerId, repaired.single {
            it.lane == removed.lane && it.slotIndex == removed.slotIndex
        }.workerId)
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
