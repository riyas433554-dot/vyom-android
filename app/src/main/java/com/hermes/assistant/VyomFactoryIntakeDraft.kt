package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject

/**
 * Validated boundary between a future multilingual interpreter and Factory registration.
 * This class validates structure only; it does not claim to understand natural language.
 */
internal data class VyomFactoryIntakeDraft(
    val originalRequest: String,
    val languageHint: String,
    val intent: String,
    val title: String,
    val objective: String,
    val constraints: List<String>,
    val nonGoals: List<String>,
    val affectedComponents: List<String>,
    val priority: String,
    val priorityRationale: String,
    val subtasks: List<String>,
    val acceptanceCriteria: List<String>,
    val clarificationQuestion: String?,
    val confidence: Double
) {
    val requiresClarification: Boolean
        get() = !clarificationQuestion.isNullOrBlank() || confidence < MIN_CONFIDENCE

    val requiresExplicitConfirmation: Boolean
        get() = requiresClarification || priority == "P0" ||
            DESTRUCTIVE_TERMS.any { originalRequest.contains(it, ignoreCase = true) }

    companion object {
        private const val SCHEMA_VERSION = 1
        private const val MIN_CONFIDENCE = 0.75
        private val PRIORITIES = setOf("P0", "P1", "P2", "P3")
        private val DESTRUCTIVE_TERMS = listOf(
            "delete all", "wipe", "factory reset", "drop database", "remove every",
            "সব মুছে", "সব ডিলিট", "حذف کریں", "سب حذف", "全て削除", "全部删除", "全部刪除"
        )

        /**
         * originalRequest is supplied by the caller and never sourced from model output.
         * Unknown/malformed model output is rejected instead of silently defaulted.
         */
        fun fromInterpreterJson(originalRequest: String, raw: String): VyomFactoryIntakeDraft {
            require(originalRequest.isNotBlank() && originalRequest.length <= 12000) {
                "Original request must contain 1–12,000 characters"
            }
            val json = JSONObject(raw)
            require(json.optInt("schema_version", -1) == SCHEMA_VERSION) { "Unsupported intake schema" }
            val priority = requiredString(json, "priority", 2)
            require(priority in PRIORITIES) { "Invalid priority" }
            val confidence = json.optDouble("confidence", Double.NaN)
            require(confidence.isFinite() && confidence in 0.0..1.0) { "Invalid confidence" }
            val question = optionalString(json, "clarification_question", 500)
            return VyomFactoryIntakeDraft(
                originalRequest = originalRequest,
                languageHint = requiredString(json, "language_hint", 40),
                intent = requiredString(json, "intent", 80),
                title = requiredString(json, "title", 120).also {
                    require(it.length >= 4) { "Title must contain at least 4 characters" }
                },
                objective = requiredString(json, "objective", 2000),
                constraints = stringList(json, "constraints", 30, 500),
                nonGoals = stringList(json, "non_goals", 30, 500),
                affectedComponents = stringList(json, "affected_components", 30, 200),
                priority = priority,
                priorityRationale = requiredString(json, "priority_rationale", 500),
                subtasks = stringList(json, "subtasks", 30, 1000),
                acceptanceCriteria = stringList(json, "acceptance_criteria", 30, 1000),
                clarificationQuestion = question,
                confidence = confidence
            )
        }

        private fun requiredString(json: JSONObject, key: String, max: Int): String {
            val value = json.optString(key, "").trim()
            require(value.isNotEmpty() && value.length <= max) { "Invalid $key" }
            return value
        }

        private fun optionalString(json: JSONObject, key: String, max: Int): String? {
            if (!json.has(key) || json.isNull(key)) return null
            return json.optString(key).trim().also {
                require(it.length <= max) { "Invalid $key" }
            }.takeIf { it.isNotEmpty() }
        }

        private fun stringList(json: JSONObject, key: String, maxItems: Int, maxLength: Int): List<String> {
            val array = json.optJSONArray(key) ?: throw IllegalArgumentException("Missing $key array")
            require(array.length() <= maxItems) { "Too many $key entries" }
            return (0 until array.length()).map { index ->
                val value = array.optString(index, "").trim()
                require(value.isNotEmpty() && value.length <= maxLength) { "Invalid $key entry $index" }
                value
            }
        }
    }
}
