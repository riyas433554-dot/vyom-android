package com.hermes.assistant

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-wide execution gate. The current Android adapter changes one shared
 * device volume stream, so concurrent side effects are deliberately disabled.
 */
internal class VyomFactoryExecutionPermit {
    private val occupied = AtomicBoolean(false)

    fun tryAcquire(): Boolean = occupied.compareAndSet(false, true)

    fun release() {
        check(occupied.compareAndSet(true, false)) { "Execution permit is not owned" }
    }

    companion object {
        val PROCESS = VyomFactoryExecutionPermit()
    }
}
