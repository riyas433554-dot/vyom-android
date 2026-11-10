package com.hermes.assistant

/** Testable planning-stage entry point: inspect only live work before triage/claim selection. */
internal class VyomFactoryPlanningStageHook(
    private val onPlanning: (VyomFactoryGitHubClient.Task, String) -> Unit
) {
    fun inspect(tasks: List<VyomFactoryGitHubClient.Task>) {
        tasks.filter {
            it.state == "open" && it.metadata.status in setOf("open", "in_progress")
        }.forEach { task ->
            onPlanning(task, task.body)
        }
    }
}
