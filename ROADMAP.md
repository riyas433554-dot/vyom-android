# VYOM Roadmap

## M1 status (2026-10-26)

- User reported PUBLIC DONE and SECRETS DONE. GitHub repository metadata read in this run still returned `visibility: private`; do not change visibility from this run.
- Debug workflow added: `.github/workflows/vyom-android-debug.yml`; commit `7a404043a8b2d861715c248c6b9c949c5dc8ec9d`. Push trigger targets `fresh-history/android-app-copy`; PR trigger targets that base branch; builds/tests unsigned debug APK and uploads versioned artifact.
- Build result/check-runs/annotations: UNVERIFIED. `fetch_commit_workflow_runs` returned an empty run list for workflow-triggering commits; no run URL, annotations, artifact size, APK SHA, or version metadata could be confirmed. No failure logs were available to compare or retry.
- Release workflow strengthened: commit `aeb5b771b1e1c989b4e41df430d9c45f1df54aff`; tag/manual release workflow now includes apksigner verify and exact expected certificate SHA-256 comparison. No tag/release was created because debug build success is unverified.
- U1 PR #1: https://github.com/riyas433554-dot/vyom-android/pull/1, draft, head `6bd274234374b2e564923b728cb7e6764692e812`. Adds an orbit-card overlay with drag rotation, but cards are placeholders and not wired to live tasks; build is unverified. No merge.
- B1 Hermes PR #46: https://github.com/riyas433554-dot/hermes/pull/46, draft, head `a030db3bddd0170004164a6a06873dbf6e2d6c9d`. Adds bounded local execution for syntax/smoke/workspace/validate-files and focused tests. Browser remains on Actions. Tests were not run; no merge/deploy/migration apply.
- Termux verification script: `scripts/verify-local-executor-termux.sh` on PR #46; clones the feature branch to a temporary directory, so it does not touch `~/hermes`.
- U2/U3: NOT STARTED. Each needs its own PR/commit and verified build URL before calling complete.
- Hermes remains private; no production DB changes, merge, deploy, or migration apply.

## RESUME AT: M1-2

1. Reconcile why GitHub metadata still reports `private` and why Actions run list is empty despite PUBLIC DONE.
2. Once the debug workflow has a real run, inspect run conclusion, checks/annotations, job logs, artifact name/size, APK SHA-256, versionName/versionCode. Fix and retry no more than twice.
3. Only after a successful debug build, create a `vyom-v*` tag and verify signed APK certificate, manifest, and public release URL.
4. Finish U1 by wiring real task state and waiting for verified CI; then implement U2 and U3 independently with build URLs.
5. Review PR #46 local execution routing and run `scripts/verify-local-executor-termux.sh`; no merge/deploy/migration without approval.
