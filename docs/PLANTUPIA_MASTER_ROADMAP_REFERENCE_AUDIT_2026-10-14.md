# PLANTUPIA — Master Roadmap, Research, References & Native Product Audit
**Date:** 2026-10-14
**Scope:** Original research repository and interactive design studies versus stacked Nova → Plantupia Android native implementation.
**Verdict:** **C0 MOSTLY CODED, NOT END-TO-END VERIFIED. C1 PARTIAL. B1 COMPANY-GATED. R&D IS NOT A SHIP COMMITMENT.**
**Review cadence:** Continue internal engineering/CI while building; user visual sign-off only at the final integrated QA APK. Do not merge/deploy to Oracle Production/Play without explicit approval.

> **Later execution source (2026-10-15):** [Finite Integrated Delivery Roadmap](./PLANTUPIA_FINITE_INTEGRATED_DELIVERY_ROADMAP_2026-10-15.md) reconciles the complete research history, Post-Pass-07 Native work and bounded G0–G6 completion gates. This 2026-10-14 document is retained as a dated audit, not a permanently updated sign-off. Later work includes reversible **plant** archive, but not space archive/replacement.

## 1. Source truth, not wishful product scope

| Source | Evidence inspected | What it establishes |
| --- | --- | --- |
| Plantupia research [Working Law](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/WORKING_LAW.md), [Source Atlases](https://github.com/omarkhair70-droid/Plantupia/tree/research/foundation-source-atlas-20261007/docs/research), [Meaning Extraction](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/synthesis/01-meaning-extraction.md) | plant × place × human × care × time × evidence | Product philosophy, **not** permission to fake real client/plant/sensor facts |
| [Master Product Spec](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/MASTER_PRODUCT_SPEC_V01_2026-10-12.md), [Capability Atlas](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/MASTER_CAPABILITY_ATLAS_V0_2026-10-12.md) | C0, C1, B1, R&D classification; six acceptance journeys | A feature being considered is not an approved V1 obligation |
| [IA/Screen Contract](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/INFORMATION_ARCHITECTURE_V01_2026-10-12.md), [Screen Flow Atlas](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/SCREEN_FLOW_ATLAS_V0_2026-10-12.md) | real user tasks and state behavior | Logical screen != required separate route |
| [Visual Pass 02](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/prototypes/nova-integrated-visual-pass-02/index.html), [Review 03](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/prototypes/full-product-review-03/index.html), [Design Critique](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/FULL_PRODUCT_VISUAL_REVIEW_03_2026-10-12.md) | interactive simulated art direction, navigation candidates A/B, whole social/media relationship | **Not** Compose screens, official photographic media, or signed-off A/B winner |
| [Nova Integration Contract](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/NOVA_INTEGRATION_VISUAL_CONTRACT_V01_2026-10-12.md), [Native ADR](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/architecture/ADR_0001_NATIVE_REUSE_AND_PRODUCT_APP_STRATEGY_2026-10-12.md) | reuse Kotlin/Compose, Navigation3, Media3, Coil, WebRTC, Django/Nova; native interactions | Neither WebView nor a rewrite in Flutter |
| [Nova PR #240](https://github.com/omarkhair-labs/nova/pull/240), [Native PR #241](https://github.com/omarkhair-labs/nova/pull/241) | actual implementation on stacked draft branches | Green CI can confirm compilable/tested code; **not** deployed QA routing or on-device polish |
| [Plantupia Brand Working Contract](./PLANTUPIA_PRODUCT_CONVERSION.md) | proposed Deep Green #123B2E, Cream #F3EEDE, Sage #9FB79A; desired Playfair Display headings/Inter UI; “Transforming Spaces. Inspiring People.” | Official logo/fonts/real imagery are not yet approved assets in runtime |
| [Screen Closure Ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md) | latest per-screen engineering state | Maintains CODED vs OWNER ACCEPTED distinction |

Private founder briefs, customer photos, contracts and operational SOPs are **not guaranteed present** in either public repository. Do not claim to have audited such non-retrieved materials or expose them in this public document. New confirmed stakeholder decision on **English-first** supersedes older Arabic-first browser copy; user-authored notes and names remain untranslated.

## 2. The seven original principles as actual product tests

| Original meaning | What survives natively | Unclosed design/product proof |
| --- | --- | --- |
| **Relational Life** | authorized account → space → plant; name/species/placement | edit placement/species, spatial light/context, room/zone semantics without pretending to sense values |
| **Living Time** | real event timestamps, dated photos/check-ins and outcomes | physically test long history, timezone/date truth, meaningful photographic progress |
| **Retained History** | private care/events and linked outcomes; photo comparison; recently added filters for All/Care/Notes/Concerns/Outcomes/Photos | long paginated history, accessibility, actor attribution and complete back behavior |
| **Care Creates Relationship** | humans choose actual care actions, next review and consented reminder | compact intuitive icon-led action composer; reschedule/skip/pause, not coercive daily alerts |
| **Living Agency** | no fabricated health percent, soil sensor telemetry or enforced watering schedules | better contextual expert knowledge with uncertainty and observation-first guidance |
| **Legibility** | selective Today attention + one focused space, genuine photo hero, optional collapsed social feed | screen density and text walls still need on-device visual challenge, attention versus newsfeed |
| **Accountable Living** | server-side roles/private records, explicit public sharing consent, care outcome tied to event | two-account device QA, image/object ACLs and approved company workflow provenance |

## 3. Roadmap reconciling C0 / C1 / B1 (current branch, no implicit sign-off)

| User journey / capability | Observed engineering evidence | Status | Release acceptance still needed |
| --- | --- | --- | --- |
| **J1 owner → own space → 1 plant → photo → care → return** | `PlantupiaSelfServiceForms`, `PlantupiaClientSection`, Django `backend/plants` | **CODED, C0 unverified** | clean registered user on isolated backend; create-photo-upload/reopen and crash/offline |
| Multiple owner spaces and shared viewer | `PlantupiaSpaceSwitcher`, permission-filtered Django, read-only flags | **CODED, C0 unverified** | 1, 7, 100+ plants, multi-account and revoked membership on device |
| Current attention and reminders | `PlantupiaTodayPanel`, check-in dates, WorkManager opt-in tasks | **CODED, PARTIAL C1** | Android 13+ permission/doze, stale account events, timezone/boot, postpone and reschedule |
| Plant care + issues + outcomes | `PlantupiaCareActions`, `PlantupiaHistoryTimeline`, server PlantEvent | **CODED, C0 unverified** | check actual timestamp/actor, duplicate tap, media failure, full paging |
| Living history / photo compare | actual event photos, oldest/last displayed, event/outcome link and history filter | **CODED, PARTIAL C1** | avoid mistaking **oldest loaded** for oldest ever; actual image detail/transition and long histories |
| **J2 real AI assistant** | `PlantupiaAssistantSheet`, Django `assistant.py` OpenAI/Groq adapters, opt-in context, request history and cap | **CODED BUT NOT LIVE-VERIFIED** | isolated QA Redis/model key/account; real 2-account request; botanical response/source/uncertainty eval; no fake answers |
| **J3 plant concern → preview → public Circle → replies → private result** | `PlantupiaCommunityPanels`, existing Nova posting and comments; EXIF strip tests | **HYBRID CODED, C0 unverified** | device public-payload preview, moderation and real account separation, media and error rollback |
| User posts, Stories, Reels and Inbox | original Nova social/media/messaging engines reachable from Circle/Create/Profile | **NOVA-INHERITED, UX audit open** | actual user-visible journey, identity coherence, permissions, performance, content safety |
| English-first UI | core Plantupia screens and root navigation translated; CI guard for selected modules | **PARTIAL C1, NOT ALL-APP CERTIFIED** | all inherited Nova routes, errors/API/legal/privacy, talkback, dates and mixed-language user data on real device |
| App theming/iconography/motion | brand colors, Compose token layer, Canvas care icons, original Nova animated/media patterns | **PARTIAL VISUAL** | official type/logo/license and real plant photo art direction; device parity and jank |
| Space/plant edit, move, replace/archive | newly added role-scoped shared plant name/species/placement PATCH with private history event; other lifecycle actions still absent | **PARTIAL C1 (new code awaits CI)** | space edit/archive, plant retirement/replacement, broader role UX and device QA |
| Sourced plant guides/species discovery | no approved licensed corpus or specialist species integration | **GAP C1** | source review, licensing and uncertainty; no fabricated generic catalog |
| Household/helper management UI | backend membership concept and authorization exist | **GAP C1 UI** | invite/revoke and handoff with auditable permissions; potential danger if shipped incomplete |
| Company-installed site / real team handoff | research hypotheses; generic Django Admin | **B1 GATED** | actual Plantupia service responsibilities, photos, rights, SLA, approved roles and user journeys |
| Physical sensors, IoT dashboards, self-watering commands | historical Living Object / FYTA exploration only | **EXCLUDED** | do not ship without new verified company capability/decision |
| Public Plantupia marketing website | separate research/creative website deliverable | **SEPARATE PRODUCT** | cannot be marked done because Android builds |

**Bottom line:** original roadmap is **not** complete; C0 can only be declared closed after the six source-spec journeys are demonstrated against a real isolated backend and QA APK. C1 must be tracked as work, not quietly counted as “done”; B1 is contingent on company facts.

## 4. Comparison of actual research visuals vs implemented native design

| Element | Study 02 / Study 03 intent | Native branch now | Creative/engineering disposition |
| --- | --- | --- | --- |
| **Hero** | editorial botanical imagery gives an inhabited spatial identity | actual uploaded plant cover photo if available; generic fallback icon otherwise | **KEEP PHOTO-FIRST**, refine composition and empty/no-image case, never fake installations |
| **Information hierarchy** | meaningful visual plant → attention → next care, not scrolling paragraphs | care moved forward, Today shown, private history later; many help paragraphs remain | **REFINE:** concise microcopy, fewer repeated explanations, labels on action and disclosure |
| **Navigation** | Study03 A = My Space / Circle / visible Create / Me; B = dedicated Care tab + visible peeking Create sheet | **A implemented**, no measured winner | **UNRESOLVED:** compare task times for check-in, add-plant, Story and DM on final device; do not silently add hidden swipe controls |
| **Care actions** | illustrated, tactile icons with one clear action | Compose Canvas botanical actions, actual Django writes | **KEEP**, test touch targets >=48dp, selection feedback, reduced motion |
| **Private/public boundary** | explicit distinct compositional lanes | genuine private event vs preview+consent public Post | **KEEP**, verify accessible disclosure and real media sanitation |
| **Living time** | photographic narrative/dated transition, not an activity feed | dated photos + linked outcomes + new filter options | **REFINE** stronger rhythm/photo sizes and history density on small screens |
| **Circle** | questions from real plants first, broad media secondary | Q&A + People/Moments; Nova feed retained | **KEEP HYBRID**, test question discovery, replies, reporting and post separation |
| **Stories/Reels** | fast native playback and authentic photo/video interaction, distinct from private history | original Nova media engine, not guaranteed plant-linked | **KEEP ENGINE**, don't claim Stories auto-link into plant history |
| **Inbox** | real human-to-human chat accessible without dominating bottom nav | original Nova messages and overlay | **KEEP**, verify open-from-Circle/Profile, keyboard/back/notifications |
| **AI** | contextual investigation, source reasoning, consent, potential later photo input | real server provider adapters but no live secret/vision/source corpus | **C0 ACTIVATE & VALIDATE**; FIELD-1 vision/search/human service only after fact/source/privacy gate |
| **Brand** | natural warm, non-glossy, tactile, asymmetrical and real photography; coherent green/cream contrast | brand token + Nova inherited typographic/icon vocabulary | **REFINE**, replace prototype's illustrative demo plants with real authorized photos, seek official logo/type |
| **English** | older browser demos Arabic RTL | new stakeholder wants all user-facing app chrome English LTR | **DECISION SUPERSEDED**, English while user content remains multilingual |

**Not a pixel comparison:** the HTML studies are scripted local illustrations, and this code audit cannot prove animation, visual balance, status-bar contrast or media behavior in a current actual APK without captures on one device. Keep Study 03 review toolbar **out of the Android product**.

## 5. Industry benchmarks — patterns to borrow selectively, not branding

| Verified reference | Transferable strength | Why Plantupia must not copy literally |
| --- | --- | --- |
| [Planta](https://getplanta.com/) | strong attention-to-care, reminder hierarchy, plant journal/photo history | their adaptive algorithms/light/identification claims are not Plantupia capabilities; no invented moisture/auto water decisions |
| [Greg](https://greg.app/support/), [Greg Community](https://greg.app/community/) | living plant + owner + community Q&A; return to plant after receiving advice | do not replace private plant home with a generic social feed; Greg's personalization is not evidence Plantupia can promise it |
| [Planti](https://planti.app/features/) | grouped spaces/rooms and practical plant records with offline aspirations | do not imply full offline sync, expert identification or species database exists in our branch |
| [Naava](https://www.naava.io/en/green-walls) | real green installations represented as ongoing spatial/service experience | do not copy wall-tech/sensor or service plans into Plantupia unless company validates it |
| [RHS houseplant guidance](https://www.rhs.org.uk/plants/types/houseplants/growing-guide) | examine dryness/environment before watering; useful source for eventual curated guide | licensed/source-attributed advice is not the same as AI making a verified diagnosis |
| [Android Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) | minimum accessible touch target 48dp, semantic components | test actual screen readers, font scaling and smaller devices; visual code alone isn't accessibility proof |

## 6. Risk register — prioritized

- **P0 / RELEASE STOP:** isolated QA Django + PostgreSQL + Redis + media with routes deployed and permission-correct; in an earlier user device screenshot the Plantupia endpoint returned HTML 404. Green branch-only CI is not network deployment.
- **P0 / RELEASE STOP:** no signed-off real AI response (a Free Plan API key is needed securely on QA; do not commit secrets). Third-party processing and quota/retention need review.
- **P0 / RELEASE STOP:** private-space/photo and public-post boundary on two real accounts; error handling and revoked membership.
- **P0 / RELEASE STOP:** real Android app device workflow J1–J5, Stories/Inbox route, notifications, crash/ANR, file upload, login/expiry, accessibility.
- **P0 / RELEASE STOP:** app package/listing/company IP and approved distribution strategy; Nova original Play package/signing/Oracle production are protected.
- **P1 / PRODUCT:** reduce lengthy text-led Home and plant detail, confirm nav Study03 A vs B via simple timed tasks in final review, verify meaningful care context, correctly classify old loaded photos.
- **P1 / PRODUCT:** plant metadata edits/retire, space settings and account-safe editable intent, helper roles only with proper access UX.
- **P1 / CONTENT:** approved type/logo/photo rights, accurate help/legal/email policies, any species-care guide source licensing.
- **P2 / AFTER CORE:** contextual imaging AI, curated botanical database, deeper multi-user scheduling/delegation, professional client service only when reality proven.

## 7. Full acceptance routes to finish, without pausing owner for each screen

1. **J1 — New owner with zero spaces:** sign up, create space/plant, upload real botanical photo, record an observation/care, schedule an optional check-in, reopen the app, verify persisted truth.
2. **J2 — Concern, human check and real AI:** plant name/species/note, context scope opt-in/out, correct server authorization, useful model response + uncertainty, no automatic DB writes.
3. **J3 — Private to public:** only selected image/text preview, explicit publish, other account sees public post but never private space/history; collect replies and save optional private result.
4. **J4 — Scaling:** 0/1/7/100+ plant cases, search/switching, paginated events, compare real dated images and filters.
5. **J5 — Shared roles/privacy:** owner/manager/carer/viewer/stranger, guessed UUID fails, revoked roles, cross-account reminders clear.
6. **Media/social smoke:** Create → Story/video → viewer → DM → return, notifications, app background, error/retry and media permission cases.
7. **English/brand/accessibility:** all major/secondary routes, English text and expected LTR in chrome; mixed RTL authored notes; 360/390/430 dp devices, dynamic fonts, TalkBack, photo zoom, 48dp tap areas, reduced motion.
8. **Operational:** actual isolated QA build metadata/signature, no Production side effects, CI green on exact final commit, export screenshots/videos from device, review entire integrated experience with owner only at end.
9. **B1 separate:** founder validates whether installations are app-visible, who owns plant care, what services exist, and what client imagery/terms may be used. Until then **DO NOT** claim B1 delivered.

## 8. Execution decision

1. Keep stacked PR #240/#241, Kotlin/Compose, working Nova technical engines and current English-first direction. Avoid premature repository/platform migration or Play deployment.
2. Implement inexpensive truthful C0/C1 improvements continuously and test them. Example this pass: **Living History filters** (All/Care/Notes/Concerns/Outcomes/Photos) without hiding paginated results or faking imagery. Code and JVM tests reside in `PlantupiaHistoryTimeline.kt` / `PlantupiaClientSection.kt`.
3. Before claiming **finished**, clear the P0 release stops above; until then “coded” remains distinct from “works in installed live Plantupia QA.”
4. Keep optional research worlds (living furniture, sensor objects, materials) as possible strategic R&D. They aren't overdue Android features.

**Official signoff state as of audit creation: NOT FINAL / NOT READY TO MERGE OR PUBLISH.**

## New C1 implementation checkpoint (2026-10-14)

The working branch now includes a **human-verified shared plant context editor** (shared label, species label and placement) separate from the personal nickname. New Django `PATCH /plantupia/plants/{uuid}/` is scoped to the space owner or manager, with authorization returning 404 for unauthorized users and a private dated observation noting real changes. The original private plant history is never overwritten. Django permission tests cover owner, manager, carer/viewer/stranger, no-op update and invalid blank label. **Code only; Android/device/QA proof still pending**. Space edit/archive, plant retirement/replacement and company service controls remain out of scope until explicitly developed/approved.
