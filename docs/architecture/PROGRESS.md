# Architecture progress log

Update this table in every consolidation PR.

| Field | Current state |
|---|---|
| Current phase | Phase 2 — Messages consolidation |
| Active PR | Phase 2 PR 9 — extract stateless conversation message list, row, date/unread dividers, reply/share context, voice playback, and full-screen photo UI |
| CI status | Pending for Phase 2 PR 9; PRs #95–#103 passed on Blacksmith |
| Completed ownership changes | Phase 1 shell ownership; Phase 2 domain/repository/state owners; live conversation list and message-row rendering now sit behind stateless state/callback contracts |
| Deleted legacy/versioned files | `NovaPrimaryHost.kt`; global `NovaPrimaryNavigationDispatcher.kt` |
| Automated verification | Phase -1 through Phase 2 PR 8 passed Blacksmith CI; PR 9 row grouping/date/unread/reply/voice tests, unread-count mutation check, full JVM/release/artifact gate, instrumentation APK compile, and 13/13 Android 16 Pixel 8 emulator tests passed locally |
| Remaining physical Samsung checks | Entire manual checklist in `SAMSUNG_SMOKE_CHECKLIST.md`; authorized Samsung SM-A266B detected, but non-destructive test install is blocked by its differently signed existing Nova package |
| Exact next PR | Phase 2 PR 10 — extract composer, attachment-picker, and voice-recorder state with one documented IME inset owner |

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

## Phase 0 discovered risks

- root policy/dispatcher have JVM tests, but notification routing is private to
  `MainActivity` and has no pure parser seam;
- terminal 401/session expiry is repeated across many screens and repositories;
- conversation UI constructs repositories/realtime owners directly, preventing a
  deterministic Compose test without a narrow seam;
- outer `ConversationScreen` and V8 composer both apply IME-related padding;
- instrumented/device tests are not run by the current hosted CI; and
- no physical Samsung device is connected to this workspace.
