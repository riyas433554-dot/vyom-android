package com.hermes.assistant

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/** Owner-controlled automation macros stored encrypted on this device. */
internal class VyomAutomationStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        val key = MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            appContext, "vyom_automations", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Synchronized
    fun list(): List<AutomationEngine.Macro> {
        return try {
            val array = JSONArray(prefs.getString(KEY_MACROS, "[]") ?: "[]")
            (0 until array.length()).mapNotNull { index ->
                runCatching { decode(array.getJSONObject(index)) }.getOrNull()
            }.filter { AutomationEngine.validate(it).valid }.take(MAX_MACROS)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun get(id: String): AutomationEngine.Macro? = list().firstOrNull { it.id == id }

    @Synchronized
    fun save(macro: AutomationEngine.Macro, ownerConfirmed: Boolean): AutomationEngine.Macro {
        check(ownerConfirmed) { "Owner confirmation required." }
        val validation = AutomationEngine.validate(macro)
        require(validation.valid) { "Invalid automation: " + validation.errors.joinToString(",") }
        val current = list().filterNot { it.id == macro.id }
        require(current.size < MAX_MACROS) { "Automation limit reached." }
        val saved = macro.copy(name = macro.name.trim())
        write(current + saved)
        return saved
    }

    @Synchronized
    fun delete(id: String, ownerConfirmed: Boolean): Boolean {
        check(ownerConfirmed) { "Owner confirmation required." }
        val current = list()
        if (current.none { it.id == id }) return false
        write(current.filterNot { it.id == id })
        return true
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean, ownerConfirmed: Boolean): Boolean {
        check(ownerConfirmed) { "Owner confirmation required." }
        val current = list()
        val existing = current.firstOrNull { it.id == id } ?: return false
        write(current.map { if (it.id == id) existing.copy(enabled = enabled) else it })
        return true
    }

    private fun write(macros: List<AutomationEngine.Macro>) {
        val array = JSONArray()
        macros.take(MAX_MACROS).forEach { array.put(encode(it)) }
        prefs.edit().putString(KEY_MACROS, array.toString()).apply()
    }

    private fun encode(macro: AutomationEngine.Macro): JSONObject {
        val steps = JSONArray()
        macro.steps.forEach { step ->
            steps.put(JSONObject()
                .put("type", step.type).put("target", step.target).put("text", step.text)
                .put("durationMs", step.durationMs).put("x1", step.x1.toDouble())
                .put("y1", step.y1.toDouble()).put("x2", step.x2.toDouble()).put("y2", step.y2.toDouble()))
        }
        return JSONObject().put("id", macro.id).put("name", macro.name)
            .put("enabled", macro.enabled).put("steps", steps)
    }

    private fun decode(json: JSONObject): AutomationEngine.Macro {
        val stepsJson = json.getJSONArray("steps")
        val steps = (0 until stepsJson.length()).map { index ->
            val step = stepsJson.getJSONObject(index)
            AutomationEngine.Step(
                type = step.optString("type"),
                target = step.optString("target"),
                text = step.optString("text"),
                durationMs = step.optInt("durationMs"),
                x1 = step.optDouble("x1").toFloat(),
                y1 = step.optDouble("y1").toFloat(),
                x2 = step.optDouble("x2").toFloat(),
                y2 = step.optDouble("y2").toFloat()
            )
        }
        return AutomationEngine.Macro(
            id = json.optString("id"),
            name = json.optString("name"),
            steps = steps,
            enabled = json.optBoolean("enabled", true)
        )
    }

    companion object {
        private const val KEY_MACROS = "macros"
        private const val MAX_MACROS = 20
    }
}
