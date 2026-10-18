# Plantupia Portrait Lab · Study 01 — Existing eight vs two original vector studies

**18 October 2026 · DRAFT ART REVIEW · NOT AN ANDROID SCREENSHOT · NOT SHIPPED**

This is the first execution of [our iterative research and making-of method](../PLANTUPIA_ITERATIVE_RESEARCH_TO_NATIVE_ART_PROTOCOL_2026-10-18.md), built from the existing [Pass 09 one-system visual direction](../PLANTUPIA_PASS09_ONE_SYSTEM_VISUAL_EXECUTION_MAP_2026-10-17.md). It **does not** replace any profile image, saved avatar choice, Android composable, production asset or server data.

## Source assets

- [Concept A — **The Window**](plantupia-avatar-window-experiment.svg), original 1000×1000 SVG portrait: off-center swept contour, split coat, tall architectural window, botanical accent.
- [Concept B — **The Courtyard**](plantupia-avatar-courtyard-experiment.svg), original 1000×1000 SVG portrait: broad sculpted crown, draped neck layer, courtyard arch, secondary botanical mark.
- The **current eight** were reconstructed in a local comparative art board from `app/src/main/java/com/nova/app/ui/components/PlantupiaAvatar.kt` at the exact PR head. That reconstruction closely follows its authored geometry and chosen-variant palette mapping, but is **not** a screenshot from Android's Compose runtime. The high-resolution PNG comparison was provided in the conversation on 18 October; it is intentionally not represented as an already-delivered Android capture in the repository.

## Comparative art-direction notes

| Dimension | Current `0..7` Compose avatars | Concept A — Window | Concept B — Courtyard |
| --- | --- | --- | --- |
| Figure distinction | Eight hairstyles/contours + garment accents, but most head/face foundations share geometry | Asymmetrical sweep, long face shape, angled tailored coat | Wide rounded head mass, curled graphic crown, neck scarf and different garment shoulder line |
| Architectural context | Four thin frames with different strokes | Tangible window plane with botanical shadow | Rounded architectural courtyard portal |
| Botanical relation | Stem and one repeat leaf token | Leaves climb a window, away from face | A contextual botanical arc at the right |
| 32–40dp | Present but small illustrated differences compress quickly | Hair + dark coat shape read; thin nose/face details become subpixel | Broad hair/scarf shape reads; tiny trim/brow detail becomes subpixel |
| Brand cohesion | Limited tonal range, lightweight graphic language | Compatible forest/cream palette but significantly more detailed rendering | Compatible sage/forest palette but differently rendered face/body planes |
| Native readiness | Working Compose source, NOT art approved | Original SVG experiment, **not** yet VectorDrawable/Compose asset | Original SVG experiment, **not** yet VectorDrawable/Compose asset |

### First critique — do not declare a winner too early

The two experiments demonstrate that **larger structural differences** can improve human recognizability more than another palette, hair stroke or small accessory. However, as a family they are still only two samples: their head scales, facial feature vocabulary, leaf styling, line weights and material shading must be standardized before expanding to 8–12 characters. They are **not yet a replacement set**. Both currently include face details that will not survive a tiny avatar. Preserve human dignity and do not tie any look to the user's name, perceived gender or other account attributes.

**Next experiment:** design one common three-tier detail grammar from these sources:
- **Micro 24–40dp** — head/hair/shoulder silhouette + palette only; omit nose/face micro-detail and tiny botanical debris; preserve distinguishable body mass.
- **Standard 48–64dp** — a few readable facial/garment marks and one restrained spatial element.
- **Hero 80–112dp** — full authored human portrait, architectural relationship and selected detail.
All tiers must be three depictions of **the same selected account identity**; no random fallback, no changing IDs on refresh.

**Device proof:** render true Compose 24/32/40/48/64/96dp (not just raster resize), real photo override, no-photo fallback, long display name, missing image, account switch, deleted account, 1× and 2× text, TalkBack. Compare identical inputs and capture screenshots on actual target Android. Preserve existing saved `plantupia_avatar_variant` values `-1..7` and original server account ID semantics.

## Creative + technical next gate

1. Compare portrait design against the **six existing care pictograms** in `PlantupiaVisualComponents.kt`, and original no-space/no-history states: thickness and detail can differ by semantic role, but each must feel authored by the same art direction, not a copied illustration set.
2. Choose a documented vector authoring/export method and test whether SVG paths faithfully import to Android `VectorDrawable` or Compose `ImageVector`; flag unsupported effects and verify performance and anti-aliasing.
3. Art-direct a **complete family** before shipping any replacement; review low- and high-detail variants on the same board, then make an opt-in test build. Do not silently replace selected avatars, delete original media or change production behavior.
4. Revisit alternative makers, editorial illustration systems and pipeline tutorials whenever the quality stalls, but make each search answer a specific outstanding decision. Avoid uncontrolled moodboarding.

**Status:** initial physical visual sheet rendered; two original editable SVG source experiments committed; comparative critique written. **Native vector conversion, full family, real device, author approval: OPEN.**
