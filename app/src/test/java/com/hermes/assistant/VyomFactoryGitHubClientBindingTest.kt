package com.hermes.assistant

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest

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

    @Test fun simultaneousSubmissionsWithTheSameIdempotencyKeyCreateOnlyOneIssue() {
        val root = tempDir()
        val server = MockWebServer()
        val issueCreated = AtomicBoolean(false)
        val issuePostCount = AtomicInteger()
        val metadata = VyomFactoryStatus.Metadata(
            priority = "P2", status = "open", source = "factory", sourceId = "same-source-id",
            idempotencyKey = "factory:same-source-id", events = listOf("created")
        )
        val issue = org.json.JSONObject()
            .put("number", 42).put("title", "[VYOM FACTORY][P2] Duplicate guard")
            .put("body", VyomFactoryStatus.withMetadata("Same complaint", metadata))
            .put("state", "open").put("html_url", "https://github.com/acme/factory/issues/42")
            .put("updated_at", "2026-11-12T00:00:00Z").put("created_at", "2026-11-12T00:00:00Z")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.method == "GET" && request.path!!.startsWith("/repos/acme/factory/issues?") -> {
                    if (!issueCreated.get()) Thread.sleep(300)
                    MockResponse().setBody(if (issueCreated.get()) org.json.JSONArray().put(issue).toString() else "[]")
                }
                request.method == "POST" && request.path == "/repos/acme/factory/issues" -> {
                    issuePostCount.incrementAndGet()
                    issueCreated.set(true)
                    MockResponse().setBody(issue.toString())
                }
                request.method == "POST" && request.path == "/repos/acme/factory/issues/42/comments" ->
                    MockResponse().setBody("{}")
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
        try {
            val registry = VyomFactorySystemRegistry(root)
            registry.register(binding(), ownerConfirmed = true)
            val api = client(registry, server)
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)
            val submissions = (1..2).map {
                pool.submit<Int> {
                    start.await(2, TimeUnit.SECONDS)
                    api.createTask(
                        title = "Duplicate guard", description = "Same complaint", priority = "P2",
                        source = "factory", sourceId = "same-source-id"
                    ).number
                }
            }
            start.countDown()
            assertEquals(42, submissions[0].get(5, TimeUnit.SECONDS))
            assertEquals(42, submissions[1].get(5, TimeUnit.SECONDS))
            pool.shutdownNow()
            assertEquals(1, issuePostCount.get())
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
