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
- Generic chat task failures do not auto-create Factory complaints. Only Factory runner infrastructure failures use the dedicated task-failure intake path; private request text and exception details remain withheld.
- Termux intake command exists and has shell/schema CI coverage; owner must still install/authenticate it and run the documented idempotency smoke test.
- These source paths are not yet accepted on a physical phone. Skill repository import is source-wired but needs the owner review/install/Diagnostics smoke test. Screenshot share-sheet receiver and explicit Attach UI are not implemented/device-verified; no upload path may be enabled until the repository is private and the owner taps Attach for a specific issue.


## OWNER ACTION NEEDED — Stage 1 owner acceptance
These are deliberately deferred until the corresponding feature is present in an exact-head APK; they are not current blockers to source work.

### OWNER ACTION NEEDED — Android phone acceptance
1. Open Vyom → speak a complaint beginning with “complaint:” and confirm it creates one GitHub Issue with source=vyom_voice and a stable issue ID.
2. Trigger a safe Diagnostics failure and a **Factory runner infrastructure failure**; confirm each records the expected sanitized result/evidence and no duplicate on retry. Generic Vyom chat task failures are intentionally not auto-registered as Factory complaints.
3. Screenshot acceptance is pending implementation: once the share receiver and Attach UI ship, Share → Vyom must keep the image in app-private storage, show the preview and target issue, and perform no upload until the owner explicitly taps **Attach**. Verify that only the selected image is attached to that exact issue; no private/secure surfaces are silently captured.
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
6. In GitHub → Settings → Developer settings → Personal access tokens, confirm the app/CLI token still has access to this private repository with Issues read/write and Contents read access as required. Do not paste the token into chat.
7. Run the Termux smoke test below and confirm issue creation still works.

**Screenshot handling contract (implementation requirement, not yet implemented/device-verified):**
- Capture or receive the screenshot into app-private internal storage (filesDir / app-private cache), never public shared storage by default.
- Do not upload during capture, intake, retry, reconnect or background processing.
- Show the image preview and target complaint. Upload/attach only after the owner explicitly taps **Attach**.
- Attach to that exact GitHub issue; preserve a local copy until the upload result is confirmed. On failure, keep the local file and show a retry option; never silently retry-upload.
- Do not attach screenshots to a public repository or issue. Do not include screenshot bytes/base64 in issue text.
- The share-sheet receiver and explicit Attach UI are still pending implementation and must be verified in the APK before this is marked complete. The owner must make the repository private before remote attachment can be enabled.

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

