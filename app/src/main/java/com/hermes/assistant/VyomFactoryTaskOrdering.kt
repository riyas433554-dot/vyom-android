package com.hermes.assistant

/** Canonical inbox order: dependencies first, then owner priority/FIFO among ready tasks. */
internal object VyomFactoryTaskOrdering {
    fun sort(tasks: List<VyomFactoryGitHubClient.Task>): List<VyomFactoryGitHubClient.Task> =
        VyomFactoryTaskIntelligence.order(tasks)
}
