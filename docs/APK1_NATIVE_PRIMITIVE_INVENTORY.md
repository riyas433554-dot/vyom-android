# APK 1 native primitive inventory — 2026-11-03

APK 1 is blocked until every required native primitive is implemented and wired. This inventory deliberately distinguishes source presence from a usable primitive.

| Native primitive | In current branch source | Wired/reachable | APK 1 status | Evidence / gap |
|---|---|---|---|---|
| Accessibility service + gestures | YES | YES, partial | PARTIAL | `VyomAccessibilityService` observes tree/OCR/screenshot and runs tap/long-press/swipe/drag/global actions; pinch/multi-touch and complete generic gesture vocabulary remain missing. |
| Shizuku ShellExecutor | YES | YES, partial | PARTIAL | `VyomShizukuUserService`, `VyomShizukuShellClient`, fixed safe command policy and coordinate gestures exist. No complete general native command/action adapter yet. |
| Termux RUN_COMMAND bridge | NO | NO | MISSING | No Termux plugin service/permission bridge or command-result path. |
| Overlay + floating bubble | PARTIAL | PARTIAL | MISSING for persistent bubble | Accessibility progress/confirmation overlay exists. No persistent cross-app floating bubble service with lifecycle/permission recovery. |
| Notification listener | NO | NO | MISSING | Voice foreground notification actions are not a notification listener. No `NotificationListenerService`. |
| MediaProjection screenshots + screen recording | PARTIAL | PARTIAL | MISSING for MediaProjection/recording | Accessibility screenshot API and local OCR exist. No MediaProjection consent/session lifecycle or screen-recording pipeline. |
| Contacts, SMS and call access | NO | NO | MISSING | Manifest does not declare these permissions; no native contact/SMS/call adapters. |
| Camera, gallery and video pickers | PARTIAL | PARTIAL | MISSING for full native media capture | CAMERA permission is declared; no complete camera capture and photo/video picker workflow is evidenced. |
| File access + share receiver | PARTIAL | PARTIAL | PARTIAL | FileProvider and app document flows exist. No generic inbound Android share receiver/complete file-operation bridge. |
| Alarms + WorkManager scheduler | NO | NO | MISSING | No WorkManager dependency, alarm receiver or scheduled generic task execution. |
| Embeddings client + local vector store | NO | NO | MISSING | Memory store/policy exists but no embeddings client or durable local vector index. |
| WebView UI host + full JS bridge | PARTIAL | PARTIAL | MISSING for full app host | WebView hosts Update Center only. Bridge is limited to update check, self-change and UI prompt patch; not full chat/voice/memory/tasks/permissions/files/shell/settings bridge. |
| Local HTTP server + in-app panel | NO | NO | MISSING | No localhost server or server-management panel. |
| Signed APK update channel | YES | YES | PARTIAL | GitHub release discovery, SHA/certificate checks, install handoff and rollback APK backup exist. Installed-device signer match and phone update/read-back remain pending. |
| A18 signed no-APK content channel | YES | YES, initial end-to-end path | PARTIAL | Signed package parser, installed-app signer key verification, GitHub package download, staging/probes, active-slot pointer, UI sync, result/history/rollback UI and crash/page-load rollback hooks now wired. Exact-head CI and actual phone demo still pending. |

## APK 1 delivery gate

- Stable release signing and exact-head CI green.
- Every native primitive above either implemented and reachable or explicitly removed from the owner's required core list (do not silently omit it).
- A18 pass demo and failing-probe rollback demo, including CI artifact evidence and phone validation.
- A17 generic engine plus Control Suite, with a real owner-runnable on-device subset.
- Installed certificate fingerprint read from the owner's currently installed APK before attempting update compatibility.
- No APK may be called final while a row above is MISSING/PARTIAL. Content updates cannot supply missing Kotlin primitives, manifest permissions or Android services.

## A14 TRUSTED default + A17 app uninstall primitive — 2026-11-03

- A14 now defaults the UI Agent to `ConfirmationMode.TRUSTED`, selected in the UI and used by the policy default. Owner-requested visible actions and bounded gestures do not receive duplicate Vyom confirmation prompts. Explicit opt-in `ALWAYS_ASK` and `RISKY_ASK` modes remain available. System UI quick-setting controls are goal- and label-allowlisted; TRUSTED executes only after exact current-screen validation. STOP CONTROL remains higher priority.
- Hard blocks were narrowed to payment/UPI/wallet/banking/authenticator/OTP and security/credential surfaces. Removed the extra blanket trading/investment app blocks. Uninstall is no longer a global hard block; protected banking/wallet/authenticator apps and Vyom itself remain protected.
- Tests added/updated: `UiAgentPolicyTest.trustedIsTheDeclaredDefaultMode`, `trustedDefaultExecutesOwnerRequestedVisibleActionsWithoutExtraConfirmation`, `trustedDefaultAllowsFreshGesturesWithoutExtraConfirmation`, `trustedDefaultAllowsAllowlistedQuickSettingsButKeepsHardBlocks`, `AppAccessPolicyTest.authenticatorAndOtpAppsAreBlockedByPackageOrLabel`, `tradingAndInvestmentAppsAreNotOverBlocked`, `DeviceActionSafetyPolicyTest.trustedDefaultAllowsOwnerRequestedConfirmClassActionsWithoutSecondPrompt`.
- A17 app-management progress: added owner-requested `UNINSTALL` intent parsing, protected-app rejection, exact installed-app label/package resolution, `QUERY_ALL_PACKAGES` visibility and Android's own uninstall confirmation intent. Tests: `ActionEngineTest.uninstallIsOwnerRequestedButProtectedAppsRemainBlocked`. Force-stop and clear-data are still missing; the A17 20-row matrix remains partial.
- F4 correction: `MainActivity.sendMessage` and voice prompt dispatch already call `memoryStore.learnFrom`; capture is reachable but `MemoryCapturePolicy` currently extracts explicit remember/keep-in-mind patterns only. This is not passive/semantic automatic capture of all useful facts.
- Source checkpoint before this docs update: `ce5b2821194d40affd30ce2234a7b93d7f985603`; Debug `41645669377` IN PROGRESS, Full CI `41645669237` PENDING. Exact-head green proof is still required after this docs update.
