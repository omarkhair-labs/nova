# Studio / source-led redevelopment — 2026-11-19

Scope: private local review. Existing four-stage authored architectural reading preserved; public/default version remains unchanged. Private mode now connects each phase to supplied Plantupia proposal evidence rather than unrelated editorial photography.

Evidence mapping:
- Read / Pixels Mall p28: architecture, circulation and spatial conditions.
- Compose / Mazal option 03 p5: alternative planting directions applied to one facade.
- Introduce / EM Sherif option E p7 + planter specification: move from atmosphere to object, dimensions and coordination.
- Return / Audi Cairo Living Interiors p14 + p17 care programme: continuing care after installation.
A four-act source-proof section explicitly labels this as a reading of the supplied proposals, not a claim that every project follows one fixed process.

Implementation: StudioAtelier accepts privateReview from app/studio/page.tsx; source-backed image/caption/output block and proposal-evidence section added. Original diagram, scroll reading, navigation and chapter transition preserved. Initial browser run exposed a repeated hydration mismatch around the dynamic SVG title. Replaced dynamic title/desc nodes with a stable aria-label; TypeScript passed and retest cleared the error.

Verification: 4 stages x 4 viewports = 16 stage checks at 320/390/768/1440; four proof rows; no horizontal overflow; zero page errors after the SVG fix. Project archive link and Studio -> Living Systems transition verified. Evidence: temp/studio-source-review/results.json and screenshots. No commit/push/deploy; final client/publication acceptance pending.