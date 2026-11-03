package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject

/** Canonical, versioned metadata stored in a GitHub Issue body. */
internal object VyomFactoryStatus {
    data class Metadata(
        val priority: String,
        val status: String,
        val source: String,
        val sourceId: String,
        val idempotencyKey: String,
        val events: List<String>
    )

    private const val MARKER = "VYOM_FACTORY_META:"
    private val priorities = setOf("P0", "P1", "P2", "P3")
    private val statuses = setOf("open", "in_progress", "blocked", "paused", "completed", "cancelled")
    private val sources = setOf("factory", "vyom_chat", "chatgpt", "termux")

    fun idempotencyKey(source: String, sourceId: String): String? {
        val normalizedSource = source.trim().lowercase()
        val normalizedId = sourceId.trim()
        if (normalizedSource !in sources || normalizedId.isBlank() || normalizedId.length > 120) return null
        if (normalizedId.any { it.isISOControl() }) return null
        return "$normalizedSource:$normalizedId"
    }

    fun readMetadata(body: String?): Metadata? {
        if (body.isNullOrBlank()) return null
        val match = Regex("<!--\\s*$MARKER(.*?)\\s*-->", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(body) ?: return null
        return runCatching {
            val json = JSONObject(match.groupValues[1])
            if (json.optInt("schema", -1) != 1) return null
            val priority = json.optString("priority")
            val status = json.optString("status")
            val source = json.optString("source")
            val sourceId = json.optString("source_id")
            val key = json.optString("idempotency_key")
            if (priority !in priorities || status !in statuses || source !in sources ||
                sourceId.isBlank() || idempotencyKey(source, sourceId) != key) return null
            val rawEvents = json.optJSONArray("events") ?: return null
            if (rawEvents.length() > 500) return null
            val events = (0 until rawEvents.length()).map { rawEvents.optString(it) }
            Metadata(priority, status, source, sourceId, key, events)
        }.getOrNull()
    }

    fun withMetadata(description: String, metadata: Metadata): String {
        require(metadata.priority in priorities)
        require(metadata.status in statuses)
        require(idempotencyKey(metadata.source, metadata.sourceId) == metadata.idempotencyKey)
        require(metadata.events.size <= 500)
        val clean = description.replace(
            Regex("<!--\\s*$MARKER.*?\\s*-->", setOf(RegexOption.DOT_MATCHES_ALL)), ""
        ).trim()
        val json = JSONObject()
            .put("schema", 1)
            .put("priority", metadata.priority)
            .put("status", metadata.status)
            .put("source", metadata.source)
            .put("source_id", metadata.sourceId)
            .put("idempotency_key", metadata.idempotencyKey)
            .put("events", JSONArray(metadata.events))
        return (if (clean.isBlank()) "" else "$clean\\n\\n") + "<!-- $MARKER$json -->"
    }

    fun canTransition(from: String, to: String): Boolean {
        if (from !in statuses || to !in statuses || from == to) return false
        return when (from) {
            "open" -> to in setOf("in_progress", "blocked", "paused", "cancelled")
            "in_progress" -> to in setOf("open", "blocked", "paused", "completed", "cancelled")
            "blocked" -> to in setOf("open", "in_progress", "cancelled")
            "paused" -> to in setOf("open", "in_progress", "cancelled")
            else -> false
        }
    }
}
