# VYOM ANDROID — ACTIVE HANDOFF

Last updated: 2026-11-04
Priority: **VYOM FACTORY FIRST**. Full previous ledger remains preserved in [HANDOFF_HISTORY.md](HANDOFF_HISTORY.md).

## Branch and guardrails

- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Latest source + handoff commit before this correction: `63e830a3de9742ebda0467f819772b7ec2ba74eb`. Activity-pause STOP invalidation is wired in `VyomFactoryDialog`; exact-head CI runs `41821666098` (Debug) and `41821666302` (Full CI) were still running at last poll. Reconfirm HEAD and CI before treating this entry as current.
- Do not touch Android `main`, Render, Hermes backend, or production Supabase.
- Inspect this handoff and history before work; do not repeat completed implementation.
- No APK delivery until required Factory stages are complete and latest exact-HEAD CI is green.
- Report only verified tests, CI, artifacts, and physical-device results.

## Canonical Factory order

1. Persistent task store/queue — implemented and tested.
2. Worker claim + lease — device-local lease store, coordinator, fresh-snapshot foreground claim session implemented.
3. Heartbeat — intentionally deferred: the only production adapter is a synchronous media-volume set with readback, too short to need lease renewal. Add heartbeat only when a real bounded long-running adapter exists.
4. Retry/backoff — bounded idempotency-gated policy, durable schedule/attempt counter and deterministic bounded jitter implemented. Foreground RUN NEXT consumes only due retries after fresh GitHub fetch, fresh lease, STOP checks and a new one-time UI approval. No automatic/background retry scheduler.
5. STOP/pause — partial: foreground STOP button cancels pending consent or sets a cooperative stop flag; runner/adapter check before side effects. Activity pause now invalidates the active run using the captured foreground-gate generation, so pause followed by resume cannot revive it. A late STOP cannot rewrite an already verified side effect as failure. Journal STOP remains lease+attempt fenced. Lifecycle instrumentation/physical validation remains pending.
6. Crash recovery — partial foreground recovery: runner marks expired/replaced nonterminal attempts INTERRUPTED before selecting the next task; never replays them. No background/startup scheduler.
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
