package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.Base64
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

    data class FilePreview(
        val path: String,
        val content: String,
        val sizeBytes: Int,
        val blobSha: String,
        val contentSha256: String,
        val redactions: Int,
        val untrusted: Boolean = true
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


    /**
     * Fetches only bounded documentation/manifests from the inspected immutable commit.
     * Returned text is untrusted and secret-pattern redacted; nothing is executed.
     */
    fun previewFiles(preview: Preview): List<FilePreview> {
        require(preview.assessment.readOnlyInspectionAllowed &&
            preview.assessment.decision != VyomFactoryRepositoryIntake.Decision.BLOCKED) {
            "Repository is blocked from file preview"
        }
        val snapshot = preview.snapshot
        require(snapshot.commitSha.matches(COMMIT_PATTERN) &&
            preview.assessment.immutableRef == snapshot.commitSha) {
            "Repository preview is not pinned to the inspected immutable commit"
        }
        val selected = selectPreviewPaths(snapshot.paths)
        var totalBytes = 0
        return selected.map { path ->
            require(isPreviewPath(path)) { "Repository file is outside the preview allowlist" }
            val encodedPath = path.split('/').joinToString("/") {
                URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }
            val ref = URLEncoder.encode(snapshot.commitSha, "UTF-8")
            val file = JSONObject(requestJson(
                "/repos/${snapshot.source.owner}/${snapshot.source.name}/contents/$encodedPath?ref=$ref"
            ))
            require(file.optString("type") == "file" && file.optString("path") == path) {
                "GitHub returned a different repository path than requested"
            }
            require(file.optString("encoding") == "base64") {
                "GitHub did not return a bounded base64 file response"
            }
            val declaredSize = file.optInt("size", -1)
            require(declaredSize in 1..MAX_PREVIEW_FILE_BYTES) {
                "Repository preview file is empty or exceeds the per-file size limit"
            }
            totalBytes += declaredSize
            require(totalBytes <= MAX_PREVIEW_TOTAL_BYTES) {
                "Repository preview exceeded the total file-size limit"
            }
            val encoded = file.optString("content").filterNot(Char::isWhitespace)
            require(encoded.isNotBlank() && encoded.length <= MAX_PREVIEW_FILE_BYTES * 2) {
                "Repository preview file content is empty or oversized"
            }
            val bytes = Base64.getDecoder().decode(encoded)
            require(bytes.size == declaredSize) { "Repository preview file size did not match GitHub metadata" }
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val original = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            val (safeContent, redactions) = redactSecrets(original)
            FilePreview(
                path = path,
                content = safeContent,
                sizeBytes = bytes.size,
                blobSha = file.optString("sha"),
                contentSha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it) },
                redactions = redactions
            )
        }
    }

    private fun selectPreviewPaths(paths: List<String>): List<String> {
        val safe = paths.filter { VyomFactoryRepositoryIntakePath.isSafe(it) }.distinct()
        val rootPriority = listOf(
            "README.md", "SKILL.md", "LICENSE", "LICENSE.md", "LICENSE.txt", "COPYING",
            "openapi.yaml", "openapi.yml", "openapi.json", "swagger.yaml", "swagger.yml",
            "package.json", "pyproject.toml", "Cargo.toml", "go.mod", "build.gradle",
            "build.gradle.kts", "pom.xml", "pubspec.yaml"
        )
        val rootFiles = rootPriority.mapNotNull { wanted ->
            safe.firstOrNull { it.equals(wanted, ignoreCase = true) }
        }
        val workflowFiles = safe.filter {
            it.startsWith(".github/workflows/") &&
                (it.endsWith(".yml", true) || it.endsWith(".yaml", true))
        }.sorted().take(2)
        val docsFiles = safe.filter { it.startsWith("docs/") && it.endsWith(".md", true) }
            .sorted().take(2)
        return (rootFiles + workflowFiles + docsFiles).distinct().take(MAX_PREVIEW_FILES)
    }

    private fun isPreviewPath(path: String): Boolean =
        path in selectPreviewPaths(listOf(path)) && VyomFactoryRepositoryIntakePath.isSafe(path)

    private fun redactSecrets(source: String): Pair<String, Int> {
        var result = source
        var count = 0
        REDACTION_PATTERNS.forEach { pattern ->
            result = pattern.replace(result) {
                count++
                "[REDACTED]"
            }
        }
        return result to count
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
        private const val MAX_PREVIEW_FILES = 8
        private const val MAX_PREVIEW_FILE_BYTES = 48 * 1024
        private const val MAX_PREVIEW_TOTAL_BYTES = 160 * 1024
        private val COMMIT_PATTERN = Regex("^[a-f0-9]{40}$")
        private val REDACTION_PATTERNS = listOf(
            Regex("-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----[\\\\s\\\\S]*?-----END [A-Z0-9 ]*PRIVATE KEY-----"),
            Regex("\\\\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\\\\b"),
            Regex("\\\\bAKIA[0-9A-Z]{16}\\\\b"),
            Regex("\\\\bsk-[A-Za-z0-9_-]{20,}\\\\b"),
            Regex("(?i)((?:api[_-]?key|access[_-]?token|client[_-]?secret|password|secret)\\\\s*[:=]\\\\s*[\\\"']?)[^\\\"'\\\\s,;]{8,}")
        )
    }
}

private object VyomFactoryRepositoryIntakePath {
    fun isSafe(path: String): Boolean =
        path.isNotBlank() && path.length <= 500 && !path.startsWith("/") &&
            !path.contains('\\\\') &&
            !path.split('/').any { it.isBlank() || it == "." || it == ".." } &&
            path.none { it.isISOControl() }
}
