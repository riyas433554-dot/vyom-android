package com.hermes.assistant

/**
 * Exact-SHA gate for the two workflows required before an in-app proposal can install.
 * A newer retry supersedes an older run of the same workflow.
 */
internal object ProposalCiGatePolicy {
    enum class State { WAITING, SUCCESS, FAILED }

    data class Run(
        val name: String,
        val headSha: String,
        val status: String,
        val conclusion: String?,
        val id: Long
    )

    data class Evaluation(val state: State, val detail: String)

    private val requiredWorkflows = setOf("Vyom Android Debug", "Vyom Android CI")

    fun evaluate(expectedSha: String, runs: List<Run>): Evaluation {
        if (!expectedSha.matches(Regex("^[a-f0-9]{40}$"))) {
            return Evaluation(State.FAILED, "Proposal commit SHA is malformed")
        }
        val latestByWorkflow = mutableMapOf<String, Run>()
        for (run in runs) {
            if (run.name !in requiredWorkflows || run.headSha != expectedSha) continue
            val existing = latestByWorkflow[run.name]
            if (existing == null || run.id > existing.id) latestByWorkflow[run.name] = run
        }
        for (workflow in requiredWorkflows) {
            val run = latestByWorkflow[workflow]
                ?: return Evaluation(State.WAITING, "$workflow has not started for exact commit $expectedSha")
            if (run.status != "completed") {
                return Evaluation(State.WAITING, "${workflow} is ${run.status} for exact commit $expectedSha")
            }
            if (run.conclusion != "success") {
                return Evaluation(State.FAILED, "${workflow} completed with conclusion ${run.conclusion ?: "missing"} for exact commit $expectedSha")
            }
        }
        return Evaluation(State.SUCCESS, "Both required workflows succeeded for exact commit $expectedSha")
    }
}
