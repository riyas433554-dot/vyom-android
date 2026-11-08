package com.hermes.assistant

/**
 * Boundary for a worker model that proposes a structured repository plan.
 * Model output is untrusted proposal data; it never carries owner approval.
 */
internal fun interface VyomFactoryRepositoryWorkerModelAdapter {
    fun propose(
        task: VyomFactoryGitHubClient.Task,
        registry: VyomFactorySystemRegistry,
        control: VyomFactoryExecutionControl
    ): VyomFactoryRepositoryChangeReviewDialog.Candidate
}

/**
 * Issue-controlled JSON is no longer accepted as a worker plan. GitHub issue
 * text is untrusted task data and cannot impersonate a model response.
 */
@Deprecated("Use VyomFactoryStructuredWorkerModelAdapter with an owner-selected provider")
internal class VyomFactoryIssueEnvelopeWorkerModelAdapter : VyomFactoryRepositoryWorkerModelAdapter {
    override fun propose(
        task: VyomFactoryGitHubClient.Task,
        registry: VyomFactorySystemRegistry,
        control: VyomFactoryExecutionControl
    ): VyomFactoryRepositoryChangeReviewDialog.Candidate {
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        throw SecurityException("Issue-controlled worker proposals are disabled")
    }
}
