# Vyom Completion Checklist — Evidence-Based Audit

> Scope: Android app only. This is a source audit and implementation acceptance checklist, not a claim that these features are complete.

## Locked constraints
- [ ] Work only on `feature/memory-intelligence` (or an explicitly approved feature branch).
- [ ] Do not touch Android `main`, Hermes/Telegram, Render, backend services, production Supabase, secrets, or deployment.
- [ ] Preserve existing pairing/chat, local memory, STOP CONTROL, allowlist, and sensitive-action protections.
- [ ] Every behavior change follows RED → GREEN → REFACTOR; no "tested" claim without actual test output.
- [ ] Final APK requires exact-head Debug + full CI success, artifact download/integrity/SHA verification, and signing-certificate comparison before replacing an installed app.
- [ ] Physical-device acceptance remains a separate user-side gate.

## Audit findings (current source evidence)

### 1. Android UI control
- [x] Accessibility service exists: `VyomAccessibilityService`; it is opt-in and user-enabled in Android Settings.
- [x] App access is fail-closed by default: `AppAccessPolicy.initialSnapshot()` sets master OFF and enabled package set empty; new packages default OFF.
- [x] UI agent loop exists in `MainActivity.startUiAgentTask()`: observe accessibility tree + local OCR, plan one action, validate, execute, wait for event settle, re-observe; 40-action ceiling and STOP checks.
- [ ] **Critical wiring defect:** `showUiAgentPanel()` is defined but has no call site in `MainActivity.kt`; the UI-agent entry point is unreachable from the current navigation.
- [ ] **Critical routing defect:** `INSTALL_PLAY_STORE` routes to `openPlayStoreSearch()`, which only opens search and tells the user to tap Install. It never invokes the UI agent.
- [ ] Capability preflight is incomplete: the user needs Accessibility enabled, app-access master ON, the target app allowed, and an NVIDIA planner key; no unified setup/diagnostic screen reports all blockers.
- [ ] Play Store install flow must verify the exact listing (app title/developer) before tapping Install, observe download/install progress, then verify installed package/version with PackageManager. Never infer success from a tap or Play Store navigation.
- [ ] Explicit user intent may authorize the requested in-app Install tap only when the exact app listing is verified; retain separate Android/Play Store system confirmations if they appear. Never bypass Play Protect, payments, parental controls, or OS confirmation.
- [ ] General UI agent must be reachable from Control and from eligible direct device commands, with visible STOP/progress and exact failure reasons.
- [ ] Current planner requires the configured NVIDIA API key; missing key safely returns no action. Add capability-aware fallback/clear diagnosis rather than silently claiming the task is impossible.
- [ ] Custom Canvas/timeline controls are not reliably exposed by accessibility/OCR. Add a separately gated visual-understanding capability; do not invent coordinates or bypass protected surfaces.
- [ ] UI-agent loop is Activity-owned, not a dedicated foreground task service. Background continuity/process-death recovery is not proven; add a user-visible foreground execution lifecycle and persistent task state before claiming reliable background control.
- [x] Event/revision-aware observation and bounded settle wait exist in current source.
- [ ] Semantic outcome verification is incomplete: current `DONE` requires an exact visible label but still tells the user to manually verify; add per-action expected postconditions and independent final verification.
- [ ] Regression matrix: stale screen, app switch, STOP during planner/confirmation/action/wait, grant expiry, service disconnect, API timeout, action rejected, no visible target, ambiguous target, screen lock, secure window, rotation, process death, and successful install verification.

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

## Implementation order (locked)
1. [ ] Wire UI Agent into Control navigation and add a capability/readiness report.
2. [ ] Route explicit Play Store install intent through verified UI automation; verify the installed package/version and report only observed outcomes.
3. [ ] Complete outcome verification, background lifecycle, STOP/app-switch/grant-expiry/action-failure regression tests.
4. [ ] Finish durable task queue and local scheduled automations without backend changes.
5. [ ] Upgrade Teach Vyom and executable skill catalogue.
6. [ ] Upgrade Memory Intelligence: typed/provenance memory, summaries, hybrid semantic retrieval, local knowledge library, conflict/migration tests.
7. [ ] Build guarded AI-generated self-update PR workflow and signed update/rollback verification.
8. [ ] Finish remaining real design screens and UI regression.
9. [ ] Run full verification, produce exact-head APK, then hand off for physical phone acceptance.

## Non-negotiable limits
- Android may still require the user to enable Accessibility, grant app-specific access, approve Android's package installer/system prompts, or authenticate for protected actions.
- Vyom must not bypass Play Protect, payment/financial security, secure-window restrictions, biometric prompts, or Android permission boundaries.
- "Action accepted", "screen changed", "download started", "installed", and "update healthy" are different states. Only report the state independently verified.
