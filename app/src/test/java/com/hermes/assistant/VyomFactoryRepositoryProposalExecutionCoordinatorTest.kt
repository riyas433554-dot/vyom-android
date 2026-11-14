package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VyomFactoryRepositoryProposalExecutionCoordinatorTest {
    private val base = "a".repeat(40)
    private val binding = VyomFactorySystemRegistry.Binding(
        "worker", "https://github.com/riyas433554-dot/vyom-android", base,
        listOf("app/src/main/java/com/hermes/assistant"), 1L
    )
    private val plan = VyomFactoryRepositoryChangePolicy.Plan(
        "worker", 17, "attempt-12345678", "Implement safe worker change",
        "feature/factory-item5-task-intelligence", base,
        listOf(VyomFactoryRepositoryChangePolicy.Change(
            "app/src/main/java/com/hermes/assistant/SafeChange.kt",
            "package com.hermes.assistant\nclass SafeChange"
        )), false
    )
    private val candidate = VyomFactoryRepositoryChangeReviewDialog.Candidate(
        plan, binding, "A bounded safe change",
        mapOf(plan.changes.single().path to "b".repeat(64))
    )
    private val task = VyomFactoryGitHubClient.Task(
        17, "Factory task", "request", "open", "https://github.com/riyas433554-dot/vyom-android/issues/17",
        "2026-11-14T10:00:00Z",
        VyomFactoryStatus.Metadata(
            "P0", "in_progress", "factory", "source-17", "factory:source-17",
            listOf("created"), VyomFactoryLaneRouter.Lane.CUSTOM, true
        )
    )

    @Test fun rejectsStaleIssueBeforeConsumingApprovalOrWritingRepository() {
        val access = FakeProposalAccess(stored("awaiting_review"))
        var approvals = 0
        var writes = 0
        val coordinator = coordinator(access, fetch = { task.copy(updatedAt = "2026-11-14T11:00:00Z") },
            verify = { _, _, _ -> approvals++ }, execute = { _, _, _ -> writes++; proposalResult() })

        assertThrows(SecurityException::class.java) {
            coordinator.execute(17, token(), "github-token-long-enough", NeverStop)
        }
        assertEquals(0, approvals)
        assertEquals(0, writes)
    }

    @Test fun executesOnlyAfterFreshTaskAndExactOwnerApprovalThenMarksSubmitted() {
        val access = FakeProposalAccess(stored("awaiting_review"))
        var approvals = 0
        var writes = 0
        val coordinator = coordinator(access, fetch = { task },
            verify = { reviewed, reviewedBinding, _ ->
                approvals++
                assertEquals(plan, reviewed)
                assertEquals(binding, reviewedBinding)
            },
            execute = { approvedPlan, token, control ->
                writes++
                assertTrue(approvedPlan.ownerApproved)
                assertEquals("github-token-long-enough", token)
                assertFalse(control.shouldStop())
                proposalResult()
            })

        val result = coordinator.execute(17, token(), "github-token-long-enough", NeverStop)

        assertEquals("c".repeat(40), result.commitSha)
        assertEquals(1, approvals)
        assertEquals(1, writes)
        assertEquals("submitted", access.current!!.status)
    }

    @Test fun refusesExecutionWhenOwnerApprovalDoesNotVerify() {
        val access = FakeProposalAccess(stored("awaiting_review"))
        var writes = 0
        val coordinator = coordinator(access, fetch = { task },
            verify = { _, _, _ -> throw SecurityException("invalid owner token") },
            execute = { _, _, _ -> writes++; proposalResult() })

        assertThrows(SecurityException::class.java) {
            coordinator.execute(17, token(), "github-token-long-enough", NeverStop)
        }
        assertEquals(0, writes)
        assertEquals("awaiting_review", access.current!!.status)
    }

    private fun coordinator(
        access: FakeProposalAccess,
        fetch: () -> VyomFactoryGitHubClient.Task?,
        verify: (VyomFactoryRepositoryChangePolicy.Plan, VyomFactorySystemRegistry.Binding, VyomFactoryRepositoryApprovalStore.Token) -> Unit,
        execute: (VyomFactoryRepositoryChangePolicy.Plan, String, VyomFactoryExecutionControl) -> VyomFactoryRepositoryChangeExecutor.Proposal
    ): VyomFactoryRepositoryProposalExecutionCoordinator {
        val root = File.createTempFile("factory-proposal-coordinator", ".tmp").apply { delete(); mkdirs() }
        val registry = VyomFactorySystemRegistry(root)
        registry.register(binding, ownerConfirmed = true)
        return VyomFactoryRepositoryProposalExecutionCoordinator(
            registry = registry,
            proposals = access,
            fetchFreshTask = { fetch() },
            verifyApproval = verify,
            executePlan = execute
        )
    }

    private fun stored(status: String) = VyomFactoryWorkerProposalStore.StoredProposal(
        17, task.metadata.sourceId, task.updatedAt, "worker-test-01", "lease-17", candidate, status
    )

    private fun token() = VyomFactoryRepositoryApprovalStore.Token("d".repeat(64), "nonce-123456789012", 999_999L)
    private fun proposalResult() = VyomFactoryRepositoryChangeExecutor.Proposal(
        "vyom-worker-17-attempt-12345678", "c".repeat(40),
        "https://github.com/riyas433554-dot/vyom-android/pull/17", "exact-sha-ci-pass"
    )

    private class FakeProposalAccess(var current: VyomFactoryWorkerProposalStore.StoredProposal?) :
        VyomFactoryRepositoryProposalExecutionCoordinator.ProposalAccess {
        override fun get(issueNumber: Int) = current?.takeIf { it.issueNumber == issueNumber }
        override fun markApproved(issueNumber: Int): Boolean {
            val value = get(issueNumber) ?: return false
            if (value.status == "approved") return true
            if (value.status != "awaiting_review") return false
            current = value.copy(status = "approved")
            return true
        }
        override fun markSubmitted(issueNumber: Int): Boolean {
            val value = get(issueNumber) ?: return false
            if (value.status == "submitted") return true
            if (value.status != "approved") return false
            current = value.copy(status = "submitted")
            return true
        }
    }

    private object NeverStop : VyomFactoryExecutionControl { override fun shouldStop() = false }
}
