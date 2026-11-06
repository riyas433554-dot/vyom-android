package com.hermes.assistant

import android.util.Base64
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryRepositoryClientSystemAccessTest {
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
                JSONObject()
                    .put("type", "file")
                    .put("path", "app/src/main/Memory.kt")
                    .put("encoding", "base64")
                    .put("size", content.size)
                    .put("sha", "blob-sha")
                    .put("content", android.util.Base64.encodeToString(content, Base64.NO_WRAP))
                    .toString()
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
