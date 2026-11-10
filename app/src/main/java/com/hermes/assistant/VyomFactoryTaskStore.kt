package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local durable inbox for Factory execution.
 *
 * GitHub Issues remains the cross-client task registry. This store is only the
 * Android device's execution inbox; it does not claim a shared/atomic lease.
 */
internal class VyomFactoryTaskStore internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    )
    data class QueuedTask(
        val issueNumber: Int,
        val title: String,
        val priority: String,
        val sourceId: String,
        val enqueuedAt: Long,
        val lane: VyomFactoryLaneRouter.Lane = VyomFactoryLaneRouter.Lane.CUSTOM,
        val lanePlanningAllowed: Boolean = false
    )


    
    fun enqueue(task: QueuedTask): Boolean = synchronized(PROCESS_LOCK) {
        require(task.issueNumber > 0)
        require(task.title.isNotBlank() && task.title.length <= 200)
        require(task.priority in setOf("P0", "P1", "P2", "P3"))
        require(task.sourceId.isNotBlank() && task.sourceId.length <= 160)
        val current = readAll().toMutableList()
        if (!VyomFactoryTaskQueuePolicy.isUnique(current, task)) return@synchronized false
        current += task
        persist(current)
        true
    }

    
    /** Atomically restores eligible shared-registry tasks after local queue loss. */
    fun reconcileOpen(tasks: List<QueuedTask>): Int = synchronized(PROCESS_LOCK) {
        val current = readAll().toMutableList()
        val additions = mutableListOf<QueuedTask>()
        var metadataChanged = false
        tasks.forEach { task ->
            require(task.issueNumber > 0)
            require(task.title.isNotBlank() && task.title.length <= 200)
            require(task.priority in setOf("P0", "P1", "P2", "P3"))
            require(task.sourceId.isNotBlank() && task.sourceId.length <= 160)
            val existingIndex = current.indexOfFirst {
                it.issueNumber == task.issueNumber && it.sourceId == task.sourceId
            }
            if (existingIndex >= 0) {
                val existing = current[existingIndex]
                if (existing.lane != task.lane ||
                    existing.lanePlanningAllowed != task.lanePlanningAllowed
                ) {
                    current[existingIndex] = existing.copy(
                        lane = task.lane,
                        lanePlanningAllowed = task.lanePlanningAllowed
                    )
                    metadataChanged = true
                }
            } else if (VyomFactoryTaskQueuePolicy.isUnique(current + additions, task)) {
                additions += task
            }
        }
        if (metadataChanged || additions.isNotEmpty()) persist(current + additions)
        additions.size
    }

    fun pending(): List<QueuedTask> = synchronized(PROCESS_LOCK) { VyomFactoryTaskQueuePolicy.order(readAll()) }

    
    fun remove(issueNumber: Int): Boolean = synchronized(PROCESS_LOCK) {
        val current = readAll()
        val remaining = current.filterNot { it.issueNumber == issueNumber }
        if (remaining.size == current.size) return@synchronized false
        persist(remaining)
        return true
    }

    
    fun contains(issueNumber: Int): Boolean = synchronized(PROCESS_LOCK) { readAll().any { it.issueNumber == issueNumber } }

    private fun readAll(): List<QueuedTask> {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index)
                        ?: throw IllegalStateException("Factory queue entry $index is invalid")
                    val issue = item.optInt("issue_number", -1)
                    val title = item.optString("title")
                    val priority = item.optString("priority")
                    val sourceId = item.optString("source_id")
                    val enqueuedAt = item.optLong("enqueued_at", -1)
                    require(issue > 0 && title.isNotBlank() && priority in setOf("P0", "P1", "P2", "P3") &&
                        sourceId.isNotBlank() && enqueuedAt >= 0) { "Factory queue entry $index failed validation" }
                    val lane = if (item.has("lane")) VyomFactoryLaneRouter.Lane.valueOf(item.getString("lane"))
                        else VyomFactoryLaneRouter.Lane.CUSTOM
                    val lanePlanningAllowed = item.optBoolean("lane_planning_allowed", false)
                    add(QueuedTask(issue, title, priority, sourceId, enqueuedAt, lane, lanePlanningAllowed))
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("Factory queue data is unreadable; refusing to overwrite it", error)
        }
    }

    private fun persist(tasks: List<QueuedTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject()
                .put("issue_number", task.issueNumber)
                .put("title", task.title)
                .put("priority", task.priority)
                .put("source_id", task.sourceId)
                .put("enqueued_at", task.enqueuedAt)
                .put("lane", task.lane.name)
                .put("lane_planning_allowed", task.lanePlanningAllowed))
        }
        check(prefs.edit().putString(KEY_QUEUE, array.toString()).commit()) {
            "Factory queue could not be persisted"
        }
    }

    companion object {
        private const val PREFS = "vyom_factory_execution"
        private const val KEY_QUEUE = "pending_tasks_v1"
        private val PROCESS_LOCK = Any()
    }
}
