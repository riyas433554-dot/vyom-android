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
        return try {
            val stopStore = VyomFactoryWorkerStopStore(context)
            if (stopStore.isStopped()) return Result.success()
            if (!VyomFactoryAutonomousExecutionAuthorizationRuntime.isEnabled(context)) return Result.success()
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

            val client = VyomFactoryGitHubClient(token, registry)
            val taskStore = VyomFactoryTaskStore(context)
            val leaseStore = VyomFactoryWorkerLeaseStore(context)
            val pool = VyomFactoryWorkerPool(
                identityStore = VyomFactoryWorkerIdentityStore(context),
                taskStore = taskStore,
                coordinator = VyomFactoryWorkerClaimCoordinator(taskStore, leaseStore),
                fetchRemoteTasks = client::listTasks
            )
            val proposalStore = VyomFactoryWorkerProposalStore(File(context.filesDir, PROPOSAL_DIRECTORY))
            val dispatcher = VyomFactoryWorkerPoolDispatcher(
                pool = pool,
                registry = registry,
                fetchRemoteTasks = client::listTasks,
                modelAdapterFactory = { VyomFactoryWorkerModelRuntime.adapter(context) },
                onProposal = { task, workerId, leaseId, candidate ->
                    proposalStore.save(task, workerId, leaseId, candidate)
                },
                isStopped = stopStore::isStopped,
                isAuthorized = { VyomFactoryWorkerLiveCallAuthorizationRuntime.isEnabled(context) },
                shouldDispatch = { !proposalStore.isHandled(it) }
            )
            val dispatched = dispatcher.dispatchAvailable()
            when (VyomFactoryWorkerDispatchResultPolicy.decide(dispatched)) {
                VyomFactoryWorkerDispatchResultPolicy.Decision.SUCCESS -> {
                    if (dispatched.proposedCount > 0 && !stopStore.isStopped() &&
                        VyomFactoryAutonomousExecutionAuthorizationRuntime.isEnabled(context)) {
                        VyomFactoryWorkerDispatchScheduler.enqueueContinuation(context)
                    }
                    Result.success()
                }
                VyomFactoryWorkerDispatchResultPolicy.Decision.RETRY -> Result.retry()
                VyomFactoryWorkerDispatchResultPolicy.Decision.FAILURE -> Result.failure(
                    androidx.work.workDataOf(
                        "reason" to if (dispatched.authorizationRequired) {
                            "OWNER_AUTHORIZATION_REQUIRED"
                        } else {
                            "WORKER_DISPATCH_FAILED"
                        },
                        "failed_count" to dispatched.failedCount,
                        "retryable_failure_count" to dispatched.retryableFailureCount,
                        "proposed_count" to dispatched.proposedCount
                    )
                )
            }
        } catch (error: VyomFactoryGitHubClient.FactoryApiException) {
            if (error.code == 429 || error.code >= 500) Result.retry() else Result.failure()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val PROPOSAL_DIRECTORY = "factory-worker-proposals"
    }
}

/** Unique one-shot dispatch; queue refresh remains a separate read-only operation. */
internal object VyomFactoryWorkerDispatchScheduler {
    internal const val WORK_NAME = "vyom-factory-worker-dispatch-v1"
    internal const val PERIODIC_WORK_NAME = "vyom-factory-worker-dispatch-periodic-v1"

    fun ensurePeriodicScheduled(context: Context) {
        val app = context.applicationContext
        if (!VyomFactoryAutonomousExecutionAuthorizationRuntime.isEnabled(app)) {
            WorkManager.getInstance(app).cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<VyomFactoryWorkerDispatchWorker>(15L, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    fun enqueue(context: Context) = enqueueContinuation(context)

    fun enqueueContinuation(context: Context) {
        val app = context.applicationContext
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<VyomFactoryWorkerDispatchWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        if (!VyomFactoryAutonomousExecutionAuthorizationRuntime.isEnabled(app)) return
        WorkManager.getInstance(app).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        WorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(app).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
