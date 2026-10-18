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

## First native implementation experiment — staged, not shipped

Two separately named Android XML vectors are generated from our original SVG studies:
- `app/src/main/res/drawable/plantupia_artlab_window.xml`
- `app/src/main/res/drawable/plantupia_artlab_courtyard.xml`

`app/src/main/java/com/nova/app/ui/components/PlantupiaPortraitArtLabPreview.kt` uses `painterResource` + Compose's actual current `PlantupiaAvatar` at **96dp and 40dp** to compare the two designs side-by-side in Android Studio. The comparator deliberately has **no route and no account state write**. Android build/VectorDrawable aapt validation on its exact commit SHA is pending, and an IDE Preview is *not* represented here as already captured on a physical device.

**Technical caveat:** these native XML conversions trace the authored SVG path geometry and opacity with Android vector paths; Android's handling of clipping, antialiasing, joins and 32dp rasterization may differ from desktop SVG. Inspect and refine against actual preview renders before calling visual parity. If the import loses details, simplify the authoring curves or use a supported Compose drawing mechanism; never silently switch to bitmap output.

## Responsive portrait detail pass — code + native resources, 18 October

**Implemented in the review branch (not visually approved):**
- The existing `PlantupiaAvatar.kt` makes its visual detail conditional on requested logical size: `Micro` at up to 40dp, `Standard` at 41–79dp, and `Hero` at 80dp or larger. The original stable `userId` / chosen `-1..7` design contract, authorized uploaded photo precedence and semantics remain unchanged.
- At micro sizes, facial lines and repeated leafy details are omitted to preserve the silhouette. Standard gets readable face detail and a single leaf. Hero retains the compositional architecture and full botanical signature. **This improves the existing family technically; it does not magically raise all eight portraits to finished illustration quality.**
- Two original simplified source portraits: [The Window / micro](plantupia-avatar-window-micro.svg) and [The Courtyard / micro](plantupia-avatar-courtyard-micro.svg). Their native review-only `VectorDrawable` resources are `plantupia_artlab_window_micro.xml` and `plantupia_artlab_courtyard_micro.xml`, displayed alongside the 96dp variants in Android Studio Compose Preview.
- The second `PlantupiaFullPortraitFamilyPreview` renders all eight actually selected Compose identities together at 92/40/24dp, rather than comparing only one variant; it has no public app route.
- `PlantupiaAvatarIdentityTest` now asserts the micro/standard/hero thresholds and that the saved identity mapping remains stable.

**Fresh maker-process references (additional disciplinary search):**
- [ustwo Games — conversation with lead artist Lili Ibrahim](https://play.google.com/store/apps/editorial?hl=en_US&id=mc_games_editorialevergreen_indiedev_interviewseries_ustwogames_fcp) and [Monument Valley 3 making-of, Design Week](https://www.designweek.co.uk/how-monument-valley-3-came-to-life/): creator-led evidence that architecture, silhouette, distant read and faceless/near-faceless character shape can carry a coherent world. We transfer the *sizing and spatial grammar*, **not Monument Valley artwork**.
- [Ken Wong — the project's architectural concept and iterations](https://architizer.com/blog/practice/materials/an-interview-with-ken-wong-of-monument-valley/): a reminder to build the screen's navigable visual logic first, then an art pass to refine it. Transfer to Plantupia: the illustration of a person belongs next to a truthful place and task, not as full-screen decoration interrupting care.
- [Google Android Vector Asset Studio limitations](https://developer.android.com/studio/write/vector-asset-studio.html) and [VectorDrawable clip-path support](https://developer.android.com/reference/android/graphics/drawable/VectorDrawable): Android vectors support solid paths, groups and clip paths; filters and some external SVG operations require simplification. We authored paths directly and kept one clip-path per group. This is **documentation of a supported method**, not proof of final device visual parity.

**Quality criteria to test, not assertions of success:** eight identities should differ in recognizable hair/head/shoulder masses when stripped of facial detail and palette, be coherent at all sizes, never suggest a user's actual likeness or sensitive personal traits, and not overwhelm the genuine photographic plant/place identity. Before acceptance, capture actual device render and verify a saved choice remains the same across Me, Circle, Posts, Stories, Inbox, People and Notifications.

**Next creative decision:** expand **one consistent portrait anatomy/graphics language** across all eight, using the two original authored examples only as a *craft exploration*. Choose final source production format by native visual comparison, not convenience. Do not delete users' uploads or rewrite the selected variant mapping.

## Creative + technical next gate

1. Compare portrait design against the **six existing care pictograms** in `PlantupiaVisualComponents.kt`, and original no-space/no-history states: thickness and detail can differ by semantic role, but each must feel authored by the same art direction, not a copied illustration set.
2. Choose a documented vector authoring/export method and test whether SVG paths faithfully import to Android `VectorDrawable` or Compose `ImageVector`; flag unsupported effects and verify performance and anti-aliasing.
3. Art-direct a **complete family** before shipping any replacement; review low- and high-detail variants on the same board, then make an opt-in test build. Do not silently replace selected avatars, delete original media or change production behavior.
4. Revisit alternative makers, editorial illustration systems and pipeline tutorials whenever the quality stalls, but make each search answer a specific outstanding decision. Avoid uncontrolled moodboarding.

**Status:** initial physical visual sheet rendered; two original editable SVG source experiments committed; comparative critique written. **Native vector conversion, full family, real device, author approval: OPEN.**

## 19 October production-avatar integration (real app code, NOT final art acceptance)

Following the initial two-vector experiment, **all eight stored `0..7` portrait choices are now linked to a unified set of actual Android VectorDrawable assets** at `app/src/main/res/drawable/plantupia_portrait_{0..7}_{hero,micro}.xml`. Source references remain in `docs/art-lab` as SVG; the two original cases are `plantupia-avatar-window-*.svg` and `plantupia-avatar-courtyard-*.svg`; six additional pairs are `plantupia-portrait-{2..7}-*.svg`.

- `PlantupiaAvatar.kt` is now a shared **real Image(painterResource) vector renderer** with deterministic `userId` fallback, unchanged saved choice and a genuine uploaded-photo override. Existing call sites in own Me, visited Person, People, Create, Posts/Comments, Circle and DMs will display the vector when they pass the account ID and no photo. We have **not** proven every full-app surface calls this component and must still audit Story/Reels/notifications and group semantics from actual UI.
- Each portrait has an authored **micro** silhouette used through 40dp, and a **hero** portrait at 41dp and above. `plantupiaPortraitDetail` still reports micro/standard/hero for staging/evaluation; standard and hero currently use the same detailed asset. Stored `0..7` IDs and previous API behavior are maintained; the original Canvas renderer has been replaced, not left as a duplicate production renderer.
- `PlantupiaAvatarIdentityTest.kt` now checks eight distinct resource mappings in both size groups plus preserved selected mapping. GitHub Actions' exact-head build is the compile gate; **JVM resource identity checks do not establish good silhouette distinction on-device.**
- `PlantupiaEmptyPlantArt` and `PlantupiaEmptyCircleArt` are two new **original native Compose illustrations**, now presented only at actual no-plant and no-public-question states. Existing `PlantupiaEmptyPlaceArt`, `PlantupiaEmptyFieldNotesArt` and six care pictograms remain.
- `PlantupiaIllustrationFamilyPreview.kt` exposes a **360/430dp Android Studio design review board** of the four empty-state art pieces and six care actions, while `PlantupiaPortraitArtLabPreview.kt` compares all eight selected portraits at 24/40/92dp. No fabricated plant/user activity is used in these previews.

**Status definitions:** 16 native avatar vectors plus 12 editable new SVG sources are committed and invoked by the actual shared renderer; two contextual empty illustrations are live in their eligible product states. **Exact-head build, real device screenshot comparison, visual art-direction approval, all call-site coverage, dark/large-font/accessibility check remain distinct gates.** A successful compilation is not an accepted illustration family.

**Quality review next:** render all 8 identity choices on an Android phone side-by-side in micro and hero; ensure the additional six meet the craft level of Window/Courtyard and none read as gender/identity inferred from account data; test missing/broken image fallback, restored saved variant, changed profile photo, deleted-account safety, and placement in all real surfaces. If rendering looks generic or too repetitive, reopen *making-of* research for contour and vector manufacturing rather than retreating to color tweaks. Keep production/deployment separate.

## 19 October / next source-integrated art and photography pass

**Implemented in actual native screens (not merely on the concept art board):**
- `PlantupiaCareActionTile` now uses readable 13sp action text with 19sp line height, no `maxLines=2` clipping, and an adaptive minimum-height card. The existing production `plantupiaCareActionColumns` preserves two columns in narrow editors and at high font scale. `PlantupiaIllustrationFamilyPreview` now **uses that very same runtime policy**, rather than a misleading fixed three-column screenshot. Verify 320/360/390/430dp, 1.0×/1.5×/2.0× text and TalkBack.
- Existing **plant without a photograph** has a distinct native `PlantupiaMissingPhotoMark` rather than `PlantupiaBrandGlyph` or the *no plants* illustration. This mark now appears in recent-moment cards and plant detail; the contact sheet includes all five distinct contexts (no space, first plant, blank notes, empty Circle, existing plant without media).
- New `PlantupiaEvidencePhoto` draws the honest visual when a user's cover photo is absent, still loading or fails, and shows real media only after Coil succeeds. Recent moments and plant detail use it. The detail viewer click is available **only after a real nonempty photo URL loaded**, not on a broken image or placeholder. `PlantupiaEvidencePhotoPolicyTest` protects the deterministic viewer policy. The plant-detail photo can now retry the **same original GET** after a failure via an explicit in-place action (Compose image-key reset), without uploading/replacing anything in a private plant record. True source/crop, slow networks, retry state and stale URL behavior still need on-device / network QA.
- Source audit found exactly **16 distinct native portrait VectorDrawable files**, two sizes for all eight saved styles, each containing an Android clip path. This is source inventory evidence, not screenshot-based art approval or evidence all 16 silhouettes are distinguishable.

**System-level art rules reaffirmed:**
1. A missing *place*, *plant*, *care history*, *public question*, or *photo of an existing plant* are five different meanings and must never share a misleading illustration.
2. Illustrated possibility is not a real photo or observed plant record. On API unavailable/loading, do **not** show an empty state.
3. At the visual checkpoint evaluate a whole family including selected/disabled/error states, action words and actual photographs, not an isolated image.
4. A successful CI proves only source compilation/tests; visual/touch/contrast and actual device evidence remain separately required.

**Still open:** visually art-direct all eight portrait silhouettes to the desired finish; inspect true Android rendering across screen/density sizes; finish all generic Nova visual remnants across non-core surfaces; unify genuine image/crop/history treatment and no-photo fallback elsewhere; test remote slow/deleted media and correct TalkBack hierarchy. No production migration, release or data clear in this pass.
