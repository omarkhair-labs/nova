# Plantupia Integrity Sprint 02 — Shared Session Safety (F01)

Scope: native shared authentication only, starting at `bd7b82276c9bb906cc28508c60002ce7140b5084` on the existing feature branch / draft PR #241. This supersedes Sprint 01's F01 investigation-only status. F05, backend data/schema, production, Play and consent/deletion policies are outside this sprint.

## Defect and test-first evidence

The former characterization now asserts that B's complete stored session remains unchanged after A's delayed refresh. At test-first commit `69d794621cf4fd5c036d03c6467d4ff66dbd1041`, the focused run failed as expected: 2 tests, 1 failure, B's access token overwritten. The fixed test supplies A's originating login identity to the real encrypted store's guarded update.

## Shared ownership implementation

- Every login attempt and explicit logout creates a new persisted random `LoginIdentity`, even for the same user and identical credentials. Access-token refresh retains that identity. Login intent invalidates previous work before awaiting a remote response; failed or cancelled new login leaves the device signed out rather than restoring a superseded account.
- All store instances share one process-wide coordinator monitor. Token/profile/generation reads and writes, conditional clearing and full credential replacement are atomic relative to each other. Full login/security saves use one SharedPreferences editor transaction containing encrypted token pair plus cached user and generation. Cached-user updates reject a different user ID.
- Asynchronous save/update/clear APIs require the originating identity; unconditional setters have been removed. Explicit user login/logout are the only unconditional transitions. Restore no longer calls login save and cannot resurrect an earlier session.
- Refresh flights are shared per login across repository instances. Concurrent callers share success or failure. A late 401 for an already rotated access token reuses the new token. Cancelling one waiter leaves other waiters intact. Login/logout/replacement invalidates waiters immediately, cancels the old worker and prevents any late response from committing. Exceptional transports complete waiting callers instead of stranding them.
- Initial requests, refreshes, retries and successful results keep their original owner. Changed-login results return a local 409 and cannot clear the current login. Invalid refresh credentials and a matching retried 401 conditionally invalidate their owner; transient failures retain credentials. Realtime retains its existing access-token fallback on transient failure.
- Profile and Me writes are guarded after suspension. Password/revoke-other-session responses replace both credentials together and rotate identity, invalidating refresh work using the previous credential pair.
- Media creation captures origin before image/audio/video preparation in Messages, Posts, Stories, Reels, Pulse, Memories and Rooms. Feed cache fallback cannot return old-account records after a switch. Queued call actions and device-registration work retain their originating generation.
- Delayed feature expiry events cannot log out a newer stored session. Confirmed expiry preserves local reminder/push cleanup, guarded while signed out, without another credential clear or generation change. Deferred push logout checks both generation and absence of credentials; lifecycle operations are serialized so later activation follows earlier teardown. On-unregistered device callbacks do not remove an installation using a new account's token.
- Optional registration photos are snapshotted by selection generation, consumed only after valid registration, retained on failure, and never consumed from a newer registration flow.

## Caller audit and compatibility

| Surface | Migration / preserved behavior |
| --- | --- |
| Login, registration, Me, profile, restore | `NovaAuthRepository`: guarded login save and cached-user writes; optional avatar remains optional; transient restore uses current owner's cached profile only. |
| Password change, revoke others, session list/revoke, deletion | `NovaAccountSecurityRepository`: shared request/refresh and conditional credential replacement/clear. Existing endpoints, authorization and deletion policy unchanged. |
| Feed/Posts, People/Social, blocked accounts | Shared authenticated request with original feature remotes, parsers and account publish checks. Feed cache remains user-scoped. |
| Messages, Inbox paging, groups/membership, appearance, conversation tools | Shared refresh/retry; Messages realtime keeps transient fallback; media preparation retains origin. Wire/realtime protocols unchanged. |
| Calls, push, notifications, privacy, sharing | Shared refresh/retry; background call/push origin bound; explicit Settings logout retained. Push delivery still needs device QA. |
| Stories, Reels/Profile Reels/watch | Existing publishing/media parsers and audience checks retained; media preparation and callbacks keep origin. |
| Memories, Rooms, Orbit, Tonight, Pulse, People paging, Plantupia | Same shared owner mechanism. Plantupia now uses login identity rather than refresh-token equality, preserving valid access rotations while rejecting re-login. |
| Read-only store users | AppContainer, profile/group identity readers, reminder ownership and push startup still read the same token/user snapshot; StoredSession additionally carries its identity. |

All 25 feature request wrappers plus Auth and Account Security were inspected. The backend explicitly retains `ROTATE_REFRESH_TOKENS = False` (`backend/nova_backend/settings.py`); the existing access-only refresh response remains compatible. Enabling server refresh-token rotation in the future would require a coordinated token-pair protocol update. Both existing refresh transports (`AuthRemoteDataSource` and `NovaApiClient`) remain unchanged; repositories invoke them inside coordinator-owned refresh callbacks. All source call sites compile against the required-owner APIs. Architecture guards now require the new shared seam instead of the old unsafe unconditional writes, and additionally protect generation checks, media capture and stale UI expiry behavior. No endpoint ownership or transport boundary was removed.

Storage upgrade requires no backend migration, credentials reset or keystore replacement: legacy sessions receive a generation on first load. AES/GCM/NoPadding, keystore alias, preference namespace, IV/ciphertext keys and token-pair separator are unchanged. Java's padded Base64 encoder is available at the existing minSdk 26 and produces the same no-wrap format. Invalid ciphertext fails closed. No generation, token or private profile values are logged.

The shared monitor is process-wide; current manifests declare no separate Android process. Introducing a separate process that writes sessions would require an IPC-backed owner rather than assuming this in-process coordinator covers it. StoredSession is an internal app API change; any future out-of-tree caller must capture an identity before suspension and use guarded APIs.

## Regression matrix and verification boundary

`NovaSessionStoreRefreshRaceTest` uses actual NovaSessionStore encryption and preferences code with JVM AES/GCM, in-memory SharedPreferences and a test-only keystore provider. Actual auth repository cases substitute the remote boundary and push activation only; actual messaging media preparation and background repository owner checks are exercised. No hosted endpoint or real account is contacted.

Coverage includes A->B, A->logout, A->new-A with identical credentials; token/profile/save/restore/clear/replacement rejection across instances; concurrent refresh success/transient failure/auth rejection; cancellation of one waiter; throwing transport completion; background realtime/call/push work; delayed initial/retried success and 401; actual delayed Me/profile and overlapping logins; actual restore after refresh; legacy encrypted-session upgrade; Plantupia access rotation; media preparation during switch; registration-photo retry and newer-flow protection; and signed-out cleanup ownership. AppViewModel tests cover late refresh success/null/401, stale feature expiry and a login appearing before cleanup.

Final local and exact-head CI outcomes are recorded in the PR verification section. No physical-device test, hosted QA/backend deployment or release acceptance is implied by this report.

## Required physical Android follow-through

1. Two accounts: hold A's HTTP/refresh/Me/profile/media requests, switch to B, release success and failure in both orders. Verify B's avatar/profile, private spaces/history, inbox and realtime connection remain B; old requests never navigate, show A's data or expire B.
2. Repeat with logout, same-user re-login, background/foreground and overlapping message/call/push reconnects. Verify reminders and device push teardown cannot disable the later login.
3. Upgrade an installation with existing encrypted credentials; cold start, offline restore, access expiry, refresh rejection and restored profile/avatar continuity. Repeat on a physical keystore after process death.
4. Registration failure/retry with a selected avatar, then a different registration/login flow; profile photo and illustrated-avatar update; password change/revoke others while refreshes are pending. Confirm server and local identities agree.

F01 is implemented with automated regression coverage; whole-app/release approval remains open. F05 uncertain-save/idempotency behavior is unchanged. Already-dispatched server mutations cannot be rolled back by local cancellation. Hosted QA isolation, physical keystore/FCM/realtime lifecycle behavior and the other audit findings remain separate gates.
