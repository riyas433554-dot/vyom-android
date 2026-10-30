# R1b — Android reply recovery API contract (SPEC ONLY)

Status: design contract only. No Hermes/backend code, Render service, production database, or migration is authorized or changed by this document.

## Goals
- Recover the original reply for a chat message after app/process/network interruption.
- Make retries safe: one logical message must not create multiple assistant generations.
- Keep old Android clients and requests without an ID working unchanged.
- Keep chat reply storage separate from task execution storage and existing production tables.

## POST /api/android/chat
Request JSON:
- `message`: required non-empty string (existing field).
- `client_message_id`: optional UUID string, generated once by Android and persisted with the local message. The same ID must be reused for every retry of that logical message.

Compatibility:
- Requests without `client_message_id` retain current behavior.
- The server must not trust client IDs as authorization. Scope every idempotency key to the authenticated account/user.
- Same authenticated user + same client_message_id + same normalized request body returns/continues the original logical message; it must never start a second model generation.
- Reuse of the same ID with a materially different message body returns HTTP 409.

## Reply persistence
- Store chat request/reply lifecycle in a dedicated reply store, separate from task/job tables and unrelated production data.
- Required fields: authenticated user ID, client_message_id, request hash, status, created_at, updated_at, reply (nullable), error_code (nullable), completed_at (nullable), expires_at.
- Unique constraint: (user_id, client_message_id).
- Status values: queued, running, done, error.
- Persist status transitions and terminal reply/error before acknowledging completion to the client.
- Retention: minimum 7 days after terminal state; pending/running records must not be expired while active. A later implementation must define cleanup and maximum payload sizes.
- Never store access tokens, pairing codes, OTPs, passwords, payment data, or raw secrets in reply rows or logs.

## GET /api/android/reply/{id}
- Requires the same Bearer authentication as POST /api/android/chat.
- `id` is the UUID client_message_id, not a database primary key.
- Enforce ownership: a user cannot read another user's reply. Return 404 for missing and non-owned IDs (do not reveal which case occurred).
- 200 response JSON:
  - `message_id`: client_message_id
  - `status`: queued | running | done | error
  - `reply`: string only when done (otherwise omitted/null)
  - `error`: safe user-facing error only when error (never stack traces or secrets)
  - `updated_at`: ISO-8601 timestamp
- 401 for expired/invalid bearer; 404 for absent/unavailable ID; 429 for rate limit.
- GET is read-only and must never start/retry model generation.

## Idempotency and concurrency
- Atomically claim (user_id, client_message_id) before model generation.
- Concurrent duplicate POSTs must converge on the same record and same reply.
- The request hash must bind the ID to the original message; changed body => 409.
- Worker crash recovery may resume a claimed request only when lease/heartbeat policy proves the prior worker is no longer active. Do not start a second generation while the original worker may still be running.
- Retryable failures must preserve the same ID and state; terminal errors remain queryable until retention expiry.

## Android feature-flag behavior
- Android may attempt GET only when `ENABLE_REPLY_RECOVERY` is true.
- 200 queued/running: keep the local message queued/running and poll with bounded backoff; never send another POST while the server reports active.
- 200 done: display/persist the recovered reply.
- 200 error: display/persist the safe terminal error.
- 404/405 or unsupported response: mark recovery unavailable for this session and use current local queued retry behavior. UI must say recovery is PARTIAL; do not claim exactly-once delivery.
- 401: follow existing session-expired pairing UI; do not call a session-validation route.
- Network/5xx: retain the message and retry GET with bounded backoff. Never discard local text or ID.
- The current Hermes backend has not been changed to implement this contract. Until backend approval and implementation, endpoint availability is unverified and Android must remain in the partial fallback path.

## Rollout and tests required before backend approval
1. Create an isolated backend staging branch and disposable test store only after explicit approval.
2. Test duplicate POST (sequential and concurrent), same ID/different body 409, GET owner isolation, auth expiry, queued/running/done/error transitions, worker crash/retry, retention, rate limits, and old-client compatibility.
3. Verify no writes to task tables or production data.
4. Deploy only after separate explicit approval; this document does not authorize staging or production deployment.
