package com.hermes.assistant

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Creates a proposal branch, commits one guarded Kotlin source file, and opens a PR
 * against the working branch. It has no merge, release, install, or main-branch path.
 */
internal class SelfModificationClient {
    data class Proposal(val branch: String, val pullRequestUrl: String)

    fun createProposal(token: String, title: String, path: String, proposedContent: String): Proposal {
        SelfModificationPolicy.validate(title, path, proposedContent)
        require(token.trim().length >= 20) { "Add a GitHub token with repository Contents and Pull requests permissions in device settings" }

        val baseRef = request("GET", "/git/ref/heads/$BASE_BRANCH", token = token)
        val baseSha = baseRef.optJSONObject("object")?.optString("sha").orEmpty()
        require(baseSha.matches(Regex("^[a-f0-9]{40}$"))) { "Could not resolve the working branch head" }

        val suffix = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(28).ifBlank { "change" }
        val proposalBranch = "vyom-proposal-${System.currentTimeMillis()}-$suffix"
        request("POST", "/git/refs", token, JSONObject()
            .put("ref", "refs/heads/$proposalBranch")
            .put("sha", baseSha))

        val encodedPath = path.split("/").joinToString("/") { Uri.encode(it) }
        val existing = request(
            "GET",
            "/contents/$encodedPath?ref=" + URLEncoder.encode(BASE_BRANCH, "UTF-8"),
            token = token,
            allowNotFound = true
        )
        val currentSha = existing.optString("sha").takeIf { it.matches(Regex("^[a-f0-9]{40}$")) }
        val payload = JSONObject()
            .put("message", "proposal: $title")
            .put("content", Base64.encodeToString(proposedContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            .put("branch", proposalBranch)
        if (currentSha != null) payload.put("sha", currentSha)
        request("PUT", "/contents/$encodedPath", token, payload)

        val pr = request("POST", "/pulls", token, JSONObject()
            .put("title", "[Vyom proposal] $title")
            .put("head", proposalBranch)
            .put("base", BASE_BRANCH)
            .put("draft", false)
            .put("body", "## Constrained self-modification proposal\n\n" +
                "- Proposed file: `$path`\n" +
                "- Created by Vyom's user-requested proposal workflow.\n" +
                "- This PR is not auto-merged or auto-installed.\n" +
                "- CI must pass; the repository owner must review the diff and manually install any approved build.\n" +
                "- Policy-critical files are blocked by the in-app allowlist and protected by CODEOWNERS."))
        val prUrl = pr.optString("html_url")
        require(prUrl.startsWith("https://github.com/riyas433554-dot/vyom-android/pull/")) {
            "GitHub did not return an approved pull request URL"
        }
        return Proposal(proposalBranch, prUrl)
    }

    private fun request(
        method: String,
        path: String,
        token: String,
        body: JSONObject? = null,
        allowNotFound: Boolean = false
    ): JSONObject {
        val connection = (URL(API_ROOT + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Vyom-Android")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_FOUND && allowNotFound) return JSONObject().put("_notFound", true)
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) throw IllegalStateException("GitHub response too large")
                    output.write(buffer, 0, count)
                }
                String(output.toByteArray(), Charsets.UTF_8)
            }.orEmpty()
            if (status !in 200..299) {
                val message = try { JSONObject(response).optString("message") } catch (_: Exception) { "" }
                throw IllegalStateException("GitHub API $status: ${message.take(160)}")
            }
            return JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val API_ROOT = "https://api.github.com/repos/riyas433554-dot/vyom-android"
        private const val BASE_BRANCH = "feature/u1-live-orbit-tasks"
        private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    }
}
