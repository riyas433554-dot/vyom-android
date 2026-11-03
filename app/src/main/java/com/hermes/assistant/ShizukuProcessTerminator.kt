package com.hermes.assistant

/** Bounded child-process termination shared by request STOP and service teardown. */
internal object ShizukuProcessTerminator {
    fun terminate(isAlive: () -> Boolean, destroy: () -> Unit, waitForExit: () -> Boolean, destroyForcibly: () -> Unit): Boolean {
        if (!isAlive()) return true
        try { destroy() } catch (_: Exception) { }
        val graceful = try { waitForExit() } catch (_: Exception) { false }
        if (graceful || !isAlive()) return true
        try { destroyForcibly() } catch (_: Exception) { }
        return try { waitForExit() } catch (_: Exception) { false }
    }
    fun <T : Any> terminateAll(requestIds: Collection<String>, processFor: (String) -> T?, terminate: (T) -> Boolean): Set<String> {
        val stopped = linkedSetOf<String>()
        requestIds.forEach { id ->
            val process = try { processFor(id) } catch (_: Exception) { null }
            if (process != null) try { if (terminate(process)) stopped += id } catch (_: Exception) { }
        }
        return stopped
    }
}
