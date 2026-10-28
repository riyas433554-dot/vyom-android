# Vyom Android — Handoff

Last updated: 2026-10-28

## Repository and safety boundaries
- Repository: `riyas433554-dot/vyom-android`.
- Working branch: `feature/u1-live-orbit-tasks`.
- Do not modify or merge into `main`.
- Do not touch the Hermes/Render backend or production database.
- API/provider keys belong only in device settings; never commit keys, tokens, or secret values.
- Do not redesign the UI during implementation. Visual polish is explicitly deferred.
- After every commit, verify the GitHub Actions CI result. Never claim a test passed without evidence.
- Do not call the app “ready” until the user has completed and passed the required phone tests.
- Final report must include a Code / CI / Phone test table. Clearly label anything not performed as pending/unverified.

## Latest reported baseline
- Latest verified branch head: `c3625015300f7a2be4474f43168cfcb9981744bc`.
- Phase 6 CI run `40677905303` is green: secret scan, `testDebugUnitTest assembleDebug`, packaging and artifact upload passed.
- Debug APK run `40677904789` is green; artifact `vyom-0.3.0-c3625015300f7a2be4474f43168cfcb9981744bc`; versionName `0.3.0`, versionCode `3`; APK SHA-256 `fa8d10ee524dbde2d0ca915edfcebd20a8393af4c6a1d9a0fb209f4832906646`.
- Phase 6 CI artifact `vyom-phase6-c3625015300f7a2be4474f43168cfcb9981744bc`; artifact ZIP SHA-256 `7fc7f2e955a4c2ddffe0b548f30eb74fd47d2038ef8d69a62399d8facce50213`.
- Phone testing remains pending; green CI does not prove device behavior.
- Phone tests remain pending: live listening, mute/unmute sounds, notification Stop, barge-in, and queued chat messages.

## Three model lanes
- LIVE: default `nvidia/nemotron-3.5-lightning-30b-a3b`; if the provider returns HTTP 404, fall back to `nemotron-3.5-bolt-e4b`. LIVE is streaming and must not use the task queue.
- CHAT: Gemma; queued chat messages must remain independently tracked and reliable.
- TASK: `nemotron-3-ultra-550b-a55b`, serial execution.
- Voice pipeline: AudioRecord + VAD (~700 ms), ElevenLabs STT/TTS, Vosk fallback.
- Do not change voice behavior or design while doing documentation or Phase 5/6 work unless a specific requirement below demands it.

## Roadmap / gates
1. Phase 5 — Android Control implementation.
2. Phase 6 — Self-update and constrained self-modification.
3. Phone tests only after Phase 5 and Phase 6 are implemented; the owner explicitly deferred device testing until then.
4. Phase 7, then phone test.
5. Voice latency polish.
6. Design polish last.

## Phase 5 — Android Control
Implement one Action Engine with voice, chat, and task entry points.
- Classify intent with deterministic/rule-based logic first; use an LLM only as fallback.
- Ordinary conversation bypasses the action engine and goes through LIVE.
- Start action execution and spoken acknowledgement in parallel where safe.
- SAFE: open apps; set volume/brightness; torch; alarms/timers; open system settings panels.
- CONFIRM: calls, SMS, sending anything, delete, install. Require an explicit on-screen tap; voice-only confirmation is not sufficient.
- BLOCKED: banking/payment apps; reading passwords or OTPs; uninstall; security settings. Never execute these.
- Kill switch: STOP CONTROL in the app and notification; it must stop control actions promptly.
- Add a Profile action log recording what happened, when, and whether it came from voice/chat/task.
- Accessibility Service must be explicitly enabled by the user.
- Wi-Fi/Bluetooth: open the relevant system settings panel; do not toggle directly.
- The kill switch, blocked list, confirmation rules, and audit logging are protected policy and must not be changeable by Vyom.

## Phase 6 — Self-update and self-modification
- Self-update only when explicitly requested by the user; verify the existing SHA-256 and signature before accepting an update.
- Self-modification may create a proposal on a separate branch and open a PR only. CI must build/test it; the user reviews and installs manually.
- Never auto-merge, auto-install, or write to `main`.
- Protect policy-critical files with CODEOWNERS/branch protections where available; Vyom must not be able to modify the kill switch, BLOCKED list, confirmation rules, or logging policy.
- Log every self-change and retain a rollback path to the previous APK.

## Verification and release rules
- Keep changes scoped to this Android repository and requested branch.
- After each commit: inspect the exact commit SHA, CI run/status, job results, and APK artifact when relevant.
- Do not treat CI green as equivalent to device validation.
- Phone-test checklist:
  - [ ] Live listening works.
  - [ ] Mute/unmute sound behavior works.
  - [ ] Notification Stop halts control/listening as specified.
  - [ ] Barge-in interrupts speech correctly.
  - [ ] Multiple queued chat messages complete independently and in order.
- Phase 5 safety checklist:
  - [x] Rule-based intent classification precedes LLM fallback; normalized model output is reclassified and checked against the original intent.
  - [x] SAFE actions include app launch, media volume, brightness, torch, alarms/timers and settings panels.
  - [x] CONFIRM actions require an explicit screen tap; calls open the dialer, SMS opens a composer, sharing opens a chooser, file deletion uses SAF plus a second confirmation, and APK install still requires Android's installer confirmation.
  - [x] BLOCKED sensitive actions are rejected at voice, chat and task entry points.
  - [x] STOP CONTROL is persistent and available in the app and voice notification.
  - [x] Profile action log records timestamp, action, result and source lane.
  - [x] Accessibility service is opt-in/manual, does not read window content, and Wi-Fi/Bluetooth commands open settings panels only.
- Phase 6 checklist:
  - [x] Self-update is user-triggered only; latest-release manifest URL is allowlisted to this repository.
  - [x] Update APK must be newer, within size limits, SHA-256 match, correct package/version, and match the installed signing certificate before Android's installer opens.
  - [x] Previous installed APK is copied to Downloads/VyomRollback on Android 10+, size- and SHA-256-verified, with a user-triggered rollback installer path. Android may require manual uninstall to downgrade; Vyom never uninstalls itself.
  - [x] Self-change requires an encrypted on-device GitHub token, validates a bounded title/path/content, creates a separate proposal branch, updates only an allowed Kotlin source file, and opens a PR against this working branch.
  - [x] No auto-merge, auto-install, release publication, or writes to main.
  - [x] Self-change success/failure and update/rollback installer events are written to the local action audit log.
  - [x] CODEOWNERS identifies the repository owner for review; CI rejects policy-critical or non-Kotlin paths in `vyom-proposal-*` PRs.
  - [x] CI workflow runs unit tests, assembles a debug APK, performs secret scanning, and uploads APK + SHA-256 artifact on branch pushes and proposal PRs.

## Current status
- Working branch: `feature/u1-live-orbit-tasks`; `main`, Hermes/Render backend and production DB remain out of scope.
- Phase 5 implementation is committed on this branch. Close Phase 5 only after the latest commit has green `testDebugUnitTest` + `assembleDebug` and its APK artifact is present.
- Phase 6 implementation is committed on this branch and final CI verification is green (run `40677905303`, commit `c3625015300f7a2be4474f43168cfcb9981744bc`). Phone and real release-update/rollback tests remain separate gates.
- Phone tests are intentionally deferred until after Phase 5 and Phase 6, per the owner's latest instruction. No phone validation has been performed; do not claim the app is ready until the owner completes the checklist.
- Secrets: never include live credentials, tokens or secret values in this file.
