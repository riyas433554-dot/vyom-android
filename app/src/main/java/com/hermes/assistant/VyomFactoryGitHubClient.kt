package com.hermes.assistant

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** GitHub Issues is the shared, auditable Factory task registry. Tokens are injected and never logged. */
internal class VyomFactoryGitHubClient(
    private val token: String,
    private val repository: String = DEFAULT_REPOSITORY,
    private val http: OkHttpClient = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
) {
    data class Task(
        val number: Int,
        val title: String,
        val body: String,
        val state: String,
        val url: String,
        val updatedAt: String,
        val metadata: VyomFactoryStatus.Metadata
    )

    class FactoryApiException(val code: Int, message: String) : IOException(message)
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun listTasks(): List<Task> {
        val result = mutableListOf<Task>()
        var page = 1
        while (page <= MAX_PAGES) {
            val array = JSONArray(execute("GET", "/issues?state=all&per_page=100&sort=updated&direction=desc&page=$page"))
            for (i in 0 until array.length()) {
                val issue = array.optJSONObject(i) ?: continue
                if (issue.has("pull_request")) continue
                val metadata = VyomFactoryStatus.readMetadata(issue.optString("body")) ?: continue
                result += issue.toTask(metadata)
            }
            if (array.length() < 100) break
            page++
        }
        return result
    }

    fun createTask(title: String, description: String, priority: String, source: String,
                   sourceId: String = UUID.randomUUID().toString()): Task {
        val cleanTitle = title.trim()
        require(cleanTitle.length in 4..120) { "Title must contain 4–120 characters" }
        require(description.trim().isNotEmpty() && description.length <= 12000) { "Description must contain 1–12,000 characters" }
        require(priority in PRIORITIES) { "Unknown priority" }
        val key = VyomFactoryStatus.idempotencyKey(source, sourceId)
            ?: throw IllegalArgumentException("Invalid source identity")
        listTasks().firstOrNull { it.metadata.idempotencyKey == key }?.let { return it }
        val metadata = VyomFactoryStatus.Metadata(priority, "open", source, sourceId, key, listOf("created"))
        val body = VyomFactoryStatus.withMetadata(description.trim(), metadata)
        val created = JSONObject(execute("POST", "/issues",
            JSONObject().put("title", "[VYOM FACTORY][$priority] $cleanTitle").put("body", body)))
        val task = created.toTask(metadata)
        addEvent(task.number, "Created from $source")
        return task
    }

    fun updateTask(current: Task, newTitle: String = current.title,
                   newPriority: String = current.metadata.priority,
                   newStatus: String = current.metadata.status,
                   newDescription: String? = null): Task {
        require(newPriority in PRIORITIES) { "Unknown task priority" }
        require(VyomFactoryStatus.canTransition(current.metadata.status, newStatus) ||
            current.metadata.status == newStatus) { "Invalid task state transition" }
        val cleanTitle = newTitle.trim().removePrefix("[VYOM FACTORY]")
            .replace(Regex("^\\[[Pp][0-3]\\]\\s*"), "").trim()
        require(cleanTitle.length in 4..120) { "Title must contain 4–120 characters" }
        val metadata = current.metadata.copy(priority = newPriority, status = newStatus,
            events = (current.metadata.events + "updated:$newStatus:$newPriority").takeLast(500))
        val description = newDescription ?: current.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
        require(description.isNotBlank() && description.length <= 12000) { "Description must contain 1–12,000 characters" }
        val body = VyomFactoryStatus.withMetadata(description, metadata)
        val closed = newStatus == "completed" || newStatus == "cancelled"
        val updated = JSONObject(execute("PATCH", "/issues/" + current.number,
            JSONObject().put("title", "[VYOM FACTORY][$newPriority] $cleanTitle")
                .put("body", body).put("state", if (closed) "closed" else "open")))
        val task = updated.toTask(metadata)
        addEvent(task.number, "Status: " + current.metadata.status + " -> " + newStatus + " · Priority: " + newPriority)
        return task
    }

    fun listEvents(issueNumber: Int): List<String> {
        require(issueNumber > 0)
        val array = JSONArray(execute("GET", "/issues/$issueNumber/comments?per_page=50&sort=created&direction=desc"))
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val author = item.optJSONObject("user")?.optString("login").orEmpty()
            val date = item.optString("created_at")
            val body = item.optString("body").take(1000)
            if (body.isBlank()) null else "$date · ${author.ifBlank { "unknown" }}\\n$body"
        }
    }

    fun addEvent(issueNumber: Int, message: String) {
        require(issueNumber > 0 && message.isNotBlank() && message.length <= 1000)
        execute("POST", "/issues/$issueNumber/comments",
            JSONObject().put("body", "[Vyom Factory] $message"))
    }

    fun verifyConnection(): String {
        val user = JSONObject(execute("GET", "user"))
        val login = user.optString("login").ifBlank { "GitHub connected" }
        return login + " · identity verified; Issues write permission is checked when creating a task"
    }

    private fun execute(method: String, path: String, payload: JSONObject? = null): String {
        if (token.isBlank() || token.length > 500) throw IllegalStateException("GitHub token is missing or invalid")
        val normalized = path.removePrefix("/")
        val url = if (normalized.isBlank()) "https://api.github.com/repos/$repository"
            else if (normalized == "user") "https://api.github.com/user"
            else "https://api.github.com/repos/$repository/$normalized"
        val builder = Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Authorization", "Bearer $token")
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post((payload ?: JSONObject()).toString().toRequestBody(jsonType))
            "PATCH" -> builder.patch((payload ?: JSONObject()).toString().toRequestBody(jsonType))
            else -> throw IllegalArgumentException("Unsupported GitHub method")
        }
        http.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
                throw FactoryApiException(response.code, "GitHub " + response.code + ": " +
                    detail.take(180).ifBlank { "GitHub request failed" })
            }
            return body
        }
    }

    private fun JSONObject.toTask(metadata: VyomFactoryStatus.Metadata): Task = Task(
        number = optInt("number"), title = optString("title"), body = optString("body"),
        state = optString("state"), url = optString("html_url"),
        updatedAt = optString("updated_at"), metadata = metadata
    )

    companion object {
        const val DEFAULT_REPOSITORY = "riyas433554-dot/vyom-android"
        private val PRIORITIES = setOf("P0", "P1", "P2", "P3")
        private const val MAX_PAGES = 10
    }
}
