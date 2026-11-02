# Vyom Design Map — Supplied HTML Boards to Android Screens

## Source of truth

Use the user-supplied Library artifact **Vyom — Design System, Home, Drawer, Automations (1)(1).html** (31 embedded boards), together with **Vyom — Complete Feature & Design Brief.pdf**. The 19-page Weakness Audit supplies acceptance/safety requirements. This map is additive: it does not replace the supplied designs or the audit.

The HTML board order and names below are preserved. Android remains native Kotlin; do not copy the HTML into a WebView as a substitute for implementing the native screens.

## Design tokens to preserve

| Token | Reference |
|---|---|
| Background | `#070B14` |
| Glass surface | `#0F1A2E` at 60% opacity |
| Glass border | `#22D3EE` at 25% opacity, 1 px |
| Cyan | `#22D3EE` |
| Violet | `#8B5CF6` |
| Amber | `#FBBF24` |
| Green | `#34D399` |
| Rose / danger | `#F43F5E` |
| Primary text | `#E6F1FF` |
| Secondary text | `#8FA3BF` |
| Typography | Space Grotesk 600 for display; body 15 sp; JetBrains Mono for status labels |

Current source already has the exact base palette in `VyomDesignSpec` and `VyomTheme`. Remaining visual mismatch: `VyomTheme.SURFACE_GLASS` is currently 90% alpha (`0xE6`) and `SURFACE_GLASS_SOFT` is 72% (`0xB8`), while the supplied glass reference is 60%; typography also still uses Android sans-serif in several screens rather than Space Grotesk. Do not mark the design system complete until these are reconciled and screenshot-tested.

## 31-board implementation map

| # | Supplied board | Current Android source | Gap / acceptance |
|---:|---|---|---|
| 1 | Design system | `VyomTheme`, `VyomDesignSpec` | Palette exists; glass alpha, font family, component sizing and full-app theme propagation need visual verification. |
| 2 | Home | `MainActivity.buildHomePanel()`, `showHome()`, `OrbSurfaceView` | Base home/orb exists; compare header, quick actions, task orbit cards, status strip and spacing to board. |
| 3 | Drawer (3-line menu) | `VyomDrawer`, `VyomDesignSpec.drawerItems` | Drawer exists. Unimplemented entries are now hidden instead of showing SOON; verify sections/search/scroll and one-tap STOP. |
| 4 | Automations panel | `VyomAutomationsDialog` | Current dialog shows task queue only; convert to a proper screen and add workflow canvas only when its real executor exists. |
| 5 | Orb — standby (hero) | `OrbSurfaceView`, `OrbRenderer` | Core renderer exists; match hero scale, transparency, glow and no-black-rectangle acceptance. |
| 6 | Orb states | `VyomDesignSpec.OrbState`, `orbMode()`, `OrbSurfaceView` | State mapping exists; verify each animation, accent, reduced-motion and Lite Mode state. |
| 7 | Chat | `MainActivity.renderChatMessages()`, chat composer/layout | Chat exists; supplied + / attachment / lane / mic / dynamic Send-Live composer and long-message behavior remain. |
| 8 | Live — Listening | `MainActivity.showVoiceMode()`, `runLiveLane()` | Live screen exists; verify state, lane badge, mic level, captions and sentence streaming. |
| 9 | Live — Muted | `showVoiceMode()`, mute/unmute handlers | Muted state exists; verify exact visuals, notification action and custom sound behavior. |
| 10 | Floating orb | `VyomAccessibilityService` agent overlay is not the same as the designed orb | Dedicated draggable orb, tap/long-press menu, mute and STOP overlay remains unimplemented/unverified. |
| 11 | Floating status panel | No dedicated persistent task-status overlay identified | Add service-hosted status panel with pending/working/done items and current step; lifecycle and permission required. |
| 12 | Notification windows | `VoiceForegroundService`, Activity dialogs/notifications | Voice notification exists; task-complete/error/permission notification designs and deep links remain. |
| 13 | Tasks | `MainActivity.createTask()`, `renderTasks()` | Task list/progress/result cards exist; compare clipping, status pills, queue controls and live updates. |
| 14 | Task detail | `VyomTaskDetailDialog` | Detail dialog exists; convert to full-screen detail with steps, logs, retry/cancel and artifact viewer. |
| 15 | Animations — VFX | No production Creative Studio screen yet | Build only after the creative pipeline adapters are real; do not show a dead drawer item. |
| 16 | New automation | `VyomAutomationsDialog` currently explains schedules are not connected | Scheduler-backed editor with trigger/repeat/action/save is pending; never claim a schedule was created until persisted. |
| 17 | Skills | `showSkillsRegistry()`, `VyomSkillsDialog` | Registry dialog exists; full-screen searchable skills registry, install/enable/disable and source/licence evidence remain. |
| 18 | Teach Vyom | `showTeachCommandDialog()`, `showTeachLibrary()`, `VyomTeachDialog` | Phrase mapping exists; parameterised multi-step procedure, preconditions, dry-run and expected-result checks remain. |
| 19 | Assistant control | `showTab(...showControl=true)`, `showControlReadiness()`, `showUiAgentPanel()` | Control and readiness exist; compare full permission checklist, persistent task lifecycle and overlay to design. |
| 20 | Audit log | `showAuditLog()`, `VyomAuditLogDialog`, `renderControlAudit()` | Local log exists; full-screen filters, search, event detail and export remain. |
| 21 | Setup — permissions | `showControlReadiness()`, `showAppAccessPanel()` | Readiness/app access exists; a single guided screen for Accessibility, overlay, visibility, notification, mic, exact alarm, installer and battery state remains. |
| 22 | Memory | `showMemoryManager()`, `VyomMemoryDialog`, memory store | Memory exists; full-screen tier filters, counts, search, pin/edit/delete and evidence remain. |
| 23 | Instructions | `showInstructions()`, `VyomInstructionsDialog` | Persona/rules dialog exists; full-screen role/persona/rules editor and TRUSTED authorization language remain. |
| 24 | Voice settings | Settings panel in `MainActivity` | Settings exist; separate voice settings screen, selectable local voices, provider fallback state and controls remain. |
| 25 | Voice picker | No dedicated picker identified | Meera/Kabir/Aarav/Ira/Veer selection, preview, persistence and Android TTS integration remain. |
| 26 | Connections | Settings panel stores provider credentials | Dedicated Connections screen with truthful connection status, masked credentials and per-provider test remains. |
| 27 | Model picker | `loadNvidiaModels()` and LIVE model settings | Model loading exists; searchable lane-aware picker, unavailable model state and verified fallback remain. |
| 28 | System | `showVoiceDiagnostics()` | Current diagnostics are an AlertDialog and some values are static/unverified; replace with measured status/latency screen. |
| 29 | Self-update | `checkForUpdate()`, settings panel, `SelfModificationClient` | Update/PR foundation exists; exact signed-artifact/certificate continuity, staged update and rollback health checks remain. |
| 30 | Icons — orb studio | `setOrbAppearance()`, orb renderer | Four color modes exist; sphere/ring/waveform/core, size, density, glow and app icon remain. |

## Navigation invariants

- Bottom navigation is exactly Home · Chat · centre Live orb · Tasks · Profile.
- Profile opens a real Profile/Settings page. The three-line menu opens the Drawer. They must never open the same UI.
- Every implemented Drawer destination opens its own titled page with back navigation; unfinished destinations stay hidden, not disabled with a SOON badge or Toast.
- Automations, Skills, Memory, Instructions, Connections, System and Audit must not all collapse into one generic settings panel or AlertDialog.
- The STOP CONTROL affordance remains one tap away on control-related screens and continues to work when the main Activity is not foregrounded.

## Current source reconciliation (2026-11-02)

- Fixed the Profile/Drawer collision: bottom Profile now opens the settings panel; the three-line menu remains the Drawer.
- Hid unimplemented Drawer entries rather than rendering SOON badges; the dead “not connected in this build” Toast route was removed.
- Added tests for filtering disabled Drawer entries. Exact-head CI and device screenshots are still pending.
- The 31-board mapping is source inspection, not screenshot acceptance. Add one screenshot/instrumentation test per implemented screen and attach exact-commit artifacts before marking a board FIXED.
