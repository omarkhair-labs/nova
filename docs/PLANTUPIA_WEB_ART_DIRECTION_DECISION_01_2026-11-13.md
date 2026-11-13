# PLANTUPIA / WEB CREATIVE DIRECTION 01
**Status:** Working decision board — research completed; visual direction proposed, NOT yet approved.  
**Date:** 2026-11-13  
**Scope:** English-only, public creative website. Research/art direction before code.  
**Working PR:** #300, branch `feature/plantupia-public-living-website-20261113`. No production or `plantupia.com` changes.

## 0 / Product and execution constraints
We are designing **a creative company website**, not a generic landing page and not an isolated art experiment.
The site ultimately needs clear navigation, accessible content, fast mobile behavior, and truthful Plantupia identity. Its memorable interaction should *express* a reason to care about nature in architecture rather than simply decorate an existing template.

We are deliberately **not** building the whole website at once. No Arabic route. No fabricated installed projects, claims about actual plant growth, monitored sensor metrics or branded photo material. Do not promote, merge or redeploy the Android/backend branches.

Our current interactive `LivingAtlas.tsx` is a **separate exploratory concept, not signed-off direction**. Preserve it for comparison; do not blindly extend its SVG diorama, 3-lens structure, or hotspots into the final site.

## 1 / What was researched and what actually transfers
Sources researched as **separate references**, not unified evidence:

| Reference | Observed evidence | Transferable idea | Not transferable |
|--|--|--|--|
| [Green Fortune / Plantwall](https://www.greenfortune.com/products/plantwall) | Large architectural vertical gardens designed for sites; tailored installations and service | Landscape interventions must be integrated into actual space and maintenance | Claiming Plantupia uses its patented systems or replicating their projects |
| [Planteria Group / Work in Photos](https://www.planteriagroup.com/our-work-in-photos/) | Real work organized by environments and client briefs | Credibility comes from a place-specific visual language; site experience must still work as a company website | Client imagery, names, quantities, business claims |
| [Benholm / Space Solutions](https://www.benholm.com/projects-space-solutions/) | Project story framed as brief, solution and outcome, including moss wall and acoustic panels | Reveal design before/after through purposeful change, not superficial hover effects | Their project, their client, their actual results |
| [Aguirre Arquitetura / Laje House](https://aguirre.arq.br/en/projects/laje-house/) | Architectural image with concrete slab, circular aperture, sunlight and vegetation | Precise cinematic composition: light and hard structure frame the living element | Using their building image as Plantupia project or copying architecture |
| [Pixelflakes](https://www.pixelflakes.com/) | Architectural visualisation, animation and interaction studio, carefully selected work | Camera, proportion, pacing, materiality and motion as core brand voice | Their visualization assets, animation, direct aesthetic imitation |
| [Marshmallow Laser Feast / Of the Oak](https://marshmallowlaserfeast.com/project/of-the-oak/) and [web companion](https://oftheoak.co.uk/information/) | An ecological relationship network explored through digital field guide; artwork uses actual research and scanning | The visitor should discover relationships; layers reveal new meaning | Claims of real scans, scientific data, species ecology, pre-recorded spatial audio |

**Interpretation:** Plantupia lives at the intersection of spatial design, living material, and continuity. We should adopt the visual credibility of architecture; use botanical editorial depth for human care; and save ecological/temporal networks for relevant digital experiences. Never compress all three references into a noisy one-screen collage.

## 2 / Three distinct visual directions

### A / CINEMATIC ARCHITECTURE — *space first*
**Emotional goal:** "I have entered an extraordinary, believable space. Plant life changes what the space feels like."

- Composition: single architecturally grounded scene; deliberate camera, sunlight, depth and quiet shadow; plants are spatially integrated and of credible scale.
- Palette: limestone `#D9D1C1`, warm plaster `#EEE8DA`, forest `#254632`, shaded olive `#788568`, bronze `#967257`.
- Materials: stone, washed concrete, natural light, wood/bronze, real foliage textures. High contrast between living softness and geometric hard surfaces.
- Type: restrained art-directed display, fine navigational text; typography does not need to be oversized to prove creativity.
- Signature interaction: the user **reveals a planting intervention** inside one architectural composition. Plants alter depth, occlusion and reflected daylight while the camera barely moves.
- Risk: sterile luxury-architecture wallpaper or expensive, fake-looking 3D that obscures actual purpose.
- Suitable for: the **first emotional encounter** and the website's primary visual identity.

### B / BOTANICAL EDITORIAL — *plant first*
**Emotional goal:** "There is intelligence, craft, and observation behind every living choice."

- Composition: specimen-like closeups, leaf veins, authentic botanic detail, visual margin notes; editorial transitions to wider architecture.
- Palette: bone `#F3EFE5`, pressed-leaf `#879A7D`, olive ink `#344633`, clay `#9D765B`.
- Materials: vellum, paper, macro plant photography, field diagrams, light grain (without gratuitous skeuomorphism).
- Signature interaction: inspect a material or area and unfold a concise editorial note on light, placement, care or change.
- Risk: turning a spatial green-design company into a generic herbarium, skincare aesthetic, or e-commerce product catalogue.
- Suitable for: project annotations, editorial secondary screens, aftercare narrative.

### C / EXPERIMENTAL DIGITAL NATURE — *relationships first*
**Emotional goal:** "Something alive persists beyond a frozen installation image."

- Composition: dark spatial field, data-line diagrams, soft organic structure, radial connections, layers through time.
- Palette: night forest `#10271F`, luminous leaf `#D8E9A9`, moss `#698B6C`, soft amber `#CCA77D`.
- Materials: glow, particles, transparent planes, thin data trajectories. All labels must be legible; no meaningless sci-fi telemetry.
- Signature interaction: one evidence-based living timeline or connected diagram with clear, factual states.
- Risk: gaming/tech aesthetic, heavy rendering, ungrounded scientific promises, hard mobile UX.
- Suitable for: the **digital continuity layer** after brand story has been established, and only when data semantics are real.

## 3 / Working direction — not a finalized vote
**Lead with A. Support with B. Keep C as a later distinct mode.**

- A provides the strong first impression and genuine spatial identity.
- B gives distinctive Plantupia authorship, the idea that care starts with observing a particular living thing.
- C should not lead the homepage: the public brand is not a fictional sensor network or a stand-alone online artwork.
- Do **not** blend the three into one visual scene: a restrained architecture composition gets only subtle botanical annotation. C belongs to its own future layer.
- No decision on 3D engine yet. Prototype the interaction first using simple controlled layers, and evaluate if GPU 3D or filmed imagery is needed.

**Creative statement:** `A Space Becoming Alive`  
**Working entry line:** `Architecture makes the space. Life makes it living.`
These are working direction cues, **not company-approved slogans**.

## 4 / First scene only: a falsifiable storyboard
One environment. One meaningful action. One transition.

**Beat 0 — BEFORE / ORIENTATION**: a quiet, believable space (wall, aperture, floor, light); structural image should make sense with no plant effect or text overlay. Minimal title + unobtrusive "Reveal life" action.

**Beat 1 — INTENT / THE GESTURE**: pressing/toggling the one action shifts the scene into a carefully composed planting intervention. It is a **design reveal**, NOT a seconds-long biological growth simulation.

**Beat 2 — CONSEQUENCE / SEE THE CHANGE**: real spatial effect: planting softens a sharp boundary, filters daylight, gives depth to the room and alters where the eye lands. User can compare before/after at their own pace. No scroll-jacking.

**Beat 3 — THE CONTINUITY HOOK**: a small contextual note: "Day one is not the whole story." Offer optional second exploration of how the relationship is cared for and recorded over time. Do not implement the long-term care experience in this slice.

### Prototype acceptance gates
1. A viewer can distinguish the two spatial states **without reading explanatory paragraphs**.
2. The transition is not decoration; they can say what visibly changed and why.
3. Same view and input make sense on a 360px phone and 1440px desktop (no impossible 3D gestures).
4. Keyboard/Enter/Space plus 44px+ touch targets work; focus visible, `prefers-reduced-motion` gets direct swap; missing JS/content fallback remains legible.
5. One hero scene only; no extra portfolio, contact forms, shop, services, or invented company projects during this experiment.
6. A still screenshot from both states looks art-directed and distinct from generic plant-wall stock advertising.
7. Avoid a heavy initial payload. Target a single controlled visual scene, lazy-load any advanced graphics, and measure mobile performance before choosing WebGL.

### Kill criteria
Reject or rework if the scene becomes "before/after decoration"; if the plant renders cannot look credible; if the user is forced through a long animation; if mobile feels like a compromised desktop; or if any explanatory copy becomes necessary to understand a visual change.

## 5 / Phase-by-phase agreement (no mass implementation)
- **Phase 01 — RESEARCH / DIRECTION** (this document + original three-direction visual board): validate visual language and choose dominant look.
- **Phase 02 — STORYBOARD**: three precise frames, placement, focal point, interaction/control state, desktop/mobile sketch. Critique before development.
- **Phase 03 — SINGLE-SCENE PROTOTYPE**: new isolated review route/branch, only one action; accessible, mobile-capable; compare against existing Living Atlas.
- **Phase 04 — ART & MOTION SYSTEM**: materials/lighting, visual rhythm, type, interaction transitions and fallbacks, after scene works.
- **Phase 05 — SITE ARCHITECTURE**: decide how a visitor reaches Work / Approach / Digital Continuity and navigates freely; website not a locked cinematic film.
- **Phase 06 — ITERATIVE PRODUCT SITE**: expand deliberately only after the entry moment proves its value; integrate verified assets and business truth later.

## 6 / Change control
The working `apps/web` website remains English-only, protected, and on PR #300 based on draft #253. This research is **not a new public launch** and does not authorize a deployed UI redesign, merging to #253, moving domains or turning off Vercel protections.
