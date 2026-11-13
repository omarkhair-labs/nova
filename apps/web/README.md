# Plantupia Living Atlas — Public Web / Interactive Design Study V02

**Status: Draft, running on a protected Vercel review deployment. Not the official company website.**

This application is a deliberately independent public web exploration. It does not substitute for the Android care app, the one canonical customer Shop or the internal Company platform.

## Product thesis
A place is never still. Architecture, plants, people and time form one relationship:
**PLACE → LIVING MATERIAL → HUMAN CARE → TIME**.

The previous version was essentially an English/Arabic typographic landing page. This iteration replaces it with an explorable spatial field guide rather than stacking the same marketing components with larger text.

## Where it lives
- GitHub Draft PR #300, based on consolidated Plantupia Draft #253.
- Next.js 16 / React 19 / TypeScript, at `apps/web`.
- Vercel linked project: `plantupia-public-preview`. Auto-generated Vercel branch previews are separate from the official domain, and may require Vercel authentication.
- English-only `/`. The requested site does not offer an Arabic route or language switch.

## Experience
- Interactive SVG architectural dioramas for dwelling, work and gather typologies, all **fictional design studies**, not built Plantupia projects.
- Three distinct lenses: SPACE, LIVING PLANT and TIME.
- Three accessible contextual hotspots in every lens: select or use the Previous/Next note controls.
- Time lens contains a chosen conceptual time sequence (not a plant timeline or measured sensor series).
- Focus mode to see the artwork more clearly; Escape exits.
- Four-layer interactive method folio with selection and original generative geometric motion.
- Responsive desktop/tablet/phone, English editorial UI, visible keyboard selection semantics and reduced-motion fallback.
- No stock photos, fake installations, fake plant care records, public authentication, checkout clones, silent external scripts, dead lead forms or implied official endorsements.
- No SEO indexing until company approval.

## Run and validate
```bash
cd apps/web
npm install --no-audit --no-fund
npm run typecheck
npm run build
npm run dev
```

The `Plantupia Public Web CI` GitHub workflow runs TypeScript checks and a production build. Actual physical-device and visual signoff is a separate acceptance gate.

## Research / design decisions
See [Living Atlas Research & Interaction System V02](../../docs/PLANTUPIA_WEB_LIVING_ATLAS_RESEARCH_AND_INTERACTION_V02_2026-11-13.md).

## Next web-only improvements
1. Inspect every interactive state on real Android/iOS browsers and 320–1440px viewports; correct overlap, loading, legibility and English typographic rhythm.
2. Explore more visually distinctive representations of light, placement and care, based on verified design drawings or licensed source material (not generic stock "plant project" imagery).
3. Add intentional accessible transitions, optional spatial sound only if consented and meaningful, and performance budgets.
4. Resolve the central product distinction: this Living Atlas is currently an interactive concept, not yet a complete creative company website. Evaluate it as the entry to genuine Work, Studio, Living Systems and Journal routes rather than treating two interactive scenes as a finished site.
5. Only after coherent direction is approved should we consider official brand assets, real case-study media, external services, domain cutover or integration with reviewed catalog data.

Current `plantupia.com`, Android, Django, Company Dashboard and production data are unchanged.
