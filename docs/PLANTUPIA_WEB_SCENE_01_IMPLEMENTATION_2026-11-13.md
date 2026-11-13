# Plantupia — A Space Becoming Alive / Scene 01
Date: 2026-11-13
Scope: **English-only desktop-first cinematic scene prototype**, in Draft PR #300. No master / PR #253 merge, official domain changes or Production rollout.

## What actually exists
Route: `/study/scene/`. The current homepage is **not** replaced or redirected.

- Three **explicit, reversible** stages: `The Space`, `With Life`, `Over Time`. Header modes, CTA, and arrows all change the same state. The bottom sequence indicates the current state rather than adding a competing second nav menu.
- Three active scene markers in every stage, with context-sensitive field notes. One button toggles the note open or closed.
- Centered `+` opens an immersive, keyboard-contained Experience Index. Escape and Close return focus.
- `Hold to compare` temporarily reveals the preceding state with pointer or keyboard, rather than autoplaying a fake time-lapse.
- Independent HTML labels and controls; not baked into a screenshot. Markers have accessible names and pressed state, background has descriptive accessibility text.
- `prefers-reduced-motion` avoids the 1050ms crossfade. Responsive fallback and desktop layout rules in `scene.css`.
- Explicit concept disclosure: NO proof of real installed projects, client data, measured growth or sensor readings.

## Media distinction / not yet signed off
The fully original approved scene work is available as three **clean** AVIF exports, not UI screenshots:
- `space.avif`: architectural room and sunlight before an intervention
- `life.avif`: immersive planted design
- `time.avif`: close botanical reading of the continuing relationship

These are NOT yet committed to the repository. To make the Vercel branch review usable, the current scene uses clearly disclosed **temporary Pexels editorial reference images** from different vantage points, not as a seamless same-camera morph and not as Plantupia projects.

**Required release gate:** integrate the three generated approved AVIFs at `apps/web/public/scene/{space,life,time}.avif`, change `visual` values in `app/study/scene/page.tsx` back to local paths, run build, and verify camera continuity. Avoid shipping a misleading transformation with different angles. Do not claim the original approved art is already live on Vercel.

## Scope still excluded
3D rendering, filmed scene, verified botanical growth, live API, real project portfolio, shop, contact forms, full site navigation, bilingual content, global homepage replacement.

## Interaction test plan
1. Open desktop 1440×900; verify no overlaid mockup text baked in imagery, + centered, three words at side and scene hotspots accessible.
2. Navigate via SPACE/LIFE/TIME and keyboard arrows. Verify only one selected top mode and bottom progress state agree.
3. Activate three hotspots on each stage; confirm the note changes, closes, has no duplicate data.
4. Press and hold COMPARE on stages 1 & 2, release to restore. Confirm disabled on stage 0. Check touch release and keyboard keyup.
5. Open + index. Tab + Shift-Tab must remain inside modal; Escape closes. Set chapter from index; returns to scene state.
6. Test English at 360/390/430/768/1024/1440 widths, no horizontal scroll, legible overlays, reduced motion, keyboard focus.
7. Verify site still remains behind Vercel Preview protection and `plantupia.com` untouched.

### Web asset credit (temporary online storyboard images)
- Pexels photo 28577085, a skylit architectural interior
- Pexels photo 33708096, an indoor atrium and planting
- Pexels photo 40883631, a planted atrium with hanging greenery

Pexels images are TEMPORARY, must not be represented as company projects, and should be removed in favor of the original generated concept assets for actual visual approval.

## Quality status
Typecheck and production build are necessary but insufficient. The production website is **not** authorized. Browser interaction, scene media and mobile visual acceptance remain open. This is an isolated design milestone, not a completed website.
