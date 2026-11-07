package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class VyomFactoryRepositoryChangeAdapterTest {
    private val base = "a".repeat(40)
    private val registry = VyomFactorySystemRegistry(
        File.createTempFile("repo-adapter", ".tmp").apply { delete(); mkdirs() }
    ).also {
        it.register(
            VyomFactorySystemRegistry.Binding(
                "worker", "https://github.com/riyas433554-dot/vyom-android", base,
                listOf("app/src/main/java/com/hermes/assistant"), 1L
            ), true
        )
    }
    private val plan = VyomFactoryRepositoryChangePolicy.Plan(
        "worker", 22, "attempt-12345678", "Improve safe reply output",
        "feature/factory-item5-task-intelligence", base,
        listOf(VyomFactoryRepositoryChangePolicy.Change(
            "app/src/main/java/com/hermes/assistant/SafeReply.kt", "class SafeReply"
        )), false
    )
    private val envelope = VyomFactoryExecutionEnvelope(
        VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE,
        "Prepare repository proposal", "propose_change", org.json.JSONObject(), true, 22
    )

    @Test fun rejectsWithoutLocalTokenEvenWhenIssueEnvelopeIsExecutable() {
        val adapter = adapter(ConcurrentHashMap())
        assertThrows(SecurityException::class.java) { adapter.run(envelope, NeverStop) }
    }

    @Test fun stopBlocksBeforeApprovalNonceCanBeConsumed() {
        val prefs = ApprovalPrefs()
        val journal = VyomFactoryExecutionJournal(prefs, VyomFactoryWorkerLeaseStore(prefs))
        val store = VyomFactoryRepositoryApprovalStore(journal, { 100L }, { "nonce-123456789012" })
        val approval = store.issueFromOwnerReview(plan, registry.get("worker")!!)
        val pending = ConcurrentHashMap<Int, VyomFactoryRepositoryChangeAdapter.ApprovedPlan>()
        pending[22] = VyomFactoryRepositoryChangeAdapter.ApprovedPlan(plan, approval)
        assertThrows(VyomFactoryExecutionStoppedException::class.java) {
            adapter(pending, store).run(envelope, StopNow)
        }
        assertTrue(pending.containsKey(22))
        assertEquals(false, journal.isApprovalNonceConsumed(approval.nonce))
    }

    private fun adapter(
        pending: ConcurrentHashMap<Int, VyomFactoryRepositoryChangeAdapter.ApprovedPlan>,
        store: VyomFactoryRepositoryApprovalStore = VyomFactoryRepositoryApprovalStore(
            VyomFactoryExecutionJournal(ApprovalPrefs(), VyomFactoryWorkerLeaseStore(ApprovalPrefs()))
        )
    ) = VyomFactoryRepositoryChangeAdapter(registry, "token-that-is-long-enough-for-github", store, pending)

    private class ApprovalPrefs : android.content.SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int) = defValue
        override fun getLong(key: String, defValue: Long) = defValue
        override fun getFloat(key: String, defValue: Float) = defValue
        override fun getBoolean(key: String, defValue: Boolean) = defValue
        override fun contains(key: String) = values.containsKey(key)
        override fun edit(): android.content.SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        private inner class Editor : android.content.SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            override fun putString(key: String, value: String?) = apply { updates[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = this
            override fun putLong(key: String, value: Long) = this
            override fun putFloat(key: String, value: Float) = this
            override fun putBoolean(key: String, value: Boolean) = this
            override fun remove(key: String) = this
            override fun clear() = this
            override fun commit(): Boolean { updates.forEach { (k, v) -> if (v != null) values[k] = v }; return true }
            override fun apply() { commit() }
        }
    }

    private object NeverStop : VyomFactoryExecutionControl { override fun shouldStop() = false }
    private object StopNow : VyomFactoryExecutionControl { override fun shouldStop() = true }
}
