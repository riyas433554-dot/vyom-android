package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class ShizukuRequestRegistryTest {
    @Test fun stopBeforeExecuteRejectsTheRequest() {
        val registry = ShizukuRequestRegistry()
        assertTrue(registry.cancel("req-1"))
        assertEquals(ShizukuRequestRegistry.Begin.CANCELLED_BEFORE_START, registry.begin("req-1"))
        assertTrue(registry.isCancelled("req-1"))
    }

    @Test fun stopDuringStartupPreventsTransitionToRunning() {
        val registry = ShizukuRequestRegistry()
        assertEquals(ShizukuRequestRegistry.Begin.ACCEPTED, registry.begin("req-2"))
        assertTrue(registry.cancel("req-2"))
        assertFalse(registry.markRunning("req-2"))
        assertEquals(ShizukuRequestRegistry.State.CANCEL_REQUESTED, registry.state("req-2"))
    }

    @Test fun runningRequestCanBeCancelledAndCannotBeReplayedAfterCompletion() {
        val registry = ShizukuRequestRegistry()
        assertEquals(ShizukuRequestRegistry.Begin.ACCEPTED, registry.begin("req-3"))
        assertTrue(registry.markRunning("req-3"))
        assertTrue(registry.cancel("req-3"))
        registry.complete("req-3")
        assertFalse(registry.cancel("req-3"))
        assertEquals(ShizukuRequestRegistry.Begin.DUPLICATE, registry.begin("req-3"))
    }

    @Test fun duplicateConcurrentBeginIsRejected() {
        val registry = ShizukuRequestRegistry()
        assertEquals(ShizukuRequestRegistry.Begin.ACCEPTED, registry.begin("same"))
        assertEquals(ShizukuRequestRegistry.Begin.DUPLICATE, registry.begin("same"))
    }

    @Test fun pendingStopTombstonesAndActiveRequestsCannotExceedCapacity() {
        val registry = ShizukuRequestRegistry(maxEntries = 2)
        assertTrue(registry.cancel("stop-a"))
        assertTrue(registry.cancel("stop-b"))
        assertFalse(registry.cancel("stop-c"))
        assertEquals(ShizukuRequestRegistry.Begin.CANCELLED_BEFORE_START, registry.begin("stop-a"))
        assertEquals(ShizukuRequestRegistry.Begin.CANCELLED_BEFORE_START, registry.begin("stop-b"))
        assertEquals(ShizukuRequestRegistry.Begin.ACCEPTED, registry.begin("active-a"))
        assertEquals(ShizukuRequestRegistry.Begin.ACCEPTED, registry.begin("active-b"))
        assertEquals(ShizukuRequestRegistry.Begin.CAPACITY_REACHED, registry.begin("active-c"))
    }

    @Test fun completedHistoryIsBoundedWithoutClearingActiveRequests() {
        val registry = ShizukuRequestRegistry(maxEntries = 2)
        registry.begin("active")
        registry.begin("old"); registry.complete("old")
        registry.begin("new"); registry.complete("new")
        registry.begin("latest"); registry.complete("latest")
        assertEquals(ShizukuRequestRegistry.State.STARTING, registry.state("active"))
        assertEquals(ShizukuRequestRegistry.Begin.ACCEPTED, registry.begin("old"))
    }
}
