# VYOM Roadmap

## M1 status (2026-10-26)

- P2/P3: BLOCKED pending user flag `PUBLIC DONE`. Do not change repository visibility.
- P4: BLOCKED pending user flag `SECRETS DONE`. Do not read or print signing secrets.
- B1: IN PROGRESS — inspect Hermes executor architecture before changing behavior. No merge, deploy, migration apply, or production DB change without user approval.
- U1/U2/U3: NOT STARTED. Must remain in `vyom-android` branch `fresh-history/android-app-copy`; each UI step requires build + APK artifact + apkanalyzer evidence.
- Hermes repo remains private; do not touch local `~/hermes` working tree.

## Verified evidence so far

- `vyom-android` branch `fresh-history/android-app-copy` was reported at `c93a2ab94fcecbb1ad22e26cbd58f6e3c97a28ec` in prior handoff. `ROADMAP.md` did not exist at that ref when fetched.
- Hermes `main` package.json blob `5a3b31b080257d8e8b7181675cd637b59073f195`: test script runs Node tests including Vyom API/store/dispatcher/finish-claim/stale-recovery.
- Hermes `main` `services/vyom-task-dispatcher.js` blob `345d85a2a73b768b53d6dc8c0b9e314f1d9fea21`: dispatcher has bounded concurrency (default 3, allowed 1-8), claims queued tasks, invokes createInternalTask/createPlan/executePlan, and persists progress/result to task store.
- Hermes `main` `services/vyom-task-store.js` blob `1682bd3b12f044397f4a9d07d526c8d74d384bb4`: task persistence uses Supabase `vyom_app_tasks`; no schema/migration applied.
- Hermes `main` `integrations/github-actions.js` blob `8b81f9892fa0ec920c618ce4a72416198ab34a77`: GitHub Actions dispatch/run/log helpers exist; do not assume Actions are available.
- Tests were inspected via GitHub source only; they were NOT executed in this run.

## RESUME AT: B1-1

Inspect `services/vyom-task-dispatcher.js` and `services/vyom-api.js` completely, trace where `executePlan` selects execution backend and where file/syntax/workspace/smoke/validate-files operations run. Design test-first local executor branch that keeps Actions only for browser/APK. Add tests, implement on a new Hermes feature branch, verify with `npt test` (no environment overrides), and open PR only. No merge/deploy/migration apply. If GitHub API cannot safely make/test the change, stop and report exact blocker rather than claiming completion.
