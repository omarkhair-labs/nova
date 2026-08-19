# Architecture progress log

Update this table in every consolidation PR.

| Field | Current state |
|---|---|
| Current phase | Phase 3 — Calls isolation |
| Active PR | Phase 3 PR 1 — move call domain models and REST/ICE auth access behind stable feature-owned contracts while preserving the existing controller/signaling/WebRTC/Telecom runtime |
| CI status | Pending for Phase 3 PR 1; Phase 2 closing PR #119 merged after the expanded hosted Messages gate passed |
| Completed ownership changes | Phase 2 Messages is complete. Phase 3 PR 1 introduces stable `feature/calls/domain/model` records plus `feature/calls/data/CallRepository`, with `AppContainer` owning the production repository while existing call runtime consumers remain compatibility-safe |
| Deleted legacy/versioned files | Phase 2 removed the historical Messages V8/V9 layers and temporary aliases. Calls compatibility aliases are temporary only during Phase 3 and must be removed before phase close |
| Automated verification | Hosted CI now includes Messages architecture enforcement, `git diff --check`, Android JVM/lint/debug/instrumentation-APK/release/AAB builds, plus Django configuration/migration/full backend tests. Phase 3 PR 1 adds call wire/terminal/display-name characterization |
| Remaining physical Samsung checks | Manual Samsung smoke remains incomplete: the authorized SM-A266B has a differently signed installed Nova package, so non-destructive replacement remains blocked. No uninstall is authorized |
| Exact next PR | Phase 3 PR 2 — place call signaling and WebRTC behind stable feature-owned interfaces/factories without changing negotiation IDs, ICE restart/replay, TURN, recovery, or media-quality algorithms |

## Completed PRs

| Phase | PR | Purpose | Evidence | Rollback |
|---|---:|---|---|---|
| -1 | #95 | replace all three Linux `ubuntu-latest` jobs with `blacksmith-2vcpu-ubuntu-2404` | actionlint; Android and backend CI passed on named Blacksmith runners; no Play release dispatched or path-triggered | revert merge commit `dcbd534` |
| 0 | #96 | record the governing plan, ownership/routes, Samsung checklist, and baseline entry contracts | Android/backend Blacksmith CI; instrumentation APK compilation; exact 72 REST/3 WebSocket inventory check | revert merge commit `0453ddb` |
| 0 | #97 | characterize push/session/Back decisions and conversation chrome | Android/backend Blacksmith CI; 13/13 Android 16 device tests; mutation-sensitive routing test | revert merge commit `dbcf56b` |
| 1 | #98 | introduce typed `AppDestination`/`AppNavigator` root contract | Android/backend Blacksmith CI; full JVM suite; inactive/active/fallback dispatcher characterization | revert merge commit `d62f3e3` |
| 1 | #99 | consolidate `AppContainer`/`AppViewModel` shell ownership and shared Messages/Reels routes | Android/backend Blacksmith CI; full local release gate; 13/13 Android 16 emulator tests | revert merge commit `3774fbf` |
| 2 | #100 | extract nine live Messages domain models from repository implementation | Android/backend Blacksmith CI; mutation-sensitive computed-behavior tests; full local release gate | revert merge commit `6f4748a` |
| 2 | #101 | establish feature-owned `MessagesRepository`/`InboxRepository` contracts and deterministic fakes | Android/backend Blacksmith CI; fake contract tests; full local release gate | revert merge commit `4908446` |
| 2 | #102 | move inbox search, paging, unread, and session-effect orchestration into `InboxViewModel`/`InboxUiState` | Android/backend Blacksmith CI; stale-response mutation check; full local release gate | revert merge commit `77ae51c` |
| 2 | #103 | move conversation repository, draft, mutation, unread/read, typing/presence, and realtime orchestration into `ConversationViewModel` | Android/backend Blacksmith CI; paging-deduplication mutation check; full local release gate; 13/13 Android 16 emulator tests | revert merge commit `ba2385c` |
| 2 | #104 | extract stateless message list/rows, date/unread grouping, reply/share/voice rendering, and full-screen photo UI | Android/backend Blacksmith CI; unread-count mutation check; full local release gate; 13/13 Android 16 emulator tests | revert merge commit `573fef8` |
| 2 | #105 | extract conversation composer UI, recorder/platform state, attachment drafts, and sole IME/navigation-bar inset ownership | Android/backend Blacksmith CI; full JVM/release/artifact gate; exact-five-minute mutation check; 13/13 Android 16 Pixel 8 emulator tests | revert merge commit `7a3c5ee` |
| 2 | #106 | establish stable feature-owned conversation search/context/media/mute repository models, contract, implementation, and AppContainer construction | Blacksmith backend and Android jobs green; full Django tests and migration check; JVM/lint/debug/release/AAB green | revert merge commit `92d9f38` |
| 2 | #107 | switch the live V9 details consumer to `AppContainer`-owned stable repository/model types and remove temporary V9 aliases | Blacksmith backend and Android jobs green; no behavior or contract changes | revert merge commit `e73787e` |
| 2 | #108 | move details tab/query/search/media/context/mute async orchestration and terminal-401 effects into `ConversationDetailsViewModel`/`ConversationDetailsUiState` | Blacksmith backend and Android jobs green; JVM characterization covers debounce/query cap/context/mute and exact legacy media-key behavior | revert merge commit `53347f9` |
| 2 | #109 | move details/search/media/context visual tree, MediaPlayer lifecycle, and full-photo UI into stable `ConversationDetailsDialog` and reduce V9 to a small wrapper | Blacksmith backend and Android jobs green; JVM/lint/debug/release/AAB green; no contract changes | revert merge commit `35799ac` |
| 2 | #110 | establish stable conversation appearance model/repository/remote implementation and AppContainer ownership while preserving local and legacy-backend theme compatibility | Blacksmith backend and Android jobs green; full Django tests/migration check and Android JVM/lint/debug/release/AAB green | revert merge commit `9faa2c9` |
| 2 | #111 | move theme load/save/optimistic rollback/picker state and terminal-401 effects into conversation-scoped `ConversationAppearanceViewModel`, switch live AppContainer ownership, and remove preference aliases | Blacksmith backend and Android jobs green; appearance JVM tests plus lint/debug/release/AAB green | revert merge commit `e95d50f` |
| 2 | #112 | move `GroupMember`, `GroupDetail`, and `ManagedGroupDetail` out of core repositories into stable `feature/messages/group/model` ownership with temporary aliases | Blacksmith backend and Android jobs green; repository diffs only removed passive declarations; JVM/lint/debug/release/AAB green | revert merge commit `088749b` |
| 2 | #113 | move managed-group detail/rename/avatar/remove-avatar/role HTTP/auth/media transport behind stable `GroupManagementRepository` ownership and `AppContainer` construction | Blacksmith backend and Android jobs green; full Django tests/migration check and Android JVM/lint/debug/release/AAB green | revert merge commit `20b909d` |
| 2 | #114 | move group create/detail/add/remove/leave/delete HTTP/auth/parsing transport behind stable `GroupMembershipRepository` ownership and `AppContainer` construction | Blacksmith backend and Android jobs green; full Django tests/migration check and Android JVM/lint/debug/release/AAB green | revert merge commit `df00717` |
| 2 | #115 | move `GroupInfoDialog` managed-detail loading and rename/avatar/role/remove/leave/delete orchestration into dialog-scoped `GroupInfoViewModel`/`GroupInfoUiState` | Blacksmith backend and Android jobs green; new JVM characterization plus lint/debug/release/AAB green | revert merge commit `1adaa04` |
| 2 | #116 | move add-members/new-group people search, selection, submission and terminal effects into stable group state owners; add AppContainer-owned group people lookup; delete historical group repository aliases | Blacksmith backend and Android jobs green; group picker JVM characterization plus lint/debug/release/AAB green | revert merge commit `31b485b` |
| 2 | #117 | move new-message people search/open-conversation async orchestration and terminal effects into dialog-scoped `NewMessageViewModel` using AppContainer-owned dependencies | Blacksmith backend and Android jobs green; new-message JVM characterization plus lint/debug/release/AAB green | revert merge commit `a5e1670` |
| 2 | #118 | collapse `ConversationScreen -> V9 -> V8` into stable `ConversationScreen -> ConversationContent`, preserve the V9 details click layer in the stable entry, and delete both V-number screen files | first Android compile exposed a helper-name collision; corrected head passed Nova CI #354: backend + JVM/lint/debug/release/AAB green | revert merge commit `41117cb` |
| 2 | #119 | remove the final Messages compatibility alias, add stable-Messages architecture enforcement, `git diff --check`, and hosted instrumentation-APK compilation, then close Phase 2 | Nova CI #356 green: architecture/whitespace + Django + JVM/lint/debug/androidTest/release/AAB | revert merge commit `f470392` |

## Remaining cross-phase risks / follow-up

- push/root navigation policy has JVM characterization, but broader notification-routing and session-expiry centralization remain later shared-shell work;
- Phase 3 must isolate calls without changing signaling/WebRTC/TURN/Telecom contracts;
- current hosted CI compiles the instrumentation APK but still does not execute instrumented tests on a device/emulator;
- the physical Samsung smoke matrix remains incomplete because the available device has a differently signed installed Nova package; and
- non-Messages features may still contain historical naming or UI-owned repository patterns to be handled in their designated later phases.
