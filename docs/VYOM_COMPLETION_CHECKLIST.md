# OWNER RESET MILESTONE PLAN — 2026-11-03

The controlling milestone order and branch/safety constraints are recorded at the top of `docs/HANDOFF.md` and `docs/MASTER_ORDERS.md`. Milestone 1 order: P1 Power Mode; A12 structured tool calls/no raw JSON; A14 TRUSTED default/no blanket confirmations; A1 verified Play Store install; verified whatsapp_send skill; A8/A16 floating orb during control tasks; F4 persisted memory auto-capture visible in Memory; R4 Diagnostics/RUN ALL CHECKS. M2 WebView exact supplied Claude Design HTML with scoped Kotlin bridge starts only after M1. M3 Learn Anywhere, ledger, attachments, real automations, human-like voice, floating bubble. Former one-final-APK rule is cancelled: one verified APK per milestone.

# Vyom Completion Checklist — Evidence-Based Audit

> Scope: Android app only. This is a source audit and implementation acceptance checklist, not a claim that these features are complete.

## Locked constraints
- [ ] Work only on `feature/memory-intelligence` (or an explicitly approved feature branch).
- [ ] Do not touch Android `main`, Hermes/Telegram, Render, backend services, production Supabase, secrets, or deployment.
- [ ] Preserve existing pairing/chat, local memory, STOP CONTROL, allowlist, and sensitive-action protections.
- [ ] Every behavior change follows RED → GREEN → REFACTOR; no "tested" claim without actual test output.
- [ ] Final APK requires exact-head Debug + full CI success, artifact download/integrity/SHA verification, and signing-certificate comparison before replacing an installed app.
- [ ] Physical-device acceptance remains a separate user-side gate.

## Linked source documents

- `docs/ACCEPTANCE_TESTS.md` — full A1–R4 acceptance matrix and phone test guide.
- `docs/ANDROID_SIGNING.md` — required certificate fingerprint, artifact mismatch evidence and release gate.
- `docs/DESIGN_MAP.md` — all 31 supplied HTML boards mapped to native Android screens.
- User-supplied Library sources remain authoritative for the 19-page fix order and 10-page feature/design brief; these repository documents supplement them.

## Audit findings (current source evidence)

### 1. Android UI control
- [x] Accessibility service exists: `VyomAccessibilityService`; it is opt-in and user-enabled in Android Settings.
- [x] App access is fail-closed by default: `AppAccessPolicy.initialSnapshot()` sets master OFF and enabled package set empty; new packages default OFF.
- [x] UI agent loop exists in `MainActivity.startUiAgentTask()`: accessibility tree + local OCR observation, one-action planning, policy validation, bounded execution, event settle and re-observation; 40-action ceiling remains.
- [x] UI Agent is wired: drawer destinations `control` and `assistant_control` open the Control panel; its START UI AGENT button calls `showUiAgentPanel()`.
- [x] Basic capability/readiness report exists: Accessibility, STOP, master app access, current foreground package/allowlist, Play Store allowlist, NVIDIA planner key and UI Agent skill state are shown with setup shortcuts.
- [x] Explicit `INSTALL_PLAY_STORE` now opens Play Store search, waits for `com.android.vending` to become foreground, then starts `startUiAgentTask()` in `USER_REQUESTED_INSTALL` mode.
- [ ] Play Store publisher/developer identity is not independently verified, and the selected listing's package ID is not captured as the install postcondition. The current policy checks one matching title and a unique exact Install control; keep this limitation visible and do not claim a verified publisher.
- [x] Requested-install policy is constrained to a unique exact `Install` control on the Play Store screen; unrelated Buy/payment/delete/send/share actions remain blocked or require confirmation. Android system prompts and Play Protect are not bypassed.
- [x] General UI Agent is reachable from Control. Direct device-command follow-through is currently implemented for the Play Store install intent, not every device command.
- [ ] Current planner requires the configured NVIDIA API key; missing key safely returns no action. Add capability-aware fallback/clear diagnosis rather than silently claiming the task is impossible.
- [ ] Custom Canvas/timeline controls are not reliably exposed by accessibility/OCR. Add a separately gated visual-understanding capability; do not invent coordinates or bypass protected surfaces.
- [ ] UI-agent loop is Activity-owned, not a dedicated foreground task service. Background continuity/process-death recovery is not proven; add a user-visible foreground execution lifecycle and persistent task state before claiming reliable background control.
- [x] Event/revision-aware observation and bounded settle wait exist. Tree capture, planner response and pre-dispatch reject stale revisions/foreground changes.
- [x] Post-action foreground guard added in `3aa20eb`: ordinary actions stop if the foreground package or foreground epoch changes; `OPEN_APP` continues only if the requested package is actually foreground. Regression tests were added; CI execution is still pending.
- [ ] Semantic outcome verification remains incomplete: `OPEN_APP` verifies the exact foreground package and Play Store install checks PackageManager for one matching installed label/version, but generic `DONE` only proves an exact visible label, not the requested real-world effect.
- [ ] Regression matrix still needs CI/device evidence for stale screen, app switch, STOP during planner/confirmation/action/wait, grant expiry, service disconnect, API timeout, rejected action, no visible target, ambiguous target, screen lock, secure window, rotation, process death, and successful install verification.

### 2. Tasks and automations
- [x] Immediate tasks are submitted through the existing task API and displayed with status/details/cancel.
- [x] Existing UI states that tasks run serially on the server; the client enforces an active-task cap.
- [ ] No local durable task scheduler/worker is proven. Add persistent task records with queued/running/done/error/cancelled, progress, timestamps, retries, cancellation, and recovery after app restart.
- [ ] Add user-defined one-time and repeating automations using Android-supported scheduling (WorkManager/AlarmManager where appropriate), with exact permission/Doze limits disclosed.
- [ ] Add task templates, dependencies, run history, notifications, retry policy, pause/resume, and idempotency.
- [ ] Keep local tasks independent from server tasks; do not change backend/API contract in this scope.

### 3. Teach Vyom and skills
- [x] Teach phrase editor exists and is integrated; phrase/action mapping and local persistence remain in existing local logic.
- [x] Local skill registry exists, but `VyomDesignSpec.deviceSkills` currently lists only four native device skill groups: open app, volume, torch, and settings.
- [ ] Expose UI automation as a real skill with readiness, supported actions, limits, and setup requirements.
- [ ] Add a searchable skill catalogue with domain packs (Android, files, browser, media, productivity, coding, research) only when each skill has an executable adapter and tests.
- [ ] Teach flow should save user-approved examples, aliases, preconditions, steps, expected outcomes, and rollback; learn from successful/failed runs only after user approval.
- [ ] Add import/export, edit/delete, versioning, conflict handling, and tests for unsafe/ambiguous taught phrases.
- [ ] Do not treat arbitrary user teaching or screen text as trusted system policy.

### 4. Memory Intelligence
- [x] Current facts are encrypted locally; current store caps at 30 facts and filters sensitive content.
- [x] Retrieval has a small synonym map and newest-wins conflict handling for selected preference topics.
- [ ] Retrieval is lexical/synonym-aware, not vector/embedding semantic search.
- [ ] Extraction supports a bounded set of explicit remember/correction/preference patterns; it is not comprehensive conversational memory.
- [ ] Add typed memory records (preference, project, decision, task, skill, entity, correction) with source, timestamp, confidence, expiry, and user controls.
- [ ] Add conversation/project summaries and recency-aware retrieval without blindly storing whole chats.
- [ ] Add semantic retrieval and hybrid lexical/vector ranking only after selecting a phone-feasible embedding model, storage format, latency and battery budget.
- [ ] Add a local knowledge library for user-approved files/notes with chunking, source/page references, retrieval citations, and deletion propagation.
- [ ] Add conflict categories, provenance, supersession, "ask when uncertain", memory review/edit/delete, export/import, and migrations with rollback tests.
- [ ] Cross-device/cloud sync remains blocked until an isolated staging environment and identity/data-isolation design are approved. Do not write production Supabase.

### 5. Self-update and self-improvement
- [x] Current self-modification UI asks the user to paste a complete Kotlin file; it does not generate code itself.
- [x] `SelfModificationClient` creates a proposal branch and PR; it has no merge, release, or install path.
- [x] `SelfModificationPolicy` blocks MainActivity, ActionEngine, accessibility, device executor, policy, audit, kill-switch, and self-modification files.
- [ ] Replace manual full-file paste with a task-driven patch workflow: inspect → plan → generate minimal diff → run focused tests → run full CI → open review PR.
- [ ] Show changed files, diff, rationale, test evidence, risk, and rollback before asking for review.
- [ ] Keep policy/permissions/STOP/security boundaries protected from autonomous merge or install; changes to these require human review and code-owner approval.
- [ ] Add update manifest verification, package ID/version/rollback checks, SHA-256, signing-certificate continuity, staged download, and explicit Android installer handoff.
- [ ] After install, verify installed version/commit and health before marking update complete; retain a tested rollback path.
- [ ] Add self-improvement proposals from audit failures and user feedback, but never auto-edit production policy or claim a PR is an installed update.

### 6. Design and product completeness
- [x] Home, Drawer, Automations, chat, memory, instructions, skills, task detail and Teach Vyom components exist in varying degrees.
- [ ] Automations currently represents a live task queue; scheduled/repeating schedules are explicitly not connected.
- [ ] Media destinations (Animations, Videos, Images, VFX, CGI, SFX) remain disabled/planned; implement only with actual local/connected execution capabilities.
- [ ] Finish remaining supplied design screens and state variants; do not ship fake buttons or static mock features as working capabilities.
- [ ] Add accessibility labels, small-screen/large-font handling, loading/empty/error states, dark theme contrast, and phone screenshot comparison.

### 7. Verification and release
- [x] The preceding APK commit `2459bf1` had Debug and full CI workflows reported SUCCESS; artifact was downloaded and checksum-checked in the prior checkpoint.
- [ ] Current branch HEAD after documentation commits is not the same source SHA as that APK; every later source change requires new exact-head CI and APK.
- [ ] Add Android instrumentation/UI tests for capability setup, Control entry point, Play Store flow (test double/controlled test app), task persistence, Teach flow, memory controls, update verification, and STOP.
- [ ] Test on the actual Android device for Accessibility setup, app allowlist, Godot listing identification, install progress/result, background execution, reboot recovery, notifications, and STOP.
- [ ] Compare installed and candidate APK signing certificates before in-place update. If they differ, do not uninstall or overwrite the existing app.
- [ ] Final handoff must include exact source SHA, CI/debug run IDs, test count/results, APK artifact ID/name/size/SHA-256, certificate status, known OS limitations, and phone acceptance checklist.

## Implementation order (locked — 2026-11-02)
1. [ ] Stable APK signing and update continuity: exact-head signed build, artifact SHA-256, certificate fingerprint comparison with the installed/base APK. Never uninstall to work around a mismatch.
2. [ ] Full Android Control: finish UI Agent, app-switch guards, post-action outcome checks, STOP invalidation, foreground lifecycle and regression tests.
3. [ ] VoiceStudio companion automation and local/on-device voice pipeline; ElevenLabs remains optional fallback.
4. [ ] GitHub URL/ZIP/documentation skill learning: inspect, licence/security audit, extract knowledge, create reusable skills, run tests and verify results.
5. [ ] Higgsfield/Creative Studio pipeline: storyboard, scenes, animation, VFX, CGI, sound and export through guarded provider adapters; never embed provider secrets in APK.
6. [ ] Persistent task engine, local automation scheduler, retries, cancellation, background execution and artifact management.
7. [ ] Controlled self-update: reviewable patch/PR, signed update, health check and tested rollback; no autonomous policy/permission changes.
8. [ ] Implement remaining HUD screens and the supplied design system; no screen marked complete until its real action path works.
9. [ ] Full acceptance: Memory Intelligence conflict/migration/semantic validation, all Android Control and creative/voice/task acceptance tests, exact-head Debug + full CI, signed APK integrity/certificate checks, and final phone test checklist.
8. [ ] Finish remaining real design screens and UI regression.
9. [ ] Run full verification, produce exact-head APK, then hand off for physical phone acceptance.

## Non-negotiable limits
- Android may still require the user to enable Accessibility, grant app-specific access, approve Android's package installer/system prompts, or authenticate for protected actions.
- Vyom must not bypass Play Protect, payment/financial security, secure-window restrictions, biometric prompts, or Android permission boundaries.
- "Action accepted", "screen changed", "download started", "installed", and "update healthy" are different states. Only report the state independently verified.


## 8. Autonomous repository learning and creative integrations
- [ ] Chat accepts GitHub repository URLs, ZIP files, documentation, images and videos with attachment previews and processing status.
- [ ] Public GitHub repository learning reads metadata, README, tree, licence, dependency manifests, security policy and relevant skill files; private repositories use encrypted least-privilege credentials.
- [ ] ZIP import is protected against path traversal, archive bombs, excessive file counts/sizes, secret leakage and build-output noise.
- [ ] Knowledge notes and executable skills are stored separately. Every learned item keeps source URL, commit/version, licence, path/line provenance, confidence and refresh state.
- [ ] Skill import validates metadata, triggers, parameters, preconditions, required capabilities, action steps, expected result, failure recovery, risk class, dependencies and licence.
- [ ] Third-party code and install scripts are treated as untrusted; static inspection, dependency/license/security review and isolated Termux/worker execution precede running code.
- [ ] Learned card reports inspected files, saved knowledge, proposed/installed skills and rejected/skipped items with reasons.
- [ ] Support skill version pinning, refresh/diff, disable, uninstall, conflict resolution and rollback.
- [ ] Implement the video-reel repo inventory in `docs/VYOM_AUTONOMY_AND_CREATIVE_STUDIO.md`; do not embed whole desktop/web products in the Android APK.
- [ ] OpenHiggsfield story workflow and official Higgsfield skills/SDK are optional cloud-provider adapters; credentials and generation costs are explicit. No false claim of free/local Higgsfield generation.
- [ ] VoiceStudio Android Preview is evaluated on the actual phone; document and test a real callable TTS/IPC path before claiming it speaks inside Vyom. Android TTS stays the offline fallback; ElevenLabs is not mandatory.
- [ ] Creative Studio accepts reference photo/video, builds storyboard/scene/character plans, renders via available adapters, assembles media, and verifies decoded output/checksum before DONE.
- [ ] Optional Termux worker has loopback-only auth, durable task IDs, progress, cancellation/STOP propagation and background/Doze limitations documented.
- [ ] Other reel repositories are only integrated where their role is useful: OpenSEO (SEO research), AppFlowy (workspace), OpenProject (project management), Cal.diy (scheduling), Chatwoot (support inbox), Cap (screen recording reference), OpenWhisper (desktop transcription reference).

## 9. Brain, task engine and media acceptance
- [ ] One structured planner routes CHAT / THINK / TASK / DEVICE_CONTROL / RESEARCH / REPO_LEARN / MEDIA_CREATE / FILE_OPERATION / AUTOMATION / SELF_UPDATE.
- [ ] Every model call receives a truthful capability manifest with current permissions/services/skills/models/storage and configured-provider presence (never secret values).
- [ ] All tool calls execute through local typed adapters; raw JSON never appears in user chat.
- [ ] Task stack supports pause/resume, dependencies, idempotency, retry, cancel and recovery without deleting unrelated tasks.
- [ ] Every action and media job has requested/executed/verified/evidence states and a verified result artifact.
- [ ] Creative Studio has Projects, Storyboard, Characters/References, Animations, Video, Voice, Assets, Render Queue and Export destinations, only enabled when backed by real adapters.
- [ ] Voice path tests Hindi/Hinglish/English, long replies, chunking, audio focus, STOP/barge-in, background playback and offline fallback.
- [ ] Media path tests reference ingest, keyframe extraction, scene continuity, render failures, retry, cancellation, file decode, duration/resolution/audio track and checksum.
- [ ] Self-update pipeline uses current branch context, protected-file guard, tests/CI, diff review, signed artifact, certificate continuity, installer handoff and rollback.

## 10. Repo/ZIP/Creative Studio lock
Implementation and acceptance details are in `docs/VYOM_AUTONOMY_AND_CREATIVE_STUDIO.md`. This specification is not proof of implementation. Do not mark any new item complete until source code, tests and exact-head CI evidence exist.

- `docs/ANDROID_CONTROL_CAPABILITY_MATRIX.md` — owner-authorized Android permissions, implemented capability inventory, platform boundaries and acceptance gates.
- `docs/skills/JARVIS_AGENT_ARCHITECTURE.md` — JARVIS brain/hands/capability-probe/tool-gate patterns adapted as Android learning guidance.
- `docs/skills/CONTINUOUS_LEARNING_AND_VERIFICATION.md` — learn-before-guess protocol, provenance, skill validation and eval workflow.


## P1 Power Mode owner-led reconnect and audit — 2026-11-03
- [x] Add policy tests for secure-settings grant gating, component validation, preserving other enabled Accessibility services and idempotent reconnect merge.
- [x] Accessibility reconnect is available only behind an explicit owner tap; no startup/resume auto-enable path exists.
- [x] Verify the app's live WRITE_SECURE_SETTINGS grant before writing; verify ENABLED_ACCESSIBILITY_SERVICES and ACCESSIBILITY_ENABLED by read-back.
- [x] Report live Accessibility global-enabled/component state and WRITE_SECURE_SETTINGS grant in Power Mode/Control readiness.
- [x] Redact Shizuku audit events to validated request ID, allowlisted command ID, outcome and optional exit code; distinguish success, nonzero exit, timeout, STOP cancellation, interruption and failure.
- [x] Clear stale Shizuku binder/service state on binder death/disconnect.
- [ ] Exact-head CI for this implementation and the remaining P1 lifecycle test matrix.
- [ ] Physical Galaxy A06 verification of Wireless ADB/Shizuku authorization, service bind/disconnect, STOP process termination, secure-settings revocation and owner-disabled behavior. This remains deferred until implementation phases are complete.
- P1 remains PARTIAL; do not advance to P2 until the remaining lifecycle/runtime acceptance is met.


### P1 effective Accessibility read-back regression — 2026-11-03
- [x] Test the effective-state truth table: global switch AND Vyom component must both be enabled; null/malformed component state is false.
- [x] Route Power Mode status and Control readiness through the same pure effective-state policy.
- [ ] Exact-head Debug and Full CI green after this correction; no pass claimed until both complete on the same commit.
- [ ] Complete live Level 0/1/2 capability read-back matrix and P1 Shizuku UserService lifecycle/STOP/audit + owner permission revocation acceptance.
- P1 remains PARTIAL. No physical-device acceptance or APK delivery is implied.


### Shizuku concurrent STOP race-test correction — 2026-11-03
- [x] Correct the concurrent test's terminal-state oracle: STOP-before-begin must end COMPLETED after tombstone consumption; begin-before-STOP must end CANCEL_REQUESTED.
- [ ] Exact-head Debug and Full CI green on the correction commit.
- [ ] Continue live Level 0/1/2 state read-back and physical Shizuku lifecycle/permission acceptance. P1 remains PARTIAL.


### P1 Shizuku bind-time authorization — in progress
- [ ] Revalidate live binder, explicit Shizuku authorization and UID 2000 immediately before UserService binding.
- [ ] Verify authorization revocation/binder death cannot leave a stale UserService binding or overstate Level 2.
- No acceptance ID changes until exact-head CI and lifecycle evidence pass.


### P1 Shizuku bind-time authorization — implementation awaiting exact-head CI
- [x] Pure policy rejects binding unless binder is live, owner authorization is granted, and Shizuku UID is exactly 2000.
- [x] Re-read binder liveness, permission and UID immediately before UserService bind/reuse; refuse and clear stale local state on any mismatch.
- [ ] Exact-head Debug and Full CI green.
- [ ] Runtime UserService lifecycle, permission revocation, STOP process termination and Level 0/1/2 read-back acceptance.
- P1 remains PARTIAL; no acceptance ID changed and no APK delivered.


### P1 Level 2 live UserService capability truthfulness — 2026-11-03
- [x] Policy contract requires live Shizuku binder, owner permission, UID 2000, local UserService binding and a live remote binder before reporting LEVEL_2.
- [x] Capability read-back clears stale UserService state and falls back to the actually effective lower level when authorization or service liveness is lost.
- [ ] Exact-head Debug and Full CI green after this change.
- [ ] Physical Shizuku disconnect/revocation/STOP acceptance remains pending; P1 remains PARTIAL.


### P1 Shizuku UserService teardown admission gate — 2026-11-03
- [x] Atomic registry shutdown closes request admission and marks in-flight STARTING/RUNNING requests cancelled.
- [x] UserService destroy invokes the registry shutdown gate before process termination.
- [x] Unit tests cover teardown idempotence, in-flight cancellation and post-shutdown rejection.
- [ ] Exact-head Debug and Full CI green.
- [ ] Physical Shizuku service teardown and STOP process-death timing remain pending; P1 remains PARTIAL.


### P1 Shizuku permission revocation — source audit checkpoint (2026-11-03)
- [x] Pure policy regression contract covers invalidation when binder, permission, UID, remote binder or bound state is lost.
- [x] Client clears stale remote/bound/request state on permission-result denial and all known invalid refreshStatus() paths; read-back and capability checks still revalidate live state.
- [ ] Exact-head Debug + Full CI for implementation SHA 87ea80ae796ca11c3f5a5b1d7633bb50169bbf51 is pending (Debug 41585343710, Full 41585343732). The test-only Debug run 41585179156 FAILED as expected with six unresolved mustInvalidateShizukuUserService references at PowerModePolicyTest.kt:209-214; Full CI 41585179120 was CANCELLED. RED is confirmed by Debug log only.
- [ ] In-flight process cancellation on authorization loss and actual Shizuku permission-revocation callback timing are not proven.
- P1 remains PARTIAL. No acceptance ID row changed. No physical-device acceptance and no final APK.


### P1 authorization-loss cancellation — source checkpoint (2026-11-03)
- [x] Previous exact-head 89513bed97de477e118a2bf1b247c9d72a3b087a Debug 41585666596 and Full CI 41585666684 both SUCCESS.
- [x] Regression tests cover cancellation attempts for all valid active IDs, dead remote binder, malformed IDs and individual cancellation transaction failure.
- [x] Client attempts best-effort cancellation over a still-live UserService binder before clearing stale authorization/service/request state; per-request failures do not prevent remaining attempts.
- [ ] Test-only RED runs Debug 41586511675 and Full CI 41586511800 were IN_PROGRESS at last poll; no RED result claimed.
- [ ] Exact-head Debug 41586567783 and Full CI 41586567829 pending on implementation SHA 3f62456c9c2c77e88c8778a1da2522b1cc518e31; require final documentation-head runs.
- [ ] Prove owner STOP and UserService process termination lifecycle; then finish live capability and secure-settings/app-op read-back matrix. Unit tests do not prove Android/Shizuku OEM process-death timing.
- P1 remains PARTIAL; no acceptance ID changed; no physical-device acceptance or APK delivery.


### P1 Shizuku child-process termination lifecycle — implementation pending exact-head CI
- [x] Added bounded graceful termination followed by force-kill fallback and bounded post-kill wait.
- [x] STOP and UserService teardown share the same process-termination helper; teardown processes every request ID from the atomic shutdown snapshot without one failure skipping later IDs.
- [x] A child process that appears after teardown's snapshot is terminated when the closed registry rejects its STARTING-to-RUNNING transition.
- [x] Teardown waits up to 2 seconds for in-flight registry entries to complete late-start cleanup, then sweeps remaining visible children before exiting.
- [ ] Exact-head Debug and Full CI green.
- [ ] Physical Shizuku process-death timing and live Level 0/1/2 + secure-settings/app-op read-back acceptance.
- P1 remains PARTIAL; no acceptance ID changed and no APK delivered.


### P1 Shizuku process termination — exact-head CI correction
- [x] Add missing production process-termination helper and UserService adapter identified by real CI compiler logs.
- [x] Restore process-termination regression test source in app test tree.
- [ ] Exact-head Debug + Full CI green.
- [ ] Continue P1 live Level 0/1/2 capability and secure-settings/app-op read-back matrix after CI resolves.
- P1 remains PARTIAL; no acceptance ID changed; no APK.
