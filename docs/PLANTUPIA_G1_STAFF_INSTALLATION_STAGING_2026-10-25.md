**Later implementation:** [25 October integration and QA contract](./PLANTUPIA_PHASE_G_H_I_INTEGRATION_2026-10-25.md) adds QA Admin issuance/revocation, exact-manifest handover checks, native activation and Care Guide structure. The original G1 snapshot below is historical; company/production approval is still absent.

# Plantupia G1 · Protected staff installation staging

**Implemented on Android Plantupia draft PR #241, backend only · 25 Oct 2026.**
**NOT a QR claim, customer handover, publication, or production deployment.**

## Why this is first

The existing `Space` / `PlantUnit` / care history model already holds real plants, but it previously had no way to mark the exact set **prepared for a Plantupia delivery**. G1 adds two staff-facing Django models (not a second Android app):

- `InstallationBatch` — one staff-owned, prepared `Space`, an internal non-PII reference, draft/ready state, preparer and reviewer.
- `InstallationManifestRow` — one immutable existing `PlantUnit` identity mapped to one actual source row in that installation. Unique source key per batch; the plant cannot be linked to another batch. Cross-space references are rejected.

The admin **Review inventory and mark as ready** action refuses empty manifests or wrongly linked plants; review is timestamped and attributed to staff. A ready batch cannot be edited through this Django admin until a separately designed correction/reopen workflow exists. No customer receives access from creation or review.

## Internal QA use (never real customer data without permission)

1. Use a controlled staff/superuser test account and non-production Django admin.
2. Create a `Space` owned by that staff account (not by the eventual customer). Do not enter a real customer's identifiable address in the QA name.
3. Create genuine *test fixture* `PlantUnit` records under that exact Space; use unknown species if not verified. Do not fabricate final botanical advice.
4. Create an `InstallationBatch` with a unique *internal* `reference_code` and that staff-owned Space. `prepared_by` is assigned from the authenticated admin.
5. Save, then add `InstallationManifestRow` entries pairing each PlantUnit with an internal source key (`001`, `002`, etc.). Save while draft, then review the list.
6. From the batch list select **Review inventory and mark as ready (no customer access)**. This locks admin edits and records reviewer/time. A batch without rows stays draft.
7. Authenticate as an unrelated test customer; the usual `GET /spaces/` and `GET /plants/` MUST NOT expose the prepared installation. No QR / claim endpoint exists at this stage.

## Implemented safeguards and boundaries

- Models reject staging in a Space owned by a non-staff user and reject non-staff preparers.
- Manifest rows must point to plants from the batch's Space; source keys cannot duplicate and a plant identity can only appear in one manifest mapping.
- The admin action rechecks inventory and staff ownership **inside a database transaction** before making it ready.
- Staff-created objects remain staff-owned. No owner transfer, membership grant, public post, customer notification, secret QR, image mirroring or care-guide verification is implied.
- A ready batch becomes read-only in admin to prevent silent post-review mutations through this view; programmatic mutation and upstream Space/Plant admin are not yet transactionally frozen. A later lifecycle must account for controlled amendments and audit logs **before live rollout**.
- These checks protect a pilot workflow, not a complete commercial procurement/ERP system. No employee role system or verified sale source has been supplied yet.

## Next implementation gates (not done)

**G2a** Company-approved delivery/invitation ownership policy: customer-owned sale vs staffed-maintenance space. Decide whether staff ownership transfers or staff membership persists. Only company can confirm real contractual responsibilities.

**G2b** Short-lived secure handover invitation & atomic, authorization-checked preview/claim API + test of replay, wrong account, expiry, revocation and concurrency. A QR *image* alone grants no access. Keep tokens out of logs. App-link/domain is optional for pilot if scan happens inside Android.

**G2c** Native Android "Received Plantupia plants" flow: scan/paste code, login-resume, inspect safe handover summary, confirm, and land in existing My Space using **the same plant UUIDs**.

**G3** Reviewed sourced care guides; no generic fixed watering orders, no artificial plant health scores.

**G4** Physical-device QA with two independent accounts and an isolated 7-plant test fixture.

## Real operations questions left open

Who owns company delivery records? Who verifies species and care instructions? Who is responsible for ongoing care? We can test a synthetic pilot without these answers but cannot offer paid support, precise watering recommendations or customer access to a real company-maintained site without approval.

[Full approved-for-discussion flow](./PLANTUPIA_CANONICAL_TO_BE_END_TO_END_FLOW_V01_2026-10-25.md) · [Current Android AS-IS flow](./PLANTUPIA_AS_IS_ANDROID_FULL_USER_FLOW_2026-10-20.md)
