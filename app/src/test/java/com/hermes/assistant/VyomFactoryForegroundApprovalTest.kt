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
        gate.onPaused()
        gate.onResumed()
        assertTrue(gate.isForeground())
        assertNotEquals(approvedEpoch, gate.generation())
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
}
