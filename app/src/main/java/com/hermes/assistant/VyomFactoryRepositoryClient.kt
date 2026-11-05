package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Read-only GitHub repository inspection for the Universal Repository Integration Engine.
 * All tree reads are pinned to the resolved commit SHA; repository contents are never executed.
 */
internal class VyomFactoryRepositoryClient(
    private val apiBaseUrl: String = "https://api.github.com",
    getJson: ((String) -> String)? = null
) {
    data class Preview(
        val snapshot: VyomFactoryRepositoryIntake.Snapshot,
        val assessment: VyomFactoryRepositoryIntake.Assessment,
        val description: String,
        val stars: Int,
        val sizeKb: Int,
        val updatedAt: String
    )

    private val requestJson: (String) -> String = getJson ?: { path -> fetchJson(path) }

    fun inspect(repositoryUrl: String): Preview {
        val source = VyomFactoryRepositoryIntake.parseUrl(repositoryUrl)
            ?: throw IllegalArgumentException("Use a canonical https://github.com/owner/repository URL")
        val prefix = "/repos/${source.owner}/${source.name}"
        val metadata = JSONObject(requestJson(prefix))
        val defaultBranch = metadata.optString("default_branch")
        require(defaultBranch.isNotBlank() && defaultBranch.length <= MAX_BRANCH_LENGTH) {
            "GitHub did not return a valid default branch"
        }
        val branchQuery = URLEncoder.encode(defaultBranch, "UTF-8")
        val commits = JSONArray(requestJson("$prefix/commits?sha=$branchQuery&per_page=1"))
        val commitSha = commits.optJSONObject(0)?.optString("sha").orEmpty()
        require(COMMIT_PATTERN.matches(commitSha)) {
            "Could not resolve an immutable repository commit"
        }

        // The recursive tree request is deliberately pinned to the immutable commit,
        // never to a mutable branch name.
        val tree = JSONObject(requestJson("$prefix/git/trees/$commitSha?recursive=1"))
        val entries = tree.optJSONArray("tree") ?: JSONArray()
        val tooManyEntries = entries.length() > MAX_TREE_ENTRIES
        val paths = (0 until minOf(entries.length(), MAX_TREE_ENTRIES)).mapNotNull { index ->
            val entry = entries.optJSONObject(index) ?: return@mapNotNull null
            if (entry.optString("type") != "blob") return@mapNotNull null
            entry.optString("path").takeIf { it.isNotBlank() }
        }
        val snapshot = VyomFactoryRepositoryIntake.Snapshot(
            source = source,
            defaultBranch = defaultBranch,
            commitSha = commitSha,
            licenseSpdx = metadata.optJSONObject("license")?.optString("spdx_id").orEmpty(),
            archived = metadata.optBoolean("archived", false),
            treeTruncated = tree.optBoolean("truncated", true) || tooManyEntries,
            paths = paths
        )
        return Preview(
            snapshot = snapshot,
            assessment = VyomFactoryRepositoryIntake.assess(snapshot),
            description = metadata.optString("description").take(MAX_DESCRIPTION_LENGTH),
            stars = metadata.optInt("stargazers_count", 0).coerceAtLeast(0),
            sizeKb = metadata.optInt("size", 0).coerceAtLeast(0),
            updatedAt = metadata.optString("updated_at").take(40)
        )
    }

    private fun fetchJson(path: String): String {
        require(path.startsWith("/") && !path.startsWith("//")) { "Invalid GitHub API path" }
        val root = apiBaseUrl.trimEnd('/')
        require(root.startsWith("https://") && root.substringAfter("https://").substringBefore('/').isNotBlank()) {
            "GitHub API base URL must use HTTPS"
        }
        val connection = (URL(root + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Vyom-Factory-Repository-Inspector")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("GitHub repository inspection returned HTTP $status")
            require(body.length <= MAX_RESPONSE_CHARS) {
                "GitHub repository response exceeded the bounded inspection limit"
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val MAX_RESPONSE_CHARS = 2_000_000
        private const val MAX_TREE_ENTRIES = 20_000
        private const val MAX_BRANCH_LENGTH = 200
        private const val MAX_DESCRIPTION_LENGTH = 500
        private val COMMIT_PATTERN = Regex("^[a-f0-9]{40}$")
    }
}
