package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryWorkerBindingPolicyTest {
    @Test fun createsOneWorkerBindingPinnedToExactCommitAndSafeKotlinRoots() {
        val binding = VyomFactoryWorkerBindingPolicy.createBinding(
            "https://github.com/riyas433554-dot/vyom-android", "a".repeat(40), 1_000L
        )
        assertEquals("worker", binding.systemId)
        assertEquals(VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL, binding.repositoryUrl)
        assertEquals("a".repeat(40), binding.commitSha)
        assertEquals(VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS, binding.allowedPaths)
        assertTrue(VyomFactoryWorkerBindingPolicy.allowsPath("app/src/main/java/com/hermes/assistant/ReplyFormatter.kt"))
        assertTrue(VyomFactoryWorkerBindingPolicy.allowsPath("app/src/test/java/com/hermes/assistant/ReplyFormatterTest.kt"))
        assertFalse(VyomFactoryWorkerBindingPolicy.allowsPath("README.md"))
        assertFalse(VyomFactoryWorkerBindingPolicy.allowsPath("README.md/child"))
    }

    @Test fun bindsTrustedAppRepositoryToSafeKotlinSourceAndTestRoots() {
        val binding = VyomFactoryWorkerBindingPolicy.createBinding(
            "https://github.com/riyas433554-dot/vyom-android", "a".repeat(40), 1_000L
        )
        assertEquals(
            listOf("app/src/main/java/com/hermes/assistant", "app/src/test/java/com/hermes/assistant"),
            binding.allowedPaths
        )
        assertTrue(VyomFactoryWorkerBindingPolicy.allowsPath(
            "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt"
        ))
        assertTrue(VyomFactoryWorkerBindingPolicy.allowsPath(
            "app/src/test/java/com/hermes/assistant/ReplyFormatterTest.kt"
        ))
        for (path in listOf(
            "README.md",
            "app/src/main/java/com/hermes/assistant/MainActivity.kt",
            "app/src/main/java/com/hermes/assistant/VyomFactoryWorkerPoolDispatcher.kt",
            "app/build.gradle.kts"
        )) assertFalse(path, VyomFactoryWorkerBindingPolicy.allowsPath(path))
    }

    @Test fun refusesRepositoriesOutsideTheConfiguredExecutorTarget() {
        assertThrows(IllegalArgumentException::class.java) {
            VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/example/test-repo", "a".repeat(40), 1_000L
            )
        }
    }

    @Test fun rejectsMalformedOrNonCanonicalRepositoryAndWrongSha() {
        val validUrl = "https://github.com/riyas433554-dot/vyom-android"
        for (sha in listOf("a".repeat(39), "a".repeat(41), "g".repeat(40), "A".repeat(40), "")) {
            assertThrows(IllegalArgumentException::class.java) {
                VyomFactoryWorkerBindingPolicy.createBinding(validUrl, sha, 1_000L)
            }
        }
        for (url in listOf(
            "https://github.com/riyas433554-dot/vyom-android/", "https://github.com/riyas433554-dot/vyom-android.git",
            "https://github.com/riyas433554-dot/vyom-android/tree/main", "http://github.com/example/test-repo",
            "https://user@github.com/example/test-repo"
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                VyomFactoryWorkerBindingPolicy.createBinding(url, "a".repeat(40), 1_000L)
            }
        }
    }

    @Test fun rejectsTraversalAndEveryPathOutsideSafeKotlinRoots() {
        for (path in listOf("../README.md", "/README.md", "docs/README.md", "README.MD", "README.md/child",
            "app/src/main/java/com/hermes/assistant/MainActivity.kt",
            "app/src/main/java/com/hermes/assistant/VyomFactoryWorkerPoolDispatcher.kt",
            "app/build.gradle.kts")) {
            assertFalse(path, VyomFactoryWorkerBindingPolicy.allowsPath(path))
            assertThrows(SecurityException::class.java) { VyomFactoryWorkerBindingPolicy.requireAllowedPath(path) }
        }
    }

    @Test fun ownerConfirmationIsRequiredAndNoBindingIsWrittenBeforeApproval() {
        val root = createTempDir(prefix = "worker-binding-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val proposed = VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/riyas433554-dot/vyom-android", "b".repeat(40), 1_000L
            )
            assertThrows(IllegalStateException::class.java) {
                VyomFactoryWorkerBindingPolicy.register(registry, proposed, ownerConfirmed = false)
            }
            assertNull(registry.get("worker"))
            assertTrue(root.listFiles().isNullOrEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun persistsOwnerApprovedBindingAndReusesSameTargetWithoutOverwriting() {
        val root = createTempDir(prefix = "worker-binding-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val proposed = VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/riyas433554-dot/vyom-android", "c".repeat(40), 1_000L
            )
            val saved = VyomFactoryWorkerBindingPolicy.register(registry, proposed, ownerConfirmed = true)
            assertEquals(proposed, saved)
            assertEquals(saved, VyomFactoryWorkerBindingPolicy.register(
                registry, proposed.copy(registeredAtEpochMs = 2_000L), ownerConfirmed = true
            ))
            assertEquals(saved, registry.get("worker"))
        } finally { root.deleteRecursively() }
    }

    @Test fun explicitMigrationRequiresOwnerConfirmationAndCompareAndSwap() {
        val root = createTempDir(prefix = "worker-binding-migration-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val original = VyomFactorySystemRegistry.Binding(
                "worker", VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL,
                "d".repeat(40), listOf("README.md"), 1_000L
            )
            registry.register(original, ownerConfirmed = true)
            val replacement = VyomFactoryWorkerBindingPolicy.createBinding(
                VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL, "e".repeat(40), 2_000L
            )
            assertThrows(IllegalStateException::class.java) {
                VyomFactoryWorkerBindingPolicy.migrate(registry, original, replacement, ownerConfirmed = false)
            }
            assertEquals(original, registry.get("worker"))
            val saved = VyomFactoryWorkerBindingPolicy.migrate(
                registry, original, replacement, ownerConfirmed = true
            )
            assertEquals(replacement, saved)
            assertThrows(SecurityException::class.java) {
                VyomFactoryWorkerBindingPolicy.migrate(registry, original, replacement, ownerConfirmed = true)
            }
            assertEquals(replacement, registry.get("worker"))
        } finally { root.deleteRecursively() }
    }

    @Test fun refusesToReplaceAnExistingWorkerBinding() {
        val root = createTempDir(prefix = "worker-binding-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val original = VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/riyas433554-dot/vyom-android", "d".repeat(40), 1_000L
            )
            VyomFactoryWorkerBindingPolicy.register(registry, original, ownerConfirmed = true)
            val replacement = VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/riyas433554-dot/vyom-android", "e".repeat(40), 2_000L
            )
            assertThrows(SecurityException::class.java) {
                VyomFactoryWorkerBindingPolicy.register(registry, replacement, ownerConfirmed = true)
            }
            assertEquals(original, registry.get("worker"))
        } finally { root.deleteRecursively() }
    }
}
