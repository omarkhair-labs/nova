# PLANTUPIA — One Living Language / Coherence Study
- Date: 2026-11-22
- Status: Private, isolated creative research — not an approved brand or production redesign
- Base: `design/plantupia-creative-lab-20261122`
- Study route: `/study/coherence`
- Worktree branch: `design/plantupia-coherent-study-20261122`

## Why this study exists
The earlier LUMEN, STRATA and AFTERIMAGE experiments proved separate behaviours, but their independent palettes and headers made it too easy to judge three different websites instead of discovering the visual language of *one* Plantupia. The product identity is already wider than spatial design alone: plant ownership, commerce, care and continuity belong to Green Technology.

This study is **not a vote for one direction**, not an attempt to copy Plantwire and not a new set of approved slogans. Its purpose is to see whether one existing Plantupia colour/motion/navigation family can hold several source-specific ways of seeing.

## What this route preserves
- Forest, plaster and subdued olive from existing Plantupia art direction rather than a new unrelated palette.
- The **real SiteIndex** centred `+` world index and its keyboard/Escape behaviour; no new fake menu.
- Source-led photographs only; labels make proposal/catalogue status explicit.
- Existing destinations for Living Systems and Shop.
- The original Creative Lab and all existing public routes remain untouched.

## The connected sequence
1. **Encounter / space:** Pixels Mall, Audi Cairo and Villa 7 proposals rotate within one spatial register; photographic work is primary.
2. **Decision / planting:** two Mazal options appear in a registered comparison. Same architectural viewpoint; changing planting choice. It is never described as built before/after.
3. **Material / context:** hospitality catalogue alternatives are read in setting, as vessel, and as a family of materials. Their images are not forced into equal-height product cards.
4. **Continuity / care:** a concise link toward the actual living system. No invented telemetry or growth claims.

## Critical visual findings
- A private research header should not reintroduce a second SHOP link alongside the shared SiteIndex; removed after screenshot review.
- Mazal source files are **1920×770** (panoramic). The initial mobile frame was artificially tall and produced a large grey letterbox. The study now respects source aspect ratio; a future deliberate image inspection mode may be worth testing, but cropping away the planting design just to fill portrait space would destroy evidence.
- Image behaviour must follow source aspect ratio and the question being asked. **Not every image is a cinematic hero and not every archive item is a card.**

## QA evidence and limitations
- TypeScript check passed before final visual refinements.
- First optimized Next.js build passed before final visual refinements.
- Playwright on 390×844 and 1440×900: HTTP 200, no document horizontal overflow, scene selection passes, comparison input passes, material selection passes, original SiteIndex opens and closes with Escape, no page errors.
- The image-state probe ran before the lazily loaded material images entered view, so it undercounted three valid image sources. Direct HEAD checks of those three local routes each returned 200. The distinction between **not yet loaded** and **broken image** matters.
- Final CSS/header refinements should receive a fresh build and screenshot check before any acceptance claim.
- Test is local only. This is not a production promotion, design sign-off or verified client photo-use authorization.

## Next art-direction review
Compare the *same photographic material* in the actual production site and the study. Evaluate photographic prominence, range of forest/plaster tones, typography pace, plus-menu opening and mobile composition. Critique the entire experience together rather than picking the 'best' individual lab concept. No deployment/merge until the direction survives this comparison and business claims/photo rights are independently validated.
