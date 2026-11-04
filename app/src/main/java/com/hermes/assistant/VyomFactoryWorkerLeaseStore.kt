package com.hermes.assistant

internal object VyomFactoryExecutionLock {
    /** Serializes lease and journal fencing within this single app process. */
    val LOCK = Any()
}

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Durable, device-local execution lease registry.
 *
 * This coordinates workers inside one Vyom app process/device only. SharedPreferences
 * plus a process lock is NOT a distributed compare-and-swap and must never be treated
 * as a cross-device GitHub lease.
 */
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
        val ownerId: String,
        val leaseId: String,
        val acquiredAt: Long,
        val expiresAt: Long
    )

    /** Claims an unleased/expired task, or returns the caller's still-live lease. */
    fun claim(issueNumber: Int, ownerId: String, now: Long, durationMs: Long): Lease? =
        synchronized(VyomFactoryExecutionLock.LOCK) {
            validate(issueNumber, ownerId, now, durationMs)
            val leases = readAll().toMutableMap()
            val existing = leases[issueNumber]
            if (existing != null && existing.expiresAt > now) {
                return@synchronized if (existing.ownerId == ownerId) existing else null
            }
            val lease = Lease(issueNumber, ownerId, idFactory(), now, Math.addExact(now, durationMs))
            require(lease.leaseId.isNotBlank() && lease.leaseId.length <= 160)
            leases[issueNumber] = lease
            persist(leases.values)
            lease
        }

    /** Extends only the exact, still-live lease; stale owners cannot revive expired work. */
    fun renew(lease: Lease, now: Long, durationMs: Long): Lease? = synchronized(PROCESS_LOCK) {
        validate(lease.issueNumber, lease.ownerId, now, durationMs)
        val leases = readAll().toMutableMap()
        val current = leases[lease.issueNumber]
        if (current == null || current.leaseId != lease.leaseId || current.ownerId != lease.ownerId || current.expiresAt <= now) {
            return@synchronized null
        }
        val renewed = current.copy(expiresAt = Math.addExact(now, durationMs))
        leases[lease.issueNumber] = renewed
        persist(leases.values)
        renewed
    }

    /** Releases only the exact lease token, so an old worker cannot release a successor's claim. */
    fun release(lease: Lease): Boolean = synchronized(PROCESS_LOCK) {
        val leases = readAll().toMutableMap()
        val current = leases[lease.issueNumber]
        if (current == null || current.leaseId != lease.leaseId || current.ownerId != lease.ownerId) return@synchronized false
        leases.remove(lease.issueNumber)
        persist(leases.values)
        true
    }

    /** Returns only an unexpired lease at the supplied clock time. */
    fun active(issueNumber: Int, now: Long): Lease? = synchronized(PROCESS_LOCK) {
        require(issueNumber > 0 && now >= 0)
        readAll()[issueNumber]?.takeIf { it.expiresAt > now }
    }

    private fun validate(issueNumber: Int, ownerId: String, now: Long, durationMs: Long) {
        require(issueNumber > 0)
        require(ownerId.isNotBlank() && ownerId.length <= 160 && ownerId.none { it.isISOControl() })
        require(now >= 0)
        require(durationMs in MIN_LEASE_MS..MAX_LEASE_MS)
        Math.addExact(now, durationMs)
    }

    private fun readAll(): Map<Int, Lease> {
        val raw = prefs.getString(KEY_LEASES, null) ?: return emptyMap()
        return try {
            val array = JSONArray(raw)
            buildMap {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: error("Invalid worker lease at index $i")
                    val lease = Lease(
                        item.getInt("issue_number"), item.getString("owner_id"),
                        item.getString("lease_id"), item.getLong("acquired_at"), item.getLong("expires_at")
                    )
                    require(lease.issueNumber > 0 && lease.ownerId.isNotBlank() &&
                        lease.leaseId.isNotBlank() && lease.acquiredAt >= 0 && lease.expiresAt > lease.acquiredAt)
                    require(put(lease.issueNumber, lease) == null) { "Duplicate worker lease issue" }
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("Worker lease data is unreadable; refusing to overwrite it", error)
        }
    }

    private fun persist(leases: Collection<Lease>) {
        val array = JSONArray()
        leases.sortedBy { it.issueNumber }.forEach { lease ->
            array.put(JSONObject()
                .put("issue_number", lease.issueNumber)
                .put("owner_id", lease.ownerId)
                .put("lease_id", lease.leaseId)
                .put("acquired_at", lease.acquiredAt)
                .put("expires_at", lease.expiresAt))
        }
        check(prefs.edit().putString(KEY_LEASES, array.toString()).commit()) {
            "Worker lease could not be persisted"
        }
    }

    companion object {
        private const val PREFS = "vyom_factory_worker_leases"
        private const val KEY_LEASES = "leases_v1"
        const val MIN_LEASE_MS = 5_000L
        const val MAX_LEASE_MS = 60 * 60 * 1000L
    }
}
