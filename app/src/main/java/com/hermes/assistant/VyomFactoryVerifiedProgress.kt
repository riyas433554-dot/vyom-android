package com.hermes.assistant

/**
 * Evidence-backed task progress. A task is never shown as 100% merely because
 * it was edited, committed, started, or marked completed in GitHub.
 *
 * The execution journal proves the exact issue + operation completed through
 * a verified adapter. Repository-change results additionally carry the
 * proposal commit/PR and the executor's exact-SHA CI evidence. Android actions
 * require the adapter's independently checked postcondition. The named owner
 * probe and every explicit acceptance criterion are separate gates.
 */
internal object VyomFactoryVerifiedProgress {
    data class Snapshot(
        val percent: Int,
        val label: String,
        val checkedCriteria: Int,
        val totalCriteria: Int,
        val implementationVerified: Boolean,
        val testsVerified: Boolean,
        val behaviorVerified: Boolean,
        val completionRecorded: Boolean,
        val verifiedComplete: Boolean
    )

    private val criteriaHeader = Regex("(?im)^\\s*Acceptance criteria\\s*:\\s*$")
    private val criteriaItem = Regex("(?m)^\\s*[-*]\\s*\\[([ xX])\\]\\s+(.+?)\\s*$")
    private val commitPattern = Regex("commit=[a-f0-9]{40}")
    private val pullPattern = Regex("pr=https://github\\.com/[^\\s]+/pull/[0-9]+")

    fun calculate(
        task: VyomFactoryGitHubClient.Task,
        journalEntries: List<VyomFactoryExecutionJournal.Entry>
    ): Snapshot {
        val description = task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
        val criteria = criteriaHeader.find(description)?.let { header ->
            criteriaItem.findAll(description.substring(header.range.last + 1)).toList()
        }.orEmpty()
        val checked = criteria.count {
            it.groupValues[1].equals("x", ignoreCase = true) &&
                it.groupValues[2].trim().length >= 4
        }
        val criteriaComplete = criteria.isNotEmpty() && checked == criteria.size

        val envelope = VyomFactoryExecutionEnvelope.parse(task.body, task.number)
        val operationKey = if (envelope.isExecutable) {
            "${envelope.target.name.lowercase()}:${envelope.operation}"
        } else null
        val latestSuccess = operationKey?.let { expected ->
            journalEntries.asSequence()
                .filter {
                    it.issueNumber == task.number &&
                        it.operationKey == expected &&
                        it.state == VyomFactoryExecutionJournal.State.SUCCEEDED &&
                        !it.result.isNullOrBlank()
                }
                .maxWithOrNull(compareBy<VyomFactoryExecutionJournal.Entry> { it.updatedAt }
                    .thenBy { it.startedAt })
        }
        val implementationVerified = latestSuccess != null
        val result = latestSuccess?.result.orEmpty()
        val testsVerified = implementationVerified && when (envelope.target) {
            VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE ->
                result.startsWith("repository_proposal_verified:") &&
                    commitPattern.containsMatchIn(result) &&
                    pullPattern.containsMatchIn(result) &&
                    result.substringAfter("ci=", "").isNotBlank()
            VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION ->
                DeviceActionExecutor.auditState(result) == "verified"
            else -> false
        }
        val behaviorVerified = VyomFactoryVerificationPolicy.canComplete(
            description,
            task.metadata.events
        )
        val completionRecorded = task.metadata.status == "completed"
        val verifiedComplete = completionRecorded && criteriaComplete &&
            implementationVerified && testsVerified && behaviorVerified

        // Weighted evidence stages: criteria 30, implementation 25, tests 25,
        // named behavior probe 10, terminal completion 5, active work 5.
        // The 100th point is reserved for the conjunction above.
        val activePoints = if (task.metadata.status in setOf("in_progress", "completed")) 5 else 0
        val criteriaPoints = if (criteria.isEmpty()) 0 else (30 * checked / criteria.size)
        val rawPercent = activePoints +
            criteriaPoints +
            (if (implementationVerified) 25 else 0) +
            (if (testsVerified) 25 else 0) +
            (if (behaviorVerified) 10 else 0) +
            (if (completionRecorded) 5 else 0)
        val percent = if (verifiedComplete) 100 else rawPercent.coerceIn(0, 99)

        val label = when {
            verifiedComplete -> "VERIFIED COMPLETE"
            completionRecorded -> "COMPLETED · UNVERIFIED"
            !criteriaComplete -> "ACCEPTANCE CRITERIA INCOMPLETE"
            !implementationVerified -> "IMPLEMENTATION NOT VERIFIED"
            !testsVerified -> "TEST / POSTCONDITION EVIDENCE MISSING"
            !behaviorVerified -> "REQUIRED BEHAVIOR PROBE PENDING"
            else -> "READY FOR VERIFIED COMPLETION"
        }

        return Snapshot(
            percent = percent,
            label = label,
            checkedCriteria = checked,
            totalCriteria = criteria.size,
            implementationVerified = implementationVerified,
            testsVerified = testsVerified,
            behaviorVerified = behaviorVerified,
            completionRecorded = completionRecorded,
            verifiedComplete = verifiedComplete
        )
    }
}
