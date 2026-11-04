package com.hermes.assistant

import java.util.concurrent.TimeUnit

/**
 * Atomic request lifecycle shared by execute(), STOP and service teardown.
 * Terminal entries are retained in a bounded insertion-ordered window to reject replay.
 */
internal class ShizukuRequestRegistry(private val maxEntries: Int = 256) {
    enum class State { CANCELLED_BEFORE_START, STARTING, RUNNING, CANCEL_REQUESTED, COMPLETED }
    enum class Begin { ACCEPTED, CANCELLED_BEFORE_START, DUPLICATE, CAPACITY_REACHED, SERVICE_STOPPED }

    private val states = LinkedHashMap<String, State>()
    private var closed = false

    @Synchronized fun begin(id: String): Begin {
        if (closed) return Begin.SERVICE_STOPPED
        when (states[id]) {
            State.CANCELLED_BEFORE_START -> { states[id] = State.COMPLETED; return Begin.CANCELLED_BEFORE_START }
            State.STARTING, State.RUNNING, State.CANCEL_REQUESTED, State.COMPLETED -> return Begin.DUPLICATE
            null -> Unit
        }
        trimForInsert()
        if (states.size >= maxEntries) return Begin.CAPACITY_REACHED
        states[id] = State.STARTING
        return Begin.ACCEPTED
    }

    /**
     * Atomically closes admission and marks every in-flight request for cancellation.
     * The returned IDs are the work that teardown must terminate at the process boundary.
     */
    @Synchronized fun shutdown(): Set<String> {
        if (closed) return emptySet()
        closed = true
        val active = states.entries
            .filter { it.value == State.STARTING || it.value == State.RUNNING }
            .map { it.key }
            .toSet()
        active.forEach { states[it] = State.CANCEL_REQUESTED }
        return active
    }

    @Synchronized fun markRunning(id: String): Boolean {
        return when (states[id]) {
            State.STARTING -> { states[id] = State.RUNNING; true }
            State.CANCEL_REQUESTED -> false
            else -> false
        }
    }

    @Synchronized fun cancel(id: String): Boolean {
        return when (states[id]) {
            State.COMPLETED -> false
            State.CANCEL_REQUESTED, State.CANCELLED_BEFORE_START -> true
            State.STARTING, State.RUNNING -> { states[id] = State.CANCEL_REQUESTED; true }
            null -> {
                if (closed) return false
                trimForInsert()
                if (states.size >= maxEntries) false
                else { states[id] = State.CANCELLED_BEFORE_START; true }
            }
        }
    }

    @Synchronized fun isCancelled(id: String): Boolean = states[id] == State.CANCEL_REQUESTED

    @Synchronized fun complete(id: String) {
        states[id] = State.COMPLETED
        trim()
        (this as java.lang.Object).notifyAll()
    }

    /** Wait for in-flight execute() calls to observe STOP and finish child cleanup. */
    @Synchronized fun awaitQuiescence(timeoutMs: Long): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs.coerceAtLeast(0L))
        while (states.values.any { it == State.STARTING || it == State.RUNNING || it == State.CANCEL_REQUESTED }) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) return false
            val millis = TimeUnit.NANOSECONDS.toMillis(remaining).coerceAtLeast(1L)
            (this as java.lang.Object).wait(millis)
        }
        return true
    }

    @Synchronized fun state(id: String): State? = states[id]

    /** Evict the oldest completed replay record only when a new slot is needed. */
    private fun trimForInsert() {
        while (states.size >= maxEntries) {
            val removable = states.entries.firstOrNull { it.value == State.COMPLETED }?.key ?: break
            states.remove(removable)
        }
    }

    private fun trim() {
        while (states.size > maxEntries) {
            val removable = states.entries.firstOrNull { it.value == State.COMPLETED }?.key ?: break
            states.remove(removable)
        }
    }
}
