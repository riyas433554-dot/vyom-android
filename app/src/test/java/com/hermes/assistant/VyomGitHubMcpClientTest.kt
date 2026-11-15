package com.hermes.assistant

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class VyomGitHubMcpClientTest {
    private class FakeTransport : VyomGitHubMcpClient.Transport {
        data class Call(
            val token: String,
            val sessionId: String?,
            val protocolVersion: String?,
            val payload: JSONObject
        )

        val calls = mutableListOf<Call>()

        override fun post(
            endpoint: String,
            token: String,
            sessionId: String?,
            protocolVersion: String?,
            payload: String
        ): VyomGitHubMcpClient.Transport.Response {
            val request = JSONObject(payload)
            calls += Call(token, sessionId, protocolVersion, request)
            return when (request.optString("method")) {
                "initialize" -> VyomGitHubMcpClient.Transport.Response(
                    200, "application/json", "test-session",
                    JSONObject().put("jsonrpc", "2.0")
                        .put("id", request.getLong("id"))
                        .put("result", JSONObject().put("protocolVersion", "2025-03-26"))
                        .toString()
                )
                "notifications/initialized" -> Response(202, "", "test-session", "")
                "tools/list" -> Response(
                    200, "application/json", "test-session",
                    JSONObject().put("jsonrpc", "2.0")
                        .put("id", request.getLong("id"))
                        .put("result", JSONObject().put("tools", org.json.JSONArray()
                            .put(JSONObject()
                                .put("name", "get_file_contents")
                                .put("description", "Read repository files")
                                .put("inputSchema", JSONObject().put("type", "object")))))
                        .toString()
                )
                "tools/call" -> Response(
                    200, "application/json", "test-session",
                    JSONObject().put("jsonrpc", "2.0")
                        .put("id", request.getLong("id"))
                        .put("result", JSONObject().put("content", org.json.JSONArray()
                            .put(JSONObject().put("type", "text").put("text", "ok"))))
                        .toString()
                )
                else -> error("Unexpected MCP method")
            }
        }
    }

    @Test
    fun connectInitializesSessionAndListsTools() {
        val transport = FakeTransport()
        val client = VyomGitHubMcpClient({ "ghp-test-token" }, transport = transport)

        val tools = client.connect()

        assertEquals(listOf("get_file_contents"), tools.map { it.name })
        assertEquals("ghp-test-token", transport.calls.first().token)
        assertEquals("initialize", transport.calls.first().payload.getString("method"))
        assertEquals("notifications/initialized", transport.calls[1].payload.getString("method"))
        assertEquals("tools/list", transport.calls[2].payload.getString("method"))
        assertEquals("test-session", transport.calls[2].sessionId)
        assertEquals("2025-03-26", transport.calls[2].protocolVersion)
    }

    @Test
    fun callToolUsesSessionAndStructuredArguments() {
        val transport = FakeTransport()
        val client = VyomGitHubMcpClient({ "ghp-test-token" }, transport = transport)
        client.connect()

        val result = client.callTool(
            "get_file_contents",
            JSONObject().put("owner", "riyas433554-dot").put("repo", "vyom-android")
        )

        assertTrue(result.has("content"))
        val call = transport.calls.last()
        assertEquals("tools/call", call.payload.getString("method"))
        assertEquals("test-session", call.sessionId)
        assertEquals("vyom-android", call.payload.getJSONObject("params")
            .getJSONObject("arguments").getString("repo"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun connectRequiresSavedGitHubToken() {
        VyomGitHubMcpClient({ "" }, transport = FakeTransport()).connect()
    }
}
