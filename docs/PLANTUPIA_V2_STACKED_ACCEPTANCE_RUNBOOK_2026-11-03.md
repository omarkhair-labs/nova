# Plantupia V2 — Stacked acceptance & safe release runbook
**Date:** 2026-11-03
**Status:** QA handoff, NOT deployed and NOT approved for production.
**Canonical contracts:** PLANTUPIA_BOTANICAL_LIBRARY_CARE_EXPERIENCE_CONTRACT_V02_2026-11-03.md and PLANTUPIA_V2_NATIVE_SCREEN_BLUEPRINTS_2026-11-03.md

## 1. Branch / PR topology — do not merge out of order
- Parent PR #253: existing isolated preview QA, legacy Company/shop/Android; leave Draft.
- PR #262: P1 curated species and staff-verification separation; base: #253 branch.
- PR #263: P2 reviewed topic guide sections, Company publishing; base: #262 branch.
- PR #264: P4 quick inspection outcomes and optional care-details drawer; base: #263 branch.
- PR #265: P6 Care/Plant Nutrition category and sourced manufacturer label, Shop UX; base: #264 branch.
- PR #266: P5 opt-in reviewed inspection notifications; base: #265 branch.
No live deployments or merges are authorized by these PRs. Test the *exact final head* and parent compatibility. Documentation and code are separate from actual author/source approvals.

## 2. Backend migration preflight
- Confirm #253 is still isolated and no automatic production deployment is enabled.
- Confirm current QA backup exists, succeeded, and has a practiced restore path; note DB host by read-only check, never copy production secrets into chat.
- Migration 0013: botanical species alias and user-selected species FK; existing free-text, staff verification and handover unchanged.
- Migration 0014: optional source-backed topic sections on CareGuideVersion; all published narratives retained intact; staff review token binds header+section state.
- Migration 0015: optional subcategory and fertilizer label metadata on CatalogProduct; existing SKUs, gallery/cart/order snapshots and QA stocks retained.
- Run Django check, makemigrations --check --dry-run, migration plan, backend tests and scope-specific tests in disposable local database before QA. After QA migration verify counts and old order snapshots without modifying them.
- Keep Nova production DB, Redis, MinIO media paths and Play release unchanged. Run all operations only in explicitly owned QA workspace.

## 3. QA fixture requirements — synthetic identities only
- Company reviewer and operator authorized independently; regular customer A, unrelated customer B; read-only carer/viewer where supported.
- Published synthetic species 1 with aliases in English and Arabic, one draft, one withdrawn; a source record with HTTPS link and actual reviewer. No artificial botanical care facts presented as real.
- Company-verified actual QA PlantUnit with a published guide and approved inspection; self-selected species unit; unknown unit; two individual units of same species but different placement, distinct history.
- One nutrition QA SKU with synthetic manufacturer-only label and source, inactive until evidence complete, legacy accessory SKU, plant and planter SKU, one sold-out case; no real payment/fulfillment.
- Real device with two app accounts, Android notification permission toggle, device restart/cold start and camera/gallery access.

## 4. Owner journey acceptance
A. Add Plant: search synonyms; choose known type, see "based on your selection" and NOT staff verified; choose Unknown fallback. Duplicate names allowed, duplicate POST not allowed; view-only Space can't add.
B. Care Guide: Water/Light/etc show reviewed content, topic source, cautions and version; withdrawn/draft never displayed as published. Old narrative remains fully visible, not silently split.
C. Quick inspection: after explicit plan opt-in select soil Dry/Moist/Unsure or leaves normal/changed; Save creates exactly one PlantEvent(kind=observation), no Watered/Fertilized. Optional free note; old-client plain note still works. Wrong choice for plan rejected; stale revision and replay behave deterministically.
D. Free care: select actual Watered/Fertilized/etc; immediate Save, optional note/photo/follow-up behind secondary control, original event kind remains care; photo visible in Living History after authorization. Observation/Concern remain available even without selected actual action.
E. Reminder: personal one-shot check-in remains separate from approved inspection recurring check. Without explicit consent no scheduled OS alert. With consent one work item per real plan/revision/day; if Android permission denied still show due task. At delivery, re-fetch authorized server plan; wrong account, retired guide, canceled enrollment, archived plant, revoked Space, old date/revision, logout all block notification. Cold-start notification opens correct authorized plant and exact plan. Never assert exact clock delivery; WorkManager may delay.
F. Today: upcoming and due personal/approved inspections grounded in persisted data, no phantom tasks/auto watering; two plants' history separate.
G. Shop: All/Plants/Pots/Care; inside Care Tools/Substrates/Plant Nutrition; old uncategorized accessories still under Care. Fertilizer is only a product; manufacturer label and source shown, not a recommendation to fertilize now. Cart/review/order retains exact SKU and price snapshot. QA says no payment/delivery.
H. Handover: Checkout/order not automatically a My Plant. Staff prepares actual inventory and reviews, recipient-bound QR claim once, original PlantUnit appears only after claim with verified identity/history. No cross-account plant content.
I. UX: 320/390/430dp and tablet, 200% font, TalkBack semantics, real photo fallback, IME no clipped CTAs, bottom nav remains Today/Plants/Create/Shop/Circle.

## 5. Notification QA evidence contract
- Test an explicitly enrolled plan with next_due_on set in a short but realistic QA window, not a retroactive date after 09:00.
- Record account ID category only (no login token/PII), plan ID redacted, source guide version, due date/timezone and planned action. Verify worker uses same plan revision.
- Screenshots/video from Android showing: opt-in → notification permission (if required) → actual OS notification → cold-start plan open → care record in private Living History.
- Repeat with logout, account B, revoked membership, revised/withdrawn guide, skipped/rescheduled plan, disabled OS notifications and offline retry. For each case assert no private notification leak or stale action.
- Verify duplicate suppression after process kill and device restart. Successful CI is NOT a substitute for this real-device evidence.
- Avoid sensitive notification text on lock screen, no plant names/notes or private photos in generic OS notification.

## 6. Stop conditions
- Failed exact-head CI, unexpected migration, failing verified-vs-selected trust boundary, any cross-account data, duplicate events, spurious watering/treatment, unreviewed nutrition claim, changed historic order snapshot, missing restoring backup, app crashing on old content, background notification wrong-account risk.
- Never skip gates to meet a calendar date; keep PRs Draft and fix on correct review branch. Production promotion requires separate owner sign-off.

## 7. Still outside this stacked slice
- Company-provided actual 100-species reference content, rights-cleared imagery, botanical reviewer approval (not just staff technical permissions).
- Subscription payments/delivery/commercial inventory; no shipment or plant ownership from QA order by itself.
- In-app Activity Center unification across support/orders/Circle and full visual/editorial Art Direction signoff (linked issues #256/#259); this needs a separate test pass and probably separate scoped PR.
- Optional photo directly on a structured inspection occurrence: users can currently add photos via separate existing Care/Observation flow, not as evidence attached to the same inspection decision; treat as a remaining UX integration task.
- Long-term scheduling across app-not-open periods and devices requires more testing; current opt-in local WorkManager reconciles after authorized app/server interactions and does NOT claim minute-accurate periodic delivery.

**Finish line:** exact-head CI green for all stacked branches, approved source content, safe QA migration/backup, signed real-device E2E with two users and staff, then explicit decision whether to integrate into parent #253. Until then, no merge/deploy.