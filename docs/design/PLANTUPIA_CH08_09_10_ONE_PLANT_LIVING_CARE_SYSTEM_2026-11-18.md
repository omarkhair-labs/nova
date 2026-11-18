# Plantupia / One Plant — Chapters 08 × 09 × 10
**Integrated Product Architecture · UX Research · Native Storyboard · Backend & Company Boundary · 18 Nov 2026**

**State:** Engineering audit and first native QA implementation candidate. **NOT final art approval, not production, not a three-screen demo.** This document references the real `omarkhair-labs/nova` repo on PR #323 as parent; see PR #324 for the working branch. Preserve the source contract and historical nuance of `docs/PLANTUPIA_BOTANICAL_LIBRARY_CARE_EXPERIENCE_CONTRACT_V02_2026-11-03.md`, `docs/PLANTUPIA_V2_NATIVE_SCREEN_BLUEPRINTS_2026-11-03.md`, and independent exploratory audit `docs/design/PLANTUPIA_SHOP_CARE_SUBMISSION_REVIEW_AR_2026-11-17.md` from PR #315. Their *principles* are the basis; an unapproved visual direction never silently becomes final.

## 0. Why these are one chapter system, not 3 feature tickets

For an actual PlantUnit, one human needs to answer in sequence:
1. **Who is this plant?** Its own true photo, nickname/space, verified vs self-selected vs unknown botanical identity, and real next documented check, if any.
2. **What do I need to know or do?** A published, versioned *general* guide qualified by identity and context; a human's explicit observation/check/action. Opening a guide is not doing care, and due does not mean water/fertilize.
3. **What happened over time?** Real dated event, outcome attached only to the actual source event, optional uploaded photo, correct author/evidence/permission and loaded-page pagination.

All three need the same plant identity, current account, permissions and history. Avoid a new `PlantDetailV2` data store, second care database, imaginary garden health scores or parallel reminder jobs. Three focused views are **navigation modes within one plant context**; not three independent tab hierarchies or gigantic accordions.

## 1. Code-verified AS-IS audit (18 Nov)

| Area | Verified code | Friction or protected truth |
|---|---|---|
| Plant detail | `PlantupiaClientSection.kt` ~2,165 lines; selected plant, history page, pending check-ins, care composer, editor, settings, human support, community and archive in one composable | Too much visible vertical content. The current "Overview / Care / History" strip merely scrolls to anchors; its selected state can lie as people scroll. Plant identity, composer, guide, project admin, check-ins and history all share one long page. Must not break old intents, Back or saves. |
| Quick action | `saveMoment()` + `PlantupiaCareAction` + optional photo/note/follow-up, `runPlantupiaMutation` and `repository.confirmSubmission` | Authentic backend with idempotent retry slot and history reload. Primary Record Care exists and is reasonably early. Additional care/observation/issue buttons are legitimate distinct *meanings*, though their placement can be simplified further. No fake success before backend ACK. |
| Guide | `PlantupiaCareGuidePanel.kt` and `PlantupiaCareGuideVisual.kt` | Collapsible 92dp entry → nested topic tiles/reading → legacy notes → provenance expansion all **inline** inside detail. This is the accordion maze; not actually a separate route. Guide has published topic selection, version/reviewer/source and self-selected identity disclaimer. Preserve all. |
| History | `PlantupiaHistoryTimeline.kt`, `PlantupiaHistoryDisplayState.kt`, `PlantupiaPhotoIndex.kt` | Already groups outcomes to source events only if both loaded. Keeps photo outcomes separate; distinct empty/loading/unavailable; filters/pagination and genuine photo comparison. Don't alter event semantics, source IDs or fabricate earlier photo. |
| Care schedule | `PlantupiaCareActivity.kt`, `PlantupiaTodayPanel.kt`, personal follow-ups and approved inspections | Separate genuine plans vs user-selected next checks; no automatic Watered inference or guaranteed notification delivery. Personal opt-in local reminder may be late. No needless new schedule database. |
| Backend & Company | `BotanicalSpecies`, `CareGuideVersion`, `PlantUnit`, `PlantEvent`, `CareInspectionPlan` and private access guards | Species library ≠ real owned specimen ≠ commercial SKU. Staff-authored review/publish/withdrawal remains separate from customer's private events. No new API required to simplify a client presentation; check guide/version data contract before new fields. |

### Source-protected product invariants
- A real photo is from authorized existing media; no stock plant image or algorithmic "healthy" state replaces it.
- `staff_verified` identity may be shown as verified; `user_selected` is a self-selection, not individual verification; unknown or guide missing gets honest fallback.
- Only a versioned reviewed guide supplies care claims. "Read Light chapter" never creates a care event.
- `CareEvent.kind`: `care`, `observation`, `issue`, `outcome`. "Check soil" is observation until a human separately confirms Watered.
- `follow_up_on` is an optional chosen check-in, *not* a mandatory watering schedule. Local reminder requires opt-in.
- Different plants of same species have separate identity, history, photos, owner permissions and next checks. A readonly collaborator cannot mutate.
- History failure is **not** a blank notebook; don't erase loaded entries or silently drop parentless outcomes.

## 2. Real comparative mechanism research (not copying competitor artwork)

**Planta — [official website](https://getplanta.com/) / [App Store](https://apps.apple.com/us/app/planta-plant-garden-care/id1410126781):**
Published product focuses on plant-specific schedules, guided care instructions, and progress/journal. Transfer: one recognizable plant object + context-aware current action + longitudinal records. Reject: unreviewed personalized scheduling, weather-derived watering prescriptions, or copied interface.

**Greg — [official support](https://greg.app/support/):**
A plant may be manually watered early, task progress may be recorded. Transfer: a human can record action independently of reminder. Reject: automatically marking "done/watered" from a check-in alone.

**PictureThis — [App Store](https://apps.apple.com/eg/app/picturethis-plant-identifier/id1252497129):**
Plant identification/care info/diagnosis emphasis. Transfer: identification status and advice context must be legible. Reject: Plantupia promising diagnosis, image interpretation, or a verified species without evidence.

**NN/g — [Progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/) and [Workflow expectations](https://www.nngroup.com/articles/workflow-expectations/):**
A screen should reveal the useful first action and defer rarely needed content. Transfer: real tab modes, focused guide reading, secondary settings. No claim from these articles that a specific Plantupia arrangement has been user-tested.

### Three design explorations (prototypes/critique candidates, NOT three production UI forks)
**A · Botanical specimen / field manual:** user-owned specimen photo, species provenance, shallow factual next-check, one Record Care CTA; separate focused guide reading and dated journal. Strength: distinctive identity + trust. Risk: too much pale editorial "museum" material can reduce scan speed. **Working candidate.**

**B · Time-first living diary:** recent changes and next check lead; plant photo smaller. Strength: returning carers review progress quickly. Risk: first-time plant with zero activity looks barren; guide hidden behind timeline.

**C · Task-first care cockpit:** due tasks first, quick recording and dense schedule. Strength: high-frequency operations. Risk: turns a relationship with a plant into an anxious checklist; could imply automatic health recommendations with absent data.

**Working hybrid:** A for Overview + readable contextual care tasks from C + deliberate dated/photographic History from B. Compare screenshots on native QA and revise before final approval.

## 3. TO-BE interaction architecture — one plant, three focused views

### Plant list → Overview (default)
- Back to **same Space scroll position**, not a fresh feed.
- Compact context: actual plant nickname, placement/species as *recorded*, identity trust label; authentic photo fills one intentional object portrait area if available; non-photo fallback intentionally compact, not a fake illustration standing in for an actual picture.
- One primary **Record care** for a plant with permission; if a genuine pending check exists, first task is an explicit **Check / record outcome** (a real inspection, not automatic watering).
- Short real "Next check" only if present; no fake health badge or countdown. A factual last recorded observation may provide a bridge to History.
- Small contextual entries for guide, history, support and plant settings. Don't repeat the same CTA in multiple cards.
- User with readonly access sees honest "View-only" state, guide and history, no active Save.

### Care (focused mode within same plant)
- Compact identity stays visible so a user always knows which specimen receives actions.
- Explicit **Record Care** composer with human-affirmed Watered / Checked soil / Pruned / Moved / Fertilized / Repotted / etc; secondary Note, Concern, Photo, Next check optional.
- Personal due checks or approved inspection are separate tasks with explicit actual observation results. Do not force action logs through a "Guide read" button.
- One **Open field guide** surface for the reviewed material, in a dedicated focused native reader with Back. **No nested expanding article under the plant page**.
- Source/author/reviewed date/caveat within guide reader; show only source-backed topics, legacy full text fallback, unknown/unpublished guidance explanation.
- Human support / Plantupia AI are contextual secondary paths; a help request and a saved observation are distinct persisted records.

### History (focused mode within same plant)
- Date-ordered **actual** care/notes/issues/outcomes, include source event and actor/evidence labels. Existing genuine photos with full-screen view; full history pagination explicit, no implied "first-ever image" on partial pages.
- Empty only after successful history fetch, offline/unavailable retry preserves uncertainty, no invented milestones.
- Pending check-in may appear with genuine source date and explicit "record outcome" or "change date" actions; never mark done on reminder expiry.
- Filter All/Care/Notes/Concerns/Outcomes/Photos; do not drop events merely because their parent isn't among loaded pages. Reuse current grouping helpers.
- Save from Care → only after backend confirmation → History can display the new record; a failed/ambiguous post remains pending for safe retry.

### Navigation / Back / draft preservation
- Tapping Overview / Care / History changes actual visible focus, not an anchor and falsely highlighted tab. Only one main body is composed at a time, with the same remembered plant identity and private data.
- From a Create observation intent → enter the Care mode with composer open. From new plant arrival, "start care" goes to Care. Android Back inside Care/History first returns to Overview; Back from Overview goes to the original Space (preserving scroll). Ask Circle intent remains separate and unchanged.
- Switching modes must **not lose unsaved care notes, selected action, image URI or optional follow-up**. The existing discard confirmation applies on leaving/switching *plants*, not on ordinary mode switch.
- Error banners, pending idempotent retries and permission changes remain obvious across modes, not hidden in whichever section happened to be active.

## 4. Native composition & actual visual work (hypotheses to critique)

**Shape:** Not a pile of identical white cards. One clear specimen portrait; restrained book-like chapter headings; strong tactile Care action; date-led journal entries; visual evidence from real photos; source-aware field reading. Warm paper / dark botanical ink / quiet sage using **existing** `PlantupiaDesignTokens.kt`; no second token library or borrowed vendor screenshots.

**Type:** One editorial name per plant; page tabs functional and accessible rather than large banner headlines; meta and trust source at ~12–14sp minimum adjusted by font scale; never shrink important care directions into 9sp. Small caps only for orientation/provenance.

**Media:** Actual plant photo uses correct aspect/crop scrutiny, original media URL and permissions; make a simple unavailable state when photo missing. Distinct photo/care event comparison uses only saved images. Don't auto-generate a hero photo, health badge or time-lapse from incomplete events.

**Motion storyboard:**
- Open a plant: preserve list context; plant identity appears and details settle. No unverified "growth" animation.
- Switch chapter: immediate content replacement + restrained native state indication (test ~120–200ms, disable/reduce for accessibility); don't scroll an arbitrary giant length and leave selected tab lying.
- Field guide: opens a focused full-height reader with native Back; only then fetch/verify published guide. Topic selection reveals *one* reading, not a grid of nested expanding chapters.
- Save: disable repeated taps; show confirmation only after real server ACK and history refresh. Retry retained on uncertain network.
- Photo: existing zoom viewer and authentic full image; no stock-image interpolation.

**Widths to inspect:** 320, 360, 390, 430dp, tablet, 200% fonts/TalkBack and RTL labels; low-end Android and interrupted connection. Real device screenshots are required for art approval.

## 5. Company Platform / backstage impact

The Care and Support workspace must remain a source/operations platform, **not** a parallel private user's diary:
- Botanical library: curator authors draft sections and source, reviewer approves/publishes, invalidated/retired guide versions do not silently rewrite historical care decisions.
- Care tasks/inspections: only evidence-backed approved inspections and user-selected check-ins. A staff-reviewed plan does not become a Watered event.
- Human support: contacts may happen outside the platform as clarified in Chapter 07. Staff may record the real outcome where it materially affects service; do **not** build a fictional call sync or shared customer health dashboard. No staff access to private events without permission and need.
- B2B installation/QR and B2C orders may lead to an actual individual PlantUnit **only through verified physical inventory + recipient-bound handover**, never merely an SKU or purchase.
- Platform IA from Ch07 (B2B / B2C / Care; shared inbox; action-first) is the shell. The care editor needs a separate future visual/operational audit on real screens, not speculative changes to guide publication models.

## 6. Implementation increments and acceptance (not a premature "done")

**Slice 1 — Safe native architecture:** replace faux scroll-tab selection with a real focused chapter selector. Keep same `PlantupiaClientSection` state/backend. Put guide into one focused reader without nested page maze. Show a truthful overview CTA, and history only within the history mode. Preserve Create intents, read-only access, pending retries, existing history list and check-ins. Add pure state tests and Compose UI tests where feasible.

**Slice 2 — Interaction/design critique:** compare 3 prototypes on screenshots; refine full overview hero / care composer / guide reader / journal density; reduce duplicated actions, move settings/Circle away from primary care. Confirm photo cropping, visual assets, transitions and large-font layout. Keep sources complete.

**Slice 3 — Service truth QA:** explicit interaction replay 1) owner has two plants, 2) guide missing, 3) guide published user-selected/verified, 4) record real care and see history, 5) observation+photo+pending follow-up/outcome, 6) readonly collaborator, 7) archived plant, 8) offline/duplicate mutation, 9) account swap, 10) Company source/version withdrawal.

**Slice 4 — Acceptance:** Android CI + real emulator/device, guide/Company content source review, backend authorization, complete storyboard, no unseen visual regressions. Document chapter 08–10 only as accepted **after** this.

## 7. Decisions / statuses

**Confirmed from previous owner direction:** 08/09/10 must be studied and redesigned together. Each interaction and Company operations also audited; not just restyling. No unapproved external content, automatic prescriptions or fake data. Work on Draft QA branch.

**Candidate pending visual critique:** A/B/C composition, guide full-reader modal vs navigation destination, exact typography and imagery.

**Explicit NOT DONE:** No full-screen pixel-approved screens, device screenshots, real customer research, approved botanical source text, production migration or release. No fabricated water frequency or diagnosis.

**Reference links:** Planta https://getplanta.com/ ; Greg https://greg.app/support/ ; PictureThis https://apps.apple.com/eg/app/picturethis-plant-identifier/id1252497129 ; NN/g progressive disclosure https://www.nngroup.com/articles/progressive-disclosure/ ; NN/g workflow expectations https://www.nngroup.com/articles/workflow-expectations/ .
