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

/**
 * Best-effort background inbox refresh only.
 *
 * This worker performs a GitHub Issues GET and persists eligible open tasks locally.
 * It intentionally does not construct worker sessions, claim leases, call providers,
 * create approvals, or mutate GitHub Issues/repositories.
 */
internal class VyomFactoryQueueRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        return try {
            val token = GitHubReleaseTokenStore.read(applicationContext)
            if (token.isBlank()) return Result.failure()

            val registry = VyomFactorySystemRegistry(
                File(applicationContext.filesDir, "factory-system-registry")
            )
            if (!registry.allowsPath(
                    VyomFactoryGitHubClient.SYSTEM_ID,
                    VyomFactoryGitHubClient.ISSUES_PATH
                )
            ) return Result.failure()

            val client = VyomFactoryGitHubClient(token, registry)
            VyomFactoryQueueRefresh(
                taskStore = VyomFactoryTaskStore(applicationContext),
                fetchRemoteTasks = client::listTasks
            ).refresh()
            Result.success()
        } catch (error: VyomFactoryGitHubClient.FactoryApiException) {
            if (error.code == 429 || error.code >= 500) Result.retry() else Result.failure()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}

/** Schedules one initial refresh and a 15-minute best-effort periodic inbox refresh. */
internal object VyomFactoryQueueRefreshScheduler {
    private const val INITIAL_WORK_NAME = "vyom-factory-queue-refresh-once-v1"
    private const val PERIODIC_WORK_NAME = "vyom-factory-queue-refresh-periodic-v1"
    private const val PERIOD_MINUTES = 15L

    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val backoffDelay = 30L

        val initial = OneTimeWorkRequestBuilder<VyomFactoryQueueRefreshWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, backoffDelay, TimeUnit.SECONDS)
            .build()
        val periodic = PeriodicWorkRequestBuilder<VyomFactoryQueueRefreshWorker>(
            PERIOD_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, backoffDelay, TimeUnit.SECONDS)
            .build()

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueueUniqueWork(INITIAL_WORK_NAME, ExistingWorkPolicy.KEEP, initial)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
    }
}
