package com.hermes.assistant

/** Search and status filtering for the full-screen live repository task board. */
internal object VyomFactoryLiveTaskBoardPolicy {
    enum class Filter { ALL, ACTIVE, BLOCKED, COMPLETED }

    data class Counts(
        val total: Int,
        val active: Int,
        val blocked: Int,
        val completed: Int
    )

    fun filter(
        tasks: List<VyomFactoryGitHubClient.Task>,
        filter: Filter,
        query: String
    ): List<VyomFactoryGitHubClient.Task> {
        val needle = query.trim().lowercase()
        return tasks.filter { task ->
            val status = task.metadata.status.lowercase()
            val matchesFilter = when (filter) {
                Filter.ALL -> true
                Filter.ACTIVE -> status in ACTIVE_STATUSES
                Filter.BLOCKED -> status == "blocked"
                Filter.COMPLETED -> status == "completed"
            }
            val searchable = listOf(
                task.number.toString(), task.title, task.body,
                task.metadata.priority, task.metadata.status, task.metadata.lane.name
            ).joinToString(" ").lowercase()
            matchesFilter && (needle.isBlank() || searchable.contains(needle))
        }
    }

    fun counts(tasks: List<VyomFactoryGitHubClient.Task>): Counts = Counts(
        total = tasks.size,
        active = tasks.count { it.metadata.status.lowercase() in ACTIVE_STATUSES },
        blocked = tasks.count { it.metadata.status.equals("blocked", ignoreCase = true) },
        completed = tasks.count { it.metadata.status.equals("completed", ignoreCase = true) }
    )

    private val ACTIVE_STATUSES = setOf("open", "in_progress")
}
