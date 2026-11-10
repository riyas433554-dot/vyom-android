package com.hermes.assistant

/**
 * Recovers a proposal branch after a write/PR request fails.
 *
 * A branch is deleted only after the caller positively confirms that no PR exists.
 * Lookup uncertainty preserves the branch so a PR created by a timed-out request
 * is never orphaned from its review history.
 */
internal object ProposalBranchRecovery {
    fun recover(
        findExistingPullRequest: () -> String?,
        deleteBranch: () -> Unit
    ): String? {
        val existingPullRequest = try {
            findExistingPullRequest()
        } catch (_: Exception) {
            return null
        }
        if (!existingPullRequest.isNullOrBlank()) return existingPullRequest
        try {
            deleteBranch()
        } catch (_: Exception) {
            // Best effort only. The original proposal failure remains authoritative.
        }
        return null
    }
}
