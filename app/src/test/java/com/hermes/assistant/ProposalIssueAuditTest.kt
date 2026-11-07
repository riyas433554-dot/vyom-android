package com.hermes.assistant

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProposalIssueAuditTest {
    @Test fun missingIssueLinkIsNotACommentFailure() {
        val server = MockWebServer()
        server.start()
        try {
            val registryRoot = tempDir()
            val registry = VyomFactorySystemRegistry(registryRoot)
            registry.register(
                VyomFactorySystemRegistry.Binding(
                    systemId = VyomFactoryGitHubClient.SYSTEM_ID,
                    repositoryUrl = "https://github.com/test/repo",
                    commitSha = "a".repeat(40),
                    allowedPaths = listOf(VyomFactoryGitHubClient.ISSUES_PATH),
                    registeredAtEpochMs = 1_000L
                ),
                ownerConfirmed = true
            )
            val client = VyomFactoryGitHubClient(
                token = "test-token",
                registry = registry,
                apiBaseUrl = server.url("/").toString().trimEnd('/')
            )
            assertTrue(ProposalIssueAudit.recordIfLinked(client, null, "proposal created"))
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test fun successfulIssueCommentIsReportedAsSaved() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":1}"""))
            val registryRoot = tempDir()
            val registry = VyomFactorySystemRegistry(registryRoot)
            registry.register(
                VyomFactorySystemRegistry.Binding(
                    systemId = VyomFactoryGitHubClient.SYSTEM_ID,
                    repositoryUrl = "https://github.com/test/repo",
                    commitSha = "a".repeat(40),
                    allowedPaths = listOf(VyomFactoryGitHubClient.ISSUES_PATH),
                    registeredAtEpochMs = 1_000L
                ),
                ownerConfirmed = true
            )
            val client = VyomFactoryGitHubClient(
                token = "test-token",
                registry = registry,
                apiBaseUrl = server.url("/").toString().trimEnd('/')
            )
            assertTrue(ProposalIssueAudit.recordIfLinked(client, 42, "PR created; no merge or install"))
            assertEquals("POST", server.takeRequest().method)
        } finally {
            server.shutdown()
        }
    }

    @Test fun failedIssueCommentIsExplicitlyReportedAndNeverTreatedAsSaved() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(500).setBody("""{"message":"comment write failed"}"""))
            val registryRoot = tempDir()
            val registry = VyomFactorySystemRegistry(registryRoot)
            registry.register(
                VyomFactorySystemRegistry.Binding(
                    systemId = VyomFactoryGitHubClient.SYSTEM_ID,
                    repositoryUrl = "https://github.com/test/repo",
                    commitSha = "a".repeat(40),
                    allowedPaths = listOf(VyomFactoryGitHubClient.ISSUES_PATH),
                    registeredAtEpochMs = 1_000L
                ),
                ownerConfirmed = true
            )
            val client = VyomFactoryGitHubClient(
                token = "test-token",
                registry = registry,
                apiBaseUrl = server.url("/").toString().trimEnd('/')
            )
            assertFalse(ProposalIssueAudit.recordIfLinked(client, 42, "PR created; no merge or install"))
            assertEquals("POST", server.takeRequest().method)
        } finally {
            server.shutdown()
        }
    }
    private fun tempDir(): File = File.createTempFile("proposal-issue-audit", ".tmp").apply {
        delete()
        mkdirs()
    }
}
