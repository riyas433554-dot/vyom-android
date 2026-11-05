package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryWorkerFailureRouterTest {
    @Test fun sameWorkerTaskAndFailureCreateStablePrivacySafeReport() {
        val first = VyomFactoryWorkerFailureRouter.create("skill_repository", "inspect:owner/repo", "IOException")
        val retry = VyomFactoryWorkerFailureRouter.create("skill_repository", "inspect:owner/repo", "IOException")
        assertEquals(first, retry)
        assertEquals("skill_repository:inspect:owner/repo", first.sourceId)
        assertTrue(first.complaint.contains("IOException"))
        assertTrue(first.complaint.contains("Original request"))
        assertFalse(first.complaint.contains("api_key"))
    }

    @Test fun differentWorkersOrTasksHaveDistinctIdentities() {
        val runner = VyomFactoryWorkerFailureRouter.create("factory_runner", "run-42", "TimeoutException")
        val skill = VyomFactoryWorkerFailureRouter.create("skill_repository", "run-42", "TimeoutException")
        val next = VyomFactoryWorkerFailureRouter.create("factory_runner", "run-43", "TimeoutException")
        assertFalse(runner.sourceId == skill.sourceId)
        assertFalse(runner.sourceId == next.sourceId)
    }

    @Test fun unsupportedWorkerAndUnsafeTaskIdentityAreRejected() {
        assertRejected { VyomFactoryWorkerFailureRouter.create("chat", "task-1", "IOException") }
        assertRejected { VyomFactoryWorkerFailureRouter.create("factory_runner", "task\n1", "IOException") }
        assertRejected { VyomFactoryWorkerFailureRouter.create("factory_runner", "x".repeat(81), "IOException") }
    }

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected invalid worker failure to be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
