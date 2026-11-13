# PLANTUPIA / THE WHOLE CREATIVE WEBSITE
## Experience Blueprint V01 — from the opening moment to a complete usable website

**Date:** 2026-11-13
**Working branch:** `feature/plantupia-public-living-website-20261113` / Draft PR #300, based on consolidated Draft PR #253.
**Status:** Creative/experience blueprint and first integration slice. English-only, desktop-led, responsibly usable on mobile. NOT a final brand signoff or production/domain launch.

### The core distinction

**The cinematic "A Space Becoming Alive" scene is the opening chapter, NOT the whole website.** A visitor must be able to skip it, enter deeper chapters, understand what Plantupia explores and—when real company content and verified commerce data are available—perform useful real-world tasks.

Don't grow the website as one long series of generic title/description/cards sections. Organize it as **one authored world** with chapters, distinctive interactions, free navigation and clear information hierarchy.

**Working north star:** Architecture makes the setting. Living material changes the experience. People and care sustain the relationship over time.

This is a *proposed artistic reading* of the existing Plantupia product direction, not a factual outcome study, verified botanical science, company-approved tagline or claim about installed client work.

---

## 1 / Visitor goals — map actions, not just topics

| Visitor intent | First question | Natural journey | Useful destination |
|--|--|--|--|
| A curious visitor | "What is Plantupia?" | Cinematic entry → choose chapter → Spaces / Studio | Clear understanding, not a mandatory movie |
| A designer / prospective space client | "What can happen in my space?" | Spaces → relevant environment → approach → verified Work when available | Real examples and scope, not invented results |
| A plant owner | "What happens after delivery?" | Living Systems → care explanation → existing customer app | Honest handoff to authentic app workflows, not a duplicate dashboard |
| A plant researcher | "Which plant and why?" | Botanical Library → curated public species / guidance (later) | Reviewed sources, variants, images and care notes |
| A shopper | "What can I buy and how?" | Shop → published SKUs → real product detail → same canonical checkout (later) | Availability, price, terms and accurate order confirmation |

No login wall for editorial exploration. No fake "active sensors", plant-growth metrics, customer testimonials, project images or commerce inventory.

## 2 / Information architecture

### Chapter A — THE EXPERIENCE / `/` (exists, under visual QA)
- Purpose: emotional introduction. One central cinematic image state with spatial/plant/time readings, not a portfolio or catalogue.
- Content: originally researched architectural photo references currently **three independent locations**, not before/after of one room. Three modes must be framed as editorial studies, never a real physical transformation.
- Actions: (1) explore optional scene detail, (2) use centered `+` index to skip anywhere, (3) **continue naturally to the Chapter Threshold** under the introductory scene.
- Acceptance: natural scroll to next part, explicit chapter link, no duplicate menus or dead CTA.

### Chapter B — SPACES / `/spaces` (working prototype)
- Purpose: explore questions and possibilities of a specific environment (dwelling / work / gathering).
- Interaction language: change scene context and read concise field notes; later spatial/real photos with actual constraints; do not create generic "services cards".
- Future evidence: project photography and real client briefs only after approval; this is not yet a complete case-study directory.
- Exit: Studio to see how those questions become design decisions.

### Chapter C — THE STUDIO / `/studio` (working prototype)
- Purpose: reveal how a living environment is considered and shaped.
- Interaction language: a layered spatial workbench / sketch-to-material logic; Observe → Compose → Introduce → Return.
- Each step changes visible composition, not just a paragraph. Need later higher-fidelity art and authentic process details.
- Exit: Living Systems—what follows installation.

### Chapter D — LIVING SYSTEMS / `/living-systems` (working prototype)
- Purpose: explain continuity and the product's digital aftercare thinking.
- Interaction: place ↔ plant ↔ human-recorded action ↔ time. Distinguish **planned work** from **confirmed work**.
- Source of truth later: the actual unified Plantupia backend/customer app. No static made-up records or misleading device readings.
- Exit: Library (for what lives) / existing app (for the owner) when verified link semantics are approved.

### Chapter E — BOTANICAL LIBRARY / planned, NOT built
- Purpose: searchable public taxonomy of reviewed plant types, visual care guidance and suitability considerations.
- Interaction: botanical specimen → conditions → guides → relationship to types of spaces; editorial knowledge with clear references.
- Data gate: public botanical-library importer and source/provenance review in consolidated backend #253. A "library not found" state must never turn into invented data.
- No independent data store for copy-pasted species and no fake search experience until real data is present.

### Chapter F — SHOP / planned, NOT built
- Purpose: discover and buy real approved products with transparent availability and price.
- Creative layer: tailored visual browsing and spatial product context; **transactional flows remain conventional and trustworthy**.
- Data gate: Company → Shop ZIP import preview/approval/publish and canonical customer checkout/ownership; same SKUs and policies across web and Android. Never build a second unconnected basket or checkout.

### Chapter G — WORK / verified projects / later, not a blocking prerequisite
- Purpose: proof of actual installed projects, not decorative filler or fake client names.
- Format: spatial narratives with real before/after (where available and licensed), brief, material, human experience, maintenance/aftercare, permissioned visual work.
- Gate: approved photos, case studies, copyright/license, consent and accurate outcomes.
- Until then, describe typologies truthfully without claiming finished projects.

**Decision:** Exactly **4 active main chapters** in the visible `+` index now. Add Library/Shop/Work when functional. Never add disabled mock links that look real.

## 3 / Navigation grammar

1. **Primary, always reachable:** quiet Plantupia signature left; **centered +** opens full-screen World Index; context-specific controls right. No five-link corporate navbar and no giant "Let's Create" CTA.
2. **On the intro scene:** SPACE / LIFE / TIME are **scene reading modes**, not global chapters. Keep them distinct from the + World Index.
3. **On every deeper chapter:** a clear local exploration control (space selection, studio layer, continuity moment) and one coherent onward route. Do not repeat scene modes in the global header.
4. **Below the cinematic scene:** the **Chapter Threshold** is an explicit bridge. It should not resemble a grid of marketing service cards: one immersive stage, three selectable gateways, one primary destination at a time.
5. **Mobile:** same purpose and links, simple touch controls, no gesture-only navigation or mandatory animations. Accessible usable fallback can differ compositionally.
6. **Technical semantics:** active states `aria-pressed` on buttons; proper links to existing routes; visible focus; modal focus trap and Escape; reduced motion honors user preference; no click required to reveal core information.

## 4 / Original design direction — one visual system, different chapters

- **World material palette:** night forest `#152920`, deep green `#204334`, warm chalk `#EEEDE1`, living sage `#A9C5A0`, muted lime `#D8E9B3`, sunlit stone / copper in the environments.
- **Lighting:** architectural daylight, rich shadow and believable material depth. No stock-website glow just for decoration.
- **Typography:** quiet serif editorial moments; precise tiny technical labels for navigation; large typography only where narrative earns it. Avoid giant text on every page.
- **Motion:** subtle cross-dissolves tied to actual content, user-controlled comparisons, no scroll-jacking. `prefers-reduced-motion` swaps directly.
- **Visual authorship:** the front scene can use temporary clearly labeled licensed architecture photography. Studio and Living Systems may employ original spatial drawings, but they must be art-directed to the same grade rather than look like unrelated generic SVG demos.
- **Language:** English only. No Arabic toggle or untranslated placeholders.
- **Brand/rights:** references are concepts, not actual Plantupia work. Real client imagery and claims require their own approval.

## 5 / The actual first build slice: CHAPTER THRESHOLD

**Location:** directly *after* the current cinematic scene on `/`, before any new marketing content. The intro scene still opens immediately.

Desktop: a dark editorial gallery-like space with a vertical selectable folio (SPACES / STUDIO / LIVING SYSTEMS) at the side, one large visual architectural field on the opposite side, changing in response to hover/focus/click/keyboard. A single explicit "ENTER [CHAPTER]" route goes into the real chapter. This is a navigational encounter, not a static service-card grid.

States:
- 01 `SPACES` → spatial enquiry → `/spaces`.
- 02 `STUDIO` → material/process enquiry → `/studio`.
- 03 `LIVING SYSTEMS` → care/continuity enquiry → `/living-systems`.

Requirements: visitors can select stage and link by mouse/touch/keyboard. The scene stays coherent during route transitions. No fake company projects are implied by editorial media. Do not copy the global + index as a second identical overlay; this is *sequential exploration*, while + is *free navigation*.

**Gate for approval:** actual production build, four-route HTTP smoke, correct hrefs, focus semantics, no mobile horizontal overflow, selected chapter visual/content changes, media behavior and visual inspection. Tests do not substitute for manual QA.

## 6 / Delivery order (incremental, no whole-site rush)

| Phase | Deliverable | Approval gate |
|--|--|--|
| **0 — Blueprint** | This document, live-state inventory and truthful content contract | Stakeholders can explain complete website journey before building extra pages |
| **1 — Website Bridge** | Home → interactive Chapter Threshold → real Spaces/Studio/Living Systems chapters | Routing, controls, build/CI, reviewed desktop layout |
| **2 — Cinematic Quality** | Single authored high-quality scene, same-camera artwork if doing true reveal, deliberate stage composition, performance | No low-res crop, no false before/after; desktop/mobile visual signoff |
| **3 — Deep Chapters** | Re-art-direct Spaces/Studio/Living Systems without changing their real purposes | Each chapter distinct yet unmistakably Plantupia; real usability |
| **4 — Botanical Library** | Data-backed browse/search/species detail after public import review | Published, sourced, accurate content, empty/loading/error states |
| **5 — Shop** | Shared catalogue and real checkout only when ready | No duplicate cart; accurate products, orders and ownership |
| **6 — Evidence & Work** | Verified case studies, project archive and brand/legal information | Source-approved facts, permissions and media |
| **7 — Release Gate** | Performance budgets, responsive QA, accessibility, error handling, SEO, analytics/privacy review, production deployment plan | Written approval; no automatic cutover |

## 7 / First actionable commercial journey — Space Conversation Outline

**Implemented preview path:** `/start-a-space` (reachable from `/spaces` and the homepage Chapter Threshold).

This is the first intentionally useful business outcome of the creative journey: **inspiration → structured expression of need → shareable outline**, not a premature "request submitted" fiction.

### Current prototype behavior

- Select the environment: home, workplace, hospitality/gathering or another setting.
- Select intention: new space, plant introduction or ongoing care.
- Select stage: exploring, planning or ready to move forward.
- Optionally add the city/area and notes, without asking for name, email, phone or sensitive information.
- A live, art-directed `Space Dossier` updates immediately. `Copy Your Space Outline` uses the device clipboard. The selectable text is always present as fallback.
- There is **no backend POST**, email, CRM submission, enquiry ID or Plantupia server storage. The browser state is ephemeral, and the page explicitly says the outline has not been submitted.
- **Do not connect a direct sending action** until Plantupia approves a verified contact destination, ownership of the receiving inbox/CRM, privacy notice, retention policy, spam handling, server validation and a real acknowledgment flow.

### What will make this a genuinely operational business pipeline

1. Confirm company-owned contact/channel and service-area scope with Plantupia (avoid guessing from older Plantophobia/Plantupia posts or non-owner listings).
2. Decide how a lead enters the Company backend (new `design-enquiry` domain model or existing validated requests process), with explicit opt-in and rate limits.
3. Capture only necessary fields, confirm consent and show a true receipt on a successful server-side send; distinguish draft, submitted, acknowledged and accepted.
4. Connect the lead to a human owner and a defined response workflow. Never promise a response SLA without agreement.
5. Instrument **privacy-reviewed** funnel events: scene → chapter entry → Space Brief start → outline prepared → channel handoff → genuine qualified enquiry (only after verified).
6. Later allow verified project evidence and reviewed case studies to support trust; avoid invented photos, testimonials or measured health benefits.

For now **copying is the only completed action**. An off-site contact route is not shown until approved. This is a deliberate scope limit, not a claimed end-to-end lead capture implementation.

## 7 / Current reality snapshot

- **Already live as preview routes:** `/`, `/spaces`, `/studio`, `/living-systems`. `/study/scene` redirects to canonical `/`. `/study/navigation` is an old isolated prototype, not a chapter.
- **Current photo states are three editorial places**, so mode selection cannot honestly be sold as a physical time-lapse.
- **Current Spaces, Studio, Living Systems are functional prototypes**, not the final art direction.
- **Not available on web yet:** Botanical Library, Shop, actual client work archive, full production launch.
- **Do not:** merge #300 into #253, move `plantupia.com`, modify Android/Company, expose unreviewed data or fake availability.

This document is a working execution blueprint, not a marketing promise.
