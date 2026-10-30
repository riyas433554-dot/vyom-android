package com.hermes.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class VyomSecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("vyom_secure", Context.MODE_PRIVATE)
    private val alias = "vyom_session_key"

    private fun key(): SecretKey {
        val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    fun put(name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(name, Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).apply()
    }

    fun get(name: String): String? {
        val packed = prefs.getString(name, null) ?: return null
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            if (bytes.size < 13) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) {
            prefs.edit().remove(name).apply()
            null
        }
    }

    fun clear(name: String) { prefs.edit().remove(name).apply() }
}

internal class VyomApiClient(context: Context) {
    private val store = VyomSecureStore(context.applicationContext)
    private val baseUrl = BuildConfig.HERMES_BASE_URL

    class TaskApiException(val statusCode: Int) : Exception("Tasks API error ($statusCode)")

    fun hasPairingCode(): Boolean = !store.get("pairing_code").isNullOrBlank()

    fun reconnect(): String {
        // Do not re-pair or call a session-validation endpoint at startup.
        // Reuse the saved bearer; the normal chat request handles a real 401.
        return store.get("access_token")
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Session expired. Pair Vyom again.")
    }

    fun pair(pairingCode: String): String {
        val response = request("/api/android/session", JSONObject().put("pairingCode", pairingCode).put("deviceName", "Vyom Android"), null)
        val token = response.optString("accessToken")
        if (token.isBlank()) throw IllegalStateException("Server did not return a session token")
        store.put("pairing_code", pairingCode)
        store.put("access_token", token)
        return token
    }

    fun chat(message: String): String {
        val token = store.get("access_token")
            ?.takeIf { it.isNotBlank() }
            ?: throw SessionExpiredException()
        return try {
            request("/api/android/chat", JSONObject().put("message", message), token)
                .optString("reply", "No reply received")
        } catch (_: UnauthorizedSessionException) {
            throw SessionExpiredException()
        }
    }

    class SessionExpiredException : Exception("Session expired. Pair Vyom again.")

    fun createTask(prompt: String): JSONObject {
        val body = JSONObject().put("prompt", prompt).put("clientRequestId", UUID.randomUUID().toString())
        val task = request("/api/v1/tasks", body, taskToken(), "POST").optJSONObject("task")
            ?: throw IllegalStateException("Invalid task response")
        val id = task.optString("id")
        if (id.isBlank()) throw IllegalStateException("Task response did not contain an id")
        val ids = savedTaskIds().toMutableSet()
        ids.add(id)
        store.put(TASK_IDS_KEY, JSONArray(ids.toList()).toString())
        return task
    }

    fun getTask(id: String): JSONObject {
        if (!TASK_ID_RE.matches(id)) throw IllegalArgumentException("Invalid task id")
        return request("/api/v1/tasks/$id", JSONObject(), taskToken(), "GET").optJSONObject("task")
            ?: throw IllegalStateException("Invalid task status response")
    }

    fun cancelTask(id: String): JSONObject {
        if (!TASK_ID_RE.matches(id)) throw IllegalArgumentException("Invalid task id")
        return request("/api/v1/tasks/$id/cancel", JSONObject(), taskToken(), "POST").optJSONObject("task")
            ?: throw IllegalStateException("Invalid task cancellation response")
    }


    // Owner-only Vyom OS APIs. The server verifies that this paired session is owner-bound.
    fun ownerDashboard(): JSONObject =
        request("/api/v1/owner/dashboard", JSONObject(), taskToken(), "GET")

    fun listAgents(): JSONArray =
        request("/api/v1/agents", JSONObject(), taskToken(), "GET").optJSONArray("agents") ?: JSONArray()

    fun createAgent(definition: JSONObject): JSONObject =
        request("/api/v1/agents", JSONObject().put("definition", definition), taskToken(), "POST")
            .optJSONObject("agent") ?: throw IllegalStateException("Invalid agent response")

    fun runAgent(agentId: String, task: String, context: JSONObject = JSONObject(), approvedTools: List<String> = emptyList()): JSONObject {
        require(agentId.matches(Regex("^[A-Za-z0-9_-]{1,128}$"))) { "Invalid agent id" }
        val tools = JSONArray()
        approvedTools.forEach { tools.put(it) }
        return request("/api/v1/agents/$agentId/run",
            JSONObject().put("task", task).put("context", context).put("approvedTools", tools),
            taskToken(), "POST").optJSONObject("result") ?: throw IllegalStateException("Invalid agent run response")
    }

    fun delegateAgents(assignments: JSONArray): JSONObject =
        request("/api/v1/agents/delegate", JSONObject().put("assignments", assignments), taskToken(), "POST")
            .optJSONObject("result") ?: throw IllegalStateException("Invalid delegation response")

    fun listMemory(query: String? = null, category: String? = null): JSONArray {
        val params = mutableListOf<String>()
        if (!query.isNullOrBlank()) params += "q=" + URLEncoder.encode(query, "UTF-8")
        if (!category.isNullOrBlank()) params += "category=" + URLEncoder.encode(category, "UTF-8")
        val suffix = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return request("/api/v1/memory$suffix", JSONObject(), taskToken(), "GET").optJSONArray("memories") ?: JSONArray()
    }

    fun saveMemory(category: String, title: String, content: String, tags: JSONArray = JSONArray()): JSONObject =
        request("/api/v1/memory", JSONObject().put("ownerConfirmed", true).put("category", category)
            .put("title", title).put("content", content).put("tags", tags), taskToken(), "POST")
            .optJSONObject("memory") ?: throw IllegalStateException("Invalid memory response")

    fun updateMemory(id: String, patch: JSONObject): JSONObject {
        require(UUID_RE.matches(id)) { "Invalid memory id" }
        return request("/api/v1/memory/$id", JSONObject().put("ownerConfirmed", true).put("patch", patch),
            taskToken(), "PATCH").optJSONObject("memory") ?: throw IllegalStateException("Invalid memory update response")
    }

    fun deleteMemory(id: String): Boolean {
        require(UUID_RE.matches(id)) { "Invalid memory id" }
        return request("/api/v1/memory/$id", JSONObject().put("ownerConfirmed", true), taskToken(), "DELETE")
            .optBoolean("deleted", false)
    }

    fun listSecrets(): JSONArray =
        request("/api/v1/secrets", JSONObject(), taskToken(), "GET").optJSONArray("secrets") ?: JSONArray()

    fun createSecret(metadata: JSONObject, value: String): JSONObject =
        request("/api/v1/secrets", JSONObject().put("metadata", metadata).put("value", value), taskToken(), "POST")
            .optJSONObject("secret") ?: throw IllegalStateException("Invalid secret metadata response")

    fun rotateSecret(id: String, value: String, metadata: JSONObject? = null): JSONObject {
        require(UUID_RE.matches(id)) { "Invalid secret id" }
        val body = JSONObject().put("value", value)
        if (metadata != null) body.put("metadata", metadata)
        return request("/api/v1/secrets/$id/rotate", body, taskToken(), "POST")
            .optJSONObject("secret") ?: throw IllegalStateException("Invalid secret rotation response")
    }

    fun revokeSecret(id: String): JSONObject {
        require(UUID_RE.matches(id)) { "Invalid secret id" }
        return request("/api/v1/secrets/$id/revoke", JSONObject(), taskToken(), "POST")
            .optJSONObject("secret") ?: throw IllegalStateException("Invalid secret revocation response")
    }

    fun exportVault(passphrase: String): JSONObject =
        request("/api/v1/secrets/export", JSONObject().put("passphrase", passphrase), taskToken(), "POST")
            .optJSONObject("backup") ?: throw IllegalStateException("Invalid encrypted backup response")

    fun importVault(passphrase: String, backup: JSONObject): JSONObject =
        request("/api/v1/secrets/import", JSONObject().put("passphrase", passphrase).put("backup", backup), taskToken(), "POST")

    fun proposeSelfImprovement(goal: String, targetFiles: JSONArray): JSONObject =
        request("/api/v1/self-improvement/proposals",
            JSONObject().put("ownerConfirmed", true).put("goal", goal).put("targetFiles", targetFiles),
            taskToken(), "POST").optJSONObject("proposal") ?: throw IllegalStateException("Invalid proposal response")

    fun savedTaskIds(): List<String> {
        val raw = store.get(TASK_IDS_KEY) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf { id -> TASK_ID_RE.matches(id) } }
        } catch (_: Exception) { emptyList() }
    }

    private fun taskToken(): String {
        val token = store.get("access_token")
        if (token.isNullOrBlank()) throw TaskApiException(HttpURLConnection.HTTP_UNAUTHORIZED)
        return token
    }

    private fun request(path: String, body: JSONObject, token: String?, method: String = "POST"): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 120_000
            doInput = true
            if (method != "GET") {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (method != "GET") connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (path.startsWith("/api/v1/tasks") && (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_UNAVAILABLE)) {
                throw TaskApiException(code)
            }
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED) throw UnauthorizedSessionException()
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
            if (code !in 200..299) throw IllegalStateException("Hermes API error ($code)")
            return json
        } finally {
            connection.disconnect()
        }
    }

    private class UnauthorizedSessionException : Exception()

    companion object {
        private const val TASK_IDS_KEY = "vyom_task_ids"
        private val TASK_ID_RE = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        private val UUID_RE = TASK_ID_RE
    }
}
