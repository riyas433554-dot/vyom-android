package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Test

class SelfModificationApprovalGateTest {
    private fun draft(content: String = "package com.hermes.assistant\ninternal fun value() = 1") =
        SelfModificationClient.Draft(
            title = "Improve reply formatting",
            path = "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
            baseSha = "a".repeat(40),
            proposedContent = content,
            preview = "exact diff preview",
            requestSummary = "Improve reply formatting"
        )

    @Test fun approvalIsBoundToExactDraftAndCanBeConsumedOnlyOnce() {
        val now = 1_000L
        val gate = SelfModificationApprovalGate(
            nowMillis = { now },
            newNonce = { "approval-nonce-00000001" }
        )
        val approvedDraft = draft()
        val token = gate.issueFromOwnerReview(approvedDraft)

        gate.consume(approvedDraft, token)
        assertFalse(runCatching { gate.consume(approvedDraft, token) }.isSuccess)
    }

    @Test fun changedDraftCannotReuseApprovalToken() {
        val gate = SelfModificationApprovalGate(
            nowMillis = { 1_000L },
            newNonce = { "approval-nonce-00000002" }
        )
        val token = gate.issueFromOwnerReview(draft())

        assertFalse(runCatching { gate.consume(draft("different source"), token) }.isSuccess)
        assertFalse(runCatching { gate.consume(draft(), token) }.isSuccess)
    }

    @Test fun expiredOrClearedApprovalCannotBeConsumed() {
        var now = 1_000L
        val gate = SelfModificationApprovalGate(
            nowMillis = { now },
            newNonce = { "approval-nonce-00000003" }
        )
        val expiredDraft = draft()
        val expired = gate.issueFromOwnerReview(expiredDraft)
        now += SelfModificationApprovalGate.TOKEN_TTL_MS + 1
        assertFalse(runCatching { gate.consume(expiredDraft, expired) }.isSuccess)

        now = 10_000L
        val clearedDraft = draft("second content")
        val cleared = gate.issueFromOwnerReview(clearedDraft)
        gate.clear()
        assertFalse(runCatching { gate.consume(clearedDraft, cleared) }.isSuccess)
    }
}
