# CURRENT MEMORY INTELLIGENCE CHECKPOINT — 2026-11-02 11:20 IST

This addendum supersedes older milestone-order and Memory Intelligence status statements below.

## Active branch and exact-head verification
- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Verified source head before this documentation-only update: `9a01558889fe63a32df58e3b9e7f8d60e32aaba8`.
- Android CI run `41394477563`: SUCCESS on that exact SHA. Secret scan, Gradle unit tests + debug APK build, checksum packaging and artifact upload all passed.
- CI artifact: `vyom-phase6-9a01558889fe63a32df58e3b9e7f8d60e32aaba8`, artifact ID `13119016261`, 45,875,108 bytes, expires 2026-12-02.
- Separate Debug run `41394477654`: SUCCESS on the same exact SHA. Unit tests, unsigned debug APK build, versioned packaging and upload all passed.
- Debug artifact: `vyom-0.3.0-9a01558889fe63a32df58e3b9e7f8d60e32aaba8`, artifact ID `13118304535`, 45,875,115 bytes, expires 2026-12-02. ZIP SHA-256: `52253d9019d69671aaf01622ba712e7526633040494c2da908d8ce4c6eff8faf`.
- The branch ref was re-read and still pointed to `9a01558889fe63a32df58e3b9e7f8d60e32aaba8` before this documentation update.
- These are debug artifacts, not a signed release. Device install/signature continuity and physical runtime remain unverified.

## Memory Intelligence implementation now present
- `MemoryCapturePolicy.kt` now canonicalizes common synonym groups for response length/detail, response/answer/reply, list/bullet formats, Bangla/Bengali, execution consent and action/task/change phrasing before lexical retrieval scoring.
- Conflict topics include tone and execution-consent; newest facts win for these conflicts even if an older fact has a higher lexical overlap with the query.
- Three regression tests were added for cross-worded response-style retrieval, newest tone conflict and newest action-confirmation conflict. RED evidence: test-only commit `488ab96dc7724a8293076f3c8264fe2884b6f257`, Debug run `41394222326`, 121 tests with exactly 3 expected failures exposing those gaps.
- Fix commit: `f3c9946ae1ccf3d089600f3612a661657331fce8`.
- Memory prompt data-flow disclosure was updated in `VyomMemoryDialog.kt` so users are told that selected safe facts are stored locally and relevant facts may be included in prompts sent to the connected AI model. Commit/current source head: `9a01558889fe63a32df58e3b9e7f8d60e32aaba8`.
- GREEN verification: exact-head CI and Debug runs listed above both passed. The Debug job ran the Gradle unit-test task and APK build successfully.
- Important precision: retrieval is synonym-aware lexical matching, NOT vector/embedding semantic search. Do not describe it as full semantic/vector memory.

## Supabase boundary — read-only inspection only
- Inspected project `Vyom-Hermes` (`kjsxsswgzyoezjuzpfqm`) read-only.
- Only one project was available; no staging branch was returned.
- Existing `public.memory` schema has `chat_id`, `memory`, `created_at`; no Android identity mapping/user_id was found.
- Vector extension is listed but has no installed version. No schema, extension, migration or data writes were performed.
- Do NOT sync Android-local memory into Telegram's `chat_id` memory table without an approved identity/isolation design.
- Production vector migration and staging validation remain BLOCKED until an isolated staging environment and identity/data-isolation design are approved. No approval or production mutation is implied by this work.

## Current honest status and next order
- Local synonym-aware retrieval, conflict preference and prompt-data disclosure are implemented and exact-head CI/Debug green.
- Memory Intelligence is NOT fully complete: embedding/vector retrieval, production-safe sync/migration, isolated staging validation and broader conflict-resolution category tests remain open.
- User's current agreed sequence supersedes older handoff ordering: (1) finish Android Control implementation, (2) finish Memory Intelligence implementation within safe approved boundaries, (3) implement the supplied Vyom design, (4) provide the final exact-head APK for the user's physical phone testing. Do not require phone testing before Memory Intelligence/design implementation unless the user changes this order.
- Android Control automated source/build checkpoint has passed previously, but full real-device acceptance remains pending. Do not claim Android framework behavior or cross-app control fully validated.
- Continue only on `feature/memory-intelligence`. Never touch Android `main`, Hermes/Telegram, Render, production Supabase, secrets or deployment.

---

# FULL ANDROID CONTROL — IMPLEMENTATION STEP 1 — 2026-11-02

## Fixed milestone order (do not reorder)
1. Full Android Control implementation.
2. Android verification (automated + exact-head CI + physical phone testing).
3. Memory Intelligence integration/verification.
Memory Intelligence work is paused until steps 1 and 2 are completed. No Telegram/connectors/backend/deployment work.

## Step 1 — OCR-target swipe/drag
- Source commits on `feature/memory-intelligence`:
  - policy + action types: `2c6886e`, confirmation hardening: `05bcd31`
  - Accessibility OCR gesture executor: `74edc49`, float-distance guard fix: `aaec31a`, syntax guard fix: `ccb0860`
  - MainActivity planner/confirmation/executor wiring: `4225a9f`
  - regression tests: `1a2357b`, fixture update: `3f99eaa`, explicit-confirmation test: `dd0c473`
- Added `swipe_ocr` (left/right/up/down) and `drag_ocr` (left/right/top/bottom/center anchors).
- Policy requires an exact OCR index + text + fingerprint from the current observation, bounded on-screen geometry, valid direction/anchors, and explicit confirmation. Session grants do not bypass OCR swipe/drag confirmation.
- Runtime executor rechecks active foreground package, per-app allowlist/master/STOP state, OCR revision/fingerprint, sensitive-text filter, bounds (minimum 8dp, maximum 10% screen area), and gesture length/duration limits before dispatch.
- Planner JSON schema, action parsing, confirmation UI, execution routing and privacy-safe audit labels are wired.
- OCR capture remains local; sensitive OCR lines are filtered before being sent as planner context.

## Verification truth
- Regression tests added: `UiAgentPolicyTest.kt` now contains 28 tests, including OCR swipe/drag freshness, invalid direction/anchors, unsafe bounds, sensitive targets and session-grant confirmation.
- Tests have NOT been executed in this environment. No CI run was triggered or polled. No compile/build success is claimed.
- Physical Android test: NOT RUN.
- This step adds OCR gestures only. It does NOT solve canvas-only/timeline controls with no OCR text, general screenshot-based vision control, or the CapCut end-to-end workflow.
- Existing 300ms re-observation and model-reported DONE limitation remain open for the next reliability step.

## Next fixed step
Implement event/revision-aware re-observation and explicit outcome verification, then add tests for stale screens, foreground app switches, STOP during confirmation, grant expiry and Android action failures. After that, address screenshot-derived visual-only controls as a separately gated capability and build a CapCut workflow test plan. Do not mark Full Android Control complete until physical-device verification passes.

## Safety
Android `main`, Hermes, Render, production Supabase, secrets, releases and deployment untouched. No CI polling.

---

# ANDROID CONTROL RECONCILIATION — 2026-11-02

This entry corrects the milestone status before further implementation. It is based on source inspection at parent head `26119c05ce083a3e3776084b61dc2f0d9fca2e60`.

## What is already implemented in source (not yet phone-verified)
- Per-app allowlist + master switch, STOP CONTROL check, financial/payment package and label deny rules.
- Accessibility tree observation (up to 180 nodes), fresh-screen revision/package checks and redaction of editable/password values.
- Local Android 11+ screenshot OCR; filtered OCR labels and node metadata are supplied to the configured NVIDIA planner. Screenshot pixels are not uploaded.
- UI Agent one-action planner loop: observe → plan → policy validate → optional user confirmation → act → re-observe. 40-action ceiling; user STOP, service loss, invalid plan or rejected Android action stops the run.
- Actions currently wired: launch allowlisted app, exact visible-text tap, OCR text tap/long-press, indexed node tap/long-press/swipe/drag, scroll, exact-goal text entry, Back, wait and done.
- Confirmation modes: Always Ask, Risky Asks and a temporary task-only session grant. The grant does not override consequential-action confirmation or protected deny rules.
- Local action audit omits typed text and visible labels.

## Important limits — do not call this unrestricted/full Android control
- Android Accessibility cannot operate every protected/secure OS surface; secure screenshots are blocked by Android.
- Custom Canvas/timeline controls may expose no Accessibility node and may have no OCR text target. There is no general screenshot-understanding vision planner or arbitrary coordinate-action API in this implementation.
- OCR currently supports tap and long-press only; it does not provide OCR-target swipe/drag.
- The fixed 300 ms pause is only a re-observation delay, not proof that the target app finished its animation or that the intended outcome occurred.
- `DONE` is model-reported; the code explicitly tells the user to manually verify the screen.
- Service methods and policy tests have source-level coverage, but no physical-device task has been validated. CI success is not Android runtime proof.

## Next implementation sequence
1. Add regression tests for the next bounded visual-interaction capability before changing behavior.
2. Extend safe OCR-target gestures for swipe/drag only when bounds are fresh, on-screen, size-limited and tied to the same foreground package/revision; keep sensitive labels blocked and require confirmation.
3. Replace the fixed-delay assumption with bounded, event/revision-aware re-observation and explicit outcome checks; never report model `DONE` as verified success.
4. Add explicit test coverage for stale screen, foreground app switch, STOP during confirmation, grant expiry and action failure.
5. Keep custom-canvas screenshot reasoning as a separately gated capability; do not pretend OCR alone solves it.
6. Update this HANDOFF after each implementation step/commit with exact source SHA, tests actually run, CI status only when checked once, and device status.

## Scope and safety
- Android repo only, branch `feature/memory-intelligence`.
- Do not touch Android `main`, Hermes/Telegram/connectors, Render, production Supabase, signing secrets or deployment.
- Do not poll CI. Any later CI check must be a single check for that exact commit, respecting the existing maximum of two checks.
- Phone tests remain deferred until the implementation phases are complete.
- No source-code changes are included in this reconciliation entry.

---

# CURRENT VERIFIED ADDENDUM — 2026-11-02 09:25 IST

This addendum supersedes earlier Android source-head and artifact claims below.

## Current branch and safety
- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Current verified head: `26119c05ce083a3e3776084b61dc2f0d9fca2e60`
- This latest commit updates only `docs/HANDOFF.md`; Android source code is unchanged from `53ff20954e055027a3d676195573f573317b5904`.
- Android `main`, Hermes production, Render, production Supabase, signing secrets and releases untouched.

## Latest CI/debug artifact
- Android CI run `41381678937`: PASS on exact head `26119c05ce083a3e3776084b61dc2f0d9fca2e60`.
- Debug build run `41381678903`: PASS on the same exact head.
- Artifact ID: `13113965421`.
- Artifact: `vyom-0.3.0-26119c05ce083a3e3776084b61dc2f0d9fca2e60`.
- Artifact is CI/debug-signed. Signing continuity with the installed APK remains unverified; do not overwrite or uninstall until certificate fingerprints are compared or the correct release key is used.
- Physical-device validation remains pending.

## Design status
- Supplied HTML board was inspected; Home, Drawer and Automations references were rendered locally.
- Existing native implementation and its honest limitations remain as documented below. No Android UI source changes were needed in this continuation.
- Scheduled/repeating automations remain unavailable; the UI does not claim otherwise.

---

# CURRENT VERIFIED ADDENDUM — 2026-11-02 08:48 IST

This addendum supersedes older Android source-head and artifact claims below.

## Active branch and safety
- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Verified Android source head: `53ff20954e055027a3d676195573f573317b5904`
- Android `main`, Hermes production, Render, production Supabase, signing secrets and releases untouched.
- No Android source changes were made during the 2026-11-02 continuation; this was a design/source review only.

## Exact-head CI and artifact
- Android CI run `41322665760`: PASS on `53ff20954e055027a3d676195573f573317b5904`.
- Debug build run `41322665691`: PASS on the same exact head.
- Artifact ID: `13088999925`.
- Artifact: `vyom-0.3.0-53ff20954e055027a3d676195573f573317b5904`.
- Artifact is CI/debug-signed. Signing continuity with the installed APK remains unverified; do not overwrite or uninstall the installed app until certificate fingerprints are compared or the correct release key is used.
- Physical-device validation remains pending.

## Supplied design artifact review
- Source: Library file `Vyom — Design System, Home, Drawer, Automations (1).html`, containing 31 embedded page bundles.
- The first four pages are Design System, Home, Drawer and Automations. The supplied board was unpacked and the Home, Drawer and Automations reference screens were rendered locally for inspection.
- Existing native implementation is present in `VyomDesignSpec.kt`, `VyomTheme.kt`, `OrbitHomeView.kt`, `VyomDrawer.kt`, `VyomAutomationsDialog.kt` and `MainActivity.buildHomePanel()`.
- Home already contains the HUD/orbit backdrop, live task context, greeting, prompt composer, quick-access actions, router/persona indicator and STOP CONTROL.
- Drawer already has searchable Workspace/Assistant/Brain/System destinations and marks unsupported destinations as SOON.
- Automations already uses live task records, pending/working/done counts, status filters and task cards. It explicitly warns that scheduled/repeating automations are not connected and does not create fake schedules.
- No design implementation changes were needed in this continuation because these native components and their design-token tests were already present. Physical rendering on the user's Android phone remains unverified.

## Honest UI boundaries
- The 31-page board is a design reference, not proof that every destination is implemented.
- Animations, Videos, Images, VFX, CGI, SFX, Instructions and Icons remain unavailable/marked SOON where no real executor/screen is connected.
- Connections still routes to existing Settings; it is not a complete OAuth connector manager.
- Scheduled automations are not implemented. New Automation presents the limitation and routes to immediate Tasks.
- Do not claim physical-device validation or release-signature continuity.

---

# CURRENT VERIFIED ADDENDUM — 2026-11-01 15:44 UTC

This addendum supersedes older Android source-head and design-status notes below.

## Active source and verification
- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Verified source head: `a9725363e706ae09fe2c26bec3b9cb9dbf3fc468`
- Android CI run `41320301809`: PASS on this exact head.
- Debug build run `41320301834`: PASS on this exact head.
- Artifact ID: `13089046691`.
- Artifact: `vyom-0.3.0-a9725363e706ae09fe2c26bec3b9cb9dbf3fc468`.
- CI/debug signing continuity with the installed APK remains unverified. Do not overwrite/uninstall until certificate fingerprints are compared or the correct release key is used.
- Physical-device testing remains pending.

## Supplied design file review
- Latest Library design source: `Vyom — Design System, Home, Drawer, Automations (1).html` (31 embedded screen/page bundles).
- Main target screens are pages 1–4: Design System, Home, Drawer and Automations.
- Existing native implementation is present in `VyomDesignSpec.kt`, `VyomTheme.kt`, `OrbitHomeView.kt`, `VyomDrawer.kt`, `VyomAutomationsDialog.kt`, and `MainActivity.buildHomePanel()`.
- Home: HUD orb/task orbit, greeting, prompt input, quick access, AUTO/persona indicator and STOP CONTROL.
- Drawer: searchable Workspace/Assistant/Brain/System destinations; unconnected workspaces remain visibly marked SOON.
- Automations: live task-derived pending/working/done counts, filters and task cards. Scheduled/repeating automations are explicitly not connected; New Automation does not create a fake schedule and routes to immediate Tasks.
- Design review did not modify Android source in this continuation; the relevant native implementation was already present and CI-green.

## Honest remaining UI boundaries
- The HTML board contains 31 screens; this does not mean all 31 app capabilities are implemented.
- Animations, Videos, Images, VFX, CGI, SFX, Instructions and Icons remain unavailable/marked SOON where no executor or screen behavior is connected.
- Connections currently routes to existing Settings, not a full OAuth connector manager.
- Do not claim scheduled automation support.
- Do not claim physical-device validation or release-signature continuity.

## Safety
- Android `main` untouched.
- No Render, production Supabase, backend deployment, signing-secret or release change made.
- Pairing/chat API boundary remains unchanged.

---

# CURRENT VERIFIED ADDENDUM — 2026-11-01 15:20 UTC

This addendum supersedes older active-head and verification claims below.

## Active source and safety
- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Verified source head: `24542baa07826215ea77535e17a5ce439b9d383f`
- Android `main` untouched. Hermes production, Render, Supabase production, signing secrets and releases untouched.
- No physical-device test was performed. Do not claim runtime/device readiness.

## Latest Android verification
- Android CI run `41319206715`: PASS on exact head `24542baa07826215ea77535e17a5ce439b9d383f`.
- Debug APK run `41319206727`: PASS on the same exact head.
- Artifact ID: `13088433453`.
- Artifact: `vyom-0.3.0-24542baa07826215ea77535e17a5ce439b9d383f`.
- Artifact is CI/debug-signed; signing continuity with the installed APK remains unverified. Do not overwrite/uninstall until certificate fingerprints are compared or the correct release key is used.

## Current implementation notes
- Supplied Design System / Home / Drawer / Automations implementation remains in the active source tree.
- Memory capture now prioritizes the specific Hinglish phrase `Yaad rakhna ki ...` before the broader `Yaad rakhna ...` matcher.
- Pairing/chat, Android-local encrypted memory and current app API boundary remain unchanged by the latest memory-intent fix.
- Physical testing and release-signature continuity remain pending.

---

# CURRENT IMPLEMENTATION ADDENDUM — 2026-11-01

This addendum supersedes older Android implementation-head and UI-status notes below.

## Active source
- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Verified source head: `168664ceede0310b9335935422bceda79aedffd2`
- `main` remains untouched.

## Supplied design implementation
- Added canonical HUD tokens, navigation destinations, safe device-skill catalog and orb/task state semantics in `VyomDesignSpec.kt`.
- Reworked top header with a three-line navigation affordance, moved the five-item dock to the bottom, and repositioned the hero orb/task orbit to remain visible above the glass panel.
- Added searchable drawer with Workspace, Assistant, Brain and System sections. Unsupported workspaces remain explicitly marked SOON.
- Home exposes Tasks, Memory, Skills, AUTO ROUTER/persona indicators and STOP CONTROL.
- Added full Memory manager: enable/disable, search, add/edit, individual delete and confirmed clear-all. Facts remain in the encrypted local store; audit entries never contain fact text.
- Added searchable Skills screen exposing only the four implemented local device actions: open app, volume, torch and settings panels.
- Added Automations workspace showing actual task records and real status counts. Time-based/repeating scheduling is explicitly shown as not connected; the New Automation action explains this and offers Tasks instead.
- Task cards use distinct status pills and show progress only when a numeric progress value is actually supplied by the API. Tapping opens task details populated only from returned task fields.
- Chat bubbles/status pills refined. Orb supports ready/listening, task/control, thinking/speaking, muted/offline and error visual states.
- STOP CONTROL is accessible from Home and the drawer. Existing pairing, chat, memory encryption and task API flows are preserved.

## Honest feature boundaries
- Animations, Videos, Images, VFX, CGI, SFX, Instructions and Icons remain marked SOON because no corresponding Android executor/workspace is connected.
- Connections currently routes to existing Settings; it is not a separate OAuth connection manager.
- Automations currently displays immediate task records only; no scheduled or recurring job has been created.
- Device UI remains dependent on existing Android permission/confirmation gates. The design work does not grant accessibility, overlay, microphone or background permissions.

## Verification and artifact
- Android CI run `41313585475`: PASS on exact source head `168664ceede0310b9335935422bceda79aedffd2`.
- Debug build run `41313585469`: PASS; unit tests and APK packaging steps successful.
- Artifact ID: `13086636946`
- Artifact name: `vyom-0.3.0-168664ceede0310b9335935422bceda79aedffd2`
- Artifact ZIP digest: `sha256:07a4ca7d4e7211734ca8b8eabaab1c887db531399e328a90a4070d9ed76c6ceb`
- CI artifact signing continuity with the installed phone APK is not established. Do not uninstall or overwrite the installed app until signing fingerprints are compared or the correct release keystore is used.
- Physical-device validation remains pending.

## Restrictions
- Android `main` untouched.
- No Hermes production changes, Supabase production migration, Render deploy or production environment change was made by Android UI work.

---

# CURRENT MEMORY + INTELLIGENCE ADDENDUM — 2026-11-01

This addendum is authoritative and supersedes older branch/baseline statements later in this document.

## Active source state
- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Verified implementation head: `d59590d5faa5dd201e5d49939093e4332a8f8b61`
- Documentation-only commit: `5439aa63934907ba2da21d7482876cf43c351b84`
- Parent implementation tip: `fd40875a1039944e308cc7805b7f2559363699aa`
- Do not modify or merge `main`.

## Memory behavior
- Memory capture recognizes explicit remember requests, preferences and corrections in English, Hinglish and Bengali.
- Sensitive-data filters include Bengali credential/OTP labels and Bengali numeric identifiers.
- Retrieval tokenization supports Unicode letters and combining marks.
- When captured preferences in the same supported category conflict, the newer fact is prioritized; facts are not automatically deleted.
- Local store remains encrypted, bounded to 30 facts, and user-manageable through Profile > Memory.

## Android ↔ Hermes boundary
- Android memory remains device-local.
- Relevant safe facts are included in the contextual message sent to `/api/android/chat`; there is no separate structured memory-sync field.
- The Hermes Android API routes the chat request to the model; it does not persist Android local facts into Telegram memory.
- Telegram memory remains separately scoped to the private Telegram chat. No Android-session-to-Telegram identity mapping or cross-store synchronization exists.
- Do not add synchronization without explicit identity mapping, authorization and leakage tests.

## Verified CI and artifact
- Android CI run `41309421821`: PASS on exact head `d59590d5faa5dd201e5d49939093e4332a8f8b61`.
- `testDebugUnitTest assembleDebug`: BUILD SUCCESSFUL.
- Artifact ID: `13085200684`
- Artifact: `vyom-phase6-d59590d5faa5dd201e5d49939093e4332a8f8b61`
- SHA-256: `3f3239858a287f1a89eba74b8bf1e7276fc21a9af95f96a8d769a8613ba399c5`
- Debug artifact uses CI signing. Signing continuity with the user's installed APK is not established; do not overwrite/uninstall until fingerprints are compared or the correct release keystore is used.
- Physical-device testing remains pending. CI success is not device validation.

## Production restrictions
- No production migration, database write, Render deploy, environment change or merge was performed.

---

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


# CURRENT DESIGN IMPLEMENTATION + BUILD ADDENDUM — 2026-11-01

This addendum supersedes prior Android source-head and artifact statements above.

## Current verified source
- Branch: `feature/memory-intelligence`
- Source SHA: `dab5c46cd93500b03a9bf31c544e8c757c1ca703`
- Android CI run `41314611445`: PASS.
- Android Debug run `41314611509`: PASS. Build, unit tests, APK packaging and artifact upload all succeeded.
- Artifact ID: `13086004190`
- Artifact name: `vyom-0.3.0-dab5c46cd93500b03a9bf31c544e8c757c1ca703`
- Artifact ZIP SHA-256: `cd14ed8d8f0a4e7dde63f1fb4906dd286e39db38c7ee763a384009efef0f1746`
- Artifact expires 2026-12-01.
- This remains a CI debug-signed artifact. Signing continuity with the installed APK is unverified; do not uninstall/overwrite until certificate fingerprints match or the correct release keystore is used.

## Design source reviewed
- `Vyom — Design System, Home, Drawer, Automations (1).html` (2026-11-01 supplied design board).
- Implemented native Android HUD design tokens, Home quick-access area, searchable drawer, bottom navigation styling, Automations task workspace, searchable Skills registry and task-detail inspector.
- Automations display only actual task API records and actual status counts. Scheduled/repeating automations remain unavailable and are clearly disclosed.
- Skills enable/disable preferences are persisted locally and enforced by the device action dispatcher for open-app, volume, torch and settings-panel operations.
- Unsupported workspace destinations remain visibly marked SOON; no fake editor integrations or sample task records are presented.

## Validation boundary
- CI verifies compilation and unit tests only. Physical Android testing remains pending.
- No backend, Render, production Supabase, migration, deploy or merge was performed.
- Android `main` remains untouched.


## 2026-11-02 — UI Agent event-aware re-observation (implementation step)

Branch: `feature/memory-intelligence`

- Replaced the fixed 300 ms post-action delay with a bounded accessibility-event settle wait (50 ms polling, 220 ms quiet window, maximum 1.2 s).
- Accessibility revision tracking now includes window-state, windows-changed, content-changed, scrolled and text-changed events from foreground apps, including app switches; event text is never retained or sent.
- The settle wait always releases on timeout and triggers a fresh observation. A timeout or missing event is explicitly logged as unverified; it is not treated as proof that an action succeeded.
- STOP/generation state is checked again before the next planner step.
- This is event-aware re-observation, not deterministic semantic outcome verification. The current `DONE` response still requires manual screen verification; automated goal-success proof remains open.
- Tests/build/CI and physical device behavior have NOT been run for this step. No CI polling was performed.
- Scope: Android feature branch only. Android `main`, Hermes/Render, backend and production data were not touched.


## 2026-11-02 — UI Agent visible completion-evidence gate

Branch: `feature/memory-intelligence`

- `UiAgentPolicy.Action` now carries `completionEvidence`; `DONE` is rejected unless the evidence is nonblank, at most 120 characters, and exactly matches a label in the latest observed accessibility/OCR label collection (case-insensitive).
- Planner JSON schema and instructions now require an exact, current, non-sensitive visible label that directly indicates the requested outcome. Invented/paraphrased evidence is not accepted.
- `DONE` audit/UI messaging now reports a visible-label match and explicitly says it is NOT final proof of success; user must manually verify the actual result.
- Added four policy regression tests: missing evidence, exact visible match, invented/paraphrased evidence, and overlong evidence. Test source currently contains 32 `@Test` methods.
- Commits: policy `76189b6260d63a1094504a1e4b544d5498efd29b`; planner/runner `d761c45290f9957134129f58e5c8060b1e873d75`; tests `9b43738a1065e9c8827088fdee6ae318703f2c7f`.
- Tests/build/CI have NOT been run. These commits are source changes only; Kotlin compilation is unverified.
- Limitation: exact label matching is a conservative evidence gate, not semantic verification. A visible toast/status label may be stale or unrelated; phone validation and stronger task-specific outcome checks remain open.
- Next fixed work: add deterministic regression coverage for stale-screen/app-switch rejection, STOP, grant expiry and Android action failure; inspect/implement any missing enforcement. Then run the agreed automated verification before physical phone testing. Memory Intelligence stays paused until Android Control is verified.
- Scope: Android feature branch only. No Android `main`, Hermes/Render, backend, production DB or deployment changes.

## 2026-11-02 — UI Agent stale-screen and execution-gate regression hardening

- Centralized freshness comparison in `UiAgentPolicy.isFreshScreen`: the observed revision must be nonnegative and unchanged, and the observed foreground package must exactly match the current foreground package. Accessibility text/node/gesture and OCR execution paths now use this check.
- Centralized execution gating in `UiAgentPolicy.mayExecute`: generation must still match, STOP CONTROL must be OFF, and any session grant must be strictly unexpired (`now < expiry`). MainActivity uses this gate for task completion display, STOP/generation checks and grant-expiry checks.
- Android action success is now passed through `UiAgentPolicy.actionAccepted`; a false result remains terminal and cannot increment progress or trigger a blind retry.
- Added six tests for stale revisions, foreground app switches, exact fresh screen matching, STOP/generation invalidation, grant expiry boundary, and failed Android action handling. `UiAgentPolicyTest` now contains 38 `@Test` methods.
- Implementation commits: policy gates `1b0a62c4858dfc02626cc0770da832dcc7a91614`; AccessibilityService freshness wiring `5b2f0a3c103a8b83133ffb540377d25f9e5373b5`; failed-action gate `94d655f23ea28e4eda790de52b50043c7f7ace96`; regression tests `ba7c5426b6859dee24effef8d9e079d6f57676de`; MainActivity execution-gate wiring `adedfded7d779fe1e841bc6339a34e58ca6ead97` and `b1c2ac1db6d79d0cfdb34b1fa37f652219d3ffac`.
- Tests/build/CI have NOT been run; Kotlin compilation remains unverified. Physical phone testing remains pending.
- Scope unchanged: Android feature branch only. No Android `main`, Hermes/Render, backend, production database or deployment changes.

## 2026-11-02 — Reject stale UI Agent planner responses

- Captures the screen revision and foreground package with the accessibility tree before OCR/planner work begins.
- When the model response returns, MainActivity re-reads the current screen observation and rejects the entire response if either revision or foreground package changed. This applies before policy validation, including DONE completion evidence; no stale action or stale DONE is accepted.
- Added policy regression tests for revision changes, app switches and unchanged-screen acceptance.
- Commits: tests `8be3656489efd3d4f0922f0da845b22bda330c56`; policy `a450eaeb77741bd72dfd67e08fa7c72ea93e4196`; MainActivity `f5fef099115ab83327a0bb95f411963882306568`.
- Test/build/CI have not yet been run for these commits. Phone testing remains pending.
- Scope: Android feature branch only. No Android `main`, Hermes, Render, backend or production data changes.


## 2026-11-02 — Android Control diff audit + freshness regression fixes

Branch: `feature/memory-intelligence`
Base reviewed: `feature/u1-live-orbit-tasks`
Scope: Android source, tests and Android workflows only. Android `main`, Hermes, Render, backend and production data were not changed.

### Diff audit
- Reviewed the 108-commit feature-branch delta and its 26 changed-file summary. Focused source audit covered `MainActivity.kt`, `UiAgentPolicy.kt`, `VyomAccessibilityService.kt`, `UiAgentPolicyTest.kt`, `vyom-android-ci.yml`, `vyom-android-debug.yml` and Gradle app configuration.
- Existing guardrails verified in source: exact observed node/OCR fingerprints, bounded on-screen gesture bounds, OCR sensitive-text filtering, explicit confirmation for OCR swipe/drag, node gesture confirmation, allowlisted app launches, STOP/generation checks, five-minute grant expiry, 40-action cap, and terminal handling of Android action failure.
- Audit found two freshness gaps:
  1. The planning revision was captured after accessibility-tree traversal, so a transition during tree capture could make a stale tree appear current. Fixed by snapshotting before traversal and rejecting if revision/package changes during traversal.
  2. Accessibility events from Vyom's own active app window were ignored together with its overlay events. Fixed by tracking same-package events only when their window ID matches the active target window, while ignoring the separate overlay window.
- Added regression coverage for tree-capture transitions and own-app target events versus overlay events. `UiAgentPolicyTest` now contains 42 `@Test` methods.
- OCR tap/long-press/swipe/drag paths retain the current OCR revision/package gate and bounded target checks. Gesture dispatch return values remain terminal action acceptance signals, not proof of semantic task success.

### New commits
- Tree-capture regression test: `90499b9d4d27da8728282f562b6f35a150c53cc3`
- Tree freshness policy helper: `409c422ea063c529ab8d5f3e739431d82e168685`
- Tree capture/revision wiring: `aa17597efe01c9fcf99258c3d93fd31ba39a7b8a`
- Own-app event regression test: `17c21e246ed17e0d8dad7213ae72e1f0d50f4814`
- Own-app event policy: `1aeff1a229f7fd17e1d568783124b64a0259e165`
- Accessibility event window filtering: `9a2e507981b6ea6c7a36405ec563b1cbd06d2eba`

### Verification state at this handoff update
- Automated test/build runs have been triggered by GitHub Actions for the implementation commits. Exact final-head result and artifact must be recorded after the documentation commit triggers its own runs.
- No local Gradle execution or physical-device test was performed in this environment.
- Do not mark Android Control verified until exact-head CI, APK artifact and user phone testing are complete.
- Memory Intelligence implementation/verification remains paused until Android Control verification.


### Additional pre-dispatch freshness hardening — 2026-11-02
- Audit found a narrow race between planner-response validation/owner confirmation and the actual Android action dispatch. Added a final current-window + observed-revision check immediately before dispatch; stale actions, including OPEN_APP, WAIT and DONE, are rejected before execution.
- Added regression test for revision change and foreground package switch between validation and dispatch. `UiAgentPolicyTest` now contains 43 `@Test` methods.
- Commits: test `65ad169d567319dee0049027b8d45dcb663a3c73`; policy `723528ccdbcf68595881176372504234ebb64232`; service `45133c45b7bc961577b2a858f80bd692a56c6b1e`; MainActivity `e5651fc41cce4cfc9d07ef93ee07d3c805c637bb`.
- Exact-head CI/build verification remains pending and will be run against the final documentation head.


### APK artifact checksum-manifest correction — 2026-11-02
- Inspected debug artifact from pre-dispatch-gate commit `a973043` and confirmed APK ZIP integrity; the APK's SHA-256 matched the checksum value, but the manifest referenced `output/<apk>` even though GitHub artifact extraction places files at archive root.
- Corrected both Android CI and Android Debug workflow checksum generation to write a basename-relative manifest from inside the `output` directory. This makes `sha256sum -c` work directly after artifact extraction.
- The inspected `a973043` artifact is diagnostic only and is NOT the final deliverable because it predates the pre-dispatch freshness gate. Final artifact must come from the latest exact-head debug run.


### Exact-head CI + APK artifact verification — 2026-11-02
- Verified branch head before this handoff entry: `e23cd526f92f0b4d4697e6f14c0c59363c4d710e`.
- Android CI run `41389700998`: SUCCESS; secret scan, `gradle testDebugUnitTest assembleDebug --no-daemon`, packaging and artifact upload all passed.
- Android Debug run `41389700994`: SUCCESS; `gradle testDebugUnitTest assembleDebug --no-daemon`, packaging and upload all passed.
- Downloaded artifact `vyom-0.3.0-e23cd526f92f0b4d4697e6f14c0c59363c4d710e` (artifact ID `13115894853`). Extracted APK is a valid Android package, versionName `0.3.0`, versionCode `3`, commit `e23cd52`, 94,965,425 bytes. Checksum manifest verified successfully: `dbfea320cd8871c1a41745073f959a5956995f854b24dd27dfd223c1509e15df`.
- `UiAgentPolicyTest.kt` has 43 test methods. Full Gradle unit-test task passed in both workflows.
- Physical phone testing is still pending and is the remaining Android Control acceptance gate. Do not resume Memory Intelligence until the user confirms the APK passes real-device testing.
- This entry is documentation-only; because it advances the branch head, run Android CI and Debug once more against the resulting exact commit before treating its APK as the final handoff.


### Android Control follow-up — OCR display mapping + raw-action freshness — 2026-11-02
- Found a visual-control coordinate assumption: ML Kit OCR boxes were treated as display pixels even though they originate in screenshot-bitmap pixels. OCR actions could be misplaced when screenshot and display dimensions differ.
- Added screenshot width/height to each OCR observation, a shared coordinate mapper that scales valid bounds and rejects invalid dimensions or portrait/landscape mismatches, and a single fresh-screen-checked mapped-bounds path used by OCR tap, long-press, swipe and drag.
- MainActivity now applies the same mapped coordinate validation before exposing OCR targets to the planner.
- Hardened legacy raw accessibility long-press, swipe and focused-field typing helpers: they now require an allowed current app and a fresh observed screen; raw gestures must remain inside display bounds.
- Added regression tests for scaled OCR coordinates, invalid/out-of-range boxes, orientation mismatch, and raw gesture bounds. `UiAgentPolicyTest.kt` currently has 47 `@Test` methods.
- Commits: OCR mapping tests `80362c4ce0a6572cf7305b6c3b420953f440915e`; mapping policy `323f8104d249b466c08f1a03aa1019b57ed92f57`; OCR service `c647881c927af9b9173697e6636b8cebd3f2a08a`; planner-bound validation `e8c6b6f7cbc9254e7ac164de864259d6cf724c33`; raw gesture regression `957e61c6fd511fe25b3b326b93600765540fd338`; gesture policy `b8fe447438bea077b720162c7e17682de775cd68`; service freshness hardening `d4c2b6936a86a499fb0789dfac1e523f4099ad6f`.
- Exact-head CI and debug build are required after this sequence. No phone test has been performed. Memory Intelligence remains paused until Android Control implementation is reported complete.


### Android Control agent-loop wait lifecycle — 2026-11-02
- Found that planner `wait` actions were previously treated as immediate success and fell through the generic screen-settle timeout (at most 1.2 seconds), so asynchronous UI operations had no intentional bounded wait contract.
- Added planner `wait_ms` with a strict 250–8000 ms range. The UI Agent now waits asynchronously on the main handler, then re-observes rather than blocking the UI thread.
- Wait resumption is gated by generation/cancellation, STOP CONTROL, session-grant expiry, accessibility-service availability and unchanged foreground package. If the user switches apps during the wait, the agent stops and does not act on the new app.
- Added regression tests for valid/invalid wait durations and resume denial on cancellation, STOP, grant expiry, app switch and missing package identity. `UiAgentPolicyTest.kt` currently has 49 `@Test` methods.
- Commits: bounded wait test `bdf088d16667e83c88ceff982ca5cd16eb06f3ef`; wait policy `d816afa1d603024089485ea0665a06013ee72e28`; async loop `0402b889a57088ae38939d7d05ad272e403853c3`; fail-closed fallback `08ff5c60808e42bd205f7bb75752f4bb8c0eda6d`; wait resume tests `7697922e0224f01e7edc901d8a0ef9cb670e91a1`; resume policy `358a237ea795d2331ca57eb84b7705f6a186107a`; loop gate `00ede7b9b7a598fce69488b2f9aaf80f669d7d48`.
- Exact-head CI and APK build remain required after the documentation update. Physical phone testing remains intentionally deferred until Memory Intelligence and the supplied design are integrated.


### Wait-window foreground transition protection — 2026-11-02
- Closed an app-switch-away-and-back race during a bounded WAIT: checking only the final foreground package could miss a transient switch if the user returned to the original app before the timer expired.
- Accessibility service now increments a foreground transition epoch whenever tracked accessibility events move between packages. Screen observations carry that epoch; wait continuation requires both the original package and original epoch to remain unchanged.
- Added regression coverage proving a same-package final state is still rejected after a transient foreground transition. `UiAgentPolicyTest.kt` currently has 49 `@Test` methods.
- Commits: regression test `eb5fed7656098088c32858dff5d32ab78764a4a8`; policy `d900e186d35ab846253bbd78ec8de33549a1521f`; service epoch `e6d33f46b1fbf0afad9cc001bbb96f0029e13c23`; MainActivity resume gate `760a4496c65dfb7051edb8b5674c8037667254de`.
- Re-run full unit tests and debug build on the final documentation head before reporting this batch complete.


### OPEN_APP final dispatch freshness gate — 2026-11-02
- The shared UI Agent dispatch gate already checked freshness, but app launch then crossed into ActivityManager through a separate helper that did not repeat the observed-screen check.
- OPEN_APP now revalidates the original foreground package and screen revision immediately inside the launch helper, alongside STOP CONTROL and per-app access checks. A transition in the narrow gap is rejected before startActivity.
- Added an explicit OPEN_APP freshness regression case. `UiAgentPolicyTest.kt` currently has 50 `@Test` methods.
- Commits: regression `a0f9f1543390308e5af79d5d641c63642e30111c`; final launch guard `b7b1bba791bc5797e4941c07c0d9081e624f49a7`.
- Final exact-head CI and APK build must run after this documentation update.


### App access least-privilege defaults — 2026-11-02
- Diff audit found first-run setup automatically enabled accessibility access for every installed non-financial app and turned the master switch ON. That contradicted the stated per-app permission model and was too broad by default.
- Fresh installs now start with MASTER APP CONTROL OFF and an empty enabled-app set. Users must explicitly enable the master switch and selected apps; the existing explicit ENABLE ALL action remains available. Existing initialized user choices are preserved and are not silently reset.
- Added a regression test for first-run deny-by-default behavior. `AppAccessPolicyTest.kt` now has 5 tests.
- Commits: regression `4793f64c29722d7d4ff2bd5b6bd5ee4591118699`; safe default policy `36d7c7802698612223c12b4d6a7536758609077c`; MainActivity initialization `d321c0f93d7db02c3fcde702000e904bcb3bd0d7`.
- This is a fresh-install default change only; already-initialized app permissions remain unchanged. Exact-head test/build verification is pending.


### Android Control compile regression correction — 2026-11-02
- Exact-head CI (`41391832674`) and Debug (`41391832630`) both failed at Kotlin compilation, not at tests: `MainActivity.kt:1139` passed a removed `installedPackages` argument to `AppAccessPolicy.initializeForInstalledApps(context)`.
- Removed the obsolete launcher-package enumeration and aligned the caller with the deny-by-default initializer. This does not alter existing stored user choices; fresh installs remain MASTER OFF with no packages enabled.
- Fix commit: `7ea3844d357e3d97a14865c012e1363c1cd6e524`.
- Must rerun both workflows against the resulting exact branch head. No APK from the failed head is valid.


### Exact-head CI and debug artifact verification — 2026-11-02
- Final branch head at this checkpoint: `0d3d22f1c9975a3ce45fe6bbcb343f471a0f2550` (`feature/memory-intelligence`).
- CI run `41392493967`: SUCCESS on this exact SHA. Secret scan passed; `:app:testDebugUnitTest` passed; `:app:assembleDebug` passed; packaging and artifact upload passed.
- Debug run `41392493977`: SUCCESS on this exact SHA. `:app:testDebugUnitTest` and unsigned debug APK build passed; versioned APK, checksum file and build-info were packaged and uploaded.
- Debug artifact: `vyom-0.3.0-0d3d22f1c9975a3ce45fe6bbcb343f471a0f2550`, artifact ID `13117953700`, 45,874,886 bytes, expires 2026-12-02. Artifact ZIP SHA-256: `80d4b08c61b6663fcb95addc7bbf732225935ec48e2e8f46fcf87b23471774c7`. This is the ZIP digest, not the APK's internal checksum.
- CI artifact: `vyom-phase6-0d3d22f1c9975a3ce45fe6bbcb343f471a0f2550`, artifact ID `13117758385`, 45,874,875 bytes, expires 2026-12-02.
- Current Android policy unit-test source has 50 UI Agent test methods and 5 AppAccessPolicy test methods. The CI job ran the Gradle unit-test task successfully.
- Scope audit performed in this pass: first-run app access is deny-by-default; existing stored choices are preserved; blocked finance/payment identifiers remain denied; STOP CONTROL is checked in the accessibility service and action loop; accessibility gestures require fresh observations and bounded screen regions; OCR actions require matching fresh OCR fingerprints and safe mapped bounds; WAIT and OPEN_APP have dedicated freshness/continuation gates.
- Remaining audit limits: this is source review plus automated unit-test/build evidence, not physical-device proof. Android framework gesture behavior, overlay visibility, OCR coordinate alignment across device-specific display modes, and app-switch/permission-revocation behavior still require device validation. Do not claim Full Android Control production-complete until these limitations and remaining cross-file audit are closed.
- The earlier compile-fix entry above is historical; its rerun requirement is satisfied by the two green exact-head workflows listed here.


## 2026-11-02 — Memory Intelligence retrieval and context hardening (verified)

Branch: `feature/memory-intelligence`

### Exact-head verification
- Source head before this documentation-only update: `9a9536bcca2bc9be9dbcbb9eb30508d402cd9fe7`.
- Android Debug run `41398310496`: SUCCESS on exact head. `:app:testDebugUnitTest`, `:app:assembleDebug`, versioned packaging and artifact upload all passed. Build log: `BUILD SUCCESSFUL in 3m 13s`.
- Debug artifact: `vyom-0.3.0-9a9536bcca2bc9be9dbcbb9eb30508d402cd9fe7`, artifact ID `13120811235`, 45,878,849 bytes, expires 2026-12-02. Artifact ZIP digest: `d7bab97828abad399ec5e5981d1f617d2e6f79416bad5ad984e4bee5af2e50c3`.
- Android CI run `41398310583`: SUCCESS on the same exact head. Secret scan, unit tests, debug APK build, packaging and upload all passed. Build log: `BUILD SUCCESSFUL in 3m 6s`.
- CI artifact: `vyom-phase6-9a9536bcca2bc9be9dbcbb9eb30508d402cd9fe7`, artifact ID `13120636678`, 45,878,842 bytes, expires 2026-12-02. Artifact ZIP digest: `5729d5bf934132c9dd72655b406af9a53d4bbef97bf7f7e37cd9af4579d72b92`.
- Gradle unit-test task reported success in both workflows. The suite contained 126 tests at this checkpoint; focused files: `MemoryCapturePolicyTest.kt` 21 tests and `ChatContextPolicyTest.kt` 6 tests.
- These are unsigned debug artifacts, not signed release APKs. Physical device behavior and installation continuity remain unverified.

### Memory retrieval improvements
- `MemoryCapturePolicy.relevantFacts` now identifies conflict categories across response style, language, theme, output format, tone and execution consent.
- For each detected conflict category, only the newest fact is included in the model context. Older facts remain in encrypted local storage and remain editable/deletable in the Memory screen; they are not erased by retrieval.
- Conflict ranking is evaluated before applying the retrieval limit, so an older high-overlap fact cannot displace the latest preference when `limit=1`.
- Added cross-script Bengali normalization: Bengali `বাংলা` aligns with English `Bangla/Bengali`; `ভাষা/ভাষায়` aligns with `language`; Bengali Hindi/English terms are normalized to their English labels.
- Regression tests cover newest-conflict-only prompt retrieval, limit-one recency, cross-worded response style, tone, execution consent, and cross-script Bengali language lookup.
- Important limitation remains: this is bounded synonym-aware lexical matching, NOT embedding/vector semantic search. Supported extraction/retrieval coverage is explicit English, Hinglish and Bengali patterns; it is not general multilingual understanding.

### Context budget and long-reply source audit
- `ChatContextPolicy.buildContext` now keeps explicit memory/history section labels and preserves complete newest lines when the context budget is tight; if the newest line alone exceeds its budget, its suffix is retained.
- Context remains bounded (production call currently uses 1,200 characters); current user message is appended separately and is not folded into that budget.
- Added tests for section-label retention, newest memory/history line retention, zero/negative budget behavior and the existing maximum-context bound.
- Source audit of `VyomApiClient.request` found the response body is read with `readText()`; no fixed response-character cap was found in that path. Chat rendering sets the full reply string on a selectable TextView and no fixed reply `take()`/substring truncation was found. This is source inspection only, not proof of full long-reply rendering on every Android device.
- Markdown rendering currently handles headings (`#` through `###`), bold, italic, inline code and fenced code blocks. It is a lightweight renderer, not full CommonMark; tables, nested lists and other Markdown extensions are not guaranteed.

### Verification history and resolved intermediate failures
- Context truncation regression initially failed on old verbose section labels; fixed by shorter labels and a 110-character boundary test.
- Cross-script Bengali regression was added before its normalization fix; the fix is in commit `8e7310f280098c9b817d6336cf84c88b4e71d943`.
- Conflict suppression was implemented in `ecd10280de9b73f7df58c4820aaf973951eacf48`; the first run then correctly exposed seven stale expectations that still asserted older facts should be returned. Assertions were updated to require newest-only results in `9a9536bcca2bc9be9dbcbb9eb30508d402cd9fe7`; exact-head CI and Debug are green as recorded above.
- Earlier retrieval limit-one regression fix `2cd72ebb029253f550149588a073ae2a3153a2c8` had exact-head CI run `41396704217` and Debug run `41396704380` both SUCCESS. This newer checkpoint supersedes that artifact.

### Remaining Memory Intelligence blockers
- Supabase was inspected read-only: one project `Vyom-Hermes` (`kjsxsswgzyoezjuzpfqm`), no staging branch, existing `public.memory` schema has `chat_id`, `memory`, `created_at`, no Android identity mapping, and vector extension has no installed version.
- No Supabase schema/data/extension/migration writes were made. Do not enable vector, create staging resources, or sync Android-local facts into Telegram's `chat_id` memory table without explicit approval and a validated Android identity/isolation design.
- Production vector migration, semantic embedding retrieval, staging validation and cross-device sync remain BLOCKED. Do not call Memory Intelligence fully production-complete.
- Current local encrypted memory capture, user-editable storage, synonym-aware lexical retrieval, conflict resolution and bounded prompt context are implemented and exact-head CI/Debug green.

### Design handoff
- Retrieved the supplied `Vyom — Design System, Home, Drawer, Automations (1).html` prototype (8,009,167 bytes) for the next phase. It contains the app-wide visual/system reference and screens including Home, Drawer, Automations, Chat, Tasks, Skills, Teach Vyom, Assistant control, Audit log, Permissions, Memory, Instructions, Voice, Connections, Model picker, System and Self-update.
- Design implementation is now IN PROGRESS. The branch contains the shared `VyomDesignSpec`/`VyomTheme` tokens, an integrated searchable drawer, Automations and Task Detail dialogs, and the newly added editable Instructions screen. This is not yet a complete port of the supplied prototype; each route must be audited against the HTML reference and the existing navigation before claiming completion. Preserve the dark HUD/liquid-glass direction, exact palette and typography tokens, and never present planned destinations as working features.
- Agreed order remains: Android Control implementation → Memory Intelligence local implementation → supplied app design implementation → final exact-head APK for user phone testing. Physical phone testing remains user-side and is deferred until the requested implementation phases are complete.
- Scope: Android repository feature branch only. Android `main`, Hermes/Telegram, Render, backend and production Supabase were not touched.


## 2026-11-02 — Design implementation checkpoint: Instructions screen

Branch: `feature/memory-intelligence`
- Added `VyomInstructionsPolicy`: persona language/tone instructions for Hinglish, English, Hindi, Formal and Fun; custom rules are normalized, capped at 1,000 characters and rejected if the sensitive-data detector flags them.
- Added `VyomInstructionsDialog`: editable persona chips, Jarvis/Ultron orb-role cards, custom rules editor, safety disclosure, SAVE validation and bottom navigation. Settings persist in the existing local preferences; custom rules are appended to the model prompt with an explicit safety-priority qualifier.
- Persona button now restores the saved persona label at activity launch (not only after opening/saving the dialog).
- Unit tests added in `VyomInstructionsPolicyTest.kt` for whitespace/line normalization, sensitive and oversized rule rejection, prompt-budget cap, and explicit persona instructions.
- Test inventory at commit `0e4b5826c4d12c4bab6c8ccb9dca1f2cbab9432c`: 130 `@Test` methods across 14 test files.
- Debug workflow `41400726323` on exact head `0e4b5826c4d12c4bab6c8ccb9dca1f2cbab9432c`: SUCCESS. Logs show `:app:testDebugUnitTest`, `:app:assembleDebug`, `BUILD SUCCESSFUL in 2m 33s`. Debug artifact ID `13120974209`, name `vyom-0.3.0-0e4b5826c4d12c4bab6c8ccb9dca1f2cbab9432c`, size 45,890,576 bytes, expires 2026-12-02; ZIP SHA-256 `6194fa3bd9d67038930e3576a41e1ff0e42ecd5d7c660b7e8a220ce3f12c0ff5`.
- The matching full CI run `41400726282` was still in progress at the time of this handoff edit; do not mark the checkpoint CI-green until it completes successfully on the exact documentation-updated head.
- Earlier compile failure on commit `f0e0d2b` was caused by dialog constructor parameters not being retained as properties. Fixed by making `initialPersona`, `initialRole` and `initialRules` constructor properties; exact-head Debug passed on `fd0e25e`, then saved-persona label was corrected and retested on `0e4b582`.
- Scope unchanged: Android feature branch only; do not touch Android `main`, Hermes/Telegram, Render, backend or production Supabase. Physical device validation remains deferred until implementation phases are complete.


## 2026-11-02 — Teach Vyom HUD phrase editor + exact-head APK verification

- Source branch: `feature/memory-intelligence`; source commit: `2459bf1f9da6a672532e14f4d8bc19c2448f185d` (`feat(ui): integrate HUD Teach Vyom phrase editor`).
- Replaced the basic Teach Vyom AlertDialog with the integrated `VyomTeachDialog` HUD phrase editor. MainActivity still owns the supported-action classification, local teaching persistence, STOP CONTROL check and classified action execution; unsupported/blocked commands and STOP state fail closed. Cancel does not save or execute.
- Exact-source workflows on this commit: Android Debug run `41401506345` SUCCESS; full Android CI run `41401506346` SUCCESS. Both completed on exact SHA `2459bf1f9da6a672532e14f4d8bc19c2448f185d`. The Debug job completed successfully; the full CI job (including secret scan, Gradle unit tests, debug APK build, packaging/upload) completed successfully.
- Debug artifact: `vyom-0.3.0-2459bf1f9da6a672532e14f4d8bc19c2448f185d`, artifact ID `13121522180`, 45,890,976 bytes, expires 2026-12-02. Downloaded and extracted APK: `vyom-0.3.0-2459bf1f.apk`; versionName `0.3.0`, versionCode `3`, embedded commit `2459bf1`. APK SHA-256: `b82cc7d62ae4cd0280827068bc827e910f89229aba52899c0cc58aad20a5cd27`; packaged checksum manifest matched the extracted APK. APK archive integrity test passed.
- Test inventory remains 130 unit-test methods across 14 test files (the Gradle unit-test task passed on this exact head). This is automated build/test evidence, not physical-device runtime validation.
- Artifact is a debug APK, not a signed production release. Installed-app signature continuity and phone installation/runtime remain unverified; preserve the existing installed app until signature compatibility is checked. Physical phone test is now the user's next acceptance step.
- Memory Intelligence limitations remain: synonym-aware bounded lexical retrieval, not vector/embedding semantic search; production Supabase migration/staging and cross-device sync remain blocked and untouched.
- Scope confirmation: Android feature branch only. Android `main`, Hermes/Telegram, Render, backend, production Supabase, secrets and deployment untouched.


# EVIDENCE-BASED PRODUCT GAP AUDIT — 2026-11-02

- Audit checklist: `docs/VYOM_COMPLETION_CHECKLIST.md` (commit `d09457e37765736500354a12e7dab7742da6f6b3`). This file separates source-confirmed implementation, missing wiring, unverified device behavior and locked implementation order.
- Critical source finding: `MainActivity.showUiAgentPanel()` is defined but has no call site in `MainActivity.kt`; the UI-agent entry point is unreachable from current navigation. The UI-agent implementation itself exists, but this is not equivalent to usable full Android control.
- Critical command-routing finding: `ActionEngine.Operation.INSTALL_PLAY_STORE` is routed to `openPlayStoreSearch()`; that method only launches Play Store search and explicitly tells the user to tap Install. It does not invoke the UI-agent loop or verify installed package/version. This is an application workflow gap, not proof of an Android system-level prohibition.
- Readiness blockers: `AppAccessPolicy.initialSnapshot()` sets master access OFF and package allowlist empty by default; Accessibility service is optional; UI-agent planning requires an NVIDIA API key. There is no single readiness report for these prerequisites.
- Existing control capabilities: Accessibility tree + local OCR, bounded node/OCR tap/long-press/swipe/drag, scroll, type exact goal text, Back, bounded wait, fresh-screen/revision checks, event-settle re-observation, STOP, task-only 5-minute grant and 40-step limit. This is a bounded accessibility agent, not unrestricted Android control.
- Outcome gap: `DONE` requires an exact current visible label, but completion UI still says this is not final proof and asks the user to manually verify. Independent app-state/package/version postcondition checks remain required.
- Task gap: immediate server tasks exist and server execution is serial; Automations is a live task queue, not a time-based/repeating scheduler. No local durable scheduler/background recovery is verified.
- Teach/skills gap: Teach Vyom is a local phrase/action mapping feature; the local Skills catalogue currently lists four native skill groups. UI Agent is not exposed as a reachable registered skill.
- Memory gap: encrypted local facts, max 30, bounded extraction and synonym-aware lexical retrieval exist; embedding/vector semantic retrieval, conversation summaries, source-grounded local knowledge library and safe sync are not complete. Production Supabase remains untouched.
- Self-update gap: current UI requires the user to paste a complete Kotlin file. Client only creates a proposal branch/PR; no code generation, test orchestration, merge, signed release or install verification exists. Keep safety-critical policy/STOP/accessibility controls protected from autonomous merge/install.
- No Android source was changed in this audit-only checkpoint. No new test/build/CI result is claimed. Continue on `feature/memory-intelligence`; do not touch Android `main`, Hermes, Render, backend, production Supabase, secrets or deployment.
- Next implementation gate: first add RED tests for UI-agent routing/readiness and installation outcome state; then wire the UI-agent entry point and command routing. Preserve Android system approval requirements and verify exact installed package/version before success is reported.


## 2026-11-02 — Memory retrieval + Play Store install policy regression fixes

- Source branch: `feature/memory-intelligence`; source changes: `7afdfc6` (pass the requested UI-agent goal into the outcome verifier), `ccc8a5a` (fix both UI-agent dispatch paths), `fe87d49` (reject Play Store Install taps when the foreground package is not the official Play Store), and `601cc15` (remove over-broad memory synonym equivalence that caused unrelated facts to match).
- Exact-head run pair on `ccc8a5a`: Debug `41410130449` FAILED and full CI `41410130480` FAILED. Kotlin compilation succeeded; unit tests completed 144 tests with 2 failures: `MemoryCapturePolicyTest.projectRetrievalUnderstandsRepositoryBranchAndBuildArtifactSynonyms` and `UiAgentPolicyTest.explicitPlayStoreInstallIntentRequiresConfirmationForWrongListingOrWrongButton`.
- Failure causes were addressed: broad `active/current/working/development` token equivalence caused false-positive memory retrieval; the TAP_TEXT Install branch could fall through to ordinary allow on a non-Play-Store package. The revised policy now rejects Install taps outside `com.android.vending`; an unverified listing in the official store remains confirmation-required.
- New exact-head Debug run: `41432570461`; full CI run: `41432570466`; both target `601cc1547fd7ef5af7a58a83d2a9e06fc100d092`. At the time of this handoff update both were still running; no passing result or APK artifact is claimed. Recheck exact-head runs and artifact before any APK handoff.
- These changes do not yet wire the UI Agent into Control navigation or route the Play Store install intent through the UI-agent loop. The checklist's two first implementation gates remain open; do not claim full Android control or verified installation.
- Scope remains Android feature branch only. Do not touch Android `main`, Hermes/Telegram, Render, backend, production Supabase, secrets or deployment. Physical phone acceptance remains pending.


## 2026-11-02 — Video-repo audit, VoiceStudio/Higgsfield integration design, and UI policy regression

- Supplied 49.7-second video inspected. Repositories identified: OpenHiggsfield, VoiceStudio, OpenSEO, AppFlowy, OpenWhisper, OpenProject, Cal.com/Cal.diy, Chatwoot and Cap.
- Added `docs/VYOM_AUTONOMY_AND_CREATIVE_STUDIO.md` at `64827f9`: repository-by-repository fit/licence/platform/cost review; repo/ZIP learning architecture; local voice design; Creative Studio; Termux worker boundary; high-level planner/capability manifest; controlled self-update; UI/UX destination map.
- Extended `docs/VYOM_COMPLETION_CHECKLIST.md` at `53f6335` with repo/ZIP learning, skill provenance, media, VoiceStudio, Creative Studio, Termux and task-engine acceptance tests. These are pending requirements, not completed features.
- Key repository findings:
  - `thehiggsfieldai/OpenHiggsfield` is an independent MIT-licensed self-hostable story studio, not the official Higgsfield platform; it requires Node 24+, Docker Compose, FFmpeg and provider credentials/costs.
  - Official `higgsfield-ai/skills` and `higgsfield-ai/higgsfield-js` are useful for agent media workflows; SDK V2 is server-side only and needs provider credentials/credits. Never bundle provider secrets into a WebView or APK.
  - `debpalash/VoiceStudio` has an Android 10+ Preview app that runs local models offline after download. Its mobile manifest documents SEND intents and a workspace URI, but no proven callable TTS service contract has been established. Do not claim it already voices Vyom. Evaluate on-device and prove IPC before integration. VoiceStudio app is AGPL-3.0; model licences differ.
  - OpenWhisper is desktop-first; AppFlowy has Android support; OpenProject/Cal.diy/Chatwoot are optional external services; Cap is desktop-first and useful as a recording/editor UX reference; OpenSEO is an optional SEO/MCP skill with DataForSEO usage costs.
- Existing code audit: `TtsProvider.kt` currently prefers ElevenLabs when configured and falls back to Android TTS. A VoiceStudio adapter is not implemented. `VyomDesignSpec.kt` still marks several media drawer destinations disabled; `VyomDrawer.kt` still displays SOON labels for disabled destinations. UI Agent is still not wired as a fully operational Control entry path; existing checklist remains authoritative.
- Build regression: exact-head Debug run `41454344720` on `de2d633` failed 1/144 tests: `UiAgentPolicyTest.requestedInstallModeNeverAuthorizesUnrelatedConsequentialActions` (line 435). First attempted guard rejected all consequential buttons and contradicted the expected CONFIRM behavior for Delete. Corrected narrowly at `1968498`: reject the Buy button in requested-install mode while preserving the test's expected guarded behavior for Delete. Do not weaken tests.
- Debug/full CI pair on `1968498`: `41454866085` and `41454866090` were running at handoff time. New documentation commits followed; run results for the exact current HEAD `53f63351f42c77892a60262525b9cd9d643e74e4` are not yet verified. No APK artifact is claimed.
- No third-party application source has been copied into the APK in this checkpoint. No Android main, Hermes backend, Render, production Supabase, secrets or deployment changes were made.
- Next: verify latest exact-head Debug + full CI; fix any remaining failures; then implement UI Agent readiness/Control wiring and install verification before moving into repository learning and VoiceStudio/Creative Studio adapters. Physical-device testing remains pending.


## 2026-11-02 — Exact-head CI, APK checksum and persistent signing investigation

- Android source branch remains `feature/memory-intelligence`. Latest source/workflow commit before this handoff note: `266009e3cd8a0957b66722ffd99b67ae672bc00e`.
- Exact-head Debug + full CI at `7e70370d6d9d134f3fa4ba0878636a3f658f275f`: Debug `41455029509` SUCCESS; CI `41455029497` SUCCESS. 144-test suite and unsigned debug APK packaging succeeded.
- Downloaded artifact `13146103223`; ZIP digest matched GitHub's SHA-256 `b8b3a1ac6c067b41f4d5edaa5a073641be6d03daafc4569096b6435199243c23`. Embedded APK SHA-256 independently matched `6aa6244ed2a8511234c0103c95a36994ca2df76f514695e19a569f5b03562406`. APK version `0.3.0`, versionCode `3`, commit `7e70370`.
- APK signing cert comparison against the earlier `2459bf1` debug APK found different cert fingerprints (`8ebde42b...` vs `bf4132c9...`). The `7e70370` debug APK is therefore not a safe in-place update for that prior installation. Do not advise uninstalling before local-data backup/migration.
- Added optional persistent release signing to `.github/workflows/vyom-android-debug.yml` at `54ff5b6`. First run failed because the Base64 secret contained formatting/URL-safe encoding; normalization was added at `4b9ecad`. That run decoded and listed the keystore but `assembleRelease` failed because the configured key password did not unlock the selected alias.
- Added key-password validation and safe fallback to the store password only when a CSR operation proves it unlocks the alias (`1874875`, repeatability correction `266009e`). No secret values are logged or exposed.
- At the time of this note, exact-head Debug run `41457214306` and full CI run `41457214252` on `266009e` were in progress/pending. Signing configuration step succeeded, but the APK packaging and certificate output are not yet verified. No signed APK artifact is claimed.
- New integration specification and expanded acceptance checklist are recorded in `docs/VYOM_AUTONOMY_AND_CREATIVE_STUDIO.md` and `docs/VYOM_COMPLETION_CHECKLIST.md`. They are specifications, not completed runtime features.
- The supplied video repositories were audited; no third-party source has yet been copied into the app. VoiceStudio companion automation, Higgsfield provider execution, repository/ZIP learning, Creative Studio, background task orchestration and self-update remain implementation work.
- Scope unchanged: do not touch Android `main`, Hermes/Telegram, Render, backend, production Supabase, secrets or deployments. Do not deliver an APK until exact-head Debug + CI, artifact checksum and stable signing certificate are verified.
