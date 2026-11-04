# VYOM ANDROID — ACTIVE HANDOFF

Last updated: 2026-11-04
Priority: **VYOM FACTORY FIRST**. Full previous ledger remains preserved in [HANDOFF_HISTORY.md](HANDOFF_HISTORY.md).

## Branch and guardrails

- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Latest branch HEAD before this handoff update: `284789f16619e935b11782c43487a8353a32f130` (retry due-time and no-replay tests). This handoff commit will advance HEAD; verify resulting exact SHA and CI before further implementation.
- Do not touch Android `main`, Render, Hermes backend, or production Supabase.
- Inspect this handoff and history before work; do not repeat completed implementation.
- No APK delivery until required Factory stages are complete and latest exact-HEAD CI is green.
- Report only verified tests, CI, artifacts, and physical-device results.

## Canonical Factory order

1. Persistent task store/queue — implemented and tested.
2. Worker claim + lease — device-local lease store, coordinator, fresh-snapshot foreground claim session implemented.
3. Heartbeat — pending; runner is synchronous foreground-only and has no heartbeat. Add heartbeat only after an actual bounded adapter runs long enough to need lease renewal.
4. Retry/backoff — bounded, idempotency-gated policy plus durable local schedule/attempt counter and deterministic bounded jitter implemented. Foreground runner records transient failures only for adapters explicitly declaring idempotency and clears schedule on success. No due-retry consumer/scheduler exists yet.
5. STOP/pause — journal STOP is fenced to exact lease + attempt; foreground runner exposes cooperative control, and adapters must poll it before each side effect. No production adapter yet; cancellation behavior verified only with fake test adapter.
6. Crash recovery — pending.
7. Bounded parallel workers — pending.
8. App-closed execution verification — pending physical Android testing.

## Current verified implementation

- GitHub Issues is the shared registry; Android SharedPreferences is a device-local inbox and lease store.
- Queue validation, priority ordering, dedupe, reconciliation and fail-closed corruption/commit handling exist.
- Multilingual intake is schema validation only, not semantic multilingual interpretation.
- Lease fencing is local to one app process/device; it is not distributed CAS.
- Claim session refreshes GitHub before each claim and reconciles only issues whose GitHub state and metadata status are both `open`.
- Factory dialog still exposes foreground CLAIM NEXT / RELEASE CLAIM only; execution runner is implemented as an orchestration component but is not wired into UI or background scheduling.
- Journal entries now persist the exact operation key. Runner replay checks are scoped to issue + operation; legacy entries with unknown operation identity block further automatic work on that issue and require manual review.
- `VyomFactoryExecutionRunner` performs one foreground claim, re-fetches the issue, verifies open status + source identity + valid execution envelope + registered adapter, begins a fenced journal attempt, checks lease/STOP before dispatch, records terminal result, and schedules transient retry only when the adapter declares idempotency.
- `VyomFactoryExecutionControl.shouldStop()` is passed into adapters; adapter contract requires bounded execution and cooperative checks before each side effect. No concrete adapters are registered in production, so current app still cannot autonomously execute Factory tasks.
- Retry schedule cleanup after terminal success is best-effort and reported separately; cleanup failure does not rewrite a successful side effect as FAILED.
- `VyomFactoryRetryPolicy` retries only known transient categories, only when the concrete adapter explicitly declares idempotency, caps at three failed attempts, uses 30s then 120s base backoff, and saturates timestamps safely.
- `VyomFactoryRetryScheduleStore` durably persists per-issue/per-operation failed-attempt counts and next-eligible timestamps, applies deterministic bounded jitter (0–25% of base delay), exposes due entries, and supports clearing on verified completion. It fails closed on malformed data/commit failure and caps at 500 entries. It does NOT run work: a future consumer must fetch fresh GitHub state, acquire a current lease, check STOP/permissions, and invoke an idempotent adapter before any side effect.
- No autonomous worker, WorkManager scheduler, or concrete Android/repository action adapter exists yet. The journal is persistence/state machinery only; it does not execute or cancel work.

## Execution routing work just added

Architecture: [VYOM_FACTORY_EXECUTION_ARCHITECTURE.md](VYOM_FACTORY_EXECUTION_ARCHITECTURE.md), commit `c82f95d6b9a0146a1ee63c9de7240abc8750d832`.

- `VyomFactoryExecutionEnvelope` parses a separate versioned `VYOM_FACTORY_EXECUTION` marker.
- Existing schema-1 task metadata, missing/malformed/duplicate/unknown/oversized execution envelopes, invalid operations, and missing arguments fail closed to `manual_review`.
- Explicit supported targets: `android_action` and `repository_change`; operation identifiers are bounded and arguments must be a bounded JSON object.
- Original request is carried separately and bounded. Parser does not execute anything.
- `VyomFactoryExecutionAdapterRegistry` routes only to one adapter registered for the exact target. Duplicate adapters are rejected; manual-review tasks never route; missing adapter returns Unsupported; adapter exceptions propagate and cannot be converted to success. Adapters now declare `isIdempotent` and receive cooperative `VyomFactoryExecutionControl`.
- There are no concrete adapters yet. A task must not be marked completed merely because it parsed or routed.

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
