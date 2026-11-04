package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable, device-local execution attempt journal.
 *
 * The shared process lock makes lease validation and journal writes atomic relative
 * to this app process's lease operations. It is not a multi-process or cross-device
 * transaction. Expired attempts are marked interrupted and are never auto-replayed.
 */
internal class VyomFactoryExecutionJournal internal constructor(
    private val prefs: SharedPreferences,
    private val leases: VyomFactoryWorkerLeaseStore
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        VyomFactoryWorkerLeaseStore(context.applicationContext.getSharedPreferences(LEASE_PREFS, Context.MODE_PRIVATE))
    )

    enum class State {
        CLAIMED, RUNNING, STOP_REQUESTED, SUCCEEDED, FAILED, INTERRUPTED
    }

    data class Entry(
        val issueNumber: Int,
        val attemptId: String,
        val leaseId: String,
        val ownerId: String,
        val state: State,
        val startedAt: Long,
        val updatedAt: Long,
        val result: String? = null,
        val errorCategory: String? = null,
        val errorMessage: String? = null,
        val stopRequestedAt: Long? = null,
        val operationKey: String = "legacy:unknown"
    ) {
        val isTerminal: Boolean
            get() = state == State.SUCCEEDED || state == State.FAILED || state == State.INTERRUPTED
    }

    /** Creates a unique attempt only while the exact supplied lease is current. */
    fun begin(lease: VyomFactoryWorkerLeaseStore.Lease, attemptId: String, now: Long, operationKey: String = "legacy:unknown"): Entry =
        synchronized(VyomFactoryExecutionLock.LOCK) {
            validateAttemptId(attemptId)
            require(OPERATION_KEY.matches(operationKey))
            require(now >= 0)
            requireCurrentLease(lease, now)
            val entries = readAll().toMutableMap()
            require(attemptId !in entries) { "Execution attempt already exists" }
            val activeForIssue = entries.values.filter {
                it.issueNumber == lease.issueNumber && !it.isTerminal
            }
            check(activeForIssue.none {
                it.leaseId == lease.leaseId && it.ownerId == lease.ownerId
            }) { "An execution attempt is already active for this issue and lease" }
            activeForIssue.forEach { previous ->
                entries[previous.attemptId] = previous.copy(
                    state = State.INTERRUPTED,
                    updatedAt = now
                )
            }
            check(entries.size < MAX_JOURNAL_ENTRIES) {
                "Execution journal is full; refusing to discard audit history"
            }
            val entry = Entry(
                issueNumber = lease.issueNumber,
                attemptId = attemptId,
                leaseId = lease.leaseId,
                ownerId = lease.ownerId,
                state = State.CLAIMED,
                startedAt = now,
                updatedAt = now,
                operationKey = operationKey
            )
            entries[attemptId] = entry
            persist(entries.values)
            entry
        }

    fun get(attemptId: String): Entry? = synchronized(VyomFactoryExecutionLock.LOCK) {
        readAll()[attemptId]
    }

    fun all(): List<Entry> = synchronized(VyomFactoryExecutionLock.LOCK) {
        readAll().values.sortedWith(compareBy<Entry> { it.issueNumber }.thenBy { it.startedAt })
    }

    fun markRunning(
        lease: VyomFactoryWorkerLeaseStore.Lease,
        attemptId: String,
        now: Long
    ): Boolean = updateWithLease(lease, attemptId, now) { current ->
        when (current.state) {
            State.CLAIMED -> current.copy(state = State.RUNNING, updatedAt = now)
            State.RUNNING -> current
            else -> null
        }
    }

    fun succeed(
        lease: VyomFactoryWorkerLeaseStore.Lease,
        attemptId: String,
        now: Long,
        result: String
    ): Boolean {
        require(result.length <= MAX_RESULT_LENGTH)
        return updateWithLease(lease, attemptId, now) { current ->
            if (current.state != State.RUNNING) null
            else current.copy(state = State.SUCCEEDED, updatedAt = now, result = result)
        }
    }

    fun fail(
        lease: VyomFactoryWorkerLeaseStore.Lease,
        attemptId: String,
        now: Long,
        category: String,
        message: String
    ): Boolean {
        require(ERROR_CATEGORY.matches(category))
        require(message.isNotBlank() && message.length <= MAX_ERROR_LENGTH)
        return updateWithLease(lease, attemptId, now) { current ->
            if (current.state != State.RUNNING && current.state != State.STOP_REQUESTED) null
            else current.copy(
                state = State.FAILED,
                updatedAt = now,
                errorCategory = category,
                errorMessage = message
            )
        }
    }

    /** Requests cooperative stop; the entry remains nonterminal until an executor confirms it. */
    fun requestStop(
        lease: VyomFactoryWorkerLeaseStore.Lease,
        attemptId: String,
        now: Long
    ): Boolean = updateWithLease(lease, attemptId, now) { current ->
        when (current.state) {
            State.RUNNING -> current.copy(
                state = State.STOP_REQUESTED,
                updatedAt = now,
                stopRequestedAt = now
            )
            State.STOP_REQUESTED -> current
            else -> null
        }
    }

    /** Marks expired nonterminal attempts interrupted. This method never retries side effects. */
    fun interruptExpired(now: Long): Int = synchronized(VyomFactoryExecutionLock.LOCK) {
        require(now >= 0)
        val entries = readAll().toMutableMap()
        val expired = entries.values.filter {
            !it.isTerminal && leaseExpired(it, now)
        }
        if (expired.isEmpty()) return@synchronized 0
        expired.forEach { entry ->
            entries[entry.attemptId] = entry.copy(state = State.INTERRUPTED, updatedAt = now)
        }
        persist(entries.values)
        expired.size
    }

    /** Deliberately empty until adapter-level idempotency makes replay safe. */
    fun replayable(): List<Entry> = emptyList()

    private fun updateWithLease(
        lease: VyomFactoryWorkerLeaseStore.Lease,
        attemptId: String,
        now: Long,
        transform: (Entry) -> Entry?
    ): Boolean = synchronized(VyomFactoryExecutionLock.LOCK) {
        require(now >= 0)
        val active = leases.active(lease.issueNumber, now)
        if (active == null || active.leaseId != lease.leaseId || active.ownerId != lease.ownerId) {
            return@synchronized false
        }
        val entries = readAll().toMutableMap()
        val current = entries[attemptId] ?: return@synchronized false
        if (current.issueNumber != lease.issueNumber ||
            current.leaseId != lease.leaseId ||
            current.ownerId != lease.ownerId
        ) return@synchronized false
        val updated = transform(current) ?: return@synchronized false
        if (updated == current) return@synchronized true
        entries[attemptId] = updated
        persist(entries.values)
        true
    }

    private fun requireCurrentLease(lease: VyomFactoryWorkerLeaseStore.Lease, now: Long) {
        val active = leases.active(lease.issueNumber, now)
        check(active != null && active.leaseId == lease.leaseId && active.ownerId == lease.ownerId) {
            "Execution lease is no longer current"
        }
    }

    private fun leaseExpired(entry: Entry, now: Long): Boolean {
        val active = leases.active(entry.issueNumber, now)
        return active == null || active.leaseId != entry.leaseId || active.ownerId != entry.ownerId
    }

    private fun validateAttemptId(attemptId: String) {
        require(attemptId.length in 1..MAX_ATTEMPT_ID_LENGTH && ATTEMPT_ID.matches(attemptId))
    }

    private fun readAll(): Map<String, Entry> {
        val raw = prefs.getString(KEY_ATTEMPTS, null) ?: return emptyMap()
        return try {
            val array = JSONArray(raw)
            require(array.length() <= MAX_JOURNAL_ENTRIES) { "Execution journal exceeds its safe entry limit" }
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: error("Invalid execution journal entry $index")
                    val state = State.valueOf(item.getString("state"))
                    val entry = Entry(
                        issueNumber = item.getInt("issue_number"),
                        attemptId = item.getString("attempt_id"),
                        leaseId = item.getString("lease_id"),
                        ownerId = item.getString("owner_id"),
                        state = state,
                        startedAt = item.getLong("started_at"),
                        updatedAt = item.getLong("updated_at"),
                        result = item.optNullableString("result"),
                        errorCategory = item.optNullableString("error_category"),
                        errorMessage = item.optNullableString("error_message"),
                        stopRequestedAt = if (item.isNull("stop_requested_at")) null else item.getLong("stop_requested_at"),
                        operationKey = item.optString("operation_key", "legacy:unknown")
                    )
                    require(entry.issueNumber > 0 && ATTEMPT_ID.matches(entry.attemptId) &&
                        entry.leaseId.isNotBlank() && entry.ownerId.isNotBlank() &&
                        entry.startedAt >= 0 && entry.updatedAt >= entry.startedAt &&
                        OPERATION_KEY.matches(entry.operationKey))
                    require(put(entry.attemptId, entry) == null) { "Duplicate execution attempt ID" }
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("Execution journal is unreadable; refusing to overwrite it", error)
        }
    }

    private fun persist(entries: Collection<Entry>) {
        val array = JSONArray()
        entries.sortedBy { it.attemptId }.forEach { entry ->
            array.put(JSONObject()
                .put("issue_number", entry.issueNumber)
                .put("attempt_id", entry.attemptId)
                .put("lease_id", entry.leaseId)
                .put("owner_id", entry.ownerId)
                .put("state", entry.state.name)
                .put("started_at", entry.startedAt)
                .put("updated_at", entry.updatedAt)
                .put("result", entry.result)
                .put("error_category", entry.errorCategory)
                .put("error_message", entry.errorMessage)
                .put("stop_requested_at", entry.stopRequestedAt)
                .put("operation_key", entry.operationKey))
        }
        check(prefs.edit().putString(KEY_ATTEMPTS, array.toString()).commit()) {
            "Execution journal could not be persisted"
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else getString(key)

    companion object {
        private const val PREFS = "vyom_factory_execution_journal"
        private const val LEASE_PREFS = "vyom_factory_worker_leases"
        private const val KEY_ATTEMPTS = "attempts_v1"
        private const val MAX_ATTEMPT_ID_LENGTH = 160
        private const val MAX_RESULT_LENGTH = 8_000
        private const val MAX_ERROR_LENGTH = 1_000
        private const val MAX_JOURNAL_ENTRIES = 500
        private val ATTEMPT_ID = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,159}")
        private val ERROR_CATEGORY = Regex("[a-z][a-z0-9_]{1,79}")
        private val OPERATION_KEY = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,159}")
    }
}
