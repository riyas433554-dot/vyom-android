package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Owner-approved mapping from a Vyom system to one immutable repository revision
 * and explicit relative path roots. This registry is local metadata; execution
 * adapters must call allowsPath before reading or changing a repository path.
 */
internal class VyomFactorySystemRegistry(private val root: File) {
    data class Binding(
        val systemId: String,
        val repositoryUrl: String,
        val commitSha: String,
        val allowedPaths: List<String>,
        val registeredAtEpochMs: Long
    )

    @Synchronized
    fun register(binding: Binding, ownerConfirmed: Boolean): Binding {
        check(ownerConfirmed) { "Owner approval is required to bind a system to a repository" }
        validate(binding)
        val dir = root.canonicalFile
        if (!dir.exists()) check(dir.mkdirs()) { "Could not create system registry" }
        check(dir.isDirectory)
        val destination = File(dir, "${fileKey(binding.systemId)}.json").canonicalFile
        check(destination.parentFile == dir) { "System registry path escaped its root" }
        if (destination.exists()) {
            val existing = read(destination)
            require(existing == binding) { "System already has a different repository binding; explicit migration is required" }
            return existing
        }
        val temp = File(dir, ".${destination.name}.${UUID.randomUUID()}.tmp")
        FileOutputStream(temp).use { out ->
            out.write(encode(binding).toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        if (!temp.renameTo(destination)) {
            temp.delete()
            throw IllegalStateException("Could not publish system registry binding")
        }
        return read(destination)
    }

    /**
     * Explicit owner-approved compare-and-swap migration. The caller must present
     * the exact previous binding observed before confirmation; stale confirmations
     * cannot overwrite a newer registry entry.
     */
    @Synchronized
    fun migrate(
        replacement: Binding,
        expectedExisting: Binding,
        ownerConfirmed: Boolean
    ): Binding {
        check(ownerConfirmed) { "Owner approval is required to migrate a system binding" }
        validate(replacement)
        require(replacement.systemId == expectedExisting.systemId) {
            "A migration cannot change the system ID"
        }
        val dir = root.canonicalFile
        check(dir.exists() && dir.isDirectory) { "System registry does not exist" }
        val destination = File(dir, "${fileKey(replacement.systemId)}.json").canonicalFile
        check(destination.parentFile == dir) { "System registry path escaped its root" }
        if (!destination.isFile) throw SecurityException("Cannot migrate a system without an existing binding")
        val current = read(destination)
        if (current != expectedExisting) {
            throw SecurityException("System binding changed after owner review; inspect it again before migration")
        }
        if (replacement == current) return current

        val temp = File(dir, ".${destination.name}.${UUID.randomUUID()}.migration.tmp")
        try {
            FileOutputStream(temp).use { out ->
                out.write(encode(replacement).toByteArray(Charsets.UTF_8))
                out.flush()
                out.fd.sync()
            }
            if (!temp.renameTo(destination)) {
                throw IllegalStateException("Could not atomically publish migrated system binding")
            }
            return read(destination)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    @Synchronized
    fun get(systemId: String): Binding? {
        require(systemId.matches(Regex("^[a-z][a-z0-9_.-]{0,63}$")))
        val file = File(root.canonicalFile, "${fileKey(systemId)}.json")
        if (!file.exists()) return null
        return read(file)
    }

    @Synchronized
    fun list(): List<Binding> {
        if (!root.exists()) return emptyList()
        check(root.canonicalFile.isDirectory)
        return root.canonicalFile.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }
            .orEmpty().map(::read).sortedBy { it.systemId }
    }

    fun allowsPath(systemId: String, relativePath: String): Boolean =
        runCatching { requireAuthorizedBinding(systemId, relativePath); true }.getOrDefault(false)

    /** Resolve the immutable system binding and fail closed before any repository read or write. */
    fun requireAuthorizedBinding(
        systemId: String,
        relativePath: String,
        repositoryUrl: String? = null,
        commitSha: String? = null
    ): Binding {
        val binding = get(systemId) ?: throw SecurityException("System has no approved repository binding")
        if (!isSafeRelativePath(relativePath) || binding.allowedPaths.none { rootPath ->
                relativePath == rootPath || relativePath.startsWith("$rootPath/")
            }) throw SecurityException("Repository path is outside this system's allowed roots")
        if (repositoryUrl != null && repositoryUrl != binding.repositoryUrl) {
            throw SecurityException("Repository does not match this system's immutable binding")
        }
        if (commitSha != null && commitSha != binding.commitSha) {
            throw SecurityException("Commit does not match this system's immutable binding")
        }
        return binding
    }

    private fun validate(binding: Binding) {
        require(binding.systemId.matches(Regex("^[a-z][a-z0-9_.-]{0,63}$"))) { "Invalid system ID" }
        val source = VyomFactoryRepositoryIntake.parseUrl(binding.repositoryUrl)
            ?: throw IllegalArgumentException("Unsupported repository URL")
        require(source.canonicalUrl == binding.repositoryUrl) { "Repository URL must be canonical" }
        require(binding.commitSha.matches(Regex("^[a-f0-9]{40}$"))) { "Repository revision must be immutable" }
        require(binding.allowedPaths.isNotEmpty()) { "At least one allowed path is required" }
        require(binding.allowedPaths.distinct().size == binding.allowedPaths.size)
        require(binding.allowedPaths.all(::isSafeRelativePath)) { "Allowed paths must be safe relative path roots" }
        require(binding.registeredAtEpochMs > 0)
    }

    private fun isSafeRelativePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\") || path.contains("\\") ||
            path.any { it.isISOControl() }) return false
        val parts = path.split("/")
        return parts.all { it.isNotBlank() && it != "." && it != ".." } &&
            !path.matches(Regex("^[A-Za-z]:.*"))
    }

    private fun encode(binding: Binding): String = JSONObject()
        .put("schema_version", 1)
        .put("system_id", binding.systemId)
        .put("repository_url", binding.repositoryUrl)
        .put("commit_sha", binding.commitSha)
        .put("allowed_paths", JSONArray(binding.allowedPaths))
        .put("registered_at_epoch_ms", binding.registeredAtEpochMs)
        .toString(2)

    private fun read(file: File): Binding {
        val dir = root.canonicalFile
        val canonical = file.canonicalFile
        check(canonical.parentFile == dir) { "System registry entry escaped its root" }
        val json = JSONObject(canonical.readText(Charsets.UTF_8))
        check(json.optInt("schema_version", -1) == 1)
        val paths = json.optJSONArray("allowed_paths") ?: error("Allowed paths missing")
        val binding = Binding(
            json.getString("system_id"), json.getString("repository_url"),
            json.getString("commit_sha"),
            (0 until paths.length()).map { paths.getString(it) },
            json.getLong("registered_at_epoch_ms")
        )
        validate(binding)
        return binding
    }

    private fun fileKey(systemId: String): String =
        VyomFactorySkillPolicy.sha256(systemId.toByteArray(Charsets.UTF_8)).take(32)
}
