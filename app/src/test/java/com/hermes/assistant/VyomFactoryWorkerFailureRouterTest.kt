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
        val registered = VyomFactoryComplaintRecord.fromFreeText("task_failure", first.complaint)
        assertEquals("execution", registered.area)
        assertEquals("P0", registered.priority)
        val unsafe = VyomFactoryWorkerFailureRouter.create("factory_runner", "run-1", "sk_live_secret_value")
        assertTrue(unsafe.complaint.contains("UnknownFailure"))
        assertFalse(unsafe.complaint.contains("sk_live_secret_value"))
    }

    @Test fun runnerFailureIncludesRedactedMessageAndTopSourceFrame() {
        val failure = IllegalArgumentException("Invalid worker argument")
        failure.stackTrace = arrayOf(StackTraceElement("com.hermes.assistant.VyomFactoryDialog", "runNextTask", "VyomFactoryDialog.kt", 768))
        val report = VyomFactoryWorkerFailureRouter.create(
            "factory_runner", "run-44", failure.javaClass.simpleName,
            failure.message, failure.stackTrace.firstOrNull()?.let { "${it.fileName}:${it.lineNumber}" }
        )
        assertTrue(report.complaint.contains("Message: Invalid worker argument"))
        assertTrue(report.complaint.contains("Source: VyomFactoryDialog.kt:768"))
    }

    @Test fun runnerFailureRedactsCredentialBearingExceptionMessage() {
        val report = VyomFactoryWorkerFailureRouter.create(
            "factory_runner", "run-45", "IllegalStateException",
            "request failed with api key sk-live-secret-value", "Worker.kt:42"
        )
        assertTrue(report.complaint.contains("Message: [REDACTED]"))
        assertFalse(report.complaint.contains("sk-live-secret-value"))
        assertTrue(report.complaint.contains("Source: Worker.kt:42"))
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
