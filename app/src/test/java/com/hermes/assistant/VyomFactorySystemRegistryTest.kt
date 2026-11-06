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

    private fun binding() = VyomFactorySystemRegistry.Binding(
        systemId = "memory",
        repositoryUrl = "https://github.com/riyas433554-dot/vyom-android",
        commitSha = "a".repeat(40),
        allowedPaths = listOf("app/src/main"),
        registeredAtEpochMs = 1_000L
    )
}
