# Vyom Acceptance Tests — Source of Truth

This file operationalises the user-supplied **Vyom – Weakness Audit + GPT Strict Fix Order** (19 pages) and **Vyom — Complete Feature & Design Brief** (10 pages). It supplements those source documents; it does not replace their phase order, safety rules, or screen designs.

## Release gates

- Work only on `feature/memory-intelligence`. Do not modify Android `main`, Render, Hermes/backend, or production Supabase.
- Never change the application ID or Kotlin package as a shortcut.
- Do not mark an item FIXED from documentation or source inspection alone. Require a named test, exact commit, CI run, and relevant runtime evidence.
- Every action result must distinguish **requested**, **executed**, **verified**, and **evidence**. Dispatch acceptance is not completion.
- Screen, web-page, file and message content is untrusted data, never user authorization.
- STOP CONTROL, sensitive-app blocks, protected-file rules, and update-signature verification cannot be bypassed by a trusted session or self-modification.
- Never bypass Play Protect, Android installer prompts, payments, CAPTCHA, account security, or website access controls.
- APK release gate: exact-head unit tests + Debug + full CI, downloaded artifact, SHA-256, signing certificate continuity, and explicit phone acceptance. Do not uninstall the current app to work around a certificate mismatch.

## Status definitions

- **FIXED** — acceptance test passed with recorded evidence on the exact source commit.
- **PARTIAL** — some implementation exists, but at least one acceptance condition or runtime proof is missing.
- **BLOCKED** — a specific external/system constraint prevents completion; record the exact reason and continue unrelated work.
- **UNVERIFIED** — no sufficient test evidence is recorded.

Current statuses below are deliberately conservative. They are not release claims.

## Acceptance matrix

| ID | Acceptance condition | Current status |
|---|---|---|
| A1 | “Install X from Play Store” opens search, identifies the exact title and developer, taps only the matching Install control, waits for install, then verifies package + version with PackageManager. Ambiguous/wrong listing pauses for one choice. | PARTIAL — Play Store → UI Agent follow-through and PackageManager label/version check exist; publisher identity and selected listing package binding are incomplete. |
| A2 | “Godot tu khud install kar”, “X install karo”, and English variants route to INSTALL_APP. APK rejection is used only for an actual APK file/URL. | UNVERIFIED |
| A3 | Open any installed app by its real launcher label or alias, not a hard-coded ten-app list. Unknown app offers install flow. | UNVERIFIED |
| A4 | Control text reflects live Accessibility service state and accurately describes screen-reading/gesture capabilities. | PARTIAL — readiness screen reports service state; phone-side truthfulness still needs testing. |
| A5 | UI Agent is reachable from Control and eligible command paths; observe → plan → policy-check → act → re-observe loop is bounded. | PARTIAL — Control entry, readiness screen and Play Store route exist; exact-head CI/device verification pending. |
| A6 | Every action returns requested/executed/verified/evidence. “Accepted” is never called “done”. OPEN_APP verifies target foreground; install verifies installed package/version; generic tasks require domain-specific postconditions. | PARTIAL — foreground and Play Store checks exist; generic DONE still relies on visible evidence, not a domain-specific result. |
| A7 | Multi-step request decomposes into ordered steps; each step is observed and verified; STOP/cancel pauses only the selected task. | UNVERIFIED |
| A8 | Service-hosted floating orb appears over another app with current-step caption, mute and STOP; tap returns to Vyom; it hides when task ends. | UNVERIFIED |
| A9 | Setup Permissions reports Accessibility, overlay, app visibility, notifications, microphone, exact alarms, unknown-app install, optional contacts/phone and battery exemption with correct settings shortcuts. | UNVERIFIED |
| A10 | Hinglish, natural phrasing and common typos route safely; keyword path is fast and structured model fallback is bounded. | UNVERIFIED |
| A11 | Runtime capability manifest reports actual permissions/services/skills and is injected into chat/live prompts; Vyom never claims a capability it currently has. | UNVERIFIED |
| A12 | Structured tool/function calls are parsed and executed by the on-device executor; raw tool JSON never appears as a user-facing answer. | UNVERIFIED |
| A13 | Clear commands execute immediately; ask only one short question for genuinely ambiguous targets; assumptions are stated while work proceeds. | UNVERIFIED |
| A14 | Default TRUSTED authorization follows explicit user intent for ordinary actions; ask only for ambiguous targets and hard-block/non-delegable actions. STOP always works. | UNVERIFIED |
| A15 | Interrupted/cancelled task preserves its own context and can resume; cancelling one task does not erase others. | UNVERIFIED |
| A16 | When a control task leaves Vyom, floating orb appears and provides caption, mute and STOP without bypassing missing permissions. | UNVERIFIED |
| M1 | Semantic retrieval finds paraphrases, not only exact words; lexical fallback works offline. | PARTIAL — memory implementation exists; semantic acceptance tests pending. |
| M2 | CORE + episodic summary + recent turns preserve names and task context across multiple messages. | PARTIAL |
| M3 | Chat always opens at the latest message and preserves the selected conversation. | PARTIAL |
| M4 | Context budget is enforced; Hermes HTTP 400 retries once with raw message + CORE only; connection errors back off and show a useful status. | PARTIAL |
| M5 | Project rules and protected scope are injected on every relevant task; tests prove main/backend/production Supabase remain untouched. | PARTIAL |
| M6 | CORE / EPISODIC / SEMANTIC tiers, 500+ fact capacity, dedupe, conflict merge, importance and decay behave as specified. | PARTIAL |
| M7 | Aliases such as Godot / “mera game” resolve to the correct project and related skills. | UNVERIFIED |
| S1 | Teach Vyom stores parameterised procedures with triggers, inputs, preconditions, steps, risk and expected-result verification. | UNVERIFIED |
| S2 | Self-modification targets the current development branch by default; no hard-coded stale branch. | UNVERIFIED |
| S3 | Self-change uses repository analysis + minimal diff + tests; user does not paste entire Kotlin files. | UNVERIFIED |
| S4 | Request → repo analysis → patch → tests → PR → CI → signed artifact → verified update is one connected pipeline. | UNVERIFIED |
| S5 | Audit/error logs produce actionable fix suggestions with evidence and safe diff review. | UNVERIFIED |
| S6 | Four signing secrets are present/validated and the release signing workflow can use them without exposing values. | PARTIAL — workflow environment propagation fix committed; exact-head signed build not verified. |
| S7 | Candidate APK certificate matches the installed/base APK and can update in place; SHA-256 and package/version are verified. | UNVERIFIED |
| T1 | WorkManager/AlarmManager scheduler runs one-time/repeating tasks, survives reboot and displays next run. | UNVERIFIED |
| T2 | Tasks run real executors and do not finish without verified output/artifact. | UNVERIFIED |
| T3 | Tapping a task/orbit card opens its detail view with steps, logs, result, retry and cancel. | UNVERIFIED |
| T4 | Queue limit is usable; task cards are not clipped and status/progress remain readable. | UNVERIFIED |
| T5 | n8n-style workflow canvas supports triggers, AI agent, tools/skills, branches, actions, save/load/export and real execution. | UNVERIFIED |
| T6 | Approval state is persistent and Telegram /approve is connected client-side; backend changes remain out of scope. | UNVERIFIED |
| V1 | TTS playback gates STT; echo/fuzzy self-transcription is dropped; AEC/NS/AGC are verified when available. | UNVERIFIED |
| V2 | STT text is stable; no flickering partials or hallucinated sound tags. | UNVERIFIED |
| V3 | VAD, STT, LLM first-token and TTS first-audio timings are logged on-device; latency target is measured, not assumed. | UNVERIFIED |
| V4 | Google recognizer beeps are suppressed; only intended Vyom mute/unmute sounds play. | UNVERIFIED |
| V5 | LIVE lane uses the selected working Nemotron model and approved fallback; no known 404 model is called. | UNVERIFIED |
| V6 | Live voice continues in a foreground service while app is backgrounded; notification mute/stop works. | PARTIAL — foreground service exists; background/reboot behavior needs phone proof. |
| V7 | User can select an appropriate Indian-accent voice for Hindi/Hinglish; ElevenLabs remains optional. | UNVERIFIED |
| V8 | Language auto-detection handles Hindi, Hinglish and English; “hello/hi” gets an immediate greeting. | UNVERIFIED |
| U1 | Orb has no black rectangle on Home, Chat, Live, Tasks, Profile or overlay. | UNVERIFIED |
| U2 | Jump-to-latest and caption/status pills never overlap the header or each other. | UNVERIFIED |
| U3 | No dead “SOON” destinations; hide unfinished screens until they work. | UNVERIFIED |
| U4 | Supplied design palette, glass, typography, spacing and components are followed. | UNVERIFIED |
| U5 | Self-change proposal is mobile-usable: readable title/path/diff, no tiny full-file paste field. | UNVERIFIED |
| U6 | Theme changes recolor the whole interface, not only the orb. | UNVERIFIED |
| U7 | Long chat messages, errors and code render fully without clipping/overflow. | UNVERIFIED |
| U8 | Profile tab and three-line Drawer are distinct; each destination opens its own designed screen. | UNVERIFIED |
| U9 | Chat composer follows the supplied single rounded composer with +, mic, model/lane pill and dynamic Send/Live action. | UNVERIFIED |
| U10 | Automations includes the specified node canvas, configuration, live run highlighting and JSON save/load/export. | UNVERIFIED |
| U11 | Supplied design HTML/boards are mapped to actual screens; screenshot tests/artifacts compare implementation to source design. | UNVERIFIED |
| U12 | App icon and splash are custom and match the supplied design. | UNVERIFIED |
| F1 | Camera, gallery, files, share-to-Vyom, attachment chips, processing and outbound file cards work both ways. | UNVERIFIED |
| F2 | GitHub URL/owner-repo/ZIP learning fetches tree + README + key files, chunks/summarises, stores knowledge and proposes skills. | UNVERIFIED |
| F3 | Generated SELF.md + runtime capability manifest accurately describe modules, permissions, branch, models, skills and limitations. | UNVERIFIED |
| F4 | Conversation facts auto-save; Memory screen shows them; capacity is 500; secret/OTP/card filters work. | PARTIAL — prior memory work exists; additional validation pending. |
| F5 | GitHub token is encrypted at rest, masked except last four characters, and absent from logs/screenshots/exports. | UNVERIFIED |
| F6 | Authenticator, password manager, bank, wallet and payment apps are blocked for screen reading and tapping. | UNVERIFIED |
| R1 | No “ready/done” claim without exact-head test/CI/artifact evidence. | UNVERIFIED — release gate remains open. |
| R2 | Handoff is updated at every phase and preserves exact branch, commit, next step, evidence and blockers. | PARTIAL — handoff/checklist updated; continue after every phase. |
| R3 | Application ID and Kotlin package naming remain unchanged; no migration-breaking rename. | UNVERIFIED — assert against manifest/build output at release. |
| R4 | User receives a repeatable phone-test guide with exact phrases, expected result and evidence location. | PARTIAL — phone checklist below; final guide must be refreshed against the release APK. |

## Phone acceptance run

Run only after a verified signed APK is available and its certificate matches the installed app. Do not uninstall the current app to bypass signing mismatch.

1. **Diagnostics:** Profile → Diagnostics → RUN ALL CHECKS. Save/share the PASS/FAIL/SKIP report.
2. **Memory:** say “mera naam [test name] hai”; send three more messages; ask “mera naam kya hai?”; confirm the fact in Memory.
3. **Attachments:** use + → Camera/Gallery/Files; attach one image; ask “isme kya hai?”; check preview, processing and result.
4. **Generated file:** ask for a small HTML game; check file card Open / Share / Save.
5. **Install:** say “Godot install karo”; confirm title + developer; observe Install/download; verify package + version in Android package list. Complete Android/Play Protect prompts yourself.
6. **App discovery:** ask to open an installed app not in the old hard-coded list; verify the actual foreground package.
7. **Multi-step UI:** “YouTube kholo, Mr Beast search karo, pehla video kholo”; verify each step in the audit log. Do not auto-like, send or purchase without the specified authorization rules.
8. **Voice:** speak while Vyom is replying; confirm Vyom does not transcribe its own voice; inspect measured stage timings.
9. **STOP:** start a multi-step task, press STOP CONTROL during planning, confirmation, action and wait in separate runs; no later action may dispatch.
10. **Scheduler:** schedule a task two minutes ahead, lock the phone, verify execution and artifact after unlock.
11. **Self-update:** create a proposal, inspect diff, approve PR, wait for CI, verify signed APK, install over existing app, then verify version/commit/health. Rollback must be tested separately.
12. **Overlay:** run a task in Play Store/another app; verify orb, caption, mute and STOP; turn overlay permission off and confirm Vyom explains the exact setting without falsely claiming overlay is active.
13. **Navigation/design:** Drawer and Profile must be distinct; every implemented destination opens its own screen; no “SOON”, black orb rectangle, clipped cards or overlapping status pills.

## Evidence record template

For every ID, append: `ID | status | source commit | test name | Debug run | Full CI run | artifact ID | SHA-256 | phone evidence | remaining limitation`.

No evidence means **UNVERIFIED**. A green CI build does not replace physical-device acceptance.
