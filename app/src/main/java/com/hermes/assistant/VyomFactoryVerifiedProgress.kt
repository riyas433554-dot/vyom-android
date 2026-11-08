package com.hermes.assistant

/**
 * Ordered, evidence-backed task progress.
 *
 * Journal result is the durable implementation receipt: it must contain the
 * exact implementation commit SHA. CI evidence is accepted only for that SHA.
 * Criteria and the optional named device probe are later, independent gates.
 */
internal object VyomFactoryVerifiedProgress {
    enum class Stage {
        NOT_STARTED,
        IMPLEMENTATION_DONE,
        CI_VERIFIED,
        CRITERIA_VERIFIED,
        PROBE_VERIFIED
    }

    data class Snapshot(
        val percent: Int,
        val stage: Stage,
        val label: String,
        val checkedCriteria: Int,
        val totalCriteria: Int,
        val commitSha: String?,
        val implementationVerified: Boolean,
        val ciVerified: Boolean,
        val criteriaVerified: Boolean,
        val probeRequired: Boolean,
        val probeVerified: Boolean,
        val executionInProgress: Boolean
    )

    private val criteriaHeader = Regex("(?im)^\\s*Acceptance criteria\\s*:\\s*$")
    private val criteriaItem = Regex("(?m)^\\s*[-*]\\s*\\[([ xX])\\]\\s+(.+?)\\s*$")
    private val commitPattern = Regex("commit=([a-f0-9]{40})")
    private val ciPattern = Regex("Both required workflows succeeded for exact commit ([a-f0-9]{40})")

    fun calculate(
        task: VyomFactoryGitHubClient.Task,
        journalEntries: List<VyomFactoryExecutionJournal.Entry>
    ): Snapshot {
        val description = task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
        val criteriaSection = criteriaHeader.find(description)?.let { header ->
            val remaining = description.substring(header.range.last + 1)
            val end = Regex("(?m)^\\s*(?:#{1,6}\\s|<!--)").find(remaining)?.range?.first
                ?: remaining.length
            remaining.substring(0, end)
        }.orEmpty()
        val criteria = criteriaItem.findAll(criteriaSection).toList()
        val checked = criteria.count {
            it.groupValues[1].equals("x", ignoreCase = true) &&
                it.groupValues[2].trim().length >= 4
        }
        val criteriaVerified = criteria.isNotEmpty() && checked == criteria.size

        val envelope = VyomFactoryExecutionEnvelope.parse(task.body, task.number)
        val operationKey = if (envelope.isExecutable) {
            "${envelope.target.name.lowercase()}:${envelope.operation}"
        } else null
        val latestAttempt = operationKey?.let { expected ->
            journalEntries.asSequence()
                .filter { it.issueNumber == task.number && it.operationKey == expected }
                .maxWithOrNull(
                    compareBy<VyomFactoryExecutionJournal.Entry> { it.updatedAt }
                        .thenBy { it.startedAt }
                )
        }
        val executionInProgress = latestAttempt?.state in setOf(
            VyomFactoryExecutionJournal.State.CLAIMED,
            VyomFactoryExecutionJournal.State.RUNNING,
            VyomFactoryExecutionJournal.State.STOP_REQUESTED
        )

        // Only the latest successful attempt may contribute. A newer STOP,
        // failure, or crash-recovered INTERRUPTED attempt invalidates old proof.
        val result = latestAttempt?.takeIf {
            it.state == VyomFactoryExecutionJournal.State.SUCCEEDED
        }?.result.orEmpty()
        val commitSha = commitPattern.find(result)?.groupValues?.getOrNull(1)
        val implementationVerified = commitSha != null
        val ciSha = ciPattern.find(result.substringAfter("ci=", ""))
            ?.groupValues?.getOrNull(1)
        val ciVerified = implementationVerified && ciSha == commitSha

        val probeRequired = VyomFactoryVerificationPolicy.assignedProbe(description) != null
        val probeVerified = !probeRequired || VyomFactoryVerificationPolicy.canComplete(
            description,
            task.metadata.events
        )

        val stage = when {
            !implementationVerified -> Stage.NOT_STARTED
            !ciVerified -> Stage.IMPLEMENTATION_DONE
            !criteriaVerified -> Stage.CI_VERIFIED
            probeRequired && !probeVerified -> Stage.CRITERIA_VERIFIED
            else -> Stage.PROBE_VERIFIED
        }
        val percent = when (stage) {
            Stage.NOT_STARTED -> 0
            Stage.IMPLEMENTATION_DONE -> 25
            Stage.CI_VERIFIED -> 50
            Stage.CRITERIA_VERIFIED -> if (probeRequired) 99 else 100
            Stage.PROBE_VERIFIED -> 100
        }
        val label = when {
            executionInProgress && !implementationVerified -> "IMPLEMENTATION RUNNING"
            !implementationVerified -> "NOT_STARTED"
            !ciVerified -> "IMPLEMENTATION DONE · CI PENDING"
            !criteriaVerified -> "CI VERIFIED · CRITERIA PENDING"
            probeRequired && !probeVerified -> "PHONE-PENDING"
            !probeRequired -> "CRITERIA VERIFIED · DEVICE PROBE NOT REQUIRED"
            else -> "PROBE VERIFIED"
        }

        return Snapshot(
            percent = percent,
            stage = stage,
            label = label,
            checkedCriteria = checked,
            totalCriteria = criteria.size,
            commitSha = commitSha,
            implementationVerified = implementationVerified,
            ciVerified = ciVerified,
            criteriaVerified = criteriaVerified,
            probeRequired = probeRequired,
            probeVerified = probeVerified,
            executionInProgress = executionInProgress
        )
    }
}
