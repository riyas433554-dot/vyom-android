package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog

/** Owner-controlled switch for unattended execution inside the fixed Factory safety boundary. */
internal class VyomFactoryAutonomousExecutionAuthorizationController(
    private val activity: Activity,
    private val gate: VyomFactoryForegroundGate,
    private val onStatus: (String) -> Unit
) {
    fun enable() {
        if (!gate.isForeground()) {
            onStatus("Enable autonomous execution only from the owner's foreground session.")
            return
        }
        if (VyomFactoryWorkerLiveCallAuthorizationStore(activity).read() == null) {
            onStatus("Enable live worker model calls first. Autonomous execution requires both authorizations.")
            return
        }
        if (GitHubReleaseTokenStore.read(activity).trim().length < 20) {
            onStatus("Save a GitHub token with Contents, Pull requests and Actions read permissions first.")
            return
        }
        val config = try {
            VyomFactoryAutonomousExecutionAuthorizationRuntime.currentConfig(activity)
        } catch (error: Exception) {
            onStatus(error.message ?: "The trusted worker binding is unavailable.")
            return
        }
        val generation = gate.generation()
        AlertDialog.Builder(activity)
            .setTitle("ENABLE AUTONOMOUS FACTORY EXECUTION")
            .setMessage(
                "Repository: ${config.repositoryUrl}\n" +
                    "Base branch: ${config.baseBranch}\n" +
                    "Exact base commit: ${config.baseCommitSha}\n" +
                    "Allowed paths: ${config.allowedPaths.joinToString(", ")}\n\n" +
                    "Vyom may generate and execute repository changes without asking you for each task. " +
                    "It will create draft PRs only and wait for exact-SHA CI. It cannot modify main/master, " +
                    "workflow/build/signing/security files, merge, release, install, or change backend/Render. " +
                    "A failed or interrupted write is not replayed automatically. Disable or use STOP CONTROL at any time. " +
                    "Enable this bounded autonomous mode?"
            )
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("ENABLE AUTONOMOUS MODE") { _, _ ->
                if (!gate.isForegroundSession(generation)) {
                    onStatus("Enable cancelled because the owner foreground session ended.")
                    return@setPositiveButton
                }
                try {
                    VyomFactoryAutonomousExecutionAuthorizationRuntime.enable(
                        activity, ownerConfirmed = true, foregroundSession = true,
                        nowMillis = System.currentTimeMillis()
                    )
                    VyomFactoryWorkerDispatchScheduler.ensurePeriodicScheduled(activity)
                    VyomFactoryWorkerDispatchScheduler.enqueue(activity)
                    onStatus("AUTONOMOUS MODE ENABLED · bounded repository execution is queued.")
                } catch (error: Exception) {
                    VyomFactoryAutonomousExecutionAuthorizationRuntime.disable(activity)
                    VyomFactoryWorkerDispatchScheduler.cancel(activity)
                    onStatus("Autonomous mode was not enabled: ${error.message ?: error.javaClass.simpleName}")
                }
            }
            .show()
    }

    fun disable() {
        try {
            VyomFactoryAutonomousExecutionAuthorizationRuntime.disable(activity)
            VyomFactoryWorkerDispatchScheduler.cancel(activity)
            onStatus("AUTONOMOUS MODE OFF · queued Factory work cancelled.")
        } catch (error: Exception) {
            onStatus("Autonomous mode could not be disabled cleanly: ${error.message ?: error.javaClass.simpleName}")
        }
    }
}
