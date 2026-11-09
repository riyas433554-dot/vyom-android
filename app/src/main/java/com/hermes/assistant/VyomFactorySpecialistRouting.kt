package com.hermes.assistant

/**
 * Selects a non-executable repository proposal handoff from the already-triaged
 * canonical issue snapshot. The owner still chooses a source path and approves
 * the exact diff before any PR is opened.
 */
internal object VyomFactorySpecialistRouting {
    const val OWNER_REVIEW_DEFERRED_EVENT = "factory:v1:repository_proposal_deferred"
    const val OWNER_REVIEW_OPENED_EVENT = "factory:v1:repository_proposal_opened"

    fun nextOwnerReview(
        decisions: List<VyomFactoryTaskTriage.Decision>,
        isRunnerEligible: (VyomFactoryGitHubClient.Task) -> Boolean = { false },
        isLocallyApproved: (Int) -> Boolean = { false }
    ): VyomFactoryTaskTriage.Decision? {
        val byIssue = decisions.associateBy { it.task.number }
        for (task in VyomFactoryTaskOrdering.sort(decisions.map { it.task })) {
            val decision = byIssue[task.number] ?: continue
            if (task.state != "open" || task.metadata.status !in setOf("open", "in_progress")) continue
            if (decision.executionReady || isLocallyApproved(task.number)) continue
            when (val plan = decision.plan) {
                is VyomFactoryTaskPlanner.Plan.NeedsOwnerReview -> {
                    if (plan.domain == VyomFactoryCapabilityRegistry.Domain.REPOSITORY &&
                        OWNER_REVIEW_DEFERRED_EVENT !in task.metadata.events &&
                        OWNER_REVIEW_OPENED_EVENT !in task.metadata.events
                    ) return decision
                }
                is VyomFactoryTaskPlanner.Plan.CapabilityCandidates -> {
                    // If this earlier item can actually run, leave queue ownership
                    // to the existing runner. Do not let a later proposal jump it.
                    if (isRunnerEligible(task)) return null
                }
                else -> Unit
            }
        }
        return null
    }
}
