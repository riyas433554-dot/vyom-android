package com.hermes.assistant

/** Fresh-snapshot claim boundary for one persistent lane-bound worker identity. */
internal class VyomFactoryWorkerSession(
    private val taskStore: VyomFactoryTaskStore,
    private val coordinator: VyomFactoryWorkerClaimCoordinator,
    val workerIdentity: VyomFactoryWorkerIdentity,
    private val fetchRemoteTasks: () -> List<VyomFactoryGitHubClient.Task>,
    private val now: () -> Long = System::currentTimeMillis,
    private val leaseDurationMs: Long = DEFAULT_LEASE_DURATION_MS
) {
    init {
        require(VyomFactoryWorkerIdentityStore.isValidWorkerId(workerIdentity.workerId))
        require(workerIdentity.slotIndex in 0 until VyomFactoryWorkerPoolConfig.MINIMUM_WORKERS_PER_LANE)
        require(leaseDurationMs in MIN_LEASE_DURATION_MS..MAX_LEASE_DURATION_MS)
    }

    fun claimNext(eligible: (VyomFactoryGitHubClient.Task) -> Boolean = { true }): VyomFactoryWorkerClaimCoordinator.Claim? {
        val snapshot = fetchRemoteTasks()
        val timestamp = now()
        val claimable = snapshot.filter {
            it.state == "open" && it.metadata.status in setOf("open", "in_progress")
        }
        taskStore.reconcileOpen(claimable.map { remote ->
            VyomFactoryTaskStore.QueuedTask(
                issueNumber = remote.number,
                title = remote.title,
                priority = remote.metadata.priority,
                sourceId = remote.metadata.sourceId,
                enqueuedAt = timestamp
            )
        })
        val cycles = VyomFactoryTaskIntelligence.circularDependencyIssues(claimable)
        val eligibleTasks = VyomFactoryTaskOrdering.sort(claimable).filter { task ->
            task.number !in cycles && !VyomFactoryTaskIntelligence.isIterationLimitReached(task) && eligible(task)
        }
        return coordinator.claimNext(eligibleTasks, workerIdentity, timestamp, leaseDurationMs)
    }

    fun isCurrent(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean = coordinator.isCurrent(claim, now())
    fun activeClaimedIssueNumbers(): Set<Int> = coordinator.activeClaimedIssueNumbers(now())
    fun renewAndCheck(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean =
        coordinator.renew(claim, now(), leaseDurationMs)
    fun release(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean = coordinator.release(claim)

    companion object {
        const val DEFAULT_LEASE_DURATION_MS = 60_000L
        const val MIN_LEASE_DURATION_MS = 5_000L
        const val MAX_LEASE_DURATION_MS = 60 * 60 * 1_000L
    }
}
