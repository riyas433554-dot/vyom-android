package com.hermes.assistant

import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Dispatches only owner-authorized repository planning; proposals remain read-only until local review. */
internal class VyomFactoryWorkerPoolDispatcher(
    private val pool: VyomFactoryWorkerPool,
    private val registry: VyomFactorySystemRegistry,
    private val fetchRemoteTasks: () -> List<VyomFactoryGitHubClient.Task>,
    private val modelAdapterFactory: () -> VyomFactoryRepositoryWorkerModelAdapter,
    private val onProposal: (
        VyomFactoryGitHubClient.Task,
        String,
        String,
        VyomFactoryRepositoryChangeReviewDialog.Candidate
    ) -> Unit,
    private val isStopped: () -> Boolean,
    private val isAuthorized: () -> Boolean = { true },
    private val shouldDispatch: (VyomFactoryGitHubClient.Task) -> Boolean = { true },
    private val heartbeatIntervalMillis: Long = DEFAULT_HEARTBEAT_INTERVAL_MS
) {
    enum class State { PROPOSED, NO_WORK, FAILED, STOPPED, FENCED }

    data class WorkerOutcome(
        val workerId: String,
        val issueNumber: Int?,
        val state: State,
        val errorType: String? = null,
        val retryable: Boolean = false
    )

    data class Result(
        val outcomes: List<WorkerOutcome>,
        val authorizationRequired: Boolean = false,
        val stopped: Boolean = false
    ) {
        val proposedCount: Int get() = outcomes.count { it.state == State.PROPOSED }
        val failedCount: Int get() = outcomes.count { it.state == State.FAILED }
        val retryableFailureCount: Int get() = outcomes.count { it.state == State.FAILED && it.retryable }
    }

    init {
        require(heartbeatIntervalMillis in 1L until pool.leaseDurationMs) {
            "Worker heartbeat must run before the configured lease expires"
        }
    }

    private fun stopRequested(): Boolean = runCatching(isStopped).getOrDefault(true)
    private fun authorizationValid(): Boolean = runCatching(isAuthorized).getOrDefault(false)

    fun dispatchAvailable(): Result {
        if (stopRequested()) return Result(emptyList(), stopped = true)
        if (!authorizationValid()) return Result(emptyList(), authorizationRequired = true)
        val snapshot = fetchRemoteTasks()
        if (stopRequested()) return Result(emptyList(), stopped = true)
        if (snapshot.none { isEligibleTask(it, snapshot) }) return Result(emptyList())

        val executor = Executors.newFixedThreadPool(pool.concurrencyLimit)
        val heartbeats = Executors.newScheduledThreadPool(minOf(4, pool.concurrencyLimit).coerceAtLeast(1))
        try {
            // Claim and mark an issue as attempted atomically. A fast failure/release
            // must not let the next session immediately reclaim the same issue.
            val claimLock = Any()
            val attemptedIssueNumbers = mutableSetOf<Int>()
            val futures = pool.sessions.map { session ->
                executor.submit<WorkerOutcome> {
                    dispatchForSession(
                        session, attemptedIssueNumbers, claimLock, heartbeats
                    )
                }
            }
            val outcomes = futures.map { future ->
                try {
                    future.get()
                } catch (error: Exception) {
                    WorkerOutcome("pool", null, State.FAILED, error.javaClass.simpleName, isRetryable(error))
                }
            }
            return Result(outcomes, stopped = stopRequested() || outcomes.any { it.state == State.STOPPED })
        } finally {
            executor.shutdownNow()
            heartbeats.shutdownNow()
        }
    }

    private fun isEligibleTask(task: VyomFactoryGitHubClient.Task, snapshot: List<VyomFactoryGitHubClient.Task>): Boolean =
        task.state == "open" &&
            task.metadata.status in setOf("open", "in_progress") &&
            task.metadata.lanePlanningAllowed &&
            (VyomFactoryTaskClassifier.classify(task) as? VyomFactoryTaskClassifier.Result.Classified)
                ?.domain == VyomFactoryCapabilityRegistry.Domain.REPOSITORY &&
            shouldDispatch(task)

    private fun dispatchForSession(
        session: VyomFactoryWorkerSession,
        attemptedIssueNumbers: MutableSet<Int>,
        claimLock: Any,
        heartbeats: java.util.concurrent.ScheduledExecutorService
    ): WorkerOutcome {
        val workerId = session.workerIdentity.workerId
        if (stopRequested()) return WorkerOutcome(workerId, null, State.STOPPED)
        val claim = try {
            synchronized(claimLock) {
                if (stopRequested()) return WorkerOutcome(workerId, null, State.STOPPED)
                // Refresh the canonical issue registry before every lane-bound claim.
                // A shared start-of-batch snapshot can authorize a stale task.
                val freshSnapshot = fetchRemoteTasks()
                if (stopRequested()) return WorkerOutcome(workerId, null, State.STOPPED)
                val freshEligibleNumbers = freshSnapshot.asSequence()
                    .filter { isEligibleTask(it, freshSnapshot) }
                    .map { it.number }
                    .toSet()
                session.claimNext(freshSnapshot) {
                    it.number in freshEligibleNumbers && it.number !in attemptedIssueNumbers
                }?.also { attemptedIssueNumbers += it.task.issueNumber }
            }
        } catch (error: Exception) {
            return WorkerOutcome(workerId, null, State.FAILED, error.javaClass.simpleName, isRetryable(error))
        } ?: return WorkerOutcome(workerId, null, State.NO_WORK)

        val leaseLost = AtomicBoolean(false)
        val heartbeat: ScheduledFuture<*> = heartbeats.scheduleAtFixedRate(
            {
                try {
                    if (!session.renewAndCheck(claim)) leaseLost.set(true)
                } catch (_: Exception) {
                    leaseLost.set(true)
                }
            },
            heartbeatIntervalMillis,
            heartbeatIntervalMillis,
            TimeUnit.MILLISECONDS
        )
        val control = object : VyomFactoryExecutionControl {
            override fun shouldStop(): Boolean =
                stopRequested() || leaseLost.get() || runCatching { session.isCurrent(claim) }.getOrDefault(false).not()
        }
        try {
            if (control.shouldStop()) return WorkerOutcome(workerId, claim.task.issueNumber, State.STOPPED)
            val candidate = modelAdapterFactory().propose(claim.remoteTask, registry, control)
            if (control.shouldStop()) return WorkerOutcome(workerId, claim.task.issueNumber, State.FENCED)
            // A task may be paused, closed, or edited while the provider is working.
            // Re-read it before persisting the proposal; approval-time checks remain separate.
            val latestTask = fetchRemoteTasks().firstOrNull { it.number == claim.task.issueNumber }
            if (control.shouldStop()) return WorkerOutcome(workerId, claim.task.issueNumber, State.STOPPED)
            if (latestTask == null || !isEligibleTask(latestTask) ||
                latestTask.metadata.sourceId != claim.remoteTask.metadata.sourceId ||
                latestTask.metadata.idempotencyKey != claim.remoteTask.metadata.idempotencyKey ||
                latestTask.metadata.lane != claim.task.lane ||
                latestTask.updatedAt != claim.remoteTask.updatedAt
            ) return WorkerOutcome(workerId, claim.task.issueNumber, State.FENCED)
            val binding = VyomFactoryRepositoryChangePolicy.validateCandidate(candidate.plan, registry)
            require(!candidate.plan.ownerApproved) { "Worker proposal must remain unapproved" }
            require(candidate.plan.issueNumber == claim.task.issueNumber) {
                "Worker proposal issue identity does not match its lease"
            }
            require(candidate.binding == binding) { "Worker proposal binding changed during validation" }
            if (control.shouldStop()) return WorkerOutcome(workerId, claim.task.issueNumber, State.FENCED)
            onProposal(claim.remoteTask, workerId, claim.lease.leaseId, candidate)
            return WorkerOutcome(workerId, claim.task.issueNumber, State.PROPOSED)
        } catch (error: VyomFactoryExecutionStoppedException) {
            return WorkerOutcome(workerId, claim.task.issueNumber, State.STOPPED)
        } catch (error: Exception) {
            return WorkerOutcome(
                workerId,
                claim.task.issueNumber,
                State.FAILED,
                error.javaClass.simpleName,
                isRetryable(error)
            )
        } finally {
            heartbeat.cancel(true)
            session.release(claim)
        }
    }

    private fun isRetryable(error: Throwable): Boolean =
        error is IOException ||
            error is VyomFactoryGitHubClient.FactoryApiException ||
            error.message?.contains("provider failed", ignoreCase = true) == true ||
            error.message?.contains("timed out", ignoreCase = true) == true ||
            error.message?.contains("temporarily unavailable", ignoreCase = true) == true

    companion object {
        const val DEFAULT_HEARTBEAT_INTERVAL_MS = 15_000L
    }
}
