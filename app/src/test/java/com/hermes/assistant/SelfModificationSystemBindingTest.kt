package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SelfModificationSystemBindingTest {
    private val repository = "https://github.com/riyas433554-dot/vyom-android"
    private val commit = "a".repeat(40)
    private val allowedPath = "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt"

    private fun tempDir(): File = File.createTempFile("vyom-registry-test", ".tmp").apply {
        delete()
        mkdirs()
    }

    private fun registry(root: File, paths: List<String> = listOf("app/src/main/java/com/hermes/assistant")): VyomFactorySystemRegistry {
        val registry = VyomFactorySystemRegistry(root)
        registry.register(
            VyomFactorySystemRegistry.Binding(
                systemId = "self-modification",
                repositoryUrl = repository,
                commitSha = commit,
                allowedPaths = paths,
                registeredAtEpochMs = 1L
            ),
            ownerConfirmed = true
        )
        return registry
    }

    @Test(expected = SecurityException::class)
    fun clientRejectsMissingOwnerApprovedBindingBeforeAnyNetworkRequest() {
        val client = SelfModificationClient(VyomFactorySystemRegistry(tempDir()))
        val draft = SelfModificationClient.Draft(
            title = "Improve reply formatting",
            path = allowedPath,
            baseSha = commit,
            proposedContent = "package com.hermes.assistant",
            preview = "exact diff",
            requestSummary = "test missing binding"
        )
        val gate = SelfModificationApprovalGate(nowMillis = { 1_000L })
        val approval = gate.issueFromOwnerReview(draft)
        client.createProposal(
            token = "test-token-that-is-long-enough-for-github",
            draft = draft,
            approval = approval,
            approvalGate = gate
        )
    }

    @Test(expected = SecurityException::class)
    fun rejectsSelfModificationWithoutOwnerApprovedSystemBinding() {
        SelfModificationPolicy.requireSystemBinding(
            VyomFactorySystemRegistry(tempDir()),
            "self-modification", repository, commit, allowedPath
        )
    }

    @Test(expected = SecurityException::class)
    fun rejectsRepositoryDifferentFromRegisteredSystemRepository() {
        SelfModificationPolicy.requireSystemBinding(
            registry(tempDir()),
            "self-modification", "https://github.com/other/repo", commit, allowedPath
        )
    }

    @Test(expected = SecurityException::class)
    fun rejectsCommitDifferentFromRegisteredImmutableBase() {
        SelfModificationPolicy.requireSystemBinding(
            registry(tempDir()),
            "self-modification", repository, "b".repeat(40), allowedPath
        )
    }

    @Test(expected = SecurityException::class)
    fun rejectsPathOutsideRegistryAllowedRoots() {
        SelfModificationPolicy.requireSystemBinding(
            registry(tempDir(), listOf("app/src/main/java/com/hermes/assistant/safe")),
            "self-modification", repository, commit, allowedPath
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathOutsideSelfModificationPolicyEvenWhenRegistryAllowsIt() {
        SelfModificationPolicy.requireSystemBinding(
            registry(tempDir(), listOf("app/src/main/java/com/hermes/assistant")),
            "self-modification", repository, commit,
            "app/src/main/java/com/hermes/assistant/MainActivity.kt"
        )
    }

    @Test
    fun returnsExactApprovedBindingForAllowedPath() {
        val binding = SelfModificationPolicy.requireSystemBinding(
            registry(tempDir()),
            "self-modification", repository, commit, allowedPath
        )
        assertEquals(repository, binding.repositoryUrl)
        assertEquals(commit, binding.commitSha)
    }
}
