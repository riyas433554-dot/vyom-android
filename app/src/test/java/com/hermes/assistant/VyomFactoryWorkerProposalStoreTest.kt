package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class VyomFactoryWorkerProposalStoreTest {
    @Test fun ownerApprovedProposalLeavesPendingReviewAndRemainsHandled() {
        val root = File(System.getProperty("java.io.tmpdir"), "worker-proposal-${UUID.randomUUID()}")
        val store = VyomFactoryWorkerProposalStore(root)
        val registry = VyomFactorySystemRegistry(File(root, "registry"))
        val binding = VyomFactoryWorkerBindingPolicy.createBinding(
            "https://github.com/riyas433554-dot/vyom-android",
            "a".repeat(40),
            1L
        )
        registry.register(binding, ownerConfirmed = true)
        val metadata = VyomFactoryStatus.Metadata(
            "P1", "open", "chatgpt", "worker-issue-101",
            VyomFactoryStatus.idempotencyKey("chatgpt", "worker-issue-101")!!,
            emptyList(), VyomFactoryLaneRouter.Lane.CUSTOM, true
        )
        val task = VyomFactoryGitHubClient.Task(
            101, "Fix Factory task runner", "Repair the Factory runner.", "open",
            "https://github.com/riyas433554-dot/vyom-android/issues/101",
            "2026-11-11T10:00:00Z", metadata, "2026-11-10T10:00:00Z"
        )
        val candidate = VyomFactoryRepositoryChangeReviewDialog.Candidate(
            VyomFactoryRepositoryChangePolicy.Plan(
                binding.systemId, task.number, "attempt-101-000",
                "Fix Factory task runner", SelfModificationPolicy.PROPOSAL_BASE_BRANCH,
                binding.commitSha,
                listOf(VyomFactoryRepositoryChangePolicy.Change("README.md", "Factory runner fixed")),
                ownerApproved = false
            ),
            binding,
            "Factory self-upgrade proposal"
        )
        store.save(task, "worker-test-101", "lease-test-101", candidate)

        assertTrue(store.markApproved(task.number))

        assertEquals("approved", store.get(task.number)?.status)
        assertTrue(store.pending().isEmpty())
        assertTrue(store.isHandled(task))
        assertFalse(store.markApproved(999))
    }
}
