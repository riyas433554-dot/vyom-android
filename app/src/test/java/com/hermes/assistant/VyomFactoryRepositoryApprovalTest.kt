package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryRepositoryApprovalTest {
    private val base = "a".repeat(40)
    private val binding = VyomFactorySystemRegistry.Binding(
        "worker", "https://github.com/riyas433554-dot/vyom-android", base,
        listOf("app/src/main/java/com/hermes/assistant"), 1L
    )
    private fun plan(content: String = "class Safe") = VyomFactoryRepositoryChangePolicy.Plan(
        "worker", 10, "attempt-12345678", "Safe change", "feature/factory-item5-task-intelligence",
        base, listOf(VyomFactoryRepositoryChangePolicy.Change(
            "app/src/main/java/com/hermes/assistant/Safe.kt", content
        )), false
    )

    @Test fun bindsRepositoryBasePathAndContentHash() {
        val store = store(MemoryPrefs(), { 100L }, { "nonce-123456789012" })
        val token = store.issueFromOwnerReview(plan(), binding)
        assertThrows(SecurityException::class.java) {
            store.verifyAndConsume(plan("tampered"), binding, token, 101L)
        }
        assertThrows(SecurityException::class.java) {
            store.verifyAndConsume(plan(), binding.copy(repositoryUrl = "https://github.com/other/repo"), token, 101L)
        }
    }

    @Test fun consumesOnceAndPersistsAcrossStoreRecreation() {
        val prefs = MemoryPrefs()
        val token = store(prefs, { 100L }, { "nonce-123456789012" })
            .issueFromOwnerReview(plan(), binding)
        store(prefs, { 101L })
            .verifyAndConsume(plan(), binding, token)
        assertThrows(SecurityException::class.java) {
            store(prefs, { 102L })
                .verifyAndConsume(plan(), binding, token)
        }
    }

    @Test fun expiredTokenIsRejected() {
        val store = store(MemoryPrefs(), { 100L }, { "nonce-123456789012" })
        val token = store.issueFromOwnerReview(plan(), binding)
        assertThrows(SecurityException::class.java) {
            store.verifyAndConsume(plan(), binding, token, 100L + VyomFactoryRepositoryApprovalStore.TOKEN_TTL_MS)
        }
    }

    @Test fun consumedNonceCommitFailureFailsClosed() {
        val prefs = MemoryPrefs()
        val token = store(prefs, { 100L }, { "nonce-123456789012" })
            .issueFromOwnerReview(plan(), binding)
        prefs.failCommit = true
        assertThrows(IllegalStateException::class.java) {
            store(prefs, { 101L }).verifyAndConsume(plan(), binding, token)
        }
        prefs.failCommit = false
        store(prefs, { 102L }).verifyAndConsume(plan(), binding, token)
        assertTrue(VyomFactoryExecutionJournal(prefs, VyomFactoryWorkerLeaseStore(prefs))
            .isApprovalNonceConsumed(token.nonce))
    }

    private fun store(
        prefs: MemoryPrefs,
        now: () -> Long = { 100L },
        nonce: () -> String = { java.util.UUID.randomUUID().toString() }
    ) = VyomFactoryRepositoryApprovalStore(
        VyomFactoryExecutionJournal(prefs, VyomFactoryWorkerLeaseStore(prefs)), now, nonce
    )

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        var failCommit = false
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int) = defValue
        override fun getLong(key: String, defValue: Long) = defValue
        override fun getFloat(key: String, defValue: Float) = defValue
        override fun getBoolean(key: String, defValue: Boolean) = defValue
        override fun contains(key: String) = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        private inner class Editor : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            override fun putString(key: String, value: String?) = apply { updates[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = this
            override fun putLong(key: String, value: Long) = this
            override fun putFloat(key: String, value: Float) = this
            override fun putBoolean(key: String, value: Boolean) = this
            override fun remove(key: String) = this
            override fun clear() = this
            override fun commit(): Boolean {
                if (failCommit) return false
                updates.forEach { (k, v) -> if (v != null) values[k] = v else values.remove(k) }
                return true
            }
            override fun apply() { commit() }
        }
    }
}
