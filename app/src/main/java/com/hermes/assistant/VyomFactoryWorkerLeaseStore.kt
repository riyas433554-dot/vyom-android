package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal object VyomFactoryExecutionLock {
    val LOCK = Any()
}

/** Device-local fenced leases; this is not a distributed/cross-device lock. */
internal class VyomFactoryWorkerLeaseStore private constructor(
    private val prefs: SharedPreferences,
    private val idFactory: () -> String
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        { UUID.randomUUID().toString() }
    )
    internal constructor(prefs: SharedPreferences) : this(prefs, { UUID.randomUUID().toString() })

    data class Lease(
        val issueNumber: Int,
        val workerId: String,
        val lane: VyomFactoryLaneRouter.Lane?,
        val workerSlot: Int?,
        val leaseId: String,
        val acquiredAt: Long,
        val expiresAt: Long
    )

    fun claim(issueNumber: Int, worker: VyomFactoryWorkerIdentity, now: Long, durationMs: Long): Lease? =
        synchronized(VyomFactoryExecutionLock.LOCK) {
            validate(issueNumber, worker, now, durationMs)
            val leases = readAll().toMutableMap()
            val existing = leases[issueNumber]
            if (existing != null && existing.expiresAt > now) {
                return@synchronized if (sameWorker(existing, worker)) existing else null
            }
            if (leases.values.any {
                    it.expiresAt > now && (it.workerId == worker.workerId ||
                        (it.lane == worker.lane && it.workerSlot == worker.slotIndex))
                }) return@synchronized null
            val lease = Lease(
                issueNumber, worker.workerId, worker.lane, worker.slotIndex,
                idFactory(), now, Math.addExact(now, durationMs)
            )
            require(lease.leaseId.isNotBlank() && lease.leaseId.length <= 160)
            leases[issueNumber] = lease
            persist(leases.values)
            lease
        }

    fun renew(lease: Lease, now: Long, durationMs: Long): Lease? = synchronized(VyomFactoryExecutionLock.LOCK) {
        validate(lease, now, durationMs)
        val leases = readAll().toMutableMap()
        val current = leases[lease.issueNumber]
        if (current == null || !sameLease(current, lease) || current.expiresAt <= now) return@synchronized null
        val renewed = current.copy(expiresAt = Math.addExact(now, durationMs))
        leases[lease.issueNumber] = renewed
        persist(leases.values)
        renewed
    }

    fun release(lease: Lease): Boolean = synchronized(VyomFactoryExecutionLock.LOCK) {
        val leases = readAll().toMutableMap()
        val current = leases[lease.issueNumber]
        if (current == null || !sameLease(current, lease)) return@synchronized false
        leases.remove(lease.issueNumber)
        persist(leases.values)
        true
    }

    fun active(issueNumber: Int, now: Long): Lease? = synchronized(VyomFactoryExecutionLock.LOCK) {
        require(issueNumber > 0 && now >= 0)
        readAll()[issueNumber]?.takeIf { it.expiresAt > now }
    }

    fun activeIssueNumbers(now: Long): Set<Int> = synchronized(VyomFactoryExecutionLock.LOCK) {
        require(now >= 0)
        readAll().values.filter { it.expiresAt > now }.map { it.issueNumber }.toSet()
    }

    private fun validate(issueNumber: Int, worker: VyomFactoryWorkerIdentity, now: Long, durationMs: Long) {
        require(issueNumber > 0)
        require(VyomFactoryWorkerIdentityStore.isValidWorkerId(worker.workerId))
        require(worker.slotIndex in 0 until VyomFactoryWorkerPoolConfig.MINIMUM_WORKERS_PER_LANE)
        require(now >= 0 && durationMs in MIN_LEASE_MS..MAX_LEASE_MS)
        Math.addExact(now, durationMs)
    }

    private fun validate(lease: Lease, now: Long, durationMs: Long) {
        require(lease.issueNumber > 0 && VyomFactoryWorkerIdentityStore.isValidWorkerId(lease.workerId))
        require((lease.lane == null) == (lease.workerSlot == null))
        if (lease.workerSlot != null) require(lease.workerSlot in 0 until VyomFactoryWorkerPoolConfig.MINIMUM_WORKERS_PER_LANE)
        require(now >= 0 && durationMs in MIN_LEASE_MS..MAX_LEASE_MS)
        Math.addExact(now, durationMs)
    }

    private fun sameWorker(lease: Lease, worker: VyomFactoryWorkerIdentity) =
        lease.workerId == worker.workerId && lease.lane == worker.lane && lease.workerSlot == worker.slotIndex

    private fun sameLease(a: Lease, b: Lease) =
        a.issueNumber == b.issueNumber && a.workerId == b.workerId && a.lane == b.lane &&
            a.workerSlot == b.workerSlot && a.leaseId == b.leaseId

    private fun readAll(): Map<Int, Lease> {
        val raw = prefs.getString(KEY_LEASES, null) ?: return emptyMap()
        return try {
            val array = JSONArray(raw)
            buildMap {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: error("Invalid worker lease at index $i")
                    val workerId = item.optString("worker_id").ifBlank { item.optString("owner_id") }
                    val lane = item.optString("lane").takeIf { it.isNotBlank() }?.let { VyomFactoryLaneRouter.Lane.valueOf(it) }
                    val slot = if (item.has("worker_slot") && !item.isNull("worker_slot")) item.getInt("worker_slot") else null
                    val value = Lease(item.getInt("issue_number"), workerId, lane, slot,
                        item.getString("lease_id"), item.getLong("acquired_at"), item.getLong("expires_at"))
                    require(value.issueNumber > 0 && VyomFactoryWorkerIdentityStore.isValidWorkerId(value.workerId) &&
                        value.leaseId.isNotBlank() && value.acquiredAt >= 0 && value.expiresAt > value.acquiredAt &&
                        ((value.lane == null) == (value.workerSlot == null)) &&
                        (value.workerSlot == null || value.workerSlot in 0 until VyomFactoryWorkerPoolConfig.MINIMUM_WORKERS_PER_LANE))
                    require(put(value.issueNumber, value) == null) { "Duplicate worker lease issue" }
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("Worker lease data is unreadable; refusing to overwrite it", error)
        }
    }

    private fun persist(leases: Collection<Lease>) {
        val array = JSONArray()
        leases.sortedBy { it.issueNumber }.forEach { value ->
            array.put(JSONObject()
                .put("issue_number", value.issueNumber)
                .put("worker_id", value.workerId)
                .put("lane", value.lane?.name ?: JSONObject.NULL)
                .put("worker_slot", value.workerSlot ?: JSONObject.NULL)
                .put("lease_id", value.leaseId)
                .put("acquired_at", value.acquiredAt)
                .put("expires_at", value.expiresAt))
        }
        check(prefs.edit().putString(KEY_LEASES, array.toString()).commit()) { "Worker lease could not be persisted" }
    }

    companion object {
        private const val PREFS = "vyom_factory_worker_leases"
        private const val KEY_LEASES = "leases_v1"
        const val MIN_LEASE_MS = 5_000L
        const val MAX_LEASE_MS = 60 * 60 * 1000L
    }
}
