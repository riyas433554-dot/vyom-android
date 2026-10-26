# VYOM + HERMES Roadmap

Last verified: 2026-10-26. Evidence-based status only; a source change is not complete until required tests and phone/CI evidence exist.

## Current resume point

- GitHub repository `riyas433554-dot/vyom-android` is PUBLIC (GitHub repo metadata verified in this session).
- Android debug workflow: `.github/workflows/vyom-android-debug.yml`; workflow run #4: https://github.com/riyas433554-dot/vyom-android/actions/runs/40274651955
- Run #4 was blocked before runner startup by GitHub account billing/spending restriction. Screenshot annotation explicitly says recent account payments failed or spending limit; no Android compile/test step ran and no artifact was produced. This is an account/Actions gate, not evidence of a source compile failure.
- Debug workflow is configured for `fresh-history/android-app-copy`, tests and assembles unsigned debug APK, and uploads a versioned artifact. Version configured in workflow: versionName 0.3.0, versionCode 3. Successful artifact, APK SHA-256 and manifest values remain UNVERIFIED.
- Release signing secrets are not configured; signed release remains blocked/skipped. Do not claim a release APK exists.
- Preserve existing Vyom pairing/chat. Do not call `/api/android/session/validate`; reconnect uses stored bearer and chat's normal 401 path. Logout remains local-only. Do not touch `~/hermes` Termux working tree.
- U1 PR #1: https://github.com/riyas433554-dot/vyom-android/pull/1 (draft; head `6bd274234374b2e564923b728cb7e6764692e812`). Orbit overlay exists, but `OrbitHomeView.kt` uses generic CHAT/APP/VIDEO/TASK/FILE labels and “Ready”/“Orbit item”; it is still decorative and not connected to live `vyom_app_tasks` data. No merge.
- Hermes PR #46: https://github.com/riyas433554-dot/hermes/pull/46 (draft; head `a030db3bddd0170004164a6a06873dbf6e2d6c9d`). User-provided Termux output: 53 tests, 53 pass, 0 fail. This is user-provided evidence, not an independently rerun result. Do not merge/deploy or apply migrations without explicit approval.
- Hermes repo stays PRIVATE. No production DB mutation, merge, deployment, or migration apply performed.

## Phase 6A — Hermes task execution + recurring tasks

- [x] Recovery logic: stale running tasks are recovered based on age/cutoff; expired tasks older than two hours fail with `error_code: expired`, younger stale tasks requeue.
- [x] Unit coverage for expired and requeued recovery paths.
- [x] PR #36 merged at `2ca446e15998b20393c2ee69bc9a0da1b2dea5d7` after three checks passed on head `b0ccafef0b9eae858551df2de89618be60d71fe9`.
- [ ] S6 remaining gates: tests and CI runner evidence; disposable database/RPC verification; crash recovery verification; generated-file result tested on the actual phone.
- [ ] PR #37 stays DRAFT until all S6 evidence is green. Do not mark complete from code-only evidence.
- [ ] Recurring task behavior: verify scheduling, duplicate prevention, retries/recovery and observed execution evidence before marking complete.
- [ ] PR #46 local executor: user-provided focused tests 53/53 pass; still review end-to-end integration and artifact/result delivery. No merge without approval.

## Phase 6B — Android multi-panel UI

- [ ] Preserve existing pairing and chat flows.
- [ ] Holo-style panels, cyan/blue accents, gold orb, compact task notifications.
- [ ] U1: replace decorative orbit cards with live task records/status and real actions; verify task schema/API contract first. Draft PR #1 stays unmerged until CI and functional evidence pass.
- [ ] U2: full-screen panels, separate PR/commit and verified build URL.
- [ ] U3: remaining panel interaction/polish, separate PR/commit and verified build URL.
- [ ] Phone test: panel navigation, rotation/touch, task status refresh, pairing/chat regression.

## Phase 7 — Settings, themes and orb

- [ ] Verify settings persist across app restart, including themes and orb choice.
- [ ] Confirm reset/default behavior and accessibility/contrast.

## Phase 8 — Voice

- [x] TTS implementation exists in current source.
- [x] Mute/voice-reply preference and mic/listening UI implementation exist in current source.
- [ ] Actual phone test: TTS output, mute, mic permission, foreground listening, cancellation/error paths and restart persistence.

## Phase 9 — Files, links and screenshots

- [ ] View/read generated artifacts in the app.
- [ ] Open the relevant panel from an artifact/link/result.
- [ ] Verify downloaded file integrity, empty/error states and actual phone behavior.

## Phase 10 — Android control

- [x] Permission/user-confirmation pattern and local audit log implementation exist in current source.
- [ ] Phone test all supported actions, denied permissions, unavailable settings screens and audit persistence.
- [ ] No silent device control; no action beyond granted/confirmed permission.

## Phase 11 — In-app self-update

- [x] Version/build info display exists in current source.
- [ ] Signed APK, signer certificate SHA-256 validation, version/manifest validation, rollback and update policy.
- [ ] End-to-end phone test with valid, invalid, older and interrupted downloads.
- [ ] Do not call release ready until successful signed build and verification evidence exist.

## Phase 12 — Final design polish + persona

- [x] Hinglish persona preference and prompt shaping exist in current source.
- [ ] Final holographic polish, layout/accessibility review and persona regression tests.
- [ ] Validate all settings and visuals on the actual phone.

## Build/release gate

1. Resolve GitHub Actions billing/spending restriction on the existing account, or choose an independently valid runner/build route; a new account is not guaranteed to solve the block.
2. Rerun debug workflow; record run URL, conclusion, test/build steps and artifact name/size.
3. Download artifact and verify APK SHA-256, package/versionName/versionCode; install and phone-test.
4. Only after a successful debug build, continue signed release checks. Signing secrets are not currently configured.
5. Keep PRs draft until their required evidence is green. No Hermes merge/deploy/production DB mutation/migration apply without explicit user approval.

## Resume order

1. Continue Android source/PR work without waiting for GitHub Actions; keep changes isolated from Hermes and preserve pairing/chat.
2. Finish U1 live task-data wiring only after verifying the real `vyom_app_tasks` contract and mapping it to available task API.
3. Implement U2 full-screen panels and U3 separately.
4. In parallel, close S6 gates on Hermes/PR #37, leaving PR #37 draft until all evidence is green.
5. Then phone-test phases 7–12 and close each only with concrete evidence.
