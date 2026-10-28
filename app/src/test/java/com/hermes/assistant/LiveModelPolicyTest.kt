package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveModelPolicyTest {
    @Test fun blankSavedModelUsesRoadmapDefault() {
        assertEquals(LiveModelPolicy.DEFAULT_MODEL, LiveModelPolicy.selectedOrDefault(null))
        assertEquals(LiveModelPolicy.DEFAULT_MODEL, LiveModelPolicy.selectedOrDefault("  "))
    }

    @Test fun selectedModelIsTrimmedAndPreserved() {
        assertEquals("custom/model", LiveModelPolicy.selectedOrDefault("  custom/model  "))
    }

    @Test fun onlyHttp404UsesFallbackModel() {
        assertEquals(LiveModelPolicy.FALLBACK_MODEL, LiveModelPolicy.fallbackFor(404, LiveModelPolicy.DEFAULT_MODEL))
        assertNull(LiveModelPolicy.fallbackFor(400, LiveModelPolicy.DEFAULT_MODEL))
        assertNull(LiveModelPolicy.fallbackFor(401, LiveModelPolicy.DEFAULT_MODEL))
        assertNull(LiveModelPolicy.fallbackFor(429, LiveModelPolicy.DEFAULT_MODEL))
        assertNull(LiveModelPolicy.fallbackFor(500, LiveModelPolicy.DEFAULT_MODEL))
    }

    @Test fun fallbackModelDoesNotRetryItself() {
        assertNull(LiveModelPolicy.fallbackFor(404, LiveModelPolicy.FALLBACK_MODEL))
    }
}
