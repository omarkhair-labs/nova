# Architecture progress log

Update this table in every consolidation PR.

| Field | Current state |
|---|---|
| Current phase | Phase 2 — Messages consolidation |
| Active PR | Phase 2 PR 14 — move conversation details/search/media/context rendering and media-player/full-photo UI from historical V9 into stable `feature/messages/details/ConversationDetailsDialog` |
| CI status | Pending for Phase 2 PR 14; PRs #95–#108 passed on Blacksmith |
| Completed ownership changes | Phase 1 shell ownership; Phase 2 domain/repository/inbox/conversation owners; list/rows/composer ownership; #106 stable conversation-tools data ownership; #107 stable live consumer; #108 lifecycle-aware details state owner; PR 14 gives the stable details package the dialog rendering and media playback UI |
| Deleted legacy/versioned files | `NovaPrimaryHost.kt`; global `NovaPrimaryNavigationDispatcher.kt`; historical `NovaMessagingV9ToolsRepository.kt` implementation; `ConversationToolsCompatibility.kt`; PR 14 removes the V9-named details/search/media/context helper implementation from `ConversationScreenV9.kt` |
| Automated verification | #108 passed full hosted backend + Android gates including Django tests/migration check and Android JVM/lint/debug/release/AAB. PR 14 is an exact UI/platform ownership move with no modifier/string/callback changes and must pass the same hosted gate before merge |
| Remaining physical Samsung checks | Entire manual checklist in `SAMSUNG_SMOKE_CHECKLIST.md`; authorized Samsung SM-A266B detected previously, but non-destructive test install is blocked by its differently signed existing Nova package |
| Exact next PR | Phase 2 PR 15 — move conversation theme preference loading/saving and picker state out of outer `ConversationScreen` behind a stable lifecycle-aware Messages appearance owner, preserving palette and session behavior |

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
| 2 | #107 | switch the live V9 details consumer to `AppContainer`-owned stable repository/model types and remove temporary V9 aliases | Blacksmith backend and Android jobs green; no comments/review blockers; no behavior or contract changes | revert merge commit `e73787e` |
| 2 | #108 | move details tab/query/search/media/context/mute async orchestration and terminal-401 effects into `ConversationDetailsViewModel`/`ConversationDetailsUiState` | Blacksmith backend and Android jobs green; JVM characterization covers debounce/query cap/context/mute and exact legacy media-key behavior | revert merge commit `53347f9` |

## Phase 0 discovered risks

- root policy/dispatcher have JVM tests, but notification routing is private to
  `MainActivity` and has no pure parser seam;
- terminal 401/session expiry is repeated across many screens and repositories;
- after PR 14, historical V9 remains only as a wrapper/chrome layer over V8; theme/group/chrome ownership still blocks the stable direct screen switch;
- outer `ConversationScreen` and V8 composer both applied IME-related padding (resolved by #105: composer is the sole owner);
- instrumented/device tests are not run by the current hosted CI; and
- the physical Samsung smoke matrix remains incomplete because the available device has a differently signed installed Nova package.
