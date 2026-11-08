package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class ShizukuProcessTerminationTest {
 @Test fun gracefulStopWaitsForProcessExitWithoutForceKill() { var alive=true; var forced=false; val stopped=ShizukuProcessTerminator.terminate({alive},{alive=false},{alive=false;true},{forced=true;alive=false}); assertTrue(stopped); assertFalse(forced); assertFalse(alive) }
 @Test fun unresponsiveProcessIsForceKilledAndWaitedFor() { var alive=true; var forceCalls=0; var waits=0; val stopped=ShizukuProcessTerminator.terminate({alive},{},{waits++; if(waits==2) alive=false; !alive},{forceCalls++;alive=false}); assertTrue(stopped); assertEquals(1,forceCalls); assertEquals(2,waits); assertFalse(alive) }
 @Test fun alreadyExitedProcessNeedsNoTerminationCalls() { var calls=0; val stopped=ShizukuProcessTerminator.terminate({false},{calls++},{calls++;true},{calls++}); assertTrue(stopped); assertEquals(0,calls) }
 @Test fun teardownAttemptsEveryProcessEvenWhenOneTerminationThrows() { val first=Any(); val second=Any(); val attempted=mutableListOf<Any>(); val stopped=ShizukuProcessTerminator.terminateAll(setOf("req-1","req-2"),{if(it=="req-1") first else second}) { attempted+=it; if(it===first) error("process handle failed"); true }; assertEquals(2,attempted.size); assertTrue(attempted.containsAll(listOf(first,second))); assertEquals(setOf("req-2"),stopped) }
 @Test fun teardownSnapshotAndLateStartupCannotLeaveAnUntrackedProcessRunning() { val registry=ShizukuRequestRegistry(); assertEquals(ShizukuRequestRegistry.Begin.ACCEPTED,registry.begin("starting")); assertEquals(setOf("starting"),registry.shutdown()); var childAlive=true; assertFalse(registry.markRunning("starting")); val stopped=ShizukuProcessTerminator.terminate({childAlive},{},{false},{childAlive=false}); assertTrue(stopped); assertFalse(childAlive) }
}
