# Vyom Factory — Foreground Claim Session (2026-11-04)

Branch: `feature/memory-intelligence`

Implementation:
- `VyomFactoryWorkerSession` fetches a fresh GitHub Issues snapshot before every claim.
- Only tasks with both GitHub issue state and Factory metadata status `open` are reconciled into the local inbox.
- It delegates claim/lease acquisition to `VyomFactoryWorkerClaimCoordinator`.
- Registry-read failures propagate; the session does not claim from stale cached remote data.
- Factory dialog exposes `CLAIM NEXT` and `RELEASE CLAIM`.
- A per-installation UUID is persisted to allow same-device claim recovery while the lease remains live.
- Release is token-fenced and leaves the task in the queue.
- Lease default is 60 seconds. There is no heartbeat.

Tests:
- Highest-priority eligible task is imported and claimed.
- Paused metadata and closed issues are excluded.
- Registry-read failure leaves the inbox unchanged.
- Releasing a claim permits a second owner to claim using the same queue and lease store.

Commits:
- `75657f0e86a1b340c4df18acc27cc7017591de8c` — test-first session contract.
- `2e324750b0b8fcd6b04fc01319c6f86b380a1abc` — corrected test fixture.
- `cb8b9275bd0380286dc7957c1f5ddd9426e0ecec` — session implementation.
- `87246f47bf8fed57fad37ebf448747f1c20c209d` — foreground UI claim/release controls.
- `5fa5745e218c4eecd69a790100828b3a26048ccd` — shared lease store across owners in release test.

Verification:
- Previous stable HEAD `fcc5903b1f719989895f570171763ff1e5f37b54`: Debug `41785656632` SUCCESS; Full CI `41785656571` SUCCESS.
- Session/UI HEAD `5fa5745e218c4eecd69a790100828b3a26048ccd`: Debug `41786551316` was still running at last check. Full CI `41786551264` failed during Gradle dependency resolution before compilation because Maven Central returned HTTP 403 for multiple transitive dependencies. This is an infrastructure/dependency-fetch failure, not evidence of a source/test failure. Retry on network recovery.

Critical limitations:
- Foreground/manual claim only. This is not a task executor, coding agent, or autonomous worker.
- No GitHub `in_progress` mutation, code execution, repository edits, WorkManager, heartbeat, retry/backoff, STOP/pause execution, crash recovery, bounded parallelism, or app-closed execution.
- Device-local SharedPreferences lease is not a distributed claim across devices, Termux, ChatGPT, or GitHub Actions.
- Do not deliver APK or call Factory execution-ready until the remaining worker stages and exact-head CI are complete.

Next: inspect exact-head CI, fix real failures, and define a real task execution adapter before heartbeat. Never renew an idle claim.
