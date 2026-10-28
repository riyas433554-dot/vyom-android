# VYOM Roadmap

Last updated: 2026-10-28

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

## Phase 6 — Self-update and constrained self-modification

- IMPLEMENTATION COMPLETE on `feature/u1-live-orbit-tasks`: user-triggered update check/download/install flow; proposal-only self-modification; protected-path policy; local audit logging; rollback backup and verification.
- Update gates: allowlisted GitHub release assets only; newer version required; APK capped at 100 MiB; SHA-256, package name, version code and installed signing certificate must match before Android's installer opens. Android 10+ rollback backup is stored under Downloads/VyomRollback and checked for size/SHA-256; checksum is rechecked before opening the rollback installer. Android's own installer requires a final user confirmation.
- Self-change gates: encrypted on-device GitHub token; bounded title/path/content; only allowed Kotlin source paths; policy/control/audit/kill-switch/update files blocked; creates a separate `vyom-proposal-*` branch and opens a PR against `feature/u1-live-orbit-tasks`. No writes to `main`, auto-merge, auto-release, or auto-install.
- CI guard: full Git history is fetched so Gitleaks can scan the pushed commit; secret scan and the proposal path guard pass on the tested workflow. The guard rejects non-Kotlin paths and policy-critical paths in proposal branches/PRs.
- CI EVIDENCE: [run 40677905303](https://github.com/riyas433554-dot/vyom-android/actions/runs/40677905303) passed secret scan, `testDebugUnitTest assembleDebug`, packaging and artifact upload on commit `c3625015300f7a2be4474f43168cfcb9981744bc`. Phase 6 artifact: `vyom-phase6-c3625015300f7a2be4474f43168cfcb9981744bc`, 25,329,026 bytes (artifact ZIP), SHA-256 `7fc7f2e955a4c2ddffe0b548f30eb74fd47d2038ef8d69a62399d8facce50213`. [Debug APK run 40677904789](https://github.com/riyas433554-dot/vyom-android/actions/runs/40677904789) also passed and uploaded `vyom-0.3.0-c3625015300f7a2be4474f43168cfcb9981744bc` (25,329,035 bytes); versionName `0.3.0`, versionCode `3`, APK SHA-256 `fa8d10ee524dbde2d0ca915edfcebd20a8393af4c6a1d9a0fb209f4832906646`.
- PHONE / RELEASE GATES STILL PENDING: no phone testing was performed. A published release manifest/APK and a real update/rollback installation have not been verified on-device. Do not mark the app ready until the user performs the required phone checklist.
- Repository branch protection is currently disabled; CODEOWNERS documents ownership but does not enforce review without branch protection. The app's self-change allowlist and CI proposal guard remain active.

## Remaining roadmap
- 6A Hermes execution/recurring tasks: keep PR #37 draft until CI runner, disposable DB/RPC, crash recovery, recurring-task duplicate/retry behavior, and generated-file result on phone are all evidenced green. Do not merge/deploy or apply production migrations without explicit approval.
- 6B Android multi-panel UI: preserve pairing/chat; complete task notifications and panels only after build/test gates.
- 7 Settings/themes/orb: verify settings persist across app restart on a phone.
- 8 Voice: TTS/mute/mic/listening code exists in MainActivity; actual phone tests for TTS, speech recognition, lifecycle and mute remain pending.
- 9 Files/links/screenshots: implement and test artifact viewing/open-related-panel flow.
- 10 Android control: permission/confirmation and local audit-log code exists; phone-test allowed actions, denied permissions, confirmation and audit trail.
- 11 Self-update: implementation and CI verified in Phase 6; published release asset and real update/rollback installation test remain pending on a phone.
- 12 Polish/persona: Hinglish persona shaping exists; final visual polish and regression tests pending.

## Safety / reporting rules
- Hermes repository remains PRIVATE. Do not change its visibility.
- Do not merge PRs, deploy Render, apply migrations, or alter production DB without explicit user approval.
- Do not touch the user's Termux `~/hermes` working tree.
- Do not claim tests/build/phone tests passed unless actual output or artifact is available.
