# Vyom Android

Standalone Android application source copied from the `android-app/` directory of Hermes. This repository has independent Git history.

Backend endpoint is configured at build time with `HERMES_BASE_URL` (default: the existing Hermes Oregon API). Signed release builds require repository Actions secrets documented in `.github/workflows/vyom-android-release.yml`.

## U1 live Orbit tasks
The feature branch `feature/u1-live-orbit-tasks` renders compact task title/status snapshots from the app's existing task store. The canvas is mounted behind interactive panels so task cards do not cover controls. This is source work only until the branch's debug workflow and on-device pairing/chat regression checks pass.
