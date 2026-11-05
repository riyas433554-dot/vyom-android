# Universal Vyom Factory Architecture

Status: Step 2 architecture contract — proposed and locked for implementation sequencing.
Scope: one Factory orchestrates every registered task and product specialist. This is not a separate complaint-only worker and not a set of disconnected factories.

## 1. Product contract

Every registered complaint/task is eligible for automatic sequential handling by the same Factory. The canonical GitHub Issue remains the durable task identity and source of truth. The Factory must not require the owner to manually choose a Kotlin file or press a separate proposal button for ordinary safe work.

Default ordering:
1. P0 before P1 before P2 before P3.
2. Within a priority tier, oldest eligible issue first (stable FIFO by created timestamp, then issue number). Updated-at ordering must not starve older complaints.
3. Skip terminal, paused, blocked, not-yet-due retry, duplicate-active and policy-ineligible work without losing its queue position.
4. A task is "running" only after a fresh remote snapshot, a successful claim/lease and durable journal transition. "Completed" only after its declared postconditions are independently verified and the canonical issue is updated.
5. A blocked task records the reason, evidence, missing capability/owner action and retry condition; then the same scheduler may advance to the next eligible task.

The worker should continuously drain eligible work while permitted. Current foreground execution is the existing baseline; persistent/background scheduling and distributed claims are separate gaps to close, not assumptions.

## 2. Shared Task Engine (reuse, do not duplicate)

Keep and extend existing:
- canonical issue identity, metadata/idempotency and priority/state lifecycle;
- task inbox reconciliation from a fresh GitHub snapshot;
- lease/claim fencing and owner identity;
- execution journal and attempt IDs;
- retry schedule with idempotency-aware replay;
- STOP, foreground, consent and approval gates;
- evidence/postcondition verification and issue audit comments.

Required engine lifecycle:
`DISCOVERED -> QUEUED -> CLAIMED -> PLANNING -> AWAITING_APPROVAL | READY -> RUNNING -> VERIFYING -> COMPLETED`
Failure branches:
`RETRY_WAIT`, `BLOCKED`, `NEEDS_OWNER`, `CANCELLED`.
Transitions must be persisted and auditable. Do not add a parallel queue, journal, lease, retry, approval or evidence store.

## 3. Universal task envelope

Versioned, typed envelope fields:
- `schema_version`, `task_id` (canonical issue number), `source`, `source_id`, `idempotency_key`;
- `domain`, `intent`, `priority`, `created_at`, `deadline` (optional);
- `evidence_refs`, `reproduction`, `expected_postconditions`, `constraints`;
- `required_capabilities`, `selected_specialist`, `plan_id`;
- `risk_class`, `approval_requirements`, `rollback_plan_ref`;
- `attempt_id`, `lease_id`, `execution_target`, `verification_profile`.

Unknown schema versions, targets, capabilities, domains or malformed arguments fail closed to a typed blocked/manual-review result. Do not execute free-form model text as code or shell commands.

## 4. Universal Planner

Planner is deterministic at its safety boundaries and model-assisted only for interpretation/planning:
1. Normalize issue text/evidence and redact secrets/private content.
2. Classify domain and intent; extract repro, expected result and constraints.
3. Resolve ambiguity from available evidence; if critical facts are missing, ask owner or mark blocked.
4. Build a typed plan DAG with bounded steps, declared inputs/outputs, budgets, timeouts, idempotency, risk and postconditions.
5. Query Capability Discovery; choose a registered specialist and exact supported adapter.
6. Validate policy, approvals and rollback before dispatch.
7. Execute one bounded step at a time, persist state, re-observe after changes, and verify postconditions.
8. On failure, classify transient/permanent/policy/capability/verification; retry only if explicitly safe and due. Otherwise record blocked evidence.
9. After a task reaches verified terminal state or blocked state, advance scheduler to next eligible issue.

Planner must not invent capabilities, mark success from tool return text alone, bypass STOP, or silently broaden scope.

## 5. Capability Discovery and Specialist Registry

A single typed registry exposes capabilities with:
- stable capability ID and version;
- specialist/domain ownership;
- accepted task/argument schema;
- execution target and required permissions;
- risk class, approval policy and reversibility;
- idempotency, timeout/budget and concurrency constraints;
- evidence produced and verifier/postconditions;
- rollback handler or explicit `irreversible/no-rollback` declaration;
- availability/readiness and diagnostic reason.

Discovery returns only registered, currently available capabilities. Missing capability creates a structured capability-gap record and routes to an allowed repository/design/skill discovery workflow or owner action. Imported skills are inert reviewed knowledge, never executable capabilities by themselves.

Initial specialist domains (all managed by this one Factory):
- Complaint / Issue intake and lifecycle;
- Errors / Diagnostics / remediation;
- Repository / Code / CI / release;
- Design / UI implementation and visual verification;
- Android Control / device action;
- Memory / retrieval / conflict resolution;
- Skills / knowledge import and validation.

These are modules behind one orchestrator, not independent queues or factories. Existing domain code is reused through adapters; no claim that all specialists are implemented yet.

## 6. Approval, security, testing and rollback

Risk tiers:
- R0 read-only inspection: may run automatically within bounded scope.
- R1 reversible low-impact edits: auto-run only when policy explicitly allows, with isolated branch/worktree, tests and rollback reference.
- R2 repository writes, dependency/build changes, UI-wide changes or operations with meaningful side effects: owner approval before write/merge as policy requires; branch/PR and exact SHA CI evidence.
- R3 signing, credentials, security policy, accessibility/service authority, STOP, permission grants, production data, irreversible or device-wide actions: explicit owner approval and specialized verification; never auto-merge or silently execute.

All writes are scoped to the authorized feature branch and allowed paths. Secrets never enter issue text, logs, prompts or generated artifacts. Every mutating step has a precondition, bounded diff, test plan, postcondition and rollback/compensation plan. If rollback is impossible, require explicit approval and say so before execution.

## 7. Domain end-to-end contracts

- Complaint: issue -> classify -> specialist -> fix plan -> tests -> review/approval where required -> CI -> exact SHA/evidence -> owner device probe if required -> close only when all required probes pass.
- Errors: diagnostic signal -> correlate -> root-cause hypothesis -> reproduce -> bounded remediation -> regression test -> recovery verification.
- Repository: inspect immutable ref -> license/security/scope check -> isolated change -> tests/build -> PR -> CI -> approval/merge; never execute arbitrary imported repository code.
- Design/UI: design brief -> inspect current screen/system -> plan tokens/components -> implement in allowed scope -> screenshot/render comparison and accessibility checks -> PR/CI -> owner/device acceptance if needed.
- Android Control: capability discovery -> fresh observation -> policy/permission/foreground/STOP validation -> action -> re-observation -> outcome verification; no unsupported action fallback.
- Memory: retrieve relevant scoped facts -> provenance/confidence/freshness/conflict check -> propose update -> approval policy -> persist through existing memory store -> read-after-write verification; no silent destructive overwrite.
- Skills: discover repo -> immutable SHA/license/file checks -> preview -> owner approval -> inert import -> integrity verification -> capability mapping only after separate reviewed implementation.

## 8. Shared history and memory

The existing execution journal is the authoritative attempt history. Add typed events for classification, plan, capability selection, approval, dispatch, observations, test results, CI SHA, verification, rollback and blocked reason. Memory may index summaries and reusable decisions with provenance, but must never replace issue/journal truth. Secrets and raw private chat must be excluded. Memory writes follow the existing conflict-resolution and retention rules.

## 9. Scheduler, resilience and fairness

- Single scheduler over the canonical issue registry; one issue has at most one valid active lease per executor identity.
- Fresh snapshot before claim; lease fencing on every mutating transition.
- Stable priority/FIFO ordering, bounded concurrency, starvation prevention and per-domain budgets.
- Continue to next eligible issue after verified completion or a safely recorded block.
- STOP, lost foreground/permission, stale snapshot, auth failure or storage failure halt dispatch safely.
- Persistent worker (WorkManager/foreground service as policy permits), reboot/Doze behavior, and distributed atomic claims must be separately implemented and tested; current local foreground lease is not a distributed lock.
- Network transient retries are bounded and idempotency-gated. Unknown/unsafe failures go to blocked/manual review, not infinite loops.

## 10. Implementation sequence and acceptance gates

A. Finish Step 1 source inventory and exact-head CI diagnosis.
B. Implement schema/registry contracts and tests without changing current behavior.
C. Implement planner/classifier and capability discovery over the existing engine.
D. Implement safe repository-change adapter (isolated branch, bounded paths/diff, tests, PR, CI SHA, owner approval; no auto-merge).
E. Add specialist adapters one by one, reusing existing domain components.
F. Implement scheduler drain/fairness and durable restart behavior.
G. End-to-end acceptance: complaint -> verified fix; error -> recovery; repo task -> reviewed PR/CI; design -> implemented UI and visual evidence; unknown task -> capability-gap discovery; control -> observe/act/re-observe; memory -> provenance-safe read/write/conflict verification.
H. Decide APK only after proving whether required changes can be delivered through current remote/content architecture. If native changes are required, produce one final APK after all gates.

Every batch: tests first, implement only missing behavior, update `docs/HANDOFF.md`, commit/push to `feature/memory-intelligence`, run CI on exact HEAD, fix real log failures. Never touch Android `main`, Render, Hermes backend or production Supabase without explicit authorization.

## 11. Explicit non-goals

- No disconnected complaint factory, design factory, repository factory or control factory.
- No duplicate task queue, journal, lease, retry, approval or memory database.
- No autonomous merge/install of unreviewed or high-risk changes.
- No fabricated CI, physical-device or visual acceptance.
- No APK generation merely to show progress.
