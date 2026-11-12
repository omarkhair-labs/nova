# Plantupia · Photo Framing and Living History
**19 October 2026 · Native Android implementation on PR #241 (Draft) · Device art/UX acceptance open**

This document continues the existing [Pass 09 visual execution system](./PLANTUPIA_PASS09_ONE_SYSTEM_VISUAL_EXECUTION_MAP_2026-10-17.md), [Art Lab](./art-lab/README.md), and [closure ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md). No Play, production, server migration or fabricated media changes.

## Product meaning: an authored view must never rewrite evidence

Plantupia has two fundamentally different uses for a user's plant photo:

- **Cover / place storytelling** — Home, Today, Me, Recent Moments, plant inventory and a living-place card can center-crop the original to compose the surface. A Home "place" hero that uses a plant cover must identify the source as **a photograph of that plant in the chosen place**, not as documentary proof that the whole architectural space looks like this. If loading fails, visual identity reverts to the honest sage/ink state.
- **Recorded evidence / field history** — a user-submitted care-event photo and the History photo index should show the entire recorded image in the allotted frame, preserving visual context and edges. The original is always available in the fullscreen viewer. Do not automatically label "before/after improvement", infer growth, pretend an edit changed the original evidence, or substitute an generated picture.
- **Unsaved photo picker** — a locally selected new photograph is a draft; it is **not a recorded event until saved**. Its composition/preview contract remains separate from historical media.

Official platform support: [Android ContentScale Fit/Crop guide](https://developer.android.com/develop/ui/compose/graphics/images/customize); [Android transformable gesture guide](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch); [Compose graphicsLayer mechanics](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers); [Coil 3 AsyncImage loading/success/error](https://coil-kt.github.io/coil/api/coil-compose-core/coil3.compose/-async-image.html).

## Implemented in code (not claims of physical-device acceptance)

1. `PlantupiaEvidencePhoto.kt`: one source-aware Compose image renderer now has an explicit `PlantupiaPhotoFraming.COVER` (native `ContentScale.Crop`) or `FULL_RECORD` (native `ContentScale.Fit`). Existing authenticated media loading, failure/empty state and retry remain, with no uploads or new server data. The full-record treatment is applied to `PlantupiaHistoryEventCard` and `PlantupiaPhotoIndex`. The cover treatment stays in the designated card and place surfaces.
2. `PlantupiaPhotoGeometry.kt`: screenshot-size-independent pure geometry computes the legal pan range from viewport, decoded photo aspect ratio, and actual zoom, rather than old hard-coded 550px/850px clamps. Axis with no visual overflow cannot pan. Invalid dimensions and 1× zoom produce zero translation.
3. `PlantupiaPhotoViewer.kt`: reads Coil's decoded `Image.width/height`, observes the actual Compose viewer dimensions, clamps gestures and resize/zoom to the fitted image, disables transformation while still loading, and supplies a visible `Zoom 2× / Reset` action for people who cannot pinch. The original image remains Fit; full-image retry and zoom prompts remain truthful.
4. `PlantupiaHistoryTimeline.kt`: `plantupiaLoadedPhotoPair` chooses an *earliest loaded* and *latest loaded* pair only when they are **different user records, different actual URLs, and different valid parsed dates**. The engine does not invent absent photos, interpolate missing history or claim the plant changed.
5. `PlantupiaPhotoIndex.kt`: a contextual two-column comparison is shown **only for an eligible real two-date pair**, along with real recorded dates and a note if earlier pages could change the comparison. With a single photo, same-day pair, duplicate photo URL or incomplete evidence, the normal photo index remains. The comparison opens only an actually loaded image.
6. JVM tests: `PlantupiaPhotoGeometryTest` covers portrait, panorama, square, zoom=1 and bad dimensions; `PlantupiaEvidencePhotoPolicyTest` covers Crop vs Fit contract; `PlantupiaHistoryTimelineTest` covers real, duplicate, same-day, malformed and single-photo comparisons.

## Critical boundaries / open questions

- **Code implementation is not art acceptance.** Exact-head Android/Django CI must pass. Native screenshots are still needed for 320, 360, 390 and 430dp frames and common photo aspects: portrait 3:4, landscape 4:3/16:9, square 1:1, very tall/wide records. Test density, screen rotation, 1–4× zoom, accessibility/focus, error and slow network states.
- Confirm the Photo Viewer has appropriate gesture conflicts/resizing behavior on device. The source-faithful pan math uses the aspect ratio of Coil's successfully decoded image, but native screenshot testing must verify visual limits and image orientation.
- Even on a multi-record plant, the earliest **loaded** image isn't guaranteed to be the first-ever photo if older pages exist. Never label the selected photos as authoritative start/end or invent health scores.
- Review color contrast and meaning across existing diagrams, portraits and notes with owner on actual Android UI. A small number of test screenshots, not an unbounded inspiration loop, should drive the next illustration/crop iteration.

**Evidence gate:** PR #241 + last green SHA when available; until then status is CODED and CI/DEVICE UNVERIFIED. Keep all existing photos, private events, accounts and branch compatibility intact.
