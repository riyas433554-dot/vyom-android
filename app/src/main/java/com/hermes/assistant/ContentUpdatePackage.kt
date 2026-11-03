package com.hermes.assistant

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

internal enum class ContentProbeKind { PRESENCE, PERFORMANCE }

internal data class ContentUpdateFile(
    val path: String,
    val sha256: String
)

internal data class ContentUpdateProbe(
    val id: String,
    val kind: ContentProbeKind,
    val target: String,
    val expected: String = "",
    val baselineKey: String = ""
)

internal data class ContentUpdateManifest(
    val version: String,
    val minAppVersion: String,
    val changes: List<String>,
    val files: List<ContentUpdateFile>,
    val probes: List<ContentUpdateProbe>,
    val rollbackTo: String
) {
    fun validate(currentAppVersion: String, suppliedFiles: Map<String, ByteArray>): List<String> {
        val errors = mutableListOf<String>()
        if (!isVersion(version)) errors += "Invalid content version"
        if (!isVersion(minAppVersion)) errors += "Invalid min_app_version"
        if (isVersion(minAppVersion) && isVersion(currentAppVersion) &&
            compareVersions(currentAppVersion, minAppVersion) < 0) {
            errors += "This update requires app version $minAppVersion or newer"
        }
        if (changes.isEmpty() || changes.any { it.isBlank() || it.length > 500 }) {
            errors += "Manifest changes must contain non-empty short descriptions"
        }
        if (files.isEmpty()) errors += "Manifest has no content files"
        val paths = files.map { it.path }
        if (paths.distinct().size != paths.size) errors += "Manifest contains duplicate file paths"
        files.forEach { file ->
            if (!isAllowedContentPath(file.path)) errors += "Protected or unsupported content path: ${file.path}"
            if (!file.sha256.matches(Regex("(?i)^[a-f0-9]{64}$"))) errors += "Invalid SHA-256 for ${file.path}"
        }
        if (suppliedFiles.keys != paths.toSet()) errors += "Package files do not exactly match the manifest"
        files.forEach { file ->
            val bytes = suppliedFiles[file.path] ?: return@forEach
            if (file.sha256.matches(Regex("(?i)^[a-f0-9]{64}$")) &&
                !file.sha256.equals(UiBundleIntegrity.sha256(bytes), ignoreCase = true)) {
                errors += "SHA-256 mismatch: ${file.path}"
            }
        }
        if (probes.size !in 1..2) errors += "Every content update must ship one or two probes"
        if (probes.firstOrNull()?.kind != ContentProbeKind.PRESENCE) {
            errors += "Probe 1 must be a presence probe"
        }
        if (probes.size == 2 && probes[1].kind != ContentProbeKind.PERFORMANCE) {
            errors += "Probe 2 must be a performance probe"
        }
        if (probes.map { it.id }.distinct().size != probes.size) errors += "Probe IDs must be unique"
        probes.forEach { probe ->
            if (!probe.id.matches(Regex("^[A-Za-z0-9._-]{1,64}$"))) errors += "Invalid probe ID"
            if (probe.target.isBlank() || probe.target.length > 240) errors += "Invalid probe target"
            if (probe.kind == ContentProbeKind.PERFORMANCE && probe.baselineKey.isBlank()) {
                errors += "Performance probe requires a stored baseline key"
            }
        }
        if (!isVersion(rollbackTo)) errors += "Invalid rollback_to version"
        return errors.distinct()
    }

    /** Deterministic, length-prefixed bytes are signed as a detached package signature. */
    fun canonicalPayload(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeUTF("VYOM-CONTENT-UPDATE-V1")
            out.writeUTF(version)
            out.writeUTF(minAppVersion)
            out.writeUTF(rollbackTo)
            out.writeInt(changes.size)
            changes.forEach(out::writeUTF)
            val sortedFiles = files.sortedBy { it.path }
            out.writeInt(sortedFiles.size)
            sortedFiles.forEach { out.writeUTF(it.path); out.writeUTF(it.sha256.lowercase()) }
            out.writeInt(probes.size)
            probes.forEach {
                out.writeUTF(it.id)
                out.writeUTF(it.kind.name)
                out.writeUTF(it.target)
                out.writeUTF(it.expected)
                out.writeUTF(it.baselineKey)
            }
        }
        return bytes.toByteArray()
    }

    companion object {
        private val contentRoots = setOf(
            "ui-bundle", "skills", "workflows", "prompts", "personas",
            "routing", "knowledge", "model-config", "policies"
        )
        private val protectedSegments = setOf(
            "kill-switch", "killswitch", "policy-engine", "update-verifier",
            "signing", "signing-config", "protected", "native", "androidmanifest.xml"
        )

        fun isAllowedContentPath(path: String): Boolean {
            if (path.isBlank() || path.length > 240 || path.startsWith("/") ||
                path.contains('\\') || path.contains(':') || path.contains('\u0000')) return false
            val segments = path.split('/')
            if (segments.size < 2 || segments.any { it.isBlank() || it == "." || it == ".." }) return false
            if (segments.first() !in contentRoots) return false
            if (segments.any { segment ->
                val lower = segment.lowercase()
                lower in protectedSegments ||
                    lower.endsWith(".kt") || lower.endsWith(".java") ||
                    lower.endsWith(".gradle") || lower.endsWith(".gradle.kts") ||
                    lower == "androidmanifest.xml"
            }) return false
            return true
        }

        fun isVersion(value: String): Boolean =
            value.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?$"))

        fun compareVersions(left: String, right: String): Int {
            fun parts(value: String) = value.substringBefore('-').substringBefore('+')
                .split('.').map { it.toIntOrNull() ?: 0 }
            val a = parts(left)
            val b = parts(right)
            for (index in 0 until maxOf(a.size, b.size)) {
                val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
                if (comparison != 0) return comparison
            }
            return 0
        }
    }
}

internal class ContentUpdatePackageVerifier(
    private val pinnedPublicKeyX509Base64: String
) {
    fun verify(
        manifest: ContentUpdateManifest,
        detachedSignature: ByteArray,
        suppliedFiles: Map<String, ByteArray>,
        installedAppVersion: String
    ): List<String> {
        val errors = manifest.validate(installedAppVersion, suppliedFiles).toMutableList()
        if (detachedSignature.isEmpty()) errors += "Package signature is missing"
        if (pinnedPublicKeyX509Base64.isBlank()) errors += "Content update trust key is not configured"
        if (errors.isNotEmpty()) return errors.distinct()
        try {
            val encodedKey = Base64.getDecoder().decode(pinnedPublicKeyX509Base64)
            val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(encodedKey))
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(manifest.canonicalPayload())
            if (!verifier.verify(detachedSignature)) errors += "Package signature verification failed"
        } catch (_: Exception) {
            errors += "Package signature or pinned trust key is invalid"
        }
        return errors.distinct()
    }
}
