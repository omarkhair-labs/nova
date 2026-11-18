# Plantupia — one living plant experience from any entry point

Date: 2026-11-11
Review base: `feature/plantupia-unified-experience-20261028` (#253).
Review head: `feature/plantupia-care-calendar-ownership-20261111` (#298).
Status: Draft implementation; QA CI/device validation pending; **not deployed**.

## Canonical user entry points

One user account can do any/all, always converging into My Plants:
- Own plant: independently create a private Space and select a published species from the general library; do not require purchase/QR or staff authorization.
- B2C Shop: purchase a plant-kind SKU, await a real shipping quote, approve the total and verified delivery. Only **after staff records actual retained delivery proof** create one customer-owned PlantUnit per delivered physical SKU unit, in the same customer's dedicated private purchase Space. Pots and care supplies do not create plant records. The customer may personalize the actual plant, record care, and track history. Product image is not a photograph of the delivered individual; source species label is not verified plant identity.
- B2B project: real quote -> customer accepts for commercial follow-up -> authorized staff explicitly creates one linked staff-owned **DRAFT** Installation -> actual inventory importer/manual entry -> separate independent review -> recipient-bound QA Handover -> claimed plant identities in customer's Space. Accepted quote is **not** purchase order, completed installation or QR authorization.

B2C physical delivery and B2B staff-reviewed inventory are intentionally different provisioning mechanisms, but produce the same PlantUnit / Living History / Care UX without a second plant database.

## One calendar, one record of truth

- Kotlin `PlantupiaCareActivity` renders a week/month grid from persisted `plants()` follow-up dates and the existing approved, enrolled `care-plans/` response. Does not create additional schedule rows.
- Tapping a dated entry opens the correct plant's existing detail, which remains the authority for event completion, care records, planned dates and Living History.
- Unknown species never causes a synthetic fertilizer/water schedule. A date represents a check, not proof of watering. Notifications are separate, opt-in, and approximate.
- Future UX work: full drill-down event-history calendar, multi-event/day planned recurrence, inline reschedule/cancel, device accessibility 320-430dp and TalkBack checks.

## General botanical reference library (not Shop)

Universal species library for *any* user. Source taxonomy and names from reputable botanical reference such as Kew POWO/WCVP, carefully checked common aliases and sourced general care sections. Photographs need permission/usable licenses (per-file rights, credit, source) and may not be copied simply because they appear on the web. Plantupia Shop records hold company-specific prices, availability, size/variant and commercial photography; PlantUnit holds an actual unique, private living specimen and dated history. These three must not be merged.

Company library bulk import with licensed images is a **later distinct task**, not claimed in this PR. Existing authenticated species picker only surfaces library species with published reviewed Care Guides. General guidance cannot assert the user's individual plant health.

## No stealth launch

- QA Preview #253 remains unchanged while PR #298 is Draft.
- Confirm Django model migration 0031, PostgreSQL tests, Android compile/lint/unit, exact-head APK; no automatic Production deployment.
- Real physical proof must be independently retained before B2C auto-provisioning; ensure replays/returns do not duplicate or erase the plant.
- Accepted B2B quote requires an authorized staff action, physical-manifest review and explicit customer Handover.
- Check customer scoping, unrelated non-plant orders, purchase provenance, existing Spaces and private photos, manual return/refund events and cross-account privacy.
