package com.hermes.assistant

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64

/** Public GitHub skill discovery/import. Downloaded instructions are stored as inert text and never executed. */
internal class VyomFactorySkillRepositoryClient(
    private val apiBaseUrl: String = "https://api.github.com"
) {
    data class Candidate(val fullName: String, val description: String, val stars: Int, val url: String)
    data class Preview(
        val repository: String,
        val branch: String,
        val commitSha: String,
        val license: String,
        val skillMarkdown: String,
        val sourceUrl: String,
        val skillPath: String
    )

    fun search(query: String): List<Candidate> {
        val clean = query.trim()
        require(clean.length in 2..80 && clean.none { it.isISOControl() }) { "Search must contain 2–80 ordinary characters" }
        val encoded = URLEncoder.encode("$clean skill", "UTF-8")
        val json = JSONObject(get("/search/repositories?q=$encoded&sort=stars&order=desc&per_page=10"))
        val items = json.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val fullName = item.optString("full_name")
            if (!VyomFactorySkillPolicy.isValidRepository(fullName)) return@mapNotNull null
            Candidate(
                fullName = fullName,
                description = item.optString("description").take(240),
                stars = item.optInt("stargazers_count", 0).coerceAtLeast(0),
                url = item.optString("html_url")
            )
        }
    }

    fun inspect(repository: String): Preview {
        require(VyomFactorySkillPolicy.isValidRepository(repository)) { "Use an exact owner/repository name" }
        val metadata = JSONObject(get("/repos/$repository"))
        val branch = metadata.optString("default_branch")
        val license = metadata.optJSONObject("license")?.optString("spdx_id").orEmpty()
        require(VyomFactorySkillPolicy.isAllowedLicense(license)) {
            "Repository license '$license' is not on the approved permissive-license list"
        }
        require(branch.isNotBlank() && branch.length <= 200 && !branch.contains("..")) {
            "Repository default branch is invalid"
        }
        val commitPayload = JSONObject(get("/repos/$repository/commits/${pathSegment(branch)}"))
        val commit = commitPayload.optString("sha")
        require(commit.matches(Regex("^[a-f0-9]{40}$"))) { "Could not resolve an immutable repository commit" }
        val treeSha = commitPayload.optJSONObject("commit")?.optJSONObject("tree")?.optString("sha").orEmpty()
        require(treeSha.matches(Regex("^[a-f0-9]{40}$"))) { "Could not resolve the repository tree" }
        val tree = JSONObject(get("/repos/$repository/git/trees/$treeSha?recursive=1"))
        require(!tree.optBoolean("truncated")) { "Repository tree is too large to inspect safely; choose a smaller skill repository" }
        val entries = tree.optJSONArray("tree") ?: org.json.JSONArray()
        val paths = (0 until entries.length()).mapNotNull { index ->
            entries.optJSONObject(index)?.takeIf { it.optString("type") == "blob" }
                ?.optString("path")?.takeIf { it.endsWith("/SKILL.md") || it == "SKILL.md" }
        }
        val skillPath = VyomFactorySkillPolicy.selectSkillDocumentPath(paths)
            ?: throw IllegalStateException("No SKILL.md was found in the repository tree")
        val encodedBranch = URLEncoder.encode(branch, "UTF-8")
        val encodedPath = skillPath.split("/").joinToString("/") { pathSegment(it) }
        val file = JSONObject(get("/repos/$repository/contents/$encodedPath?ref=$encodedBranch"))
        require(file.optString("encoding") == "base64") { "Selected SKILL.md is not returned as base64 text" }
        val raw = file.optString("content").replace("\n", "").trim()
        require(raw.isNotBlank() && raw.length <= VyomFactorySkillPolicy.MAX_BASE64_LENGTH) {
            "SKILL.md is empty or exceeds the safe import size"
        }
        val bytes = Base64.getMimeDecoder().decode(raw)
        val markdown = VyomFactorySkillPolicy.decodeDocument(bytes)
        return Preview(repository, branch, commit, license, markdown, "https://github.com/$repository/blob/$branch/$skillPath", skillPath)
    }

    private fun pathSegment(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun get(path: String): String {
        val connection = (URL(apiBaseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Vyom-Factory-Skill-Importer")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("GitHub skill source returned HTTP $code")
            require(body.length <= MAX_RESPONSE_CHARS) { "GitHub skill response exceeded the safe size limit" }
            return body
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val MAX_RESPONSE_CHARS = 1_000_000
    }
}

/** Validation and app-private, content-only storage for imported SKILL.md documents. */
internal object VyomFactorySkillPolicy {
    const val MAX_DOCUMENT_BYTES = 64 * 1024
    const val MAX_BASE64_LENGTH = MAX_DOCUMENT_BYTES * 2
    private val repositoryPattern = Regex("^[A-Za-z0-9_.-]{1,39}/[A-Za-z0-9_.-]{1,100}$")
    private val approvedLicenses = setOf("MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC", "CC0-1.0")

    fun isValidRepository(value: String): Boolean =
        repositoryPattern.matches(value) && !value.contains("..")

    fun isAllowedLicense(spdx: String): Boolean = spdx in approvedLicenses

    fun selectSkillDocumentPath(paths: List<String>): String? =
        paths.asSequence()
            .filter { it == "SKILL.md" || it.endsWith("/SKILL.md") }
            .filter { path -> path.split("/").all { it.isNotBlank() && it != "." && it != ".." } }
            .sortedWith(compareBy<String> { if (it == "SKILL.md") 0 else 1 }
                .thenBy { it.count { char -> char == '/' } }
                .thenBy { it })
            .firstOrNull()


    fun decodeDocument(bytes: ByteArray): String {
        require(bytes.isNotEmpty() && bytes.size <= MAX_DOCUMENT_BYTES) { "SKILL.md must be 1–64 KiB" }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val markdown = decoder.decode(ByteBuffer.wrap(bytes)).toString()
        validateDocument(markdown)
        return markdown
    }

    fun storageFolderName(repository: String): String {
        require(isValidRepository(repository))
        return sha256(repository.toByteArray(Charsets.UTF_8)).take(24)
    }

    fun validateDocument(markdown: String) {
        val bytes = markdown.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= MAX_DOCUMENT_BYTES) { "SKILL.md must be 1–64 KiB" }
        require(markdown.none { it == '\u0000' || (it.isISOControl() && it != '\n' && it != '\r' && it != '\t') }) {
            "SKILL.md contains binary or control characters"
        }
        require(Regex("(?m)^#\\s+\\S").containsMatchIn(markdown)) {
            "SKILL.md must contain a Markdown title heading"
        }
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}

internal class VyomFactorySkillStore(private val root: File) {
    data class Installed(
        val repository: String,
        val commitSha: String,
        val license: String,
        val sha256: String,
        val skillFile: File
    )

    data class Audit(val total: Int, val valid: Int, val invalid: Int)

    fun auditInstalled(): Audit {
        if (!root.exists()) return Audit(0, 0, 0)
        val rootCanonical = root.canonicalFile
        val directories = rootCanonical.listFiles()?.filter { it.isDirectory }.orEmpty()
        var valid = 0
        var invalid = 0
        directories.forEach { directory ->
            try {
                check(directory.canonicalFile.parentFile == rootCanonical)
                val skill = File(directory, "SKILL.md")
                val metadataFile = File(directory, "source.json")
                check(skill.isFile && metadataFile.isFile)
                val markdown = skill.readText(Charsets.UTF_8)
                VyomFactorySkillPolicy.validateDocument(markdown)
                val metadata = JSONObject(metadataFile.readText(Charsets.UTF_8))
                val repository = metadata.optString("repository")
                val commit = metadata.optString("commit_sha")
                val license = metadata.optString("license")
                val hash = metadata.optString("sha256")
                check(VyomFactorySkillPolicy.isValidRepository(repository))
                check(directory.name == VyomFactorySkillPolicy.storageFolderName(repository))
                check(commit.matches(Regex("^[a-f0-9]{40}$")))
                check(VyomFactorySkillPolicy.isAllowedLicense(license))
                check(metadata.optString("content_type") == "inert-markdown")
                check(hash.matches(Regex("^[a-f0-9]{64}$")))
                check(VyomFactorySkillPolicy.sha256(skill.readBytes()) == hash)
                valid++
            } catch (_: Exception) {
                invalid++
            }
        }
        return Audit(directories.size, valid, invalid)
    }

    fun install(preview: VyomFactorySkillRepositoryClient.Preview, ownerConfirmed: Boolean): Installed {
        check(ownerConfirmed) { "Owner review and explicit Install confirmation are required" }
        require(VyomFactorySkillPolicy.isValidRepository(preview.repository))
        require(preview.commitSha.matches(Regex("^[a-f0-9]{40}$")))
        require(VyomFactorySkillPolicy.isAllowedLicense(preview.license))
        VyomFactorySkillPolicy.validateDocument(preview.skillMarkdown)

        val rootCanonical = root.canonicalFile
        if (!rootCanonical.exists()) check(rootCanonical.mkdirs()) { "Could not create app-private skill storage" }
        val folderName = VyomFactorySkillPolicy.storageFolderName(preview.repository)
        val directory = File(rootCanonical, folderName).canonicalFile
        check(directory.parentFile == rootCanonical) { "Skill destination escaped app-private storage" }
        if (!directory.exists()) check(directory.mkdirs()) { "Could not create skill directory" }
        val skill = File(directory, "SKILL.md")
        val metadata = File(directory, "source.json")
        check(!skill.exists() && !metadata.exists()) { "This skill is already installed; silent replacement is disabled" }
        val skillTemp = File(directory, "SKILL.md.tmp")
        val metadataTemp = File(directory, "source.json.tmp")
        val bytes = preview.skillMarkdown.toByteArray(Charsets.UTF_8)
        val hash = VyomFactorySkillPolicy.sha256(bytes)
        val metadataJson = JSONObject()
            .put("repository", preview.repository)
            .put("branch", preview.branch)
            .put("commit_sha", preview.commitSha)
            .put("license", preview.license)
            .put("source_url", preview.sourceUrl)
            .put("sha256", hash)
            .put("content_type", "inert-markdown")
        writeSynced(skillTemp, bytes)
        writeSynced(metadataTemp, metadataJson.toString(2).toByteArray(Charsets.UTF_8))
        check(skillTemp.renameTo(skill)) { "Could not atomically publish the reviewed SKILL.md" }
        check(metadataTemp.renameTo(metadata)) {
            skill.delete()
            "Could not publish skill source metadata"
        }
        val stored = skill.readBytes()
        check(VyomFactorySkillPolicy.sha256(stored) == hash) { "Installed skill hash verification failed" }
        val storedMetadata = JSONObject(metadata.readText(Charsets.UTF_8))
        check(storedMetadata.optString("commit_sha") == preview.commitSha &&
            storedMetadata.optString("sha256") == hash &&
            storedMetadata.optString("content_type") == "inert-markdown") {
            "Installed skill source metadata verification failed"
        }
        return Installed(preview.repository, preview.commitSha, preview.license, hash, skill)
    }

    private fun writeSynced(file: File, bytes: ByteArray) {
        FileOutputStream(file).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
    }
}
