package com.hermes.assistant

import java.io.File

internal data class ContentProbeResult(
    val id: String,
    val passed: Boolean,
    val durationMs: Long,
    val detail: String
)

internal data class ContentUpdateResult(
    val version: String,
    val passed: Boolean,
    val probeResults: List<ContentProbeResult>,
    val timingDeltaMs: Long?,
    val message: String
) {
    fun timingSummary(): String = when {
        timingDeltaMs == null -> "baseline not established"
        timingDeltaMs < 0L -> "faster ${-timingDeltaMs} ms"
        timingDeltaMs > 0L -> "slower +${timingDeltaMs} ms"
        else -> "same"
    }
}

internal fun interface ContentUpdateProbeRunner {
    fun run(probe: ContentUpdateProbe, stagedSlot: File): ContentProbeResult
}

/** Inactive-slot staging with a single atomic pointer replacement after all probes pass. */
internal class ContentUpdateSlotStore(private val root: File) {
    private val activePointer get() = File(root, "active-slot")
    private val previousPointer get() = File(root, "previous-slot")
    private val historyFile get() = File(root, "update-history.log")
    private val baselineDirectory get() = File(root, "probe-baselines")

    fun activeVersion(): String = activePointer.takeIf { it.isFile }?.readText(Charsets.UTF_8)?.trim().orEmpty()

    fun recordPerformanceBaseline(key: String, durationMs: Long) {
        require(durationMs >= 0L) { "Performance duration cannot be negative" }
        writeAtomically(baselineFile(key), durationMs.toString())
    }

    /** Seeds the first trusted slot from APK-bundled assets; never overwrites an existing active slot. */
    fun seedInitialSlot(version: String, bundledFiles: Map<String, ByteArray>) {
        require(version.matches(Regex("^[A-Za-z0-9._-]{1,64}$"))) { "Invalid bundled content version" }
        if (activeVersion().isNotBlank()) return
        require(bundledFiles.isNotEmpty()) { "Bundled content slot is empty" }
        val staging = File(root, ".seed-$version")
        root.mkdirs()
        staging.deleteRecursively()
        require(staging.mkdirs()) { "Could not create initial content slot" }
        try {
            bundledFiles.forEach { (path, bytes) ->
                require(ContentUpdateManifest.isAllowedContentPath(path)) { "Protected bundled path rejected: $path" }
                val target = File(staging, path)
                require(target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) { "Bundled path escaped slot" }
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
            }
            val destination = File(root, version)
            require(!destination.exists()) { "Initial content slot already exists without an active pointer" }
            require(staging.renameTo(destination)) { "Could not seal initial content slot" }
            writeAtomically(activePointer, version)
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }

    fun applySigned(
        manifest: ContentUpdateManifest,
        detachedSignature: ByteArray,
        files: Map<String, ByteArray>,
        installedAppVersion: String,
        verifier: ContentUpdatePackageVerifier,
        probeRunner: ContentUpdateProbeRunner
    ): ContentUpdateResult {
        val verificationErrors = verifier.verify(manifest, detachedSignature, files, installedAppVersion)
        if (verificationErrors.isNotEmpty()) {
            val result = ContentUpdateResult(manifest.version, false, emptyList(), null,
                "Package rejected: ${verificationErrors.joinToString("; ")}")
            appendHistory(result)
            return result
        }
        return stageVerifiedPackage(manifest, files, probeRunner)
    }

    private fun stageVerifiedPackage(
        manifest: ContentUpdateManifest,
        files: Map<String, ByteArray>,
        probeRunner: ContentUpdateProbeRunner
    ): ContentUpdateResult {
        val oldVersion = activeVersion()
        if (oldVersion.isNotBlank() && manifest.rollbackTo != oldVersion) {
            return ContentUpdateResult(manifest.version, false, emptyList(), null,
                "rollback_to does not match the currently active slot")
        }
        if (oldVersion.isNotBlank() && oldVersion == manifest.version) {
            return ContentUpdateResult(manifest.version, false, emptyList(), null, "Version is already active")
        }
        root.mkdirs()
        val staging = File(root, ".staging-${manifest.version}")
        staging.deleteRecursively()
        require(staging.mkdirs()) { "Could not create inactive content slot" }
        try {
            files.forEach { (path, bytes) ->
                require(ContentUpdateManifest.isAllowedContentPath(path)) { "Protected content path rejected: $path" }
                val target = File(staging, path)
                require(target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                    "Content path escaped staging slot"
                }
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
            }
            val stagedResults = manifest.probes.map { probe -> probeRunner.run(probe, staging) }
            val allPassed = stagedResults.size == manifest.probes.size && stagedResults.all { it.passed }
            if (!allPassed) {
                staging.deleteRecursively()
                val failed = stagedResults.filterNot { it.passed }.joinToString { it.id }
                val result = ContentUpdateResult(manifest.version, false, stagedResults, null, "Probe failed: $failed; previous slot remains active")
                appendHistory(result)
                return result
            }
            if (oldVersion.isNotBlank()) writeAtomically(previousPointer, oldVersion)
            val finalSlot = File(root, manifest.version)
            if (finalSlot.exists()) finalSlot.deleteRecursively()
            require(staging.renameTo(finalSlot)) { "Could not seal staged content slot" }
            writeAtomically(activePointer, manifest.version)
            val performance = stagedResults.lastOrNull()?.takeIf {
                manifest.probes.lastOrNull()?.kind == ContentProbeKind.PERFORMANCE
            }
            val delta = performance?.let { result ->
                val baselineFile = baselineFile(manifest.probes.last().baselineKey)
                val baseline = baselineFile.takeIf { it.isFile }?.readText(Charsets.UTF_8)?.toLongOrNull()
                if (baseline == null) {
                    writeAtomically(baselineFile, result.durationMs.toString())
                    0L
                } else {
                    writeAtomically(baselineFile, result.durationMs.toString())
                    result.durationMs - baseline
                }
            }
            val result = ContentUpdateResult(manifest.version, true, stagedResults, delta, "Content update activated")
            appendHistory(result)
            return result
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }

    /** Called when the newly active page or skill fails its first real load. */
    fun rollbackAfterLoadFailure(failedVersion: String): Boolean {
        if (failedVersion.isBlank() || activeVersion() != failedVersion) return false
        val restored = rollback()
        if (restored) {
            appendHistory(ContentUpdateResult(failedVersion, false, emptyList(), null,
                "Automatic rollback after page/skill load failure"))
        }
        return restored
    }

    fun rollback(): Boolean {
        val previous = previousPointer.takeIf { it.isFile }?.readText(Charsets.UTF_8)?.trim().orEmpty()
        if (!previous.matches(Regex("^[A-Za-z0-9._-]{1,64}$"))) return false
        if (!File(root, previous).isDirectory) return false
        val current = activeVersion()
        writeAtomically(activePointer, previous)
        if (current.isNotBlank()) writeAtomically(previousPointer, current)
        appendHistory(ContentUpdateResult(previous, true, emptyList(), null, "Owner-triggered rollback from $current"))
        return true
    }

    fun recordCrashAndRollback(nowMs: Long): Boolean {
        val active = activeVersion()
        if (active.isBlank()) return false
        val crashFile = File(root, "crashes-$active.log")
        val recent = crashFile.takeIf { it.isFile }?.readLines()?.mapNotNull { it.toLongOrNull() }
            .orEmpty().filter { nowMs >= it && nowMs - it <= CRASH_WINDOW_MS }
        val updated = recent + nowMs
        writeAtomically(crashFile, updated.joinToString("\n"))
        if (updated.size < 2) return false
        val rolledBack = rollback()
        if (rolledBack) crashFile.delete()
        return rolledBack
    }

    fun recordStableLaunch() {
        val active = activeVersion()
        if (active.isNotBlank()) File(root, "crashes-$active.log").delete()
    }

    fun history(): List<String> = historyFile.takeIf { it.isFile }?.readLines().orEmpty()

    private fun baselineFile(key: String): File {
        require(key.matches(Regex("^[A-Za-z0-9._-]{1,80}$"))) { "Invalid performance baseline key" }
        baselineDirectory.mkdirs()
        return File(baselineDirectory, key)
    }

    private fun appendHistory(result: ContentUpdateResult) {
        historyFile.parentFile?.mkdirs()
        historyFile.appendText(
            "${System.currentTimeMillis()}|${result.version}|${if (result.passed) "PASS" else "FAIL"}|deltaMs=${result.timingDeltaMs ?: "n/a"}|probes=${result.probeResults.joinToString(",") { "${it.id}:${if (it.passed) "PASS" else "FAIL"}:${it.durationMs}ms" }}|${result.message.replace('|', '/') }\n",
            Charsets.UTF_8
        )
    }

    private fun writeAtomically(target: File, value: String) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(value, Charsets.UTF_8)
        try {
            java.nio.file.Files.move(
                temp.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
        } catch (error: Exception) {
            temp.delete()
            throw IllegalStateException("Atomic pointer replacement is unavailable for ${target.name}", error)
        }
    }

    companion object { private const val CRASH_WINDOW_MS = 60_000L }
}
