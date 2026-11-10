package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryForegroundApprovalTest {
    @Test fun foregroundGateTracksResumeAndPause() {
        val gate = VyomFactoryForegroundGate()
        assertFalse(gate.isForeground())
        gate.onResumed()
        assertTrue(gate.isForeground())
        gate.onPaused()
        assertFalse(gate.isForeground())
    }

    @Test fun foregroundEpochChangesWhenActivityLeavesForeground() {
        val gate = VyomFactoryForegroundGate()
        gate.onResumed()
        val approvedEpoch = gate.generation()
        assertTrue(gate.isForegroundSession(approvedEpoch))
        gate.onPaused()
        gate.onResumed()
        assertTrue(gate.isForeground())
        assertNotEquals(approvedEpoch, gate.generation())
        assertFalse(gate.isForegroundSession(approvedEpoch))
    }

    @Test fun approvalCanBeConsumedOnlyOnce() {
        val approvals = VyomFactoryOneTimeApprovalStore()
        val id = approvals.issue()
        assertTrue(approvals.consume(id))
        assertFalse(approvals.consume(id))
    }

    @Test fun clearingApprovalsInvalidatesEveryPreviouslyIssuedToken() {
        val approvals = VyomFactoryOneTimeApprovalStore()
        val first = approvals.issue()
        val second = approvals.issue()

        approvals.clear()

        assertFalse(approvals.consume(first))
        assertFalse(approvals.consume(second))
    }

    @Test fun unknownAndBlankApprovalIdsAreRejected() {
        val approvals = VyomFactoryOneTimeApprovalStore()
        assertFalse(approvals.consume(""))
        assertFalse(approvals.consume("not-issued"))
    }

    @Test fun approvalIsNotConsumedByWrongToken() {
        val approvals = VyomFactoryOneTimeApprovalStore()
        val id = approvals.issue()
        assertFalse(approvals.consume("wrong"))
        assertTrue(approvals.consume(id))
    }

    @Test fun fullApprovalStoreRejectsNewIssueWithoutEvictingExistingApprovals() {
        var nextId = 0
        val approvals = VyomFactoryOneTimeApprovalStore(newId = { "approval-${++nextId}" })
        val issued = (1..64).map { approvals.issue() }

        val error = try {
            approvals.issue()
            null
        } catch (expected: IllegalStateException) {
            expected
        }

        assertNotNull("The 65th pending approval must be rejected", error)
        assertTrue(error!!.message.orEmpty().contains("task must remain pending"))
        issued.forEach { assertTrue("Existing approval was evicted: $it", approvals.consume(it)) }
    }
}
