package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryRepositoryClientSystemAccessTest {
    @Test fun branchHeadFetchUsesTheRequestedGitHubBranchAndReturnsItsExactCommit() {
        val sha = "c".repeat(40)
        var requested = ""
        val client = VyomFactoryRepositoryClient(getJson = { path ->
            requested = path
            """{"name":"feature/real-worker-model","commit":{"sha":"$sha"}}"""
        })

        assertEquals(sha, client.readBranchHead(
            "https://github.com/riyas433554-dot/vyom-android",
            "feature/real-worker-model"
        ))
        assertEquals(
            "/repos/riyas433554-dot/vyom-android/branches/feature%2Freal-worker-model",
            requested
        )
    }

    @Test fun branchHeadRejectsMalformedGitHubCommitSha() {
        val client = VyomFactoryRepositoryClient(getJson = {
            """{"commit":{"sha":"not-a-commit"}}"""
        })
        try {
            client.readBranchHead("https://github.com/acme/repo", "feature/work")
            fail("Expected malformed branch SHA rejection")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun boundReadUsesOnlyRegisteredRepositoryAndCommit() {
        val root = createTempDir(prefix = "vyom-system-read-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            val sha = "b".repeat(40)
            registry.register(
                VyomFactorySystemRegistry.Binding(
                    systemId = "memory",
                    repositoryUrl = "https://github.com/acme/memory",
                    commitSha = sha,
                    allowedPaths = listOf("app/src/main"),
                    registeredAtEpochMs = 1L
                ),
                ownerConfirmed = true
            )
            val content = "safe source".toByteArray()
            var requested = ""
            val client = VyomFactoryRepositoryClient(getJson = { path ->
                requested = path
                """{"type":"file","path":"app/src/main/Memory.kt","encoding":"base64","size":${content.size},"sha":"blob-sha","content":"${java.util.Base64.getEncoder().encodeToString(content)}"}"""
            })
            val result = client.readBoundSystemFile("memory", "app/src/main/Memory.kt", registry)
            assertEquals("/repos/acme/memory/contents/app/src/main/Memory.kt?ref=$sha", requested)
            assertEquals("safe source", result.content)
            assertEquals("app/src/main/Memory.kt", result.path)
        } finally { root.deleteRecursively() }
    }

    @Test fun deniedPathFailsBeforeNetworkRequest() {
        val root = createTempDir(prefix = "vyom-system-read-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            registry.register(
                VyomFactorySystemRegistry.Binding(
                    "memory", "https://github.com/acme/memory", "b".repeat(40),
                    listOf("app/src/main"), 1L
                ),
                ownerConfirmed = true
            )
            var called = false
            val client = VyomFactoryRepositoryClient(getJson = {
                called = true
                error("Network must not be called for a denied path")
            })
            try {
                client.readBoundSystemFile("memory", "app/src/test/SecretTest.kt", registry)
                fail("Expected path authorization failure")
            } catch (_: SecurityException) { }
            assertFalse(called)
        } finally { root.deleteRecursively() }
    }

    @Test fun unknownSystemCannotReadRepositoryFiles() {
        val root = createTempDir(prefix = "vyom-system-read-")
        try {
            val registry = VyomFactorySystemRegistry(root)
            var called = false
            val client = VyomFactoryRepositoryClient(getJson = {
                called = true
                error("Network must not be called for an unknown system")
            })
            try {
                client.readBoundSystemFile("unknown", "README.md", registry)
                fail("Expected unknown system rejection")
            } catch (_: SecurityException) { }
            assertFalse(called)
        } finally { root.deleteRecursively() }
    }
}
