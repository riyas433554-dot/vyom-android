package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryIntakeDraftTest {
    @Test fun preservesOriginalUnicodeRequestAndAcceptsStructuredDraft() {
        val original = "বাংলা: অ্যাপে crash হচ্ছে। Fix file `MainActivity.kt`"
        val draft = VyomFactoryIntakeDraft.fromInterpreterJson(original, validJson())
        assertEquals(original, draft.originalRequest)
        assertEquals("P2", draft.priority)
        assertEquals(listOf("Fix MainActivity.kt crash path"), draft.subtasks)
        assertFalse(draft.requiresClarification)
        assertFalse(draft.requiresExplicitConfirmation)
    }

    @Test fun lowConfidenceAndClarificationRequireUserInput() {
        val json = JSONObject(validJson())
            .put("confidence", 0.42)
            .put("clarification_question", "Which screen should be changed?")
        val draft = VyomFactoryIntakeDraft.fromInterpreterJson("画面を直してください", json.toString())
        assertTrue(draft.requiresClarification)
        assertTrue(draft.requiresExplicitConfirmation)
    }

    @Test fun destructiveRequestRequiresExplicitConfirmation() {
        val draft = VyomFactoryIntakeDraft.fromInterpreterJson(
            "সব মুছে নতুন করে শুরু করো", validJson()
        )
        assertTrue(draft.requiresExplicitConfirmation)
    }

    @Test fun rejectsMissingSchemaAndInvalidPriorityInsteadOfDefaulting() {
        val missingSchema = JSONObject(validJson()).put("schema_version", 9)
        assertThrows(IllegalArgumentException::class.java) {
            VyomFactoryIntakeDraft.fromInterpreterJson("hello", missingSchema.toString())
        }
        val invalidPriority = JSONObject(validJson()).put("priority", "urgent")
        assertThrows(IllegalArgumentException::class.java) {
            VyomFactoryIntakeDraft.fromInterpreterJson("hello", invalidPriority.toString())
        }
    }

    @Test fun rejectsMalformedListsAndOutOfRangeConfidence() {
        val malformed = JSONObject(validJson()).put("acceptance_criteria", JSONArray().put(""))
        assertThrows(IllegalArgumentException::class.java) {
            VyomFactoryIntakeDraft.fromInterpreterJson("hello", malformed.toString())
        }
        val confidence = JSONObject(validJson()).put("confidence", 1.5)
        assertThrows(IllegalArgumentException::class.java) {
            VyomFactoryIntakeDraft.fromInterpreterJson("hello", confidence.toString())
        }
    }

    private fun validJson() = JSONObject()
        .put("schema_version", 1)
        .put("language_hint", "bn")
        .put("intent", "bug_fix")
        .put("title", "Fix application crash")
        .put("objective", "Prevent the app from crashing on this screen")
        .put("constraints", JSONArray().put("Keep existing pairing behavior"))
        .put("non_goals", JSONArray().put("Do not change backend"))
        .put("affected_components", JSONArray().put("MainActivity.kt"))
        .put("priority", "P2")
        .put("priority_rationale", "Affects one user flow; no data loss shown")
        .put("subtasks", JSONArray().put("Fix MainActivity.kt crash path"))
        .put("acceptance_criteria", JSONArray().put("Screen opens without a crash"))
        .put("confidence", 0.91)
        .toString()
}
