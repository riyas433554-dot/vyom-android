package com.hermes.assistant

/**
 * Selects and leases one task from this device's durable Factory inbox.
 *
 * The caller must supply a fresh GitHub snapshot. This class never mutates GitHub
 * status because a device-local SharedPreferences lease is not a cross-device CAS.
 * It guarantees only that competing Vyom workers in this app process/device cannot
 * both receive the same unexpired local claim.
 */
internal class VyomFactoryWorkerClaimCoordinator(
    private val taskStore: VyomFactoryTaskStore,
    private val leaseStore: VyomFactoryWorkerLeaseStore
) {
    data class Claim(
        val task: VyomFactoryTaskStore.QueuedTask,
        val remoteTask: VyomFactoryGitHubClient.Task,
        val lease: VyomFactoryWorkerLeaseStore.Lease
    )

    /**
     * Claims the highest-priority queued task that still matches a fresh, open
     * GitHub issue. Returns null when no eligible task is available.
     */
    fun claimNext(
        remoteTasks: List<VyomFactoryGitHubClient.Task>,
        ownerId: String,
        now: Long,
        durationMs: Long
    ): Claim? = synchronized(CLAIM_LOCK) {
        require(ownerId.isNotBlank())
        val remoteByIssue = remoteTasks
            .asSequence()
            .filter { it.state == "open" && it.metadata.status == "open" }
            .associateBy { it.number }

        val candidates = taskStore.pending().mapNotNull { queued ->
            val remote = remoteByIssue[queued.issueNumber] ?: return@mapNotNull null
            if (remote.metadata.sourceId != queued.sourceId) return@mapNotNull null
            queued to remote
        }
        val queuedByIssue = candidates.associateBy { it.second.number }
        val orderedRemoteTasks = VyomFactoryTaskOrdering.sort(candidates.map { it.second })

        for (remote in orderedRemoteTasks) {
            val queued = queuedByIssue[remote.number]?.first ?: continue
            val lease = leaseStore.claim(queued.issueNumber, ownerId, now, durationMs) ?: continue
            val persisted = leaseStore.active(queued.issueNumber, now)
            if (persisted != lease || !taskStore.contains(queued.issueNumber)) {
                leaseStore.release(lease)
                continue
            }
            return@synchronized Claim(queued, remote, lease)
        }
        null
    }

    /** Revalidates the exact lease token and confirms the task remains in the inbox. */
    fun isCurrent(claim: Claim, now: Long): Boolean =
        taskStore.contains(claim.task.issueNumber) &&
            leaseStore.active(claim.task.issueNumber, now) == claim.lease

    /** Releases only this claim's fenced lease; the task remains queued for retry. */
    fun release(claim: Claim): Boolean = leaseStore.release(claim.lease)

    private companion object {
        val CLAIM_LOCK = Any()
    }
}
