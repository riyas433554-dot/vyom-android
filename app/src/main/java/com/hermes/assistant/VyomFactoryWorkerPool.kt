package com.hermes.assistant

/**
 * Persistent four-lane worker session registry.
 *
 * This provisions stable identities and claim sessions only. It does not start
 * background threads or bypass the execution runner's consent/STOP/verification gates.
 */
internal class VyomFactoryWorkerPool(
    identityStore: VyomFactoryWorkerIdentityStore,
    private val taskStore: VyomFactoryTaskStore,
    private val coordinator: VyomFactoryWorkerClaimCoordinator,
    private val fetchRemoteTasks: () -> List<VyomFactoryGitHubClient.Task>,
    private val now: () -> Long = System::currentTimeMillis,
    val leaseDurationMs: Long = VyomFactoryWorkerSession.DEFAULT_LEASE_DURATION_MS
) {
    private val identities = identityStore.ensurePool()

    val sessions: List<VyomFactoryWorkerSession> = identities.map { identity ->
        VyomFactoryWorkerSession(
            taskStore = taskStore,
            coordinator = coordinator,
            workerIdentity = identity,
            fetchRemoteTasks = fetchRemoteTasks,
            now = now,
            leaseDurationMs = leaseDurationMs
        )
    }

    val workerCount: Int = identities.size
    val concurrencyLimit: Int = VyomFactoryWorkerPoolConfig.concurrencyLimit(identities)

    init {
        require(workerCount >= VyomFactoryWorkerPoolConfig.totalWorkerCount) {
            "Worker pool is below the four-lane minimum"
        }
    }

    fun sessionsForLane(lane: VyomFactoryLaneRouter.Lane): List<VyomFactoryWorkerSession> =
        sessions.filter { it.workerIdentity.lane == lane }
}
