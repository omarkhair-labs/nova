# Plantupia: After handover → first photograph → first real care moment
**18 Nov 2026 · Fix / QA, stacked on #253 · not deployed**

## User evidence (real device test)
A customer completed a test delivery/handover journey, received a PlantUnit automatically, but could not find an action to set the plant's photograph or start its experience. This is an actual discoverability + missing capability defect, not a user education problem.

## Verified source defect
- `PlantupiaClientSection.kt` Overview rendered the literal text `Add a photo of this plant` for a blank cover, inside a passive `Surface` with no click handler. A delivered/handed-over PlantUnit does not pass through the Create Plant form, where `cover_photo` was previously supported.
- `PlantupiaRepository.kt` supported photo only during initial creation and as evidence attached to a care event. It did not expose an explicit existing PlantUnit cover update.
- `PlantDetailView` `PATCH` deliberately limits owner/manager to shared name/species/placement/archive and should **not** be overloaded with uncontrolled photos or identity verification.
- Independently, the **Care** chapter already contains an explicit `Care / Observation / Concern` composer with optional event photo, check-in date and subsequent actual History. This is not the same as setting a specimen's initial portrait.

## Real intended interaction
1. `My Plants` → select the plant you received. `Overview` displays actual plant identity, no guessed health, and a visibly interactive **Add your first plant photo** CTA if authorized.
2. Android document/image picker selects JPEG, PNG or WebP, maximum 10 MB. A real server upload normalizes the image and removes EXIF/GPS. The same PlantUnit is updated (no duplicate claim, public post or inventory record). After authenticated backend success the inventory reloads and the photo is rendered from actual server media.
3. `Care` → choose **Care**, **Observation** or **Concern**; a genuine explicit event can include a *different* photo of the observation, optional follow-up, human action with no forced watering prediction. Save only after ACK → `History` with real timeline.
4. `History` shows actual data and the audited change of private portrait as an observation with no treatment/diagnosis claim. The customer never has to invent a species or record watering merely to choose a picture.
5. Already-photographed plants have **Change this plant's photo** action; viewers and archived plant records have an explanatory, genuinely noninteractive state.
6. If a user is `VIEWER`, show view-only; owner and `CARER`/`MANAGER` of an active PlantUnit may add a private portrait, consistent with who can record photo evidence. No escalation to metadata/ownership authority.

## Implemented API contract
`POST /api/v1/plantupia/plants/<uuid:plant_id>/cover/` (authenticated multipart `cover_photo`). Uses the same existing image sanitizer as new plant creation. Authorization: exact accessible plant, active, care-authorized. Response is the same PlantSerializer shape including `cover_photo` and `can_record`. No new schema/migrations. Public Circle and Company QR remain unchanged.

## QA acceptance
- Owner can add first photo without adding second plant; sanitized JPEG, empty EXIF, private image and a genuine audit; original ownership preserved.
- Company-owned handover's `CARER` member can update portrait; `VIEWER` cannot; a stranger gets 404, unauthenticated 401, archived 409, invalid photo 400.
- Android restores context and refreshes the photo, handling selection cancellation, account switches and current-plant changes without writing to the previous plant.
- **Must validate on actual phone:** one plant and seven plants after genuine QR, 320dp/large font, image picker + permission, camera gallery 10MB threshold, network interruption and retry (upload may have committed before lost ACK), uploaded media reachable via the actual deployment's storage/CDN.
- No automatic production deployment from this Draft PR. Coolify QA endpoint must include this backend route before APK action can succeed. Re-check the actual backend release and persistent media configuration.
