# VYOM ANDROID — ACTIVE HANDOFF

Last updated: 2026-11-04
Priority override: **VYOM FACTORY FIRST**. This file is the active, concise handoff. The previous detailed ledger is preserved verbatim in [HANDOFF_HISTORY.md](HANDOFF_HISTORY.md).

## Working branch and guardrails

- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Source HEAD before the handoff-only commits: `5fa5745e218c4eecd69a790100828b3a26048ccd`
- Handoff addendum commit: `7c72301181742de566931923361c64322d7757b8`
- Handoff compaction commit: `e40a9009d70be9a303bf3cd8bf5d0cbe9884861b`
- Do not touch Android `main`, Render, Hermes backend, or production Supabase.
- Never repeat completed work; inspect this file and history before changing code.
- Do not deliver an APK until the required Factory stages are complete and latest exact-HEAD CI is green.
- Report only verified tests, CI, artifacts, and device results.

## Canonical Factory worker order

1. Persistent task store/queue — implemented and tested.
2. Worker claim + lease — device-local lease store and coordinator implemented; foreground claim consumer now wired.
3. Heartbeat — not started; do not renew idle claims.
4. Retry/backoff — pending.
5. STOP/pause — pending for real execution; UI release only releases an unstarted claim.
6. Crash recovery — pending.
7. Bounded parallel workers — pending.
8. App-closed execution verification — pending physical Android testing.

## Current implementation

- GitHub Issues is the shared task registry; Android SharedPreferences is only a device-local inbox and lease store.
- Queue supports persistence, validation, priority ordering, dedupe, reconciliation and fail-closed corruption/commit handling.
- Multilingual intake draft is schema validation only, not a semantic interpreter. Plain chat responses are not a structured task parser.
- `VyomFactoryWorkerLeaseStore` provides issue-scoped owner/token/expiry leases with fencing. It is single-device/single-app-process only, not distributed CAS.
- `VyomFactoryWorkerClaimCoordinator` selects the highest-priority queued task only against a fresh open GitHub snapshot with matching source identity.
- `VyomFactoryWorkerSession` fetches a fresh registry snapshot before each claim, reconciles only issues where both GitHub state and metadata status are `open`, then calls the coordinator. Registry-read failures propagate.
- Factory dialog now has foreground `CLAIM NEXT` and `RELEASE CLAIM` controls. A per-installation owner UUID is persisted. Claims default to 60 seconds; no heartbeat exists. The UI explicitly says the task is not executing.
- **There is still no task executor or autonomous worker.** No task instructions are executed, no repository files are changed by the app, no GitHub `in_progress` mutation is made, and no WorkManager/background worker exists.

## Latest work and commits

- Session tests: `75657f0e86a1b340c4df18acc27cc7017591de8c`
- Test fixture correction: `2e324750b0b8fcd6b04fc01319c6f86b380a1abc`
- Session implementation: `cb8b9275bd0380286dc7957c1f5ddd9426e0ecec`
- Foreground claim/release UI: `87246f47bf8fed57fad37ebf448747f1c20c209d`
- Shared-lease test correction: `5fa5745e218c4eecd69a790100828b3a26048ccd`
- This handoff addendum: `7c72301181742de566931923361c64322d7757b8`

## CI status — do not call green yet

- Previous stable HEAD `fcc5903b1f719989895f570171763ff1e5f37b54`: Debug `41785656632` SUCCESS; Full CI `41785656571` SUCCESS. This does not verify the new session/UI.
- Session/UI HEAD `5fa5745e218c4eecd69a790100828b3a26048ccd`: Full CI `41786551264` FAILED before compilation because Maven Central returned HTTP 403 for multiple transitive dependencies. Debug `41786551316` was still running at last poll.
- Current handoff HEAD `7c72301181742de566931923361c64322d7757b8`: Debug `41786900393` and Full CI `41786900383` were in progress at last poll. Verify both exact-SHA runs and inspect real logs. If dependency fetch again fails with 403, retry after repository access recovers; do not mislabel it a code failure.

## Next actions

1. Poll Debug `41786900393` and Full CI `41786900383` for exact HEAD `7c72301181742de566931923361c64322d7757b8`.
2. Inspect actual Gradle logs. Fix source/test failures; treat Maven 403 as dependency infrastructure failure and retry if possible.
3. After green, design and implement a real task execution adapter. Do not add heartbeat until the executor actually owns and runs work.
4. Continue in the canonical order above, update this file after each batch, commit/push each batch, and verify latest exact HEAD CI.
5. No APK until all acceptance stages are complete and physically testable.

No APK delivered. No Android `main`, Render, Hermes backend, or production Supabase changes.
