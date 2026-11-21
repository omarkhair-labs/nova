# Plantupia / Full World Build Review
Date: 2026-11-21
Branch: `design/plantupia-full-world-build-20261121`
Scope: protected full-site review candidate. Production promotion is not authorized by this document.

## What this build changes
The real website routes now share one product/world model:
**Spatial Design → Living Material → Green Technology → Continuing Care → Living History**

The home route `/` is the world entry, not a separate design-study route.

Implemented route roles:
- Home: world threshold, place, alternatives, object/material, relations, shop and system.
- Spaces: environment reader with SPACE / LIFE / SYSTEM lenses.
- Hospitality / Commercial / Offices: source-grounded sector narratives connected to project cases, material and Living Systems.
- Project Archive: direct PROJECTS reading plus associative RELATIONS (Arrival / Edge / Vessel / Threshold).
- Case studies: source-specific behavior remains distinct.
- Pixels Mall: rebuilt as an uncropped eight-scene spatial sequence; no fixed viewer or equal thumbnail frame.
- Studio: Read / Compose / Introduce / Return working table.
- Living Systems: physical care + digital continuity / Green Technology.
- Shop: live commerce preserved; material/spatial context supports rather than hides product access.
- About: Spatial Design / Living Material / Green Technology practice map.
- Start a Space: live working brief; preview submission remains disabled when inbox is unavailable.

## Truth boundaries
- Proposal images remain labelled as proposal studies where installation is not verified.
- Physical care terms are grounded in supplied proposals; exact service terms remain project-specific.
- Digital continuity represents current product direction: authorized QR handover, My Plants, Care Schedule and Living History.
- No live sensor feed, automated diagnosis, fabricated telemetry, invented sustainability metrics or unsupported client outcomes are introduced.
- Associative archive labels are a reading tool, not a formal company taxonomy.

## Verification
### Production build
- `npm run typecheck`: passed.
- `npm run build`: passed on Next.js 16.4.6.
- All expected public application routes included in optimized build.

### Integrated route audit
17 routes × 390×844 and 1440×900 = **34/34 passed** after final About heading repair.
Checks per route:
- HTTP 200
- no horizontal overflow
- no visible broken images
- no console/page errors
- primary heading present

### Edge viewport + reduced-motion audit
17 routes × 320×720 and 768×1024 plus World Index keyboard path and Pixels ratio checks.
**43/43 passed.**
- reduced-motion browser preference enabled during viewport route checks
- World Index opens from keyboard, closes with Escape and returns focus to trigger
- all eight Pixels proposal images preserve rendered vs natural aspect ratio within tolerance

### Interaction audit
390×844 and 1440×900 = **16/16 passed** across:
- Spaces SYSTEM lens selection
- Project Archive RELATIONS mode and VESSEL relation
- Mazal comparison swap
- Studio Return stage selection
- Start a Space carried field notes and Care & Support mode
- Living Systems physical/digital evidence lanes
- Shop filter or truthful catalogue-unavailable state
- zero captured console/page errors

## Evidence folders
Local QA evidence:
- `D:/02_QA-and-Builds/Plantupia/2026-11-21-full-world-qa`
- `D:/02_QA-and-Builds/Plantupia/2026-11-21-world-v4`

## Release boundary
This branch is a protected review candidate.
Do not:
- merge to production solely because automated QA is green,
- promote `plantupia-public-preview.vercel.app`,
- remove Vercel Authentication,
- claim publication/client approval for proposal imagery.

Next release gate: protected Preview review of the complete real route tree, then explicit production decision.
