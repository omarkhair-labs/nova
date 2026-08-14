# Architecture progress log

Update this table in every consolidation PR.

| Field | Current state |
|---|---|
| Current phase | Phase 2 — Messages consolidation |
| Active PR | Phase 2 PR 6 — establish the `MessagesRepository` contract and deterministic fake |
| CI status | Pending for Phase 2 PR 6; PRs #95–#100 passed on Blacksmith |
| Completed ownership changes | Phase 1 shell ownership; Phase 2 domain models plus a feature-owned repository contract consumed by `AppContainer` and realtime coordination |
| Deleted legacy/versioned files | `NovaPrimaryHost.kt`; global `NovaPrimaryNavigationDispatcher.kt` |
| Automated verification | Phase -1 through Phase 2 PR 5 passed Blacksmith CI; PR 6 fake contract tests, full JVM suite, `lintRelease`, debug/release APKs, release AAB, and instrumentation APK compile passed |
| Remaining physical Samsung checks | Entire checklist in `SAMSUNG_SMOKE_CHECKLIST.md`; a physical Android device is connected but USB-unauthorized and provides no Samsung evidence |
| Exact next PR | Phase 2 PR 7 — introduce `InboxViewModel`/`InboxUiState` and move inbox loading/search/unread orchestration behind `MessagesRepository` |

## Completed PRs

| Phase | PR | Purpose | Evidence | Rollback |
|---|---:|---|---|---|
| -1 | #95 | replace all three Linux `ubuntu-latest` jobs with `blacksmith-2vcpu-ubuntu-2404` | actionlint; Android and backend CI passed on named Blacksmith runners; no Play release dispatched or path-triggered | revert merge commit `dcbd534` |
| 0 | #96 | record the governing plan, ownership/routes, Samsung checklist, and baseline entry contracts | Android/backend Blacksmith CI; instrumentation APK compilation; exact 72 REST/3 WebSocket inventory check | revert merge commit `0453ddb` |
| 0 | #97 | characterize push/session/Back decisions and conversation chrome | Android/backend Blacksmith CI; 13/13 Android 16 device tests; mutation-sensitive routing test | revert merge commit `dbcf56b` |
| 1 | #98 | introduce typed `AppDestination`/`AppNavigator` root contract | Android/backend Blacksmith CI; full JVM suite; inactive/active/fallback dispatcher characterization | revert merge commit `d62f3e3` |
| 1 | #99 | consolidate `AppContainer`/`AppViewModel` shell ownership and shared Messages/Reels routes | Android/backend Blacksmith CI; full local release gate; 13/13 Android 16 emulator tests | revert merge commit `3774fbf` |
| 2 | #100 | extract nine live Messages domain models from repository implementation | Android/backend Blacksmith CI; mutation-sensitive computed-behavior tests; full local release gate | revert merge commit `6f4748a` |

## Phase 0 discovered risks

- root policy/dispatcher have JVM tests, but notification routing is private to
  `MainActivity` and has no pure parser seam;
- terminal 401/session expiry is repeated across many screens and repositories;
- conversation UI constructs repositories/realtime owners directly, preventing a
  deterministic Compose test without a narrow seam;
- outer `ConversationScreen` and V8 composer both apply IME-related padding;
- instrumented/device tests are not run by the current hosted CI; and
- no physical Samsung device is connected to this workspace.
