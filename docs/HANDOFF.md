# OWNER PRIORITY OVERRIDE — VYOM FACTORY FIRST (2026-11-03)

This is the newest owner instruction and supersedes all conflicting milestone sequencing below. Repository and safety boundaries remain unchanged.

## M0 IMPLEMENTATION CHECKPOINT — SOURCE PRESENT, ACCEPTANCE OPEN (2026-11-03)

### Implemented in source
- Native Workspace drawer destination **Vyom Factory**.
- GitHub Issues task client using the existing encrypted `github_proposal_token` preference; no token is hard-coded or logged. Settings now states the additional repository Issues read/write requirement.
- Native Factory screen lists only issues carrying the validated Factory metadata marker; supports create, edit title/description, priority changes, bounded state transitions, issue-comment history, and 15-second refresh while open.
- Normal Vyom chat supports explicit `complaint:`, `factory:`, `factory complaint:`, `register complaint:`, `report bug:` and `issue:` intake. Chat message UUID is the source identity; queued Factory complaints resume through Factory intake after process restart.
- Task metadata policy validates schema/version, allowed source/priority/status, idempotency key and bounded state transitions. Retries find and update the existing issue rather than intentionally creating a duplicate.
- Termux adapter `scripts/vyom-factory.sh` supports create/list/edit/priority/status/comment through GitHub CLI. Full CLI syntax is checked by the Android CI workflow.
- `docs/VYOM_FACTORY_PROTOCOL.md` defines the shared metadata contract. ChatGPT can create/update the same issues through the connected GitHub integration when the owner explicitly asks; this is assistant-invoked, not silent automatic monitoring of every chat.
- Diagnostics now attempts a live Factory issue read and reports PASS/FAIL/SKIP based on the actual response.

### Current acceptance state — NOT COMPLETE
- Exact-head Debug + Full CI must both pass after this checkpoint; named Factory policy tests and Termux `bash -n` step must be green.
- Owner phone test still required: token with Issues read/write, Factory screen load/create/edit/priority/status/history, 15-second external-change refresh, normal-chat intake and app-restart recovery.
- Termux CLI must be exercised with the owner's authenticated `gh` session. If needed, install `gh` and `jq` once and authenticate with `gh auth login`; never paste credentials into chat.
- ChatGPT issue create/edit is available through the connected GitHub integration, but has not yet been exercised against a real owner complaint.
- Parallel code workers, live worker heartbeat, durable execution/restart, skills/repository install, schedules, cross-source semantic duplicate detection, full runner/CI orchestration and true 24/7 operation remain unfinished. A GitHub Issue is a task record, not proof a worker is running.
- No APK delivered. Do not deliver M0 APK until Factory acceptance, exact-head CI and signing/certificate gates are satisfied.

## M0 — VYOM FACTORY (FIRST IMPLEMENTATION PRIORITY)
Owner explicitly approved building Vyom Factory before receiving the next APK. Do not deliver an APK until the Factory milestone is implemented and exact-head CI is green.

### Product contract
- Add a permanent **Vyom Factory** destination inside the existing Workspace/Drawer, matching the existing native app architecture and visual language. This is a real app screen, not a mock dashboard or separate website.
- Show live, evidence-backed work: active/pending/blocked/completed tasks, current step, worker, timestamps, progress events, skill/repository research and installation state, CI/test results, update history, errors and rollback state. Never display fabricated progress or percentages.
- Complaint intake must converge on one canonical task identity across (a) Vyom normal chat, (b) Vyom Factory, (c) ChatGPT via the connected GitHub integration, and (d) Termux via GitHub CLI/API. GitHub Issues are the proposed shared registry; verify repository permissions, auth, issue schema and rate limits before wiring. Same task must be editable/reprioritizable from every source without creating duplicates. Use stable external IDs/idempotency keys and preserve an audit trail.
- Factory controls: create complaint, edit description, reorder priority, pause/resume, cancel, merge duplicates, split into subtasks, add evidence/comments, and view immutable event history. State changes must be authorized and persisted before UI reports success.
- Parallel work is allowed only in isolated branches/worktrees or non-overlapping file ownership. Serialize conflicting writes, preserve STOP/cancel semantics, bounded retries, CI gates and review. No autonomous merge to protected/main branches.
- Skills/repos: discover, inspect repository and SKILL.md, review licence/security, install only into an approved user-writable content location, run a real validation test, record source/revision/hash and report exact result. Never claim installed from merely listing a repo.
- Update visibility: distinguish content-only A18 updates from native APK releases. Show signed package verification, staged probes, active slot, history and rollback only from actual A18 state. Native Kotlin/permission changes still require a signed APK update; never promise every change is APK-free or silent.
- Always-on operation means durable queued work plus an actual configured runner/scheduler and observable heartbeat. GitHub Actions alone is event/queue execution, not a guarantee of 24/7 continuous uptime. Show offline/runner unavailable explicitly; do not fake background activity.
- Secrets: never embed a personal GitHub token, PAT, app private key or signing secret in APK/source/logs. Use a supported OAuth/device-flow or secure broker design, encrypted local token storage if applicable, least privilege, revocation and explicit setup. ChatGPT and Termux use their own authenticated GitHub connections.
- User's request to use Vyom while it upgrades must be preserved: keep chat/control usable during background work; never replace the installed APK with an incompatible signer or require uninstall to update.

### M0 implementation sequence
1. Inventory existing task APIs/models, local persistence, chat task creation, drawer navigation, skills/update screens and Diagnostics. Reuse working components; mark unwired/partial paths accurately.
2. Define and test canonical complaint/task schema, idempotent intake, priority/state transitions, append-only events and conflict-safe updates. GitHub Issues adapter is canonical only after auth/permissions are verified; otherwise clearly report the blocker and do not silently substitute local-only storage.
3. Build the native Vyom Factory screen and wire it into Workspace/Drawer. All cards/actions must use live task/event data; no hard-coded demo counters.
4. Wire complaint intake from Factory and normal Vyom chat; document/test ChatGPT GitHub issue intake and Termux `gh issue` intake against the same task schema/labels.
5. Wire worker/run status, skill/repository operations and CI/update evidence to actual execution records. No pretend workers or fake install state.
6. Add safe task controls, event history, retry/cancel/STOP and multi-task isolation tests.
7. Add a real Diagnostics row and acceptance tests for each delivered Factory capability; exact-head Debug + Full CI; only then issue the M0 APK after signing/certificate checks.

### Termux / owner setup
- Do not ask the owner to perform Termux setup pre-emptively. First complete repository-side work and identify the exact unavoidable local prerequisite.
- If Termux is genuinely required (e.g. one-time `gh auth login`, device bridge, or phone acceptance), give the exact command, purpose, expected output and whether it is one-time. Never ask the owner to paste tokens or secrets into chat.
- If GitHub OAuth app registration/client ID, GitHub permissions, or a runner registration requires owner action, stop at that exact gate and provide the minimal safe setup steps.

### Evidence and delivery
- Keep this section controlling until M0 is accepted. Keep HANDOFF, MASTER_ORDERS and completion checklist synchronized after each meaningful batch.
- Every task status must have source SHA, named test/CI run where applicable, event timestamp and evidence link. Do not claim cross-source intake, 24/7 operation, skill installation, or automatic update until exercised end-to-end.
- Work only on `feature/memory-intelligence`. Never touch Android `main`, Render, Hermes backend or production Supabase.
- Continue in batches; do not stop after one tiny item. Commit/push each coherent verified batch and run exact-head CI. Fix failures from actual logs.
- Report only verified changes, current full 40-character HEAD, CI run IDs/results, changed acceptance rows, exact owner prerequisite (if any), and next unfinished Factory item.

---

# OWNER PRIORITY OVERRIDE — UPDATE-FIRST MILESTONE ROUTE (2026-11-03)

This is the newest owner instruction. It supersedes the milestone order in the earlier OWNER RESET in this file and all conflicting sequencing elsewhere. Repository/branch and safety restrictions remain in force.

## Immutable scope and delivery rules
- Repository: `riyas433554-dot/vyom-android`; work ONLY on `feature/memory-intelligence`.
- Never touch Android `main`, Render, Hermes backend, or production Supabase.
- One verified APK at the end of EACH milestone. Candidate APK must match the certificate of the already-installed app before install; never suggest uninstalling to bypass a mismatch.
- Stable signing secrets/keystore must be verified without exposing secret values. Release APKs must use the same key on every build. If secrets are absent/unverifiable, stop signing-dependent delivery and report the exact missing evidence; do not silently create a new production key.
- Protected files (kill-switch, policy engine, update verifier, signing configuration) are review-only for self-editing. Owner/GPT repository changes still require review, CI and verification; never auto-apply protected-file patches.
- No dead code: FIXED means reachable from UI or voice/chat, end-to-end behavior verified, and a real Diagnostics row exists. First task in EACH milestone: inventory all previously implemented/claimed items, identify unwired paths, wire them, and re-verify. Re-do failed claims.
- Evidence per ID: full source SHA, CI run ID/result, named test, Diagnostics row. No physical-device claim without actual owner/device evidence.

## MILESTONE 1 — APK 1 (UPDATE SYSTEM FIRST)
1. Stable signing + update pipeline (S6/S7): verify keystore secrets, stable release signing, same-certificate update compatibility. Candidate certificate comparison against installed package certificate is a mandatory pre-install gate.
2. Complete all THREE end-to-end update paths:
   - In-app update screen: owner request -> GitHub API repo read -> minimal patch/diff (never whole-file replacement) -> PR on dev branch -> CI wait -> signed APK artifact download -> SHA-256 + certificate verification -> install. Shizuku silent install only when Power Mode is active and Android permits; otherwise one-tap user install.
   - External GPT/Claude/other agent commit -> CI signed APK -> Vyom Check for updates discovers and installs it.
   - Owner chat phrase “Vyom, ye badlo” routes into the same guarded in-app patch/PR flow.
3. **A18 NO-APK UPDATE CHANNEL — immediately after stable signing, before all other M1 features.** Build a signed, content-only update system over the same GitHub release path. Stable native Kotlin core owns tap/shell/voice/memory/files/network/permissions; content layer owns UI bundle, skills, workflows, prompts/personas, routing rules, knowledge notes, model/lane config and non-protected policies. Manifest: version, min_app_version, changes, per-file SHA-256, 1–2 probes, rollback_to. Verify signature + hashes + app-version compatibility, stage in a separate slot, run only the shipped probes, atomically switch, retain prior slot. Auto-rollback on probe/page/skill load failure or 2 crashes within 60 seconds. Protected kill-switch, policy engine, update verifier and signing config are never content-updatable. Each update shows version, PASS/FAIL, timing delta (faster/same/slower +X ms), history and one-tap rollback. Full Control Suite only at milestones/on demand. Vyom must also generate content packages from owner prompts and test staging before apply; ask owner only if a protected file is involved. Required evidence: visible-element demo with passing probes, failing-probe auto-rollback demo, and result-card screenshot in CI artifact or Diagnostics.
4. Implement and verify A17 Generic Control Engine (20-row coverage + owner-run device subset); then finish and wire P1 Power Mode, A12 structured tool-call execution (no raw JSON in chat), A14 TRUSTED default/no blanket confirmations while retaining hard blocks, A1 Play Store install as an independent acceptance probe, `whatsapp_send` as an independent acceptance probe, floating orb during control tasks, F4 persistent auto-capture visible in Memory, and Diagnostics > RUN ALL CHECKS.

M1 APK gate: stable signing; A18 no-APK update channel with signed package verification, stage/probe/atomic switch/rollback, demo evidence and result card; A17 20-row implementation/test/Diagnostics coverage and owner-run real-device subset; all other items must be reachable, end-to-end, tested and represented in Diagnostics; exact-head Debug + Full CI green; release artifact SHA-256 and certificate verified; certificate matches installed app before update; deliver one APK and owner phone-test guide.

## A17 — GENERIC CONTROL ENGINE (M1 SCOPE CLARIFICATION — 2026-11-03)

A17 is mandatory M1 scope. A1 Play Store install and `whatsapp_send` are acceptance probes only, not the definition of full phone control. Required generic loop: Accessibility tree + screenshot/vision/OCR observe → plan → act → verify post-condition → recover. Use authorized ShellExecutor/Power Mode when available; otherwise Accessibility. Generic action vocabulary includes tap/long-press/swipe/scroll-until-found/type, Back/Home/Recents, gestures, key events, clipboard, share sheet, intents and deep links. Re-observe after every action; retry/alternative paths must be bounded and based on fresh state.

Unknown app/screen invokes Learn Anywhere (research → bounded try → verify → save tested skill), not a hard-coded per-app script. Capability manifest must reflect live grants and identify the exact missing permission plus one-tap Settings fix. Never answer only “I can't”.

Coverage matrix has 20 rows: (1) app lifecycle/install; (2) arbitrary app UI; (3) system toggles; (4) notifications; (5) calls/SMS/contacts; (6) camera/photos/video/screenshots/recording; (7) file manager; (8) alarms/timers/calendar/reminders; (9) messaging apps/email; (10) browser; (11) clipboard/text; (12) per-app permissions; (13) battery/storage/app info/settings; (14) Termux bridge; (15) location/maps; (16) media; (17) pinch/drag/multi-step gestures; (18) popups/ads/permission dialogs; (19) multi-app data handoff; (20) scheduling. Every row requires reachable implementation + named automated test + Diagnostics check.

Hard limits: FLAG_SECURE, biometrics/lock-screen credentials, Play Protect, payment/banking/OTP/Authenticator apps, Vyom kill-switch and protected files. No bypass. Control Suite must run 20 varied safe tasks across different apps/domains with PASS/FAIL/SKIP and evidence. M1 remains open until all rows are covered and an owner-runnable real-device subset passes on the actual phone. The existing UI Agent is partial groundwork, not A17 completion.

## MILESTONE 2 — APK 2
- UI through hot-swappable bundle. Combine Grok-style composer (+, model pill, mic, Speak), Voice Settings, drawer and Automations with the owner's last Claude Design files. The supplied design is authoritative; do not redesign it. Drawer and Profile are separate screens. Add activity timeline, source chips and stop button.
- Advanced memory: CORE + episodic + semantic, embeddings, 500+ facts, conflict handling.
- Model Hub: owner-managed API keys; route chat/task/vision/live lanes to best configured model with secure key storage.
- Claude-Code-style agent loop: plan, read files, edit, run, verify, retry.
- First M2 task remains no-dead-code inventory/wiring of every previously implemented M2 item.

## MILESTONE 3 — APK 3
- Learn Anywhere (research, try, verify, save).
- Install skills from GitHub URL or ZIP in chat: inspect SKILL.md and repository, safety/licence review, install, run a test, report; editable learned-skills list.
- Agent Company orchestrator with planner/coder/researcher/tester roles and shared task board. Researcher finds useful repos/skills; skill-factory creates and tests new repos/skills. Show agents/status on Automations and workflow canvas.
- In-app local server plus Termux bridge (RUN_COMMAND intent or ShellExecutor); show localhost sites inside Vyom panel. Vyom executes Termux commands itself; owner should not need manual Termux steps.
- First M3 task remains no-dead-code inventory/wiring of every previously implemented M3 item.

## Execution protocol
- Update these controlling instructions first (done by this commit), then inspect current source and workflow. Work in long batches; commit/push to this branch only; keep HANDOFF and MASTER_ORDERS current.
- Start M1 with signing/update-system audit and inventory of existing update/signing/UI-bundle implementations and their actual UI/command/Diagnostics reachability. Do not jump to P1 or other M1 features until the update system is end-to-end.
- Use minimal patch operations for agent-authored code changes. Never implement self-edit by blind whole-file replacement. PRs target the development branch and require CI.
- Keep the three update paths distinct in tests and Diagnostics. Verify hash, signing cert, installed cert match, rollback and failure states.
- Do not expose signing secrets, GitHub tokens or private keys in logs, chat, diffs or artifacts.
- Report only current full 40-char HEAD, CI run IDs/results, and ID rows changed. For milestone delivery, include one APK's artifact/checksum/certificate evidence and Diagnostics/phone-test checklist.


### Update pipeline / Shizuku install compile correction — 2026-11-03
- Exact source SHA `24b325571b653d376120a224cae4f7946d8187fa`: Debug `41600380211` and Full CI `41600380229` both FAILED at Kotlin compilation.
- Actual compiler error from both logs: `MainActivity.kt:3998:35 No value passed for parameter 'expectedSha256'`. The client API had been hardened to require the expected SHA-256, but the UI call site still used the previous one-argument signature.
- Corrected the UI call to pass the exact `expectedHash` already computed from the verified APK. This preserves the shell-private staging hash read-back gate; it does not weaken signing/hash checks.
- The previous source SHA `3387385b76d03aa3d43a0e657458182c2c20cb3f` passed release signing/certificate verification but its GitHub Release publication failed with HTTP 403. This is now corrected: publishing jobs have job-scoped `contents: write` and use only the built-in `secrets.GITHUB_TOKEN`; no personal token is referenced by any workflow. If a future publish still returns 403, set repository Settings → Actions → General → Workflow permissions → Read and write permissions, then Save.
- No M1 acceptance ID rows changed. No APK delivered. Next: exact-head CI, inspect real logs; then validate update paths, UI bundle, and wiring/Diagnostics inventory. Signing fingerprint observed in CI is `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`; installed-device certificate match is still unverified.


### Proposal-branch CI base correction — 2026-11-03
- Audit found `.github/workflows/vyom-android-ci.yml` still used the retired `feature/u1-live-orbit-tasks` branch as the zero-before-SHA fallback for the first push to a `vyom-proposal-*` branch, and did not target `feature/memory-intelligence` for PR events.
- Corrected the fallback and pull-request target to the locked development branch `feature/memory-intelligence`. This prevents a first proposal push from being compared against an unrelated historical branch and ensures PR CI attaches to the intended target.
- This is required for update path (a); proposal-branch CI remains pending exact-head verification. Do not count proposal CI as green until both required workflow runs are for the exact proposal SHA.

---

### M1 / P1 Power Mode read-back and termination checkpoint — 2026-11-03
- Process termination regression on source SHA `fad60e3106139f1cd63bbe58e64c7904b6e67a14`: Debug `41592559997` and Full CI `41592559989` both FAILED at `:app:testDebugUnitTest`; 221 tests completed, one failed: `ShizukuProcessTerminationTest.teardownSnapshotAndLateStartupCannotLeaveAnUntrackedProcessRunning`.
- Correction commit `4840b2e06f2bba415075234a36402453a51860e0`: after force-kill, the terminator now also checks process liveness; a confirmed-dead child is not incorrectly reported as still running when the bounded wait callback returns false.
- Added `PowerModeReadbackPolicyTest` for exact successful `pm check-permission` output, explicit overlay app-op allow vs default/denied, exact Accessibility global 0/1, and exact Vyom component membership. Test commit `882e2a7b3c76dc624e9a591aa0141c9f04ed7796`.
- Added `PowerModeReadbackPolicy.kt` and wired Power Mode read-back buttons to display interpreted GRANTED/DENIED/ALLOWED/BLOCKED/ENABLED/DISABLED/UNKNOWN states rather than presenting raw shell output as proof. Source commits: `ffc84e775c594135ce813eb72aae7c082fda0acc`, `69fd02d289971451844aefd1614953b82c3131d3`.
- Debug `41594969989` and Full CI `41594969891` target exact source SHA `69fd02d289971451844aefd1614953b82c3131d3`; both were IN_PROGRESS at this checkpoint. These do not cover the documentation commit below; exact final-head CI is still required.
- P1 remains PARTIAL. No feature/acceptance ID rows changed. No physical Galaxy A06 Shizuku, permission-revocation or STOP timing acceptance is claimed. No APK delivered.
- Next: exact-head CI after this checkpoint; then complete the remaining P1 live Level 0/1/2 + secure-settings/app-op read-back and owner STOP/permission-revocation acceptance before starting A12.

---

### P1 Shizuku force-kill exit read-back correction — 2026-11-03
- Verified failing source SHA: `fad60e3106139f1cd63bbe58e64c7904b6e67a14`; Debug run `41592559997` and Full CI run `41592559989` both FAILED in `:app:testDebugUnitTest`.
- Real failure: `ShizukuProcessTerminationTest.teardownSnapshotAndLateStartupCannotLeaveAnUntrackedProcessRunning` (221 tests, 1 failed). The helper called force-kill, but returned only the bounded wait result; if the process was already dead while the wait callback returned false, it incorrectly reported failure.
- Fix commit: `4840b2e06f2bba415075234a36402453a51860e0`. After force-kill, termination now succeeds if either the bounded wait confirms exit OR an independent `isAlive()` read-back confirms the child is dead.
- Exact-head Debug `41594755705` and Full CI `41594755829` were triggered for this source SHA; results pending at this checkpoint. No GREEN claim.
- No feature/acceptance ID rows changed. P1 remains PARTIAL. Next: verify both exact-head workflows; then finish live Level 0/1/2, app-op/secure-settings read-back and remaining P1 lifecycle evidence.

---

# OWNER RESET — MILESTONE DELIVERY PLAN (2026-11-03)

This section is the newest controlling owner order and supersedes conflicting older milestone sequencing or the former one-final-APK rule. Repository and safety boundaries remain unchanged.

## Scope and branch lock
- Repository: `riyas433554-dot/vyom-android`.
- Work only on `feature/memory-intelligence`.
- Never modify Android `main`, Render, the Hermes backend, or production Supabase.
- Preserve the existing owner safety policy: TRUSTED is the default; no blanket confirmations. Hard blocks remain payments, OTP/Authenticator/banking/wallet apps, security settings, Vyom's own kill-switch and protected files. Power Mode is owner-initiated through Wireless ADB from Termux, then Shizuku; never bypass Android safeguards.
- STRICT evidence: claims require exact 40-character source SHA, workflow run IDs/results and named test evidence. No claim of runtime/device verification without it.

## APK delivery policy
- The former “one final APK only” rule is cancelled.
- Deliver one APK per completed milestone, so the owner can test each milestone early.
- A milestone APK may be delivered only after that milestone's exact-head CI is green and the artifact's integrity/signing state is verified. Clearly distinguish debug vs signed APK and report any update-install signing mismatch; never advise uninstalling to bypass it.
- Exactly one APK artifact is handed off per milestone; do not send interim build artifacts.

## MILESTONE 1 — implement in this exact order
1. Finish P1 Power Mode.
2. A12 structured tool-call execution; raw JSON must never appear in user chat.
3. A14 TRUSTED default with no blanket confirmations, preserving the explicit hard-block list.
4. A1 Play Store installation end-to-end with listing identity, install progress, PackageManager version/presence verification and truthful outcome.
5. Implement `whatsapp_send` skill with recipient/message validation, execution and independent delivery/result verification; do not claim sent without evidence.
6. A8/A16 floating orb during control tasks, with mute/stop and task lifecycle behavior.
7. F4 memory auto-capture that persists actual records and visibly lists them in the Memory screen, with privacy filters and user controls.
8. R4 Diagnostics screen with a functional RUN ALL CHECKS action and per-check truthful status/evidence.

After all M1 items are implemented, update HANDOFF and completion checklist with evidence. Run Debug and Full CI on the exact final M1 head, fix failures from real logs, verify APK artifact checksum and signing certificate, then deliver the single M1 APK with the Diagnostics check list and exact owner phone-test script. Do not start M2 before M1 is accepted as implementation-complete and its APK is handed off.

## MILESTONE 2 — UI (only after M1)
- Host the supplied Claude Design HTML in an Android WebView and reproduce it exactly; no redesign or reinterpretation.
- Add a narrowly scoped Kotlin JavaScript bridge for chat, voice, memory, tasks and permissions. Validate message schemas/origins and keep privileged actions behind native policy gates.
- Use native Compose only where HTML/WebView cannot provide the required Android integration.
- Include the Grok-style composer; Drawer and Profile as separate screens; activity timeline; n8n-style workflow canvas in the web layer.
- Preserve all Android safety, STOP and hard-block boundaries across the bridge.

## MILESTONE 3
- Learn Anywhere, ledger, attachments, real-running automations, human-like voice and floating bubble.
- Each capability must execute through a real adapter, persist results where required, expose progress/errors, support cancellation/STOP and verify outcomes. No placeholder capabilities.

## Execution protocol
- Work in long autonomous batches; do not stop after each item.
- Follow the exact M1 order; commit and push incremental verified work to this branch only. Keep HANDOFF and completion checklist current after each meaningful batch.
- Run CI against the exact pushed head; inspect actual job logs and fix failures before claiming green.
- Report only: current head SHA (40 chars), CI run IDs and results, and ID rows changed. For milestone delivery, additionally include the APK artifact/integrity/signing evidence and Diagnostics/phone-test checklist required by the owner.
- No questions unless a truly blocking ambiguity or missing owner asset prevents safe progress; do not silently substitute a design or relax policy.

---

### Exact-head CI race-test correction — 2026-11-03
- Exact source SHA: `806fa49ae8daefb41759ae13642992de73764d95`.
- Debug run `41576923369` FAILED in `:app:testDebugUnitTest`: `ShizukuRequestRegistryTest.concurrentBeginAndStopNeverLoseCancellationOrExceedCapacity`, assertion at line 76. 207 tests ran; one failed. Kotlin compilation and APK packaging reached completion before the test task failed. Full CI run `41576923374` was still in progress at the time of this update.
- Root cause is the test's invalid expected-state set, not a registry defect: when STOP creates a pre-start tombstone and begin subsequently consumes it, the specified registry transition is `CANCELLED_BEFORE_START -> COMPLETED`. The test incorrectly rejected this valid terminal state.
- Correction: assert the two valid outcomes only: STOP wins first => COMPLETED; begin wins first => CANCEL_REQUESTED. No production behavior is weakened.
- Next: exact-head Debug + Full CI; then continue P1 live capability matrix and remaining Shizuku lifecycle acceptance. P1 remains PARTIAL; no APK delivered.

---

### P1 effective Accessibility read-back — 2026-11-03
- Test-first source SHA: `8a6e694181beddbffffb808c8c46775db65abf78`.
- Exact-head Full CI run `41576567452` FAILED in `:app:compileDebugUnitTestKotlin`: six unresolved-reference diagnostics at `AccessibilityReconnectPolicyTest.kt:27-35` for missing `AccessibilityReconnectPolicy.isEffectivelyEnabled`. The production source compiled; secret scan passed. The Debug workflow `41576567444` was still in progress at the time of this correction.
- Correction: add the pure effective-state policy (global Accessibility flag AND Vyom component membership), and use it in both Power Mode status and Control readiness so displayed state cannot drift from the tested rule. Reconnect success read-back also continues to require both secure settings.
- Added regression cases already on the test-first commit: global off/component on; global on/component absent; both off; both on; null service list; malformed component.
- No matrix ID is marked complete by this source correction. P1 remains PARTIAL pending Shizuku UserService lifecycle/STOP/audit verification and owner-led permission revocation/read-back; physical Galaxy A06 acceptance remains deferred.
- Next: finish the live Level 0/1/2 capability table/read-back matrix, then continue the remaining P1 lifecycle and owner permission acceptance. Do not advance to P2.

---

### P1 bounded-registry follow-up — 2026-11-03
- Adversarial review found a further resource-bound issue: pending STOP tombstones could prevent the registry's completed-only trim from reducing the map. Registry now enforces a hard entry cap and refuses new tombstones/requests when all slots are active or pending.
- Commits: strict capacity `ce49a55fafd4d2b03a2870a8f0b70de83d47af97`; UserService capacity response `f0d13c6b2d7386101de54223b68e5517e870adc1`; hard-cap regression test `20bb2988e7a1e10fc69764e6f6e25f5a2b9d03d6`.
- Added `ShizukuRequestRegistryTest.pendingStopTombstonesAndActiveRequestsCannotExceedCapacity`. This proves tombstones and active requests cannot grow the registry beyond its configured cap and that capacity exhaustion is explicit.
- Exact-head CI from the prior documentation checkpoint was still running when this follow-up was made; those runs do not verify the new source. New exact-head runs are required after this checkpoint.

## P1 Shizuku request lifecycle and STOP hardening — 2026-11-03
- Added `ShizukuRequestRegistry` with atomic STARTING/RUNNING/CANCEL_REQUESTED/COMPLETED states and a bounded completed-history window. STOP-before-execute, STOP-during-startup, duplicate begin, terminal replay and active-entry retention have dedicated JVM tests in `ShizukuRequestRegistryTest`.
- UserService now reserves request IDs before process startup, registers the process before transitioning to RUNNING, checks cancellation at the startup boundary, kills the process on STOP/timeout, and records bounded audit events through Android Log (request ID, allowlisted command ID, outcome only; no command output or secrets).
- The previous implementation kept independent `running`, `cancelled` and `completed` collections and cleared all completed IDs after 256 entries. That could race between STOP and process startup, and wholesale clearing could erase replay protection. The registry replaces that split state.
- Commits: registry `17bd00e1a47ff2174e772cd564295bd23fcc31dc`; lifecycle tests `3b14ed74da887616f3b21bb5dd8943e1693375bb`; pre-start cancellation state `362b1e2fe2595eb1b94ff8572343937c2d9cca5f`; test assertion `7969a1fea830e834cf72655658d1fea44703d06e`; service integration `9eac1b4afdc194b1279c48abec6d08bca4ca5e63`; non-null process reference `c2100eb2c86d683f5728739cc263646f254c1133`.
- Automated coverage is still incomplete: no instrumentation test can yet prove Shizuku's actual binder/UserService lifecycle, process-kill timing, durable audit collection, or Samsung runtime permission read-back. Keep P1 PARTIAL and do not advance to P2.
- CI must be checked on the final documentation commit SHA; no pass is claimed until exact-head Debug and Full CI complete.

## Shizuku identity boundary hardening — 2026-11-03
- Test-first commit: `18ea9e8022645b2e17cdb1d5fe74c87e21915422` adds `PowerModePolicyTest.rootOrUnknownShizukuIdentityNeverGetsLevelTwo`; it asserts root (UID 0), system (UID 1000) and unknown (-1) cannot obtain Level 2 even with a live, authorized binder.
- Policy commit: `3b933a4b97f737f65de56dfd6d7770e5f9582b34` now requires UID 2000 (ADB shell) in addition to binder liveness and owner authorization.
- Client commit: `a472cfeff8eb46c18b2efe18dec57be7154731d6` blocks authorization requests and UserService binding for root/unknown Shizuku identities, and includes UID in the effective capability policy.
- This closes a safety gap: previously any authorized Shizuku identity, including root, could be reported as Level 2 despite the intended non-root shell-only contract.
- Exact-head CI is pending for the source head; do not claim tests/build pass until the new exact-head Debug and Full CI runs complete.
- P1 remains PARTIAL. This does not validate UserService lifecycle, cancellation/audit races, live Android permission read-back, or physical-device behavior.

# CURRENT P1 POWER MODE CHECKPOINT — 2026-11-03

This checkpoint supersedes the earlier P1 authorization-gap statement above.

## Shizuku authorization-aware capability state
- Implementation source commits on `feature/memory-intelligence`:
  - `88db603ad7061e6f50c57b0f53c4fd962e521757`: regression tests for unauthorised binder and explicit authorization.
  - `b889f41109b0a4a5a3dbbba5528607945a4f5a82`: Level 2 policy now requires both a live Shizuku binder and explicit authorization.
  - `3ceba5ec7c11c82c49a4671e4f2388937615d355`: Shizuku status reads `Shizuku.checkSelfPermission()` and distinguishes connected-but-unauthorized.
  - `a22d62e3490941854be62fdbafbab29aaf8bcce7`: Power Mode wizard reports effective capability level from the current Shizuku/Accessibility state and corrects stale executor disclosure.
  - `fc08ec51088ba713011bd610afa68febfb7e6d97`: client capability helper reads binder + permission state and feeds the pure policy.
- Regression coverage: `PowerModePolicyTest.shizukuBinderWithoutOwnerAuthorizationDoesNotGrantLevelTwo` proves a connected-but-unapproved binder cannot elevate to Level 2; `PowerModePolicyTest.levelTwoRequiresLiveBinderExplicitAuthorizationAndAdbShellUid` covers the positive/negative state combinations and protected-surface boundary.
- Shizuku command surface remains fixed read-back IDs; arbitrary shell text is not accepted. Existing mutating `set_overlay` policy still requires explicit owner confirmation and an exact allowlisted mode.
- Exact-head CI for implementation SHA `fc08ec51088ba713011bd610afa68febfb7e6d97`: Debug run `41561433950` and Full CI run `41561433988`. At documentation time Debug was IN_PROGRESS and Full CI QUEUED. No pass is claimed. Re-check both against their exact `head_sha`.
- No APK delivered.

## P1 remaining gaps (do not mark complete)
- The wizard still does not automatically detect Samsung Auto Blocker state; current UI gives owner-inspection guidance only. Do not claim automatic detection.
- Verify the Shizuku UserService binder lifecycle, actual service authorization, request completion, STOP cancellation and audit behavior with targeted tests; source-only state is not phone proof.
- Verify secure-settings grant read-back and owner-opt-in Accessibility reconnect state, including revocation/expiry and owner-disabled behavior.
- Verify all appops only when the corresponding feature is declared and enabled.
- Add the live capability table and exact state read-back tests for Level 0/1/2; physical Galaxy A06 validation remains deferred until all implementation phases are complete.
- Preserve protected surfaces: secure windows, biometrics, OTPs/Authenticator, payment approvals, credentials and Play Protect must remain blocked.

## Current matrix
- `P1` is **PARTIAL**, not DONE. The matrix row names the two new regression methods. No other ID row changed in this checkpoint.
- Mandatory next item remains P1: Shizuku UserService lifecycle/STOP/audit tests and remaining owner-led permission read-back. Do not start P2 until P1 acceptance is complete.

## Source head before this documentation update
- `fc08ec51088ba713011bd610afa68febfb7e6d97`

---

# CURRENT CONTINUATION — 2026-11-03

## Exact-head compile failure and correction
- Branch: `feature/memory-intelligence`; Android `main`, Hermes/Render and production Supabase remain untouched.
- Failed exact-head source: `b8493d545d94c09a0c335da5056a722266d94b77`.
- Debug run `41559636024` and full CI run `41559636010` both failed at `MainActivity.kt:2006:21`: Kotlin reported `'val' cannot be reassigned`. Both logs reached `:app:compileDebugKotlin FAILED`; secret scan in full CI passed.
- Root cause: `isTextSelectable = true` was treated as a read-only Kotlin property on Android `TextView`; the previous property spelling fix was still incorrect.
- Correction commit: `22fff1dc6147fed5388009544dd8107fa33cd39d`; source now calls the actual setter `setTextIsSelectable(true)`.
- Exact-head CI for correction commit: full CI run `41560177554` and Debug run `41560177582`; both were IN_PROGRESS at the last check. No pass is claimed. Re-check these exact runs before further code changes.
- No feature ID row changed from this compile-only correction. No APK is being delivered.

## Next P1 Power Mode implementation gap identified from source
- `PowerModePolicy.capabilityLevel(shizukuRunning, accessibilityEnabled)` currently reports LEVEL_2 whenever Shizuku is running, without requiring `Shizuku.checkSelfPermission() == PERMISSION_GRANTED`. This overstates effective authority and needs a regression test plus an authorization-aware state.
- `VyomShizukuShellClient.refreshStatus()` checks installation, binder liveness, API version and UID, but does not read the app's Shizuku authorization state. It can therefore report a connected binder without distinguishing authorized from unauthorized.
- The wizard's static capability text is not a live capability read-back. Keep LEVEL_2 unavailable until both binder and explicit owner authorization are confirmed.
- The wizard copy currently says the Shizuku executor is not enabled, while the client exposes read-only allowlisted read-back commands. Correct that stale disclosure after the authorization-state fix.
- Existing Shizuku UI exposes only read-back commands; mutation commands must remain owner-confirmed and narrowly allowlisted. Do not add arbitrary shell text execution.
- Auto Blocker is currently explained but not reliably detected from code. Do not claim automatic detection unless a supported Samsung signal is established; otherwise clearly label the limitation and provide owner inspection steps.
- Physical-device verification remains deferred until implementation phases are complete.

## Current exact source head
- `22fff1dc6147fed5388009544dd8107fa33cd39d`

---

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

## 2026-11-02 — Signing workflow environment propagation fix (verification pending)

- Inspected exact source/workflow and failed job logs for run `41457214306` on workflow commit `266009e3cd8a0957b66722ffd99b67ae672bc00e`.
- `testDebugUnitTest assembleDebug` completed successfully; release packaging failed at `:app:packageRelease` with `KeytoolException: keystore password was incorrect`.
- Root cause confirmed in workflow scope: the signing-validation step received `ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_ALIAS` as step-local environment variables, but only wrote `ANDROID_KEYSTORE_PATH` and `ANDROID_KEY_PASSWORD` to `GITHUB_ENV`. The later Gradle process therefore saw empty store-password/alias values. This was a workflow propagation defect, not proof that the user's keystore password itself is invalid.
- Fixed in commit `0afd2790bda7e225d42cbc592583912c154cd976`: after validation, the workflow now exports the validated keystore path, store password, alias and key password together to subsequent steps. Secret values are not printed. The key password fallback remains allowed only after `keytool -certreq` proves the store password unlocks the alias.
- The previous full CI run `41457214252` was cancelled before unit tests; only its secret-scan step completed. It is NOT a green CI result.
- Exact-head Debug/full CI for the fix commit and a signed APK/certificate output have not yet been verified. Do not claim signing continuity, do not uninstall the installed app, and do not deliver an APK until a successful signed artifact is independently inspected and its certificate is compared with the installed/base APK.
- No Android app source, Android `main`, Hermes/backend, Render, production Supabase, secrets or deployment settings were changed. Only the feature-branch workflow was corrected.

## 2026-11-02 — Android Control continuation and external repository review

- Current feature-branch checkpoint before this handoff update: `c2e9454d57bfe1a4bde37c0cd7e158d0743284c1`. Work remains only on `feature/memory-intelligence`; no Android `main`, Hermes/backend, Render, production Supabase, secrets or deployment changes.
- Signing workflow fix remains `0afd2790bda7e225d42cbc592583912c154cd976`; the handoff record is `256f24122d4e2807585d58a1823d3372de8b3b08`. The Debug workflow now exports validated keystore path, store password, alias and key password to the Gradle step. Exact-head Debug/full CI run status, signed APK artifact and certificate continuity have NOT been verified. Do not mark signing complete or deliver an APK.
- Re-audited current source against the older checklist. The earlier claims that `showUiAgentPanel()` was unreachable and Play Store install never invoked UI Agent were stale: the drawer routes `control`/`assistant_control` to the Control panel, START UI AGENT calls `showUiAgentPanel()`, and the Play Store route waits for `com.android.vending` foreground before calling `startUiAgentTask()` in `USER_REQUESTED_INSTALL` mode. A basic Control Readiness report is also present.
- Added 3 policy regression tests in `9b3b9d3` (current `UiAgentPolicyTest`: 58 tests; expected whole unit suite count rises from 144 to 147). Added pure guards in `0ad78b8`: post-action continuation requires the same foreground package and foreground epoch; OPEN_APP outcome requires the requested package to be foreground; task generation can be invalidated.
- Wired those guards in `3aa20eb`: after an action, an unexpected foreground package/epoch change stops the UI Agent instead of allowing it to continue in another app; OPEN_APP only continues when the requested package is foreground. Central STOP now invalidates pending callbacks and hides the overlay; notification STOP is persisted by `VoiceForegroundService.recordControlStop()` and is applied by the Activity receiver. These source changes are not yet verified by a CI run or physical phone test.
- Remaining Android Control gaps: Play Store publisher/developer identity and selected listing package ID are not independently bound to the install result; generic `DONE` still verifies an exact visible label rather than a domain-specific real-world outcome; the UI Agent remains Activity-owned, so background/process-death recovery is not proven; custom canvas/timeline visual control remains unsupported. Do not overstate full Android control.
- Updated `docs/VYOM_COMPLETION_CHECKLIST.md` to remove stale wiring claims and lock the user's latest implementation order: signing → Full Android Control → VoiceStudio → GitHub/ZIP skill learning → Higgsfield/Creative Studio → persistent tasks/automations → controlled self-update/rollback → remaining HUD screens → full acceptance/Memory Intelligence validation/final APK.
- External repository review: `calesthio/OpenMontage` is an independent AGPL-3.0 video-production pipeline with useful pipeline/skill/preflight/approval/cost/QA patterns, but its Python + FFmpeg + Node + Remotion/HyperFrames runtime is not proven on Android/Termux. Use as a reference/skill-learning source only; do not vendor its source into the APK without licence review. The supplied `h4ckf0r0day` string is an account, not one exact repo. Its `obscura` browser includes stealth/anti-detection and Linux/glibc binaries with no Android compatibility proof; its CAPTCHA solver is excluded. No code/dependencies were copied.
- Verification limitation: current GitHub connector status lookup returned no commit statuses for `c2e9454`, and its workflow-run lookup exposes PR-triggered runs only. A local clone/test attempt could not reach GitHub because the container has no DNS/network. Therefore tests/build/CI remain UNVERIFIED, not PASS.
- Next: verify exact-head Debug + full CI and the signed artifact/certificate first. If signing succeeds, proceed through the locked Full Android Control acceptance matrix; if it fails, diagnose only from job logs and preserve the existing installation. Physical device testing remains user-side and must not be skipped.

## 2026-11-02 — Acceptance matrix and supplied HUD design integration

- Recovered and read the user's Library source documents: **Vyom – Weakness Audit + GPT Strict Fix Order.pdf** (19 pages), **Vyom — Complete Feature & Design Brief.pdf** (10 pages), and **Vyom — Design System, Home, Drawer, Automations (1)(1).html** (31 embedded boards). Their content is now integrated by reference, not replaced.
- Created `docs/ACCEPTANCE_TESTS.md`: full A1–A16, M1–M7, S1–S7, T1–T6, V1–V8, U1–U12, F1–F6 and R1–R4 matrix, status definitions, safety/release gates and phone acceptance script. Current states are conservative; no untested item is marked FIXED.
- Created `docs/DESIGN_MAP.md`: maps all 31 HTML boards to the current native Kotlin components and records exact visual/navigation gaps. The reference palette is present in source, but the 60% glass alpha and Space Grotesk typography still need reconciliation and screenshot tests.
- Found a direct design defect in source: bottom Profile and the three-line menu both opened the Drawer. Fixed in `e30f610`: Profile now opens the actual Profile/Settings panel; the hamburger still opens the Drawer.
- Found disabled Drawer items were visibly rendered as `SOON` and their routes showed “not connected” Toasts. Added `VyomDesignSpec.visibleDrawerItems()`, filtered disabled entries out of the UI, removed the SOON badge and dead Toast route. The original item metadata remains available for future activation after implementation.
- Added 2 Drawer regression tests in `2b85cc8`; `VyomDesignSpecTest` now contains 9 tests. Together with the 3 Android Control tests, the expected unit-suite count rises from the previous 144 to 149. This count is calculated from added test methods; the tests have NOT been run in this environment.
- Updated `docs/VYOM_COMPLETION_CHECKLIST.md` to link both new documents. Latest checkpoint before this handoff append: `4b80129fd70287fed82793f2bcfdd73cc2b2e0da` on `feature/memory-intelligence`.
- Remaining design gaps include full-screen implementations for several current dialogs, true Profile hub completeness, Connections/System/Audit/Memory/Skills full screens, floating orb/status panel, Voice picker, workflow canvas, creative VFX screen, app icon and screenshot/instrumentation comparisons. The supplied boards remain the target; do not replace them with generic AlertDialogs or a WebView.
- CI/Debug/full CI, signed APK, certificate continuity and phone acceptance are still UNVERIFIED. No APK is delivered. Local test execution could not be performed because the container cannot resolve GitHub and the GitHub connector does not expose push-triggered run history.

## 2026-11-02 — Navigation and placeholder cleanup

- Followed the supplied HUD source further: Profile and Drawer are distinct destinations. The bottom Profile now opens the real Settings panel; the hamburger remains the Drawer (`e30f610`).
- Disabled Creative Studio Drawer entries are filtered out by `VyomDesignSpec.visibleDrawerItems()`; the `SOON` badge and dead Toast route are gone (`ee93a4c`, `4b82a6e`). The original destination metadata remains disabled until a real screen is implemented.
- Removed the visible `NEW AUTOMATION` button because it could not persist a schedule and only showed a “not connected” dialog (`50633dc`). It must return only after the scheduler saves, lists and executes a real schedule.
- Updated `docs/ACCEPTANCE_TESTS.md` statuses U3/U8 and `docs/DESIGN_MAP.md` board mappings. These are source-level fixes; no CI or phone test is claimed.
- Latest checkpoint before this note: `42731f530d59a7ccccd897a84cf7cbb500c03e5b` on `feature/memory-intelligence`. Expected whole unit test count remains 149 (144 previous + 5 new regression tests); not executed.

## 2026-11-02 — Voice navigation correction

- The supplied design separates Live voice from Voice settings. The centre orb remains the Live entry; Drawer `Voice` now opens the Settings panel instead of unexpectedly starting a live listening session (`b2537a3`).
- The Settings panel is still a combined Profile/Settings surface, not the final dedicated Voice Settings / Voice Picker boards. That remains a design gap; local voice selection and VoiceStudio integration are not complete.
- Updated `docs/DESIGN_MAP.md` accordingly (`be3aa06`). No CI/device verification is claimed.

## 2026-11-02 — Exact signing certificate gate

- Re-read the 19-page audit's signing instruction. Required base certificate SHA-256 is `7A:75:64:FC:A9:C2:A3:2B:23:DD:52:03:52:A4:E3:AA:FE:44:56:21:D2:B9:9A:FA:15:40:7D:23:86:69:9F:0A` (audit baseline build `2459bf1`).
- Downloaded prior Actions artifact `13146103223` from run `41455029509`, commit `7e70370d6d9d134f3fa4ba0878636a3f658f275f`. APK SHA-256 `6aa6244ed2a8511234c0103c95a36994ca2df76f514695e19a569f5b03562406` independently matches its sidecar.
- Parsed its APK v2 signing block and extracted certificate SHA-256 `BF:41:32:C9:D3:4A:2F:80:CF:03:77:0C:2D:5A:85:A8:04:AA:A8:6B:D8:DF:9B:62:84:C4:2A:2D:C8:38:6E:95`. This does not match the audit's expected `7A:75…9F:0A`; this APK is not safe to install over the existing base.
- The actual `2459bf1` APK bytes were not found in Library; expected fingerprint is sourced from the user's audit. Before release, the signed APK must match this fingerprint and the installed app must still be tested in-place.
- Updated `.github/workflows/vyom-android-debug.yml` in `4aac05e`: supports both `ANDROID_KEY_ALIAS/ANDROID_KEY_PASSWORD` and `KEY_ALIAS/KEY_PASSWORD`; adds a later-step Gradle environment check; verifies the release certificate with `apksigner`; fails release packaging if the fingerprint differs; records certificate SHA-256 in build-info.
- Bumped build defaults and workflow to `0.3.1`, versionCode `4` in `806071f`. Application ID `com.vyom.assistant` and namespace `com.hermes.assistant` were not changed.
- Added `docs/ANDROID_SIGNING.md` and linked it from the checklist; updated S6/S7 acceptance statuses.
- Current checkpoint before this handoff append: `5be4f0a155093d88ea086291dfb3ab1dbc8d6e6b`. No exact-head Debug/full CI run, signed artifact, certificate comparison or phone update test has been observed after these changes. Release remains BLOCKED/UNVERIFIED; do not deliver an APK.

## 2026-11-02 — Device action outcome honesty and readback

- Added `DeviceActionExecutor.auditState()`; unknown/legacy result text defaults to `unverified`, not `executed`. Blocked, failed, permission-required, pending, not-verified and verified are distinct audit states.
- Media volume increase/decrease/set/mute/unmute now read back Android's actual `STREAM_MUSIC` value and report `Verified` only when the actual value equals the requested value.
- Brightness writes now read back `Settings.System.SCREEN_BRIGHTNESS`; success is reported only when the stored value matches. Torch requests are explicitly `Pending verification` because the current path does not yet observe a TorchCallback. App/settings/alarm/timer/dialer/SMS/share launches are explicitly pending until foreground/result verification; they no longer claim the target action is complete. Calls/messages/shares/alarms are not claimed sent or set.
- MainActivity now logs the executor's verification state instead of treating every non-exception response as executed/confirmed; voice acknowledgement says “Checking” rather than implying success. Settings shortcuts are also logged as verification-pending.
- Added `DeviceActionExecutorTest` (2 tests) for status classification, blocked/no-op handling and unknown-result fallback. Expected whole unit suite count is now 151 (144 baseline + 3 UI Agent + 2 design + 2 device-result tests). Tests have not been run.
- Updated A6 in `docs/ACCEPTANCE_TESTS.md`. Direct app-launch foreground verification, TorchCallback verification and domain-specific postconditions for generic UI actions remain incomplete.
- Current checkpoint before this handoff append: `955a27714c8f2ef43253bc45ee02c3e3a863e8a2` on `feature/memory-intelligence`. CI, Debug/full CI, signed artifact and phone testing remain UNVERIFIED.


## 2026-11-02 — Screenshot vision control (TDD RED checkpoint)

- Full Android Control is the only active implementation scope. Existing Accessibility tree/OCR planner is text-only and cannot target unlabeled Canvas/timeline controls; screenshot capture exists locally but is not connected to visual reasoning.
- Added `UiAgentVisionPolicyTest.kt` with six regression cases for normalized screenshot coordinates, safe display bounds, bounded gestures, sensitive-screen denial and unknown gesture rejection. Added the policy skeleton in `UiAgentVisionPolicy.kt`.
- This is the test-first checkpoint. The expected failing assertions have NOT yet been executed in a CI run; do not claim RED observed. Next implement the pure geometry policy, opt-in screenshot upload to NVIDIA's documented VLM endpoint, and confirmation-gated point gestures. Default screenshot upload must remain OFF and existing text/OCR planning must remain available.
- Branch remains `feature/memory-intelligence` only. No Android `main`, Hermes/backend, Render, production Supabase, secrets or deployment changes.


## 2026-11-02 — Screenshot-based visual Android control implementation

- Added `UiAgentVisionPolicy` with normalized-coordinate mapping, screenshot/display aspect-ratio guard, 24dp edge exclusion, bounded swipe/drag geometry, and whole-screen sensitive-content denial. Added 10 focused policy tests (coordinate mapping, invalid values, aspect ratio, bounds, movement, sensitive content, unknown gesture).
- Added screenshot-based NVIDIA Vision planner using the documented OpenAI-compatible multimodal chat format and model `nvidia/nemotron-nano-12b-v2-vl`. It can propose tap/long-press/swipe/drag points for custom Canvas/timeline controls that do not expose accessibility nodes or OCR labels.
- Screenshot upload is explicitly opt-in in the UI Agent dialog and OFF by default. When enabled, a resized JPEG is uploaded to NVIDIA; the temporary local screenshot is deleted after planning. The existing text/OCR planner remains the fallback if screenshot capture or VLM planning fails.
- Visual point actions are never auto-approved by session grant: they always require explicit confirmation, fresh package/revision/foreground-epoch checks at dispatch, current per-app access + STOP checks, and a second sensitive-screen scan. No password/OTP/payment/security/permission screen may receive coordinate gestures.
- Source implementation is committed on `feature/memory-intelligence`. CI/build and physical-device runtime verification are not yet confirmed. Screenshot upload and Canvas/timeline interaction remain to be tested on-device; do not describe Android's protected/secure surfaces as controllable.


## 2026-11-02 — Visual-control safety hardening

- Expanded the sensitive-screen deny scan to include destructive/consequential labels (delete/remove/send/share/post/publish/purchase/checkout/subscribe/approve/grant/install/uninstall/confirm), OTP-like numeric strings, card-like number sequences and email addresses.
- Added regression coverage proving screenshot point actions always require confirmation even under a temporary session grant, and reject stale observations or sensitive-screen context.
- Fixed temporary screenshot bitmap cleanup on incompatible aspect ratios, decode/resize errors and successful image preparation; fixed the screenshot-copy bitmap cleanup in the local capture method.
- Verification remains pending for the latest commit. Do not claim the screenshot VLM path or Android gestures runtime-tested until exact-head CI and physical-device checks complete.


## 2026-11-02 — Global Android navigation actions

- Extended the UI Agent planner/action policy with Android global Back, Home, Recents, Notifications shade and Quick Settings actions. These use AccessibilityService global actions and still require a fresh allowed foreground app, active STOP check and post-action foreground/revision re-observation.
- If a global action transitions into an unapproved System UI/launcher surface, the task stops rather than continuing blindly in a new package. Added a policy regression test for the allowed global-action set.
- Latest source remains on `feature/memory-intelligence`; verification for the resulting commit is pending. This does not bypass Android protected surfaces or OEM restrictions.


## 2026-11-02 — Visual target confirmation markers

- Screenshot-derived point actions now show a non-touchable on-screen marker at the exact proposed tap/long-press point; swipe/drag shows labelled start/end markers. The user can inspect the actual target before pressing CONFIRM.
- Marker overlays are removed together with the confirmation/progress overlay and do not receive touch events. If a marker cannot be displayed, the confirmation step fails closed and the gesture is not dispatched.
- Narrowed the VLM prompt to visual point actions only; normal text/OCR planner owns app launch, labeled controls and Android global navigation.

- Prompt reconciliation: the text-only planner schema explicitly lists Back/Home/Recents/Notifications/Quick Settings; the screenshot VLM schema is restricted to point gestures plus `none` (which falls back to the text planner).

- Screenshot capture now closes the hardware buffer and recycles both hardware/software bitmap handles in a `finally` path, including file-write failures. Additional sensitive-context tests cover destructive labels, numeric OTP-like values and email addresses.

- Final prompt/safety adjustment: the screenshot VLM is instructed to ignore Vyom's own floating progress/STOP overlay; export actions are included in the sensitive-screen deny scan. Marker circles now use the explicit Android oval shape setter.


## 2026-11-02 — Android screenshot capability and overlay-safe capture

- Declared `android:canTakeScreenshot="true"` in the Accessibility Service metadata. Android's API explicitly requires this capability for `takeScreenshot` / `takeScreenshotOfWindow`.
- On Android 14+ (API 34), both local OCR and opt-in visual planning now use `takeScreenshotOfWindow` for the exact active app window, so Vyom's own accessibility overlay does not cover the app screenshot. Android 11–13 retain the display screenshot fallback.
- Secure-window screenshots remain blocked by Android and are not bypassed.

- Coordinate mapping correction: OCR remains based on full-display screenshots to preserve its established display-coordinate mapping. Visual AI uses active-window screenshots on API 34+ and maps normalized VLM points through the captured window's actual screen bounds; API 30–33 use full-display bounds.

- Visual point actions also fail closed whenever any editable or password input field is present in the active window; text entry continues through the separately validated accessibility-node path.

- Tightened screenshot/window aspect-ratio tolerance to 5% so coordinate mapping fails closed rather than silently shifting targets when the captured window and screen bounds differ. Temporary screenshot files are cleared before a new capture and deleted in the planner `finally` path; OCR hardware buffers are now closed/recycled on both success and failure.

- Enabled `flagRetrieveInteractiveWindows` so API 34+ can resolve the exact active accessibility window and its on-screen bounds before calling `takeScreenshotOfWindow`.


## 2026-11-02 — Controlled Android System UI / Quick Settings access

- Added Android System UI (`com.android.systemui`) as a separate manual opt-in in Profile > App access. It is excluded from ENABLE ALL and is not launchable as an ordinary app.
- After an explicit Quick Settings request, the UI Agent may continue into System UI only when that package is manually enabled. Notification-shade access does not receive this transition exception.
- System UI observations are filtered to a narrow allowlist of quick-setting labels (Wi-Fi/Internet, Bluetooth, Airplane mode, DND, Battery Saver, Flashlight, Location, Auto-rotate, Screen record, Hotspot, Mobile data, Brightness). Other notification text is never sent to the planner. Screenshot/VLM upload is disabled while System UI is foreground.
- System UI actions require the user's goal to name the matching setting and always require confirmation; only exact safe tile taps and the Brightness slider drag are permitted. Coordinate gestures, typing, scrolling and arbitrary navigation are rejected. Added policy regression tests for label filtering, goal matching, mandatory confirmation and point-action rejection.

- Corrected System UI label/goal regexes to use Kotlin raw-string regex escapes (word boundaries and whitespace). The quick-settings allowlist now matches actual labels instead of literal backslash sequences.

- Tightened System UI isolation further: only nodes whose package is exactly `com.android.systemui` survive the filter; OCR text is never sent to the planner for System UI, and screenshot/VLM remains disabled there. If System UI access is off, the agent explains the required manual opt-in instead of silently stopping.

- Tightened System UI interaction from text/OCR targets to exact clickable accessibility-node IDs/fingerprints only; the only non-clickable exception is the labelled Brightness SeekBar, which permits a confirmed drag. Notification text cannot become a tappable target merely because it resembles a safe label.

- Quick Settings completion now requires a visible state matching the requested direction (e.g. Wi-Fi On for “turn on Wi-Fi”, Off for “turn off Wi-Fi”); merely seeing the tile no longer satisfies DONE. If System UI opt-in is missing after opening Quick Settings, the user receives the exact Profile > App access instruction.

- Accessibility tree entries now retain each node's actual package name (while the screen observation still uses the active root package). This lets the System UI filter exclude notification RemoteViews owned by other apps rather than treating every child as `com.android.systemui`.

- System UI tree filtering now redacts all non-allowlisted text/description/hint fields even on an otherwise safe tile node; only the safe tile label and non-text geometry/fingerprint metadata are forwarded. State labels with “is on/off” phrasing are recognized.

- The System UI label matcher also accepts common “is On/Off” state phrasing while keeping the exact allowlist.

- Replaced the System UI state-label regex as a complete block after detecting an escaped-string replacement error; the current Kotlin raw-string pattern is syntactically complete and covers optional “is on/off” status.

- Quick Settings DONE verification now requires every requested toggle category to be visibly in the requested on/off state; mixed-direction requests fail closed rather than claiming partial completion. Opening Quick Settings itself is recognized as a separate visible-panel goal.

- Added an explicit non-null state guard in the multi-toggle verification path so absent accessibility state labels cannot be treated as a valid toggle state.

- Node-based confirmation now displays the exact visible accessibility label (e.g. Wi-Fi or Brightness) instead of only a numeric node index, so users can judge the target before approving.

- Kept “open notification shade” separate from “open Quick Settings”; only the explicit Quick Settings path receives the controlled System UI continuation exception.


## 2026-11-02 — CI compile fixes

- CI run 41479233768 exposed three Kotlin compile errors: allowedApps was out of scope in policy validation, one no-screenshot path still called the old one-argument planner lambda, and the VLM request's outer try/finally was not returned from the nullable action function.
- Fixed all three: one immutable allowlist snapshot is shared by validation and planner, the fallback passes (null, null), and the VLM request now returns its try/finally expression. Latest verification must be rerun; no APK is ready from the failed run.

- Follow-up compile correction: the allowlist snapshot is now declared in the OCR callback scope before the planner lambda, so both policy validation and background model planning can access the same list.

- Explicitly excluded com.android.systemui from both the regular launcher-app list and ENABLE ALL, even on OEMs that expose it as a launchable activity; it remains a separate manual opt-in.

- The UI Agent's OPEN_APP candidate list also explicitly excludes System UI; Quick Settings is reachable only through the dedicated global action and opt-in transition.

- Added System UI direction guards: a tile already in the requested On/Off state cannot be tapped again, ambiguous/mixed-direction toggle goals are rejected, and Brightness drags must move in the requested direction (left-to-right for brighter, right-to-left for dimmer).


## 2026-11-02 — Sensitive-screen preflight

- Moved accessibility-label/node extraction ahead of the model request. Credential-like or sensitive screens now stop before any screen tree/OCR is sent to the planner.
- When an editable field is present, Vyom keeps text-entry via the redacted accessibility tree but disables OCR and screenshot/VLM sharing for that step. System UI remains OCR- and screenshot-free.

- Separated sensitive content from consequential controls: passwords/OTP/payment/security/credential-like content still blocks planner upload, but labels such as Send/Delete/Export/Post no longer block the whole screen. Those actions remain governed by UiAgentPolicy's mandatory confirmation path. App Access now explicitly discloses that visible UI text may be sent to the configured planner; screenshot upload remains a separate Visual AI opt-in.

- CI compile/test run 41481075757 reached unit-test compilation and found one duplicate test declaration for safe global navigation. Removed the duplicate; the production Kotlin sources compiled in that run, and the next run will verify the full test suite.

# OWNER FULL-CONTROL + LEARN-BEFORE-GUESS CHECKPOINT — 2026-11-03

## Scope and branch
- Repository: `riyas433554-dot/vyom-android`
- Branch: `feature/memory-intelligence`
- Starting head verified before this work: `3c98ba5bf2b084cf37619ff19ff8548d0dc4d741`.
- All changes in this continuation are on the Android feature branch. Android `main`, Hermes backend/Telegram, Render, production Supabase, deployment and signing secrets are untouched.

## Owner permission / Android Control work
- Added `AndroidPermissionPolicy.kt` and `AndroidPermissionPolicyTest.kt`. Core UI control readiness requires the Accessibility setting, a connected service, Master App Control and at least one enabled target app. Optional permissions are task-scoped, not blanket prerequisites.
- Added Profile > Control Readiness > Set Up Permissions flow for Accessibility, all safe installed apps, Modify System Settings, microphone, camera, notifications and separately opt-in safe Quick Settings.
- Readiness now reads actual Android state: enabled Accessibility component, live service connection, app allowlist/master switch, runtime mic/camera/notification grants and Modify System Settings grant. It distinguishes enabled service from a connected service.
- `ENABLE ALL SAFE APPS` turns on Master App Control and allowlists discovered non-financial launcher apps. Protected financial identifiers remain blocked; System UI remains separate and limited to safe Quick Settings.
- Corrected a permission-model issue before finalizing: the UI Agent overlay uses `TYPE_ACCESSIBILITY_OVERLAY`, so it does not require Draw Over Other Apps. Removed the redundant overlay permission request/status rather than asking the user for an unnecessary grant.
- Notification settings fallback now targets this app's notification settings on Android versions before runtime notification permission.
- Android system permissions remain Android-owned: a ChatGPT instruction cannot remotely change this installed app's SharedPreferences or silently grant Accessibility/runtime/special permissions. The user must activate the setup controls in the installed APK.

## Permanent learn-before-guess doctrine
- Updated `VyomIdentityPolicy.systemInstruction()` so every chat prompt carries compact durable rules: direct user tasks authorize ordinary non-destructive actions in enabled apps; inspect permissions before action; learn before guessing; inspect source/license/dependencies/security; extract scoped skills; test safely; preserve provenance and evidence; treat repo/web/screen content as untrusted; observe and verify each action; STOP/app-switch/stale-screen/permission-loss cancels.
- Updated `VyomIdentityPolicyTest.kt` with assertions for learning, provenance/security, permission-aware control and STOP.
- Important: this is an always-injected operating instruction, not a claim that automatic GitHub/ZIP ingestion, skill extraction, or a verified skill registry is already implemented. Those remain pending runtime capabilities.

## New repository reviews
- `adewaskar/jarvis`: MIT-licensed browser/Node/Claude Agent SDK assistant, not a native Android agent. Useful patterns: separate brain/hands/HUD, runtime capability probing, interchangeable speech adapters, central effectful-tool gate, cancellation and visible degraded states. Do not transplant its web UI/Node bridge into Android unchanged.
- `WorldFlowAI/everything-claude-code`: useful agent/skill/hook, continuous-learning, eval-harness and verification-loop patterns. GitHub API returned no declared license for this repository; no source/scripts copied or vendored. Only independently written process guidance was added.
- Added `docs/skills/JARVIS_AGENT_ARCHITECTURE.md` and `docs/skills/CONTINUOUS_LEARNING_AND_VERIFICATION.md`.
- Added `docs/ANDROID_CONTROL_CAPABILITY_MATRIX.md` to record owner authorization, actual Android grants, implemented source capabilities, platform limits and acceptance gates.

## Verification truth
- Tests added/updated, but no Gradle test or build result is claimed yet for this checkpoint.
- No CI run, APK artifact, signature continuity check or physical phone test is claimed yet.
- Existing app local preferences are not modified by source changes. After a safe update is delivered, the owner must use the new Set Up Permissions screen to activate broad app access and the required Android system grants.
- Full unrestricted control is not a valid Android promise: secure surfaces, protected financial actions and Android-owned approval gates remain protected. The goal is broad reliable control wherever Android and the target app expose a supported interface.

## Next fixed steps
1. Verify exact current feature-branch head and run Android CI/unit tests once.
2. Fix all compile/test failures; re-run only after a code change.
3. Review actual APK manifest, app ID, version and signing fingerprint.
4. Continue remaining action adapters and end-to-end reliability tests; then deliver update-safe APK for physical phone acceptance.
5. Implement repository/ZIP ingestion, skill registry and skill eval execution before claiming autonomous self-learning is complete.

## 2026-11-03 — Permission readiness and event freshness follow-up

- Core readiness now requires: Android Accessibility enabled, service actually connected, Master App Control ON, at least one safe target app, NVIDIA UI planner configured, UI Agent skill enabled and STOP CONTROL OFF. Missing states are explicitly listed.
- Added regression coverage for missing planner, disabled UI Agent skill and STOP state.
- Added WRITE_SETTINGS to AndroidManifest so Android's Modify System Settings approval screen can grant the special permission; the readiness panel checks Settings.System.canWrite().
- Notification settings fallback now includes the app package extra on older Android versions.
- Accessibility observation revisions now also advance on click, selection and focus events, not only window/content/scroll/text changes. Added policy tests and subscribed to those event types in the service metadata.
- Corrected the event import ordering before any build; final CI is still required.
- Corrected permission inventory: the UI Agent progress card uses TYPE_ACCESSIBILITY_OVERLAY, so no Draw Over Other Apps grant is needed.
- Latest implementation checkpoint before this handoff append: source commit `aee4dde2b91ce3e502c3dc139ebdf31753993a87`; latest docs commit is expected to follow this append. The current branch head must be re-read before CI evidence is recorded.
- No Gradle tests/build, CI success, APK artifact, signing verification or physical phone test is claimed yet.

## 2026-11-03 — Owner recovery and app-target correctness

- Owner-tapped Android Settings navigation is no longer blocked by STOP CONTROL. This allows the owner to open Accessibility/special-permission settings and repair grants while the UI Agent remains stopped; automated device actions remain blocked by STOP.
- Corrected allowed-target count and ENABLE ALL SAFE APPS to exclude Vyom's own controller package. Readiness can no longer appear ready merely because the controller itself is listed.
- These are source changes only. Build/CI/device verification remains pending for the latest branch head.

## 2026-11-03 — Encrypted memory seed for learn-before-act

- Added `VyomMemoryStore.ensureCorePrinciples()`. On app startup, when encrypted memory is enabled, it additively seeds the user's explicit preference: if Vyom lacks knowledge/skill, learn from trusted docs/repos, review license/source/dependencies/security, test safely, then act and report only verified results.
- Existing memory facts are preserved; the principle is deduplicated and stored through the existing EncryptedSharedPreferences store. If memory is disabled, no seed is written; the permanent identity instruction still carries the operating rule in chat/live prompts.
- Added `VyomMemoryPrincipleTest` to verify the seeded rule contains the expected process and is not classified as sensitive.
- This persists the preference but does not itself implement GitHub/ZIP ingestion or a learned-skill registry; those remain explicitly pending.

## Test inventory at source head e3f8671

- Counted all 19 Kotlin test files in the feature-branch tree: 177 `@Test` methods are present (including the new encrypted-memory principle test).
- This is a source inventory only, not a passing-test claim. The exact-head CI and Debug workflows are queued; no test execution result has been observed.


---

# AUTHORITATIVE CONTROL / AUTONOMY MATRIX — 2026-11-03

This section is the current acceptance contract and supersedes any older shorthand matrix. Scope remains Android repository only, branch `feature/memory-intelligence`. Never modify Android `main`, Hermes/Telegram, Render, production Supabase, secrets, or deployment.

## SHA and evidence hygiene
- Always record full 40-character commit SHAs; never abbreviate a SHA in status records.
- Status vocabulary: `DONE` requires implementation + named automated tests + exact-head green CI + required runtime acceptance; `PARTIAL` requires a named test that proves the limited behavior stated; `UNVERIFIED` means no named proving test or no current evidence; `BLOCKED` means an explicit dependency/safety gate.
- A test name proves only the behavior it asserts. Source presence, test inventory, compilation, or CI success does not prove physical-device behavior.
- For every PARTIAL row, record the exact test class/method. If no such test exists, downgrade to UNVERIFIED.
- Never claim a CI run belongs to a commit unless its head_sha exactly matches that full SHA.

## P1-P8 exact definitions and acceptance

### P1 — Power Mode
Owner-led, one-time Wireless ADB setup initiated from Termux. Guided steps must support Samsung One UI 8 / Android 16:
1. Detect Wireless debugging availability and explain that Wi-Fi must be enabled and phone/Termux must be on the same local network.
2. Pair with the Android Wireless debugging pairing code. Because Samsung may dismiss the pairing-code dialog when switching apps, show split-screen or pop-up-view Termux instructions before the user opens pairing; do not assume the code remains visible.
3. Detect Samsung Auto Blocker / USB command blocking and show the exact Settings route to disable it temporarily only if it is actually blocking the owner-authorized setup. Never silently toggle it.
4. Owner runs the pairing/grant commands in Termux; no app silently pairs, grants, or changes global security settings.
5. Explicit owner-run `pm grant <package> android.permission.WRITE_SECURE_SETTINGS` only after ADB pairing; verify with `pm check-permission`/package permission read-back where supported. Do not assume command success.
6. Guide owner through required `appops` for overlay and all-files only where the app's declared features require them; read back each mode and display exact state.
7. Shizuku must be installed, started by the owner, and separately authorized. ShellExecutor must use a strict command allowlist, argument validation, bounded timeouts, output redaction, audit records, and immediate STOP cancellation. No arbitrary shell, root assumption, or silent privilege escalation.
8. Accessibility service reconnect is attempted only after verified grant and owner action; show actual enabled/connected state, never infer it from an ADB command.
9. Live read-back checklist and capability table:
   - Level 0: ordinary app APIs / granted runtime permissions only.
   - Level 1: Accessibility and explicitly granted special app access; protected/secure surfaces remain blocked.
   - Level 2: owner-authorized ADB/Shizuku operations, limited to allowlisted commands; still not root and not a bypass for biometrics, OTPs, payment approval, secure windows, Play Protect, or Android security prompts.
Acceptance: setup wizard accurately detects each state, explains Samsung-specific failure points, supports owner cancellation/STOP, and tests every read-back state. Physical Galaxy A06 validation remains required.

### P2 — Learn Anywhere loop
Mandatory lifecycle: KNOW-CHECK -> RESEARCH (web_search, web_fetch, GitHub read, Play Store, docs, screen) -> LEARN (Knowledge notes + draft SKILL.md) -> TRY (dry run or low-risk) -> DO -> VERIFY -> SAVE.
Acceptance test: ask “Instagram mein story kaise lagate hain” when Vyom has no skill. It must discover the gap, research sources, draft/try a safe skill, verify the actual outcome, save the skill and report what it learned. Also ingest:
- `github.com/adewaskar/jarvis`
- `github.com/WorldFlowAI/everything-claude-code`
- `github.com/morluto/rea` — tag as desktop-only; do not imply Android compatibility.
For each repo, record readable files, unreadable files, license, provenance, security review, extracted concepts, and whether any code was actually copied. Report honestly what was readable.

### P3 — Capability Ledger and self-awareness
Per skill/action record required permission, actual permission state, last result, success rate, latency, and known failure causes. Inject a compact, bounded capability summary into every relevant prompt. Before saying “I can't”, consult the ledger and runtime capability manifest; name the exact missing permission and the one-tap Power Mode fix when available. Include a “what I know about this phone” map: installed apps and learned UI paths. Stale capability data must be marked stale and refreshed before action.

### P4 — Continuous learning
Model the continuous-learning pattern from `WorldFlowAI/everything-claude-code`: after any session with 10+ messages or any failure-then-success, extract `error_resolution`, `user_corrections`, `workarounds`, and app-specific UI paths into candidate learned skills. Default “auto-approve learned skills” ON only for non-protected skills; protected/sensitive skills always require review. Provide an editable Learned Skills list, provenance, version, enable/disable, rollback and deletion. Never convert secrets or sensitive personal data into skills.

### P5 — Jarvis brain
Intent + slots + coreference resolution (including “usse”, “wahi”, “pehle wala”); per-task working memory; plan-act-verify-reflect with explicit step budget, retry budget and loop detection; “ye kya hai” invokes current-screen capture + local vision/OCR + grounded explanation and only acts when asked/authorized. Proactive suggestions are opt-in. Optional “Hey Vyom” wake word is OFF by default and requires explicit microphone/privacy disclosure and owner enablement.

### P6 — Human-like fast voice
Streaming STT partials, streaming LLM tokens, TTS starts on first complete clause; target first audio <800 ms p50 and <1.5 s p95 measured and shown in Diagnostics (not promised until measured). Semantic end-of-turn; barge-in target <=150 ms; truthful short fillers while a task runs; mirror user's language; do not read Markdown syntax, raw URLs or emoji aloud; cache common phrases; Indian voice default for Hindi/Hinglish. Measure on the actual device/network and report sample count and percentile method.

### P7 — Competitor parity
Implement comparable product patterns without copying proprietary UI/assets:
- Gemini: system-wide assistant bubble and ask-about-screen.
- ChatGPT: interruptible voice, memory manager, Projects, scheduled tasks.
- Grok: compact composer, personalities, Automations.
- Claude: skills/connectors list.
- Perplexity: source chips, follow-up suggestions, Deep research, Spaces.
Use one consistent line-icon set and no emoji in app chrome. Settings groups: Account / Appearance / Voice / Memory / Skills / Connectors / Permissions / About. Do not mark parity complete where a real capability/backend executor is absent.

### P8 — System-wide floating assistant bubble/orb
One overlay service shared with A16. Show “How can I help you today?” sheet above other apps, support ask-about-screen with current-screen freshness/privacy gates, and tap-to-expand into full Vyom. Respect overlay permission, foreground service lifecycle, Android background restrictions, per-app exclusions, STOP and owner hide controls. A16 auto-show/hide during control tasks must be implemented in this same service, not a second competing overlay.

## Full ID matrix — current conservative status
“PARTIAL” below is limited to the named source test and is not device acceptance. “UNVERIFIED” is mandatory wherever no proving test is named.

| ID | Scope | Status | Evidence / remaining acceptance |
|---|---|---|---|
| A1 | Play Store publisher/package/version verification | UNVERIFIED | Add publisher identity and package/version tests; real Play Store verification |
| A2 | Hinglish install routing and APK rejection | UNVERIFIED | Named routing tests required |
| A3 | Installed-app discovery/open | UNVERIFIED | App discovery/launch tests and device check |
| A4 | Permission inventory/readiness | PARTIAL | `AndroidPermissionPolicyTest`; verify live Samsung settings/read-back |
| A5 | UI Agent policy entry points | PARTIAL | `UiAgentPolicyTest`; full entry-point execution tests |
| A6 | Action postconditions | UNVERIFIED | Add action-specific outcome tests |
| A7 | Multi-step plan/verify/pause | PARTIAL | `UiAgentPolicyTest`; bounded loop and pause/resume acceptance |
| A8 | Floating orb controls | UNVERIFIED | Shared overlay service tests + physical overlay |
| A9 | Special permission/Power Mode inventory | PARTIAL | `AndroidPermissionPolicyTest`; actual ADB/appops/Shizuku read-back |
| A10 | Hinglish typo-tolerant routing | UNVERIFIED | Named parser tests required |
| A11 | Runtime capability manifest in prompts | UNVERIFIED | Prompt-injection and stale-ledger tests |
| A12 | Structured tool-call parsing/execution | UNVERIFIED | Schema validation, dispatch and raw-JSON suppression tests |
| A13 | Clear-command fast path | UNVERIFIED | Routing tests for clear vs ambiguous instructions |
| A14 | TRUSTED default / consequential-action confirmation | PARTIAL | `VyomIdentityPolicyTest`; full policy matrix tests |
| A15 | Persistent task stack/pause/resume/cancel | UNVERIFIED | Persistence/restart/cancel tests |
| A16 | Auto-show/hide control-task orb | UNVERIFIED | Shared P8 overlay lifecycle tests |
| M1 | Memory capture/retrieval | PARTIAL | `MemoryCapturePolicyTest`; broader paraphrase/semantic coverage |
| M2 | Core/episodic/recent memory tiers | UNVERIFIED | Tier isolation and retrieval tests |
| M3 | Conversation restoration/scroll | UNVERIFIED | Restoration tests |
| M4 | Context budget/fallback | UNVERIFIED | Budget and truncation regression tests |
| M5 | Project/scope injection | PARTIAL | `VyomIdentityPolicyTest`; cross-project isolation tests |
| M6 | 500-fact cap/dedupe/conflict/migration | PARTIAL | `VyomMemoryPrincipleTest`; staging/migration tests |
| M7 | Project aliases linked to skills | UNVERIFIED | Alias resolution and linking tests |
| S1 | Procedures/preconditions/outcomes/rollback | UNVERIFIED | Procedure contract tests |
| S2 | Self-change branch targeting | UNVERIFIED | Branch isolation tests |
| S3 | Repository analysis/minimal patch | UNVERIFIED | Patch scope and diff tests |
| S4 | Patch -> tests -> PR -> CI -> artifact/update | UNVERIFIED | End-to-end self-change workflow tests |
| S5 | Reviewable fixes from audit failures | UNVERIFIED | Review/approval tests |
| S6 | Release signing validation | UNVERIFIED | Signing fingerprint tests and CI |
| S7 | Candidate APK certificate continuity | UNVERIFIED | Compare actual candidate and installed/base certificate |
| T1 | Durable scheduled work/reboot/Doze | UNVERIFIED | WorkManager/AlarmManager behavior tests |
| T2 | Executors/artifacts/retry/cancel | UNVERIFIED | Executor integration tests |
| T3 | Task detail/log/result/retry/cancel UI | UNVERIFIED | UI state/action tests |
| T4 | Queue limit and clipping | UNVERIFIED | Small-screen/large-font tests |
| T5 | n8n-style workflow canvas/execution | UNVERIFIED | Canvas serialization/execution tests |
| T6 | Persistent approval UI | BLOCKED | Android approval UI in scope; server-side/Telegram persistence excluded |
| V1 | Voice state/echo/barge-in | PARTIAL | `VoiceStateMachineTest`; real audio latency/barge-in test |
| V2 | Hindi/Hinglish/English STT cleanup | PARTIAL | `SttTranscriptCleanerTest`; device transcription tests |
| V3 | On-device voice latency metrics | UNVERIFIED | Measure p50/p95 on Galaxy A06 |
| V4 | Beep/custom sound behavior | UNVERIFIED | Audio output tests |
| V5 | Live model/fallback policy | PARTIAL | `LiveModelPolicyTest`; live provider/fallback verification |
| V6 | Background/reboot/STOP/mute | PARTIAL | `VoiceStateMachineTest`; device lifecycle tests |
| V7 | Indian Hindi/Hinglish voice quality | UNVERIFIED | Human listening evaluation |
| V8 | Language detection/instant greeting | UNVERIFIED | Language-routing tests + device latency |
| U1 | Orb/home shell | PARTIAL | `VyomDesignSpecTest`; overlay is separate P8 acceptance |
| U2 | Small screen/large font | UNVERIFIED | Screenshot/layout tests on supported sizes |
| U3 | Drawer destinations | UNVERIFIED | Destination routing tests |
| U4 | Design fidelity | PARTIAL | `VyomDesignSpecTest`; screenshot comparison |
| U5 | Self-change proposal/diff UI | UNVERIFIED | Proposal rendering and approval tests |
| U6 | Theme tokens | PARTIAL | `VyomDesignSpecTest`; full screen token coverage |
| U7 | Markdown/code/error rendering | UNVERIFIED | Rendering tests for long and malformed content |
| U8 | Separate Drawer/Profile destinations | UNVERIFIED | Navigation tests |
| U9 | Compact Grok-style composer | UNVERIFIED | Composer interaction/layout tests |
| U10 | Workflow canvas UI | UNVERIFIED | Canvas editing/persistence tests |
| U11 | Design-board mapping/screenshot comparison | UNVERIFIED | Per-screen visual regression |
| U12 | Adaptive app icon/splash | UNVERIFIED | Manifest/resource and launcher verification |
| F1 | Attachments in/out/share receiver | UNVERIFIED | URI permission, preview, send/receive tests |
| F2 | GitHub/ZIP learning + provenance | UNVERIFIED | P2 repo-ingestion acceptance and license/security tests |
| F3 | SELF.md/runtime self-model | PARTIAL | `VyomIdentityPolicyTest`; ledger freshness and prompt tests |
| F4 | Auto-capture + visible facts + 500 cap | PARTIAL | `MemoryCapturePolicyTest`; capture rate/limit UI tests |
| F5 | GitHub token encryption/masking | UNVERIFIED | Secret storage/log/export redaction tests |
| F6 | Authenticator/password-manager hard block | PARTIAL | `AppAccessPolicyTest`; actual screen/tap denial tests |
| P1 | Power Mode owner-led ADB/Shizuku | PARTIAL | `PowerModePolicyTest.shizukuBinderWithoutOwnerAuthorizationDoesNotGrantLevelTwo`; `PowerModePolicyTest.levelTwoRequiresLiveBinderAndExplicitOwnerAuthorization`; remaining owner setup/read-back, Auto Blocker, STOP and Galaxy A06 tests pending |
| P2 | Learn Anywhere loop | UNVERIFIED | Instagram story skill acceptance + three-repo ingestion ledger |
| P3 | Capability ledger/self-awareness | UNVERIFIED | Permission/result/latency ledger and prompt injection tests |
| P4 | Continuous learning | UNVERIFIED | 10+ message and failure-success extraction tests |
| P5 | Jarvis brain | UNVERIFIED | Coreference, task memory, loop budget and screen-vision tests |
| P6 | Human-like fast voice | UNVERIFIED | Measured p50/p95, barge-in and streaming tests |
| P7 | Competitor parity | UNVERIFIED | Feature-by-feature acceptance; no fake parity |
| P8 | System-wide assistant bubble | UNVERIFIED | Shared overlay service physical validation |
| R1 | Normalized coordinate fix | PARTIAL | `UiAgentVisionPolicyTest.normalizedCoordinatesMapToScreenPixels`; see CI record below |
| R2 | Handoff matrix commit | DONE | This commit; verify commit SHA after push |
| R3 | Package/version/signing identity | UNVERIFIED | Manifest + certificate continuity verification |
| R4 | Diagnostics/PHONE_TEST_GUIDE | UNVERIFIED | Diagnostics tests and device-specific guide |

## Exact CI record for coordinate checkpoint
- Commit: `9ba2cc7f5e3c4a5a5385f7f569d04828c4041399`.
- Android CI run `41553588741`: SUCCESS on exact SHA. Secret scan, unit tests/debug APK build, checksum packaging and artifact upload passed.
- Debug workflow run `41553588766`: FAILED on exact SHA at packaging step 8, after Gradle debug/release build succeeded. Exact failure: `Could not extract a valid SHA-256 signing certificate fingerprint.` The workflow's `apksigner verify --verbose --print-certs` output did not match the parser's expected line format; no artifact uploaded. This is a packaging/certificate-parser failure, NOT a failing unit test. No exact test failure was reported for this SHA.
- Current branch head at time of this update: `9ba2cc7f5e3c4a5a5385f7f569d04828c4041399`.
- No APK from this checkpoint is approved for delivery. Fix certificate fingerprint extraction, rerun exact-head CI/debug, and verify artifact and certificate before final APK.

## Mandatory implementation order
1. P1 Power Mode.
2. Authorization + structured tool-call execution (A12-A14).
3. UI Agent / Play Store install / send-message skills (A1-A7, A15-A16).
4. Learn Anywhere + provenance ledger + self-model (P2-P3, F2-F3).
5. Continuous learning + memory (P4, M1-M7, F4).
6. Attachments (F1).
7. Jarvis brain, navigation/composer/canvas/design and competitor parity (P5, U1-U12, T5, P7).
8. Human-like voice (P6, V1-V8).
9. One shared system-wide floating overlay (P8 + A8 + A16).
10. Diagnostics + refreshed Galaxy A06 PHONE_TEST_GUIDE (R4).
11. One final APK only after all required work, exact-head green CI, artifact checksum, package/version and signing-certificate verification. No interim APK delivery.

## Galaxy A06 device profile
- Product: Samsung Galaxy A06; model SM-A065F/DS.
- One UI 8.0; Android 16.
- Firmware: A065FXXUECZI1 / A065FOJMECZI1 / A065FXXUECZI1.
- Security patch: 2026-07-05.
- Wireless debugging requires Wi-Fi; same-network and pairing-dialog handling must be explicit.
- Pairing dialog may disappear when switching apps: instruct split-screen or Samsung pop-up view for Termux before opening the pairing dialog.
- Detect Samsung Auto Blocker and explain its exact user-controlled setting; do not silently disable it.
- Device test is pending; firmware profile is user-supplied and not a substitute for runtime validation.


---

## P1 implementation progress — 2026-11-03

- Current sequence: P1 is active; no later milestone has been started.
- Source commits (full SHAs):
  - Policy tests first: `02bca50e61ae92d806d1d2050ec99c1db970e2f0`.
  - Pure setup policy/command validation: `65cb086adac830400fe33799253ccaabbe0d7160`.
  - Control screen owner-led Power Mode guide: `05799da798c68751d7769904ef46c304b03078b2`.
  - Declared `SYSTEM_ALERT_WINDOW` for the planned overlay capability: `6cb6d6f5b53fdff4caf62abb4d99689639a56721`.
- New test class: `PowerModePolicyTest` (Wireless ADB Wi-Fi/network gates, host:port validation, app-ID-safe command generation, malformed-input rejection, grant-before-Accessibility-reconnect, scoped appops, and Level 2 protected-surface boundary). Tests have not yet been run on this source head; status remains UNVERIFIED pending exact-head CI.
- The UI prepares/copies commands only. It does not execute ADB or grant permissions. The owner must run commands manually in Termux and review each command.
- Source inspection found a critical package identity detail: Gradle `applicationId` is `com.vyom.assistant`, while Kotlin namespace is `com.hermes.assistant`. Power Mode commands use runtime `packageName`; never hardcode the namespace as the installed package.
- Wizard includes Wi-Fi/same-network requirements, separate pairing/debug endpoints, split-screen/pop-up Termux guidance for the disappearing pairing-code dialog, grant/read-back commands, Accessibility read-back, and Samsung Auto Blocker caveat.
- All-files access is intentionally not requested because current file handling uses the document picker/SAF; no `MANAGE_EXTERNAL_STORAGE` app-op is emitted unless that feature is later explicitly declared and justified.
- Samsung Auto Blocker is not automatically disabled. Official Samsung documentation describes USB-command blocking and unauthorized-source install blocking. Wireless ADB should be preferred; only temporarily disable the specific Auto Blocker feature if the owner confirms that exact blocked path is needed, then re-enable it.
- Shizuku UserService/ShellExecutor integration is NOT implemented yet. The UI explicitly says it is not enabled. Remaining P1 work: Shizuku API integration, explicit Shizuku authorization/binder state, strict command allowlist, bounded execution, audit/redaction, STOP cancellation, live permission/app-op read-back and verified Accessibility reconnect gating.
- No physical Galaxy A06 test has been performed. P1 is not complete; no APK is approved.
- Current branch source head at time of this note: `6cb6d6f5b53fdff4caf62abb4d99689639a56721`.


### P1 Shizuku executor progress — 2026-11-03 (not yet accepted)
- Additional source commits:
  - Shizuku command-policy tests: `fecf058a7271e093a6b3e2b2539f8354c336ab75`.
  - Strict command allowlist: `6825d09c38090b42b88259577dcfc3dde38bedaa`.
  - AIDL contract: `41fdd50939710033c03038f506bd4ff8a45a245b`.
  - Bounded UserService: `c7e174ef00c36b84a2f6e89f6ba222ecb76af7ff`.
  - Shizuku 13.1.5 API/provider dependencies: `07850af4d6227ec169d0a88ec3be5b214af1c43c`.
  - Provider registration: `89df7549d67f1de501ed52660796a78f328b31cc`.
  - Owner authorization/read-back UI and STOP hook: `58c03d8b045aa060e02e4f04f4b4db53dd04b8b8`.
  - Pre-start/active STOP cancellation race hardening: `ee9f5be8471cb819508d098236454afa79a4d3b7`.
- Tests: `ShizukuCommandPolicyTest` covers fixed read-back argv, explicit owner confirmation for the one overlay mutation, injection/unknown-command rejection and bounded timeout. `PowerModePolicyTest` covers ADB endpoints, generated owner commands and capability-level boundaries. Exact-head CI has not yet completed; no pass is claimed.
- UserService accepts command IDs only. It does not parse or execute arbitrary shell strings. It captures at most 8 KiB output, bounds each command to 4 seconds, and exposes request-ID cancellation. STOP CONTROL calls `cancelAll()`; Activity destruction also closes/unbinds the client.
- Current app UI exposes Shizuku status, explicit owner authorization, read-back for secure settings/overlay/Accessibility and a separate STOP command control.
- The only mutating Shizuku allowlist entry is `set_overlay`, and it requires an explicit ownerConfirmed flag plus the exact `allow` or `default` mode. Current UI does not expose that mutation; overlay grant remains an owner-run Termux command.
- Shizuku v13.1.5 is the documented API dependency. Official project guidance states non-root Shizuku runs with ADB shell UID 2000, not root, and must be restarted after reboot. UserService is used rather than deprecated `newProcess`.
- Auto Blocker cannot be reliably identified through a documented public Android API in this implementation. Therefore the wizard gives Samsung's official manual Settings route and does not falsely claim automatic detection or disablement. Wireless ADB is preferred; Samsung documents USB command blocking and unauthorized-source install blocking.
- Important: Shizuku status/binder and command behavior still need exact-head CI and physical Galaxy A06 validation. P1 remains incomplete. Do not advance to P2 until P1 acceptance is satisfied.
- Current source head: `ee9f5be8471cb819508d098236454afa79a4d3b7`.


### Debug/release packaging parser correction — 2026-11-03
- Commit: `d0ee04a10b4cd42a98f75fbfaef9397bd88e5230`.
- The exact-head Debug run `41553588766` for `9ba2cc7f5e3c4a5a5385f7f569d04828c4041399` completed Gradle tests and APK builds successfully, then failed only at certificate fingerprint extraction. The log did not include stderr in the saved certificate output.
- Workflow now captures both stdout and stderr from `apksigner`, then extracts the first 64-hex digest from the SHA-256 line. The expected certificate SHA-256 remains unchanged; mismatch still blocks publication.
- This is a CI workflow fix only. It is not yet verified until the latest exact-head Debug run completes. Do not claim the packaging issue fixed before that run passes.


### P1 manifest permission correction — 2026-11-03
- Commit: `d2d8a355c34f67ef39b0ebe2683e408d207259bd`.
- Declared `android.permission.WRITE_SECURE_SETTINGS` in the manifest. This declaration is required before the owner-run `pm grant` command can grant the permission to this package; the grant remains owner-initiated and must be read back.
- The app does not yet auto-enable or auto-reconnect its Accessibility service. That remains a separate explicit-owner-opt-in implementation step gated on the actual runtime grant check. Never silently re-enable a service the owner has disabled.
- This manifest correction is awaiting exact-head CI.


### P1 AIDL compile failure and correction — 2026-11-03
- Exact failing source head: `c38630a5bbd2f2ba4cadb9d91fcca6a7fb0fab57`, Debug run `41557357263`.
- Exact build failure: `Task :app:compileDebugAidl FAILED`; `IVyomShellService.aidl:6.9-17: You must either assign id's to all methods or to none of them.`
- Fix commit: `03c9e570fc9571ad434fbc0aa8c67b18adfcd585`. Assigned stable transaction IDs 1 and 2 to execute/cancel while preserving Shizuku's reserved destroy transaction ID 16777114.
- This is a build/compiler failure, not a unit-test failure. The fix is awaiting exact-head CI/Debug verification.
- Phone testing is deferred until the complete implementation sequence and final APK, per the user's requested order. Do not require Galaxy A06 physical testing before moving through the remaining code phases; P1 must first pass its automated policy/build/CI acceptance. Record actual device permission and Wireless ADB behavior as pending for the final phone-test guide.


## Master orders archive and current checkpoint — 2026-11-03

- Full Part 2, Part 3 and Part 5 user orders are preserved in `docs/MASTER_ORDERS.md` (commit `57e16ead4cab761cbe99899c37fb2aa8d958c118`). The original Part 2/3 text is transcribed from the user's 19-page source PDF; Part 5 records the user's P1–P8 instructions supplied in the project messages.
- Added matrix IDs:
  - U13 — Activity Timeline: **UNVERIFIED**. Acceptance test not implemented/verified yet: per-step chat timeline, status/icon, expandable detail/log, same source as audit/task detail, newest at bottom.
  - U14 — Web Search UI: **UNVERIFIED**. Acceptance test not implemented/verified yet: searched-sites row, favicon chips, expandable title/domain source list with page opening, inline citation chips.
  - Composer/generation controls: round square-icon STOP replaces Send during generation/action; compact circular down-arrow replaces wide JUMP TO LATEST; composer retains +, mic and Send. **UNVERIFIED**.
  - Theme constraint: use Vyom dark-glass/cyan tokens; do not import competitor color palettes. U13/U14 screenshot tests required.
- Real branch checkpoint before this HANDOFF update: `64df8d306ea80fe68b23f952340427457480e110`.
- Exact-head CI for that checkpoint:
  - Debug `41559566186`: in progress at last check.
  - Full CI `41559566221`: pending at last check.
- Previous exact-head failure on `1abb30771d016c58430d9df6e38f3bc559e1943f`:
  - Debug `41557885850` — FAILED.
  - Full CI `41557885817` — FAILED.
  - Exact compiler error: `MainActivity.kt:2006:21 Unresolved reference 'textIsSelectable'.`
  - Corrected in commit `64df8d306ea80fe68b23f952340427457480e110` to use Android TextView's Kotlin property `isTextSelectable = true`. This fix is not accepted until exact-head CI passes.
- Earlier P1 AIDL issue was corrected in `03c9e570fc9571ad434fbc0aa8c67b18adfcd585`; subsequent build got past `:app:compileDebugAidl`, confirming that specific AIDL compile error is resolved on that build attempt.
- **Exact next step:** obtain the completed Debug and Full CI results for `64df8d306ea80fe68b23f952340427457480e110`; if either fails, use its real job log, fix the exact failure, commit and rerun both workflows on the new exact head. Then continue P1 Power Mode acceptance (secure-settings grant read-back, explicit owner-opt-in Accessibility reconnect after verified grant, audit logging and policy tests). Do not start P2 before P1 acceptance; preserve the user's fixed order. One final APK only after all items and exact-final-head CI/signing/artifact checks.


### Shizuku registry compile correction — 2026-11-03
- Exact-head Debug run `41562824222` and Full CI run `41562824313` both failed at `:app:compileDebugKotlin` with the compiler diagnostic: `ShizukuRequestRegistry.kt:34:16 'when' expression must be exhaustive. Add the 'CANCELLED_BEFORE_START' branch or an 'else' branch.`
- Root cause: `cancel()` handled completed, cancel-requested, starting, running and null states, but omitted the pre-start STOP tombstone state introduced by the bounded registry.
- Correction: repeated STOP on `CANCELLED_BEFORE_START` is now idempotent and returns true. Regression test now verifies repeated STOP, tombstone consumption into COMPLETED, and duplicate rejection after consumption.
- This fixes the exact Kotlin compile error; no tests/build are claimed until both new exact-head workflows pass.
- No feature matrix ID changed. P1 remains PARTIAL; next item remains P1 live permission/app-op read-back and owner-opt-in Accessibility reconnect, plus UserService lifecycle/audit verification.
- Correction source commits: see exact-head history after this addendum is committed.


### Shizuku registry capacity regression — 2026-11-03
- Verified branch before correction: `cb9d7501baad9be1bce7330a49a294d1ec5c816b` on `feature/memory-intelligence`.
- Exact-head Debug run `41563233822` failed in `:app:testDebugUnitTest`: 196 tests completed, 2 failed. Failing methods: `ShizukuRequestRegistryTest.pendingStopTombstonesAndActiveRequestsCannotExceedCapacity` (line 47) and `ShizukuRequestRegistryTest.completedHistoryIsBoundedWithoutClearingActiveRequests` (line 59). Kotlin compilation succeeded.
- Root cause from source: insertion called `trim()` only when map size was already greater than the cap. At exactly maxEntries, completed replay entries were not evicted to make room for the next request/tombstone, so valid insertions were rejected; completion later overfilled then trimmed the map.
- Correction commit: `7c723cde50d26a49ea14a09ce15a64853420b778`. Added `trimForInsert()` which evicts the oldest COMPLETED entry only when a new slot is needed; STARTING/RUNNING/CANCEL_REQUESTED/CANCELLED_BEFORE_START entries remain non-evictable. Existing completed-history trimming still enforces the hard upper bound after completion.
- This is a behavior correction against two existing regression tests; no test was weakened or removed. GREEN is not yet claimed: exact-head Full CI run `41563816253` is queued for this source SHA. Re-check its head and result, and require the Debug workflow on the same exact head before accepting.
- No feature ID changed. P1 remains PARTIAL. After exact-head CI passes, continue P1 UserService lifecycle/STOP/audit and live permission/app-op read-back plus owner-opt-in Accessibility reconnect; do not start P2.
- No APK produced or delivered.


### P1 owner-led reconnect and Shizuku audit hardening — 2026-11-03
- RED test commit: `13d428e0e197c31685261768317f68e977b2ad2c`. Added `AccessibilityReconnectPolicyTest` (grant gate, preserve-other-services merge, idempotence, malformed component rejection) and `ShizukuAuditTest` (outcome/exit recording, redaction, nonzero exit). Exact-head Debug run `41575898510` and Full CI run `41575898508` were started on that test-only head; inspect their completed logs for the expected missing-policy RED result before accepting the implementation commit.
- Implementation correction: owner-only reconnect button checks the live app `WRITE_SECURE_SETTINGS` grant, preserves other enabled Accessibility services, writes only after explicit tap, and verifies both Settings.Secure values by read-back. There is no automatic reconnect on app launch/resume; owner-disabled state remains disabled until another explicit tap.
- Capability status now includes live secure-settings grant and Accessibility global-enabled + component read-back. Control readiness uses the same two-part Accessibility check.
- UserService audit now distinguishes success, nonzero exit, timeout, STOP cancellation, startup cancellation, interruption and failure. Audit records contain only validated request ID, allowlisted command ID, outcome and optional exit code; no command output, arguments or secret values.
- Shizuku binder death/disconnect clears stale remote/bound/request state so a dead binder is not treated as a live UserService connection.
- P1 remains PARTIAL: these source/policy tests do not prove a physical Shizuku binder, Galaxy A06 Settings.Secure write/read-back, OEM behavior, or runtime STOP process-kill timing. Continue the mandatory P1 lifecycle/runtime matrix before P2. No APK.


### P1 Shizuku bind-time authorization revalidation — RED — 2026-11-03
- Starting exact source head: `26c95453e44539d65be9bae9854ad7545258feb6`; Debug run `41577340647` and Full CI run `41577340639` both completed SUCCESS on this exact SHA. Debug includes unit tests, APK build, checksum packaging and artifact upload.
- Security edge: Shizuku binder/UID/permission are checked before authorization, but `bindUserService()` does not independently re-read binder liveness, owner permission and shell UID at the binding boundary. These values can change between the earlier check and bind.
- RED test adds a pure contract: bind only when binder is live, explicit permission is granted, and UID is exactly 2000. The policy method is intentionally not implemented in this test-first commit.
- P1 remains PARTIAL. No matrix/acceptance ID is marked complete. Next: implement bind-time guard, then continue UserService lifecycle/STOP and permission read-back acceptance. No APK delivered.


### P1 bind-time Shizuku authorization — GREEN implementation; exact-head CI pending — 2026-11-03
- Exact RED evidence at `b9c085714329419fe1ea76bee5ab1d667a3e15d2`: Debug run `41578130250` and Full CI `41578130284` both failed in `:app:compileDebugUnitTestKotlin` on six unresolved references to `PowerModePolicy.mayBindShizukuUserService` in `PowerModePolicyTest.kt:142-147`. This is the expected missing-policy RED result.
- Implementation: pure policy allows binding only for live binder + granted Shizuku permission + UID 2000; `bindUserService()` now re-reads all three immediately before checking/reusing/binding the UserService. If any check fails, stale local remote/bound state is cleared and bind is refused.
- This closes the time-of-check/time-of-use gap at the binding boundary. It does not by itself prove Android/Shizuku binder lifecycle or physical permission revocation behavior.
- P1 remains PARTIAL; no feature/acceptance IDs changed. Next: exact-head Debug and Full CI; then test UserService disconnect/revocation lifecycle and continue the mandatory P1 live capability/read-back matrix. No APK delivered.


### P1 bound UserService authorization revalidation — RED evidence and implementation — 2026-11-03
- Test-only head: `c15a62b48bb215f16e57f9b9731233be88e95e73`. Debug `41579936271` and Full CI `41579936266` both FAILED as expected in `:app:compileDebugUnitTestKotlin`: six unresolved references to `PowerModePolicy.mayUseBoundShizukuUserService` at `PowerModePolicyTest.kt:151-156`.
- Implemented pure live-use policy: require live Shizuku binder, currently granted permission, UID 2000, live remote binder and locally bound state.
- Before a read-back request and again on the worker immediately before dispatch, re-read all authorization and binder inputs. Any failed check invalidates local remote/bound/request state and refuses dispatch. STOP also refuses stale remote access.
- This is app-side revalidation; it cannot prove the OEM/Shizuku runtime revocation callback timing. P1 remains PARTIAL and no acceptance IDs changed. Continue the actual UserService lifecycle/STOP tests and capability/app-op read-back matrix. No APK delivered.


### P1 capability-level live UserService read-back — RED — 2026-11-03
- Current source inspection found a capability-reporting gap: `capabilityLevel()` reported LEVEL_2 from Shizuku binder + permission + UID 2000 alone, even if the app's UserService was never bound or its remote binder had died.
- Added a regression contract requiring a locally bound and live remote UserService before LEVEL_2 is reported; Shizuku authorization without that service must fall back to LEVEL_1 when Accessibility is effective.
- Test-only commit is intentionally expected to fail compilation until the policy signature and client read-back are implemented. No capability ID is marked complete; P1 remains PARTIAL.
- Next: inspect exact RED CI logs, implement live service-backed capability reporting, then continue remaining P1 lifecycle/STOP and app-op read-back acceptance. No APK.


### P1 capability-level live UserService read-back — implementation — 2026-11-03
- RED CI evidence on test-only head `8bde96f00a6a553770b2b75c751d6b53fc129bba`: Full CI run `41582291401` failed at `:app:compileDebugUnitTestKotlin` exactly because the new test's `userServiceBound` and `remoteBinderAlive` policy inputs did not yet exist. The real compiler log showed eight unresolved named-parameter diagnostics; production Kotlin compilation succeeded.
- Implemented truthful LEVEL_2 reporting: Shizuku binder + permission + UID 2000 are necessary but not sufficient; the UserService must also be locally bound and its remote binder alive. Capability read-back now invalidates stale remote/bound/request state when any of those conditions is lost.
- Updated existing LEVEL_2 policy tests to supply the complete live-service state. No physical-device behavior is claimed. P1 remains PARTIAL; no acceptance IDs changed.
- Next: exact-head Debug and Full CI; then continue the P1 UserService disconnect/revocation/STOP and full capability/app-op read-back matrix. No APK.


### P1 Shizuku UserService teardown admission race — RED — 2026-11-03
- Source inspection found `VyomShizukuUserService.destroy()` only iterated currently running process IDs and had no atomic service-closed gate shared with `execute()`. A request could pass `begin()` while teardown was in progress; a STARTING request was also not guaranteed to be included in the process-kill snapshot.
- Added regression tests requiring atomic shutdown to cancel STARTING/RUNNING requests, preserve completed history, be idempotent, and reject every new/replayed request after shutdown.
- Test-only commit is expected to fail until `ShizukuRequestRegistry.shutdown()` and a terminal `SERVICE_STOPPED` begin result are implemented. No acceptance IDs changed; P1 remains PARTIAL.
- Next: inspect exact-head RED CI logs, implement registry/service teardown integration, then run exact-head Debug and Full CI. No APK.


### P1 Shizuku UserService teardown admission race — RED evidence and implementation — 2026-11-03
- Test-only head `bc62e7597f6b3b08744b9422a88d103954950e29`: Debug run `41583947297` and Full CI run `41583947422` both FAILED as expected in `:app:compileDebugUnitTestKotlin`. Real logs showed unresolved `shutdown()` and `SERVICE_STOPPED` references in the new registry regression tests.
- Added an atomic closed gate to `ShizukuRequestRegistry`: shutdown closes admission and marks every STARTING/RUNNING request CANCEL_REQUESTED in the same synchronized operation; returns their IDs for process termination. Repeated shutdown is idempotent. New and replayed execute attempts after shutdown return SERVICE_STOPPED.
- Integrated shutdown into `VyomShizukuUserService.destroy()` before process termination. Startup racing teardown cannot transition to RUNNING after the registry has closed.
- Unit tests cover STARTING and RUNNING cancellation, completed-history preservation, idempotent teardown and rejection after shutdown. This proves registry semantics, not OEM Shizuku process-death timing. P1 remains PARTIAL; no acceptance IDs changed.
- Next: exact-head Debug and Full CI; then permission-revocation invalidation and owner STOP/process termination lifecycle. No APK.


### P1 Shizuku permission-revocation invalidation — implementation; exact-head CI pending — 2026-11-03
- Source audit found that the Shizuku permission-result denial callback only emitted a status message, while refreshStatus() returned from unauthorized/dead/unsupported identity paths without clearing a previously bound UserService and active request IDs.
- Test-first commit: 2c53a89a6ce7c8d7030a8ceddf08524fe29673bd; added PowerModePolicyTest.authorizationLossInvalidatesEveryBoundServiceState for binder death, permission revocation, UID change, remote-binder death, unbound state and the fully valid state. Its Debug run 41585179156 and Full CI run 41585179120 were still IN_PROGRESS at the last poll; the expected RED compiler result has NOT been confirmed from logs.
- Implementation commit: 87ea80ae796ca11c3f5a5b1d7633bb50169bbf51. Added pure mustInvalidateShizukuUserService; permission denial/revocation, missing Shizuku, dead binder, unsupported API, non-shell UID and failed live-use checks now clear the remote reference, bound flag and active request tracking. Binder-death and service-disconnect callbacks use the same invalidation helper.
- Exact-head implementation CI: Debug run 41585343710 was IN_PROGRESS and Full CI run 41585343732 was PENDING at the last poll. Do not claim GREEN until both finish on this exact SHA and logs are inspected if either fails.
- This is app-side stale-state invalidation only. It does not prove Shizuku's OEM permission-revocation callback timing or that a command already executing inside UserService is terminated by permission revocation. Physical Galaxy A06 acceptance remains pending.
- No acceptance/matrix ID rows changed; P1 remains PARTIAL. No APK delivered.
- Next: verify exact-head CI and inspect failure logs; then harden in-flight cancellation on authorization loss and complete owner STOP/process-termination lifecycle tests, followed by the live Level 0/1/2 + secure-settings/app-op read-back matrix.


### P1 authorization-loss in-flight cancellation — implementation; exact-head CI pending — 2026-11-03
- Previous authorization-invalidation implementation head 87ea80ae796ca11c3f5a5b1d7633bb50169bbf51 is now verified: Debug 41585343710 SUCCESS; Full CI 41585343732 CANCELLED by a newer push. The documentation/source checkpoint 89513bed97de477e118a2bf1b247c9d72a3b087a has both exact-head workflows GREEN: Debug 41585666596 SUCCESS and Full CI 41585666684 SUCCESS.
- Source gap: permission/binder/UID invalidation cleared the client's active request IDs and remote handle but did not attempt to cancel already-dispatched UserService processes. Those shell processes could continue until their own timeout.
- Test-first commit 0b63c75e9187002f1f3ce3b0d40650a045db9ca9 adds ShizukuRequestCancellationTest for live/dead remote binder, invalid request IDs, and independent failures while iterating cancellation requests. Its Debug 41586511675 and Full CI 41586511800 were IN_PROGRESS at the last poll; no RED outcome is claimed yet.
- Implementation commit 3f62456c9c2c77e88c8778a1da2522b1cc518e31 adds a pure bounded-ID cancellation helper and invokes it before dropping a still-live remote service handle. It best-effort sends cancel for each valid active request, tolerates per-request binder failures, skips dead remote binders, and then clears local state. This is best-effort: a dead binder or rejected transaction cannot prove the remote process stopped.
- Current implementation Debug 41586567783 is QUEUED and Full CI 41586567829 is PENDING; both must complete on the final documentation head below before claiming GREEN.
- No feature/acceptance ID rows changed. P1 remains PARTIAL; physical Galaxy A06 binder revocation/STOP process-death behavior remains pending. No APK delivered.
- Next: verify final-head CI and inspect any failure logs; then add/verify explicit STOP + UserService process termination lifecycle evidence, then complete the live Level 0/1/2 and secure-settings/app-op read-back matrix.


### P1 Shizuku child-process termination lifecycle — RED evidence and implementation — 2026-11-03
- Test-only commit `3e0c912b25f1bd2654b786849f15e1a4c3343a83` added deterministic process-termination tests for graceful exit, force-kill fallback, already-exited children, per-child failure isolation and the STARTING-vs-teardown race.
- Exact-head Debug run `41588019672` and Full CI run `41588019702` both FAILED at `:app:compileDebugUnitTestKotlin`. Real logs show unresolved `ShizukuProcessTerminator` references in `ShizukuProcessTerminationTest.kt`; this is the expected test-first RED result, not an unrelated production compile failure.
- Implemented bounded graceful-then-force termination and a per-request bulk termination helper. UserService STOP uses the same helper, waits up to 2 seconds for STARTING/RUNNING/CANCEL_REQUESTED executions to complete their late-start cleanup, then makes a final pass over visible children before `System.exit(0)`. A process created after the shutdown snapshot is force-terminated when `markRunning()` refuses the closed registry. Normal timeout and cleanup paths also use bounded termination.
- These are deterministic JVM lifecycle tests and best-effort process APIs; they do not establish OEM Shizuku's external process-death timing. P1 remains PARTIAL; no acceptance IDs changed and no APK delivered.
- Follow-up correction: teardown's initial process snapshot could miss a process whose `ProcessBuilder.start()` had not returned yet. Added registry quiescence waiting and a final running-child sweep; late startup uses the same verified terminator. This bounded wait is not an unbounded guarantee against a permanently stuck OS process creation call.
- Next: run Debug and Full CI on this implementation head; inspect real logs for any failures and fix them before moving to live Level 0/1/2 capability and secure-settings/app-op read-back.


### P1 Shizuku process-termination CI failure correction — 2026-11-03
- Exact-head `bb8ba3540ae66ee4724022ab972c9f6972c8233e`: Debug `41589916070` and Full CI `41589916022` FAILED at `:app:compileDebugKotlin`. Real logs show unresolved `ShizukuProcessTerminator` and missing `terminateProcess` in `VyomShizukuUserService.kt`; no APK artifact was produced.
- Root cause: previous tree update did not include the helper source file or service adapter. Corrected by adding the production helper and restoring its regression test source in the actual tree.
- No ID rows changed. P1 remains PARTIAL. Next: exact-head Debug + Full CI; do not proceed to the capability matrix until CI resolves.


## M1 update-first no-dead-code inventory — 2026-11-03

Audited source/workflows at branch head `1df8bb08a39f874d3ee5319137a8f507f1581ee5` (the two docs commits immediately preceding this inventory are included in the new head). This is an inventory only; none of these IDs is marked FIXED.

| ID / path | Existing implementation found | Reachability / missing proof | Status |
|---|---|---|---|
| S6/S7 signing | `vyom-android-debug.yml` validates keystore secrets/alias and expected cert fingerprint when secrets exist; Gradle release signing config; `ANDROID_SIGNING.md` records expected cert | `vyom-android-release.yml` has a separate weaker signing path and does not verify expected cert; no observed proof secrets are configured/current; prior artifact cert was `BF:41…6E:95`, not expected `7A:75…9F:0A`; installed app cert has not been read from owner's phone in this session | BLOCKED / INCONSISTENT |
| Update path B: external agent commit -> CI -> Check for updates | Settings has reachable CHECK FOR UPDATES; it reads GitHub latest Release manifest, downloads APK, checks SHA-256, package/version and compares APK signer with installed signer | Push/CI branch builds do not publish a release manifest/asset; updater only queries `releases/latest`; no end-to-end CI artifact discovery | PARTIAL |
| Update path A: prompt -> minimal patch/diff -> PR -> CI -> signed APK -> verify/install | Profile settings exposes PROPOSE SELF-CHANGE; `SelfModificationClient` reads GitHub source and opens a PR; proposal branch is restricted | Model is instructed to return an entire `file_content` and commits whole-file replacement, not a minimal patch; no diff review UI; no CI wait, artifact retrieval, SHA/certificate verification/install handoff; target branch constant needs validation against intended dev branch | PARTIAL / WHOLE-FILE RISK |
| Update path C: chat “Vyom, ye badlo” | Chat and self-change code exist separately | No command router path found linking that phrase to guarded proposal UI/workflow | NOT WIRED |
| Hot-swappable UI bundle | Bundled assets directory exists, currently device command JSON | No HTML/CSS/JS bundle loader, app-private versioned bundle store, JS bridge, hash validation or automatic rollback found in tree | NOT IMPLEMENTED |
| Diagnostics > RUN ALL CHECKS | No dedicated Diagnostics implementation/test path found in source tree; settings has update and self-change controls | No RUN ALL CHECKS UI/action or update diagnostics row | NOT IMPLEMENTED |

Source evidence inspected: `docs/ANDROID_SIGNING.md`; `.github/workflows/vyom-android-debug.yml`; `.github/workflows/vyom-android-release.yml`; `app/build.gradle.kts`; `SelfModificationClient.kt`; `SelfModificationPolicy.kt`; `SelfModificationPolicyTest.kt`; `MainActivity.kt` update/profile wiring. Existing updater's certificate comparison is present at `sameSigningCertificate(archive)`, but this does not prove it matches the owner's currently installed APK until executed on that phone.

Current branch CI after the priority-order documentation commits: Debug `41596703009` and Full CI `41596702934`, both target `1df8bb08a39f874d3ee5319137a8f507f1581ee5`; at inventory time Debug was IN_PROGRESS and Full CI PENDING. No pass is claimed. No ID rows changed. No APK delivered.

Next implementation order: unify and harden release signing first; prove configured secret identity and certificate; then wire all update paths and hot-swappable bundle; Diagnostics rows must accompany each feature. Keep update verifier/signing/policy/kill-switch review-only for self-edit proposals.


## M1 update-first implementation checkpoint — 2026-11-03

Source checkpoint before this documentation commit: `ee9fe53b01a42fa8b1157807cb77919ffef15565`. All work remains on `feature/memory-intelligence`; Android `main`, Render, Hermes backend and production Supabase were not modified.

### Implemented and wired (not yet accepted as FIXED)
- **S6/S7 signing workflow:** release workflow now validates the persistent keystore, alias and key password without logging secrets; both signed-build workflows run `apksigner verify` and reject anything except the expected certificate `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`. Release manifest includes SHA-256, certificate SHA-256 and full source commit. Debug CI logs prove keystore/alias/password validation passed in runs `41597804231`, `41598264694` and later runs at the time they reached the signing steps. No artifact fingerprint pass is claimed yet.
- **Path B:** pushes to `feature/memory-intelligence` and `vyom-proposal-*` are configured to publish signed prerelease APK + sidecar checksum + cert report + `vyom-update.json` after tests/build/signing succeed. In-app update discovery scans releases including prereleases and requires valid SHA-256, certificate fingerprint and full commit SHA. Candidate APK is checked against manifest, package/version and the installed app signer before Android installer opens.
- **Path A:** self-change model now returns bounded exact-anchor patch operations, not a whole-file response. Patch application fails closed on missing/ambiguous anchors. Settings exposes the workflow; owner sees a minimal patch preview before PR creation. The app waits for combined proposal commit status, then searches only for the signed release whose manifest commit equals that exact proposal SHA and stages/opens install only after checksum + certificate checks.
- **Path C:** chat phrases `Vyom, ye badlo` / `Vyom ye badlo` open the guarded self-change form with the request prefilled.
- **Protected self-change files:** in-app policy and CI proposal gate block policy, kill, audit, control, accessibility, update verifier, signing and keystore paths. Protected update/signing/kill-switch/policy code remains review-only.
- **Hot-swappable UI bundle:** bundled HTML/CSS/JS is seeded into app-private versioned storage; hashes and restrictive content rules are verified. A local-only WebView Update Center is reachable from Profile. Its narrowly scoped Kotlin bridge supports update check, self-change entry and prompt-driven HTML/CSS/JS exact-anchor patches; applying a patch requires native confirmation, creates a versioned bundle and reloads without an APK. Main-frame page-load errors record failure and attempt rollback to the previous bundle.
- **Diagnostics:** Profile > Update System Diagnostics > RUN ALL CHECKS is reachable. It checks installed signer identity, expected certificate identity, GitHub release discovery/manifest fields, self-change protected-path enforcement, app-private bundle integrity and last WebView load status. Other M1 features are not represented as passed by this update-focused diagnostics run yet.

### CI evidence and unresolved gates
- Exact-head runs for source SHA `ee9fe53b01a42fa8b1157807cb77919ffef15565`: Debug `41598918013` IN_PROGRESS; Full CI `41598917977` PENDING at this checkpoint. These exact-head results must be polled again after the documentation commit.
- Actual prior test failure: Debug run `41597804231` at source SHA `da1cffb171d443e4696a7f73192ea689a767b637` failed `UiBundleIntegrityTest.computesStableSha256ForUtf8BundleContent` (233 tests, 1 failed) because the assertion truncated the computed digest to 32 characters while comparing a full 64-character expected digest. The assertion was corrected in `705a0c162529b216547f63c9ffc444de41d0c481`; later exact-head CI is still required to prove it.
- Signing secret/keystore/alias/key-password checks succeeded in CI, but expected APK certificate verification and signed artifact publication have not yet been observed to pass. The previously inspected APK fingerprint `BF:41:32:C9:D3:4A:2F:80:CF:03:77:0C:2D:5A:85:A8:04:AA:A8:6B:D8:DF:9B:62:84:C4:2A:2D:C8:38:6E:95` differs from the required `7A:75…9F:0A`; the owner's currently installed phone APK fingerprint still needs an on-device read-back.
- Shizuku silent install is not yet implemented/verified; current install path opens Android's installer. Full app UI has not yet migrated to WebView—the hot-swappable WebView currently hosts Update Center only. These are explicit PARTIAL gaps, not completed claims.
- No physical-device update install was run. No APK delivered. No feature/acceptance ID rows changed.

### Next
1. Poll exact-head Debug + Full CI; fix any failures from their actual logs.
2. Verify the signed artifact's SHA-256 and actual APK certificate from CI artifact bytes; compare with the expected fingerprint and, later, the owner's installed APK fingerprint.
3. Prove a branch-push prerelease can be discovered by the app and that a proposal build is selected only by exact commit SHA.
4. Complete Shizuku silent install under verified Power Mode and finish full UI bundle migration/rollback runtime proof before calling update-first M1 complete.

## Release-token permission correction and exact-head CI — 2026-11-03

- Release publishing workflows now grant job-scoped `permissions: contents: write` on the publishing jobs and pass only `${{ secrets.GITHUB_TOKEN }}` to `softprops/action-gh-release`. The optional `VYOM_RELEASE_TOKEN` personal-token path has been removed from both CI prerelease publishing and tagged release publishing. Android signing secrets remain repository Actions secrets; no personal GitHub token is stored in workflow YAML.
- If GitHub still returns HTTP 403 `Resource not accessible by integration`, change this exact repository setting: **Settings → Actions → General → Workflow permissions → Read and write permissions → Save**. Do not add a PAT as a workaround.
- Before this permission-only correction, exact source SHA `20ad49186ed2a58f804873be98f5f88181cb5d5c` passed Debug run `41623371371` and Full CI run `41623371384`; the signed prerelease release step also succeeded on that SHA. The signed APK release asset was `vyom-0.3.1-20ad491-signed.apk` only if verified against its own release; no APK delivery or installed-device certificate match is claimed.
- Permission correction commits: Debug workflow `26aeebc761133e49138138af97a6521f6a0071ec`; tagged release workflow `fbce0c067a807b0cc234f1971aa15cc94622717b`. Exact-head CI for the resulting branch head is required before accepting this correction.
- M1 remains IN PROGRESS. Continue in order: validate signed artifact bytes/checksum/certificate; prove updater discovers the exact branch release; finish all three update paths and the hot-swappable bundle runtime/rollback proof; then close remaining M1 feature IDs with reachable UI, end-to-end tests and Diagnostics rows. Physical installed-app certificate comparison remains owner-device evidence.

## Exact-head release permission and artifact verification — 2026-11-03

- Current source SHA: `36e9c5c431a59cbfcc05efb3e66baa1f48d7d551` on `feature/memory-intelligence`.
- Full CI `41625535668`: SUCCESS. Debug/release workflow `41625535813`: SUCCESS; job-scoped `contents: write`, built-in `GITHUB_TOKEN` release publication succeeded. No personal GitHub token is used in workflows. No 403 remains on this exact head.
- Artifact `13226900200`, `vyom-0.3.1-36e9c5c-signed.apk`: SHA-256 `d220b3900e92aefdb3184823171e21d0f2304bcda0c4b6688c2ac4723a8daa79`; sidecar and independently computed hash match. `apksigner` report says v2 signature valid; certificate SHA-256 `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`, matching expected identity. Manifest commit equals exact source SHA and is published in the release.
- Release: https://github.com/riyas433554-dot/vyom-android/releases/tag/vyom-ci-36e9c5c431a59cbfcc05efb3e66baa1f48d7d551
- Do not deliver/install this APK yet: owner-installed app certificate still requires on-device read-back and comparison. No physical-device update test is claimed.
- M1 next order: (1) prove the in-app updater discovers this branch-push release and enforces exact commit/SHA/certificate; (2) complete proposal path A through both exact-SHA CI workflows and verified install handoff; (3) verify chat phrase path C; (4) finish hot-bundle page-load/rollback runtime proof; (5) proceed to remaining M1 IDs. Keep diagnostics rows PARTIAL until runtime proof exists.


### A17 implementation checkpoint — bounded scroll-until-found — 2026-11-03
- Owner clarified A17 as mandatory M1 core scope; all 20 coverage domains and Control Suite acceptance requirements are recorded in the top-level A17 sections of HANDOFF, MASTER_ORDERS, ACCEPTANCE_TESTS and ANDROID_CONTROL_CAPABILITY_MATRIX. A1 and whatsapp_send are acceptance probes only.
- Existing generic UI Agent is the current partial foundation: Accessibility tree, local OCR, optional screenshot/vision, one-action planning, policy validation, bounded action loop, event/revision re-observation, STOP and foreground guards. It is NOT a universal Android-control engine.
- Added a test-first bounded `SCROLL_UNTIL_FOUND` action. Policy rejects blank/blocked/already-visible targets, invalid direction and maxScrolls outside 1..8. Planner schema exposes the action; runtime scrolls one page then re-observes and counts per-target attempts, stopping when the bound is exhausted. Test: `UiAgentPolicyTest.scrollUntilFoundRequiresAnAbsentVisibleTargetAndBoundedDirection`.
- Test-first commit: `aebba60158c7d1bc45b59c597798cd709134beaa`; production implementation commit: `0ed83c1ee6f6a158bc14fdb0693c681213211333`.
- Exact-head workflows on `0ed83c1ee6f6a158bc14fdb0693c681213211333`: Debug `41631445760` IN PROGRESS; Full CI `41631445805` PENDING at last poll. Older test-first runs `41631188888` (Debug) and `41631188907` (Full CI) had not completed; no RED result is claimed.
- No A17 coverage row is marked FIXED. Diagnostics Control Suite, live capability manifest, ShellExecutor/Accessibility adapter selection, Learn Anywhere execution, remaining generic action adapters and owner-device subset remain open. Next: inspect exact-head CI logs; then wire engine/manifest and add safe, evidence-producing Control Suite checks without pretending static capability checks are device task passes.


### A17 live capability manifest and adapter-gate checkpoint — 2026-11-03
- Added `ControlCapabilityManifest`, wired into the text and vision UI planners, Diagnostics output and task preflight. It reports Accessibility enabled/connected, Master App Control, enabled-app count, NVIDIA planner configuration, STOP state and Shizuku Level 2. It deliberately reports that generic ShellExecutor UI input is NOT implemented yet and selects Accessibility as fallback.
- Missing readiness now shows exact guidance instead of silently returning. Accessibility missing opens Android Accessibility Settings; Master App Control missing opens the App Access panel. Planner-key guidance points to the actual Profile > Settings screen.
- Adapter policy is fail-closed: STOP, missing planner configuration or Master App Control blocks all adapters; Shizuku Level 2 alone does not claim generic shell UI support; Accessibility is selected only when enabled and connected.
- Tests: `ControlCapabilityManifestTest.prefersAuthorizedShellUiAdapterWhenItIsActuallyAvailable`, `fallsBackToAccessibilityWhenShellUiAdapterIsUnavailable`, `refusesControlWhenAccessibilityOrAppAccessIsMissing`, `stopControlAlwaysOverridesAvailableAdapters`, `plannerConfigurationIsRequiredBeforeAnyAdapterCanRun`, `ShizukuLevelTwoDoesNotClaimGenericShellUiInputUntilAdapterExists`, `shellExecutorMustNotBypassMasterAppAccessGate`, `manifestNamesExactOneTapFixesForMissingAccessibilityAndAppAccess`.
- Scroll-until-found test: `UiAgentPolicyTest.scrollUntilFoundRequiresAnAbsentVisibleTargetAndBoundedDirection`; implementation bounds target scroll attempts to 1..8 and re-observes after each page.
- Test-first evidence/fixes: test-only SHA `aebba60158c7d1bc45b59c597798cd709134beaa` Debug `41631188888` FAILED with unresolved `SCROLL_UNTIL_FOUND` / `maxScrolls` (expected RED). Production commit `0ed83c1ee6f6a158bc14fdb0693c681213211333`. Debug `41631445760` and `41631573291` then FAILED with JUnit `UiAgentPolicyTestKt > initializationError` because the appended test was outside the class; fixed by `6dfc27d01caf7e7123dc9dc63a7f1f93e878d93e`. Debug `41632805000` FAILED because the new test fixture omitted `powerModeLevel2`; fixed in `8a64f127813a89b636a99ae626bb2c332e499ecc`. Later app-access gate test/implementation commits: `c8c043a294671bd43f5f149df4a40615038c7380`, `0d5cf6b2d8aff0c5272c0479a90fdf678c4d3e86`.
- Latest source SHA before this documentation commit: `0d5cf6b2d8aff0c5272c0479a90fdf678c4d3e86`. Debug `41633460021` queued; Full CI `41633459928` pending. Earlier runs are superseded and do not prove this head green.
- A17 remains PARTIAL. No coverage row is FIXED. Generic ShellExecutor input, remaining action adapters, Learn Anywhere runtime, full capability inventory, 20-task Control Suite and physical-device subset are still open. Do not deliver M1 APK.


### A17 Power Mode UI input adapter checkpoint — 2026-11-03
- Added fixed Shizuku command IDs `ui_tap`, `ui_long_press`, `ui_swipe`, `ui_keyevent`, and `ui_type_text`. No arbitrary shell command is accepted. Coordinates are bounded to display dimensions; swipe/long-press require policy confirmation; keycodes are allowlisted; text rejects control characters, credential/OTP patterns and long numeric sequences.
- Added asynchronous `VyomShizukuShellClient.executeUiAction` with live binder/UID/owner authorization checks, bounded timeouts, request registry and STOP cancellation.
- Wired Power Mode into the generic UI Agent for vision-coordinate gestures when Shizuku Level 2 is live. Before dispatch it checks App Access, foreground package, screen revision and foreground epoch. After dispatch it re-observes; rejected shell actions fall back to Accessibility only if the original screen is still fresh. Node/OCR/tree actions still use Accessibility; this is a partial adapter, not complete ShellExecutor coverage.
- Capability manifest reports ShellExecutor readiness from live Power Mode Level 2 and explicitly distinguishes coordinate gestures from Accessibility tree/OCR fallback.
- Tests added: `ShizukuCommandPolicyTest.genericUiTapIsBoundToTargetDisplayAndSafeCoordinates`, `genericUiSwipeRequiresConfirmationAndValidDisplayBounds`, `genericKeyEventsAreRestrictedToKnownNavigationAndEditingKeys`, `genericTextInputRejectsCredentialPatternsAndEncodesSpaces`. Capability tests cover adapter selection, STOP, planner configuration, App Access gate and exact missing-permission guidance.
- Source commits in this batch: `6072f266cfdb6ace644ab1ec3fe8f5cfa634e859` (tests), `6af479f18e520338c93453540985205ac6c67bb0` (command policy), `0d48665520d8ed8c21f4c677ae8fcf0ae81d9dda` and `09407e1b715e5fe1cfda784995d5d813fecb4681` (validation fixes), `1e08f456035d64aac70e71d2b271160b37da3a6f` (client), `3f6e63cd9e7fd7f2103d4fe9d8bb589192874c29` (UI wiring), `a36b04a07fe471db25693de979ae6272bf6a7854` and `fe25bf949620a246379e2d5878ef32acd1491b45` (manifest), `7329c8f2bdb09180ce86d768d46382dd3bebff64` (stale coordinate guard).
- Exact source SHA `fe25bf949620a246379e2d5878ef32acd1491b45`: Debug `41634415972` and Full CI `41634416270` were IN PROGRESS; exact source SHA `7329c8f2bdb09180ce86d768d46382dd3bebff64`: Debug `41634772851` IN PROGRESS, Full CI `41634772763` PENDING. No green claim yet.
- A17 remains PARTIAL. Generic ShellExecutor for node/OCR/key/text actions, app lifecycle/system APIs, clipboard/share/intents/deep links, Learn Anywhere runtime, 20 varied Control Suite tasks and owner-device subset remain open. No coverage row is complete; M1 APK remains blocked.


## A18 — NO-APK UPDATE CHANNEL (M1; NEXT IMMEDIATELY AFTER SIGNING — 2026-11-03)

**Owner contract:** ordinary product evolution must not require an APK update. An APK is required only when a genuinely new native Kotlin primitive or native permission/manifest change is necessary. Content updates must be safe, reversible and observable.

- **Stable native core:** Kotlin primitives for tap, shell, voice, memory, files, network and permissions. These are not content-updatable.
- **Content layer:** UI bundle, skills, workflows, prompts/personas, routing rules, knowledge notes, model/lane configuration and non-protected policies.
- **Package manifest:** `version`, `min_app_version`, `changes`, `files[{path,sha256}]`, `probes[1..2]`, `rollback_to`. Package signature is verified against a public key pinned in the native core; every file hash is verified before staging. Reject unsupported app versions, malformed/duplicate paths, protected paths, missing probes and signature/hash mismatches.
- **Apply transaction:** download from the same GitHub Release path or generate from an owner prompt; write into a new inactive slot; validate package and files; run only the 1–2 package probes in staging; atomically switch active-slot pointer; retain previous slot. Any failed probe, page/skill load failure or 2 crashes inside 60 seconds restores the previous slot. Never mutate the active slot in place.
- **Probe contract:** Probe 1 proves the changed screen/skill/behavior is present and responds. Probe 2 measures the changed path duration and compares it with a persisted baseline; show faster/same/slower +X ms. Do not run the 20-task Control Suite after every content update—only at milestones or owner request.
- **Result UX:** after every update show version, PASS/FAIL, each probe result, timing delta, and update history; one-tap rollback restores the previous slot and records the action. The result card screenshot is included in the demo CI artifact or exposed in Diagnostics.
- **Self-generated updates:** Vyom can turn an owner prompt into a content-only patch/package, install it into staging, run probes, and apply only after all checks pass. Protected-file involvement is rejected from this channel and requires an owner-reviewed native repository change; never silently edit it.
- **Required demonstrations:** (1) update visibly changes one UI element, both shipped probes pass, timing delta and result card captured; (2) intentionally failing probe causes automatic rollback and prior UI restored; (3) CI artifact contains screenshot/result evidence. Include automated tests for signature, min_app_version, path allowlist/protected paths, hash mismatch, stage isolation, atomic activation, probe failure rollback, load failure rollback and crash-window rollback.

**Order lock:** stable signing and certificate identity → A18 → A17 → remaining M1 items. Play Store install and `whatsapp_send` are acceptance tests only. A17 is full generic phone control, not a hard-coded list of app-specific skills.


### A18 content-package path policy and CI correction — 2026-11-03
- Added typed content manifest validation, deterministic signed payload bytes, EC/RSA detached-signature verification, per-file SHA-256 validation, app-version compatibility checks, 10 MiB / 100-file limits, and content-root allowlisting. Protected native/signing/update-verifier/kill-switch/policy-engine paths are rejected; ordinary non-protected policy files remain allowed.
- Added inactive-slot staging, exact rollback_to check, probe execution before activation, atomic active-pointer replacement, retained previous slot, one-tap rollback, update history, performance delta reporting, and two-crash/60-second rollback logic. APK-bundled content can seed the initial trusted slot.
- Tests added: ContentUpdatePackageTest.rejectsPathsThatCouldReachNativeOrProtectedFiles, ContentUpdatePackageTest.validatesAppVersionFileHashesAndOneOrTwoOrderedProbes, ContentUpdatePackageTest.verifiesDetachedEcdsaSignatureOverCanonicalManifest, ContentUpdatePackageTest.refusesUnknownSigningKeyAndMissingTrustConfiguration, ContentUpdateSlotStoreTest.stagesRunsProbeAndSwitchesOnlyAfterProbePasses, ContentUpdateSlotStoreTest.failedProbeLeavesPreviousSlotActiveAndDeletesStagingSlot, ContentUpdateSlotStoreTest.ownerRollbackRestoresPreviousSlot, ContentUpdateSlotStoreTest.twoCrashesWithinSixtySecondsRollbackActiveContent, ContentUpdateSlotStoreTest.performanceProbeReportsFasterAndSlowerDeltasAgainstStoredBaseline.
- Real CI failure on source SHA e6783cee0cd37b01fa9f4d9336d90951068c415b: Debug run 41636824780 FAILED, 274 tests / 1 failed: ContentUpdatePackageTest.rejectsPathsThatCouldReachNativeOrProtectedFiles. The allowlist incorrectly treated the ordinary filename non-protected.json as protected because it matched the substring "protected". Fixed by matching the exact protected segment only; correction commit 7b886a6b2625f8337ab986d24b1b756563f77270. Exact-head CI for the correction is required.
- A18 remains PARTIAL: the package verifier/slot transaction is not yet wired into release download, prompt-generated package signing, Update Center result card/history/rollback UI, startup crash hook, or the WebView's active UI slot. No A18 acceptance demo is claimed.


## A18 runtime wiring checkpoint — 2026-11-03

- Added `ContentUpdateReleaseLocator`: selects only the newest non-draft prerelease asset named `vyom-content-update-demo.zip` from the exact repository release path. Tests: `ContentUpdateReleaseLocatorTest.selectsNewestSignedContentPackageFromPublicRepositoryReleases`, `ignoresDraftAndReleasesWithoutContentPackage`, `rejectsContentPackageUrlOutsideTheExactRepositoryReleasePath`.
- Added `ContentUpdateManager` and wired it into Profile: it seeds the content slot, downloads the package from the repository release feed, parses the bounded archive, derives the verifier key from the installed APK's signing certificate, verifies the signature and hashes, stages/runs the package's 1–2 probes, switches the slot, syncs UI assets into the app-private WebView host, displays probe/timing result and update history, and exposes one-tap content rollback. Page-load failure attempts content-slot rollback. An uncaught-exception handler records crashes; the slot store rolls back after two crashes within 60 seconds; a stable launch clears the crash window.
- Added safe active-slot file access and stored-baseline detection. Test: `ContentUpdateSlotStoreTest.exposesOnlyFilesInsideTheActiveSlotAndDetectsStoredPerformanceBaseline`.
- Chained content updates now support `rollback_to: previous`, binding rollback to the currently active slot at apply time instead of hard-coding `1.0.0`. Test: `ContentUpdatePackageTest.validatesAppVersionFileHashesAndOneOrTwoOrderedProbes` now verifies the dynamic rollback marker. Demo builder emits this marker.
- CI release step now emits a second rollback result card and copies the actual `ContentUpdateSlotStoreTest` JUnit XML into release assets, alongside the passing demo card/package. This is automated evidence; it is not a substitute for an on-device visual update/rollback test.
- Added A18 slot/apply/history diagnostics rows and Profile controls for checking content updates and rolling back. `RUN ALL CHECKS` is still update-focused; it is NOT the requested 20-task A17 Control Suite.
- Latest implementation source before the inventory/docs commits: `bf46f3d34b095368816c462ede83c8b875650d83`; Debug `41642634678` IN PROGRESS, Full CI `41642634634` PENDING at checkpoint. These runs predate the inventory documentation commit and must not be treated as exact-head evidence for the current branch head.
- A18 remains PARTIAL until exact-head CI is green, release package signature matches the installed app certificate, the app successfully applies the package without changing APK version, the visible UI element changes, the failing probe restores the old UI, and the result/history/rollback card is observed on the owner's phone.

## APK 1 native primitive audit — 2026-11-03

See `docs/APK1_NATIVE_PRIMITIVE_INVENTORY.md`. Confirmed source inventory: Accessibility tree/OCR/screenshot and some gestures; Shizuku fixed safe command executor; voice foreground service; file provider/document flows; app-private memory store; Update Center WebView; signed APK update and A18 content package manager. Missing or partial native primitives include Termux RUN_COMMAND, persistent floating bubble, NotificationListenerService, MediaProjection screen recording, contacts/SMS/calls, full camera/gallery/video picker, inbound share receiver, alarms/WorkManager, embeddings/local vector index, full app-wide WebView JS bridge, localhost server/panel, pinch/multitouch and full generic shell action adapter. These are APK-only work and cannot be supplied by A18 content updates.

A17: all 20 coverage-matrix rows remain PARTIAL; no row has a complete implementation + Diagnostics check + automated test + on-device evidence. A1 and whatsapp_send remain acceptance tests only. R4 full Control Suite remains absent. A14 TRUSTED default and F4 automatic conversation capture still need explicit wiring/tests. No APK 1 delivery is authorized while these gaps remain.

## A14 TRUSTED default + A17 app uninstall primitive — 2026-11-03

- A14 now defaults the UI Agent to `ConfirmationMode.TRUSTED`, selected in the UI and used by the policy default. Owner-requested visible actions and bounded gestures do not receive duplicate Vyom confirmation prompts. Explicit opt-in `ALWAYS_ASK` and `RISKY_ASK` modes remain available. System UI quick-setting controls are goal- and label-allowlisted; TRUSTED executes only after exact current-screen validation. STOP CONTROL remains higher priority.
- Hard blocks were narrowed to payment/UPI/wallet/banking/authenticator/OTP and security/credential surfaces. Removed the extra blanket trading/investment app blocks. Uninstall is no longer a global hard block; protected banking/wallet/authenticator apps and Vyom itself remain protected.
- Tests added/updated: `UiAgentPolicyTest.trustedIsTheDeclaredDefaultMode`, `trustedDefaultExecutesOwnerRequestedVisibleActionsWithoutExtraConfirmation`, `trustedDefaultAllowsFreshGesturesWithoutExtraConfirmation`, `trustedDefaultAllowsAllowlistedQuickSettingsButKeepsHardBlocks`, `AppAccessPolicyTest.authenticatorAndOtpAppsAreBlockedByPackageOrLabel`, `tradingAndInvestmentAppsAreNotOverBlocked`, `DeviceActionSafetyPolicyTest.trustedDefaultAllowsOwnerRequestedConfirmClassActionsWithoutSecondPrompt`.
- A17 app-management progress: added owner-requested `UNINSTALL` intent parsing, protected-app rejection, exact installed-app label/package resolution, `QUERY_ALL_PACKAGES` visibility and Android's own uninstall confirmation intent. Tests: `ActionEngineTest.uninstallIsOwnerRequestedButProtectedAppsRemainBlocked`. Force-stop and clear-data are still missing; the A17 20-row matrix remains partial.
- F4 correction: `MainActivity.sendMessage` and voice prompt dispatch already call `memoryStore.learnFrom`; capture is reachable but `MemoryCapturePolicy` currently extracts explicit remember/keep-in-mind patterns only. This is not passive/semantic automatic capture of all useful facts.
- Source checkpoint before this docs update: `ce5b2821194d40affd30ce2234a7b93d7f985603`; Debug `41645669377` IN PROGRESS, Full CI `41645669237` PENDING. Exact-head green proof is still required after this docs update.


## Vyom Factory Termux registry acceptance — 2026-11-03

### Owner-run live results
- Owner confirmed `jq` is installed at the Termux binary path.
- `bash -n ~/bin/vyom-factory` returned `SYNTAX PASS`.
- First `vyom-factory list` returned no rows (empty registry; no error text).
- Created controlled test task using source ID `factory-termux-test-001`: GitHub Issue #8.
- Subsequent `list` showed issue #8 with open state and P3.
- Priority mutation P3 → P2 succeeded.
- Factory status mutation open → in_progress succeeded.
- Comment operation succeeded.
- Edit operation updated title and description successfully.
- Independent GitHub issue read confirmed metadata survived edit: schema=1, priority=P2, status=in_progress, source=termux, source_id=factory-termux-test-001, idempotency key present, and event history retained created/priority/status/edited events.
- GitHub Issue #8 was then closed as completed to clean up the temporary acceptance task.
- This verifies the Termux CLI ↔ GitHub Issues registry path for create/list/priority/status/comment/edit. It does NOT yet prove the Android app and ChatGPT intake create identical records, nor a worker that executes tasks.

### Current Factory milestone status
- **M0 registry/CLI integration: PASS for the tested Termux path.**
- **M1 durable worker/orchestrator: NOT IMPLEMENTED / NOT ACCEPTED.** No continuous task claimant, lease/heartbeat, worker run state, bounded retry/recovery, cancellation, or safe multi-worker scheduler is proven.
- **M2 execution safety: NOT ACCEPTED.** Need isolated task branches/worktrees, conflict serialization, policy gates, exact-head tests/CI, and audited completion evidence.
- **M3 skills/repository learning: NOT ACCEPTED.** Need repository/skill discovery, source + SKILL.md inspection, licence/security checks, pinned revision/hash, controlled install and validation.
- **M4 schedules/availability: NOT ACCEPTED.** No 24/7 guarantee; need scheduled runner/heartbeat and explicit offline reporting.
- **Factory UI: partial.** A screen/registry path is not proof of live worker events, all intake sources, or complete operator controls.

### Immediate next engineering work
1. Inspect the current Factory Android source, registry adapters, task/event models, and GitHub Actions workflows. Identify the exact active intake paths for Android chat, Factory UI, Termux and ChatGPT/connector-created issues; do not assume they already converge.
2. Implement a durable worker lifecycle on the existing trusted execution infrastructure: claim with compare-and-set lease, unique run ID, heartbeat/expiry, append-only events, bounded retry/backoff, cancellation/STOP and restart recovery. Do not use an always-on claim unless a runner actually remains available.
3. Add safe execution boundaries: per-task branch/worktree, file ownership/conflict detection, approval/policy checks, test commands, exact-HEAD CI and verified completion artifact.
4. Implement skill/repository intake with pinned source revision, manifest/hash, licence/security review, allowlisted install destination and smoke test.
5. Wire Factory dashboard to persisted task/event/run data; add filters, unlimited durable registration with pagination, priority/dependency editing, complaint source attribution, logs, retry/pause/cancel and transparent failure states.
6. Add schedules only after runner lifecycle is reliable; expose next run, last heartbeat and offline state.
7. Add cross-channel acceptance tests and run exact-head Debug + Full CI before any APK release. Do not require uninstall; preserve signing identity and the A18 no-APK update channel gates.

### Test cleanup
- Temporary acceptance issue #8 (`factory-termux-test-001`) closed after all tested mutations and independent metadata read passed.


## Continuation checkpoint — Termux status display correction (2026-11-03)

### Change implemented
- Fixed `scripts/vyom-factory.sh list`: the second column now comes from canonical Factory metadata `status`, not GitHub's coarse open/closed issue state. Priority is now shown as a separate column as well.
- The output columns are now: issue number, Factory status, priority, title, URL.
- Invalid/malformed metadata and ordinary GitHub issues remain excluded.
- Added `scripts/test-vyom-factory-list.sh` with fixtures proving that an issue whose GitHub state is `open` but Factory metadata is `in_progress` renders as `in_progress`; completed/closed tasks render as `completed`; ordinary and malformed issues are excluded.
- Added the fixture test to `vyom-android-ci.yml` after the existing shell syntax check.

### Commits on `feature/memory-intelligence`
- `f0d6606c13d7ef6c2543d877e34d9dd8a67df7da` — CLI canonical status/priority rendering.
- `a5b2787efd3277afe20f490ddef12b0ec51582b0` — CLI output regression test.
- `b439570b28e3a0923662ea22699c6e1f0697fb03` — CI runs the regression test.

### Verification state
- Owner's prior Termux create/list/priority/status/comment/edit round-trip passed for issue #8; issue #8 was closed after the test.
- New regression test is committed and will run in CI; it has NOT yet been independently executed in this session. Do not claim it passes until workflow evidence is checked.
- New exact-head Debug and Full CI results are pending. No APK delivery is authorized.
- Next immediate verification: inspect latest Actions runs for the new branch HEAD; read actual job results/logs and fix any failures. Then ask owner to update the Termux script from this branch and confirm list output shows the Factory status column accurately.

### Next engineering focus after CI
1. Complete code-level intake audit across Factory screen, Vyom chat, Termux, and explicit ChatGPT-created issues. Verify each source's metadata identity and duplicate prevention.
2. Design/implement a real durable worker lifecycle. First prove the available executor can atomically claim a task and persist run/lease/heartbeat/event state; then implement retries, STOP/cancel and crash recovery.
3. Add isolated task execution branches/worktrees and safe conflict serialization before parallel execution.
4. Build repository/skill intake with pinned revisions, licence/security review and validation.
5. Wire persisted run/events into Factory UI. Schedules and always-on claims only after an actual runner/heartbeat exists.


## Owner confirmation — Termux canonical status rendering (2026-11-03)

### Owner-run result after updating the Termux adapter
- Owner downloaded the current branch version of `scripts/vyom-factory.sh` to `~/bin/vyom-factory`.
- `chmod 700 ~/bin/vyom-factory` completed without error.
- `bash -n ~/bin/vyom-factory` completed with no syntax error.
- Owner first typed `~/bin/vyom-factory lis`; the CLI correctly printed usage and `ERROR: Unknown command: lis`. This is a user command typo, not a script failure.
- Owner then ran the correct command `~/bin/vyom-factory list`. Actual output:
  `8  in_progress  P2  [VYOM FACTORY][P2] TEST - Factory integration updated  https://github.com/riyas433554-dot/vyom-android/issues/8`
- This confirms the updated CLI is installed on the owner's Termux and displays Factory metadata status and priority in the intended columns.

### Important reconciliation — test issue #8
- Independent GitHub API read at 2026-11-03 confirmed Issue #8 has GitHub issue state `closed`, while the Factory metadata rendered by the CLI is `in_progress` / P2.
- Therefore the earlier note saying issue #8 was "closed as completed" is only true for GitHub's issue state; the Factory status metadata was NOT transitioned to `completed`. Treat this as a test-data inconsistency, not an active worker task and not a successful completion event.
- Do not silently rewrite this history. Before using issue #8 for further acceptance, reconcile it explicitly (set Factory metadata to `completed` with an audit event, or mark it as a cancelled test record) and verify both GitHub state and Factory metadata. Keep the CLI's display based on Factory metadata, because GitHub open/closed is not the Factory lifecycle.
- This mismatch exposes an acceptance requirement: the app/CLI must visibly distinguish the Factory lifecycle from GitHub issue state and must not imply a worker is running merely because metadata says `in_progress`. A later worker implementation must bind `in_progress` to a real claimed run/lease/heartbeat.

### CI state at this checkpoint
- Exact branch HEAD before this documentation-only checkpoint: `f3868bb85861d584fcc3eca8014959ae160becb6`.
- Debug run `41659785640`: still `in_progress` when checked.
- Full CI run `41659785658`: still `in_progress` when checked.
- Neither run is green yet; do not infer success from elapsed time. Re-check both against their exact source SHA after this docs commit and inspect job logs for any failure.
- No APK delivered and no acceptance milestone declared complete.

### Carry-forward rules (do not drop)
1. Preserve this owner-run Termux output and the issue #8 metadata/GitHub-state discrepancy in future handoffs.
2. After every meaningful implementation batch, update `docs/HANDOFF.md` with source SHA(s), exact tests, workflow run IDs/results, owner/device evidence, unresolved contradictions and next ordered item. Keep controlling order and `MASTER_ORDERS` synchronized whenever the controlling plan changes.
3. Separate code implemented, automated test passed, GitHub workflow passed, owner phone-tested, and production accepted. These are different evidence levels.
4. Never label a Factory task `running` without a real worker run ID plus live lease/heartbeat; never label it `completed` without verified outputs/tests and an append-only completion event.
5. Continue only on `feature/memory-intelligence`; do not touch Android `main`, Render, Hermes backend or production Supabase. No APK until all M1 gates and exact-head CI are satisfied.


## Factory execution architecture audit — 2026-11-03

### Source evidence inspected
- `VyomFactoryGitHubClient.kt` (blob SHA `3345e3c0ea0912cf4cc79250fd994069e563794c`): task registry is GitHub Issues REST API; list reads up to 10 pages × 100; create performs list-by-idempotency-key then POST; update PATCHes issue title/body/open-vs-closed and writes a separate issue comment; history reads up to 50 comments.
- `VyomFactoryStatus.kt` (blob SHA `10ed57fecd6532810a0abafa970ff28e8eb952870`): schema-1 metadata validates source, priority, status, source identity and up to 500 embedded event strings; state machine allows a human/manual `open -> in_progress` transition.
- `VyomFactoryDialog.kt` (blob SHA `29ff1807d8d19d3742e78ad7472608ca536f193a`): Factory UI's START action directly changes metadata to `in_progress`; UI refresh is 15 seconds; displayed task cards are registry data, not worker run records.
- `MainActivity.kt` (blob SHA `45b6fb774c1724df968ac3e676855213d0c5d436`): normal-chat complaint prefixes route to `createTask(source="vyom_chat", sourceId=chatMessage.id)`; message moves through local queued/running/done/error while registering the issue. These are complaint-registration states, not coding-worker states.
- `scripts/vyom-factory.sh` (blob SHA `b394137e064289d50c0f3af6aa1be2c9d2cc8579`): Termux adapter uses `gh` + `jq`, metadata identity `termux:<source-id>`, and same issue body marker. The list fixture test checks status/priority rendering but not full identity validation or concurrency.
- Workflow inventory currently contains only `vyom-android-ci.yml`, `vyom-android-debug.yml`, and `vyom-android-release.yml`; no task-executor/worker workflow was found in the workflow directory.

### Verified architecture conclusions / defects to carry forward
1. **No coding worker exists yet.** No task claim, lease, heartbeat, run ID, runner event stream, recovery loop, per-task branch/worktree, code-writing agent, bounded retry, STOP propagation or task-completion verifier is present in the inspected Factory path. GitHub Issues + Android UI are registry/intake only.
2. **Manual START currently overstates execution.** The Factory UI can set a task to `in_progress` without a worker. Until a real worker exists, this label is not trustworthy. Next code batch must remove/disable this manual transition or introduce a distinct queued/requested state and reserve `in_progress` for a worker claim with evidence.
3. **Idempotency is not atomic.** Android `createTask` performs read-then-create; two concurrent clients can both see no match and POST duplicate issues. Termux create also POSTs without a preflight lookup. Stable keys help retries only after the first issue is visible. Add a concurrency-safe dedupe/claim design and tests before claiming cross-source exactly-once behavior.
4. **Audit history is split.** Embedded metadata events and issue comments are separate writes. A successful PATCH followed by a failed comment can leave a mutation without its comment; a successful POST followed by a failed comment can leave a created task even if the caller reports an error. Model canonical event records and reconcile partial failures/idempotent retries.
5. **Issue #8 remains inconsistent**: GitHub issue state is closed while Factory metadata says in_progress/P2. Do not use it as worker evidence. Explicitly reconcile to a terminal test state with a recorded event, then verify both fields.
6. **Termux regression test scope is narrow**: it verifies valid status/priority display and exclusion of ordinary/malformed bodies, but not invalid source/key, duplicate markers, malformed event arrays, state-vs-issue-state mismatch or concurrent create behavior.
7. **Runner availability constraint**: GitHub Actions workflows are finite event-triggered runners, not an always-on daemon. Any implementation must show queued/runner-unavailable/expired states and heartbeat age; do not promise continuous 24/7 execution.

### CI evidence
- Prior HEAD `f3868bb85861d584fcc3eca8014959ae160becb6`: Debug run `41659785640` completed SUCCESS; Full CI run `41659785658` was CANCELLED after a later branch commit superseded it. This is not a full CI pass.
- HEAD `5b8503371b6b7ed592ce6f28aebe7d40f971180c`: Debug run `41660581607` and Full CI run `41660581524` were both IN_PROGRESS when inspected. Their jobs had started; no result claimed.
- No APK delivered. No Factory worker milestone accepted.

### Next implementation order
1. Check exact-head CI for `5b850337...`; inspect logs on any failure.
2. Correct Issue #8 metadata state to a terminal test state and retain the event/comment evidence.
3. Stop presenting manual `START -> in_progress` as a worker. Add a distinct queue/dispatch state or disable the transition until an executor can claim it.
4. Implement and test atomic task claim/dedupe and durable run lifecycle on a real available executor. Record run ID, lease owner/expiry, heartbeat, attempts, STOP/cancel, recovery and event history. If GitHub Actions is the executor, clearly model its finite runner lifecycle and availability.
5. Add isolated task branch/worktree execution and conflict protection; only then add code-writing agent integration and verified output/CI completion.


## Factory state-integrity correction + issue #8 reconciliation — 2026-11-03

### Issue #8 reconciliation (real GitHub mutation, independently read back)
- Temporary Termux test issue #8 had GitHub state `closed` but Factory metadata `in_progress` / P2.
- Updated only the Factory metadata marker to `status=completed`, appended event `completed:test-cleanup-reconciled`, kept GitHub state `closed`, and added an issue comment explaining that this was test cleanup, NOT a worker execution result.
- Independent read-back confirmed issue #8 remains closed and metadata is now completed/P2. This resolves the test-record mismatch without inventing a worker run.
- The event marker intentionally has no fabricated timestamp; the GitHub issue's updated_at/comment timestamps are the external audit time.

### Manual worker-state claim removed
- Audit found Factory UI's START button directly wrote `in_progress` despite there being no worker. This was misleading and could falsely signal active execution.
- `VyomFactoryStatus.canTransition` now rejects manual `open -> in_progress`; the unit test `VyomFactoryStatusTest.transitionsAreBounded` now asserts that rejection.
- Factory UI no longer shows START for open tasks. It still offers pause/block controls; open tasks remain pending until a real executor claims them.
- Termux CLI transition table no longer permits `open -> in_progress` from a manual status command.
- Shared protocol now states that only a verified worker claim may set `in_progress`. This policy must also be followed by any future ChatGPT/other intake adapter.
- Commits: `843d1c52aa945f4ef1dbda4016bc306ac122dfd4` (state policy), `f5ea703654f530e3589766f456f8609a26608b85` (unit test), `9c949f9a3f517001bea540b753b3471566548571` (Factory UI), `906a158bf682dce53f787f730785e0fc82489e8e` (protocol), `2cb67ed5015fa2534c7c84746cea2369c07e72b5` (Termux CLI).

### Verification and limitations
- Source edits are committed/pushed on `feature/memory-intelligence`; no local Gradle runner was available in this session, so no local Android test pass is claimed.
- Exact source SHA `906a158bf682dce53f787f730785e0fc82489e8e`: Debug run `41661387498` IN_PROGRESS; Full CI run `41661387321` PENDING at checkpoint. These are BEFORE the final Termux CLI commit `2cb67ed...` and are not exact-head proof for the latest branch.
- Earlier source SHA `f3868bb...` Debug run `41659785640` passed, but Full CI `41659785658` was cancelled. It does not validate these changes.
- No durable executor/worker was added in this batch. The change only prevents false worker-running claims; it does not create a worker.
- No APK delivered.

### Next exact work
1. Recheck workflows for final HEAD `2cb67ed...`; inspect actual job steps/logs and fix failures from the real output.
2. Add tests for manual status-transition rejection across Android and Termux, including attempts to set `in_progress` from `open`.
3. Design the first real executor slice with explicit runner availability, claim/lease/heartbeat/STOP/recovery and run IDs. Do not call GitHub Actions a persistent daemon.
4. Fix create idempotency race and partial-write reconciliation before claiming exactly-once cross-source intake.
5. Only after worker run evidence exists, allow `in_progress` from the worker-owned transition path and display its run/heartbeat in Factory.


## Worker-state transition hardening — 2026-11-03 (continued batch)

### Additional defect found and fixed
- A second audit found the earlier guard was incomplete: although `open -> in_progress` was blocked, Factory UI still exposed `RESUME -> in_progress` for paused tasks, and the shared Android transition policy still allowed blocked/paused -> in_progress. Termux had the same manual resume loophole.
- Added unit assertions that manual `paused -> in_progress` and `blocked -> in_progress` are rejected.
- Updated `VyomFactoryStatus.canTransition` so only a worker-owned path can ever enter `in_progress`; manual transitions from blocked/paused now return to `open`.
- Factory UI button for paused tasks now says `REOPEN` and transitions to `open`, not `in_progress`.
- Termux status transition table removes `blocked -> in_progress` and `paused -> in_progress`.
- Protocol clarifies that resuming a paused/blocked task requeues it as open; a worker must claim it again.
- Commits: test `83020487fc1cb69e119855b56d71335530164ad6`; Android policy `74878bd28744cd1291c15ad046e800262cdff770`; UI `740c067076e369df4c2ec3dcb9461337152ef088`; Termux `798639da42885d9cefcfdf6eec9b8b024e6b5cbb`; protocol `db97e7879727eaaad7944013622e7892fe5857dc`.

### CI evidence at checkpoint
- Prior HANDOFF HEAD `8d1950156ded16761dade79fa0c395e312323fbb`: Debug run `41661483857` and Full CI run `41661483942` were both still running. Full CI had completed Bash syntax, Termux list regression, JDK/Gradle setup and secret scan; Gradle unit tests + debug APK were still in progress. No pass/fail claim yet.
- These workflow runs do NOT include the later commits in this section. New exact-head workflows must be checked for the latest SHA.
- No local Gradle execution or device test was performed in this session. No APK delivered.

### Remaining worker architecture blockers
- This patch closes manual status loopholes only; there is still no task runner, durable claim/lease/heartbeat, run ID, cancellation propagation, retry policy, crash recovery, isolated execution branch/worktree or verified completion.
- Do not implement an untrusted GitHub Actions worker that executes arbitrary issue text with repository write privileges. Before coding the executor, define a constrained task contract, allowed operations, scoped credentials, sandbox boundaries, branch isolation, cancellation, audit trail and completion verification.
- Existing GitHub Actions are CI/debug/release workflows; they are finite jobs, not a resident 24/7 worker.

### Next exact sequence
1. Read exact-head CI for latest branch commit, wait for active runs and inspect logs; fix any failures using actual failing step output.
2. Add Termux status-policy tests (including paused/blocked -> in_progress rejection) and verify against a mocked `gh` boundary without live issue mutation.
3. Fix cross-source create idempotency race; document GitHub Issues read-then-create limitation and ensure duplicate retries are reconciled rather than reported as exactly-once.
4. Specify a safe executor threat model and constrained task schema before creating any worker workflow.
5. Implement durable run/lease records only with an approved durable store and scoped worker identity. Do not touch Render or production Supabase; do not assume GitHub Issues metadata alone is a safe atomic lease primitive.


## Termux transition regression + retry dedupe work — 2026-11-03

### Implemented
- Extracted the CLI's allowed-transition logic into pure `valid_transition(from,to)`; CLI mutations now call that same function.
- Existing CI test script `scripts/test-vyom-factory-list.sh` now sources the CLI safely (main only runs when executed directly) and tests all allowed transitions plus forbidden manual `open/paused/blocked -> in_progress` transitions. It also checks completed/cancelled cannot be reopened by manual status mutation.
- Added Termux create retry preflight: before POST, it pages through existing issues and checks the canonical `idempotency_key`. If the same Termux source ID already exists, it returns that issue rather than creating another on a sequential retry.
- Added a mocked `gh` boundary test for the idempotency lookup using the existing JSON fixture. No live issue is mutated by the test.
- Commits: `3c11b3ccc017dee27b201053ce5685bef8479cc7` (pure transition helper); `6e24ec4b7f4591bbf04dabbd7a0eac4b877e8552` (transition tests); `e43959f281b9df0ce9c4aea28b4c00360f2acd09` (initial retry preflight, later found malformed by CI); `a6c6b63c6e7378ace5fbd30b8d60b3a929ef1a46` (reconstructed and repaired CLI); `30d80644b448576f100bc2fb87f09a4a16d80d94` (mocked idempotency test).

### Failure and correction — do not omit
- Full CI run `41662536143` for SHA `e43959f281b9df0ce9c4aea28b4c00360f2acd09` FAILED at `bash -n scripts/vyom-factory.sh`, exact log: line 329 unexpected EOF while looking for matching quote. This was caused by a malformed source rewrite while adding the duplicate preflight. It was not a pre-existing failure.
- The CLI was then reconstructed as a complete clean file at `a6c6b63...`, and the idempotency regression was added at `30d80644...`. Do not claim the syntax is fixed until the current exact-head CI confirms it.
- This is a sequential retry dedupe improvement only, NOT atomic exactly-once. Two concurrent clients can still both preflight before either POST is visible and create duplicate issues. GitHub Issues does not provide a uniqueness constraint on `idempotency_key`. Android `createTask` also uses read-before-create and has the same race. Cross-source concurrency-safe idempotency remains open; no exactly-once claim.
- The Termux preflight scans paginated repository issues and can be slow/rate-limit expensive on a large issue history. A dedicated registry with atomic uniqueness/transactions is the correct eventual fix, but production Supabase is explicitly out of scope.

### Exact-head CI
- SHA `30d80644b448576f100bc2fb87f09a4a16d80d94` triggered Debug run `41662634097` and Full CI run `41662634099`; both were IN_PROGRESS when recorded. Wait for and inspect the exact run results; neither is green yet.
- No local Termux execution of this revised CLI, no Android unit-test execution locally, no device test, and no APK delivery are claimed.

### Next exact sequence
1. Inspect exact-head Debug/Full CI for `30d80644...`; fix failures from actual step logs. Verify both tests execute and pass, not just syntax.
2. Run the updated script on the owner's Termux only after CI is green; test duplicate create with the same source ID and verify it returns the existing issue without a second POST.
3. Implement Android unit tests for task creation idempotency with mocked HTTP responses. Keep read-before-create limitation explicit.
4. Design atomic cross-source identity/claim storage without touching production Supabase or backend; if no approved durable store is available, label dedupe best-effort and surface possible duplicate reconciliation.
5. Do not start a coding worker until safe task contract, scoped credentials, isolated branch/worktree, STOP, lease/heartbeat, bounded retry and output verification are designed and tested.


## CI failure fix: Termux idempotency test — 2026-11-03 (continued)

### Actual failure evidence
- Full CI run `41662745519` on exact SHA `c2f959c1fe94ec3565aa3d091f8c0292e251b219` failed in `Test Termux Factory status and priority rendering`.
- Exact log: list metadata test PASS; transition policy test PASS; then `scripts/test-vyom-factory-list.sh: line 80: unexpected argument 'newline' to conditional binary operator`.
- Root cause: malformed multiline Bash expected-value assertion introduced in the mocked idempotency test. The CLI syntax check itself passed; failure was in the test assertion, not app Kotlin code.
- Fixed assertion using a single ANSI-C quoted expected TSV value: `expected_existing=$'41\\thttps://github.com/example/repo/issues/41'`. Commit `0de75563fb24063cf798fecdf44b5af04b1790f3`.

### Strengthened retry test
- The first mock only called `find_task_by_idempotency_key`; that did not prove the public `create_task` path avoids a POST.
- Replaced it with a mocked `gh` boundary that invokes actual `create_task(title, description, P2, same_source_id)`, asserts it returns existing issue #41, and asserts exactly one GitHub API invocation (the paginated read). Any POST or other API call fails the test.
- This tests sequential retry behavior without live issue mutation. It still cannot prove atomicity under concurrent clients.
- Commit `fd93dfed9b741e3a01c13e1633e25bd484ac2480`.

### CI checkpoint and remaining proof
- For `0de75563...`, Full CI run `41663301896` and Debug run `41663301788` were both IN_PROGRESS when checked. Full CI had passed Bash syntax, list/transition/idempotency preflight tests, JDK/Gradle setup and secret scan; Gradle unit tests/debug APK remained running.
- Those runs predate the stronger final retry-path test at `fd93dfed...`; they are not exact-HEAD verification.
- Need verify new exact-head workflow runs for `fd93dfed...`, inspect their logs and results. Do not call CI green until Full CI and Debug both complete successfully for the same exact SHA.
- Older Debug run `41662745598` for `c2f959c...` remained in progress at checkpoint; it is not a pass and should be observed only for cleanup, not counted as latest-head proof.
- No local Termux execution, no Android device test, no APK delivered.

### Next exact steps
1. Confirm workflow runs were triggered for `fd93dfed...`; inspect job steps/logs. Fix any real failure before proceeding.
2. Add Android `createTask` retry unit tests with mocked HTTP; ensure a sequential same-key retry reuses the existing task and does not issue a second POST. Preserve honest warning that read-before-create is still racy.
3. Audit all status mutation entry points (Android, Termux, ChatGPT docs) to ensure manual callers cannot set or resume `in_progress`.
4. Design atomic cross-source idempotency only after selecting an allowed durable transactional store; production Supabase and backend remain explicitly untouched.


## Android Factory retry semantics + CI test recovery — 2026-11-03 (continued)

### CI failure diagnosis and repair
- Full CI `41662745519` for SHA `c2f959c1fe94ec3565aa3d091f8c0292e251b219` failed in `Test Termux Factory status and priority rendering`.
- Actual log: `Factory list metadata test PASS`, `Factory transition policy test PASS`, then line 80 error `unexpected argument 'newline' to conditional binary operator`.
- Fixed malformed multiline expected TSV string with ANSI-C quoted `$'41\\thttps://github.com/example/repo/issues/41'` in commit `0de75563fb24063cf798fecdf44b5af04b1790f3`.
- Strengthened test to invoke actual `create_task` with an existing source ID and assert only one GitHub API call (the read) occurs; any POST/PATCH fails. Commit `fd93dfed9b741e3a01c13e1633e25bd484ac2480`.
- Added a HANDOFF entry for the failure and correction at `a3999603b8878e7136b23c7bf93bf4395bc961b0`.

### Android duplicate retry behavior corrected
- Source review found Android `createTask` previously found an existing issue by idempotency key and then called `updateTask` using the retry's title/description/priority. That made a retry unexpectedly mutate the original task.
- Changed duplicate retry to return the existing task unchanged; actual edits remain the responsibility of `updateTask`.
- Added injectable GitHub API base URL (default remains `https://api.github.com`) so JVM tests can use a local MockWebServer without live GitHub credentials/network.
- Added test dependency `com.squareup.okhttp3:mockwebserver:4.12.0`.
- Code commits: `54af841f858a9311329b21dae79c08fb4d4f2932` and `7ab5d46792af13325767e33e167c5a500de41909`.

### Branch protection / PR handling
- Direct creation of the new Android test file was rejected by repository rules requiring a pull request. No bypass attempted.
- First draft PR #9 was closed without merge because its generated Git tree had an unintended 145-file diff (22,861 deletions). It was never merged and changed no base branch files.
- Recreated correctly using the exact base commit tree. PR #10 is open and targets `feature/memory-intelligence`; it contains exactly one new test file (56 additions, 0 deletions), commit `a8a4f92b46dcea2bf05003b5e286e97d7c31a1e2`.
- PR #10 test asserts same-key retry returns original task and performs only one GET request, with no second HTTP mutation. Awaiting PR CI. Do not merge if changed-file count differs from 1 or the diff expands unexpectedly.
- Protocol docs updated to remove stale `blocked/paused -> in_progress` transitions; manual recovery from those states goes to `open`, then worker claim is required. Commit `94cbdd76fd98665f5fe7560aa15bc461d76a3353`.

### Verification checkpoint
- SHA `fd93dfed...`: Full CI run `41663387755` pending and Debug run `41663387907` in progress when checked; they were triggered before the HANDOFF and protocol commits, so not exact latest-head proof.
- SHA `0de75563...`: Full CI `41663301896` and Debug `41663301788` were in progress; both predate the stronger retry-path test.
- Current feature branch has later commits `94cbdd76...` and subsequent HANDOFF commit; new exact-head CI runs must be checked.
- PR #10 has its own CI; inspect actual workflow and test output. No local Gradle/Termux execution or physical Android testing was performed.
- No APK delivered. No actual worker implemented. No Render, backend, production Supabase or `main` changes.

### Next exact steps
1. Inspect PR #10 checks and verify its one-file diff and test compilation/result.
2. Check CI for latest `feature/memory-intelligence` HEAD including protocol/HANDOFF; wait for Debug and Full CI on the same SHA.
3. If PR #10 test fails, use the exact Gradle/JUnit log to correct it; do not merge before green.
4. Add Android tests for create-key validation, no-match POST, and pagination boundaries; retain explicit limitation that list-before-create is not atomic under concurrent writers.
5. Decide how cross-source duplicate creation will be made atomic without touching production Supabase/backend; until then surface best-effort dedupe and duplicate reconciliation.


## Mocked Termux retry test: CI-discovered logger defect and clean Android test PR — 2026-11-03

### Second CI failure (actual log)
- Full CI run `41663872294` on SHA `94cbdd76fd98665f5fe7560aa15bc461d76a3353` failed in the Termux regression script after list and transition tests passed.
- Exact log: `Retry performed more than one GitHub API call`, but the call log showed exactly one command. Root cause: the test's mock logger wrote the two-character sequence `\\n` rather than an actual newline, so `wc -l` could not count calls correctly. The CLI had made only the expected single GET; this was a test harness defect.
- A first attempted correction at `0792bf439ab98b85a91ec5914058c106d94de9e8` did not correct the escaped format string; subsequent source inspection caught that. Final correction replaces the mock logger with `echo "$*"`, commit `3f528aec89b3b691b1828cedcb1292e94710ddd7`. Await exact-head CI for proof; no pass claimed yet.

### Android retry semantics code
- Android `createTask` now returns the existing issue unchanged when its idempotency key is already present. It no longer silently overwrites title, priority or description during a retry. Explicit edits still use `updateTask`.
- `apiBaseUrl` is injectable for deterministic local HTTP tests; default remains GitHub API. Added MockWebServer test dependency.
- Commits: `54af841f858a9311329b21dae79c08fb4d4f2932`, `7ab5d46792af13325767e33e167c5a500de41909`.
- Added JVM regression test in PR #11: same source identity retry returns issue #73 unchanged, and the mock server receives only the single GET list request (no POST/PATCH). PR #11 targets current `feature/memory-intelligence` HEAD and currently has exactly 1 file, 54 additions, 0 deletions; head commit `b0613507b0ed31e983cd8336a5be44ff96014f05`.
- PR #9 was closed without merge due an incorrect 145-file tree diff. PR #10 was closed without merge because it was based on a stale feature-branch SHA. PR #11 is the clean replacement; do not merge unless its diff remains exactly one file and its Gradle tests pass.

### Protocol correction
- Protocol previously retained stale blocked/paused -> in_progress bullets despite the Android/Termux implementation rejecting those manual transitions. Removed those stale bullets in `94cbdd76fd98665f5fe7560aa15bc461d76a3353`.

### Current CI checkpoints
- SHA `94cbdd76...`: Full CI `41663872294` failed due test logger defect; Debug `41663872253` still in progress at checkpoint.
- SHA `0792bf43...`: Full CI `41664012507` failed (same logger issue persisted); Debug `41664012497` in progress.
- SHA `3f528aec...`: Full CI `41664062901` in progress; Debug `41664062496` queued when checked. These are before this HANDOFF append and will not be exact latest-head verification.
- No local Gradle run, no Termux run of the latest script, no physical Android test, and no APK delivered.

### Next
1. Check exact-head CI for current branch after this HANDOFF commit. Verify Termux script list, transition and idempotent retry tests pass.
2. Check PR #11 workflow and MockWebServer Gradle test. Fix failures from actual logs; never merge a malformed or broad PR diff.
3. If Android retry test passes, add tests for no-match POST and multiple-page issue lookup. Then document that concurrent create race remains.
4. Implement cross-source atomic dedupe only with an approved durable transactional store; production Supabase/backend remain off-limits.
5. Do not add executor workflows until task schema, untrusted input handling, credential scoping, branch isolation, lease/heartbeat, cancellation and verified completion are addressed.


## Debug CI investigation: real Gradle failure and API URL fix — 2026-11-03

### User asked whether long-running Debug CI was actually working
- Inspected GitHub Actions job steps and logs rather than assuming it was stuck.
- Full CI `41664216657` for SHA `30b7838903448060620d832ac39443b39bce9011` completed SUCCESS.
- Debug workflow `41664216559` for the same SHA showed JDK/Gradle setup complete and `Build and test APKs` running. GitHub log archive for the still-running job was not downloadable at that moment; do not interpret that as no work.
- PR #11 had two completed Debug checks, both FAILED in `testDebugUnitTest`. Exact test output: 300 tests completed, 1 failed; `VyomFactoryGitHubClientTest > duplicateCreateRetryReturnsExistingTaskWithoutMutation FAILED`, `IllegalArgumentException at VyomFactoryGitHubClientTest.kt:37`.

### Root cause found in source (not merely a test-only issue)
- Inspected exact test line and production `VyomFactoryGitHubClient.execute()`.
- The injectable API URL had been written as string concatenation that generated literal `$"+"{base}` / `$"+"{repository}` text, not Kotlin interpolation. This produced an invalid HTTP URL and explains the IllegalArgumentException.
- Corrected to normal Kotlin interpolation:
  - `"${base}/repos/${repository}"`
  - `"${base}/user"`
  - `"${base}/repos/${repository}/${normalized}"`
- Fix committed to feature branch: `b46d0797377900fa58d11ea53bd755332a7a91aa`. This is a production code bug introduced by the testability change; it is not waived as a test-only issue.

### PR handling
- PR #11 closed without merge after the failure and because it was based on the earlier feature head.
- PR #12 created on top of corrected feature HEAD, exact one-file diff (54 additions, 0 deletions), test commit `7d67adf73a630243a11268e8b3cd54147808cf0a`.
- PR #12 repeats the MockWebServer test with local API URL. Wait for its real Gradle result. Do not merge before green.

### Verification still pending
- Need exact-head Full CI and Debug CI for feature branch after `b46d079...` and the subsequent HANDOFF commit.
- Need PR #12 Debug checks. The previous PR #11 failure must remain recorded; do not erase it.
- No APK has been delivered. No local Termux or physical Android test has been performed. No worker implementation claimed.

### Next steps
1. Inspect exact-head CI and PR #12 Gradle logs. Confirm API URL now points to MockWebServer and same-key retry performs GET only.
2. If test passes, add test cases for no matching idempotency key -> one POST and for issue pagination; then run full suite.
3. Keep read-before-create concurrency race explicitly open; do not claim atomic idempotency.
4. Update HANDOFF after each verified batch. Keep main, Render, backend and production Supabase untouched.


## 2026-11-03 — Android Factory create-path regression coverage (PR #12)

- Extended VyomFactoryGitHubClientTest on work/factory-android-idempotency-test-v2 with two MockWebServer cases:
  - A genuinely new task must perform one list GET, exactly one issue-creation POST, then one audit-comment POST; it verifies the issue endpoint and idempotency key in the posted body.
  - An unsupported source identity must throw before any HTTP request (requestCount == 0).
- Existing duplicate-retry test remains: an existing matching identity is returned unchanged and causes no mutation.
- Test commit: 9e0450bac89c8f48f51b5052576d0f4f0c42490a. PR #12 head was advanced by this commit; verify the changed-file list remains only this test file.
- These are regression tests, not proof of concurrency-safe idempotency. The current read-then-create algorithm can still race across simultaneous clients; an atomic durable uniqueness primitive is still required, and backend/production Supabase changes remain out of scope.
- CI status at handoff: current feature branch HEAD 0649f7228b4fbde05beb59631c04fb7c4f32de5b has Full CI run 41665284058 and Debug CI run 41665283308 in progress. Earlier Debug run 41665134204 is also in progress on b46d0797377900fa58d11ea53bd755332a7a91aa; its paired Full CI 41665134203 was cancelled after newer work superseded it. Prior exact SHA 30b7838903448060620d832ac39443b39bce9011 passed both Full CI 41664216657 and Debug CI 41664216559.
- Next: inspect PR #12 checks for the new test commit, read actual Gradle/JUnit logs, fix any failure from evidence, then continue adding bounded tests for pagination/duplicate discovery before merging anything.


## 2026-11-03 — Factory duplicate pagination test added

- Added a MockWebServer regression case where the first GitHub Issues page contains exactly 100 unrelated entries and the matching idempotency key is on page 2. The retry must return the existing issue unchanged and issue no POST/PATCH.
- PR #12 test branch commit: 2a1521c5731454dbc00f352d5fa46013b4ec7023. PR #12 currently remains a one-file test-only change; recheck head and changed-file list after this push.
- Test suite now covers: duplicate retry is read-only, new task creation issues one issue POST plus one audit comment POST, invalid source identity fails before network, and duplicate discovery on page 2.
- CI runs for the previous test head 9e0450bac89c8f48f51b5052576d0f4f0c42490a were 41665773529 (Full CI) and 41665773431 (Debug CI), both in progress at last check. Current feature branch HEAD 0649f7228b4fbde05beb59631c04fb7c4f32de5b runs 41665284058 (Full CI) and 41665283308 (Debug CI), both in progress at last check. The pagination-test commit will trigger new PR-branch checks; do not treat earlier runs as verification of it.
- Read-before-create remains non-atomic under concurrent clients. No worker/executor is implemented by this test work; GitHub Issues remains only the shared registry. Backend/Render/production Supabase remain untouched.


## 2026-11-03 — PR #12 assertion failure diagnosis

- Retrieved the real Debug CI log for run 41665233369, job 126018641402. The failing assertion was line 45 in the duplicate retry test: it expected the description prefix to equal a trimmed string exactly.
- The client correctly returned the original issue body unchanged (the preceding full-body equality assertion passed); the description extraction assertion was too strict about the metadata separator formatting produced by VyomFactoryStatus.withMetadata. This was a test assertion defect, not a duplicate-retry mutation.
- Updated the assertion to verify the original description prefix while retaining the stronger exact body equality check. Test branch commit: ad405dbb448b5444070432dab48511070232502e.
- New PR branch CI must pass on this exact commit before accepting the test. Existing runs on older PR heads do not validate this change.
- Factory background execution remains unimplemented: this code is registry/intake only, not a persistent worker. Do not advertise app-closed task execution until a durable worker/lease/heartbeat/retry/recovery design is implemented and verified.

## BACKGROUND FACTORY EXECUTION — IMPLEMENTATION TRACK (2026-11-03)

Owner-approved fixed order:
1. persistent task store/queue
2. worker claim + lease
3. heartbeat
4. retry/backoff
5. STOP/pause
6. crash recovery
7. bounded parallel workers
8. app-closed execution verification

### Stage 1 source work — partial implementation, not yet accepted
- Added `VyomFactoryTaskStore`: device-local durable SharedPreferences-backed JSON inbox; synchronous commit is required before enqueue reports success; rejects duplicate issue numbers and source IDs; validates priority/identity; queue order is P0→P3 then enqueue time; supports pending/remove/contains.
- Added pure `VyomFactoryTaskQueuePolicy` and JVM tests for stable priority ordering and duplicate identity rejection.
- Files: `app/src/main/java/com/hermes/assistant/VyomFactoryTaskStore.kt`, `VyomFactoryTaskQueuePolicy.kt`, `app/src/test/java/com/hermes/assistant/VyomFactoryTaskQueuePolicyTest.kt`.
- Commits: `127f637f346e3d4b4e11cb5d112f9d827abbed5b`, `29ced6b228849a6ddd298caadd4a4701da914708`, `02f443b31bf507f197b6246cb666169dd2fe37b7`, `b9eca76d724d7ffbe2a5a77ca09bdc19e11b9aca`.
- Important: queue is not yet wired to GitHub task intake and no worker consumes it. This is storage groundwork only, not background execution.

### Architecture constraints
- Android minSdk 26, targetSdk 35. Current manifest has INTERNET and a microphone foreground service only; no WorkManager dependency exists yet.
- Existing Factory dialog uses a dialog-lifetime single-thread executor and 15-second UI polling; dismissing it shuts that executor down. This is not a durable worker.
- GitHub Issues remains the shared task registry. A local Android queue cannot provide a cross-device/Termux/ChatGPT atomic lease. Do not write GitHub status=in_progress until a safe claim protocol exists; do not imply local queue uniqueness prevents cross-client races.
- WorkManager is the likely durable deferred-work substrate, subject to Android force-stop/Doze/battery/network constraints. Never promise guaranteed 24/7 operation on a phone.
- No backend, Render, production Supabase, or main branch changes are permitted.

### Verification / next actions
- Run exact-head Debug and Full CI for the queue implementation; inspect real logs and fix any failure before proceeding.
- Add persistence-level instrumentation coverage (process recreation / app context), wire successful Factory intake to durable enqueue, and make task state truthful.
- Then implement worker claim + lease before heartbeat/retry controls. Do not skip ahead or mark the milestone complete based on queue class existence alone.
- PR #12 stale test branch failure observed on 2026-11-03: both runs `41666365674` and `41666365436` fail at `VyomFactoryGitHubClientTest.duplicateOnSecondIssuePageIsFoundWithoutCreatingAnotherTask`, assertion line 144. Feature branch exact head `265403a42ddc47c715ae99c391d09cc661c447d1` had Debug `41666384836` PASS and Full CI `41666384862` PASS. PR #12 is not merged.
- CI failure for queue policy was traced to a bad test fixture, not production code: duplicate-source candidate accidentally used a different source ID. Corrected fixture in commit `82c84a3114a8e885b560645842889e6f218a7a08`. Earlier exact-head runs `41669816790` Debug and `41669816711` Full CI failed on that assertion; rerun required.
- Fixed queue policy test fixture: duplicate-source assertion now supplies the same source ID explicitly (commit `82c84a3114a8e885b560645842889e6f218a7a08`). The failing CI run was `41669816790` (Debug) and `41669816711` (Full CI), both failed at `VyomFactoryTaskQueuePolicyTest.kt:19` due solely to fixture mismatch.
- Wired successful Factory Dialog registration and successful chat complaint registration to persist the returned canonical GitHub issue number/title/priority/source ID in the device-local queue (commits `a0fda90cff7d019bd8f4123e801b8946e2eff043`, `c041f2d2156ed630c541d6bbf10104154e8a92c9`). This is an inbox, not execution. A process death in the narrow interval after GitHub create succeeds but before local enqueue can still leave a registry task absent locally; later reconciliation/retry is required. No worker has been introduced yet.
- Queue hardening: process-wide lock now serializes read/modify/write across distinct store instances; malformed persisted JSON/entries now fail closed instead of silently appearing empty and being overwritten. Commits `3f6bedcc2a0f3cd19c0752ac94c98e05cc9af4f3` and follow-up recorded in next HEAD.
- Stage 1 remains incomplete pending successful exact-head CI and persistence tests. Queue writes are device-local and still do not establish a remote lease or run work.


## 2026-11-04 — Multilingual natural-language Factory intake requirement

Owner requirement: Factory must accept natural-language task requests beyond English/Hinglish. Initial coverage includes Bengali/Bangla regional and colloquial variants, Romanized Bangla, Urdu (script and Roman Urdu), Japanese, and Simplified/Traditional Chinese, while retaining Hindi, Hinglish and English.

### Current verified limitation
- MainActivity.extractFactoryComplaint() only recognizes six English prefixes: factory complaint:, register complaint:, complaint:, report bug:, factory:, and issue:.
- Chat complaint registration assigns fixed P2 and stores extracted text as description. This path has no language detection, semantic intent extraction, structured plan generation, ambiguity handling, or acceptance-criteria extraction.
- Therefore current Factory intake is NOT multilingual natural-language understanding. Do not describe it as such.

### Required behavior and guardrails
- Accept free-form text without requiring command prefixes; preserve exact original Unicode text as canonical source.
- Treat language/script detection as a hint, not a gate. Support mixed-language/code-switched input and uncertain detection.
- Produce a schema-validated draft: intent, title, objective, constraints/non-goals, grounded affected files/components, priority plus rationale, subtasks/dependencies, and measurable acceptance criteria.
- Preserve filenames, paths, commands, URLs, stack traces, identifiers, symbols and quoted strings verbatim. Never translate technical literals.
- Ask one focused clarification when meaning or consequential constraints are ambiguous. Require confirmation for destructive, broad, costly, security-sensitive or low-confidence tasks.
- Store original request and structured interpretation separately. Interpreter failure must preserve the original text and use a safe clarification fallback.
- A translated-prefix list is not semantic understanding. Interpretation/registration must never imply worker execution.

### Required language test matrix
- Bengali: standard written, colloquial/regional (West Bengal and Bangladesh), Romanized Bangla, Bangla-English code switching.
- Urdu: Urdu-script RTL, Roman Urdu, Urdu-English code switching, punctuation/digit direction.
- Japanese: kana, kanji, polite/casual phrasing, Latin technical identifiers.
- Chinese: Simplified and Traditional, punctuation variants, Latin technical identifiers.
- Regression: Hindi/Devanagari, Hinglish/Roman Hindi, English.
- Tests must assert semantic intent, constraints, original text and technical-literal preservation, subtasks, priority rationale, acceptance criteria, ambiguity behavior and safe failure—not just script detection.

### Implementation gate
- Android-only. Do not touch Render, Hermes backend, production Supabase or main.
- Inspect existing Android chat/API/model routing before choosing the interpreter. If the current API cannot provide structured interpretation, expose the gap and build a testable contract/fallback rather than claiming semantic support.
- Multilingual intake is a separate track from the approved persistent queue/worker sequence. Both tracks remain incomplete until their own tests and exact-head CI pass.
- Requirements document: docs/VYOM_FACTORY_MULTILINGUAL_INTAKE.md (commit 3bcd3eb2eba59fffd9cc21cac8387c21b6b64380).


## 2026-11-04 — Android Factory structured-intake contract (implementation foundation)

- Added `VyomFactoryIntakeDraft`, a pure Kotlin schema boundary for a future multilingual interpreter. It validates schema version, required fields, priority, confidence, bounded arrays/strings, and retains the caller-supplied original Unicode request separately from model output.
- Low confidence or a clarification question forces clarification; destructive request cues and P0 force explicit confirmation.
- Added JVM tests for Bengali Unicode preservation with a technical filename, Japanese original text with low-confidence clarification, Bengali destructive confirmation, invalid schema/priority, malformed acceptance criteria, and out-of-range confidence.
- Commits: `db16dbcc425908fe12e31d68df149b78cafb80ac` (contract), `c6a281210759fa3dfcccf7e13a87b4b460bd61cc` (tests).
- This is a validation contract only. It does NOT perform language detection, semantic interpretation, model calls, task creation, or worker execution. Current Android chat API still returns a plain text `reply`; no structured interpreter is wired. Existing prefix-only intake and fixed P2 behavior remain unchanged.
- The original request is preserved as a separate immutable constructor input; affected components and priority rationale are untrusted interpreter proposals until grounded/confirmed. No task registration path consumes this draft yet.
- No backend, Render, production Supabase, or `main` changes. No APK produced.
- Verification: pending CI on this branch's exact latest HEAD. Do not accept this foundation until Debug and Full CI pass on the same SHA.
- Next: inspect current model-routing options available in the Android app without changing backend contracts; add an explicit interpreter adapter only if an existing supported model endpoint can return validated structured output. Otherwise keep this contract disconnected and report the capability gap. In parallel, resume Factory Stage 1 persistence/reconciliation tests before implementing the worker sequence.


## 2026-11-04 — Factory Stage 1 persistence tests

- Added `VyomFactoryTaskStorePersistenceTest` using an in-memory `SharedPreferences` implementation. Covers persisted queue across store recreation, priority ordering after recreation, duplicate rejection without overwriting, persisted removal, malformed JSON fail-closed without data overwrite, and failed commit reporting.
- Refactored `VyomFactoryTaskStore` to accept injected `SharedPreferences` for deterministic JVM testing; the existing Context constructor remains the production entry point and uses the same app-private preferences file.
- Commits: `1f0d86cec78aaffbfc76031883d2f8fb6dc2ccec` (tests first), `df003ecfab600d1366f7a5af54683d2af3306992` (initial injection), `b23118ce2dbbe061fc9187ff17f4c0eb0e94b9f1` (constructor correction), `2a0657c241262d562f6f380efcc8e8bae0b7e984` (final constructor cleanup).
- Prior multilingual contract PR CI passed: Debug run `41779756383` and Full run `41779756385` on `e56c84aa122cadcb1b4c37585390baa6fb6a0e56`. These do not verify the newer persistence-test commits.
- Current persistence-test branch HEAD: `2a0657c241262d562f6f380efcc8e8bae0b7e984`. CI pending; no local Gradle execution performed in this session.
- Scope remains Android-only. No WorkManager, worker claim/lease, heartbeat, retry, STOP, recovery, or app-closed execution is implemented yet. No APK.
- Next: run CI against this exact branch HEAD and fix actual logs. If green, proceed with queue reconciliation for the GitHub-create/local-enqueue crash window before adding a worker.


## 2026-11-04 — Factory Stage 1 exact-head CI and chat registration recovery

- Exact pre-change HEAD `ce3d9cdd761740f777ea9e2ab834446f6b29cb13` completed both workflows successfully:
  - Debug run `41780598146`: job completed SUCCESS; build/test, versioned APK/checksum packaging, no-APK demo package, and artifact upload steps completed successfully.
  - Full CI run `41780598004`: job completed SUCCESS; Termux adapter syntax/status checks, unit tests/debug APK, packaging and artifact upload completed successfully.
- This verifies only `ce3d9c...`, not commits made after it. The workflow artifact is a CI verification artifact only; no APK is being delivered before all Factory acceptance stages.
- Found a concrete recovery gap: chat complaint registration marked any GitHub-create/local-enqueue exception as terminal `error`. `resumeQueuedChatMessages()` only retries `queued` messages, so a process/storage failure after GitHub accepted the issue could leave the shared issue unqueued locally.
- Changed the chat complaint failure path to retain the original message UUID and return it to `queued`, with an honest pending/reconnect explanation. On the next reconnect, existing `createTask()` searches by the same source/idempotency key before creating; then local enqueue is retried. This covers retry after response loss and local enqueue failure, subject to the existing non-atomic read-before-create race across concurrent clients.
- Added a persistence regression test: after a simulated failed commit, retrying the same canonical issue succeeds once, and a duplicate enqueue is rejected.
- Commits: `73576b74a5d5d8d3e3e7447889f3436b77f0115f` (retryable chat registration failure); `0bb4f8ae90f70b0407cc8c5dd4775fca44c39c64` (queue retry test).
- Current acceptance still requires new exact-head Debug + Full CI for the changes above. The previous successful CI does not verify them.
- Remaining limitations: retry happens on reconnect, not continuously in the background; Factory dialog retains its pending draft ID for user retry but does not yet run a durable reconciliation worker; GitHub create idempotency remains read-before-create and is not atomic across clients. No claim of worker execution is made.
- Next: verify CI for the new exact HEAD, fix real failures, then finish Stage 1 reconciliation design/tests before worker claim + lease. No backend, Render, production Supabase, or `main` changes.


## 2026-11-04 — Factory local inbox reconciliation (implementation batch; CI pending)

- Added `VyomFactoryTaskStore.reconcileOpen()`: validates candidates, merges only identities not already present by issue number or source ID, and commits all additions in one SharedPreferences write under the existing process lock. A failed commit throws and leaves the prior persisted value intact.
- Added persistence tests for restoring missing registry tasks, idempotent repeated reconciliation, issue/source identity conflicts without overwriting existing tasks, and failed batch commit preserving the previous queue.
- Wired best-effort reconciliation after successful Android pairing/reconnect. It reads GitHub Issues and imports only issues where both GitHub issue state and Factory metadata status are `open`. Paused, blocked, completed, cancelled, and `in_progress` metadata are not imported. Registry/token failures are deliberately non-fatal to pairing/chat and retry on the next reconnect.
- Commits: `395b08e834644e5f7cea2104dae1c44c24cb7d62` (atomic reconciliation API); `ed67d8aab1dd5cd04eabbfa8d48686fddbb06722` (persistence tests); `cb12e47080b1b0e68b3ed5312de168101cd5b2d3` (pair/reconnect wiring).
- This is still Stage 1 inbox recovery, NOT a Factory worker. There is no claim/lease, heartbeat, retry/backoff worker, STOP/pause execution control, crash recovery worker, bounded parallel execution, or app-closed execution verification yet. Queue data remains device-local; GitHub read-before-create remains non-atomic across concurrent clients.
- Verification: CI for prior exact HEAD `b19687a555b9097846676b43437f846d0af1e9ff` was still running when this batch began (Debug `41781513162`, Full CI `41781513083`). Those runs do not verify this batch. New exact-head CI must be checked after the final HANDOFF commit.
- No APK delivered. No backend, Render, production Supabase, or `main` changes.
- Next fixed step after exact-head CI: inspect actual worker prerequisites and implement worker claim + lease protocol before heartbeat or later stages. Do not jump ahead.


### CI failure correction — 2026-11-04

- Exact-head CI on `b87ec931f08fb97984b63db251cc85089848f6fe` failed in both Debug run `41781945678` and Full CI run `41781945713`.
- Actual failure: `VyomFactoryTaskStorePersistenceTest.reconciliationSkipsIssueAndSourceIdentityConflictsWithoutOverwriting`; the assertion expected issue 41 before issue 43 even though the existing queue contract correctly sorts P1 before P2.
- Corrected only the test's expected order to `[43, 41]`; production queue policy was not changed. Commit: `c7833dcf55e0e9de7e790f5dade65ed00ec3166c`.
- This is a test expectation correction, not a new feature. Reconciliation implementation from the preceding batch remains the implementation; do not recreate it.
- CI for this correction's new exact HEAD is pending. Worker claim + lease remains next only after verifying CI and re-inspecting current worker code.
