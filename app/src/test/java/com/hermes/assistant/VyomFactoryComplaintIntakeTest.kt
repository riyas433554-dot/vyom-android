package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryComplaintIntakeTest {
    @Test fun acceptsAllOwnerNamedComplaintSources() {
        val sources = listOf("chatgpt", "claude", "codex", "termux", "vyom")
        sources.forEach { source ->
            val intake = VyomFactoryComplaintIntake.fromExternalShare(
                source,
                "Factory worker lease renewal failed"
            )
            assertEquals(source, intake.record.source)
            assertEquals("execution", intake.record.area)
            assertEquals("P0", intake.record.priority)
        }
    }

    @Test fun createsStableSourceScopedIdentityForRetryDeduplication() {
        val first = VyomFactoryComplaintIntake.fromExternalShare("claude", "Factory worker lease renewal failed")
        val retry = VyomFactoryComplaintIntake.fromExternalShare("claude", "Factory worker lease renewal failed")
        val otherSource = VyomFactoryComplaintIntake.fromExternalShare("chatgpt", "Factory worker lease renewal failed")

        assertEquals(first.sourceId, retry.sourceId)
        assertNotEquals(first.sourceId, otherSource.sourceId)
        assertTrue(first.sourceId.startsWith("external:"))
    }

    @Test fun rejectsUnknownExternalSourceInsteadOfMisattributingIt() {
        try {
            VyomFactoryComplaintIntake.fromExternalShare("unknown", "Factory worker failed")
            throw AssertionError("unknown source must be rejected")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun rejectsBlankAndOversizedSharedComplaints() {
        listOf("", "x".repeat(12_001)).forEach { text ->
            try {
                VyomFactoryComplaintIntake.fromExternalShare("termux", text)
                throw AssertionError("invalid complaint must be rejected")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun canonicalizesSupportedCallerPackagesAndExplicitSourceHints() {
        assertEquals("chatgpt", VyomFactoryComplaintIntake.resolveSource("chatgpt", null))
        assertEquals("claude", VyomFactoryComplaintIntake.resolveSource(null, "com.anthropic.claude"))
        assertEquals("termux", VyomFactoryComplaintIntake.resolveSource(null, "com.termux"))
        assertEquals("codex", VyomFactoryComplaintIntake.resolveSource(null, "com.openai.codex"))
        assertEquals("share_sheet", VyomFactoryComplaintIntake.resolveSource(null, "com.example.unknown"))
    }
}
