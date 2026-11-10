package com.hermes.assistant

/**
 * Live RUN NEXT planning boundary. The detector sees the same fresh snapshot
 * consumed by triage, but never sees closed tasks or active device-local claims.
 */
internal class VyomFactoryLivePlanningStage(
    private val onPlanning: (VyomFactoryGitHubClient.Task, String) -> Unit
) {
    fun inspect(
        tasks: List<VyomFactoryGitHubClient.Task>,
        claimedIssueNumbers: Set<Int>
    ) {
        val unclaimed = tasks.filterNot { it.number in claimedIssueNumbers }
        VyomFactoryPlanningStageHook { task, output ->
            // Detection is advisory. An unexpected detector exception must not abort planning.
            runCatching { onPlanning(task, output) }
        }.inspect(unclaimed)
    }
}
