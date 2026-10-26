# VYOM Roadmap

## M1 status (2026-10-26)

- P3: BLOCKED until explicit user flag `PUBLIC DONE`; repository visibility was not changed.
- P4: BLOCKED until explicit user flag `SECRETS DONE`; signing secrets were not read or printed.
- B1: IN PROGRESS, source reconnaissance only. No code changes, tests, merge, deploy, migration apply, or production DB changes.
- U1/U2/U3: NOT STARTED. Keep Android changes on `fresh-history/android-app-copy`; every UI step requires a build, APK artifact, and apkanalyzer version/SHA evidence.
- Hermes repo remains private. Local `~/hermes` working tree was not touched.

## Source evidence

- `vyom-android/ROADMAP.md` created in this run at commit `2ad20b1a00e9072a45811c12e9c3ba36d6131801`; subsequent roadmap update is recorded by the commit returned for this file.
- Hermes `main` `package.json`, blob `5a3b31b080257d8e8b7181675cd637b59073f195`: test script invokes Node's test runner over Android API/session and Vyom task-store/dispatcher/stale-recovery tests.
- Hermes `main` `services/vyom-task-dispatcher.js`, blob `345d85a2a73b768b53d6dc8c0b9e314f1d9fea21`: local dispatcher supports bounded concurrency (default 3, max 8), claims queued rows, runs `createInternalTask` → `createPlan` → `executePlan`, and persists task progress/results.
- Hermes `main` `services/vyom-task-store.js`, blob `1682bd3b12f044397f4a9d07d526c8d74d384bb4`: task rows live in Supabase `vyom_app_tasks`; no migration was applied.
- Hermes `main` `services/vyom-api.js`, blob `fb7933b91bd8c50617501a49f3c6b8eb114476cf`: API refuses task creation if the dispatcher is missing/not ready.
- Hermes `main` `services-execution.js`, blob `336e556bf6a0b63725c16b459a42893dd16e8f80`: `requestExecution` creates `execution_jobs`, dispatches `hermes-executor.yml` through GitHub Actions, and marks the job failed if dispatch/run detection errors. This is a separate execution-job path; call sites and operation routing still need full tracing.
- Hermes `main` `integrations/github-actions.js`, blob `8b81f9892fa0ec920c618ce4a72416198ab34a77`: dispatch, polling, jobs, and logs helpers exist.
- Existing dispatcher tests include concurrency, duplicate-dispatch protection, and queue continuation after a task fails (`tests/unit/vyom-task-dispatcher.test.cjs`, blob `aacf85c3dd2c6c8a49587ab449f7192c6435751e`).
- All findings are source inspection via GitHub connector. No test command was run; do not claim tests green.

## RESUME AT: B1-2

Trace every call site of `requestExecution` and the executor workflow operation switch, plus worker/tool implementations for `syntax`, `workspace`, `smoke`, and `validate-files`. Then write focused tests first, implement the local Render path while retaining Actions only for browser/APK, and run `npt test` with environment overrides removed. Use a new Hermes feature branch and open a PR only. If a real test runner/local execution cannot be obtained, stop with the precise blocker; never claim unrun tests passed.
