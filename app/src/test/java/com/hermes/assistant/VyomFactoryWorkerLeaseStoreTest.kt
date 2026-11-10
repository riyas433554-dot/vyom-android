package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryWorkerLeaseStoreTest {
    @Test fun onlyOneOwnerCanHoldAnUnexpiredIssueLease() {
        val store = VyomFactoryWorkerLeaseStore(MemoryPrefs())
        val first = store.claim(12, "worker-a", 1_000, 10_000)!!
        assertNull(store.claim(12, "worker-b", 2_000, 10_000))
        assertEquals(first, store.claim(12, "worker-a", 2_000, 10_000))
        assertEquals(first, store.active(12, 2_000))
    }

    @Test fun expiredLeaseCanBeClaimedByANewOwnerAndOldTokenCannotReleaseIt() {
        val store = VyomFactoryWorkerLeaseStore(MemoryPrefs())
        val first = store.claim(12, "worker-a", 1_000, 5_000)!!
        val second = store.claim(12, "worker-b", 6_000, 5_000)!!
        assertNotEquals(first.leaseId, second.leaseId)
        assertFalse(store.release(first))
        assertEquals(second, store.active(12, 6_000))
    }

    @Test fun renewRequiresExactUnexpiredLeaseAndPersistsAcrossRecreation() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryWorkerLeaseStore(prefs)
        val lease = store.claim(9, "worker-a", 10_000, 5_000)!!
        val renewed = store.renew(lease, 12_000, 20_000)!!
        assertEquals(32_000L, renewed.expiresAt)
        assertNull(store.renew(lease.copy(leaseId = "stale"), 13_000, 5_000))
        assertEquals(renewed, VyomFactoryWorkerLeaseStore(prefs).active(9, 13_000))
        assertNull(store.active(9, 32_000))
    }

    @Test fun releaseIsTokenFencedAndDurable() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryWorkerLeaseStore(prefs)
        val lease = store.claim(7, "worker-a", 100, 5_000)!!
        assertFalse(store.release(lease.copy(workerId = "worker-b")))
        assertTrue(store.release(lease))
        assertNull(VyomFactoryWorkerLeaseStore(prefs).active(7, 101))
    }

    @Test fun oneWorkerIdentityCannotHoldTwoConcurrentTaskLeases() {
        val store = VyomFactoryWorkerLeaseStore(MemoryPrefs())
        val first = store.claim(101, "worker-a", 1_000, 10_000)!!

        assertNull(store.claim(102, "worker-a", 1_001, 10_000))
        assertEquals(first, store.claim(101, "worker-a", 1_002, 10_000))
    }

    @Test fun distinctWorkerIdentitiesCanHoldDistinctTaskLeasesConcurrently() {
        val store = VyomFactoryWorkerLeaseStore(MemoryPrefs())
        val first = store.claim(103, "worker-a", 1_000, 10_000)!!
        val second = store.claim(104, "worker-b", 1_000, 10_000)!!

        assertNotEquals(first.workerId, second.workerId)
        assertNotEquals(first.leaseId, second.leaseId)
    }

    @Test fun legacyOwnerIdLeaseRecordsRemainReadableAsWorkerIdentity() {
        val prefs = MemoryPrefs()
        prefs.edit().putString(
            "leases_v1",
            """[{"issue_number":105,"owner_id":"worker-legacy","lease_id":"legacy-lease-123","acquired_at":1000,"expires_at":11000}]"""
        ).commit()

        val lease = VyomFactoryWorkerLeaseStore(prefs).active(105, 2_000)

        assertNotNull(lease)
        assertEquals("worker-legacy", lease!!.workerId)
    }

    @Test fun invalidLeaseBoundsAndCorruptPersistenceFailClosed() {
        val prefs = MemoryPrefs()
        val store = VyomFactoryWorkerLeaseStore(prefs)
        assertThrows(IllegalArgumentException::class.java) { store.claim(1, "w", 0, 1) }
        prefs.edit().putString("leases_v1", "{broken").commit()
        assertThrows(IllegalStateException::class.java) { store.claim(2, "w", 1, 5_000) }
        assertEquals("{broken", prefs.getString("leases_v1", null))
    }

    @Test fun failedCommitDoesNotReportLeaseClaimAsSuccessful() {
        val prefs = MemoryPrefs().apply { failCommit = true }
        val store = VyomFactoryWorkerLeaseStore(prefs)
        assertThrows(IllegalStateException::class.java) { store.claim(3, "w", 1, 5_000) }
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
