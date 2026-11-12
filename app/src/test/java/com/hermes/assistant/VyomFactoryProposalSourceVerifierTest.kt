package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryProposalSourceVerifierTest {
    @Test fun capturesEveryProposalFileAtTheExactRegisteredCommit() {
        val (plan, binding, registry) = fixture()
        val hashes = VyomFactoryProposalSourceVerifier.capture(plan, binding, registry) { systemId, path, current ->
            assertEquals("worker", systemId)
            assertEquals(registry.get("worker"), current.get("worker"))
            preview(path, "a".repeat(64))
        }

        assertEquals(mapOf(PATH to "a".repeat(64)), hashes)
    }

    @Test fun blocksMissingOrMismatchedSourceHashBeforeReview() {
        val (plan, binding, registry) = fixture()
        val candidate = VyomFactoryRepositoryChangeReviewDialog.Candidate(
            plan, binding, "summary", mapOf(PATH to "a".repeat(64))
        )

        org.junit.Assert.assertThrows(SecurityException::class.java) {
            VyomFactoryProposalSourceVerifier.verify(candidate, registry) { _, path, _ ->
                preview(path, "b".repeat(64))
            }
        }
        org.junit.Assert.assertThrows(SecurityException::class.java) {
            VyomFactoryProposalSourceVerifier.verify(candidate.copy(sourceFileSha256 = emptyMap()), registry) { _, path, _ ->
                preview(path, "a".repeat(64))
            }
        }
    }

    private fun fixture(): Triple<
        VyomFactoryRepositoryChangePolicy.Plan,
        VyomFactorySystemRegistry.Binding,
        VyomFactorySystemRegistry
    > {
        val root = java.io.File.createTempFile("proposal-source", ".tmp").apply { delete(); mkdirs() }
        val registry = VyomFactorySystemRegistry(root)
        val binding = VyomFactorySystemRegistry.Binding(
            "worker", "https://github.com/acme/repo", "c".repeat(40),
            listOf("app/src/main"), 1L
        )
        registry.register(binding, ownerConfirmed = true)
        val plan = VyomFactoryRepositoryChangePolicy.Plan(
            "worker", 7, "attempt-12345678", "Verify proposal source files",
            "feature/work", binding.commitSha,
            listOf(VyomFactoryRepositoryChangePolicy.Change(PATH, "proposed content")), false
        )
        return Triple(plan, binding, registry)
    }

    private fun preview(path: String, hash: String) = VyomFactoryRepositoryClient.FilePreview(
        path, "source", 6, "blob-sha", hash, 0
    )

    companion object {
        private const val PATH = "app/src/main/java/com/hermes/assistant/Allowed.kt"
    }
}