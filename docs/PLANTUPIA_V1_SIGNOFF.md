# Plantupia Android V1 — evidence-based sign-off ledger

**Draft review artifact; no deployment or merge authorized.** Update this ledger using actual CI, Django and on-device results, not assumptions.

## What this PR actually changes

| Product journey | Current branch implementation | What CI does not establish |
| --- | --- | --- |
| Signed-in home | Nova session retained; authenticated Plantupia spaces and real-photo living hero; community feed optional | Live endpoint/migrations/data in current Oracle runtime |
| Find a real unit | Account-scoped spaces; list, search, recent-activity ordering, incremental unit rendering | Site zoning and performance on a physical device with >100 units |
| Living history | Personal alias, note/photo, observed care or issue, older-event pages, recorded evidence, real dated comparison | Site maintenance rules, media access policy and full real-world chronology |
| Public plant help | Separate Circle questions using existing Nova Posts, comments, block/report infrastructure, explicit photo consent and outcome saved to private history | E2E moderation, object-media privacy, reporting and real customer consent |
| People/community | Existing Nova discovery, relationships, chat, posts and optional media tools kept | That standalone Reels/Stories/Pulse/Rooms should be visible in V1 |
| Personal profile | Nova identity/posts remain; private client spaces only on self profile | Visual approval, accessible RTL and private/public state QA |
| Android delivery | Original `com.omarkhair70.nova` retained; CI builds unsigned/test artifacts | Same-signature Play AAB, actual Internal Testing availability |

## Verified vs not verified

- Automated code checks are run on each PR update: Django tests and migrations checks, Android unit tests, lint, debug and release assemblies, bundle release. See the latest successful GitHub Actions run on PR #240.
- A previous Android QA screenshot showed **404 HTML at the Plantupia Space endpoint**. It is evidence that deployed routes did not match the branch. The branch-only Django API is not live merely because CI is green.
- The current official Playfair / Inter and final vector logo asset files are not shipped as authorized signed-off brand assets; existing substitutions are provisional.
- Physical-device Android navigation, TalkBack/RTL, media/network resilience, two-account security tests, restored Oracle database migrations, storage ACLs, signed Play release and production moderation review are **not confirmed passed**.
- The prior stand-alone debug-sideload package was an experiment and has been removed from the release path. Do not install it expecting it to be the final original Play app.

## Acceptance walkthrough to execute on a test environment

1. Log in as a **customer/space owner** and as a **viewer** on separate accounts; ensure each only sees permitted spaces/units.
2. For an installation with many units, search by name, species and placement; load later records without omissions.
3. Open a unit, rename it personally, record a photo-only observation, a care act, and an issue; verify each private event has the correct actual time and actor.
4. Scroll into older dated history and compare only available authentic images; do not infer diagnosis from them.
5. Create a plant question: inspect the *public preview*, explicitly consent to the chosen image/topic, verify the public audience sees no private space identity, then reply and record an observed outcome in the private history.
6. Traverse Circle ↔ unit ↔ Home, Android Back, personal/public profiles, blocked people, offline/media failure, and chats.
7. Compare screen-by-screen against the approved brand assets and the original Nova interaction quality; check an actual Android device with RTL and TalkBack.
8. Only after owner approval, deploy/back up/migrate the already existing backend through its **confirmed** pipeline; approve a signed, correctly versioned AAB for the existing Play Internal Testing listing.

**Stop condition:** never call this a final, available Plantupia build until the backend, signing and in-device acceptance gates above are all evidenced. Do not merge or deploy production without explicit review.
