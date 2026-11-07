package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryTaskIntelligenceTest {
    @Test fun blockerDependencyIsOrderedBeforeDependentEvenWhenItsPriorityIsLower() {
        val dependent = task(1, "Build feature", "P0", "Blocked by: #2")
        val blocker = task(2, "Fix compile error", "P2", "Acceptance criteria:\n- [ ] Build passes")
        assertEquals(listOf(2, 1), VyomFactoryTaskIntelligence.order(listOf(dependent, blocker)).map { it.number })
    }
    @Test fun circularDependenciesAreReportedAndNeverSilentlyOrdered() {
        val tasks = listOf(task(1, "A", "P1", "Blocked by: #2"), task(2, "B", "P1", "Blocked by: #1"))
        assertEquals(setOf(1, 2), VyomFactoryTaskIntelligence.circularDependencyIssues(tasks))
    }
    @Test fun duplicateFindingDoesNotCreateAnotherTaskProposal() {
        val tasks = listOf(task(7, "Login crashes", "P1", "Login crashes on launch"))
        val finding = VyomFactoryTaskIntelligence.Finding(VyomFactoryTaskIntelligence.Stage.TEST, VyomFactoryTaskIntelligence.FindingKind.BUG, "Login crashes", "Login crashes on launch")
        assertTrue(VyomFactoryTaskIntelligence.isDuplicate(finding, tasks))
        assertNull(VyomFactoryTaskIntelligence.toSeparateTaskProposal(7, finding, tasks))
    }
    @Test fun taskStopsAtItsConfiguredMaximumIteration() {
        assertTrue(VyomFactoryTaskIntelligence.isIterationLimitReached(task(8, "Fix retry loop", "P1", "Max iterations: 2\nIterations used: 2")))
        assertFalse(VyomFactoryTaskIntelligence.isIterationLimitReached(task(9, "Fix retry loop", "P1", "Max iterations: 3\nIterations used: 2")))
        assertTrue(VyomFactoryTaskIntelligence.isIterationLimitReached(task(90, "Retry", "P1", "Max iterations: 2"), iterationsUsed = 2))
    }
    @Test fun detectorIdeaBecomesSeparateTaskWithoutChangingCurrentTaskScope() {
        val parent = task(10, "Fix login crash", "P1", "Current scope: fix login crash")
        val finding = VyomFactoryTaskIntelligence.Finding(VyomFactoryTaskIntelligence.Stage.IMPLEMENTATION, VyomFactoryTaskIntelligence.FindingKind.IDEA, "Add passkey login", "Support passkey authentication")
        val proposal = VyomFactoryTaskIntelligence.toSeparateTaskProposal(parent.number, finding, listOf(parent))
        assertNotNull(proposal); assertEquals(10, proposal!!.parentIssueNumber)
        assertNotEquals(parent.title, proposal.title); assertEquals("Support passkey authentication", proposal.description)
        assertTrue(parent.body.contains("Current scope: fix login crash"))
    }
    @Test fun detectorAcceptsPlanningImplementationAndTestingStageMarkers() {
        val planning = VyomFactoryTaskIntelligence.detect(VyomFactoryTaskIntelligence.Stage.PLANNING, "[MISSING_FEATURE] Add export")
        val implementation = VyomFactoryTaskIntelligence.detect(VyomFactoryTaskIntelligence.Stage.IMPLEMENTATION, "[BUG] Null result when input is empty")
        val testing = VyomFactoryTaskIntelligence.detect(VyomFactoryTaskIntelligence.Stage.TEST, "[IDEA] Add offline mode")
        assertEquals(VyomFactoryTaskIntelligence.Stage.PLANNING, planning.single().stage)
        assertEquals(VyomFactoryTaskIntelligence.FindingKind.BUG, implementation.single().kind)
        assertEquals(VyomFactoryTaskIntelligence.FindingKind.IDEA, testing.single().kind)
    }
    @Test fun completionRequiresExplicitAcceptanceCriteriaAndAllChecked() {
        assertFalse(VyomFactoryTaskIntelligence.acceptanceCriteriaSatisfied(task(11, "Ship", "P1", "No criteria")))
        assertFalse(VyomFactoryTaskIntelligence.acceptanceCriteriaSatisfied(task(12, "Ship", "P1", "Acceptance criteria:\n- [ ] Unit tests pass")))
        assertTrue(VyomFactoryTaskIntelligence.acceptanceCriteriaSatisfied(task(13, "Ship", "P1", "Acceptance criteria:\n- [x] Unit tests pass")))
    }
    private fun task(number: Int, title: String, priority: String, body: String) = VyomFactoryGitHubClient.Task(number, title, body, "open", "https://github.com/example/repo/issues/$number", "2026-11-07T00:00:00Z", VyomFactoryStatus.Metadata(priority, "open", "factory", "task-$number", "factory:task-$number", emptyList()), "2026-11-01T00:00:00Z")
}
