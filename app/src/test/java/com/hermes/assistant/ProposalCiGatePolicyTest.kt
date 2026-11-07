package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class ProposalCiGatePolicyTest {
    private val sha = "a".repeat(40)

    private fun run(
        name: String,
        headSha: String = sha,
        status: String = "completed",
        conclusion: String? = "success",
        id: Long = 1L
    ) = ProposalCiGatePolicy.Run(name, headSha, status, conclusion, id)

    private fun successRuns() = listOf(
        run("Vyom Android Debug", id = 10L),
        run("Vyom Android CI", id = 11L)
    )

    @Test fun bothNamedWorkflowsMustSucceedForTheExactCommit() {
        assertEquals(ProposalCiGatePolicy.State.SUCCESS,
            ProposalCiGatePolicy.evaluate(sha, successRuns()).state)
    }

    @Test fun staleCommitRunsAndUnrelatedWorkflowsAreIgnored() {
        val runs = listOf(
            run("Vyom Android Debug", headSha = "b".repeat(40)),
            run("Vyom Android CI", headSha = "b".repeat(40)),
            run("Other workflow")
        )
        assertEquals(ProposalCiGatePolicy.State.WAITING,
            ProposalCiGatePolicy.evaluate(sha, runs).state)
    }

    @Test fun missingRequiredWorkflowKeepsTheGateWaiting() {
        assertEquals(ProposalCiGatePolicy.State.WAITING,
            ProposalCiGatePolicy.evaluate(sha, listOf(run("Vyom Android Debug"))).state)
    }

    @Test fun inProgressWorkflowCannotPassEvenIfItHasAStaleSuccessConclusion() {
        val runs = listOf(
            run("Vyom Android Debug", status = "in_progress", conclusion = "success"),
            run("Vyom Android CI")
        )
        assertEquals(ProposalCiGatePolicy.State.WAITING,
            ProposalCiGatePolicy.evaluate(sha, runs).state)
    }

    @Test fun failedRequiredWorkflowFailsTheGate() {
        val runs = listOf(
            run("Vyom Android Debug"),
            run("Vyom Android CI", conclusion = "failure")
        )
        assertEquals(ProposalCiGatePolicy.State.FAILED,
            ProposalCiGatePolicy.evaluate(sha, runs).state)
    }

    @Test fun latestRunForEachWorkflowWins() {
        val runs = listOf(
            run("Vyom Android Debug", id = 10L),
            run("Vyom Android Debug", conclusion = "failure", id = 12L),
            run("Vyom Android CI", id = 11L)
        )
        assertEquals(ProposalCiGatePolicy.State.FAILED,
            ProposalCiGatePolicy.evaluate(sha, runs).state)
    }

    @Test fun invalidExpectedShaFailsClosed() {
        assertEquals(ProposalCiGatePolicy.State.FAILED,
            ProposalCiGatePolicy.evaluate("short", successRuns()).state)
    }
}
