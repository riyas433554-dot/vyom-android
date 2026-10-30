package com.hermes.assistant

import org.json.JSONObject

/** Validated response from the optional R1b GET /api/android/reply/{id} endpoint. */
internal data class ReplyRecoverySnapshot(
    val messageId: String,
    val status: String,
    val reply: String?,
    val error: String?
) {
    val isTerminal: Boolean get() = status == "done" || status == "error"

    companion object {
        private val allowedStatuses = setOf("queued", "running", "done", "error")

        fun fromJson(value: JSONObject): ReplyRecoverySnapshot? {
            val id = value.optString("message_id").trim()
            val status = value.optString("status").trim().lowercase()
            if (id.isBlank() || status !in allowedStatuses) return null
            val reply = value.optString("reply").takeIf { it.isNotBlank() && it != "null" }
            val error = value.optString("error").takeIf { it.isNotBlank() && it != "null" }
            if (status == "done" && reply == null) return null
            if (status == "error" && error == null) return null
            if (status == "queued" || status == "running") {
                if (reply != null || error != null) return null
            }
            return ReplyRecoverySnapshot(id, status, reply, error)
        }
    }
}