# VYOM Roadmap

Last updated: 2026-10-26

## U1 — Live Orbit task cards
- SOURCE IMPLEMENTED (CI verified on commit `cbf9e80917f5cc07976f3c4fc9a7eb38b074e6af`; phone verification pending): `OrbitHomeView.kt` renders up to five task title/status snapshots with status-based colors.
- SOURCE WIRED: `MainActivity.kt` retains the OrbitHomeView instance and feeds snapshots from `tasksById` whenever `renderTasks()` runs.
- Canonical detailed task list remains in the Tasks panel; existing pairing/chat/backend flows were not intentionally changed.
- Evidence commits: `6c5703b9656cf9517d5c05b853167f6ad47e8102` (Orbit renderer), `680c0e41e40ec2307310fa9fa869ee49ef84ba45` (live wiring).
- CI EVIDENCE: run [40288062727](https://github.com/riyas433554-dot/vyom-android/actions/runs/40288062727) passed `testDebugUnitTest assembleDebug` and uploaded artifact `vyom-0.3.0-cbf9e80917f5cc07976f3c4fc9a7eb38b074e6af`; artifact SHA-256 `77d69dc9ecae1c8199735f1ef6d9a1ff4dd518720783edb28770f9eb3bebed3a` (archive digest). Artifact size 10,762,473 bytes. CI is green; phone installation, pairing/chat regression and live task rendering remain required.

## Build / release gates
- Debug CI: an earlier run was blocked before runner startup by account billing/spending restrictions. The latest feature-branch run subsequently completed successfully; see U1 evidence above.
- Release signing: UNVERIFIED. Signing secrets/certificate and signed APK have not been verified in this work.
- Any successful build must report run URL, commit SHA, task results, artifact name, APK size, versionName/versionCode, and SHA-256. Phone install/chat/pairing tests remain required.
- No backend, Render, Supabase migration, production database, or Hermes deployment changes are in scope.

## Remaining roadmap
- 6A Hermes execution/recurring tasks: keep PR #37 draft until CI runner, disposable DB/RPC, crash recovery, recurring-task duplicate/retry behavior, and generated-file result on phone are all evidenced green. Do not merge/deploy or apply production migrations without explicit approval.
- 6B Android multi-panel UI: preserve pairing/chat; complete task notifications and panels only after build/test gates.
- 7 Settings/themes/orb: verify settings persist across app restart on a phone.
- 8 Voice: TTS/mute/mic/listening code exists in MainActivity; actual phone tests for TTS, speech recognition, lifecycle and mute remain pending.
- 9 Files/links/screenshots: implement and test artifact viewing/open-related-panel flow.
- 10 Android control: permission/confirmation and local audit-log code exists; phone-test allowed actions, denied permissions, confirmation and audit trail.
- 11 Self-update: signed APK, signing certificate SHA-256, version/SHA validation, rollback and real update test pending.
- 12 Polish/persona: Hinglish persona shaping exists; final visual polish and regression tests pending.

## Safety / reporting rules
- Hermes repository remains PRIVATE. Do not change its visibility.
- Do not merge PRs, deploy Render, apply migrations, or alter production DB without explicit user approval.
- Do not touch the user's Termux `~/hermes` working tree.
- Do not claim tests/build/phone tests passed unless actual output or artifact is available.
