# Vyom Factory shared task protocol

Status: M0 implementation in progress. This protocol defines the shared task identity used by the Android Factory screen, Vyom chat, ChatGPT's connected GitHub integration and the Termux CLI.

## Canonical registry

Repository: `riyas433554-dot/vyom-android`

A Factory task is a GitHub Issue whose title begins with `[VYOM FACTORY][P0|P1|P2|P3]` and whose body ends with one metadata marker:

```html
<!-- VYOM_FACTORY_META:{"schema":1,"priority":"P2","status":"open","source":"chatgpt","source_id":"replace-with-stable-id","idempotency_key":"chatgpt:replace-with-stable-id","events":["created"]} -->
```

Allowed sources: `factory`, `vyom_chat`, `chatgpt`, `termux`.
Allowed statuses: `open`, `in_progress`, `blocked`, `paused`, `completed`, `cancelled`.
Allowed priorities: `P0`, `P1`, `P2`, `P3`.

The issue number is the canonical task ID. Always edit/comment on the existing issue when a task already exists. Do not create a second issue just to change priority, status or description. Keep the metadata marker intact when editing the issue body. Append audit events as GitHub issue comments.

## Vyom Android

- Open Workspace → Vyom Factory to list and manage issues.
- The screen reads the repository live and refreshes while open.
- Use the form to register a complaint; task IDs are GitHub issue numbers.
- In normal chat, prefix a message with `complaint:`, `factory:`, `factory complaint:`, `register complaint:`, `report bug:` or `issue:` to register it directly in the shared registry.
- A chat complaint uses the persisted chat message UUID as its idempotency key so retries target the same issue.

## ChatGPT

When the owner explicitly asks to register a complaint/task from ChatGPT, use the connected GitHub integration to create one issue with:
- title: `[VYOM FACTORY][P2] concise title`
- body: the complaint details plus the metadata marker with `source=chatgpt`, a unique stable source ID and matching idempotency key.
- Add later clarifications as comments or update the same issue body; preserve the marker and issue number.

ChatGPT's connected GitHub action is an assistant-invoked operation. It does not silently monitor every conversation or create issues for ordinary messages without an explicit complaint/task request.

## Task state rules

Allowed transitions:
- open → blocked / paused / cancelled (manual controls cannot claim a worker run)
- in_progress → open / blocked / paused / completed / cancelled. Only a verified worker claim may set in_progress; UI/Termux/ChatGPT manual state edits must not do so, including RESUME from paused or blocked. Manual recovery from paused/blocked returns to open and must be claimed by a worker again.
- blocked → open / in_progress / cancelled
- paused → open / in_progress / cancelled
- completed and cancelled are terminal.

Changing priority is independent of state. Completing or cancelling closes the GitHub issue; reopening a non-terminal task keeps the issue open. Never report a change as saved until GitHub confirms the write.

## Evidence and limitations

- GitHub Issues is the shared record, not a live worker by itself.
- A registered issue does not mean code execution has started. The Factory UI must distinguish queued, running, blocked and completed only when an actual runner/event proves that state.
- Current implementation provides issue intake, listing, edit, priority/status controls and comment history. Parallel workers, skill installation, runner heartbeat/restart, automatic CI orchestration and 24/7 execution are still unfinished M0 work.
- The Android token is read from existing encrypted device storage. It must have repository Issues read/write access; the app never displays or logs its value.
- Do not store a PAT, GitHub App private key or signing secret in source, APK or issue bodies.
