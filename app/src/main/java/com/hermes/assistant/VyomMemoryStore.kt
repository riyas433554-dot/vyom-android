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

    fun facts(): List<String> {
        if (!isEnabled()) return emptyList()
        return try {
            val array = JSONArray(prefs.getString(KEY_FACTS, "[]") ?: "[]")
            (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf { s -> s.isNotBlank() } }
                .filterNot(::isSensitive).take(MAX_FACTS)
        } catch (_: Exception) { emptyList() }
    }

    fun learnFrom(message: String) {
        if (!isEnabled() || isSensitive(message)) return
        val patterns = listOf(
            Regex("(?i)\\bmy name is ([a-z][a-z .'-]{1,48})"),
            Regex("(?i)\\bmera naam ([a-z][a-z .'-]{1,48}) hai"),
            Regex("(?i)\\bI prefer ([^.!?\\n]{2,100})"),
            Regex("(?i)\\bI like ([^.!?\\n]{2,100})"),
            Regex("(?i)\\bplease remember that ([^.!?\\n]{2,120})"),
            Regex("(?i)\\bmeri preference hai ([^.!?\\n]{2,100})")
        )
        val extracted = patterns.mapNotNull { it.find(message.trim().take(500))?.value?.trim() }
            .map { it.replace(Regex("\\s+"), " ").take(140) }.filterNot(::isSensitive)
        if (extracted.isEmpty()) return
        write((facts() + extracted).distinctBy { it.lowercase() }.takeLast(MAX_FACTS))
    }

    fun replaceFacts(text: String) {
        val safe = text.lineSequence().map(String::trim).filter(String::isNotBlank)
            .filterNot(::isSensitive).map { it.take(140) }.distinctBy { it.lowercase() }
            .takeLast(MAX_FACTS).toList()
        write(safe)
    }

    fun clearFacts() { prefs.edit().remove(KEY_FACTS).apply() }
    private fun write(items: List<String>) { prefs.edit().putString(KEY_FACTS, JSONArray(items).toString()).apply() }

    private fun isSensitive(text: String): Boolean {
        val lower = text.lowercase()
        val forbidden = listOf("password", "passcode", "otp", "one-time password", "verification code",
            "credit card", "debit card", "cvv", "cvc", "card number", "api key", "secret key",
            "access token", "refresh token", "private key", "recovery phrase")
        return forbidden.any(lower::contains) ||
            Regex("\\b(?:\\d[ -]*?){13,19}\\b").containsMatchIn(text) ||
            Regex("(?i)\\b(?:sk-|nvapi-|gh[pousr]_)\\S{12,}").containsMatchIn(text)
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_FACTS = "facts"
        private const val MAX_FACTS = 30
    }
}
