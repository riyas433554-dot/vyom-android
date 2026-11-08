## Real worker model — one-worker scope — 2026-11-08

- **Branch:** `feature/real-worker-model`
- **Parent:** `feature/factory-item5-task-intelligence` at `sha:3aba5079451f`
- **Verified implementation HEAD:** `sha:accaf27a44e4`
- **Exact-HEAD CI:** Full `42486785759`; Debug `42486785757`. Both ran on branch `feature/real-worker-model`, HEAD `sha:accaf27a44e4`, and concluded SUCCESS.
- **Full-history scan:** literal log output `no leaks found` and `FULL_HISTORY_GITLEAKS_FINDINGS=0`.
- **Gradle:** Full log `> Task :app:testDebugUnitTest` followed by `BUILD SUCCESSFUL in 2m 38s`; Debug log `> Task :app:testDebugUnitTest` followed by `BUILD SUCCESSFUL in 2m 43s`. Total suite count was not emitted in the retrieved logs: **UNVERIFIED**. The new worker test file contains 14 test methods.
- **APK artifact:** Full run produced `vyom-phase6-accaf27a44e4` (versioned debug APK and checksum).

### Worker boundary and safety

- Exactly one model worker system binding is used: `worker`. Repository URL, immutable base SHA and allowed path roots come only from that owner-approved registry binding. Issue text cannot select another repository or system ID.
- Provider-neutral contract: `VyomFactoryWorkerModelProvider`. No provider was guessed or added. Fake provider is used by tests. `REAL_MODEL_CALLS_ENABLED=false`; runtime remains fail-closed until the owner selects one provider and its single implementation is wired. No extra model families or providers were added.
- Model output is strict structured plan data only: `repository`, `base_sha`, and `files[{path, content}]`. Base branch is taken from the existing protected proposal policy, not model output. Title is derived from the trusted task title.
- GitHub issue title/body are passed in a separate `taskDescriptionData` field marked untrusted. System instructions remain separate and authoritative. The model receives only the registry repository, base SHA, allowed path roots and issue task text—no repository source files, credentials or other-repository content.
- Output JSON schema is strict; repository and base SHA must exactly match the registry; every path is validated by the existing repository-change policy; file count is capped at the existing 20-file limit.
- The model adapter has no GitHub writer, branch writer, approval issuer or repository-file reader. It returns an unapproved candidate into the existing review dialog. Only owner approval creates the existing local one-time approval; the ephemeral executor-routing envelope is created only after that approval. Reject is local-only. Repository-review triage events and pre-approval failure reporting are also suppressed, so no issue/branch/repository write occurs before approval or on Reject.
- Provider credential storage uses AES-GCM with an Android Keystore-held key; only IV and ciphertext are persisted in app-private preferences. The input buffer is wiped after save and the decrypted character buffer after each call. Credential value is not included in prompt, issue, journal or logs. UI can store/replace it, but live calls remain disabled until provider selection.
- Limits: 8,192 total reported tokens per task; per-call input/output budgets are passed to the provider and reported usage is checked; output ceiling 4,096; max 20 files; 60-second request timeout; 20 actual provider calls per UTC day; one retry maximum. STOP is polled during the request and cancels the in-flight future. No unbounded retry loop.
- Every actual provider call is journaled with task number, timestamp, provider ID, input/output token counts, result and call-started flag. The journal contains no credential, full prompt, issue body or model output. Missing provider/binding/credential, exhausted budget or call cap records BLOCKED; malformed/out-of-policy output or provider failure records FAILED; timeout and STOP have their own results.
- The existing verified-progress gate now requires acceptance criteria to be verified before a probe can count as passed. Three inherited test calls were corrected to use the named `description` argument, and the CI-at-50 fixture now explicitly has an unchecked criterion. These are test/progress-gate corrections, not another worker feature.

### New worker tests — exact 14

1. `fakeProviderProducesOnlyAnUnapprovedRegistryBoundPlan`
2. `issueInjectionRemainsDataAndOutOfRegistryModelPathsAreRejected`
3. `apiKeyNeverAppearsInPromptOrCallJournal`
4. `timeoutFailsWithoutRetryLoop`
5. `dailyCallCapBlocksFurtherProviderCalls`
6. `stopPreventsProviderCall`
7. `malformedModelOutputIsRejectedAndJournaled`
8. `workerPlanDoesNotCreateApprovalOrWriteBeforeOwnerReview`
9. `stopCancelsAnInFlightProviderCall`
10. `providerFailureUsesOnlyTheConfiguredSingleRetry`
11. `providerCannotExceedConfiguredOutputTokenLimit`
12. `unselectedProviderBlocksTaskLocallyWithoutStartingCall`
13. `missingWorkerBindingBlocksWithoutCallingProvider`
14. `credentialDecryptionFailureBlocksWithoutCallingProvider`

### Previously approved historical scan entries

Only the four already-approved exact commit-qualified fingerprints were restored; no fifth entry, wildcard or broad suppression was added. Short SHA forms are used here as required.

1. `sha:4f6878bffd98 | app/src/test/java/com/hermes/assistant/SelfModificationPolicyTest.kt | line 72` — fixed synthetic unit-test fixture literal; not loaded from configuration or storage.
2. `sha:4f6878bffd98 | docs/HANDOFF_HISTORY.md | line 2529` — ordinary English wording in device-local lease documentation; not a credential.
3. `sha:4f6878bffd98 | docs/HANDOFF.md | line 988` — documented 40-character Git commit identifier.
4. `sha:dfc0bc5285e9 | .gitleaksignore | line 17` — historical ignore-file self-comment; no credential value.

### Owner phone acceptance — pending provider selection

1. Owner names exactly one provider. Implement only that provider, then enable the live-call flag after review; until then no live model request is made.
2. On phone, store the provider credential using **STORE / REPLACE WORKER MODEL KEY**. Confirm the UI never displays it again.
3. Register the single `worker` binding to the target repository, exact current base SHA, protected base branch `feature/memory-intelligence`, and allowed root `README.md`.
4. Create a small repository task asking for one README line. Run the worker and confirm the plan shows the registry repository, immutable base SHA, `README.md` path and proposed content.
5. Reject a separate test task first; confirm no issue event/comment, branch or repository change. Then use a fresh task for the approval path.
6. Approve the second plan. Confirm the existing local approval flow produces a draft PR, then wait for CI on the exact proposed commit.
7. Confirm progress reaches 100% only after CI and all acceptance criteria are checked. If the task declares a device verification probe, that probe must also be passed; otherwise it must not be declared required for this README-only task.

- **Not yet completed:** provider selection, live provider transport, owner phone test and end-to-end draft-PR test. No claim of live model invocation is made.
- No PR was created or merged for this work. PR #104 remains OPEN + DRAFT + UNMERGED. No force-push or history rewrite.
- Android `main`, Render, backend, production Supabase and PR #102 remain locked and untouched. No follow-on item was started.

### Implementation commit SHAs (all short forms)

`sha:8ac2f6bab69b`, `sha:2b7c8bd64a31`, `sha:d9de0f21fb82`, `sha:48a9548c9c1a`, `sha:8e5db4928ecc`, `sha:493549fdd7a7`, `sha:3bafc2b0575f`, `sha:819332ba1826`, `sha:fec27177f0bb`, `sha:b93e52fbe57a`, `sha:beaee2004c5b`, `sha:e4619250cb8c`, `sha:f72d84365e55`, `sha:bbc57d925ef4`, `sha:82701c7f753c`, `sha:767c360f51e6`, `sha:398a44618ef7`, `sha:0774b65545c0`, `sha:7d4056cd8ada`, `sha:e2eb4c49e283`, `sha:006b4be6ab23`, `sha:17b376c25020`, `sha:accaf27a44e4`.

---

### Item 7 — verifier + evidence-backed progress — 2026-11-07

<badge color="success">CODE-VERIFIED / PHONE-PENDING</badge>

- **Verified code/test HEAD:** `34d92589f3ba0f1e97a782584bbfb0b4e9a2161e` on `feature/factory-item5-task-intelligence`.
- **Exact-HEAD Full CI:** run `42382150570` — **PASS / SUCCESS**; head SHA exactly matches the verified code/test HEAD. `testDebugUnitTest` + `assembleDebug` passed; build log reports `BUILD SUCCESSFUL in 3m 42s`. Full-history Gitleaks scan and redacted report upload passed.
- **Exact-HEAD Debug CI:** run `42382150563` — **PASS / SUCCESS**; head SHA exactly matches the verified code/test HEAD. `testDebugUnitTest` + `assembleDebug` passed; build log reports `BUILD SUCCESSFUL in 2m 52s`. The no-APK content-update demo package also built successfully. Debug/signed APK artifact upload and signed-release publication were **SKIPPED by workflow gate**.
- Full CI artifact: `vyom-phase6-27cc1b81713944d75954e93f188d6d2ae65155a6` (artifact ID `13578655333`, 46,416,623 bytes). This is a CI artifact, not a final APK release or device-install verification. Debug workflow returned no artifacts. No final APK publication is claimed.
- **Implementation:** added `VyomFactoryVerifiedProgress.kt` and `VyomFactoryVerifiedProgressTest.kt`; `VyomFactoryDialog.kt` now displays a per-task progress percentage, bar and evidence breakdown, refreshes it from runner checkpoints / adapter results / owner probe results, and refreshes after a run.
- **100% rule:** 100 is emitted only when all explicit acceptance criteria are checked, the latest matching issue+operation journal attempt is SUCCEEDED, its test/postcondition proof is valid, the exact named behavior probe has PASS evidence, and the canonical task status is completed. Repository-change test evidence requires the verified proposal marker, commit SHA, draft PR URL, and the `ProposalCiGatePolicy` success evidence for that **same exact commit SHA**. Android actions require `DeviceActionExecutor.auditState(result) == "verified"`.
- **Live/negative states:** a CLAIMED / RUNNING / STOP_REQUESTED attempt is shown as RUNNING and earns only partial implementation points; a newer FAILED / INTERRUPTED attempt invalidates an older success. A GitHub task already marked completed without the full proof conjunction is shown as **COMPLETED · UNVERIFIED**, never 100%.
- **Progress weights:** acceptance criteria 25, implementation 25 (10 while actively running), tests/postcondition 25, named behavior probe 15, started state 5, completed state 5. The final 100 is reserved for the full proof conjunction.
- **Tests added (11):** all proof gates required for 100; implementation/tests without behavior; active attempt; completed without journal proof; missing exact-SHA CI; CI for a different commit; failed/interrupted attempt; newer failure invalidating older success; unchecked acceptance criteria; changed probe invalidating old evidence; and all evidence before DONE remaining at 95%.
- **Important limitation:** execution journal is device-local SharedPreferences. Cross-device progress synchronization is **UNVERIFIED / NOT IMPLEMENTED**. Existing GitHub DONE status transition still checks acceptance criteria + owner probe; it can therefore record a completed status without local worker proof, but this progress model labels that state UNVERIFIED and keeps it below 100%. Do not describe this as a server-side completion lock.
- **Phone-pending checklist — Item 7 (do not execute until owner asks):**
  1. Open Factory task cards and confirm progress shows the criteria / implementation / tests / behavior breakdown; a started task or commit alone must not show 100%.
  2. On a test repository proposal, confirm exact-SHA Full + Debug CI evidence is required before the tests gate is marked verified; confirm owner probe PASS is a separate gate.
  3. Confirm a completed issue without successful local journal proof, or with a newer failed attempt / changed probe, remains **COMPLETED · UNVERIFIED** and below 100%; only the complete proof conjunction reaches 100%.
- No physical-phone verification, cross-device sync verification, final APK publication, or production release is claimed. Item 8 self-upgrade remains **NOT STARTED**. Android `main`, Render, Hermes backend and production Supabase were not touched.

---

### Item 6 — repository worker: local approval + live runner integration — 2026-11-07

<badge color="success">CODE-VERIFIED / PHONE-PENDING</badge>

- **Verified code/test SHA:** `0e9d2b40c4766a46f4f647df207befabd8dd9ffd`.
- **Exact-HEAD Full CI:** run `42378140652` — **PASS / SUCCESS**; run head SHA exactly matches the verified code/test SHA.
- **Exact-HEAD Debug CI:** run `42378140654` — **PASS / SUCCESS**; run head SHA exactly matches the verified code/test SHA.
- Both workflow runs and their sole `debug` jobs completed with conclusion `success`. Full CI completed secret scan, full reachable-history Gitleaks scan/report upload, unit tests/debug APK, packaging/checksum and debug APK upload. Debug CI completed build/tests, APK packaging and no-APK content-update demo package; its debug/signed artifact upload and release publication steps were skipped by the workflow gate (not failures). No physical-phone test or final APK publication is claimed.
- **Approval flow:** local structured review dialog → plan-bound local token → worker-model adapter interface → token-verifying repository adapter → `VyomFactoryRepositoryChangeExecutor` → existing runner journal `succeed()`.
- **Coverage matrix — all rows below refer to verified code/test SHA `0e9d2b40c4766a46f4f647df207befabd8dd9ffd`:**
  - Issue JSON `ownerApproved=true` ignored — **PASS**: `VyomFactoryRepositoryChangeReviewDialogTest.issueOwnerApprovedTrueIsIgnoredAndNeverCreatesLocalApproval`.
  - No local token — **PASS**: `VyomFactoryRepositoryChangeAdapterTest.rejectsWithoutLocalTokenEvenWhenIssueEnvelopeIsExecutable`.
  - Tampered plan/hash and repository binding — **PASS**: `VyomFactoryRepositoryApprovalTest.bindsRepositoryBasePathAndContentHash`.
  - Replay / single-use nonce — **PASS**: `VyomFactoryRepositoryApprovalTest.consumesOnceAndPersistsAcrossStoreRecreation`.
  - Expiry — **PASS**: `VyomFactoryRepositoryApprovalTest.expiredTokenIsRejected`.
  - Consumed nonce after restart and persistence failure fail-closed — **PASS**: `consumesOnceAndPersistsAcrossStoreRecreation`, `consumedNonceCommitFailureFailsClosed`.
  - STOP before token consumption / before side effect — **PASS**: `VyomFactoryRepositoryChangeAdapterTest.stopBlocksBeforeApprovalNonceCanBeConsumed`, `VyomFactoryExecutionRunnerTest.externalStopAfterConsentSkipsBeforeJournalAndSideEffect`.
  - Exact-SHA CI timeout — **PASS**: `VyomFactoryRepositoryChangeExecutorTest.timesOutWhenExactShaCiNeverCompletes`.
  - Crash/restart recovery without replay — **PASS**: `VyomFactoryExecutionJournalTest.reopenedJournalRecoversExpiredStopRequestedAttemptWithoutReplay`, `startupRecoveryInterruptsExpiredAttemptsWithoutReplayingOrTouchingLiveLeases`.
  - Path traversal and duplicate paths — **PASS**: `VyomFactoryRepositoryChangeReviewDialogTest.rejectsPathTraversalBeforeShowingAnApprovalDialog`, `VyomFactoryRepositoryChangePolicyTest.refusesTraversalAndDuplicatePaths`.
  - Unauthorized/out-of-registry path — **PASS**: `VyomFactoryRepositoryChangeReviewDialogTest.rejectsPathOutsideTheRegisteredRootBeforeShowingAnApprovalDialog`, `VyomFactoryRepositoryChangePolicyTest.refusesPathsOutsideSystemAllowedRoots`.
  - Duplicate execution / journal lease fencing — **PASS**: `VyomFactoryExecutionRunnerTest.liveRunnerDispatchesLocallyApprovedRepositoryPlanToExecutorAndJournalsResult`, `VyomFactoryExecutionJournalTest.beginRejectsSecondNonterminalAttemptForSameIssueAndLease`.
  - Per-file size and empty set — **PASS**: `VyomFactoryRepositoryChangePolicyTest.refusesOversizedOrEmptyChangeSets`.
  - Maximum file count — **PASS**: `VyomFactoryRepositoryChangePolicyTest.refusesMoreThanMaximumFileCount` verifies 21 files are rejected against the 20-file cap.
  - Aggregate content limit — **PASS**: `VyomFactoryRepositoryChangePolicyTest.refusesAggregateContentAboveMaximumAcrossIndividuallyValidFiles` verifies 500,001 aggregate characters while each file remains below the 200,000 per-file cap.
  - Worker-model adapter interface — **PRESENT**: `VyomFactoryRepositoryWorkerModelAdapter` is a separate `fun interface`; STOP-before-parse test `VyomFactoryRepositoryWorkerModelAdapterTest.stopPreventsModelProposalParsing` is present. No provider integration was requested or added.
  - Issue-controlled approval bypass / source grep — **PASS for the new repository-change path**: `VyomFactoryExecutionEnvelope.kt` contains no `ownerApproved`; `parseCandidate` in `VyomFactoryRepositoryChangeReviewDialog.kt` explicitly sets `ownerApproved=false` and does not read the issue field; the only `ownerApproved=true` assignment is in `VyomFactoryRepositoryChangeAdapter.kt`, after local token removal, issue identity check and `verifyAndConsume`. Test with issue JSON `ownerApproved=true` confirms it is ignored. **Scope note:** legacy `MainActivity.openFactoryCodeProposal` / `onProposeFix` self-modification UI remains present and untouched as explicitly required; it is a separate manually invoked legacy route, not evidence that the issue field can approve the new repository-change adapter.
  - Review dialog fields — **PASS by source**: `VyomFactoryRepositoryChangeReviewDialog.kt` lines 46–53 show repository URL, immutable base branch + short SHA, all file paths and at most three summary lines. Only the local APPROVE button issues the approval token.
- **Missing coverage:** none in the Item 6 matrix above. All required matrix rows are covered by source checks/tests, and exact-HEAD Full + Debug CI both pass.
- **Status:** Item 6 is **CODE-VERIFIED / PHONE-PENDING** on code/test SHA `0e9d2b40c4766a46f4f647df207befabd8dd9ffd`. This HANDOFF-only commit is documentation synchronization; CI verification applies to the code/test SHA above, not this docs-only commit.
- **Phone-pending checklist — Item 6 (do not execute until owner asks):**
  1. On the paired phone, open a structured repository-change task and confirm the dialog displays the registered repository, immutable base SHA, every proposed path and no more than three summary lines.
  2. Reject one proposal and confirm no repository PR/write occurs; on a fresh proposal, approve locally and confirm the approval is bound to that exact issue/plan.
  3. Confirm the result is only a draft PR on the registered feature branch with exact-SHA CI evidence; do not merge or install.
- Worker-model adapter consumes an already-structured proposal; no new model provider or unstructured code generation. Executor remains draft-PR-only and trusted-repository/feature-branch constrained.
- PR #102 remains open/draft/unmerged, with existing base `feature/memory-intelligence`; no merge, retarget or PR metadata change. Its head branch is this implementation branch, so branch commits are reflected in its diff. Android `main`, Render, backend and production Supabase were not touched. Item 7 is recorded above as CODE-VERIFIED / PHONE-PENDING; Item 8 remains NOT STARTED.

---### Item 5 — exact-HEAD CI verification — 2026-11-07

- Item 5 implementation/code HEAD: `02e3f205a6ff8ece1d1741a6003139392953f3cd` on `feature/factory-item5-task-intelligence`.
- Exact-HEAD Full CI run `42363894376` = **SUCCESS** (started 10:25:29Z, updated/completed 10:29:55Z; duration 4m26s). Previous Full PASS `42362014444` duration was 4m00s. Full CI produced versioned artifact `vyom-phase6-819fbf5424be89ae4c2125f147acc1cdd972aeaf` (artifact ID `13571271465`, 46,368,789 bytes) and redacted full-history Gitleaks report artifact `13570452730`.
- Exact-HEAD Debug CI run `42363894388` = **SUCCESS** (started 10:25:29Z, updated/completed 10:32:21Z; duration 6m52s). Previous Debug PASS `42362014435` duration was 7m13s. Build/tests, versioned APK packaging/checksums, and no-APK content update demo package completed successfully. Debug APK upload and signed CI release steps were **SKIPPED by workflow gate**; Debug run produced no artifacts. Do not claim Debug artifact publication.
- Both exact-head runs completed below the stuck thresholds (2× prior duration and 40 minutes). No cancel/rerun was performed. No source changes were made during monitoring.
- Item 5 is **CODE-VERIFIED** on implementation HEAD `02e3f205a6ff8ece1d1741a6003139392953f3cd`. This does not upgrade automatic stage-by-stage detector invocation: no autonomous planner/implementation/test producer is wired to call `createDetectedTasks`; that remains **UNVERIFIED / NOT COMPLETE**. No physical-device verification or final APK publication is claimed.
- No Android `main`, Render, Hermes backend, or production Supabase changes.

- **Item 5 phone/runtime acceptance remains pending.** Automatic invocation of the stage detector by autonomous planning, each implementation checkpoint and test/verification producer remains **NOT IMPLEMENTED / NOT VERIFIED**; do not infer that gap is closed from the Item 5 CI pass.
- **Phone-pending checklist — Item 5 (do not execute until owner asks):**
  1. On the paired phone, create linked tasks with `Blocked by: #N` prerequisites and a cyclic pair; confirm ready tasks respect dependencies and cyclic tasks remain blocked.
  2. Exercise a task with unchecked acceptance criteria and the configured iteration cap; confirm DONE is blocked until criteria are checked and retries stop at the cap.
  3. Probe planning, implementation-checkpoint and verification outputs containing `[BUG]`, `[MISSING_FEATURE]` and `[IDEA]`; record whether separate deduplicated tasks appear. **Automatic stage-hook wiring is not complete, so this is a gap probe, not an expected PASS.**

---

### Item 4 — CODE-VERIFIED / PHONE-PENDING — 2026-11-07

- Exact source HEAD audited: `26fe7829a8463ee36f3fb64e1ea61b66b5ad042b`.
- Exact-HEAD Full CI run `42362014444` = **SUCCESS**; exact-HEAD Debug CI run `42362014435` = **SUCCESS**. Debug job API reported job `128372210467` completed/success and all executed build/test/package steps successful; APK upload and signed-release steps were skipped by the workflow gate. `BlobNotFound` from the log endpoint was not treated as a failure.
- Registry binding and access-boundary test proof covers unregistered system, wrong repo/commit, unauthorized root, traversal, absolute/backslash paths and sibling-prefix bypass. Evidence publisher tests cover traversal/out-of-root denial, public-repository fail-closed, stale/concurrent branch-head rejection, hash verification and non-force ref update.
- Prior 113-file source grep audit is recorded in the preceding Item 4 history; targeted reinspection confirms the old fixed-branch uploader/callsite is absent and no generic worker repository writer/executor adapter exists. No generic worker repository execution or write isolation is claimed.
- Code gate is closed. Owner phone-side binding inventory and user flow remain outstanding; therefore Item 4 is **CODE-VERIFIED / PHONE-PENDING**, not fully phone-verified.

#### OWNER PHONE CHECKLIST — Item 4

1. **System registry:** Open Factory → repository registration/review. Confirm `factory-task-engine`, `factory-evidence`, and `self-modification` each show the intended canonical repo, immutable commit and allowed roots; save only after reviewing each binding. Expected: each binding persists and a different binding cannot silently overwrite it.
2. **Evidence publishing:** From a private repository binding, trigger screenshot evidence for a test issue. Expected: dedicated `.factory-evidence/` branch update, verified uploaded hash/receipt, then issue comment; public repositories must fail closed.
3. **Self-modification:** Use an owner-approved feature-branch binding to prepare a proposal. Expected: PR only on the approved feature branch, no auto-merge, no release/install, and no Android `main` change.

---

### Item 5 — Planner + detector + prioritizer — implementation batch

#### Confirmed gaps against the existing planner/classifier/triage code

- Existing classifier is a conservative domain classifier; it does not scan structured planning, implementation and test outputs for explicit bug/missing-feature/idea findings.
- Existing ordering was priority + FIFO only; it did not model `Blocked by: #N` prerequisite edges or report cycles.
- Existing task model had no enforced acceptance-criteria completion gate.
- Existing worker had no per-task hard attempt/iteration ceiling.
- Existing dedupe was issue-creation idempotency only; there was no finding fingerprint check or separate-task routing for detector ideas.

#### Changes in this batch

- Added `VyomFactoryTaskIntelligence.kt`: deterministic dependency extraction, topological ordering with priority/FIFO tie-break, cycle detection, explicit `[BUG]` / `[MISSING_FEATURE]` / `[IDEA]` marker detection for PLANNING / IMPLEMENTATION / TEST, normalized duplicate detection, deterministic separate-task proposals, acceptance-criteria checkbox validation, and a bounded iteration policy (default 3; configurable 1–10).
- `VyomFactoryTaskOrdering.kt`, `VyomFactoryTaskTriage.kt`, `VyomFactoryWorkerSession.kt`, and `VyomFactoryWorkerClaimCoordinator.kt` now use dependency-aware ordering; cyclic tasks are excluded from claim selection and exposed as triage blockers.
- `VyomFactoryExecutionRunner.kt` refuses another attempt when the per-task journal history reaches the configured maximum.
- `VyomFactoryGitHubClient.kt` blocks completion until every explicit acceptance criterion is checked and exposes `createDetectedTasks`, which stores nonduplicate findings as separate, deterministic `task_failure` issues without editing the parent task.
- Added `VyomFactoryTaskIntelligenceTest.kt` for prerequisite ordering, circular dependencies, duplicate suppression, max-iteration stop, separate idea task/scope isolation, all three detector stages, and acceptance-criteria completion.
- Runtime integration limitation: the explicit stage detector and separate-task persistence API are implemented, but there is not yet an autonomous planner/implementation/test producer wired to call `createDetectedTasks` after every stage. Automatic stage-by-stage discovery is **UNVERIFIED / NOT COMPLETE**; no claim of autonomous continuous detection.
- No Android `main`, Render, Hermes backend, or production Supabase changes. No physical-device test or final APK publication claimed.

### Item 4 — latest exact-HEAD CI + registry boundary audit — 2026-11-07

- Latest source/documentation HEAD before this note: `04f9d8667623396f98b14c338516dc017babcb38` on `feature/memory-intelligence`.
- Exact-HEAD Full CI `42349657966` = **SUCCESS**; exact-HEAD Debug CI `42349657917` = **SUCCESS**. Both report that exact `head_sha`. Full CI artifact `vyom-phase6-04f9d8667623396f98b14c338516dc017babcb38` (artifact ID `13565445614`, 46,354,574 bytes) and redacted full-history Gitleaks report (artifact ID `13564958500`) exist. Debug workflow artifact list is empty; its APK upload and signed-release steps were skipped by the workflow gate. This is CI evidence, not a final APK publication or device-install verification.
- Re-audited the registry and repository-scoped access paths:
  - `factory-evidence`: only screenshot content writer; owner-bound to `.factory-evidence/`, private-repository check, expected branch-head comparison, non-force ref update, commit-pinned byte/hash readback and receipt.
  - `self-modification`: owner-bound source proposal path; allowlist + pinned feature-branch base checks, stale-base recheck, PR-only; no merge/release/install/main path.
  - `factory-task-engine`: Issues namespace is registry-bound through virtual path `issues`; issue metadata/comments are remote writes, not source-content writes. `/user` identity lookup is not repository-scoped.
  - Bound system file reader is read-only and resolves repo + immutable commit from the registry before network access. Arbitrary repository inspect/preview and skill/release reads remain GET-only and do not grant execution capability.
  - No generic worker repository writer/executor adapter exists; do not claim generic worker read/write isolation or autonomous repository execution.
- The registry is app-private device-local storage. GitHub source/CI cannot reveal the entries currently present on the owner's installed phone. Therefore the actual installed binding inventory and owner review of each system's repository + commit + allowed roots remain **NOT VERIFIED**. Registration UI supports explicit owner binding and compare-and-swap migration, but operationally registering/confirming every intended system is still outstanding.
- Repository remains public and contains a committed `.factory-evidence/issue-101/a1ef05be-53bc-4e39-94c9-005b4b455ef4.png` from earlier work. No deletion, history rewrite, or privacy change was authorized; new evidence publishing rejects public repositories.
- Item 4 remains **IN PROGRESS / NOT CLOSED** pending owner-side binding inventory/review and the full executor access-boundary proof. Item 5 must not start until Item 4 is explicitly closed and owner says GO.
- No Android `main`, Render, Hermes backend, or production Supabase changes. No final APK publication or physical-device verification claimed.

---

### Item 4 isolation test expansion + CI correction — 2026-11-06

- Added tests that two system IDs retain separate repository bindings and cannot borrow each other's allowed roots; repository URL/commit mismatch is rejected; sibling prefix paths (for example `app/src2` when only `app/src` is allowed) are denied. Test commit: `c77324e329990fa849c4be6cf0edd45ab204edd9`.
- CI logs for the earlier bound-read implementation (`4bb5c7d2da8c29780eafb8096346cd99adaa08c8`) showed `VyomFactoryRepositoryClientSystemAccessTest > boundReadUsesOnlyRegisteredRepositoryAndCommit` failing at test line 34 with `RuntimeException`; build compilation and APK assembly had completed. The fixture used Android `org.json.JSONObject.toString()` in a local JVM test. To remove that Android mock dependency, changed the fixture to a literal JSON response string. Fix commit: `f5cd14a6e9b066041a81f846d8a0fd9ca92d5b2e`. The failure's underlying mock-stub message was not emitted in the retained log, so the precise exception cause remains unverified; the fixture no longer invokes that method.
- CI: Debug `42238666059` succeeded on `42e7b5d99ae780ceb746020da1f3ee305934c7b5`. Full CI `42238666033` on that SHA remained in progress at last check. On `c77324e329990fa849c4be6cf0edd45ab204edd9`, Full CI `42239057356` was cancelled and Debug `42239057640` remained in progress. On current code HEAD `f5cd14a6e9b066041a81f846d8a0fd9ca92d5b2e`, Full CI `42239150235` was pending and Debug CI `42239150248` was in progress at last check. These runs are SHA-specific; no latest-head pass is claimed.
- Item 4 remains **IN PROGRESS**. Owner-reviewed binding UI is reachable from the Factory dialog (`INSPECT / REGISTER ANY GITHUB REPOSITORY`); it binds system ID + allowed roots to a pinned repository revision. Guarded file reads use only the registry binding and deny unknown systems/out-of-root paths before network access. No repository write/change adapter exists, so no end-to-end read/write executor isolation claim.
- Item 3 remains **CLOSED (single-device scope)**. Continuous heartbeat remains **UNVERIFIED / NOT IMPLEMENTED**. Cross-device shared atomic coordination remains deferred with the existing caveat.
- No Android `main`, Render, Hermes backend, or production Supabase changes.

---

### Item 4 owner binding flow — 2026-11-06

- Updated `VyomFactoryRepositoryReviewDialog.kt` to require an explicit system ID and one or more allowed relative path roots during owner-reviewed repository registration. On the explicit bind action, it stores the immutable repository URL + commit + roots in `VyomFactorySystemRegistry` and retains the source in the read-only repository catalogue.
- Invalid system IDs and empty path-root lists are rejected in the UI; registry validation still rejects traversal, absolute paths, backslashes and non-canonical repository URLs. Existing bindings cannot be silently replaced.
- Commit: `c339b4df5d3305cb3452433ae09156f858981da1`.
- The owner-binding UI, registry resolver and guarded repository read are now connected. The repository write/change adapter still does not exist, so writes are not yet executing through this policy. Item 4 remains **IN PROGRESS**; no end-to-end executor isolation claim.
- CI for this UI commit and the final HANDOFF synchronization commit is pending; exact run IDs/results must be recorded after GitHub reports them. Prior Full CI `42237560686` passed only for earlier SHA `35e474c567d0b879d92e44f37069aed46b1c19a0`; it does not validate the later read-guard/UI commits.
- No Android `main`, Render, Hermes backend, or production Supabase changes.

---

### Item 4 continuation — bind repository reads to system allowlists — 2026-11-06

- Added `VyomFactorySystemRegistry.requireAuthorizedBinding` as the fail-closed resolver for a system's owner-approved repository URL, immutable commit SHA and allowed relative path roots. Existing `allowsPath` now delegates to this resolver.
- Added `VyomFactoryRepositoryClient.readBoundSystemFile(systemId, relativePath, registry)`. It resolves repository and commit only from the system registry, rejects unknown systems/out-of-root paths before any network call, and fetches the requested file at the pinned commit. No caller-supplied repository or branch can override the binding.
- Added `VyomFactoryRepositoryClientSystemAccessTest.kt` covering pinned repo/commit selection, deny-before-network for disallowed paths, and unknown-system denial.
- Commits: registry primitive `35e474c567d0b879d92e44f37069aed46b1c19a0`; bound read implementation `4bb5c7d2da8c29780eafb8096346cd99adaa08c8`; JVM-only Base64 test correction `96367840b695c91e15f2865469eedb0ab5e296ba`.
- CI evidence: Full CI `42237560686` on `35e474c567d0b879d92e44f37069aed46b1c19a0` = **SUCCESS**. Debug CI `42237560362` on that SHA remained in progress at last check. On `4bb5c7d2da8c29780eafb8096346cd99adaa08c8`, Full CI `42238389407` and Debug CI `42238389400` were in progress. On `96367840b695c91e15f2865469eedb0ab5e296ba`, Full CI `42238416346` was pending and Debug CI `42238416404` was in progress. These runs are not interchangeable; only exact matching SHA results count.
- Item 4 remains **IN PROGRESS**: per-system pinned read access is now wired through a guarded client method, but there is no repository mutation/execution adapter yet, and the existing generic intake preview remains a read-only, non-system-scoped inspection flow. Do not claim complete read/write execution isolation until every executor read/write path is forced through this policy and tested.
- Continuous Task Engine heartbeat remains **UNVERIFIED / NOT IMPLEMENTED**; Item 3's single-device closure and cross-device caveat remain unchanged. No Android `main`, Render, Hermes backend, or production Supabase changes.

---

### Owner decision + Item 3 closure / Item 4 start — 2026-11-06

- Owner explicitly narrowed Task Engine acceptance to a single-device deployment (one owner Android phone). Item 3 is **CLOSED (single-device scope)** based on the existing local durable queue, journal, lease fencing, STOP and conservative expiry recovery, plus checkpoint lease renewal. This closure does not claim distributed coordination.
- Mandatory caveat: **Cross-device atomic claim + shared coordination authority deferred; do not run multiple Vyom installations against the same task queue. Two devices can independently claim the same task.**
- Continuous background heartbeat was inspected but **UNVERIFIED / NOT IMPLEMENTED** in this batch. Current renewal is only invoked at runner claim-validation / adapter STOP checkpoints. A timer-based renewal requires safe ownership/cancellation across claim acquisition, terminal release, STOP, dialog dismissal, foreground-generation invalidation and worker shutdown; adding a timer without a claim-scoped lifecycle and tests risks renewing a stale claim or leaking a thread. No continuous-heartbeat claim is made. Existing checkpoint-renewal proof remains implementation source commit `d9fedf6cb8ab6ea9e14cba715f163b967778440e`, Full CI `42235839898`, Debug CI `42235839837` on HANDOFF HEAD `71aa32085901eda2877ae95bf635f733c440f2e6`.
- Item 4 **STARTED**: added `VyomFactorySystemRegistry.kt` and `VyomFactorySystemRegistryTest.kt`. The new local registry binds a system ID to one canonical repository URL, immutable 40-character commit, and explicit relative allowed-path roots; owner approval is required; silent rebinding is rejected; traversal/absolute/backslash paths are denied. Tests cover approval, immutable mapping and path boundary checks.
- Item 4 is **IN PROGRESS, NOT CLOSED**: current change introduces the registry primitive, but existing repository execution/adapters are not yet wired to resolve system bindings and enforce `allowsPath` before every read/write. Do not claim end-to-end path isolation until that integration and tests exist.
- New source/test files are in this batch; exact commit SHA and exact-head Full/Debug CI IDs/results are to be appended after GitHub reports them. No Android `main`, Render, Hermes backend, or production Supabase changes.

---

### Item 3 follow-up — checkpoint lease renewal — 2026-11-06

- Added a focused regression test first in `VyomFactoryWorkerSessionTest.kt`: `currentClaimRenewsItsLeaseAtExecutionCheckpoints`. Test-only commit: `403897c9290bd4295b7677cb944a77a1ffe926b6`. Its intended behavior requires `renewAndCheck`, which did not exist at that test commit; CI runs for that commit are still running/slow and have not provided a completed red result. Do not claim a verified RED test.
- Implementation commits:
  - `b9937206a883748f1b38bbd4c1445e672bfbc647` — `VyomFactoryWorkerClaimCoordinator.kt`: identity-based current-lease check and exact-token renew.
  - `6ba80f1f4d021baea2a5055644b05a4b362a4398` — `VyomFactoryWorkerSession.kt`: `renewAndCheck` with the configured lease duration.
  - `d9fedf6cb8ab6ea9e14cba715f163b967778440e` — `VyomFactoryDialog.kt`: runner's existing claim-validation checkpoints now renew the lease.
- Intended effect: the runner renews the same fencing token whenever it checks `isClaimCurrent`, including before dispatch and each adapter `shouldStop()` checkpoint. Renewal changes expiry only; the attempt journal remains bound to the stable lease ID/owner ID.
- Limitation: this is cooperative checkpoint-based heartbeat, not a timer thread. If an adapter blocks without polling `shouldStop()`, the lease may still expire. Do not describe this as continuous heartbeat.
- Cross-device atomic lease/journal remains unresolved. Queue, lease and journal are still device-local SharedPreferences; a shared atomic authority is required to prevent two installations from claiming the same issue. Backend/Render/production Supabase remain out of scope, so Item 3 stays **INCOMPLETE**.
- No Android `main`, Render, Hermes backend, or production Supabase changes. Repository still reports public visibility; no private data uploaded.
- Final implementation + HANDOFF source HEAD: `0f8bdd977fc8b3dcc31ce5c6a844d0165383c369`. Exact-head Full CI `42234764933` = **SUCCESS** (secret scan, unit tests/debug build, APK packaging/checksum). Exact-head Debug CI `42234764938` = **SUCCESS** (build/tests and packaging/checksum; publication remains gated). These prove the checkpoint-renewal change compiles and passes CI; no physical-device test or artifact delivery is claimed.
- Test-first evidence: test-only commit `403897c9290bd4295b7677cb944a77a1ffe926b6` Full CI `42234284768` failed at `Unit tests and debug APK`; available job metadata does not expose the compiler/test detail, so the exact failure cause is **UNVERIFIED**. The test-only Debug run `42234284897` remained in progress at last check; it is not used as pass evidence.
- Item 3 remains **INCOMPLETE** because the lease/journal authority is still local-only and no cross-device atomic claim exists.

### Item 3 task-engine audit — 2026-11-06

- Audit branch: `feature/memory-intelligence`; audited source HEAD: `7dc5e163b76d29dd39c1870aa0d7fbbdf18d99e4`. No application source was changed in this audit batch.
- Exact-head Full CI run `42232645990` = **SUCCESS**; exact-head Debug CI run `42232645717` = **SUCCESS**. Both target the audited SHA above. Full CI completed Termux syntax/intake checks, configured secret scan, pinned full reachable-history Gitleaks scan/report upload, unit tests/debug build and packaging. Debug CI completed build/tests and packaging; artifact/release publication remains behind the explicit manual gate.
- Existing implementation verified by source review:
  - `VyomFactoryTaskStore.kt`: SharedPreferences-backed device-local queue; dedupe/reconcile, ordering via queue policy, synchronous `commit()`, and fail-closed behavior on corrupt data or failed writes.
  - `VyomFactoryExecutionJournal.kt`: durable attempt state machine (CLAIMED/RUNNING/STOP_REQUESTED/SUCCEEDED/FAILED/INTERRUPTED), lease-fenced writes, bounded history, startup expiry recovery, and fail-closed persistence. Expired attempts are interrupted, never auto-replayed; `replayable()` is deliberately empty.
  - `VyomFactoryWorkerLeaseStore.kt`: durable acquire/renew/release with exact-token fencing and expiry. Tests cover contention, stale-token rejection, expiry, recreation and failed writes.
  - `VyomFactoryWorkerClaimCoordinator.kt` / `VyomFactoryWorkerSession.kt`: fresh GitHub snapshot before claim, identity/state checks, local claim coordination and queue reconciliation.
  - `VyomFactoryExecutionRunner.kt`: cooperative STOP checks, pre-side-effect approval, fresh task validation, operation identity, verified postcondition requirement, idempotency-gated retry and terminal journal-before-release ordering.
  - Relevant tests: `VyomFactoryTaskStorePersistenceTest.kt`, `VyomFactoryWorkerLeaseStoreTest.kt`, `VyomFactoryWorkerClaimCoordinatorTest.kt`, `VyomFactoryExecutionJournalTest.kt`, `VyomFactoryExecutionRunnerTest.kt`.
- **Item 3 remains INCOMPLETE / BLOCKED; do not mark closed.** Two material gaps are confirmed:
  1. Queue and lease are explicitly device/process-local SharedPreferences plus in-process locks. They do not provide an atomic cross-device/shared lease or a canonical shared journal. Multiple Vyom installations can claim the same GitHub issue independently.
  2. Lease renewal exists as a store method, but no heartbeat/renewal call is wired into `VyomFactoryWorkerSession`, the runner, or the drain loop. Default lease is 60 seconds; longer execution can outlive its lease and be fenced.
- Recovery is conservative rather than automatic replay: expired RUNNING/STOP_REQUESTED work becomes INTERRUPTED and requires manual review. This avoids duplicate external side effects, but does not yet provide safe crash-resume. STOP is cooperative and depends on adapter `shouldStop()` checks; it cannot forcibly interrupt an adapter blocked in an uninterruptible operation.
- Tests are present for the listed local invariants, but CI green does not prove cross-device exclusion, heartbeat behavior, or physical crash recovery. Those are not tested by the current implementation.
- No safe cross-device lease can be added without an authorized shared atomic coordination service; backend/Render/production Supabase are explicitly out of scope. Do not fake distributed guarantees with another local lock. Next work should be a bounded owner-approved design decision for the lease/journal authority and heartbeat integration, then implementation/tests. No Android `main`, Render, Hermes backend, or production Supabase changes.
- Repository privacy caveat persists: GitHub metadata reports `private=false`; no private data or remote screenshots should be added.

## Live core audit continuation — 2026-11-06 (owner final order)

- Audit target HEAD before this documentation refresh: `e1e8864345280f37b24619c5ce5bd79b3137e8b6`; branch ref was verified against GitHub immediately before inspection. Repository remains public (`private=false`), so the requested private-repository connection requirement is not currently satisfied. No visibility change was made.
- Exact-head workflows were triggered by the owner-order commit and are still running at time of this entry: Debug run `42170109408` (job `127735688921`) and Full CI run `42170109392` (job `127735689307`). Both runs have `head_sha=e1e8864345280f37b24619c5ce5bd79b3137e8b6`; do not treat in-progress as pass. In Full CI, Termux syntax and CLI checks passed, secret scan passed, and Gradle unit tests/debug APK step was still in progress. Debug workflow build/test step was still in progress. No artifact conclusion yet.
- Source audit (existence is not proof of end-to-end operation):
  - Task queue: `VyomFactoryTaskStore.kt` is a durable device-local SharedPreferences inbox with dedupe/reconciliation. It explicitly is not a shared atomic lease.
  - Journal: `VyomFactoryExecutionJournal.kt` records attempts and fences against the local lease; its own contract says single-process only, and expired attempts are interrupted, not replayed.
  - Lease/claim: `VyomFactoryWorkerLeaseStore.kt` and `VyomFactoryWorkerClaimCoordinator.kt` provide device/process-local claims only, not cross-device/distributed claims.
  - STOP/recovery: runner and journal include stop/fencing/interruption paths; exact behavior still needs tests/CI and integration review.
  - System registry: `VyomFactoryRepositoryRegistry.kt` stores immutable, owner-approved repository source references. It is not yet a registry mapping each Vyom system to its own repository plus enforced allowed paths.
  - Planner: `VyomFactoryTaskPlanner.kt` currently classifies and returns capability candidates, blocked capability, clarification, or owner review. Its contract explicitly says candidate discovery only; it does not yet implement the full missing-requirement detector/prioritizer.
  - Worker/executor: `VyomFactoryExecutionRunner.kt` is documented as one-at-a-time foreground execution through registered adapters. It is not yet an isolated repository code worker/executor and does not provide app-closed autonomy.
  - Verifier: `VyomFactoryVerificationPolicy.kt` requires an assigned verification probe and a passing evidence event before issue completion. It does not yet prove universal behavior or evidence-based percentage progress.
- Current source tree includes many Factory classes/tests; preserve and extend them rather than recreate them. No application source was changed in this audit.
- CI test-source blockers previously recorded remain visible in current branch source: `VyomFactoryDrainCoordinatorTest.kt` contains literal escaped newline sequences in a Kotlin expression; `SelfModificationClientIntegrationTest.kt` contains malformed escaped source string syntax; `VyomFactoryRepositoryCatalogTest.kt` references `VyomFactoryRepositoryCatalog`, while no matching production source file exists in the current recursive tree. Exact CI run will establish actual compiler result.
- Core item 1 (audit + HANDOFF sync): source and current CI state reconciled and recorded; audit snapshot complete for this pass. Core item 2 (historical secrets check) remains blocked: original redacted per-file/per-line/per-commit inventory for the 169 Discord-pattern hits and the third historical credential assignment record are still unavailable. A passing configured Gitleaks CI step does not classify those historical broad-pattern hits.
- No changes to Android `main`, Render, backend, or production Supabase.

# OWNER-LOCKED VYOM FACTORY CORE ORDER — 2026-11-06

**This is the controlling plan. It supersedes all older phase gates, Stage 1–7 ordering, APK 1-only scope language, and any instruction to wait for a separate phase GO. There are no P0/P1/P2 phases. Work continuously in the exact order below. Do not reorder, skip, or re-implement completed items.**

## Final core order

1. **Audit + HANDOFF sync** — establish actual branch/HEAD, reconcile implementation and CI evidence, preserve prior history, and keep this file current.
2. **Secrets check (historical 169 / 3 / 4 findings)** — classify historical matches without exposing secret values; no clean/security-safe claim while any finding remains unresolved.
3. **Task engine** — canonical journal, queue, durable claim/lease/heartbeat, STOP, cancellation, crash recovery and duplicate/partial-write safety. Reuse existing implementation; verify before extending.
4. **System registry** — each Vyom system maps to its own repository and explicitly allowed paths; enforce repository/path boundaries at execution time.
5. **Planner + detector + prioritizer** — understand tasks, detect missing requirements/blockers during planning and execution, and prioritize prerequisite/blocking work first.
6. **Worker + isolated executor** — safely implement approved task changes in the correct repository/path, run commands in a constrained environment, and recover from failures. This is the initial externally implemented capability; after it works, Factory should use its own worker for later work.
7. **Verifier + truthful progress** — verify behavior, tests, CI and delivery evidence. Progress is evidence-based; 100% means all acceptance checks are verified, never merely code-written or queued.
8. **Factory self-upgrade (last)** — only after items 1–7 operate end-to-end, run the first Factory-owned task to inspect, upgrade and verify Factory itself.

## After the core is operational

Factory itself owns the subsequent implementation and upgrades: repository browser, review/approval UI and safe adapters; Android Control; Memory Intelligence; Automation; UI/design; reasoning/research; app-closed background autonomy; and final APK. These are not separate manual workstreams and must not displace the eight core items.

## Execution invariants

- Work only on `feature/memory-intelligence`. Never touch Android `main`, Render, Hermes backend, or production Supabase.
- Do not auto-run imported repository scripts/workflows. Inspect and classify first; execute only through the constrained executor and policy.
- No auto-install or auto-merge. Preserve required owner approval for destructive, sensitive, or external side-effect actions.
- Do not expose secret values in logs, reports, comments, or HANDOFF. Record only safe file/line/commit metadata and redacted context.
- Do not claim implementation/test/CI/artifact/device success without exact evidence. A queued workflow is not a pass; an unverified phone action is not device-verified.
- Keep one canonical task journal/queue; do not create parallel queues for repository imports or product areas.
- Detect and fix newly discovered blockers/defects automatically when they are within authorized scope and prerequisites are met. Record dependencies, avoid infinite self-improvement loops, and never silently expand scope.
- Keep this HANDOFF updated after every meaningful implementation batch, including commit SHA, exact tests/CI runs and outcomes, changed files, remaining blockers, and next item.

## Initial live-state reconciliation — 2026-11-06

- Repository: `riyas433554-dot/vyom-android`; target branch: `feature/memory-intelligence`.
- Live branch HEAD verified from Git ref: `3e6d538a37b7aa61eb04f2640e8b6aa4fd455de3`.
- This HEAD's commit is documentation-only and its parent is `eb4abb7a8de1cd63caa80b15ec0d379a4be243a3`. The HANDOFF file on the live branch was fetched and reviewed before this update.
- Repository metadata currently reports `visibility=public`; this is not a private repository. Do not enable remote screenshot upload or place private user data in public issue/repository content. Repository visibility changes require owner authorization and are not performed by this task.
- The previously recorded exact-tip CI runs `42161654583` (Debug) and `42161654678` (Full CI) belong to parent SHA `eb4abb7a8de1cd63caa80b15ec0d379a4be243a3`, not current HEAD `3e6d538...`; both failed. No CI result for current HEAD is established by those runs.
- Known failures on parent SHA: malformed Kotlin test syntax in `SelfModificationClientIntegrationTest.kt:43-45`, `VyomFactoryDrainCoordinatorTest.kt:22`, and unresolved `VyomFactoryRepositoryCatalog` references in `VyomFactoryRepositoryCatalogTest.kt`. No APK/artifact was produced. These remain recorded blockers; no fixes have yet been made.
- Historical secret scan reported 169 broad Discord-pattern matches, 3 credential-assignment matches, and 4 OpenAI-style matches. Four OpenAI-style matches and two current credential assignments have been classified as test fixtures. The third historical credential assignment is unreconciled. The 169 historical Discord matches remain UNCLASSIFIED because the original redacted per-file/per-line/per-commit inventory was not found. No confirmed REAL_LEAK has been established; this is not proof that history is clean. Do not rotate credentials absent a confirmed real leak, and do not disclose values.
- Core item 1 (audit/HANDOFF synchronization) is **in progress**; this entry records the live state and supersedes prior phase-gate instructions. Core item 2 remains **blocked on missing historical scan inventory**. Items 3–8 have not yet been re-audited against source in this session; their prior statuses below are historical evidence, not current verification. Reconcile each before changing it.
- No application source, Android `main`, Render, backend, or production Supabase was changed in this update.

---

# VYOM ANDROID — ACTIVE HANDOFF

Last updated: 2026-11-05
Priority: **VYOM FACTORY FIRST**. Full previous ledger remains preserved in [HANDOFF_HISTORY.md](HANDOFF_HISTORY.md).

## Branch and guardrails

- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Current source tip before this handoff refresh: `684bd735bfa5ad4de839e3675d1df740165f6996`. Earlier Full CI `41968349353` and Debug CI `41968349286` passed on `efb16a2f906f3c59191c9c428121bc6dc34fe99e`, but do not validate the newer Factory repository path-discovery changes. Latest exact-tip runs `42038390185` (Full CI) and `42038390172` (Debug) both failed before runner allocation; retry attempt 2 again reported `runner_id=0`, empty runner name and `steps=[]`. No Gradle/test command ran. The following handoff/docs commit also requires fresh exact-tip CI before any APK 1 authorization.
- Do not touch Android `main`, Render, Hermes backend, or production Supabase.
- Inspect this handoff and history before work; do not repeat completed implementation.
- APK 1 gate follows the Factory-only plan below: Factory workflow end-to-end, only the execution/update safeguards Factory itself requires, exact-head CI green and artifact integrity. Stage 6 owner acceptance is a hard prerequisite; the signed CI candidate is not final APK 1. Unrelated Android/product features and lifecycle work outside the Factory workflow are excluded.
- Report only verified tests, CI, artifacts, and physical-device results.

## Scope lock — Factory is the only APK 1 deliverable (2026-11-05)

- APK 1 contains **Vyom Factory only**: complaint intake/register, triage, priority, routing, guarded execution/proposals, verification, recovery, evidence and audit. No standalone product-area feature is to be implemented for APK 1.
- The canonical complaint register MUST accept complaints from every Vyom area: control, Android control, memory, design, personality, repo, skills, schedule, voice, update, WhatsApp, Play Store and floating orb. A generic `complaint:` prefix is sufficient; it must not require "Vyom Factory" wording.
- Acceptance into the register is not authorization to implement that product feature in APK 1. Non-Factory feature requests remain visible in the canonical issue register, receive `backlog-after-factory`, and are not routed as APK 1 implementation work.
- Priority policy: control/Android-control and update = P0; memory and design = P1; voice = P2. Other areas retain their existing issue priority unless the owner changes it. New complaints default by area: control/update P0, memory/design P1, voice P2, other named product areas P2, unknown/other P3.
- Preserve the existing Factory-only APK 1 gate and Stage 1–7 order below exactly. The register is cross-product; the APK 1 implementation scope is not.
- Previously `not_planned` product issues have been reopened and labeled `backlog-after-factory`, including WhatsApp, Play Store and #52 update. Their issue Area fields now match the product domains; owner priority tiers are applied. Do not implement those backlog features in APK 1.
- Screenshot evidence requirement: capture/store screenshots in app-private storage. Upload/attach to the originating GitHub issue only after the owner explicitly taps **Attach**. Repository must be private before remote screenshot attachment is enabled. Never silently upload screenshots.
- Branch and environment guardrails remain: only `feature/memory-intelligence`; never touch Android `main`, Render, Hermes backend or production Supabase. No auto-merge or auto-install of unreviewed proposals.
- Report only verified tests, CI, artifacts and physical-device results. Keep `docs/HANDOFF.md` updated after each meaningful batch.

## Current controlling plan — Factory-first (2026-11-04)

This section supersedes older APK gates and Factory ordering text below where they conflict with the user's latest instruction.

- APK 1 implements **Vyom Factory only**; the complaint register accepts every Vyom product area so all later fixes/features enter through Factory.
- Stage 1 remains complaint intake, triage, canonical register, priority ordering, fix routing, evidence and verification lifecycle. Generic Vyom complaints are accepted and classified; non-Factory feature work is parked as `backlog-after-factory` and is not implemented in APK 1.
- Keep the existing Stage 1–7 order and APK 1 gate unchanged. Implement only capabilities directly required for Factory's own complaint-to-fix workflow in APK 1.
- APK 1 may be delivered only when the Factory workflow works end-to-end and its required safety controls are verified, with exact-HEAD CI green and artifact integrity checked. Do not block on post-APK backlog features or lifecycle work outside Factory.
- Keep work only on `feature/memory-intelligence`. Never touch Android `main`, Render/Hermes backend, or production Supabase.

### Cross-area complaint intake + backlog restoration — 2026-11-05

- Owner changed the register boundary: all Vyom areas are accepted as complaints, while APK 1 remains Factory-only. A generic `complaint:` prefix no longer requires Factory wording. This is registration only; post-APK-1 product work is not implemented or selected for APK 1.
- TDD regression tests added in `VyomFactoryComplaintRecordTest`: memory/design/personality/repo/skills/schedule/voice/update/control/Android-control/WhatsApp/Play Store/floating-orb acceptance, generic `other` fallback, Factory-prefixed workflow classification, and priority tiers. Test commit: `17563e29edbed44dcd1ed7c17493bcae9dc7d93f`; taxonomy expectation adjustment: `e4bcb48d7989ce8a8745efd9beecc06b72db1fe0`.
- `VyomFactoryComplaintRecord` no longer rejects WhatsApp/Play Store or requires explicit Factory context. It classifies named product areas and defaults unknown complaints to `other`. Priority defaults: control/Android-control/update P0; memory/design P1; voice and other named areas P2; unknown/other P3. Implementation commit: `95cea4e0ad9745bcfb8f221a6f549df52bef34fd`.
- MainActivity now reports only malformed/empty/overlong intake errors rather than an out-of-scope rejection. Commit: `74a4e2da321bb8ac7347573fdbbd1df6448e092c`.
- Termux `vyom-complain` accepts all requested area values and derives owner priority defaults when `--priority` is omitted. Shell regression coverage added for area acceptance and defaults. Commits: `326b4bf637dd65e458e4ebb7fa141a38fe29cbb0`, `3ef5f997067dd344a013b0de07ea62fdff216c65`, `f2c27529311596b946ab437b8cee7f70c0b190ea`.
- Reopened **70** previously `not_planned` product issues: #14–15, #17–37, #39–42, #44–46, #52–53, #55–70, #72–82, #84, #89–95 and #97–99. Verified search returns 70 open issues carrying `backlog-after-factory`. Their Factory metadata status is `paused`, so they remain visible in the register but cannot be claimed by the APK 1 worker. #52 update is P0; memory/design P1; voice P2; control priorities are P0; other seeded priorities retained unless mapped by the owner policy. Factory active issues such as #16/#38/#43/#47–51/#54/#71/#83/#85–88/#96 were not reopened or relabeled.
- `docs/OWNER_ACTIONS.md` now marks private repository conversion as **OWNER ACTION NEEDED** and defines app-private screenshot storage plus owner-tapped **Attach** as the required contract. Screenshot receiver/Attach is source-wired with app-private storage and private-repository checks; device acceptance remains pending and no screenshot was uploaded.
- The current repository is still public until the owner completes the documented visibility change. Do not register real private data or enable remote screenshot upload before then.
- No product-area feature implementation, no auto-merge/install, no Android `main`, Render, Hermes backend or production Supabase changes. No APK delivered.
- Code/test commits: `17563e29edbed44dcd1ed7c17493bcae9dc7d93f`, `95cea4e0ad9745bcfb8f221a6f549df52bef34fd`, `74a4e2da321bb8ac7347573fdbbd1df6448e092c`, `326b4bf637dd65e458e4ebb7fa141a38fe29cbb0`, `e4bcb48d7989ce8a8745efd9beecc06b72db1fe0`, `3ef5f997067dd344a013b0de07ea62fdff216c65`, `f2c27529311596b946ab437b8cee7f70c0b190ea`, `7bcb785b5b252880b2a7425f233021821e6a3b1b`. Documentation commits before this refresh: `1cdecc3d880469ad9ee51e1c459f8d9af9663eb8`, `2b9ec38fd722fbb7678678eb76771407bf52ef97`, `4642b789db304d948359e3afbe7b00eed7037371`. Exact-head CI and test results must be checked; source edits and test additions are not a passing-test claim. Exact-head CI and test results must be checked; source edits and test additions are not a passing-test claim.

### Exact-head CI and artifact verification — 2026-11-05

- Code/test source SHA `c8aa73dc7b99fdd50e0025a7ceebd059bcb79b67` was verified by Full CI `41963331282` = SUCCESS and Debug CI `41963331348` = SUCCESS. Both workflow runs completed on that exact SHA.
- Full CI: Termux adapter syntax PASS; Termux complaint/priority tests PASS; secret scan PASS; unit tests + debug APK PASS; package/checksum/upload PASS. Artifact `13385984320` (`vyom-phase6-c8aa73dc7b99fdd50e0025a7ceebd059bcb79b67`) ZIP SHA-256 `179988fda6813e6527169a0747c54b27d99942c1322997dad37811ff4cbfb0c7`, independently matches GitHub digest.
- Debug CI artifact `13386517377` ZIP SHA-256 `08f94e06389b3442f11713279113f0b8fba8f8e0c2296dba70b05b4aa5b0befa`, independently matches GitHub digest. Contains debug APK and release-signed APK. Debug APK SHA-256 `2e1e52d9cafbed904a565f94976e109e160c9881d2bdc4e699a4a2451c2f3385`; signed APK SHA-256 `396c75414be2f23e169de7b9cc242b5f5081e4b2b278f4aa45c80481eb35ec37`; both match embedded checksum files.
- `build-info.txt`: versionName `0.3.1-ci.1375`, versionCode `1963331348`, commit `c8aa73...`, `releaseSigning=true`, certificate SHA-256 `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`. Certificate report confirms APK Signature Scheme v2 and the expected signer digest.
- Content update demo result PASS; presence PASS; performance probe PASS (content-file read timing only, not WebView render latency). Rollback XML: 8 tests, 0 failures, 0 errors.
- Signed CI release published: [vyom-ci-c8aa73dc7b99fdd50e0025a7ceebd059bcb79b67](https://github.com/riyas433554-dot/vyom-android/releases/tag/vyom-ci-c8aa73dc7b99fdd50e0025a7ceebd059bcb79b67).
- This verifies the code/test source SHA above. Documentation-only commits `f38e159c1f71830f4ccd57839ed5564786463dd3` and this HANDOFF refresh follow it; re-run exact-tip CI before using any newer tip as APK 1 evidence.
- APK 1 is **not authorized for delivery yet**: exact-head source/CI/artifact gates are now green, but Stage 6 owner actions remain. Screenshot receiver/Attach is source-wired but not physically verified. No APK 1 delivered.

### Latest source audit and CI (2026-11-04)

- Latest source branch tip before this handoff refresh: `b24063de806f451e993f29d96a8c634a8c806a1e`. `docs/OWNER_ACTIONS.md` now includes guarded complaint-to-code proposal acceptance steps. Proposal pipeline failures/timeouts are also recorded back to the originating complaint (without exposing exception message/request text); cancellation/back-dismissal clears the pending issue link.
- Historical pre-change classifier note: generic `behavior` did not imply personality; explicit tone/persona terms did. The old Factory-only rejection boundary and `327e0e4...` voice-intake strict-scope fix are superseded by the 2026-11-05 cross-area acceptance change recorded above. Generic Vyom complaints now register without Factory wording.
- Priority order is shared by Factory UI, Automations and worker selection: P0→P3, newest GitHub update first within each tier, issue number as deterministic tie-break. `ye complaint pehle #N` promotes only active open/in-progress complaints to P0.
- Complaint lifecycle: open issues can START, paused issues can RESUME, and DONE is blocked until a named Verification probe has a recorded PASS for that exact probe. VERIFY records owner-reported device evidence (PASS/FAIL); changing the probe invalidates old PASS. This is manual owner attestation, not autonomous device verification. Existing legacy completed issues remain editable without requiring a newly created probe event.
- Worker state mismatch fixed: START changes metadata to `in_progress`; worker session/coordinator/runner/Factory eligibility accept GitHub-open issues with metadata status `open` or `in_progress`. Regression tests cover session claim, coordinator claim and runner execution. Commits: `d05a800da6c0307a9ce29a75a9019d63874f0223`, `957c0f38742d476118c20d7fc738c5b074a0234f`, `db45a8b3e9c864487d2ef80cfdce3629c35dbde6`, `ed93a42c699bd12f7eeecf0277141aeb18be4de8`, `e5d172090b1cc65a8bbc40a36de2dbbd2e31f4b4`, `c1723c68af2ddf5211d5c0ddf53d3c7c7d747531`, `22c19b21a880978b3127422f185e782d9438af06`.
- Probe closure policy is tested for exact probe identity, failed probe, unassigned probe, changed probe and legacy completion. Owner promotion rejects closed/cancelled complaints. Commits: `839ca96886fc7cbf91bbe79f05a66fb2c1f80eda`, `7e961916328979a7ea39b5b9faa4d4ee9fb2a430`, `cbb38396ce3731dc8cda6ea033d6ba4a171eb616`, `0a15c1b589ebea80076044fe00f6dc6c983a3951`.
- Worker outcomes write truthful comments to the canonical issue: verified adapter postcondition while leaving complaint open, or FAILED with category and no success claim. Skipped/fenced outcomes do not write misleading comments. Commits: `c0e1129273c9379b6edb9e205cc7f6895f5de33b`, `c9f33b987325a269e9fb6985e83b1dff4fb1ce6c`, `12636067a40120714f40fe609e032fd6d681c711`, `e7f61354f97b68ffc27479844ce5d9f138dd39d1`, `b258df13a1a8c61a545b564aa18993b0bcdabf93`.
- **Important Factory fix path now wired in source:** active complaint cards expose `PROPOSE REVIEWED CODE FIX`; issue title/body and issue number are included in a bounded proposal prompt after removing hidden metadata; sensitive title/body is rejected; user must choose an allowed Kotlin source path; existing policy rejects protected policy/control/update/signing/accessibility files; the model produces exact minimal edits; user sees the diff before PR creation; PR targets only `feature/memory-intelligence`; both required CI workflows are awaited; PR URL and exact CI SHA are written back to the originating issue where GitHub comment permission allows. Regression tests cover issue identity, metadata omission and sensitive title/body rejection.
- This is **not autonomous self-repair yet**: one Kotlin file per proposal, user chooses the file path, model-generated changes need owner diff review and PR review/merge; CI success on an unmerged PR does not produce an installable release. No auto-merge or auto-install. Proposal is deliberately not allowed to modify MainActivity, ActionEngine, accessibility, policy, STOP, audit, signing or update-security files.
- Stage 1 source-wired intake: GitHub Issues canonical register with schema/idempotency; Factory-scoped chat/voice complaint intake; sanitized/deduplicated Factory Diagnostics failure intake; Factory runner infrastructure failures only (generic chat task failures are excluded); Termux command with shell/schema tests; Automations shared issue visibility; seeded Factory-only requirement matrix.
- Stage 1 remains **incomplete**: Termux install/auth/idempotency smoke test and physical phone acceptance are owner-side; screenshot share-sheet receiver/Attach is source-wired with app-private storage and fail-closed private-repository upload; repo privacy and phone acceptance remain owner actions; automatic new-complaint capture is proven only for chat failures, while Factory worker failures are comments on the existing issue; GPT/Claude/Codex source acceptance is not device-verified. Public issue bodies are visible to everyone; evidence must be sanitized.
- Complaint intake starts with UNASSIGNED probe. Owner must edit it to one exact observable probe, START the complaint, check the real phone, then record sanitized PASS/FAIL evidence. This is manual owner attestation, not autonomous device verification.
- A17 has a versioned safe execution envelope, lease/journal/retry/STOP/foreground-consent controls, one process-wide runner, and one concrete `set_media_volume` adapter. General Android actions and autonomous repository fixes are not yet implemented.
- A18 has signed package verification against the installed APK signing certificate, file hashes/path allowlist, inactive-slot staging, presence probes, atomic activation only after probes pass, rollback on failed probe, WebView presence-failure rollback, and crash-triggered rollback. CI signs the package with the persistent Android release keystore alias and self-verifies the detached signature. Optional PERFORMANCE probe measures content-file read time, not actual WebView render latency.
- CI: last observed Full CI run `41838529328` was pending on ancestor SHA `a81648d72d03107d0cae2ef870f5d529539112d3` and had no jobs yet; ancestor Debug `41838467957` was queued on `d68ab40279151eee3a28d0cf963dbd0dc5d6db6f`. Newer source commits supersede both. Exact-head Debug + Full CI for this checkpoint and the following handoff commit must be checked. Do not claim tests passed or APK built until both finish successfully.
- No final APK has been delivered and no physical phone acceptance is claimed.

### Next work (mandatory order; stages 1–7 remain locked)

1. **CI gate — COMPLETE on source SHA `efb16a2f906f3c59191c9c428121bc6dc34fe99e`.** Full CI `41968349353` and Debug `41968349286` both SUCCESS. This handoff/docs-only commit must receive a fresh exact-tip CI pair before APK 1 authorization.
2. **Lifecycle/parity gate — COMPLETE on exact source SHA above.** Existing `VyomFactoryLifecycleIntegrationTest` verifies START → worker claim/run → issue event/result → owner probe PASS → DONE. Existing Factory/Automations ordering and paused-task exclusion tests are retained; do not duplicate them.
3. **Stage 1 intake/evidence gate — SOURCE COMPLETE; OWNER ACCEPTANCE PENDING.** Cross-area complaint intake, sanitized Diagnostics and non-chat worker failure routing, external-agent source identity tests, app-private screenshot share/Attach and private-repository fail-closed checks are in source. Owner must complete Termux smoke, phone acceptance, and live external-agent checks. No real screenshot upload while repository remains public.
4. **Proposal safety gate — COMPLETE on exact source SHA above.** CI executes regression coverage for safe-path rejection, PR creation failure/uncertain outcome, exact-SHA CI failure/timeout, issue-comment failure, and no merge/release/install. Do not auto-merge or auto-install.
5. **A18 Factory-required rollback gate — COMPLETE on exact source SHA above.** Exact-head signed demo PASS, presence PASS, content-file performance probe PASS; rollback report has 8 tests, 0 failures, 0 errors. This is not WebView render-latency evidence.
6. **Owner acceptance — BLOCKED ONLY ON OWNER ACTION.** The repository API still reports `visibility=public`. Follow `docs/OWNER_ACTIONS.md` in order: make repository private; verify token permissions; install and run the signed exact-head CI candidate manually (never auto-install); complete voice/Diagnostics/Factory-runner complaint, screenshot Attach, Factory/Automations ordering, verification-probe, Termux idempotency, skill-import and GPT/Claude/Codex source-identity checks. Record actual results before marking Stage 6 complete.
7. **Final APK 1 — NOT AUTHORIZED YET.** After Stage 6 owner acceptance, refresh this handoff with evidence, run Full + Debug CI on the resulting exact HEAD, verify the final signed APK checksum and pinned certificate, and only then deliver APK 1. Current CI candidate release is not APK 1.

## Superseded legacy Factory order — historical context only; DO NOT FOLLOW

1. Persistent task store/queue — implemented and tested.
2. Worker claim + lease — device-local lease store, coordinator, fresh-snapshot foreground claim session implemented.
3. Heartbeat — intentionally deferred: the only production adapter is a synchronous media-volume set with readback, too short to need lease renewal. Add heartbeat only when a real bounded long-running adapter exists.
4. Retry/backoff — bounded idempotency-gated policy, durable schedule/attempt counter and deterministic bounded jitter implemented. Foreground RUN NEXT consumes only due retries after fresh GitHub fetch, fresh lease, STOP checks and a new one-time UI approval. No automatic/background retry scheduler.
5. STOP/pause — PARTIAL: foreground STOP button cancels pending consent or sets a cooperative stop flag; runner/adapter check before side effects. Activity pause invalidates the active run using the captured foreground-gate generation. The generation is captured synchronously on the UI thread at RUN NEXT tap, before executor queueing; the adapter checks the same generation before consuming approval and immediately before the volume side effect. A late STOP cannot rewrite verified success as failure. Journal STOP remains lease+attempt fenced. Added `VyomFactoryMainActivityLifecycleTest` under androidTest to exercise the real MainActivity pause/resume callbacks, verify the old generation stays invalid after resume, and verify pending one-time approvals are cleared on pause. Added AndroidX instrumentation runner dependencies and an API 35 emulator step to the Debug workflow. Exact-head CI/instrumentation and physical phone acceptance are pending.
6. Crash recovery — PARTIAL: runner marks expired/replaced nonterminal attempts INTERRUPTED before selecting the next task; never replays them. MainActivity.onCreate now invokes conservative startup recovery, which interrupts only attempts whose exact lease is no longer current; live leases remain untouched, and journal corruption/commit failures fail closed without overwriting history. Added a regression test for expired-vs-live lease recovery and idempotent second startup pass. Still no background scheduler; physical process-kill/relaunch acceptance remains pending.
7. Bounded workers — one process-wide execution slot is now enforced; actual parallelism above one remains disabled until adapters declare/lock shared device resources and concurrency tests prove isolation.
8. App-closed execution verification — pending physical Android testing.

## Current verified implementation

- GitHub Issues is the shared registry; Android SharedPreferences is a device-local inbox and lease store.
- Queue validation, priority ordering, dedupe, reconciliation and fail-closed corruption/commit handling exist.
- Multilingual intake is schema validation only, not semantic multilingual interpretation.
- Lease fencing is local to one app process/device; it is not distributed CAS.
- Claim session refreshes GitHub before each claim and reconciles only issues whose GitHub state and metadata status are both `open`.
- Factory dialog exposes CLAIM NEXT / RELEASE CLAIM and RUN NEXT VERIFIED ACTION. The foreground runner is wired to the UI with one concrete allowlisted Android adapter (`set_media_volume`) and an explicit one-time confirmation prompt. There is still no background scheduling or autonomous worker.
- Journal entries now persist the exact operation key. Runner replay checks are scoped to issue + operation; legacy entries with unknown operation identity block further automatic work on that issue and require manual review.
- `VyomFactoryExecutionRunner` performs one foreground claim, re-fetches the issue, verifies open status + source identity + valid execution envelope + registered adapter, begins a fenced journal attempt, checks lease/STOP before dispatch, records terminal result, and schedules transient retry only when the adapter declares idempotency.
- `VyomFactoryExecutionControl.shouldStop()` is passed into adapters; adapter contract requires bounded execution and cooperative checks before each side effect. The production registry now registers only the media-volume adapter from the foreground RUN NEXT flow; repository changes and every other Android operation remain unsupported.
- Retry schedule cleanup after terminal success is best-effort and reported separately; cleanup failure does not rewrite a successful side effect as FAILED.
- `VyomFactoryRetryPolicy` retries only known transient categories, only when the concrete adapter explicitly declares idempotency, caps at three failed attempts, uses 30s then 120s base backoff, and saturates timestamps safely.
- `VyomFactoryRetryScheduleStore` durably persists per-issue/per-operation failed-attempt counts and next-eligible timestamps, applies deterministic bounded jitter (0–25% of base delay), exposes due entries, and supports clearing on verified completion. It fails closed on malformed data/commit failure and caps at 500 entries. The store does NOT run work itself; the foreground RUN NEXT consumer filters for due schedules, then the runner fetches fresh GitHub state, acquires a current lease, checks STOP/consent and invokes only the registered idempotent adapter.
- No autonomous worker or WorkManager scheduler exists. The foreground runner can execute only the one bounded media-volume adapter after a local user tap; the journal remains persistence/state machinery and does not itself execute or cancel work.

## Execution routing work just added

Architecture: [VYOM_FACTORY_EXECUTION_ARCHITECTURE.md](VYOM_FACTORY_EXECUTION_ARCHITECTURE.md), commit `c82f95d6b9a0146a1ee63c9de7240abc8750d832`.

- `VyomFactoryExecutionEnvelope` parses a separate versioned `VYOM_FACTORY_EXECUTION` marker.
- Existing schema-1 task metadata, missing/malformed/duplicate/unknown/oversized execution envelopes, invalid operations, and missing arguments fail closed to `manual_review`.
- Explicit supported targets: `android_action` and `repository_change`; operation identifiers are bounded and arguments must be a bounded JSON object.
- Original request is carried separately and bounded. Parser does not execute anything.
- `VyomFactoryExecutionAdapterRegistry` routes only to one adapter registered for the exact target. Duplicate adapters are rejected; manual-review tasks never route; missing adapter returns Unsupported; adapter exceptions propagate and cannot be converted to success. Adapters now declare `isIdempotent` and receive cooperative `VyomFactoryExecutionControl`.
- The only concrete adapter is `android_action:set_media_volume`, registered by the foreground RUN NEXT flow after the UI obtains one-time consent. `repository_change` and all other Android operations remain unsupported. A task must not be marked completed merely because it parsed or routed.

## Recent commits and CI

- Debug run `41810491911` on `702ad5677be31738b1241ef06122d07df02f2b3a` failed at test compilation: missing registry `control` argument and outdated fake-adapter named parameters. Corrected in `b2234a4eace6439fce140ea9a1b1b869803ce335` and `387ed8a02b9c17fb8d468f87e7e6b8d9d63cee3c`.
- Journal operation identity: `537dee11a0b15e074aa947f5715103a4aa2d91cc`; runner operation-scoped replay guard: `379bb9b61fdb9697976d1525e1a02fdd8894e747`; legacy fail-closed guard: `9c9ea8f2b886705f2b2d6d603598fc9c52ef920e`; tests: `a254191e8c7341c33904d326f211d328422dffee`, `284789f16619e935b11782c43487a8353a32f130`.
- Handoff HEAD `bba74118207f2a649ebc6e0cb6e0b00c4c1a04f4`: Debug `41811035830` IN_PROGRESS; Full CI `41811035919` PENDING at last poll.

- Historical exact HEAD `56efc35772dfbb538818559394f82003919d0285`: Debug workflow run `41787089042` SUCCESS; build/test, APK packaging and upload steps all SUCCESS. This is not current source validation.
- Artifact from that source-equivalent HEAD: `vyom-phase6-56efc35772dfbb538818559394f82003919d0285`, artifact ID `13302535151`, SHA-256 `0e335fcfaeac3b44b3ed241208a1cc991437f5cc961d924a5d768bd25d988d6b`. Not a final release.
- Architecture doc: `c82f95d6b9a0146a1ee63c9de7240abc8750d832`.
- Envelope contract tests: `c131b68c7ddca1b6d32746c58aaaef1496b9c106`.
- Envelope parser implementation: `919a13d1fea3e7661a8df13dc040b8e397f1dfcc`.
- Adapter routing tests: `f0b6f288dedcb25c143c8c9e9dd5f36c406dd9dc`, expectation correction `719d75b4f8a13be579120add51f5eafbbcde36d8`, method alignment `12d28eccc4496a6bfc396177a72d5259e39043c7`.
- Adapter registry implementation: `a3c1e0284aa519a98f6a3530d62dcad7e39d9c73`.
- Additional parser edge tests (duplicate markers, oversized arguments, target-specific field mismatch): `de9eb5fa64a12b4def4a58ef1c772909d9496fee`.
- Debug CI run `41788314857` on `aad88b0d58c563c6729a5516e11defcaab016306` failed only at `VyomFactoryExecutionAdapterRegistryTest.routesOnlyToAdapterMatchingExplicitTarget`: test expected `android_action:open_settings`, while the recording adapter returned `android_action: open_settings`. Build and Kotlin compilation succeeded; 345 tests ran, 1 failed.
- Corrected the test expectation in `99f4a121cd52fa9faf70df168a35e3df51fcd0c2`.
- Added durable execution journal contract tests (7 cases) in `06ea7d7fb9d6daec39b68ae54d20b627778c7a0e`.
- Added a shared process lock for lease/journal fencing (`e4fac5015e55fdd4cf5ab759b63631d660bc01cc`) and journal implementation (`e58ad2e3469625ed8b2071e41809b673ba2c6e11`); follow-up constructor/stale-lease and Kotlin declaration-order corrections: `2486e5131b4aeb7793b2042280d2ae99ff6e92bd`, `62b51a69e50fd04fafe9903889b9b3ed44dae15d`, `e9bb4c38d842676c1f27e719b343b9b23504d75f`.
- Journal persists claim/running/stop-requested/succeeded/failed/interrupted attempts, fences state writes against the exact current unexpired lease within the same app process, rejects duplicate terminal writes, bounds results/errors, and marks expired attempts interrupted without replay. It is not cross-device/multi-process atomic.
- CI on `c7c59e36ee10cb76974e4618d3c3e367fd3f13f7` failed in both Debug `41804566081` and Full `41804566058`: Kotlin compile errors showed three stale `PROCESS_LOCK` references in lease `renew`, `release`, and `active`. Earlier commits in the same batch also exposed these compile failures; no tests ran.
- Fixed all remaining lease lock references to use `VyomFactoryExecutionLock.LOCK` in `bffee52a8d0859ec7aa99ebde7439cbc889d1925`.
- Exact HEAD `09028f1bbec6fba4f6cd80b78907fa5722fed2e7`: Debug `41805255577` FAILURE and Full CI `41805255325` FAILURE. Both compiled Kotlin and built the debug APK, then failed the same unit test: `VyomFactoryExecutionJournalTest.terminalAttemptRejectsDuplicateOrConflictingWrites` at line 59. 352 tests ran, 1 failed; packaging/upload skipped. The test attempted `succeed()` while the journal state was still `CLAIMED`, but implementation/ADR requires `CLAIMED -> RUNNING -> terminal`.
- Corrected that test to call `markRunning()` before `succeed()` in `e58a843807795d2a544aff8f049f40e28e00aa76`. This is a test-fixture correction; production journal behavior was not weakened. Exact-HEAD CI for this correction is pending.
- Exact HEAD `4da63479f400ae000348521431bbd02a9181867e`: Debug `41806744377` SUCCESS and Full CI `41806744491` SUCCESS. Both runs checked out that exact SHA.
- Journal edge-case RED tests added in `7b44842a4cf5a6e8f8b2da731e77238377be70fc`: reject a second nonterminal attempt for the same issue/lease; allow a new attempt after terminal completion; require STOP to target a running attempt using the exact lease token; stale attempts become INTERRUPTED when a successor lease begins.
- Journal implementation hardening: `0ca4076e156649882b9e79c3e9047a10195891f2`; stale-attempt test expectation corrected in `759bbbaeb0437a75020a335f17b9b4b0e1dd93f7`; duplicate-active conflict now uses an IllegalStateException in `0f9e9c02f7015e7dab2ea1c366e56f08325a5809`.
- Current journal behavior: only one nonterminal attempt per issue/lease; when a successor lease takes over, previous nonterminal attempt is fenced and marked INTERRUPTED, never replayed. STOP now requires `(lease, attemptId, now)`, verifies the active lease token and exact attempt, and only transitions RUNNING → STOP_REQUESTED (repeated request is idempotent). CLAIMED work cannot be mislabeled as running/stopped.
- Journal storage now has a hard 500-entry ceiling. It fails closed rather than deleting audit history; this is a deliberate safety cap, not an automatic archival policy.
- Exact current HEAD `0f9e9c02f7015e7dab2ea1c366e56f08325a5809`: Debug `41807931917` IN_PROGRESS and Full CI `41807931989` PENDING at last poll. Test-only commit `7b44842...` workflows `41807848949` / `41807848960` were still IN_PROGRESS at last poll; later implementation commits superseded those runs. Do not claim the journal edge-case suite green until exact-HEAD CI finishes.
- Earlier Full CI `41786551264` failed before compilation due Maven Central HTTP 403; this was dependency resolution infrastructure, not a demonstrated source/test failure.

## Immediate next actions

1. Verify Debug and Full CI on the handoff-updated exact HEAD; fix source/test failures from real logs.
2. Implement one narrow concrete adapter only after defining explicit allowlisted operations, argument schemas, permission/confirmation gates, and bounded timeouts. Do not add shell execution.
3. Add UI/foreground invocation only after the concrete adapter is tested; due retries must re-fetch GitHub, acquire a fresh lease, check STOP, and require adapter idempotency.
4. Add heartbeat only during real active progress; then complete crash recovery, bounded parallelism, and physical app-closed tests in canonical order.

No backend, Render, production Supabase, or Android `main` changes. No physical Android app-closed test claimed.


## Latest CI failure and test-fixture corrections (2026-11-04)

- Exact HEAD before the test fixes: `ee7e8e71f692297db887ff47c0edca8ba850117f`.
- Debug run `41811152403` and Full CI run `41811152418` both checked out that exact HEAD and failed only in the unit-test task; Kotlin compilation and APK compilation/package work completed, but artifact upload was skipped because tests failed.
- Real job logs identify two failures: `VyomFactoryRetryScheduleStoreTest.persistsBoundedRetryScheduleAndReturnsOnlyDueEntries` assumed the first retry would be due at the base 30-second timestamp, but the store intentionally adds deterministic 0–25% jitter; its assertion now checks the persisted `nextAttemptAt` boundary. `VyomFactoryExecutionRunnerTest.idempotentFailureWaitsUntilPersistedRetryIsDue` used a stale released claim on its third invocation; the fixture now reacquires a fresh lease after a skipped/not-due run and advances to the exact persisted retry timestamp. The explicit stale-lease test continues to exercise a deliberately stale token.
- Test-only commits: `20013bb35cf1efbf349b2dc2f643519f66262048` (runner fixture claim reacquisition) and `8a913c21c8d2f7cdead7a9f0d04c360ad05e0f09` (jitter-aware due-time assertion).
- These are fixture/assertion corrections, not production behavior changes. Exact-HEAD CI after the handoff update is still required; do not claim green until both workflows pass on the same final SHA.

## Immediate next actions (updated)

1. Run and inspect Debug + Full CI for the latest handoff-updated HEAD; fix any further real test failures.
2. Once CI is green, inspect the runner's lease release/terminal lifecycle and design one explicit bounded adapter with a narrow allowlist, argument validation, confirmation/permission gates, STOP checks and truthful outcome verification. No shell adapter.
3. Keep heartbeat pending until a real bounded executor can demonstrate active progress; then proceed in canonical order with crash recovery, bounded parallelism, and physical app-closed testing.


## Terminal claim lifecycle batch (2026-11-04)

- Added regression tests requiring the exact worker claim to be released after a durable terminal SUCCEEDED or FAILED journal record. Test-first commit: `0d85f1af0cefcc1a97eeb5ed44bbe0152c7c92a4`.
- Runner now releases the exact claim after terminal success/failure is durably journaled. Release is token-fenced and best-effort; a release/cleanup failure never rewrites a terminal side effect as failed. It does not release on a fenced journal write or before a terminal journal transition.
- Retry schedule persistence after a durable FAILED journal record is now best-effort. If schedule persistence fails, the journal still records failure and the runner releases its claim; missing retry schedule causes future execution to fail closed rather than replay. No retry is claimed unless a durable schedule exists.
- Implementation commit: `3669b69aef075eb7d2485b5233134cc24a8a132d`.
- Exact HEAD for this batch: `3669b69aef075eb7d2485b5233134cc24a8a132d`; Debug `41813282596` and Full CI `41813282641` were started on this exact SHA. Both remain pending/in progress at the time of this handoff update; no green result or APK artifact is claimed.
- Earlier long-running CI runs on `22031390e296255f3090273f6c3239aa9f0260ee` did not report completion when polled. Their job remained at the Gradle build/test step. They are superseded by the exact-head runs above; verify their final status only as useful for diagnosing runner congestion.

## Next actions

1. Poll Debug `41813282596` and Full CI `41813282641`; both must pass on exact HEAD `3669b69aef075eb7d2485b5233134cc24a8a132d`.
2. If green, continue canonical order. Before a concrete Android adapter, inspect full ActionEngine + DeviceActionExecutor behavior and add a strict adapter contract with a very small allowlist, argument validation, permission/confirmation and foreground constraints, STOP/lease checks before every side effect, and post-action verification. No arbitrary action passthrough and no shell.
3. Keep heartbeat, WorkManager, crash recovery, bounded parallelism and app-closed claims pending until a real bounded executor exists and is tested. Physical app-closed verification remains user/device-side pending.


## Adapter postcondition verification contract (2026-11-04)

- Review of `DeviceActionExecutor` found that several operations return `Pending verification:` (for example opening Settings, dialer/SMS composer, alarm/timer UI and torch callback not yet observed). The previous runner accepted any returned string as terminal success. Registering those actions directly would incorrectly mark an unverified request complete.
- Added mandatory `VyomFactoryExecutionAdapter.isVerifiedResult(result)`. Every concrete adapter must independently assert its requested postcondition; the registry exposes this check and the runner refuses `SUCCEEDED` unless it returns true. Unverified outcomes are journaled as `FAILED/execution_error`, are not automatically retried, and the terminal lease is released. This requires every future adapter to implement verification explicitly.
- Regression test verifies an adapter returning an unverified result cannot write terminal success or schedule a retry. Registry fake adapter also declares its verification rule.
- Test commits: `815a016cf08a74f3a4989f13bd4f96178a02822e`, `4bbbcf653663bd091dd58090dfdbf8c5de440e10`; interface/registry implementation `4faf13b1ab96f3e8ca35970c8d24db6432e71a16`; runner enforcement `130a750cfd0b47a2c880e84e4a3864c15e76c914`.
- Exact branch HEAD at this entry: `130a750cfd0b47a2c880e84e4a3864c15e76c914`. Debug run `41813499913` and Full CI run `41813500083` are running/queued on this exact SHA. No test result or APK artifact is claimed yet.
- No production adapters are registered and runner remains unwired from the Factory UI. This is a safety-contract improvement, not a claim that Android actions can now execute.

## Next implementation gate

1. Verify exact-head CI for the adapter verification contract.
2. Implement a narrow foreground Android adapter only after its exact argument schema, confirmation model, Activity foreground gate, STOP checks and verified readback are covered by tests. Current `DeviceActionExecutor` pending-verification outputs must never be accepted as completed.
3. Only after that adapter is real and invoked through a user-visible foreground flow should heartbeat/retry consumer and later canonical stages proceed. No WorkManager/background guarantee before then.


## First bounded Android adapter (2026-11-04)

- Inspected the full ActionEngine and DeviceActionExecutor. set_media_volume is the first viable narrow operation: VOLUME_SET reads Android's media stream back and emits Verified: only when the actual value matches the requested absolute level. Settings launches, alarms/timers, dialer/SMS/share, torch and permission prompts return pending-verification outcomes and are not eligible for terminal success.
- Added VyomFactoryAndroidActionAdapter, allowlisting only android_action:set_media_volume; requires exactly percent (integer 0–100) and confirmation_id; checks STOP and a foreground predicate immediately before the side effect; consumes a one-time confirmation callback; and refuses any result that DeviceActionExecutor.auditState does not classify as verified.
- Tests cover the allowlist, strict argument keys/types/range, foreground gate, one-time approval, STOP-before-side-effect, and verified readback. Test file commit: f9f95da0fa913356f6fa8be785245a771abf1e10. Adapter commit: 0559e1efbaa98fdf865715e75c71ad9724bcf9a3.
- Important limitation: this adapter is not registered in the production registry, has no production Activity/confirmation provider, and is not invoked by the Factory UI. It is an implementation candidate, not usable Android control yet. A GitHub issue field cannot prove user consent; the eventual UI must create and consume an unguessable one-time approval locally. MainActivity is a plain android.app.Activity, so foreground state must be tracked explicitly (its onResume/onPause callbacks exist); do not assume it is a LifecycleOwner.
- CI for the new test + adapter commits has not yet been verified. The prior source SHA 130a750cfd0b47a2c880e84e4a3864c15e76c914 had Debug run 41813499913 still at Gradle build/test and Full run 41813500083 cancelled; rerunning the Full job was accepted by GitHub but the queried job snapshot still reported the prior attempt cancelled. Do not claim either current CI green.
- Next: obtain exact-head CI results; then add production Activity foreground tracking + one-time confirmation UI and wire this adapter into the registry/runner only through that user-visible flow. Keep other Android actions unsupported. Heartbeat remains pending until a real adapter is actively executing.


## Foreground consent boundary (2026-11-04)

- Added a process-memory foreground gate and one-time approval store. The approval store issues opaque UUIDs, caps pending approvals at 64, expires them after two minutes, and consumes each token once. It deliberately does not persist approvals, so process death invalidates pending consent.
- MainActivity now marks the foreground gate resumed in onResume and paused at the start of onPause. This Activity is still android.app.Activity; no LifecycleOwner/base-class change.
- Added the production adapter factory binding to the existing DeviceActionExecutor VOLUME_SET implementation, with exact readback through its Verified result. It consumes only the Activity-owned one-time approval store and checks the Activity-owned foreground gate.
- Test-first commit for gate/approval behavior: cf99ccaceb3219238980adee2072f2f9b495ac68. Gate/approval implementation: 07d857be0d5fdda8ed7814f38a491f3a100f0bb0. Activity lifecycle wiring: 1027df23a1171472d3a084e678741e8a9d194b3c. Adapter-to-DeviceActionExecutor binding: 07b08a739032eae5e7203dc950145946ff18826c.
- Still not complete: VyomFactoryDialog has not yet been wired to create and consume approval tokens, show an explicit action-specific confirmation, construct the registry/runner, or dispatch the claimed issue. Therefore no Android action is yet executable from the UI. The adapter factory is only the production binding primitive. No heartbeat, WorkManager, or background execution claim.
- CI: no workflow runs were returned for the new commits when queried; do not infer pass/fail. Previous run 41813499913 remained in Gradle build/test on older source 130a750cfd0b47a2c880e84e4a3864c15e76c914, and 41813500083 was cancelled. The adapter and lifecycle changes are not covered by those runs.
- Next: implement a user-visible foreground confirmation flow in VyomFactoryDialog. It must display the exact action and percentage, mint the one-time token only after affirmative tap, run on the foreground worker, and never execute if dialog/Activity leaves foreground or the claim/STOP fence is lost. Then fetch fresh issue body, instantiate runner/registry with only this adapter, and test cancellation, expiry, dialog dismissal, and no-side-effect paths before CI.


## Foreground approval UI + runner integration (2026-11-04)

- Test-first commit: `6999d28ca00beacd02959d4af6bc2071492cc634`. Added runner tests for consent decline (no journal attempt, no adapter call, exact claim released), locally-added approval token, and rejection if preparation changes the requested operation. Added foreground-epoch invalidation test.
- Source commit: `eaa1f93648b3d69ac263a0e0c3aa000820a3ffa0`.
- `VyomFactoryExecutionRunner` now accepts a preparation boundary after fresh GitHub fetch and adapter allowlist checks but before journal begin. Null/throwing preparation skips safely and releases the claim. Prepared envelopes must preserve target, operation, original request and all original arguments; only `confirmation_id` may be added for `set_media_volume`. A remote issue cannot pre-supply that token.
- Factory UI now exposes RUN NEXT VERIFIED ACTION. It creates a foreground runner with only `VyomFactoryAndroidActionAdapter.forActivity(activity)`, fetches a fresh GitHub Issues snapshot for the claimed issue, and dispatches through the existing journal/lease/retry safety path.
- Before journaling/dispatch, the UI displays the exact requested media percentage. The one-time token is issued only inside the positive button callback. Cancel/dismiss/timeout/worker interruption returns no approval and creates no execution journal attempt. The adapter still consumes the token once, rechecks STOP + Activity foreground immediately before the side effect, and verifies media volume readback.
- Activity pause increments the foreground epoch, clears all pending approvals, and cancels the visible confirmation prompt. Returning to foreground cannot revive a prompt from an earlier foreground epoch. Factory dialog dismissal also cancels a pending prompt and interrupts its worker.
- Scope remains deliberately narrow: only absolute media-volume set (0–100%) is executable. Other Android operations and repository changes are manual review/unsupported. This is foreground execution, not a background worker or 24/7 agent.
- Exact-head CI: Debug run `41815865975` and Full CI run `41815865798` were started on source HEAD `eaa1f93648b3d69ac263a0e0c3aa000820a3ffa0`. Debug job `126526327504` was at Gradle build/test; Full CI run had no job snapshot at the time of this update. No green result or APK artifact is claimed.
- Earlier test-only runs `41815671472` and `41815671555` targeted `6999d28ca00beacd02959d4af6bc2071492cc634` and were still in Gradle test/build when superseded. The source commit CI above is the required exact-head verification.
- Next: inspect exact-head Debug + Full CI results and fix any real failures. Then add STOP/pause UI and cooperative cancellation tests before heartbeat. Do not add heartbeat until an actual long-running bounded executor needs renewal; crash recovery, bounded parallelism and app-closed execution remain pending.


## Consent preparation hardening + CI diagnosis (2026-11-04)

- Follow-up source commits: `176bac28726df55d342ae20664c2d8136e5c4892` and `c26499db68ec2ea8f63fe4c9c6e90d76eb133da7`.
- Runner now checks prior attempt/retry history before opening a consent prompt. A task with a verified prior success is skipped without asking the user to approve an action that will not run.
- Consent preparation is accepted only after the original issue/operation/request and original arguments are preserved. For `set_media_volume`, the only allowed addition is a locally-issued `confirmation_id`; all other argument mutation is rejected. The remote issue cannot supply a usable consent token.
- Added regression coverage: decline releases the claim without journal/adapter activity; locally-added one-time approval reaches the adapter; operation tampering is rejected; prior verified success skips before prompting; approval-store clear invalidates all pending tokens; foreground epoch changes across pause/resume.
- Fixed Factory UI button state so RUN NEXT is disabled while a manual claim is held or being released, and re-enabled after successful release. Removed unused coordinator construction from the runner UI path.
- CI diagnosis from the test-first RED commit: Debug run `41815671555` failed at unit-test compilation because `prepareExecution` and `generation()` had not yet been implemented. The errors were expected at the test-first stage; implementation landed in `eaa1f93648b3d69ac263a0e0c3aa000820a3ffa0` and refinements in `176bac28726df55d342ae20664c2d8136e5c4892` / `c26499db68ec2ea8f63fe4c9c6e90d76eb133da7`. The failing RED run is not a source regression and is not counted as green.
- Exact source HEAD `c26499db68ec2ea8f63fe4c9c6e90d76eb133da7`: Debug run `41816150471` (job `126527286001`) and Full CI run `41816150476` (job `126527331868`) are both in progress. Debug was at Gradle build/test; Full CI was at secret scan. No current green result or APK artifact is claimed.
- Earlier source runs `41815865975`, `41815957307`, `41815957380`, and `41816087082` / `41816087209` were superseded by later commits; several Full CI runs were cancelled by newer pushes. Do not use them as final verification.
- Next: poll the exact-head runs above. If any Kotlin/test failure appears, inspect the actual job log and fix it. Keep the current one-adapter scope; next functional work is explicit STOP/pause UI and cancellation tests. Heartbeat still waits until an actual long-running adapter needs renewal.


## Foreground STOP control + verified terminal semantics (2026-11-04)

- Test-first commit: `0385c1e8326ea5b13094c60f6e050b0176e3f449` adds coverage that an external STOP after consent prevents journal creation/adapter invocation, and that an adapter sees STOP before its side effect.
- Foreground STOP UI and runner external-stop hook: `3e73abf7ad61d24479953d1350652d27b563e7e6`.
  - RUN NEXT enables STOP CURRENT RUN.
  - STOP sets an external cooperative flag and dismisses any pending confirmation prompt. If consent is pending, the runner returns `stopped_before_execution`, writes no journal attempt, and releases the claim.
  - If the attempt has begun, the adapter's pre-side-effect STOP checks see the flag and the journal records a stopped failure with no retry.
  - Dismissing the Factory dialog also requests STOP and cancels pending confirmation.
- Added late-stop regression test in `8fa7a8cf447e51e121ff8aebaf3de706d37f1017`: if an adapter has already completed and verified its side effect, a STOP arriving afterward must not rewrite that verified outcome as FAILED. Runner correction: `69b67a640495caba07bfc6d6d154f625284252d8`. This is important because STOP cannot undo a side effect that already completed.
- CI on earlier source HEAD `c26499db68ec2ea8f63fe4c9c6e90d76eb133da7`: Debug run `41816150471` failed at Kotlin compilation. Real errors: `VyomFactoryDialog.kt:262` passed generic `Activity` to `forActivity(MainActivity)`; line 387 dereferenced nullable `MainActivity?`. Corrected by binding a non-null `hostActivity` before adapter construction/confirmation UI in `eac97f2d868e25d6876588ce34c67a429ea8b8b7`. The corresponding Full CI run `41816150476` was cancelled; it is not a pass.
- Exact latest source HEAD `eac97f2d868e25d6876588ce34c67a429ea8b8b7`: Debug run `41816675456` is IN_PROGRESS; Full CI run `41816675469` is PENDING. No green result or artifact is claimed. Older runs `41816575411`, `41816529872`, `41816482281` and `41816482477` are on superseded source commits and must not be used as final verification.
- Next: inspect exact-head logs/results. If green, proceed to retry due-work consumer only after ensuring each retry obtains fresh GitHub state, a new lease, fresh explicit approval, and checks STOP. Do not add heartbeat for this fast synchronous volume adapter; heartbeat remains deferred until a genuinely long-running adapter exists. Crash recovery, bounded parallel workers and app-closed verification remain pending.


## RUN NEXT starvation prevention (2026-11-04)

- Added filtered fresh-snapshot claim selection in `VyomFactoryWorkerSession.claimNext(eligible)`. It still reconciles every open task into the device inbox, but the foreground runner can ask the coordinator to consider only execution-eligible issues.
- RUN NEXT now filters out malformed/manual-review envelopes, unsupported Android operations, non-media-volume operations, remote-supplied confirmation IDs, legacy/terminal/interrupted/active attempts, and failed attempts without a due idempotent retry schedule. A previously succeeded or not-yet-due high-priority issue can no longer be claimed repeatedly and starve lower-priority executable work.
- Filtered tasks remain in the shared inbox; this is not deletion or automatic status mutation. The separate manual CLAIM NEXT flow can still claim them for review.
- Test-first commit: `b9d190602873235285f355ac7d5ac2f838a8c02b`; implementation commit: `80b2df68c4f67fca77a3a5826db4bf9304a9f186`.
- Exact latest source HEAD `80b2df68c4f67fca77a3a5826db4bf9304a9f186`: Debug run `41816939027` is IN_PROGRESS; Full CI run `41816938933` is PENDING. Older runs on `eac97f2...` and previous commits were superseded/cancelled; no CI pass or artifact is claimed yet.
- Next: inspect exact-head compile/test results. If green, keep heartbeat deferred (volume set is synchronous and short), then implement a due-retry manual/foreground consumer with a fresh GitHub snapshot, a new lease and a new explicit user approval per retry. No autonomous retry without an app-visible scheduler and crash recovery.


## Foreground crash-recovery guard (2026-11-04)

- Test-first commit: `b2d0e1e082a57a4b9e84fcdb7cf6bdd644bf2557` verifies that a nonterminal RUNNING attempt whose lease expired is marked INTERRUPTED before the next claim is selected, is not replayed, and does not invoke the adapter.
- Implementation commit: `e2e10724bfcf759ae59bed82fcae114c1a06c9e9`. `VyomFactoryExecutionRunner.runNext()` now calls `journal.interruptExpired(now())` before invoking its claim selector. This ordering matters because the production selector filters terminal/active history; without recovery first, an expired RUNNING entry could remain ineligible forever and block the inbox.
- Scope: recovery occurs when the user taps foreground RUN NEXT after the app is available. It marks expired/replaced attempts interrupted and requires manual review; it does not replay a side effect, run on app startup independently, or provide background execution.
- Exact source HEAD `e2e10724bfcf759ae59bed82fcae114c1a06c9e9`: Debug run `41817236650` is QUEUED; Full CI run `41817236425` is PENDING. No green result or APK artifact is claimed. Older runs on `80b2df6...` and prior source SHAs are superseded; some Full CI jobs were cancelled while multiple pushes were being tested.
- Next: poll the exact-head Debug + Full CI. If green, review the filtered retry path and add bounded retry/attempt status UI. Bounded parallelism and app-closed verification remain pending. Heartbeat remains deferred until an adapter has a measurable long-running phase.


## Retry jitter herd prevention (2026-11-04)

- Test-first commit: `03a6314dde76e1c9993941a1b2792da124f204ee` verifies that the same issue/operation receives deterministic retry jitter while different issue IDs receive different offsets.
- Implementation commit: `a61c5a74356843ee9598c306adc4d4357c1786dc`. Retry jitter identity now hashes issue number + operation key. Previously it hashed only operation key, causing every issue using the same adapter to receive the same delay and potentially retry in synchronized bursts.
- Delay remains bounded to the existing 0–25% jitter window; only the stable seed changed. This does not create a scheduler or background retries.
- CI for prior source HEAD `aad1445228d55914f3bbefe6f1c477a671470f9a`: Debug `41817304881` and Full CI `41817304876` were still running in Gradle unit-test/build steps when this follow-up landed; neither was green at last poll. They are superseded by the new source commit and cannot verify it.
- New exact source HEAD `a61c5a74356843ee9598c306adc4d4357c1786dc`: poll its Debug + Full CI runs. No green CI or APK artifact is claimed.
- Next: inspect exact-head CI. Then continue bounded retry status/observability and remaining Factory stages. Do not introduce heartbeat for the current short media-volume adapter.


## CI compile failure and test-source correction (2026-11-04)

- Exact log from Debug run `41817304881` (source HEAD `aad1445228d55914f3bbefe6f1c477a671470f9a`, job `126531225422`) identified Kotlin syntax errors in `VyomFactoryExecutionRunnerTest.kt` lines 314, 370 and 387. The test-fixture signature and constructor arguments contained literal backslash-n sequences rather than actual line breaks, producing cascading unresolved references. This was a real compile failure; no artifact was uploaded.
- Corrected the test source in commit `3aee6fc05b8682daf34e2bfe9d7a48cf574eb626`. Production code was not changed by this fix.
- Retry jitter herd prevention remains in source commit `a61c5a74356843ee9598c306adc4d4357c1786dc`: deterministic issue+operation seed instead of operation-only seed.
- Latest source HEAD is `3aee6fc05b8682daf34e2bfe9d7a48cf574eb626`. The previous Debug run `41817783421` was on the parent handoff SHA and is not verification for this fix; parent Full CI `41817783410` was cancelled. New exact-head Debug + Full CI runs must be checked; do not claim green until they complete.
- Next: inspect exact-head CI compiler/test output, fix any further real errors, then add retry queue status UI only after compile/test stability. No final APK.


## Latest source verification snapshot (2026-11-04)

- Retry jitter spread fix: source commit `a61c5a74356843ee9598c306adc4d4357c1786dc`; deterministic jitter now incorporates issue identity, not just operation identity.
- Debug run `41817304881` on older source `aad1445228d55914f3bbefe6f1c477a671470f9a` FAILED at `compileDebugUnitTestKotlin`. Actual syntax errors were literal `\\n` sequences in `VyomFactoryExecutionRunnerTest.kt` fixture/runner signatures. Corrected in source commit `3aee6fc05b8682daf34e2bfe9d7a48cf574eb626`.
- Source-equivalent Debug run `41817853816` on `3aee6fc...` is still IN_PROGRESS at Gradle build/test; Full CI run `41817853867` was cancelled by the subsequent handoff update and is not a pass.
- Latest handoff HEAD before this update: `22e911bed58b30a207e0b5c77d9e311beffe5a01`. Exact-head Debug `41817904327` and Full CI `41817904289` are IN_PROGRESS (Debug at Gradle build/test; Full CI at unit tests/debug APK). No green result or APK artifact is claimed.
- Next: finish exact-head CI inspection; fix any actual compile/test failures before proceeding. Retry queue status UI remains a later item; do not claim Factory completion or produce a final APK.


## Retry queue observability + post-consent STOP correction (2026-11-04)

- Test-first retry overview contract: `0e69a1b9510a94f4c62e88d13456f31118a56b6b` (branch recovery snapshot included the new test without dropping existing tracked files).
- Retry overview implementation: `8ba017cea65ab08d9d9c9e82a6a1f0e1b5d3b00a`. Pure read-only summary reports total, due, waiting, and the next scheduled issue with a rounded-up countdown. It does not schedule or start work.
- Factory dialog integration: `813f0b69aa7c5e17712794d59abf585d3b18d2f1`. Worker panel shows persisted retry status; status refreshes on opening and after GitHub task refresh. Store read errors are shown as unavailable/manual review, not silently treated as an empty queue.
- Corrected overview ordering/label in `c14eea1a488e02404b001f95a43ab395866c6865`: when some retries are already due, the line highlights the next future scheduled retry rather than incorrectly labelling that future issue "DUE NOW".
- Exact-head Debug run `41818023297` on `36662759219b74ffaf6f395701b88f420ed619b8` FAILED after compilation and 402 unit tests: two failures. Real logs:
  - `foregroundPreparationMayAddOnlyLocalApprovalWithoutChangingRequestedAction` had an incorrect expected operation string (`safe_action`) despite the fixture explicitly requesting `set_media_volume`. Corrected expectation and added original percentage preservation assertion in `74f59f2ae3a3268277e9f632e0c49ca54de4e0ef`.
  - `externalStopAfterConsentSkipsBeforeJournalAndSideEffect` exposed a real race: STOP arriving while consent UI was open was not rechecked after preparation and before journal creation. Runner now rechecks external STOP after validating the prepared envelope and before journaling; STOP releases the claim without creating an attempt or invoking the adapter. Fix: `edf2d347bbe52cccd29dbeeb038e186fd18839ca`.
- Current source HEAD before this handoff update: `edf2d347bbe52cccd29dbeeb038e186fd18839ca`. Debug `41818674188` is IN_PROGRESS; Full CI `41818673943` is PENDING at last poll. Parent runs `41818640405` / `41818640497` are on test-only parent SHA `74f59f2...` and are not final verification. Retry UI tests and source tests must pass on the final handoff-updated exact HEAD.
- The retry overview is visibility only. Retry execution still requires foreground RUN NEXT, a fresh GitHub snapshot, new lease, STOP checks, and a new one-time approval. No automatic scheduler, WorkManager, heartbeat or background execution was added.
- No APK artifact is claimed. No physical device test is claimed. Factory stages 7 (bounded parallel workers) and 8 (app-closed physical verification) remain pending; heartbeat remains deferred because the only adapter is a short synchronous volume operation.
- Next: verify latest handoff-updated exact-HEAD Debug + Full CI, inspect real failures and fix them. Then proceed with safe bounded-worker design only after stage dependencies; do not deliver a final APK until all canonical stages and exact-head CI/artifact checks are complete.


## Retry overview CI corrections (2026-11-04)

- Exact-source Debug run `41818674188` on `edf2d347bbe52cccd29dbeeb038e186fd18839ca` compiled and packaged the debug APK locally, but 406 unit tests had two failures in the newly added retry overview tests:
  - `dueOnlyQueueDoesNotShowMisleadingFutureCountdown`: when all entries are due, choose a stable due issue by lowest issue number rather than the oldest timestamp.
  - `countdownRoundsUpAndSwitchesToMinutes`: the 1-second-ahead entry is scheduled, not already due; corrected the expected due count from 1 to 0.
- Corrected test expectation commit: `a576e7ae0041637c8666d78d4b30282abe5d7c48`.
- Corrected overview selection commit: `102e98f5198b5a0ee37c48f12a9f2e758fab1ee8`. When future schedules exist, display the earliest future retry; when all schedules are already due, display the lowest issue number as the next immediately eligible retry.
- Additional hardening in parent source commit `c47c23d9aa08fddff365b3da899a6f189182c529`: avoid overflow when rounding a saturated `Long.MAX_VALUE` retry timestamp; refresh the overview after a RUN NEXT result so newly-created/cleared schedules appear immediately.
- Test imports for the retry overview assertions were corrected in `5927931e0612853388a043c757564760c9e511d4`. The saturated timestamp case is covered.
- Latest source HEAD before this handoff update: `102e98f5198b5a0ee37c48f12a9f2e758fab1ee8`. Debug run `41819203837` was started on parent test commit `a576e7a...`; it does not include the final helper selection correction. Full CI on that parent was cancelled. The exact latest source SHA must receive new Debug + Full CI verification; do not treat parent runs as proof.
- STOP-after-consent production fix remains in `edf2d347bbe52cccd29dbeeb038e186fd18839ca`; test expectation correction in `74f59f2ae3a3268277e9f632e0c49ca54de4e0ef`.
- No final APK. No physical device verification. Retry status is read-only UI; no scheduler or background execution was added.
- Next: run Debug + Full CI on the handoff-updated exact HEAD and fix only failures confirmed by logs. Then resume canonical Factory order; bounded parallel workers and app-closed verification are still pending.


## Process-wide bounded worker admission (2026-11-04)

- Exact prior HEAD `66c1b416f7d6fbea67937c781c6d4fe2e9a8121a` passed both workflows: Debug `41819331230` SUCCESS; Full CI `41819331290` SUCCESS. Debug build/test, versioned APK packaging, no-APK update demo package, artifact upload and CI release publish all succeeded.
- Intermediate CI artifact exists: `vyom-0.3.1-66c1b416f7d6fbea67937c781c6d4fe2e9a8121a`, artifact ID `13316536354`, 88,116,748 bytes. This is not final APK delivery and is not evidence of physical-device testing.
- Stage 7 audit: the dialog's single-thread executor and per-dialog `busy` flag were not sufficient as a process-wide guarantee if more than one Factory dialog/runner were ever created. Existing lease locking only protects claim/journal state transitions; it does not serialize the actual Android device side effect. The sole production adapter changes the shared media-volume stream, so concurrent volume actions are unsafe.
- Test-first contract commit: `f0d1200e8698ee0c41f05d324b79c576b0b16ed2` adds a regression test that a process permit admits one owner, rejects a second, and can be reacquired after release.
- Implementation commit: `997f5d8a23cc801eae4147f5cb0061500111fbad`. Added `VyomFactoryExecutionPermit` with an atomic process-wide singleton. `VyomFactoryExecutionRunner.runNext()` acquires the permit before journal recovery or claim selection and releases it in `finally`, including exceptional exits. A contending runner returns `Outcome.Busy` without claiming work or touching the journal.
- Runner regression + UI commit: `82e2a52be6a8a7d8fd2b7cbdba2e82cd693bbb01`. Confirms a busy runner does not claim a task, create a journal attempt or call an adapter; Factory UI reports that another action is active.
- Concurrency remains intentionally capped at **1**. No thread pool >1 or concurrent Android side effects were enabled. To increase the cap later, first introduce resource-keyed adapter serialization and prove isolation for shared device resources (especially media volume), plus STOP/lease/journal race tests. This completes the safe bounded-admission baseline, not multi-action parallel execution.
- Exact previous-head CI success does not verify these new commits. Poll fresh Debug + Full CI on the handoff-updated exact HEAD. If either fails, fix only from real logs. Do not deliver a final APK until canonical stages are complete and exact-head artifact is verified.
- No backend/Render/production Supabase/Android main changes. No physical device or app-closed execution test claimed.


## Activity pause invalidation hardening (2026-11-04)

- Audit found a lifecycle gap: dialog dismissal set the runner's cooperative STOP flag, and the Android adapter independently checked foreground state immediately before its device side effect, but Activity pause itself was not wired into the runner's STOP callback.
- Source commit: `d14ddb7554101872526aa3306361435a5d7fd926`. RUN NEXT captures the Activity foreground-gate generation when constructing the runner. The external STOP predicate now becomes true if the user presses STOP, the Activity is not resumed, or the foreground generation changes. A pause followed by resume cannot silently revive the old run; it remains cancelled for that run.
- Existing runner regression tests already cover external STOP before journal/adapter work and STOP observed by the adapter before its side effect. The production UI wiring changed; no claim of an Android instrumentation/lifecycle test is made.
- This is a STOP/pause safety correction, not background execution. The adapter still rechecks foreground state and one-time approval before changing media volume.
- Exact current source HEAD: `d14ddb7554101872526aa3306361435a5d7fd926`. Debug and Full CI must be run on the final handoff-updated HEAD. No final APK or physical-device test is claimed.
- Next: verify exact-head Debug + Full CI, inspect any real failures, then continue stage 5/6 coverage and retain the one-runner cap. Stage 8 still requires physical app-closed verification.


## Activity-pause STOP correction and exact-head CI status (2026-11-04)

- Source commit: `d14ddb7554101872526aa3306361435a5d7fd926`. `VyomFactoryDialog` captures the owning Activity's foreground-gate generation before constructing the runner. The runner's external STOP predicate checks the user STOP flag, foreground state, and unchanged generation. A pause/resume cycle permanently invalidates that run.
- Handoff/source tip recorded by previous automation: `63e830a3de9742ebda0467f819772b7ec2ba74eb`. Verify actual branch HEAD because the commit's file contents must be checked independently.
- Exact-head CI at SHA `63e830a3de9742ebda0467f819772b7ec2ba74eb`: Debug run `41821666098` and Full CI run `41821666302`. At the latest poll, both were still running their Gradle unit-test/build stages; no artifact was available yet. Older SHA `d14ddb7554101872526aa3306361435a5d7fd926` CI was superseded/cancelled and is not authoritative.
- Existing runner tests cover external STOP before execution and STOP checks around adapter dispatch, but no Android Activity lifecycle instrumentation test was added in this batch. Do not describe the lifecycle behavior as instrumented-tested.
- Next: poll these exact-head runs; diagnose only real logs. Then add a deterministic lifecycle-gate regression test if the foreground gate can be unit-tested without Android instrumentation. Keep process-wide worker cap at one. Stage 8 remains physical app-closed verification; no final APK is authorized yet.


## Activity-session fence at Android side-effect boundary (2026-11-04)

- Audit finding: RUN NEXT already captured the Activity foreground generation and the runner's external STOP predicate rejected a pause/resume cycle. The Android adapter separately checked foreground state twice, including immediately before the volume side effect, but those checks only read the current resumed boolean. A pause followed by resume between runner checks could make the boolean true again; the adapter did not compare against the generation approved for this run.
- Regression tests added first in commit `00f51b7f80b21bd93971495eb44838589663ce56`: reject a stale Activity session before consuming approval, and reject a session invalidated during approval before the device side effect.
- Production fix: `c3a4700dd06eb37e94af59cd2d185814310662be` adds a foreground-session validity callback to the adapter and checks it both before consuming approval and immediately before the side effect. `26ee84e7da4fbd73b40c7051953656b159f82c34` adds the gate's atomic resumed+generation predicate. `3d96f7da44052865fbafd9f3ba1eeeb7d9e65b0a` binds the adapter to the exact generation captured for RUN NEXT.
- Test contract: one test starts with an invalidated generation and confirms approval is not consumed; another invalidates the session during approval consumption and confirms no volume call occurs. These are unit tests; they do not replace Android lifecycle instrumentation or physical-device testing.
- CI for test-only RED commit `00f51b7f80b21bd93971495eb44838589663ce56`: Debug `41822969467` and Full CI `41822969595` were started while the test intentionally referenced the not-yet-added adapter callback; inspect their immutable-commit results as the RED proof. They are superseded and must not be used for final verification.
- Exact current source HEAD `3d96f7da44052865fbafd9f3ba1eeeb7d9e65b0a`: Debug `41823023646` IN_PROGRESS; Full CI `41823023648` PENDING at last poll. No pass or APK artifact is claimed for this HEAD.
- Previous exact source `f5bfbd4d995d6d3b4e5ddef7fd8236bdd24a9967`: Full CI `41822316018` SUCCESS; Debug `41822315988` was still IN_PROGRESS at last poll. This older CI does not verify the Activity-session fence.
- Scope remains Android-only. No changes to Android `main`, Render, Hermes backend, or production Supabase. Worker cap remains one. Heartbeat remains deferred for the short synchronous media-volume adapter. App-closed verification remains a physical-device requirement.
- Next: poll exact-head Debug + Full CI; diagnose only real logs. If green, continue canonical Factory audit and keep APK delivery blocked until all required stages and physical verification are complete.


## Activity-session regression test fixture correction (2026-11-04)

- Source review caught a test-only type mismatch before treating CI as evidence: the new helper takes a `() -> Boolean`, while the stale-session test initially passed a Boolean literal. Corrected it to a lambda in `73876f0af7643f8b43db3dccc8fc03315a7644ee`.
- The correction does not alter production behavior. The exact new HEAD must receive fresh Debug + Full CI; earlier runs on `3d96f7d...` and parents are superseded.
- Current code-HEAD runs at handoff update: Debug `41823174698` IN_PROGRESS; Full CI `41823174672` IN_PROGRESS. A documentation-only handoff commit followed; fresh exact-branch-tip runs are `41823206360` (Debug, IN_PROGRESS) and `41823206324` (Full CI, PENDING). No green result or artifact claimed.


## CI follow-up after handoff correction (2026-11-04)

- Code HEAD remains `73876f0af7643f8b43db3dccc8fc03315a7644ee`; the subsequent commit only corrected this handoff's run-ID references.
- Code-HEAD CI: Debug `41823174698` and Full CI `41823174672` were both IN_PROGRESS at last poll.
- Exact branch-tip (documentation-only commit `739adcbfa84146d23dd45441e2f74ce3125d08c6`) CI: Debug `41823206360` IN_PROGRESS; Full CI `41823206324` PENDING at last poll.
- Logs for the active Gradle jobs were not yet downloadable (GitHub returned BlobNotFound while jobs were running); this is not evidence of a build failure.
- Next: poll exact branch-tip runs, inspect real Gradle output when available, and do not use any superseded run as final verification.


## RUN NEXT lifecycle capture race + crash-recovery coverage (2026-11-04)

- Audit found the foreground generation was being captured inside the executor worker, not at the owner's RUN NEXT tap. If the Activity paused and resumed while the worker was queued, the worker could capture the new generation and mistakenly treat the old tap/consent as a fresh foreground run.
- Test strengthened in `10e50e72b1339ea59554444a352e912a880d2a40`: the previously approved generation must remain invalid after pause+resume even though the Activity is foreground again.
- Fix in `1573eec0a397067e7e0b9fcb414966ed2e1c2409`: capture Activity + generation synchronously on the UI thread before `worker.execute`; refuse RUN NEXT if Activity is not foreground. Worker and adapter receive that immutable generation.
- Crash-recovery tests added in `4f9dea810ea0387c6df365dfa5deb7818e22c4e3`: reopen journal after simulated process restart and recover STOP_REQUESTED attempt; interrupt only expired attempts while preserving a live lease; fail closed without overwriting journal when recovery commit fails.
- Parent exact branch-tip `9d4ad60aeac5db3382ff6113f144ea521fc911a9`: Debug `41823371366` SUCCESS and Full CI `41823371481` SUCCESS; Debug artifact `vyom-0.3.1-9d4ad60aeac5db3382ff6113f144ea521fc911a9` (artifact ID `13318995039`, 88,119,554 bytes); Full CI artifact `vyom-phase6-9d4ad60aeac5db3382ff6113f144ea521fc911a9` (artifact ID `13318800342`, 46,195,921 bytes). These artifacts predate the current changes and are not final delivery.
- New source HEAD `4f9dea810ea0387c6df365dfa5deb7818e22c4e3` has no verified CI result yet. Handoff-only commit follows; exact branch-tip CI must be polled before claiming success.
- Factory stages changed: #5 STOP/pause test coverage + queued-run generation race closed; #6 crash-recovery test coverage strengthened. No production background execution added; stage #8 physical app-closed verification remains owner/device-only.


## Exact-head verification refresh — 2026-11-04

- Branch tip before this documentation refresh: `34d14881a7e6b8278d03389c01ac29f65aee18ba`.
- Debug workflow `41825573775`: SUCCESS. Build/tests, APK packaging/checksum, signed no-APK content-update demo, artifact upload, and signed CI update release steps all completed successfully.
- Full CI workflow `41825573748`: SUCCESS. Termux Factory adapter syntax/status checks, secret scan, unit tests/debug APK, packaging/checksum and artifact upload completed successfully.
- Debug artifact: `vyom-0.3.1-34d14881a7e6b8278d03389c01ac29f65aee18ba`, ID `13318513312`, 88,121,760 bytes, archive digest `sha256:3bfbd5ec9384ff091bb3914f8bcc7afe30d124802c0290322f0fe53ce007733d`.
- Factory stage #5 remains PARTIAL: source-level STOP/pause fencing and tests pass, but Android lifecycle instrumentation / physical pause-resume acceptance has not been run. Stage #6 remains PARTIAL: journal restart/recovery tests pass, but actual process-kill/relaunch phone acceptance and startup recovery scheduling are not verified. Stage #8 remains PENDING physical device verification. Do not mark any of these complete based on JVM CI.
- No APK delivered. No Android `main`, Render, Hermes backend, or production Supabase changes.

## Startup crash-recovery integration batch — 2026-11-04

- Test-first regression added in 35726cf9d8651ea42b02147f64d5722a07bc3e30: startup recovery interrupts expired attempts, preserves live leases, does not replay work, and is idempotent on a second startup pass.
- 92153ff0ae7826e5cd263bcbd3bfad4c0d98fdc6: added explicit VyomFactoryExecutionJournal.recoverAtStartup(now) API; it delegates to the existing lease-fenced interruptExpired transaction.
- f44d214522aa0d88bc4f30057c8a517d8a1586e9: MainActivity.onCreate now runs startup recovery before the rest of app initialization. Failures are logged and fail closed; no attempt is replayed and no journal data is discarded.
- Scope remains deliberately bounded: if a lease is still valid at startup, recovery leaves it live. RUN NEXT retains its pre-selection recovery. No WorkManager, background scheduler, heartbeat, or app-closed execution is claimed.
- Exact-head CI for source f44d214522aa0d88bc4f30057c8a517d8a1586e9: Debug 41828785468 IN_PROGRESS; Full CI 41828785520 PENDING when this section was authored. CI must be rechecked on the HANDOFF-updated HEAD.
- Factory stage #6 remains PARTIAL until physical process-kill/relaunch acceptance is recorded. Stage #5 lifecycle instrumentation/physical validation and Stage #8 app-closed physical verification remain outstanding.

## Foreground lifecycle instrumentation batch — 2026-11-04

- Added `app/src/androidTest/java/com/hermes/assistant/VyomFactoryMainActivityLifecycleTest.kt`, using ActivityScenario against the real MainActivity. It asserts foreground state on resume, pause invalidation, old-generation rejection after resume, and one-time approval invalidation on pause.
- Configured AndroidX instrumentation runner + test dependencies in app/build.gradle.kts.
- Added API 35 Google APIs x86_64 emulator execution (`connectedDebugAndroidTest`) to `.github/workflows/vyom-android-debug.yml`; this is CI emulator coverage, not a substitute for physical phone acceptance.
- Startup crash-recovery integration from HEAD `f44d214522aa0d88bc4f30057c8a517d8a1586e9` remains in place. Stage #6 remains PARTIAL until process-kill/relaunch acceptance on a real device; Stage #5 remains PARTIAL until emulator instrumentation and physical lifecycle acceptance pass; Stage #8 remains PENDING.
- Latest source HEAD before this HANDOFF update: `c5c657c4723bfd5ef4b116be59ad136adf0f0b94`. Debug `41829739822` IN_PROGRESS; Full CI `41829739866` PENDING. Recheck exact-head runs after this documentation commit; no APK delivery is authorized.

## Factory lifecycle integration test batch (2026-11-04)

- Added `VyomFactoryLifecycleIntegrationTest` to exercise the complaint lifecycle across the real GitHub client, worker session/claim coordinator, execution runner/journal, and verification policy using MockWebServer and in-memory preferences.
- The scenario covers START to `in_progress`, fresh registry claim, verified runner outcome, worker audit event, rejection of DONE before owner verification, exact assigned-probe PASS, and final DONE/closed state on the same canonical issue.
- Test source is present at source checkpoint `1c790d3918c1060791949ceb096cc8e054daaf11`. The initial test-only commit had a wrong MockWebServer import and was immediately corrected; the later test fixture also models the START PATCH response as `in_progress`. Do not claim this integration test passed until CI executes the corrected source.
- Exact corrected-source CI: Full CI `41840844317` PENDING; Debug `41840844507` QUEUED at handoff refresh. Earlier Full CI `41839292627` SUCCESS verifies only parent source `832f49e406953aead5032d1f9d7b091be29f2bab`, not the new test. The parent Debug run `41839292787` remains QUEUED and is not current verification.
- Additional audit finding: `VyomAutomationsDialog.renderTasks()` reverses the already-canonical Factory complaint list with `.asReversed()`. Factory and Automations therefore can show opposite complaint order. Fix with a regression test after the lifecycle integration test result is known.
- Next: poll corrected-source CI; fix only actual test/compiler failures. Then correct Automations ordering while preserving the existing reverse-chronological order for local tasks. Refresh this handoff after that batch.

## Automations ordering correction (2026-11-04)

- Fixed the confirmed inversion: Factory complaints were already sorted P0 → P3 by `VyomFactoryTaskOrdering`, but Automations reversed the combined local+Factory list. It now reverses only local task recency and preserves the incoming canonical Factory complaint order.
- Added pure helper `VyomAutomationsTaskOrdering` and tests proving local tasks remain newest-first while Factory P0/P1 order is unchanged, plus the empty-Factory case.
- Updated `VyomAutomationsDialog` to use the helper; no backend, main branch, Render, or production Supabase changes.
- Current source checkpoint before this handoff refresh: `ba13c03346c1c2b8db9dd5380aa850a9f7c74f56`.
- Exact-head Full CI `41842466230` PENDING; Debug `41842466165` QUEUED. Previous corrected lifecycle-test CI was repeatedly cancelled/queued due to newer pushes before its Android test step completed. The lifecycle integration test and Automations regression tests remain unverified until a stable-head run finishes.
- Note: earlier Full CI `41839292627` SUCCESS is only for ancestor `832f49e...`; it does not validate these later tests or the current source.
- Next: poll exact-head workflows; obtain real test logs and fix compile/test failures. After stable green CI, inspect Factory proposal workflow tests and continue remaining Stage 1 gaps. Do not issue APK until the agreed Factory-first gate and exact-head Debug artifact checks pass.

## Factory worker infrastructure-failure intake (2026-11-04)

- Closed a Stage 1 gap found in `VyomFactoryDialog.runNextTask()`: exceptions thrown before the runner could return a typed `Outcome` were previously swallowed into `null`, leaving no canonical Factory complaint.
- Added an optional `onRunnerFailure` callback. The dialog now reports only the exception class name (never raw exception text, request text, or credentials) to the Activity on the main thread.
- `MainActivity.showVyomFactory()` creates a unique `task_failure` complaint using the existing idempotent GitHub issue intake, with private request/exception details withheld. This is specifically for worker infrastructure exceptions; typed runner `Failed` outcomes still audit the original issue.
- Source checkpoint before this handoff refresh: `01c161b4baff97f920783eb8df5167e77207d7ca`.
- Current exact-head CI: Debug `41843156481` QUEUED; Full CI `41843156742` PENDING. No build/test result is yet available for this source. The immediately prior source `30dfc0db49d9d53f171e1d09a589da8906ceec4a` Full CI `41843123386` was cancelled when the next commit arrived; it is not verification.
- Earlier run `41842588194` on `b844d262dacea9768092acadc527fb8359a0b587` was cancelled and must not be reported as success. Runners appear delayed/queued; continue checking exact-head jobs and logs, do not infer failure or success from queue time.
- Existing integration/regression tests remain unverified until a stable exact-head CI run completes: Factory START→claim→run→probe PASS→DONE; Automations canonical priority order; runner failure intake UI wiring.
- Next: poll exact-head CI, fix only actual failures; then audit remaining Stage 1 gaps (external-agent source acceptance, Diagnostics auto-intake, screenshot evidence owner decision, Termux smoke and physical-device acceptance). Keep APK 1 blocked until Factory-first gate and exact-head Debug artifact are verified.

## Factory retry and intake audit (2026-11-04)

- Found and fixed a retry-routing defect in the new worker infrastructure-failure intake: its local chat item ID now starts with `task-failure:`, matching `resumeQueuedChatMessages()`. If GitHub registration fails, reconnect retries the same complaint path instead of sending the report as an ordinary Hermes chat message. Stable per-attempt UUID is retained.
- Added explicit regression coverage for the `chatgpt` external-agent source identity. Existing allowlist/tests already cover `claude`, `codex`, `termux`, and `share_sheet`; this validates source-key acceptance only, not a live external agent integration.
- Re-audited Diagnostics: `runDiagnostics()` sends each `FAIL ·` row to `submitAutomaticDiagnosticFailure()`; content-specific SHA-256 identity prevents duplicate issue creation on retry. This was already implemented; no duplicate implementation added.
- Confirmed share-sheet screenshot receiver is absent from `AndroidManifest.xml`. It remains intentionally blocked until owner chooses private evidence storage or explicitly approves manually redacted public uploads. The repository is public and issue bodies are public; owner must also decide whether to keep it public with sanitized complaints or make it private before real user reports.
- CI runs `41843225962` (Debug) and `41843225983` (Full) were still QUEUED/PENDING at the last poll for parent SHA `204d44abd0161c81110a6c7d0578e0bad0506bc2`. New commits in this batch supersede that SHA; no result from those runs validates the latest source.
- Source commits in this batch: `304b4f0025ba16f9936daa8f2a3135d1f80f6cf3` (retry routing) and `346b7c33f7e58bb2e279f1e13246dd8d18ad861e` (ChatGPT source identity test).
- Next: inspect latest exact-head CI; then continue Stage 1 audit and keep screenshot/visibility owner decisions explicit. No APK readiness claim until exact-head Full CI, Debug CI, and artifact are verified.

## Debug CI failure correction (2026-11-04)

- Exact-head Debug CI 41843876699 on parent SHA 9a164bf3b460b750029057bd287d43dd53e1b9c2 completed FAILURE. The log shows :app:compileDebugKotlin completed, but :app:compileDebugUnitTestKotlin failed at VyomFactoryLifecycleIntegrationTest.kt lines 33, 107, and 121: JSONObject was passed where the helper requires a String.
- Corrected all three MockWebServer response calls to serialize the JSONObject with .toString(). Commit: bbae296400390ef7e149dacf4a0bc2afcdd9e114.
- This is a test-source compile correction only; no app behavior changed. The corrected test has not yet been executed by CI.
- Parent Full CI 41843876818 was still IN_PROGRESS on the old parent SHA when this correction was pushed; it does not validate the corrected commit. Poll its final result separately.
- Next: wait for exact-head Debug and Full CI for the correction commit; inspect real logs. Do not call APK ready until both exact-head workflows succeed and the expected artifact is present and verified.
- Separate audit note: automatic complaint registration returns early with error when the GitHub token is missing, while reconnect retries only queued items. This token-recovery retry gap is confirmed by source inspection but is not yet changed; add a regression test before modifying it.


## Latest CI fixture correction (2026-11-04)

- Exact HEAD 12f6cf0009de1764a5f03b41d816cfba2bbb7d46 failed Debug 41845283985 and Full CI 41845286085. Both logs show two remaining JSONObject-to-String test fixture compile errors at VyomFactoryLifecycleIntegrationTest.kt lines 107 and 121.
- Fixed both response fixtures with JSONObject.toString() in d0b4430ffbad9fc0f81c7b0f6f499bd6c9791eb5. Test-only change; no production behavior changed.
- Previous note claiming all three sites were fixed was inaccurate: line 33 was fixed, but lines 107 and 121 remained. Await exact-head CI and actual test execution before claiming green.
- Next: verify exact-head Debug and Full CI, then continue the missing-GitHub-token complaint retry audit.

## Missing-token complaint retry correction — 2026-11-04

- Confirmed source defect: `submitFactoryComplaint()` marked an intake item `error` when the encrypted GitHub token was absent. `resumeQueuedChatMessages()` intentionally retries only `queued` items, so saving a token and reconnecting could never resume that complaint.
- Test-first regression: `VyomFactoryComplaintSubmissionPolicyTest.missingGitHubTokenKeepsComplaintQueuedForReconnectRetry` added in `dec212514416b5845cd370045c0da254a9b7d670`.
- Production correction: `VyomFactoryComplaintSubmissionPolicy.missingGitHubToken()` now returns a queued/pending decision; `MainActivity.submitFactoryComplaint()` uses that decision instead of marking the complaint as a terminal error. The existing stable item ID and reconnect retry routing remain unchanged.
- Commits: `71291f76107982e818998fb15b75a1e773a365ea` (policy), `4be1871f7a31f85e1654012c6572eedee430c848` (Activity integration).
- This only fixes the missing-token path. Network/API exceptions were already kept queued and retried with the same item ID; no duplicate retry mechanism was added.
- Exact new HEAD `4be1871f7a31f85e1654012c6572eedee430c848` has no CI result yet. Parent Debug run `41846518242` is still IN_PROGRESS on `28e65663f082286ae7e3829458fc27aea6007158`; it is not evidence for this new source. Parent Full CI `41846518438` SUCCESS also does not validate this new source.
- No backend, Render, Supabase, or Android `main` changes. No APK delivery authorized.
- Next: run/poll exact-head Debug + Full CI; inspect the real emulator instrumentation result from the parent run if it finishes. Then continue auditing Stage 1 complaint/worker failure paths without duplicating existing Diagnostics or worker exception intake.


## Exact-head CI and artifact audit — 2026-11-04

- Audited source: `e54badb24ced6a5bd76bbdd1acbaefb8e4d6aaa9` on `feature/memory-intelligence`.
- Full CI `41849546572`: SUCCESS on the exact source SHA above.
- Debug CI `41849546727`: build/unit-test/package/upload steps all SUCCESS; deferred foreground lifecycle instrumentation remained IN_PROGRESS when checked. It is marked `continue-on-error: true` and is outside the APK 1 gate, but lacked a timeout, allowing a stalled emulator to hold the workflow open.
- Artifact `13336505140` (`vyom-0.3.1-e54badb24ced6a5bd76bbdd1acbaefb8e4d6aaa9`) was downloaded for integrity inspection. ZIP SHA-256 matches GitHub's advertised digest `83ca774b4b7ab45cdbc1959235416b836996ba15663f32f618e5602905f0c39f`. Debug and signed APK checksum files both validate. Release-signing metadata reports `releaseSigning=true`; signed APK certificate report verifies APK Signature Scheme v2 with SHA-256 `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`. Eight content-update rollback tests report zero failures/errors. These checks verify the uploaded artifact package, not the still-running Debug workflow conclusion.
- Source audit reconfirmed every worker claim refreshes the live GitHub registry before creating a device-local claim; claim/read failures do not create a false claim. RUN NEXT routes runner/infrastructure exceptions through the existing idempotent `task_failure` complaint path. No duplicate intake path was added.
- Confirmed workflow reliability gap: optional emulator instrumentation had no bounded timeout. This commit adds a 15-minute step timeout; it remains non-gating through `continue-on-error: true`.
- No APK delivered. APK 1 still requires exact-HEAD Full CI and Debug CI success plus artifact integrity checks. No Android `main`, Render, Hermes backend, or production Supabase changes.
- Next: verify exact-head Full CI and Debug CI for this timeout/handoff commit. Then continue remaining Factory-first Stage 1 gaps and refresh this ledger.


## Proposal branch failure recovery — 2026-11-05

- Exact source checkpoint before this HANDOFF update: `872cd51af57b32027dcea1e76d7277a57a5ac912`.
- Closed a proposal reliability gap: after creating a uniquely named `vyom-proposal-*` branch, failures during source write or PR creation could leave an orphan branch.
- Added test-first policy coverage in `ProposalBranchRecoveryTest`: preserve branch if a PR exists despite a lost response; delete only after PR absence is positively confirmed; preserve branch when PR lookup is uncertain; swallow best-effort cleanup failure so it cannot mask the original proposal failure.
- Added `ProposalBranchRecovery` and integrated it into `SelfModificationClient.createProposal()`. On failure after branch creation, the client queries PRs for that exact owner+branch. If an existing PR is found, it returns that PR. If absence is confirmed, it attempts to delete only that exact newly-created branch. If lookup fails/permission is insufficient, the branch is preserved. No base/working branch can be selected by cleanup.
- GitHub API response parsing now handles JSON array responses and empty 204 responses required by PR lookup and ref deletion.
- Commits: test `f701c48582bf57049a5d384cdfd8ee025ab01355`; recovery policy `0ba73779601f6240474d4b0ff2a5b906cdc96b15`; client integration `872cd51af57b32027dcea1e76d7277a57a5ac912`; ambiguous PR outcome hardening `eff0ba783ea8aac18a35d0c760feaa598d32e55b`.
- CI on source checkpoint `872cd51...` was superseded by the final hardening commit before it could be accepted as exact-head verification. Latest source checkpoint `eff0ba...` requires fresh Debug + Full CI; earlier intermediate runs on test/policy/client commits are not verification.
- Previous source `3a8453e5970a0c47883fdaa6f09242e262d5ef14` exact-head Full CI `41851333829` and Debug CI `41851333877` both SUCCESS. Debug artifact `13336032068` ZIP digest matched GitHub digest `06c4158eab39aee081604c307c27a0de23316ac4ac59e7738165cdd889ec297d`; signed APK checksum and v2 signature/certificate were validated; rollback test report had zero failures. This is predecessor-source evidence, not validation of the proposal recovery changes.

- Safety refinement: if PR creation was attempted and the request outcome is ambiguous (network error or server 5xx), the branch is preserved even when the immediate PR lookup returns no result. A definitive GitHub 4xx rejection may clean up only after the lookup confirms no PR. A matching PR with an invalid/missing URL is treated as lookup uncertainty and never triggers deletion. This avoids deleting a branch whose PR creation may have succeeded but whose response was lost.
- No backend, Render, production Supabase, or Android `main` changes. No APK delivered.
- Next: poll exact branch-tip Debug + Full CI; inspect real logs and fix only actual failures. If green, verify the new exact-head artifact. Continue the Factory-first Stage 1 audit and retain owner-only phone/Termux/privacy acceptance gaps.


## Debug CI emulator stall correction — 2026-11-05

- Exact-head Debug run `41959969638` on `f6e817dac23eaa9029992311fdee6e35bd0d0b60` remained `in_progress` well beyond the configured 15-minute timeout. Step 12 (`reactivecircus/android-emulator-runner`) never left `in_progress`; release publication step 13 remained pending. GitHub run metadata stayed frozen at `updated_at=2026-11-05T03:11:05Z`; job-log download returned `404 BlobNotFound`. This is a runner/action stall, not a source test failure.
- Steps 1–11 succeeded, including `testDebugUnitTest assembleDebug`, APK packaging, signed content-update demo, and artifact upload. Artifact `13385471456` is on the exact source SHA; ZIP digest independently matched GitHub's advertised digest `e825d56aa81f16feab525681c45a6c9f790af3deeccb646669b909d59309c808`. This does not turn the still-running workflow into a successful CI run.
- Corrective workflow commit `80118ad748cf6f2b8cea710e036586f7d1d28dd4`: removed the optional emulator instrumentation action from the APK-gating Debug job. The lifecycle instrumentation was explicitly optional/non-gating, and its stalled third-party emulator action prevented the signed release step and final workflow conclusion. It must not block the Factory APK gate. The test source remains in the repository; no app behavior or test source was changed.
- The workflow correction commit is `80118ad748cf6f2b8cea710e036586f7d1d28dd4`; this HANDOFF refresh is a subsequent documentation commit and also triggers the branch workflows. Verify Debug + Full CI on the resulting branch tip, release publication, artifact digest, APK checksums/signature and rollback test report before closing Locked Order 1.
- No issue rows changed. No Android `main`, Render, Hermes backend, or production Supabase changes. No APK delivered.


## Exact-tip CI, artifact verification and owner-action correction — 2026-11-05

- Branch tip before this documentation batch: `b03fb16734d0d051a4bcd6af7b116fbe9e066f39`.
- Exact-tip Debug CI `41964102995` and Full CI `41964102924` both completed with conclusion **SUCCESS** on branch `feature/memory-intelligence` and exact SHA `b03fb16734d0d051a4bcd6af7b116fbe9e066f39`. Debug steps 1–12 all succeeded, including build/tests, packaging, signed content-update demo, artifact upload and signed CI release publication. Full CI steps 1–11 succeeded, including Termux syntax, complaint/priority tests, secret scan, unit tests/debug APK, package/checksum/upload.
- Debug artifact `13386668343`, advertised ZIP SHA-256 `93c219870e7aaca1249ed41d6539b434681f708f9d69bc1d3c1f54197b96a3e8`; downloaded ZIP SHA-256 independently matches.
- Full artifact `13387161699`, advertised ZIP SHA-256 `74ed99513f94b278c979150ada48c2d646b4dc483ddd7e448a529c0d326accff`; downloaded ZIP SHA-256 independently matches.
- Debug artifact `build-info.txt`: versionName `0.3.1-ci.1377`, versionCode `1964102995`, exact commit `b03fb16734d0d051a4bcd6af7b116fbe9e066f39`, `releaseSigning=true`, certificate SHA-256 `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`.
- Signed APK SHA-256 `0a0d4512490b8e97570293963fadd5c573797a20dc2281f6e9fbb3b1bba1710a`; debug APK SHA-256 `3219d5873e6b74830a69ea8c741ece84ba6b71fe6446c2fd260d27c9ff1a8b2d`. Both match their embedded checksum files. Certificate report verifies APK Signature Scheme v2, one signer, and the expected SHA-256 certificate fingerprint.
- A18 content-update demo reports PASS for version `1.0.1793851019165`, presence PASS, performance probe PASS with reported timing `same` (Δ -0.0002 ms). Rollback test XML: 8 tests, 0 failures, 0 errors. This is automated artifact evidence, not owner phone acceptance.
- Corrected `docs/OWNER_ACTIONS.md`: the phone acceptance step now names **Factory runner infrastructure failure**, not generic chat task failure (generic chat failure auto-intake remains intentionally disconnected). Screenshot acceptance now explicitly requires app-private storage and no upload until owner taps **Attach** for the exact issue. Share receiver/Attach UI remain unimplemented and unverified.
- Owner-only actions remain: convert the public repository to private before enabling remote screenshot attachment; install/authenticate Termux and run the documented idempotency smoke test; perform physical-phone acceptance; exercise external GPT/Claude/Codex source identities. These do not justify claiming screenshot attachment or phone acceptance complete.
- The owner-actions correction commit is `86551c08c2d1d8baf105c44c993efaf094b396f7`. This HANDOFF refresh will create a newer documentation commit; therefore `b03fb167...` is the last exact-tip green checkpoint, not proof for the post-refresh tip. Trigger and verify new exact-tip Debug + Full CI before treating the new tip as release evidence.
- APK 1 remains **not delivered**. Do not use the verified b03 artifact as APK 1: Stage 1 acceptance and remaining Factory order are not complete, and the installed-device certificate comparison is still an owner-side gate.


## Factory run-history visibility batch — 2026-11-05

- Source changes are on `feature/memory-intelligence` only:
  - `929d5eb6997f0d14c69ba125174c5c564a2a2f1b`: add a read-only Factory execution-history summary over the persisted journal. It counts active/verified/failed/interrupted attempts, orders recent records, and deliberately omits raw result/error text.
  - `40a917f9ba652fd291562dac5b5675b44d72993c`: add `VyomFactoryExecutionHistoryTest` for counts/order, five-row limit, empty journal and sensitive-text redaction.
  - `93620e682c7f44fa1de9b7f023df0b91c8c4fbad`: display recent redacted attempt rows in the Factory worker panel alongside retry status.
  - `880c54ec21cf4cbcb4204e315ab4a9298756307a`: add a real Diagnostics result row for persisted Factory execution-journal readability and summary.
- Existing lifecycle/ordering/safety coverage was reused, not reimplemented: `VyomFactoryLifecycleIntegrationTest`, `VyomFactoryTaskOrderingTest`, `VyomAutomationsTaskOrderingTest`, `VyomFactoryExecutionRunnerTest`, `VyomFactoryExecutionPermitTest`, `VyomFactoryExecutionJournalTest`, and the worker claim/session tests remain the controlling tests for those behaviors.
- Stage 5 is **still partial**. Factory now exposes actual device-local attempt history and Diagnostics evidence, while worker execution remains one-at-a-time and foreground-only. Only the explicit media-volume adapter is executable. The guarded repository proposal flow is separate and still requires owner diff/PR review; skill/repository discovery, licence/security review, safe installation, real skill validation and source/hash records are not yet implemented as a Factory workflow. Do not describe these as complete.
- Stage 6 safeguards are present in source (STOP, one-time foreground approval, lease fencing, durable journal, no replay of interrupted side effects, bounded idempotency-gated retry, and existing multi-task/permit tests). They are not a substitute for exact-head CI or owner phone acceptance.
- Stage 7 now has a Diagnostics row for the actual persisted execution journal, in addition to the existing live GitHub Issues read row. Other delivered Factory capability rows still need the exact Diagnostics/acceptance matrix audit before the stage can be marked complete.
- Last verified exact-tip green checkpoint remains `b03fb16734d0d051a4bcd6af7b116fbe9e066f39` (Debug `41964102995`, Full CI `41964102924`, both SUCCESS). New source commits after that checkpoint have not yet completed exact-head CI. Earlier intermediate runs were cancelled or remained in progress as new commits superseded them; never use those as green evidence.
- Current branch HEAD at this handoff edit: `880c54ec21cf4cbcb4204e315ab4a9298756307a`. The HANDOFF refresh itself will create another docs-only commit; poll the newest Debug and Full CI for the resulting exact tip and inspect actual job steps/logs.
- No APK 1 delivered. The b03 APK artifact is an earlier verified CI package, not the final Factory APK. Repository privacy, Termux smoke test, physical phone acceptance and external-agent acceptance remain owner actions; the screenshot receiver/Attach UI remains unimplemented.


## Factory skill/repository workflow batch — 2026-11-05

- Added `VyomFactorySkillRepositoryClient`: searches public GitHub repositories by topic, inspects repository metadata/default branch, resolves an immutable commit SHA, requires a root `SKILL.md`, and accepts only the explicit permissive SPDX allowlist (MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, ISC, CC0-1.0). No private repository credentials are used by this importer.
- Added explicit in-Factory review/install UI. The owner must inspect the full Markdown preview and tap **INSTALL REVIEWED SKILL**. Content is stored only under app-private `filesDir/factory-skills` as inert Markdown; scripts are never executed and the imported instructions are not automatically followed.
- Import validation requires bounded UTF-8 text, a Markdown title, exact immutable source commit, approved license, and SHA-256. Invalid UTF-8 is rejected. Storage directory identity is derived from a SHA-256 repository key to avoid owner/repository name collisions. Existing installs cannot be silently overwritten.
- Added `VyomFactorySkillStore.auditInstalled()` and a Diagnostics row. It verifies source repository, commit SHA, SPDX license, inert content type, Markdown validity and stored SHA-256; tampered content produces a FAIL row. Tests cover policy rejection, invalid UTF-8, explicit owner confirmation, no-overwrite, hash/source metadata and tamper detection.
- Factory worker panel now displays recent persisted execution attempts (newest first, max five), with counts for active/verified/failed/interrupted. Raw result and exception text are never displayed in this history. Diagnostics now includes the execution-journal readability row.
- Related source commits: `2d9718c8ac90ac27849e9f517070773c9f9434a5` (skill importer), `8422a345155e771e922b181a582f23ebbb74c517` (initial tests), `e58e130d01abacc0b9bb3d93a259126bd7419da5` (Factory review/install UI), `8702253f74d32eaa93eea457955e2db873c1e10f` (diagnostic audit regression), `4301343092113405d6af3cbaf5db109f7657e37c` (source/revision/hash audit and overwrite protection), `e9217f9b593be4b20348be5b4f1516c579e99ca8` (Diagnostics integration), and `c76ee989ca4282a2d5825ea3e3791341b5e32a29` (strict UTF-8 and collision-resistant storage key).
- Stage 5 progress: run-history visibility, inert skill repository discovery/import, proposal PR/CI path and signed content-update evidence are source-wired. Remaining: no general repository checkout/build/test/install executor; imported skill Markdown is stored but not executed; worker remains foreground-only with one implemented Android volume adapter; no live heartbeat or always-on runner. Do not claim full M0 Stage 5 complete until these limits are resolved or explicitly accepted in the locked scope.
- Stage 6 existing safeguards/tests remain: one-time approval, foreground generation fence, STOP, lease/journal fencing, retry gating, no automatic replay, and task isolation. No new duplicate implementations were added.
- Stage 7 now has actual Diagnostics rows for the execution journal, installed skill repository integrity and shared GitHub Issues read. A physical phone Diagnostics run is still pending.
- Last exact-tip green CI checkpoint remains `b03fb16734d0d051a4bcd6af7b116fbe9e066f39` (Debug `41964102995`, Full `41964102924`, both SUCCESS). Current source SHA before this HANDOFF refresh is `c76ee989ca4282a2d5825ea3e3791341b5e32a29`; its Debug `41965904258` is IN_PROGRESS and Full `41965904272` is PENDING/starting. Previous source-run IDs were cancelled as later commits superseded them. The HANDOFF refresh will create a new tip and trigger new exact-head runs.
- APK 1 is not delivered. Do not use the b03 artifact as the final APK. Owner actions remain private-repository conversion, Termux smoke test, phone acceptance and external-agent acceptance; share-sheet screenshot receiver/Attach UI is still not implemented.


## Skill import protocol and owner smoke-test handoff — 2026-11-05

- Updated `docs/VYOM_FACTORY_PROTOCOL.md` to specify the constrained public-repository `SKILL.md` import contract, explicit review/install, inert storage, immutable revision/hash metadata, Diagnostics validation, and current limitations (no executable tools, scripts, or general repository checkout/build engine).
- Updated `docs/OWNER_ACTIONS.md` with the exact phone smoke test: inspect full skill content/license/commit/hash, cancel and confirm no install, then explicitly install and verify the Diagnostics hash row; imported Markdown must remain inert. Screenshot receiver/Attach remains unimplemented, and remote upload remains blocked until the owner makes the repository private.
- Documentation commit: `734a1da7d65b72b0bde4eb58be85d9651ca031a5`. Current branch HEAD at this refresh: `734a1da7d65b72b0bde4eb58be85d9651ca031a5`.
- Source checkpoint immediately before these docs-only changes: `c76ee989ca4282a2d5825ea3e3791341b5e32a29`. Its Debug CI `41965904258` remained in progress when last checked; its Full CI `41965904272` was cancelled by the later documentation push. The next full exact-tip checks must be read from the new branch head; no source test/build result is claimed for the current tip yet.
- Newest previous docs-tip workflows: Debug `41965946948` and Full CI `41965946598` were started on `b63b3f629429e224ded2accc04f163146fa1dcd6`; they are not evidence for this later docs commit.
- Stage 5 is source-wired for foreground execution history, reviewed skill repository import, guarded code proposal PR/CI, and signed content update evidence. It is not full autonomous repo execution, skill execution, or always-on work. Stage 6 safety logic/tests are already present; Stage 7 Diagnostics now includes journal, installed-skill integrity and live GitHub registry rows. Device acceptance is still required.
- No APK 1 delivered. Do not deliver until the locked Factory stages and exact-tip CI/artifact gate are complete; the installed APK certificate comparison and owner phone acceptance remain mandatory.


## Screenshot share/Attach implementation batch — 2026-11-05

- Implemented Android `ACTION_SEND image/*` receiver on the existing MainActivity. One shared PNG/JPEG/WebP image (max 10 MiB) is MIME/signature checked, copied to app-private `filesDir/factory-evidence`, synced, and SHA-256 read-back verified. Share intake itself performs no network upload.
- Factory opens with the pending screenshot, shows a sampled local preview, lets the owner select an exact active non-paused issue, and requires a second explicit **Attach** confirmation.
- Remote attachment is fail-closed: GitHub repository `private=true` is checked before issue lookup and again immediately before the Contents write. Public repositories are rejected before any upload. The image is written under `.factory-evidence/issue-<n>/` on the existing `feature/memory-intelligence` branch, then linked in the exact issue comment with SHA-256 and evidence commit SHA. The GitHub client now supports the required guarded Contents API PUT.
- Local screenshot is deleted only after the issue comment is confirmed. If upload fails, the local copy remains for a manual retry. If upload succeeds but comment fails, a local upload receipt records URL/commit/hash and the UI prevents duplicate upload while showing the manual-link recovery path.
- Tests added: `VyomFactoryScreenshotEvidenceTest` validates allowed MIME/signatures, app-private copy/hash, size cap, mismatch rejection and upload receipt; `VyomFactoryScreenshotAttachmentTest` uses MockWebServer to prove public repositories are rejected before issue lookup/write and private repositories require the explicit client attachment path, with PUT before issue comment.
- Key source commits: screenshot policy/store `7c872bc3abc74e1d4375d2aaa49bb2b9e25ef01f`; intake tests `e78b1b2f69edded9231d1b546d29648d6b7be503`; Android share filter `5628f93f2559c2dd5699012335f2d0fca7ab5a69`; guarded attachment API `7a725147c454da067f844c0a6fb948880c2aa181`; MainActivity intake `57678eef75696930481b6e8caa597cd190e5f916`; Factory preview/Attach UI `3427a83d5aae060b8355a6e8d58bf4f137a6d1db`; privacy recheck `cfea081a7e746f1e7dcf4d219325650c581a1b99`; MockWebServer attachment tests `32979a30668d0a2f95d846e007befbf6dfcc5c3a`; preview `710e74f6b95157ddae537aae229f821288458964`; active-issue filter `da23d1067a474597f006739ac85007528dd38b69`; Contents PUT support `2893265da036ffacf568cfd476ab4c000ff387a8`; share wiring syntax repair `f91931d83cf9dec0ad4ded10276b2e98712fcb16` and `bb6cbf1bf1b1e3b7683e450504187dcc5fac5d32`; reopen-pending support `4fa18859a254a76ee248b99a3a3b747ff5b6e91a`; receipt `6c5ee53446a02965f895c7f6544d4c68249ff8c0`; duplicate-upload UI `fd68d0e9b47e801932187bf5be43bfdd65319d2d`; receipt validation test `db474ff71720b71228a0bfe3c76b913ba8b18eed`.
- Documentation updated: `docs/OWNER_ACTIONS.md` now gives the Gallery → Vyom → preview → choose exact issue → Attach acceptance steps and requires Contents write + Issues write. `docs/VYOM_FACTORY_PROTOCOL.md` documents the privacy checks, storage path, upload receipt and branch-protection fail-closed behavior.
- This implementation is source-wired only; physical phone acceptance and owner repository-privacy conversion remain pending. If branch protection rejects direct Contents API writes, do not bypass it; the app must preserve the local file.
- Source SHA immediately before the latest docs-only refresh: `db474ff71720b71228a0bfe3c76b913ba8b18eed`. Exact-tip Debug `41966466312` is QUEUED and Full CI `41966466300` is PENDING on docs commit `2e8e4138019f37e3bb505462ad7535ed4c762fdb`; these are not green yet. Poll real jobs/logs and fix any failures before another APK decision.
- APK 1 is not delivered. Repository is still public until owner completes the visibility action; do not perform a real screenshot upload or include real personal screenshots before the owner confirms Private.


## Mandatory-order stages 2–4 regression batch — 2026-11-05

- Stage 2 E2E lifecycle coverage was verified in source and reused: `VyomFactoryLifecycleIntegrationTest` already covers START → claim/run → issue event/result → owner probe PASS → DONE. Factory/Automations shared priority/order tests and paused-task exclusion tests remain in place. No duplicate lifecycle test was added.
- Stage 3 generic non-chat worker failure routing is now source-wired for Factory runner and skill-repository infrastructure failures. `VyomFactoryWorkerFailureRouter` builds stable worker/task source IDs, accepts only known worker and exception class identifiers, withholds original request/credentials/exception details, and routes through the existing `task_failure` canonical register. Skill search/inspect reports only network/HTTP-5xx infrastructure failures; expected missing-license/missing-file/publication policy rejections are not turned into complaints. Pending worker failures resume by the stable `task-failure:factory-worker:` key.
- Stage 3 external-agent source identity tests now verify `chatgpt`, `claude`, `codex` and `termux` remain distinct canonical complaint sources and retain source-specific idempotency keys. This is source-level identity acceptance, not live connected-agent acceptance; owner test remains required.
- Stage 4 proposal workflow tests added:
  - protected source path rejected before any network request;
  - confirmed PR creation rejection cleans the proposal branch only after GitHub confirms no PR exists;
  - uncertain PR creation outcome preserves the branch;
  - exact-commit required-workflow failure aborts, and zero-timeout does not begin polling;
  - issue comment success/failure is explicitly represented by `ProposalIssueAudit`, so comment failure is not reported as saved;
  - request sequence contains no merge or release/install endpoint.
- `SelfModificationClient` now accepts an injectable API root for deterministic MockWebServer tests; production default remains the canonical GitHub repository API root. No runtime endpoint was changed.
- Key commits: `23f5c25ba99a43c3ea18694d6ed378e5c19a9521` (proposal API root injection), `4b6bc1b2b8db7e3ca439e998426eec8353748c13` (proposal integration tests), `9278580aacc1d6e6bfe6bec5903756bcc34251ce` (issue audit helper), `e915151834236fef938842527e602d2fe35beabd` (issue comment tests), `a972e77cae34f0d52bedc868be0b1c9c8d7ebe0a` (MainActivity integration), `bc21ba822946aba15754699d6f48253baa40a8d4` (worker failure router), `30d3fc04d3b5d43bee73337918042d0a4de6790f` (router tests), `2e8fce5120272fb481031baf18800a75bb6c86ab` (skill worker routing), `9bc669e43c6f21f4f19efa570e8d8d3801e90370` (MainActivity routing), and `5d01222ea8b17884b79294cbe7f75215c1dfd5b4` (stable IDs, failure-type allowlist and external-agent tests).
- Stage 3/4 source and regression tests are added but are not marked CI-passing until exact-tip workflows complete. Exact current source tip `5d01222ea8b17884b79294cbe7f75215c1dfd5b4`: Debug `41966801599` IN_PROGRESS; Full CI `41966801414` PENDING. Newer HANDOFF refresh will create another docs-only tip and require exact-tip verification.
- Stage 5 remains exact-head A18 signed content-update/rollback demo and artifact verification. Historical successful demo on `b03fb167...` is not a current-tip proof.
- Stage 6 owner acceptance still requires private repository conversion, Termux smoke test, physical phone screenshot/Factory acceptance and external-agent tests. Stage 7 APK 1 remains blocked until those owner actions and exact-tip CI/artifact gates pass.


## Current mandatory-order source checkpoint — 2026-11-05

- Latest source tip before this handoff refresh: `7f2109d9e7291be8902212fe8fe564300742045f`.
- Stage 1 screenshot receiver/Attach is source-wired: Android Share intent → strict PNG/JPEG/WebP intake (10 MiB cap) → app-private copy/hash → Factory preview → owner selects exact active issue → explicit Attach confirmation → private-repository check twice → Contents API write on `feature/memory-intelligence` → exact issue comment with URL/hash/commit. Local copy is removed only after comment success; upload/comment uncertainty is retained with a receipt. The repository API currently reports `private=false`, `visibility=public`; therefore real remote screenshot upload is correctly blocked pending owner action.
- Stage 2 E2E test already exists and is not duplicated: `VyomFactoryLifecycleIntegrationTest` verifies START → worker claim/run → issue event/result → owner probe PASS → DONE. Existing Factory/Automations ordering and paused-task exclusion tests remain the parity coverage.
- Stage 3 source work: stable privacy-safe non-chat worker failure routing is wired for Factory runner and skill-repository infrastructure failures. Source IDs are deterministic and bounded; only known exception class names are recorded; original request, credentials and exception messages are withheld. External agent source identity tests cover ChatGPT, Claude, Codex and Termux. These tests do not prove live external-agent integrations; owner acceptance remains.
- Stage 4 proposal tests: protected path rejection before network; confirmed PR failure cleans branch only after confirmed absence; uncertain PR failure preserves branch; exact-SHA CI failure aborts; zero timeout does not poll; issue-comment failure is not represented as success; successful proposal request sequence ends at unmerged PR and has no merge/release/install call. MockWebServer tests use an injectable API root; production default remains unchanged.
- Proposal client no longer depends on Android-only `Uri` or `Base64` helpers; path encoding and Base64 use Java APIs so the HTTP integration tests can run in JVM unit tests.
- Current source commits since the prior stage checkpoint: `86b0ff40971466de2ae91802c9a3cf24225cb0a6` (source ID bound), `863317b63c78342d9d83ed755b3262cca73e3b70` (Factory execution classification), `9089f69407b9eb8038046f7eeb74371162090f82` (strict UTF-8 test correction), `715a9d07a7244c020ab7733a0f14522ed067f244` (successful proposal no-merge test), and `7f2109d9e7291be8902212fe8fe564300742045f` (JVM-safe proposal HTTP helpers).
- Exact source-tip CI `41967009865` (Debug) is IN_PROGRESS at “Build and test APKs”; Full CI `41967010009` is PENDING. These runs are for `7f2109d...`, not this documentation refresh. Previous ancestor runs were cancelled as commits superseded them; no test/build success is claimed for this tip.
- Stage 5 A18 signed content-update demo/rollback evidence must be verified from the current exact-tip Debug artifact; the earlier b03 artifact is historical only.
- Stage 6 owner actions: repository privacy conversion, Termux smoke test, physical phone Factory/screenshot acceptance, skill import acceptance, and live external-agent identity acceptance. The repository is confirmed public, so owner must change it to private before any real screenshot attachment test.
- Stage 7 APK 1 remains not delivered. It requires owner acceptance, exact-tip Full + Debug CI success, A18 demo/rollback artifact inspection, signed APK checksum/certificate verification, and no auto-merge/auto-install.


## Exact-head CI, A18 artifact and failure repair — 2026-11-05

- CI failure found from real logs: Debug run `41967129516` and Full run `41967257693` failed only in `VyomFactoryScreenshotAttachmentTest.privateRepositoryUploadAndIssueCommentOccurOnlyAfterAttachCall` at test line 47. The upload path called Android's `android.util.Base64` from a JVM unit test; the Android stub throws `RuntimeException: Method ... not mocked`. Fixed in source commit `b5a50129dc8b0a68fa17346cb211a09f413ced24` by using `java.util.Base64.getEncoder()` for Contents API payloads. No other test failure was reported in those runs.
- Exact source head `b5a50129dc8b0a68fa17346cb211a09f413ced24` is now green:
  - Full CI `41967548802`: SUCCESS; Termux syntax/intake/priority checks PASS; unit tests and debug APK build PASS; artifact upload PASS.
  - Debug `41967548806`: SUCCESS; JDK/Gradle setup, persistent signing validation, `testDebugUnitTest`, debug APK, signed release APK, packaging, A18 demo, artifact upload and signed CI release publication all PASS.
- Full CI artifact: ID `13388800725`, name `vyom-phase6-b5a50129dc8b0a68fa17346cb211a09f413ced24`, ZIP SHA-256 `793d227df90b5749b35605e36c076f864acaaba770084f7f69f5b77b36b840ab`; extracted debug APK SHA-256 `f9599f7c93867c8f73b9f89fc8d64762d265840949e976dc6bffda843ab187cd`.
- Debug/A18 artifact: ID `13387489928`, name `vyom-0.3.1-b5a50129dc8b0a68fa17346cb211a09f413ced24`, ZIP SHA-256 `92c989a12ae5ad70412d9107554ca4d2921d37952bc48210aa1fcbafe3c8eaaa`; downloaded ZIP digest matches GitHub artifact digest.
- Build metadata: versionName `0.3.1-ci.1437`, versionCode `1967548806`, source commit `b5a50129...`, persistent release signing true.
- APK hashes independently recomputed from downloaded artifact:
  - Debug APK `vyom-0.3.1-b5a5012-debug.apk`: `c55b46b5f308f0e83db9f648dacd26f7410e9a71c59c666e32cbbd65291be51e`.
  - Signed APK `vyom-0.3.1-b5a5012-signed.apk`: `44938d7510f0c905b55a9844d60b21119f5ead0adeb7d5f4bf11a987cb39e7ba`.
  - apksigner report: signature scheme v2 verified; one signer CN=Vyom; certificate SHA-256 `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`, matching the pinned expected fingerprint.
- A18 no-APK content update demo: manifest signed; content update `status=PASS`, presence probe `PASS`, performance probe `PASS`; baseline 0.0124 ms, candidate 0.0121 ms, delta -0.0003 ms (same bucket). This is the content-file probe, not measured WebView render latency.
- A18 rollback report: `ContentUpdateSlotStoreTest` has 8 tests, 0 failures, 0 errors. Includes failed-probe rollback, owner rollback, stale-version rejection, two-crashes rollback and load-failure rollback. Demo ZIP and both result cards are present in artifact.
- Published CI candidate release (not APK 1): https://github.com/riyas433554-dot/vyom-android/releases/tag/vyom-ci-b5a50129dc8b0a68fa17346cb211a09f413ced24
- Stages 1–5 source/CI gates are now implemented and verified on `b5a50129...`: complaint intake/backlog, screenshot share/Attach source and private gate, existing lifecycle E2E, non-chat worker routing/external-agent source tests, proposal safety tests, and A18 signed demo/rollback artifact.
- Stage 6 remains owner-only: repository is still public (`private=false` confirmed); owner must change visibility to Private, confirm Contents read/write + Issues read/write token permissions, run the Termux smoke test, perform physical phone screenshot/Factory and skill-import acceptance, and verify live ChatGPT/Claude/Codex/Termux source identity. No screenshot has been uploaded.
- Stage 7 APK 1 is intentionally not delivered yet. The published artifact above is a CI candidate, not the final APK 1. This handoff update is docs-only; exact-head Full + Debug CI must be rerun on the new documentation head before declaring the current tip green. Then stop only for the owner actions above; do not auto-merge or auto-install.


## Exact-head Factory gate and candidate verification — 2026-11-05 (source SHA efb16a2)

- Exact source SHA `efb16a2f906f3c59191c9c428121bc6dc34fe99e` passed both required workflows:
  - Full CI `41968349353`: SUCCESS. Termux adapter syntax PASS, complaint/priority shell tests PASS, secret scan PASS, policy-critical proposal scan step skipped by workflow condition, unit tests + debug APK PASS, checksum packaging and artifact upload PASS.
  - Debug `41968349286`: SUCCESS. Persistent release-signing setup/verification PASS, `assembleDebug` + `testDebugUnitTest` PASS, signed APK packaging PASS, A18 content-update demo/rollback evidence generation PASS, artifact upload PASS, signed CI candidate release publication PASS.
- Full CI artifact ID `13389006161`, name `vyom-phase6-efb16a2f906f3c59191c9c428121bc6dc34fe99e`, ZIP SHA-256 `cca530205c2a505de3a595a2510d7717e689a35d3aef7786dbe8fbc83bca59bc`. Downloaded ZIP digest independently matches the GitHub artifact digest. Embedded debug APK SHA-256 `ef8644ffc36f6b676b3385651fca4b97275a4ed576e314bb340bf50163c85ae7`, matching its embedded checksum.
- Debug/A18 artifact ID `13388842064`, name `vyom-0.3.1-efb16a2f906f3c59191c9c428121bc6dc34fe99e`, ZIP SHA-256 `65cd0a3c7538b4fca42da5326c4f09f17fb717529058f43fa70996716c3cf804`. Downloaded ZIP digest independently matches the GitHub artifact digest.
- Build metadata: versionName `0.3.1-ci.1438`, versionCode `1968349286`, commit `efb16a2f906f3c59191c9c428121bc6dc34fe99e`, `releaseSigning=true`.
- Debug APK SHA-256 `2002a89a225a41e9b342bf6e3024c2c76e41b76853cc962f9fbfa1194922755f`; signed APK SHA-256 `541b4254f2cbd80f7bbf0da42d1c21966edfa5ed82bc3a3eba087e7526299b6e`. Both match their embedded checksum files.
- Signed APK certificate report: APK Signature Scheme v2 verified; one signer CN=Vyom, O=Vyom, C=IN; certificate SHA-256 `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`, matching the pinned expected fingerprint.
- A18 result JSON: status PASS, presence PASS, performance PASS; baseline 0.0097 ms, candidate 0.0093 ms, delta -0.0003 ms (same timing bucket). This measures content-file access only, not WebView rendering.
- Rollback XML: `ContentUpdateSlotStoreTest`, 8 tests, 0 skipped, 0 failures, 0 errors. Covers staging/probe switch, failed-probe rollback, owner rollback, stale version rejection, two-crash rollback, and page/skill load-failure rollback.
- Published exact-source CI candidate release: https://github.com/riyas433554-dot/vyom-android/releases/tag/vyom-ci-efb16a2f906f3c59191c9c428121bc6dc34fe99e. This is a manually installable acceptance candidate only, not final APK 1.
- Repository visibility checked through GitHub API: `public` (not private). Remote screenshot upload remains blocked by the source privacy guard until owner converts repository to private. No screenshots were uploaded.
- Current blocker is exclusively Stage 6 owner acceptance. Exact actionable steps and candidate checksums are in `docs/OWNER_ACTIONS.md`. No auto-install/merge and no changes to Android `main`, Render, Hermes backend or production Supabase.
- This handoff refresh and the OWNER_ACTIONS update are documentation-only. Run Full + Debug on the final docs HEAD before final APK 1 authorization.

## Factory architecture and implementation audit — 2026-11-05

- Audited the live source on branch `feature/memory-intelligence` at exact HEAD `0ba9d700ebc82cc946a5222f454623aa93c0931a`. Exact-head Full CI run `42005793423` and Debug CI run `42005793487` both completed SUCCESS on that SHA. This is CI evidence only; it does not substitute for owner phone acceptance.
- Owner's clarified architecture intent: Vyom Factory is the shared work-management and guarded-execution system, not a repository-only importer. Its managed domains include complaints/issues, errors/diagnostics, design/UI work, Android control, memory, repository/skills/integrations, automation and unknown-capability discovery. Each domain is a specialist module over shared intake, task identity, planning, approvals, execution, verification, audit and recovery; it is not a set of disconnected factories.
- Preserve the existing APK 1 scope lock: cross-area complaints are accepted and tracked, but standalone product-area feature implementation remains `backlog-after-factory`. The new architecture must not silently expand APK 1 to implement memory/design/control/voice product features. Only changes required for Factory's own complaint-to-fix workflow are in the APK 1 implementation scope unless the owner explicitly revises that gate.
- Verified source inventory:
  - Canonical GitHub Issues intake/register, area classification, priority ordering, owner edit/status/verification lifecycle, complaint source identity, screenshot evidence workflow, diagnostics/failure routing and audit comments exist in source.
  - Factory has a device-local SharedPreferences inbox and lease, fresh GitHub snapshot before claim, execution journal, retry schedule, STOP/foreground checks and a lifecycle integration test. This is a single-installation foreground runner; local leases are not distributed locks and the dialog owns a single-thread executor that shuts down on dismissal.
  - The execution envelope recognizes `android_action` and `repository_change`, but the production dialog currently registers only `VyomFactoryAndroidActionAdapter`. That adapter supports only `set_media_volume`; `repository_change` has no registered adapter and therefore cannot execute. Missing/legacy/unknown envelopes fail closed to manual review.
  - Skill repository discovery/import is intentionally narrow: root `SKILL.md`, approved SPDX license allowlist, immutable commit SHA, explicit owner review, app-private inert Markdown storage and integrity audit. It does not checkout/build/install arbitrary source code, MCP servers, SDKs, packages or Android projects.
  - Memory capture/store, Android control/UI Agent, design system/UI, voice, signed content updates and self-modification proposal components exist elsewhere in the app source. Their presence does not mean Factory can currently execute or maintain those domains end-to-end.
  - Automated unit/integration test files cover the Factory envelope, adapter registry, runner, lifecycle, queue, leases, retries, evidence, skill import and complaint classification. Exact-head CI is green as recorded above; physical Android acceptance and app-closed execution are not established by those JVM/CI tests.
- Main gaps confirmed by source inspection: no universal typed specialist-module registry; no general repository intelligence/install adapters; no repository-change executor; no general error-to-remediation planner; only one narrow Android action adapter; no WorkManager-backed persistent runner; no cross-device atomic claim; no demonstrated app-closed/Doze/reboot execution; no live verified design/UI implementation adapter; and no proof that memory/control modules are invoked as Factory specialists.
- Architecture guardrails for the next implementation plan:
  1. Keep schema-1 and untyped complaints as `manual_review`; never execute natural-language issue prose as shell or code.
  2. Add typed domain/module/capability routing while retaining the canonical GitHub issue number and existing source/idempotency identity.
  3. Separate repository inspection from integration/execution. Classify source type, license, dependencies, permissions, platform compatibility and provenance before offering a reviewed adapter. Arbitrary repository code must never run inside the Android app process.
  4. Add specialist adapters only with bounded inputs, least privilege, owner approval, tests, verified postconditions, rollback and truthful unsupported-state reporting.
  5. Do not claim 24/7/background, cross-device locking, universal Android control or autonomous code repair until their specific runtime and physical-device acceptance tests pass.
  6. Reuse existing complaint, journal, approval, retry, STOP, verification and evidence components; do not duplicate them.
- This audit is documentation-only. No source behavior, Android `main`, Render, Hermes backend or production Supabase was changed. No APK was built or delivered by this audit.
- Next: write a versioned Universal Factory module/capability contract and gap-to-existing-component map, then implement the smallest Factory-core slice test-first. Keep the APK 1 Factory-only scope lock unless the owner explicitly changes it.


## Priority repair batch — private update access and Teach Vyom chat isolation — 2026-11-05

- Owner reconfirmed APK 1 is Factory-only. Factory is the shared intake/management layer for complaints across all domains; standalone Control Engine, floating orb, composer, voice and memory product implementation stays post-APK 1 and must be handled as later Factory complaints.
- Priority 1 update/diagnostics 404 audit found the actual defect: APK update discovery fetched the private GitHub releases API without Authorization; the manifest and APK were fetched through unauthenticated browser download URLs. The no-APK content channel had the same missing authentication. The saved encrypted token is vyom_voice_secrets/github_proposal_token.
- Implemented shared GitHubReleaseAssetClient + GitHubReleaseTokenStore: release feed and asset API requests use the saved token; asset requests use GitHub's release asset API IDs; a 30x redirect is validated against the GitHub release asset CDN allowlist and the PAT is deliberately not forwarded to the CDN. Response size limits and the existing signature/hash checks remain in place. APK and no-APK release discovery now use this authenticated path.
- Added GitHubReleaseAssetPolicyTest and updated ContentUpdateReleaseLocatorTest to select authenticated release asset IDs rather than public browser URLs.
- Priority 2 issue #101 audit confirmed the prior chat dispatch called the automatic unknown-command Teach Vyom dialog. Changed chat routing so unknown/unclassified chat text returns to Hermes chat; automatic fallback no longer opens or pre-fills Teach Vyom. Explicit manual entry remains through Profile → Teach Library → Add Phrase. Known classified device actions and saved phrases remain supported.
- Added regression tests for conversational questions containing device-action words. The RED test commit aef45933f22f3f62bea89d884743a4d49f525930 failed at compileDebugUnitTestKotlin because the new release-asset policy was not implemented yet (Full CI 42010368806, Debug CI 42010368755); the log showed unresolved GitHubReleaseAssetPolicy, as expected for the test-first step.
- Source commits: update/authentication b6e1c31c647ebde31beadbec1a3f682abb7a02b9; chat/Teach isolation a86b1a81304850cb74e0024505acd08232d16a1d (current source head at time of this note).
- Exact-head CI for current source SHA: Full run 42011059270 pending at documentation time; Debug run 42011059260 in progress. Earlier intermediate source-head runs 42010672272 and 42010672256 were also still in progress. Do not report either fix CI-green until the exact-head runs complete successfully.
- Verification status: NOT verified on a physical device. Owner must run Check for Updates, Run All Checks, and Check No-APK Content Updates with the already-saved GitHub token; then verify release discovery, manifest/asset download, SHA-256, signer identity, content probes and active slot. For #101, send ordinary conversational questions that mention actions and confirm they remain in chat with no Teach Vyom dialog or prefilled text; explicitly open Teach Library and test Add Phrase separately. Keep #101 open until that device probe passes and evidence is attached.
- No APK 1 produced, no auto-merge/install, no Android main, Render, Hermes backend or production Supabase changes.
- Next after these priority repairs pass exact-head CI and owner acceptance is the small shared Factory core and the REPOSITORY_CHANGE adapter, before Android adapters, using the guarded proposal → tests → PR → both CI gates → signed artifact → A18 content update → probe → evidence flow.


## Follow-up: remove obsolete unauthenticated update helpers — 2026-11-05

- Removed the now-unused legacy unauthenticated HTTP helpers from ContentUpdateManager after switching the no-APK channel to GitHubReleaseAssetClient. This prevents an accidental future call path from bypassing the saved-token/asset-ID implementation and removes the stale RELEASES_API reference.
- Latest source SHA is 20370798db1c02e666e1c1d071f04ffe1e16b8e0. Exact-head Full CI run 42011548725 and Debug CI run 42011548589 were queued/in progress when this note was prepared; verify final outcomes before calling the source candidate green.
- Issue #101 remains OPEN. A progress comment (GitHub comment 6655794795) records the candidate behavior, exact source SHA, CI run IDs and required owner phone test. No device verification or issue closure was recorded.
- The earlier exact-head docs CI and source runs were superseded/cancelled by newer commits; only the newest full and debug runs on the final handoff head count.


## Follow-up: repair authenticated release-client constructor wiring — 2026-11-05

- Rechecked the previously reported update-auth and #101 changes against the live branch. The code is present, but CI was not green: Full run 42011969802 and Debug run 42011969831 both failed during Kotlin compilation. The exact log showed two constructor call sites using trailing-lambda syntax, which bound the lambda to the final `connectionFactory` parameter instead of the required `tokenProvider`: `ContentUpdateManager.kt:17` and `MainActivity.kt:87`. Errors: missing `tokenProvider` and expected `HttpURLConnection` but got `String`.
- Fixed both call sites using the named argument `tokenProvider = { ... }`, preserving the saved encrypted GitHub token providers. No change to token storage, redirect policy, update signature verification, or update behavior was intended.
- Commits: ContentUpdateManager call-site fix `e7a9c5f66a12fd3a675531722927b611b2018074`; MainActivity call-site fix `af95a2328667c8b56004366415ca52ba4a8ee0f1`.
- The source edits have been written to `feature/memory-intelligence`. Exact-head CI for `af95a2328667c8b56004366415ca52ba4a8ee0f1` has not yet been observed. Do not call either update fix CI-green until fresh Full and Debug runs complete successfully. The connector's commit-workflow lookup is PR-only and returned no runs; combined commit status also returned no statuses at the time of this note.
- #101 remains OPEN. Its normal-chat dispatch fix is present in source, but the required on-device chat and explicit Teach Library probe has not been performed. Verified on device: NO.
- Repository privacy was rechecked through GitHub repository metadata and now reports `visibility=private`. This satisfies the repository-privacy prerequisite only; screenshot Attach still requires owner phone acceptance.
- Next implementation after the update fix's exact-head CI is green: inspect/reuse the existing task, envelope, registry, coordinator and guarded proposal components; add the smallest shared typed-task classification + specialist registry/capability routing seam; then implement the REPOSITORY_CHANGE adapter first through the existing proposal/PR/CI/signed artifact/A18/probe/evidence path. Do not create a second queue, journal, lease, verification engine or release mechanism. Android adapters remain post-REPOSITORY_CHANGE and separate complaints.
- No APK 1 produced. No auto-merge/auto-install. No Android `main`, Render, Hermes backend or production Supabase changes.


## Owner phone-test failure — update API 404 and Android in-app action follow-through — 2026-11-05

- Owner physically tested the installed candidate and reported: **PASS** installed signing certificate SHA-256 `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`; **PASS** stable release identity; **FAIL** update diagnostics HTTP 404; **FAIL** Check for Updates HTTP 404; **FAIL** no-APK content update HTTP 404. Content channel remained at version `1.0.0`, baseline not established, active slot unchanged. This is a real device failure; Stage 6 owner acceptance is NOT passed.
- Owner also reports apps launch but in-app work does not complete. A saved phrase for “Install Godot” returned only the verified-Vyom-update restriction; no successful Play Store search, listing confirmation, installation or app launch was evidenced. Do not claim the Play Store workflow works.
- Source audit: `GitHubReleaseAssetClient.readReleases()` calls the private repository releases API with a bearer token from encrypted `github_proposal_token`; both APK and no-APK channels use this client. Factory Issues read success proves the token can read Issues, **not** that it can read private release metadata/assets. GitHub commonly returns 404 for a private repository when a fine-grained token lacks repository access or the required Contents read permission. This is the leading explanation, not yet proven; do not change endpoint or bypass authentication based on the status alone.
- Owner action required before another phone probe: edit the SAME GitHub token's repository access to include `riyas433554-dot/vyom-android` and grant **Contents: Read-only** (plus Metadata read, which GitHub requires); retain the Issues permissions used by Factory. If using a classic PAT, it needs the private-repository `repo` scope. Save the token again in Vyom Settings. Never paste the token into chat. If the token already has these permissions, capture only the non-secret GitHub response message/request ID for diagnosis.
- Source audit for app actions: `ActionEngine` distinguishes `INSTALL_PLAY_STORE` from generic `INSTALL`; generic APK installation is deliberately restricted to the verified Vyom update flow or a selected APK file. Play Store follow-through requires the Play Store operation and then the dedicated UI Agent, which checks Accessibility service, MASTER APP CONTROL, Play Store allowlist and NVIDIA planner key. The UI Agent is not a general silent background worker; it must observe the live Play Store screen and verify the installed package before success. The exact taught canonical command and exact origin of the “verified Vyom update” response still need a focused reproduction test; do not broaden install permissions or weaken signer/hash checks.
- No code fix or CI run is claimed in this note. No APK produced. No Android `main`, Render, Hermes backend or production Supabase changes.
- Next order: (1) verify token permission/access and repeat the three release probes; (2) reproduce the exact taught-phrase classification with a regression test, then fix only the routing/target extraction defect while retaining the verified-Vyom APK gate; (3) trace the generic in-app task request into the existing UI Agent and add a bounded, verified app-task route only where supported; (4) update HANDOFF and run both CI workflows on the exact source HEAD. Factory typed-module/REPOSITORY_CHANGE work remains behind these owner acceptance failures.


## Follow-up: reproduce and fail closed on malformed Play Store phrase — 2026-11-05

- Exact owner phrase inspected: `Install Godot From going play store and search godot confirm godot app then install it my phone and open it that time`. The existing permissive Play Store regex parsed the app target as **`Godot From going`**, not `Godot`. This is a concrete routing defect; the command grammar expects a single supported action such as `Install Godot from Play Store`, not a multi-step workflow description.
- Added regression test `playStorePhraseMustNotAbsorbExtraInstructionsIntoTheAppName` and changed `ActionEngine` to fail closed (BLOCKED) when Play Store target text contains instruction-noise words, while preserving the valid canonical command and generic APK install behavior. Test commit `d617f7519d4d23e8016a9e3333999e5c7a3e22e1`; implementation commit / current source HEAD `05dca634951ca03770f5f139c2c2d1281eb36d7c`.
- Exact-head Full CI push run `42020998098` was created for `05dca634951ca03770f5f139c2c2d1281eb36d7c`; status was pending and no job had started at last inspection. Do not call the test or fix green until the real job log completes successfully. No APK produced.
- Source trace confirms Play Store execution is separate from generic APK installation: `INSTALL_PLAY_STORE` calls `openPlayStoreSearch`, which opens the Play Store and schedules the UI Agent in `USER_REQUESTED_INSTALL` mode. It requires enabled Vyom Accessibility, MASTER APP CONTROL, Play Store enabled in App access, and a saved NVIDIA planner key. The UI Agent must observe the live listing and verify the installed package; it must not tap a guessed listing or report success without package-manager evidence. Generic `INSTALL` remains restricted to the verified Vyom update flow / selected APK path. Generic app-open actions do not automatically start a multi-step UI Agent session; other in-app work currently needs the explicit UI Agent flow.
- The linked GitHub connector successfully read the private repository's releases API and found existing releases containing `vyom-update.json` and signed APK assets. This confirms the repository endpoint and release assets exist, but does not test the distinct PAT saved inside the Android app. Since Factory Issues read succeeds but app release API returns 404, missing private-repository access or fine-grained token **Contents: Read-only** is the leading explanation. Owner must update/save the app token and repeat update probes; do not weaken authentication or signature checks.
- Docs refreshed after the physical failure. Owner acceptance remains FAIL until all three update probes pass and the Play Store flow is physically verified. No Android `main`, Render, Hermes backend or production Supabase changes.


### Exact-head CI result — Play Store parser correction (2026-11-05)

- Exact code/documentation candidate SHA: `9900bff8bf5a7326131af62c1d61a186fa893ee4` on `feature/memory-intelligence`.
- Full CI run `42021183283`: SUCCESS. Job `127224861338` completed successfully; Termux syntax/intake tests, secret scan, unit tests/debug APK, package/checksum and upload all passed. Artifact `13411378566` (`vyom-phase6-9900bff8bf5a7326131af62c1d61a186fa893ee4`), ZIP digest `sha256:d8e375d6c47c550295b59446b931f15414357c9f33021de2d8b23bcc232817d6`.
- Debug CI run `42021183325`: SUCCESS. Job `127224058706` completed successfully; APK build/tests, checksum packaging, signed no-APK demo package, and artifact upload passed. Artifact `13412660928` (`vyom-0.3.1-9900bff8bf5a7326131af62c1d61a186fa893ee4`), ZIP digest `sha256:15b8168f06cd88f953ff3b59f495d589f7482ae0c833acc6a1439d4f971a6cb9`.
- These are CI/build results only. They do not prove the updated on-device PAT can read private releases, do not prove update/content probes pass on the phone, and do not prove Play Store install or general UI Agent execution. Physical verification remains NO until owner reports actual probes.
- Owner reports the existing fine-grained token permissions have now been updated. Next owner probe: RUN ALL CHECKS, CHECK FOR UPDATES, CHECK NO-APK CONTENT UPDATES. Capture only non-secret result text/request ID. Do not install an APK during update-channel verification.
- Play Store parser source fix is included in this exact SHA; physical test still requires the canonical phrase `Install Godot from Play Store`, Accessibility, MASTER APP CONTROL, Play Store App access, and NVIDIA planner key. Verify installed package evidence before marking PASS.
- No APK delivered. No Android `main`, Render, Hermes backend, or production Supabase changes.


### Shared GitHub token permission caveat — 2026-11-05

- The current app setting `github_proposal_token` is reused by the private release/update client and Factory GitHub write workflows. Therefore Contents: Read-only is sufficient for release discovery/download reads but is insufficient for Factory operations that write repository content (for example explicit screenshot Attach) or create/update reviewed code proposals.
- Do not tell the owner that Read-only preserves every Factory capability. For the current single-token implementation, preserving both update reads and Factory repository writes requires Contents: Read and write, plus the separately required Issues/Pull requests permissions. Alternatively, implement distinct least-privilege update and Factory-write credentials as a separate reviewed change; do not assume that split exists today.
- The owner has just updated the token to Read-only per the update-404 diagnostic. First test the update probes. Before claiming full Factory write readiness, restore Contents: Read and write on that same token (or wait for a separately implemented credential split) and verify a harmless authorized write probe. Never expose the token.


### Token permission claim verified against current Android source — 2026-11-05

- Re-opened the exact current `MainActivity.kt` source on `feature/memory-intelligence`. The Settings token field hint explicitly says: `GitHub token: Contents + Pull requests + Issues read/write (encrypted on device)`. The same stored key `github_proposal_token` is read by the release client, self-modification proposal flow, Factory issue audit comments, and Factory task/diagnostic paths. `GitHubReleaseTokenStore.read(context)` reads that same preference key from encrypted preferences.
- This confirms there is one shared credential, not separate read-only update and read/write Factory credentials. Contents: Read-only may be enough for private release read, but it is not enough for the app's documented self-change content/PR workflow. Keep Pull requests and Issues read/write as the Settings hint specifies. Do not claim full Factory write readiness under a read-only Contents token.
- No app code changed in this audit. The next safe sequence remains: owner verifies the three update probes; then, before using Factory/self-change writes, ensure the shared token has Contents read/write + Pull requests read/write + Issues read/write. Never share the PAT.


### Shared GitHub token restored and resaved — owner report (2026-11-05)

- Owner now reports that the shared fine-grained GitHub token has been updated to the required read/write permissions and resaved in Vyom Settings after the earlier incorrect Read-only recommendation.
- Current app architecture still uses one `github_proposal_token` for private release/update reads and Factory/self-change writes. Required current scopes remain: repository Contents **Read and write**, Pull requests **Read and write**, Issues **Read and write**; Metadata read is automatic.
- This is an owner-reported credential update/save only. It does **not** prove the Android app can authenticate against private Releases, download/verify the signed manifest/assets, or perform Factory writes.
- Next phone verification, in this exact order: (1) Settings → **RUN ALL CHECKS**; (2) **CHECK FOR UPDATES**; (3) **CHECK NO-APK CONTENT UPDATES**. Do not install an APK during these probes. Report PASS/FAIL and only non-secret HTTP message/request ID; never share the PAT.
- If any probe fails, diagnose its actual response before changing permissions, endpoints, or signature checks. Keep physical verification NO until results are supplied. No APK 1 authorization from this report alone.

### Owner update probes after token resave — 2026-11-05

- Owner physically reran all three checks after updating/resaving the GitHub token. Results: `RUN ALL CHECKS` — signing certificate PASS and stable release identity PASS, but update diagnostics FAIL HTTP 404; `CHECK FOR UPDATES` — HTTP 404; `CHECK NO-APK CONTENT UPDATES` — HTTP 404, active content slot unchanged at version 1.0.0, performance baseline not established.
- This is a real owner-device failure. Do not mark update access or Stage 6 acceptance PASS. Signing identity is verified; it does not explain the HTTP 404.
- Source audit at this branch confirms `GitHubReleaseAssetClient.RELEASES_API` is `https://api.github.com/repos/riyas433554-dot/vyom-android/releases?per_page=30`; both APK and no-APK clients use this same endpoint and `github_proposal_token`. The content updater catches the same client error and reports the unchanged active slot.
- Independent linked GitHub connector check confirms repository `riyas433554-dot/vyom-android` currently exists and is private, and its Releases API returns releases containing `vyom-update.json`, signed APK assets and `vyom-content-update-demo.zip`. This rules out a missing repository/release feed from the connector's credentials; it does NOT prove the token stored on the phone can access the private Releases API.
- Most likely remaining cause is app-token access to this exact private repository (fine-grained token Repository access selection) or its effective Contents permission/validity. Permission scopes alone are insufficient if Repository access is not set to `Only select repositories` → `vyom-android` (or `All repositories`). Confirm token is active/not expired and Contents is Read and write; retain Issues and Pull requests Read and write. Do not create another token or change endpoints yet.
- Next owner check: GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens → the exact token saved in Vyom → verify Resource owner `riyas433554-dot`, Repository access includes `vyom-android`, and Contents Read and write. Never share token value. If these are all correct, next source improvement should expose only the GitHub error response message and `X-GitHub-Request-Id` in diagnostics (never Authorization headers/token), then retest with a newly built APK if source changes.
- No code changes, no token read by assistant, no APK delivered. No Android `main`, Render, Hermes backend or production Supabase changes.


## Follow-up: expose safe GitHub release API failure details — 2026-11-05

- Owner confirmed that the existing fine-grained token's repository access and all required permissions are correct, while the installed app still receives HTTP 404 from both APK release discovery and no-APK content updates. Do not ask the owner to rotate another token without evidence.
- Added focused regression coverage in \`GitHubReleaseAssetPolicyTest\` for extracting only GitHub's JSON \`message\`, including a safe \`X-GitHub-Request-Id\`, rejecting unrelated response fields, sanitizing escaped control characters, and falling back to the HTTP status when no message is available. Test commit: \`3e9fc5613bc20d107243588572d02b36cf8c29b5\`; fixture correction: \`0bb6a9a2f023ea44208e7b236abe5181775f1087\`.
- \`GitHubReleaseAssetClient\` now reads at most 8 KiB from the HTTP error stream and surfaces only the bounded GitHub \`message\` (max 240 chars) and a validated request ID (max 80 safe characters). Applied to release-list, asset API, and trusted asset-download HTTP failures. It never logs or returns Authorization headers, PAT values, raw response bodies, documentation URLs, or arbitrary response fields. Error-body read occurs before disconnecting the connection. Code commits: \`4eed34ecc32562b744dbe3cdce8f3d1e9a9cf256\`, \`949fdb47370e6332880abcce5e8dba93d85fbc5d\`, \`c514280d99173f966b1018f18b425698f546c1ba\`, \`01c014cc3e078828a43942f4aae7150505ff3c5d\`.
- Existing UI already displays \`error.message\` for Check for Updates and the update diagnostics row, while the no-APK content update result displays the caught exception message. Therefore the enriched safe message should be visible in all three probes without changing UI or update/signature policy.
- Verification status at documentation time: source and tests are committed, but no Gradle test or exact-head CI result has yet been observed. Do not claim this fix works or deliver an APK until Full CI and Debug CI succeed on the exact final branch tip.
- Next: inspect exact branch tip and workflow runs, fix any real CI failures from logs, then owner installs the new signed candidate and reruns RUN ALL CHECKS, CHECK FOR UPDATES, and CHECK NO-APK CONTENT UPDATES. Share only non-secret error text and request ID. If response says \`Not Found\` despite the confirmed token settings, correlate the request ID with GitHub Support / audit evidence; do not weaken authentication or signature checks.
- No Android \`main\`, Render, Hermes backend, production Supabase, token rotation, APK delivery, or physical-device PASS claim.


- Diagnostic follow-up correction: allow GitHub request IDs containing colon separators and add a regression assertion. Commits: `42de6e5ac4619f37589dc30acc79933dcbe8a6d5`, `e48f79e8dbff573ba87230e4271925101ca5ec27`. The implementation remains pending exact-head CI.


## CI failure follow-up — 2026-11-05

- Exact-head CI for `f59ce5c6e73313dd038d86c16e828ab6e992e290` failed in both Debug run `42027129529` and Full run `42027129532`. Both reached unit tests; 490/491 passed. The sole failure was `GitHubReleaseAssetPolicyTest.includesOnlyGitHubErrorMessageAndRequestIdInDiagnostics` at line 39: implementation intentionally preserves a colon in validated GitHub request IDs, while the test expected a hyphen.
- Corrected only the stale test expectation to preserve `REQ_123:Abc` (source behavior unchanged). Test-fix commit: `0fea777b9e3a4b8e465b1ddcc17bfe37a2421d0f`. Fresh exact-tip CI is required; do not treat the previous failed runs as passing.
- Issue #101 remains OPEN. Factory proposal action exists, but its current one-file Kotlin allowlist excludes MainActivity/control dispatch and other protected routing files. Do not claim #101 is executable through the current proposal route until the root-cause file and safe Factory policy path are proven. Do not manually patch #101 outside Factory.


## Universal Factory Step 1 audit refresh — 2026-11-05

Scope: source/documentation audit only; no APK, no product feature changes, and no changes to Android `main`, Render, Hermes backend or production Supabase.

| Domain | Verified existing capability | Actual gap / status |
|---|---|---|
| Complaints / Issues | Canonical GitHub Issues register; cross-area intake; idempotency/source identity; P0–P3 ordering; edit/status/probe lifecycle; screenshot share/Attach source path; external-agent/Termux intake contracts and tests | Source-wired but owner acceptance remains incomplete (Termux, external agents, phone evidence, screenshot Attach). Registered backlog is visible but paused; it is not automatically repaired. |
| Errors / Diagnostics | Sanitized Factory worker/diagnostic failure routing, issue comments, execution journal history and Diagnostics journal readability | No general error-to-remediation planner that diagnoses arbitrary errors, selects a repair capability, runs it, and verifies recovery. |
| Design / UI | Design system and UI components exist; design-related complaints can be registered and tracked | No live Factory design specialist that turns a design request into an implemented UI change, screenshot comparison, verified PR and rollback. Acceptance tests still mark multiple design criteria PARTIAL/UNVERIFIED. |
| Android Control | Existing UI Agent, capability manifest, policy gates, STOP/foreground guards and bounded observe/plan/act/re-observe foundations | Not universal: acceptance matrix says A17 PARTIAL; no 20-domain adapter/Diagnostics suite; no demonstrated Factory specialist invocation; physical device subset pending. |
| Memory | Memory capture/store and semantic/lexical retrieval components exist elsewhere in app | No proof Factory invokes Memory as a specialist, uses shared task context/memory safely, or resolves memory conflicts as part of a Factory workflow. Memory acceptance remains PARTIAL/UNVERIFIED in acceptance docs. |
| Repository / Skills | Public GitHub repository discovery, metadata/default branch/immutable SHA lookup; constrained root `SKILL.md` import with SPDX allowlist, owner review, inert app-private storage and integrity metadata; guarded one-file Kotlin PR proposal path | No general repository inspect/classify/dependency/license/security/build/test/install adapter; no production `repository_change` adapter. Existing proposal is owner-directed, one-file and approval-gated—not autonomous repair. |
| Shared execution | Canonical issue identity, task inbox, local lease, fresh remote snapshot, journal, retry schedule, STOP/foreground/consent gates, postcondition verification contract, lifecycle tests | Only one foreground runner and one concrete `android_action:set_media_volume` adapter are registered. `repository_change` is recognized by the envelope but unsupported by production registry. No persistent app-closed/Doze/reboot worker or cross-device atomic claim. |
| Planning / Discovery / Recovery | Existing bounded prompt/proposal and typed execution envelope; retry policy for known transient idempotent work | No universal planner, specialist-module registry, runtime capability discovery/fallback workflow, generic remediation loop, or shared rollback coordinator across modules. |

Evidence inspected: `docs/VYOM_FACTORY_PROTOCOL.md`, `docs/ACCEPTANCE_TESTS.md`, `docs/ANDROID_CONTROL_CAPABILITY_MATRIX.md`, `VyomFactoryDialog.kt`, `VyomFactoryExecutionRunner.kt`, `VyomFactoryExecutionAdapterRegistry.kt`, `VyomFactoryAndroidActionAdapter.kt`, `VyomFactoryWorkerSession.kt`, `MainActivity.kt`, and the canonical GitHub issue register (including open #101). Existing lifecycle/order/lease/retry/evidence tests are present; CI and JVM tests do not prove physical Android acceptance.

**Audit conclusion:** Step 1 was partially done in the earlier 2026-11-05 audit, but this refresh confirms it is not equivalent to the full Universal Factory. The core queue/journal/lease/verification mechanisms should be reused. The largest missing architecture pieces are a typed specialist registry/capability contract, a universal planner/discovery seam, and a guarded repository-change adapter. Do not create another queue, journal, lease, approval, retry or evidence system. Keep product-domain feature implementation in the post-APK-1 backlog; only Factory-core capability work belongs in APK 1 unless the owner explicitly changes scope.

**Next locked sequence:** finish the remaining Step 1 inventory against every module and exact CI/source tip; then Step 2 write the versioned shared task/module/capability contract and gap map; only then Step 3 implement the smallest missing Factory-core capability test-first. No APK decision until the end-to-end gates in Step 4.


## Universal Factory scope correction — automatic complaint repair is CORE (2026-11-05)

Owner clarified that automatic sequential repair of registered complaints is not unrelated backlog and must not be deferred outside APK/Factory scope. Correct interpretation: one Universal Factory manages every registered complaint and all specialist domains. Complaint intake alone is insufficient; the same orchestrator must automatically discover eligible issues in P0→P3 priority order (FIFO within tier), classify/plan, discover a specialist capability, execute bounded safe work, test, verify, record evidence and advance to the next eligible task. Unsafe or approval-gated work waits for explicit owner approval; blocked work records exact reason/evidence and the scheduler continues where safe. Never mark a task completed without its required postconditions/device probes.

Step 2 architecture contract added: `docs/UNIVERSAL_FACTORY_ARCHITECTURE.md`, commit `6969708fb5793ac0cd2f01b7a178304c1b441dbf`. It defines one shared Task Engine (reuse existing issue/inbox/lease/journal/retry/approval/evidence), typed task envelope, universal planner, capability/specialist registry, risk gates, rollback, shared history/memory, scheduler fairness/resilience and domain E2E gates. Specialist domains are modules behind one orchestrator, never separate factories.

Correction to prior Step 1 audit language: the row saying registered backlog is paused and not automatically repaired describes the current implementation gap, NOT a decision to keep automatic complaint repair out of scope. The earlier statement to keep product-domain implementation outside APK 1 is superseded for Factory orchestration and its required specialist adapters. The final APK decision remains deferred until end-to-end gates show whether native changes are needed.

Next implementation order: (1) finish source inventory and diagnose exact-head CI failures; (2) TDD the typed capability registry and universal envelope on top of the existing runner; (3) planner/classifier and priority/FIFO drain; (4) guarded repository-change adapter with isolated branch/PR/CI and owner approval; (5) specialist adapters and E2E gates. Continue in batches, update this handoff after each batch, and do not duplicate existing engine primitives.


## Universal Factory Step 3 — capability registry foundation (2026-11-05)

- Added test-first contract in `VyomFactoryCapabilityRegistryTest.kt` (commit `8669e99c7118c60a3c487000238e380b97889757`) for available-only discovery, domain/operation matching, explicit unavailable reasons, duplicate-ID rejection, R2/R3 mutation approval, rollback declaration and fail-closed unknown IDs.
- Added `VyomFactoryCapabilityRegistry.kt` (commit `bb5cc0f5a0d8fd66b89110139cb85e81b87de486`). It is a typed discovery index only; it does not execute capabilities and is not yet wired into the production runner. Existing execution remains behind the current fenced adapter registry. This is the first foundation step, not a completed Universal Planner/specialist integration.
- Current descriptor captures stable ID/version, domain, operations, risk tier, owner approval requirement, mutability, reversibility/rollback handler, idempotency and availability diagnostics. Duplicate IDs and unsafe/incomplete descriptors fail closed.
- CI: exact-head run `42029169347` (Vyom Android CI) was queued at observation time for `bb5cc0f5a0d8fd66b89110139cb85e81b87de486`. Debug/Full runs on prior docs/test-only commits failed quickly; GitHub returned no job steps/log URL and the log artifact endpoint returned 404, so root cause is not yet established. Do not label CI green or infer a Kotlin failure from those runs.
- Next: poll exact-head CI; inspect workflow configuration/runner startup if the quick failures recur. Then wire existing adapters into capability descriptors and runner selection with tests, implement the issue classifier/planner and stable P0→P3/FIFO sequential drain, then guarded repository change execution. Keep automatic registered-complaint repair in core scope. Do not add a second queue/lease/journal or produce an APK.

- Compatibility hardening after source review: replaced Java 8 `Map.putIfAbsent` with `containsKey` + assignment to avoid depending on a newer Android API surface in the registry. Commit `64b23e5210e88b62c2c833737f427f4b71233567`. Exact-head CI still must prove compilation/runtime compatibility.


## Universal Factory Step 3 — production adapter capability wiring (2026-11-05)

- Added test-first operation-level dispatch coverage in `VyomFactoryExecutionAdapterRegistryTest`: a typed adapter must reject an undeclared operation before invocation and accept only its declared operation. Test commit: `76364833bd17073d54947403d87a32d00f091a04`.
- Extended the existing adapter contract with typed capability descriptors and added operation-aware support checks in the existing adapter registry. Production Android action adapter now advertises only `android_control.set_media_volume` (R1, mutating, idempotent); unknown Android operations fail closed as unsupported. Existing legacy/test adapters with no descriptors retain the prior target-only behavior, so migration remains incremental rather than breaking unrelated runner fixtures. Commits: `06de392f0f0ab304b3b8bf9d8ae361efd990630c`, `ab22a2fe84499aa5a3b346145bfba31f6f60778d`.
- Runner now checks the full envelope against the registered operation capability before creating an execution attempt; unsupported operations are skipped as `capability_unsupported`, without calling an adapter. Commit: `107584e2943e484f64f3357adcf5261a9ac882fe`.
- This is a bounded integration step, not a Universal Planner, issue classifier, priority/FIFO scheduler, repository-change adapter, or automatic complaint repair. No new queue/lease/journal was added. The only production capability remains media volume.
- CI diagnostic: prior run `42029404953` failed in one `debug` job. GitHub returned no steps; downloading job logs returned BlobNotFound (request ID `141cb5cf-f01e-0099-1623-6de739000000`). Combined commit status exposed no status entries. Root cause remains unknown; do not attribute it to source or infrastructure without further evidence.
- Exact current source tip after this batch: `107584e2943e484f64f3357adcf5261a9ac882fe` before this HANDOFF commit. CI must be checked on the final documentation-updated branch tip. No Gradle test, CI success, artifact, or APK is claimed.
- Next: verify the exact tip's CI and source compilation; then add explicit runner regression coverage for unsupported operations being skipped before journal/adapter invocation. Continue with planner/classifier and P0→P3 FIFO sequential draining only after the operation-level capability gate is proven. Preserve branch/environment guardrails; no APK.


## Universal Factory Step 3 — runner fail-closed regression (2026-11-05)

- Added `unsupportedTypedCapabilityIsSkippedBeforeJournalOrAdapterExecution` to `VyomFactoryExecutionRunnerTest` (test commit `3d3d6cd3964b3e2e34b77d402d39aa4fc74be3bc`). It configures the production-shaped typed capability list to expose only `set_media_volume`, submits `launch_app`, and asserts: outcome is `capability_unsupported`, adapter invocation count remains zero, journal remains empty, and the exact worker claim is released.
- The runner's existing source order was inspected: it parses the fresh remote envelope, checks `adapters.supports(envelope)`, and only then performs consent/journal/adapter work. This regression test locks that contract. No additional production logic was needed for this specific case.
- Test was added but not executed in this environment: the GitHub connector exposes source and workflow-run inspection but no local Gradle runner or workflow-dispatch action. Do not call it RED/GREEN or PASS without an actual run.
- Current next step: retrieve exact branch tip and CI/workflow state, then inspect the existing canonical issue ordering implementation before designing any planner/scheduler changes. The HANDOFF earlier states newest-updated-first within priority; that conflicts with the corrected FIFO-within-tier requirement and must be resolved in the existing selector, not by creating a second queue.
- No APK, no backend/Render/main/production Supabase changes.


## Universal Factory Step 3 — stable FIFO correction (2026-11-05)

- Source audit found the active selector contradicted the owner's locked rule: `VyomFactoryTaskOrdering` sorted P0→P3, then `updatedAt DESC`, allowing newer updates/promotions to jump ahead of older issues within the same tier.
- Added regression tests first (commit `e3a26ef3151fdc734ee700cf5990cd88e5df4840`) proving priority remains primary, creation time is FIFO ascending, later updates do not change queue position, and issue number is a deterministic tie-break for equal creation timestamps.
- Added `createdAt` to the existing GitHub task model (trailing default to `updatedAt` preserves existing constructors) and populated it from GitHub `created_at`; changed the existing canonical ordering function to P0→P3 then `createdAt ASC` then issue number. Commits: `097c149ba67e5c82df6258db78177a1b9844a156`, `c23c07319311b70d2d0376eda88e47ee1323a952`.
- This modifies the current selector used by the existing worker claim coordinator; it does not add a second queue. GitHub task listing already fetches all pages (up to the existing bounded 10-page cap) and filters canonical Factory metadata. Physical acceptance and distributed multi-device locking remain unproven.
- Runner fail-closed regression was added in commit `3d3d6cd3964b3e2e34b77d402d39aa4fc74be3bc`: unsupported typed operation must skip before adapter invocation/journal and release the claim. Neither this test nor FIFO tests have been executed here; do not claim pass.
- Exact latest code tip before this handoff refresh: `c23c07319311b70d2d0376eda88e47ee1323a952`. Previous CI diagnostics remain inconclusive; obtain exact-head CI and real Gradle logs before claiming verification. No APK.
- Next: inspect and implement the universal task classifier/planner seam over existing canonical issues and capability registry. Must retain blocked/approval outcomes and sequentially advance to next eligible issue; no duplicate queue, lease, journal or retry store.


## CI root-cause evidence — GitHub runner never started (2026-11-05)

- Retrieved repository Actions API directly for exact branch tip `df3f66d4f1a8d0294af0ad5ea2d0d3e20e124c15`.
- Debug run `42030359440` and Full CI run `42030359545` both failed within ~3 seconds. Their jobs had `runner_id=0`, empty `steps=[]`, and no runner name. This means the job never reached Checkout/JDK/Gradle or any repository step; these failures do not establish a source/test failure.
- Retried both failed jobs using GitHub Actions rerun. Latest attempts again failed within ~2 seconds with `runner_id=0`, `steps=[]` (Debug latest job `127255384638`; Full CI latest job `127255418220`). This is strong evidence of runner provisioning/service failure before job execution, not a Kotlin compile/test result. Keep status as CI unavailable/runner-start failure; do not report PASS.
- Workflow definitions confirm both workflows are configured for push to `feature/memory-intelligence`; the runs were created from push events. No workflow source change is indicated by this failure evidence.
- The exact branch head at this observation was `df3f66d4f1a8d0294af0ad5ea2d0d3e20e124c15`. Both latest-attempt runs are still failed; rerunning has not fixed runner provisioning.
- Next: continue source/test work without treating these failed runs as code regressions, then retry exact-head CI after GitHub runner service recovers. If failures again have a real runner ID and step logs, diagnose those logs. Do not alter workflow or signing secrets based on runner_id=0 alone.


## Universal Factory Step 3 — conservative domain classifier foundation (2026-11-05)

- Added test cases for unique Android-control, diagnostics and Design/UI signals; ambiguous cross-domain issue text; unknown issue text; and hidden execution metadata not influencing classification. Test commit: `563ad3c02bb3736f90d28df51795f0585f9d322e`.
- Added `VyomFactoryTaskClassifier` (commit `3b75c762b0c5489b05eae4edbb34ab8712d16166`). It is deterministic and conservative: strips hidden Factory metadata/execution comments, scans bounded issue text, classifies only when exactly one domain signal is present, and returns explicit clarification reasons for ambiguous/unknown complaints.
- Important limitation: this is only a first-pass domain classifier. It does not infer executable operations, generate a plan, select a capability, or run a specialist. Do not treat keyword matches as a completed universal planner; false negatives deliberately go to owner clarification, and domain-specific intent parsing remains to be implemented/tested.
- FIFO implementation and regression tests are recorded above. Capability-dispatch runner regression is also recorded above. All newly added tests remain unexecuted because GitHub Actions runner provisioning is failing before any job step; no Gradle PASS is claimed.
- CI evidence on preceding source tip `df3f66d4f1a8d0294af0ad5ea2d0d3e20e124c15`: Debug run `42030359440` and Full CI `42030359545` both fail with `runner_id=0`, zero steps; reruns also failed before runner startup. The current tip after classifier and this handoff must be checked again when GitHub runners are available.
- Next: integrate classification with capability discovery as a typed planning result (READY / NEEDS_OWNER / BLOCKED_CAPABILITY), without dispatching unsupported work; then sequential scheduler drain must continue after safely recording blocked tasks. Preserve all owner-approval and verification gates.


## Universal Factory Step 3 — capability-aware planning boundary (2026-11-05)

- Added planner tests for: candidate discovery limited to the classified domain; no registered specialist becomes a typed capability block; unavailable specialist preserves its diagnostic reason; unknown request remains owner clarification. Test commit: `3972f2d99473038068cbfed239f63e44f027d146`.
- Added `VyomFactoryTaskPlanner` (commit `0d6ef3659a92ce76af0a7ecdb5f322c56d630a00`). It composes the conservative classifier with the existing capability registry and returns one of `CapabilityCandidates`, `BlockedCapability`, or `NeedsOwnerClarification`. Candidate discovery is domain-scoped and deterministic.
- Safety boundary: candidate discovery is NOT execution readiness. Planner intentionally does not invent an operation, arguments, plan DAG, or invoke adapters. A domain with no registered capability is explicitly blocked; unavailable descriptors preserve their reason. The only currently registered production capability remains Android media volume.
- Tests remain unexecuted due to GitHub Actions jobs failing before runner startup (`runner_id=0`, empty steps); no Gradle/test pass is claimed.
- Current code tip before this handoff refresh: `0d6ef3659a92ce76af0a7ecdb5f322c56d630a00`. Next: wire the planner into the existing issue execution path as a non-mutating triage stage, record blocked/clarification outcomes in the canonical issue journal/comments, and ensure the scheduler continues to the next eligible FIFO issue. Do not silently run candidate-only plans.


## Follow-up CI observation and planner wiring hardening (2026-11-05)

- Exact push runs on source `0d6ef3659a92ce76af0a7ecdb5f322c56d630a00`: Debug `42030846240` and Full CI `42030846337` failed with `runner_id=0` and zero steps. The documentation-tip Debug run `42030877834` failed with the same runner_id=0/zero-step signature. No source command ran; tests/compilation remain unverified.
- Replaced the planner's default classifier callable reference with an explicit lambda for conservative Kotlin compiler compatibility. Commit: `38c0bd18390af2008c308fa9cbda5a20642fd4bf`.
- The classifier/planner are still standalone foundations, not yet invoked by the production worker. Next integration must preserve the existing runner's fresh-snapshot, lease, STOP, consent and journal sequence; candidate-only plans must not dispatch. Record blocked/clarification outcomes and then continue to the next FIFO issue only after a safe durable state transition exists.
- Current source tip before this HANDOFF update: `38c0bd18390af2008c308fa9cbda5a20642fd4bf`. CI must be retried on the new exact tip when runners start successfully. No APK.
## Universal Factory triage integration — 2026-11-05

- Added `VyomFactoryTaskTriage` as a read-only pass over the fresh canonical GitHub issue snapshot. It filters to open `open`/`in_progress` issues, uses the existing P0→P3/FIFO ordering, and returns typed planner outcomes plus stable, content-free event keys. Test-first specification: `VyomFactoryTaskTriageTest.kt` (test commit `e9932b589904f382b07b7c526cca24be146d4b03`); implementation commit `f197a32e04b855b75178e0d5d076bc5dcf29e7eb`.
- Wired the triage pass into the foreground `RUN NEXT` path before the existing execution runner. Each new outcome is persisted through the existing GitHub issue metadata event/comment path using `updateTask(additionalEvent=...)`; stable event keys prevent repeat writes. If persistence fails, the run fails closed before any Android action is attempted. Integration commit `136d154d89cddb17912ad0ab9b0ec1a12d168b67`.
- A discovered capability is explicitly **not** execution-ready (`executionReady=false`). The existing runner still independently fetches the issue again and enforces envelope parsing, exact supported adapter operation, fresh state/source identity, lease, STOP, one-time owner confirmation, journal and verified postcondition. No candidate-only plan is dispatched.
- The triage registry is currently built from the production Android adapter's declared capability list; all other classified domains therefore produce explicit `no_registered_capability` until their specialist adapters are registered. This is a capability gap, not a completed universal specialist system.
- Important remaining gap: the current UI path still performs one executable action per `RUN NEXT` tap. This batch adds durable classification/planner outcomes and allows non-executable/blocked issues to be accounted for without claiming success, but does not yet implement unattended multi-task drain or arbitrary complaint repair.
- Tests have been added but **not executed**. GitHub Actions runner provisioning failures previously showed `runner_id=0` and zero steps; verify exact-head CI and real Gradle output before claiming compilation/tests pass. No APK, no physical-device PASS, and no backend/Render/main/production-Supabase changes.
- Next: verify latest branch head and CI; add a tested scheduler/drain coordinator that advances across blocked/clarification outcomes and continues only after durable recording, while preserving one-at-a-time approval and execution permits. Then implement the guarded repository-change specialist and remaining domain adapters. Do not produce an APK before the full E2E gates.
## Universal Factory bounded sequential drain — 2026-11-05

- Added `VyomFactoryDrainPolicy` and regression coverage for continuation after verified success, recoverable failure and non-terminal skip; stop on STOP, fence, busy or no-work. Policy commit `18d772091526ef14dd0a9c9c3d7626fa17355af4`; test commit `501af337ef04579fceedfc4f8bb34628d92eda19`.
- Replaced the one-call `runner.runNext()` path with a bounded foreground drain of up to 20 distinct eligible issues per RUN NEXT tap. Each result is tracked by issue number so a declined confirmation or recoverable skip cannot cause an infinite retry loop in the same batch. Each iteration still calls the existing runner, so every issue gets a fresh snapshot/claim and the same execution permit, STOP, foreground-generation, approval, journal, retry and verified-postcondition gates. Dialog integration commit `6761df3e8fb234b8c5cbf7c509ae5298ae1a4f4b`.
- Verified successes and failures from every item are written through the existing execution audit comment path; UI now reports verified/failed/skipped counts and indicates when the 20-task bound is reached. No new task queue, lease, journal, retry store or execution adapter was introduced.
- This is a foreground, user-started bounded drain—not an unattended background scheduler. STOP or Activity foreground-generation change ends the batch; physical-device approval and postcondition behavior remain unverified.
- Exact branch head before this HANDOFF update: `6761df3e8fb234b8c5cbf7c509ae5298ae1a4f4b`. The preceding exact-head CI runs on `c1659f0ea1e874c78cd60dffc31886dadaef978f` were Debug `42033598725` and Full CI `42033598501`; both failed before runner startup (`runner_id=0`, `steps=[]`). The new drain changes have not yet had CI or Gradle execution. Do not claim compilation/tests pass.
- Next: check exact updated-head CI; if runner service still fails before steps, continue bounded source review and tests without claiming verification. Then make planner triage and execution outcomes visible per issue, add drain-level integration tests, and implement the guarded repository-change specialist with owner approval and isolated branch/PR/CI. No APK until all Factory E2E gates are met.
## Sequential drain safety hardening and progress — 2026-11-05

- Tightened drain continuation: a per-issue execution error may advance, but shared GitHub/auth/rate-limit/server/network failures stop the batch to avoid cascading repeated failures. STOP, fencing, busy and no-work remain terminal. Added regression coverage for the systemic failure categories; test commit `d046c328d7d62be5be6d928cf11eeb188d0d3b79`; policy commit `01446a4068b56301476abca3d8cf12ed8917b2a5`.
- Added per-issue foreground progress updates while draining; commit `ed6751bf1f33bd8c7283fe73d3f9671c28e7e9f3`. The user can see each verified/failed/skipped task as the bounded batch proceeds.
- Source review confirms `VyomFactoryExecutionAudit.comment` accepts the common runner outcome type and emits audit comments only for verified success/failure; skips do not get mislabeled as completion. The drain persists audit comments for each terminal outcome, not just the final one.
- Exact source tip before this HANDOFF update: `ed6751bf1f33bd8c7283fe73d3f9671c28e7e9f3`. CI query for this code tip had not returned workflow runs at observation time. Previous exact HANDOFF-tip runs `42033961769` (CI) and `42033961947` (Debug) were retried; attempt 2 again failed with `runner_id=0`, no runner name and `steps=[]`. No Gradle/test command ran. Do not claim PASS.
- Next: inspect exact updated-tip CI; continue source-level audit and add a drain integration test around the visited-issue selector and skip/continue behavior. Then build the guarded repository-change adapter: isolated feature branch only, bounded allowed paths, generated diff review, CI verification, owner approval before PR merge; never auto-merge or target `main`.
## Exact-head CI refresh — 2026-11-05

- Current exact code+HANDOFF tip at this refresh: `325ad900af0af7ca5a4e70b02e3f451d67dec6a2`. Both push workflows ran on this exact SHA: Debug `42034254047` and Full CI `42034253917`.
- Both workflows failed before any repository step: their latest jobs reported `runner_id=0`, empty runner name and `steps=[]`. This matches the prior runner-provisioning failure signature; it does not establish Kotlin compilation or test failure. No Gradle/test command executed and no artifact was produced.
- The bounded drain and triage code remain unverified at runtime. Continue source review, but do not label CI/tests green or create an APK until real runner execution and all Factory E2E gates pass.
## Drain coordinator extraction and regression coverage — 2026-11-05

- Added `VyomFactoryDrainCoordinator` to own the bounded run loop and terminal/continuation behavior; it delegates each individual attempt to the existing runner and checks the STOP/foreground gate before starting every next attempt. It returns the complete outcome list and whether the configured bound was reached. Implementation commit `78ac206de71a836eec46a2fab8cf2f2bc94e3dc6`.
- Added coordinator tests for recoverable skip/failure/success followed by NoWork, STOP preventing the next attempt, shared rate-limit failure stopping the batch, hard maximum enforcement, and terminal fenced/busy/stopped outcomes. Test commits `9c116bdba11e14ebbc900883208ae31262422e1e` and `9fb42548c9c5145e10de23f9ce87191ec14712cd`.
- Replaced the inline UI drain loop with the tested coordinator; the production path still updates visited issue IDs and per-issue progress from the outcome callback. Integration commit `38e1ed71851f978054890f9fd58c6474670168fa`.
- Tests remain unexecuted because GitHub Actions jobs have repeatedly failed before runner allocation (`runner_id=0`, no steps). No Kotlin compilation or test pass is claimed. No APK or physical-device PASS.
- Next: audit canonical inbox population versus GitHub issue snapshot and verify every task in the Factory queue is represented exactly once; add tests around claim selector fairness and visited issue behavior. Then continue guarded repository-change specialist work.
## Canonical inbox and FIFO audit — 2026-11-05

- Audited the real production claim path: `VyomFactoryWorkerSession.claimNext()` fetches a fresh full GitHub task snapshot on every attempt, filters open `open`/`in_progress` issues, reconciles all eligible remote tasks into the existing device inbox, then the coordinator claims from the filtered snapshot. Existing queue persistence and session tests cover idempotent reconciliation, identity conflicts, closed/paused exclusion and fresh-read failure. No second queue was added.
- Found a contradictory legacy claim-coordinator test: it expected the newest-updated issue to win within a priority tier, despite the locked FIFO contract and production selector sorting by `createdAt`. Corrected the test to assert the oldest-created issue wins even when a newer issue was updated more recently; helper now sets `createdAt` explicitly. Test commit `1c44e473d5eeafe06ddcfc3c19dc52e0101e40b9`. Production ordering code was already correct; no redundant implementation change made.
- Queue audit confirms the drain's `visitedIssues` exclusion is scoped to one foreground batch, while the worker session refreshes/reconciles before every subsequent claim. A skipped issue remains in the inbox and can be reconsidered on a later user-started run.
- Tests remain unexecuted. CI runner provisioning failure remains unresolved; no Gradle PASS, APK or physical-device PASS claimed.
## Repository proposal approval gate — 2026-11-05

- Audited the existing repository-change path: `SelfModificationClient` already creates an isolated `vyom-proposal-*` branch, edits only one allowlisted Kotlin file, opens a PR against `feature/memory-intelligence`, waits for exact-SHA Debug + CI success, and contains no merge/release/install call. The UI displayed the minimal diff before the owner tapped OPEN PR, but the client API itself did not enforce that approval.
- Added an explicit `ownerReviewedDiff` gate to both proposal creation entry points; it defaults to false and is checked before any GitHub request. The only production call site passes true inside the positive OPEN PR callback, which is reached after the diff-review dialog. Direct/unreviewed calls fail closed. Implementation commit `14f39924e3ec2c0e077f26d59006eb53b3f71b40`; UI wiring commit `5c98213a6b6571f53b7d49726544f8d473c5e3da`.
- Added an integration test proving an unreviewed proposal makes zero GitHub requests. Existing success/failure/uncertain-PR integration cases now explicitly state that the owner reviewed the diff. Test commits `4b653822c29a164ade19ace8d360c2e337f244fa` and `4c9ca7872553d963d3d7dc779974906adf681f5e`.
- This hardens the existing manually initiated proposal path; it does NOT yet auto-select a source file, auto-generate proposals for every repository complaint, auto-merge, auto-install, or replace owner review. Proposal branches remain isolated and PRs remain unmerged pending owner action.
## Exact-head CI result for proposal approval gate — 2026-11-05

- Exact code+HANDOFF SHA: `3828cc987ce8dd67556c9d66a6cb8ef575cba2cd`. Debug run `42035802699`; Full CI run `42035802684`.
- Both failed before runner allocation. Retry attempt 2 also reported `runner_id=0`, empty runner name and `steps=[]`. No Checkout, JDK, Gradle, unit test, compile or APK step executed. This remains an infrastructure provisioning failure, not evidence of source correctness or source failure.
- No merge, install, backend/Render, production Supabase or Android `main` changes were made. Current branch remains `feature/memory-intelligence`.


## Universal Factory — guarded repository source-path discovery (2026-11-05)

- Added test-first coverage for repository path discovery: exact allowlisted paths only, model-invented/protected path rejection, sensitive complaint rejection before any GitHub/NVIDIA request, truncated tree rejection, and branch-change detection. Test commit: `33829a18212c5c945365a344babde03e5a7a4cd0`.
- Added `SelfModificationPolicy.validateDiscoveryTask`; proposal titles as well as complaint details are checked for sensitive content before external model use. Commit: `104faa6c0acbb6fefc790b8aab7e7d7df203e813`.
- Added `SelfModificationClient.suggestAllowedPaths`. It reads the current `feature/memory-intelligence` commit and recursive tree, fails closed on a truncated tree or more than 500 eligible Kotlin files, filters every path through the existing allowlist, asks the configured NVIDIA model to rank 1–5 suggestions, validates every returned path against the exact tree and policy, and rechecks the branch SHA before returning. Discovery performs no branch creation, source write, PR, merge, release or install. Commits: `9dc68ca2811125e807b6a2794f2a93b7c9aa3ac7`, `d3be20a5838a1bc8a26bf7fa35df5ed9750fb182`.
- Added **FIND RELEVANT SOURCE FILES** to the existing review-only proposal form. The owner still chooses the suggested file and must inspect the generated diff before OPEN PR; the existing `ownerReviewedDiff` gate remains mandatory. UI commit: `4e74974a27001de6912cf1fa88a1d09039b8d133`; local declaration-order correction: `1fcec00be550935286f7d20918201eb1ff96e328`.
- This is a read-only path-discovery improvement, **not autonomous repository repair**. It does not infer that a suggested path is definitely correct, create a proposal automatically, or remove owner review. Multi-file proposals, auto-merge and auto-install remain unsupported.
- Exact source tip `1fcec00be550935286f7d20918201eb1ff96e328`: Full CI `42037715894`, Debug CI `42037715730`. Both failed within about 2 seconds with `runner_id=0`, empty runner name and `steps=[]`; GitHub returned BlobNotFound for job logs. No Checkout, Kotlin compile, Gradle or tests ran. The prior exact code tip `d3be20a5838a1bc8a26bf7fa35df5ed9750fb182` had the same failure signature (Full `42037459675`, Debug `42037459610`). **Tests are added but unexecuted; no source PASS/FAIL is claimed.**
- Repository privacy was rechecked through GitHub repository metadata and now reports `visibility=private`. `docs/OWNER_ACTIONS.md` was corrected; do not repeat the old make-private step. Saved-token permissions and physical screenshot Attach acceptance remain owner-side pending.
- No Android `main`, Render, Hermes backend or production Supabase changes. No APK delivered and no physical-device PASS claimed.

### Next work

1. Recheck exact branch tip and retry Debug + Full CI when GitHub runner provisioning is available; only diagnose source errors from real job steps/logs.
2. Continue Factory repository specialist integration from the current read-only suggestion stage, keeping owner path/diff approval, isolated feature branch, exact-SHA CI and no-merge/no-install gates.
3. Preserve the bounded drain, FIFO, triage, issue journal, approval and verification systems; do not create duplicate queues or mark a suggested path as an executable capability.


### Follow-up test hardening and latest CI — 2026-11-05

- Made the allowlist predicate explicit in the repository-tree filter: `ba75c677e0a4ce4b7d287052a79e1abcebaa05af`.
- Extended the valid-discovery integration test to assert GitHub requests are GET-only, the recursive tree is read at the exact base SHA, and the NVIDIA request contains the allowed-path list but no source-file content. Test commits: `3a798d2d158ff40383b82425286b1f79ffe156b7`, `684bd735bfa5ad4de839e3675d1df740165f6996`.
- Exact latest source tip: `684bd735bfa5ad4de839e3675d1df740165f6996`. Full CI `42038390185` and Debug CI `42038390172` both failed; retry attempt 2 again ended with `runner_id=0`, empty runner name and no steps. No Gradle/test execution occurred. Do not infer a source pass or source failure from these infrastructure-only attempts.
- Current branch remains `feature/memory-intelligence`. No APK, merge, Android `main`, Render, Hermes backend or production Supabase changes.


## Repository specialist triage boundary — 2026-11-05

- Audited the production RUN NEXT path: it already evaluates a fresh canonical issue snapshot with `VyomFactoryTaskTriage`, persists stable typed events, and then independently gates execution through the existing runner/adapter registry. Repository tasks currently have no registered executable adapter; the read-only path discovery and one-file PR proposal are separate owner-invoked UI flows.
- Corrected the planner's repository outcome: when a task classifies as REPOSITORY and no executable capability is registered, it now returns typed `NeedsOwnerReview(REPOSITORY, repository_proposal_requires_owner_review)` rather than the generic `no_registered_capability` block. Triage persists a stable `triage:v1:needs_owner_review:...` event. This explicitly routes the complaint to the existing owner-reviewed proposal path without inventing an executable capability or bypassing the runner.
- Test-first commits: planner test `52c167b21d622bc45eed599242596e61ec9f2ba2`; triage test `65bbc3691b8a11811924c760d31afb09467846bf`. Implementation commits: planner `b5340a9235b61725f26fbba26a8e5a4a55aae653`; triage event `5115a6e755e6e4b3b29beef7531c3478c4837a50`.
- This is a truthful routing/observability improvement only. It does **not** automatically run NVIDIA path discovery, generate a patch, select a file, open a PR, or make repository work execution-ready. Owner-reviewed diff, isolated feature branch, exact-SHA CI, no auto-merge and no auto-install remain mandatory. Next implementation step: connect read-only source discovery and bounded proposal drafting to the Factory repository specialist flow while preserving the owner diff-approval gate and continuing to later queue items when the proposal is declined/blocked.
- Tests were added but not run locally. Latest branch source SHA is `5115a6e755e6e4b3b29beef7531c3478c4837a50`; exact-head CI is pending verification. Previous exact-tip CI for `25f0c07045711d1d203cce97fe295503b7037713` failed pre-runner with `runner_id=0`, no job steps; no Gradle/tests ran.
- No APK, Android `main`, Render, Hermes backend or production Supabase changes. No physical-device PASS claimed.


### Exact-head CI evidence for repository triage batch — 2026-11-05

- Source commits: tests `52c167b21d622bc45eed599242596e61ec9f2ba2`, `65bbc3691b8a11811924c760d31afb09467846bf`; implementation `b5340a9235b61725f26fbba26a8e5a4a55aae653`, `5115a6e755e6e4b3b29beef7531c3478c4837a50`. HANDOFF update commit: `798c073efc99d5a11e51249ae91bae78ed3a643d`.
- Exact HANDOFF SHA `798c073efc99d5a11e51249ae91bae78ed3a643d`: Debug run `42041713442` and Full CI run `42041713542`; both attempt 1 failed. Retried only failed jobs (attempt 2); both still failed. Latest job step queries returned empty arrays; downloadable logs returned GitHub BlobNotFound. No Checkout/JDK/Gradle/test steps ran. This is consistent with runner provisioning failure; it is not evidence that the new tests passed or failed.
- Source SHA `5115a6e755e6e4b3b29beef7531c3478c4837a50` also had Debug `42041656873` and Full CI `42041656911` fail before any steps. The newer HANDOFF commit above was checked separately.
- No source test PASS, APK or artifact is claimed. Do not change workflow/signing settings based on an empty-step failure. Next work remains repository specialist automation behind the owner-reviewed diff gate.


## RUN NEXT → repository specialist handoff — 2026-11-05

- Audit confirmed the existing RUN NEXT path already triaged and persisted typed outcomes, but it did not dispatch `NeedsOwnerReview`; the Android runner accepts only its concrete media-volume adapter and would otherwise skip repository complaints.
- Added `VyomFactorySpecialistRouting` to choose the next eligible repository owner-review decision from the canonical priority/FIFO order. It checks earlier decisions: if an earlier Android candidate is actually eligible for the existing runner, repository review does not jump it. Clarification/blocked/ineligible tasks are skipped according to the existing queue policy. Paused/closed tasks, already-deferred proposals and already-opened proposals are not auto-routed.
- RUN NEXT now hands the selected repository complaint to the existing `openFactoryCodeProposal` path after triage. This opens read-only source discovery; the owner still chooses an existing allowlisted file, reviews the exact one-file diff and explicitly approves OPEN PR. `ownerReviewedDiff` remains mandatory. No source file is changed by routing/discovery; no auto-merge, auto-install or repository execution adapter was added.
- Cancel/back-dismissal records `factory:v1:repository_proposal_deferred` on the originating issue so the same complaint does not monopolize every RUN NEXT tap. A successfully opened PR records `factory:v1:repository_proposal_opened`; a proposal generation failure or diff cancellation leaves it eligible for another attempt. If metadata marker persistence fails after PR creation, the UI warns that the same complaint may be offered again.
- Tests added first for priority/FIFO, deferred/opened deduplication, inactive-task exclusion and not routing executable/clarification decisions. Test commits: `bc3b733335c025690898da9d39940f4c4a7e0124`, `be007b38735f1c80ed543b5888366215f1ede2e0`, `06f752d35e19cc0eee3509f51bfd3de2b72486c5`, `277b4110e0bea5edccadc60a42522e7529d208a9`. Implementation commits: `92b8d67b5244fa388965154dea928e67ec6c49fd`, `b19fbc1694e097734bbbc3af10f760dad9378f70`, `a1948b4c7e52563a39e5d2c8bf6afd022d401538`, `ab3b40f92768b2fb2668e2392df9ccaf007959c8`, `943b929df738ac27a45db4eb71dffb71a497e6d6`, `969a2762fbad16ff2cf5406d336defef67cbdf83`, `bb0061b67ca6e1decf33124a12f4b078dab5a67f`, `aaffa6d1d53d7dd7fb25874457ccbd8a653680f7`.
- Repository API verified `visibility=private` on this checkpoint. No screenshot upload was made; owner-tapped Attach and physical acceptance remain pending.
- Exact source HEAD before this handoff update: `aaffa6d1d53d7dd7fb25874457ccbd8a653680f7`. Debug `42043215526` and Full CI `42043215721` failed with job step arrays empty; no Gradle/test commands ran. This does not establish a source failure or source pass. No local Gradle run was available through the connected repository workflow.
- Current scope remains Factory-only APK 1. No APK, no physical-device PASS, no Android `main`, Render, Hermes backend or production Supabase changes.
- Next: verify exact-head CI after this handoff commit. If runners remain unavailable, continue source-level review and tests without changing CI/signing configuration. Then proceed to the next Factory specialist gap; do not claim repository repair is autonomous—path discovery, file choice, diff approval and PR review remain owner-controlled.


### Exact-head CI result for repository specialist handoff — 2026-11-05

- HANDOFF commit `845c2e0f1e87ed997ceae6605ddcb6622c8ed076` received Debug `42043388184` and Full CI `42043388220`. Attempt 1 failed; failed jobs were retried as attempt 2 and failed again. Both attempts report empty job steps; attempt-2 job step queries also returned `[]`. No Gradle or test command ran.
- These results do not prove source failure or success. Do not modify workflow/signing configuration on this evidence. The repository-specialist routing tests remain unexecuted until GitHub allocates a runner or a trusted local Gradle environment is available.
- Repository remains private as verified by repository metadata. No APK/artifact generated or delivered; no physical-device PASS.


### Automatic read-only discovery launch from RUN NEXT — 2026-11-05

- Extended the repository handoff so a Factory-origin proposal form automatically starts the existing read-only `FIND RELEVANT SOURCE FILES` request after the form is shown. It still only lists existing allowlisted paths at an exact base SHA; the owner chooses the path, then separately requests a patch, reviews the full diff and explicitly taps OPEN PR.
- Manual/non-Factory self-change forms do not auto-start discovery. Existing sensitive-content rejection, exact-tree/path validation, stale-SHA checks, one-file boundary and `ownerReviewedDiff` gate remain unchanged.
- Source commit: `d831f7d9472611316dfaba945d840f11cb18d332`; follow-up placement correction: `b6eb25212c0d856227a786cc085961407f33e689`.
- Exact source SHA `b6eb25212c0d856227a786cc085961407f33e689`: Debug `42043781449` and Full CI `42043781547` both failed; job-step queries returned empty arrays. No Gradle/test command ran. No source PASS/FAIL inferred.
- Current limitations remain: no automatic path choice, no automatic patch acceptance, no auto-merge/install; external model discovery may be unavailable if owner credentials are missing or the privacy gate rejects the complaint. A failed or cancelled proposal remains eligible for a later retry unless the owner explicitly deferred it.


## GitHub Actions runner allocation investigation — 2026-11-05

- Investigated raw run and job records for exact branch HEAD `e64daf90d14a7d3f9773b6e168ebe9b83078e190`.
- Debug run `42043890718`, job `127301546211`; Full CI run `42043890656`, job `127301546637`. Raw `GET /actions/runs/{run_id}/jobs` confirms both jobs requested label `ubuntu-latest`, but both report `runner_id=0`, `runner_name=""`, `runner_group_id=0`, `runner_group_name=""`, `steps=[]`. They transitioned from started to failed in about 1–2 seconds. Their log download endpoints return `BlobNotFound`; there are no step logs to inspect.
- Cross-checked older runs `42041920028`, `42041920026`, and `42038541669`; same runner ID 0, empty runner identity and empty steps. This is a repeated pre-execution runner-allocation failure, not a Gradle/Kotlin/test failure and not attributable to the latest source diff.
- Both workflow YAML files are present on `feature/memory-intelligence`, triggered by push to this branch, and declare `runs-on: ubuntu-latest`. Repository metadata reports private visibility and admin/push permissions for the connected account. The workflows did start and create jobs, so this is not a workflow YAML parse/branch-trigger failure.
- Attempted to inspect repository Actions permission settings through the available GitHub connection; its approved endpoint allowlist rejects `/actions/permissions` and `/actions/permissions/workflow`. The connector also cannot retrieve the missing job logs. Therefore the precise upstream reason (GitHub-hosted runner service/capacity incident vs account billing/spending-limit or policy restriction) remains **unconfirmed**. Do not state one as fact.
- Owner-side next check in GitHub UI: repository **Settings → Actions → General** (Actions permissions / runner access) and account **Settings → Billing and licensing → Plans and usage → Actions** (included minutes, spending limit, payment/billing hold). Also check GitHub Status for Actions incidents at the failure timestamps. Do not change workflow YAML, signing secrets or source code until these settings/status are checked or GitHub exposes a concrete error.
- No workflow configuration changed during this investigation. No rerun triggered. No Gradle/test execution, APK, or physical-device PASS is claimed. Android `main`, Render, Hermes backend and production Supabase remain untouched.

## Universal repository intake engine — 2026-11-05

- Owner supplied a ranked candidate list of 100 public GitHub repositories for Factory integration. Treat these as candidate sources, not pre-approved imports. The names/stars in the supplied table are user-provided discovery metadata; canonical owner/repository URLs, current immutable commits, licenses, archived state, security and compatibility still require live inspection.
- Reconfirmed the controlling architecture in `docs/UNIVERSAL_FACTORY_ARCHITECTURE.md`: Factory is one shared orchestrator; repository integration is a specialist module, not a second queue. Existing task queue, leases, journal, retry, STOP, approvals and evidence stores must be reused.
- Existing `VyomFactorySkillRepositoryClient` only inspects root `SKILL.md`, requires an allowlisted license and stores inert Markdown. It does not support arbitrary source repositories, API specs, workflows, frameworks or documentation. Imported skills remain inert and are not executable capabilities.
- Added test-first specification `VyomFactoryRepositoryIntakeTest` (commit `d441a7ba1109d46e840aa8565724398e7b5d4c45`) for canonical GitHub URL parsing, repository-type classification, unknown-license review, archived/truncated tree blocking, and the non-executable-by-default invariant.
- Added `VyomFactoryRepositoryIntake` (commit `c5c0cc1fefc4fe5538d05c6a05e1aa8ca4c13583`). It parses only canonical HTTPS github.com owner/repo URLs, classifies SKILL/source/API/workflow/docs/unknown from tree paths, requires immutable 40-character commit identity, blocks archived/truncated/unsafe trees, sends unknown licenses/types to owner review, and always returns `sourceExecutionAllowed=false` and `installationApproved=false`.
- This is an intake assessment boundary only. It does not yet call GitHub, fetch repository trees, resolve licenses, store candidate records, install anything, execute imported code, or route candidates into the existing task planner. No source repository from the user's list has been imported yet.
- Tests were added before implementation but were not run in a local Gradle environment. Exact-head CI has not yet been verified for the new commits; prior CI attempts failed before runner allocation (`runner_id=0`, `steps=[]`). Do not claim test pass/fail.
- Current branch remains `feature/memory-intelligence`; do not touch Android `main`, Render, Hermes backend or production Supabase. No APK or physical-device verification is claimed.
- Next implementation order: (1) inspect and reuse GitHub transport/auth and current skill storage; (2) add immutable repository snapshot fetch with bounded tree/file limits and license metadata; (3) add owner-reviewed candidate persistence/preview; (4) connect typed repository tasks to the existing planner/approval/journal without a second queue; (5) only then import specific approved repos. Keep imported source inert; no arbitrary build scripts or repository code execution.

### Immutable repository snapshot inspection and candidate catalogue — 2026-11-05

- Added a fake-transport test proving repository tree inspection uses the resolved immutable commit SHA, not a mutable branch ref: `15995085b2253acc25db0ead951df47dff69c3e4`.
- Added `VyomFactoryRepositoryClient` (`280a7364d06356d80c85b4eb5729af1bcadeffda`). It accepts only canonical GitHub URLs, reads repository metadata, resolves the default branch to a commit SHA, then reads the recursive tree at that exact SHA. It bounds HTTP response size and tree entry count, refuses redirects, and never executes source.
- Added `docs/FACTORY_REPOSITORY_CANDIDATES.md` (`7a27b9e5652ec761346d0ef47a35685d7cd34d2f`) recording all 100 owner-supplied names. The list is a discovery queue only: canonical owners/URLs, immutable revisions, licenses and repository health remain unverified. The DeepSeek harness is explicitly excluded under the owner's no-DeepSeek rule. No repository was installed or executed.
- CI on code SHA `c5c0cc1fefc4fe5538d05c6a05e1aa8ca4c13583`: Full CI `42053372235` and Debug CI `42053372486` failed before runner allocation; both jobs show `runner_id=0`, empty runner name and `steps=[]`. No Gradle/test command ran. This does not prove source pass or source failure.
- Later CI for `34830f73f6c6d45ead67542f1a4cb96bc2b07122` and `280a7364d06356d80c85b4eb5729af1bcadeffda` was superseded/cancelled as newer commits were pushed. Latest candidate-catalogue SHA `7a27b9e5652ec761346d0ef47a35685d7cd34d2f` has Full CI run `42053707755` pending and Debug run `42053707776` queued at the time of this update; exact job/test outcomes are not yet known.
- Tests have been written before implementation, but the connected workflow does not provide a local Gradle runner and GitHub-hosted runner allocation is currently unreliable. Do not claim a test pass.
- Next: add a bounded repository preview model that surfaces license, immutable SHA, repository kind and blockers to the existing owner-review flow; wire it into the shared Factory planner/journal without adding a second queue or executing imported code. Then resolve canonical URLs for the first relevant candidates and inspect them individually.

### Owner-reviewed universal repository registration — 2026-11-05

- Added registry tests first (`1425668bd80ba2670401355da840f243e4a4e109`) for explicit owner confirmation, immutable SHA pinning, idempotent duplicate registration, rejection of silent revision replacement, blocked-source rejection and the no-execution invariant.
- Added `VyomFactoryRepositoryRegistry` (`a50f35d263a7c4fe4a55d68ae9bd0ad2e320fb6d`). It stores only immutable repository metadata in app-private storage; it is a source catalogue, not a task queue. Same URL+SHA registration is idempotent; a different SHA cannot silently replace the registered revision. Execution and installation grants are persisted as false and rejected if tampered.
- Added `VyomFactoryRepositoryReviewDialog` (`759bce9ad22369534212b79a39cc978b2cf93bc2`) and exposed **INSPECT / REGISTER ANY GITHUB REPOSITORY** in the existing Factory workspace (`39cf8dbb1837d64d2578a7e4f299a09f5b531b08`). The owner pastes a canonical GitHub URL, sees immutable commit/type/license/health/review flags, and explicitly registers only the reference metadata. Blocked sources cannot be registered. Unknown license/type is marked pending owner review. No source code is downloaded, installed or executed.
- The existing root-SKILL.md importer remains available as a separate specialized adapter. Do not remove it or treat it as the universal engine.
- Latest exact HEAD: `39cf8dbb1837d64d2578a7e4f299a09f5b531b08`. Full CI run `42054157961` is pending; Debug run `42054158526` is queued. No Gradle/test outcome is available yet. Earlier code SHA `c5c0cc1fefc4fe5538d05c6a05e1aa8ca4c13583` failed before runner allocation (`runner_id=0`, `steps=[]`); no tests ran. Do not claim a pass.
- No source repository from the 100-item candidate list has been registered yet; the user must supply/choose canonical URLs. Candidate list is in `docs/FACTORY_REPOSITORY_CANDIDATES.md`. DeepSeek harness remains excluded.
- Next: add a bounded file-level preview/manifest extractor for non-SKILL repositories (still read-only, allowlisted paths, per-file byte caps, secret redaction and untrusted-content labeling); then route repository review outcomes into the existing canonical issue/journal flow. No arbitrary scripts, builds, plugin execution, auto-merge or auto-install.

### Bounded repository file preview and multi-artifact classification — 2026-11-05

- Added file-preview regression coverage (`8e16482149a968dfa709dab3f9a3753a2284441b`, fixture correction `6f6ee2cb377bdc1188c8a145df2e996c64cc74ef`). It asserts immutable-SHA file reads, recognized GitHub token/API-key redaction, and untrusted-content labeling.
- Added bounded file preview to `VyomFactoryRepositoryClient` (`efff57ff2ae2f6f57c07367f7530e954411bbff4`), then corrected regex escaping and path separator validation (`656e2c72fee7f76caafc3fe1fb42ba6375180222`) and made whitespace filtering explicit (`05f2da1391321564a2970a1f21c68fdc506fff28`). Preview is restricted to README, license, skill, known manifests, OpenAPI specs, a maximum of two workflow files and two docs files; max 8 files, 48 KiB/file, 160 KiB total. Strict UTF-8 and Base64 size checks apply. Private-key, GitHub token, AWS key, common API-key/password and OpenAI-style key patterns are redacted. Original content is never executed.
- The owner-review dialog now displays the bounded redacted file preview and clearly labels all content untrusted (`6c97afdbd32069834e68472dae26bdf222b93ecf`). No full source tree is downloaded.
- Added multi-artifact detection so a repo may be simultaneously SOURCE_CODE + SKILL + API_SPEC + WORKFLOW + DOCUMENTATION, instead of losing its secondary skills/workflows. Primary classification prioritizes source manifests. Implementation `630bdc00bc9ec7b6b0eb2896499fccdff76f1694`; registry persistence `f435c5a082b7147652bcdac5a14f7dfd29543ff9`; tests `3e8debdf42c17718ed2f72ef00bdee2db38ee65f` and `8c3d2f43422fc376b6170c2a0ee0ead9c3e86d9e`; UI display `17e85eb770f0b05b740298778b5d1ba3609b1532`.
- Resolved and pinned 14 candidate repositories in `docs/FACTORY_REPOSITORY_CANDIDATES.md` (`9aed70c53da0b454695aadf583b77d55c19b7977`): rea, openclaw, superpowers, ECC, hermes-agent, opencode, n8n, firecrawl, browser-use, codex, markitdown, graphify, mattpocock/skills and anthropics/skills. Licenses observed: MIT/Apache-2.0 for several; n8n NOASSERTION, Firecrawl AGPL-3.0 and Anthropic Skills missing license metadata all require explicit review. OpenClaw (66,562 entries), Hermes Agent (23,616) and n8n (40,498) exceed the current 20,000-entry tree cap and need bounded subtree discovery. No source was installed or added to the app registry.
- Test assertion cleanup: `b712d373645e423de200ddb9b8d15a8cf81e3ab7`.
- Current exact HEAD: `b712d373645e423de200ddb9b8d15a8cf81e3ab7`. Full CI run `42055409067` is pending; Debug run `42055408772` is queued. The prior Debug run `42055317783` had a real runner and reached Gradle setup, but was superseded by later commits before an outcome was captured. No test/build PASS is claimed.
- Remaining in this subtask: run/inspect exact-head CI; make the preview UI show all detected artifact kinds; add bounded subtree discovery for large repositories; add owner-approved update/removal workflow for pinned refs; connect registered source references to canonical Factory issue/journal records without a duplicate queue. No arbitrary source/build/workflow execution.

### Additional Android, voice, memory and security repository intake — 2026-11-05

- Added 17 owner-supplied repositories to `docs/FACTORY_REPOSITORY_CANDIDATES.md` (catalogue commit `b6574e9261437d7b1e85454fa50f898ebe4ca634`). GitHub metadata was checked for canonical owner/name, default branch and license metadata. No candidate was installed or registered in the app.
- Highest-priority architecture reviews: `AnaaySampat/jarvis`, `VarsshanCoder/JARVIS-OS-Agentic-AI-Assistant-for-Android`, `mem0ai/mem0`, `open-jarvis/OpenJarvis`, and `letta-ai/letta`. `Ayush0Chaudhary/blurr` has NOASSERTION license metadata; review only until licensing is resolved.
- Security references (`h4cker`, `cybersecurity-ethical-hacking`, `PayloadsAllTheThings`, `SecLists`, `strix`, `shannon`, `PentestGPT`) are marked reference/research-only. No imported scanner, payload, workflow or agent is to execute against a target. Shannon's AGPL-3.0 and several missing license declarations require separate review.
- Ambiguous names `GLaDOS`, `Agentic-Bug-Hunter`, `Claude-BugHunter`, `SecSkills`, `Anthropic-Cybersecurity-Skills` and the supplied xiaozhi URL remain unresolved; exact canonical URLs are required. Repo-view/GitShare and PAT/collaborator suggestions are access/sharing methods, not Factory source integrations; do not pass credentials in URLs.
- Full reachable-history pattern scan supplied by owner: 27 remote branches, 265 tags, 14,123 unique Git objects and 1,938 blobs scanned. It reported 169 broad Discord-token regex matches, 3 credential-assignment matches in `SelfModificationPathDiscoveryTest.kt`, and 4 OpenAI-style matches in test files. These are not confirmed secrets; Discord regex is permissive and test fixtures may be dummy values. Do not label repository clean or public-safe until each hit is classified without exposing values. If any actual credential was committed, rotate/revoke it.
- Exact current HEAD before catalogue commit was `47051d912e45cecdeca52a52e51cd44fe7db5dd2`; Debug run `42055639057` was in progress and Full CI run `42055639066` pending at inspection. No build/test pass is claimed.
- Next: inspect exact-head CI after latest documentation commit; review the 3 flagged test lines and a representative sample of Discord matches using value-redacted context; then inspect the highest-priority Android and memory repositories at pinned SHAs. Keep all source intake read-only and owner-approved.

### First-pass review of priority Android/memory/security sources — 2026-11-05

- Pinned README review completed for AnaaySampat/jarvis (`04da6e06a70d36ee21b2224b5e6e923bd6682c28`), VarsshanCoder/JARVIS-OS-Agentic-AI-Assistant-for-Android (`fcf3f4fc1d2fb0f99c634c55034165f3be48c164`), Ayush0Chaudhary/blurr (`5ead5b431c957782c44a34722d7b1878c480dc4a`), mem0 (`ed1f10fc87080f0b67f99be370817504888462ab`), OpenJarvis (`103b1227cb65af71fa930de0945e8d11844aef11`), Letta (`8816d3a8516893bfdc0fa14bfb1235226012e295`), Strix (`09536cf1481731c790995bd8060db19a3b75ff48`) and Shannon (`8c75a9b5a89ac02b7be8efeca7e129284d37e20f`). Detailed notes are in the candidate catalogue.
- Best Android architecture candidate for deeper source inspection: AnaaySampat/jarvis, because its README describes native accessibility operation, screen/result verification, global STOP and confirmations for messages/calls. These claims still need source inspection and cannot be treated as verified behavior.
- JARVIS-OS is a Kotlin/Compose + FastAPI + Qdrant + Accessibility/MCP architecture candidate; its "zero hallucinations" claim is not evidence. Inspect SMS/call action gates before considering any design.
- blurr's own README identifies a Personal Use Only license and says its memory is disabled. Do not copy its code without suitable explicit rights.
- Mem0's README distinguishes managed-platform benchmark results from OSS SDK performance. OpenJarvis includes code-execution/file-I/O agent capabilities and needs strict adapter isolation. Letta's README says current active source moved to `letta-ai/letta-code`; inspect that repo instead if Letta is pursued.
- Strix and Shannon explicitly perform active security testing; Shannon says it executes real exploits. Keep both out of normal Vyom execution. Research/reference only unless a separately approved isolated security-testing feature is ever specified.
- Latest catalogue/source-review commit: `ab0eaf47874b641654e4eb1e516542c8b0c01c9f`. Exact-head Debug run `42056220116` was in progress and Full CI run `42056220035` pending at inspection. No test/build PASS claimed.
- Next implementation/review order: (1) exact-head CI outcome and fix any real failure; (2) inspect source files for AnaaySampat/jarvis and JARVIS-OS at immutable SHAs, focusing on STOP, accessibility grants, action verification, foreground service and confirmation; (3) compare Vyom memory interfaces with Mem0/Letta before proposing any adapter; (4) connect registered refs to existing Factory journal/issue records. Do not create a second queue or execute imported code.

### Native Android control safety comparison — 2026-11-05

- Inspected pinned source, not just README: Anaay JARVIS `JarvisAccessibilityService.kt` blob `e0ebc774f925dac868f0562c9de54e6a727b0eba`, `OperatorCore.kt` blob `d9a30472c536f9252030947bbbf79616186ce7be`, `NativeOperator.kt` blob `d4fd1975916525a10fbaf1dd32303de527a7f209`; JARVIS-OS service blob `332fd6aecc83a97b912e6a02a9f126f2a3a80d88`.
- Anaay source has a real cross-app STOP overlay that increments a stop sequence, invalidates in-flight callbacks and cancels the active task; it avoids tapping its own STOP overlay. OperatorCore checks STOP, uses screen observations, rejects repeated/cyclic actions and gates higher-risk actions with approval. NativeOperator rejects concurrent active phone tasks and checks cancellation between steps.
- JARVIS-OS's inspected AccessibilityService provides click/type/scroll/back/home helpers and package/text waits, but its `onInterrupt()` only logs and this service class contains no equivalent global STOP/cancellation epoch or per-action approval gate. This is limited to the inspected class; other repository files were not fully audited.
- Recommendation recorded: use Anaay as a design reference for STOP, cancellation, stale observation and risk gates; do not transplant its whole project. Do not replace Vyom's existing event/revision-aware executor with the simpler JARVIS-OS helper service.
- Latest candidate source-review commit: `a8d695b850742041c68f494fedab0ad280d49aab`. Exact-head CI has not yet been triggered for this latest documentation commit. The immediately preceding HEAD `2cd343b5c601d06d41813f9b954dec20a02ccef4` had Debug run `42056284551` and Full CI run `42056284756` both running on real runners; no final results captured yet.
- Next: inspect those CI outcomes; run CI for latest exact HEAD; then review Anaay's specific STOP/risk-policy tests and compare against Vyom's existing control tests before any code changes.

### Android control test-suite review — 2026-11-05

- Inspected Anaay JARVIS `OperatorCoreTest.kt` blob `26e37d2613c744bbf6d2976b5961db5f988fda64`. Tests explicitly cover stopped-state checks, stale-observation retry only when the same control remains, refusal when control meaning changes/disappears, cycle detection, duplicate-message prevention, R2 approval, and R3 always requiring a human. Candidate catalogue records this as the most useful control-policy test reference.
- Letta's README points active development to `letta-ai/letta-code`, verified Apache-2.0; use that instead of the older Letta repo if pursuing a source adapter.
- No third-party source was copied into Vyom. No external code was executed or built. This remains static source/test inspection only.
- Latest source/test-reference note commit: `94853b197d885b728736c933dda0383a6e629172`. Exact-head CI has not yet been checked for this latest commit. Prior runs `42056284551` (Debug) and `42056284756` (Full CI) were running on real runners when last inspected; no final outcome was recorded.
- Next: check exact-head CI; then connect repository source references to existing Factory issue/journal flow, preserving the single canonical queue and owner approval. Do not auto-register every candidate or execute imported source.

### CI compile blockers found and corrected — 2026-11-05

- Real Debug/Full CI logs for runs `42056284551` and `42056284756` exposed two Kotlin compile blockers:
  1. `SelfModificationClient.kt` path-discovery system prompt had malformed quote escaping. Replaced it with a Kotlin triple-quoted literal (commit `77a2e577651ba7988b9ca2e0ceb188fb99265099`).
  2. `VyomFactoryRepositoryReviewDialog.kt` called a nonexistent four-argument `AlertDialog.setButton` overload. Removed the redundant call; the existing positive label already says BLOCKED and the button is disabled after show (commit `b9e87047b783c2b9efea083cf5ce0ef94663447d`).
- Both earlier CI jobs reached `:app:compileDebugKotlin` on real runners and failed on these exact compiler errors; no unit tests or APK artifacts were produced by those runs. Do not report them as test failures in the new repository-catalogue test.
- A new red-test commit `25efc41381b733b83cbfb9f2da69e6123dedbcee` added `VyomFactoryRepositoryCatalogTest`; it specifies 17 curated entries, canonical/unique URLs, seven security-reference-only entries, license-review flags, and Android/memory track discovery. The catalogue production class is intentionally not added yet.
- Next: run CI after the compile fixes. Once compilation reaches the new test and fails specifically because `VyomFactoryRepositoryCatalog` is missing, implement the minimum catalogue model, rerun, and then expose it in the Factory UI.


### P0 security-audit checkpoint — 2026-11-06

- Exact audited branch/HEAD: `feature/memory-intelligence` / `723903ab43e2b4fc8ce29b97050876bc65908cfb`. The earlier uncertainty about repository identity was incorrect; this exact SHA exists in `riyas433554-dot/vyom-android`.
- Exact-head CI runs: Debug `42057111518` and Full CI `42057111418`; both completed with **FAILURE**. Debug job `127349227645` failed at **Build and test APKs**; Full CI job `127349337673` passed Termux syntax/intake checks and Gitleaks secret scan, then failed at **Unit tests and debug APK**. Packaging/artifact upload were skipped. No APK artifact is available from these runs.
- Current exact-head unit-test tree contains 80 Kotlin test files. Static redacted inspection found four OpenAI-style key-pattern occurrences, all in test-only sanitization/rejection fixtures: `SelfModificationPolicyTest.kt:72`, `VyomAuditPolicyTest.kt:12`, and `VyomFactorySelfChangePromptTest.kt:21,31`. Classified `TEST_FIXTURE`; no actual credential confirmed.
- Historical scan note above reports three credential-assignment matches in `SelfModificationPathDiscoveryTest.kt`. At this exact HEAD, only two credential-like assignments are present in that file (`token` line 12 and `apiKey` line 13); both are test fixture values used by the path-discovery tests. The reported third match is **not reproduced** at this HEAD. This historical count discrepancy remains open; no full secret value is recorded here.
- A redacted current-snapshot scan of all 80 Kotlin unit-test files found no Discord token-format or Discord webhook-pattern matches. This does **not** classify the 169 broad Discord-pattern matches reported across 14,123 reachable Git objects / 1,938 blobs. Their per-file/per-commit inventory was not available through the current connector inspection, so all 169 remain **UNCLASSIFIED**; do not label them false positives or declare history clean.
- GitHub repository metadata still reports `visibility=public`. Private conversion remains an owner action; keep remote screenshot attachment disabled until it is private. No confirmed `REAL_LEAK` has been established, so no credential rotation was performed or recommended based on an unconfirmed match.
- P0 remains **INCOMPLETE / BLOCKED** pending the original 169-match inventory (file, line/blob, commit, redacted context) and reconciliation of the third historical credential-assignment match. P1 and P2 remain locked. No application source, main branch, Render, backend, or production Supabase changes were made.


### P0 security-audit continuation — 2026-11-06

- Revalidated the live target branch: `feature/memory-intelligence` points to `eb4abb7a8de1cd63caa80b15ec0d379a4be243a3` (parent `723903ab43e2b4fc8ce29b97050876bc65908cfb`). The earlier audited SHA remains its parent; it is no longer the live branch HEAD. This current commit changes only HANDOFF documentation.
- Exact-current-HEAD workflow runs: Debug `42161654583` and Full CI `42161654678`; both failed. Debug job `127708506558`; Full CI job `127708507168`. No APK/artifact was produced.
- Full CI Gitleaks passed on this HEAD with explicit result `no leaks found`, using the default Gitleaks configuration. This is not a substitute for classifying the separately reported 169 broad Discord-pattern hits across 14,123 historical Git objects.
- Current Kotlin test snapshot rechecked: `SelfModificationPathDiscoveryTest.kt:12-13` still contains two test-fixture assignments. No third assignment was found in that file. Four OpenAI-style occurrences remain in the four test-only locations recorded above. No full credential values were recorded.
- Repository tree inspection found no saved scan inventory/report. GitHub code search and tree inspection did not locate the original 169-hit per-file/per-commit inventory. Redacted current-snapshot inspection of Kotlin tests found no Discord token/webhook-pattern matches; this cannot classify historical matches.
- Repository metadata still reports `visibility=public`. No actual credential is confirmed from available evidence. No rotation was performed. Do not claim repository clean/public-safe; owner should consider private visibility until historical scan findings are reconciled.
- Latest exact-HEAD CI compile failures: `SelfModificationClientIntegrationTest.kt:43-45` malformed test string syntax; `VyomFactoryDrainCoordinatorTest.kt:22` malformed Kotlin expression; and unresolved `VyomFactoryRepositoryCatalog` references in `VyomFactoryRepositoryCatalogTest.kt`. Packaging/artifact upload were skipped. Recorded only; P0 does not authorize P1 implementation or CI repair.
- P0 remains **INCOMPLETE / BLOCKED**: all 169 historical Discord-pattern hits remain UNCLASSIFIED and the third historical credential-assignment match remains unreconciled. No REAL_LEAK is confirmed, but absence of confirmation is not proof of cleanliness. P1/P2 remain locked pending P0 completion and owner GO. No application source, `main`, Render, backend, or production data changed.


## Owner-order continuation — 2026-11-06 (latest exact-head evidence)

- Revalidated live branch `feature/memory-intelligence`: HEAD `53a9f7a5aa4643f00f62fa6cce9eaedba235fd46`.
- Recovered `vyom-prepublic-scan.sh` from the ChatGPT Library. It is the scanner script, not the historical scan output; no 169-match per-file/per-line/per-commit inventory is present in the Library or current repository tree. Commit search for the script path returned no historical tracked file.
- Exact-HEAD Debug CI run `42170343764` failed in job `127736428906`; Full CI run `42170343760` failed in job `127736854968`. Both ran on exact SHA `53a9f7a5aa4643f00f62fa6cce9eaedba235fd46`. Debug failed at `Build and test APKs`; Full CI passed Termux syntax/intake checks and secret scan, then failed at `Unit tests and debug APK`. Packaging and artifact upload were skipped; no APK artifact was produced.
- Both real CI logs confirm compile blockers: malformed Kotlin expression in `VyomFactoryDrainCoordinatorTest.kt:22` (escaped newline sequences causing parser errors) and unresolved `VyomFactoryRepositoryCatalog` references in `VyomFactoryRepositoryCatalogTest.kt` (production catalogue class absent). These are recorded for the later ordered implementation items; no source fix is authorized while core item 2 is incomplete.
- Secrets classification remains unchanged: 169 historical Discord-pattern hits UNCLASSIFIED; third historical credential-assignment match unresolved; four OpenAI-style occurrences and two current assignments classified TEST_FIXTURE. Full CI Gitleaks step passed, but it does not classify the 169 historical broad-pattern hits. No confirmed REAL_LEAK; no secret values recorded; no rotation performed.
- Core item 1 audit/HANDOFF sync is refreshed with latest exact-head CI evidence. Core item 2 remains BLOCKED pending the original redacted inventory/output (file/path, line/blob, commit, rule/category, redacted context). Items 3–8 remain in owner-locked order and have not been started. No Android `main`, Render, Hermes backend, or production Supabase changes.


### Real CI compile-blocker remediation — 2026-11-06

- Fixed VyomFactoryDrainCoordinatorTest.kt: the onOutcome when body contained literal escaped \\n text instead of Kotlin line breaks. Replaced the malformed sequence with real source newlines. Commit: 75cdf35e592ec489b30fdb514d63b46d53a97fa0.
- Added the missing production class VyomFactoryRepositoryCatalog.kt from the exact 17 owner-supplied candidates already documented in this handoff and docs/FACTORY_REPOSITORY_CANDIDATES.md. It exposes stable IDs, canonical URLs, tracks, license-review flags and reference-only flags. Every entry defaults to executionAllowed = false; all seven security candidates are reference-only. No repository code is imported or executed. Commit: 1db4115ad538b7a0d2b2cce456ed07bff9411e18.
- Exact-head CI was triggered for both commits. At handoff update time, Debug run 42171578429 and Full CI run 42171578493 for 1db4115ad538b7a0d2b2cce456ed07bff9411e18 were queued/pending; earlier exact-head runs for 75cdf35e592ec489b30fdb514d63b46d53a97fa0 were still running/pending. No green result or APK artifact is claimed until those runs complete.
- Security inventory blocker remains separate and unresolved: 169 historical broad Discord-pattern hits still lack the original redacted per-file/per-line/per-commit inventory. Do not mark them clean or rotate credentials without a confirmed real leak.
- No Android main, Render, backend or production Supabase changes.


- Follow-up test-contract correction: RepositoryCatalogTest requires the stable Letta candidate ID `letta-code`; changed the catalogue ID from `letta` to `letta-code` while preserving the documented canonical source URL. Commit: 717c4ada16e441b71cd34d7dd623992ccdf36163. Exact-head CI for this follow-up is pending; no pass is claimed.


### Next exact-head CI failure — SelfModificationClientIntegrationTest fixture — 2026-11-06

- Latest verified pre-fix HEAD `3332157bb7e2cb8ecfbf8f80cdd634bc0ec6035b` had Debug run `42171637368` and Full CI run `42171637359`; both completed FAILURE on the same SHA.
- Real logs show production Kotlin compilation and `:app:assembleDebug` succeeded. Both workflows then failed at `:app:compileDebugUnitTestKotlin` due malformed Kotlin string syntax in `SelfModificationClientIntegrationTest.kt:43-45` (`reviewed` parsed as an identifier and `ownerReviewedDiff` became an unresolved token). This is a test-source compile blocker, not a production source compile failure.
- Repaired only the malformed `proposedContent` test fixture using a Kotlin triple-quoted string with `trimIndent()`, preserving the intended source payload and `ownerReviewedDiff = false` assertion. Commit: `92d42248eb8fd7c39a7906703b96d88bcd255acc`.
- Exact-head CI for `92d42248eb8fd7c39a7906703b96d88bcd255acc` must be checked before claiming the fix is verified. No green result or APK artifact is claimed at this checkpoint.
- Core owner order remains unchanged: secrets audit item 2 is still BLOCKED by the missing redacted historical inventory (169 Discord-pattern hits remain UNCLASSIFIED; third historical credential-assignment match unreconciled). Items 3–8 remain locked and unstarted. No Android `main`, Render, Hermes backend, or production Supabase changes.

### Exact-head CI continuation — bounded large-repository inventory — 2026-11-06

- CI for `92d42248eb8fd7c39a7906703b96d88bcd255acc`: Debug run `42172539824` and Full CI run `42172540603` both FAILED. Kotlin compilation and `:app:assembleDebug` passed; unit suite ran 557 tests with one failure: `VyomFactoryRepositoryIntakeTest.oversizedRepositoryUsesBoundedSubtreeInventoryAndRequiresReview`.
- Failure cause confirmed from test source: large-repository path expected immutable commit object -> root tree -> bounded subtree requests, but client always requested a recursive tree and test rejected the unexpected API path. This was a real production behavior gap, not a malformed test.
- Implemented size-gated large-repository inspection in `VyomFactoryRepositoryClient`: resolve root tree from immutable commit SHA, inspect at most 200 root entries and 8 subtrees with at most 500 entries each, cap collected paths at 20,000, mark snapshot `treePartial=true`, and preserve truncation state. Small repositories retain recursive commit-pinned tree path. No repository content is executed.
- Fix commit: `d6247fdeffdefe78e38a7cd39aca1a99b2de360d`. Exact-head CI must be checked; no passing result or APK artifact claimed yet.
- `docs/HANDOFF.md` updated on the next documentation commit. Secrets audit item 2 remains blocked by absent redacted historical scan inventory; do not bypass locked order. No Android `main`, Render, Hermes backend, or production Supabase changes.

### Exact-head CI green after bounded repository inventory fix — 2026-11-06

- Live branch verified: `feature/memory-intelligence`, HEAD `284d625c42dbb2197e19f387637517a00006647a`.
- Exact-HEAD Debug run `42173025708` = **SUCCESS** and Full CI run `42173025509` = **SUCCESS**. Both report the exact same HEAD SHA above.
- Debug artifact exists: ID `13482630020`, name `vyom-0.3.1-284d625c42dbb2197e19f387637517a00006647a`, size 88,351,378 bytes, SHA-256 `5d40e9dcfc0ebe3ea676164fe135547d70feb3b2ca07f6ab85fbae1d290e32e5`, expires 2026-12-06. This confirms CI artifact publication, not phone installation or device acceptance.
- The earlier code-fix SHA `d6247fdeffdefe78e38a7cd39aca1a99b2de360d` Debug run `42173014168` was still in progress when checked; its Full CI run `42173014175` was cancelled after the newer HEAD workflows completed. The authoritative latest-HEAD results are the two successful runs above.
- Secrets audit remains the controlling blocker: 169 historical broad Discord-pattern matches are still UNCLASSIFIED and the third historical credential-assignment match remains unreconciled. The CI Gitleaks action passing on the latest run does not reproduce or classify the missing historical 169-hit inventory. The owner-provided `vyom-prepublic-scan.sh` is a working-tree heuristic plus optional Gitleaks history scan; it is not the missing original report.
- No source changes beyond the previously recorded bounded-inventory fix; this entry only synchronizes CI/artifact evidence. Core item 2 remains BLOCKED; items 3–8 remain locked. No Android `main`, Render, Hermes backend, or production Supabase changes. No phone/device test claimed.


### Full reachable-history Gitleaks audit and CI correction — 2026-11-06

- Exact branch at audit: `feature/memory-intelligence`; implementation commit `a7e6a55b7e123590c6525b3f24d9e3a856fb1cfe`.
- Audit exposed a coverage flaw: the existing `gitleaks/gitleaks-action@v2` push invocation logged `--log-opts=-1` and `1 commits scanned`; its green result was latest-commit-only, not full-history evidence.
- Added a pinned Gitleaks 8.24.3 full-reachable-history scan using `gitleaks git --redact --no-banner --log-opts="--all"`, a redacted JSON report, and a CI artifact. The first attempt had a missing binary path; corrected by explicitly installing the pinned binary with Go. A malformed workflow-step structure was also corrected before the successful scan execution.
- Full-history scan run `42228876982` on SHA `a7e6a55b7e123590c6525b3f24d9e3a856fb1cfe`: scanned 1,690 commits / approximately 5.03 MB; found 6 Gitleaks generic-api-key matches. Redacted report artifact ID `13507276239` (`redacted-full-history-gitleaks`).
- Classification from exact source lines at the reported commits:
  - 2 findings in `SelfModificationPolicyTest.kt` lines 68 and 72 are dummy API-key strings inside sanitizer/prompt-injection test fixtures: **TEST_FIXTURE**.
  - 4 findings in `docs/HANDOFF.md` / `docs/HANDOFF_HISTORY.md` are commit-hash text and a long Factory class identifier, not credential assignments: **FALSE_POSITIVE**.
  - No secret values are retained in this record. The exact per-finding redacted metadata is in the CI job log and report artifact.
- This Gitleaks run does **not** resolve the separate historical 169 broad Discord-pattern matches: their original regex and redacted per-file/line/commit inventory remain missing. It also does not reproduce or settle the third historical credential-assignment count. Keep 169 UNCLASSIFIED and the third assignment UNRESOLVED; no REAL_LEAK has been confirmed from available evidence.
- Full CI run `42228876982` failed at the full-history scan as expected due to the six findings; later unit-test/APK steps were skipped. Do not claim a green full CI or APK from this run. Debug build run `42228877131` on the same SHA was still in progress when this entry was prepared.
- Found a release-policy footgun: `.github/workflows/vyom-android-debug.yml` previously published a signed prerelease on every push when signing secrets existed. Changed it to require an explicit `workflow_dispatch` boolean `publish_ci_update_release=true`; ordinary branch pushes no longer publish releases. Existing older prereleases were not modified or deleted.
- Core item 2 remains BLOCKED by the unreconciled 169 Discord-pattern findings and third credential-assignment match. Items 3–8 remain locked. No Android `main`, Render, Hermes backend, or production Supabase changes.


### Follow-up: test-fixture candidates and APK artifact release gate — 2026-11-06

- Rechecked `app/src/test/java/com/hermes/assistant/SelfModificationPathDiscoveryTest.kt` at the live branch. It contains three credential-like literals used only as test inputs: `token` at line 12 (redacted prefix `test`, length 41), `apiKey` at line 13 (prefix `nvap`, length 34), and a prompt-injection test payload at line 89 (prefix `nvap`, length 22). These are test fixtures, not production credentials. Provisional source classification: **TEST_FIXTURE** for all three candidates.
- The original owner's 3-hit historical inventory is still absent, so there is no proof these exact three literals map one-to-one to the reported three matches. Keep the aggregate historical 3-hit finding **PROVISIONALLY CLASSIFIED / ORIGINAL-MATCH-MAPPING UNVERIFIED**; do not close the overall secrets audit.
- Exact latest-HEAD Full CI run `42229486326` on `8beeb28045415dc4fe8f572bd1377d08f34ff651` scanned 1,692 commits and reproduced the same 6 generic-api-key false positives. Redacted report artifact ID `13507835900`. The full-history scan failed; subsequent unit-test/APK steps were skipped.
- Updated `.github/workflows/vyom-android-debug.yml` so ordinary pushes neither publish signed prereleases nor upload APK artifacts. Both now require an explicit manual `workflow_dispatch` with `publish_ci_update_release=true`. This keeps CI builds/tests running while withholding intermediate APK artifacts. Existing prereleases from earlier workflow behavior were not deleted.
- Latest Debug run `42229486480` on the same SHA was still building at this checkpoint; no latest-HEAD Debug result or APK artifact is claimed.
- Core item 2 remains BLOCKED by the missing original 169 Discord regex inventory and unverified mapping of the historical aggregate. Items 3–8 remain locked. No Android `main`, Render, Hermes backend, or production Supabase changes.

### Locked core item 2 — fresh evidence reconciliation — 2026-11-06

- Re-read the controlling **OWNER-LOCKED VYOM FACTORY CORE ORDER** at the live branch. The eight-item order in this file supersedes the older P0/P1/P2 and Stage 1–7 instructions. Continue strictly in order; do not start task engine item 3 while secrets item 2 is unresolved.
- Rechecked the Library and GitHub repository for the original historical scan report. The only recovered file is `vyom-prepublic-scan.sh`; inspection confirms it is a working-tree heuristic for private-key/GitHub/Google/AWS/credential-assignment patterns plus optional Gitleaks. It contains no Discord-token regex and no historical per-file/per-line/per-commit report. It cannot reproduce the original 169 Discord-pattern count.
- Targeted Library search for the original redacted inventory/transcript and GitHub code search for the report/rule names returned no matching report. The original 169 Discord-pattern findings remain **UNCLASSIFIED**; original regex/rule and per-file/line/commit inventory are still unavailable. Historical 3 credential-assignment findings remain incompletely reconciled (two current test fixtures found; third original match not mapped). Four OpenAI-style findings remain classified as test-only fixtures from prior source review.
- Downloaded and inspected the exact redacted full-history Gitleaks report artifact from run `42229748698`, artifact `13507512039`, SHA-256 `cac41625226e4ae1db7147aa3a67cae31b89b807b8f79c99afae043728b82baf`. It contains six `generic-api-key` findings across 1,693 scanned commits: two test fixture literals in `SelfModificationPolicyTest.kt` (lines 68 and 72), and four documentation-only matches in HANDOFF text (commit-hash/class-identifier/token-description text). These six are separate from the owner's original 169/3/4 aggregate and do not classify those findings. The report stores secret fields as REDACTED; no secret values were exposed or copied into this file.
- Exact-head Full CI `42229748698` on `68dd0bca8172210a5344f639d3bf125f7dc8a14e` = **FAILURE** at full-history Gitleaks; report upload succeeded, subsequent unit-test/APK steps were skipped.
- Exact-head Debug run `42229748504` on the same SHA completed **SUCCESS**. `Build and test APKs`, packaging/checksums, and signed no-APK content-update demo all passed. Artifact upload and release publication were **SKIPPED** by the new ordinary-push gate; therefore no artifact is available from this run and no APK is claimed as delivered.
- Core item 1 audit/HANDOFF sync is refreshed by this entry. Core item 2 remains **BLOCKED** by missing original 169-match evidence and unresolved mapping of the third historical credential-assignment match. Items 3–8 remain not started. No Android `main`, Render, Hermes backend, or production Supabase changes.

### Exact Gitleaks false-positive fingerprint handling — 2026-11-06

- Added root `.gitleaksignore` with **six commit-qualified fingerprints only**, matching the six redacted findings classified from full-history Gitleaks artifact `13507512039`: two known test fixtures and four HANDOFF documentation strings. No rule-wide, path-wide, or secret-pattern allowlist was added; later commits/new locations remain detectable. The six original occurrences remain recorded as reviewed false positives, not deleted from history.
- Change commit: `196702a5ff4faf15f2cecb68400bb3cd6c065049`.
- Exact-head CI for that SHA: Full CI `42230823644` was PENDING at last check; Debug `42230823628` was IN_PROGRESS during Gradle setup. No pass is claimed. The preceding SHA's Debug run `42229748504` completed SUCCESS for build/tests/package/checksums/demo, but artifact upload/release were skipped by the ordinary-push gate; Full CI `42229748698` failed at the six known Gitleaks findings.
- This narrow fingerprint handling addresses only the six Gitleaks findings. It does **not** resolve the owner's separate 169 historical Discord-pattern findings or the unmapped third credential-assignment finding. Core item 2 remains BLOCKED; do not start item 3.


### Owner batch close — Items 1 + 2 — 2026-11-06

- Owner's corrected scope/order accepted as controlling: the eight core items are exactly 1) audit + HANDOFF sync, 2) secrets check, 3) task engine, 4) system registry, 5) planner/detector/prioritizer, 6) worker/isolated executor, 7) verifier/real progress, 8) self-upgrade. The out-of-core systems listed by the owner are deferred to Factory itself; do not build them in this batch.
- Item 1 — **CLOSED for this batch**. Audit target source HEAD: `8f37a8fe26f6d837763a0dd6957b2609587109f2`, branch `feature/memory-intelligence`. HANDOFF evidence synchronized here. Relevant paths: `.github/workflows/vyom-android-ci.yml` (full reachable-history Gitleaks scan and redacted report artifact); `.gitleaksignore` (six exact commit-qualified fingerprints only); `docs/HANDOFF.md` (this audit ledger). Exact-head Full CI run `42230892698` = **SUCCESS** and Debug CI run `42230892663` = **SUCCESS**, both on the same SHA. Full CI passed Termux adapter syntax/intake checks, configured secret scan, pinned Gitleaks installation, full reachable Git history scan, redacted report upload, unit tests/debug APK, and APK packaging/checksum. Debug CI passed build/tests and packaging; upload/release steps were skipped by the explicit manual-publish gate. No phone/device verification is claimed.
- Item 2 — **CLOSED WITH CAVEAT** under the owner's revised acceptance rule. Fresh full reachable-history Gitleaks scan ran on exact HEAD `8f37a8fe26f6d837763a0dd6957b2609587109f2`, Full CI run `42230892698`; scan step **SUCCESS**. Redacted report artifact `13510720080` (`redacted-full-history-gitleaks`) was downloaded and inspected; `history-gitleaks.json` contains `[]` (zero findings). Gitleaks scanned the full reachable Git history as configured by `--log-opts="--all"`. This is the fresh current-head security evidence required by the owner; it supersedes the previous requirement to recover the old 169-hit report as a blocker.
- Caveat: the fresh Gitleaks result establishes zero findings under the configured Gitleaks rules across reachable history at this HEAD. It does **not** reproduce the original custom Discord regex or prove that the old aggregate 169/3/4 counts were mapped one-to-one; those historical counts remain unverified as historical report data. No confirmed real secret was exposed by this scan. Keep the caveat visible; do not overstate as universal proof that no credential of any kind ever existed.
- The six prior findings remain handled only by the six commit-qualified entries in `.gitleaksignore`; no broad rule/path suppression was added. Fresh report zero confirms the current scan passes with those narrow exceptions in place; future new commits/locations remain in scope.
- Items 3–8 are now **UNLOCKED BY ORDER**, but not started in this batch. Item 3 must be the next batch only after owner GO. Reuse and verify the existing task engine; do not rebuild it.
- Repository metadata at the last verified check still reported `visibility=public`. This conflicts with the owner's private-repository requirement. No visibility change was made in this batch; owner must make the repository private before private data or remote screenshot uploads are used. No Android `main`, Render, backend, or production Supabase changes.
- Current source HEAD for the evidence above: `8f37a8fe26f6d837763a0dd6957b2609587109f2`. HANDOFF synchronization commit follows this source HEAD; its own documentation-only exact-head CI is separate and must not be conflated with the source evidence above.


### Core item 4 — system registry / exact-HEAD verification update — 2026-11-06

- Active branch: `feature/memory-intelligence`; implementation HEAD audited: `eb6c4277110cdefb5aabdb374d1189818da66932`.
- Exact-HEAD Full CI run `42239663718` = **SUCCESS**. The workflow completed the full reachable-history Gitleaks scan, unit tests/debug APK, versioned APK/checksum packaging, and debug APK artifact upload.
- Exact-HEAD Debug CI run `42239663726` = **SUCCESS**. Build/tests, APK packaging/checksums, and the no-APK content-update demo package passed. Artifact upload/release steps were skipped by the explicit manual-publish gate; do not treat this workflow as the uploaded APK evidence.
- Full CI artifact `13515567221`, name `vyom-phase6-eb6c4277110cdefb5aabdb374d1189818da66932`, size 46,340,612 bytes, SHA-256 `e1cf1ab17f33de31b964dd502b1824b576656310acf742b91acfa65e2cae294f`, expires 2026-12-06. This proves CI artifact publication only, not phone installation or device acceptance.
- The previous fixture-correction SHA `f5cd14a6e9b066041a81f846d8a0fd9ca92d5b2e` also has Full CI `42239150235` = **SUCCESS** and Debug CI `42239150248` = **SUCCESS**.
- Item 4 code paths: `app/src/main/java/com/hermes/assistant/VyomFactorySystemRegistry.kt` stores owner-confirmed per-system repository URL, immutable commit SHA, and explicit allowed path roots; `VyomFactoryRepositoryClient.kt` enforces the binding before `readBoundSystemFile`; `VyomFactoryRepositoryReviewDialog.kt` captures system ID and path roots; tests cover approval, immutability, traversal, root-boundary, cross-system isolation, and repository/commit mismatch.
- **Item 4 remains IN PROGRESS.** The existing guarded adapter is read-only. No repository write/change adapter or end-to-end enforcement at every worker/executor repository access has been verified. Do not claim complete isolation or close item 4. Next implementation work must identify all repository mutation/read call paths and route them through a common system-bound authorization layer before adding any write transport.
- Item 3 remains closed with its documented single-device coordination caveat; continuous lease heartbeat remains unverified/not implemented. Do not reopen or rebuild it.
- No Android `main`, Render, Hermes backend, or production Supabase changes. No physical-device test claimed. Repository visibility remains public at the last metadata check; do not upload private data.

### Item 4 repository mutation-path audit — 2026-11-06

- Audited exact source HEAD `1e7258dccb7d1a5c65a917c5e32ab2c1cd385a7b`.
- Exact-head Full CI run `42240924654` = **SUCCESS**. Exact-head Debug CI run `42240924581` remained **IN PROGRESS** at the latest check (job `127970683455`, step `Build and test APKs`). Do not claim Debug CI success until completed.
- Confirmed source-content write bypass: `VyomFactoryGitHubClient.attachScreenshot()` sends GitHub Contents API `PUT` to `.factory-evidence/issue-$issueNumber/<uuid>` on fixed branch `feature/memory-intelligence`. It checks repository privacy and explicit owner tap, but does not resolve a system ID, immutable registry binding, allowed path root, or expected branch head. This is an actual repository-content mutation outside Item 4's system/path authorization. The current repository metadata reports `private=false`, so the upload path fails closed today; privacy check is not a substitute for registry authorization if visibility later changes.
- Call path: `VyomFactoryDialog.uploadScreenshot()` → `client.attachScreenshot()`; UI says upload is owner-confirmed and then links the resulting file in the issue. Do not silently remove this feature. A safe fix needs an explicit decision on whether evidence storage is a registered Factory system/path and must define immutable base/branch, expected-head concurrency, allowed path, task/issue identity and commit verification.
- Distinguished non-source writes: `createTask/updateTask/addEvent` POST/PATCH GitHub Issues endpoints (task metadata/comments), not repository file contents. `VyomFactorySkillStore.install()` writes app-private inert Markdown locally, not to the source repository. Generic repository intake inspection is read-only preview, not executor-authorized access.
- No repository write adapter exists for worker execution; no end-to-end executor read/write isolation claim. Item 4 remains **IN PROGRESS**. No code behavior changed in this audit entry; no Android `main`, Render, Hermes backend, or production Supabase changes.


### Exact-HEAD CI verification before factory-evidence replacement — 2026-11-06

- Audit target: `a7e87dc55c450361b48814a863ae5322bc5a6cca` on `feature/memory-intelligence`.
- Exact-HEAD Full CI run `42241749960` = **SUCCESS**. Job `debug` completed successfully, including configured secret scan, full reachable-history Gitleaks scan, unit tests/debug APK, APK packaging/checksum and artifact upload.
- Exact-HEAD Debug CI run `42241750103` = **SUCCESS**. Job `debug` completed successfully; build/tests and packaging passed. Debug APK artifact/release upload steps were skipped by the existing manual-publish gate.
- Both runs report the exact target SHA above; no result from another commit is substituted.
- Current audit confirms the legacy screenshot writer is `VyomFactoryGitHubClient.attachScreenshot()`: GitHub Contents API PUT to `.factory-evidence/issue-$issueNumber/$fileName`, hard-coded branch `feature/memory-intelligence`; UI caller is `VyomFactoryDialog.uploadScreenshot()`. It does not consult the system registry. The public-repository guard currently blocks it, but is not an authorization boundary for a future private repository.
- Owner approved replacement with a `factory-evidence` system binding (exact repository + immutable commit + `.factory-evidence/` allow-root), dedicated evidence branch, expected-head fencing, non-force ref update, fail-closed checks, content hash verification before issue comment, and explicit upload/comment failure handling. Required tests: unregistered system, wrong repository, wrong commit, traversal, unauthorized path, stale branch, upload failure, comment failure.
- No source behavior changed in this audit/documentation entry. Implementation remains pending; no claim of screenshot upload safety until replacement and exact-HEAD CI pass. No Android `main`, Render, backend, or production Supabase changes.


### Exact-HEAD CI verification before factory-evidence replacement — 2026-11-06 (synchronized)

- Exact audit target `a7e87dc55c450361b48814a863ae5322bc5a6cca`: Full CI `42241749960` = **SUCCESS**; Debug CI `42241750103` = **SUCCESS**. Both workflow runs report the exact target SHA. Full CI completed secret scan, full reachable-history Gitleaks, unit tests/debug APK, packaging/checksum and artifact upload. Debug CI build/tests and packaging passed; artifact/release publication was skipped by the manual-publish gate.
- At HANDOFF synchronization, active branch `feature/memory-intelligence` HEAD was independently read as `bb5ddbc6cd0288d698ac1600d61c05b9b1988ccf`. This is newer than the audited target; the target CI evidence above is not represented as validation of this newer HEAD.
- Approved next change: replace `VyomFactoryGitHubClient.attachScreenshot()` fixed-branch Contents API writer with a registry-bound `factory-evidence` system, exact repository + immutable commit, `.factory-evidence/` allow-root, dedicated evidence branch, expected-head check, non-force update, fail-closed behavior, content hash verification before issue comment, and explicit upload/comment failure handling.
- Required regression cases: unregistered system, wrong repository, wrong commit, path traversal, unauthorized path, stale branch, upload failure, comment failure. Implementation and exact-HEAD CI remain pending. No design/implementation-plan documents will be added per owner direction.
- No Android `main`, Render, backend, or production Supabase changes. No physical-device verification claimed.


### Factory evidence writer replacement — implementation and exact-HEAD CI checkpoint — 2026-11-06

- Re-verified the previously requested audit commit `a7e87dc55c450361b48814a863ae5322bc5a6cca` by exact `head_sha`: Full CI run `42241749960` = **SUCCESS**; Debug CI run `42241750103` = **SUCCESS**. Both runs' `head_sha` fields exactly equal the audit commit. This resolves the earlier unverified status for that audit commit.
- Re-read the active branch ref. Before this HANDOFF update, exact active HEAD was `6337b06c9d97e253ac67edfa8bdbb9636c0d9f85`.
- At that exact HEAD, Full CI run `42248798201` = **SUCCESS** and Debug CI run `42248798218` = **SUCCESS**; both report the exact SHA. Full CI job completed successfully; APK artifact/release publication was skipped by the configured manual-publish gate. Debug CI job completed successfully; its debug artifact/release publication was also skipped by the manual-publish gate.
- Replaced the old screenshot writer with `app/src/main/java/com/hermes/assistant/VyomFactoryEvidencePublisher.kt` (Git Database blob/tree/commit/ref transport and publishing policy). The publisher requires the registered `factory-evidence` binding, exact repository URL and immutable base commit, and a path under `.factory-evidence/`; it checks repository privacy, uses a dedicated `factory-evidence` branch, checks expected branch head before ref update, uses non-force update, reads the uploaded content back by commit SHA, verifies SHA-256, then attempts the issue comment.
- Pending screenshot state/receipts are in `app/src/main/java/com/hermes/assistant/VyomFactoryScreenshotEvidence.kt`. UI integration is `app/src/main/java/com/hermes/assistant/VyomFactoryDialog.kt:703`; it resolves the owner-confirmed registry binding and invokes the publisher. On comment failure it preserves a receipt and does not claim the issue comment succeeded. Upload errors preserve the local screenshot.
- Removed `VyomFactoryGitHubClient.attachScreenshot()` and removed the generic HTTP PUT capability from `VyomFactoryGitHubClient.kt`. The UI no longer calls the old method. The dedicated Git Database transport is the intended sole source-content writer for this evidence path.
- Added publisher policy tests in `app/src/test/java/com/hermes/assistant/VyomFactoryEvidencePublisherTest.kt`: unregistered system, wrong repository, wrong pinned commit, path traversal, path outside registered root, public-repository fail-closed, stale branch head, concurrent head race, upload failure, comment failure, and success/hash/non-force assertions.
- Added HTTP contract tests in `app/src/test/java/com/hermes/assistant/VyomFactoryScreenshotAttachmentTest.kt` for public-repository fail-closed and the private-repository dedicated-branch Git Database flow, expected-head recheck, non-force ref update, read-back hash verification, and comment ordering.
- Exact-HEAD CI log for Full run `42248798201` ended with `BUILD SUCCESSFUL`; no test/build failure was reported. Test source warnings only: deprecated `createTempDir` usage.
- **Remaining verification before Item 4 closure:** complete repository-wide source grep at the latest HEAD for `attachScreenshot`, Contents API `/contents/` screenshot writer, and fixed-branch evidence upload patterns; review the publisher/store for replay/idempotency and ambiguous issue-comment outcomes; ensure all findings are documented. The branch remains Item 4 IN PROGRESS until those checks and any needed fixes pass exact-HEAD Full + Debug CI. Do not start Item 5 yet.
- No Android `main`, Render, backend, or production Supabase changes. No physical-device test claimed; no final APK published.

### Factory evidence replacement — exact-head audit and legacy-writer grep — 2026-11-07

- Active branch exact HEAD audited: `26f264b561e16bce4ece0ba9082a6e4efb32e5c8` (`feature/memory-intelligence`).
- Exact-HEAD Full CI run `42251568240` = **SUCCESS**; its `head_sha` exactly matches this HEAD. Job `128007723724`: Termux adapter checks, secret scan, full reachable-history Gitleaks scan, unit tests/debug APK, APK packaging/checksum, and Full CI debug APK artifact upload all completed successfully.
- Exact-HEAD Debug CI run `42251568334` = **SUCCESS**; its `head_sha` exactly matches this HEAD. Job `128007723795`: build/tests, APK packaging/checksums, and no-APK content-update demo package passed. Debug artifact/release publication was skipped by the configured manual-publish gate.
- Exact Git tree `382cbba5d0f420ec221133e887a8a4c465fc1b77` contains 113 production Kotlin/Java files under `app/src/main`. A targeted source scan across all 113 files for `attachScreenshot`, `.factory-evidence`, `factory-evidence`, GitHub `/contents/`, Git ref writes, and HTTP PUT found no legacy `attachScreenshot` symbol or old fixed-branch uploader callsite. The only evidence-content writer found is `VyomFactoryEvidencePublisher.kt`; its Contents API use is GET-only read-back pinned to the new commit.
- Other source matches are not alternate screenshot uploaders: `VyomFactoryRepositoryClient.kt` and `VyomFactorySkillRepository.kt` use Contents API GET for read-only inspection/import. `SelfModificationClient.kt` still has a separate Contents API PUT for user-approved self-change proposals; `SelfModificationPolicy.kt` restricts those paths to app Kotlin source files and excludes `.factory-evidence` (which is outside the allowed path regex). It is a separate Item 4 repository-mutation authorization gap to audit later, not a route to upload Factory screenshots.
- Required publisher policy tests are present in `app/src/test/java/com/hermes/assistant/VyomFactoryEvidencePublisherTest.kt`: unregistered system, wrong repository, wrong pinned commit, traversal, unauthorized path, public-repository fail-closed, stale branch, concurrent head race, upload failure, comment failure/receipt replay block, and successful hash-before-comment/non-force behavior. HTTP contract coverage is in `app/src/test/java/com/hermes/assistant/VyomFactoryScreenshotAttachmentTest.kt`. Both exact-HEAD CI workflows' test/build steps passed.
- **Public repository exposure finding:** exact HEAD's Git tree still contains an existing binary at `.factory-evidence/issue-101/a1ef05be-53bc-4e39-94c9-005b4b455ef4.png`. The repository metadata in the exact-HEAD Full/Debug workflow responses reports `private=false`. This is a pre-existing committed screenshot in the public repository, not created by this replacement batch. It was not downloaded, inspected, or deleted. Treat the image as publicly exposed; owner should review its sensitivity and explicitly authorize any removal/history remediation. The publisher's current privacy guard correctly rejects new uploads to a public repository.
- Item 4 remains **IN PROGRESS**: the screenshot uploader bypass is replaced and the legacy callsite grep is clear, but system-to-repository mapping and all remaining repository read/write enforcement (including the separate self-modification proposal writer) are not complete. Do not start Item 5. Next after owner GO: continue Item 4 system-by-system repository binding/path enforcement audit.
- No Android `main`, Render, backend, or production Supabase changes. No physical-device verification or final APK publication claimed.

### Factory evidence batch closure + Item 4 continuation audit — 2026-11-07

- Exact active HEAD before this HANDOFF entry: `f7404653e7276ec08eda5ead9beb13e44a6d1f07`.
- Exact-HEAD Full CI run `42341773795` = **SUCCESS**; exact-HEAD Debug CI run `42341773797` = **SUCCESS**. Both runs report `head_sha=f7404653e7276ec08eda5ead9beb13e44a6d1f07`. Full CI completed Termux adapter checks, secret scan, full reachable-history Gitleaks scan, unit tests/debug APK, packaging/checksum and artifact upload. Debug CI completed build/tests and packaging; debug artifact/release publication remains behind the manual-publish gate.
- This closes the factory-evidence uploader replacement batch's CI requirement. The previous source grep remains: 113 production Kotlin/Java files scanned; no `attachScreenshot` callsite or legacy fixed-branch screenshot writer. Required publisher policy/HTTP tests are present and CI passed.
- Item 4 continuation audit found remaining repository access paths:
  - `VyomFactoryRepositoryClient.inspect(repositoryUrl)` and `previewFiles(preview)` are explicitly read-only intake/preview APIs for arbitrary user-submitted repositories; they pin to resolved immutable commit, redact secret patterns and return untrusted content. They are not execution authority and must remain separated from worker execution.
  - `VyomFactoryRepositoryClient.readBoundSystemFile(systemId, relativePath, registry)` is the only current per-system guarded read path; it resolves exact repo+commit and checks allowed roots before network access.
  - `VyomFactorySkillRepositoryClient` reads public `SKILL.md` at a resolved commit, validates license and stores content as inert app-private Markdown. It is an import/catalogue path, not source execution/write.
  - `VyomFactoryGitHubClient` writes GitHub Issues metadata/comments (POST/PATCH) for Factory tasks, not repository source content.
  - `SelfModificationClient` is a distinct repository source writer. It currently has a hard-coded API root `https://api.github.com/repos/riyas433554-dot/vyom-android`, hard-coded proposal base branch `feature/memory-intelligence`, GETs current source, creates proposal branch, PUTs source through Contents API, and opens a PR. `SelfModificationPolicy` restricts target paths to a subset of `app/src/main/java/com/hermes/assistant/*.kt` and excludes protected files, but it does **not** consult `VyomFactorySystemRegistry` for a system ID, exact repository binding, immutable base commit, or allowed roots. This remains an Item 4 authorization bypass for the self-modification system. Do not treat its path regex alone as system registry enforcement.
- No self-modification behavior changed in this audit. Next Item 4 implementation batch: require an owner-approved registry binding for a dedicated self-modification system ID, derive repository and immutable base commit only from that binding, require the policy allowlist to be a subset of registry allowed roots, and test denial for missing/mismatched binding and paths outside either allowlist. Preserve PR-only/no-auto-merge behavior and ensure PR base/head are pinned and race-checked; do not reuse the screenshot evidence writer.
- The existing screenshot binary `.factory-evidence/issue-101/a1ef05be-53bc-4e39-94c9-005b4b455ef4.png` remains in the public repository tree (`private=false` at last verified metadata). It may be publicly exposed. No deletion/history rewrite was authorized or performed.
- Item 4 remains **IN PROGRESS**. Do not start Item 5 until all system read/write paths are bound and tested. No Android `main`, Render, backend, or production Supabase changes. No physical-device verification or final APK publication claimed.

### Item 4 — self-modification repository binding enforcement — implementation checkpoint — 2026-11-07

- Self-modification is now wired to the owner-approved `self-modification` system registry entry. `SelfModificationClient` requires a non-null `VyomFactorySystemRegistry`; it no longer has a constructor path that can run without a registry.
- The GitHub API repository root is derived from the registered canonical `https://github.com/{owner}/{repo}` URL. The former hard-coded `riyas433554-dot/vyom-android` API root is removed from this client. A test-only API-root override exists solely for MockWebServer tests.
- Path discovery filters the Git tree through both `SelfModificationPolicy.isAllowedPath()` and the registry's `allowsPath("self-modification", path)`. Draft generation and proposal creation call `SelfModificationPolicy.requireSystemBinding()` before any repository request, enforcing both the narrow policy and the registry path root.
- Path discovery, draft generation, and proposal creation require the configured base branch HEAD to equal the registered immutable commit SHA. Proposal creation repeats the base-HEAD check after uploading the proposal branch's source and immediately before opening the PR; a moved base fails closed and no PR is opened.
- MainActivity's three self-modification entry points now pass `VyomFactorySystemRegistry(File(filesDir, "factory-system-registry"))`. Proposal PRs remain review-only; no merge, release, or install action was added.
- Tests updated for mandatory registry injection and exact pinned-HEAD checks. Added client-level denial for a missing owner-approved binding; existing registry tests cover wrong repository, wrong commit, and unauthorized roots; integration/path-discovery tests cover PR-only behavior, stale branch rejection, and pre-PR base recheck.
- Exact-HEAD CI was pending on implementation SHA `39133b6a1ef379c833ce30b11aef09c50c626425`: Full CI run `42342924692` = **PENDING**; Debug CI run `42342924749` = **IN_PROGRESS**. These runs must be rechecked against the final HEAD after this HANDOFF update; no passing result is claimed yet.
- Operational requirement: an owner must explicitly register a `self-modification` binding whose repository URL is canonical, commit SHA exactly matches the working base branch HEAD, and allowed roots intersect `app/src/main/java/com/hermes/assistant`. Missing or stale binding blocks this workflow by design.
- Item 4 remains **IN PROGRESS**. This closes the self-modification writer's registry bypass only; continue auditing every other system's repository read/write path and mapping before Item 4 closure. Do not start Item 5.
- No Android `main`, Render, backend, or production Supabase changes. No physical-device verification or final APK publication claimed.

### Item 4 — self-modification registry enforcement and repository access audit — 2026-11-07

- Final implementation checkpoint before this HANDOFF update: `f81cb0e0886a3dda32a52641c26944704a1963d2`.
- `SelfModificationClient` now requires a non-null registry dependency and an owner-approved `self-modification` binding. Repository API root and recovery owner are derived from the canonical registered repository URL; the hard-coded production repository target was removed. The only API-root override is a test injection for MockWebServer.
- All three MainActivity entry points pass the app-private `factory-system-registry`. Discovery filters files through both the SelfModificationPolicy and registry allowed roots. Draft/read/write paths require the same system binding before any GitHub request.
- The configured feature branch HEAD must exactly equal the registered immutable commit for discovery, draft generation, and proposal creation. Proposal creation rechecks the pinned base after the source upload and immediately before PR creation. A changed base aborts and cleans up the proposal branch; it does not open a PR. Proposals remain PR-only, with no merge/release/install path.
- Tests cover missing binding at the client boundary, wrong repository/commit/path at the policy boundary, owner approval, exact base pinning, stale branch during discovery, base movement after upload, and no-merge/no-install proposal behavior.
- Repository access audit: `VyomFactoryRepositoryClient.readBoundSystemFile` is guarded by registry repo+commit+path before network access. `inspect`/`previewFiles` are arbitrary-repository read-only intake/preview paths pinned to inspected immutable commits; returned content is untrusted and redacted, not executed. `VyomFactorySkillRepositoryClient` only imports bounded public `SKILL.md` as inert text. `VyomFactoryEvidencePublisher` is the separately registry-bound `.factory-evidence/` writer. `VyomFactoryGitHubClient` writes issue metadata/comments, not repository source content. No generic worker repository write adapter was found; repository execution must not be claimed or enabled through these read-only paths.
- Exact-HEAD Full CI run `42343030592` and Debug CI run `42343030440` both target code SHA `f81cb0e0886a3dda32a52641c26944704a1963d2`; at this checkpoint both were **IN_PROGRESS** (Full was in unit tests/debug APK; Debug was in build/tests). No pass is claimed. Recheck the CI triggered by this HANDOFF commit as well.
- Operational caveat: the owner must register `self-modification` with a canonical repo URL, exact current feature-branch commit SHA, and allowed roots intersecting `app/src/main/java/com/hermes/assistant`. The registry intentionally rejects silent rebinding; explicit migration/update UX remains an Item 4 follow-up before this binding can be conveniently advanced after branch commits.
- Item 4 remains **IN PROGRESS**. Do not start Item 5. Continue only after exact-HEAD CI results are known, then close remaining registry/system mapping gaps. No Android `main`, Render, backend, or production Supabase changes. No physical-device verification or final APK publication claimed.

### Item 4 — self-modification test failure correction — 2026-11-07

- Current implementation/test checkpoint before this HANDOFF update: `d670604f695819d3ea68bfbf0535755ededcb321`.
- A real Debug CI failure was inspected from run `42343017890` (SHA `14dd38e0be8a2024311f04c7b9b483a5c12ed38f`). The log identified a malformed escaped Kotlin string in the base-movement regression test and unresolved `File.createTempDir()` references in three test files. The production Android sources compiled and `assembleDebug` completed; failure was in `compileDebugUnitTestKotlin`.
- Corrected the test string escaping and replaced every `File.createTempDir()` with a Java `File.createTempFile(...).apply { delete(); mkdirs() }` helper in `SelfModificationClientIntegrationTest`, `SelfModificationPathDiscoveryTest`, and `SelfModificationSystemBindingTest`.
- Latest exact-HEAD Full CI run `42343276964` targets `d670604f695819d3ea68bfbf0535755ededcb321` and is **PENDING**; latest exact-HEAD Debug CI run `42343277063` targets the same SHA and is **IN_PROGRESS** in Build and test APKs. No passing result is claimed. Fix any further failure from its real log.
- Item 4 remains **IN PROGRESS**; Item 5 has not started. The previous registry enforcement, repository access audit, owner-binding operational caveat, and no-backend/main restrictions remain in force.

### Item 4 — explicit registry migration + immutable source history — 2026-11-07

- Added `VyomFactorySystemRegistry.migrate(replacement, expectedExisting, ownerConfirmed)`: requires explicit owner confirmation, same system ID, valid canonical repository/commit/path roots, and an exact compare-and-swap match against the prior binding. A stale confirmation fails closed. Existing `register` remains immutable and does not silently overwrite.
- Updated `VyomFactoryRepositoryReviewDialog`: when a system ID already has a different binding, it displays old/new repository URL, commit, and path roots and requires a separate `CONFIRM MIGRATION` action. A repeat registration with identical repo/commit/roots reuses the existing binding and does not spuriously request migration.
- Repository catalogue now stores immutable snapshots by repository URL + commit, so multiple revisions of the same repository can coexist without overwriting history. It retains backward compatibility with existing URL-only catalogue entries: same old revision is reused; a different revision is added separately.
- Added registry migration tests for owner approval, successful compare-and-swap, stale expected binding, and refusing to create a new system through migration. Updated repository catalogue test to prove distinct immutable revisions coexist.
- Audit evidence: evidence publisher requires `factory-evidence` registry binding and `.factory-evidence/` allowed path before write; MainActivity self-modification callsites inject the app-private registry; repository `readBoundSystemFile` requires repo/path binding and reads only pinned commit. General repository inspect/preview and skill import remain read-only untrusted text, not execution adapters. No generic repository write adapter exists; do not treat read-only intake/preview as worker execution.
- Latest code HEAD `252a51d4c52715c51f8f389a86fa50c12a9a4ed3`: Full CI `42348154408` = **PENDING**; Debug CI `42348154378` = **IN_PROGRESS**. Recheck both against the final HANDOFF-updated HEAD; no pass claimed for this batch yet.
- Item 4 remains **IN PROGRESS**. Do not start Item 5. Remaining audit must enumerate all system IDs and confirm each repository write path has a binding and path gate; worker repository execution remains unavailable until a dedicated isolated writer adapter is implemented and verified.
- No Android `main`, Render, backend, or production Supabase changes. No physical-device verification or final APK publication claimed.

### Item 4 — remaining GitHub Issues system binding gap — 2026-11-07

- Further source audit found `VyomFactoryGitHubClient` is the Factory task ledger and performs GitHub Issues writes (`POST /issues`, `PATCH /issues/{n}`, `POST /issues/{n}/comments`) against its hard-coded `DEFAULT_REPOSITORY = riyas433554-dot/vyom-android`. These are issue metadata/comment writes, not Contents API source-file writes, but the task-engine repository target is not yet resolved through `VyomFactorySystemRegistry`.
- This is an Item 4 gap: add an explicit `factory-task-engine` binding (repository URL + virtual allowed root such as `issues`), derive the Issues API repository from that binding, fail closed on missing/mismatched binding, and provide a reachable owner-registration path before making the binding mandatory. Do not break the initial Factory onboarding path.
- Until that work is completed, Item 4 cannot be closed and Item 5 must not start. This audit finding is not yet implemented.

### Item 4 — Factory task-engine Issues binding enforcement — 2026-11-07

- `VyomFactoryGitHubClient` no longer has a hard-coded `DEFAULT_REPOSITORY` or caller-selectable repository string. Every repository-scoped Issues operation resolves `factory-task-engine` from `VyomFactorySystemRegistry`, authorizes the virtual `issues` root, parses the canonical bound repo URL, and only then builds the API request. Missing binding or missing `issues` root fails before network access. The GitHub `/user` identity check remains outside repository scope.
- `MainActivity` direct client construction now goes through an app-private registry helper; `VyomFactoryDialog` also injects the same app-private registry. `showVyomFactory` blocks task access when the binding is missing/unreadable or lacks `issues`, then opens the owner review dialog prefilled with `factory-task-engine` and `issues`. After successful owner registration/migration, Factory reopens.
- `VyomFactoryRepositoryReviewDialog` now supports default URL/system/path values and an `onRegistered` continuation. Existing system bindings with different repo/commit/path roots require a separate confirmation showing current and proposed values; identical bindings are reused.
- `VyomFactoryRepositoryRegistry` now retains multiple immutable snapshots for the same canonical repo, keyed by URL + commit. It still recognizes legacy URL-only entries, reuses the exact same legacy revision, and adds a distinct snapshot for a different commit rather than silently overwriting history.
- Added `VyomFactoryGitHubClientBindingTest`: approved repo is used for Issues API path; missing binding and unauthorized `issues` root both fail before network access. System registry migration tests cover owner approval, exact prior binding compare-and-swap, stale binding rejection, and no implicit system creation.
- Remaining Item 4 audit status: repository file-content writes are limited to registry-bound self-modification proposals and registry-bound `.factory-evidence/` uploads; GitHub Issues writes are now separately bound to `factory-task-engine/issues`. Repository inspection/preview and public skill import remain read-only untrusted text. No generic worker repository write adapter exists; worker repository execution remains unavailable.
- Current code checkpoint before this HANDOFF update: `9e560e60855913f9f5a4af1d4c21d8924a92ca99`. Exact-HEAD Full CI `42348405265` = **PENDING**; Debug CI `42348405283` = **QUEUED**. Recheck the CI triggered by this HANDOFF commit against its exact new HEAD; no passing result is claimed yet.
- Item 4 remains **IN PROGRESS** until the current exact-HEAD CI passes and the final source-write/registry audit is closed. Item 5 has not started. No Android `main`, Render, backend, or production Supabase changes. No physical-device verification or final APK publication claimed.


### Item 4 — exact-HEAD CI compile correction — 2026-11-07

- Debug CI run `42348424940` checked out exact code SHA `65ae15eaff47c75188aaf87e206d94f73666c8b3` and failed at Kotlin compilation. The real log reported: (1) `VyomFactoryDialog.kt:211` passed a trailing lambda into the wrong constructor parameter; `onStatus` is required, and (2) `VyomFactoryEvidencePublisher.kt:136` retained a removed `VyomFactoryGitHubClient.DEFAULT_REPOSITORY` reference.
- Corrected the review-dialog call to use named `onStatus`; removed the evidence transport's hard-coded default repository and made its repository argument mandatory; the evidence attach callsite now derives owner/name from the already-approved `factory-evidence` registry binding and passes that explicit repository into the transport. This prevents a silent fallback to the app repository.
- Source audit confirms MainActivity injects the app-private `self-modification` registry into all three SelfModificationClient callsites (path discovery, draft generation, and proposal creation). Client methods require the registered binding, check path against both SelfModificationPolicy and registry roots, pin the working branch HEAD to the immutable registry commit, and derive API root from the bound repository. Discovery enumerates only paths allowed by both policy and registry. This is source-level verification; current post-fix CI remains pending.
- Latest code fixes are on `feature/memory-intelligence`; the prior exact-HEAD Debug run failed and the prior Full CI run `42348424960` was still in progress at SHA `65ae15eaff47c75188aaf87e206d94f73666c8b3`. New CI must be checked against the final HANDOFF-updated HEAD; no passing result is claimed.
- Item 4 remains **IN PROGRESS**. Do not start Item 5. No Android `main`, Render, backend, or production Supabase changes. No physical-device verification or final APK publication claimed.


### Item 4 — registry constructor test fixture correction — 2026-11-07

- Exact-HEAD CI at `687cfd00db0345b47a211381caddc777397bda6c` compiled production sources and assembled the debug APK, but unit-test compilation failed because `ProposalIssueAuditTest` still called the removed `VyomFactoryGitHubClient(token, repository, ...)` constructor three times and `VyomFactoryLifecycleIntegrationTest` still supplied the removed `repository` named argument.
- Updated both test fixtures to create an explicit owner-confirmed `factory-task-engine` registry binding for `https://github.com/test/repo`, authorize only the virtual `issues` root, and inject that registry into the client. This keeps tests aligned with the new fail-closed production constructor rather than adding a backwards-compatible hard-coded repository path.
- CI failures: Debug `42348717901`; Full `42348717907`. Both reached `compileDebugUnitTestKotlin` and failed only on the stale test constructors. Full-history Gitleaks scan passed in Full CI.
- Corrected test files: `app/src/test/java/com/hermes/assistant/ProposalIssueAuditTest.kt`, `app/src/test/java/com/hermes/assistant/VyomFactoryLifecycleIntegrationTest.kt`. Exact-HEAD CI is pending for the updated branch head; no pass is claimed.
- Item 4 remains **IN PROGRESS**. Item 5 remains blocked until registry/write-path audit and exact-HEAD Full + Debug CI pass.


### Item 4 — exact-HEAD CI pass + final source access audit checkpoint — 2026-11-07

- Exact active branch code HEAD: `9a11258ae1b97a14cce16f4b586a6545d415c922` on `feature/memory-intelligence`.
- Exact-HEAD Debug CI run `42349071429` = **SUCCESS** and Full CI run `42349071421` = **SUCCESS**; both report the exact same `head_sha=9a11258ae1b97a14cce16f4b586a6545d415c922`. Debug workflow completed `Build and test APKs` and versioned APK packaging/checksum. Full workflow completed secret scan, pinned Gitleaks install, full reachable-history scan, unit tests/debug APK, packaging/checksum and artifact upload. Full run artifact `vyom-phase6-9a11258ae1b97a14cce16f4b586a6545d415c922` exists (46,354,636 bytes); this is CI evidence only, not an authorized final APK publication.
- Source access audit at this HEAD:
  - `VyomFactoryEvidencePublisher` is the repository content writer for screenshot evidence; its callsite derives repo from the owner-approved `factory-evidence` binding, requires the `.factory-evidence/` root, checks expected branch HEAD, forbids force updates, verifies the uploaded bytes/hash before issue comment, and records an upload receipt to prevent blind duplicate retries.
  - `SelfModificationClient` is the separate constrained source proposal writer. It requires the owner-approved `self-modification` binding, derives the API repository from that binding, requires policy + registry path authorization, pins the configured feature-branch HEAD to the immutable registered commit, rechecks before PR creation, and remains PR-only/no auto-merge. Its Contents API PUT writes only to a generated proposal branch; the PR base remains the configured feature branch. It is not a generic worker writer.
  - `VyomFactoryGitHubClient` writes GitHub Issues task metadata/comments only after resolving `factory-task-engine/issues` from the registry; the repository path is derived from that binding.
  - `VyomFactoryRepositoryClient.readBoundSystemFile` is registry-bound and commit-pinned for per-system file reads. Its arbitrary-repository inspect/preview endpoints remain read-only intake and treat returned content as untrusted.
  - `VyomFactorySkillRepositoryClient` performs GET-only remote skill reads; imported content is stored as inert Markdown. `GitHubReleaseAssetClient` is GET-only for release assets. Registry/catalogue file writes are app-private local metadata, not GitHub repository writes.
  - No generic worker repository write adapter exists. Do not claim repository code execution or worker write isolation as implemented.
- No source change was made in this checkpoint; this entry records verified CI and audit evidence. The public-repository screenshot exposure caveat remains unresolved and requires owner authorization before deletion/history remediation.
- Item 4 remains **IN PROGRESS** pending a complete system registry inventory/owner-binding operational review and explicit closure audit. Item 5 has not started. No Android `main`, Render, backend, or production Supabase changes. No physical-device verification or final APK publication claimed.
