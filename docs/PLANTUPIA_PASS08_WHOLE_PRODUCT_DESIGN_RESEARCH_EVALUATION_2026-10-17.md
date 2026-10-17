# PLANTUPIA — PASS 08 / Whole-product visual R&D, competitive re-evaluation and honest feature audit

**Date:** 2026-10-17 · **Owner direction:** Re-examine *all* of Plantupia's appearance and usefulness: identity/profile, editorial photography and authored illustration, icons vs words, typography, motion, social, botanical humanity, actual capabilities and what differs from nearby apps. Read the **existing briefs and award atlases again**. This is research-to-decision, **not a declaration that the Android changes are already implemented, not a new source-atlas reboot, and not client approval**.

**Sources of truth (in priority order)**:
1. The current real-device captures and [second QA audit](./PLANTUPIA_2026-10-17_SECOND_PHONE_VISUAL_QA.md), [first QA audit](./PLANTUPIA_REAL_DEVICE_VISUAL_BRIEF_AUDIT_2026-10-17.md), [Product Bible](./PLANTUPIA_PRODUCT_BIBLE_AND_HANDOFF_V01_2026-10-17.md), [Screen Ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md).
2. Source repository `omarkhair70-droid/Plantupia` branch `research/foundation-source-atlas-20261007`: `docs/research/01-market-reference-landscape.md`, `docs/research/06-award-winning-cross-domain-discovery-pass-04-2026-10-14.md`, `docs/design/07-award-winners-behavioral-teardown-pass-05-2026-10-14.md`, `docs/design/09-pass07-integrated-product-synthesis-contract-2026-10-14.md`, `docs/design/FULL_PRODUCT_VISUAL_REVIEW_03_2026-10-12.md`, `docs/product/MASTER_CAPABILITY_ATLAS_V0_2026-10-12.md`, `docs/product/NOVA_INTEGRATION_VISUAL_CONTRACT_V01_2026-10-12.md`.
3. Current `omarkhair-labs/nova` branch `feature/plantupia-native-experience-20261012`, draft PR #241 stacked onto #240. **Code is evidence of an implementation, not evidence of a good design or live service integration.**
4. Primary external listings, winners and developer documentation, checked October 17, 2026 (links below). An **award** is different from a popular app, finalist or conceptual inspiration.

## 00. Core thesis: the relationship Plantupia actually changes

A typical plant utility asks "What is this plant?" or "When do I water it?" Plantupia's chosen hypothesis instead asks: "**Where does this individual plant live; what did a real person observe and do for it; what changed with time; and how can another person help without owning its private history?**"

Human–plant relationship here means:
- **Context**: real space/office/home/placement gives the living entity somewhere to belong. No invented floor plan.
- **Attention**: user notices what is actually present. The app is not a bot issuing watering orders.
- **Action**: a real person performs an actual task (watered, soil checked, pruned, moved, fertilized, repotted) and may record it.
- **Memory and accountability**: dated authored photo, note, action and return/check-in. Both care and its result stay findable.
- **Learning together**: optional chosen public question → real human reply → separate, later recorded observed outcome. Private placement and care data do not cross the boundary by default.
- **Life beyond the screen**: the user should get back to the actual plant in seconds, not compulsively earn engagement points.

These are **design mechanisms and potential differentiators**. No proof of market uniqueness, reduced plant deaths or improved care outcomes exists yet. The company operations model must be verified before business promises.

## 01. Adjacent products — updated competitive reference decomposition

| Reference | Verifiable capability or reference mechanism | What Plantupia can learn / opportunity | We must not claim |
|---|---|---|---|
| **[Planta](https://play.google.com/store/apps/details?id=com.stromming.planta)** — Google Play listing | Personalized care reminders (watering, feeding, pruning), plant ID, light meter, journal, family-shared task planning, community | Checklists can be fast; inventory can scale; clear active/due states. Our thesis should emphasize **human-authored evidence + place + separately confirmed outcome**, rather than pretend journals/social are unique | "Planta has no journal/community" is **false**. We have no measured care effectiveness advantage. |
| **[Greg](https://play.google.com/store/apps/details?id=greg.io.care_app_android)** — Google Play listing | Plant identification, adaptive watering guidance, active plant community/questions | A rich human social engine does belong in a plant context. Design the question as a **source-linked knowledge object**, not just another social image tile | "We're first to combine plants and social" is **false**. |
| **[PictureThis](https://play.google.com/store/apps/details?id=cn.danatech.xingseus)** — Google Play listing | Recognize plants, botanical information and diagnostic-oriented features | Our user can start with unknown species and record honestly; future identification needs explicit sourcing, confidence and correction | We have NOT built a verified species recognition or diagnostic engine. |
| **[PlantIn](https://play.google.com/store/apps/details?id=com.myplantin.app)** — Google Play listing | ID, plant care guidance, reminders and expert-oriented service descriptions | A clear onboarding value statement matters; avoid leading with 5 layers of product poetry | Do not sell unsupported expert availability, paid service SLA or automated diagnoses. |
| **[Day One](https://dayoneapp.com/features/)** — non-award *mechanism* reference | Photos and dated entries are retrievable, making time meaningful | Photo-index/compare (only with two real dates); avoid forcing users through 200 text cards | Not proof that Day One is a plant competitor or award winner. |
| **[Are.na](https://help.are.na/docs/getting-started/channels)** — non-award *mechanism* reference | People assemble source-linked content into reusable channels and blocks | Circle: a public question has accountable author, optional chosen photo, replies and owner-recorded outcome | A generic feed is not automatically an archive of verified expertise. |
| **[One Big Green](https://onebiggreen.com/)** / **[FYTA](https://fyta.de/)** — operations / sensing landscape in our R1 | Situated professional site care / sensor-connected context are adjacent worlds | Keep separate consumer C0 vs real installation B1, and evaluate future staff/plant/site journeys | Plantupia currently has no verified sensor hardware integration and no complete commercial field-service workflow. |

**Market discipline:** These listings are self-reported product features, not our own timed usability tests; pricing and rankings are intentionally not treated as evidence of user value.

## 02. Award references — what exactly to transfer

| Verified award/source | Principle | Actual Plantupia proposal | Explicitly refuse |
|---|---|---|---|
| [CapWords — 2025 Apple Design Award, Delight and Fun](https://developer.apple.com/articles/capwords) | Physical capture → deliberate confirmation → tactile/animated durable object | After uploading a **real plant photo**, show preview → chosen identity / actual place → saved acknowledgement; photo visually follows the user from place to plant to timeline | Copy its stickers, use automatic cut-outs that erase pot/light context, fake saved state |
| [Moonlitt — 2026 Apple Design Award, Interaction](https://developer.apple.com/design/awards/) | Nature/time complexity made direct and elegantly readable | One planted identity, one user-chosen next check, one real evidence line; coherent navigation into real history | iOS-only Liquid Glass/space 3D copied onto Android |
| [Tide Guide — 2026 Apple Design Award, Visuals and Graphics](https://www.apple.com/sg/newsroom/2026/06/apple-reveals-winners-of-the-2026-apple-design-awards/) | Visualized *genuine* environmental variables; meaningful visual transitions | Real date/photo pairs and logged changes can form a graphic narrative. Palette/animation only changes with a truly known state | Fictional moisture/health graph or growth prediction |
| [Focus Friend — 2025 Google Play Best App](https://blog.google/products-and-platforms/platforms/google-play/best-apps-games-2025/) | Attention budget is a design feature; a small motivating ritual | 15–25-second “inspect → record → leave phone” test | Gamification, coins, streak shaming, artificial cartoon pet |
| [Art of Fauna — 2025 Apple Design Award (game, Inclusivity)](https://developer.apple.com/design/awards/2025/) | Illustration forms a coherent authored universe with alternate readable modalities | Commission custom botanical field-mark graphics for identity/empty/onboarding and keep functional care icon family legible | Use game artwork as plant-identification evidence or copy art |
| [Sago Mini Jinja's Garden — 2026 Apple Design Award (game, Interaction)](https://developer.apple.com/design/awards/) | Discoverable direct manipulation with little explanatory reading | Native tactile care choices, graphic action families | Cartoonification or misleading physical behavior |
| [Vocabulary — 2025 Apple Design Awards **finalist**, Visuals and Graphics](https://developer.apple.com/design/awards/2025/) | Consistent typography + iconography + supportive illustration/haptics | Mature unified word/icon/motion grammar across private, social and profile | Calling a finalist a winner |
| [Katha Room — 2026 Apple Design Awards finalist, Social Impact](https://developer.apple.com/design/awards/) | Local art language supports story, not decorative generic assets | Illustrations may have a culturally meaningful botanical/architectural *authored* style | Copy regional art patterns or assume awards apply to our app |

Award winners support *craft hypotheses*, not app superiority or actual usage metrics.

## 03. Brand design grammar — visual system to test

### A. Typography: when do words earn their place?

**Current original brand-direction target:** Playfair Display editorial accent; Inter 400–600 UI text; Forest `#123B2E`, Cream `#F3EEDE`, Sage `#9FB79A`. **Source vs implementation gap:** `PlantupiaDesignTokens.kt` currently uses `FontFamily.Serif` (generic platform serif), while other components use `NovaType`. Don't state Playfair/Inter are embedded or distribute confidential font assets. Licensing and official company files remain gated.

**Proposed hierarchy to prototype, not yet implemented everywhere:**
- **Hero / editorial serif** only for one emotionally significant *place or actual plant name / photographic history title* in a view, typically 24–32sp, with resilient Arabic fallback. Avoid 35–44sp copy-heavy repeated slogans.
- **Task header / readable UI sans** e.g. 18–22sp for `What needs attention?` / `Recorded on 17 Oct`.
- **Body / forms** readable 14–16sp, line height 1.35–1.5, adjustable with system font scale; especially concise errors, notes and warnings.
- **Small labels** 12–14sp on Android; don't make 10–11sp the primary way to understand a complex private/public state.
- **Time/metadata** short 12–14sp with semantic units, localized plural and ISO when appropriate; user-generated Arabic text preserves language, bidi, line-wrap, author.
- **Voice**: one sentence to explain unusual privacy or biological semantics **at the decision point**; not repeated below every button. Show longer explanations only after `Learn why` or during consent.

Copy tests: delete any sentence that doesn't alter the next decision, establish source trust or prevent harm. Rewrite `Your moments become tomorrow's history` as *brand storytelling for onboarding*, not a paragraph between a care action and its Save button.

### B. Icon, text, image, graphic — which medium when?

| Semantic situation | Default medium | Why / accessibility | Concrete candidate |
|---|---|---|---|
| Bottom primary navigation | **Icon + persistent short label** | Target is recognizable without memory; TalkBack label and selected state | My Space = room/leaf-in-place, Circle = linked leaves/people, Create = clear plus, Me = human mark |
| 6 routine care actions | **Authored line illustration + short verb** | The same gesture can mean different care types; never icon-only for novices | Droplet/soil/probe, pruning shears, place arrows, fertilizer granules, re-pot. Prototype 6 as one family, same stroke/bounds; color not the only indicator |
| Destructive/private/public choices | **Explicit readable words and audience icon** | Responsibility/privacy cannot be silently represented by a lock alone | `Save privately`, `Review public question`, `Archive plant` |
| User's plant identity & space | **Authentic user/photo material** with crop/lighting discipline | Source truth first; photos connect real place across screens | 1 shared real plant thumbnail in inventory→detail→dated evidence, with fallback when absent |
| No photo / first space / first plant | **Commissioned botanical field mark / sparse architecture line art** | Gives personality without fabricating evidence | 3–5 simple custom illustrations, off-white ink paper line strokes, no fantasy plant count |
| A care event saved / follow-up due | **Text + small icon + real timestamp and restrained motion** | Confirmation must represent backend result; no artificial health signal | Recorded checkmark upon success; due -> completed linked to actual event |
| Public question | **Title, author and replies first**, approved small photo second | Knowledge object, not giant content-for-engagement image | Question+photo card, record outcome only after actual user input |
| Owner Profile | **Human avatar + actual plant portrait collection and relationship motif** | A person isn't a "0" circle. Private collection only for authenticated owner | See Profile worlds below |
| Help/uncertainty | **Short explanation / source link**, sometimes photo | Visual confidence fiction is dangerous | `Not confirmed; inspect soil`, not a glowing AI diagnosis icon |
| Ambient brand mood | **One original illustration or real image**, not more prose | Editorial identity without obstructing tasks | A botanical shadow/field note motif only at low-attention views |

### C. Visual asset inventory — actual graphics needed (proposed, no assets generated or licensed yet)

1. **Logo/wordmark**: verify official vector/clear-space; *not* manually reconstruct a confidential company identity from screenshots.
2. **6 actual-care pictograms**: one visually calibrated vector family; existing `PlantupiaCareActionTile` already draws six native Canvas symbols and has selection animation. Redraw/tune, don't claim absent.
3. **Architecture × botanical line art** (2–3 motifs): the relationship of a plant, vessel, window/light plane and human absence/presence; abstract enough not to imply a measured floor plan.
4. **Empty-state set** (3–5): zero space; zero plant; no records; no real photos; no public questions. Identity without fake activity.
5. **Profile motif** (one): a **living atlas ring/constellation** or **dated fieldbook spine** derived only from actual item count and media; avoid converting empty avatar into “0”.
6. **Timeline marks**: care, inspect, photo, observed change, outcome. Pair date + action text; no chart until observed data supports it.
7. **Photo guidance overlay** only during *opt-in user capture*: natural framing, readable plant/pot/place context. Do not hide user photos with licensed stock.
8. **Public/private system marks**: audience labels and a clear review screen, never just 2 visually similar green buttons.
9. **Motion states**: loading, upload, saved, unsaved/retry, photo transition, question publish confirmation, profile section switch; assets optional, focus causality.
10. **Story/Reel media**: retain real Nova optimized formats; do not cover user-authored content with decorative overlays.

Design direction: **editorial natural material + field notes + architectural spatial rhythm**, not photoreal CG botanical wallpaper, not generic green SaaS cards, and not cartoons in every page.

### D. Motion — a system with causes (prototype hypotheses)

- 120–180ms pressed/selected care pictogram feedback, with haptic only if available and user settings allow.
- 180–260ms compact panel reveal, e.g. Profile public/community or date outcome, if it doesn't move keyboard focus away.
- 250–350ms photo continuity from real plant thumb → plant detail; use the same actual photo and fall back safely on slow media.
- An understated `saving...` -> `saved at [actual time]` transition only after successful network acknowledgement; timeout gives retry, no falsely positive animation.
- Recorded check-in → actual observed outcome link in timeline. Never interpolate growth/sensor measurements.
- Support Android Back, interrupted animation, reduce motion accessibility option, font scale, and no animation under image loading errors. [Compose shared element guidance](https://developer.android.com/develop/ui/compose/animation/shared-elements); [Material 3 customization](https://developer.android.com/develop/ui/compose/designsystems/material3).

Numbers above are **design targets for prototyping**, not timings measured on device.

## 04. Me/Profile — settled visual direction and discarded explorations

**Existing photographed state:** new sage own-account identity component still coexists with private-space mega-card, Moments/Followers/Following, Posts/Reels/Reposts. It's less visibly violet Nova, but still an overly tall collage of separate modules, and a number may be misread as avatar identity. Source `PlantupiaMeIdentity.kt` still styles text through some `NovaType` and imported Nova visual tokens.

**Builder clarification, same date:** We have **already decided Plantupia's core art direction**, have real native screens, and must now refine **one coherent design**. No three competing HTML files, no restart from first principles, and no mandatory three visual routes. The three concepts below are historical inspiration notes, NOT parallel design deliverables. Implement a unified **Human & Living Places** profile first, taking only useful archive/connection details from the other notes.

### Adopted direction — The Human & Living Places (refinement of our native Plantupia design)
Compact real person portrait/name/bio (not a stock illustrated person) → one horizontally navigable **actual** photographed place → small recognizable plant portraits/date → a **clear Community tab** for posts/reels/follows/messages. One subtle **connecting botanical line** transitions from human area to plant inventory, drawn with authored vector geometry; no fabricated graph/number. User's private place only on owner's profile.

### Optional inspiration only — Personal Fieldbook
A physical notebook/archival aesthetic: short personal identity, dated latest *actual* field note as first photo, tiny tactile page/spine cues on chronological list; public posts are a distinct “shared pages” tab. Strong narrative continuity, but potentially too nostalgic/slow for quick care and 100+ plants.

### Optional inspiration only — Living Atlas / Place × People
One personal portrait node and actual spaces as adjacent photo tokens, private/social separated by marked concentric areas. Select real place -> living plants; public node -> posts/stories/inbox. Novel animated transition only when underlying entities exist. Risk: metaphoric graph confuses navigation, accessibility, older phones and empty accounts.

**Decision clarified by builder:** **No A/B design multiplication or three HTML prototypes** at this stage. Refine the existing Android system into one authored Plantupia world. Own profile: real human identity → actual private places/plants and memories → a distinct optional public social section. Add subtle native photographic continuity / a botanical connective motif only when it expresses real entities. Other-person public profile must NEVER expose owner-only places; DMs stay person-to-person. Alternative archival ideas are available to improve this *single* direction, not competing products.

## 05. Feature inventory explained simply — CODE vs demonstrated vs not shipped

**Legend:** CODED = native/Django path on working branch; SCREEN SEEN = supported by owner's phone captures (narrower than full end-to-end proof); INHERITED = real Nova subsystem requiring Plantupia integration QA; GATED / R&D = not a delivered Plantupia feature.

| Capability family | In plain Arabic (“بالبلدي”) | Evidence/status and boundaries |
|---|---|---|
| Account/login/password recovery | "تدخل بحسابك وتسترجع الباسورد" | **SCREEN TESTED for login/reset in this chat**, inherited auth; server isolation and other-account rights separate |
| Private spaces | "تعمل بيتي أو مكتبي وتحط كل نبات في مكانه" | **CODED + SCREEN SEEN** two spaces/plant summaries; actual fresh space creation/add permission server field still unverified |
| Actual plant identity | "النبتة دي اسمها إيه، موجودة فين، صورتها إيه، وليها اسم أنت مديهولها" | **CODED + SCREEN SEEN**; no plant species scanner. Optional unknown species is valid |
| Human care actions | "سقيت، فحصت تربة، قصيت، نقلت، سمّدت، غيرت الأصيص" | **CODED + SCREEN SEEN** actual icon actions; no autonomous watering, no health prediction |
| Notes and photos | "تكتب اللي شفته وتحط الصورة اللي صورتها" | **CODED + SCREEN SEEN** Arabic note/photo in dated record; genuine content identity, offline and retries still device-QA |
| Dated living history | "بعد شهر ترجع تعرف عملت إيه للنبتة إمتى" | **CODED + SCREEN SEEN** history, filters, photo index, pagination; no guaranteed image comparison if only one photo |
| Check again / return | "تفكّر نفسك تبص عليها يوم أنت تختاره وتكتب حصل إيه" | **CODED + SCREEN SEEN** selected check-in date/outcome controls; mobile reminder exact timing and notification permissions still QA |
| Archive/restoration | "توقف متابعة نبتة من غير ما تمسح قصتها" | **CODED**, visual/role test pending; replacement as separate biological plant **not implemented** |
| Multiple people/access | "صاحب المساحة أو مسؤول رعاية أو متفرج، كل واحد يشوف/يعمل اللي مسموحله" | **Backend role checks CODED**, no finished invitation/staff portal; test second account + missing `can_add_plant` on deployed QA |
| Circle | "تسأل عن نبتة بصورتك وتاخد ردود من ناس" | **CODED + SCREEN SEEN** real question + 0 replies. Two-account successful answer/outcome privacy still missing |
| Public consent | "تشوف بالضبط إيه الصورة والكلام اللي هيتنشر قبل ما توافق" | **CODED + SCREEN SEEN** review workflow; server EXIF/media ACL verification still required |
| Public social posts / stories / video | "تنزل بوست، ستوري 24 ساعة، أو فيديو عادي لو حبيت" | **INHERITED Nova CODE**, Plantupia full phone QA/placement and audience flows not accepted |
| Messaging / people | "تكلّم الناس في الخاص وترد وتتابعهم" | **INHERITED Nova CODE**, permissions/media/calls/notifications need integrated testing |
| AI assistant | "تسأل مساعد عن نبتتك، بس ما يعملش أو يأمر بالسقي من نفسه" | **Backend adapter and UI CODED**, live provider and quality/limits not proven; may be disabled without credentials |
| Identification/diagnosis | "تصوّر النبتة فيعرف نوعها أو مرضها تلقائيًا" | **NOT SHIPPED/NOT PROVEN**. No trustworthy claim |
| Enterprise installations/staff | "شركة تركّب نباتات وتتابع الزيارات والشغل للعميل" | **B1 GATED**, actual agreements, staff roles, proof of visit/company process lacking |
| Sensors/IoT | "حساس بيقول التربة عطشانة وبيسقي لوحده" | **EXCLUDED** from Android scope. No real sensors or automatic readings |
| Google Play separate final app | "تطبيق Plantupia مستقل باسمه على جوجل بلاي" | **NOT YET APPROVED / CONFIGURED**; current side-by-side QA app isn't the final signed independent Play release |

**Critical lesson:** number of screens is not number of useful outcomes. User's actual recorded care/photo/date and 0-reply question are stronger evidence than a dozen decorative features.

## 06. Our own scores (transparent editorial diagnosis, not measured UX benchmark)

**Scoring rubric:** 1 very weak, 3 working but rough, 5 mature and validated. Based on **screenshots + docs + branch code** only; no timed competitor test or paid user interview. Re-score after device A/B and full E2E.

| Dimension | Our provisional 1–5 | Reason |
|---|---:|---|
| Research depth / reference traceability | **4** | Source Atlases 01–05, four worlds, Pass 04–07, screen ledger; some decisions have not been field validated |
| Core product thesis / meaning | **4** | Real plant-in-place, actual human care, dated record and opt-in social outcome are a coherent vision |
| Actual native/backend capability breadth | **3** | A lot of Kotlin/Django + Nova working engines; QA-host integration, auth roles and public/media routes not fully demonstrated |
| User's physical care task clarity | **2** | Phone captures show huge scroll, verbose guidance and malformed concern control; source fix not yet phone accepted |
| Typography, identity, illustration, visual hierarchy | **2** | Brand palette present, but fallback serif, mixed Nova tokens, text-heavy layouts, insufficient actual authored art system |
| Motion and interaction expressiveness | **2** | Some animated native care symbols / inherited Nova media, not a coherent product-wide causal motion grammar |
| Private/public trust model | **3** | Deliberate preview and backend roles exist, yet real two-account + deployed media authorization not proven |
| Profile as Plantupia | **2** | Mixed private-space hero and legacy follow/posts/reels; new sage identity exists but composition is still not resolved |
| Evidence of effectiveness/differentiation | **1** | No comparable competitor task study, care outcome measurement or real retention study |
| Release readiness | **2** | CI/APKs and photo/care proof, but stable signing, isolated QA backend, Play identity and final integrated QA outstanding |

**No headline composite score:** Averaging research quality into live product quality would hide important weak gates. These scores are a self-evaluation of evidence and unfinished craft, not a final company/award judgment.

### What we genuinely do differently vs adjacent apps?
- **Potential:** organize real plants by their *physical places* and retain separate private individual histories; combine dated care with exact-preview public conversation and optional observed outcomes, using working social/media infrastructure. We should verify whether competitors have overlapping versions of this and show actual task value.
- **Not defensible:** claiming only we have plant journals, community, reminders, photos or sharing — Planta and Greg do too.
- **Not yet evidenced:** that our user gets care done faster, stays more connected to greenery, learns better or has healthier plants. Those require real longitudinal study.

## 07. Prioritized whole-product design work — BEFORE Codex final technical pass

**P0 now — trust and broken experience:** fix phone-exposed non-wrapping long care action; ensure normal vs QUESTION branch composition doesn't duplicate/private leak; verify exact-head Android, signing + QA backend `can_add_plant` roles; 360/390/430dp at 1×/1.5×/2× font; real second-account privacy and persistence.

**P1 — identity system prototypes (one design family, not screen skinning):**
1. Art-direct and refine **one integrated Android composition** — My Space/Today + Plant Detail/Care + Circle + Me/Profile + Create + Story/Inbox entry — based on the native app and original research. No triple HTML design set.
2. Prepare a **component board**: Playfair-like licensed approved heading candidate vs current generic serif, consistent UI sans, 6 care glyphs, 4 tab glyphs + captions, paper/forest/sage roles, error/warning/disabled, touch and type scale.
3. Validate the existing chosen navigation/profile and real-photo hierarchy against the actual phone tasks, tapping, backtracking and accessibility; test a small alternative **only** when real friction warrants it, not as a mandatory new visual direction.
4. Authored botanical architecture **illustration kit** only for empty/onboarding/profile ambient, separate from actual user photo evidence. Get company/owner approval and rights before shipping assets.
5. Implement an **interaction storyboard** for real image continuity, save success, reschedule, photo dates, explicit public transition, Profile tabs, Story/DM return and all failures.
6. Remove repetitive “PLANTUPIA / CIRCLE”, “you may choose” and “private history” prose after initial explanation while keeping audience/privacy confirmation at the correct moment.
7. Review colors, Arabic typography/RTL, image aspect, large fonts and contrast in *real Android Compose*, not only static HTML.

**P2 — after core acceptance:** before/after with actual 2+ dated photos; semantic motion refinements, nuanced profile identity, material textures only where UI performance/accessibility survives; evidence-backed professional/B1 concept interviews if company approves.

### Pass 09 implementation clarification — designed human identity, not a letter

The builder explicitly requested a Reddit-*behavior*-inspired **distinct original illustrated default person avatar** for every account that hasn't chosen a photo. This is **not implemented** in the present native avatar component: `NovaAvatar.kt` currently falls back to the initial of the name. See [Pass 09 / one-system execution map](./PLANTUPIA_PASS09_ONE_SYSTEM_VISUAL_EXECUTION_MAP_2026-10-17.md) for a stable privacy-safe, persistent, human-centered illustration system, shared across Me, another person's profile, Circle/comments/DM/people/Stories; selected photo overrides it. No Snoo copy, no inferred demographic features, no random character change on each render. Reuse the existing Art Direction and Native Compose implementation; **no three HTML designs**.

## 08. Codex creative brief — give it a voice and a responsibility

Future Codex is authorized to **challenge our visual decisions**, inspect every screen and repo/source brief, generate alternative icon/type/illustration/motion/profile compositions, propose new behaviors and prototype *reversible* ideas **on an isolated review branch**. It must explain what human task improves, what evidence motivated it, which alternatives it compared, how privacy/data provenance is preserved, and how to test on real Android. It must **not** rubber-stamp our taste, claim unsupported QA, overwrite Nova production/default branch, invent official branding, ship confidential photos, fabricate sensors or silently replace the company's product direction.

**Success gate:** One recognizable and cohesive Plantupia world where a person finds their real plant, records actual care, sees truth over time and optionally exchanges knowledge; artwork and native motion make those actions intuitively understood. The experience must remain quick when there are **zero images**, one plant, or one hundred plants.

**Research status:** refreshed primary-source review and product-direction hypotheses only. No image/illustration assets created, no Flutter/iOS work, no implementation of motion or profile A/B, no new APK or provider connection in this research commit.
