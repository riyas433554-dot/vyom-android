package com.hermes.assistant

/**
 * Read-only remote inbox reconciliation. It never claims a lease, invokes a model,
 * issues approval, or writes to the remote task registry.
 */
internal class VyomFactoryQueueRefresh(
    private val taskStore: VyomFactoryTaskStore,
    private val fetchRemoteTasks: () -> List<VyomFactoryGitHubClient.Task>,
    private val now: () -> Long = System::currentTimeMillis
) {
    fun refresh(): Int {
        val timestamp = now()
        val openTasks = fetchRemoteTasks().filter { task ->
            task.state == "open" && task.metadata.status in ELIGIBLE_STATUSES
        }
        return taskStore.reconcileOpen(openTasks.map { remote ->
            VyomFactoryTaskStore.QueuedTask(
                issueNumber = remote.number,
                title = remote.title,
                priority = remote.metadata.priority,
                sourceId = remote.metadata.sourceId,
                enqueuedAt = timestamp,
                lane = remote.metadata.lane,
                lanePlanningAllowed = remote.metadata.lanePlanningAllowed
            )
        })
    }

    companion object {
        private val ELIGIBLE_STATUSES = setOf("open", "in_progress")
    }
}
