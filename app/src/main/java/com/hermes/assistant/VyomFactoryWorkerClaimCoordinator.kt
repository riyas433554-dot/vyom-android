package com.hermes.assistant

/** Device-local task selection; the supplied worker identity is lane/slot-bound. */
internal class VyomFactoryWorkerClaimCoordinator(
    private val taskStore: VyomFactoryTaskStore,
    private val leaseStore: VyomFactoryWorkerLeaseStore
) {
    data class Claim(
        val task: VyomFactoryTaskStore.QueuedTask,
        val remoteTask: VyomFactoryGitHubClient.Task,
        val lease: VyomFactoryWorkerLeaseStore.Lease
    )

    fun claimNext(
        remoteTasks: List<VyomFactoryGitHubClient.Task>,
        worker: VyomFactoryWorkerIdentity,
        now: Long,
        durationMs: Long
    ): Claim? = synchronized(CLAIM_LOCK) {
        val remoteByIssue = remoteTasks.asSequence()
            .filter {
                it.state == "open" && it.metadata.status in setOf("open", "in_progress") &&
                    it.metadata.lane == worker.lane && it.metadata.lanePlanningAllowed
            }
            .associateBy { it.number }
        val candidates = taskStore.pending().mapNotNull { queued ->
            val remote = remoteByIssue[queued.issueNumber] ?: return@mapNotNull null
            if (remote.metadata.sourceId != queued.sourceId) return@mapNotNull null
            if (queued.lane != worker.lane || !queued.lanePlanningAllowed) return@mapNotNull null
            queued to remote
        }
        val queuedByIssue = candidates.associateBy { it.second.number }
        for (remote in VyomFactoryTaskOrdering.sort(candidates.map { it.second })) {
            val queued = queuedByIssue[remote.number]?.first ?: continue
            val lease = leaseStore.claim(queued.issueNumber, worker, now, durationMs) ?: continue
            if (leaseStore.active(queued.issueNumber, now) != lease || !taskStore.contains(queued.issueNumber)) {
                leaseStore.release(lease)
                continue
            }
            return@synchronized Claim(queued, remote, lease)
        }
        null
    }

    fun isCurrent(claim: Claim, now: Long): Boolean =
        taskStore.contains(claim.task.issueNumber) && leaseStore.isCurrent(claim.lease, now)

    fun activeClaimedIssueNumbers(now: Long): Set<Int> = leaseStore.activeIssueNumbers(now)

    fun renew(claim: Claim, now: Long, durationMs: Long): Boolean =
        taskStore.contains(claim.task.issueNumber) && leaseStore.renew(claim.lease, now, durationMs) != null

    fun release(claim: Claim): Boolean = leaseStore.release(claim.lease)

    private companion object { val CLAIM_LOCK = Any() }
}
