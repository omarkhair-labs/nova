# Plantupia — bounded integrated Android closure

Scope: one native Android app plus required isolated Django APIs, on PR #333. No website, company dashboards, production deploy/migrate/merge, billing or Play release in this pass. Preserve source-of-truth, private records, existing identities and engines. This ledger supersedes older interim progress labels for this branch, not the requirements or source research. Use these three axes throughout. **Implemented** means code exists; **bench-tested** means a named test passed; **integrated** means the real journey uses the same data/authority; **field-validated** requires representative physical evidence. **Accepted** requires final integrated visual/user acceptance. Do not substitute a green build for acceptance.

## A — technology completion ledger

| Track | Existing work, including this increment | Remaining bounded acceptance |
| --- | --- | --- |
| Capture | CameraX, whole/detail views, quality, repeat capture, private saved visit photos | A26 permissions, lighting, upload/retry and repeat framing |
| Species recognition | Strict two-view DINOv2; first retrieval and trained ridge studies documented, both rejected for false accepts | Representative indoor known/unknown validation; retain manual choice/abstention. Do not reopen rejected model search without a named evidence gap and new holdout. |
| Segmentation | GrabCut and optional MagicTouch signed reviewed outlines | Expert-labelled actual indoor organs, negative controls, ordinary touch prompts, latency and failure. Automatic multi-organ detection not established. |
| Live tracking | QA-only contrast guide plus bounded template translation with fresh contour, explicit reacquisition on loss | Physical A26 alignment, latency, thermal and occlusion tests. No automatic semantic identity or cross-visit tracking claim. |
| Visible condition | Saved RGB fractions and local patch bounds, source-verified outline inspection, person reports, care/location context, specialist review | Actual observed symptoms/quality, expert review, false positives; no disease or treatment automation. |
| Time/measurement | Saved-date comparison, repeat-photo overlay, background ORB registration, independently checked planar/ArUco projected area | Physical repeatability, colour/pose/scale validity; no inferred whole-plant growth. |
| ARCore | Floor corners and bounded raw-depth surface observations | Tape comparison, tracking loss, physical supported phone |
| 3D | Private 5–8-view reconstruction, textured/stereo evidence, layout revision | Real room and plant capture; thin foliage, reconstruction gaps, metric/visual distinction |
| Environment | Direct-ray solar scenario, opaque screen shadow, two-position sampled comparison; optional local phone ambient-light sensor | Physical room/obstruction comparison and reference light meter; device availability, glazing and indirect light unmodelled; no simulated lux or plant suitability |
| Twin | Versioned room, plant identity, placement and history in connected views; common as-of placement projection in Field Notes and Ask | Physical historical consistency and live updates on real scenario |
| Ask | Explicit sharing of recorded evidence, reviewed guide and same as-of room projection as Field Notes; provider/consent fallback | Isolated authorized provider and multimodal evidence boundary; no unsupported claims |

## B — one experience

Connect every actual plant/place/photo/history action through the same authorized IDs, same camera and records, no parallel demo pathways. Exercise arrival → first plant or delivery → Today → care → field inspection → same visit/history → room → contextual Ask → return. Also shop/catalog/cart/order/delivery, Circle/post/reply/people/profile/message/call, settings, error/offline/account switch/Back/permissions. Do not claim the combined journey is complete solely from individual features.

## C — screen-by-screen creative and functional closure

All these groups have existing implementation; their whole-experience design acceptance is OPEN. Use the current whole-experience reinvention document and screen closure ledger for detailed sub-surfaces. For each group capture exact-build native evidence, critique and refine, exercise ordinary/empty/error/loading/permission state, compact and large text/RTL, and retain authentic art direction. No arbitrary screen count or completion percentage.

- Arrival, login/register/recovery/legal and first-run profile: open
- Today and care schedule: open
- Spaces, collection, creation, plant overview/guide/care/history/inspection/photo/time: open
- Room plan, AR scan, 3D/photo scene and environment: open
- Handover and ownership: open
- Shop, object study, variants/cart/checkout/delivery/order/history/inquiry: open
- Ask, team/support/membership: open
- Create/Circle/posts/replies/people/profiles/media/messages/calls/activity: open
- Settings and shared components, typography/illustration, motion, accessibility: open

## Final gate

At baseline 20b2268 all four workflows (Nova CI, core roots, native craft, public web CI) passed. That is prior exact-head evidence, not evidence for new commits. Require exact-head tests and architecture checks, one isolated Living QA APK plus matching test backend/model files, physical A26 field exercises, and final owner review. A blocked field or model capability is explicitly deferred or gated, not silently labeled complete. No production action. Avoid unbounded research: finite evidence gates and accept/reject/defer decisions.

## Increment: live correspondence and outline inspection

The existing local preview now acquires a centred foreground mask from two consecutive agreeing frames. A fixed bounded YUV template samples both sides of the boundary; the tracker searches a maximum of 225 translations per update, scores <=256 samples, rejects ambiguous/poor matches, and requires a fresh segmentation agreeing with both the translated anchor and last mask. It can follow a region beyond the centre, but loss, occlusion, long gaps or changed view coordinates require selection again. It does not provide persistent plant-part identity or prove rotation/scale tracking. Preview-only frames stay in memory and are never uploaded. Production remains default off.

A selected saved plant outline can request the existing inspection service using its trace ID. The service verifies event membership and original photo hash, applies current photo quality flags and uses the already saved bounded colour patches. It returns separate source, visible appearance, and inspection steps with the same historical care/placement/guide context. No record or diagnosis is written; natural patterns/reflections remain alternatives. Low-quality images receive retake guidance. Existing person-reported triage and specialist correction remain separate. Tests use synthetic controls; actual symptom validation remains open.


## Increment: environmental study in the existing room

Keep the original solar position/one-aperture model and saved revision. In the same daylight workspace, keep a reference point (the recorded plant placement when interior) and tap a hypothetical alternative. Both share the selected date, plane height, opening, orientation, and optional reported screen. Counts and paired timelines use the same actual half-hour sample instants; they are not exposure hours, predicted lux or a recommendation. Alternative positions never write room placements. The backend verifies ceiling constraints when a room height is recorded and uses ray intersection at the opaque screen for point answers, independently of grazing polygon drawing. This is a bounded one-window geometric scenario, not a complete radiometric model.

A separate explicit 6-second local Android TYPE_LIGHT check shows actual sensor-reported values and range, with event count and low-accuracy/saturation warnings. No camera-derived lux, no sensor fallback estimate, no location request, no network or retained reading. Stale/out-of-range events are discarded; stop on exit/pause; a device with no compatible sensor or no fresh event is reported. This is uncalibrated phone illuminance, not canopy light/PAR/DLI or a historical reading. Actual A26 availability and comparison with a reference meter remain unverified. No automatic placement or diagnosis.


## Increment: one historical placement projection

Field Notes and Ask now derive placement from one authorized plant/current-space helper, selecting the latest room revision recorded on or before the actual visit. A plant absent from that revision is not recovered from older layouts. Field Notes shows the reported coordinates, revision and nearest recorded window centre if present. Ask receives a reduced projection (visit/layout date, revision, distance or null, and not-measured illuminance) only with the existing explicit evidence and guide/layout consent. No room ID, coordinates, other plants, temporary phone readings, or inferred exposure is sent. The existing provider is text-only; unit tests mock transport, and live-provider acceptance remains a separate authorized gate.
