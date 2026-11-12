package com.hermes.assistant

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class VyomFactoryWorkerPoolDispatchTest {
    @Test fun dispatchesEligibleTasksAcrossTheTwentyStableWorkerSessionsInParallel() {
        val tasks = (1..20).map { remote(it, VyomFactoryLaneRouter.Lane.values()[(it - 1) / 5], true) }
        val pool = createPool(tasks)
        val registry = createRegistry()
        val entered = CountDownLatch(20)
        val release = CountDownLatch(1)
        val active = AtomicInteger()
        val maximumActive = AtomicInteger()
        val proposed = Collections.synchronizedList(mutableListOf<Int>())
        val store = VyomFactoryWorkerProposalStore(File(tempRoot(), "proposals"))
        val dispatcher = VyomFactoryWorkerPoolDispatcher(
            pool = pool,
            registry = registry,
            fetchRemoteTasks = { tasks },
            modelAdapterFactory = {
                VyomFactoryRepositoryWorkerModelAdapter { task, _, control ->
                    assertFalse(control.shouldStop())
                    val current = active.incrementAndGet()
                    maximumActive.updateAndGet { previous -> maxOf(previous, current) }
                    entered.countDown()
                    try {
                        assertTrue(release.await(5, TimeUnit.SECONDS))
                    } finally {
                        active.decrementAndGet()
                    }
                    candidate(task, registry)
                }
            },
            onProposal = { task, workerId, leaseId, candidate ->
                store.save(task, workerId, leaseId, candidate)
                proposed += task.number
            },
            isStopped = { false },
            heartbeatIntervalMillis = 25L
        )
        val executor = Executors.newSingleThreadExecutor()
        try {
            val future = executor.submit<VyomFactoryWorkerPoolDispatcher.Result> {
                dispatcher.dispatchAvailable()
            }
            assertTrue("All 20 worker sessions should enter provider execution", entered.await(5, TimeUnit.SECONDS))
            assertEquals(20, maximumActive.get())
            release.countDown()
            val result = future.get(10, TimeUnit.SECONDS)
            assertEquals(20, result.proposedCount)
            assertEquals(0, result.failedCount)
            assertEquals((1..20).toSet(), proposed.toSet())
            assertEquals(20, store.pending().size)
            assertEquals(20, pool.concurrencyLimit)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test fun dispatchSkipsUnapprovedAndNonRepositoryTasks() {
        val tasks = listOf(
            remote(1, VyomFactoryLaneRouter.Lane.INSTANT, false),
            remote(2, VyomFactoryLaneRouter.Lane.CUSTOM, true, title = "Set media volume"),
            remote(3, VyomFactoryLaneRouter.Lane.CUSTOM, true)
        )
        val pool = createPool(tasks)
        val registry = createRegistry()
        val proposed = mutableListOf<Int>()
        assertTrue(VyomFactoryTaskClassifier.classify(tasks[2]) ==
            VyomFactoryTaskClassifier.Result.Classified(VyomFactoryCapabilityRegistry.Domain.REPOSITORY))
        assertTrue(VyomFactoryTaskClassifier.classify(tasks[1]) is
            VyomFactoryTaskClassifier.Result.NeedsOwnerClarification)
        val dispatcher = VyomFactoryWorkerPoolDispatcher(
            pool, registry, { tasks },
            modelAdapterFactory = {
                VyomFactoryRepositoryWorkerModelAdapter { task, _, _ -> candidate(task, registry) }
            },
            onProposal = { task, _, _, _ -> proposed += task.number },
            isStopped = { false },
            heartbeatIntervalMillis = 25L
        )

        val result = dispatcher.dispatchAvailable()

        assertEquals(
            "classifier2=${VyomFactoryTaskClassifier.classify(tasks[1])}; " +
                "classifier3=${VyomFactoryTaskClassifier.classify(tasks[2])}; result=${result.outcomes}; proposed=$proposed",
            listOf(3),
            proposed
        )
        assertEquals(1, result.proposedCount)
        assertEquals(0, result.failedCount)
    }

    @Test fun oneProviderFailureDoesNotCancelTheOtherNineteenWorkers() {
        val tasks = (1..20).map { remote(it, VyomFactoryLaneRouter.Lane.values()[(it - 1) / 5], true) }
        val pool = createPool(tasks)
        val registry = createRegistry()
        assertTrue(tasks.all {
            VyomFactoryTaskClassifier.classify(it) ==
                VyomFactoryTaskClassifier.Result.Classified(VyomFactoryCapabilityRegistry.Domain.REPOSITORY)
        })
        val proposed = Collections.synchronizedList(mutableListOf<Int>())
        val dispatcher = VyomFactoryWorkerPoolDispatcher(
            pool, registry, { tasks },
            modelAdapterFactory = {
                VyomFactoryRepositoryWorkerModelAdapter { task, _, _ ->
                    if (task.number == 1) throw IllegalStateException("provider failed")
                    candidate(task, registry)
                }
            },
            onProposal = { task, _, _, _ -> proposed += task.number },
            isStopped = { false },
            heartbeatIntervalMillis = 25L
        )

        val result = dispatcher.dispatchAvailable()

        assertEquals("result=${result.outcomes}; proposed=$proposed", 19, result.proposedCount)
        assertEquals(1, result.failedCount)
        assertEquals((2..20).toSet(), proposed.toSet())
    }

    @Test fun revalidatesRemoteTaskStateBeforeEveryWorkerClaim() {
        val tasks = VyomFactoryLaneRouter.Lane.values().mapIndexed { index, lane ->
            remote(index + 1, lane, true).copy(
                metadata = remote(index + 1, lane, true).metadata.copy(
                    priority = if (index == 0) "P0" else "P1"
                )
            )
        }.toMutableList()
        val pool = createPool(tasks)
        val registry = createRegistry()
        val fetchCount = AtomicInteger()
        val firstProposalReady = CountDownLatch(1)
        val proposed = Collections.synchronizedList(mutableListOf<Int>())
        val dispatcher = VyomFactoryWorkerPoolDispatcher(
            pool, registry,
            fetchRemoteTasks = {
                val call = fetchCount.incrementAndGet()
                if (call > 2) {
                    assertTrue("First worker must finish its remote-state mutation", firstProposalReady.await(5, TimeUnit.SECONDS))
                }
                tasks.toList()
            },
            modelAdapterFactory = {
                VyomFactoryRepositoryWorkerModelAdapter { task, _, _ ->
                    synchronized(tasks) {
                        tasks.indices.forEach { index ->
                            if (tasks[index].number != task.number) {
                                tasks[index] = tasks[index].copy(
                                    metadata = tasks[index].metadata.copy(
                                        status = "paused",
                                        lanePlanningAllowed = false
                                    )
                                )
                            }
                        }
                    }
                    firstProposalReady.countDown()
                    candidate(task, registry)
                }
            },
            onProposal = { task, _, _, _ -> proposed += task.number },
            isStopped = { false },
            heartbeatIntervalMillis = 25L
        )

        val result = dispatcher.dispatchAvailable()

        assertEquals(1, result.proposedCount)
        assertEquals(0, result.failedCount)
        assertEquals(1, proposed.size)
        assertTrue(proposed.single() in 1..4)
        assertEquals(22, fetchCount.get())
    }

    @Test fun globalStopLatchPersistsAcrossStoreInstances() {
        val prefs = MemoryPrefs()
        val first = VyomFactoryWorkerStopStore(prefs)
        first.stopAll()
        assertTrue(VyomFactoryWorkerStopStore(prefs).isStopped())
        VyomFactoryWorkerStopStore(prefs).resumeAll()
        assertFalse(first.isStopped())
    }

    @Test fun globalStopPreventsClaimsAndProviderCalls() {
        val tasks = (1..4).map { remote(it, VyomFactoryLaneRouter.Lane.INSTANT, true) }
        val pool = createPool(tasks)
        val registry = createRegistry()
        val calls = AtomicInteger()
        val dispatcher = VyomFactoryWorkerPoolDispatcher(
            pool, registry, { tasks },
            modelAdapterFactory = {
                VyomFactoryRepositoryWorkerModelAdapter { task, _, _ ->
                    calls.incrementAndGet()
                    candidate(task, registry)
                }
            },
            onProposal = { _, _, _, _ -> },
            isStopped = { true },
            heartbeatIntervalMillis = 25L
        )

        val result = dispatcher.dispatchAvailable()

        assertEquals(0, calls.get())
        assertEquals(0, result.proposedCount)
        assertTrue(result.stopped)
    }

    @Test fun proposalStoreDeduplicatesUntilTheCanonicalIssueChangesAndPreservesOwnerReviewState() {
        val root = File(tempRoot(), "proposal-store")
        val store = VyomFactoryWorkerProposalStore(root)
        val registry = createRegistry()
        val task = remote(7, VyomFactoryLaneRouter.Lane.CUSTOM, true)
        val candidate = candidate(task, registry)

        store.save(task, "worker-test-01", "lease-test-01", candidate)

        assertTrue(store.isHandled(task))
        assertEquals(1, store.pending().size)
        val restored = store.get(task.number)!!
        assertEquals(candidate.plan, restored.candidate.plan)
        assertEquals(task.metadata.sourceId, restored.sourceId)
        store.markRejected(task.number)
        assertTrue(store.pending().isEmpty())
        assertTrue(store.isHandled(task))
        assertFalse(store.isHandled(task.copy(updatedAt = "2026-11-11T12:00:00Z")))
    }

    private fun createPool(tasks: List<VyomFactoryGitHubClient.Task>): VyomFactoryWorkerPool {
        val queue = VyomFactoryTaskStore(MemoryPrefs())
        val coordinator = VyomFactoryWorkerClaimCoordinator(queue, VyomFactoryWorkerLeaseStore(MemoryPrefs()))
        val identities = MemoryPrefs()
        val sequence = AtomicInteger()
        return VyomFactoryWorkerPool(
            identityStore = VyomFactoryWorkerIdentityStore(identities) { "dispatch-worker-${sequence.incrementAndGet()}-id" },
            taskStore = queue,
            coordinator = coordinator,
            fetchRemoteTasks = { tasks }
        )
    }

    private fun createRegistry(): VyomFactorySystemRegistry {
        val root = tempRoot()
        val registry = VyomFactorySystemRegistry(File(root, "registry"))
        val binding = VyomFactoryWorkerBindingPolicy.createBinding(
            "https://github.com/riyas433554-dot/vyom-android",
            "a".repeat(40),
            1L
        )
        registry.register(binding, ownerConfirmed = true)
        return registry
    }

    private fun candidate(
        task: VyomFactoryGitHubClient.Task,
        registry: VyomFactorySystemRegistry
    ): VyomFactoryRepositoryChangeReviewDialog.Candidate {
        val binding = registry.get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID)!!
        return VyomFactoryRepositoryChangeReviewDialog.Candidate(
            VyomFactoryRepositoryChangePolicy.Plan(
                systemId = binding.systemId,
                issueNumber = task.number,
                attemptId = "attempt-${task.number}-000",
                title = "Implement repository task ${task.number}",
                baseBranch = SelfModificationPolicy.PROPOSAL_BASE_BRANCH,
                baseCommitSha = binding.commitSha,
                changes = listOf(VyomFactoryRepositoryChangePolicy.Change(
                    "app/src/main/java/com/hermes/assistant/WorkerGeneratedNote.kt",
                    "package com.hermes.assistant\n// Updated for #${task.number}"
                )),
                ownerApproved = false
            ),
            binding,
            "Worker proposal for issue #${task.number}"
        )
    }

    private fun remote(
        issue: Int,
        lane: VyomFactoryLaneRouter.Lane,
        planningAllowed: Boolean,
        title: String = "Fix repository Gradle build #$issue"
    ): VyomFactoryGitHubClient.Task {
        val sourceId = "dispatch-$issue"
        val metadata = VyomFactoryStatus.Metadata(
            "P1", "open", "chatgpt", sourceId,
            VyomFactoryStatus.idempotencyKey("chatgpt", sourceId)!!,
            emptyList(), lane, planningAllowed
        )
        return VyomFactoryGitHubClient.Task(
            issue, title, "Repository Gradle build fails; compile task must pass.", "open",
            "https://github.com/riyas433554-dot/vyom-android/issues/$issue",
            "2026-11-11T10:00:00Z", metadata, "2026-11-10T10:00:00Z"
        )
    }

    private fun tempRoot() = File(System.getProperty("java.io.tmpdir"), "vyom-worker-${UUID.randomUUID()}").apply { mkdirs() }

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue
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
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { updates[key] = value }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { updates[key] = value }
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
}
