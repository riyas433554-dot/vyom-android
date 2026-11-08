package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryExecutionAuditTest {
    @Test fun verifiedAdapterResultIsRecordedWithoutClaimingComplaintClosure() {
        val outcome = VyomFactoryExecutionRunner.Outcome.Succeeded(
            issueNumber = 42,
            attemptId = "attempt-123456",
            result = "Volume set to 35% and read back as 35%",
            retryScheduleCleared = true
        )

        val comment = VyomFactoryExecutionAudit.comment(outcome)!!
        assertTrue(comment.contains("postcondition verified"))
        assertTrue(comment.contains("35%"))
        assertTrue(comment.contains("complaint remains open"))
    }

    @Test fun failedExecutionIsRecordedAsFailureAndNeverAsSuccess() {
        val outcome = VyomFactoryExecutionRunner.Outcome.Failed(42, "attempt-123456", "permission_denied")
        val comment = VyomFactoryExecutionAudit.comment(outcome)!!
        assertTrue(comment.contains("FAILED"))
        assertTrue(comment.contains("permission_denied"))
        assertTrue(comment.contains("No success was recorded"))
        assertFalse(comment.contains("verified"))
    }

    @Test fun skippedAndFencedOutcomesDoNotWriteMisleadingIssueEvents() {
        assertNull(VyomFactoryExecutionAudit.comment(VyomFactoryExecutionRunner.Outcome.Skipped(42, "manual_review_required")))
        assertNull(VyomFactoryExecutionAudit.comment(VyomFactoryExecutionRunner.Outcome.Fenced(42)))
        assertNull(VyomFactoryExecutionAudit.comment(VyomFactoryExecutionRunner.Outcome.NoWork))
    }
}
