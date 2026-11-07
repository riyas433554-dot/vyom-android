package com.hermes.assistant

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SelfModificationPathDiscoveryTest {
    private val token = "test-token-that-is-long-enough-for-github"
    private val apiKey = "nvapi-test-key-that-is-long-enough"
    private val sha = "a".repeat(40)
    private val title = "Fix Markdown rendering"
    private val task = "Fix the chat Markdown list rendering in long replies."

    @Test fun returnsOnlyModelSelectedPathsPresentInTheCurrentAllowedTree() {
        val github = MockWebServer()
        val model = MockWebServer()
        github.start()
        model.start()
        try {
            github.enqueue(json("""{"object":{"sha":"$sha"}}"""))
            github.enqueue(json(tree(
                "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
                "app/src/main/java/com/hermes/assistant/feature/ChatMarkdown.kt",
                "app/src/main/java/com/hermes/assistant/MainActivity.kt"
            )))
            github.enqueue(json("""{"object":{"sha":"$sha"}}"""))
            model.enqueue(json("""{"choices":[{"finish_reason":"stop","message":{"content":"{\"paths\":[\"app/src/main/java/com/hermes/assistant/feature/ChatMarkdown.kt\",\"app/src/main/java/com/hermes/assistant/ReplyFormatter.kt\"]}"}}]}"""))

            val client = client(github, model)
            val result = client.suggestAllowedPaths(token, apiKey, title, task, "test-model", 3)

            assertEquals(sha, result.baseSha)
            assertEquals(
                listOf(
                    "app/src/main/java/com/hermes/assistant/feature/ChatMarkdown.kt",
                    "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt"
                ),
                result.paths
            )
            assertEquals(3, github.requestCount)
            assertEquals(1, model.requestCount)
            val githubRequests = (0 until 3).map { github.takeRequest() }
            assertEquals(listOf("GET", "GET", "GET"), githubRequests.map { it.method })
            assertTrue(githubRequests[1].path.orEmpty().contains("/git/trees/$sha?recursive=1"))
            val modelRequest = model.takeRequest()
            assertEquals("POST", modelRequest.method)
            val modelBody = modelRequest.body.readUtf8()
            assertTrue(modelBody.contains("allowed_paths"))
            assertFalse(modelBody.contains("current_source"))
        } finally {
            github.shutdown()
            model.shutdown()
        }
    }

    @Test fun rejectsModelPathNotInAllowedTreeBeforeReturningSuggestions() {
        val github = MockWebServer()
        val model = MockWebServer()
        github.start()
        model.start()
        try {
            github.enqueue(json("""{"object":{"sha":"$sha"}}"""))
            github.enqueue(json(tree("app/src/main/java/com/hermes/assistant/ReplyFormatter.kt")))
            model.enqueue(json("""{"choices":[{"finish_reason":"stop","message":{"content":"{\"paths\":[\"app/src/main/java/com/hermes/assistant/MainActivity.kt\"]}"}}]}"""))

            assertRejected {
                client(github, model).suggestAllowedPaths(token, apiKey, title, task, "test-model", 3)
            }
            assertEquals(2, github.requestCount)
            assertEquals(1, model.requestCount)
        } finally {
            github.shutdown()
            model.shutdown()
        }
    }

    @Test fun rejectsSensitiveComplaintBeforeAnyGitHubOrModelRequest() {
        val github = MockWebServer()
        val model = MockWebServer()
        github.start()
        model.start()
        try {
            assertRejected {
                client(github, model).suggestAllowedPaths(
                    token, apiKey, "Fix this API key nvapi-abcdefghijklmnop",
                    task, "test-model", 3
                )
            }
            assertEquals(0, github.requestCount)
            assertEquals(0, model.requestCount)
        } finally {
            github.shutdown()
            model.shutdown()
        }
    }

    @Test fun rejectsTruncatedRepositoryTreeWithoutAskingTheModel() {
        val github = MockWebServer()
        val model = MockWebServer()
        github.start()
        model.start()
        try {
            github.enqueue(json("""{"object":{"sha":"$sha"}}"""))
            github.enqueue(json("""{"truncated":true,"tree":[{"path":"app/src/main/java/com/hermes/assistant/ReplyFormatter.kt","type":"blob"}]}"""))

            assertRejected {
                client(github, model).suggestAllowedPaths(token, apiKey, title, task, "test-model", 3)
            }
            assertEquals(2, github.requestCount)
            assertEquals(0, model.requestCount)
        } finally {
            github.shutdown()
            model.shutdown()
        }
    }

    @Test fun rejectsBranchChangeDuringPathDiscovery() {
        val github = MockWebServer()
        val model = MockWebServer()
        github.start()
        model.start()
        try {
            github.enqueue(json("""{"object":{"sha":"$sha"}}"""))
            github.enqueue(json(tree("app/src/main/java/com/hermes/assistant/ReplyFormatter.kt")))
            github.enqueue(json("""{"object":{"sha":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}}"""))
            model.enqueue(json("""{"choices":[{"finish_reason":"stop","message":{"content":"{\"paths\":[\"app/src/main/java/com/hermes/assistant/ReplyFormatter.kt\"]}"}}]}"""))

            assertRejected {
                client(github, model).suggestAllowedPaths(token, apiKey, title, task, "test-model", 3)
            }
            assertEquals(3, github.requestCount)
            assertEquals(1, model.requestCount)
        } finally {
            github.shutdown()
            model.shutdown()
        }
    }

    private fun client(github: MockWebServer, model: MockWebServer): SelfModificationClient {
        val registry = VyomFactorySystemRegistry(File.createTempDir())
        registry.register(
            VyomFactorySystemRegistry.Binding(
                systemId = "self-modification",
                repositoryUrl = "https://github.com/riyas433554-dot/vyom-android",
                commitSha = sha,
                allowedPaths = listOf("app/src/main/java/com/hermes/assistant"),
                registeredAtEpochMs = 1L
            ),
            ownerConfirmed = true
        )
        return SelfModificationClient(
            registry = registry,
            modelApiRoot = model.url("/v1/chat/completions").toString(),
            apiRootOverrideForTests = github.url("/repos/test/repo").toString().trimEnd('/')
        )
    }

    private fun tree(vararg paths: String): String =
        JSONObject()
            .put("truncated", false)
            .put("tree", org.json.JSONArray().apply {
                paths.forEach { put(JSONObject().put("path", it).put("type", "blob")) }
            })
            .toString()

    private fun json(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected path discovery to fail closed")
        } catch (_: IllegalArgumentException) {
            // expected
        } catch (_: IllegalStateException) {
            // expected
        } catch (_: SecurityException) {
            // expected
        }
    }
}
