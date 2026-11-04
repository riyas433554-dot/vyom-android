package com.hermes.assistant

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Activity-owned gate; execution is permitted only while the host Activity is resumed. */
internal class VyomFactoryForegroundGate {
    private val resumed = AtomicBoolean(false)
    fun onResumed() { resumed.set(true) }
    fun onPaused() { resumed.set(false) }
    fun isForeground(): Boolean = resumed.get()
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
            approvals[id] = now() + ttlMillis
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
    private fun prune() {
        val time = now()
        val iterator = approvals.entries.iterator()
        while (iterator.hasNext()) if (iterator.next().value <= time) iterator.remove()
    }

    companion object { private const val MAX_PENDING = 64 }
}
