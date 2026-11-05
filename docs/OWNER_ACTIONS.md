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

## OWNER ACTION NEEDED — make the complaint/evidence repository private

**OWNER ACTION NEEDED**

The current repository is public. The requested screenshot workflow must use a private repository before any remote image attachment is enabled.

1. Open https://github.com/riyas433554-dot/vyom-android/settings.
2. Open **General** → scroll to **Danger Zone** → **Change repository visibility**.
3. Select **Make private** and confirm the repository name when GitHub asks.
4. Verify the repository header shows **Private**.
5. Open **Settings → Actions → General** and confirm GitHub Actions are permitted for this repository.
6. In GitHub → Settings → Developer settings → Personal access tokens, confirm the app token still has access to this private repository with Issues read/write and Contents read/write permissions (Contents write is required only when the owner taps Attach). Do not paste the token into chat.
7. Run the Termux smoke test below and confirm issue creation still works.

**Screenshot handling contract (source-wired; device acceptance pending):**
- Android Share → Vyom accepts one PNG/JPEG/WebP image up to 10 MiB and copies it to app-private `filesDir/factory-evidence`; the Factory panel shows a sampled preview.
- No upload occurs during share, intake, preview, cancellation, refresh, retry or background processing.
- The owner selects an exact active issue and explicitly taps **Attach** in a confirmation dialog. The app checks repository privacy before the issue lookup and again immediately before the Contents API write; a public repository is rejected before upload.
- After explicit Attach, the image is written under `.factory-evidence/issue-<n>/` on `feature/memory-intelligence` and linked in the exact issue comment with SHA-256 and commit SHA. No `main` branch, Render, backend or production Supabase is touched.
- The local file is deleted only after the issue comment is confirmed. Upload/comment failure keeps the local file; if the image upload succeeded but comment creation failed, the UI shows the link and disables duplicate upload. No silent retry.
- This uses GitHub Contents write permission and stores binary evidence in the private repo branch. Do not enable real uploads until the owner has made the repository private. Device verification remains mandatory.

## OWNER ACTION NEEDED — protect issue visibility before real complaints

**OWNER ACTION NEEDED**

The canonical issue register contains complaint titles and descriptions. Complete the private-repository steps above before registering real device/user details. Until GitHub confirms the repository is private, use only generic, non-sensitive test complaints. Never paste credentials, OTPs, private messages or personal screenshots into issue bodies.

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

