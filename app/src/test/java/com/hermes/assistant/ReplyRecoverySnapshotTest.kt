package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyRecoverySnapshotTest {
    @Test fun parsesCompletedReply() {
        val value = ReplyRecoverySnapshot.fromFields("m-1", "done", "Recovered answer", null)
        assertEquals("m-1", value?.messageId)
        assertEquals("done", value?.status)
        assertEquals("Recovered answer", value?.reply)
        assertTrue(value?.isTerminal == true)
    }

    @Test fun queuedAndRunningAreNonTerminal() {
        listOf("queued", "running").forEach { status ->
            val value = ReplyRecoverySnapshot.fromFields("m-2", status, null, null)
            assertEquals(status, value?.status)
            assertFalse(value?.isTerminal ?: true)
        }
    }

    @Test fun parsesTerminalError() {
        val value = ReplyRecoverySnapshot.fromFields("m-3", "error", null, "worker failed")
        assertEquals("worker failed", value?.error)
        assertTrue(value?.isTerminal == true)
    }

    @Test fun rejectsUnknownOrIncompleteResponses() {
        assertNull(ReplyRecoverySnapshot.fromFields("m", "maybe", null, null))
        assertNull(ReplyRecoverySnapshot.fromFields("m", "done", null, null))
        assertNull(ReplyRecoverySnapshot.fromFields(null, "queued", null, null))
        assertNull(ReplyRecoverySnapshot.fromFields("m", "running", "unexpected", null))
    }
}