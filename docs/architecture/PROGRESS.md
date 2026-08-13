# Architecture progress log

Update this table in every consolidation PR.

| Field | Current state |
|---|---|
| Current phase | Phase 0 — freeze and baseline |
| Active PR | Phase 0 PR 2 — testable push/session/Messages policies and conversation chrome contract |
| CI status | Pending for Phase 0 PR 2; PRs #95 and #96 passed on Blacksmith |
| Completed ownership changes | Phase 0 records behavior; PR 2 introduces behavior-neutral policy/scaffold seams without changing public ownership |
| Deleted legacy/versioned files | None |
| Automated verification | Phase -1 and Phase 0 PR 1 backend/Android CI; Phase 0 PR 2 results pending |
| Remaining physical Samsung checks | Entire checklist in `SAMSUNG_SMOKE_CHECKLIST.md`; no device is connected to the development workspace |
| Exact next PR | Phase 1 PR 3 — introduce the typed `AppNavigator`/destination contract and adapt the existing dispatcher as a compatibility boundary |

## Completed PRs

| Phase | PR | Purpose | Evidence | Rollback |
|---|---:|---|---|---|
| -1 | #95 | replace all three Linux `ubuntu-latest` jobs with `blacksmith-2vcpu-ubuntu-2404` | actionlint; Android and backend CI passed on named Blacksmith runners; no Play release dispatched or path-triggered | revert merge commit `dcbd534` |
| 0 | #96 | record the governing plan, ownership/routes, Samsung checklist, and baseline entry contracts | Android/backend Blacksmith CI; instrumentation APK compilation; exact 72 REST/3 WebSocket inventory check | revert merge commit `0453ddb` |

## Phase 0 discovered risks

- root policy/dispatcher have JVM tests, but notification routing is private to
  `MainActivity` and has no pure parser seam;
- terminal 401/session expiry is repeated across many screens and repositories;
- conversation UI constructs repositories/realtime owners directly, preventing a
  deterministic Compose test without a narrow seam;
- outer `ConversationScreen` and V8 composer both apply IME-related padding;
- instrumented/device tests are not run by the current hosted CI; and
- no physical Samsung device is connected to this workspace.
