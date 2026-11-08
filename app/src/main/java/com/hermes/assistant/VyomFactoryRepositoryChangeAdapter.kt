package com.hermes.assistant

import java.util.concurrent.ConcurrentHashMap

/** Bridges the locally approved plan to the existing repository proposal executor. */
internal class VyomFactoryRepositoryChangeAdapter(
    private val registry: VyomFactorySystemRegistry,
    private val githubToken: String,
    private val approvalStore: VyomFactoryRepositoryApprovalStore,
    private val approvedPlans: ConcurrentHashMap<Int, ApprovedPlan>,
    private val executor: VyomFactoryRepositoryChangeExecutor = VyomFactoryRepositoryChangeExecutor(registry)
) : VyomFactoryExecutionAdapter {
    data class ApprovedPlan(
        val plan: VyomFactoryRepositoryChangePolicy.Plan,
        val token: VyomFactoryRepositoryApprovalStore.Token
    )

    override val target = VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE
    override val isIdempotent = false

    override fun isVerifiedResult(result: String): Boolean =
        result.startsWith("repository_proposal_verified:") &&
            Regex("commit=[a-f0-9]{40}").containsMatchIn(result) &&
            Regex("https://github\\.com/[^\\s]+/pull/[0-9]+").containsMatchIn(result) &&
            result.contains("ci=")

    override fun run(
        envelope: VyomFactoryExecutionEnvelope,
        control: VyomFactoryExecutionControl
    ): String {
        if (envelope.issueNumber <= 0) throw SecurityException("Repository task identity is missing")
        if (envelope.operation != "propose_change") throw SecurityException("Unsupported repository worker operation")
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        val approved = approvedPlans.remove(envelope.issueNumber)
            ?: throw SecurityException("A local owner-review approval token is required")
        if (approved.plan.issueNumber != envelope.issueNumber) {
            throw SecurityException("Local approval belongs to a different issue")
        }
        val binding = registry.get(approved.plan.systemId)
            ?: throw SecurityException("Repository binding disappeared after owner review")
        approvalStore.verifyAndConsume(approved.plan, binding, approved.token)
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        val trustedPlan = approved.plan.copy(ownerApproved = true)
        val proposal = executor.execute(trustedPlan, githubToken, control)
        return "repository_proposal_verified: issue=${approved.plan.issueNumber}; " +
            "commit=${proposal.commitSha}; pr=${proposal.pullRequestUrl}; ci=${proposal.ciEvidence}"
    }
}
