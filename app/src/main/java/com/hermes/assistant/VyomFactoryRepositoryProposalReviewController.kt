package com.hermes.assistant

import android.app.Activity
import java.io.File
import java.util.concurrent.Executor

/** Foreground owner-review entry point for durable worker proposals. */
internal class VyomFactoryRepositoryProposalReviewController(
    private val activity: Activity,
    private val io: Executor,
    private val onStatus: (String) -> Unit
) {
    fun reviewNext() {
        val githubToken = GitHubReleaseTokenStore.read(activity).trim()
        if (githubToken.length < 20) {
            onStatus("Save a GitHub token with Contents, Pull requests and Issues permissions first.")
            return
        }
        val registry = VyomFactorySystemRegistry(File(activity.filesDir, "factory-system-registry"))
        val binding = registry.get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID)
        if (binding == null) {
            onStatus("Worker repository is not bound. Bind the trusted repository first.")
            return
        }
        val proposalStore = VyomFactoryWorkerProposalStore(
            File(activity.filesDir, VyomFactoryWorkerDispatchWorker.PROPOSAL_DIRECTORY)
        )
        onStatus("Loading the next saved worker proposal and rechecking its GitHub issue…")
        io.execute {
            try {
                val stored = (proposalStore.pending() + proposalStore.approved())
                    .distinctBy { it.issueNumber }
                    .sortedBy { it.issueNumber }
                    .firstOrNull()
                    ?: run {
                        postStatus("No saved worker proposals are waiting for owner review.")
                        return@execute
                    }
                val client = VyomFactoryGitHubClient(githubToken, registry)
                val task = client.listTasks().firstOrNull { it.number == stored.issueNumber }
                    ?: run {
                        postStatus("Proposal #${stored.issueNumber} is stale: its GitHub issue no longer exists.")
                        return@execute
                    }
                if (task.state != "open" || task.metadata.sourceId != stored.sourceId ||
                    task.updatedAt != stored.updatedAt
                ) {
                    postStatus("Proposal #${stored.issueNumber} is stale. Regenerate it from the current issue before review.")
                    return@execute
                }
                if (activity.isFinishing || activity.isDestroyed) return@execute
                activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                    val approvalStore = VyomFactoryRepositoryApprovalStore(activity)
                    VyomFactoryRepositoryChangeReviewDialog(
                        activity = activity,
                        task = task,
                        candidate = stored.candidate,
                        approvalStore = approvalStore,
                        onApproved = { _, approval ->
                            postStatus("Owner approval received for #${stored.issueNumber}. Executing the pinned repository proposal and waiting for exact-SHA CI…")
                            io.execute {
                                try {
                                    val coordinator = VyomFactoryRepositoryProposalExecutionCoordinator(
                                        registry = registry,
                                        proposals = object : VyomFactoryRepositoryProposalExecutionCoordinator.ProposalAccess {
                                            override fun get(issueNumber: Int) = proposalStore.get(issueNumber)
                                            override fun markApproved(issueNumber: Int) = proposalStore.markApproved(issueNumber)
                                            override fun markSubmitted(issueNumber: Int) = proposalStore.markSubmitted(issueNumber)
                                        },
                                        fetchFreshTask = { issueNumber ->
                                            client.listTasks().firstOrNull { it.number == issueNumber }
                                        },
                                        verifyApproval = { plan, reviewedBinding, token ->
                                            approvalStore.verifyAndConsume(plan, reviewedBinding, token)
                                        },
                                        executePlan = { plan, token, control ->
                                            VyomFactoryRepositoryChangeExecutor(registry)
                                                .execute(plan, token, control)
                                        }
                                    )
                                    val control = object : VyomFactoryExecutionControl {
                                        override fun shouldStop(): Boolean =
                                            VyomFactoryWorkerStopStore(activity).isStopped() ||
                                                activity.isFinishing || activity.isDestroyed
                                    }
                                    val result = coordinator.execute(
                                        stored.issueNumber, approval, githubToken, control
                                    )
                                    postStatus(
                                        "REAL REPOSITORY EXECUTION PASS · #${stored.issueNumber} · " +
                                            "commit ${result.commitSha.take(12)} · exact-SHA CI passed · ${result.pullRequestUrl}"
                                    )
                                } catch (error: Exception) {
                                    postStatus(
                                        "REPOSITORY EXECUTION NOT VERIFIED · #${stored.issueNumber} · " +
                                            "${error.message ?: error.javaClass.simpleName}"
                                    )
                                }
                            }
                        },
                        onRejected = {
                            if (stored.status == "awaiting_review") proposalStore.markRejected(stored.issueNumber)
                            postStatus("Repository proposal #${stored.issueNumber} rejected; no repository write was started.")
                        }
                    ).show()
                }
            } catch (error: Exception) {
                postStatus("Could not load worker proposal · ${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    private fun postStatus(message: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread {
            if (!activity.isFinishing && !activity.isDestroyed) onStatus(message)
        }
    }
}
