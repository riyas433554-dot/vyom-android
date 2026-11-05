package com.hermes.assistant

/**
 * Read-only triage over the canonical GitHub issue snapshot.
 *
 * Decisions are deliberately non-executable. Even a capability candidate must
 * pass the existing envelope, fresh-issue, lease, STOP, consent and adapter gates.
 */
internal class VyomFactoryTaskTriage(
    private val planner: VyomFactoryTaskPlanner = VyomFactoryTaskPlanner()
) {
    data class Decision(
        val task: VyomFactoryGitHubClient.Task,
        val plan: VyomFactoryTaskPlanner.Plan,
        val eventKey: String,
        val executionReady: Boolean = false
    )

    fun evaluate(
        tasks: List<VyomFactoryGitHubClient.Task>,
        capabilities: VyomFactoryCapabilityRegistry
    ): List<Decision> = VyomFactoryTaskOrdering.sort(
        tasks.filter {
            it.state == "open" && it.metadata.status in setOf("open", "in_progress")
        }
    ).map { task ->
        val plan = planner.plan(task, capabilities)
        Decision(task, plan, eventKey(plan))
    }

    private fun eventKey(plan: VyomFactoryTaskPlanner.Plan): String = when (plan) {
        is VyomFactoryTaskPlanner.Plan.CapabilityCandidates ->
            "triage:v1:capability_candidates:${plan.domain.name}:${plan.capabilityIds.sorted().joinToString(",")}"
        is VyomFactoryTaskPlanner.Plan.BlockedCapability ->
            "triage:v1:blocked_capability:${plan.domain.name}:${stableReason(plan.reason)}"
        is VyomFactoryTaskPlanner.Plan.NeedsOwnerClarification ->
            "triage:v1:needs_owner_clarification:${stableReason(plan.reason)}"
        is VyomFactoryTaskPlanner.Plan.NeedsOwnerReview ->
            "triage:v1:needs_owner_review:${plan.domain.name}:${stableReason(plan.reason)}"
    }

    private fun stableReason(reason: String): String =
        reason.lowercase().replace(Regex("[^a-z0-9_-]+"), "_").take(120).ifBlank { "unspecified" }
}
