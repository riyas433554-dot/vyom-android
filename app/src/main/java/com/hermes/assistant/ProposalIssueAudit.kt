package com.hermes.assistant

/** Best-effort Factory issue audit: a failed comment is reported to the UI, never hidden as success. */
internal object ProposalIssueAudit {
    fun recordIfLinked(
        client: VyomFactoryGitHubClient,
        issueNumber: Int?,
        message: String
    ): Boolean {
        if (issueNumber == null) return true
        if (issueNumber <= 0 || message.isBlank() || message.length > 1_000) return false
        return runCatching { client.addEvent(issueNumber, message) }.isSuccess
    }
}
