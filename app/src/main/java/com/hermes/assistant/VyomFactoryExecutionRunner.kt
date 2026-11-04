package com.hermes.assistant

/**
 * One-at-a-time foreground execution coordinator.
 *
 * Only validated envelopes with registered adapters are eligible. Adapters
 * must be bounded, check shouldStop() before every side effect, and truthfully
 * declare idempotency. This is not an autonomous background worker.
 */
internal class VyomFactoryExecutionRunner(
    private val claimNext: () -> VyomFactoryWorkerClaimCoordinator.Claim?,
    private val isClaimCurrent: (VyomFactoryWorkerClaimCoordinator.Claim) -> Boolean,
    private val releaseClaim: (VyomFactoryWorkerClaimCoordinator.Claim) -> Boolean,
    private val fetchFreshTask: (Int) -> VyomFactoryGitHubClient.Task?,
    private val journal: VyomFactoryExecutionJournal,
    private val adapters: VyomFactoryExecutionAdapterRegistry,
    private val retrySchedules: VyomFactoryRetryScheduleStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val newAttemptId: () -> String,
    private val prepareExecution: (VyomFactoryExecutionEnvelope) -> VyomFactoryExecutionEnvelope? = { it },
    private val externalStopRequested: () -> Boolean = { false }
) {
    sealed class Outcome {
        data class Succeeded(val issueNumber: Int, val attemptId: String, val result: String, val retryScheduleCleared: Boolean) : Outcome()
        data class Failed(val issueNumber: Int, val attemptId: String, val category: String) : Outcome()
        data class Skipped(val issueNumber: Int, val reason: String) : Outcome()
        object NoWork : Outcome()
        data class Fenced(val issueNumber: Int) : Outcome()
    }

    fun runNext(): Outcome {
        val claim = claimNext() ?: return Outcome.NoWork
        val issue = claim.task.issueNumber
        fun skip(reason: String): Outcome.Skipped {
            releaseClaim(claim)
            return Outcome.Skipped(issue, reason)
        }

        if (!isClaimCurrent(claim)) return Outcome.Fenced(issue)
        val remote = fetchFreshTask(issue) ?: return skip("task_missing")
        if (remote.number != issue || remote.state != "open" ||
            remote.metadata.status != "open" ||
            remote.metadata.sourceId != claim.task.sourceId
        ) return skip("task_no_longer_open_or_identity_changed")

        val envelope = VyomFactoryExecutionEnvelope.parse(remote.body)
        if (!envelope.isExecutable || envelope.target == VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW) {
            return skip("manual_review_required")
        }
        if (!adapters.supports(envelope.target)) return skip("adapter_unsupported")
        if (!isClaimCurrent(claim)) return Outcome.Fenced(issue)
        if (externalStopRequested()) return skip("stopped_before_execution")

        // Expired leases never authorize replay. Only a persisted due retry for
        // this exact issue/operation and an adapter still declaring idempotency can retry.
        val operationKey = envelope.operationKey()
        val history = journal.all().filter { it.issueNumber == issue }
        if (history.any { it.operationKey == "legacy:unknown" }) {
            return skip("legacy_attempt_requires_review")
        }
        val previous = history.lastOrNull { it.operationKey == operationKey }
        if (previous != null) {
            when (previous.state) {
                VyomFactoryExecutionJournal.State.SUCCEEDED ->
                    return skip("previous_attempt_succeeded")
                VyomFactoryExecutionJournal.State.FAILED -> {
                    val retry = retrySchedules.all().firstOrNull {
                        it.issueNumber == issue && it.operationKey == envelope.operationKey()
                    }
                    if (!adapters.isIdempotent(envelope.target) || retry == null ||
                        retry.nextAttemptAt > now()
                    ) return skip("retry_not_due_or_manual_review_required")
                }
                VyomFactoryExecutionJournal.State.INTERRUPTED ->
                    return skip("interrupted_attempt_requires_manual_review")
                VyomFactoryExecutionJournal.State.CLAIMED,
                VyomFactoryExecutionJournal.State.RUNNING,
                VyomFactoryExecutionJournal.State.STOP_REQUESTED ->
                    return skip("previous_attempt_still_active")
            }
        }

        // Consent is collected before an attempt is journaled. A declined or
        // unavailable foreground prompt leaves the task queued and replayable.
        val prepared = try {
            prepareExecution(envelope)
        } catch (_: Exception) {
            null
        } ?: return if (externalStopRequested()) skip("stopped_before_execution") else skip("confirmation_declined_or_unavailable")
        if (!isValidPreparedEnvelope(envelope, prepared)) {
            return skip("invalid_prepared_execution")
        }
        if (!isClaimCurrent(claim)) return Outcome.Fenced(issue)

        val attemptId = newAttemptId()
        journal.begin(claim.lease, attemptId, now(), operationKey)
        if (!journal.markRunning(claim.lease, attemptId, now())) return Outcome.Fenced(issue)

        val control = object : VyomFactoryExecutionControl {
            override fun shouldStop(): Boolean {
                val entry = journal.get(attemptId)
                return entry?.state == VyomFactoryExecutionJournal.State.STOP_REQUESTED ||
                    entry?.isTerminal != false || externalStopRequested() || !isClaimCurrent(claim)
            }
        }

        return try {
            if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
            val result = when (val dispatched = adapters.dispatch(prepared, control)) {
                is VyomFactoryExecutionAdapterRegistry.Outcome.Executed -> dispatched.result
                VyomFactoryExecutionAdapterRegistry.Outcome.ManualReview ->
                    throw IllegalStateException("Execution envelope requires manual review")
                is VyomFactoryExecutionAdapterRegistry.Outcome.Unsupported ->
                    throw IllegalStateException("No adapter for ${dispatched.target}")
            }
            if (!adapters.isVerifiedResult(envelope.target, result)) {
                throw IllegalStateException("Adapter returned without verifying the requested postcondition")
            }
            // The adapter has completed and verified its side effect. A STOP
            // arriving now cannot undo it and must not rewrite verified success.
            if (!journal.succeed(claim.lease, attemptId, now(), result)) {
                Outcome.Fenced(issue)
            } else {
                // Do not rewrite an already successful side effect as FAILED if cleanup fails.
                val cleared = runCatching {
                    retrySchedules.clear(issue, envelope.operationKey())
                    true
                }.getOrDefault(false)
                // A terminal journal record is durable before the exact claim is released.
                releaseTerminalClaim(claim)
                Outcome.Succeeded(issue, attemptId, result, retryScheduleCleared = cleared)
            }
        } catch (error: Exception) {
            val category = when (error) {
                is VyomFactoryExecutionStoppedException -> "stopped"
                is VyomFactoryGitHubClient.FactoryApiException -> when (error.code) {
                    401, 403 -> "permission_denied"
                    429 -> "rate_limited"
                    in 500..599 -> "server_unavailable"
                    else -> "github_rejected"
                }
                is java.io.IOException -> "network_timeout"
                else -> "execution_error"
            }
            val message = error.message?.take(1_000)?.ifBlank { category } ?: category
            if (!journal.fail(claim.lease, attemptId, now(), category, message)) {
                return Outcome.Fenced(issue)
            }
            if (category != "stopped") {
                // Retry persistence is best-effort after the durable FAILED journal entry.
                // A storage failure must not strand the terminal worker claim.
                runCatching {
                    retrySchedules.recordFailure(
                        issueNumber = issue,
                        operationKey = envelope.operationKey(),
                        errorCategory = category,
                        now = now(),
                        adapterIdempotent = adapters.isIdempotent(envelope.target)
                    )
                }
            }
            releaseTerminalClaim(claim)
            Outcome.Failed(issue, attemptId, category)
        }
    }

    private fun isValidPreparedEnvelope(
        original: VyomFactoryExecutionEnvelope,
        prepared: VyomFactoryExecutionEnvelope
    ): Boolean {
        if (!prepared.isExecutable || prepared.target != original.target ||
            prepared.operation != original.operation ||
            prepared.originalRequest != original.originalRequest
        ) return false

        val originalArguments = org.json.JSONObject(original.arguments.toString())
        val preparedArguments = org.json.JSONObject(prepared.arguments.toString())
        if (original.target == VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION &&
            original.operation == "set_media_volume"
        ) {
            if (originalArguments.has("confirmation_id")) return false
            val approvalId = preparedArguments.opt("confirmation_id") as? String
            if (approvalId.isNullOrBlank() || approvalId.length > 128) return false
            preparedArguments.remove("confirmation_id")
        }
        return preparedArguments.toString() == originalArguments.toString()
    }

    private fun releaseTerminalClaim(claim: VyomFactoryWorkerClaimCoordinator.Claim) {
        // Release is fenced by the exact lease token; cleanup failure never rewrites
        // an already-persisted terminal execution result.
        runCatching { releaseClaim(claim) }
    }

    private fun VyomFactoryExecutionEnvelope.operationKey(): String =
        "${target.name.lowercase()}:$operation"
}
