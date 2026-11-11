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
        require(heartbeatIntervalMillis in 1L..VyomFactoryWorkerSession.MAX_LEASE_DURATION_MS)
    }

    fun dispatchAvailable(): Result {
        if (isStopped()) return Result(emptyList(), stopped = true)
        if (!isAuthorized()) return Result(emptyList(), authorizationRequired = true)
        val snapshot = fetchRemoteTasks()
        if (isStopped()) return Result(emptyList(), stopped = true)
        val eligibleNumbers = snapshot.asSequence()
            .filter { task ->
                task.state == "open" &&
                    task.metadata.status in setOf("open", "in_progress") &&
                    task.metadata.lanePlanningAllowed &&
                    (VyomFactoryTaskClassifier.classify(task) as? VyomFactoryTaskClassifier.Result.Classified)
                        ?.domain == VyomFactoryCapabilityRegistry.Domain.REPOSITORY &&
                    shouldDispatch(task)
            }
            .map { it.number }
            .toSet()
        if (eligibleNumbers.isEmpty()) return Result(emptyList())

        val executor = Executors.newFixedThreadPool(pool.concurrencyLimit)
        val heartbeats = Executors.newScheduledThreadPool(minOf(4, pool.concurrencyLimit).coerceAtLeast(1))
        try {
            val futures = pool.sessions.map { session ->
                executor.submit<WorkerOutcome> {
                    dispatchForSession(session, snapshot, eligibleNumbers, heartbeats)
                }
            }
            val outcomes = futures.map { future ->
                try {
                    future.get()
                } catch (error: Exception) {
                    WorkerOutcome("pool", null, State.FAILED, error.javaClass.simpleName, isRetryable(error))
                }
            }
            return Result(outcomes, stopped = isStopped())
        } finally {
            executor.shutdownNow()
            heartbeats.shutdownNow()
        }
    }

    private fun dispatchForSession(
        session: VyomFactoryWorkerSession,
        snapshot: List<VyomFactoryGitHubClient.Task>,
        eligibleNumbers: Set<Int>,
        heartbeats: java.util.concurrent.ScheduledExecutorService
    ): WorkerOutcome {
        val workerId = session.workerIdentity.workerId
        if (isStopped()) return WorkerOutcome(workerId, null, State.STOPPED)
        val claim = try {
            session.claimNext(snapshot) { it.number in eligibleNumbers }
        } catch (error: Exception) {
            return WorkerOutcome(workerId, null, State.FAILED, error.javaClass.simpleName, isRetryable(error))
        } ?: return WorkerOutcome(workerId, null, State.NO_WORK)

        val leaseLost = AtomicBoolean(false)
        val heartbeat: ScheduledFuture<*> = heartbeats.scheduleAtFixedRate(
            {
                if (!session.renewAndCheck(claim)) leaseLost.set(true)
            },
            heartbeatIntervalMillis,
            heartbeatIntervalMillis,
            TimeUnit.MILLISECONDS
        )
        val control = object : VyomFactoryExecutionControl {
            override fun shouldStop(): Boolean =
                isStopped() || leaseLost.get() || !session.isCurrent(claim)
        }
        try {
            if (control.shouldStop()) return WorkerOutcome(workerId, claim.task.issueNumber, State.STOPPED)
            val candidate = modelAdapterFactory().propose(claim.remoteTask, registry, control)
            if (control.shouldStop()) return WorkerOutcome(workerId, claim.task.issueNumber, State.FENCED)
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
