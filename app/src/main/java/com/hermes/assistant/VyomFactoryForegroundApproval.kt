package com.hermes.assistant

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Activity-owned gate; execution is permitted only while the host Activity is resumed. */
internal class VyomFactoryForegroundGate {
    private val resumed = AtomicBoolean(false)
    private val foregroundEpoch = java.util.concurrent.atomic.AtomicLong(0)
    fun onResumed() { resumed.set(true) }
    fun onPaused() { resumed.set(false); foregroundEpoch.incrementAndGet() }
    fun isForeground(): Boolean = resumed.get()
    fun generation(): Long = foregroundEpoch.get()
}

/**
 * Process-memory, single-use approvals. These are deliberately not persisted:
 * process death, expiry, or a new app session invalidates every pending approval.
 */
internal class VyomFactoryOneTimeApprovalStore(
    private val now: () -> Long = System::currentTimeMillis,
    private val ttlMillis: Long = 120_000L,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val approvals = LinkedHashMap<String, Long>()

    init { require(ttlMillis in 1_000L..300_000L) }

    @Synchronized
    fun issue(): String {
        prune()
        while (approvals.size >= MAX_PENDING) {
            val oldest = approvals.keys.firstOrNull() ?: break
            approvals.remove(oldest)
        }
        return newId().also { id ->
            require(id.isNotBlank() && id.length <= 128)
            check(!approvals.containsKey(id)) { "Approval ID collision" }
            val currentTime = now()
            approvals[id] = if (Long.MAX_VALUE - currentTime < ttlMillis) Long.MAX_VALUE else currentTime + ttlMillis
        }
    }

    @Synchronized
    fun consume(id: String): Boolean {
        if (id.isBlank()) return false
        prune()
        val expiry = approvals.remove(id) ?: return false
        return expiry > now()
    }

    @Synchronized
    fun clear() { approvals.clear() }

    @Synchronized
    private fun prune() {
        val time = now()
        val iterator = approvals.entries.iterator()
        while (iterator.hasNext()) if (iterator.next().value <= time) iterator.remove()
    }

    companion object { private const val MAX_PENDING = 64 }
}
