package com.hermes.assistant

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun skillDocumentMustBeBoundedTextWithAHeading() {
        VyomFactorySkillPolicy.validateDocument("# Example Skill\n\nSafe inert instructions.\n")
        assertRejected { VyomFactorySkillPolicy.validateDocument("No heading here") }
        assertRejected { VyomFactorySkillPolicy.validateDocument("# Binary\n\u0000") }
        assertRejected {
            VyomFactorySkillPolicy.validateDocument("# Too large\n" + "x".repeat(VyomFactorySkillPolicy.MAX_DOCUMENT_BYTES))
        }
    }

    @Test fun installationRequiresExplicitOwnerConfirmation() {
        val root = createTempDir(prefix = "vyom-skill-test-")
        try {
            val store = VyomFactorySkillStore(root)
            assertRejected { store.install(preview(), ownerConfirmed = false) }
            assertFalse(File(root, "owner_example/SKILL.md").exists())
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
        sourceUrl = "https://github.com/owner/example/blob/main/SKILL.md"
    )

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected validation to reject the input")
        } catch (_: IllegalArgumentException) {
            // expected
        } catch (_: IllegalStateException) {
            // expected
        }
    }
}
