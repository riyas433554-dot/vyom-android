package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VyomFactoryRepositoryChangeExecutorTest {
    private val repository = "https://github.com/riyas433554-dot/vyom-android"
    private val baseSha = "a".repeat(40)
    private val commitSha = "b".repeat(40)

    private fun registry(repositoryUrl: String = repository): VyomFactorySystemRegistry {
        val root = File.createTempFile("vyom-executor-registry", ".tmp").apply {
            delete()
            mkdirs()
        }
        return VyomFactorySystemRegistry(root).also {
            it.register(
                VyomFactorySystemRegistry.Binding(
                    systemId = "factory-worker",
                    repositoryUrl = repositoryUrl,
                    commitSha = baseSha,
                    allowedPaths = listOf(
                        "app/src/main/java/com/hermes/assistant",
                        "app/src/test/java/com/hermes/assistant"
                    ),
                    registeredAtEpochMs = 1L
                ),
                ownerConfirmed = true
            )
        }
    }

    private fun plan() = VyomFactoryRepositoryChangePolicy.Plan(
        systemId = "factory-worker",
        issueNumber = 71,
        attemptId = "attempt-12345678",
        title = "Improve reply formatting",
        baseBranch = "feature/real-worker-model",
        baseCommitSha = baseSha,
        changes = listOf(
            VyomFactoryRepositoryChangePolicy.Change(
                "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
                "package com.hermes.assistant\nclass ReplyFormatter"
            ),
            VyomFactoryRepositoryChangePolicy.Change(
                "app/src/test/java/com/hermes/assistant/ReplyFormatterTest.kt",
                "package com.hermes.assistant\nclass ReplyFormatterTest"
            )
        ),
        ownerApproved = true
    )

    @Test fun createsOneAtomicCommitAndDraftPrOnlyAfterExactShaCiPasses() {
        val transport = RecordingTransport(baseSha, commitSha)
        val executor = executor(transport)

        val proposal = executor.execute(plan(), TOKEN, NeverStop, timeoutMillis = 1_000)

        assertEquals(commitSha, proposal.commitSha)
        assertEquals("vyom-worker-71-attempt-12345678", proposal.branch)
        assertEquals("$repository/pull/72", proposal.pullRequestUrl)
        val treeWrite = transport.calls.single { it.method == "POST" && it.path == "/git/trees" }
        val treeEntries = treeWrite.body!!.getJSONArray("tree")
        assertEquals(2, treeEntries.length())
        assertEquals("app/src/main/java/com/hermes/assistant/ReplyFormatter.kt", treeEntries.getJSONObject(0).getString("path"))
        assertTrue(transport.calls.any { it.method == "POST" && it.path == "/git/commits" })
        assertTrue(transport.calls.any { it.method == "POST" && it.path == "/git/refs" })
        assertTrue(transport.calls.any { it.method == "POST" && it.path == "/pulls" })
        assertFalse(transport.calls.any { it.method == "PUT" })
        assertFalse(transport.calls.any { it.path.contains("/merge") })
    }


    @Test fun refusesUntrustedRepositoryBeforeAnyNetworkRequest() {
        val transport = RecordingTransport(baseSha, commitSha)
        val error = runCatching {
            executor(transport, registry("https://github.com/example/project"))
                .execute(plan(), TOKEN, NeverStop, timeoutMillis = 1_000)
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
        assertTrue(transport.calls.isEmpty())
    }

    @Test fun staleRegisteredBaseStopsBeforeAnyRepositoryWrite() {
        val transport = RecordingTransport("c".repeat(40), commitSha)
        val error = runCatching {
            executor(transport).execute(plan(), TOKEN, NeverStop, timeoutMillis = 1_000)
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
        assertFalse(transport.calls.any { it.method == "POST" || it.method == "PUT" || it.method == "PATCH" })
    }

    @Test fun stopBeforeExecutionPerformsNoNetworkRequest() {
        val transport = RecordingTransport(baseSha, commitSha)
        val error = runCatching {
            executor(transport).execute(plan(), TOKEN, StopImmediately, timeoutMillis = 1_000)
        }.exceptionOrNull()

        assertTrue(error is VyomFactoryExecutionStoppedException)
        assertTrue(transport.calls.isEmpty())
    }

    @Test fun doesNotReportSuccessWhenRequiredExactShaCiFails() {
        val transport = RecordingTransport(baseSha, commitSha, ciConclusion = "failure")
        val error = runCatching {
            executor(transport).execute(plan(), TOKEN, NeverStop, timeoutMillis = 1_000)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("Vyom Android"))
        assertTrue(transport.calls.any { it.method == "POST" && it.path == "/pulls" })
    }

    @Test fun timesOutWhenExactShaCiNeverCompletes() {
        val clock = java.util.concurrent.atomic.AtomicLong(0L)
        val transport = RecordingTransport(baseSha, commitSha, ciAvailable = false)
        val timedExecutor = VyomFactoryRepositoryChangeExecutor(
            registry = registry(),
            transportFactory = { _, _ -> transport },
            nowMillis = { clock.get() },
            waitMillis = { clock.addAndGet(it) }
        )
        val error = runCatching {
            timedExecutor.execute(plan(), TOKEN, NeverStop, timeoutMillis = 1_000)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("Timed out waiting for exact-SHA"))
    }

    @Test fun rejectsPullRequestUrlOutsideRegisteredRepository() {
        val transport = RecordingTransport(baseSha, commitSha, pullUrl = "https://github.com/other/repo/pull/72")
        val error = runCatching {
            executor(transport).execute(plan(), TOKEN, NeverStop, timeoutMillis = 1_000)
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
    }

    private fun executor(
        transport: RecordingTransport,
        registry: VyomFactorySystemRegistry = registry()
    ) = VyomFactoryRepositoryChangeExecutor(
        registry = registry,
        transportFactory = { _, _ -> transport },
        nowMillis = { 10_000L },
        waitMillis = {}
    )

    private class RecordingTransport(
        private val baseSha: String,
        private val commitSha: String,
        private val ciConclusion: String = "success",
        private val pullUrl: String = "https://github.com/riyas433554-dot/vyom-android/pull/72",
        private val ciAvailable: Boolean = true
    ) : VyomFactoryRepositoryChangeExecutor.Transport {
        data class Call(val method: String, val path: String, val body: JSONObject?)
        val calls = mutableListOf<Call>()

        override fun request(method: String, path: String, body: JSONObject?): JSONObject {
            calls += Call(method, path, body?.let { JSONObject(it.toString()) })
            return when {
                method == "GET" && path == "/git/ref/heads/feature/real-worker-model" ->
                    JSONObject().put("object", JSONObject().put("sha", baseSha))
                method == "GET" && path.startsWith("/git/commits/") ->
                    JSONObject().put("tree", JSONObject().put("sha", "c".repeat(40)))
                method == "POST" && path == "/git/trees" ->
                    JSONObject().put("sha", "d".repeat(40))
                method == "POST" && path == "/git/commits" ->
                    JSONObject().put("sha", commitSha)
                method == "POST" && path == "/git/refs" ->
                    JSONObject().put("object", JSONObject().put("sha", commitSha))
                method == "GET" && path.startsWith("/git/ref/heads/vyom-worker-") ->
                    JSONObject().put("object", JSONObject().put("sha", commitSha))
                method == "POST" && path == "/pulls" ->
                    JSONObject().put("html_url", pullUrl)
                        .put("head", JSONObject().put("ref", "vyom-worker-71-attempt-12345678"))
                method == "GET" && path.startsWith("/actions/runs?head_sha=") -> {
                    val runs = if (ciAvailable) JSONArray()
                        .put(workflow("Vyom Android CI", ciConclusion, 2L))
                        .put(workflow("Vyom Android Debug", ciConclusion, 3L)) else JSONArray()
                    JSONObject().put("workflow_runs", runs)
                }
                method == "GET" && path.startsWith("/git/ref/heads/") ->
                    JSONObject().put("object", JSONObject().put("sha", baseSha))
                else -> error("Unexpected GitHub request: $method $path")
            }
        }

        private fun workflow(name: String, conclusion: String, id: Long) = JSONObject()
            .put("name", name)
            .put("head_sha", commitSha)
            .put("status", "completed")
            .put("conclusion", conclusion)
            .put("id", id)
    }

    private object NeverStop : VyomFactoryExecutionControl { override fun shouldStop() = false }
    private object StopImmediately : VyomFactoryExecutionControl { override fun shouldStop() = true }

    private companion object {
        const val TOKEN = "test-token-that-is-long-enough-for-github"
    }
}
