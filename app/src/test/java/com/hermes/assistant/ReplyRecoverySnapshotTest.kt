package com.hermes.assistant

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyRecoverySnapshotTest {
    @Test fun parsesCompletedReply() {
        val value = ReplyRecoverySnapshot.fromJson(
            JSONObject("""{"message_id":"m-1","status":"done","reply":"Recovered answer"}""")
        )
        assertEquals("m-1", value?.messageId)
        assertEquals("done", value?.status)
        assertEquals("Recovered answer", value?.reply)
        assertTrue(value?.isTerminal == true)
    }

    @Test fun queuedAndRunningAreNonTerminal() {
        listOf("queued", "running").forEach { status ->
            val value = ReplyRecoverySnapshot.fromJson(
                JSONObject().put("message_id", "m-2").put("status", status)
            )
            assertEquals(status, value?.status)
            assertFalse(value?.isTerminal ?: true)
        }
    }

    @Test fun parsesTerminalError() {
        val value = ReplyRecoverySnapshot.fromJson(
            JSONObject("""{"message_id":"m-3","status":"error","error":"worker failed"}""")
        )
        assertEquals("worker failed", value?.error)
        assertTrue(value?.isTerminal == true)
    }

    @Test fun rejectsUnknownOrIncompleteResponses() {
        assertNull(ReplyRecoverySnapshot.fromJson(JSONObject().put("message_id", "m").put("status", "maybe")))
        assertNull(ReplyRecoverySnapshot.fromJson(JSONObject().put("message_id", "m").put("status", "done")))
        assertNull(ReplyRecoverySnapshot.fromJson(JSONObject().put("status", "queued")))
    }
}