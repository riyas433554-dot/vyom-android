package com.hermes.assistant

/**
 * Bounded, best-effort child process termination shared by STOP and service teardown.
 * A graceful destroy is always attempted first; force-kill is followed by a second
 * bounded wait so callers can distinguish a completed termination from a live child.
 */
internal object ShizukuProcessTerminator {
    fun terminate(
        isAlive: () -> Boolean,
        destroy: () -> Unit,
        waitForExit: () -> Boolean,
        destroyForcibly: () -> Unit
    ): Boolean {
        if (!isAlive()) return true
        try { destroy() } catch (_: Exception) { }
        val gracefullyExited = try { waitForExit() } catch (_: Exception) { false }
        if (gracefullyExited || !isAlive()) return true
        try { destroyForcibly() } catch (_: Exception) { }
        val forceExited = try { waitForExit() } catch (_: Exception) { false }
        return forceExited || !isAlive()
    }

    fun <T : Any> terminateAll(
        requestIds: Collection<String>,
        processFor: (String) -> T?,
        terminate: (T) -> Boolean
    ): Set<String> {
        val stopped = linkedSetOf<String>()
        requestIds.forEach { requestId ->
            val process = try { processFor(requestId) } catch (_: Exception) { null }
            if (process != null) {
                try {
                    if (terminate(process)) stopped += requestId
                } catch (_: Exception) {
                    // A failed process handle must not prevent later children being stopped.
                }
            }
        }
        return stopped
    }
}
