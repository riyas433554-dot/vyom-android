package com.hermes.assistant

/**
 * Boundary for a provider-backed worker that proposes a structured repository plan.
 * The returned candidate is unapproved and cannot write to GitHub.
 */
internal fun interface VyomFactoryRepositoryWorkerModelAdapter {
    fun propose(
        task: VyomFactoryGitHubClient.Task,
        registry: VyomFactorySystemRegistry,
        control: VyomFactoryExecutionControl
    ): VyomFactoryRepositoryChangeReviewDialog.Candidate
}
