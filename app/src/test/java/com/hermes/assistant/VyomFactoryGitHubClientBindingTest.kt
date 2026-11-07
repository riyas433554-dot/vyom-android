package com.hermes.assistant

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VyomFactoryGitHubClientBindingTest {
    @Test fun issueRequestsUseTheOwnerApprovedRepositoryBinding() {
        val root = tempDir()
        val server = MockWebServer()
        server.start()
        try {
            val registry = VyomFactorySystemRegistry(root)
            registry.register(binding(), ownerConfirmed = true)
            server.enqueue(MockResponse().setBody("[]"))

            client(registry, server).listTasks()

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertTrue(request.path!!.startsWith("/repos/acme/factory/issues?"))
        } finally {
            server.shutdown()
            root.deleteRecursively()
        }
    }

    @Test fun missingTaskEngineBindingFailsBeforeNetworkAccess() {
        val root = tempDir()
        val server = MockWebServer()
        server.start()
        try {
            val registry = VyomFactorySystemRegistry(root)
            try {
                client(registry, server).listTasks()
                throw AssertionError("Expected missing task-engine binding to fail closed")
            } catch (_: SecurityException) { }
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
            root.deleteRecursively()
        }
    }

    @Test fun taskEngineBindingMustAuthorizeTheVirtualIssuesRoot() {
        val root = tempDir()
        val server = MockWebServer()
        server.start()
        try {
            val registry = VyomFactorySystemRegistry(root)
            registry.register(binding().copy(allowedPaths = listOf("app/src/main")), ownerConfirmed = true)
            try {
                client(registry, server).listTasks()
                throw AssertionError("Expected issues root authorization to fail")
            } catch (_: SecurityException) { }
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
            root.deleteRecursively()
        }
    }

    private fun client(registry: VyomFactorySystemRegistry, server: MockWebServer) =
        VyomFactoryGitHubClient(
            token = "test-token",
            registry = registry,
            http = OkHttpClient(),
            apiBaseUrl = server.url("/").toString().trimEnd('/')
        )

    private fun binding(allowedPaths: List<String> = listOf("issues")) =
        VyomFactorySystemRegistry.Binding(
            systemId = VyomFactoryGitHubClient.SYSTEM_ID,
            repositoryUrl = "https://github.com/acme/factory",
            commitSha = "a".repeat(40),
            allowedPaths = allowedPaths,
            registeredAtEpochMs = 1_000L
        )

    private fun tempDir(): File = File.createTempFile("vyom-factory-issues", ".tmp").apply {
        delete()
        mkdirs()
    }
}
