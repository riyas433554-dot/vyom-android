package com.hermes.assistant

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class VyomFactoryExecutionRunnerTest {
    @Test fun executesOnlyFreshOpenTaskAndPersistsTerminalSuccess() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val runner = fixture.runner(adapter)
        val outcome = runner.runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals(1, adapter.calls)
        assertEquals(VyomFactoryExecutionJournal.State.SUCCEEDED, fixture.journal.get("attempt-1")?.state)
    }

    @Test fun liveRunnerDispatchesLocallyApprovedRepositoryPlanToExecutorAndJournalsResult() {
        val fixture = Fixture(executionTarget = "repository_change", executionOperation = "propose_change",
            executionArguments = """{"system_id":"worker","title":"Improve safe reply output","base_branch":"feature/memory-intelligence","base_commit_sha":"${"a".repeat(40)}","changes":[{"path":"README.md","content":"class SafeReply"}]}""")
        val baseSha = "a".repeat(40)
        val commitSha = "b".repeat(40)
        val registryRoot = File.createTempFile("repo-live-runner", ".tmp").apply { delete(); mkdirs() }
        val registry = VyomFactorySystemRegistry(registryRoot)
        val binding = VyomFactorySystemRegistry.Binding(
            "worker", "https://github.com/riyas433554-dot/vyom-android", baseSha,
            listOf("README.md"), 1L
        )
        registry.register(binding, ownerConfirmed = true)
        val plan = VyomFactoryRepositoryChangePolicy.Plan(
            "worker", 71, "repo-attempt-12345678", "Improve safe reply output",
            "feature/memory-intelligence", baseSha,
            listOf(VyomFactoryRepositoryChangePolicy.Change(
                "README.md", "class SafeReply"
            )), false
        )
        val workerClock = java.util.concurrent.atomic.AtomicLong(2_000L)
        val approvalStore = VyomFactoryRepositoryApprovalStore(
            fixture.journal, { workerClock.get() }, { "repo-nonce-1234567890" }
        )
        val approval = approvalStore.issueFromOwnerReview(plan, binding)
        val pending = ConcurrentHashMap<Int, VyomFactoryRepositoryChangeAdapter.ApprovedPlan>()
        pending[71] = VyomFactoryRepositoryChangeAdapter.ApprovedPlan(plan, approval)
        val transport = RepositoryWorkerTransport(baseSha, commitSha)
        val executor = VyomFactoryRepositoryChangeExecutor(
            registry = registry,
            transportFactory = { _, _ -> transport },
            nowMillis = { workerClock.get() },
            waitMillis = { workerClock.addAndGet(it) }
        )
        val adapter = VyomFactoryRepositoryChangeAdapter(
            registry, "github-token-that-is-long-enough", approvalStore, pending, executor
        )

        val outcome = fixture.runner(adapter).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertTrue(transport.calls.any { it.first == "POST" && it.second == "/pulls" })
        assertTrue(fixture.journal.get("attempt-1")?.result.orEmpty().contains("repository_proposal_verified:"))
        assertTrue(fixture.journal.get("attempt-1")?.result.orEmpty().contains(commitSha))
        assertTrue(fixture.journal.isApprovalNonceConsumed(approval.nonce))

        val requestCountAfterSuccess = transport.calls.size
        val duplicate = fixture.runner(adapter).runNext()
        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "previous_attempt_succeeded"),
            duplicate
        )
        assertEquals(requestCountAfterSuccess, transport.calls.size)
    }

    @Test fun executesOwnerStartedInProgressComplaint() {
        val fixture = Fixture(remoteMetadataStatus = "in_progress")
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)

        val outcome = fixture.runner(adapter).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals(1, adapter.calls)
    }

    @Test fun skipsTaskThatWasClosedOrChangedBeforeExecution() {
        val fixture = Fixture(remoteState = "closed")
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val outcome = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "task_no_longer_open_or_identity_changed"),
            outcome
        )
        assertEquals(0, adapter.calls)
        assertNull(fixture.journal.get("attempt-1"))
    }

    @Test fun transientFailureSchedulesRetryOnlyForIdempotentAdapter() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = true, isIdempotent = true)
        val outcome = fixture.runner(adapter).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Failed)
        assertEquals(1, fixture.retryStore.all().size)
        assertEquals(1, fixture.retryStore.all().single().failedAttempts)
    }

    @Test fun nonIdempotentAdapterFailureNeverSchedulesReplay() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = true, isIdempotent = false)
        fixture.runner(adapter).runNext()

        assertTrue(fixture.retryStore.all().isEmpty())
    }

    @Test fun idempotentFailureWaitsUntilPersistedRetryIsDue() {
        val fixture = Fixture()
        val firstAdapter = RecordingAdapter(fail = true, isIdempotent = true)
        val first = fixture.runner(firstAdapter)
        assertTrue(first.runNext() is VyomFactoryExecutionRunner.Outcome.Failed)

        val earlyAdapter = RecordingAdapter(fail = false, isIdempotent = true)
        val early = fixture.runner(earlyAdapter).runNext()
        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "retry_not_due_or_manual_review_required"),
            early
        )
        assertEquals(0, earlyAdapter.calls)

        fixture.advanceTo(fixture.retryStore.all().single().nextAttemptAt)
        val retryAdapter = RecordingAdapter(fail = false, isIdempotent = true)
        val retried = fixture.runner(retryAdapter).runNext()
        assertTrue(retried is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals(1, retryAdapter.calls)
    }

    @Test fun failedNonIdempotentOperationIsNeverAutomaticallyReplayed() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = true, isIdempotent = false)
        fixture.runner(adapter).runNext()

        val second = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "retry_not_due_or_manual_review_required"),
            second
        )
        assertEquals(1, adapter.calls)
    }

    @Test fun successfulExecutionClearsOnlyItsMatchingRetrySchedule() {
        val fixture = Fixture()
        fixture.retryStore.recordFailure(71, "android_action:safe_action", "network_timeout", 1_000, true)

        val outcome = fixture.runner(RecordingAdapter(fail = false, isIdempotent = true)).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertTrue((outcome as VyomFactoryExecutionRunner.Outcome.Succeeded).retryScheduleCleared)
        assertTrue(fixture.retryStore.all().isEmpty())
    }

    @Test fun legacyJournalEntryBlocksReplayWhenOperationIdentityIsUnknown() {
        val fixture = Fixture()
        fixture.seedLegacySuccess()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)

        val outcome = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "legacy_attempt_requires_review"),
            outcome
        )
        assertEquals(0, adapter.calls)
    }

    @Test fun expiredOrReplacedLeasePreventsAdapterInvocation() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        fixture.releaseLease()

        val outcome = fixture.runner(adapter).runNext()

        assertEquals(VyomFactoryExecutionRunner.Outcome.Fenced(71), outcome)
        assertEquals(0, adapter.calls)
        assertNull(fixture.journal.get("attempt-1"))
    }

    @Test fun terminalSuccessReleasesExactWorkerClaim() {
        val fixture = Fixture()
        val outcome = fixture.runner(RecordingAdapter(fail = false, isIdempotent = true)).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertNull(fixture.activeLease())
    }

    @Test fun terminalFailureReleasesExactWorkerClaimAfterRecordingFailure() {
        val fixture = Fixture()
        val outcome = fixture.runner(RecordingAdapter(fail = true, isIdempotent = true)).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Failed)
        assertEquals(VyomFactoryExecutionJournal.State.FAILED, fixture.journal.get("attempt-1")?.state)
        assertNull(fixture.activeLease())
        assertEquals(1, fixture.retryStore.all().single().failedAttempts)
    }

    @Test fun unverifiedAdapterOutcomeCannotBecomeTerminalSuccess() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(
            fail = false,
            isIdempotent = true,
            verified = false
        )

        val outcome = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Failed(71, "attempt-1", "execution_error"),
            outcome
        )
        assertEquals(VyomFactoryExecutionJournal.State.FAILED, fixture.journal.get("attempt-1")?.state)
        assertNull(fixture.journal.get("attempt-1")?.result)
        assertTrue(fixture.retryStore.all().isEmpty())
        assertNull(fixture.activeLease())
    }

    @Test fun adapterCanObserveStopSignalBeforeSideEffect() {
        val fixture = Fixture()
        var sideEffects = 0
        val adapter = object : VyomFactoryExecutionAdapter {
            override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
            override val isIdempotent = true
            override fun isVerifiedResult(result: String) = true
            override fun run(
                envelope: VyomFactoryExecutionEnvelope,
                control: VyomFactoryExecutionControl
            ): String {
                fixture.requestStop()
                if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
                sideEffects++
                return "done"
            }
        }
        val runner = fixture.runner(adapter)
        val outcome = runner.runNext()
        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Failed)
        assertEquals(0, sideEffects)
        assertTrue(fixture.retryStore.all().isEmpty())
    }

    @Test fun expiredRunningAttemptIsInterruptedBeforeFreshClaimSelection() {
        val fixture = Fixture()
        fixture.seedRunningAttempt()
        fixture.advanceTo(102_000L)
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)

        val outcome = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "interrupted_attempt_requires_manual_review"),
            outcome
        )
        assertEquals(VyomFactoryExecutionJournal.State.INTERRUPTED, fixture.journal.get("old-running-attempt")?.state)
        assertEquals(0, adapter.calls)
        assertNull(fixture.activeLease())
    }

    @Test fun externalStopAfterConsentSkipsBeforeJournalAndSideEffect() {
        val fixture = Fixture()
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val outcome = fixture.runner(
            adapter,
            prepareExecution = { stop.set(true); it },
            externalStopRequested = { stop.get() }
        ).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "stopped_before_execution"),
            outcome
        )
        assertEquals(0, adapter.calls)
        assertTrue(fixture.journal.all().isEmpty())
        assertNull(fixture.activeLease())
    }

    @Test fun detectorHookRunsAtEveryExplicitRunnerCheckpoint() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val checkpoints = mutableListOf<String>()

        val outcome = fixture.runner(
            adapter,
            onCheckpoint = { _, checkpoint, _ -> checkpoints += checkpoint }
        ).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals(
            listOf(
                "fresh_task_validated",
                "claim_current_before_consent",
                "claim_current_after_consent",
                "before_adapter_dispatch",
                "after_adapter_dispatch",
                "postcondition_verified"
            ),
            checkpoints
        )
    }

    @Test fun verificationDetectorHookReceivesRealAdapterPostconditionResult() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true, verified = false)
        val results = mutableListOf<Pair<Boolean, String>>()

        val outcome = fixture.runner(
            adapter,
            onVerificationResult = { _, passed, evidence -> results += passed to evidence }
        ).runNext()

        assertEquals(VyomFactoryExecutionRunner.Outcome.Failed(71, "attempt-1", "execution_error"), outcome)
        assertEquals(1, results.size)
        assertFalse(results.single().first)
        assertTrue(results.single().second.contains("did not verify"))
    }

    @Test fun lateStopAfterVerifiedSideEffectDoesNotEraseSuccess() {
        val fixture = Fixture()
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        var sideEffects = 0
        val adapter = object : VyomFactoryExecutionAdapter {
            override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
            override val isIdempotent = true
            override fun isVerifiedResult(result: String) = result == "verified"
            override fun run(envelope: VyomFactoryExecutionEnvelope, control: VyomFactoryExecutionControl): String {
                sideEffects++
                stop.set(true)
                return "verified"
            }
        }

        val outcome = fixture.runner(adapter, externalStopRequested = { stop.get() }).runNext()

        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals(1, sideEffects)
        assertEquals(VyomFactoryExecutionJournal.State.SUCCEEDED, fixture.journal.get("attempt-1")?.state)
    }

    @Test fun externalStopIsVisibleToAdapterBeforeItsSideEffect() {
        val fixture = Fixture()
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        var sideEffects = 0
        val adapter = object : VyomFactoryExecutionAdapter {
            override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
            override val isIdempotent = true
            override fun isVerifiedResult(result: String) = true
            override fun run(envelope: VyomFactoryExecutionEnvelope, control: VyomFactoryExecutionControl): String {
                stop.set(true)
                if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
                sideEffects++
                return "completed"
            }
        }

        val outcome = fixture.runner(adapter, externalStopRequested = { stop.get() }).runNext()

        assertEquals(VyomFactoryExecutionRunner.Outcome.Failed(71, "attempt-1", "stopped"), outcome)
        assertEquals(0, sideEffects)
        assertTrue(fixture.retryStore.all().isEmpty())
        assertNull(fixture.activeLease())
    }

    @Test fun previousVerifiedSuccessSkipsBeforeRequestingNewConsent() {
        val fixture = Fixture()
        fixture.seedVerifiedSuccess()
        var prompts = 0
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)

        val outcome = fixture.runner(adapter, prepareExecution = { prompts++; it }).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "previous_attempt_succeeded"),
            outcome
        )
        assertEquals(0, prompts)
        assertEquals(0, adapter.calls)
    }

    @Test fun declinedForegroundPreparationReleasesClaimWithoutStartingJournalAttempt() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val outcome = fixture.runner(adapter, prepareExecution = { null }).runNext()
        assertEquals(VyomFactoryExecutionRunner.Outcome.Skipped(71, "confirmation_declined_or_unavailable"), outcome)
        assertEquals(0, adapter.calls)
        assertTrue(fixture.journal.all().isEmpty())
        assertNull(fixture.activeLease())
    }

    @Test fun foregroundPreparationMayAddOnlyLocalApprovalWithoutChangingRequestedAction() {
        val fixture = Fixture(executionOperation = "set_media_volume", executionArguments = "{\"percent\":35}")
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val outcome = fixture.runner(adapter, prepareExecution = { envelope ->
            envelope.copy(arguments = org.json.JSONObject(envelope.arguments.toString()).put("confirmation_id", "local-approval"))
        }).runNext()
        assertTrue(outcome is VyomFactoryExecutionRunner.Outcome.Succeeded)
        assertEquals("local-approval", adapter.lastEnvelope?.arguments?.optString("confirmation_id"))
        assertEquals("set_media_volume", adapter.lastEnvelope?.operation)
        assertEquals(35, adapter.lastEnvelope?.arguments?.optInt("percent"))
    }

    @Test fun foregroundPreparationCannotChangeTargetOrOperation() {
        val fixture = Fixture()
        val adapter = RecordingAdapter(fail = false, isIdempotent = true)
        val outcome = fixture.runner(adapter, prepareExecution = { envelope -> envelope.copy(operation = "different_action") }).runNext()
        assertEquals(VyomFactoryExecutionRunner.Outcome.Skipped(71, "invalid_prepared_execution"), outcome)
        assertEquals(0, adapter.calls)
        assertTrue(fixture.journal.all().isEmpty())
        assertNull(fixture.activeLease())
    }

    @Test fun busyRunnerDoesNotClaimOrInterruptWorkWhilePermitHeld() {
        val permit = VyomFactoryExecutionPermit()
        assertTrue(permit.tryAcquire())
        try {
            val fixture = Fixture(executionPermit = permit)
            val adapter = RecordingAdapter(fail = false, isIdempotent = true)
            val outcome = fixture.runner(adapter).runNext()
            assertEquals(VyomFactoryExecutionRunner.Outcome.Busy, outcome)
            assertEquals(0, fixture.claimRequests)
            assertEquals(0, adapter.calls)
            assertTrue(fixture.journal.all().isEmpty())
        } finally {
            permit.release()
        }
    }

    @Test fun unsupportedTypedCapabilityIsSkippedBeforeJournalOrAdapterExecution() {
        val fixture = Fixture(executionOperation = "launch_app")
        val adapter = object : VyomFactoryExecutionAdapter {
            override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
            override val isIdempotent = true
            override val capabilities = listOf(
                VyomFactoryCapabilityRegistry.Capability(
                    id = "android_control.set_media_volume",
                    version = 1,
                    domain = VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                    operations = setOf("set_media_volume"),
                    risk = VyomFactoryCapabilityRegistry.Risk.R1,
                    requiresOwnerApproval = false,
                    mutating = true,
                    reversible = false,
                    rollbackCapabilityId = null,
                    idempotent = true,
                    available = true,
                    unavailableReason = null
                )
            )
            var calls = 0
            override fun isVerifiedResult(result: String) = result == "verified"
            override fun run(
                envelope: VyomFactoryExecutionEnvelope,
                control: VyomFactoryExecutionControl
            ): String {
                calls++
                return "verified"
            }
        }

        val outcome = fixture.runner(adapter).runNext()

        assertEquals(
            VyomFactoryExecutionRunner.Outcome.Skipped(71, "capability_unsupported"),
            outcome
        )
        assertEquals(0, adapter.calls)
        assertTrue(fixture.journal.all().isEmpty())
        assertNull(fixture.activeLease())
    }

    private class Fixture(
        remoteState: String = "open",
        remoteMetadataStatus: String = "open",
        executionOperation: String = "safe_action",
        executionArguments: String = "{}",
        private val executionPermit: VyomFactoryExecutionPermit = VyomFactoryExecutionPermit(),
        executionTarget: String = "android_action"
    ) {
        var claimRequests = 0
        private val prefs = MemoryPrefs()
        private val leaseStore = VyomFactoryWorkerLeaseStore(prefs)
        private var lease = leaseStore.claim(71, "runner-test", 1_000, 100_000)!!
        private var preserveReleasedLeaseForNextClaim = false
        private val task = VyomFactoryTaskStore.QueuedTask(71, "Test execution", "P2", "src-71", 900)
        private val metadata = VyomFactoryStatus.Metadata(
            "P2", remoteMetadataStatus, "factory", "src-71", "factory:src-71", listOf("created")
        )
        private val operationField = if (executionTarget == "android_action") "action" else "operation"
        val remote = VyomFactoryGitHubClient.Task(
            71, "Test execution",
            VyomFactoryStatus.withMetadata(
                "Execute a bounded action",
                metadata
            ) + "\n<!-- VYOM_FACTORY_EXECUTION:{\"schema\":1,\"target\":\"$executionTarget\",\"$operationField\":\"$executionOperation\",\"request\":\"run safe action\",\"arguments\":$executionArguments} -->",
            remoteState, "https://github.com/example/repo/issues/71", "2026-11-04", metadata
        )
        val journal = VyomFactoryExecutionJournal(prefs, leaseStore)
        val retryStore = VyomFactoryRetryScheduleStore(MemoryPrefs())
        private var attemptSequence = 0
        private var clock = 2_000L

        fun advanceTo(value: Long) { clock = value }

        fun activeLease() = leaseStore.active(71, clock)

        fun requestStop() {
            assertTrue(journal.requestStop(lease, "attempt-1", 2_001))
        }

        fun seedLegacySuccess() {
            journal.begin(lease, "legacy-attempt", 1_500)
            assertTrue(journal.markRunning(lease, "legacy-attempt", 1_600))
            assertTrue(journal.succeed(lease, "legacy-attempt", 1_700, "old result"))
        }

        fun seedRunningAttempt() {
            journal.begin(lease, "old-running-attempt", 1_500, "android_action:safe_action")
            assertTrue(journal.markRunning(lease, "old-running-attempt", 1_600))
        }

        fun seedVerifiedSuccess() {
            journal.begin(lease, "verified-attempt", 1_500, "android_action:safe_action")
            assertTrue(journal.markRunning(lease, "verified-attempt", 1_600))
            assertTrue(journal.succeed(lease, "verified-attempt", 1_700, "old result"))
        }

        fun releaseLease() {
            assertTrue(leaseStore.release(lease))
            preserveReleasedLeaseForNextClaim = true
        }

        init {
            assertEquals(remoteMetadataStatus, metadata.status)
        }

        fun runner(
            adapter: VyomFactoryExecutionAdapter,
            prepareExecution: (VyomFactoryExecutionEnvelope) -> VyomFactoryExecutionEnvelope? = { it },
            externalStopRequested: () -> Boolean = { false },
            onCheckpoint: (VyomFactoryGitHubClient.Task, String, String) -> Unit = { _, _, _ -> },
            onVerificationResult: (VyomFactoryGitHubClient.Task, Boolean, String) -> Unit = { _, _, _ -> }
        ): VyomFactoryExecutionRunner {
            val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))
            return VyomFactoryExecutionRunner(
                claimNext = {
                    claimRequests++
                    val current = if (preserveReleasedLeaseForNextClaim) lease else
                        leaseStore.claim(71, "runner-test", clock, 100_000)!!
                    lease = current
                    VyomFactoryWorkerClaimCoordinator.Claim(task, remote, current)
                },
                isClaimCurrent = { claim -> leaseStore.active(71, clock) == claim.lease },
                releaseClaim = { leaseStore.release(it.lease) },
                fetchFreshTask = { if (it == 71) remote else null },
                journal = journal,
                adapters = registry,
                retrySchedules = retryStore,
                now = { clock },
                newAttemptId = { "attempt-${++attemptSequence}" },
                prepareExecution = prepareExecution,
                externalStopRequested = externalStopRequested,
                executionPermit = executionPermit,
                onCheckpoint = onCheckpoint,
                onVerificationResult = onVerificationResult
            )
        }
    }

    private class RepositoryWorkerTransport(
        private val baseSha: String,
        private val commitSha: String
    ) : VyomFactoryRepositoryChangeExecutor.Transport {
        val calls = mutableListOf<Pair<String, String>>()
        override fun request(method: String, path: String, body: JSONObject?): JSONObject {
            calls += method to path
            return when {
                method == "GET" && path == "/git/ref/heads/feature/memory-intelligence" ->
                    JSONObject().put("object", JSONObject().put("sha", baseSha))
                method == "GET" && path.startsWith("/git/commits/") ->
                    JSONObject().put("tree", JSONObject().put("sha", "c".repeat(40)))
                method == "POST" && path == "/git/trees" -> JSONObject().put("sha", "d".repeat(40))
                method == "POST" && path == "/git/commits" -> JSONObject().put("sha", commitSha)
                method == "POST" && path == "/git/refs" ->
                    JSONObject().put("object", JSONObject().put("sha", commitSha))
                method == "GET" && path.startsWith("/git/ref/heads/vyom-worker-") ->
                    JSONObject().put("object", JSONObject().put("sha", commitSha))
                method == "POST" && path == "/pulls" ->
                    JSONObject().put("html_url", "https://github.com/riyas433554-dot/vyom-android/pull/99")
                        .put("head", JSONObject().put("ref", "vyom-worker-71-repo-attempt-12345678"))
                method == "GET" && path.startsWith("/actions/runs?head_sha=") ->
                    JSONObject().put("workflow_runs", JSONArray()
                        .put(workflow("Vyom Android CI"))
                        .put(workflow("Vyom Android Debug")))
                else -> error("Unexpected repository worker request: $method $path")
            }
        }
        private fun workflow(name: String) = JSONObject()
            .put("name", name).put("head_sha", commitSha).put("status", "completed")
            .put("conclusion", "success").put("id", if (name == "Vyom Android CI") 1 else 2)
    }

    private class RecordingAdapter(
        private val fail: Boolean,
        override val isIdempotent: Boolean,
        private val verified: Boolean = true
    ) : VyomFactoryExecutionAdapter {
        override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
        var calls = 0
        var lastEnvelope: VyomFactoryExecutionEnvelope? = null
        override fun isVerifiedResult(result: String) = verified
        override fun run(envelope: VyomFactoryExecutionEnvelope, control: VyomFactoryExecutionControl): String {
            calls++
            lastEnvelope = envelope
            if (fail) throw IOException("temporary network timeout")
            return "completed"
        }
    }

    private class MemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int) = defValue
        override fun getLong(key: String, defValue: Long) = defValue
        override fun getFloat(key: String, defValue: Float) = defValue
        override fun getBoolean(key: String, defValue: Boolean) = defValue
        override fun contains(key: String) = key in values
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        private inner class Editor : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            override fun putString(key: String, value: String?) = apply { changes[key] = value } as SharedPreferences.Editor
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = this
            override fun putLong(key: String, value: Long) = this
            override fun putFloat(key: String, value: Float) = this
            override fun putBoolean(key: String, value: Boolean) = this
            override fun remove(key: String) = apply { removals += key } as SharedPreferences.Editor
            override fun clear() = this
            override fun commit(): Boolean {
                removals.forEach(values::remove)
                changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                return true
            }
            override fun apply() { commit() }
        }
    }
}
