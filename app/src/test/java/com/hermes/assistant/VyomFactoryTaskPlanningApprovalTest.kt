package com.hermes.assistant

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class VyomFactoryTaskPlanningApprovalTest {
    @Test fun ownerStartPersistsPlanningApprovalInCanonicalIssueMetadata() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"number":42,"title":"[VYOM FACTORY][P2] Fix repository build","state":"open","html_url":"https://github.com/owner/repo/issues/42"}"""
            ))
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":1}"""))
            val registry = registry()
            val client = VyomFactoryGitHubClient(
                "test-github-token-for-unit-tests-only",
                registry,
                apiBaseUrl = server.url("/").toString().trimEnd('/')
            )
            val current = task(planningAllowed = false)

            val updated = client.updateTask(
                current,
                newStatus = "in_progress",
                newLanePlanningAllowed = true
            )

            assertTrue(updated.metadata.lanePlanningAllowed)
            val patch = server.takeRequest()
            assertEquals("PATCH", patch.method)
            val body = JSONObject(patch.body.readUtf8()).getString("body")
            val metadata = VyomFactoryStatus.readMetadata(body)!!
            assertTrue(metadata.lanePlanningAllowed)
            assertTrue(metadata.events.contains("planning:owner_approved"))
        } finally {
            server.shutdown()
        }
    }

    @Test fun planningApprovalCannotBeGrantedWithoutStartingTheTask() {
        val server = MockWebServer()
        server.start()
        try {
            val client = VyomFactoryGitHubClient(
                "test-github-token-for-unit-tests-only",
                registry(),
                apiBaseUrl = server.url("/").toString().trimEnd('/')
            )

            val error = runCatching {
                client.updateTask(task(false), newStatus = "paused", newLanePlanningAllowed = true)
            }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    private fun registry(): VyomFactorySystemRegistry {
        val root = File(System.getProperty("java.io.tmpdir"), "factory-task-registry-${UUID.randomUUID()}")
        val registry = VyomFactorySystemRegistry(root)
        registry.register(
            VyomFactorySystemRegistry.Binding(
                VyomFactoryGitHubClient.SYSTEM_ID,
                "https://github.com/owner/repo",
                "b".repeat(40),
                listOf(VyomFactoryGitHubClient.ISSUES_PATH),
                1L
            ),
            ownerConfirmed = true
        )
        return registry
    }

    private fun task(planningAllowed: Boolean): VyomFactoryGitHubClient.Task {
        val sourceId = "owner-start-42"
        val metadata = VyomFactoryStatus.Metadata(
            "P2", "open", "chatgpt", sourceId,
            VyomFactoryStatus.idempotencyKey("chatgpt", sourceId)!!,
            listOf("created"), VyomFactoryLaneRouter.Lane.CUSTOM, planningAllowed
        )
        return VyomFactoryGitHubClient.Task(
            42, "[VYOM FACTORY][P2] Fix repository build",
            VyomFactoryStatus.withMetadata("Repository Gradle build fails.", metadata),
            "open", "https://github.com/owner/repo/issues/42",
            "2026-11-11T10:00:00Z", metadata, "2026-11-10T10:00:00Z"
        )
    }
}
