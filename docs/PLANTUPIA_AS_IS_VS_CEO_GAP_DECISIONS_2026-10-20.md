# Plantupia — AS-IS Android vs CEO customer journey: gap decisions
**20 October 2026 · Comparison for product/operations review · No production feature implementation**

This compares two separately evidenced artifacts: [source-traced Android AS-IS flow](./PLANTUPIA_AS_IS_ANDROID_FULL_USER_FLOW_2026-10-20.md) and [CEO-oriented delivery/QR/service blueprint](./PLANTUPIA_CUSTOMER_JOURNEY_AND_QR_HANDOVER_SERVICE_BLUEPRINT_2026-10-20.md).

## Main decision

**Keep the current individual self-service route. Reuse its care/history engine. Bridge the missing company-to-customer handover rather than rewriting the app.** The existing source *codes* private places, individual plants, care recordings, observations/photos, human-selected check-ins, dated history and separately consented sharing. It does not prove device acceptance, market adoption, approved plant-care guidance or a working commercial handover.

## Mapping the real six-stage experience

| Stage | AS-IS existing Android/backend | CEO desired customer experience | Status | Owner |
| --- | --- | --- | --- | --- |
| 01. Before app | No commercial sale/delivery/installation record integrated with the plant app | Staff prepares real customer site, delivered plant-unit list and verified data | **New gap** | Company operations decides source of truth; backend provisions |
| 02. Activation | Valid login, then user manually enters space/plant | One-time secure invitation QR, verified login and consented claim of genuine prepared inventory | **New gap** | Product/backend/security/operations |
| 03. First use | Private Space/Plant fields, optional species and placement | Actual delivered plants already appear with company-verified identity/care instructions | **Bridge + guide gap** | Staff/botanical reviewer + Android UX |
| 04. Daily care | Real observation/care/issue, note/photo, optional selected check-in and local reminder | Reuse existing care and record actions, do not assert automatic watering | **Reuse** | Current native/backend product, QA needed |
| 05. History/help | Timeline, event/photo comparison and explicit-consent Circle questions | Reuse history; add professional service request only if offered by company | **Partial** | Product + CEO service definition |
| 06. Company relationship | No confirmed service visits, SLAs, client entitlements or renewal cycle | Who is responsible? Optional permitted staff access and actual visits/outcomes | **Decision first** | CEO operations, contractual services, permissions |

### Specific points not to confuse

- **QR handover is not a QR image generation problem.** A staff-provisioned, authenticated, expiring, single-use claim process is required; permanent plant ID tags do **not** grant ownership.
- A user-selected check-in is **not** a verified watering interval or botanical guidance. The company must assign a credible species/source reviewer and allow uncertain IDs to stay uncertain.
- Existing private event records must never automatically become public Circle content or service analytics without proper permission.
- No purchase required: the manual independent plant-add flow remains.
- One real delivered customer pilot should precede any promise of AI diagnosis, IoT, automatic irrigation, subscriptions, staff dashboards or maintenance SLA.
- Care/system bugs found on QA phones are still part of the acceptance ledger; CI is not proof of a completed end-to-end journey.

## First vertical pilot to validate

One consented non-production customer site with an authentic inventory (for example seven units) → staff enters verified plant data → one secure invitation → user signs in and confirms the correct site → correct prefilled inventory appears → approved care guidance is shown without automatic watering claims → user records one real check-in/photo → dated history shows the same plant record → unauthorized second-account claim and expired/replayed token are rejected.

**Measured proof:** correct identity, no manual reentry of purchased plants, actual completed care record, private access, zero duplicated plant identities, recoverable invalid handover, and explicit operations ownership. No assumed conversion or sales metric is a verified outcome before piloting.

## Three CEO questions to resolve before building

1. Who actually records each delivered plant and its place (nursery, designer, installer, spreadsheet or order software), and who signs off the handover?
2. Who approves per-plant care advice and who is responsible for actual watering/maintenance at customer sites?
3. What is sold today: plants, installation, professional upkeep or a combination? Are staff responses/visits contractually included, and who sees the history?

## Evidence status

**AS-IS map: source-traced. Service blueprint: conceptual. Comparison: analytical. QR/provisioning and verified care-guidance: not coded.** Every implemented UI state still needs end-to-end QA in the exact APK and target backend.

For the CEO, use the six-stage comparison board and retain the full Android flow diagrams for drilling down into real screens and branch conditions.
