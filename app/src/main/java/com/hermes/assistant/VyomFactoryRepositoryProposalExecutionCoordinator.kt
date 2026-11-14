package com.hermes.assistant

/**
 * Executes one stored worker proposal only after a fresh GitHub task read and
 * consumption of the one-use token issued by the local owner-review dialog.
 *
 * The executor creates a pinned-commit draft PR and waits for exact-SHA CI.
 * It never merges, releases, installs, or writes to a protected branch.
 */
internal class VyomFactoryRepositoryProposalExecutionCoordinator(
    private val registry: VyomFactorySystemRegistry,
    private val proposals: ProposalAccess,
    private val fetchFreshTask: (Int) -> VyomFactoryGitHubClient.Task?,
    private val verifyApproval: (
        VyomFactoryRepositoryChangePolicy.Plan,
        VyomFactorySystemRegistry.Binding,
        VyomFactoryRepositoryApprovalStore.Token
    ) -> Unit,
    private val executePlan: (
        VyomFactoryRepositoryChangePolicy.Plan,
        String,
        VyomFactoryExecutionControl
    ) -> VyomFactoryRepositoryChangeExecutor.Proposal
) {
    interface ProposalAccess {
        fun get(issueNumber: Int): VyomFactoryWorkerProposalStore.StoredProposal?
        fun markApproved(issueNumber: Int): Boolean
        fun markSubmitted(issueNumber: Int): Boolean
    }

    fun execute(
        issueNumber: Int,
        approval: VyomFactoryRepositoryApprovalStore.Token,
        githubToken: String,
        control: VyomFactoryExecutionControl
    ): VyomFactoryRepositoryChangeExecutor.Proposal {
        require(issueNumber > 0)
        require(githubToken.trim().length >= 20) { "GitHub repository write token is unavailable" }
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()

        val stored = proposals.get(issueNumber)
            ?: throw SecurityException("No stored worker proposal exists for this issue")
        if (stored.status !in setOf("awaiting_review", "approved")) {
            throw SecurityException("Proposal is not eligible for owner-approved execution")
        }
        if (stored.issueNumber != issueNumber || stored.sourceId.isBlank() || stored.updatedAt.isBlank()) {
            throw SecurityException("Stored proposal identity is incomplete")
        }

        val fresh = fetchFreshTask(issueNumber)
            ?: throw SecurityException("GitHub task disappeared before execution")
        if (fresh.number != issueNumber || fresh.state != "open" ||
            fresh.metadata.status !in setOf("open", "in_progress") ||
            fresh.metadata.sourceId != stored.sourceId ||
            fresh.updatedAt != stored.updatedAt
        ) {
            throw SecurityException("GitHub task changed after proposal generation; regenerate and review a fresh proposal")
        }

        val candidate = stored.candidate
        val plan = candidate.plan
        if (plan.issueNumber != issueNumber || plan.ownerApproved) {
            throw SecurityException("Worker proposal identity or approval state is invalid")
        }
        val binding = registry.get(plan.systemId)
            ?: throw SecurityException("Owner-approved repository binding is missing")
        if (binding != candidate.binding) {
            throw SecurityException("Repository binding changed after proposal generation")
        }
        val validated = VyomFactoryRepositoryChangePolicy.validateCandidate(plan, registry)
        if (validated != binding) {
            throw SecurityException("Repository binding changed during proposal validation")
        }
        val expectedPaths = plan.changes.map { it.path }.toSet()
        if (candidate.sourceFileSha256.keys != expectedPaths ||
            candidate.sourceFileSha256.values.any { !it.matches(Regex("^[a-f0-9]{64}$")) }
        ) {
            throw SecurityException("Proposal is missing complete immutable-base source hash evidence")
        }

        // Consume the exact, short-lived token created by the explicit local owner tap.
        verifyApproval(plan, binding, approval)
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        check(proposals.markApproved(issueNumber)) {
            "Approved proposal state could not be persisted; repository write was not started"
        }

        val result = executePlan(plan.copy(ownerApproved = true), githubToken, control)

        // The executor returns only after the exact proposal commit and required CI are verified.
        check(proposals.markSubmitted(issueNumber)) {
            "Repository proposal passed execution but its local submitted state could not be persisted"
        }
        return result
    }
}
