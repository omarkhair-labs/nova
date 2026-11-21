# Plantupia / Full World Website Build Brief
Date: 2026-11-21
Status: implementation brief for the complete protected website build. Not client approval, publication authorization, or production promotion.

## 1. Product truth
Plantupia is not presented as a generic landscaping studio, a plant shop, or a fictional sensor company.

The website must express one connected practice:
**Spatial Design → Living Material → Green Technology → Continuing Care → Living History**

Current source-backed layers:
- Spatial design and planting proposals across hospitality, commercial, workplace and residential contexts.
- Plants, planters, finishes and catalogue material.
- Physical care programme terms present in supplied proposals: selection & installation, smart irrigation, scheduled maintenance, plant warranty. Exact terms remain project-specific.
- Digital continuity direction already present in the product: authorized QR handover, My Plants, Care Schedule, Living History, human-recorded observations and outcomes.
- Commerce: real published catalogue data and inquiry/order routes must stay direct and usable.

Never imply:
- live sensor telemetry unless it truly exists,
- completed installation when only a proposal exists,
- automatic diagnosis or watering records,
- fabricated sustainability metrics,
- invented project outcomes, client endorsement or publication rights.

## 2. The website thesis
The website is one living spatial archive, not a set of marketing templates.

The visitor should be able to move between:
PLACE → ALTERNATIVE → MATERIAL → PROJECT → COMMERCE → SYSTEM

The same source can be read through different relationships:
SPACE / LIFE / TIME
ARRIVAL / EDGE / VESSEL / THRESHOLD

Associative reading is a private/public interface grammar, not a claim that Plantupia formally categorizes projects this way.

## 3. Visual system
Lead with spatial credibility.
Support with botanical/editorial detail.
Introduce green technology as quiet infrastructure.

Palette:
- deep forest / near-black green
- warm mineral paper
- soft limestone
- muted living green
- pale living accent only for active state / continuity

Material:
- real project/proposal photography and catalogue imagery
- technical folio labels
- restrained paper/field-note surfaces
- no generic glassmorphism, sci-fi dashboards, meaningless grids or fake telemetry
- no AI-cinematic gloss

Typography:
- expressive editorial serif for spatial statements
- small technical mono/sans for evidence, state and provenance
- text must never overpower project material

## 4. Interaction grammar
Motion is structural, not decoration.
Use motion only to:
- preserve continuity between image states
- reveal a comparison
- connect project → object → system
- show that a choice or relationship persists

No scroll-jacking.
No mandatory long intros.
No hidden navigation on mobile.
Touch targets >=44 px.
Respect prefers-reduced-motion.
Keyboard path must remain complete.

## 5. Route roles
### /
World threshold and primary orientation.
Show Plantupia as spatial design + living material + green technology.
The first screen must work without explanatory paragraphs.
Then reveal PLACE / RELATIONS / SYSTEM / SHOP entry points from real source material.

### /spaces
Environment-led discovery, not a services grid.
Hospitality / Commercial / Offices & HQs are the current primary B2B focus.
Residential / Industry / Shared spaces remain available.
The selected environment should change image, reading and relevant case entry.

### /spaces/projects
Associative archive + normal project access.
Allow source-preserving project views and cross-project readings.
Do not flatten every project into identical cards.

### Individual case studies
The source decides behavior:
- Pixels: spatial sequence.
- Mazal: same-frame alternatives.
- EM Sherif / El Hallab: planting/specimen alternatives.
- Audi: room/chapter sequence + plant palette.
- Villa 7: broad choice atlas / areas and alternatives.
Always preserve source status and provenance.

### /studio
A working table, not an agency process infographic.
Read → Compose → Introduce → Return.
Source evidence from proposals should sit beside the method.

### /shop
Direct commerce inside the same world.
The visitor may arrive from a place, vessel, plant or material relation.
Never hide product identity, availability or the path to buy behind storytelling.
Spatial context supports commerce; it does not replace it.

### /living-systems
This is the Green Technology / continuity chapter.
Physical layer:
Install / Smart Irrigation / Scheduled Maintenance / Plant Warranty.
Digital layer:
QR Handover / My Plants / Care Schedule / Living History.
Keep both evidence types visually distinct.
No live sensor or automated diagnosis claims.

### /about
Explain the practice through the relationship of space, life and technology.
No invented founder history, team biographies or metrics.
Show sectors, method and evidence rather than generic corporate adjectives.

### /start-a-space
A calm working brief.
Project / Care / General remain separate modes.
Carry selected space notes forward.
If inbox is unavailable, preserve full drafting experience but do not fake submission.

## 6. Navigation
Persistent Plantupia signature.
Centered + remains World Index, not hamburger.
Shop remains directly reachable.
World Index should show the site as chapters of one world, with current location clear.
Mobile is authored independently; never a squeezed desktop overlay.

## 7. Technology principles
- Next.js server-first where possible.
- Native/Baseline browser behavior before heavy libraries.
- View Transitions only as progressive enhancement.
- No WebGL until a real spatial interaction proves it needs WebGL.
- Optimize real project imagery; responsive delivery and lazy loading outside critical scene.
- Build for reduced motion, keyboard, screen readers and 320px+ widths.
- Keep client state small and meaningful.
- No unnecessary analytics or third-party trackers in the design build.

## 8. Current reference transfers
### Field / archive / art
- Studio Olafur Eliasson / Your uncertain archive:
  living archive through associations and experiences; proximity can create new readings.
- Riverbed / spatial encounter:
  movement changes perception; the path can be the interface.
- Vitra Magazine:
  products, architecture, culture and authors can coexist as facets of one project/world.

### Commerce / universe
- Gentle Monster:
  distinct creative universes can lead into direct product access without separating story and commerce.
  Transfer the principle, never the visual identity.

### Biophilic / industry
- Green Fortune:
  spatial systems, integrated irrigation, maintenance and long-term thinking are part of the product.
- Benholm:
  design → installation → ongoing maintenance is a continuous service, not isolated decoration.
- Garden on the Wall:
  be precise about what living, preserved and artificial botanical material can and cannot claim.

### Contemporary web
2026 trends are filters, not instructions:
- useful: functional motion, experimental but understandable navigation, immersive depth, accessibility, sustainable performance
- reject unless earned: gratuitous 3D, neon futurism, maximalism for its own sake, gamification, dashboard aesthetics

## 9. Build order
1. Shared world shell + navigation + typography + motion primitives.
2. Home becomes the real World entry.
3. Spaces + Project Archive.
4. Source-specific case studies.
5. Studio.
6. Living Systems / Green Technology.
7. Shop integration.
8. About.
9. Start a Space.
10. Full integrated QA and protected Preview.

## 10. Acceptance / kill criteria
Reject any route if:
- copy carries the experience more than source material,
- images are cropped or normalized in a way that destroys source composition,
- a technology section looks like a fake dashboard,
- commerce becomes harder because of the art direction,
- the same template is imposed on materially different projects,
- mobile feels like compressed desktop,
- a claim exceeds source evidence,
- the experience needs an explanation to understand its primary action,
- motion exists without changing understanding,
- performance/accessibility regressions are knowingly accepted for visual effect.

A route is not complete until it passes:
- 320 / 390 / 768 / 1440 visual and overflow checks
- keyboard interaction
- reduced motion
- broken-image and console checks
- truthful source/provenance review
- production build
- protected Preview review

Production promotion is a separate explicit decision.
