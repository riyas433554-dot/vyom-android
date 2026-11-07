package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryVerifiedProgressTest {
    private val probe = "Open the app and confirm the approved change is visible"
    private val commit = "a".repeat(40)
    private val verifiedResult =
        "repository_proposal_verified: issue=77; commit=$commit; " +
            "pr=https://github.com/riyas433554-dot/vyom-android/pull/9; " +
            "ci=Both required workflows succeeded for exact commit $commit"

    @Test fun reachesOneHundredOnlyAfterAllProofsAndCompletedState() {
        val task = task(status = "completed", events = listOf(passEvent()))
        val progress = VyomFactoryVerifiedProgress.calculate(task, listOf(successEntry()))

        assertEquals(100, progress.percent)
        assertTrue(progress.verifiedComplete)
        assertEquals("VERIFIED COMPLETE", progress.label)
    }

    @Test fun successfulImplementationAndTestsWithoutRequiredBehaviorStayBelowOneHundred() {
        val task = task(status = "in_progress")
        val progress = VyomFactoryVerifiedProgress.calculate(task, listOf(successEntry()))

        assertEquals(85, progress.percent)
        assertTrue(progress.implementationVerified)
        assertTrue(progress.testsVerified)
        assertFalse(progress.behaviorVerified)
        assertFalse(progress.verifiedComplete)
    }

    @Test fun completedIssueWithoutSuccessfulJournalIsExplicitlyUnverified() {
        val task = task(status = "completed", events = listOf(passEvent()))
        val progress = VyomFactoryVerifiedProgress.calculate(task, emptyList())

        assertTrue(progress.percent < 100)
        assertFalse(progress.implementationVerified)
        assertEquals("COMPLETED · UNVERIFIED", progress.label)
    }

    @Test fun successfulJournalWithoutExactShaCiEvidenceDoesNotPassTestGate() {
        val task = task(status = "in_progress")
        val entry = successEntry(result = "repository_proposal_verified: commit=$commit; pr=https://github.com/x/y/pull/1; ci=Full workflow is waiting")

        val progress = VyomFactoryVerifiedProgress.calculate(task, listOf(entry))

        assertTrue(progress.implementationVerified)
        assertFalse(progress.testsVerified)
        assertTrue(progress.percent < 100)
    }


    @Test fun ciEvidenceForDifferentCommitCannotVerifyTheProposedImplementation() {
        val otherSha = "b".repeat(40)
        val task = task(status = "in_progress")
        val entry = successEntry(
            result = "repository_proposal_verified: issue=77; commit=$commit; " +
                "pr=https://github.com/x/y/pull/1; ci=Both required workflows succeeded for exact commit $otherSha"
        )

        val progress = VyomFactoryVerifiedProgress.calculate(task, listOf(entry))

        assertTrue(progress.implementationVerified)
        assertFalse(progress.testsVerified)
        assertTrue(progress.percent < 100)
    }

    @Test fun failedOrInterruptedAttemptsNeverCountAsImplementationProof() {
        val task = task(status = "in_progress")
        val failed = successEntry().copy(state = VyomFactoryExecutionJournal.State.FAILED)
        val interrupted = successEntry(attemptId = "attempt-2").copy(
            state = VyomFactoryExecutionJournal.State.INTERRUPTED,
            updatedAt = 3_000L
        )

        val progress = VyomFactoryVerifiedProgress.calculate(task, listOf(failed, interrupted))

        assertFalse(progress.implementationVerified)
        assertFalse(progress.testsVerified)
        assertTrue(progress.percent < 100)
    }

    @Test fun incompleteAcceptanceCriteriaPreventVerifiedCompletion() {
        val task = task(
            status = "completed",
            description = description().replace("[x] Add the reviewed implementation", "[ ] Add the reviewed implementation"),
            events = listOf(passEvent())
        )
        val progress = VyomFactoryVerifiedProgress.calculate(task, listOf(successEntry()))

        assertEquals(1, progress.checkedCriteria)
        assertEquals(2, progress.totalCriteria)
        assertFalse(progress.verifiedComplete)
        assertTrue(progress.percent < 100)
    }

    @Test fun changingTheNamedProbeInvalidatesOldBehaviorEvidence() {
        val task = task(
            status = "completed",
            description = description().replace(probe, "Open settings and confirm the new preference is visible"),
            events = listOf(passEvent())
        )
        val progress = VyomFactoryVerifiedProgress.calculate(task, listOf(successEntry()))

        assertFalse(progress.behaviorVerified)
        assertFalse(progress.verifiedComplete)
        assertTrue(progress.percent < 100)
    }

    @Test fun allEvidenceWithoutRecordedCompletionRemainsNinetyFivePercent() {
        val task = task(status = "in_progress", events = listOf(passEvent()))
        val progress = VyomFactoryVerifiedProgress.calculate(task, listOf(successEntry()))

        assertEquals(95, progress.percent)
        assertFalse(progress.verifiedComplete)
        assertEquals("READY FOR VERIFIED COMPLETION", progress.label)
    }

    private fun task(
        status: String,
        description: String = description(),
        events: List<String> = emptyList()
    ): VyomFactoryGitHubClient.Task {
        val metadata = VyomFactoryStatus.Metadata(
            priority = "P2",
            status = status,
            source = "factory",
            sourceId = "progress-77",
            idempotencyKey = "factory:progress-77",
            events = events
        )
        return VyomFactoryGitHubClient.Task(
            number = 77,
            title = "[VYOM FACTORY][P2] Progress verification",
            body = VyomFactoryStatus.withMetadata(description, metadata),
            state = if (status == "completed") "closed" else "open",
            url = "https://github.com/riyas433554-dot/vyom-android/issues/77",
            updatedAt = "2026-11-07T10:00:00Z",
            metadata = metadata
        )
    }

    private fun description() =
        "- **Verification probe:** $probe\n\n" +
            "Acceptance criteria:\n" +
            "- [x] Add the reviewed implementation\n" +
            "- [x] Verify the required CI checks\n\n" +
            """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"repository_change","operation":"propose_change","request":"Apply reviewed code change","arguments":{}} -->"""

    private fun passEvent() =
        VyomFactoryVerificationPolicy.resultEvent(probe, "The exact approved change is visible on the device", true)

    private fun successEntry(
        attemptId: String = "attempt-1",
        result: String = verifiedResult
    ) = VyomFactoryExecutionJournal.Entry(
        issueNumber = 77,
        attemptId = attemptId,
        leaseId = "lease-progress",
        ownerId = "owner-progress",
        state = VyomFactoryExecutionJournal.State.SUCCEEDED,
        startedAt = 1_000L,
        updatedAt = 2_000L,
        result = result,
        operationKey = "repository_change:propose_change"
    )
}
