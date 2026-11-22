# Plantupia — Continuous Material Motion study
Date: 2026-11-22
Status: research and measured diagnosis for the existing apps/web website. No animation code deployed in this study.

## Desired art direction
The next scene should feel gently drawn into place while the previous scene retains its material weight for a moment. The effect is **continuity of light, composition and layered presence** rather than generic fading, long global scrolling inertia, literal glass panels, or a permanent transparent film. Do not flatten the original architectural photography.

Distinguish the material:
- Same architectural place, alternate planting (Pixels p08/p11 and Hospitality catalogue choices): hold the registered frame, reveal the different planting treatment. No before/after or growth claim.
- Different location or chapter (Home three independent scenes; case-to-case navigation): pass the visual rhythm and image weight, not morph separate buildings into one place.

## Verified state of current site
The local website was inspected at 1440×900 and 390×844 on /spaces/projects/pixels-mall.
- The two Pixels alternatives are overlapping image tags; the active image is opacity 1, inactive opacity 0.
- Current CSS transitions both images independently with opacity 0.38s ease-out, with computed z-index auto.
- Behind both is a fixed RGB(207,220,207) background. If both image alphas reach 0.5 halfway, combined composited coverage is 0.75, exposing approximately 25% plate color. This can cause an unintentional washed-out intermediate frame, rather than deliberate luminous overlap.
- Both directions depend on DOM stacking order: forward and backward do not have explicit consistent incoming/on-top behavior.
- Home image handoff is opacity 0.48s plus a small scale settling over 0.7s. Spaces sector swaps take opacity 0.42s. These timings and optical mechanisms are not yet one authored motion language.
- Browser screenshot operations are asynchronous and slow enough that this baseline is **not** a valid exact-frame FPS benchmark. No FPS or latency gain is claimed.
- The old scroll-velocity Rapid Index overlay was removed after user-reported flashing. It must remain absent.

## Proposed one-family motion grammar: HOLD → RECEIVE → SETTLE
1. **Hold the frame.** Decode/preload the destination image before revealing it. Keep the outgoing image fully present rather than simultaneously fading both pictures to the background. Explicitly determine incoming and outgoing layer order in either direction.
2. **Receive the new material.** Bring the incoming image as a real layer over the outgoing one, with controlled translucent overlap, matching the existing architecture's light. For same-position planting comparisons, the geometry should stay registered. Do not create fake physical change.
3. **Settle gently.** A subtle optional translation of no more than about 8–12 pixels or a tiny (~1%) settle may give the pulled/silk feel. Neither the text nor the image should stretch, jelly-warp, or look like a stock cinematic preset.
4. **Stay responsive.** Never block pointer, keyboard, or touch while the transition plays. Rapid repeat clicks should cancel/re-target the previous motion cleanly. No blank intermediate frames.
5. **Use one easing intention, different appropriate durations.** Starting test bands (not a final visual decree): interaction feedback 120–220ms, within-image handoff 400–600ms, shared-image page handoff 450–700ms. Choose final durations by actual frame review.
6. **No global scroll hijacking.** Native mouse/trackpad/touch scrolling remains intact. A subtle once-on-entry section reveal may be evaluated later, but never an automatic full-screen alternative index.
7. **Image-to-page continuity only where meaningful.** Test View Transitions/shared-image identity from Archive/CaseTrail photo to the matching case lead in the current Next.js router. Provide no-animation fallback and do not install a second router.
8. **Keep the image true.** No full-page animated backdrop-filter/blur, permanent pale overlay, heavy WebGL shader, or floating glass card. Consider subtle tonal veiling only at the moment two frames meet, if it genuinely improves the photography.

## First small production-intended test, not a new lab
Perform the reversible change in the **existing** Pixels p08/p11 comparison, on the existing development branch. Compare before and after in both directions on desktop 1440 and mobile 390. Only expand to Hospitality, Home, Spaces, and shared case navigation after visual review and profiling.
Acceptance criteria: source images remain correctly labelled and fully loaded; no flash or exposed plate; same feeling forward and backward; no layout shift/scroll takeover; reduced-motion preference provides no or shortened animation; keyboard/Back works; browser timeline has no damaging long tasks or unexpected memory growth. Measure frame rendering on a real mid-range device instead of claiming 60 FPS from a local build.

## Reference principles, not copied UI
- MDN: https://developer.mozilla.org/en-US/docs/Web/API/View_Transition_API/Using
- Chrome: https://developer.chrome.com/docs/modern-web-guidance/guides/ui-behaviors/same-document-transitions
- Codrops image layering and reveal: https://tympanus.net/codrops/2023/10/31/image-layer-animations-with-clip-path/
- web.dev animations: https://web.dev/explore/animations
- MDN reduced-motion: https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@media/prefers-reduced-motion

Decision: **study supports intentional image/material continuity, not adopting a general-purpose smooth-scroll plugin**. Do not change public Production or turn this into a standalone lab. First establish the correct visual feel in one real Pixels comparison and critique that feeling before extending it.
