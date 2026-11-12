package com.hermes.assistant

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class VyomFactoryWorkerPoolFakeProviderIntegrationTest {
    private data class WorkerResult(
        val issueNumber: Int,
        val workerId: String,
        val leaseId: String,
        val succeeded: Boolean,
        val stopped: Boolean = false,
        val ownerApproved: Boolean = false
    )

    @Test fun twentyTasksUseTwentyDistinctLaneLeasesAndOverlapWithoutFailureCascading() {
        val fixture = Fixture(taskCount = 20)
        val provider = BlockingFakeProvider(expectedCalls = 20, failTaskNumber = 1)
        val adapter = fixture.adapter(provider, maxRetries = 0)
        val executor = boundedExecutor(fixture.pool.concurrencyLimit)
        val control = neverStop()
        try {
            val futures = fixture.pool.sessions.map { session ->
                executor.submit<WorkerResult> {
                    val claim = session.claimNext() ?: error("Worker did not claim an eligible task")
                    try {
                        val candidate = adapter.propose(claim.remoteTask, fixture.registry, control)
                        WorkerResult(
                            claim.task.issueNumber,
                            session.workerIdentity.workerId,
                            claim.lease.leaseId,
                            succeeded = true,
                            ownerApproved = candidate.plan.ownerApproved
                        )
                    } catch (_: Exception) {
                        WorkerResult(
                            claim.task.issueNumber,
                            session.workerIdentity.workerId,
                            claim.lease.leaseId,
                            succeeded = false
                        )
                    } finally {
                        session.release(claim)
                    }
                }
            }

            assertTrue("All twenty fake-provider calls must overlap", provider.started.await(10, TimeUnit.SECONDS))
            assertEquals(20, provider.maxActive.get())
            assertEquals(20, fixture.leaseStore.activeIssueNumbers(1_000L).size)
            provider.release.countDown()
            val results = futures.map { it.get(15, TimeUnit.SECONDS) }
            assertTrue("Provider call intervals must overlap", provider.latestStartNanos() < provider.earliestEndNanos())

            assertEquals(20, results.size)
            assertEquals(20, results.map { it.issueNumber }.toSet().size)
            assertEquals(20, results.map { it.workerId }.toSet().size)
            assertEquals(20, results.map { it.leaseId }.toSet().size)
            assertEquals(19, results.count { it.succeeded })
            assertEquals(1, results.count { !it.succeeded })
            assertFalse(results.filter { it.succeeded }.any { it.ownerApproved })
            assertEquals(20, provider.calls.get())
            assertEquals(19, fixture.journal.entries.count { it.result == "SUCCESS" })
            assertEquals(1, fixture.journal.entries.count { it.result == "FAILED" })
            assertEquals(0, fixture.leaseStore.activeIssueNumbers(2_000L).size)
            VyomFactoryLaneRouter.Lane.values().forEach { lane ->
                assertEquals(5, results.count { result ->
                    fixture.pool.sessions.single { it.workerIdentity.workerId == result.workerId }
                        .workerIdentity.lane == lane
                })
            }
        } finally {
            provider.release.countDown()
            executor.shutdownNow()
        }
    }

    @Test fun poolWideStopCancelsEveryInFlightFakeProviderProposal() {
        val fixture = Fixture(taskCount = 20)
        val provider = BlockingFakeProvider(expectedCalls = 20)
        val adapter = fixture.adapter(provider, maxRetries = 0)
        val executor = boundedExecutor(fixture.pool.concurrencyLimit)
        val stop = AtomicBoolean(false)
        val control = object : VyomFactoryExecutionControl {
            override fun shouldStop() = stop.get()
        }
        try {
            val futures = fixture.pool.sessions.map { session ->
                executor.submit<WorkerResult> {
                    val claim = session.claimNext() ?: error("Worker did not claim an eligible task")
                    try {
                        adapter.propose(claim.remoteTask, fixture.registry, control)
                        WorkerResult(
                            claim.task.issueNumber, session.workerIdentity.workerId, claim.lease.leaseId,
                            succeeded = true
                        )
                    } catch (error: VyomFactoryExecutionStoppedException) {
                        WorkerResult(
                            claim.task.issueNumber, session.workerIdentity.workerId, claim.lease.leaseId,
                            succeeded = false, stopped = true
                        )
                    } finally {
                        session.release(claim)
                    }
                }
            }

            assertTrue(provider.started.await(10, TimeUnit.SECONDS))
            stop.set(true)
            val results = futures.map { it.get(15, TimeUnit.SECONDS) }

            assertEquals(20, results.size)
            assertEquals(20, results.count { it.stopped })
            assertEquals(0, results.count { it.succeeded })
            assertEquals(20, fixture.journal.entries.count { it.result == "STOPPED" })
            assertEquals(0, fixture.journal.entries.count { it.result == "SUCCESS" })
            assertEquals(0, fixture.leaseStore.activeIssueNumbers(2_000L).size)
        } finally {
            provider.release.countDown()
            executor.shutdownNow()
        }
    }

    @Test fun twentyActiveWorkerLeasesLeaveTheNextTaskQueuedUntilAWorkerIsReleased() {
        val fixture = Fixture(taskCount = 21)
        val claims = fixture.pool.sessions.map { session ->
            session to (session.claimNext() ?: error("Expected one claim per worker slot"))
        }

        assertEquals(20, claims.size)
        assertEquals(20, claims.map { it.second.lease.leaseId }.toSet().size)
        assertEquals(20, fixture.leaseStore.activeIssueNumbers(1_000L).size)
        val claimedNumbers = claims.map { it.second.task.issueNumber }.toSet()
        val waiting = fixture.remoteTasks.single { it.number !in claimedNumbers }
        assertTrue(fixture.taskStore.contains(waiting.number))
        assertEquals(1, fixture.taskStore.pending().count { it.issueNumber !in claimedNumbers })

        val (releasedSession, releasedClaim) = claims.first()
        assertTrue(releasedSession.release(releasedClaim))
        val completedIndex = fixture.remoteTasks.indexOfFirst { it.number == releasedClaim.task.issueNumber }
        val completedTask = fixture.remoteTasks[completedIndex]
        fixture.remoteTasks[completedIndex] = completedTask.copy(
            state = "closed", metadata = completedTask.metadata.copy(status = "completed")
        )
        fixture.taskStore.remove(releasedClaim.task.issueNumber)
        val next = releasedSession.claimNext()

        assertNotNull(next)
        assertEquals(waiting.number, next!!.task.issueNumber)
        assertEquals(releasedSession.workerIdentity.workerId, next.lease.workerId)
        releasedSession.release(next)
        claims.drop(1).forEach { (session, claim) -> session.release(claim) }
    }

    @Test fun noProgressOutputFingerprintPersistsAndResetsWhenOutputChanges() {
        val prefs = MemoryPrefs()
        val journal = VyomFactoryWorkerModelJournal(prefs)
        val repeatedHash = "a".repeat(64)
        repeat(5) { index ->
            journal.record(
                VyomFactoryWorkerModelCallJournal.Entry(
                    taskNumber = 88,
                    timestampMillis = 1_000L + index,
                    inputTokens = 1,
                    outputTokens = 1,
                    result = "FAILED",
                    providerId = "fake-pool",
                    providerCallStarted = true,
                    callId = "call-id-${index.toString().padStart(4, '0')}",
                    outputHash = repeatedHash
                )
            )
        }

        assertEquals(5, journal.consecutiveOutputCount(88, repeatedHash))
        val reopened = VyomFactoryWorkerModelJournal(prefs)
        assertEquals(5, reopened.consecutiveOutputCount(88, repeatedHash))

        reopened.record(
            VyomFactoryWorkerModelCallJournal.Entry(
                taskNumber = 88,
                timestampMillis = 2_000L,
                inputTokens = 1,
                outputTokens = 1,
                result = "SUCCESS",
                providerId = "fake-pool",
                providerCallStarted = true,
                callId = "call-id-0006",
                outputHash = "b".repeat(64)
            )
        )
        assertEquals(0, reopened.consecutiveOutputCount(88, repeatedHash))
    }

    private class Fixture(taskCount: Int) {
        val taskStore = VyomFactoryTaskStore(MemoryPrefs())
        val leaseStore = VyomFactoryWorkerLeaseStore(MemoryPrefs())
        val journal = MemoryJournal()
        val remoteTasks = (1..taskCount).map { number ->
            val lane = if (number <= 20) {
                VyomFactoryLaneRouter.Lane.values()[(number - 1) / 5]
            } else {
                VyomFactoryLaneRouter.Lane.INSTANT
            }
            val sourceId = "pool-fake-task-$number"
            val metadata = VyomFactoryStatus.Metadata(
                "P2", "open", "factory", sourceId, "factory:$sourceId", emptyList(),
                lane = lane, lanePlanningAllowed = true
            )
            VyomFactoryGitHubClient.Task(
                number, "Fake provider task $number", "Implement a harmless Kotlin helper for task $number",
                "open", "https://github.com/riyas433554-dot/vyom-android/issues/$number", "2026-11-11T00:00:00Z", metadata
            )
        }.toMutableList()
        private var idSequence = 0
        val pool = VyomFactoryWorkerPool(
            identityStore = VyomFactoryWorkerIdentityStore(MemoryPrefs()) { "worker-${++idSequence}-stable" },
            taskStore = taskStore,
            coordinator = VyomFactoryWorkerClaimCoordinator(taskStore, leaseStore),
            fetchRemoteTasks = { remoteTasks },
            now = { 1_000L },
            leaseDurationMs = 30_000L
        )
        val registry = VyomFactorySystemRegistry(
            File.createTempFile("vyom-worker-pool", ".tmp").apply { delete(); mkdirs() }
        ).apply {
            register(
                VyomFactorySystemRegistry.Binding(
                    "worker", VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL, "a".repeat(40), VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS, 1L
                ),
                ownerConfirmed = true
            )
        }

        fun adapter(
            provider: VyomFactoryWorkerModelProvider,
            maxRetries: Int
        ) = VyomFactoryStructuredWorkerModelAdapter(
            provider = provider,
            keyStore = object : VyomFactoryWorkerApiKeyStore {
                override fun readKey(): CharArray = "fake-provider-key".toCharArray()
                override fun saveKey(value: CharArray) = Unit
                override fun clear() = Unit
            },
            journal = journal,
            limits = VyomFactoryWorkerModelLimits(timeoutMillis = 10_000L, maxRetries = maxRetries),
            nowMillis = { 1_000L },
            preCallAuthorization = { true },
            repositoryClient = VyomFactoryRepositoryClient(getJson = { requestPath ->
                val relativePath = requestPath.substringAfter("/contents/").substringBefore("?ref=")
                val sourceContent = "base snapshot for $relativePath".toByteArray(Charsets.UTF_8)
                """{"type":"file","path":"$relativePath","encoding":"base64","size":${sourceContent.size},"sha":"blob-sha","content":"${java.util.Base64.getEncoder().encodeToString(sourceContent)}"}"""
            })
        )
    }

    private class BlockingFakeProvider(
        expectedCalls: Int,
        private val failTaskNumber: Int? = null
    ) : VyomFactoryWorkerModelProvider {
        override val providerId = "fake-pool"
        val calls = AtomicInteger()
        private val active = AtomicInteger()
        val maxActive = AtomicInteger()
        val started = CountDownLatch(expectedCalls)
        val release = CountDownLatch(1)
        private val starts = ConcurrentLinkedQueue<Long>()
        private val ends = ConcurrentLinkedQueue<Long>()

        override fun generate(
            request: VyomFactoryWorkerModelProvider.Request,
            apiKey: CharArray
        ): VyomFactoryWorkerModelProvider.Response {
            calls.incrementAndGet()
            val running = active.incrementAndGet()
            maxActive.updateAndGet { previous -> maxOf(previous, running) }
            starts.add(System.nanoTime())
            started.countDown()
            try {
                check(release.await(10, TimeUnit.SECONDS)) { "Fake provider test release timed out" }
                if (request.taskNumber == failTaskNumber) {
                    throw IllegalStateException("simulated fake provider failure")
                }
                val output = JSONObject()
                    .put("repository", request.repository)
                    .put("base_sha", request.baseSha)
                    .put("files", JSONArray().put(
                        JSONObject().put("path", "app/src/main/java/com/hermes/assistant/WorkerGeneratedNote.kt").put("content", "package com.hermes.assistant\n// Reviewed proposal for task ${request.taskNumber}")
                    ))
                    .toString()
                return VyomFactoryWorkerModelProvider.Response(output, 10, 5)
            } finally {
                ends.add(System.nanoTime())
                active.decrementAndGet()
            }
        }

        override fun cancelCurrentCall() {
            release.countDown()
        }

        fun latestStartNanos(): Long = starts.maxOrNull() ?: 0L
        fun earliestEndNanos(): Long = ends.minOrNull() ?: Long.MAX_VALUE
    }

    private class MemoryJournal : VyomFactoryWorkerModelCallJournal {
        val entries = mutableListOf<VyomFactoryWorkerModelCallJournal.Entry>()
        @Synchronized override fun record(entry: VyomFactoryWorkerModelCallJournal.Entry) {
            val index = entries.indexOfFirst { it.callId == entry.callId }
            if (index >= 0) entries[index] = entry else entries += entry
        }
        @Synchronized override fun callsSince(epochMillis: Long) =
            entries.count { it.timestampMillis >= epochMillis && it.providerCallStarted }
        @Synchronized override fun callsForTask(taskNumber: Int) =
            entries.count { it.taskNumber == taskNumber && it.providerCallStarted }
        @Synchronized override fun tokensForTask(taskNumber: Int) =
            entries.filter { it.taskNumber == taskNumber && it.providerCallStarted }
                .sumOf { it.inputTokens.toLong() + it.outputTokens.toLong() }
    }

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun contains(key: String) = key in values
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String): SharedPreferences.Editor = apply { removals += key }
            override fun clear(): SharedPreferences.Editor = this
            override fun commit(): Boolean {
                removals.forEach(values::remove)
                updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                return true
            }
            override fun apply() { commit() }
        }
    }

    private fun boundedExecutor(concurrency: Int): ExecutorService =
        ThreadPoolExecutor(
            concurrency, concurrency, 0L, TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(concurrency)
        )

    private fun neverStop() = object : VyomFactoryExecutionControl {
        override fun shouldStop() = false
    }
}
