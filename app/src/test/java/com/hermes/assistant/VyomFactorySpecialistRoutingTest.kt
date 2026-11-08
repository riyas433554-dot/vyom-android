package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VyomFactorySpecialistRoutingTest {
    @Test fun routesOldestUnresolvedRepositoryComplaintForOwnerReview() {
        val decisions = listOf(
            decision(12, "P1", "2026-11-02T00:00:00Z", repositoryReview()),
            decision(11, "P1", "2026-11-01T00:00:00Z", repositoryReview()),
            decision(10, "P0", "2026-10-31T00:00:00Z", repositoryReview())
        )

        assertEquals(10, VyomFactorySpecialistRouting.nextOwnerReview(decisions)?.task?.number)
    }

    @Test fun usesOldestCreatedIssueWithinTheSamePriorityTier() {
        val decisions = listOf(
            decision(12, "P1", "2026-11-02T00:00:00Z", repositoryReview()),
            decision(11, "P1", "2026-11-01T00:00:00Z", repositoryReview())
        )

        assertEquals(11, VyomFactorySpecialistRouting.nextOwnerReview(decisions)?.task?.number)
    }

    @Test fun ignoresPausedOrTerminalRepositoryTasks() {
        val paused = decision(10, "P0", "2026-10-31T00:00:00Z", repositoryReview()).let {
            it.copy(task = it.task.copy(metadata = it.task.metadata.copy(status = "paused")))
        }
        val completed = decision(11, "P1", "2026-11-01T00:00:00Z", repositoryReview()).let {
            it.copy(task = it.task.copy(state = "closed"))
        }

        assertNull(VyomFactorySpecialistRouting.nextOwnerReview(listOf(paused, completed)))
    }

    @Test fun preservesHigherPriorityExecutableTaskBeforeLowerPriorityRepositoryReview() {
        val decisions = listOf(
            decision(20, "P1", "2026-11-02T00:00:00Z", repositoryReview()),
            decision(10, "P0", "2026-11-01T00:00:00Z",
                VyomFactoryTaskPlanner.Plan.CapabilityCandidates(
                    VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                    listOf("android_control.set_media_volume")
                ))
        )

        assertNull(VyomFactorySpecialistRouting.nextOwnerReview(decisions, isRunnerEligible = { it.number == 10 }))
    }

    @Test fun routesHigherPriorityRepositoryReviewBeforeLowerPriorityExecutableTask() {
        val decisions = listOf(
            decision(10, "P0", "2026-11-01T00:00:00Z", repositoryReview()),
            decision(20, "P1", "2026-11-02T00:00:00Z",
                VyomFactoryTaskPlanner.Plan.CapabilityCandidates(
                    VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                    listOf("android_control.set_media_volume")
                ))
        )

        assertEquals(
            10,
            VyomFactorySpecialistRouting.nextOwnerReview(decisions, isRunnerEligible = { it.number == 20 })?.task?.number
        )
    }

    @Test fun skipsPreviouslyDeferredRepositoryComplaintAndRoutesNextInFifoOrder() {
        val decisions = listOf(
            decision(10, "P0", "2026-10-31T00:00:00Z", repositoryReview(),
                events = setOf(VyomFactorySpecialistRouting.OWNER_REVIEW_DEFERRED_EVENT)),
            decision(11, "P1", "2026-11-01T00:00:00Z", repositoryReview())
        )

        assertEquals(11, VyomFactorySpecialistRouting.nextOwnerReview(decisions)?.task?.number)
    }

    @Test fun skipsRepositoryComplaintWithAlreadyOpenedProposal() {
        val decisions = listOf(
            decision(10, "P0", "2026-10-31T00:00:00Z", repositoryReview(),
                events = setOf(VyomFactorySpecialistRouting.OWNER_REVIEW_OPENED_EVENT))
        )

        assertNull(VyomFactorySpecialistRouting.nextOwnerReview(decisions))
    }

    @Test fun crashLostLocalApprovalAllowsOwnerReviewAgain() {
        val decisions = listOf(decision(33, "P1", "2026-11-01T00:00:00Z", repositoryReview()))
        // A new process has no in-memory approval token, so it must re-open local review.
        assertEquals(33, VyomFactorySpecialistRouting.nextOwnerReview(decisions)?.task?.number)
    }

    @Test fun locallyApprovedPlanSkipsReviewRouteUntilRunnerConsumesIt() {
        val decisions = listOf(decision(34, "P1", "2026-11-01T00:00:00Z", repositoryReview()))
        assertNull(VyomFactorySpecialistRouting.nextOwnerReview(decisions, isLocallyApproved = { it == 34 }))
    }

    @Test fun doesNotRouteExecutableOrClarificationDecisionsToRepositoryProposal() {
        val decisions = listOf(
            decision(10, "P0", "2026-10-31T00:00:00Z",
                VyomFactoryTaskPlanner.Plan.CapabilityCandidates(
                    VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                    listOf("android_control.set_media_volume")
                )),
            decision(11, "P1", "2026-11-01T00:00:00Z",
                VyomFactoryTaskPlanner.Plan.NeedsOwnerClarification("multiple_domains"))
        )

        assertNull(VyomFactorySpecialistRouting.nextOwnerReview(decisions))
    }

    private fun repositoryReview() = VyomFactoryTaskPlanner.Plan.NeedsOwnerReview(
        VyomFactoryCapabilityRegistry.Domain.REPOSITORY,
        "repository_proposal_requires_owner_review"
    )

    private fun decision(
        number: Int,
        priority: String,
        createdAt: String,
        plan: VyomFactoryTaskPlanner.Plan,
        events: Set<String> = emptySet()
    ): VyomFactoryTaskTriage.Decision {
        val task = VyomFactoryGitHubClient.Task(
            number = number,
            title = "Gradle compile failure in repository",
            body = "Expected result and verification probe",
            state = "open",
            url = "https://github.com/example/repo/issues/$number",
            updatedAt = createdAt,
            metadata = VyomFactoryStatus.Metadata(
                priority = priority,
                status = "open",
                source = "factory",
                sourceId = "routing-$number",
                idempotencyKey = "factory:routing-$number",
                events = events.toList()
            ),
            createdAt = createdAt
        )
        return VyomFactoryTaskTriage.Decision(
            task = task,
            plan = plan,
            eventKey = "triage:test:$number"
        )
    }
}
