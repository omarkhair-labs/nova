# Architecture progress log

Update this table in every consolidation PR.

| Field | Current state |
|---|---|
| Current phase | Phase 0 — freeze and baseline |
| Active PR | Phase 0 PR 1 — documentation and characterization tests |
| CI status | Pending for Phase 0 PR 1; Phase -1 PR #95 passed on Blacksmith |
| Completed ownership changes | None; Phase 0 records behavior only |
| Deleted legacy/versioned files | None |
| Automated verification | Phase -1 backend/Android CI; Phase 0 test results pending |
| Remaining physical Samsung checks | Entire checklist in `SAMSUNG_SMOKE_CHECKLIST.md`; no device is connected to the development workspace |
| Exact next PR | Phase 0 PR 2 — add the smallest reliable shell/Messages test seams and device-oriented navigation/IME characterization without production redesign |

## Completed PRs

| Phase | PR | Purpose | Evidence | Rollback |
|---|---:|---|---|---|
| -1 | #95 | replace all three Linux `ubuntu-latest` jobs with `blacksmith-2vcpu-ubuntu-2404` | actionlint; Android and backend CI passed on named Blacksmith runners; no Play release dispatched or path-triggered | revert merge commit `dcbd534` |

## Phase 0 discovered risks

- root policy/dispatcher have JVM tests, but notification routing is private to
  `MainActivity` and has no pure parser seam;
- terminal 401/session expiry is repeated across many screens and repositories;
- conversation UI constructs repositories/realtime owners directly, preventing a
  deterministic Compose test without a narrow seam;
- outer `ConversationScreen` and V8 composer both apply IME-related padding;
- instrumented/device tests are not run by the current hosted CI; and
- no physical Samsung device is connected to this workspace.
