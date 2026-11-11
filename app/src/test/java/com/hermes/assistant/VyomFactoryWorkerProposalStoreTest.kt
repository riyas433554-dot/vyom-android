package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class VyomFactoryWorkerProposalStoreTest {
    private data class Fixture(
        val root: File,
        val store: VyomFactoryWorkerProposalStore,
        val task: VyomFactoryGitHubClient.Task,
        val candidate: VyomFactoryRepositoryChangeReviewDialog.Candidate
    )

    private fun fixture(): Fixture {
        val root = File(System.getProperty("java.io.tmpdir"), "worker-proposal-${UUID.randomUUID()}")
        val store = VyomFactoryWorkerProposalStore(root)
        val binding = VyomFactoryWorkerBindingPolicy.createBinding(
            "https://github.com/riyas433554-dot/vyom-android",
            "a".repeat(40),
            1L
        )
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
        return Fixture(root, store, task, candidate)
    }

    @Test fun ownerApprovalPersistsAndMovesProposalOutOfPendingReview() {
        val f = fixture()
        f.store.save(f.task, "worker-test-101", "lease-test-101", f.candidate)

        assertTrue(f.store.markApproved(f.task.number))
        assertTrue(f.store.markApproved(f.task.number))
        assertFalse(f.store.markApproved(999))

        val reloaded = VyomFactoryWorkerProposalStore(f.root)
        assertEquals("approved", reloaded.get(f.task.number)?.status)
        assertTrue(reloaded.pending().isEmpty())
        assertEquals(1, reloaded.approved().size)
        assertTrue(reloaded.isHandled(f.task))

        // A repeated dispatch for the same source/update cannot reset owner approval.
        assertEquals("approved", reloaded.save(
            f.task, "worker-test-101", "lease-test-102", f.candidate
        ).status)
    }

    @Test fun verifiedSubmissionMovesApprovedProposalToTerminalHandledState() {
        val f = fixture()
        f.store.save(f.task, "worker-test-101", "lease-test-101", f.candidate)
        assertTrue(f.store.markApproved(f.task.number))

        assertTrue(f.store.markSubmitted(f.task.number))
        assertTrue(f.store.markSubmitted(f.task.number))

        val reloaded = VyomFactoryWorkerProposalStore(f.root)
        assertEquals("submitted", reloaded.get(f.task.number)?.status)
        assertTrue(reloaded.pending().isEmpty())
        assertTrue(reloaded.approved().isEmpty())
        assertTrue(reloaded.isHandled(f.task))
        assertFalse(reloaded.markApproved(f.task.number))
    }

    @Test fun rejectedProposalCannotBeApproved() {
        val f = fixture()
        f.store.save(f.task, "worker-test-101", "lease-test-101", f.candidate)
        assertTrue(f.store.markRejected(f.task.number))

        assertFalse(f.store.markApproved(f.task.number))
        assertEquals("rejected", f.store.get(f.task.number)?.status)
    }
}
