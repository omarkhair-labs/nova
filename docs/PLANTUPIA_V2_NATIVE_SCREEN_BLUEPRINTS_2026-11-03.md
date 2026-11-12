# Plantupia V2 — Native screen and interaction blueprints
**Date:** 2026-11-03 | **Status:** UX specification, not shipped
**Parent:** PLANTUPIA_BOTANICAL_LIBRARY_CARE_EXPERIENCE_CONTRACT_V02_2026-11-03.md
**Target:** Android native Jetpack Compose, existing five-root navigation; Company Workspace under /company/.

## Design intent
Quiet, warm, botanical, contextual. Users should think “I know what needs checking” instead of “I must fill out maintenance paperwork.” Brand appears once at meaningful entry and as modest context elsewhere. No giant repeated brand header. No invented health score, humidity sensor reading, treatment or “next watering date” from a species name. Exactly one dominant primary action per view.

## Token baseline (adapt to code-owned Plantupia colors and typography; no parallel design system)
- Surfaces: warm ivory, white/off-white cards, soft sage observation groups, forest green active actions, readable muted text, accessible separators.
- Horizontal content margin 20dp; vertical section rhythm 24/32dp; cards padding 16/20dp; inside-field rhythm 12/16dp.
- Cards radius 14–20dp; buttons min 48–52dp height; filters min 48dp target.
- Serif/editorial headline for page title and named plant; clear sans for actions and statuses. Avoid multiple giant serif headings.
- Imagery: actual plant photos, object-fit crop; fixed aspect ratio (square cards, hero 4:3/3:4); no fake checkerboard. Missing image uses calm placeholder with informative alt.
- On 320dp: single-column, scrollable compact filters; 390/430dp: shop grid 2 cols; tablet: bounded content width. Never giant spacer above first task.
- Native subtle haptics/motion at completed task, reduced-motion fallback. No animation just for decoration.
- Bottom nav unchanged: Today / Plants / Create (+) / Shop / Circle. Library or Fertilizers do not become sixth root.

## Screen 01 — Today
Contextual greeting/time + small notifications/account; no redundant global brand block in nested detail.
Above fold: “Today” + real due count; if due, one featured card: photo, plant nickname, action title (e.g. “Check the soil”), source (“Approved inspection” / “Your check-in”), due date and “Check now”. More due rows compact, Upcoming secondary.
No tasks: “Nothing needs checking right now” + “View my plants”, not a fabricated healthy dashboard.
Tap routes to exact plant/task without duplicating Today inside a nested screen. Order actual overdue/due then upcoming.

## Screen 02 — My Plants
Real PlantUnit list grouped by Space with privacy roles. Row: photo/nickname, identity state staff-verified / user-selected / unknown, context-aware due, optional last event. Do not display “healthy”, “thirsty” or “needs fertilizer” without actual evidence.
CTA “Add a plant”; received inventory via existing Handover. Archived plants discoverable as read-only private history.

## Screen 03 — Create > Add Plant > Select type
Title “Which plant do you have?” Immediate search scientific/common/Arabic aliases with distinct visual results. Each item accepted name + aliases + reviewed guide availability + rights-cleared thumbnail.
No result: “I can't find it” → Unknown species. Ambiguous alias: show alternatives, never guess a verified identity.
On choice: “You selected this species — Plantupia has not verified your individual plant.” Do not block creating plant.

## Screen 04 — Create > Add Plant > Make it yours
Title “Give your plant a place.” Fields personal name REQUIRED, Space REQUIRED, placement OPTIONAL, personal photo OPTIONAL with preview/replace.
Progressive “More context” collapsible for indoor/outdoor, light, pot/drainage, soil only where needed. No mandatory ten-field form.
Primary “Add plant”; prevent duplicate submit, retain draft on error, respect keyboard/IME and font scaling. Shared view-only space shows why creation isn't allowed.

## Screen 05 — Added / first visit
Photo + nickname + “Added to My Plants.”
If curated type chosen: “Based on your selection” and link “Read reviewed general care.” If staff-verified: “Identity verified by Plantupia.” Unknown: “Species not confirmed; you can still keep care history.”
Optional “Choose a check-in date,” no automatic phone push. Default CTA “Go to plant”.
Handover-installed plants skip create; original PlantUnit appears under recipient after authenticated claim.

## Screen 06 — Plant Detail
Compact back header: nickname, actual photo, placement, trust badge. First viewport “Next check” only if persisted; primary “Record care” or “Check today” contextual to real task.
Care Guide cards: Water, Light, Soil, Humidity, Fertilizing, Pruning, Repotting (only nonempty approved sections), each with short excerpt, details and source.
For self-selected plant show “General information for the type you chose; this plant's identity isn't verified.” Old published narrative appears as clearly labeled fallback. Unavailable guide: “No reviewed guide yet” and non-prescriptive observation options.
Separate actual Living History, staff/AI help and Circle sharing, not repetitive blocks.

## Screen 07 — Quick Inspection
Opened from due task: title “Check soil” or reviewed action, not a generic six-icon maintenance form.
Question + short structured answers e.g. “Dry / Moist / Not sure”. Optional photo and note. One primary “Save check” → real observation and task closure; show next due only if server has one.
If the user actually watered, secondary explicit “I watered this plant” creates a Watered action; never infer from “Dry,” due date or tap on inspection. Same for Fertilized.
“Not now” dismisses (does not skip). “Skip this check” is explicit secondary; “Stop plan” under Manage schedule confirmation.
Saved message “Check saved to Living History”; never speculative healthy banner.

## Screen 08 — Record Care (secondary)
Existing actions remain Watered, Checked soil, Pruned, Moved, Fertilized, Repotted, but compact 2-column choice sheet with clear one Save button. Optional photo/note. Observation and Concern are separate modes, not three duplicate Save buttons in one form.
Next check date optional, optional Remind me only with user consent. Preserve PlantEvent semantics, retry idempotency and private history.

## Screen 09 — Care Schedule / Reminders
Single authoritative schedule view: company-reviewed recurring inspections + user-chosen personal check-ins, with source labels, due, status.
Per item actions Change date / Skip / Disable. Do not auto-enable push. Explain: “We'll remind you to check, not automatically to water.”
Tasks readable in app even if OS notification permission denied or delayed. Correct plant/task deep-links after warm/cold start, membership check and account switch. Server remains source of truth, Android delivery inexact.

## Screen 10 — Shop Home
“Plants & essentials” plus compact cart count and My Orders in header, not giant repeated tabs. Search + All / Plants / Pots / Care; Care subfilters Tools / Soil & Substrates / Plant Nutrition.
Products grouped by family with SKU/variant chosen on detail. Cards have rights-cleared real images, small type label, title, QA price and one “View details” link. 2-column when legible.
QA/no-charge limits remain visible but not walls of repeated legal copy. Cover empty/loading/offline/error/out-of-stock states.

## Screen 11 — Fertilizer Product Detail
Real product image, company-authorized brand/model, size, variants, price/availability and genuine manufacturer label including formulation, precautions and use; primary Add to cart.
No fabricated dose, “works for every plant,” or automatic care schedule. Optional curated relationship to plant category only after staff review with source.
Cart, Review QA Order, My Orders and Detail reuse existing real QA backend; no payment or delivery introduced.

## Screen 12 — Company Care Library and Approval
/Company/care search table species, published/draft guides, source, reviewer, last review, aliases and photo rights. This is evidence workspace, not an unstructured massive form.
Species editor canonical name/aliases/photo provenance; guide editor topic cards/source links; review → publish immutable version or withdraw. No automatic 100-entry publication.
Approve inspection templates against actual species + guide version and real placement; staff Shop Nutrition editor maintains manufacturer evidence separately. Audit identity changes and reviewer activity.

## Screen 13 — Purchase > Verified Handover
QA order detail remains request, not “your new plant”. Staff reviews actual inventory, prepares a DRAFT/READY installation, recipient-bound QR only from approved path. Customer previews, explicitly claims and gets the existing individual PlantUnit(s), not product records or duplicated plants. Retail ordering and a personal plant journal stay separate until verified handover.

## Interaction copy and identity semantics
- “You selected this species — it isn't verified by Plantupia yet.” vs “Verified by Plantupia” only after staff review.
- “Check soil” vs “I watered”; “What did you notice?”; “Dry / Moist / Not sure”; “Save check”.
- “Remind me to check this plant” only opt-in; “Notifications are off; your schedule still appears here.”
- “I don't know the species”; “You can add it and keep a history.”
- “Reviewed care guide · vN” with source/reviewer/date, or “No reviewed guide available.”
- Fertilizer label is manufacturer data; no dosing outside approved evidence.
- QA orders say “Preview only · no payment or delivery” until commercial acceptance.

## Compose component map (suggested; replace rather than duplicate old sheets)
PlantupiaSpeciesPicker; PlantupiaAddPlantFlow (migrate SelfServiceForms); PlantupiaIdentityBadge; PlantupiaCareGuideSections; PlantupiaDueTaskCard; PlantupiaQuickCheckSheet; PlantupiaCareActionSheet; PlantupiaReminderSettings; PlantupiaShopNutritionFilters; CompanySpeciesReviewer.
Keep account/state restoration safe; no double API fetch each root switch.

## Acceptance storyboard
1. Existing owner adds known species, remains user_selected, no push scheduled.
2. Unknown species can be added/photo/history; no fake reviewed guide.
3. Two plants same species in different contexts keep separate history/due.
4. Soil “Moist” + optional photo completes actual observed check; no Watered recorded.
5. “I watered” separately creates one explicit care event.
6. Consent/permission denied/delayed/canceled reminder still leaves truthful in-app schedule, no cross-user leak.
7. Care > Plant Nutrition, real SKU/gallery/cart/QA order; checkout never auto-creates personal plant.
8. Staff inventory review + recipient QR claim reveals original plant only after claim.
9. Validate 320/390/430dp/tablet, 200% text, TalkBack, image fallback, offline retry, no bottom nav obscuring content.

**Freeze condition:** every screen maps to real model and API, has one primary action, explicit identity/trust state, finite empty/error/loading states, and documented permission + account changes. None of these unimplemented views are claimed as live.
