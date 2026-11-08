package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProposalBranchRecoveryTest {
    @Test fun preservesBranchWhenPullRequestWasCreatedDespiteLostResponse() {
        var deleted = false
        val result = ProposalBranchRecovery.recover(
            findExistingPullRequest = { "https://github.com/riyas433554-dot/vyom-android/pull/123" },
            deleteBranch = { deleted = true }
        )
        assertEquals("https://github.com/riyas433554-dot/vyom-android/pull/123", result)
        assertEquals(false, deleted)
    }

    @Test fun deletesOnlyWhenPullRequestAbsenceWasConfirmed() {
        var deleted = false
        val result = ProposalBranchRecovery.recover(
            findExistingPullRequest = { null },
            deleteBranch = { deleted = true }
        )
        assertNull(result)
        assertEquals(true, deleted)
    }

    @Test fun preservesBranchWhenPullRequestLookupIsUncertain() {
        var deleted = false
        val result = ProposalBranchRecovery.recover(
            findExistingPullRequest = { throw IllegalStateException("network unavailable") },
            deleteBranch = { deleted = true }
        )
        assertNull(result)
        assertEquals(false, deleted)
    }

    @Test fun cleanupFailureDoesNotEscapeRecoveryOrMaskOriginalFailure() {
        val result = ProposalBranchRecovery.recover(
            findExistingPullRequest = { null },
            deleteBranch = { throw IllegalStateException("delete denied") }
        )
        assertNull(result)
    }
}
