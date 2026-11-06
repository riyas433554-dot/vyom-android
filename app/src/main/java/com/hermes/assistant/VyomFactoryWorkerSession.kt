package com.hermes.assistant

/**
 * Synchronous claim boundary for a foreground Factory consumer.
 *
 * Every claim starts with a fresh GitHub snapshot. Only currently-open remote
 * tasks are reconciled into the device inbox, then the coordinator acquires a
 * fenced device-local lease. This is not a task executor or a distributed lock.
 */
internal class VyomFactoryWorkerSession(
    private val taskStore: VyomFactoryTaskStore,
    private val coordinator: VyomFactoryWorkerClaimCoordinator,
    private val ownerId: String,
    private val fetchRemoteTasks: () -> List<VyomFactoryGitHubClient.Task>,
    private val now: () -> Long = System::currentTimeMillis,
    private val leaseDurationMs: Long = DEFAULT_LEASE_DURATION_MS
) {
    init {
        require(ownerId.isNotBlank())
        require(leaseDurationMs in MIN_LEASE_DURATION_MS..MAX_LEASE_DURATION_MS)
    }

    /**
     * Refreshes the shared registry before every claim. Network/read failure is
     * propagated; no claim is created from stale cached GitHub data.
     */
    fun claimNext(eligible: (VyomFactoryGitHubClient.Task) -> Boolean = { true }): VyomFactoryWorkerClaimCoordinator.Claim? {
        val snapshot = fetchRemoteTasks()
        val timestamp = now()
        val claimableTasks = snapshot.filter {
            it.state == "open" && it.metadata.status in setOf("open", "in_progress")
        }
        taskStore.reconcileOpen(claimableTasks.map { remote ->
            VyomFactoryTaskStore.QueuedTask(
                issueNumber = remote.number,
                title = remote.title,
                priority = remote.metadata.priority,
                sourceId = remote.metadata.sourceId,
                enqueuedAt = timestamp
            )
        })
        return coordinator.claimNext(snapshot.filter(eligible), ownerId, timestamp, leaseDurationMs)
    }

    fun isCurrent(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean =
        coordinator.isCurrent(claim, now())

    /**
     * Renews the exact live lease at runner checkpoints. Adapters must call
     * shouldStop() regularly; a blocked adapter cannot be heartbeated here.
     */
    fun renewAndCheck(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean =
        coordinator.renew(claim, now(), leaseDurationMs)

    /** Releases only this session's exact lease; the inbox entry stays queued. */
    fun release(claim: VyomFactoryWorkerClaimCoordinator.Claim): Boolean =
        coordinator.release(claim)

    companion object {
        const val DEFAULT_LEASE_DURATION_MS = 60_000L
        const val MIN_LEASE_DURATION_MS = 5_000L
        const val MAX_LEASE_DURATION_MS = 60 * 60 * 1_000L
    }
}
