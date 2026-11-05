package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * App-private catalogue of owner-approved, immutable repository references.
 * This is source metadata, not a task queue and not an executable plugin registry.
 */
internal class VyomFactoryRepositoryRegistry(private val root: File) {
    data class Entry(
        val canonicalUrl: String,
        val commitSha: String,
        val licenseSpdx: String,
        val kind: VyomFactoryRepositoryIntake.Kind,
        val decision: VyomFactoryRepositoryIntake.Decision,
        val reasons: List<String>,
        val detectedKinds: Set<VyomFactoryRepositoryIntake.Kind>,
        val addedAtEpochMs: Long,
        val sourceExecutionAllowed: Boolean = false
    )

    @Synchronized
    fun register(
        preview: VyomFactoryRepositoryClient.Preview,
        ownerConfirmed: Boolean
    ): Entry {
        check(ownerConfirmed) { "Owner must explicitly approve registering this repository source" }
        val snapshot = preview.snapshot
        val assessment = preview.assessment
        require(assessment.decision != VyomFactoryRepositoryIntake.Decision.BLOCKED) {
            "Blocked repositories cannot be registered"
        }
        require(assessment.source == snapshot.source &&
            assessment.immutableRef == snapshot.commitSha &&
            snapshot.commitSha.matches(Regex("^[a-f0-9]{40}$"))) {
            "Repository assessment does not match the inspected immutable snapshot"
        }
        require(!assessment.sourceExecutionAllowed && !assessment.installationApproved) {
            "Repository intake cannot grant execution or installation permission"
        }

        val directory = root.canonicalFile
        if (!directory.exists()) check(directory.mkdirs()) { "Could not create app-private repository registry" }
        check(directory.isDirectory) { "Repository registry path is not a directory" }
        val fileName = VyomFactoryRepositoryIntakeHash.of(snapshot.source.canonicalUrl) + ".json"
        val destination = File(directory, fileName).canonicalFile
        check(destination.parentFile == directory) { "Repository registry path escaped app-private storage" }

        if (destination.exists()) {
            val existing = readEntry(destination)
            require(existing.canonicalUrl == snapshot.source.canonicalUrl) {
                "Repository registry identity hash collision"
            }
            require(existing.commitSha == snapshot.commitSha) {
                "This repository is already registered at another commit; silent source updates are disabled"
            }
            return existing
        }

        val entry = Entry(
            canonicalUrl = snapshot.source.canonicalUrl,
            commitSha = snapshot.commitSha,
            licenseSpdx = snapshot.licenseSpdx,
            kind = assessment.kind,
            decision = assessment.decision,
            reasons = assessment.reasons.take(20).map { it.take(120) },
            detectedKinds = assessment.detectedKinds,
            addedAtEpochMs = System.currentTimeMillis()
        )
        val temp = File(directory, ".$fileName.${UUID.randomUUID()}.tmp")
        writeSynced(temp, encode(entry).toByteArray(Charsets.UTF_8))
        if (!temp.renameTo(destination)) {
            temp.delete()
            if (destination.exists()) {
                val existing = readEntry(destination)
                require(existing.canonicalUrl == entry.canonicalUrl && existing.commitSha == entry.commitSha) {
                    "A different repository revision was registered concurrently"
                }
                return existing
            }
            throw IllegalStateException("Could not publish the repository registry entry")
        }
        return readEntry(destination)
    }

    @Synchronized
    fun list(): List<Entry> {
        if (!root.exists()) return emptyList()
        val directory = root.canonicalFile
        check(directory.isDirectory) { "Repository registry path is not a directory" }
        return directory.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }
            .orEmpty()
            .map(::readEntry)
            .sortedBy { it.canonicalUrl }
    }

    private fun encode(entry: Entry): String = JSONObject()
        .put("schema_version", 1)
        .put("canonical_url", entry.canonicalUrl)
        .put("commit_sha", entry.commitSha)
        .put("license_spdx", entry.licenseSpdx)
        .put("kind", entry.kind.name)
        .put("decision", entry.decision.name)
        .put("reasons", JSONArray(entry.reasons))
        .put("detected_kinds", JSONArray(entry.detectedKinds.map { it.name }))
        .put("added_at_epoch_ms", entry.addedAtEpochMs)
        .put("source_execution_allowed", false)
        .put("installation_approved", false)
        .toString(2)

    private fun readEntry(file: File): Entry {
        val rootCanonical = root.canonicalFile
        val canonical = file.canonicalFile
        check(canonical.parentFile == rootCanonical) { "Repository registry entry escaped app-private storage" }
        val json = JSONObject(canonical.readText(Charsets.UTF_8))
        check(json.optInt("schema_version", -1) == 1) { "Unknown repository registry schema" }
        val url = json.optString("canonical_url")
        val source = VyomFactoryRepositoryIntake.parseUrl(url)
            ?: throw IllegalStateException("Invalid repository URL in local registry")
        check(source.canonicalUrl == url) { "Non-canonical repository URL in local registry" }
        val commit = json.optString("commit_sha")
        check(commit.matches(Regex("^[a-f0-9]{40}$"))) { "Invalid immutable repository commit in local registry" }
        check(!json.optBoolean("source_execution_allowed", true) &&
            !json.optBoolean("installation_approved", true)) {
            "Repository registry cannot contain execution or installation grants"
        }
        val kind = runCatching { VyomFactoryRepositoryIntake.Kind.valueOf(json.optString("kind")) }
            .getOrElse { throw IllegalStateException("Unknown repository kind in local registry") }
        val decision = runCatching { VyomFactoryRepositoryIntake.Decision.valueOf(json.optString("decision")) }
            .getOrElse { throw IllegalStateException("Unknown repository decision in local registry") }
        check(decision != VyomFactoryRepositoryIntake.Decision.BLOCKED)
        val reasonArray = json.optJSONArray("reasons") ?: JSONArray()
        val reasons = (0 until reasonArray.length()).map { reasonArray.optString(it).take(120) }
        val detectedArray = json.optJSONArray("detected_kinds")
            ?: throw IllegalStateException("Repository artifact types are missing from local registry")
        val detectedKinds = (0 until detectedArray.length()).map { index ->
            runCatching { VyomFactoryRepositoryIntake.Kind.valueOf(detectedArray.optString(index)) }
                .getOrElse { throw IllegalStateException("Unknown repository artifact type in local registry") }
        }.toSet()
        check(detectedKinds.isNotEmpty() && kind in detectedKinds)
        val addedAt = json.optLong("added_at_epoch_ms", 0L)
        check(addedAt > 0L)
        return Entry(url, commit, json.optString("license_spdx"), kind, decision, reasons, detectedKinds, addedAt, false)
    }

    private fun writeSynced(file: File, bytes: ByteArray) {
        FileOutputStream(file).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
    }
}

private object VyomFactoryRepositoryIntakeHash {
    fun of(value: String): String = VyomFactorySkillPolicy.sha256(value.toByteArray(Charsets.UTF_8)).take(32)
}
