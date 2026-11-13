# Plantupia Creative Website — Whole-Site Integration / Slice 01

**Status:** English-only, desktop-led, connected design preview. **Not** the official Plantupia website or a production release.

## Creative direction
Architecture sets the stage. Plants change the experience of that space. Care and time keep that relationship alive.

The project intentionally rejects a conventional five-link corporate header and a scroll of generic marketing sections. Its navigation has three levels:

1. **Center +** — a full-screen, accessible **World Index** for the four real website experiences.
2. **Within an experience** — local contextual controls that change the scene, the studio plan, or the conceptual relationship being explained.
3. **Within a specific scene** — hotspots and explicit before/after/sequence controls, not decorative dead buttons.

This is an overarching **creative company website**, not only a cinematic hero or a standalone interactive art piece. The mobile fallback must work, but **desktop is the primary visual composition**.

## Actual app routes

| Route | Role | Current implementation |
|--|--|--|
| `/` | **The Experience** | Original interactive Living Atlas, currently the main preview; shared center + now connects it to the other real routes; direct route into cinematic Scene 01 |
| `/spaces` | **Spaces** | Original interactive spatial studies for dwelling / work / gathering, with scene composition and contextual notes; not case studies or client projects |
| `/studio` | **The Studio** | Four selectable design-process layers; diagram actively changes with Observe / Compose / Introduce / Return |
| `/living-systems` | **Living Systems** | Three connected conceptual relationship states: installation, human care and recorded memory; no live customer data or fake sensor statistics |
| `/study/scene` | **Cinematic Scene 01** | Three editorial visual states, interactive hotspots, stage selection, keyboard arrows, Hold to Compare, + world index. **Temporary reference photography** is not camera-aligned original scene media |
| `/study/navigation` | **Legacy navigation experiment** | Standalone historic study; keep for design reference, not part of main world index |

No Arabic version. No incomplete fake portfolio or services routes. All pages have `noindex` until approval. Nothing changes `plantupia.com`.

## Development boundaries
- GitHub **Draft PR #300**, based on consolidated Plantupia Draft **#253**.
- Vercel linked project `plantupia-public-preview`, branch previews only.
- The current main homepage Living Atlas is a **working prototype**. The cinematic asset approval and transition QA are open. Don't claim it already matches the approved art-direction mockup.
- No Android, backend, Company Dashboard, commerce data, domain/DNS, private data or production account changes in this workstream.
- Public web should eventually communicate company truth without copy-pasting the Android care UI or inventing proof of completed projects.

## Slice stages — avoid a mass launch
1. **Whole-site skeleton and real index routes (this slice)**: connect the four experiences, give each purposeful interaction, keyboard navigability and consistent design language. Build, TypeScript and route smoke tests should pass.
2. **Cinematic imagery and Scene 01 quality**: integrate original clean assets, control one precise camera, align shots, tune lighting/compositing and performance; no false growth animation.
3. **Experience quality across the site**: refine architecture of Spaces, Studio and Living Systems into more cinematic, distinctive, accessible compositions. Avoid a giant marketing scroll.
4. **Company content truth**: only after evidence is available, fit real services/projects/photos into these experiences, plus any appropriate usable information architecture.
5. **Release review**: desktop/phone visual QA, interaction tests, Lighthouse/performance, reduced-motion, accessibility, media licenses, legal/company approval, preview sign-off, and only then discuss production/domain migration.

## Run and verify
```bash
cd apps/web
npm install --no-audit --no-fund
npm run typecheck
npm run build
npm run dev
```

GitHub workflow `Plantupia Public Web CI` runs TypeScript, a production build and HTTP smoke tests for `/`, `/spaces`, `/studio`, `/living-systems` and `/study/scene`.

Original research and scene acceptance notes are retained under `docs/PLANTUPIA_WEB_*`. Screen-level accessibility, real-browser scene QA and authoritative visual sign-off are separate from successful builds.
