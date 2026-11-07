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

    @Test fun differentRevisionsAreStoredAsSeparateImmutableSnapshots() {
        val root = createTempDir(prefix = "vyom-repo-registry-")
        try {
            val registry = VyomFactoryRepositoryRegistry(root)
            val first = registry.register(preview(), ownerConfirmed = true)
            val second = registry.register(preview(commit = "b".repeat(40)), ownerConfirmed = true)

            assertEquals("a".repeat(40), first.commitSha)
            assertEquals("b".repeat(40), second.commitSha)
            assertEquals(2, registry.list().size)
            assertTrue(registry.list().any { it == first })
            assertTrue(registry.list().any { it == second })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun sourceRevisionMigrationRequiresOwnerConfirmationAndCompareAndSwap() {
        val root = createTempDir(prefix = "vyom-repo-registry-")
        try {
            val registry = VyomFactoryRepositoryRegistry(root)
            val original = registry.register(preview(), ownerConfirmed = true)
            val updatedPreview = preview(commit = "b".repeat(40))

            assertRejected {
                registry.migrate(updatedPreview, expectedExisting = original, ownerConfirmed = false)
            }
            assertEquals(original, registry.list().single())

            val migrated = registry.migrate(
                updatedPreview,
                expectedExisting = original,
                ownerConfirmed = true
            )
            assertEquals("b".repeat(40), migrated.commitSha)
            assertEquals(listOf(migrated), registry.list())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun sourceRevisionMigrationRejectsStaleSnapshotAndDifferentRepository() {
        val root = createTempDir(prefix = "vyom-repo-registry-")
        try {
            val registry = VyomFactoryRepositoryRegistry(root)
            val original = registry.register(preview(), ownerConfirmed = true)
            assertRejected {
                registry.migrate(
                    preview(commit = "b".repeat(40)),
                    expectedExisting = original.copy(commitSha = "c".repeat(40)),
                    ownerConfirmed = true
                )
            }
            val otherRepository = preview(commit = "b".repeat(40)).copy(
                snapshot = preview().snapshot.copy(
                    source = VyomFactoryRepositoryIntake.SourceRef(
                        "acme", "other", "https://github.com/acme/other"
                    ),
                    commitSha = "b".repeat(40)
                )
            )
            assertRejected {
                registry.migrate(otherRepository, expectedExisting = original, ownerConfirmed = true)
            }
            assertEquals(original, registry.list().single())
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
