# Android Control Capability Matrix

Status: implementation inventory at the current feature-branch source checkpoint. A source implementation is not a physical-device acceptance claim.

## Owner authorization model

The owner has explicitly requested broad Android control. Vyom's persistent identity prompt now treats direct user task instructions as authorization for ordinary, non-destructive actions in owner-enabled apps, avoiding needless repeat confirmations.

The Android app still needs its local owner setup because a chat instruction cannot alter the installed APK's local preferences or Android's system-owned grant state. In Profile > Control Readiness > Set Up Permissions:
- Enable Accessibility for Vyom in Android Settings.
- Choose Enable all safe installed apps to turn on Master App Control and allowlist visible non-financial launcher apps.
- Enable Android System UI / Quick Settings separately; this grants only the existing narrow Quick Settings controls.
- Grant microphone, camera, notifications and Modify System Settings only when the corresponding task needs them.

The setup UI reports current grants and the Accessibility service connection. Missing permissions are never reported as granted. Runtime and special permissions are granted only by Android's own system UI.

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
