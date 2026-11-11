package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryPlanningApprovalPolicyTest {
    @Test fun changingPriorityOnAnInProgressTaskDoesNotGrantPlanningApproval() {
        assertNull(
            VyomFactoryPlanningApprovalPolicy.requestedLanePlanningAllowed(
                currentStatus = "in_progress",
                nextStatus = "in_progress",
                ownerApprovedStart = false
            )
        )
    }

    @Test fun explicitOwnerStartAndResumeGrantPlanningApproval() {
        assertEquals(
            true,
            VyomFactoryPlanningApprovalPolicy.requestedLanePlanningAllowed(
                currentStatus = "open",
                nextStatus = "in_progress",
                ownerApprovedStart = true
            )
        )
        assertEquals(
            true,
            VyomFactoryPlanningApprovalPolicy.requestedLanePlanningAllowed(
                currentStatus = "paused",
                nextStatus = "in_progress",
                ownerApprovedStart = true
            )
        )
    }

    @Test fun pausingBlockingOrCompletingRevokesPlanningApproval() {
        listOf("paused", "blocked", "completed", "cancelled").forEach { next ->
            assertEquals(
                false,
                VyomFactoryPlanningApprovalPolicy.requestedLanePlanningAllowed(
                    currentStatus = "in_progress",
                    nextStatus = next,
                    ownerApprovedStart = false
                )
            )
        }
    }

    @Test fun approvalCannotBeGrantedWithoutAnExplicitStartTransition() {
        assertThrows(IllegalArgumentException::class.java) {
            VyomFactoryPlanningApprovalPolicy.requestedLanePlanningAllowed(
                currentStatus = "in_progress",
                nextStatus = "in_progress",
                ownerApprovedStart = true
            )
        }
    }
}
