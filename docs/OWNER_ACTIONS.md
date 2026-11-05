# Vyom Factory — Owner Actions

Last reviewed: 2026-11-05
Controlling source: docs/HANDOFF.md (ORDER V2)

## OWNER ACTION NEEDED — enable the Termux complaint command

The command is now in the feature branch as scripts/vyom-complain. Install it in Termux and authenticate GitHub CLI once:

```bash
pkg install -y gh jq curl
gh auth login -h github.com
mkdir -p "$PREFIX/bin"
curl -fsSL https://raw.githubusercontent.com/riyas433554-dot/vyom-android/feature/memory-intelligence/scripts/vyom-factory.sh -o "$PREFIX/bin/vyom-factory.sh"
curl -fsSL https://raw.githubusercontent.com/riyas433554-dot/vyom-android/feature/memory-intelligence/scripts/vyom-complain -o "$PREFIX/bin/vyom-complain"
chmod 700 "$PREFIX/bin/vyom-factory.sh" "$PREFIX/bin/vyom-complain"
vyom-complain --help
```

Area-based priority defaults when `--priority` is omitted: control/Android control/update=P0; memory/design=P1; voice and other named areas=P2; other/unknown=P3. An explicit `--priority` still overrides the default.

Then register one test complaint (this creates a real GitHub Issue):

```bash
vyom-complain \
  --title "Factory Termux intake smoke test" \
  --area other \
  --severity low \
  --priority P3 \
  --evidence "No evidence; command smoke test only" \
  --repro "Run the vyom-complain command with the supplied arguments" \
  --expected "One canonical GitHub issue is created" \
  --probe "Read the issue back and confirm all required fields and metadata" \
  --fix-type "native change needing an APK" \
  --source-id "termux:owner-smoke-test-001"
```

Re-running the exact command with the same --source-id must return the existing issue number, not create a duplicate. Do not reuse this source ID for a different complaint.

## Intake implementation status (source-wired; device acceptance pending)
- Generic `complaint:` / `report bug:` and explicit `factory:` prefixes route complaints from every Vyom product area to the shared GitHub Issues register with stable local message identity; no Factory wording is required for a generic complaint.
- Diagnostics failures are converted into sanitized complaint records with content-specific stable IDs; queued reports retry registration after reconnect.
- Generic chat task failures do not auto-create Factory complaints. Factory runner and unexpected skill-repository infrastructure failures use the dedicated stable task-failure intake path; expected policy rejections (unknown license, missing SKILL.md, public-repository gate) do not. Private request text, credentials and exception messages remain withheld.
- Termux intake command exists and has shell/schema CI coverage; owner must still install/authenticate it and run the documented idempotency smoke test.
- These source paths are not yet accepted on a physical phone. Skill repository import and screenshot share/Attach are source-wired but need the owner review/install/Diagnostics and private-repository phone smoke tests.


## OWNER ACTION NEEDED — Stage 1 owner acceptance
These are the remaining Stage 6 acceptance gates and therefore block final APK 1 delivery. The exact-head signed CI candidate is available for manual owner acceptance; it is not APK 1 and must not be auto-installed.

### OWNER ACTION NEEDED — Android phone acceptance
1. After completing the private-repository and token-permission steps below, manually install the signed CI candidate from [the exact-head candidate release](https://github.com/riyas433554-dot/vyom-android/releases/tag/vyom-ci-efb16a2f906f3c59191c9c428121bc6dc34fe99e). Verify signed APK SHA-256 `541b4254f2cbd80f7bbf0da42d1c21966edfa5ed82bc3a3eba087e7526299b6e` and signer certificate SHA-256 `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a` before installation. This is a CI candidate for acceptance only, not final APK 1. No automatic install is permitted.
2. In the candidate app, speak a complaint beginning with “complaint:” and confirm it creates one GitHub Issue with source=vyom_voice and a stable issue ID. Trigger a safe Diagnostics failure and a **Factory runner infrastructure failure**; confirm each records the expected sanitized result/evidence and no duplicate on retry. Generic Vyom chat task failures are intentionally not auto-registered as Factory complaints.
3. From Gallery or another app, Share → Vyom with one non-sensitive PNG/JPEG/WebP screenshot (maximum 10 MiB). Confirm the preview appears in Factory and remains app-private. Cancel once and verify no upload. Then choose the exact active issue and tap **Attach**. Verify only that explicit tap uploads; the image is stored under `.factory-evidence/issue-<n>/` on `feature/memory-intelligence` and linked in that exact issue. Confirm the local file is retained on upload/comment failure and no silent retry occurs. Do not share private/secure surfaces.
4. Open Factory and Automations; confirm owner can move a complaint to the top, say “ye complaint pehle”, and see the same order/status in both screens.
5. Open a complaint, use EDIT to replace `UNASSIGNED` with one exact observable **Verification probe**, START it, then use VERIFY after checking the real phone. Record sanitized evidence with PROBE PASSED or PROBE FAILED. DONE must appear only after a recorded PASS for that exact probe; a failed probe must leave the complaint open. This is owner-reported device verification, not an automated probe.

### OWNER ACTION NEEDED — Termux acceptance
Run the exact command and arguments documented by the shipped Termux installer/help output. Do not guess a command path or manually construct a GitHub API call. Confirm one issue is created and a retry with the same source identity returns the same issue number.

### OWNER ACTION NEEDED — external-agent acceptance
From GPT, Claude and Codex, create a complaint through each agent's connected GitHub integration using source_id unique to that agent/request. Confirm each appears in Vyom Factory with the same canonical GitHub issue number and all required complaint fields.

## Credentials / settings
- Never paste GitHub tokens, signing passwords, keystore contents or private keys into chat, issue bodies, screenshots or logs.
- No new token or secret is requested at this stage.
- Do not change Android main, Render, Hermes backend or production Supabase.

## Repository privacy — VERIFIED PRIVATE (2026-11-05)

GitHub repository metadata currently reports `visibility=private`. Do not repeat the visibility-change steps. Recheck only if GitHub later reports a different visibility.

**Remaining owner action:** confirm the saved app token still has access to this private repository with Issues read/write and Contents read/write permissions (Contents write is required only when the owner taps Attach). Also confirm GitHub Actions are permitted under **Settings → Actions → General**. Do not paste the token into chat.

Then run the Termux smoke test below and confirm issue creation still works.

**Screenshot handling contract (source-wired; device acceptance pending):**
- Android Share → Vyom accepts one PNG/JPEG/WebP image up to 10 MiB and copies it to app-private `filesDir/factory-evidence`; the Factory panel shows a sampled preview.
- No upload occurs during share, intake, preview, cancellation, refresh, retry or background processing.
- The owner selects an exact active issue and explicitly taps **Attach** in a confirmation dialog. The app checks repository privacy before the issue lookup and again immediately before the Contents API write; a public repository is rejected before upload.
- After explicit Attach, the image is written under `.factory-evidence/issue-<n>/` on `feature/memory-intelligence` and linked in the exact issue comment with SHA-256 and commit SHA. No `main` branch, Render, backend or production Supabase is touched.
- The local file is deleted only after the issue comment is confirmed. Upload/comment failure keeps the local file; if the image upload succeeded but comment creation failed, the UI shows the link and disables duplicate upload. No silent retry.
- This uses GitHub Contents write permission and stores binary evidence in the private repo branch. Repository privacy is now verified; real Attach remains unaccepted until the token permission and physical-phone tests pass.

## Issue visibility — PRIVATE REPOSITORY VERIFIED

The canonical issue register contains complaint titles and descriptions. GitHub currently reports this repository as private. Continue to keep issue bodies sanitized: never paste credentials, OTPs, private messages or personal screenshots into issue bodies.

## OWNER ACTION NEEDED — use the Factory complaint-to-code proposal flow (after the matching APK passes CI)

This is a guarded proposal workflow, not autonomous repair. It does not edit Android `main`, merge a PR, or install an APK.

1. In Vyom Settings, enter a GitHub token with repository Contents read/write, Pull requests read/write, and Issues read/write permission. Tap **SAVE GITHUB TOKEN**.
2. Enter the NVIDIA API key and select a supported code model; tap **SAVE VOICE SETTINGS**. Do not send either secret in chat or GitHub issues.
3. Open Vyom Factory and select an active complaint. Tap **PROPOSE REVIEWED CODE FIX**.
4. Choose the exact allowed Kotlin source path that should be changed. The app intentionally does not guess a file. Protected files (MainActivity, control/action dispatch, policy, STOP, audit, accessibility, signing and update-security files) are rejected.
5. Review the generated minimal patch diff before creating the PR. Cancel if the diff is unrelated, broader than needed, or changes security-sensitive behavior.
6. Wait for both required CI workflows. The PR URL and exact passing SHA should be recorded back on the originating complaint. A passing PR is still unmerged and is not an installable release.
7. Owner must review/merge the PR through GitHub. Only after a separate signed release is produced should the normal update verifier be used; installation remains a separate explicit owner action.

Limitations: one Kotlin file per proposal; user chooses the source path; no automatic worker selection of files, no autonomous merge, no automatic install, and no physical-device verification. If issue-comment write fails, record the PR URL on the complaint manually.



## OWNER ACTION NEEDED — verify private GitHub release update fix and #101

No new token is requested. Use the GitHub token already saved in Vyom Settings (github_proposal_token). The app now reads it from encrypted on-device storage for the private release feed and release-asset API.

### Update / diagnostics 404 acceptance
1. Open Settings → tap RUN ALL CHECKS. Confirm GitHub update discovery no longer reports HTTP 404 and that the signed update manifest is read.
2. Tap CHECK FOR UPDATES. If a newer signed APK release is offered, verify the manifest SHA-256 and signer certificate checks pass. Do not install unless you explicitly choose the Android installer.
3. Tap CHECK NO-APK CONTENT UPDATES. Confirm the private release asset downloads, package signature/hash validation passes, and the presence/performance probes report PASS. Confirm the active content version changes only after successful probes. Do not report success if the release API returns 404, an asset download fails, or a probe fails.
4. Send the sanitized result text/screenshots only; never include the GitHub token.

### Issue #101 — Teach Vyom must not capture normal chat
1. Send ordinary conversational text such as: “Could you help me understand how I can enable airplane mode?” Confirm it stays in the normal chat thread and no Teach Vyom dialog opens or pre-fills the message.
2. Send another normal question mentioning Settings or opening an app. Confirm it remains chat.
3. Explicitly open Profile → Teach Library → Add Phrase. Save a harmless test phrase and verify intentional teaching still works. This confirms Teach is available only after the explicit library action.
4. Report the exact observed result. Keep issue #101 open until the chat-isolation probe and explicit Teach Library test both pass on the phone; attach evidence to #101 only after explicitly choosing Attach.

Current source candidate: a86b1a81304850cb74e0024505acd08232d16a1d. CI runs at that exact SHA: Full 42011059270, Debug 42011059260 (check current status before treating the build as accepted). No physical-device verification has been recorded yet.


Latest source candidate was advanced to 20370798db1c02e666e1c1d071f04ffe1e16b8e0 after removing the unused unauthenticated update helper path. Recheck the latest exact-head CI run IDs from GitHub Actions before installing or accepting this candidate. The #101 progress comment is GitHub comment 6655794795; the issue remains open and the physical-device probe is still required.


## Latest CI correction — 2026-11-05

The first authenticated-release implementation did not pass CI. Full run 42011969802 and Debug run 42011969831 both failed at Kotlin compilation because two GitHubReleaseAssetClient call sites passed the token lambda as a trailing lambda instead of the named `tokenProvider` argument. That source wiring has now been corrected in commits `e7a9c5f66a12fd3a675531722927b611b2018074` and `af95a2328667c8b56004366415ca52ba4a8ee0f1`. Fresh exact-head Full and Debug CI results are still pending; do not install or accept this candidate until both are green.

Phone acceptance remains required: Run All Checks, Check for Updates, Check No-APK Content Updates, then perform the #101 ordinary-chat / explicit Teach Library probe. Keep #101 open until both #101 device checks pass and evidence is recorded. Verified on device: NO.


## Latest phone-test failures — 2026-11-05

- Owner reports installed certificate and stable release identity PASS, but Check for Updates, Update diagnostics and Check No-APK Content Updates all fail HTTP 404. Content version remains 1.0.0; active slot unchanged. Owner acceptance remains FAIL.
- Before retrying, update the saved GitHub token: ensure it has access to the private `riyas433554-dot/vyom-android` repository and **Contents: Read-only** plus Metadata read. Keep Factory Issues permissions. Classic PAT requires `repo` scope. Save the token again in Vyom Settings. Do not share the token in chat. An Issues read PASS does not prove private release read access.
- Then rerun: (1) Settings → RUN ALL CHECKS; (2) CHECK FOR UPDATES; (3) CHECK NO-APK CONTENT UPDATES. Expected: release API reachable, signed manifest/asset fetched, SHA-256 and signer checked, staged content probes PASS, active version changes only after probes. Do not install any APK during this test. If still 404, capture the non-secret response message/request ID only.
- Android app actions also failed owner acceptance: apps launch, but the reported “Install Godot” phrase did not complete Play Store search/listing/install/launch. Do not mark Android control or Play Store execution PASS. Preserve the existing restriction that only a SHA-256 and signer-verified Vyom APK can update Vyom itself. Focused source reproduction and tests are required before another APK.


## Play Store phrase correction — 2026-11-05

- The long taught action text was not a valid single command: the old parser extracted the Play Store search target as `Godot From going`. The source now fails closed on ambiguous Play Store phrase targets. The supported canonical phrase is exactly **`Install Godot from Play Store`**; put multi-step instructions in the explicit UI Agent task goal, not in Teach Library's single-action mapping.
- Before retrying Play Store: ensure Vyom Accessibility is enabled, MASTER APP CONTROL is ON, Google Play Store is enabled under App access, and NVIDIA planner key is saved. Then use the supported canonical command. Vyom should open the Store, observe the actual listing, and only attempt the Install button when the listing matches Godot; Android's own confirmation may still be required. Success must be confirmed by installed package evidence.
- Generic app opening is not the same as in-app UI automation. Multi-step tasks for other apps still require the explicit UI Agent flow and its live-screen permissions; do not infer that opening an app means the task completed.


## CI result and next phone probe — 2026-11-05

- Exact candidate SHA `9900bff8bf5a7326131af62c1d61a186fa893ee4`: Full CI `42021183283` SUCCESS and Debug CI `42021183325` SUCCESS. This confirms the parser source candidate builds/tests in CI; it does not prove the phone update channel or UI Agent works.
- Owner reports the existing fine-grained GitHub token was updated. Now run, in order: Settings → RUN ALL CHECKS; CHECK FOR UPDATES; CHECK NO-APK CONTENT UPDATES. Do not install an APK during these probes. Report whether each passes or fails; if failure, include only the non-secret HTTP message/request ID. Never share the token.
- If update probes pass, separately test the exact canonical command `Install Godot from Play Store`. Preconditions: Vyom Accessibility enabled, MASTER APP CONTROL ON, Google Play Store enabled in App access, NVIDIA planner key saved. Verify the installed package and app launch; opening Play Store alone is not success.
- Keep `Verified on device: NO` until those results are supplied and independently recorded. No final APK 1 authorization from CI alone.


## Important — shared token also controls Factory writes

The current app stores one `github_proposal_token` used by both private release/update reads and Factory GitHub write workflows. Contents: Read-only can satisfy update-feed reads, but it will block repository-content writes used by explicit screenshot Attach and some reviewed proposal operations. The current app does not yet provide a verified separate read-only update token plus Factory-write token.

- First run the update probes with the newly saved token as requested above.
- Before testing Factory repository write actions, restore Contents: Read and write on this same token (while retaining required Issues and Pull requests permissions), or wait until separate credential support is implemented and verified.
- Do not claim all Factory write features work merely because update checks pass. Never paste the token into chat.
