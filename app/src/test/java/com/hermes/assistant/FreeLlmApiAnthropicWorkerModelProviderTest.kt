package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class FreeLlmApiAnthropicWorkerModelProviderTest {
    private class FakeTransport(
        var response: VyomFactoryWorkerModelHttpTransport.Response =
            VyomFactoryWorkerModelHttpTransport.Response(
                200,
                JSONObject()
                    .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", "{\"action\":\"final\"}")))
                    .put("usage", JSONObject().put("input_tokens", 31).put("output_tokens", 17))
                    .toString()
            )
    ) : VyomFactoryWorkerModelHttpTransport {
        var url: String? = null
        var headers: Map<String, String> = emptyMap()
        var body: String? = null
        var timeoutMillis: Long = 0
        var cancelled = false

        override fun post(
            url: String,
            headers: Map<String, String>,
            body: String,
            timeoutMillis: Long
        ): VyomFactoryWorkerModelHttpTransport.Response {
            this.url = url
            this.headers = headers
            this.body = body
            this.timeoutMillis = timeoutMillis
            return response
        }

        override fun cancel() { cancelled = true }
    }

    private fun request() = VyomFactoryWorkerModelProvider.Request(
        taskNumber = 12,
        repository = "owner/repo",
        baseSha = "a".repeat(40),
        baseBranch = "feature/worker",
        allowedPaths = listOf("app/src/main/java/com/hermes/assistant"),
        systemInstructions = "You are Vyom. Return JSON only.",
        taskDescriptionData = "UNTRUSTED ISSUE DATA"
    )

    @Test fun sendsAnthropicMessagesRequestToLoopbackWithUnifiedKey() {
        val transport = FakeTransport()
        val provider = FreeLlmApiAnthropicWorkerModelProvider(transport = transport)

        val response = provider.generate(request(), "freellmapi-test-key".toCharArray())

        assertEquals("freellmapi-anthropic", provider.providerId)
        assertEquals(FreeLlmApiAnthropicWorkerModelProvider.ENDPOINT, transport.url)
        assertEquals("freellmapi-test-key", transport.headers["x-api-key"])
        assertEquals("2023-06-01", transport.headers["anthropic-version"])
        assertEquals("application/json", transport.headers["Content-Type"])
        assertFalse(transport.url!!.contains("freellmapi-test-key"))
        assertFalse(transport.body!!.contains("freellmapi-test-key"))
        val payload = JSONObject(transport.body!!)
        assertEquals(FreeLlmApiAnthropicWorkerModelProvider.DEFAULT_MODEL_ID, payload.getString("model"))
        assertEquals(8192, payload.getInt("max_tokens"))
        assertEquals("You are Vyom. Return JSON only.", payload.getString("system"))
        assertTrue(payload.getJSONArray("messages").getJSONObject(0).getString("content")
            .contains("UNTRUSTED TASK DESCRIPTION DATA"))
        assertEquals(31, response.inputTokens)
        assertEquals(17, response.outputTokens)
        assertEquals("{\"action\":\"final\"}", response.structuredOutput)
    }

    @Test fun rejectsHttpErrorsWithoutEchoingProviderBodyOrCredential() {
        val transport = FakeTransport(
            VyomFactoryWorkerModelHttpTransport.Response(
                401, """{"error":{"message":"bad key freellmapi-test-key"}}"""
            )
        )
        val provider = FreeLlmApiAnthropicWorkerModelProvider(transport = transport)

        val error = assertThrows(IllegalStateException::class.java) {
            provider.generate(request(), "freellmapi-test-key".toCharArray())
        }

        assertTrue(error.message!!.contains("HTTP 401"))
        assertFalse(error.message!!.contains("bad key"))
        assertFalse(error.message!!.contains("freellmapi-test-key"))
    }

    @Test fun rejectsMissingUsageOrTextContent() {
        val transport = FakeTransport(
            VyomFactoryWorkerModelHttpTransport.Response(
                200, """{"content":[{"type":"tool_use"}]}"""
            )
        )
        val provider = FreeLlmApiAnthropicWorkerModelProvider(transport = transport)

        assertThrows(IllegalArgumentException::class.java) {
            provider.generate(request(), "key".toCharArray())
        }
    }

    @Test fun stopDelegatesToHttpTransportCancellation() {
        val transport = FakeTransport()
        val provider = FreeLlmApiAnthropicWorkerModelProvider(transport = transport)

        provider.cancelCurrentCall()

        assertTrue(transport.cancelled)
    }
}
