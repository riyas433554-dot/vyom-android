package com.hermes.assistant

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** App-closed, network-constrained planner worker. It never approves or writes repository changes. */
internal class VyomFactoryWorkerDispatchWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val context = applicationContext
        return try {
            val stopStore = VyomFactoryWorkerStopStore(context)
            if (stopStore.isStopped()) return Result.success()
            if (!VyomFactoryWorkerLiveCallAuthorizationRuntime.isEnabled(context)) return Result.failure(
                androidx.work.workDataOf("reason" to "OWNER_AUTHORIZATION_REQUIRED")
            )

            val token = GitHubReleaseTokenStore.read(context)
            if (token.isBlank()) return Result.failure()
            val registry = VyomFactorySystemRegistry(File(context.filesDir, "factory-system-registry"))
            if (!registry.allowsPath(
                    VyomFactoryGitHubClient.SYSTEM_ID,
                    VyomFactoryGitHubClient.ISSUES_PATH
                )
            ) return Result.failure()
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
            when {
                dispatched.stopped || dispatched.authorizationRequired -> Result.success()
                dispatched.retryableFailureCount > 0 -> Result.retry()
                else -> Result.success()
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

    fun enqueue(context: Context) {
        val app = context.applicationContext
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<VyomFactoryWorkerDispatchWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}
