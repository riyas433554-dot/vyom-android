package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VyomFactoryRepositoryRegistryTest {
    @Test fun registrationRequiresExplicitOwnerConfirmation() {
        val root = createTempDir(prefix = "vyom-repo-registry-")
        try {
            val registry = VyomFactoryRepositoryRegistry(root)
            assertRejected { registry.register(preview(), ownerConfirmed = false) }
            assertTrue(root.listFiles().isNullOrEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun registrationStoresPinnedMetadataButNeverExecutionPermission() {
        val root = createTempDir(prefix = "vyom-repo-registry-")
        try {
            val registry = VyomFactoryRepositoryRegistry(root)
            val entry = registry.register(preview(), ownerConfirmed = true)

            assertEquals("https://github.com/openai/example", entry.canonicalUrl)
            assertEquals("a".repeat(40), entry.commitSha)
            assertEquals(VyomFactoryRepositoryIntake.Kind.SOURCE_CODE, entry.kind)
            assertEquals(VyomFactoryRepositoryIntake.Decision.READY_FOR_REVIEW, entry.decision)
            assertFalse(entry.sourceExecutionAllowed)
            assertEquals(listOf(entry), registry.list())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun registeringSameImmutableRevisionIsIdempotent() {
        val root = createTempDir(prefix = "vyom-repo-registry-")
        try {
            val registry = VyomFactoryRepositoryRegistry(root)
            val first = registry.register(preview(), ownerConfirmed = true)
            val second = registry.register(preview(), ownerConfirmed = true)

            assertEquals(first, second)
            assertEquals(1, registry.list().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun aDifferentRevisionCannotSilentlyReplaceRegisteredSource() {
        val root = createTempDir(prefix = "vyom-repo-registry-")
        try {
            val registry = VyomFactoryRepositoryRegistry(root)
            registry.register(preview(), ownerConfirmed = true)

            assertRejected {
                registry.register(preview(commit = "b".repeat(40)), ownerConfirmed = true)
            }
            assertEquals("a".repeat(40), registry.list().single().commitSha)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun blockedRepositoryCannotBeRegistered() {
        val root = createTempDir(prefix = "vyom-repo-registry-")
        try {
            val blocked = preview().copy(
                assessment = preview().assessment.copy(
                    decision = VyomFactoryRepositoryIntake.Decision.BLOCKED,
                    readOnlyInspectionAllowed = false,
                    reasons = listOf("repository_archived")
                )
            )
            assertRejected {
                VyomFactoryRepositoryRegistry(root).register(blocked, ownerConfirmed = true)
            }
            assertTrue(root.listFiles().isNullOrEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun preview(commit: String = "a".repeat(40)): VyomFactoryRepositoryClient.Preview {
        val source = VyomFactoryRepositoryIntake.SourceRef(
            "openai", "example", "https://github.com/openai/example"
        )
        val snapshot = VyomFactoryRepositoryIntake.Snapshot(
            source, "main", commit, "MIT", false, false,
            listOf("README.md", "package.json")
        )
        return VyomFactoryRepositoryClient.Preview(
            snapshot,
            VyomFactoryRepositoryIntake.assess(snapshot),
            "Example source",
            42,
            123,
            "2026-11-05T00:00:00Z"
        )
    }

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected registry to reject the operation")
        } catch (_: Exception) {
            // expected
        }
    }
}
