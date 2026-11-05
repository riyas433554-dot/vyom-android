package com.hermes.assistant

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VyomFactoryScreenshotAttachmentTest {
    @Test fun publicRepositoryIsRejectedBeforeIssueReadOrAnyUpload() {
        val server = MockWebServer()
        server.start()
        val file = screenshotFile()
        try {
            server.enqueue(json("""{"private":false}"""))
            val client = VyomFactoryGitHubClient("test-token", "test/repo", apiBaseUrl = server.url("/").toString().trimEnd('/'))
            assertRejected { client.attachScreenshot(42, file) }
            assertEquals(1, server.requestCount)
            assertFalse(server.takeRequest().path!!.contains("/contents/"))
        } finally {
            file.delete()
            server.shutdown()
        }
    }

    @Test fun privateRepositoryUploadAndIssueCommentOccurOnlyAfterAttachCall() {
        val server = MockWebServer()
        server.start()
        val file = screenshotFile()
        try {
            server.enqueue(json("""{"private":true}"""))
            server.enqueue(json(JSONArray().put(issue()).toString()))
            server.enqueue(json("""{"private":true}"""))
            server.enqueue(json(
                JSONObject()
                    .put("content", JSONObject().put("html_url", "https://github.com/test/repo/blob/feature/memory-intelligence/.factory-evidence/issue-42/test.png"))
                    .put("commit", JSONObject().put("sha", "b".repeat(40)))
                    .toString()
            ))
            server.enqueue(json("""{"id":123}"""))

            val client = VyomFactoryGitHubClient("test-token", "test/repo", apiBaseUrl = server.url("/").toString().trimEnd('/'))
            val result = client.attachScreenshot(42, file)

            assertTrue(result.issueCommentRecorded)
            assertEquals("b".repeat(40), result.commitSha)
            assertEquals(VyomFactoryScreenshotEvidencePolicy.sha256(file.readBytes()), result.sha256)
            val requests = (0 until 5).map { server.takeRequest() }
            assertEquals("GET", requests[0].method)
            assertEquals("GET", requests[1].method)
            assertEquals("GET", requests[2].method)
            assertEquals("PUT", requests[3].method)
            assertTrue(requests[3].path!!.contains(".factory-evidence/issue-42/"))
            assertEquals("POST", requests[4].method)
            assertTrue(requests[4].body.readUtf8().contains("SHA-256"))
        } finally {
            file.delete()
            server.shutdown()
        }
    }

    private fun issue(): JSONObject {
        val metadata = VyomFactoryStatus.Metadata(
            priority = "P2",
            status = "open",
            source = "factory",
            sourceId = "screenshot-test",
            idempotencyKey = "factory:screenshot-test",
            events = listOf("created")
        )
        return JSONObject()
            .put("number", 42)
            .put("title", "[VYOM FACTORY][P2] Screenshot test")
            .put("body", VyomFactoryStatus.withMetadata("Verification probe: confirm screenshot appears", metadata))
            .put("state", "open")
            .put("html_url", "https://github.com/test/repo/issues/42")
            .put("updated_at", "2026-11-05T00:00:00Z")
    }

    private fun screenshotFile(): File = File.createTempFile("vyom-screenshot-", ".png").apply {
        writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
    }

    private fun json(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected public repository to be rejected")
        } catch (_: IllegalStateException) {
            // expected
        }
    }
}
