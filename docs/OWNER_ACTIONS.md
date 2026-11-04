# Vyom Factory — Owner Actions

Last reviewed: 2026-11-04
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

## Other intake sources still awaiting implementation
Voice complaint registration, automatic Diagnostics/task-failure issue creation, and the Android screenshot share-sheet receiver are not yet implemented/verified. Do not claim these paths work yet.

## Required when Stage 1 implementation is ready
These are deliberately deferred until the corresponding feature is present in an exact-head APK; they are not current blockers to source work.

### Android phone acceptance
1. Open Vyom → speak a complaint beginning with “complaint:” and confirm it creates one GitHub Issue with source=vyom_voice and a stable issue ID.
2. Trigger a safe Diagnostics failure and a task failure; confirm each creates one complaint with captured result/evidence and no duplicate on retry.
3. From Gallery or another app, Share → Vyom with one screenshot; confirm the image is attached to the complaint and private/secure surfaces are not silently captured.
4. Open Factory and Automations; confirm owner can move a complaint to the top, say “ye complaint pehle”, and see the same order/status in both screens.
5. Verify the complaint closes only after its named probe passes on the device; force a failing probe and confirm the issue remains/reopens with evidence.

### Termux acceptance — only after command is added to the repository
Run the exact command and arguments documented by the shipped Termux installer/help output. Do not guess a command path or manually construct a GitHub API call. Confirm one issue is created and a retry with the same source identity returns the same issue number.

### External-agent acceptance
From GPT, Claude and Codex, create a complaint through each agent's connected GitHub integration using source_id unique to that agent/request. Confirm each appears in Vyom Factory with the same canonical GitHub issue number and all required complaint fields.

## Credentials / settings
- Never paste GitHub tokens, signing passwords, keystore contents or private keys into chat, issue bodies, screenshots or logs.
- No new token or secret is requested at this stage.
- Do not change Android main, Render, Hermes backend or production Supabase.

## OWNER ACTION NEEDED — choose safe screenshot evidence storage

The source repository is public. Screenshots from the Android share sheet can contain notifications, names, messages, account details or other private data. I will not upload them into this public repository or embed them in public issue bodies.

Before implementing screenshot-to-issue attachment, choose one:
1. Provide the name of a private GitHub evidence repository and authorize the app/CLI token to create/read issue evidence there; or
2. Explicitly approve public screenshot uploads only after the owner manually redacts each image.

Until this is decided, share-sheet screenshots must not be uploaded remotely. Local-only paths are not considered shared evidence because GPT/Claude/Codex agents cannot access them.

## OWNER ACTION NEEDED — confirm complaint register visibility

The repository is public, so GitHub Issues, complaint titles and issue bodies are publicly readable. The Android intake now redacts content detected by the existing sensitive-memory policy and withholds chat text/exception details from automatic task-failure reports, but this is not a complete PII detector.

Choose one before sending real user complaints:
1. Keep the repository public and confirm that only non-sensitive, manually sanitized complaint details may be registered; or
2. Change repository visibility to private in GitHub → Repository Settings → General → Danger Zone → Change repository visibility, then verify GitHub Actions and the app's issue token still have access.

Do not paste credentials, OTPs, private messages or personal screenshots into public issue bodies. The seeded matrix issues are generic requirement records and contain no device evidence.
