# VYOM ANDROID — ACTIVE HANDOFF

Last updated: 2026-11-04
Priority: **VYOM FACTORY FIRST**. Full previous ledger remains preserved in [HANDOFF_HISTORY.md](HANDOFF_HISTORY.md).

## Branch and guardrails

- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Latest known HEAD: `2a49ae1197b0d54df87eca49f8962dca8d0e5e0e`
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
- No autonomous worker, WorkManager scheduler, or concrete Android/repository action adapter exists yet.

## Execution routing work just added

Architecture: [VYOM_FACTORY_EXECUTION_ARCHITECTURE.md](VYOM_FACTORY_EXECUTION_ARCHITECTURE.md), commit `c82f95d6b9a0146a1ee63c9de7240abc8750d832`.

- `VyomFactoryExecutionEnvelope` parses a separate versioned `VYOM_FACTORY_EXECUTION` marker.
- Existing schema-1 task metadata, missing/malformed/duplicate/unknown/oversized execution envelopes, invalid operations, and missing arguments fail closed to `manual_review`.
- Explicit supported targets: `android_action` and `repository_change`; operation identifiers are bounded and arguments must be a bounded JSON object.
- Original request is carried separately and bounded. Parser does not execute anything.
- `VyomFactoryExecutionAdapterRegistry` routes only to one adapter registered for the exact target. Duplicate adapters are rejected; manual-review tasks never route; missing adapter returns Unsupported; adapter exceptions propagate and cannot be converted to success.
- There are no concrete adapters yet. A task must not be marked completed merely because it parsed or routed.

## Recent commits and CI

- Exact HEAD `56efc35772dfbb538818559394f82003919d0285`: Debug workflow run `41787089042` SUCCESS; build/test, APK packaging and upload steps all SUCCESS.
- Artifact from that source-equivalent HEAD: `vyom-phase6-56efc35772dfbb538818559394f82003919d0285`, artifact ID `13302535151`, SHA-256 `0e335fcfaeac3b44b3ed241208a1cc991437f5cc961d924a5d768bd25d988d6b`. Not a final release.
- Architecture doc: `c82f95d6b9a0146a1ee63c9de7240abc8750d832`.
- Envelope contract tests: `c131b68c7ddca1b6d32746c58aaaef1496b9c106`.
- Envelope parser implementation: `919a13d1fea3e7661a8df13dc040b8e397f1dfcc`.
- Adapter routing tests: `f0b6f288dedcb25c143c8c9e9dd5f36c406dd9dc`, expectation correction `719d75b4f8a13be579120add51f5eafbbcde36d8`, method alignment `12d28eccc4496a6bfc396177a72d5259e39043c7`.
- Adapter registry implementation: `a3c1e0284aa519a98f6a3530d62dcad7e39d9c73`.
- Additional parser edge tests (duplicate markers, oversized arguments, target-specific field mismatch): `de9eb5fa64a12b4def4a58ef1c772909d9496fee`.
- Latest source/test commits have not yet had verified CI results. Do not claim tests pass.
- Earlier Full CI `41786551264` failed before compilation due Maven Central HTTP 403; this was dependency resolution infrastructure, not a demonstrated source/test failure.

## Immediate next actions

1. Verify current branch HEAD `2a49ae1197b0d54df87eca49f8962dca8d0e5e0e` and obtain exact-HEAD CI results.
2. Inspect CI logs; fix actual parser/registry compile or test failures.
3. Run exact-HEAD CI and fix any actual compile/test failures.
4. Add a durable execution journal and idempotency/result fencing before concrete adapters.
5. Implement one narrowly scoped concrete adapter only after task schemas and capability permissions are validated.
6. Add WorkManager only after a concrete bounded adapter exists; never claim 24/7 reliability.
7. Continue the canonical order, update this file after each batch, and physically verify app-closed behavior before release.

No backend, Render, production Supabase, or Android `main` changes. No physical Android app-closed test claimed.
