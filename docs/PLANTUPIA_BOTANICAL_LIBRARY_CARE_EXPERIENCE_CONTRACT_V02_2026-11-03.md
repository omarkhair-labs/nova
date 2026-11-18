# Plantupia — Botanical Library × Care Experience × Shop
## Canonical product, UX, architecture and acceptance contract (V02)
**Date:** 2026-11-03
**Status:** PRODUCT DIRECTION FROZEN FOR IMPLEMENTATION — not implemented by this document, no production approval.
**Delivery location:** Draft PR #253, feature/plantupia-unified-experience-20261028 (never merge predecessor #240/#241 separately).
**Stack:** Android native Kotlin / Jetpack Compose; Django / DRF; PostgreSQL; existing authenticated media + QA MinIO; Android WorkManager; Company Workspace.
**Origin:** Owner feedback from 2026-11-03 meeting: less manual maintenance input; a curated species library (~100 was an illustrative number); plant-specific reviewed guide; one-tap actions and reminders; fertilizer products in Shop; existing purchase/handover integration; contextual header, not repeated logo.

### 0 — Decision and non-negotiable product truth
The product is a guided, context-aware, factual care workflow, not a generic reminder generator.
A user can select a botanical identity from a curated searchable library; the library contains approved general knowledge, not a diagnosis or automatic assertion that the physical plant was verified. Individual owned plants retain independent care events, placement, follow-ups and histories. A staff-verified inventory plant uses a different identity-verification path.
Do not infer that a plant needs water, fertilizer, pesticide, pruning, or treatment solely from its species and a calendar interval. A suggested inspection/check is **not** proof of a required care action. Care recommendations and product matches require source review and appropriate context.
“100 plants” is a discovery target, not a launch gate. No fake or bulk AI-written care advice to meet a count. Start from actual customer/Company stock and approved source-backed entries.

### 1 — Confirmed existing code (reuse rather than rewrite)
- Backend: plants.models.BotanicalSpecies (curated key + scientific_name), CareGuideVersion (source, published version, reviewer), PlantUnit (free-text species_label + verified_species), private PlantEvent, Space membership, owned-plant list, company botanical guide/review library.
- Backend: CareInspectionPlan + CarePlanEnrollment + CarePlanDecision, scoped due dates and review actions; Shop CatalogProduct (plant/planter/accessory), family-key/SKU variant/gallery, authenticated cart and QA order with immutable order-line snapshots; staff inventory/import, draft installation, reviewed handover and recipient-bound QR claim.
- Android: PlantupiaSelfServiceForms currently free-text species entry; PlantupiaCareGuidePanel currently displays entire approved instructions from verified_species, not structured sections; PlantupiaInspectionPanel requires typed note for “complete”; Today displays dated personal checks and approved inspections; PlantupiaCareReminderScheduler uses WorkManager for a consented human-chosen one-time check-in, not recurring approved inspections; PlantupiaQaShop has search, four chips All/Plants/Pots/Care, family-aware item cards, detail/gallery, Cart, Review QA Order, My Orders and Order Detail.
- QA preview #253 and app are a test environment. A successful health check, CI, or screenshots do not prove end-to-end notification delivery, production commerce or completed handover.
References: backend/plants/models.py, backend/plants/demo_models.py, backend/plants/views.py, backend/plants/demo_services.py, backend/plants/inventory_import.py, app/.../PlantupiaSelfServiceForms.kt, PlantupiaCareGuidePanel.kt, PlantupiaDemoJourney.kt, PlantupiaCareReminderScheduler.kt.

### 2 — Canonical entities, identities and ownership
Keep three independent layers:
**Species Library**: reusable botanical taxon / accepted scientific name / aliases (Arabic and English)/ reference image / source-backed guide versions / approved inspection templates.
**My Plant**: real individual or group in a Space with owner/carer, user-facing nickname, placement, optional own photo, optional claimed species, staff-verified species if independently established, private care and observations, due actions and preferences.
**Shop SKU**: company product listing and variant, stock/price/photo; optional approved species association for plant category only. A paid order, a QA order or a product family is NEVER an individual PlantUnit.

Identity states are explicit:
- unknown: user added plant with no species match; can record observations/care, cannot receive tailored authoritative advice.
- user_selected: user selected a library species; show “based on your selection”, never “verified”, never impersonate staff confirmation.
- staff_verified: identity verified with evidence and responsible actor; source-backed published guide may be shown as verified for this specific plant.
- needs_review: conflicting user correction, unsupported identification, withdrawn sources, or changed evidence. Preserve immutable history and present safe general observations without claiming certainty.
- archived: no scheduled task/push; prior private history remains read-only.
Changing claimed identity does not silently preserve previous recommendations or schedules. Customer text cannot write verified_species or staff verification fields; staff route must audit every identity action.

### 3 — Data changes (additive migrations; no destructive rewrite)
**BotanicalSpecies enhancements**: accepted scientific name from reviewed taxonomic source (POWO/WCVP candidate), localized common names and aliases, normalized search tokens, approved cover image and rights/provenance, lifecycle active/draft/reviewed, curator timestamp. Avoid unsupervised bulk synonym merges.
**PlantUnit**: add nullable selected_species FK + selection_method=library/manual/unknown + selected_at, and derived identity_state; keep existing species_label for legacy clients. NEVER write selected_species into verified_species on consumer creation. Preserve old rows and leave unknown unless actual review evidence exists.
**Guide content**: retain CareGuideVersion.instructions/context_notes; add versioned CareGuideSection child records with enum water, light, humidity, soil, fertilizing, pruning, repotting, inspection, warning. Each has reviewed text, context/trigger, optional evidence-specific caution, display order and source reference. No autogenerated fixed watering frequency. Publishing validates required sections/sources, reviewer, immutability and a source provenance chain; support old free-text published guides with a clearly labeled fallback, not invented per-topic split.
**Inspection task templates**: based on an explicitly published guide revision + approval. Keep existing per-PlantUnit CareInspectionPlan + enrollment as source of persisted inspections. If reusing templates, create instantiated plans only where company-authorized and relevant; no automatic opt-in. A template describes a check/observe step and a reasonable review window, never an instruction that a physical action definitely must occur.
**Shop**: additive CatalogProduct.subcategory (tools, substrate, nutrition, other for accessory); optional botanical_species FK when kind=plant, added only after company curation. Nutrition metadata should be structured: formulation, NPK if on manufacturer label, size/units, label/instructions, source/manufacturer, precautions and applicability claims requiring review. Existing order snapshots must remain stable; do not auto-match a fertilizer to a plant unless approved rule and conditions exist. No real SKU, real price or stock without supplied company data.
**Activity / Reminder**: event/task identity, status pending/done/skipped/rescheduled/cancelled, due_at/timezone, enrollment/revision, recipient and consent/preferences. Prefer deriving visual Today list from authoritative server plans + personal check-ins; do not fabricate background tasks or “healthy” states. Dedupe idempotently and preserve audit/event lineage.
All migrations reversible where feasible, existing rows preserved. Index active species aliases/search, plant ID/context, next due, customer-scoped orders.

### 4 — API contract (proposal, not currently shipped)
GET /api/v1/plantupia/library/species/?q=&cursor= — approved visible taxa only, bounded search, name, common aliases, thumb, current published guide availability. Do not expose internal drafts or unverifiable images.
GET /api/v1/plantupia/library/species/{key}/ — curated identity, source provenance, full approved general guide metadata and coverage.
POST /api/v1/plantupia/plants/ — existing multipart contract extended with optional selected_species_key; empty/unknown allowed; server validates published visible identity, ownership of space and image; no privilege to set verified_species.
PATCH /api/v1/plantupia/plants/{plant_id}/ — change self-selected species only with explicit confirmation; invalidates previous self-selected recommendation display; staff-verified identity changes use existing staff authorization/audit path.
GET /api/v1/plantupia/plants/{plant_id}/care-guide/ — backward compatible; add identity_state, basis (staff_verified/user_selected/none), guide_version, section array, source metadata, last reviewed. Do not write guides based on user notes. Unverified user selection receives clearly qualified *general* knowledge, not verified advice; unknown yields unavailable safely.
GET /api/v1/plantupia/plants/{plant_id}/care-tasks/ — unified view of opt-in approved inspections and chosen personal checks, explicit task/source/next_due/status; pagination and stale validity.
POST /api/v1/plantupia/care-tasks/{task_id}/actions/ — confirm observation with structured result (e.g. soil_dry/soil_moist/unsure, leaves_ok/changes_noticed) or completed actual action; skip/snooze/stop; idempotency key and revision; typed note not mandatory for a valid predefined observation answer, but required when an issue or unstructured observation lacks a reliable structured outcome.
GET/PATCH /api/v1/plantupia/reminder-preferences/ — per-user/per-plant opt-in and category. Explicit disable; respect OS notification permission; no secret inference.
Existing /catalog/, /cart/, /demo/orders/ stay compatible; add subcategory and approved species metadata with default null. Existing handover/claim unchanged.
Use envelope with error codes identity_review_required, guide_unavailable, permission_denied, conflict_revision, stale_guide, notification_disabled; never fake success.

### 5 — Journey A: Add Plant (user already owns it)
Entry: Create (+) > Add Plant, or My Plants empty state. No redundant giant Plantupia header in subflow.
Step 1: “Which plant is yours?” search approved library by scientific/common/Arabic aliases, illustrated distinct rows, optional Recently used / popular (only if data). Each row includes an honest name and thumbnail. Search no match: “I’m not sure” leads to unknown-species flow, never a fake confident ID.
Step 2: “Make it yours” = personal name (required), space (required), placement (optional), own photo (optional). Progressive environmental context only where genuinely useful (light, indoors/outdoors, soil/pot), with skip affordance; no exhausting questionnaire.
Step 3: confirm selected identity as “you selected this type” (not staff-verified), save new actual PlantUnit.
Step 4: success plant card, “View care guide” if reviewed guide exists, “Choose a check-in” or “Not now”; do not start multiple push reminders, weekly watering, product subscriptions, or editable public advice silently.
Legacy Create Plant free-text remains supported by old API clients while Android progressively adopts library picker.
Empty/error states: unknown photo, no result, ambiguous aliases, offline, missing published guide, create forbidden in shared Space, duplicate plant names allowed because plants are separate real units, upload fail/retry without duplicate creation.

### 6 — Journey B: Care Guide and per-plant context
Plant Detail compact context header (nickname, photo, placement, identity badge). Content:
1. What needs attention: due or next optional inspection *only if persisted*.
2. Care Guide categories: Water / Light / Fertilizing / Soil / Humidity / Pruning / Repotting; show only reviewed nonempty categories, with brief summary collapsed and full source/context expanded. Clear distinction between guidance and recorded activity.
3. Quick actions: Checked soil, Watered, Fertilized, Pruned, Repotted, Moved, Observation / Concern. Actual actions require explicit user selection; do not synthesize care from click on “check”.
4. Living History: real dated observations, completed tasks, photo evidence, schedule decisions; immutable actor and time context.
5. Contextual staff/AI/Circle links only where pertinent, no redundant repeated top-level brand/navigation.
Display an “information based on selected species; not verified” message for self-selected identity; for unknown or withdrawn guide show general safe observation flows, not a ghost timetable.
For a guide update, keep references to the guide version used by each previous task and let authorized company review whether plans must retire or be superseded.

### 7 — Journey C: Today / one-tap check / photo
Today is a prioritized, server-grounded action list, not a second care database. Each card: plant image/name, source-approved “Check soil/leaves/drainage/light”, why, due date, one primary action.
Open a due soil task → “What did you find?” structured options Dry / Moist / Unsure + optional photo and free note. Confirm records observation to real history and closes the occurrence. No fabricated statement “watered”.
If the user chooses “I watered” separately → explicitly record a Watered care action, optional photo/note. The same for Fertilized. Never mark such actions from schedule expiry.
Show contextual buttons “Done”, “Later”, “More details” only when appropriate; put stop plan in manage reminders/settings with accessible secondary affordance.
Keep independent tasks for two plants of same species; a Watered event on plant A never closes plant B.
Do not force a new follow-up date for every care event; do not show unrelated long maintenance form during simple check. For unknown/needs_review, default to human-chosen check-in and observations only.
All action completion is transactional, duplicate-safe, permission-checked, auditable and undo/correction reviewed rather than silent rewrites.

### 8 — Journey D: Reminders and notifications
Three distinct capabilities:
A. personal chosen follow-up: existing consented local WorkManager OneTimeWork; approximate morning reminder; app session/account guard; not a watering instruction.
B. company-approved recurring inspection: existing persistent server schedule + Today presence; notification delivery **not implemented today**; design new opt-in delivery with per-plant preference, dedupe, cancellation/reschedule, reconnect, timezone change, clock drift, archive/claim/logout.
C. real support/order notifications: preserve existing separate push architecture and account-scoped Activity.
Start with server task source of truth and in-app “Due/Upcoming”; add local or server push only once compliance, idempotency and exact deep-link tests pass. For recurrence, use next planned occurrence and unique notification identity, not repeating unattended Watered operations. WorkManager timing is inexact; never promise minute-level reminder. Android 13+ POST_NOTIFICATIONS must be consented; notification opt-out cannot block reading schedules in the app.
Require direct deep-link to correct plant/task, across warm/cold start, revoked membership, logout, multiple accounts. No plant/private note content in lock-screen preview by default.

### 9 — Journey E: Shop / Nutrition / order / handover
Navigation stays Today | Plants | Create | Shop | Circle, with a context-aware Shop icon consistent with the others. Shop filters remain All | Plants | Pots | Care; inside Care group Tools, Soil & Substrates, Plant Nutrition. “Fertilizers” are real or QA product SKU listings in Care > Plant Nutrition, **not** an automatic Fertilized log and not a separate bottom navigation destination.
Existing product listing/details, family/SKU option selection, gallery, availability, cart, review QA order, My Orders/detail all remain; restyle hierarchy, states, notification entry and accessible controls. QA continues no-charge/no-delivery; do not disguise as commercial.
For plant products, company may associate a reviewed library species to a SKU; after an order, company verifies actual individual inventory and prepares draft installation; only reviewed shipment + recipient-bound QR claim makes the original real PlantUnit visible in My Plants. Never create a plant from checkout; no automatic claim.
Shop fertilizer product must show manufacturer label use/cautions, size/variant, exact SKU, source rights; optional curated “related care supplies” only when company-reviewed, clearly separate from plant-specific guidance. No medical-like or proprietary unfounded promises, no unlabeled dosing recommendation.
Shop designer deliverables: listing/card/taxonomy; product details/photo/variant; cart/quantity; review and no-charge consent; success; orders and detail; empty/loading/offline/error; notification entry; clean header and accessible bottom nav.

### 10 — Company Workspace authoring workflow
Company Botanical Library: find/create/curate taxa and aliases → attach rights-cleared photo → draft guide sections with credible URL and scoped context → reviewer approval → publish version. No published advice without reviewer provenance. Existing guide versions never rewritten in place; withdrawal retains the record.
Company Care Planning: staff-approved check templates/plans scoped to verified species and actual plants/placement; preview tasks and notification behavior; opt-in/decline, auditable changes.
Company Shop: category/subcategory; item data and nutrition label; image gallery provenance; stock/pricing governance; author-approved species association for plant SKUs; QA import remains isolated.
Company Fulfillment: order → physical inventory review → READY batch → staff-issued recipient invitation → explicit customer claim; link real plant, not generic species.
Company operations should have clear reviewer role, source check, conflict queue and stale-guide invalidation; no new public admin permissions.

### 11 — State and error matrix (must design + test)
Unknown species / ambiguous name / missing photo / no published care source / withdrawn guide / correction that invalidates staff verification / client versus carer versus viewer / archived plant / offline and stale cached data / duplicate submit / permission denial / different-account deep link / DST or timezone adjustment / notification permission denied / canceled reminder / delayed WorkManager / two plants same species with different context / two care actions same day / user chose “unsure” / edited SKU or archived shop product / order without actual delivery / handover not claimed.
In every state: preserve truthful UI, explicit provenance and not-yet-verified status, and deterministic next action.

### 12 — Visual design rules: authored, native, restrained
- Primary bottom nav unchanged; contextual headers on nested screens, no 150px repeated logo/header.
- Use clear named hierarchy: one editorial title; practical cards; key action in visible first viewport; avoid giant whitespace and stacked repeated CTAs.
- Real authored plant photography (rights verified) with stable aspect ratio; checkerboard is never presented as a styled background; native loading/empty fallback if missing.
- System tokens: warm paper/ivory, forest green, muted botanical sage, restrained lines and shape; type hierarchy and space from existing Plantupia design system. Avoid new generic “AI gardening” dashboard and fake health scores.
- Observe-first semantics: task title must say “Check soil” not “Water plant” if only inspection is approved.
- Minimum 48dp interactive touch targets, font scaling 200%, TalkBack descriptions, 320/390/430dp and tablet widths, keyboard/screen-reader form order, reduced motion, contrast check, avoid text clipping.
- Fast create and care flow: no more than 3 high-level steps, simple check action completed in a focused interaction with at most optional evidence, no full form imposed.
- Quick references: Species chooser → Plant setup → Success → Plant guide; Today task → Outcome → History → Next due; Shop Nutrition → SKU detail → Cart → Order → Company fulfillment → QR claim → My Plants.

### 13 — Implementation stages and dependency order
P0 Freeze spec, inventory current functionality, record source/reference matrix; all follow-ups point here. **This document only.**
P1 BACKEND: curated species + aliases/assets + public-authenticated read API + selected_species distinct from verified_species + migration/backward compat + auth tests.
P2 COMPANY: structured source-backed guide sections/editor/review + optional approved inspection templates + sample content authored by company, publishing gate.
P3 ANDROID: native library picker + short Add Plant + unknown path + trust states + plant guide categories.
P4 CARE: collapse long form into compact contextual action + structured check completion (without false watering), preserve PlantEvent/history and plan audit; Today display.
P5 REMINDERS: opt-in recurring inspection delivery, account-scoped deep links, Activity consistency, timezone and cancel/revoke; keep the proven one-time reminders.
P6 SHOP: Plant Nutrition subcategory and metadata, unchanged SKU/cart/order guarantees, company product evidence, UX end-to-end.
P7 HANDOVER: curated SKU→species mapping (optional), inventory→verified identity→claim exactly once; no order auto-creation.
P8 DESIGN/QA: screen polish, accessibility matrix and real device E2E, 2 accounts, API integration + background tests; only then seek release review.
Do not build an independent recommendation ML engine or camera identification without data, consent, validation and separate review; not a prerequisite.

### 14 — Minimum acceptance / regression suite
- Species search aliases, canonical taxonomy and publication gates; locale and fuzzy ambiguity; no drafts surfaced.
- Owner can add self-selected Monstera, still identity_state=user_selected; cannot forge verified_species or staff actor. Unknown species can save plant and no false guidance. Changing selected species invalidates stale guide without erasing history.
- Staff-owned verified plant retains original ID through claim; shop order cannot generate plant nor confer ownership; multi-account recipient checks.
- Two plants same species but different placement: separate care history/check-ins and no cross-plant completion. Unverified species cannot receive a fake recurring water action.
- “soil moist” closes a documented inspection as an observation, not Watered; actual Watered/Fertilized requires affirmative action. Photo and note optional for structured outcome, explicit uncertainty supported.
- Guide unpublished/withdrawn never displayed as authoritative; version/provenance preserved and plan invalidation tested.
- Recurring schedule in server visible in Today, no default push; opt-in push can be denied/delayed/canceled, no duplicates, deep-link scoped. Logout/revoked space/archived plant cancel or block private notifications.
- Fertilizer is accessible under Care > Plant Nutrition only when catalog data authorized; order totals and SKU variant/name/price snapshots remain immutable; no mock charge or delivery.
- Responsive snapshots at 320/390/430dp/tablet, large text, TalkBack, dark-mode where supported. Real Android device QA for one owner + one staff + separate customer, QA hosted exact head, cleared local app state.
- Django migrations+test suite, Android unit+lint+assemblies, no architecture guard regression. CI passing is necessary, not physical UX proof.

### 15 — Research grounding and claims boundary
- Kew POWO/WCVP: accepted botanical names, synonyms, identification/reference and provenance (not automatic advice). https://powo.science.kew.org/about ; https://powo.science.kew.org/about-wcvp
- Royal Horticultural Society houseplants: check needs before watering, light/humidity/context, avoid automatic fixed watering; feeding during active growth with cautious product-use context. https://www.rhs.org.uk/plants/types/houseplants/growing-guide
- University of Maryland Extension: fertilizer forms, follow product label directions, excessive fertilizer harms; guidance varies by houseplant and conditions. https://extension.umd.edu/resource/fertilizer-indoor-plants
- Android WorkManager: periodic scheduling inexact under battery optimizations. https://developer.android.com/reference/kotlin/androidx/work/PeriodicWorkRequest
- Android notification permission: Android 13+ explicit permission for most notifications. https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Climate caveat: horticulture articles often describe UK/US season months; never mechanically treat “March–October” as universally correct for an Egyptian indoor home. Use growth/light/context and reviewer decision.

### 16 — Open business decisions (not blockers for core code)
Who signs off plant species and fertilizer label content? Who owns and licenses botanical images? Exact company priority species list (e.g., from inventory/sales, not arbitrary generated 100), bilingual Arabic/English review and customer language preference; real SKU/stock/pricing and delivery contract; frequency and channel of opt-in notifications; approved care protocol coverage. Until answered, design and code must display honest unavailable/draft states, never fabricated company approvals.

### 17 — Change-control and release protection
Do not mutate live Oracle/Nova production, the live database, existing media bucket paths, Play distribution, or merge #253 without explicit review. Existing QA #253 and MinIO nova-media can remain as already configured; no new bucket implied here. Any new migrations: preflight backup QA, run tests, confirm old rows and API unchanged, deploy preview #253 ONLY. Preserve functional Shop/QR/AI/social features. Do not silently replace earlier docs; this contract supersedes UX assumptions for future care/library/nutrition work while referencing implementation truth in code.
