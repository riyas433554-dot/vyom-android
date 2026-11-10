package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryComplaintSubmissionPolicyTest {
    @Test
    fun missingGitHubTokenKeepsComplaintQueuedForReconnectRetry() {
        val decision = VyomFactoryComplaintSubmissionPolicy.missingGitHubToken()

        assertEquals("queued", decision.state)
        assertTrue(decision.reply.contains("GitHub token missing"))
        assertTrue(decision.reply.contains("reconnect"))
    }
}
