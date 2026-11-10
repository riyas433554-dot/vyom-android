package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactorySystemRegistryTest {
    @Test fun registrationRequiresOwnerApprovalAndPinsRepositoryRevision() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val binding = binding()
            try {
                registry.register(binding, ownerConfirmed = false)
                fail("Expected owner approval to be required")
            } catch (_: IllegalStateException) { }
            assertTrue(root.listFiles().isNullOrEmpty())
            assertEquals(binding, registry.register(binding, ownerConfirmed = true))
            assertEquals(binding, registry.get("memory"))
        } finally { root.deleteRecursively() }
    }

    @Test fun pathAuthorizationAllowsOnlyExplicitRootsAndTheirChildren() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            registry.register(binding(), ownerConfirmed = true)
            assertTrue(registry.allowsPath("memory", "app/src/main/Memory.kt"))
            assertTrue(registry.allowsPath("memory", "app/src/main/nested/Memory.kt"))
            assertFalse(registry.allowsPath("memory", "app/src/test/MemoryTest.kt"))
            assertFalse(registry.allowsPath("memory", "../secrets.txt"))
            assertFalse(registry.allowsPath("memory", "/etc/passwd"))
            assertFalse(registry.allowsPath("memory", "app\\src\\main\\Memory.kt"))
            assertFalse(registry.allowsPath("unknown", "app/src/main/Memory.kt"))
        } finally { root.deleteRecursively() }
    }

    @Test fun existingSystemCannotSilentlyChangeRepositoryOrAllowedPaths() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            registry.register(binding(), ownerConfirmed = true)
            try {
                registry.register(binding().copy(allowedPaths = listOf("app/src")), ownerConfirmed = true)
                fail("Expected immutable binding")
            } catch (_: IllegalArgumentException) { }
        } finally { root.deleteRecursively() }
    }

    @Test fun eachSystemKeepsItsOwnRepositoryAndCannotBorrowAnotherBinding() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val memory = binding()
            val planner = binding().copy(
                systemId = "planner",
                repositoryUrl = "https://github.com/acme/planner",
                commitSha = "c".repeat(40),
                allowedPaths = listOf("planner/src")
            )
            registry.register(memory, ownerConfirmed = true)
            registry.register(planner, ownerConfirmed = true)

            assertEquals(memory, registry.get("memory"))
            assertEquals(planner, registry.get("planner"))
            assertTrue(registry.allowsPath("memory", "app/src/main/Memory.kt"))
            assertFalse(registry.allowsPath("memory", "planner/src/Planner.kt"))
            assertTrue(registry.allowsPath("planner", "planner/src/Planner.kt"))
            assertFalse(registry.allowsPath("planner", "app/src/main/Memory.kt"))
        } finally { root.deleteRecursively() }
    }

    @Test fun repositoryAndCommitMismatchesFailClosed() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            registry.register(binding(), ownerConfirmed = true)
            try {
                registry.requireAuthorizedBinding(
                    "memory", "app/src/main/Memory.kt",
                    repositoryUrl = "https://github.com/acme/other"
                )
                fail("Expected repository mismatch rejection")
            } catch (_: SecurityException) { }
            try {
                registry.requireAuthorizedBinding(
                    "memory", "app/src/main/Memory.kt",
                    commitSha = "c".repeat(40)
                )
                fail("Expected commit mismatch rejection")
            } catch (_: SecurityException) { }
        } finally { root.deleteRecursively() }
    }

    @Test fun allowedRootDoesNotMatchSiblingPrefix() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            registry.register(binding().copy(allowedPaths = listOf("app/src")), ownerConfirmed = true)
            assertTrue(registry.allowsPath("memory", "app/src/main/File.kt"))
            assertFalse(registry.allowsPath("memory", "app/src2/main/File.kt"))
        } finally { root.deleteRecursively() }
    }

    @Test fun invalidTraversalRootsAreRejected() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            for (path in listOf("../", "/absolute", "app/../secrets", "app\\src")) {
                try {
                    registry.register(binding().copy(allowedPaths = listOf(path)), ownerConfirmed = true)
                    fail("Expected unsafe path rejection: $path")
                } catch (_: IllegalArgumentException) { }
            }
        } finally { root.deleteRecursively() }
    }


    @Test fun explicitMigrationRequiresOwnerApprovalAndExactPreviousBinding() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val original = binding()
            val replacement = original.copy(
                commitSha = "b".repeat(40),
                allowedPaths = listOf("app/src/main", "docs")
            )
            registry.register(original, ownerConfirmed = true)

            try {
                registry.migrate(replacement, expectedExisting = original, ownerConfirmed = false)
                fail("Expected explicit owner approval")
            } catch (_: IllegalStateException) { }
            assertEquals(original, registry.get("memory"))

            assertEquals(
                replacement,
                registry.migrate(replacement, expectedExisting = original, ownerConfirmed = true)
            )
            assertEquals(replacement, registry.get("memory"))
        } finally { root.deleteRecursively() }
    }

    @Test fun migrationRejectsStalePreviousBindingAndCannotCreateUnregisteredSystem() {
        val root = createTempDir(prefix = "vyom-system-registry-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val original = binding()
            registry.register(original, ownerConfirmed = true)
            val replacement = original.copy(commitSha = "b".repeat(40))

            try {
                registry.migrate(
                    replacement,
                    expectedExisting = original.copy(commitSha = "c".repeat(40)),
                    ownerConfirmed = true
                )
                fail("Expected stale previous binding rejection")
            } catch (_: SecurityException) { }
            assertEquals(original, registry.get("memory"))

            try {
                registry.migrate(
                    replacement.copy(systemId = "planner"),
                    expectedExisting = original.copy(systemId = "planner"),
                    ownerConfirmed = true
                )
                fail("Expected migration to require an existing binding")
            } catch (_: SecurityException) { }
            assertNull(registry.get("planner"))
        } finally { root.deleteRecursively() }
    }

    private fun binding() = VyomFactorySystemRegistry.Binding(
        systemId = "memory",
        repositoryUrl = "https://github.com/riyas433554-dot/vyom-android",
        commitSha = "a".repeat(40),
        allowedPaths = listOf("app/src/main"),
        registeredAtEpochMs = 1_000L
    )
}
