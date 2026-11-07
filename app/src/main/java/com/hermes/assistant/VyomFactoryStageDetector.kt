package com.hermes.assistant

/**
 * Runs the explicit finding detector at each Factory lifecycle stage.
 * Detection and persistence are best-effort: failures are journaled and never
 * allowed to turn an otherwise valid task execution into a failure.
 */
internal class VyomFactoryStageDetector(
    private val detect: (VyomFactoryTaskIntelligence.Stage, String) -> List<VyomFactoryTaskIntelligence.Finding> =
        { stage, output -> VyomFactoryTaskIntelligence.detect(stage, output) },
    private val loadTasks: () -> List<VyomFactoryGitHubClient.Task>,
    private val createTask: (
        Int,
        VyomFactoryTaskIntelligence.Finding,
        List<VyomFactoryGitHubClient.Task>
    ) -> VyomFactoryGitHubClient.Task?,
    private val recordEvent: (Event) -> Unit
) {
    enum class Outcome {
        NOTHING_CREATED, TASKS_CREATED, DUPLICATES_ONLY, DETECTOR_FAILED, PERSISTENCE_FAILED
    }

    data class Event(
        val parentIssueNumber: Int,
        val stage: VyomFactoryTaskIntelligence.Stage,
        val checkpoint: String,
        val outcome: Outcome,
        val findingCount: Int,
        val createdIssueNumbers: List<Int>,
        val duplicateCount: Int,
        val detail: String,
        val recordedAt: Long = System.currentTimeMillis()
    )

    data class Result(
        val outcome: Outcome,
        val findingCount: Int,
        val createdTasks: List<VyomFactoryGitHubClient.Task>,
        val duplicateCount: Int,
        val detectorFailed: Boolean,
        val journaled: Boolean
    )

    fun onPlanning(
        task: VyomFactoryGitHubClient.Task,
        planningOutput: String = task.body
    ): Result = run(task, VyomFactoryTaskIntelligence.Stage.PLANNING, "planning", planningOutput)

    fun onImplementationCheckpoint(
        task: VyomFactoryGitHubClient.Task,
        checkpoint: String,
        checkpointOutput: String
    ): Result {
        require(checkpoint.matches(Regex("[a-z0-9_]{1,64}")))
        return run(task, VyomFactoryTaskIntelligence.Stage.IMPLEMENTATION, checkpoint, checkpointOutput)
    }

    fun onVerificationResult(
        task: VyomFactoryGitHubClient.Task,
        passed: Boolean,
        evidence: String
    ): Result {
        val output = if (passed) {
            "Verification passed: " + evidence
        } else {
            "[BUG] Verification failed: " + evidence
        }
        return run(task, VyomFactoryTaskIntelligence.Stage.TEST, "verification_result", output)
    }

    private fun run(
        task: VyomFactoryGitHubClient.Task,
        stage: VyomFactoryTaskIntelligence.Stage,
        checkpoint: String,
        output: String
    ): Result {
        val findings = try {
            detect(stage, output)
        } catch (error: Exception) {
            return finish(
                task, stage, checkpoint, Outcome.DETECTOR_FAILED, 0, emptyList(), 0,
                error.javaClass.simpleName + ": " + (error.message ?: "detector error"),
                detectorFailed = true
            )
        }

        if (findings.isEmpty()) {
            return finish(task, stage, checkpoint, Outcome.NOTHING_CREATED, 0, emptyList(), 0,
                "nothing found", detectorFailed = false)
        }

        return try {
            val existing = loadTasks().toMutableList()
            val created = mutableListOf<VyomFactoryGitHubClient.Task>()
            var duplicates = 0
            findings.forEach { finding ->
                if (VyomFactoryTaskIntelligence.isDuplicate(finding, existing)) {
                    duplicates++
                } else {
                    val proposal = VyomFactoryTaskIntelligence.toSeparateTaskProposal(
                        task.number, finding, existing
                    )
                    if (proposal == null) {
                        duplicates++
                    } else {
                        val child = createTask(task.number, finding, existing)
                        if (child == null) {
                            duplicates++
                        } else {
                            created += child
                            existing += child
                        }
                    }
                }
            }
            val outcome = when {
                created.isNotEmpty() -> Outcome.TASKS_CREATED
                duplicates > 0 -> Outcome.DUPLICATES_ONLY
                else -> Outcome.NOTHING_CREATED
            }
            finish(
                task, stage, checkpoint, outcome, findings.size, created, duplicates,
                if (findings.isEmpty()) "nothing found" else "detector findings processed",
                detectorFailed = false
            )
        } catch (error: Exception) {
            finish(
                task, stage, checkpoint, Outcome.PERSISTENCE_FAILED, findings.size, emptyList(), 0,
                error.javaClass.simpleName + ": " + (error.message ?: "finding persistence error"),
                detectorFailed = false
            )
        }
    }

    private fun finish(
        task: VyomFactoryGitHubClient.Task,
        stage: VyomFactoryTaskIntelligence.Stage,
        checkpoint: String,
        outcome: Outcome,
        findingCount: Int,
        created: List<VyomFactoryGitHubClient.Task>,
        duplicateCount: Int,
        detail: String,
        detectorFailed: Boolean
    ): Result {
        val event = Event(
            parentIssueNumber = task.number,
            stage = stage,
            checkpoint = checkpoint,
            outcome = outcome,
            findingCount = findingCount,
            createdIssueNumbers = created.map { it.number },
            duplicateCount = duplicateCount,
            detail = detail.take(500)
        )
        val journaled = runCatching { recordEvent(event) }.isSuccess
        return Result(outcome, findingCount, created, duplicateCount, detectorFailed, journaled)
    }
}
