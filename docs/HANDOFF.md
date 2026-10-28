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
- Reported commit: `fabaeea1`.
- Reported CI run: `#96`, green; APK artifact produced.
- These baseline details were supplied by the project owner and must be re-verified against GitHub before relying on them as current.
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
  - [ ] User-request-only update and SHA-256/signature validation.
  - [ ] Self-changes create separate branch + PR and run CI.
  - [ ] No auto-merge, auto-install, or main writes.
  - [ ] Policy-critical files protected; changes logged and rollback available.

## Current status
- Working branch: `feature/u1-live-orbit-tasks`; `main`, Hermes/Render backend and production DB remain out of scope.
- Phase 5 implementation is committed on this branch. Close Phase 5 only after the latest commit has green `testDebugUnitTest` + `assembleDebug` and its APK artifact is present.
- Phase 6 has not started in this handoff revision.
- Phone tests are intentionally deferred until after Phase 5 and Phase 6, per the owner's latest instruction. Do not request or claim phone validation during those phases.
- Secrets: never include live credentials, tokens or secret values in this file.
