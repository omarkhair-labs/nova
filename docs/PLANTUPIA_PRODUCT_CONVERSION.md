# Plantupia — Product Conversion on Nova (working production branch)

**Status: Under implementation / do not merge or publish yet.**

This branch is on the original Nova repository; the original Android application identity is intentionally kept for the **existing Google Play listing**:

- Current Play package ID: `com.omarkhair70.nova` (must stay the same).
- Nova backup `omarkhair70-droid/nova-backup` has its master head verified to match the Nova baseline at `fdc2c29a29dfc5cda41bf104c73812a516433e40`.
- Existing Android auth, media, posts, Stories, messaging, realtime and release foundation are retained.
- No production Oracle migrations or Play releases are authorized by this PR.

## Brand direction (from Plantupia 2026 guidelines)

Position: commercial biophilic integration, **not a retail plant supplier**.
Territory: Nature x Architecture x Human Experience x Business Performance.
Voice: confident, warm, precise, evidence-first.

Colors:
- Deep Green `#123B2E`
- Cream `#F3EEDE`
- Sage Green `#9FB79A`
- Muted Sage `#7E9C87`
- Forest Text `#16302A`

Typography policy: **Playfair Display** for brand headlines and **Inter** for UI text. The initial Compose screen uses a system serif fallback for headlines; approved font files and final logo artwork must come from authorized brand assets before visual sign-off. Do not extract and redistribute bundled fonts from internal PDF. The branded wordmark in code is **not** a replacement for a vetted logo asset.

Tagline: “Transforming Spaces. Inspiring People.”

## Working product contract

The starting customer is a client who purchased a Plantupia installation or has a Plantupia-managed business space.

Primary experience:
1. Existing Nova login signs in.
2. Client enters their owned/shared spaces.
3. Each living unit is shown in place/context. A unit can be an individual plant or a group installation.
4. Client names the plant; personal nickname is distinct from species and installation label.
5. Client records direct observations/care and sees history over time.
6. Nova social feed, media publishing, replies, stories and messages remain built-in platform capabilities.
7. Joining a public community conversation is explicit; private space history is **never automatically published**.

No mandatory serial number/QR. The Django Admin is the first small internal system, not an enterprise operations suite.

## Existing implementation

- `backend/plants/`: separate new Django app, models, migration, serializers, authenticated APIs, permissions and Django Admin.
- `/api/v1/plantupia/spaces/` and `/api/v1/plantupia/plants/` expose account-scoped collections in a `{"results":[...]}` envelope.
- `/api/v1/plantupia/plants/{uuid}/events/` records and returns chronology; `/nickname/` sets personal alias.
- `app/feature/plants/data/PlantupiaRepository.kt`: existing Oracle API origin, Nova token session/refresh. No new Supabase, no separate backend.
- `app/feature/plants/PlantupiaClientSection.kt`: real-data Plantupia UI in the existing Home; existing Nova feed remains below.

## Still required before a truthful full production release

- Run Django `check`, `makemigrations --check`, `test plants` on a restored/staging DB.
- Android assemble/test on CI and an actual device.
- Review the original Nova Oracle process, PostgreSQL version, storage and backup strategy **without copying production secrets to Git**.
- Check migration on Oracle staging/DB snapshot; backup real data; ensure rollback.
- Wire planted-unit photos and approved company content to the UI (without fabricating plant status).
- Connect Posts/Stories to plant context with explicit share consent and a moderation path.
- Update remaining route/screen branding; remove or revise Nova-specific product metaphors with UX sign-off.
- Obtain actual approved Playfair/Inter assets and logo. Test RTL and accessibility.
- Obtain final brand/feature approval from company and explicit commercial/IP scope confirmation.
- Keep Play package ID and compatible Google Play App Signing identity. Build an internal-test AAB; promote only after end-to-end checks and explicit approval.
- iOS requires separate platform development: the current Nova client is native Android Kotlin/Compose.

## Important limits

A GitHub PR changes **code**, not the running Oracle VM or Play listing. This branch is intentionally safe to inspect without triggering the Play-release workflow which only watches the default branch. Production release must be a separate reviewed action.

Do not commit the internal Brand Guidelines PDF, offer letter, private messages, real customer photos, cloud access details or API keys.
