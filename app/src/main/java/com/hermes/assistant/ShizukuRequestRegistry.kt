package com.hermes.assistant

/**
 * Atomic request lifecycle shared by execute(), STOP and service teardown.
 * Terminal entries are retained in a bounded insertion-ordered window to reject replay.
 */
internal class ShizukuRequestRegistry(private val maxEntries: Int = 256) {
    enum class State { STARTING, RUNNING, CANCEL_REQUESTED, COMPLETED }
    enum class Begin { ACCEPTED, CANCELLED_BEFORE_START, DUPLICATE }

    private val states = LinkedHashMap<String, State>()

    @Synchronized fun begin(id: String): Begin {
        when (states[id]) {
            State.STARTING, State.RUNNING, State.CANCEL_REQUESTED, State.COMPLETED -> return Begin.DUPLICATE
            null -> Unit
        }
        states[id] = State.STARTING
        trim()
        return Begin.ACCEPTED
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
            State.CANCEL_REQUESTED -> true
            State.STARTING, State.RUNNING -> { states[id] = State.CANCEL_REQUESTED; true }
            null -> { states[id] = State.CANCEL_REQUESTED; trim(); true }
        }
    }

    @Synchronized fun isCancelled(id: String): Boolean = states[id] == State.CANCEL_REQUESTED

    @Synchronized fun complete(id: String) {
        states[id] = State.COMPLETED
        trim()
    }

    @Synchronized fun state(id: String): State? = states[id]

    private fun trim() {
        while (states.size > maxEntries) {
            val removable = states.entries.firstOrNull { it.value == State.COMPLETED }?.key ?: break
            states.remove(removable)
        }
    }
}
