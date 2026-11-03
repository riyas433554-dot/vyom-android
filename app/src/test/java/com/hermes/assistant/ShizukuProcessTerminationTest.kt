package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class ShizukuProcessTerminationTest {
    @Test fun gracefulStopWaitsForProcessExitWithoutForceKill() {
        var alive = true
        var forced = false
        val stopped = ShizukuProcessTerminator.terminate(
            isAlive = { alive },
            destroy = { alive = false },
            waitForExit = { alive = false; true },
            destroyForcibly = { forced = true; alive = false }
        )
        assertTrue(stopped)
        assertFalse(forced)
        assertFalse(alive)
    }

    @Test fun unresponsiveProcessIsForceKilledAndWaitedFor() {
        var alive = true
        var forceCalls = 0
        var waits = 0
        val stopped = ShizukuProcessTerminator.terminate(
            isAlive = { alive },
            destroy = { },
            waitForExit = { waits++; if (waits == 2) alive = false; !alive },
            destroyForcibly = { forceCalls++; alive = false }
        )
        assertTrue(stopped)
        assertEquals(1, forceCalls)
        assertEquals(2, waits)
        assertFalse(alive)
    }

    @Test fun alreadyExitedProcessNeedsNoTerminationCalls() {
        var calls = 0
        val stopped = ShizukuProcessTerminator.terminate(
            isAlive = { false },
            destroy = { calls++ },
            waitForExit = { calls++; true },
            destroyForcibly = { calls++ }
        )
        assertTrue(stopped)
        assertEquals(0, calls)
    }

    @Test fun teardownAttemptsEveryProcessEvenWhenOneTerminationThrows() {
        val first = Any()
        val second = Any()
        val attempted = mutableListOf<Any>()
        val stopped = ShizukuProcessTerminator.terminateAll(
            requestIds = setOf("req-1", "req-2"),
            processFor = { if (it == "req-1") first else second }
        ) {
            attempted += it
            if (it === first) error("process handle failed")
            true
        }
        assertEquals(2, attempted.size)
        assertTrue(attempted.containsAll(listOf(first, second)))
        assertEquals(setOf("req-2"), stopped)
    }

    @Test fun teardownSnapshotAndLateStartupCannotLeaveAnUntrackedProcessRunning() {
        val registry = ShizukuRequestRegistry()
        assertEquals(ShizukuRequestRegistry.Begin.ACCEPTED, registry.begin("starting"))
        val stoppedIds = registry.shutdown()
        assertEquals(setOf("starting"), stoppedIds)

        // No Process existed at the teardown snapshot. Once startup returns,
        // the closed registry refuses RUNNING and the caller must kill that child.
        var childAlive = true
        assertFalse(registry.markRunning("starting"))
        val stopped = ShizukuProcessTerminator.terminate(
            isAlive = { childAlive },
            destroy = { },
            waitForExit = { false },
            destroyForcibly = { childAlive = false }
        )
        assertTrue(stopped)
        assertFalse(childAlive)
    }
}
