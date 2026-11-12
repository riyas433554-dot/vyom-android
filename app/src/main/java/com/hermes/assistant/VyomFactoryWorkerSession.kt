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

    fun claimNext(eligible: (VyomFactoryGitHubClient.Task) -> Boolean = { true }): VyomFactoryWorkerClaimCoordinator.Claim? =
        claimNext(fetchRemoteTasks(), eligible)

    /** Reuses one fresh canonical snapshot across parallel worker sessions. */
    fun claimNext(
        snapshot: List<VyomFactoryGitHubClient.Task>,
        eligible: (VyomFactoryGitHubClient.Task) -> Boolean = { true }
    ): VyomFactoryWorkerClaimCoordinator.Claim? {
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
                enqueuedAt = timestamp,
                lane = remote.metadata.lane,
                lanePlanningAllowed = remote.metadata.lanePlanningAllowed
            )
        })
        val cycles = VyomFactoryTaskIntelligence.circularDependencyIssues(claimable)
        val dependencyReady = claimable.filter {
            VyomFactoryTaskIntelligence.dependenciesSatisfied(it, snapshot)
        }
        val eligibleTasks = VyomFactoryTaskOrdering.sort(dependencyReady).filter { task ->
            task.metadata.lane == workerIdentity.lane &&
                task.metadata.lanePlanningAllowed &&
                task.number !in cycles &&
                !VyomFactoryTaskIntelligence.isIterationLimitReached(task) &&
                eligible(task)
        }
        return coordinator.claimNext(eligibleTasks, workerIdentity, timestamp, leaseDurationMs)
    }

    fun isCurrent(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean = coordinator.isCurrent(claim, now())
    fun activeClaimedIssueNumbers(): Set<Int> = coordinator.activeClaimedIssueNumbers(now())

    /** Renews the current lease; active dispatchers schedule this continuously while work is running. */
    fun renewAndCheck(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean =
        coordinator.renew(claim, now(), leaseDurationMs)
    fun release(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean = coordinator.release(claim)

    companion object {
        const val DEFAULT_LEASE_DURATION_MS = 60_000L
        const val MIN_LEASE_DURATION_MS = 5_000L
        const val MAX_LEASE_DURATION_MS = 60 * 60 * 1_000L
    }
}
