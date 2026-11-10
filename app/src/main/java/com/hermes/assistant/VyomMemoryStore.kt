package com.hermes.assistant

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray

/** Encrypted, user-controlled on-device facts. Safety policy and credentials are never stored here. */
internal class VyomMemoryStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        val key = MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            appContext, "vyom_memory", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)
    fun setEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_ENABLED, enabled).apply() }

    /** Seeds the user's explicit learn-before-act preference into encrypted on-device memory. */
    fun ensureCorePrinciples() {
        if (!isEnabled()) return
        val principle = CORE_LEARN_BEFORE_ACT
        if (facts().none { it.equals(principle, ignoreCase = true) }) {
            write((facts() + principle).takeLast(MAX_FACTS))
        }
    }

    fun facts(): List<String> {
        if (!isEnabled()) return emptyList()
        return try {
            val array = JSONArray(prefs.getString(KEY_FACTS, "[]") ?: "[]")
            (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf { s -> s.isNotBlank() } }
                .filterNot(::isSensitive).take(MAX_FACTS)
        } catch (_: Exception) { emptyList() }
    }

    fun learnFrom(message: String) {
        if (!isEnabled()) return
        val extracted = MemoryCapturePolicy.extract(message)
        if (extracted.isEmpty()) return
        write((facts() + extracted).distinctBy { it.lowercase() }.takeLast(MAX_FACTS))
    }

    fun replaceFacts(text: String) {
        val safe = text.lineSequence().map(String::trim).filter(String::isNotBlank)
            .filterNot(::isSensitive).map { it.take(300) }.distinctBy { it.lowercase() }
            .toList().takeLast(MAX_FACTS)
        write(safe)
    }

    fun clearFacts() { prefs.edit().remove(KEY_FACTS).apply() }
    private fun write(items: List<String>) { prefs.edit().putString(KEY_FACTS, JSONArray(items).toString()).apply() }

    private fun isSensitive(text: String): Boolean = MemoryCapturePolicy.isSensitive(text)

    companion object {
        const val CORE_LEARN_BEFORE_ACT =
            "Vyom preference: if knowledge or a skill is missing, learn from trusted documentation/repositories, review license/source/dependencies/security, test safely, then act; report only verified results."
        private const val KEY_ENABLED = "enabled"
        private const val KEY_FACTS = "facts"
        private const val MAX_FACTS = 100
    }
}
