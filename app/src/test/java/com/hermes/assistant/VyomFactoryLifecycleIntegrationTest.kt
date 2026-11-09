package com.hermes.assistant

import android.content.SharedPreferences
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class VyomFactoryLifecycleIntegrationTest {
    @Test fun startClaimRunRecordProbePassAndCompleteUsesTheSameCanonicalIssue() {
        val server = MockWebServer()
        server.start()
        try {
            val issueNumber = 42
            val probe = "Open Vyom and confirm the requested reviewed change is visible"
            val evidence = "The requested change is visible on the device after refresh"
            val executionMarker =
                """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"repository_change","operation":"reviewed_patch","request":"Apply the reviewed Factory change","arguments":{}} -->"""
            val description = "Factory lifecycle integration test\n- **Verification probe:** $probe\n\nAcceptance criteria:\n- [x] Lifecycle integration succeeds\n\n$executionMarker"
            val openMetadata = metadata("open", listOf("created"))
            val startedMetadata = openMetadata.copy(
                status = "in_progress",
                events = openMetadata.events + "updated:in_progress:P2"
            )
            val openBody = VyomFactoryStatus.withMetadata(description, openMetadata)
            val startedBody = VyomFactoryStatus.withMetadata(description, startedMetadata)

            // START persists the in-progress state before the worker is allowed to claim.
            server.enqueue(json(issue(issueNumber, startedMetadata, startedBody, "open").toString()))
            server.enqueue(json("""{"id":1}"""))
            val registryRoot = tempDir()
            val registry = VyomFactorySystemRegistry(registryRoot)
            registry.register(
                VyomFactorySystemRegistry.Binding(
                    systemId = VyomFactoryGitHubClient.SYSTEM_ID,
                    repositoryUrl = "https://github.com/test/repo",
                    commitSha = "b".repeat(40),
                    allowedPaths = listOf(VyomFactoryGitHubClient.ISSUES_PATH),
                    registeredAtEpochMs = 1_000L
                ),
                ownerConfirmed = true
            )
            val client = VyomFactoryGitHubClient(
                token = "test-token",
                registry = registry,
                apiBaseUrl = server.url("/").toString().trimEnd('/')
            )
            val openTask = task(issueNumber, openBody, "open", openMetadata)
            val started = client.updateTask(openTask, newStatus = "in_progress")
            assertEquals("in_progress", started.metadata.status)

            // The real worker session refreshes the GitHub registry, reconciles the inbox,
            // and leases the same issue before the execution runner re-fetches it.
            server.enqueue(json(JSONArray().put(issue(issueNumber, startedMetadata, startedBody, "open")).toString()))
            server.enqueue(json(JSONArray().put(issue(issueNumber, startedMetadata, startedBody, "open")).toString()))
            val prefs = MemoryPrefs()
            val taskStore = VyomFactoryTaskStore(prefs)
            val leaseStore = VyomFactoryWorkerLeaseStore(prefs)
            val session = VyomFactoryWorkerSession(
                taskStore,
                VyomFactoryWorkerClaimCoordinator(taskStore, leaseStore),
                ownerId = "factory-lifecycle-test",
                fetchRemoteTasks = { client.listTasks() },
                now = { 10_000L },
                leaseDurationMs = 60_000L
            )
            val adapter = object : VyomFactoryExecutionAdapter {
                override val target = VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE
                override val isIdempotent = false
                override fun isVerifiedResult(result: String) = result == "Reviewed change applied and independently checked"
                override fun run(
                    envelope: VyomFactoryExecutionEnvelope,
                    control: VyomFactoryExecutionControl
                ): String {
                    assertTrue(envelope.isExecutable)
                    assertTrue(!control.shouldStop())
                    return "Reviewed change applied and independently checked"
                }
            }
            val runner = VyomFactoryExecutionRunner(
                claimNext = { session.claimNext() },
                isClaimCurrent = { session.isCurrent(it) },
                releaseClaim = { session.release(it) },
                fetchFreshTask = { number -> client.listTasks().firstOrNull { it.number == number } },
                journal = VyomFactoryExecutionJournal(prefs, leaseStore),
                adapters = VyomFactoryExecutionAdapterRegistry(listOf(adapter)),
                retrySchedules = VyomFactoryRetryScheduleStore(MemoryPrefs()),
                now = { 10_000L },
                newAttemptId = { "lifecycle-attempt-1" }
            )

            val outcome = runner.runNext()
            assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
            assertEquals(issueNumber, (outcome as VyomFactoryExecutionRunner.Outcome.Succeeded).issueNumber)

            // The production dialog records a worker result as an issue event, not as DONE.
            server.enqueue(json("""{"id":2}"""))
            client.addEvent(issueNumber, "Worker verified the adapter postcondition; owner probe is still required.")

            // Owner verification is tied to the exact currently assigned probe.
            server.enqueue(json(JSONArray().put(issue(issueNumber, startedMetadata, startedBody, "open")).toString()))
            val freshForVerification = client.listTasks().single()
            assertThrows(IllegalStateException::class.java) {
                client.updateTask(freshForVerification, newStatus = "completed")
            }

            val passEvent = VyomFactoryVerificationPolicy.resultEvent(probe, evidence, true)
            val verifiedMetadata = startedMetadata.copy(
                events = startedMetadata.events + "updated:in_progress:P2" + passEvent
            )
            val verifiedDescription = VyomFactoryVerificationPolicy.appendEvidence(
                description, probe, evidence, passed = true
            )
            val verifiedBody = VyomFactoryStatus.withMetadata(verifiedDescription, verifiedMetadata)
            server.enqueue(json(issue(issueNumber, verifiedMetadata, verifiedBody, "open").toString()))
            server.enqueue(json("""{"id":3}"""))
            val verified = client.recordVerificationResult(freshForVerification, passed = true, evidence = evidence)
            assertTrue(VyomFactoryVerificationPolicy.canComplete(
                verified.body.substringBefore("<!-- VYOM_FACTORY_META:").trim(),
                verified.metadata.events
            ))

            // DONE closes only after the same issue carries PASS for the exact current probe.
            val completedMetadata = verifiedMetadata.copy(
                status = "completed",
                events = verifiedMetadata.events + "updated:completed:P2"
            )
            val completedBody = VyomFactoryStatus.withMetadata(verifiedDescription, completedMetadata)
            server.enqueue(json(issue(issueNumber, completedMetadata, completedBody, "closed").toString()))
            server.enqueue(json("""{"id":4}"""))
            val completed = client.updateTask(verified, newStatus = "completed")
            assertEquals(issueNumber, completed.number)
            assertEquals("completed", completed.metadata.status)
            assertEquals("closed", completed.state)
            assertTrue(VyomFactoryVerificationPolicy.canComplete(
                completed.body.substringBefore("<!-- VYOM_FACTORY_META:").trim(),
                completed.metadata.events
            ))
        } finally {
            server.shutdown()
        }
    }

    private fun tempDir(): File = File.createTempFile("factory-lifecycle-registry", ".tmp").apply {
        delete()
        mkdirs()
    }

    private fun metadata(status: String, events: List<String>) = VyomFactoryStatus.Metadata(
        priority = "P2",
        status = status,
        source = "factory",
        sourceId = "lifecycle-42",
        idempotencyKey = "factory:lifecycle-42",
        events = events
    )

    private fun task(
        number: Int,
        body: String,
        state: String,
        metadata: VyomFactoryStatus.Metadata
    ) = VyomFactoryGitHubClient.Task(
        number = number,
        title = "[VYOM FACTORY][P2] Lifecycle",
        body = body,
        state = state,
        url = "https://github.com/test/repo/issues/$number",
        updatedAt = "2026-11-04T12:00:00Z",
        metadata = metadata
    )

    private fun issue(
        number: Int,
        metadata: VyomFactoryStatus.Metadata,
        body: String,
        state: String
    ) = JSONObject()
        .put("number", number)
        .put("title", "[VYOM FACTORY][P2] Lifecycle")
        .put("body", body)
        .put("state", state)
        .put("html_url", "https://github.com/test/repo/issues/$number")
        .put("updated_at", "2026-11-04T12:00:00Z")

    private fun json(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { updates[key] = value }
            override fun remove(key: String): SharedPreferences.Editor = apply { removals += key }
            override fun clear(): SharedPreferences.Editor = apply { removals += values.keys }
            override fun commit(): Boolean {
                removals.forEach(values::remove)
                updates.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
                return true
            }
            override fun apply() { commit() }
        }
    }
}
