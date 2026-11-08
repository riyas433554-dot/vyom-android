package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ContentUpdatePackageArchiveTest {
    private val content = "A tested calendar skill".toByteArray()
    private val manifest = ContentUpdateManifest(
        "1.2.0", "0.3.1", listOf("Update calendar skill"),
        listOf(ContentUpdateFile("skills/calendar/SKILL.md", UiBundleIntegrity.sha256(content))),
        listOf(ContentUpdateProbe("presence", ContentProbeKind.PRESENCE, "skills/calendar/SKILL.md", "A tested calendar skill")),
        "1.1.0"
    )

    private fun manifestJson(value: ContentUpdateManifest = manifest): ByteArray {
        val root = JSONObject()
            .put("version", value.version)
            .put("min_app_version", value.minAppVersion)
            .put("rollback_to", value.rollbackTo)
            .put("changes", JSONArray().apply { value.changes.forEach(::put) })
            .put("files", JSONArray().apply {
                value.files.forEach { put(JSONObject().put("path", it.path).put("sha256", it.sha256)) }
            })
            .put("probes", JSONArray().apply {
                value.probes.forEach {
                    put(JSONObject().put("id", it.id).put("type", it.kind.name.lowercase())
                        .put("target", it.target).put("expected", it.expected).put("baseline_key", it.baselineKey))
                }
            })
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    private fun archive(includeUnlisted: Boolean = false): Pair<ByteArray, ByteArray> {
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val signer = Signature.getInstance("SHA256withECDSA").apply {
            initSign(pair.private)
            update(manifest.canonicalPayload())
        }
        val signature = signer.sign()
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun add(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            add("manifest.json", manifestJson())
            add("manifest.canonical", manifest.canonicalPayload())
            add("manifest.sig", signature)
            add("skills/calendar/SKILL.md", content)
            if (includeUnlisted) add("skills/extra/SKILL.md", "unlisted".toByteArray())
        }
        return output.toByteArray() to pair.public.encoded
    }

    @Test fun readsSignedPackageAndVerifiesManifestAndFileHashes() {
        val (bytes, publicKey) = archive()
        val parsed = ContentUpdatePackageArchive.read(bytes)
        val verifier = ContentUpdatePackageVerifier(Base64.getEncoder().encodeToString(publicKey))
        assertTrue(verifier.verify(parsed.manifest, parsed.detachedSignature, parsed.files, "0.3.1").isEmpty())
    }

    @Test fun rejectsUnlistedPackageFilesAndManifestTampering() {
        val (bytes, _) = archive(includeUnlisted = true)
        assertThrows(IllegalArgumentException::class.java) { ContentUpdatePackageArchive.read(bytes) }
        val (validBytes, _) = archive()
        val parsed = ContentUpdatePackageArchive.read(validBytes)
        assertEquals("1.2.0", parsed.manifest.version)
        val bad = parsed.manifest.copy(changes = listOf("different change"))
        assertNotEquals(manifest.canonicalPayload().toList(), bad.canonicalPayload().toList())
    }

    @Test fun rejectsOversizedArchives() {
        assertThrows(IllegalArgumentException::class.java) {
            ContentUpdatePackageArchive.read(ByteArray(10 * 1024 * 1024 + 1))
        }
    }
}
