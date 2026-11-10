package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * The only repository-content writer for Factory screenshot evidence.
 * All writes are pinned to the factory-evidence system binding and use a
 * compare-before-update + non-force Git ref update to reject stale writers.
 */
internal class VyomFactoryEvidencePublisher(
    private val registry: VyomFactorySystemRegistry,
    private val transport: Transport,
    private val evidenceStore: VyomFactoryScreenshotEvidenceStore
) {
    data class Result(
        val url: String,
        val commitSha: String,
        val sha256: String,
        val issueCommentRecorded: Boolean
    )

    interface Transport {
        fun isRepositoryPrivate(repositoryUrl: String): Boolean
        fun branchHead(branch: String): String?
        fun createBranch(branch: String, fromCommit: String)
        fun commitTree(commitSha: String): String
        fun createBlob(bytes: ByteArray): String
        fun createTree(baseTreeSha: String, path: String, blobSha: String): String
        fun createCommit(message: String, treeSha: String, parentSha: String): String
        fun updateRef(branch: String, newCommit: String, expectedHead: String, force: Boolean)
        fun readFile(path: String, ref: String): ByteArray
        fun addIssueComment(issueNumber: Int, message: String)
    }

    fun publish(
        systemId: String,
        repositoryUrl: String,
        pinnedCommitSha: String,
        issueNumber: Int,
        relativePath: String,
        file: File
    ): Result {
        require(issueNumber > 0 && file.isFile) { "Select an existing issue and a stored screenshot" }
        check(evidenceStore.uploadReceipt(file) == null) {
            "This screenshot already has an upload receipt; automatic duplicate uploads are blocked"
        }
        require(relativePath.startsWith(".factory-evidence/")) { "Evidence path must stay below .factory-evidence/" }
        val binding = registry.requireAuthorizedBinding(
            systemId, relativePath, repositoryUrl, pinnedCommitSha
        )
        check(transport.isRepositoryPrivate(binding.repositoryUrl)) {
            "Evidence upload is disabled because the bound repository is public"
        }

        val bytes = file.readBytes()
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> throw IllegalArgumentException("Unsupported stored screenshot format")
        }
        val extension = VyomFactoryScreenshotEvidencePolicy.extensionFor(mime, bytes)
        require(relativePath.endsWith(".$extension")) { "Evidence path extension does not match image bytes" }
        val hash = VyomFactoryScreenshotEvidencePolicy.sha256(bytes)

        val branch = EVIDENCE_BRANCH
        val storedExpectedHead = evidenceStore.expectedBranchHead(binding.commitSha)
        var actualHead = transport.branchHead(branch)
        if (actualHead == null) {
            transport.createBranch(branch, binding.commitSha)
            actualHead = transport.branchHead(branch)
                ?: throw IllegalStateException("Evidence branch creation could not be verified")
        }
        if (actualHead != storedExpectedHead) {
            throw SecurityException("Evidence branch head is stale; refresh/reconcile before another upload")
        }

        val baseTree = transport.commitTree(storedExpectedHead)
        val blob = transport.createBlob(bytes)
        val tree = transport.createTree(baseTree, relativePath, blob)
        val commit = transport.createCommit(
            "factory: attach verified screenshot evidence for issue #$issueNumber",
            tree, storedExpectedHead
        )
        require(COMMIT_PATTERN.matches(commit)) { "GitHub did not return a valid evidence commit" }
        // The transport re-reads the ref immediately before PATCH. GitHub's
        // non-force fast-forward rule rejects a concurrent divergent update.
        transport.updateRef(branch, commit, storedExpectedHead, force = false)
        evidenceStore.recordBranchHead(commit)

        val storedBytes = transport.readFile(relativePath, commit)
        val verifiedHash = VyomFactoryScreenshotEvidencePolicy.sha256(storedBytes)
        check(storedBytes.contentEquals(bytes) && verifiedHash == hash) {
            "Uploaded screenshot bytes failed commit-pinned SHA-256 verification"
        }

        val source = VyomFactoryRepositoryIntake.parseUrl(binding.repositoryUrl)
            ?: throw SecurityException("Bound evidence repository URL is invalid")
        val encodedPath = relativePath.split('/').joinToString("/") {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }
        val url = "https://github.com/${source.owner}/${source.name}/blob/$branch/$encodedPath"
        // Persist the upload receipt before attempting the comment. A process death
        // after the content commit must not silently trigger a duplicate upload.
        evidenceStore.recordUploaded(file, url, commit, hash)
        val comment = "Owner attached screenshot evidence after explicitly tapping Attach: ![Factory screenshot]($url)\nSHA-256: $hash\nEvidence commit: $commit"
        val commentRecorded = runCatching {
            transport.addIssueComment(issueNumber, comment)
        }.isSuccess
        if (commentRecorded) runCatching { evidenceStore.clearUploadReceipt(file) }
        return Result(url, commit, hash, commentRecorded)
    }

    companion object {
        const val SYSTEM_ID = "factory-evidence"
        const val EVIDENCE_BRANCH = "factory-evidence"
        private val COMMIT_PATTERN = Regex("^[a-f0-9]{40}$")
    }
}

/** GitHub Git-Database transport. Source files are changed only through this publisher. */
internal class VyomFactoryGitHubEvidenceTransport(
    private val token: String,
    private val repository: String,
    private val apiBaseUrl: String = "https://api.github.com",
    private val http: OkHttpClient = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
) : VyomFactoryEvidencePublisher.Transport {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val repoPath = "/repos/$repository"

    override fun isRepositoryPrivate(repositoryUrl: String): Boolean {
        require(repositoryUrl == "https://github.com/$repository") {
            "Evidence repository does not match the configured GitHub repository"
        }
        return JSONObject(request("GET", repoPath)).optBoolean("private", false)
    }

    override fun branchHead(branch: String): String? {
        val response = requestOrNull("GET", "$repoPath/git/ref/heads/${encodeSegment(branch)}")
            ?: return null
        val sha = JSONObject(response).optJSONObject("object")?.optString("sha").orEmpty()
        require(sha.matches(COMMIT_PATTERN)) { "Evidence branch returned an invalid head" }
        return sha
    }

    override fun createBranch(branch: String, fromCommit: String) {
        require(fromCommit.matches(COMMIT_PATTERN))
        request("POST", "$repoPath/git/refs", JSONObject()
            .put("ref", "refs/heads/$branch").put("sha", fromCommit))
    }

    override fun commitTree(commitSha: String): String {
        val tree = JSONObject(request("GET", "$repoPath/git/commits/$commitSha"))
            .optJSONObject("tree")?.optString("sha").orEmpty()
        require(tree.matches(COMMIT_PATTERN)) { "Pinned base commit has no valid tree" }
        return tree
    }

    override fun createBlob(bytes: ByteArray): String {
        val result = JSONObject(request("POST", "$repoPath/git/blobs", JSONObject()
            .put("content", Base64.getEncoder().encodeToString(bytes))
            .put("encoding", "base64")))
        return result.optString("sha").also {
            require(it.matches(COMMIT_PATTERN)) { "GitHub returned an invalid evidence blob" }
        }
    }

    override fun createTree(baseTreeSha: String, path: String, blobSha: String): String {
        val parts = path.split('/')
        require(parts.size >= 2 && parts.first() == ".factory-evidence" &&
            parts.none { it.isBlank() || it == "." || it == ".." || it.contains('\\') }) {
            "Unsafe evidence repository path"
        }
        val entries = JSONArray().put(JSONObject()
            .put("path", path).put("mode", "100644").put("type", "blob").put("sha", blobSha))
        val result = JSONObject(request("POST", "$repoPath/git/trees",
            JSONObject().put("base_tree", baseTreeSha).put("tree", entries)))
        return result.optString("sha").also {
            require(it.matches(COMMIT_PATTERN)) { "GitHub returned an invalid evidence tree" }
        }
    }

    override fun createCommit(message: String, treeSha: String, parentSha: String): String {
        val result = JSONObject(request("POST", "$repoPath/git/commits",
            JSONObject().put("message", message).put("tree", treeSha)
                .put("parents", JSONArray().put(parentSha))))
        return result.optString("sha").also {
            require(it.matches(COMMIT_PATTERN)) { "GitHub returned an invalid evidence commit" }
        }
    }

    override fun updateRef(branch: String, newCommit: String, expectedHead: String, force: Boolean) {
        require(!force) { "Force updates are forbidden for Factory evidence" }
        val current = branchHead(branch)
        if (current != expectedHead) throw SecurityException("Evidence branch head changed before update")
        request("PATCH", "$repoPath/git/refs/heads/${encodeSegment(branch)}",
            JSONObject().put("sha", newCommit).put("force", false))
        val updated = branchHead(branch)
        check(updated == newCommit) { "Evidence branch update could not be verified" }
    }

    override fun readFile(path: String, ref: String): ByteArray {
        val encodedPath = path.split('/').joinToString("/") { encodeSegment(it) }
        val encodedRef = URLEncoder.encode(ref, "UTF-8")
        val json = JSONObject(request("GET", "$repoPath/contents/$encodedPath?ref=$encodedRef"))
        require(json.optString("type") == "file" && json.optString("path") == path &&
            json.optString("encoding") == "base64") {
            "GitHub returned a different evidence file"
        }
        val bytes = Base64.getDecoder().decode(json.optString("content").filterNot { it.isWhitespace() })
        require(bytes.size == json.optInt("size", -1)) { "Evidence file size did not match GitHub metadata" }
        return bytes
    }

    override fun addIssueComment(issueNumber: Int, message: String) {
        require(issueNumber > 0 && message.length <= 4_000)
        request("POST", "$repoPath/issues/$issueNumber/comments", JSONObject().put("body", "[Vyom Factory] $message"))
    }

    private fun requestOrNull(method: String, path: String, payload: JSONObject? = null): String? =
        try { request(method, path, payload) } catch (error: GitHubEvidenceHttpException) {
            if (error.status == 404) null else throw error
        }

    private fun request(method: String, path: String, payload: JSONObject? = null): String {
        if (token.isBlank() || token.length > 500) throw IllegalStateException("GitHub token is missing or invalid")
        val url = apiBaseUrl.trimEnd('/') + path
        val builder = Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Authorization", "Bearer $token")
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post((payload ?: JSONObject()).toString().toRequestBody(jsonType))
            "PATCH" -> builder.patch((payload ?: JSONObject()).toString().toRequestBody(jsonType))
            else -> throw IllegalArgumentException("Unsupported evidence HTTP method")
        }
        http.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw GitHubEvidenceHttpException(response.code, body)
            return body
        }
    }

    private fun encodeSegment(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    private class GitHubEvidenceHttpException(val status: Int, detail: String) :
        IOException("GitHub evidence request failed with HTTP $status: ${detail.take(160)}")

    companion object {
        private val COMMIT_PATTERN = Regex("^[a-f0-9]{40}$")
    }
}
