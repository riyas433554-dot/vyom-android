package com.hermes.assistant

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SelfModificationClientIntegrationTest {
    private val token = "test-token-that-is-long-enough-for-github"

    @Test fun rejectsProtectedPathBeforeMakingAnyGitHubRequest() {
        val server = MockWebServer()
        server.start()
        try {
            val client = client(server)
            assertRejected {
                client.createProposal(
                    token, "Change protected behavior",
                    "app/src/main/java/com/hermes/assistant/MainActivity.kt",
                    "package com.hermes.assistant",
                    ownerReviewedDiff = true
                )
            }
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test fun rejectsProposalWithoutExplicitOwnerDiffReviewBeforeAnyGitHubRequest() {
        val server = MockWebServer()
        server.start()
        try {
            val client = client(server)
            assertRejected {
                client.createProposal(
                    token = token,
                    title = "Improve reply formatting",
                    path = "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
                    proposedContent = """
                        package com.hermes.assistant
                        internal fun reply() = "reviewed"
                    """.trimIndent(),
                    ownerReviewedDiff = false
                )
            }
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test fun successfulProposalStopsAtUnmergedPrWithoutReleaseOrInstall() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(json("""{"object":{"sha":"${"a".repeat(40)}"}}"""))
            server.enqueue(json("""{"ref":"refs/heads/vyom-proposal-test"}"""))
            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not Found"}"""))
            server.enqueue(json("""{"content":{"sha":"${"b".repeat(40)}"}}"""))
            server.enqueue(json("""{"object":{"sha":"${"a".repeat(40)}"}}"""))
            server.enqueue(json("""{"html_url":"https://github.com/riyas433554-dot/vyom-android/pull/321"}"""))

            val client = client(server)
            val proposal = client.createProposal(
                token, "Improve reply formatting",
                "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
                "package com.hermes.assistant\ninternal fun reply() = \"reviewed\"",
                ownerReviewedDiff = true
            )
            assertEquals("https://github.com/riyas433554-dot/vyom-android/pull/321", proposal.pullRequestUrl)
            val requests = (0 until 6).map { server.takeRequest() }
            assertEquals(listOf("GET", "POST", "GET", "PUT", "GET", "POST"), requests.map { it.method })
            assertFalse(requests.any { it.path.orEmpty().contains("/merge") })
            assertFalse(requests.any { it.path.orEmpty().contains("/releases") })
        } finally {
            server.shutdown()
        }
    }

    @Test fun confirmedPrCreationFailureCleansBranchButNeverMergesOrInstalls() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(json("""{"object":{"sha":"${"a".repeat(40)}"}}"""))
            server.enqueue(json("""{"ref":"refs/heads/vyom-proposal-test"}"""))
            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not Found"}"""))
            server.enqueue(json("""{"content":{"sha":"${"b".repeat(40)}"}}"""))
            server.enqueue(json("""{"object":{"sha":"${"a".repeat(40)}"}}"""))
            server.enqueue(MockResponse().setResponseCode(422).setBody("""{"message":"Validation failed"}"""))
            server.enqueue(json("[]"))
            server.enqueue(MockResponse().setResponseCode(204))

            val client = client(server)
            assertRejected {
                client.createProposal(
                    token, "Improve reply formatting",
                    "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
                    "package com.hermes.assistant\ninternal fun reply() = \"reviewed\"",
                    ownerReviewedDiff = true
                )
            }

            val requests = (0 until 8).map { server.takeRequest() }
            assertEquals(listOf("GET", "POST", "GET", "PUT", "GET", "POST", "GET", "DELETE"),
                requests.map { it.method })
            assertTrue(requests[1].body.readUtf8().contains("refs/heads/vyom-proposal-"))
            assertTrue(requests[3].path!!.contains("/contents/"))
            assertTrue(requests[7].path!!.contains("/git/refs/heads/"))
            assertFalse(requests.any { it.path.orEmpty().contains("/merge") })
            assertFalse(requests.any { it.path.orEmpty().contains("/releases") })
        } finally {
            server.shutdown()
        }
    }

    @Test fun uncertainPrCreationFailurePreservesBranchForManualRecovery() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(json("""{"object":{"sha":"${"a".repeat(40)}"}}"""))
            server.enqueue(json("""{"ref":"refs/heads/vyom-proposal-test"}"""))
            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not Found"}"""))
            server.enqueue(json("""{"content":{"sha":"${"b".repeat(40)}"}}"""))
            server.enqueue(json("""{"object":{"sha":"${"a".repeat(40)}"}}"""))
            server.enqueue(MockResponse().setResponseCode(500).setBody("""{"message":"Server error"}"""))
            server.enqueue(json("[]"))

            val client = client(server)
            assertRejected {
                client.createProposal(
                    token, "Improve reply formatting",
                    "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
                    "package com.hermes.assistant\ninternal fun reply() = \"reviewed\"",
                    ownerReviewedDiff = true
                )
            }

            val requests = (0 until 7).map { server.takeRequest() }
            assertEquals(listOf("GET", "POST", "GET", "PUT", "GET", "POST", "GET"),
                requests.map { it.method })
            assertFalse(requests.any { it.method == "DELETE" })
        } finally {
            server.shutdown()
        }
    }

    @Test fun exactCommitCiFailureStopsProposalPipelineAndTimeoutDoesNotStartPolling() {
        val server = MockWebServer()
        server.start()
        try {
            val sha = "c".repeat(40)
            server.enqueue(json("""{"object":{"sha":"$sha"}}"""))
            server.enqueue(json(
                JSONObject().put("workflow_runs", org.json.JSONArray()
                    .put(JSONObject().put("name", "Vyom Android Debug").put("head_sha", sha)
                        .put("status", "completed").put("conclusion", "success").put("id", 1))
                    .put(JSONObject().put("name", "Vyom Android CI").put("head_sha", sha)
                        .put("status", "completed").put("conclusion", "failure").put("id", 2))
                ).toString()
            ))
            val client = client(server)
            assertRejected { client.waitForProposalCi(token, "vyom-proposal-test", 10_000) }
            assertEquals(2, server.requestCount)
            assertRejected { client.waitForProposalCi(token, "vyom-proposal-test", 0) }
            assertEquals(2, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    private fun client(server: MockWebServer): SelfModificationClient {
        val registry = VyomFactorySystemRegistry(File.createTempDir())
        registry.register(
            VyomFactorySystemRegistry.Binding(
                systemId = "self-modification",
                repositoryUrl = "https://github.com/riyas433554-dot/vyom-android",
                commitSha = "a".repeat(40),
                allowedPaths = listOf("app/src/main/java/com/hermes/assistant"),
                registeredAtEpochMs = 1L
            ),
            ownerConfirmed = true
        )
        return SelfModificationClient(
            registry = registry,
            apiRootOverrideForTests = server.url("/repos/test/repo").toString().trimEnd('/')
        )
    }

    private fun json(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected proposal workflow to fail closed")
        } catch (_: IllegalArgumentException) {
            // expected
        } catch (_: IllegalStateException) {
            // expected
        } catch (_: SecurityException) {
            // expected
        }
    }
}
