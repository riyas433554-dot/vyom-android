package com.hermes.assistant

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class VyomFactoryGitHubClientTest {
    @Test fun duplicateCreateRetryReturnsExistingTaskWithoutMutation() {
        val server = MockWebServer()
        val source = "vyom_chat"
        val sourceId = "chat-message-123"
        val key = VyomFactoryStatus.idempotencyKey(source, sourceId)!!
        val metadata = VyomFactoryStatus.Metadata(
            priority = "P1",
            status = "open",
            source = source,
            sourceId = sourceId,
            idempotencyKey = key,
            events = listOf("created")
        )
        val existingBody = VyomFactoryStatus.withMetadata("Original task description", metadata)
        val issue = JSONObject()
            .put("number", 73)
            .put("title", "[VYOM FACTORY][P1] Original task")
            .put("body", existingBody)
            .put("state", "open")
            .put("html_url", "https://github.com/example/repo/issues/73")
            .put("updated_at", "2026-11-03T00:00:00Z")
        server.enqueue(MockResponse().setResponseCode(200).setBody(JSONArray().put(issue).toString()))
        server.start()
        try {
            val client = VyomFactoryGitHubClient(
                token = "test-token",
                repository = "example/repo",
                http = OkHttpClient(),
                apiBaseUrl = server.url("/").toString().trimEnd('/')
            )
            val result = client.createTask(
                title = "Changed retry title",
                description = "Changed retry description",
                priority = "P0",
                source = source,
                sourceId = sourceId
            )
            assertEquals(73, result.number)
            assertEquals("[VYOM FACTORY][P1] Original task", result.title)
            assertEquals(existingBody, result.body)
            assertEquals("P1", result.metadata.priority)
            assertEquals("Original task description", result.body.substringBefore("<!-- VYOM_FACTORY_META:").trim())
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertTrue(request.path!!.contains("/issues?state=all"))
            assertFalse(server.takeRequest(100, TimeUnit.MILLISECONDS) != null)
        } finally {
            server.shutdown()
        }
    }
}
