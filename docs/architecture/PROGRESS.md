# Architecture progress log

Update this table in every consolidation PR.

| Field | Current state |
|---|---|
| Current phase | Phase 1 — application shell consolidation |
| Active PR | Phase 1 PR 3 — typed `AppDestination`/`AppNavigator` root contract |
| CI status | Pending for Phase 1 PR 3; PRs #95–#97 passed on Blacksmith |
| Completed ownership changes | Phase 0 test seams; Phase 1 makes root navigation consumers depend on `AppNavigator` while retaining the dispatcher as a compatibility transport |
| Deleted legacy/versioned files | None |
| Automated verification | Phase -1 and both Phase 0 PRs passed backend/Android CI; 13/13 device tests passed on Android 16 emulator; Phase 1 PR 3 results pending |
| Remaining physical Samsung checks | Entire checklist in `SAMSUNG_SMOKE_CHECKLIST.md`; no device is connected to the development workspace |
| Exact next PR | Phase 1 PR 4 — introduce `AppContainer`/`AppViewModel`, move overlay and session-expiry ownership to the app host, and document route/window factories |

## Completed PRs

| Phase | PR | Purpose | Evidence | Rollback |
|---|---:|---|---|---|
| -1 | #95 | replace all three Linux `ubuntu-latest` jobs with `blacksmith-2vcpu-ubuntu-2404` | actionlint; Android and backend CI passed on named Blacksmith runners; no Play release dispatched or path-triggered | revert merge commit `dcbd534` |
| 0 | #96 | record the governing plan, ownership/routes, Samsung checklist, and baseline entry contracts | Android/backend Blacksmith CI; instrumentation APK compilation; exact 72 REST/3 WebSocket inventory check | revert merge commit `0453ddb` |
| 0 | #97 | characterize push/session/Back decisions and conversation chrome | Android/backend Blacksmith CI; 13/13 Android 16 device tests; mutation-sensitive routing test | revert merge commit `dbcf56b` |

## Phase 0 discovered risks

- root policy/dispatcher have JVM tests, but notification routing is private to
  `MainActivity` and has no pure parser seam;
- terminal 401/session expiry is repeated across many screens and repositories;
- conversation UI constructs repositories/realtime owners directly, preventing a
  deterministic Compose test without a narrow seam;
- outer `ConversationScreen` and V8 composer both apply IME-related padding;
- instrumented/device tests are not run by the current hosted CI; and
- no physical Samsung device is connected to this workspace.
