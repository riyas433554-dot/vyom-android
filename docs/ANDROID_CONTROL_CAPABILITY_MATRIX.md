# Android Control Capability Matrix

Status: implementation inventory at the current feature-branch source checkpoint. A source implementation is not a physical-device acceptance claim.

## Owner authorization model

The owner has explicitly requested broad Android control. Vyom's persistent identity prompt now treats direct user task instructions as authorization for ordinary, non-destructive actions in owner-enabled apps, avoiding needless repeat confirmations.

The Android app still needs its local owner setup because a chat instruction cannot alter the installed APK's local preferences or Android's system-owned grant state. In Profile > Control Readiness > Set Up Permissions:
- Enable Accessibility for Vyom in Android Settings.
- Choose Enable all safe installed apps to turn on Master App Control and allowlist visible non-financial launcher apps (excluding Vyom itself).
- Enable Android System UI / Quick Settings separately; this grants only the existing narrow Quick Settings controls.
- Grant microphone, camera, notifications and Modify System Settings only when the corresponding task needs them.

The setup UI reports current grants and the Accessibility service connection. Missing permissions are never reported as granted. Owner-tapped permission/settings navigation remains available even while STOP is active, so the owner can repair access; STOP still blocks agent-executed actions. Runtime and special permissions are granted only by Android's own system UI.

## Implemented in source

| Capability | Implementation | Runtime proof |
|---|---|---|
| App discovery | Launcher activity enumeration, package-to-app mapping and package visibility declarations | Physical-device app inventory pending |
| Cross-app UI | Accessibility tree observation, fresh revision/package checks, target fingerprints | Phone acceptance pending |
| Text/OCR interaction | Visible-node tap/long-press, OCR tap/long-press and bounded node/OCR swipe/drag | Phone acceptance pending |
| Visual controls | Android screenshot capture, local OCR and opt-in visual point planner with visible confirmation markers | OEM/device testing pending |
| Global navigation | Back, Home, Recents, Notifications and Quick Settings global actions | Phone acceptance pending |
| System UI | Separate opt-in, filtered safe Quick Settings labels, exact node/fingerprint checks and visible-state completion evidence | OEM/device testing pending |
| Reliability | Event/revision re-observation (including click/selection/focus events), foreground epoch checks, STOP generation invalidation, bounded retries/step count and action outcome evidence | Unit/build verification pending on this source head |
| Direct device actions | Volume and brightness readback; torch and launch/composer operations marked pending when no independent outcome is available | Phone acceptance pending |
| Permission inventory | Accessibility enabled + connected state, app master/allowlist, microphone, camera, notifications and Modify System Settings (manifest declares WRITE_SETTINGS) | System settings verification on device pending |
| Owner all-safe-app setup | One action enables Master App Control and allowlists all discovered non-financial launcher apps; protected financial identifiers stay blocked | User must activate in installed APK |

## Task-scoped grants

Not every permission is a global prerequisite. App switching and UI automation do not require microphone/camera/notification grants. Speech input needs microphone; camera capture needs camera; background notification display needs notification permission on Android 13+; brightness changes need Modify System Settings. The UI Agent progress overlay uses `TYPE_ACCESSIBILITY_OVERLAY` and is covered by the Accessibility service grant; Vyom must not ask for redundant Draw Over Other Apps access for that overlay.

## A17 owner scope — 20-row coverage requirements

A17 is M1 scope. The current UI Agent is partial baseline, not universal control. A1 Play Store and `whatsapp_send` are acceptance tests only. Each row below must have (a) a reachable generic adapter, (b) named automated test, (c) a Control Suite Diagnostics check, and (d) physical-device evidence where Android/OEM behavior is involved.

| # | Domain | Required generic operations | Status |
|---:|---|---|---|
| 1 | App lifecycle | Open/close/force-stop/clear/uninstall/install any app | PARTIAL |
| 2 | Arbitrary app UI | Forms, lists, search, dialogs, pickers, web pages | PARTIAL |
| 3 | System controls | Wi-Fi/Bluetooth/mobile data/hotspot/brightness/volume/DND/rotation/flashlight/airplane mode where allowed | PARTIAL |
| 4 | Notifications | Read/filter/act/dismiss | PARTIAL |
| 5 | Communications | Calls/SMS/contacts | PARTIAL |
| 6 | Camera and media files | Camera/photos/videos/screenshots/screen recording | PARTIAL |
| 7 | File manager | Browse/create/move/delete/zip/share | PARTIAL |
| 8 | Time and calendar | Alarms/timers/calendar/reminders | PARTIAL |
| 9 | Messaging | WhatsApp/Instagram/Telegram/email through generic UI | PARTIAL |
| 10 | Browser | Search/open/fill/download | PARTIAL |
| 11 | Clipboard and text | Clipboard and text entry in arbitrary fields | PARTIAL |
| 12 | Permissions | Per-app grant/revoke via authorized Power Mode where Android permits | PARTIAL |
| 13 | Device settings | Battery/storage/app info/settings pages | PARTIAL |
| 14 | Termux | Commands through owner-enabled bridge | PARTIAL |
| 15 | Location and maps | Location/map tasks with permission-aware routing | PARTIAL |
| 16 | Media control | Music/video/volume | PARTIAL |
| 17 | Complex gestures | Pinch/drag/multi-step | PARTIAL |
| 18 | Transient UI | Wait for and handle popups/ads/permission dialogs in ordinary apps | PARTIAL |
| 19 | Multi-app workflows | Pass data between apps and verify both ends | PARTIAL |
| 20 | Scheduling | Schedule supported tasks above and verify execution | PARTIAL |

## Control Suite and acceptance gate

Diagnostics → Control Suite must run 20 varied safe tasks across different apps/domains and show per-task PASS/FAIL/SKIP plus evidence. No default destructive actions, sends, calls, permission changes or sensitive settings. Every row needs implementation + named automated test + Diagnostics row. M1 stays open until all 20 rows have coverage and an owner-runnable physical-device subset passes on the actual phone.

Hard limits: FLAG_SECURE screens; biometrics and lock-screen credentials; Play Protect prompts; payments/banking/OTP/Authenticator apps; Vyom kill-switch and protected files. Never bypass these; report the exact blocked capability/permission and one-tap repair route if a permission is missing.

## Non-negotiable platform boundaries

- Accessibility cannot bypass secure windows, lock screens, biometrics, OTP/password fields, permission prompts, Play Protect or Android security confirmations.
- Payment/banking/wallet and other protected financial packages remain blocked by AppAccessPolicy.
- External send/post/publish, purchases, destructive actions and app installs retain their action-specific confirmation gates.
- System UI access is limited to the explicit Quick Settings allowlist. Notification content is filtered; screenshots/VLM upload are disabled on System UI.
- No root, ADB shell, hidden API, exploit, silent permission grant or accessibility-policy bypass is used.
- A screen action can fail because an app uses inaccessible custom Canvas controls, an OEM changes its UI, Android rejects a gesture, or the foreground app changes. Vyom must report the limitation rather than claim universal control.

## Learning and memory

The permanent Vyom identity instruction now says: learn before guessing; inspect trusted docs/repositories; review license, source, dependencies and security; extract a scoped skill; test safely; then execute. It stores verified skills, source/version, capability limits and test evidence as durable operating principles.

Added reference skill documents:
- `docs/skills/JARVIS_AGENT_ARCHITECTURE.md`
- `docs/skills/CONTINUOUS_LEARNING_AND_VERIFICATION.md`

These are knowledge packs for future repository-learning integration, not a claim that URL/ZIP ingestion, automatic skill extraction or an on-device eval runner already exists.

## Remaining work before calling this complete

1. Exact-head CI unit tests and APK build.
2. Review CI failures and fix until green.
3. Inspect resulting APK manifest, package/version and signing identity.
4. Physical phone tests: Accessibility grant, app-switch, text/OCR, visual controls, gestures, STOP mid-action, grant revocation, foreground switch, Quick Settings and background/Doze.
5. Implement repository/ZIP ingestion, encrypted skill registry and per-skill verification before claiming autonomous self-learning.
6. Only then publish an update-safe APK; never uninstall the existing app as a signing workaround.


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
