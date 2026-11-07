package com.hermes.assistant

/** Pure intake decision for a complaint that cannot be submitted until credentials exist. */
internal object VyomFactoryComplaintSubmissionPolicy {
    data class Decision(val state: String, val reply: String)

    fun missingGitHubToken() = Decision(
        state = "queued",
        reply = "Factory registration pending: GitHub token missing. Save a token with repository Issues read/write permission in Settings, then reconnect to retry."
    )
}
