package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryWorkerBindingPolicyTest {
    @Test fun createsOneWorkerBindingPinnedToExactCommitAndReadmeOnly() {
        val binding = VyomFactoryWorkerBindingPolicy.createBinding(
            "https://github.com/example/test-repo", "a".repeat(40), 1_000L
        )
        assertEquals("worker", binding.systemId)
        assertEquals("https://github.com/example/test-repo", binding.repositoryUrl)
        assertEquals("a".repeat(40), binding.commitSha)
        assertEquals(listOf("README.md"), binding.allowedPaths)
        assertTrue(VyomFactoryWorkerBindingPolicy.allowsPath("README.md"))
        assertFalse(VyomFactoryWorkerBindingPolicy.allowsPath("README.md/child"))
    }

    @Test fun rejectsMalformedOrNonCanonicalRepositoryAndWrongSha() {
        val validUrl = "https://github.com/example/test-repo"
        for (sha in listOf("a".repeat(39), "a".repeat(41), "g".repeat(40), "A".repeat(40), "")) {
            assertThrows(IllegalArgumentException::class.java) {
                VyomFactoryWorkerBindingPolicy.createBinding(validUrl, sha, 1_000L)
            }
        }
        for (url in listOf(
            "https://github.com/example/test-repo/", "https://github.com/example/test-repo.git",
            "https://github.com/example/test-repo/tree/main", "http://github.com/example/test-repo",
            "https://user@github.com/example/test-repo"
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                VyomFactoryWorkerBindingPolicy.createBinding(url, "a".repeat(40), 1_000L)
            }
        }
    }

    @Test fun rejectsTraversalAndEveryNonReadmePath() {
        for (path in listOf("../README.md", "/README.md", "docs/README.md", "README.MD", "README.md/child", "app/src/main/Secret.kt")) {
            assertFalse(path, VyomFactoryWorkerBindingPolicy.allowsPath(path))
            assertThrows(SecurityException::class.java) { VyomFactoryWorkerBindingPolicy.requireAllowedPath(path) }
        }
    }

    @Test fun ownerConfirmationIsRequiredAndNoBindingIsWrittenBeforeApproval() {
        val root = createTempDir(prefix = "worker-binding-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val proposed = VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/example/test-repo", "b".repeat(40), 1_000L
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
                "https://github.com/example/test-repo", "c".repeat(40), 1_000L
            )
            val saved = VyomFactoryWorkerBindingPolicy.register(registry, proposed, ownerConfirmed = true)
            assertEquals(proposed, saved)
            assertEquals(saved, VyomFactoryWorkerBindingPolicy.register(
                registry, proposed.copy(registeredAtEpochMs = 2_000L), ownerConfirmed = true
            ))
            assertEquals(saved, registry.get("worker"))
        } finally { root.deleteRecursively() }
    }

    @Test fun refusesToReplaceAnExistingWorkerBinding() {
        val root = createTempDir(prefix = "worker-binding-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val original = VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/example/first-test-repo", "d".repeat(40), 1_000L
            )
            VyomFactoryWorkerBindingPolicy.register(registry, original, ownerConfirmed = true)
            val replacement = VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/example/second-test-repo", "e".repeat(40), 2_000L
            )
            assertThrows(SecurityException::class.java) {
                VyomFactoryWorkerBindingPolicy.register(registry, replacement, ownerConfirmed = true)
            }
            assertEquals(original, registry.get("worker"))
        } finally { root.deleteRecursively() }
    }
}
