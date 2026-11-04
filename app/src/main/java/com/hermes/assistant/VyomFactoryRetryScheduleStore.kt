package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable, device-local retry schedule. This stores eligibility only; it never
 * starts work. A consumer must re-fetch the GitHub task, acquire a fresh lease,
 * and revalidate STOP/permissions before invoking an idempotent adapter.
 */
internal class VyomFactoryRetryScheduleStore internal constructor(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    )

    data class Schedule(
        val issueNumber: Int,
        val operationKey: String,
        val failedAttempts: Int,
        val errorCategory: String,
        val scheduledAt: Long,
        val nextAttemptAt: Long
    )

    fun recordFailure(
        issueNumber: Int,
        operationKey: String,
        errorCategory: String,
        now: Long,
        adapterIdempotent: Boolean
    ): Schedule? = synchronized(LOCK) {
        validateIdentity(issueNumber, operationKey, now)
        val schedules = readAll().toMutableMap()
        val key = key(issueNumber, operationKey)
        val previous = schedules[key]
        val failedAttempts = (previous?.failedAttempts ?: 0) + 1
        val decision = VyomFactoryRetryPolicy.afterFailure(
            completedAttempts = failedAttempts,
            errorCategory = errorCategory,
            now = now,
            adapterIdempotent = adapterIdempotent
        )
        if (!decision.retry) {
            if (previous != null) {
                schedules.remove(key)
                persist(schedules.values)
            }
            return@synchronized null
        }

        val baseTime = requireNotNull(decision.nextAttemptAt)
        val baseDelay = (baseTime - now).coerceAtLeast(0)
        val jitterWindow = baseDelay / 4
        // Include issue identity: operation-only jitter synchronizes every task
        // using the same adapter and can create a retry thundering herd.
        val stableHash = Integer.toUnsignedLong(31 * issueNumber + operationKey.hashCode())
        val jitter = if (jitterWindow == 0L) 0L else stableHash % (jitterWindow + 1)
        val nextAttemptAt = if (Long.MAX_VALUE - baseTime < jitter) Long.MAX_VALUE else baseTime + jitter
        val schedule = Schedule(
            issueNumber = issueNumber,
            operationKey = operationKey,
            failedAttempts = failedAttempts,
            errorCategory = errorCategory,
            scheduledAt = now,
            nextAttemptAt = nextAttemptAt
        )
        schedules[key] = schedule
        check(schedules.size <= MAX_SCHEDULES) {
            "Retry schedule is full; refusing to discard pending retry records"
        }
        persist(schedules.values)
        schedule
    }

    /** Returns eligible timestamps only; callers still need a fresh registry read and lease. */
    fun due(now: Long): List<Schedule> = synchronized(LOCK) {
        require(now >= 0)
        readAll().values
            .filter { it.nextAttemptAt <= now }
            .sortedWith(compareBy<Schedule> { it.nextAttemptAt }.thenBy { it.issueNumber })
    }

    fun all(): List<Schedule> = synchronized(LOCK) {
        readAll().values.sortedWith(compareBy<Schedule> { it.issueNumber }.thenBy { it.operationKey })
    }

    fun clear(issueNumber: Int, operationKey: String): Boolean = synchronized(LOCK) {
        require(issueNumber > 0)
        require(OPERATION_KEY.matches(operationKey))
        val schedules = readAll().toMutableMap()
        if (schedules.remove(key(issueNumber, operationKey)) == null) return@synchronized false
        persist(schedules.values)
        true
    }

    private fun validateIdentity(issueNumber: Int, operationKey: String, now: Long) {
        require(issueNumber > 0)
        require(OPERATION_KEY.matches(operationKey))
        require(now >= 0)
    }

    private fun key(issueNumber: Int, operationKey: String) = "$issueNumber:$operationKey"

    private fun readAll(): Map<String, Schedule> {
        val raw = prefs.getString(KEY_SCHEDULES, null) ?: return emptyMap()
        return try {
            val array = JSONArray(raw)
            require(array.length() <= MAX_SCHEDULES) { "Retry schedule exceeds its safe entry limit" }
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: error("Invalid retry schedule entry $index")
                    val schedule = Schedule(
                        issueNumber = item.getInt("issue_number"),
                        operationKey = item.getString("operation_key"),
                        failedAttempts = item.getInt("failed_attempts"),
                        errorCategory = item.getString("error_category"),
                        scheduledAt = item.getLong("scheduled_at"),
                        nextAttemptAt = item.getLong("next_attempt_at")
                    )
                    require(schedule.issueNumber > 0 && OPERATION_KEY.matches(schedule.operationKey) &&
                        schedule.failedAttempts in 1..2 && schedule.errorCategory.matches(ERROR_CATEGORY) &&
                        schedule.scheduledAt >= 0 && schedule.nextAttemptAt >= schedule.scheduledAt)
                    require(put(key(schedule.issueNumber, schedule.operationKey), schedule) == null) {
                        "Duplicate retry schedule identity"
                    }
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("Retry schedule is unreadable; refusing to overwrite it", error)
        }
    }

    private fun persist(schedules: Collection<Schedule>) {
        val array = JSONArray()
        schedules.sortedWith(compareBy<Schedule> { it.issueNumber }.thenBy { it.operationKey })
            .forEach { schedule ->
                array.put(JSONObject()
                    .put("issue_number", schedule.issueNumber)
                    .put("operation_key", schedule.operationKey)
                    .put("failed_attempts", schedule.failedAttempts)
                    .put("error_category", schedule.errorCategory)
                    .put("scheduled_at", schedule.scheduledAt)
                    .put("next_attempt_at", schedule.nextAttemptAt))
            }
        check(prefs.edit().putString(KEY_SCHEDULES, array.toString()).commit()) {
            "Retry schedule could not be persisted"
        }
    }

    companion object {
        private const val PREFS = "vyom_factory_retry_schedule"
        private const val KEY_SCHEDULES = "retry_schedules_v1"
        private const val MAX_SCHEDULES = 500
        private val LOCK = Any()
        private val OPERATION_KEY = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,159}")
        private val ERROR_CATEGORY = Regex("[a-z][a-z0-9_]{1,79}")
    }
}
