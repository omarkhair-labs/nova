# Architecture progress log

Update this table in every consolidation PR.

| Field | Current state |
|---|---|
| Current phase | Phase 2 — Messages consolidation |
| Active PR | Phase 2 PR 11 — move conversation search/context/media/mute data ownership behind a stable feature-owned repository boundary |
| CI status | Pending for Phase 2 PR 11; PRs #95–#105 passed on Blacksmith |
| Completed ownership changes | Phase 1 shell ownership; Phase 2 domain/repository/state owners; live list/row rendering and composer platform state now sit behind focused state/callback contracts; PR 11 moves conversation-tools HTTP/auth/parsing implementation into `feature/messages/details` behind `ConversationToolsRepository` |
| Deleted legacy/versioned files | `NovaPrimaryHost.kt`; global `NovaPrimaryNavigationDispatcher.kt`; implementation body of `NovaMessagingV9ToolsRepository.kt` is replaced by a temporary compatibility alias file while the live V9 consumer is switched in the next slice |
| Automated verification | Phase -1 through Phase 2 PR 10 passed Blacksmith CI; PR 10 also passed the full JVM/release/artifact gate and 13/13 Android 16 Pixel 8 emulator tests. PR 11 adds pure JVM characterization for media-type normalization, reply-preview priority, and media-URL resolution; hosted CI remains the execution gate for the full repository in this workspace |
| Remaining physical Samsung checks | Entire manual checklist in `SAMSUNG_SMOKE_CHECKLIST.md`; authorized Samsung SM-A266B detected previously, but non-destructive test install is blocked by its differently signed existing Nova package |
| Exact next PR | Phase 2 PR 12 — switch the live conversation-details consumer to the `AppContainer`-owned `ConversationToolsRepository` and remove the temporary V9 compatibility aliases without changing UI/state behavior |

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

## Phase 0 discovered risks

- root policy/dispatcher have JVM tests, but notification routing is private to
  `MainActivity` and has no pure parser seam;
- terminal 401/session expiry is repeated across many screens and repositories;
- conversation UI still contains direct data-owner construction in the V9 details path; Phase 2 PR 11 moves the implementation behind a stable feature boundary and PR 12 switches that live consumer;
- outer `ConversationScreen` and V8 composer both applied IME-related padding (resolved by Phase 2 PR 10: composer is the sole owner);
- instrumented/device tests are not run by the current hosted CI; and
- the physical Samsung smoke matrix remains incomplete because the available device has a differently signed installed Nova package.
