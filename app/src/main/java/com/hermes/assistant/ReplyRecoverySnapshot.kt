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

        fun fromJson(value: JSONObject): ReplyRecoverySnapshot? = fromFields(
            value.optString("message_id"),
            value.optString("status"),
            value.optString("reply"),
            value.optString("error")
        )

        /** Pure parser entry point so JVM unit tests do not depend on Android's org.json stubs. */
        fun fromFields(messageId: String?, rawStatus: String?, rawReply: String?, rawError: String?): ReplyRecoverySnapshot? {
            val id = messageId?.trim().orEmpty()
            val status = rawStatus?.trim()?.lowercase().orEmpty()
            if (id.isBlank() || status !in allowedStatuses) return null
            val reply = rawReply?.takeIf { it.isNotBlank() && it != "null" }
            val error = rawError?.takeIf { it.isNotBlank() && it != "null" }
            if (status == "done" && reply == null) return null
            if (status == "error" && error == null) return null
            if ((status == "queued" || status == "running") && (reply != null || error != null)) return null
            return ReplyRecoverySnapshot(id, status, reply, error)
        }
    }
}