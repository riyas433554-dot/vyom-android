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
 * Adapts the structured model proposal already recorded in the task envelope.
 * This does not call or configure a new model integration.
 */
internal class VyomFactoryIssueEnvelopeWorkerModelAdapter : VyomFactoryRepositoryWorkerModelAdapter {
    override fun propose(
        task: VyomFactoryGitHubClient.Task,
        registry: VyomFactorySystemRegistry,
        control: VyomFactoryExecutionControl
    ): VyomFactoryRepositoryChangeReviewDialog.Candidate {
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        val candidate = VyomFactoryRepositoryChangeReviewDialog.parseCandidate(task.number, task.body, registry)
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        return candidate
    }
}
