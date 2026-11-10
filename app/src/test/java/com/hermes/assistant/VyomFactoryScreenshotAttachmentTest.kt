package com.hermes.assistant

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

class VyomFactoryScreenshotAttachmentTest {
    private val repositoryUrl = "https://github.com/test/repo"
    private val baseCommit = "a".repeat(40)
    private val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Test fun publicRepositoryIsRejectedBeforeBranchCreationOrUpload() {
        val server = MockWebServer().apply { start() }
        val root = createTempDir(prefix = "public-evidence-")
        try {
            server.enqueue(json("""{"private":false}"""))
            val (publisher, file) = publisher(server, root)
            assertRejected {
                publisher.publish("factory-evidence", repositoryUrl, baseCommit, 42,
                    ".factory-evidence/issue-42/image.png", file)
            }
            assertEquals(1, server.requestCount)
            assertFalse(server.takeRequest().path!!.contains("/contents/"))
        } finally {
            root.deleteRecursively()
            server.shutdown()
        }
    }

    @Test fun privateRepositoryUsesDedicatedBranchExpectedHeadHashVerificationThenIssueComment() {
        val server = MockWebServer().apply { start() }
        val root = createTempDir(prefix = "private-evidence-")
        try {
            server.enqueue(json("""{"private":true}"""))
            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not Found"}"""))
            server.enqueue(json("""{"ref":"refs/heads/factory-evidence","object":{"sha":"$baseCommit"}}""", 201))
            server.enqueue(json("""{"ref":"refs/heads/factory-evidence","object":{"sha":"$baseCommit"}}"""))
            server.enqueue(json("""{"tree":{"sha":"dddddddddddddddddddddddddddddddddddddddd"}}"""))
            server.enqueue(json("""{"sha":"eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"}""", 201))
            server.enqueue(json("""{"sha":"ffffffffffffffffffffffffffffffffffffffff"}""", 201))
            server.enqueue(json("""{"sha":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}""", 201))
            server.enqueue(json("""{"ref":"refs/heads/factory-evidence","object":{"sha":"$baseCommit"}}"""))
            server.enqueue(json("""{"ref":"refs/heads/factory-evidence","object":{"sha":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}}""", 200))
            server.enqueue(json("""{"ref":"refs/heads/factory-evidence","object":{"sha":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}}"""))
            server.enqueue(json(
                JSONObject().put("type", "file")
                    .put("path", ".factory-evidence/issue-42/image.png")
                    .put("encoding", "base64")
                    .put("content", Base64.getEncoder().encodeToString(imageBytes))
                    .put("size", imageBytes.size).toString()
            ))
            server.enqueue(json("""{"id":123}""", 201))

            val (publisher, file) = publisher(server, root)
            val result = publisher.publish("factory-evidence", repositoryUrl, baseCommit, 42,
                ".factory-evidence/issue-42/image.png", file)

            assertTrue(result.issueCommentRecorded)
            assertEquals("b".repeat(40), result.commitSha)
            assertEquals(VyomFactoryScreenshotEvidencePolicy.sha256(imageBytes), result.sha256)
            assertNull(VyomFactoryScreenshotEvidenceStore(File(root, "pending")).uploadReceipt(file))

            val requests = (0 until 13).map { server.takeRequest() }
            assertEquals("GET", requests[0].method) // repository privacy
            assertEquals("GET", requests[1].method) // branch absent
            assertEquals("POST", requests[2].method) // create dedicated branch from pinned commit
            assertTrue(requests[2].body.readUtf8().contains("refs/heads/factory-evidence"))
            assertEquals("POST", requests[5].method) // blob
            assertEquals("POST", requests[6].method) // tree
            assertEquals("POST", requests[7].method) // commit parented to expected head
            assertEquals("GET", requests[8].method) // expected-head recheck immediately before ref update
            assertEquals("PATCH", requests[9].method)
            assertTrue(requests[9].body.readUtf8().contains("\"force\":false"))
            assertEquals("GET", requests[11].method) // read-back by immutable commit SHA
            assertTrue(requests[11].path!!.contains("ref=" + "b".repeat(40)))
            assertEquals("POST", requests[12].method) // issue comment occurs only after hash read-back
            assertTrue(requests[12].body.readUtf8().contains(result.sha256))
        } finally {
            root.deleteRecursively()
            server.shutdown()
        }
    }

    private fun publisher(server: MockWebServer, root: File): Pair<VyomFactoryEvidencePublisher, File> {
        val registryRoot = File(root, "registry")
        VyomFactorySystemRegistry(registryRoot).register(
            VyomFactorySystemRegistry.Binding(
                "factory-evidence", repositoryUrl, baseCommit, listOf(".factory-evidence"), 1L
            ),
            ownerConfirmed = true
        )
        val pending = File(root, "pending").apply { mkdirs() }
        val file = File(pending, "image.png").apply { writeBytes(imageBytes) }
        val store = VyomFactoryScreenshotEvidenceStore(pending)
        val transport = VyomFactoryGitHubEvidenceTransport(
            "test-token", "test/repo", server.url("/").toString().trimEnd('/')
        )
        return VyomFactoryEvidencePublisher(VyomFactorySystemRegistry(registryRoot), transport, store) to file
    }

    private fun json(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            fail("Expected public repository to be rejected")
        } catch (_: IllegalStateException) {
        }
    }
}
