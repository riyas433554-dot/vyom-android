# VYOM ANDROID — ACTIVE HANDOFF

Last updated: 2026-11-04
Priority: **VYOM FACTORY FIRST**. Full previous ledger remains preserved in [HANDOFF_HISTORY.md](HANDOFF_HISTORY.md).

## Branch and guardrails

- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Latest known HEAD: `c82f95d6b9a0146a1ee63c9de7240abc8750d832`
- Do not touch Android `main`, Render, Hermes backend, or production Supabase.
- Inspect this handoff and history before work; do not repeat completed implementation.
- No APK delivery until required Factory stages are complete and latest exact-HEAD CI is green.
- Report only verified tests, CI, artifacts, and physical-device results.

## Canonical Factory order

1. Persistent task store/queue — implemented and tested.
2. Worker claim + lease — device-local lease store, coordinator, fresh-snapshot foreground claim session implemented.
3. Heartbeat — pending; never renew idle claims.
4. Retry/backoff — pending.
5. STOP/pause — pending for actual running work; current release button only releases an unstarted claim.
6. Crash recovery — pending.
7. Bounded parallel workers — pending.
8. App-closed execution verification — pending physical Android testing.

## Current verified implementation

- GitHub Issues is the shared registry; Android SharedPreferences is a device-local inbox and lease store.
- Queue validation, priority ordering, dedupe, reconciliation and fail-closed corruption/commit handling exist.
- Multilingual intake is schema validation only, not semantic multilingual interpretation.
- Lease fencing is local to one app process/device; it is not distributed CAS.
- Claim session refreshes GitHub before each claim and reconciles only issues whose GitHub state and metadata status are both `open`.
- Factory dialog exposes foreground CLAIM NEXT / RELEASE CLAIM only. It does not execute tasks.
- No autonomous worker, WorkManager scheduler, execution adapter, or code/action execution exists yet.
- Existing issue metadata is schema 1 and has no validated execution target. Existing free-form descriptions must not be executed as shell or arbitrary Android actions.

## Execution architecture decision

See [VYOM_FACTORY_EXECUTION_ARCHITECTURE.md](VYOM_FACTORY_EXECUTION_ARCHITECTURE.md), commit `c82f95d6b9a0146a1ee63c9de7240abc8750d832`.

- Use separate explicit `android_action` and `repository_change` adapters.
- Existing schema-1 and unknown/ambiguous task types default to `manual_review`; no silent inference or fallback.
- WorkManager is considered only after a concrete bounded adapter exists; it is not a 24/7 guarantee.
- Current local leases cannot safely coordinate multiple devices. Keep execution single-installation or use a remote atomic executor.
- Persist execution journal/results with fencing and idempotency before terminal status updates.
- STOP checked before each side effect; heartbeat only while active work makes progress.
- Start parallelism at one; physical app-closed verification remains mandatory.

## Recent commits and CI

- Foreground claim/session source HEAD: `5fa5745e218c4eecd69a790100828b3a26048ccd`.
- Handoff compaction and reference correction: `e40a9009d70be9a303bf3cd8bf5d0cbe9884861b`, `56efc35772dfbb538818559394f82003919d0285`.
- Exact HEAD `56efc35772dfbb538818559394f82003919d0285`: Debug workflow run `41787089042` SUCCESS; build/test, APK packaging and upload steps all SUCCESS.
- Artifact: `vyom-phase6-56efc35772dfbb538818559394f82003919d0285`, artifact ID `13302535151`, SHA-256 `0e335fcfaeac3b44b3ed241208a1cc991437f5cc961d924a5d768bd25d988d6b`.
- This verifies the debug workflow at that exact source-equivalent HEAD only. It does not mean Factory is complete or that the artifact is a final release.
- Earlier Full CI `41786551264` failed before compilation due Maven Central HTTP 403; do not treat that as a source/test failure.
- New architecture doc commit `c82f95d6b9a0146a1ee63c9de7240abc8750d832` needs exact-HEAD CI verification.

## Immediate next actions

1. Verify latest branch HEAD and CI for `c82f95d6b9a0146a1ee63c9de7240abc8750d832`.
2. Implement schema-versioned execution-envelope parser and tests (test-first). Schema-1/missing/unknown target must resolve to `manual_review`.
3. Implement adapter registry with strict routing and fail-closed tests.
4. Then durable execution journal/idempotency; only after a real adapter exists add WorkManager scheduling.
5. Continue canonical order, update this file after each batch, commit/push each batch, and inspect actual CI logs.
6. No APK until Factory acceptance stages and physical verification are complete.

No backend, Render, production Supabase, or Android `main` changes. No physical Android app-closed test claimed.
