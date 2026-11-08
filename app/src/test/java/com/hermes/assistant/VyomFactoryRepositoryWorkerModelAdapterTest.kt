package com.hermes.assistant

import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class VyomFactoryRepositoryWorkerModelAdapterTest {
    @Test fun stopPreventsModelProposalParsing() {
        val root = File.createTempFile("repo-model", ".tmp").apply { delete(); mkdirs() }
        val registry = VyomFactorySystemRegistry(root)
        val task = VyomFactoryGitHubClient.Task(
            1, "Repository proposal", "", "open", "https://example.invalid/1", "now",
            VyomFactoryStatus.Metadata("P2", "open", "test", "source-12345678", "idempotency", emptyList())
        )
        assertThrows(VyomFactoryExecutionStoppedException::class.java) {
            VyomFactoryIssueEnvelopeWorkerModelAdapter().propose(
                task, registry, object : VyomFactoryExecutionControl {
                    override fun shouldStop() = true
                }
            )
        }
    }
}
