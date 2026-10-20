# Plantupia — Research When Blocked / How They Actually Made It
**18 October 2026 | Active creative R&D method + first art/avatars research spike | Android Native / PR #241 Draft**

This augments [Pass 08](./PLANTUPIA_PASS08_WHOLE_PRODUCT_DESIGN_RESEARCH_EVALUATION_2026-10-17.md), [Pass 09](./PLANTUPIA_PASS09_ONE_SYSTEM_VISUAL_EXECUTION_MAP_2026-10-17.md), the [Product Bible](./PLANTUPIA_PRODUCT_BIBLE_AND_HANDOFF_V01_2026-10-17.md), and the [screen ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md). It is **not** a new divergent art direction, three HTML explorations, an assertion of device acceptance, or a release commitment.

## Creative working principle

Treat each system as a **whole-family deliverable** (e.g. all illustrated identities in all contexts), not five independent green-card fixes. Research does not end with the original inspiration atlas. Once a result feels limited, too familiar, awkward, technically difficult, or inconsistent, deliberately reopen research **from a different field or medium**, inspect the **making**, not merely the finished screenshot, then implement and critique a bounded Native experiment.

No false closure: an asset drawn in Compose is **CODED**, not ART-APPROVED; a successful Android CI is **COMPILED**, not a review of visual quality; a polished static visual is not a functioning interface. Do not claim a reference studio used software it did not identify publicly.

### Repeatable research-to-build loop — use at every creative plateau

1. **State the actual plateau.** Capture a screenshot and say what feels inadequate in terms of hierarchy, structure, distinction, motion, evidence, or usability. Compare one to all system siblings, not the one screen in isolation.
2. **Open at least three independent search angles when useful.** (A) the product's close competitors; (B) award-recognized but *cross-domain* work, editorial illustration, identity systems, photography, installation design, film/motion, information design; (C) technical production methods and tool capabilities. Sources can be primary team case studies, process breakdowns, code/docs, talks, tutorials, credible peer critiques.
3. **Read the authors' process.** Distinguish: confirmed authored production method; available tools that *could* produce a similar category of result; our own inference. Look for concept sketches, geometry/asset kit, constraints, layering, data contract, animation state machine, export and runtime, performance, accessibility, licensing.
4. **Translate behavior or craft, not aesthetic surface.** Write: observed mechanism -> Plantupia-specific need -> privacy/data consequence -> what not to copy -> shortest reversible experiment. A beautiful reference is not automatically appropriate for a real plant record.
5. **Choose the tool by the job.** Try current native approach before adding a runtime; use a vector-asset workflow for more authored static line art; prototype Rive or Lottie only if actual interactive/animated scope justifies dependency, asset format, license, runtime/memory/accessibility cost.
6. **Build an A/B *technical treatment* of the same chosen art direction**, not three different apps. Compare side-by-side in identical states and at multiple rendered sizes. Discard weaker treatment; don't leave parallel systems behind.
7. **Audit across every surface.** Reuse a shared component/semantic contract from onboarding and empty states to Me, person, discovery, post/comments, Circle, Story, messages, notifications. Test loading, photo/no photo, expired/deleted user, large text, light/dark media, 24/32/40/64/96dp. A single isolated Compose preview cannot close the family.
8. **Review again after execution.** Look up additional making-of material *if* it still feels generic or blocked. Record where the new technique failed and change approach intentionally. No endless unbounded inspiration browsing; every research pass must change a real design decision or falsify an assumption.
9. **Close only on evidence.** A visual system is done when the author accepts a whole-family board + actual integrated Android device pass, with functionality and data boundaries intact. Record follow-up work without a fake DONE label.

## First research spike — illustrated people, 18 October

### Evidence from creators (do not misattribute tools)

- **BUCK × Notion Faces —** [primary studio case study](https://buck.co/work/notion-faces). The studio describes an *illustrated components library* for eyes, brows, hair, accessories, cultural details, layered skin tones and compatibility tests, visually aligned by hand; Notion's illustrator Roman Muradov and multiple BUCK illustrators authored the family. The case study credits art/creative technology contributors but does **not** establish a definitive Figma/Illustrator/After Effects software pipeline. [Notion's product docs](https://www.notion.com/help/notion-faces) confirm users can configure facial features and download their portrait.
  - **Lesson:** do not judge avatar distinction only by changed colors and one hair contour. Design a compatible, authored kit of **human contours, pose/shoulder/collars, facial rhythm, accessory options and architectural frames** (sensitivity/privacy appropriate), then test actual combinations and account persistence.
  - **Not copying:** no Notion likeness, features, exact portrait style, avatar UI or assets. The Plantupia human-with-place metaphor remains ours.
- **BUCK × Facebook Alegria —** [primary studio case study](https://buck.co/work/facebook-alegria). Studio documents a complete flat/geometric *style guide* and an extensible illustration library spanning people, buildings, plants and other categories, rather than unrelated empty illustrations. The public case study does not provide a verified per-asset software chain.
  - **Lesson:** design Plantupia's real *family* (person, space/architecture, plant possibility, care glyphs, empty history, first question). Calibrate common visual grammar: ink weight, negative space, paper frame, geometric vocabulary, visual scale and accessibility.
  - **Not copying:** abstract oversized-body Alegria figures, colors, or its signature visual idiom.
- **Studio pipeline thought:** BUCK calls its own character systems *kit-of-parts* in its [studio capabilities note](https://buck.co/agent-first-inquiry-protocol). Treat this as evidence for *system thinking*, not as authorization to make an infinite customization engine now.

### Implementation tools — documentation verified, proposed use (not asserted BUCK process)

| Tool / pipeline | Confirmed capabilities | Proposed Plantupia use | Watch-out / gate |
| --- | --- | --- | --- |
| Kotlin Jetpack **Compose Canvas** / `DrawScope` | Programmatic primitives and drawing modifiers, responsive composable drawing ([Android docs](https://developer.android.com/develop/ui/compose/graphics/draw/overview)) | Small dynamic state/identity marks, authored care pictograms, first native artwork experiments linked to data | Complex organic curves hand-coded as circles/lines can look rigid; avoid a hundred opaque magic constants. Check 24–96dp, density, recomposition and semantics. |
| **SVG/vector artwork -> Vector Asset Studio -> VectorDrawable/ImageVector** | Import SVG/PSD to Android vector drawables and render through Compose resources ([Android documentation](https://developer.android.com/studio/write/vector-asset-studio.html); [Compose vector/bitmap](https://developer.android.com/develop/ui/compose/graphics/images/compare)) | Author more sophisticated reusable static assets outside Kotlin; support multi-density without importing stock art | SVG support is a **subset**, some effects/layering won't transfer. Own the rights, audit transforms, gradients, asset size and visual quality. Choice of illustration authoring editor is open (Figma, Illustrator, Inkscape etc.), not historically verified for BUCK. |
| **Lottie Compose** | Render `Lottie` assets in Compose with progress/animation APIs ([Airbnb docs](https://github.com/airbnb/lottie/blob/master/android-compose.md)) | Deliberate one-shot state confirmation or richer authored micro-movement, only if Compose's native animations are insufficient | Motion asset pipeline, unsupported effects, CPU/battery, reduced-motion and test overhead. No dependency until a real example demonstrates an advantage. |
| **Rive editor + Android runtime** | State-machine-led interactive graphics and Android runtime ([Rive editor](https://rive.app/editor), [Android API](https://api-docs.rive.app/android/latest/)); 2026 Android semantics support is emerging and must be evaluated ([Rive runtime note](https://community.rive.app/c/announcements/rive-runtime-updates-august-31-2026)) | An optional interactive human-place or timeline illustration that *reacts to confirmed app state*, only if it genuinely clarifies an action | Do not animate imagined plant growth, sensor status or successful save; assess runtime and accessibility before adopting. |
| Existing user's **real photos** | Actual camera/gallery-upload evidence tied to authorized plants and event date | Physical continuity of space/plant and truthful history | Never replace with AI-generated photographs pretending to be an observation. |

**Practical default:** Keep existing Compose for the small icon and data-dependent layer. Try a more expressive **authored vector artboard** for portrait/empty illustration if the Canvas treatment is visually too formulaic, then integrate via one consistent native identity layer. This is an investigation proposal, **not** a claim that the new assets exist.

### Current observed limits of our code (verified from PR branch)

- `app/src/main/java/com/nova/app/ui/components/PlantupiaAvatar.kt`: 8 authored contour variants, 5 palettes, 3 accent types, now varied architectural backgrounds and shoulders. **The base head and large portions of the construction remain structurally shared.** We have not reviewed an export board showing all 8 at 32/40/64/96dp. Do not assume changed numbers make eight great illustrations.
- Existing `plantupiaAvatarStyle(userId, chosenVariant)` resolves a stable account ID or stored `0..7` choice. Real photo overrides. **Do not break this persistence contract** or change the server choice range merely for a visual prototype.
- `PlantupiaVisualComponents.kt` already has 6 care pictograms; `PlantupiaEmptyPlaceArt` and separate blank-history artwork also exist. **Tune these into one authored family**, do not rewrite as absent or pad every screen with decorations.
- Review identity consistency at **every call site**, including Create, Me, People, Circle question, posts/comments, Story, DM, notification, deleted users; actual device tests are the decisive gate.

### Proposed first controlled experiments, same chosen Plantupia direction

1. **Visual contact sheet:** eight current actual avatars × 32/40/64/96dp plus 8 real no-photo app placements, alongside all actual empty-state/care artwork. Find where each loses uniqueness, becomes too symmetrical or inconsistent.
2. **Art anatomy test:** draw *two* higher-quality original faces/shoulder/garment/frame assemblies in an external vector artboard using the same warm paper/forest/sage world; evaluate distinguishability and design craft. The test asks whether modular hand-authored parts outperform shape-parameter tuning. Do not build an enormous avatar editor before proof.
3. **Rendering test:** compare one resulting artwork as static VectorDrawable with current Compose Canvas, in the same avatar component and tiny size. Evaluate weight, antialiasing, accessibility, APK impact, and photo/failure fallback.
4. **System language board:** care pictograms + no-space + no-plant + no-history + first-question + human portraits, rendered next to actual Plantupia screens. Calibrate silhouette, shape rhythm, frame, line weight and visual hierarchy *as one authored family*.
5. **Interaction test only when needed:** prototype a single genuinely useful save acknowledgment / state transition in native Compose, then compare Rive or Lottie if that cannot achieve the needed quality. No idle looping animation merely to look advanced.

### Decision register for this pass

- **Selected:** one cohesive art world, human centered and architecturally situated, separate real media and optional editorial marks; first audit the artwork *as artwork*. Continue from Pass 09, no three competing sites.
- **Hypothesis under test:** an authored vector/modular-part pipeline could raise avatar quality above the current Canvas geometry. Tool has NOT been selected and no new assets have yet been created.
- **Not selected:** copied Notion Faces/Alegria, default gender assignment, AI portraits of real people, automatic video feed, fake plant/photos, new runtime by default, new app/Play/server work.
- **Acceptance required:** source credit and rights, actual comparative art board, whole-app propagation, native small-size QA, current commit CI, author visual review. Keep PR Draft.

## Next handoff discipline

When blocked in **typography**, read editorial/type system case studies and inspect font shaping/licensing and real Android rendering; when blocked in **photography**, research archive/field notebook, photo evidence and image crop handling; when blocked in **motion**, inspect actual storyboards and state machines, not flashy loops; when blocked in **navigation**, compare real interaction patterns and instrument device Back/keyboard/focus; when blocked in **architecture/scaling**, read production implementation and test size/latency. Record *what exact idea moved* and **what evidence will disprove it**.

**Per-system closure:** research -> make -> review -> iterate until materially stronger -> apply across product -> real integrated visual/functional test -> owner acceptance. Escalate methods/techniques when stuck; never call the system finished just because we changed something in source.
