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
