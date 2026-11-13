# Plantupia Public Web — Living Atlas / Design Research & Interaction System V02
Date: 2026-11-13
Branch: `feature/plantupia-public-living-website-20261113` / PR #300
Status: **Interactive design study**, not a verified company portfolio, commercial website launch, account app, or production signoff.

## Why V01 was insufficient
The first bilingual site (`apps/web/app/page.tsx`, `app/ar/page.tsx`) applied warm paper, plant illustration, large serif typography, and common marketing hierarchy: header → oversized hero → philosophy → services/typologies → digital screen mockup → process → contact. It was visually coherent but structurally generic. It explained that Plantupia understands the living world while making visitors passively scroll. It did not represent the product's central relationship: **PLACE → PLANT → HUMAN CARE → TIME**.

It also opened a design study by asking for company contacts and projects rather than delivering an experience. That was the wrong priority for this pass.

## Research — studied mechanics, not visual assets to copy
1. [Marshmallow Laser Feast, Of the Oak (Kew, 2025)](https://marshmallowlaserfeast.com/project/of-the-oak/)
   - A tree is treated as a network of life and relationships, accessed via an online interactive field guide, rather than a linear product page.
   - **Transferable:** select relationships/spatial nodes, discover contextual information; one environment supports several reading paths.
   - **Do not transfer:** imagery, audio, species data, claims about scientific scans, or the specific artwork.
2. [Penderecki's Garden — digital garden / map](https://bachtrack.com/feature-penderecki-garden-website-december-2021)
   - Map as non-linear index with explorable points. The visitor's choice provides narrative agency.
   - **Transferable:** location-led discovery, not an unordered stack of static cards.
   - **Do not transfer:** composition, map assets, spatial data, sound, or commemorative content.
3. [Weleda, The Open Garden — web design study](https://www.behance.net/gallery/112284911/Weleda-The-Open-Garden)
   - Herbarium-like information and plant-centric navigation can create a distinct information grammar.
   - **Transferable:** object/context/observation hierarchy, visual identifiers for chapters and states.
   - **Do not transfer:** botanical illustrations, product claims, plant data, catalog layout or marketing copy.
4. [Bélanger Salach architecture archive — 60-year timeline](https://codewebbarcelona.com/en/architecture-web-design-belanger-salach/)
   - Multiple ways to navigate chronology and a spatial archive without forcing every visitor into the same vertical scroll.
   - **Transferable:** navigation through time as a first-class interaction, while keeping explanatory detail available.
   - **Do not transfer:** project photography, timelines, or built-case claims.
5. [Digital Garden by Alicia](https://digitalgarden.si/) (public descriptions)
   - Exploration with different visual states and device constraints. A garden should remain usable on mobile and with accessibility controls.
   - **Transferable:** controllable view and simple fallback over heavy involuntary WebGL. Do not claim this site's tool/rendering is ours.

## Chosen direction — Living Atlas, not another marketing template

**One experience with several reading lenses**, not a dozen similar sections:

- **Choose an imagined spatial typology:** dwelling, work, gather. Each has its own SVG architectural scene, furniture/light arrangement and contextual note.
- **Change the lens** across PLACE, LIVING PLANT and TIME. Each lens changes visual annotations, their meaning, and the selected field note. Lens selection is explicit, not triggered silently by scrolling.
- **Explore three hotspots** (keyboard/Touch accessible buttons), each revealing a specific relationship and editable editorial description.
- **Time lens:** choose conceptual arrival, return and across-time moments. Visual scale/trace shifts represent a way to think, not fabricated plant events or sensor facts.
- **Explore the four-layer method:** location / living material / human care / living time via a second interactive folio, not another services grid.
- **Focus mode:** expand the scene and keep the interaction available. Escape restores the editorial layout.

The interface uses an original programmatic SVG architectural diorama, muted limestone/clay interiors, a dark forest-green field, electric pale-lime active annotation, paper note surfaces, technical folio labels and editorial headlines. No downloaded competitor assets. No fictional Plantupia installation photography or client claims.

## Working editorial / data contract
- This is a **CONCEPT STUDY / NOT A BUILT PROJECT**.
- The drawings illustrate design relationships. They are not botanically validated species, installation records, real plant growth or measured conditions.
- Time phases are chosen illustrative narrative states, not live sensor events or real customer data.
- Website and customer app remain separate; no public credentials or care records.
- Language parity in English and Arabic; no duplicated original homepages.
- No publicly exposed checkout or dead marketing contact forms.

## Interaction acceptance before any release
1. **Entry:** Desktop 1440×900 and tablet 1024×768 display a meaningful scene and working controls without a four-screen empty hero.
2. **Viewport:** 320, 360, 390, 430, 768, 1024, 1440 widths; no horizontal overflow, clipped narrative, hotspots unreachable or collapsed menus.
3. **States:** all 3 spaces × 3 lenses × 3 hotspots, focus open/close, time phases, four method chapters, English/Arabic, and navigation between them.
4. **Keyboard:** Tab/Enter/Space through controls, focus visibility; Escape exits focus. Buttons indicate selection with `aria-pressed`. Readout uses polite live regions.
5. **Reduced motion:** No mandatory animation or scroll motion for users with reduced-motion preferences. The UI is usable before JavaScript hydration where possible via SSR markup, with interaction progressive.
6. **Audit:** Real Vercel preview route / and /ar, no secrets, font license issue or fake project photos, no 404 links; standard `npm run typecheck` and `npm run build` on exact head.
7. **Future investigation (not in this slice):** Native real-project photography (when licensed), physically accurate spatial studies based on client materials, advanced scroll/performance work only if it improves understanding, content accessibility for screen readers, authentic mobile usability participants.

## Source and merge boundaries
- New self-contained React client experience at `apps/web/app/LivingAtlas.tsx`; styles at `apps/web/app/atlas.css`.
- English and Arabic route wrappers share the same scene/data model.
- Only PR #300 changes; base remains consolidated draft #253. Android, Django, Company Portal, and current `plantupia.com` are untouched.
- Vercel linked project: `plantupia-public-preview`; non-production branch deployments automatically appear as protected review previews. Check deployment exact SHA, do not promote or reassign the company's domain.
