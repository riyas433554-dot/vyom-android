# Vyom Factory — Owner Actions

Last reviewed: 2026-11-04
Controlling source: docs/HANDOFF.md (ORDER V2)

## No immediate owner action
Do not run any command for an intake source that has not shipped yet. The Termux command `vyom-complain`, voice complaint registration, automatic Diagnostics/task-failure issue creation, and Android screenshot share-sheet receiver are not yet implemented and verified.

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
