# Plantupia Integrity Sprint 01 — 2026-10-18

Scope: F06, F07 and F12 implementation; F01 investigation only. Base: `f3dea7cb821521b7b97bbcbc1d702f596d93206a`, existing stacked draft PR #241. No backend schema, consent, deletion policy, production or Play change.

## F12 shared-network caller audit

Before changing shared parsing, reviewed requestJson/requestMultipart call sites across Auth, Posts, People, Stories, Reels, Messaging/Inbox/Groups/Appearance/Tools, Calls, Privacy/Sharing/Notifications/Security, Memories, Pulse, Rooms, Orbit, Tonight and Plantupia. Their shared transfer contract returns a JSONObject; feature parsers own required fields and collection envelopes.

Nova's backend intentionally returns empty HTTP 204 for deletion/session revocation (Post, Story, Reel, Pulse, Room item, Memory draft, Group, account session). These remain successful empty objects; HTTP 205 no-content is also accepted. Existing non-2xx error parsing and architecture seams are retained. Push has a separate transfer implementation and is unchanged.

For other 2xx responses, transport rejects blank/non-object/malformed/trailing JSON. A valid `{}` remains a valid shared object because not every Nova endpoint has a results field. Plantupia separately requires results arrays, object rows with identities, explicit nullable pagination, and valid mutation acknowledgements. Invalid rows fail the whole response rather than silently disappearing. Missing legacy can_add_plant remains unknown/false rather than granting access or invalidating a genuine list.

Mutation validation checks returned identities (including matching edited IDs), positive public post IDs, and acknowledged date/outcome fields. A malformed acknowledgement remains an uncertain save: the UI preserves the draft; idempotent retries remain outside this sprint (F05).

## Verification boundary

Local Android unit suite: 375 tests passed after F06/F07/F12 additions. Local Django check and migration drift check passed; all 43 plant-domain tests passed against explicitly disposable in-memory SQLite/caches/storage with external service credentials overridden. Final PostgreSQL/Redis CI, release lint/build and physical device cases are tracked separately. No device acceptance is inferred from these tests.

F01 investigation and final sprint evidence are recorded in the next increment. Required manual regressions: delayed mutations after Back/plant/account changes; independent pending failure/retry with an unsaved care draft; malformed 2xx and valid empty data/no-content responses. No private screenshots are committed.

### F06 callback follow-through

Photo-picker, notification-permission and date-dialog returns are also tied to the originating detail/login. A delayed platform callback cannot attach a photo or date to a subsequently selected plant. Final sprint CI includes these source changes; physical picker/permission-dialog transitions still need device QA.

## F01 deterministic investigation — shared authentication NOT changed

`NovaSessionStoreRefreshRaceTest.delayedARefreshCanMixItsAccessTokenWithBsSession` runs the actual NovaSessionStore save/load/updateAccessToken methods with JVM AES/GCM, in-memory Android preferences and a test-only keystore/Base64 adapter. A CompletableDeferred barrier holds A's refresh while B is saved, then releases the old callback. The test confirms the current defect: B's user ID and refresh token coexist with refreshed A access. This is a characterization test asserting the known unsafe state, not a fixed-security regression or device proof.

A second test exercises the actual PlantupiaRepository login guard: both account B and a fresh login by account A invalidate the original repository. Neither test calls the hosted API. Focused run: 2 tests passed. No NovaSessionStore/NovaAuthRepository source was modified.

Safest proposed shared repair (requires separate approval):

1. One shared session owner must atomically compare an immutable originating login generation and identity before applying token, cached-user, clear or restore writes. Save token/profile state consistently; per-instance locks are insufficient because repositories create separate stores.
2. A delayed refresh/Me success or stale 401 failure must never replace, clear or resurrect a later login. Guard restoreWithRefresh/save and updateUser as well as updateAccessToken; a fix limited to one token setter is incomplete.
3. Coalesce refreshes per originating login. A new login/logout invalidates that flight; do not treat rotating access tokens as login identity.
4. Promote the characterization into a B-remains-unchanged regression when repaired; cover A->B, A->logout, A->new-A, reversed replies, late failures and cached-user updates.

F01 remains OPEN and remains a release risk in inherited Nova callers. The Plantupia-local guard is bounded protection, not a substitute for the shared repair. Uncertain-acknowledgement duplicate writes (F05), hosted QA isolation, physical device/permissions testing and the other audit findings remain outside this sprint. Circle consent/resolution behavior and account-deletion policy are unchanged.

### F06 records-only reload regression follow-through

Shared metadata/archive confirmations now refresh inventory, history and pending reviews through a new guarded reload epoch without reopening the plant or clearing the care note, photo, action or chosen date. The helper rejects a switch after either inventory or history returns and cannot clear a newer busy flag. Three additional JVM regressions pass (380 total local unit tests). Question drafts retain their existing plant-scoped state while callback lifetimes change; platform picker results retain the originating generation. Transient/contract refresh failures retain already-loaded same-login records with an error rather than discarding them.
