# Plantupia — A Space Becoming Alive / Canonical Homepage Slice

Date: 2026-11-13  
Scope: English-only, desktop-first public creative website, **Draft PR #300**. No official `plantupia.com` cutover, production release, or merge into #253.

## What is implemented and where

- **`/`** is now the canonical cinematic homepage: `app/page.tsx` renders `app/CinematicExperience.tsx`.
- **`/study/scene`** redirects to `/`, preserving previously shared review links without two competing scenes.
- The old isometric **Living Atlas** has been removed from the homepage and kept in `app/LivingAtlas.tsx` and `app/atlas.css` for historical design reference, with no entry in the public world index.
- **Shared centered `+` SiteIndex** opens the same full-screen world navigation from `/`, `/spaces`, `/studio`, `/living-systems`. Each index item now resolves to a real page, not a placeholder.
- **Three stage buttons** SPACE / LIFE / TIME, main scene action and previous/next arrows all control one stage state; three scene markers per stage open distinct contextual field notes.
- **Hold to Compare** temporarily reveals the previous stage by pointer/touch or keyboard. It is disabled for the initial stage and releases to the current stage.
- Real HTML controls (not screenshots) have keyboard focus, descriptive names and selected/pressed semantics. Escape closes field notes or details. The shared index manages its own focus trap and Escape.
- Motion honors `prefers-reduced-motion`.

## Original scene assets — uploaded and live in code

Three originally generated **clean** AVIF files without embedded words/buttons were committed as Git blobs to:

- `apps/web/public/scene/space.avif` — bright architecture before planting;
- `apps/web/public/scene/life.avif` — same concept with integrated tropical planting;
- `apps/web/public/scene/time.avif` — closer botanical reading of a living space.

Each is 960×540. `CinematicExperience.tsx` uses their local `/scene/...` URLs, not temporary third-party editorial images. The CI server smoke test downloads all three assets and checks nontrivial sizes.

**Important remaining art-direction limitation:** the third image changes viewpoint. The image sequence is a conceptual design reveal, **not** frame-accurate 3D camera animation, biological growth, measured air/wellbeing improvement, live data or a verified installed Plantupia project. The 960px source resolution also needs real desktop inspection.

## Acceptance status

- Verified: Vercel successful build on the PR preview and GitHub `Plantupia Public Web CI` production build, TypeScript, all four HTTP routes, the old route's redirect, and three AVIF assets.
- **Still open:** interactive visual browser/device QA on 360/390/430/768/1024/1440 widths, comparison touch gestures, focus trap, shot continuity, image quality under large displays, final desktop composition and motion pacing. No automated authenticated browser run was completed.
- **Still open:** stylistic convergence of Spaces / Studio / Living Systems with the approved cinematic art direction. They are structurally linked prototypes, not visually signed-off chapters.

## Scope boundary / what comes next

Do not reintroduce Living Atlas as a competing homepage or add more unconnected demos. Focus next on one coherently art-directed website:

1. Evaluate the cinematic homepage at native desktop size, refine camera/color grading and the TIME closer view without misleading time-lapse claims.
2. Upgrade the three linked page worlds to the same level of art direction while preserving their distinct purposes.
3. Review the central World Index and all interactive states on real devices, accessibility and performance.
4. Keep project photos, claims and commerce integrations evidence-based. No domain promotion or Production release without approval.
