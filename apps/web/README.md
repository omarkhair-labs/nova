# Plantupia Creative Website — Whole-Site Blueprint + Chapter Threshold / Slice 02

**Status:** English-only, desktop-led, connected design preview. **Not** the official Plantupia website or a production release.

## Website experience blueprint

The opening cinematic scene is only the **first chapter**, not the whole site. A live visitor can continue by scrolling into an interactive **Chapter Threshold** (choose Spaces / Studio / Living Systems, with one media field and three selectable chapter doors) or use the center `+` to jump straight into the World Index. The selected chapter changes the editorial image and text, and **ENTER [CHAPTER]** navigates to an actual route.

**Source of truth for future work:** [Full website Experience Blueprint V01](../../docs/PLANTUPIA_WEB_EXPERIENCE_BLUEPRINT_V01_2026-11-13.md).

**Cinematic chapter visual integration (Slice 03):** [Cinematic Chapters / source credits & acceptance](../../docs/PLANTUPIA_WEB_CINEMATIC_CHAPTERS_SLICE_03_2026-11-13.md). The spatial, studio and continuity scenes now use a shared architectural photographic language rather than separate cartoon CSS artwork. Sources are temporary licensed editorial **reference locations**, not actual Plantupia project photos or documented outcomes.

The current preview also has an optional **business-action path**: `/start-a-space` lets visitors prepare and copy a project outline, but does not collect or submit it. Real delivery to a Plantupia-owned channel must pass the contact/privacy/CRM approval gate.

The complete target includes four current chapters and three gated additions: Botanical Library (only with reviewed species data), Shop (only through the canonical product/checkout system) and Work/Projects (only with verified, approved project assets). No fake empty routes have been exposed.

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
| `/` | **The Experience** | Canonical cinematic **A Space Becoming Alive** opening: three independent high-resolution editorial photo references, controls, hotspots and World Index; **continues into the interactive Chapter Threshold** linking real Spaces, Studio and Living Systems chapters |
| `/spaces` | **Spaces** | Three independent editorial photo locations for dwelling/work/gathering, plus selectable light/movement/care design prompts; contextual next step to `/start-a-space` |
| `/studio` | **The Studio** | Four selectable Observe / Compose / Introduce / Return stages with changing original conceptual blueprint layers over one reference architectural photograph |
| `/living-systems` | **Living Systems** | Three selectable place/care/memory stages against a botanical editorial scene and honest conceptual continuity timeline; not a real care dashboard or live telemetry |
| `/start-a-space` | **Space Conversation Outline** | Interactive client-side brief with four choices, live dossier and copyable text; explicitly **not an enquiry submission**, no server-side lead capture or contact data collection |
| `/study/scene` | **Former prototype URL** | Redirects to `/` so previously shared links still reach the canonical experience; no duplicate scene copy |
| `/study/navigation` | **Legacy navigation experiment** | Standalone historic study; keep for design reference, not part of main world index |

No Arabic version. No incomplete fake portfolio or services routes. All pages have `noindex` until approval. Nothing changes `plantupia.com`.

## Development boundaries
- GitHub **Draft PR #300**, based on consolidated Plantupia Draft **#253**.
- Vercel linked project `plantupia-public-preview`, branch previews only.
- The **Living Atlas** (`app/LivingAtlas.tsx` and `app/atlas.css`) is archived as source-only reference and is not the homepage or linked in the world index. The cinematic homepage currently uses three temporary Pexels photographic studies from **different locations**, with local generated concept fallback files at `apps/web/public/scene/hd/{space,life,time}.webp`. These are **not** before/after evidence of one place, not Plantupia installations, and not a growth time-lapse. This photographic treatment is approved only as an interim visual direction; final site imagery and interaction QA remain open.
- No Android, backend, Company Dashboard, commerce data, domain/DNS, private data or production account changes in this workstream.
- Public web should eventually communicate company truth without copy-pasting the Android care UI or inventing proof of completed projects.

## Slice stages — avoid a mass launch
1. **Whole-site skeleton and Chapter Threshold (current slice)**: connect four real experiences through the shared index, and place an interactive post-opening gateway on the homepage to actually continue the visitor journey. Review mobile/desktop composition, keyboard selection, route semantics and photo disclosures.
2. **Cinematic quality**: improve the interim real-photo atmosphere while producing a genuine single-space, same-camera art-directed sequence for the definitive reveal. Do not call three locations a before/after; remove photo references when the coherent higher-quality scene passes desktop QA.
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

GitHub workflow `Plantupia Public Web CI` runs TypeScript, a production build and HTTP smoke tests for `/`, `/spaces`, `/studio`, `/living-systems`, `/start-a-space`, the redirect from `/study/scene`, local HD WebP fallback scene assets, the homepage's Chapter Threshold and the first client-side project-brief path.

Original research and scene acceptance notes are retained under `docs/PLANTUPIA_WEB_*`. Screen-level accessibility, real-browser scene QA and authoritative visual sign-off are separate from successful builds.

## Temporary photo sources (not Plantupia projects)
- SPACE — Öznur Yüksel, [Modern Architectural Interior with Skylight](https://www.pexels.com/photo/modern-architectural-interior-with-skylight-28577085/).
- LIFE — David Allen, [Indoor Atrium with Lush Greenery](https://www.pexels.com/photo/indoor-atrium-with-lush-greenery-and-modern-architecture-33708096/).
- TIME — Rahime Gül, [Indoor Atrium with Hanging Greenery and Skylight](https://www.pexels.com/photo/indoor-atrium-with-hanging-greenery-and-skylight-40883631/).
- [Pexels License](https://www.pexels.com/license/) allows website use, subject to its restrictions and any additional third-party rights. Do not imply Plantupia designed, installed or owns the pictured architecture.
