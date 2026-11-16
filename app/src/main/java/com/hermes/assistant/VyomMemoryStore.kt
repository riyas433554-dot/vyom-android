package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/** Minimal storage boundary so atomic memory behavior can be tested without Android/Robolectric. */
internal interface VyomMemoryStorage {
    fun readBoolean(key: String, defaultValue: Boolean): Boolean
    fun readString(key: String, defaultValue: String): String?
    fun commitBoolean(key: String, value: Boolean): Boolean
    fun commitString(key: String, value: String): Boolean
    fun remove(key: String): Boolean
}

private class EncryptedVyomMemoryStorage(context: Context) : VyomMemoryStorage {
    private val prefs: SharedPreferences by lazy {
        val key = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context.applicationContext, "vyom_memory", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun readBoolean(key: String, defaultValue: Boolean) = prefs.getBoolean(key, defaultValue)
    override fun readString(key: String, defaultValue: String) = prefs.getString(key, defaultValue)
    override fun commitBoolean(key: String, value: Boolean) =
        prefs.edit().putBoolean(key, value).commit()
    override fun commitString(key: String, value: String) =
        prefs.edit().putString(key, value).commit()
    override fun remove(key: String) = prefs.edit().remove(key).commit()
}

/**
 * Encrypted, user-controlled on-device facts. Every persisted fact is untrusted reference data.
 * All mutation methods use synchronous SharedPreferences.commit() and MUST be called off the main
 * thread. The store rejects main-thread mutations rather than risking UI-thread disk I/O.
 */
internal class VyomMemoryStore private constructor(
    private val storage: VyomMemoryStorage,
    private val isMainThread: () -> Boolean
) {
    constructor(context: Context) : this(
        EncryptedVyomMemoryStorage(context.applicationContext),
        { Thread.currentThread().name == "main" }
    )
    internal constructor(storage: VyomMemoryStorage, testOnly: Boolean = true) :
        this(storage, { Thread.currentThread().name == "main" })
    internal constructor(
        storage: VyomMemoryStorage,
        isMainThread: () -> Boolean,
        testOnly: Boolean = true
    ) : this(storage, isMainThread)

    fun isEnabled(): Boolean = synchronized(LOCK) {
        storage.readBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(enabled: Boolean) = synchronized(LOCK) {
        if (!requireBackgroundThread()) return@synchronized
        check(storage.commitBoolean(KEY_ENABLED, enabled)) { "Memory setting could not be persisted" }
    }

    /** Seeds the user-controlled learn-before-act preference; it remains tagged as untrusted data. */
    fun ensureCorePrinciples() = synchronized(LOCK) {
        if (!isEnabledLocked()) return@synchronized
        val current = readFactsLocked()
        if (current.none { it.equals(CORE_LEARN_BEFORE_ACT, ignoreCase = true) }) {
            writeLocked(current + CORE_LEARN_BEFORE_ACT)
        }
    }

    fun facts(): List<String> = synchronized(LOCK) {
        if (!isEnabledLocked()) emptyList() else readFactsLocked()
    }

    fun learnFrom(message: String) = synchronized(LOCK) {
        if (!isEnabledLocked()) return@synchronized
        val extracted = MemoryCapturePolicy.extract(message)
            .mapNotNull(::safeFact)
        if (extracted.isEmpty()) return@synchronized
        writeLocked(readFactsLocked() + extracted)
    }

    fun replaceFacts(text: String) = synchronized(LOCK) {
        val safe = text.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .mapNotNull(::safeFact)
            .toList()
        writeLocked(safe)
    }

    fun clearFacts() = synchronized(LOCK) {
        if (!requireBackgroundThread()) return@synchronized
        check(storage.remove(KEY_FACTS)) { "Memory facts could not be cleared" }
    }

    private fun isEnabledLocked() = storage.readBoolean(KEY_ENABLED, true)

    /**
     * SharedPreferences.commit() is synchronous. A missed main-thread call must never crash the
     * activity: safely skip that write. All known UI callers dispatch through background executors.
     */
    private fun requireBackgroundThread(): Boolean = !isMainThread()

    /** Reads both legacy string entries and the current tagged-object schema. */
    private fun readFactsLocked(): List<String> {
        val raw = storage.readString(KEY_FACTS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            val decoded = (0 until array.length()).mapNotNull { index ->
                when (val item = array.opt(index)) {
                    is JSONObject -> {
                        if (item.optString("trust") != UNTRUSTED_TAG) null
                        else item.optString("text").takeIf { it.isNotBlank() }
                    }
                    is String -> item.takeIf { it.isNotBlank() } // legacy values are treated as untrusted
                    else -> null
                }
            }
            decoded.mapNotNull(::safeFact).distinctBy { it.lowercase() }.takeLast(MAX_FACTS)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun safeFact(value: String): String? {
        val normalized = value.replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank() || MemoryCapturePolicy.isSensitive(normalized)) return null
        return normalized.take(MAX_FACT_CHARS).trim().takeIf { it.isNotBlank() }
    }

    /** One JSON snapshot, one synchronous commit, under a process-wide read/modify/write lock. */
    private fun writeLocked(items: List<String>) {
        if (!requireBackgroundThread()) return
        val safe = items.mapNotNull(::safeFact)
            .distinctBy { it.lowercase() }
            .takeLast(MAX_FACTS)
            .toMutableList()
        var encoded = encode(safe)
        while (safe.isNotEmpty() && encoded.toByteArray(Charsets.UTF_8).size > MAX_SERIALIZED_BYTES) {
            safe.removeAt(0)
            encoded = encode(safe)
        }
        check(encoded.toByteArray(Charsets.UTF_8).size <= MAX_SERIALIZED_BYTES) {
            "Memory facts exceed the storage size limit"
        }
        check(storage.commitString(KEY_FACTS, encoded)) {
            "Memory facts could not be atomically persisted"
        }
    }

    private fun encode(items: List<String>): String {
        val array = JSONArray()
        items.forEach { fact ->
            array.put(JSONObject().put("text", fact).put("trust", UNTRUSTED_TAG).put("instruction_like", MemoryCapturePolicy.isInstructionLike(fact)))
        }
        return array.toString()
    }

    companion object {
        const val CORE_LEARN_BEFORE_ACT =
            "Vyom preference: if knowledge or a skill is missing, learn from trusted documentation/repositories, review license/source/dependencies/security, test safely, then act; report only verified results."
        private const val KEY_ENABLED = "enabled"
        private const val KEY_FACTS = "facts"
        internal const val MAX_FACTS = 100
        internal const val MAX_FACT_CHARS = 300
        internal const val MAX_SERIALIZED_BYTES = 32 * 1024
        internal const val UNTRUSTED_TAG = "untrusted"
        private val LOCK = Any()
    }
}
