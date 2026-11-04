package com.hermes.assistant

/** Canonical inbox order: priority tier first, then newest owner promotion/update, then issue number. */
internal object VyomFactoryTaskOrdering {
    private val priorityRank = mapOf("P0" to 0, "P1" to 1, "P2" to 2, "P3" to 3)

    fun sort(tasks: List<VyomFactoryGitHubClient.Task>): List<VyomFactoryGitHubClient.Task> =
        tasks.sortedWith(
            compareBy<VyomFactoryGitHubClient.Task> { priorityRank[it.metadata.priority] ?: 4 }
                .thenByDescending { it.updatedAt }
                .thenBy { it.number }
        )
}
