# PLANTUPIA — Canonical end-to-end product flow V0.1

**Working product decision · 25 Oct 2026 · CEO discussion draft · Author: product/design/engineering**
**Status: PROPOSED. Not CEO-approved, not implemented, not real customer evidence.**
**Scope: Android Native + existing Django backend. Keep Nova social/messaging systems, existing Plantupia privacy and history. No production deployment or customer data changes.**

## 0. The one-sentence user promise

**"Know the plants that actually live with you, understand their care, record what you did, and get the right help when you need it."**

Two ways to start; **one shared living-place / plant / care / history domain**:
- **A. Plantupia customer**: received an actual Plantupia delivery or installed space. A company-authorized *handover invitation* associates already prepared, verified records to their account.
- **B. Independent user**: already owns plants or wants to track their own. Manually creates a private space and plant; does not need a purchase, QR, service agreement or company approval.

**This corrects the current gap**: present source implements B and common care/history/social infrastructure; A, approved botanical care knowledge and company service fulfillment are not yet live.

## 1. CEO presentation — primary customer journey (TO-BE)

~~~mermaid
flowchart LR
  F["Plantupia prepares real delivered plant records"] --> D["Delivery & secure handover invitation"]
  D --> Open["Customer scans delivery QR / opens invite"]
  Open --> Auth["Install/open app & sign in; invitation persists"]
  Auth --> Preview["Confirm correct delivery and plant inventory"]
  Preview --> Claim["Claim this installation (server validates)"]
  Claim --> Home["My Space: actual plants are already present"]
  Home --> Guide["Plant detail: verified care guidance & responsible carer"]
  Guide --> Check["Inspect plant when relevant; not an automatic watering order"]
  Check --> Log["Record real care / observation / photo"]
  Log --> History["Private dated history & optional check-in"]
  History --> Return["Return and document the actual outcome"]
  Return --> Guide
  Guide --> Help{"Need help?"}
  Help -- "No" --> Check
  Help -- "Yes" --> Choice["Private company support if contracted OR consented public Circle question"]
  Choice --> History
  Own["I have my own plants"] --> Create["Manual private space + add real plant"]
  Create --> Home
~~~

**First-minute target (hypothesis, not tested):** accepted install -> site/plant count -> first plant -> first source-backed guidance or meaningful inspection. The customer's 7 delivered plants are **not** seven blank create forms. No fabricated photos, readings, diagnoses, recurring watering instructions, plants, metrics or warranty.

## 2. Company + customer swimlane — the operation behind that journey

~~~mermaid
flowchart TB
  subgraph Ops["PLANTUPIA / OPERATIONS — before and after handover"]
    O1["Record real sale / installation / delivery reference"]
    O2["Prepare actual site, each plant-unit and placement"]
    O3["Verify identity/species as known and approve care guide/source"]
    O4["Confirm delivery; issue single-use invitation"]
    O5["If contracted: accept/perform/report genuine support work"]
  end
  subgraph Server["DJANGO / PRODUCT — security and persistence"]
    S1["Store prepared installation and individual plant identities"]
    S2["Store consent-safe invitation as hashed expiring token"]
    S3["Authenticate, preview non-sensitive summary, authorize atomic claim"]
    S4["Grant scoped Space membership without duplicating plants"]
    S5["Serve versioned care guide + private permissions/history"]
  end
  subgraph Customer["ANDROID / CUSTOMER"]
    C1["Scan QR or open link"]
    C2["Login or register, retain invite"]
    C3["Review what was delivered; confirm"]
    C4["See actual living space / plants and care responsibility"]
    C5["Inspect, log, revisit or seek help"]
  end
  O1 --> O2 --> O3 --> S1
  O3 --> O4 --> S2 --> C1 --> C2 --> S3 --> C3 --> S4 --> C4 --> S5 --> C5
  C5 -. "Authorized service request only if available" .-> O5
~~~

**Lean operations recommendation:** a protected, manual **Django Admin/staff import** for one real installation is enough for the first pilot. Do not invent a separate staff Android app or full ERP now. Plantupia must provide real plant records and botanical approvals; we build Android and backend.

## 3. Screen-by-screen user flow and microcopy contract

Each proposed screen here is marked **NEW** or **REUSE**. Labels are working English UI copy, not final marketing/legal text.

| Order | Screen/state | Primary UI / user action | Data and rules | Status |
| --- | --- | --- | --- | --- |
| 00 | Splash/session resume | Continue authenticated session or Welcome | Cached state scoped to account; expired session routes to login | REUSE |
| 01 | Welcome / entry | "Explore your plants" with **Received Plantupia plants** / **Add my own plants** | Receiving a deep link keeps invitation through sign-up/login and restart | NEW dual entry |
| 02A | Received plants — invitation | QR from company handover card, or in-app **Scan handover code** / secure invite URL | QR stores only opaque invitation identifier; no private place or account data | NEW |
| 03A | Authentication | Sign in / create account and return to pending handover | Never grant ownership from scan alone; use authenticated current account | REUSE + NEW resume |
| 04A | Confirm handover | "Is this your delivery?" + limited count/site label and responsible service contact if approved; **Confirm plants** / **Not mine** | No private plant listing before claim; confirmation rights server-side | NEW |
| 05A | Activation result | "Your plants are ready" → **Open My Space**; on error show specific recovery | Claim atomic/idempotent, no duplicated site/plants, cannot reveal to stranger | NEW |
| 02B | Independent route | "Create a private space" → **Add a plant** | No QR, purchase, company service or forced sign-up beyond account requirements | REUSE current forms |
| 06 | My Space / Home | Real site, plant count, relevant genuine check-ins, space chooser, primary inspect/record CTA | Do not displace care behind social feed; show 0/1/7/100 honestly | REUSE + editorial refinements |
| 07 | Plant Detail | Real plant name, place, uploaded photo, known species/unknown, **How to care**, **Record care** | Guide has source/reviewer/version and includes uncertainty | REUSE plant detail + NEW guidance |
| 08 | Care Guide | Light & placement, watering *inspection* cues, drainage, safety, approved actions; **Check this plant** | Species-only rigid timetable is not a diagnosis or guaranteed watering need | NEW |
| 09 | Inspect/record | "What did you notice or do?" actual action, text/photo, optional follow-up | Can save "inspected, not watered"; don't create an event when viewing guide | REUSE |
| 10 | Save result | Backend acknowledgement, linked event and private timeline; retry original intent | Offline/upload failures stay retryable; no duplicate events | REUSE |
| 11 | Today / Check-ins | Real human-chosen due date; **Record outcome**, **Move date**, optional local reminder | Never claim "water now" based solely on elapsed days | REUSE |
| 12 | Living History | Actual dated entries, photos, comparison only with genuine dated pair | Filters, pagination, archive, full photo with truthful fallback | REUSE |
| 13 | Need help | **Ask the Circle** (explicit public preview) or **Contact Plantupia** only if real staff support approved | Two different destinations/privacy rules | Circle REUSE; company support GATED |
| 14 | Circle / Share | Approved photo+question preview, explicit publication consent | Private space, history and photos never auto-shared | REUSE |
| 15 | People / Inbox / Me | Shared Moments, people, follow, DMs, Story and optional legacy formats | Contextual secondary features, never mandatory to complete care | REUSE |
| 16 | Replacement/exit | Archived former plant, replacement with **new plant identity**, revoke transfer on account change | History preserved per plant and scoped to authorized membership | PARTIAL REUSE + NEW service workflow |

**Navigation proposal:** preserve existing four bottom destinations **My Space, Circle, Create, Me**. Put "Received Plantupia plants" as a **first-run/access entry**, and "Add a delivered space" in a contextual My Space action for existing users. Do not add a fifth QR tab. "Create" continues to offer private care/plant/space and deliberately public post/question/story. Messaging stays contextual.

**Guidance microcopy sample** (illustrative, NOT horticultural advice):
- Heading: **"Understand this plant"**
- Verified: **"Care information reviewed by Plantupia · [date]"** only when actually reviewed.
- Unverified: **"Species not confirmed. We can't give species-specific instructions yet."**
- Action: **"Check the soil and visible condition before deciding to water."** Only when approved as a suitable generic cue; never fabricate moisture readings.
- Secondary: **"Record what you noticed"** / **"Choose a check-in day"**.
- Responsible party: **"You care for this plant"** or **"Plantupia maintains this plant"** *only if contract/rule validated*, otherwise no role claim.

## 4. Operational lifecycle and invariants

**Installation lifecycle (proposed):** \`DRAFT -> VERIFIED -> READY_TO_HANDOVER -> DELIVERED -> CLAIMED -> ACTIVE -> ARCHIVED\`. A staff action authorizes each transition; \`CANCELLED\` and \`CLAIM_EXPIRED\` are recoverable states, not reasons to delete the underlying plant inventory.

**Individual plant lifecycle:** prepared -> active in verified space -> observed care/events -> optionally archived/replaced. **Replacing a living plant produces a new UUID**; planter/site continuity can reference both, but earlier care history never migrates onto a different biological individual.

**Handover claim invariants:**
- Prepared site and plants belong to one verifiable installation batch; don't create duplicates for repeated scans.
- QR contains **opaque non-PII token only**. Token stored server-side as a hash, randomly generated, scoped to one batch, with expiry, revocation, attempt limit and claimed state.
- **Scan != claim.** Customer must authenticate; backend validates who may claim before exposing private inventory.
- Preview must be minimal and non-sensitive. If customer identity is unknown, require company verification or staff-approved invitation; don't grant by token possession alone for sensitive customer spaces.
- Claim is transactional: mark one invitation used and grant role/ownership under database lock; repeated same-account claim returns same result, different-account replay is denied. Never let a company space become a public profile.
- Handover QR is **not** a permanent plant tag. If per-plant QR is used later, it is a non-secret ID that resolves only for current authorized members.
- No sign-up/login loop should discard a pending invitation. Never persist it to another user's account.
- For install where company staff retain care responsibility, server roles must be explicitly assigned, auditable and revocable; owner/manager/carer/viewer already exist but staff/organization semantics are **NOT confirmed**.
- Customer can always use B/manual route if never delivered Plantupia plants.

### Ownership transfer vs ongoing company-managed installation

The existing Django model distinguishes `Space.owner` from `SpaceMember(role=manager/carer/viewer)`. Therefore **"plants appear in the customer app" does not itself decide legal or technical ownership**. To avoid an unsafe implicit company account staying in charge forever:

- **Outright delivery/sale — proposed default for a customer-owned space**: staff safely stage the real inventory internally. After verified claim, an atomic handover makes the authenticated customer the `Space.owner` (or creates the equivalent client-owned space without duplicating plant identity and history). Temporary company provisioning access is revoked unless the customer explicitly authorizes a separately contracted service. No staff member remains entitled to personal photos by default.
- **Company-maintained site — explicitly approved agreement only**: ownership/control and customer manager/carer/viewer membership are defined by the actual contract. The app communicates who maintains what, and which logs either side can see. Revocation and staff reassignment are auditable.
- **No approved contract yet**: pilot with isolated test accounts and a documented simulated ownership policy; **do not** assert a real customer has ceded their space to Plantupia. A confirmed QR must not silently override an existing private `Space.owner`.
- **Idempotent claim** must resolve this ownership branch under a database transaction alongside invitation consumption and membership updates. The same exact plant IDs must persist whether rights are transferred or granted.

This product decision needs CEO confirmation before a real customer rollout but does **not** block designing or testing both branches in isolation.

## 5. Care: useful without manufacturing biological certainty

**Layer 1 — shared plant/species guide (new and gated):** verified taxonomy when known, source URL/document & revision, approving botanist/staff member, light/placement, substrate/drainage, water-inspection cues, pruning/fertilizer cautions and safety notes. Missing/unknown species is a valid state.

**Layer 2 — real individual context (partially existing):** plant identity, selected space, actual placement, pot/drainage/light context if staff recorded it, responsible caretaker if contract confirmed. Do **not** extrapolate sensors.

**Layer 3 — lived observations (existing):** dated actions and images performed by a human, optional chosen revisit day, later recorded outcome. A customer can inspect without watering; postponed inspection does not assert care happened.

**Scheduling decision:** V0 first shows source-approved *how to inspect* and offers an optional user-chosen review. A suggested review cadence is **not automatically a watering order** and only becomes a sourced, context-aware feature after botanical approval. Existing WorkManager notification is optional and subject to Android settings, not a contractual SLA.

## 6. Service & support — avoid pretending we have a staffed operation

If Plantupia already offers contractual maintenance, add a **permissioned support/service path**: customer submits real concern -> gets service eligibility and request status -> actual assigned staff actions and visit evidence -> customer views outcome. Company must define who responds, available hours, SLA, location sharing, and whether staff may view private images/history.

**Until that exists:** no "technician assigned", no estimated visit dates, no "24/7 expert" label and no simulated booking. The current private issue record + separately consented Circle question remain useful; the optional assistant should never imply a human has responded.

## 7. Lean data/API extension (design, not implemented)

Current backend already has \`Space\`, \`SpaceMember\`, \`PlantUnit\`, \`PlantEvent\`, \`PlantDiscussion\`, replay-safe \`PlantSubmission\`. Proposed additive, versioned entities:
- **InstallationBatch** — real external order reference, site label, status, delivered_at, source/import provenance; links to prepared \`Space\` and plant IDs. Sensitive customer name/address not in public QR or analytics.
- **HandoverInvitation** — batch, salted/hashed opaque token, expiry, issued/revoked/claimed timestamps, approved recipient/account channel and retry/audit evidence. Unique constraint ensures one canonical active claim per batch per agreed policy.
- **CareGuideVersion** — known species key or explicitly unknown, curated structured advice, source license/reference, last reviewed date, reviewer; no model-generated 'verified' badge.
- **PlantCareAssignment** (optional pilot if company-maintained) — plant/site, caretaker role, effective date, approver and revocation; not inferred merely because the person is a follower.
- **ServiceRequest** (later; only contracted) — scoped private plant, eligible customer, assigned staff, status, actual service log and closure.

**Potential authenticated endpoint contract** (relative under existing plants URL mount, exact implementation to be finalized): \`POST /handovers/preview/\` minimal auth-safe summary; \`POST /handovers/claim/\` atomic idempotent redemption; \`GET /plants/{id}/care-guide/\` scoped guide/version. Staff provisioning initially via guarded admin/import, not customer API. Protect logs/telemetry from full QR tokens. Rate-limit invalid attempts and always re-check authenticated permissions.

No environment secrets, API keys, database changes, migration, hosted deploy, QR feature or care guide were introduced by this decision document.

## 8. No-happy-path-only flow: edge cases

| Situation | Exact user experience | Server/design behavior |
| --- | --- | --- |
| App not installed after QR | Official HTTPS page -> store/install -> resume same pending invitation | Android App Links / verified domain **if** company controls suitable domain; in-app scanner fallback |
| Existing logged-in user | Open confirmation; ask explicit acceptance | Verify intended recipient; don't silently attach an unrelated installation |
| Wrong account / wrong delivery | "This handover needs verification" + safe contact pathway | No disclosure of private inventory; no side-effect |
| Token expired, revoked, already used | Specific recoverable guidance; authorized team can re-issue | Old token unusable, existing plants preserved |
| Scan twice while network retries | Existing success displayed if same account | Server-side transaction and idempotency |
| No Internet during claim | Pending/try again; no "activated" UI | No premature local ownership or duplicated plants |
| Plant species uncertain | "Species not confirmed"; generic safe check approved if any | No invented species-specific watering/fertilizing instruction |
| No photo or broken photo | Editorial non-evidence fallback | Never substitute fake plant/customer images |
| No user care permission | Read-only guide/history; no Save action | Existing server role guards; assignment validated |
| Customer declines publicity | Private care works normally | Consent checkbox never preselected; private media stays private |
| Plant swapped/replaced | New individual shown; older one archived with history | New identity; true previous install/planter association if recorded |
| Client loses phone/logs out | Recover own authenticated spaces after login | Claim not repeated; account isolation, no token leak |
| More than one site / 100 plants | Select site, search inventory, paginate history | Never load 100+ editing cards on first viewport |
| Local reminder denied/suppressed | Care save succeeds; honest reminder status | No blanket guarantee Android will deliver at exact minute |

## 9. Definition of done — test the whole experience, not the drawing

**Proof fixture**: company-authorized *synthetic QA* installation batch with **one space, seven distinct plant UUIDs** and approved care-guidance fixture, plus a separately seeded manual-only user. Clearly mark every test user/site as simulated; do not represent as real customer outcomes.

Acceptance walk-through:
1. Staff provisions approved sample site and seven distinct units; duplicate external identifiers rejected.
2. Issue one invitation; unauthenticated device scans and installs/opens app, signs in, retains invitation.
3. Correct account sees limited summary, confirms once, gets exactly seven correct records, none copied into another account.
4. Wrong user, second account, expired/revoked invitation, double/retry claim all fail or recover safely without duplicate plant records.
5. Open first plant; verified/unknown guidance respects species evidence and care responsibilities.
6. Record one real human observation and selected follow-up; reload/restart, no duplicate submission, matching private history.
7. Record the outcome; reschedule another event without inventing care; photo viewer displays only actual photos.
8. Post a plant question with explicit image/text review; second user sees only published details, not private space/history.
9. Independently add a plant without handover; all core care flows remain available.
10. Exercise no-network, long name, permissions, TalkBack, 320/360/390/430dp, 2x font, re-login/account switch and lost-token recovery.

**Current source state**: Existing care/history/privacy code partially covers #5–#9; QR/provisioning and curated guide do not yet exist. CI success alone is not a real Android phone E2E certificate.

## 10. Decision record, delivery order and tiny CEO validation gate

### Internally selected working defaults (not company facts)

| Decision | Our default now | Why |
| --- | --- | --- |
| Main customer experience | **One app, dual-entry, place-first after entry** | Reuses production domain, no restart |
| Handover representation | One **invitation per physical delivery/site**, not a secret QR per plant | Seven delivered plants can be claimed together; fewer scans and fewer vulnerabilities |
| Who provisions | **Manual protected staff back-office/import for pilot** | Minimal new surface; operational source still needs validation |
| Who maintains | **Assume customer performs personal care unless documented service contract states otherwise** | Never invent technician or paid service |
| Guidance | **Verified sourced care content first, observations second** | Separates expert knowledge from true human action |
| Calendar | **Optional human check-in**, not auto-watering | Biologically honest and already largely coded |
| Community | **Optional Circle, not auto-public** | Strong privacy boundary |
| Core navigation | Keep four native bottom tabs | Avoid gratuitous redesign |
| Commercial/IoT scope | Deferred until service truth is known | Protects engineering effort and user trust |

### Proposed build stages (not yet started)
- **G0 — freeze and verify current APK:** real existing create/care/history/account isolation paths, Edit Profile & People regression, latest backend reachability.
- **G1 — staff-ready installation contract:** controlled input records, installation batch model, immutable plant IDs, audit; company validation needed for source.
- **G2 — claim/activation:** secure invitation, preview/claim APIs and tests, Android scan/deep-link resume, no scanner dependency for noncustomers.
- **G3 — initial care understanding:** one reviewed sourced guide and responsible-care display per known plant; unknown and absent-guide fallback.
- **G4 — customer end-to-end trial:** one test delivery through care/outcome and privacy verification; adjust only from actual evidence.
- **G5 — optional professional service:** gated on commercial process, not a requirement to pilot G1–G4.

### Only three factual decisions we cannot invent for Plantupia

1. **What is delivered today?** plant-only, completed installed space, ongoing service, or a combination; what actual order/site data exists?
2. **Who approves real botanical care instructions?** named approver and approved source/version for at least a few pilot plants.
3. **Who is contractually responsible for care and support?** customer/company/shared, and if company then what human service capacity and access permissions?

Work on wireframes, schemas, validation and synthetic test runs can proceed meanwhile. Production handover/guide promises cannot.

## 11. Source-of-truth cross-reference

- [AS-IS Android app flow](./PLANTUPIA_AS_IS_ANDROID_FULL_USER_FLOW_2026-10-20.md)
- [CEO service blueprint](./PLANTUPIA_CUSTOMER_JOURNEY_AND_QR_HANDOVER_SERVICE_BLUEPRINT_2026-10-20.md)
- [AS-IS vs TO-BE gap comparison](./PLANTUPIA_AS_IS_VS_CEO_GAP_DECISIONS_2026-10-20.md)
- [Product Bible](./PLANTUPIA_PRODUCT_BIBLE_AND_HANDOFF_V01_2026-10-17.md)
- Android: \`app/src/main/java/com/nova/app/NovaApp.kt\`, \`feature/plants/PlantupiaClientSection.kt\`, \`feature/plants/PlantupiaCreateIntent.kt\`
- Backend: \`backend/plants/models.py\`, \`backend/plants/urls.py\`
- Tests/closure: \`docs/PLANTUPIA_SCREEN_CLOSURE_LEDGER.md\`

**Work remains Draft PR #241; do not merge or deploy to the existing Nova production environment without explicit business/release approval.**
