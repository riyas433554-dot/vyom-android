package com.hermes.assistant

import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

internal data class ParsedContentUpdatePackage(
    val manifest: ContentUpdateManifest,
    val detachedSignature: ByteArray,
    val files: Map<String, ByteArray>
)

internal object ContentUpdatePackageArchive {
    private const val MAX_PACKAGE_BYTES = 10 * 1024 * 1024
    private const val MAX_ENTRIES = 110

    private fun readBounded(zip: ZipInputStream, remainingBytes: Long): ByteArray {
        require(remainingBytes >= 0L) { "Uncompressed content package exceeds 10 MiB" }
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = zip.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= remainingBytes) {
                "Uncompressed content package exceeds 10 MiB"
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    fun read(packageBytes: ByteArray): ParsedContentUpdatePackage {
        require(packageBytes.isNotEmpty() && packageBytes.size <= MAX_PACKAGE_BYTES) {
            "Content update package must be between 1 byte and 10 MiB"
        }
        val entries = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        ZipInputStream(ByteArrayInputStream(packageBytes)).use { zip ->
            var entry = zip.nextEntry
            var count = 0
            while (entry != null) {
                count++
                require(count <= MAX_ENTRIES) { "Content update package contains too many entries" }
                require(!entry.isDirectory) { "Directory entries are not allowed in content packages" }
                val entryBytes = readBounded(zip, MAX_PACKAGE_BYTES - totalBytes)
                require(entries.putIfAbsent(entry.name, entryBytes) == null) {
                    "Duplicate content package entry: ${entry.name}"
                }
                totalBytes += entryBytes.size
                require(totalBytes <= MAX_PACKAGE_BYTES) { "Uncompressed content package exceeds 10 MiB" }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val manifestBytes = entries.remove("manifest.json") ?: error("Content update manifest is missing")
        val canonicalBytes = entries.remove("manifest.canonical") ?: error("Canonical signed payload is missing")
        val signature = entries.remove("manifest.sig") ?: error("Detached package signature is missing")
        val manifest = ContentUpdateManifest.parse(String(manifestBytes, Charsets.UTF_8))
        require(manifest.canonicalPayload().contentEquals(canonicalBytes)) {
            "Canonical payload does not match the parsed manifest"
        }
        require(entries.keys == manifest.files.map { it.path }.toSet()) {
            "Package entries do not exactly match the manifest file list"
        }
        return ParsedContentUpdatePackage(manifest, signature, entries)
    }
}
