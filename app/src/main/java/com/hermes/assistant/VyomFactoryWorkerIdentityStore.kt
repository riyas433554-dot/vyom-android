package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class VyomFactoryWorkerIdentity(
    val workerId: String,
    val lane: VyomFactoryLaneRouter.Lane,
    val slotIndex: Int
) {
    init {
        require(VyomFactoryWorkerIdentityStore.isValidWorkerId(workerId)) { "Invalid worker identity" }
        require(slotIndex in 0 until VyomFactoryWorkerPoolConfig.MINIMUM_WORKERS_PER_LANE)
    }
}

internal object VyomFactoryWorkerPoolConfig {
    const val MINIMUM_WORKERS_PER_LANE = 5
    val totalWorkerCount: Int
        get() = VyomFactoryLaneRouter.Lane.values().size * MINIMUM_WORKERS_PER_LANE
    fun concurrencyLimit(workers: Collection<VyomFactoryWorkerIdentity>): Int {
        require(workers.map { it.workerId }.toSet().size == workers.size) {
            "Worker pool contains duplicate identities"
        }
        VyomFactoryLaneRouter.Lane.values().forEach { lane ->
            require(workers.count { it.lane == lane } >= MINIMUM_WORKERS_PER_LANE) {
                "Worker pool is below the minimum for lane $lane"
            }
        }
        return workers.size
    }
}

internal class VyomFactoryWorkerIdentityStore internal constructor(
    private val prefs: SharedPreferences,
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        { UUID.randomUUID().toString() }
    )

    fun ensurePool(): List<VyomFactoryWorkerIdentity> = synchronized(LOCK) {
        val current = readAll()
        val existingSlots = current.map { it.lane to it.slotIndex }.toSet()
        val usedIds = current.map { it.workerId }.toMutableSet()
        val result = current.toMutableList()
        var changed = false
        VyomFactoryLaneRouter.Lane.values().forEach { lane ->
            for (slot in 0 until VyomFactoryWorkerPoolConfig.MINIMUM_WORKERS_PER_LANE) {
                if (existingSlots.contains(lane to slot)) continue
                val id = idFactory()
                check(isValidWorkerId(id)) { "Worker identity generator returned an invalid ID" }
                check(usedIds.add(id)) { "Worker identity generator returned a duplicate ID" }
                result += VyomFactoryWorkerIdentity(id, lane, slot)
                changed = true
            }
        }
        val ordered = result.sortedWith(compareBy<VyomFactoryWorkerIdentity> { it.lane.priorityRank }.thenBy { it.slotIndex })
        check(ordered.size == VyomFactoryWorkerPoolConfig.totalWorkerCount)
        check(ordered.map { it.workerId }.toSet().size == ordered.size)
        if (changed) persist(ordered)
        ordered
    }

    private fun readAll(): List<VyomFactoryWorkerIdentity> {
        val raw = prefs.getString(KEY_WORKERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            require(array.length() <= VyomFactoryWorkerPoolConfig.totalWorkerCount)
            val values = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                VyomFactoryWorkerIdentity(
                    item.getString("worker_id"),
                    VyomFactoryLaneRouter.Lane.valueOf(item.getString("lane")),
                    item.getInt("worker_slot")
                )
            }
            require(values.map { it.workerId }.toSet().size == values.size) { "Duplicate worker ID" }
            require(values.map { it.lane to it.slotIndex }.toSet().size == values.size) { "Duplicate lane slot" }
            values
        } catch (error: Exception) {
            throw IllegalStateException("Worker identity registry is unreadable; refusing to overwrite it", error)
        }
    }

    private fun persist(values: List<VyomFactoryWorkerIdentity>) {
        val array = JSONArray()
        values.forEach { value ->
            array.put(JSONObject().put("worker_id", value.workerId).put("lane", value.lane.name)
                .put("worker_slot", value.slotIndex))
        }
        check(prefs.edit().putString(KEY_WORKERS, array.toString()).commit()) {
            "Worker identity registry could not be persisted"
        }
    }

    companion object {
        private const val PREFS = "vyom_factory_worker_identities"
        private const val KEY_WORKERS = "workers_v1"
        private val LOCK = Any()
        private val WORKER_ID = Regex("^[A-Za-z0-9][A-Za-z0-9_-]{7,79}$")
        internal fun isValidWorkerId(value: String): Boolean = WORKER_ID.matches(value)
    }
}
