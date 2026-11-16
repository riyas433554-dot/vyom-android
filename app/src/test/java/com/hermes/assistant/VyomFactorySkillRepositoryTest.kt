package com.hermes.assistant

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class VyomFactorySkillRepositoryTest {
    @Test fun repositoryAndLicensePolicyRejectUnsafeOrUnknownSources() {
        assertTrue(VyomFactorySkillPolicy.isValidRepository("openai/example-skill"))
        assertFalse(VyomFactorySkillPolicy.isValidRepository("../private/repo"))
        assertFalse(VyomFactorySkillPolicy.isValidRepository("owner/repo/extra"))
        assertTrue(VyomFactorySkillPolicy.isAllowedLicense("MIT"))
        assertTrue(VyomFactorySkillPolicy.isAllowedLicense("Apache-2.0"))
        assertFalse(VyomFactorySkillPolicy.isAllowedLicense("NOASSERTION"))
    }

    @Test fun nestedSkillSelectionRequiresAnExactPathWhenAmbiguous() {
        val paths = listOf("agents/android/SKILL.md", "agents/kotlin/SKILL.md")
        assertEquals(null, VyomFactorySkillPolicy.selectSkillDocumentPath(paths))
        assertEquals("agents/kotlin/SKILL.md",
            VyomFactorySkillPolicy.selectSkillDocumentPath(paths, "agents/kotlin/SKILL.md"))
        assertEquals("SKILL.md",
            VyomFactorySkillPolicy.selectSkillDocumentPath(paths + "SKILL.md"))
        assertEquals(null,
            VyomFactorySkillPolicy.selectSkillDocumentPath(paths, "../outside/SKILL.md"))
    }

    @Test fun referencedSkillPathsRejectTraversalAndNonMarkdownFiles() {
        assertTrue(VyomFactorySkillPolicy.isValidReferencePath("references/testing.md"))
        assertFalse(VyomFactorySkillPolicy.isValidReferencePath("../secrets.md"))
        assertFalse(VyomFactorySkillPolicy.isValidReferencePath("/etc/passwd.md"))
        assertFalse(VyomFactorySkillPolicy.isValidReferencePath("references/secret.txt"))
    }

    @Test fun skillDocumentMustBeBoundedTextWithAHeading() {
        VyomFactorySkillPolicy.validateDocument("# Example Skill\n\nSafe inert instructions.\n")
        assertRejected { VyomFactorySkillPolicy.validateDocument("No heading here") }
        assertRejected { VyomFactorySkillPolicy.validateDocument("# Binary\n\u0000") }
        assertRejected {
            VyomFactorySkillPolicy.validateDocument("# Too large\n" + "x".repeat(VyomFactorySkillPolicy.MAX_DOCUMENT_BYTES))
        }
    }


    @Test fun acceptsExactlySixtyFourKiBAndRejectsOneByteOver() {
        val header = "# A\\n"
        val exact = header + "x".repeat(VyomFactorySkillPolicy.MAX_DOCUMENT_BYTES - header.toByteArray(Charsets.UTF_8).size)
        assertEquals(VyomFactorySkillPolicy.MAX_DOCUMENT_BYTES, exact.toByteArray(Charsets.UTF_8).size)
        VyomFactorySkillPolicy.validateDocument(exact)
        val oneByteOver = exact + "x"
        assertEquals(VyomFactorySkillPolicy.MAX_DOCUMENT_BYTES + 1, oneByteOver.toByteArray(Charsets.UTF_8).size)
        assertThrows(IllegalArgumentException::class.java) { VyomFactorySkillPolicy.validateDocument(oneByteOver) }
    }

    @Test fun invalidUtf8IsRejectedInsteadOfSilentlyReplaced() {
        assertRejected { VyomFactorySkillPolicy.decodeDocument(byteArrayOf(0x23, 0x20, 0xC3.toByte(), 0x28)) }
    }

    @Test fun installationRequiresExplicitOwnerConfirmation() {
        val root = createTempDir(prefix = "vyom-skill-test-")
        try {
            val store = VyomFactorySkillStore(root)
            assertRejected { store.install(preview(), ownerConfirmed = false) }
            assertFalse(root.listFiles()?.any { File(it, "SKILL.md").exists() } == true)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun reimportCannotSilentlyOverwriteAnInstalledSkill() {
        val root = createTempDir(prefix = "vyom-skill-no-overwrite-")
        try {
            val store = VyomFactorySkillStore(root)
            store.install(preview(), ownerConfirmed = true)
            assertRejected { store.install(preview().copy(commitSha = "b".repeat(40)), ownerConfirmed = true) }
            assertEquals("a".repeat(40),
                JSONObject(File(root, VyomFactorySkillPolicy.storageFolderName("owner/example") + "/source.json").readText()).getString("commit_sha"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun diagnosticsAuditInstalledSkillSourceAndDetectsContentTampering() {
        val root = createTempDir(prefix = "vyom-skill-audit-")
        try {
            val store = VyomFactorySkillStore(root)
            store.install(preview(), ownerConfirmed = true)
            val clean = store.auditInstalled()
            assertEquals(1, clean.total)
            assertEquals(1, clean.valid)
            assertEquals(0, clean.invalid)

            File(root, VyomFactorySkillPolicy.storageFolderName("owner/example") + "/SKILL.md").writeText("# Tampered\\n")
            val tampered = store.auditInstalled()
            assertEquals(1, tampered.total)
            assertEquals(0, tampered.valid)
            assertEquals(1, tampered.invalid)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun auditDetectsTamperedPinnedReferenceFiles() {
        val root = createTempDir(prefix = "vyom-skill-reference-audit-")
        try {
            val store = VyomFactorySkillStore(root)
            val installed = store.install(preview(), ownerConfirmed = true)
            val reference = File(installed.skillFile.parentFile, "references/references/testing.md")
            assertTrue(reference.isFile)
            assertEquals(1, store.auditInstalled().valid)
            reference.writeText("tampered reference")
            val audit = store.auditInstalled()
            assertEquals(0, audit.valid)
            assertEquals(1, audit.invalid)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun installWritesInertSkillAndVerifiesExactSourceRevisionAndHash() {
        val root = createTempDir(prefix = "vyom-skill-test-")
        try {
            val installed = VyomFactorySkillStore(root).install(preview(), ownerConfirmed = true)
            assertTrue(installed.skillFile.isFile)
            assertTrue(installed.skillFile.canonicalPath.startsWith(root.canonicalPath))
            assertEquals(VyomFactorySkillPolicy.sha256(installed.skillFile.readBytes()), installed.sha256)
            val metadata = JSONObject(File(installed.skillFile.parentFile, "source.json").readText())
            assertEquals("owner/example", metadata.getString("repository"))
            assertEquals("a".repeat(40), metadata.getString("commit_sha"))
            assertEquals(installed.sha256, metadata.getString("sha256"))
            assertEquals("inert-markdown", metadata.getString("content_type"))
            assertEquals("MIT", installed.license)
            val reference = File(installed.skillFile.parentFile, "references/references/testing.md")
            assertTrue(reference.isFile)
            val referenceHashes = metadata.getJSONObject("reference_hashes")
            assertEquals(
                VyomFactorySkillPolicy.sha256(reference.readBytes()),
                referenceHashes.getString("references/testing.md")
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private fun preview() = VyomFactorySkillRepositoryClient.Preview(
        repository = "owner/example",
        branch = "main",
        commitSha = "a".repeat(40),
        license = "MIT",
        skillMarkdown = "# Example Skill\n\nDo not execute this content.\n",
        sourceUrl = "https://github.com/owner/example/blob/main/SKILL.md",
        referenceFiles = mapOf("references/testing.md" to "Use focused unit tests and verify the failure first.")
    )

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected validation to reject the input")
        } catch (_: Exception) {
            // expected validation or strict UTF-8 decoding failure
        }
    }
}
