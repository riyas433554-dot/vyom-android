# VYOM ANDROID — ACTIVE HANDOFF

Last updated: 2026-11-04
Priority: **VYOM FACTORY FIRST**. Full previous ledger remains preserved in [HANDOFF_HISTORY.md](HANDOFF_HISTORY.md).

## Branch and guardrails

- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Latest source checkpoint before this handoff refresh: `b24063de806f451e993f29d96a8c634a8c806a1e`. Exact branch-tip CI must be refreshed after this documentation commit. Earlier successful artifacts listed below are historical only, not current source proof.
- Do not touch Android `main`, Render, Hermes backend, or production Supabase.
- Inspect this handoff and history before work; do not repeat completed implementation.
- APK 1 gate follows the current Factory-first plan below: Stage 1 end-to-end behavior plus only the A18/A17 safety-critical pieces Factory itself needs, exact-head CI green and artifact integrity. Do not require the entire Android-control list, Play Store/WhatsApp feature implementation, or unrelated lifecycle stages 5/6/8.
- Report only verified tests, CI, artifacts, and physical-device results.

## Current controlling plan — Factory-first (2026-11-04)

This section supersedes older APK gates and Factory ordering text below where they conflict with the user's latest instruction.

- Make **Vyom Factory itself** reliable before implementing the full Android-control feature list.
- Stage 1 complaint intake, triage, canonical register, priority ordering, fix routing, evidence, and verification lifecycle is the primary deliverable.
- Implement only the A18 signed no-APK update/probe/rollback and A17 execution/verification capabilities that Factory itself needs. Do not complete every A17 Android operation merely to satisfy an old milestone list.
- Play Store installation and WhatsApp messaging remain Factory-managed complaints for later fixing. Do not implement those product features directly in this Factory-first batch.
- APK 1 may be delivered when Stage 1 works end-to-end and the minimum A18/A17 safety-critical pieces required by Factory are verified, with exact-HEAD CI green and artifact integrity checked. Do not block on unrelated full-control operations or Factory lifecycle stages 5/6/8.
- Keep work only on `feature/memory-intelligence`. Never touch Android `main`, Render/Hermes backend, or production Supabase.

### Latest source audit and CI (2026-11-04)

- Latest source branch tip before this handoff refresh: `b24063de806f451e993f29d96a8c634a8c806a1e`. `docs/OWNER_ACTIONS.md` now includes guarded complaint-to-code proposal acceptance steps. Proposal pipeline failures/timeouts are also recorded back to the originating complaint (without exposing exception message/request text); cancellation/back-dismissal clears the pending issue link.
- Complaint triage: generic `behavior` no longer implies personality; explicit tone/persona terms still do. Play Store installation and WhatsApp message complaints classify as `control / P0`; Hinglish failure phrases (`nahi ho raha`, `send nahi`, `install nahi`, etc.) raise severity to high. Regression/fix commits: `520e4fce532923d9040e439229eb704146431574`, `725edddf4bc6f8d5a2ec46e775d7779363ac40fa`, `1eb496089c2e0b1f34b6af59dacf9c79a9011088`, `0fdebdd9f31d80a8f90e53b1de1a4c4c2ab1efec`, `c5a818a311ad7810fec626e4c776e149fcb881a4`, `b050bcdbb590641a055f045a9ffab367b8c7dbbb`.
- Priority order is shared by Factory UI, Automations and worker selection: P0→P3, newest GitHub update first within each tier, issue number as deterministic tie-break. `ye complaint pehle #N` promotes only active open/in-progress complaints to P0.
- Complaint lifecycle: open issues can START, paused issues can RESUME, and DONE is blocked until a named Verification probe has a recorded PASS for that exact probe. VERIFY records owner-reported device evidence (PASS/FAIL); changing the probe invalidates old PASS. This is manual owner attestation, not autonomous device verification. Existing legacy completed issues remain editable without requiring a newly created probe event.
- Worker state mismatch fixed: START changes metadata to `in_progress`; worker session/coordinator/runner/Factory eligibility accept GitHub-open issues with metadata status `open` or `in_progress`. Regression tests cover session claim, coordinator claim and runner execution. Commits: `d05a800da6c0307a9ce29a75a9019d63874f0223`, `957c0f38742d476118c20d7fc738c5b074a0234f`, `db45a8b3e9c864487d2ef80cfdce3629c35dbde6`, `ed93a42c699bd12f7eeecf0277141aeb18be4de8`, `e5d172090b1cc65a8bbc40a36de2dbbd2e31f4b4`, `c1723c68af2ddf5211d5c0ddf53d3c7c7d747531`, `22c19b21a880978b3127422f185e782d9438af06`.
- Probe closure policy is tested for exact probe identity, failed probe, unassigned probe, changed probe and legacy completion. Owner promotion rejects closed/cancelled complaints. Commits: `839ca96886fc7cbf91bbe79f05a66fb2c1f80eda`, `7e961916328979a7ea39b5b9faa4d4ee9fb2a430`, `cbb38396ce3731dc8cda6ea033d6ba4a171eb616`, `0a15c1b589ebea80076044fe00f6dc6c983a3951`.
- Worker outcomes write truthful comments to the canonical issue: verified adapter postcondition while leaving complaint open, or FAILED with category and no success claim. Skipped/fenced outcomes do not write misleading comments. Commits: `c0e1129273c9379b6edb9e205cc7f6895f5de33b`, `c9f33b987325a269e9fb6985e83b1dff4fb1ce6c`, `12636067a40120714f40fe609e032fd6d681c711`, `e7f61354f97b68ffc27479844ce5d9f138dd39d1`, `b258df13a1a8c61a545b564aa18993b0bcdabf93`.
- **Important Factory fix path now wired in source:** active complaint cards expose `PROPOSE REVIEWED CODE FIX`; issue title/body and issue number are included in a bounded proposal prompt after removing hidden metadata; sensitive title/body is rejected; user must choose an allowed Kotlin source path; existing policy rejects protected policy/control/update/signing/accessibility files; the model produces exact minimal edits; user sees the diff before PR creation; PR targets only `feature/memory-intelligence`; both required CI workflows are awaited; PR URL and exact CI SHA are written back to the originating issue where GitHub comment permission allows. Regression tests cover issue identity, metadata omission and sensitive title/body rejection.
- This is **not autonomous self-repair yet**: one Kotlin file per proposal, user chooses the file path, model-generated changes need owner diff review and PR review/merge; CI success on an unmerged PR does not produce an installable release. No auto-merge or auto-install. Proposal is deliberately not allowed to modify MainActivity, ActionEngine, accessibility, policy, STOP, audit, signing or update-security files.
- Stage 1 source-wired intake: GitHub Issues canonical register with schema/idempotency; chat and voice complaint prefixes; sanitized/deduplicated Diagnostics failure intake; privacy-reduced chat task-failure complaint; Termux command with shell/schema tests; Automations shared issue visibility; seeded requirement matrix.
- Stage 1 remains **incomplete**: Termux install/auth/idempotency smoke test and physical phone acceptance are owner-side; screenshot share-sheet receiver is not implemented because repo is public and safe evidence storage is unresolved; automatic new-complaint capture is proven only for chat failures, while Factory worker failures are comments on the existing issue; GPT/Claude/Codex source acceptance is not device-verified. Public issue bodies are visible to everyone; evidence must be sanitized.
- Complaint intake starts with UNASSIGNED probe. Owner must edit it to one exact observable probe, START the complaint, check the real phone, then record sanitized PASS/FAIL evidence. This is manual owner attestation, not autonomous device verification.
- A17 has a versioned safe execution envelope, lease/journal/retry/STOP/foreground-consent controls, one process-wide runner, and one concrete `set_media_volume` adapter. General Android actions and autonomous repository fixes are not yet implemented.
- A18 has signed package verification against the installed APK signing certificate, file hashes/path allowlist, inactive-slot staging, presence probes, atomic activation only after probes pass, rollback on failed probe, WebView presence-failure rollback, and crash-triggered rollback. CI signs the package with the persistent Android release keystore alias and self-verifies the detached signature. Optional PERFORMANCE probe measures content-file read time, not actual WebView render latency.
- CI: last observed Full CI run `41838529328` was pending on ancestor SHA `a81648d72d03107d0cae2ef870f5d529539112d3` and had no jobs yet; ancestor Debug `41838467957` was queued on `d68ab40279151eee3a28d0cf963dbd0dc5d6db6f`. Newer source commits supersede both. Exact-head Debug + Full CI for this checkpoint and the following handoff commit must be checked. Do not claim tests passed or APK built until both finish successfully.
- No final APK has been delivered and no physical phone acceptance is claimed.

### Next work (mandatory order)

1. Poll exact-head Debug + Full CI; fix compile/test failures from the actual logs. Do not let a new commit cancel in-flight checks unnecessarily.
2. Add/verify an end-to-end Factory test: START → worker claim/run → issue event/result → owner probe PASS → DONE; verify Factory/Automations status and priority parity.
3. Finish remaining Stage 1 intake gaps where source-safe: generic non-chat worker failure routing and external-agent source identity; keep screenshot receiver blocked until private evidence storage is approved.
4. Complete Factory proposal path tests: safe-path rejection, PR creation failure handling, CI failure/timeout handling, issue comment failure, and prove no merge/install occurs for unmerged PRs.
5. Verify A18 signed demo/test artifacts on exact current head only where Factory needs the no-APK rollback safety. Do not expand into unrelated A17/A18 work.
6. Owner acceptance remains required for Termux smoke test, physical phone, repository visibility/privacy decision, and private screenshot evidence storage. Update this handoff after each meaningful batch.
7. Deliver APK 1 only after the Factory-first source gate, exact-head Debug + Full CI, and required artifact checks pass. Do not deliver a final APK while checks are pending.

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

- Exact-head Debug CI 41843876699 on parent SHA 9a164bf3b460ef7e149dacf4a0bc2afcdd9e114 completed FAILURE. The log shows :app:compileDebugKotlin completed, but :app:compileDebugUnitTestKotlin failed at VyomFactoryLifecycleIntegrationTest.kt lines 33, 107, and 121: JSONObject was passed where the helper requires a String.
- Corrected all three MockWebServer response calls to serialize the JSONObject with .toString(). Commit: bbae296400390ef7e149dacf4a0bc2afcdd9e114.
- This is a test-source compile correction only; no app behavior changed. The corrected test has not yet been executed by CI.
- Parent Full CI 41843876818 was still IN_PROGRESS on the old parent SHA when this correction was pushed; it does not validate the corrected commit. Poll its final result separately.
- Next: wait for exact-head Debug and Full CI for the correction commit; inspect real logs. Do not call APK ready until both exact-head workflows succeed and the expected artifact is present and verified.
- Separate audit note: automatic complaint registration returns early with error when the GitHub token is missing, while reconnect retries only queued items. This token-recovery retry gap is confirmed by source inspection but is not yet changed; add a regression test before modifying it.
