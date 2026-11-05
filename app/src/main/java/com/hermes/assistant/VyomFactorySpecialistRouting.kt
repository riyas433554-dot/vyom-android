package com.hermes.assistant

/**
 * Selects a non-executable repository proposal handoff from the already-triaged
 * canonical issue snapshot. The owner still chooses a source path and approves
 * the exact diff before any PR is opened.
 */
internal object VyomFactorySpecialistRouting {
    const val OWNER_REVIEW_DEFERRED_EVENT = "factory:v1:repository_proposal_deferred"

    fun nextOwnerReview(
        decisions: List<VyomFactoryTaskTriage.Decision>
    ): VyomFactoryTaskTriage.Decision? {
        val byIssue = decisions.associateBy { it.task.number }
        return VyomFactoryTaskOrdering.sort(decisions.map { it.task })
            .asSequence()
            .mapNotNull { task -> byIssue[task.number] }
            .firstOrNull { decision ->
                val task = decision.task
                val plan = decision.plan
                !decision.executionReady &&
                    task.state == "open" &&
                    task.metadata.status in setOf("open", "in_progress") &&
                    plan is VyomFactoryTaskPlanner.Plan.NeedsOwnerReview &&
                    plan.domain == VyomFactoryCapabilityRegistry.Domain.REPOSITORY &&
                    OWNER_REVIEW_DEFERRED_EVENT !in task.metadata.events
            }
    }
}
