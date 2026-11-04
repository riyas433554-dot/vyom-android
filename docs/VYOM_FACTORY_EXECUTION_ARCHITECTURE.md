# Vyom Factory execution architecture

Status: design decision for the next implementation batch
Date: 2026-11-04
Scope: Android app repository only. No backend, Render, production Supabase, or Android `main` changes.

## Why the current claim UI cannot become a worker by itself

The current queue and lease are device-local SharedPreferences state. The foreground dialog owns a single-thread executor that is shut down when the dialog is dismissed. GitHub Issues currently carries a natural-language description plus schema-1 priority/status/source metadata; it does not carry a validated execution target or typed arguments. Therefore a background worker must not infer that arbitrary prose is safe to execute.

## Execution targets

Use separate, explicit adapters. Never silently fall back from one target to another.

1. `android_action`: an on-device action adapter. It may invoke only registered, allow-listed Android capabilities and must validate arguments, permission state, foreground/background constraints, and STOP state immediately before each side effect. Accessibility/Shizuku capabilities remain permission-gated. A generic shell command from issue prose is not an allowed action.
2. `repository_change`: a repository adapter. It dispatches a bounded job to the repository's isolated CI/Termux executor; the Android app must not treat its own private files as the source of truth or execute arbitrary issue text as shell. Changes must use a proposal branch, tests, review/approval, and no auto-merge/auto-install.
3. `manual_review`: the compatibility and safe default for existing schema-1 tasks, missing/invalid target metadata, unknown task kinds, and ambiguous requests. It never executes.

No schema-1 issue is auto-upgraded to an executable target. New task metadata must use a versioned, validated execution envelope and preserve the original user request verbatim.

## Scheduling and ownership

- WorkManager is the persistent scheduling mechanism for eligible bounded work, not a 24/7 guarantee. Android Doze, OEM battery management, force-stop, permissions, and OS policy can delay or stop work.
- A worker must re-fetch the GitHub issue and validate both remote issue state and metadata status immediately before starting.
- Current SharedPreferences leases are device-local only. They do not prevent two phones from claiming the same issue. Until a server-side atomic claim/CAS exists, execution must be explicitly single-installation or use a separate remote executor with its own atomic job claim. Never describe local leases as distributed locks.
- Worker input must contain only stable task identity, not copied untrusted instructions or secrets. Resolve the current task from the registry at execution time.
- Persist an execution record with attempt identity, state, timestamps, and result before reporting completion. A stale lease/token cannot write a terminal result.
- Heartbeat may renew only while a real handler is actively making bounded progress. Never renew idle claims.

## Lifecycle and safety rules

- `queued -> claimed -> running -> completed|error|blocked|paused` transitions must be explicit and validated.
- STOP/pause must be checked before every side-effect boundary. STOP is not equivalent to closing the Factory dialog.
- Retries must be idempotent, bounded, exponential-backoff with jitter, and distinguish retryable network/worker interruption from permanent validation/permission failures.
- Crash recovery must reconcile persisted execution records with a fresh registry snapshot and fenced lease. It must not blindly replay a possibly completed side effect.
- Bounded parallelism defaults to one worker until per-target concurrency and shared-resource safety are proven.
- App-closed behavior is not accepted until tested on a physical Android device, including process death, swipe-away, force-stop, Doze, reboot, grant expiry, and OEM battery restrictions.

## Implementation order

1. Add schema-versioned execution-envelope parsing and tests; schema-1/missing/unknown target always maps to `manual_review`.
2. Add an adapter registry and strict routing tests; unknown target must fail closed.
3. Add durable execution journal and idempotency/result fencing.
4. Add WorkManager scheduling only after a concrete adapter can execute one bounded, safe operation.
5. Add heartbeat tied to active progress, then bounded retry/backoff, STOP/pause, crash recovery, and bounded parallelism.
6. Verify app-closed behavior physically; only then update capability claims.

## Explicit non-goals

- No arbitrary natural-language-to-shell execution.
- No direct editing of the app's own source tree from an on-device worker.
- No claim of cross-device locking from SharedPreferences.
- No claim of guaranteed 24/7 background execution.
- No APK release while the required Factory stages remain incomplete.
