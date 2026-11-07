package com.hermes.assistant

/** Pure queue ordering and identity policy, independently testable on the JVM. */
internal object VyomFactoryTaskQueuePolicy {
    fun order(tasks: List<VyomFactoryTaskStore.QueuedTask>): List<VyomFactoryTaskStore.QueuedTask> =
        tasks.sortedWith(compareBy<VyomFactoryTaskStore.QueuedTask> {
            when (it.priority) { "P0" -> 0; "P1" -> 1; "P2" -> 2; else -> 3 }
        }.thenBy { it.enqueuedAt })

    fun isUnique(existing: List<VyomFactoryTaskStore.QueuedTask>,
                 candidate: VyomFactoryTaskStore.QueuedTask): Boolean =
        existing.none { it.issueNumber == candidate.issueNumber || it.sourceId == candidate.sourceId }
}
