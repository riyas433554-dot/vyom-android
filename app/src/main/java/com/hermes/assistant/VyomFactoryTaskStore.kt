package com.hermes.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local durable inbox for Factory execution.
 *
 * GitHub Issues remains the cross-client task registry. This store is only the
 * Android device's execution inbox; it does not claim a shared/atomic lease.
 */
internal class VyomFactoryTaskStore(context: Context) {
    data class QueuedTask(
        val issueNumber: Int,
        val title: String,
        val priority: String,
        val sourceId: String,
        val enqueuedAt: Long
    )

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun enqueue(task: QueuedTask): Boolean {
        require(task.issueNumber > 0)
        require(task.title.isNotBlank() && task.title.length <= 200)
        require(task.priority in setOf("P0", "P1", "P2", "P3"))
        require(task.sourceId.isNotBlank() && task.sourceId.length <= 160)
        val current = readAll().toMutableList()
        if (!VyomFactoryTaskQueuePolicy.isUnique(current, task)) return false
        current += task
        persist(current)
        return true
    }

    @Synchronized
    fun pending(): List<QueuedTask> = VyomFactoryTaskQueuePolicy.order(readAll())

    @Synchronized
    fun remove(issueNumber: Int): Boolean {
        val current = readAll()
        val remaining = current.filterNot { it.issueNumber == issueNumber }
        if (remaining.size == current.size) return false
        persist(remaining)
        return true
    }

    @Synchronized
    fun contains(issueNumber: Int): Boolean = readAll().any { it.issueNumber == issueNumber }

    private fun readAll(): List<QueuedTask> {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val issue = item.optInt("issue_number", -1)
                    val title = item.optString("title")
                    val priority = item.optString("priority")
                    val sourceId = item.optString("source_id")
                    val enqueuedAt = item.optLong("enqueued_at", -1)
                    if (issue > 0 && title.isNotBlank() && priority in setOf("P0", "P1", "P2", "P3") &&
                        sourceId.isNotBlank() && enqueuedAt >= 0) {
                        add(QueuedTask(issue, title, priority, sourceId, enqueuedAt))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(tasks: List<QueuedTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject()
                .put("issue_number", task.issueNumber)
                .put("title", task.title)
                .put("priority", task.priority)
                .put("source_id", task.sourceId)
                .put("enqueued_at", task.enqueuedAt))
        }
        check(prefs.edit().putString(KEY_QUEUE, array.toString()).commit()) {
            "Factory queue could not be persisted"
        }
    }

    companion object {
        private const val PREFS = "vyom_factory_execution"
        private const val KEY_QUEUE = "pending_tasks_v1"
    }
}
