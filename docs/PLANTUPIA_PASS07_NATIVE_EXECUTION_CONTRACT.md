# Plantupia Native — Pass 07 Research → Implementation Contract
**2026-10-14 | Draft product engineering contract — no final design signoff, no production rollout.**

Research source: [Four-world Integrated Candidate contract](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/09-pass07-integrated-product-synthesis-contract-2026-10-14.md), [Pass 05 research](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/07-award-winners-behavioral-teardown-pass-05-2026-10-14.md), [Pass 06 validation](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/research/08-field-realities-validation-protocol-pass-06-2026-10-14.md). The companion Pass 07 interactive HTML prototype is a **conversation attachment**, not shipped app code.

## Do not mistake prototypes for finished product
Current branch has working Compose/Django implementations for My Space, plants, manual check-ins, reminders, actual private history, Circle, posts and inherited Nova media/messaging. A native Android build and Django test pass is **not** actual hosted QA operation. The interactive R&D prototype includes stock imagery and local-only events, never client data or network writes.

## Integration order, not art-style imitation
| Priority | Native surface and files | Evidence from research | Implementation target | Automated + physical QA |
| --- | --- | --- | --- | --- |
| P0 | `PlantupiaClientSection.kt`, `PlantupiaTodayPanel.kt`, `HomeScreen.kt` | Integrated Candidate combines PLACE/CARE/NOTES; prior screenshots were paragraph-heavy | Reduce copy repetition. Establish a clear order with a **real selected place image if authorized**, one human-chosen check-in if present and dated record; no fake fallback hero. **Compare ordering** before moving features between navigation roots | 0/1/7/100 fixtures; voice focus order; first action above fold on 390dp |
| P0 | `PlantupiaSelfServiceForms.kt`, Plantupia Django | CapWords photo-confirm flow, no fake upload | Clear chooser/photo preview; distinguish unsaved vs server-ACK; preserve placement context, owner-only files | deny permission, retry/offline, duplicate taps, large image, revocation |
| P0 | `PlantupiaCareActions.kt`, `PlantupiaClientSection.kt` | Quiet Care: document real observation, not command to water | Care composer foremost icon action; optional note/photo/date; accurate persistent confirmation | no action unless tapped; failed write leaves draft; real event ID only |
| P0 | `PlantupiaHistoryTimeline.kt`, history cards | Field Notes: photos and observations tied to real time | Preserve timestamp/actor and linked outcome, expose Photos filter/dated comparisons only with two genuinely recorded photos | 0/1/200+ events, pagination duplicate/orphan results |
| P0 | `PlantupiaCommunityPanels.kt`, Nova post/media | Are.na: public knowledge linked to private activity while privacy holds | Preview exactly selected public photo/title/body; private placement and history not auto-serialized; separate result | two real accounts, permissions, public EXIF, failed upload, back |
| P0 | `PlantupiaAssistantSheet.kt` + Django provider | Merlin: hypothesis→human check→revisit | Secure provider configured on isolated QA, scoped opt-in context, cited sources only when actual, no invented live sensors | actual two-account AI calls, cost/limits/failure/uncertainty eval |
| P0 | existing Nova `OrbitScreen`, `CreateHubScreen`, Stories/Reels/Inbox | existing Nova engine is strategic reusable implementation | Keep native Media3/messaging; harmonize English/brand without breaking original workflows | Story→DM→back, player black frame, keyboard, voice/call regressions |
| P1 | shared space/plant admin UX | 0/7/100 site scaling requires organization | Finish owner/manager edit, archive/retire and confirmation; rights-preserving history. Do not fabricate company workflows | revoke role, owner edit, no-op, history retention |
| P1 | semantic motion, assets/type | photo-led/quiet editorial aesthetic | Official wordmark/vector + approved image rights; Playfair/Inter if safely licensed; image identity transition after actual performance proof | font 1.3–2×, dynamic type, 360dp, TalkBack, reduced motion |
| B1 | commercial installs/maintenance | real company field-work research not performed | Ask founder for actual site/SOP/service/photos and ownership rights | do NOT implement fake staff, IoT, carbon or maintenance promises |

## Candidate-specific risks to watch
- `Living Atlas` may be terrible for 0 images or a 100-plant office; **a real searchable inventory must remain first-class**. Existing native `findSpacePlants` + paging are worth keeping.
- `Field Notes` may slow frequent checks; quick care should never require a staged cinematic workflow.
- `Quiet Care` can over-prioritize one item and hide broader space responsibilities; overdue check-ins come from a human-chosen date only, not a watering diagnosis.
- `Integrated Candidate` risks stacking too many visual cards. Simplicity is a **measured outcome**, not fewer screens in a document.

## Existing source observations
- `PlantupiaClientSection.kt` currently emits a generic “A living space changes over time” header plus a longer philosophy paragraph before the user's photos, check-ins and actual spaces. This is a candidate for **later compact microcopy work** after visual test.
- `PlantupiaTodayPanel.kt` currently shows both due and future planned check-ins; don't rename a future check-in as “due today” as a way to fill an empty layout.
- The hero image currently chooses the first authorized plant cover in focused space rather than an actual *space-level photograph*. Label it accurately and don't imply a company/site panorama. In R&D demo photo source is explicitly illustrative.
- User's screenshot comparison was from an earlier QA build. **Do not declare visual parity** with this HEAD unless the newest APK is installed and captured.

## Acceptance criteria and workflow
1. **Research / design hypothesis**: compare four worlds on same tasks without manufacturing winner scores. Preserve per-world scripted action traces separately from real usability.
2. **Implementation ticket**: only convert a candidate to Compose after specifying actual data source, empty/errors, authorization, testable user action, no private auto-sharing, and fallback.
3. **Continuous CI**: specific-head Android + Django tests; keyboard/photo/media smoke on actual QA; no speculative release declarations.
4. **P0 completion**: isolated backend deployment and AI live QA, two-account separation, error/permissions, real-device 0/1/7/100, media, English/accessibility.
5. **Final owner review**: one integrated Plantupia QA APK. No screen-by-screen checkpoints, no merge into main/Production/Play/signing unless explicitly approved.

**Status now:** This is a traceable implementation contract, **not** implementation or final sign-off of the four-world design.

## Implementation increment 01 — Quiet Care attention priority (2026-10-14)

Native `PlantupiaTodayPanel.kt` was updated in commit `ed050963`: when no human-selected check-in is actually due, it no longer displays an oversized zero-count urgency panel. The next future check-in remains actionable in a compact row, and a user with no planned check-ins sees one calm sentence. Genuine due items retain the previous high-priority card. This is a **small reversible UX refinement**, not acceptance of the experimental four-world navigation. Android CI and device checks are still required; the real user review remains only at the final integrated APK.
