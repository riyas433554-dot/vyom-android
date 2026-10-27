package com.hermes.assistant

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

internal class NemotronLiveClient(private val apiKey: String) {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var connection: HttpURLConnection? = null
    @Volatile private var active = false

    fun stream(prompt: String, onDelta: (String) -> Unit, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        cancel()
        active = true
        executor.execute {
            var full = StringBuilder()
            try {
                require(apiKey.isNotBlank()) { "Add NVIDIA API key in encrypted Profile voice settings to use LIVE lane." }
                val conn = (URL("https://integrate.api.nvidia.com/v1/chat/completions").openConnection() as HttpURLConnection).also {
                    connection = it
                    it.requestMethod = "POST"
                    it.connectTimeout = 15000
                    it.readTimeout = 90000
                    it.doOutput = true
                    it.setRequestProperty("Authorization", "Bearer $apiKey")
                    it.setRequestProperty("Content-Type", "application/json")
                    it.setRequestProperty("Accept", "text/event-stream")
                }
                val body = JSONObject()
                    .put("model", "nvidia/nemotron-3-super-120b-a12b")
                    .put("stream", true)
                    .put("temperature", 0.6)
                    .put("max_tokens", 2048)
                    .put("messages", org.json.JSONArray().put(JSONObject().put("role", "system").put("content", "You are Vyom, a helpful voice assistant. Reply naturally and concisely.")).put(JSONObject().put("role", "user").put("content", prompt)))
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode !in 200..299) throw IllegalStateException("NVIDIA LIVE HTTP ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(300)}")
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    while (active) {
                        val line = reader.readLine() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        val choices = JSONObject(data).optJSONArray("choices") ?: continue
                        val delta = choices.optJSONObject(0)?.optJSONObject("delta")?.optString("content").orEmpty()
                        if (delta.isNotEmpty()) {
                            full.append(delta)
                            onDelta(delta)
                        }
                    }
                }
                if (active) onComplete(full.toString())
            } catch (e: Exception) {
                if (active) onError(e.message ?: "LIVE stream failed")
            } finally {
                connection?.disconnect()
                connection = null
                active = false
            }
        }
    }

    fun cancel() {
        active = false
        try { connection?.disconnect() } catch (_: Exception) {}
        connection = null
    }
}
