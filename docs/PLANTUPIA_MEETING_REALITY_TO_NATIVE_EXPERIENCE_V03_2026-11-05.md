# Plantupia / After the meeting — Native Experience V03
**Date:** 2026-11-05
**Target:** Android Kotlin/Jetpack Compose + existing Django/Company backend.
**Base:** Draft #253. **New design implementation:** Draft #269. No production promotion.

## A. Meeting truth vs what the last APK actually showed

**The APK is NOT the finished vision.** It contains real APIs, auth, plant states, staff-reviewed content contracts, history, QA Shop and reminders, but many screens still feel like independent panels. Shipping a feature is not the same as designing the moment where the user understands, acts and comes back.

Source of scope: product design master, original 13-screen V2 blueprint dated 2026-11-03, user's meeting feedback, review of actual Compose source. This document is an implementation audit, not claim of client sign-off.

### Navigation decision — supersedes the older five-tab line in V02
**Primary:** Today / Plants / Create / Shop / AI.
**Contextual:** Circle / Inbox / Notifications / Profile / Orders / Team.
- Circle is NOT deleted or replaced in the backend. It stays a full real public/community route accessible via Today (community shelf), Create (Explore Circle), AI (Prefer people?), and plant-specific "Ask the Circle" composer. Existing posts, stories and people graph must survive.
- AI is a real full-screen destination using the existing authorized assistant endpoint, source/uncertainty display and per-question consent. Not an inaccessible duplicate sheet and not an imagined diagnostic capability. Plant Detail still has contextual Ask AI for *this* plant.
- Create is a purposeful *choice of intent*: Record privately, Add Plant, Ask Circle publicly, Share, Story. Never silently publish a private care photo.
- Me/profile remains through a top contextual user identity; avoid 5 separate giant repeat logos. Inbox belongs to authenticated social; team support belongs to a plant/ticket.

### What was missed (and should not be described as done)
1. Care Guide: back end has source-reviewed topics, but old front end repeated same sage accordion. Needs topic palette, original pictograms, responsive editorial hierarchy, tactile selection, full-source detail and distinctive unpublished / legacy state. Native first slice in #269; company must still publish real topics.
2. Today: actual saved check-ins and approved plans exist, but their priority/order and clean focus need another per-device pass; Circle must be available without taking a tab; due vs upcoming vs genuinely empty is not fabricated.
3. Plant Detail: too many sections and long forms in one scroll; create logical Overview / Care / Living History hierarchy, obvious Record care, quiet guide entrance, concise state; retain actual photos, identity verification label, private events and team support.
4. Quick inspection: typed findings are real, but same-inspection camera evidence still not integrated into one event; never infer watered/fertilized from "dry".
5. Notifications: scheduled per approved plan with opt-in and account checks, but cold-start/permission/restart/locale behavior requires physical phone QA; OS WorkManager is approximate, not an exact timer.
6. Add Plant: validated species chooser now exists, but type-selection pace, chosen-name identity and first-plant celebration still need deliberate transitions and real photography.
7. Shop: Care > Nutrition exists with manufacturer sources; presentation/photography/variant navigation and truthful empty/sold-out/basket states need art direction. Existing Checkout is QA, no payment or shipping.
8. Circle: old social UI remains technically reachable; visual continuity with care/AI and intentional sharing boundaries need a redesign, not a new social backend.
9. Company portal: source editor and approvals are functional but user-facing botanical source content/imagery has not been supplied/approved. Review workflow must not become auto-generated care.
10. Profile/Inbox/activity: preserve Nova's working systems but avoid leaking internal/notification surfaces into the five primary tabs; keep consistent semantics and avatar controls.
11. Intro/QR/Handover: physical inventory and recipient consent remain necessary; Shop taxonomy is never individual plant verification.

## B. Exact mobile composition contracts

| Screen | First 8 seconds | One dominant action | Secondary | Distinctive treatment |
|---|---|---|---|---|
| Today | Which *saved* checks actually need attention? | Open due check / Record care | Circle, All plants | Calm editorial brief and real photo; no false "100% healthy" |
| Plants | Which space / actual plant? | Open plant | Add, connect delivery | Photo-led, space-first, no stock avatars |
| Add Plant / Type | Do you know the name? | Choose a curated type or Unknown | Back | Search/results with clarity "user selected" |
| Add Plant / Details | Where is *this* plant? | Save plant | Optional photo | Short progressive form with no invented defaults |
| First Visit | The plant has a permanent private record | View plant | Set personal check-in | Real plant identity not a confetti claim of verification |
| Plant Overview | Which physical plant? What happened last? | Record care | Read guide, Ask AI | Large real evidence photo, editorial name and clear trust label |
| Care Guide | What should I learn in context? | Open one reviewed chapter | Source link | Topic color family and hand-drawn art, gentle open animation |
| Quick Inspection | What did you *see*? | Save observation | Skip/schedule | Low-friction choices, never "watered" from a soil check |
| Record Care | What did you *do*? | Save action | Photo/note/date | One action per tap, editing extras hidden initially |
| Reminders | What did I *choose* to revisit? | Opt in / amend | Disable | OS permission without coercion, dated plan from source |
| AI | What's the user unsure about? | Ask a question | Plant context with consent, Circle | New full-height conversation and original calm signal art |
| Shop | Which real catalog item? | Open SKU / Add to cart | Category | Photography/label, no fake stock or personalized fertilizer claim |
| Care Nutrition | What does manufacturer actually say? | Inspect source-backed label | Add to cart | Warm amber/slate editorial information, honest cautions |
| Circle | Do I need a human experience? | Ask or explore question | People / Moments | Contextual route, no accidental plant-data share |
| QR Handover | Is this a real company-verified installation? | Confirm authorized claim | Preview | Exact individual plant record after staff approval |
| Company Guide | Can staff document evidence? | Review/publish new version | Alias/editor | Immutable approval and source provenance |

## C. Visual / motion discipline (native Compose, not animation for its own sake)

- **Material direction:** warm parchment canvas, forest green actions, botanical sage context, gentle material/paper grain only when real brand assets permit. Color belongs to the actual content type, not random accents.
- **Care palette:** Water mineral blue; Light warm sun yellow; Soil clay; Humidity soft sea glass; Nutrition olive; Pruning muted orchid; Repotting terra; Observation muted green; Cautions warm coral. Every chapter has its own Canvas icon and label. Accessibility must not rely on color alone.
- **Motion:** 180–320ms route/chapter transitions, 120–200ms press/focus; one-off settle, not perpetual bouncing plants. Honor device animation-scale settings; no animation for loading success before API acknowledgment. Use screen-level semantics/48dp taps.
- **Image truth:** only real user/company plant photos can represent actual plants, spaces or products. Original flat icons may illustrate concepts/empty states, but may not impersonate observed leaves, diagnosed pests or scanned sensor data.
- **AI truth:** conversational provider is live only when backend entitlement/provider is available; no fabricated health confidence or camera diagnosis. Context opt-in resets per question. Human Circle stays separated.
- **Data/motion:** don't celebrate a care action until server persisted a PlantEvent; don't show "water now" because a local date elapsed; only show genuine due-checks or contextual assessment.
- **Responsive:** 320dp, 360dp, 390dp, 430dp, large text 1.3×/2×, tablets and keyboard. Topic shelf becomes a single vertical list when width/text demands.
- **Accessibility:** TalkBack names action and state; pictograms decorative; state uses words not color only; Focus during sheets; no hidden source links behind gestures.

## D. Engineering execution, in reviewable slices
1. **Now — PR #269:** New fifth AI tab and actual full-screen conversation; Circle contextual with Today/Create/AI access; care chapter visual component family; original AI intro; copy in Today for grounded check-ins. Unit tests and CI.
2. **Next:** Plant Detail information architecture: Overview/Care/History with sections brought into view; a single visible Record action; consistent photo hierarchy and review source entry.
3. **Then:** true same-event photo upload for structured inspections (backend mutation, storage and idempotency), enhanced Today priority and completed states.
4. **Then:** Shop, Create, Circle and Handover screen-specific transitions/layout and real company media.
5. **Finally:** physical-phone E2E and art direction QA; only promote after user sees and signs off actual screens.

## E. Release truth
GitHub CI passing means code compiles/tests. It does not prove the visual design or the hosted #253 backend is ready. #269 stays stacked Draft until device approval. Do not modify Nova master or production. Don't merge new visual decisions directly into already deployed QA just to make a screenshot.
