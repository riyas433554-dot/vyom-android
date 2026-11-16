package com.hermes.assistant

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** App-closed, network-constrained Factory worker. Autonomous writes remain separately gated. */
internal class VyomFactoryWorkerDispatchWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val context = applicationContext
        fun dispatchData(reason: String, result: VyomFactoryWorkerPoolDispatcher.Result? = null) =
            androidx.work.workDataOf(
                "reason" to reason,
                "skipped_tasks" to result?.skippedTasks.orEmpty().joinToString("\n") {
                    "#${it.issueNumber} · ${it.reason}"
                },
                "proposed_count" to (result?.proposedCount ?: 0),
                "repaired_count" to (result?.repairedCount ?: 0),
                "failed_count" to (result?.failedCount ?: 0),
                "retryable_failure_count" to (result?.retryableFailureCount ?: 0)
            )
        return try {
            val stopStore = VyomFactoryWorkerStopStore(context)
            if (stopStore.isStopped()) return Result.success(dispatchData("GLOBAL_STOP_ACTIVE"))
            if (!VyomFactoryAutonomousExecutionAuthorizationRuntime.isEnabled(context)) return Result.success(dispatchData("AUTONOMOUS_EXECUTION_DISABLED"))
            if (!VyomFactoryWorkerLiveCallAuthorizationRuntime.isEnabled(context)) return Result.failure(
                androidx.work.workDataOf("reason" to "OWNER_AUTHORIZATION_REQUIRED")
            )

            val token = GitHubReleaseTokenStore.read(context)
            if (token.isBlank()) return Result.failure(
                androidx.work.workDataOf("reason" to "GITHUB_TOKEN_MISSING")
            )
            val registry = VyomFactorySystemRegistry(File(context.filesDir, "factory-system-registry"))
            if (!registry.allowsPath(
                    VyomFactoryGitHubClient.SYSTEM_ID,
                    VyomFactoryGitHubClient.ISSUES_PATH
                )
            ) return Result.failure(
                androidx.work.workDataOf("reason" to "TASK_REGISTRY_ISSUES_PERMISSION_OR_BINDING_MISSING")
            )
            if (registry.get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID) == null) return Result.failure(
                androidx.work.workDataOf("reason" to "WORKER_REPOSITORY_NOT_BOUND")
            )

            val codingPrefs = context.getSharedPreferences(
                VyomGitHubMcpCodingAgentRuntime.PREFS, Context.MODE_PRIVATE
            )
            if (!codingPrefs.getBoolean(VyomGitHubMcpCodingAgentRuntime.KEY_ENABLED, false)) {
                return Result.success(dispatchData("VYOM_MCP_CODING_MODE_DISABLED"))
            }
            val proposalStore = VyomFactoryWorkerProposalStore(File(context.filesDir, PROPOSAL_DIRECTORY))
            if (proposalStore.pending().isNotEmpty()) {
                return Result.success(dispatchData("LEGACY_PROPOSALS_REQUIRE_OWNER_REVIEW"))
            }

            val client = VyomFactoryGitHubClient(token, registry)
            val taskStore = VyomFactoryTaskStore(context)
            val leaseStore = VyomFactoryWorkerLeaseStore(context)
            val pool = VyomFactoryWorkerPool(
                identityStore = VyomFactoryWorkerIdentityStore(context),
                taskStore = taskStore,
                coordinator = VyomFactoryWorkerClaimCoordinator(taskStore, leaseStore),
                fetchRemoteTasks = client::listTasks
            )
            val dispatcher = VyomFactoryWorkerPoolDispatcher(
                pool = pool,
                registry = registry,
                fetchRemoteTasks = client::listTasks,
                modelAdapterFactory = { error("Legacy worker planner is disabled in Vyom MCP coding mode.") },
                codingAgentRunner = { task, control ->
                    setProgressAsync(androidx.work.workDataOf(
                        "runtime_issue_number" to task.number,
                        "runtime_state" to "MCP_CODING_AGENT_STARTED"
                    )).get(10L, TimeUnit.SECONDS)
                    try {
                        val result = VyomGitHubMcpCodingAgentRuntime.run(context, task, registry, control)
                        setProgressAsync(androidx.work.workDataOf(
                            "runtime_issue_number" to task.number,
                            "runtime_state" to "MCP_CODING_AGENT_RETURNED",
                            "runtime_result" to result.take(8_000)
                        )).get(10L, TimeUnit.SECONDS)
                        result
                    } catch (error: Exception) {
                        setProgressAsync(androidx.work.workDataOf(
                            "runtime_issue_number" to task.number,
                            "runtime_state" to "MCP_CODING_AGENT_FAILED",
                            "runtime_error_type" to error.javaClass.simpleName
                        )).get(10L, TimeUnit.SECONDS)
                        throw error
                    }
                },
                onProposal = { _, _, _, _ ->
                    throw IllegalStateException("MCP coding mode must not create legacy worker proposals.")
                },
                isStopped = stopStore::isStopped,
                // Provider planning is read-only and requires live model-call authorization.
                // Autonomous authorization is checked separately before repository writes.
                isAuthorized = {
                    VyomFactoryWorkerLiveCallAuthorizationRuntime.isEnabled(context)
                },
                shouldDispatch = { proposalStore.pending().isEmpty() && !proposalStore.isHandled(it) },
                maxConcurrentSessions = 1,
                dispatchSkipReason = { task ->
                    when {
                        proposalStore.pending().isNotEmpty() -> "PENDING_PROPOSAL_QUEUE"
                        proposalStore.isHandled(task) -> "ALREADY_HANDLED"
                        else -> "ALREADY_HANDLED_OR_PENDING"
                    }
                }
            )
            val dispatched = dispatcher.dispatchAvailable()

            when (VyomFactoryWorkerDispatchResultPolicy.decide(dispatched)) {
                VyomFactoryWorkerDispatchResultPolicy.Decision.SUCCESS -> {
                    val reason = when {
                        stopStore.isStopped() || dispatched.stopped -> "GLOBAL_STOP_ACTIVE"
                        dispatched.repairedCount > 0 -> "REPAIR_EXECUTED"
                        dispatched.reason == "ZERO_ELIGIBLE_TASKS" -> "ZERO_ELIGIBLE_TASKS"
                        dispatched.proposedCount > 0 -> "PROPOSAL_CREATED"
                        else -> dispatched.reason
                    }
                    Result.success(dispatchData(reason, dispatched))
                }
                VyomFactoryWorkerDispatchResultPolicy.Decision.RETRY -> {
                    setProgressAsync(dispatchData("RETRY_PENDING", dispatched))
                    Result.retry()
                }
                VyomFactoryWorkerDispatchResultPolicy.Decision.FAILURE -> Result.failure(
                    dispatchData(
                        if (dispatched.authorizationRequired) "OWNER_AUTHORIZATION_REQUIRED"
                        else "WORKER_DISPATCH_FAILED",
                        dispatched
                    )
                )
            }
        } catch (error: VyomFactoryGitHubClient.FactoryApiException) {
            if (error.code == 429 || error.code >= 500) { setProgressAsync(dispatchData("GITHUB_RETRY_PENDING")); Result.retry() } else Result.failure(dispatchData("GITHUB_API_REJECTED"))
        } catch (_: IOException) {
            setProgressAsync(dispatchData("NETWORK_RETRY_PENDING"))
            Result.retry()
        } catch (_: Exception) {
            Result.failure(dispatchData("WORKER_UNEXPECTED_FAILURE"))
        }
    }

    companion object {
        const val PROPOSAL_DIRECTORY = "factory-worker-proposals"
    }
}

/** Periodic trigger only enqueues the unique one-time worker; it never overlaps task execution. */
internal class VyomFactoryWorkerPeriodicTrigger(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val context = applicationContext
        if (VyomFactoryWorkerStopStore(context).isStopped()) return Result.success()
        if (context.getSharedPreferences(VyomGitHubMcpCodingAgentRuntime.PREFS, Context.MODE_PRIVATE)
                .getBoolean(VyomGitHubMcpCodingAgentRuntime.KEY_ENABLED, false)
        ) return Result.success() // Continuous MCP issue scheduling is enabled only after the Task 9 recovery/duplicate guards.
        if (!VyomFactoryAutonomousExecutionAuthorizationRuntime.isEnabled(context) ||
            VyomFactoryWorkerLiveCallAuthorizationStore(context).read() == null
        ) return Result.success()
        VyomFactoryWorkerDispatchScheduler.enqueue(context)
        return Result.success()
    }
}

/** Unique one-shot dispatch; queue refresh remains a separate read-only operation. */
internal object VyomFactoryWorkerDispatchScheduler {
    internal const val WORK_NAME = "vyom-factory-worker-dispatch-v1"
    internal const val PERIODIC_WORK_NAME = "vyom-factory-worker-dispatch-periodic-v1"

    fun ensurePeriodicScheduled(context: Context) {
        val app = context.applicationContext
        if (app.getSharedPreferences(VyomGitHubMcpCodingAgentRuntime.PREFS, Context.MODE_PRIVATE)
                .getBoolean(VyomGitHubMcpCodingAgentRuntime.KEY_ENABLED, false) ||
            !VyomFactoryAutonomousExecutionAuthorizationRuntime.isEnabled(app) ||
            VyomFactoryWorkerLiveCallAuthorizationStore(app).read() == null) {
            WorkManager.getInstance(app).cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<VyomFactoryWorkerPeriodicTrigger>(15L, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    /** Initial/manual enqueue does not create duplicate chains when the app is reopened. */
    fun enqueue(context: Context) = enqueueWork(context, ExistingWorkPolicy.KEEP)

    /** A successful task appends exactly one next cycle after the current worker finishes. */
    fun enqueueContinuation(context: Context) = enqueueWork(context, ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueueWork(context: Context, policy: ExistingWorkPolicy) {
        val app = context.applicationContext
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<VyomFactoryWorkerDispatchWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        // This is a local, non-blocking presence check. The worker performs full
        // network-backed README/config validation off the UI thread before any provider call.
        if (!VyomFactoryAutonomousExecutionAuthorizationRuntime.isEnabled(app) ||
            VyomFactoryWorkerLiveCallAuthorizationStore(app).read() == null) return
        WorkManager.getInstance(app).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        WorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(app).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
