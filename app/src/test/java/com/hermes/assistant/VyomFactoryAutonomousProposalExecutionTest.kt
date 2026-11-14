package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VyomFactoryAutonomousProposalExecutionTest {
    private val base = "a".repeat(40)
    private val repo = VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL
    private val paths = VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS
    private val binding = VyomFactorySystemRegistry.Binding(
        VyomFactoryWorkerBindingPolicy.SYSTEM_ID, repo, base, paths, 1L
    )
    private val config = VyomFactoryAutonomousExecutionConfig(
        repo, SelfModificationPolicy.PROPOSAL_BASE_BRANCH, base, paths
    )
    private val task = VyomFactoryGitHubClient.Task(
        17, "Factory task", "request", "open",
        "$repo/issues/17", "2026-11-14T10:00:00Z",
        VyomFactoryStatus.Metadata(
            "P0", "in_progress", "factory", "source-17", "factory:source-17",
            listOf("created"), VyomFactoryLaneRouter.Lane.CUSTOM, true
        )
    )
    private val plan = VyomFactoryRepositoryChangePolicy.Plan(
        binding.systemId, 17, "attempt-12345678", "Implement safe worker change",
        config.baseBranch, base,
        listOf(VyomFactoryRepositoryChangePolicy.Change(
            "app/src/main/java/com/hermes/assistant/SafeChange.kt",
            "package com.hermes.assistant\nclass SafeChange"
        )), false
    )
    private val candidate = VyomFactoryRepositoryChangeReviewDialog.Candidate(
        plan, binding, "bounded change",
        mapOf(plan.changes.single().path to "b".repeat(64))
    )

    @Test fun validPersistentOwnerAuthorizationExecutesWithoutPerTaskApproval() {
        val access = FakeAccess()
        var writes = 0
        val coordinator = coordinator(access, execute = { approved, _, _ ->
            writes++
            assertTrue(approved.ownerApproved)
            result()
        })
        val authorization = VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
            config, ownerConfirmed = true, foregroundSession = true, nowMillis = 100L
        )

        val result = coordinator.executeAutonomously(
            17, authorization, config, 101L, "github-token-long-enough", NeverStop
        )

        assertEquals("c".repeat(40), result.commitSha)
        assertEquals(1, writes)
        assertEquals("submitted", access.current!!.status)
    }

    @Test fun changedBindingRejectsAutonomousExecutionBeforeRepositoryWrite() {
        val access = FakeAccess()
        var writes = 0
        val coordinator = coordinator(access, execute = { _, _, _ -> writes++; result() })
        val authorization = VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
            config, ownerConfirmed = true, foregroundSession = true, nowMillis = 100L
        )

        assertThrows(SecurityException::class.java) {
            coordinator.executeAutonomously(
                17, authorization, config.copy(baseCommitSha = "d".repeat(40)),
                101L, "github-token-long-enough", NeverStop
            )
        }
        assertEquals(0, writes)
        assertEquals("awaiting_review", access.current!!.status)
    }

    @Test fun failedAutonomousWriteIsMarkedTerminalAndCannotBeReplayed() {
        val access = FakeAccess()
        val coordinator = coordinator(access, execute = { _, _, _ ->
            throw IllegalStateException("CI failed")
        })
        val authorization = VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
            config, ownerConfirmed = true, foregroundSession = true, nowMillis = 100L
        )

        assertThrows(IllegalStateException::class.java) {
            coordinator.executeAutonomously(
                17, authorization, config, 101L, "github-token-long-enough", NeverStop
            )
        }
        assertEquals("autonomous_failed", access.current!!.status)
        assertFalse(access.markAutonomousExecuting(17))
    }

    private fun coordinator(
        access: FakeAccess,
        execute: (VyomFactoryRepositoryChangePolicy.Plan, String, VyomFactoryExecutionControl) -> VyomFactoryRepositoryChangeExecutor.Proposal
    ): VyomFactoryRepositoryProposalExecutionCoordinator {
        val root = File.createTempFile("factory-auto-coordinator", ".tmp").apply { delete(); mkdirs() }
        val registry = VyomFactorySystemRegistry(root).apply { register(binding, ownerConfirmed = true) }
        return VyomFactoryRepositoryProposalExecutionCoordinator(
            registry = registry,
            proposals = access,
            fetchFreshTask = { task },
            verifyApproval = { _, _, _ -> throw AssertionError("per-task approval must not be requested") },
            executePlan = execute
        )
    }

    private inner class FakeAccess : VyomFactoryRepositoryProposalExecutionCoordinator.ProposalAccess {
        var current: VyomFactoryWorkerProposalStore.StoredProposal? =
            VyomFactoryWorkerProposalStore.StoredProposal(
                17, task.metadata.sourceId, task.updatedAt, "worker-test-01", "lease-17", candidate, "awaiting_review"
            )
        override fun get(issueNumber: Int) = current?.takeIf { it.issueNumber == issueNumber }
        override fun markApproved(issueNumber: Int) = false
        override fun markAutonomousExecuting(issueNumber: Int): Boolean {
            val value = get(issueNumber) ?: return false
            if (value.status != "awaiting_review") return false
            current = value.copy(status = "autonomous_executing")
            return true
        }
        override fun markAutonomousFailed(issueNumber: Int): Boolean {
            val value = get(issueNumber) ?: return false
            if (value.status != "autonomous_executing") return false
            current = value.copy(status = "autonomous_failed")
            return true
        }
        override fun markSubmitted(issueNumber: Int): Boolean {
            val value = get(issueNumber) ?: return false
            if (value.status !in setOf("approved", "autonomous_executing")) return false
            current = value.copy(status = "submitted")
            return true
        }
    }

    private fun result() = VyomFactoryRepositoryChangeExecutor.Proposal(
        "vyom-worker-17-attempt-12345678", "c".repeat(40),
        "$repo/pull/17", "exact-sha-ci-pass"
    )
    private object NeverStop : VyomFactoryExecutionControl { override fun shouldStop() = false }
}
