package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryStatusTest {
    @Test fun metadataRoundTrips() {
        val key = VyomFactoryStatus.idempotencyKey("Vyom_Chat", "message-42")!!
        val meta = VyomFactoryStatus.Metadata("P1", "in_progress", "vyom_chat", "message-42", key, listOf("created"))
        val body = VyomFactoryStatus.withMetadata("Fix update flow", meta)
        assertTrue(body.contains("Fix update flow"))
        assertEquals(meta, VyomFactoryStatus.readMetadata(body))
    }

    @Test fun invalidMetadataIsRejected() {
        assertNull(VyomFactoryStatus.readMetadata("ordinary complaint"))
        assertNull(VyomFactoryStatus.readMetadata("<!-- VYOM_FACTORY_META:{bad} -->"))
    }

    @Test fun replacementKeepsOneMarker() {
        val key = VyomFactoryStatus.idempotencyKey("factory", "local-1")!!
        val first = VyomFactoryStatus.Metadata("P2", "open", "factory", "local-1", key, emptyList())
        val next = first.copy(priority = "P0")
        val body = VyomFactoryStatus.withMetadata(VyomFactoryStatus.withMetadata("Body", first), next)
        assertEquals(1, Regex("VYOM_FACTORY_META:").findAll(body).count())
        assertEquals("P0", VyomFactoryStatus.readMetadata(body)?.priority)
    }

    @Test fun sourceKeysAreValidated() {
        assertEquals("termux:issue-18", VyomFactoryStatus.idempotencyKey("TERMUX", "issue-18"))
        assertNull(VyomFactoryStatus.idempotencyKey("unknown", "1"))
        assertNull(VyomFactoryStatus.idempotencyKey("factory", " "))
    }

    @Test fun transitionsAreBoundedAndTerminalStatesStayTerminal() {
        assertTrue(VyomFactoryStatus.canTransition("open", "in_progress"))
        assertTrue(VyomFactoryStatus.canTransition("in_progress", "paused"))
        assertTrue(VyomFactoryStatus.canTransition("blocked", "open"))
        assertFalse(VyomFactoryStatus.canTransition("open", "completed"))
        assertFalse(VyomFactoryStatus.canTransition("completed", "in_progress"))
        assertFalse(VyomFactoryStatus.canTransition("cancelled", "open"))
    }
}
