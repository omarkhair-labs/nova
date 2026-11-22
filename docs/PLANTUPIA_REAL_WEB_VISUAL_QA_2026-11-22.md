# Plantupia — Real Website Visual QA / 22 November 2026

## Scope
Refinement of the **existing public-web source** (`apps/web`) rather than a new lab, route or concept. Source branch: `design/plantupia-web-refinement-20261122`; same six-chapter navigation, six case-study bodies, material-led shop and care journey. Production alias was deliberately left unchanged.

## Connected preview
- Vercel project: `plantupia-public-preview`.
- Protected preview URL: https://plantupia-public-preview-3jm3y39e8-omarkhair70-droids-projects.vercel.app
- Created via authenticated CLI, using the actual worktree website source plus the existing private-review assets as an untracked staging layer.
- READY optimized cloud build; Vercel Authentication enabled. Never infer public access from authenticated CLI checks.
- Authenticated cloud checks: `/`, `/spaces`, `/spaces/projects`, `/spaces/projects/mazal`, `/spaces/offices`, `/studio`, `/living-systems`, `/about`, `/shop`, `/start-a-space` all HTTP 200. Two protected WebP assets returned HTTP 200 with `image/webp` content type.
- Git builds for arbitrary `design/*` branches are canceled *by the project's intended Ignored Build Step allowlist*, not because TypeScript or design failed. Do not remove that allowlist casually.

## What was actually checked
- Playwright audited 16 routes at 1440 x 900 and 390 x 844 (32 checks): all routes 200; no uncaught page errors; homepage alone initially had document-level horizontal overflow.
- Isolated cause: scaled cinematic images in `.ps-opening`. Fixed with `overflow-x:clip` on the opening stage. Retested at 390 and 1440: document width equals viewport exactly.
- Source-image audit: 93 WebP images from the previously curated 96 private review assets (three fonts are not photos); 90 are named in real website TS/TSX/CSS code and three (`residence.webp`, `terrace-evergreen.webp`, `terrace-lavender.webp`) are not referenced by current live pages. This does **not** mean all candidates from the nine source PDFs were approved for publication.
- The older 19-Nov inventory had 165 first-pass web candidates, but selection excludes pricing, inspiration, duplicates and weak references by default.
- Real image verification caught a misleading file name: `shop-office-greenery.webp` shows a Sandwichroom restaurant facade, not an office. It was **not adopted** as the Offices & HQs hero; the source-labelled vessel palette stays until an appropriate approved workplace source exists.

## Art-direction guardrails
- Keep original architectural image rhythm, visual contrast and movement where it genuinely supports the experience.
- Do not turn the archive into repeated generic cards, nor add features that already exist and call them new.
- Distinguish project *proposal images* from independently verified completed work. No fake before/after, growth or measurements.
- Review the menu, arrival, spatial field, case studies, Studio, Living Systems, Shop and Contact as one visual system, not independent template exercises.
- Private original source assets remain outside Git and are packaged only into the Vercel-authenticated preview stage.

## Outstanding
- Artistic acceptance of whole-page image choices and tempo remains open.
- Production public release requires explicit company copy/image permission and functional signoff. Enquiry submission remains disabled in protected Preview.
- The QA URL is a preview, not `plantupia-website-qa.vercel.app` or the public production alias.
