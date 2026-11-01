# Vyom Android — Handoff

Last updated: 2026-11-01

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
- Latest verified branch head: `4ee5c9669a6be06666f1d958e2f898b52a616bee`.
- Final CI run `40679364514` and debug APK run `40679364326` both completed successfully on that exact SHA.
- CI tasks: `testDebugUnitTest assembleDebug`; secret scan, packaging and artifact upload passed.
- Debug APK artifact: `vyom-0.3.0-4ee5c9669a6be06666f1d958e2f898b52a616bee`; versionName `0.3.0`, versionCode `3`; APK SHA-256 `c201c98359b44070cd6b35cd50cd56770dfb72392c9f84b8fcaf2fee3ec474db`.
- Phase 6 CI artifact: `vyom-phase6-4ee5c9669a6be06666f1d958e2f898b52a616bee`; artifact ZIP SHA-256 `ecd0573f543d0b64ab7d60c30a6c828ae2e7823dbabd7f1ab829451577255114`.
- Proposal guard smoke run `40678366127` passed on a temporary allowed-path PR; temporary PR #6 was closed without merging.
- Phone testing and published-release update/rollback tests remain pending; green CI does not prove device behavior.
- Do not change `main`, backend/Render, or production DB for this Android-only task.


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
- Phase 5 implementation and CI gate are verified on implementation commit `cb600d687ef0325706ffe34d24c5e78a8d26100f`; `testDebugUnitTest`, `assembleDebug`, secret scan and APK artifact upload passed. Phone tests remain pending.
- Phase 6 implementation and final CI verification are green on implementation commit `cb600d687ef0325706ffe34d24c5e78a8d26100f` (run `40678795668`). Phone and real release-update/rollback tests remain separate gates.
- Phone tests are intentionally deferred until after Phase 5 and Phase 6, per the owner's latest instruction. No phone validation has been performed; do not claim the app is ready until the owner completes the checklist.
- Secrets: never include live credentials, tokens or secret values in this file.

## 2026-10-29 follow-up implementation notes
- Branch-only commit `332dee7d6486ddd12466cbb199585f4507bd0f7b` added Profile > Skills toggles for Open apps, Media volume, Torch, and System settings, plus a proposal-only Fix suggestions view based on local audit events.
- CI run [40810090398](https://github.com/riyas433554-dot/vyom-android/actions/runs/40810090398) failed: `ActionEngineTest.hinglishAndTypoTolerantControls`, line 20 (Hinglish mute classification). This was followed by a source fix commit `000a3fad786ffb293f09e8d1a09e1d8726561ab6`; a green CI run on that SHA must be verified before distributing an APK.
- The current Skills implementation is local preferences-backed and the vocabulary source still includes an asset snapshot. Do not describe editable on-device vocabulary JSON as complete until it is implemented and tested.
- Release signing workflow expects GitHub Actions secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Secret presence and certificate continuity are not verified by source inspection alone. Never add the keystore or values to git.
- Phone tests, installed-app update/rollback tests, signature continuity, and runtime checks remain pending until recorded against a build with green CI.

## 2026-10-29 — Batch A–E implementation update

This section supersedes older "current status" notes above where they conflict. Working branch only: `feature/u1-live-orbit-tasks`. No changes to `main`, Render/Hermes backend, or production DB were made. No keys or signing material were committed.

### Implemented in source (CI and phone gates remain separate)
- A1: Chat renders the latest 50 messages by default, jumps to the newest message on opening Chat, exposes a Jump to Latest control, and loads older messages when scrolling to the top.
- A2: Recent conversation context (up to 20 messages) and safe extracted facts are embedded in chat and LIVE prompts. Profile > Memory supports view/edit/delete and enable/disable. Facts use AndroidX encrypted preferences; password, OTP, card, API-key, token, and private-key patterns are rejected from fact storage.
- A3: Legacy live ring/dot/speaker controls were replaced by a large transparent orb, listening/thinking/speaking/muted state label, captions toggle (off by default), menu, Mute, and End. Orb particle size responds to measured microphone level. Task scroll containers allow content to expand without clipping. Shared theme tokens cover primary surfaces, text, spacing, and radii; this is a partial tokenization, not a full app-wide token migration.
- A4 / Files: Saved task links and screenshot links are listed in Files and open through the existing HTTPS viewer flow. The saved-task loading method was renamed to reflect reconnecting to real saved tasks. Final-head source scan found no matching TODO/placeholder/stub/not-implemented/will-be-added/Phase terms in app source. Historical roadmap Phase headings remain documentation, not UI placeholders.
- B1: 16 kHz mono capture now buffers about 300 ms of pre-roll, performs about 600 ms startup noise calibration, uses an adaptive RMS threshold, and enables Android AEC, NoiseSuppressor, and AGC when supported. Profile shows a microphone level meter; a 300 ms post-TTS start hold-off is in place. Device-level audio behavior remains unverified.
- B2 / B4: Live orb and caption controls are implemented. Chat voice replies are suppressed while the live voice loop owns audio; the live loop still speaks its own replies/acknowledgements.
- C1: The opt-in Accessibility Service can describe the visible node tree with password fields redacted, click visible non-editable text targets, long-press, swipe, scroll, type only into a focused non-password field, perform Back/Home/Recents/Notifications global actions, and save a local screenshot on API 30+. Android secure-window screenshot refusal is respected. These methods are not yet connected to an autonomous task planner.
- Model lane configuration: LIVE default remains `nvidia/nemotron-3.5-lightning-30b-a3b` with the existing 404 fallback `nemotron-3.5-bolt-e4b`; task planning is intended to use `nvidia/nemotron-3-ultra-550b-a55b`; chat remains Gemma via Hermes. No backend/model configuration was changed.

### Explicitly blocked / incomplete
- B3: Profile diagnostics currently reports capture configuration and honestly labels stage samples unverified. Persistent WebSocket reuse, a verified STT latency comparison, true first-audio streaming metrics, and a measured sub-1.5-second end-to-end result are not complete.
- C2–C4: The observe-plan-act-verify loop, bounded planner integration, full action audit for every accessibility step, floating overlay orb/drag/snap/caption bubble, foreground control session, per-app deny list, cloud screenshot redaction/notice, and complete SAFE/CONFIRM/BLOCKED session-grant enforcement are not complete. Do not use autonomous screen control until these gates exist.
- D1–D3: Existing Skills toggles and Teach Vyom local phrase mapping remain available. Saving successful multi-step actions as parameterized macros, automatic 2–3 strategy attempts, and an Ultra-generated Kotlin fix draft/PR flow are not complete.
- D4: Stable signing and update-over-existing-app continuity are BLOCKED until the existing signing secrets/certificate can be verified without exposing them. Never generate a replacement key or commit signing material.
- D5: Always ask / Risky asks / Session grants selector is not complete.
- E1: Shared theme tokens are partial; all colors/glows/radii/spacing and gradient-pill styling have not been migrated across the whole app.

### Verification gates
- Code/CI: only the exact latest green commit and its artifact count. Check the final GitHub Actions run for both `testDebugUnitTest` and `assembleDebug`, secret scan, packaging, and artifact upload.
- Phone test: NOT RUN. The owner must test installation, pairing/chat, scroll/history, memory, captions/mute/end, mic behavior, Accessibility opt-in, safety denials, and update/rollback. Do not call this app ready before phone tests pass.
- Debug artifact: unsigned. It is not evidence of signing-certificate continuity and may not install as an update over an existing signed app.


## 2026-10-29 — Supplied Design System v2 integration
- Source reference: user-provided `Vyom — Design System v2 + screens.html`. Its design-system page specifies #070B14 background, #0F1A2E glass panels, #E6F1FF primary text, #8FA3BF secondary text, cyan/violet/amber/green/danger semantic accents, 18px glass panels, pill controls, and an accent-following orbit.
- Implemented in `VyomTheme.kt`: shared v2 palette, glass/panel backgrounds, radii/spacing/type tokens, gradient primary pills, outlined secondary pills and danger pills.
- Updated home background/panel/dock, prompt field, suggestion chips, quick-access cards, app status/navigation accents, native buttons and text fields to follow the shared tokens.
- Updated `OrbRenderer.kt` to use the reference's exact semantic listening/executing/reasoning/done colors. Orb surface remains transparent.
- This is source-level design integration only. CI for the resulting commit and phone rendering are separate required checks; do not claim pixel-perfect parity or phone validation without evidence.


## 2026-11-01 continuation — chat reliability and install routing

Branch: `feature/u1-live-orbit-tasks` only. No `main`, Hermes/Render backend, or production database changes.

### Implemented in source (CI/device verification pending)
- Hermes chat context is capped at 2,000 characters for memory + recent history; current user message remains separate and is not truncated by that context cap.
- HTTP 400 can retry once with the raw user message when it differs from the contextual prompt.
- Connection IO failures retry up to three times with 500 ms, 1 s, and 2 s delays (capped policy); UI status displays “HERMES JAG RAHA HAI” while retrying.
- “install/get/download <app> from Play Store” is classified separately from generic APK installation, still requires explicit CONFIRM, and opens a Play Store app-search URL. User must choose the app and tap Install in Play Store.
- Generic “install this APK” remains on the Android document picker path.
- Added ChatContextPolicy and ActionEngine regression tests for context bounds, raw 400 retry conditions, connection backoff, and Play Store routing.

### Verified existing behaviour (no rewrite)
- Chat queue uses a single-thread executor and stores per-message queued/running/done/error states.
- Chat transcript renders Markdown headings and bold; HTTP response body is read as a complete string. No explicit reply truncation was found in the inspected path.

### Not yet verified
- Latest branch CI/unit-test outcome and final APK artifact/checksum.
- Runtime Hermes 400 recovery and offline retry behaviour on device.
- Play Store intent handling across Android devices.
- Phone testing remains pending.

### Next work
- Continue remaining device skill/agent-loop, accessibility observe-plan-act-verify, result viewer/game output, and UI/safety regression tasks.


### Follow-up safety and UI hardening
- Added a centralized device-action safety decision: STOP CONTROL overrides SAFE, CONFIRM, and BLOCKED; CONFIRM remains explicit-tap only; BLOCKED remains denied. Added unit tests for the precedence matrix and wired the policy into the action execution gate.
- Primary OpenGL orb now explicitly uses a transparent view background.
- Queue Limit control now has a neutral grey surface rather than inheriting a dynamic button theme.
- Task result cards and their nested result scroller disable child clipping/nested scrolling to reduce card clipping risk.
- These UI adjustments are source changes only; visual device verification remains pending.


## 2026-11-01 — Per-app Accessibility access foundation
- Added a per-app access policy with a master switch and explicit package allowlist. STOP CONTROL is checked independently and always blocks the Accessibility service.
- Added Profile > App access: launchable installed apps are listed with individual ON/OFF checkboxes, a master ON/OFF switch, and Enable All. The first visit initializes launchable apps as enabled; the user can turn off any app.
- Accessibility screen-tree inspection, visible-text click, swipe/long-press, scroll, focused-field typing, global navigation, and local screenshot now check the current foreground package against the app allowlist and STOP CONTROL.
- Added unit tests for master-off precedence, per-app isolation, and default denial of unknown packages.
- This is the permission/control gate only. It does NOT yet implement the autonomous observe → plan → act → verify loop, universal app-specific workflows, or guarantee control over secure/inaccessible custom UI. Phone test remains pending.


## 2026-11-01 — UI Agent observe/act/re-observe foundation
- Added Profile > UI Agent entry. User opens the target app, enables it in App access, then enters a natural-language UI task.
- Added one-action-at-a-time planner using the configured NVIDIA live model. It receives the current accessibility tree as untrusted data and returns a constrained JSON action.
- Added policy validation: text taps must exactly match a currently visible label/description; typing is allowed only when the exact text is explicitly in the user's task; credential-like text is rejected; consequential visible controls require an on-screen confirmation; run is capped at 12 actions.
- After each accepted action, the UI is observed again before the next plan. STOP CONTROL, service loss, empty/disabled screen, invalid plan, or rejected Android action stops the run.
- This initial loop supports semantic text taps, scroll, type, back, wait and done. It does not yet support visual-only custom canvases or OCR-based coordinate actions; complex editors such as CapCut may expose controls that Accessibility cannot describe. Phone testing remains pending.


## 2026-11-01 — UI Agent guarded node interactions
- Re-observation delay after each accepted action is 300 ms (not a promise that every target app has completed its animation).
- Added per-app AppAccessPolicy implementation and tests. First App access initialization enables the launchable app snapshot; packages added later are denied until explicitly enabled. Master OFF and persistent STOP CONTROL deny accessibility operations.
- Added indexed TAP_NODE and LONG_PRESS_NODE actions for unlabeled controls that expose clickable/long-clickable Accessibility nodes. Node actions always require explicit confirmation and are rejected if the node's latest-observation fingerprint changes before execution.
- Corrected Kotlin compile issues in the UI Agent step counter and ScrollView child layout parameters.
- Still unsupported: controls with no Accessibility node, OCR/vision coordinate interaction, some custom Canvas editor surfaces, protected/secure windows. Phone testing remains pending.


## 2026-11-01 — UI Agent app launch and bounded gestures
- UI Agent can launch an app only when its exact package is in the user's current App access allowlist; the model receives only enabled launcher apps and cannot invent a package.
- Added confirmed tap/long-press/swipe gestures for a bounded accessibility node when it has no clickable action. Gesture regions must be on-screen, at least 24 dp in each dimension, and no larger than 35% of the display. Password/editable nodes and stale node fingerprints are rejected.
- The UI Agent start dialog now explains that visible screen labels are sent to the configured NVIDIA model for planning. Do not use it on private/secure screens.
- Remaining gap: visual-only controls that expose no useful Accessibility node still need local OCR/vision-based perception. Secure windows remain blocked by Android.


## 2026-11-01 — Foreground-safe UI Agent and local OCR
- Replaced task progress/confirmation AlertDialogs with a non-focusable Android Accessibility Overlay panel. It has STOP, CONFIRM/CANCEL, and CLOSE controls without intentionally moving the target app out of the foreground.
- Added local ML Kit text recognition from Android screenshots. Screenshots are processed in memory and are not uploaded; filtered OCR text labels and the Accessibility tree are sent to the configured NVIDIA planner after the user starts UI Agent.
- Added tap_ocr and long_press_ocr planning. Each must select an exact OCR label/index from the latest observation and is rejected if the target app/window changed before execution. Always Ask and Risky Asks require confirmation; a temporary Session Grant can skip confirmation only for non-consequential OCR targets in allowlisted apps. Consequential targets still require confirmation. OCR lines matching credential/OTP/card/email patterns are filtered.
- This increases coverage for visible text in some custom views, but does not provide universal visual understanding or reliable editing of every custom Canvas/timeline surface. Phone testing remains required.


## 2026-11-01 — Financial/security action deny rules
- AppAccessPolicy excludes common bank, UPI, wallet, payment, trading and investment apps from the allowlist and displays matching apps as BLOCKED. The block is also enforced in the AccessibilityService and app launcher path, not just the UI.
- UI policy rejects visible targets labelled as payment/UPI/wallet/bank/transfer, security/permission/biometric/password/OTP, or uninstall. Node gestures inspect the node label too.
- This is a conservative identifier/label blocklist, not a guarantee that every financial app can be recognized. Unknown apps must still be manually reviewed in App access.


## 2026-11-01 — Exact field targeting and app-window binding
- UI Agent text entry now requires the exact editable Accessibility node index and a current node fingerprint. Password/editable contents are redacted from the planner tree; non-sensitive field hints remain visible.
- Tap-text, scroll, and back actions carry the observed foreground package and are rejected if a different app is now active. Node fingerprints include package, bounds, flags, and a hash of visible labels so changed controls are rejected without placing label text in the fingerprint.
- Financial/payment app blocking is a conservative package/label heuristic; it is not exhaustive. The allowlist and action-level deny rules remain in force.


## 2026-11-01 — UI Agent continuation
- Action-to-re-observation cadence is 300 ms.
- Added OCR-target tap/long-press, bounded accessibility-node tap/long-press/swipe/drag, exact editable-node text entry, and app launch from the explicitly enabled launcher-app allowlist.
- Extended the per-task bounded action budget to 40 steps so multi-screen workflows are not cut off after 12 actions.
- Text entry is rejected if the current screen exposes payment/security-sensitive labels; screenshots remain local while filtered OCR labels may be sent to the configured planner.
- These controls are guarded automation, not unrestricted control of every Android surface. Custom canvas/editor flows and protected OS screens still require device validation.


## 2026-11-01 — UI Agent confirmation modes and action audit

Implementation commit: `bf714cac1afdc6444bbaff6fba89aa07b66e37b7` on `feature/u1-live-orbit-tasks`.

- Profile > UI Agent now offers three per-task confirmation modes:
  - **Always ask:** visible text taps require explicit confirmation; node/OCR gestures continue to require confirmation.
  - **Risky asks (default):** ordinary visible text taps can proceed; consequential targets and unlabeled/OCR gestures require confirmation.
  - **Session grant:** after a second explicit grant confirmation, safe bounded node/OCR gestures can proceed without repeated prompts for this task only. Grant expires after 5 minutes or the existing 40-action limit.
- Session grants never override the app allowlist, STOP CONTROL, password/editable-field protections, payment/banking/security/uninstall deny rules, fresh-screen fingerprints, bounded gesture regions, or consequential-action confirmations. If the task goal or target label indicates sending, sharing, deletion, purchase, install, posting, transfer or similar consequential work, confirmation remains required.
- The selected mode and grant start/expiry/revocation are recorded in the local action audit. Every UI Agent action records its action kind and outcome; typed contents and visible labels are deliberately omitted from audit descriptions.
- Corrected the UI Agent progress text to show the actual 40-step budget (it previously displayed /12 in one progress message).
- Regression tests cover all three modes, session-grant handling of consequential OCR targets, and blocked sensitive targets.
- RED verification: test-only commit `3bf41351163e00dab2f2369bfe15b80ece36b8a1` failed as expected because `ConfirmationMode` and the `confirmationMode` parameter did not yet exist.
- GREEN verification: CI run `41280298743` and debug run `41280298861` both succeeded on implementation SHA `bf714cac1afdc6444bbaff6fba89aa07b66e37b7`. Secret scan, unit tests, debug APK build, packaging and upload succeeded.
- Debug artifact: `vyom-0.3.0-bf714cac1afdc6444bbaff6fba89aa07b66e37b7` (artifact ID `13073271470`); APK SHA-256 `56d2f793f75aad69b1d8fe71be2701a804a4f3ae65c60f95399ea57e1179a9ce`. Artifact ZIP digest: `3b216fda35e8754b0180f0c80d32ba5cd377810e9cbce0099b52bf1bfbf8f2bb`.
- The APK is a debug build, not a release-signed update. Its debug signing key is generated in the CI runner; certificate continuity with the previously installed APK has not been verified. Do not uninstall the current app or install this artifact over it until the signing-certificate fingerprints are compared or a release is built with the existing keystore. CI does not prove device behavior or that a task's final result is correct. Phone testing remains pending. UI Agent's `DONE` status is still a model-reported completion and must be checked against the actual app screen.
- Scope unchanged: this is Android-only. No Android `main`, Hermes/Render backend, or production DB changes.



## 2026-11-01 — Memory + Intelligence Android integration

Branch: `feature/memory-intelligence`
Implementation SHA: `11a5abcb405f6720ace38078288347a0ac328759`

- Added `MemoryCapturePolicy`: explicit remember requests, simple preference statements, corrections and safe name statements can become local facts. Ordinary questions are not automatically saved.
- Sensitive-value filters reject passwords, OTP/PIN/CVV, API/access tokens, card/account identifiers, government IDs, health/medication details, email addresses, phone numbers and long numeric identifiers.
- `VyomMemoryStore` continues to use Android EncryptedSharedPreferences; maximum 30 facts. Profile > Memory allows enable/disable, edit/replace and clear.
- Relevant local facts are ranked for the current message; nonmatching facts are excluded when matches exist. Current message remains separate from the bounded context.
- Added `VyomIdentityPolicy` to ground responses in actual connected capabilities and limitations, distinguish local UI Agent from Hermes task execution, and require verification before claiming actions.
- Identity policy is used by the Android chat persona prompt. LIVE voice prompt no longer forces every response to one or two short sentences; selected language is retained.
- Regression tests cover memory capture, sensitive-data rejection, relevance ranking, identity/capability boundaries, context budget and language mode.
- RED verification: initial Android CI run `41285930431` failed 3 new tests (relevance ranking and two identity-prompt assertions). Fixed the implementation and prompt.
- GREEN verification: Android CI run `41293163136` and Debug run `41293163133` both succeeded on SHA `11a5abcb405f6720ace38078288347a0ac328759`.
- Artifact `vyom-0.3.0-11a5abcb405f6720ace38078288347a0ac328759`, ID `13077883833`; ZIP SHA-256 `ccb6e28af72566fcb6e99b7d6677acd10024d06dc3676c1a6478177477456c44`; APK SHA-256 `c19577de869347ed9bf1e7fb6e87a598a074add17391f985ec7f02b2e70d909f`.
- APK is an unsigned CI debug variant with a CI-generated debug signing key. Certificate continuity with the previously installed Vyom APK is not established; do not uninstall the existing app or install over it until certificate fingerprints are compared or the correct release keystore is configured.
- Device behavior remains unverified until user phone testing. No production deployment or merge to Android `main`.



## 2026-11-01 — Android memory relevance and identity refinement

Implementation commit: 26a45fcf69b471221c925d1e3e81299cea341c7f.

- Local memory retrieval now ranks facts against the current request and limits identity/context prompt size; unrelated facts are not injected when relevant matches exist.
- LIVE voice preserves the selected language and uses the same capability-grounded identity contract; simple requests can stay brief while detailed requests can receive detail.
- Android CI run 41293535616 and Debug run 41293535594 both passed on this exact SHA.
- Debug artifact ID 13078422171, name vyom-0.3.0-26a45fcf69b471221c925d1e3e81299cea341c7f. ZIP SHA-256 7e42f1ac38dc928a10170091b58346202ed74bc750e568eb26fb1be10e4d0f07; extracted APK SHA-256 d2aacec55feebf454f3e62eecb95d249775d6c96aad3a676c1d8a354ad4eb00d.
- This remains an unsigned CI debug variant with a runner-generated debug certificate. Do not install over the current app or uninstall it until certificate continuity is checked or a release is built with the original keystore.
- Android main remains unchanged. No Hermes deployment, Render deployment or production database migration was performed by the Android branch.


## 2026-11-01 — Android memory/identity integration handoff refresh

Latest Android branch tip before this documentation refresh: `fd40875a1039944e308cc7805b7f2559363699aa`.

- Android memory/identity implementation commits: `11a5abcb405f6720ace38078288347a0ac328759` (capture policy, encrypted local memory, identity/capability prompt) and `26a45fcf69b471221c925d1e3e81299cea341c7f` (relevance ranking, prompt-budget refinement and LIVE language preservation).
- Latest Android CI run `41297836301` passed on exact branch tip `fd40875a1039944e308cc7805b7f2559363699aa`.
- Latest Android Debug run `41297836236` passed on the same exact SHA. Artifact: `vyom-0.3.0-fd40875a1039944e308cc7805b7f2559363699aa`, artifact ID `13080450404`, ZIP digest `sha256:99d9579dee465281d2baa3e683af28abcbd20885e21e08d46b042a74c0ab018b`.
- The artifact is a CI debug APK with a runner-generated debug signing key. Certificate continuity with the user's installed Vyom APK is not established. Do not uninstall or overwrite the installed app until signing fingerprints are compared or a build uses the original release keystore.
- The local memory store remains encrypted and bounded to 30 facts. Capture is deliberately conservative; it is not a general semantic classifier. Profile > Memory provides local enable/disable/edit/clear controls.
- Android branch remains isolated from Android `main`; no Hermes deploy, Render deploy or production DB migration was performed as part of Android implementation.
- Device/phone validation remains pending. CI green proves build/tests, not UI Agent task accuracy, background behavior, accessibility coverage or signature continuity.

Next: finish cross-system identity/data-flow review, then proceed with semantic retrieval only after production schema/migration approval. Keep production migration, Render deploy and merge to either `main` gated separately.
