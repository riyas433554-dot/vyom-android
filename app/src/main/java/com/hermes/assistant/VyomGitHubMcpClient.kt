package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

/**
 * Minimal Streamable HTTP MCP host client for GitHub's official remote MCP server.
 *
 * Uses the owner's existing GitHub PAT; credentials are sent only in the HTTPS
 * Authorization header and are never included in requests, results, or exceptions.
 */
internal class VyomGitHubMcpClient(
    private val tokenProvider: () -> String,
    private val endpoint: String = DEFAULT_ENDPOINT,
    private val transport: Transport = UrlConnectionTransport()
) {
    data class Tool(
        val name: String,
        val description: String,
        val inputSchema: JSONObject
    )

    interface Transport {
        data class Response(
            val statusCode: Int,
            val contentType: String,
            val sessionId: String?,
            val body: String
        )

        fun post(
            endpoint: String,
            token: String,
            sessionId: String?,
            protocolVersion: String?,
            payload: String
        ): Response
    }

    private val requestIds = AtomicLong(0)
    @Volatile private var sessionId: String? = null
    @Volatile private var negotiatedProtocolVersion: String? = null

    @Synchronized
    fun connect(): List<Tool> {
        val token = tokenProvider().trim()
        require(token.isNotBlank()) { "Save the GitHub token in Vyom Settings first." }

        val initialize = rpc(
            method = "initialize",
            params = JSONObject()
                .put("protocolVersion", PROTOCOL_VERSION)
                .put("capabilities", JSONObject())
                .put("clientInfo", JSONObject().put("name", "vyom-android").put("version", "0.3.1")),
            token = token,
            includeSession = false
        )
        val negotiated = initialize.optString("protocolVersion").takeIf { it.isNotBlank() }
            ?: PROTOCOL_VERSION
        negotiatedProtocolVersion = negotiated
        transport.post(
            endpoint, token, sessionId, negotiated,
            JSONObject()
                .put("jsonrpc", "2.0")
                .put("method", "notifications/initialized")
                .toString()
        )
        return listTools()
    }

    fun listTools(): List<Tool> {
        val token = requireToken()
        check(sessionId != null) { "Connect GitHub MCP before listing tools." }
        val all = mutableListOf<Tool>()
        var cursor: String? = null
        var pages = 0
        do {
            val params = JSONObject()
            cursor?.let { params.put("cursor", it) }
            val result = rpc("tools/list", params, token)
            val tools = result.optJSONArray("tools") ?: JSONArray()
            for (index in 0 until tools.length()) {
                val item = tools.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                if (name.isBlank()) continue
                all += Tool(
                    name = name,
                    description = item.optString("description"),
                    inputSchema = item.optJSONObject("inputSchema") ?: JSONObject()
                )
            }
            cursor = result.optString("nextCursor").takeIf { it.isNotBlank() && it != "null" }
            pages++
            check(pages <= MAX_TOOL_PAGES) { "GitHub MCP returned too many tool pages." }
        } while (cursor != null)
        return all
    }

    fun callTool(name: String, arguments: JSONObject = JSONObject()): JSONObject {
        require(name.matches(Regex("^[A-Za-z0-9_.-]{1,160}$"))) { "Invalid MCP tool name." }
        check(sessionId != null) { "Connect GitHub MCP before calling tools." }
        return rpc(
            "tools/call",
            JSONObject().put("name", name).put("arguments", arguments),
            requireToken()
        )
    }

    @Synchronized
    fun disconnect() {
        sessionId = null
        negotiatedProtocolVersion = null
    }

    private fun requireToken(): String = tokenProvider().trim().also {
        require(it.isNotBlank()) { "GitHub token is missing. Save it in Vyom Settings." }
    }

    private fun rpc(
        method: String,
        params: JSONObject,
        token: String,
        includeSession: Boolean = true
    ): JSONObject {
        val id = requestIds.incrementAndGet()
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method)
            .put("params", params)
        val response = transport.post(
            endpoint,
            token,
            if (includeSession) sessionId else null,
            if (includeSession) negotiatedProtocolVersion else null,
            payload.toString()
        )
        if (response.statusCode !in 200..299) {
            val reason = when (response.statusCode) {
                401, 403 -> "GitHub MCP authorization failed (HTTP ${response.statusCode}); check token access."
                404 -> "GitHub MCP endpoint was not found (HTTP 404)."
                else -> "GitHub MCP request failed (HTTP ${response.statusCode})."
            }
            throw IllegalStateException(reason)
        }
        response.sessionId?.takeIf { it.isNotBlank() }?.let { sessionId = it }
        val body = response.body.trim()
        if (body.isBlank()) throw IllegalStateException("GitHub MCP returned an empty response.")
        val jsonText = if (response.contentType.contains("text/event-stream", ignoreCase = true)) {
            body.lineSequence()
                .filter { it.startsWith("data:") }
                .map { it.removePrefix("data:").trim() }
                .firstOrNull { it.startsWith("{") }
                ?: throw IllegalStateException("GitHub MCP stream contained no JSON response.")
        } else body
        val root = try {
            JSONObject(jsonText)
        } catch (_: Exception) {
            throw IllegalStateException("GitHub MCP returned an invalid JSON-RPC response.")
        }
        val error = root.optJSONObject("error")
        if (error != null) {
            val code = error.optInt("code")
            val message = error.optString("message").take(180)
            throw IllegalStateException("GitHub MCP error $code: ${message.ifBlank { "request rejected" }}")
        }
        val result = root.optJSONObject("result")
            ?: throw IllegalStateException("GitHub MCP response omitted the result.")
        if (result.optBoolean("isError", false)) {
            val content = result.optJSONArray("content")
            val message = content?.optJSONObject(0)?.optString("text").orEmpty().take(180)
            throw IllegalStateException("GitHub MCP tool failed: ${message.ifBlank { "unknown tool error" }}")
        }
        return result
    }

    internal class UrlConnectionTransport : Transport {
        override fun post(
            endpoint: String,
            token: String,
            sessionId: String?,
            protocolVersion: String?,
            payload: String
        ): Transport.Response {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = true
                useCaches = false
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json, text/event-stream")
                sessionId?.let { setRequestProperty("Mcp-Session-Id", it) }
                protocolVersion?.let { setRequestProperty("MCP-Protocol-Version", it) }
            }
            try {
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > MAX_RESPONSE_BYTES) {
                            throw IllegalStateException("GitHub MCP response exceeded the size limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                    output.toString(Charsets.UTF_8.name())
                }.orEmpty()
                return Transport.Response(
                    statusCode = status,
                    contentType = connection.contentType.orEmpty(),
                    sessionId = connection.getHeaderField("Mcp-Session-Id"),
                    body = body
                )
            } finally {
                connection.disconnect()
            }
        }

        companion object {
            private const val CONNECT_TIMEOUT_MILLIS = 15_000
            private const val READ_TIMEOUT_MILLIS = 45_000
            private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.githubcopilot.com/mcp/"
        private const val PROTOCOL_VERSION = "2025-03-26"
        private const val MAX_TOOL_PAGES = 20
    }
}
