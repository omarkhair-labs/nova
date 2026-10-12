# Plantupia — Native Visual Parity / Delivery Truth

**Date:** 2026-10-12
**Design reference:** [Full Product Design Review 03](https://github.com/omarkhair70-droid/Plantupia/tree/research/foundation-source-atlas-20261007/prototypes/full-product-review-03) and preceding Plantupia research/product contract.
**Code branch:** `experiment/plantupia-care-create-native-20261012` derived from unmerged Nova PR #240.
**Latest verified source build candidate:** commit `5f05c9cb07b118f9fe0cecc3be07a433aeaff864` (CI run [38258197091](https://github.com/omarkhair-labs/nova/actions/runs/38258197091)).
**Boundary:** AI assistant NOT part of this design pass. No backend schema/AI changes, production deployments, Play release, or master branch merges.

## Why this matters

The user's previous feedback: producing a rich Plantupia HTML design and then shipping just a navigation change is **not** a finished product. It is incorrect to treat Nova social engines' presence as the same as complete, consistent Plantupia UX. This file records a **source-backed** full-surface audit and tangible native implementation — not a claim of overall design signoff.

## Current code parity ledger

| Experience/surface | Actually done natively, in current branch | Still not closed |
|---|---|---|
| Home identity | User-based time greeting; plant/spatial context; actual Plantupia header | Final approved brand type/mark and real-device screenshot validation |
| **Living Space hero** | `PlantupiaVisualComponents.kt` authored photo-first 304dp card, privacy badge, actual plant count/name/location, open space action; native botanical fallback only when no photo | Real-space site photos and design brand approval; correct visual hierarchy on tiny/large screens |
| **Plant grid/list** | `PlantupiaPlantPortrait` reused for actual active plant lists; full photo/name/species/placement/last recorded date, clickable | Different species/spacing/search scenarios on real devices; specialized 100+ inventory performance |
| **Plant detail** | `PlantupiaPlantIdentityHero`: real photo or visibly illustrative fallback, full name/actual context and existing private action/history editor remains | Dedicated route vs expanded Home remains unapproved; care composer deep-scroll, media loading states, before/after compare UI |
| **Care root** | New visual attention view from actual `PlantupiaRepository.plants()`: user-selected due check-in, next checks, image record cards and private care entry; actual heart icon asset | Due page from detailed history, true refresh on return, optional reminders/preferences and one-tap native care composer UX; device QA |
| **Create (+)** | Compact real Material 3 Bottom Sheet with full visual action groups: private care, add plant, public Circle question, original Story, Post/Reels and access to all original Nova Create formats | Real talkback/swipe/keyboard/back and Visual Pass 03 comparison; possible shorter copy and tool placement |
| **Circle questions** | Existing permission-aware Plantupia question panel and Nova social interactions retained | More integrated question/story/media art direction and public payload previews; device QA |
| **Stories in Circle** | Real `StoriesRail` now appears under People & Moments in Circle; actual existing viewer/composer engine reused | Confirm performance/media, 24h and reply-to-Inbox links on preview real accounts, user review of its prominence |
| **Inbox/messages** | Existing Nova Inbox, conversations, media and DM navigation **retained intact**, not replaced with fake chat | **Visual parity still OPEN:** private conversations need Plantupia-specific header/list design and screenshots, without degrading Nova behavior |
| **Profile** | Original rich Nova public identity/follows/grid kept; private spaces now photo-first cards with actual counts and location; separate from public posts | Real screen capture, mixed private/public hierarchy, large text/RTL/layout |
| Notifications | Existing Nova activity/DM notification engine remains in app | Plant follow-up push notifications not implemented; deep link tests and preference screens not closed |
| Media/video | Original Nova publishing/video functionality remains untouched | Real plant-context capture, album/media UX and actual review of Story/Reels execution not closed |
| Client site/field ops | Research only; no invented customer data or visits | Requires company-confirmed contracts, permissions, handovers, staff process and imagery |
| Smart sensors | None | **OUT OF SCOPE**. Plantupia has no sensors in plants |
| iOS / public website | No changes | Separate program; no iOS release or company website built here |

## Technical source truth

- Android code currently uses the Nova auth/media/notifications/social repository. `NovaServerConfig` routes QA variant to PR #240 preview host, not the production Nova host. Server preview database/media isolation has **NOT** been independently verified here.
- No generative photos of real clients. A first user-uploaded plant photo can become the space hero; otherwise an authored placeholder is clearly illustrative.
- Correct use of permission-scoped private plant data, real follow-up dates, and no fabricated health score, irrigation need or sensor moisture percentage.
- Real Stories and Inbox are still existing Nova engines — do not mistake reuse for completed visual parity.
- CI validation: backend unit and migrations; Android Kotlin unit + lint; side-by-side QA APK built and ZIP integrity checked. **CI is not an on-device UX test.**
- Artifact raw `app-qa.apk` extracted from GitHub Actions id `11752191130`, SHA256 `a3954c962fc23e34a6575635b45e9e0e7403d07993c91f387a6538ac46767cdd`; app QA build code commit `5f05c9cb07b118f9fe0cecc3be07a433aeaff864`.

## Acceptance work not falsely closed

1. Capture Android device screens: Home (empty, one plant, 7 plants), Plant detail, Care, Create sheet, Circle/Stories, Inbox/conversation, Profile, loading/error/back behavior.
2. Compare same journeys side-by-side against V03 design and *original Nova* screenshots; classify KEEP / REDESIGN / FIX / DON'T SHIP.
3. Confirm actual Stories media/replies, DMs, session restore, media privacy and Circle public permission on truly isolated staging data.
4. Close the specific visual parity gaps above with original native components; do not claim UI approved without review.
5. **No preview auto-deploy or production database sharing.** Stop before any Coolify/Play action until isolation and brand/company permissions known.

**Delivery truth:** This branch materially improves native visual coherence and surfaces the existing Story engine, but it is **not full V03 parity or finished Plantupia**. The earlier failure was overclaiming completeness after isolated changes; this tracker prevents repeating it.
