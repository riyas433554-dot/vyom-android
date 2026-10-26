# VYOM Roadmap

## M1 status (2026-10-26)

- P3/P4: BLOCKED by a state mismatch. User reports PUBLIC DONE and SECRETS DONE, but GitHub repo metadata currently reports `visibility: private`. Do not change visibility from this run; user confirmation/action needed to reconcile.
- P3 workflow/run URL: UNVERIFIED. No debug workflow was found at the guessed paths; directory listing and workflow dispatch are unavailable through the current GitHub connector.
- P4 signed release: UNVERIFIED. Signing secrets were not read or printed; no release build or certificate verification was run.
- B1: Source inspection only. No Hermes code changed, no tests executed, no PR opened.
- U1/U2/U3: NOT STARTED. No Android source edits or builds occurred. Do not claim an APK exists.
- Hermes remains private. No merge, deploy, migration apply, production DB changes, or local `~/hermes` working-tree access.

## Evidence

- `app/build.gradle.kts` blob `5969cb62373907ad83ec35c2448ac5a38ff17965`: app ID `com.vyom.assistant`, namespace `com.hermes.assistant`, version code/name default to 3 / 0.3.0; release signing uses environment variables `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- Hermes `services-execution.js` blob `336e556bf6a0b63725c16b459a42893dd16e8f80`: `requestExecution` persists an `execution_jobs` row, dispatches `hermes-executor.yml` using GitHub Actions, polls for the run, and marks failures when dispatch/run detection throws.
- Hermes `services/vyom-task-dispatcher.js` blob `345d85a2a73b768b53d6dc8c0b9e314f1d9fea21`: Vyom tasks are planned then sent to `executePlan`; task statuses/results are persisted through the Supabase store.
- The workflow file and Kotlin source paths could not be retrieved by the current connector. No full operation-routing trace or local-mode implementation was possible.
- No test/build/signature command ran. All CI/build/certificate results are UNVERIFIED.

## RESUME AT: B1-2

1. Resolve repository visibility mismatch in GitHub UI; confirm repo is public without exposing Hermes.
2. Inspect exact workflow and Kotlin source paths using authenticated repository tree access.
3. Add B1 local-executor implementation + focused tests on a new Hermes feature branch; retain Actions only for browser/APK. Open PR only, no merge/deploy/migration apply.
4. Run the single Termux verification script against that branch; do not claim green unless actual output is captured.
5. Then add P3/P4 workflow runs and U1, U2, U3 one at a time, each with a verified build URL and APK artifact; run apkanalyzer and apksigner certificate SHA256 comparison.
