# PLANTUPIA
## Product Bible · Experience Rationale · Engineering & Client Handoff
**Working edition v0.1 | 17 October 2026 | Android Native | Review in progress**

**Product:** Plantupia
**Prepared by:** Product-building / design & engineering workstream
**Audience:** Client stakeholders, product/design partners, future Android/backend maintainers, internal reviewers
**Status:** **WORKING HANDOFF — not company approval, app-store clearance or production sign-off**
**Single source of truth for live feature closure:** [Native screen ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md)
**Implementation:** [PR #240](https://github.com/omarkhair-labs/nova/pull/240) → [stacked PR #241](https://github.com/omarkhair-labs/nova/pull/241). All branch links in this edition target `feature/plantupia-native-experience-20261012`; revisions continue until one integrated device-tested build is approved.

> **Product thesis** — A living plant is not an isolated object or generic reminder. It exists in a real place, is cared for by real people, and becomes understood through honest observation and time. Plantupia connects **place → plant → human care → dated evidence → observed outcome**, with community knowledge and a voluntary assistant as **optional** additions, never automatic exposure of private records.

This is the **narrative parent document**. The linked source atlases, interactive design studies, backend tests, product screen contracts and release evidence remain separate appendices—not overwritten by one polished story. A design concept is not represented as client-accepted simply because it is included here.

---

## 01 · The problem, market and actual operating context

Plant-care apps often reduce care to disconnected species advice, a timer, and a social feed. That loses *the physical context* of a specific plant, who observed it, what was done, and whether the intervention helped weeks later.

The company context is **green technology / biophilic spaces**, spanning nature, architecture, human experience and prospective business outcomes. The provided company brand positioning includes **“Transforming Spaces. Inspiring People.”** Do not collapse this into a generic plant-shop identity. Consumer-owned household/office spaces are a practical, accessible starting point; commercial installed-site management is **not** presumed to exist without approved operational evidence, people/roles and service contracts.

**Users/scenarios used to stress the product, not verified customer interviews:**
- **First plant / zero state:** a person should create a genuine private place, then a plant and first photo/record, without fake demo data.
- **Household with ~7 plants:** a person should return to a real selected review date, record a short observed action, and retrieve prior evidence.
- **Large inventory 100+:** an authorized person should search/switch actual places and progressively access records without a 100-card screen or invisible due check-ins.
- **Company-maintained space:** a potential future B1 scenario, **gated on validated company roles, installations, entitlements and operating procedures**.
- **Living physical object:** explored as an alternative conceptual world, not an approved Android V1 sensor or hardware deliverable.

**Product promise for working review (not approved marketing copy):** Know the plants that live with you, keep their true context, care with more confidence, and remember what actually changed.

### Non-negotiable truths
1. Never fabricate plant photos, installations, sensor readings, diagnosis confidence or health scores.
2. A check-in date is chosen by a person; it is **not** an automatic watering instruction.
3. A care record is private unless an authorized person separately authors, reviews and consents to specific public content.
4. A proposal, HTML prototype, GitHub CI pass and real device-tested feature are **different states**.
5. Native Kotlin/Jetpack Compose Android only in this release. Reuse the existing Nova social/video/messages infrastructure instead of rebuilding it.

---

## 02 · Discovery and the chain of reasoning

### How the work was done
**Reality / company context → direct and cross-domain research → Source Atlases 02/03/04/05 → seven provisional meanings → product-system translation → three-scenario stress test → four possible product worlds → critique → product/IA/screen atlas → native conversion → Visual Study 01/02/03 → Pass 04 award-backed cross-domain research → Pass 05 behavior teardown → Pass 06 field-validation protocol → Pass 07 integrated candidate → Native engineering/QA.**

The process is cyclical, but **the current client delivery is bounded**. An unresolved research hypothesis does not justify perpetual redesign or block a truthful, smaller test version.

### Seven underlying meanings (provisional synthesis, 7 Oct)
| Research-derived meaning | Product behavior, not just interface styling |
| --- | --- |
| 1. **Relational Life** | Give each plant a place, human context and optional placement; do not design only a flat species grid. |
| 2. **Living Time** | Represent check-in dates and change as dated human records, not a static “healthy/sick” badge. |
| 3. **Retained History** | Preserve **observation → decision/intervention → later outcome** with author/evidence provenance. |
| 4. **Care Creates Relationship** | Make observation and meaningful human involvement easy; automate burden without erasing care. |
| 5. **Living Agency** | Let a person deliberately choose care, recording, future review, sharing and AI access. |
| 6. **Legibility** | Make uncertainty, access role, next action and source of information understandable. |
| 7. **Accountable Living** | Identify who records what and when, and keep private/public permissions enforceable. |

The exact original formulations and alternative implications are retained in [S1 Meaning Extraction](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/synthesis/01-meaning-extraction.md) and [S2 Product Translation](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/synthesis/02-product-translation.md).

### Four early product worlds (alternatives, not four committed apps)
| World | Core opportunity | Disposition |
| --- | --- | --- |
| **Living Operations** | Continuity for professionally maintained sites: installation → actions → evidence → outcome | Preserve logic, defer full field-service ERP without company operational validation. |
| **Plant Literacy** | Learn to notice and understand plants in context | Keep attentive observation, defer unapproved botanical knowledge corpus or diagnoses. |
| **Living Object** | Plant/architecture/physical interaction as a product | Research concept; **no currently installed sensors** or hardware claims. |
| **Living Materials / Specifier** | Living greenery as architectural and material design choice | Relevant to brand/site story, separate from first native care flow. |

Source: [Exploration and cross-world critique](https://github.com/omarkhair70-droid/Plantupia/tree/research/foundation-source-atlas-20261007/docs/exploration). No comparative field results justify claiming one of these worlds won.

### Why the last research restart matters
Pass 04 deliberately searched **outside plant-app competitors**. Pass 05 reverse-engineered interactions by **trigger → action → feedback → real data change → return → failure**, not screenshot imitation. Relevant inspiration included object capture confirmation (CapWords), truthfully surfaced time (Moonlitt / Tide Guide), low-distraction attention (Focus Friend), linked sources (Are.na), and chronological personal evidence (Day One). These are **interaction principles**, not copied UI, endorsement by the referenced products or proof Plantupia will succeed.

Pass 05 compared **Living Atlas / PLACE**, **Field Notes / EVIDENCE**, **Quiet Care / ATTENTION** using *simulated* fixtures; Pass 06 provided a plan to interview/observe real people but **no actual participant scores**. Pass 07 synthesized a fourth candidate:

**PLACE (where it lives) → QUIET CARE (what deserves attention now) → FIELD NOTES (what was truly seen/done) → HUMAN HELP (optional Circle; assistant explicitly opted in).**

This is the working design direction translated selectively to Compose, **not** a signed-off winning usability study. The original Pass 05/07 interactive HTML artifacts are distinct from native Android and must not be mistaken for shipped application screens.

References: [Pass 04](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/research/06-award-winning-cross-domain-discovery-pass-04-2026-10-14.md) · [Pass 05](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/07-award-winners-behavioral-teardown-pass-05-2026-10-14.md) · [Pass 06](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/research/08-field-realities-validation-protocol-pass-06-2026-10-14.md) · [Pass 07](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/09-pass07-integrated-product-synthesis-contract-2026-10-14.md).

---

## 03 · Main decisions and why they were taken

Decision classes: **LOCKED WORKING** = chosen for current implementation (not store approval); **CANDIDATE** = design validation pending; **GATED** = requires another stakeholder/source; **REJECTED CURRENT RELEASE** = does not belong in Android client test.

| ID / state | Decision | Rationale / evidence | Trade-off and review trigger |
| --- | --- | --- | --- |
| D01 · LOCKED WORKING | Android Native **Kotlin + Jetpack Compose** | Existing Nova foundation includes actual login, native UI, posts, Stories, Reels, DM, media and CI | Codebase reuse introduces legacy UI debt; no Flutter/iOS/WebView rewrite now. |
| D02 · LOCKED WORKING | Domain identity **space → plant → private dated event → optional follow-up/outcome** | 7 principles + stress tests; records need biological and spatial context | Some site admin features deferred; no raw CRUD-only plant inventory as the core experience. |
| D03 · LOCKED WORKING | Keep user-chosen care dates, no automatic health/soil sensor data | Actual company/project scope has no installed sensors | Basic timer-only users may prefer simplicity; never manufacture recommendations to fill UI. |
| D04 · LOCKED WORKING | Private care and public Circle question are distinct permissions and user actions | Evidence/accountability + photo privacy | More explicit steps to publish, intentionally. |
| D05 · CANDIDATE IN NATIVE | **My Space / Circle / Create / Me** tabs; Inbox contextual | Preserves Nova's engine, creates space-first entry point | Study 03 alternative Care-tab + Create-drawer wasn't chosen from actual user scores. Revisit only with observed device evidence. |
| D06 · CANDIDATE IN NATIVE | Photo-led selected-place Home + due cross-space items | Pass 07 synthesis | Actual plant photo shown as *plant in place*, not a fabricated full-room photo; avoid hero pushing care too far down. |
| D07 · LOCKED WORKING | Existing Django/Nova APIs, Oracle-hosted app process, PostgreSQL/Redis infrastructure | Retains real session and media/social systems | Installed/reachable *latest staging deployment* must still be proven before QA claims. |
| D08 · GATED | Assistant: server-only provider key, contextual access by consent | Useful optional human prompt; no autonomous plant action | Provider live replies, rate limits and uncertainties need actual QA. |
| D09 · GATED | Plantupia **separate Google Play app** on the builder's own Play account | Most recent explicit distribution direction | Distinct package ID, Firebase, signing, final AAB and Play policy metadata not implemented/authorized simply by recording this decision. Existing Nova listing stays unchanged. |
| D10 · GATED | Company installation/technician roles, service guarantees and invoicing | Research saw a possible B2B/aftercare lifecycle | No guessed customers, employees, SLA, zones or paid entitlements in current Android demo. |
| D11 · GATED | Approved final logo assets, typography licenses and company-facing claims | Official company Brand Guidelines provided as private source | Use verified palette/direction, do not publish confidential source PDF or embed unlicensed fonts/logo. |
| D12 · REJECTED CURRENT RELEASE | IoT pairing, silent watering AI, auto diagnosis, fabricated images and fake historical data | No sensor system or validated biological model exists | Explore independently in a future contracted product track only. |

### Design choices should be auditable, not anecdotes
For **every future decision**, record: **ID · date · decision owner · problem · alternatives · evidence source · chosen behavior · implementation link · risks · status · validation/falsification trigger**. An interface screenshot without the decision behind it is not enough. Existing implementation decisions are reflected in the [finite roadmap](./PLANTUPIA_FINITE_INTEGRATED_DELIVERY_ROADMAP_2026-10-15.md) and [screen ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md).

---

## 04 · Experience and information architecture

### The core 30–60 second journey (actual data, not stock demo)
1. Sign in through the existing authenticated Nova session.
2. First-time user creates **My Space** (home, balcony, office or other named location); space is private.
3. Add a real plant with shared label, optional species/placement, optional personal cover photo.
4. On **My Space / Today**, inspect real due check-ins across authorized spaces, or switch to a selected place.
5. Open the plant → choose **actual observed care action / observation / concern** → optionally attach selected local photo and choose next human check-in day → save to Django.
6. Return later, find the dated event, record what **actually changed** and close or reschedule pending review.
7. If desired, compose a **separately previewed public Circle question** about a chosen plant, with a particular photo, reviewed title/text and explicit consent. Public post uses existing Nova posts/comments; it must never carry the source plant's private history/placement automatically.
8. People can reply through the social engine; the plant owner can write a new **private observed outcome** linked to that public question. The assistant is optional, never silent.
9. My **Profile** holds public identity/people/posts and a distinct self-only private-space summary. Inbox retains original person-to-person messaging, groups, and existing multimedia routes.

### Navigation/current authored experience
| Destination | Primary purpose | Related data and privacy | Native implementation | Still needs proof |
| --- | --- | --- | --- | --- |
| **My Space** | Today + selected space → plant inventory / history | Authenticated private user/member data | `feature/home/HomeScreen.kt`, `feature/plants/PlantupiaClientSection.kt`, `PlantupiaTodayPanel.kt` | Device scroll, space select, 0/1/7/100, offline & real photos |
| **Circle** | Human questions/replies and social discovery | Public Nova post object with consent; no private plant payload | `feature/orbit/OrbitScreen.kt`, `PlantupiaCommunityPanels.kt` | Two-account publication/comments/moderation and refresh |
| **Create** | Intentional separation: record privately, add plant, publish question, optional social media | Private vs public actions must not merge | `feature/create/CreateHubScreen.kt`, `PlantupiaCreateIntent.kt` | Back, nested Story/media viewer, composer visibility and upload |
| **Me** | Public social identity + self-only private-space card | Private spaces only on own Profile, not other people's profiles | `feature/profile/ProfileScreen.kt`, `PlantupiaProfileSpaces.kt` | Owner/stranger proof, refresh after edits |
| **Inbox / DMs** | Human help through messages/calls | Nova authenticated messaging permissions, no implicit plant data | `MessagesActivity.kt`, `feature/messages/MessagesScreen.kt`, `InboxViewModel.kt` | Keyboard, groups/calls/media, cross-account & filter behavior |
| **Plant detail** | Real plant identity, care, reviews and history | Owner/manager/carer/viewer server roles | `PlantupiaClientSection.kt`, history/photo/review components | On-device real save/slow API/photo and permission QA |

The user requested **one final integrated Android APK review**, not piecemeal screen acceptances; source comments such as “pass coded” are not client signoff.

---

## 05 · Brand and UI art direction

**Company context:** sophisticated biophilic design/green technology: real nature **in** architecture and people’s lives, not decorative stock leaves.

**Working palette based on supplied brand direction:** forest `#123B2E`, warm cream `#F3EEDE`, sage `#9FB79A`, support dark `#16302A`. Final contrast, use cases, logos and licensing must be checked against the owner-supplied **official Brand Guidelines** before release. Serif editorial headings and restrained readable body typography express architectural materiality, but **bundled Playfair Display/Inter brand assets must not be assumed**: current Compose code often uses system `FontFamily.Serif`, not verified licensed final fonts.

**Visual laws**:
- Let a real plant/photo carry meaning. If absent, show an honest editorial empty state, never impersonate a client installation.
- Make **care controls immediate** and distinct from optional Circle/assistant/social actions.
- Editorial whitespace, calm physical material, architecture + greenery; **not** a generic growth-gamification dashboard, maximal-green recolor or cinematic mockup.
- User-authored records keep original language; new product UI is **English-first**.
- No automatic plant health percentage, suspicious empty graphs, fabricated “before/after”, imaginary site floorplans or unapproved service badges.
- Use native Android touch size, focus, Back, offline states, photo permissions, high contrast, text enlargement, TalkBack and reduced-motion judgement.
- Motion serves confirmation and wayfinding; inherited Nova media performance must not regress.

**Prototype rationale and acceptance are separate:**
- [Visual Study 03 review](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/FULL_PRODUCT_VISUAL_REVIEW_03_2026-10-12.md) explored options including a Care-tab/drawer; those are **not automatically implemented**.
- [Pass 07 interaction contract](./PLANTUPIA_PASS07_NATIVE_EXECUTION_CONTRACT.md) maps genuine observations to native behavior.
- Final visual evidence requires **actual 360/390/430dp Android screenshots and walkthrough video**, not reusing HTML viewport captures.

---

## 06 · Platform, domain model and architecture handoff

### Android client / inherited platform
Kotlin + Jetpack Compose Material3; existing Nova app shell/session and Navigation3; Coil image handling; native Android photo picker/media upload; WorkManager local opt-in check-in reminders; Media3/ExoPlayer reels/video; existing Nova social posts/comments, Stories, people/relationships and inbox/DM/group calls. The existing Nova technical modules are **not** a public design specification for Plantupia.

### Django backend / data owners (current repo)
- `backend/plants/models.py`: **Space**, **SpaceMember** (owner / manager / carer / viewer), **PlantUnit**, private **PlantNickname**, **PlantEvent**, public-to-private link **PlantDiscussion**.
- `backend/plants/views.py`, `serializers.py`, `urls.py`: authenticated space/plant creation, permission-scoped listing/updates, private care & photo, rescheduled follow-up, pending outcome closure, archived plant retention, public question with consent & sanitized photo, Circle feed.
- `backend/plants/assistant.py`: server-side adapter for a user-approved model provider; absence of a configured key is not evidence of live assistant functionality.
- `backend/plants/test_permissions.py` and `test_native_product_journeys.py`: backend unit/contract scenarios and privacy cases, **not a live phone QA certificate**.
- `feature/plants/data/PlantupiaRepository.kt`: authenticated Kotlin REST client using the existing Nova API/session wiring.

**Truthful state:** GitHub tests/CI have passed on prior exact heads, but verified *currently deployed* Oracle/preview route revision, isolated DB/media/Redis settings, phone-to-service reachability, AI provider live answer and two real accounts remain a separate acceptance gate. Do not publish credentials or internal host/database connection strings in client handoff.

### Permission model
| Actor | View authorized space | Add plant / edit shared plant details | Record care | View private photos | Publish own plant question |
| --- | --- | --- | --- | --- | --- |
| Owner | Yes | Yes | Yes | Yes | Yes, **only** after explicit public preview/consent |
| Manager | Yes | Yes | Yes | Yes | Subject to existing server-authorized care/public post rules |
| Carer | Yes | No shared-identity admin | Yes | Yes | Only if authorized and explicitly consented |
| Viewer | Yes | No | No | Yes within authorized space | No direct care-owned question creation |
| Unrelated account | **No** | No | No | **No** | May browse only posts visible through social privacy rules |

Server permission checks, not hidden Android buttons, are the trust boundary. A private nickname belongs to its user; a shared plant identity affects authorized members and must be audit-traced.

### Dated record data / correct semantics
A saved event has a plant, actor, kind (care / observation / concern / outcome), evidence type, observed timestamp, note and optional private photo. A user can select a follow-up date; a later event may explicitly complete that review. Rescheduling does not claim an observation or plant watering happened. Archive/restore retains previous private events/photos; a replacement plant is a **different biological identity**, not a renamed old one.

### External dependencies / separation
- Existing Oracle-hosted Nova/Plantupia application process and previously prepared PostgreSQL/Redis resources are **not new infrastructure to rebuild**; verify the actual staging wiring/revision rather than guess.
- A distinct Plantupia app on the builder’s own Google Play Console is the current intended destination, **but** the shared Nova source still uses original default package naming at the time of this document. A separately owned `applicationId`, suitable signing/Firebase, a signed **AAB**, policy/permissions/account-deletion metadata and distribution workflow are **work remaining**, never implied by an already-built QA APK.
- Existing Nova production listing and server data must not be silently overwritten. Client distribution, IP/ownership, confidentiality and access-sharing are business approvals, not matters solved by a GitHub commit.

---

## 07 · Requirement-to-design-to-code traceability (what the client actually paid to understand)

| Need / principle | Research/decision anchor | Design result | Shipped-code evidence so far | Remaining acceptance |
| --- | --- | --- | --- | --- |
| A real plant belongs somewhere | Relational Life, Living Atlas | Selected Space → plant identity and placement | Space/PlantUnit and native space hero, switcher, scoped inventory | Persisted real device data; account roles |
| Do care without drowning in UI | Care Creates Relationship, Quiet Care | Due check-in then one act / optional note/photo/date | Today panel, care composer, progressive due reveal | 0/1/7/100 device task time & errors |
| Keep evidence, not fake confidence | Retained History / Field Notes | Dated photo journal & follow-up outcome | PlantEvent, photo index, timeline, paged history | Long history, image zoom and server persistence |
| Human knowledge without leaking private history | Accountable Living / Are.na | Preview exact chosen image/topic, explicit public checkbox | PlantDiscussion + sanitized Nova Post, Circle feed | Two accounts, EXIF/no-data exposure, replies |
| Let users author their identity | Living Agency | Personal nickname vs manager shared context | nickname, edit and audit, archive/restore | Different actor roles, screen copy |
| Preserve the original social foundation | Architecture ADR-0001 | English-first Circle/Create/Profile/Inbox/Stories/Reels | Reused Nova native features, current Pass 07 integration | Full navigation, media, chat and reliability |
| Make a new app, not replace Nova | Latest owner distribution decision | Independent branded Play listing | **NOT YET CODE-COMPLETE** | Approved package/signing/FCM/Play Internal Test |

### Current screen-closure status
The fine-grained A00–A04, S01–S07, P01–P04, C01–C08, AI01/02, M01–M08, U01–U05, B01 statuses and explicit **GAP / HYBRID / CODED** semantics are maintained only in [the screen ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md). In particular:
- Current Kotlin coding covers private spaces/plants, care, check-ins, history, typed updates, archive/restore, private/profile, consented questions, and a substantial reused Nova social/messaging engine.
- Recent closure increments guard stale private history after fast navigation, truthful photo provenance, private care Save→Back behavior, editor discoverability and stale Circle feed pagination; same-head CI/device test outcomes must be attached to the ledger.
- Known **not shipped**: real operational company service workflows, invite/member management UI, site zoning/archival workflow, species guide curated from approved sources, linked Story→Plant, image AI/RAG, automatic IoT telemetry.
- Installed/buildable features do **not** equal integrated physical-device or company acceptance.

---

## 08 · QA, evidence and the one integrated client review

### Definition of a real tested feature
- **CODED:** relevant client/API functionality exists.
- **UNIT/CI PASSED:** tests/build/lint on *named exact commit* succeeded.
- **STAGING VERIFIED:** same release runs on an actually isolated QA backend, correctly connected to database/media/Redis and authorized login.
- **DEVICE VERIFIED:** real Android session, photo permissions and content, network failures, keyboard, 0/1/7/100, social multimedia and slow navigation have been checked.
- **TWO-ACCOUNT PRIVACY VERIFIED:** owner and stranger/carer/viewer cannot see each other's private data; chosen question appears publicly only as expected.
- **CLIENT REVIEWED:** client reviews **the one integrated QA APK**, including known exclusions; approval or changes captured by date and build ID.
- **PLAY INTERNAL TEST AVAILABLE:** separate signing/applicationId/AAB listing and testers configured; not necessarily Production.
- **PRODUCTION APPROVED:** all applicable app-store policies and contractual business gates resolved.

### Required regression walk-through
1. Fresh install → welcome/login/session resume and expiry; no legacy Nova branding in company-visible primary flow.
2. Empty account → Space → Plant with/without cover → real upload/save → reopen → app restart.
3. Authorized second space and 1/7/100 plant inventory → selected Space, filtering, archive/restore, manager/viewer permissions.
4. Record a **specific actual** soil-check/observation/concern with note/photo → see dated history → optional local review → reschedule/outcome → no duplicate mutation.
5. Switch plant quickly and press Android Back while old request loads; verify old private events never appear on new plant.
6. Create private moment versus public question; preview exact photo and text, consent, no EXIF metadata, second account comments, private observed outcome.
7. Story/video Create, public post, Circle, Profile → Inbox/DM/group → Android Back; actual player state and keyboard.
8. Offline, API error/404, denied notifications/images, large photo, logout/login as another user, TalkBack/text enlargement, 360/390/430dp screen capture.
9. Actual AI response only if provider is authorized and enabled; otherwise deliver an honest unavailable state and written exclusion.
10. One exact commit → latest Android/backend green CI → physical evidence → integrated client APK handoff.

**Evidence log to fill during device test (blank by design):**
| Date/build SHA | Android device/OS | API environment | User journey | Pass/fail | Photo/video/capture location | Issue owner/status |
| --- | --- | --- | --- | --- | --- | --- |
| _Pending_ | _Pending_ | _Pending_ | _Pending_ | _Pending_ | _Pending_ | _Pending_ |

Do not fill rows with hypothetical successes.

---

## 09 · Versioned delivery and what to hand to the company

**Recommended company handoff set:**
1. **This Product Bible**: thesis, research logic, alternatives, decisions, experience/system contracts and delivery boundary.
2. **Visual annex**: dated actual Android screenshots (all key screens/states), approved identity/assets/licenses, Figma/interactive prototypes linked explicitly as *research*, never passed off as source UI.
3. **Functional appendix**: exact [screen closure ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md), accepted user journeys, known limitations; no unapproved commitments.
4. **Technical handoff**: version/commit and PRs, API and role model, migrations, dependency/configuration checklist **without sharing credentials**.
5. **QA evidence**: exact-head CI, tested device matrix, account-separation scenarios, bugs and fixes, actual APK/AAB hash and install/test instructions.
6. **Release note / next-phase decisions**: target Play account and package, signed AAB/track availability, permissions/privacy/app content, version, responsible maintainers, pending B1 and AI/guidance gates.

**Proposed artifact names** (generated only when the evidence exists):
- `Plantupia_Product_Bible_v1.0.pdf`
- `Plantupia_Android_UX_Screen_Annex_v1.0.pdf`
- `Plantupia_Technical_Handoff_v1.0.pdf`
- `Plantupia_QA_Signoff_<buildSHA>.pdf`
- `Plantupia_Android_QA_<buildSHA>.apk`
- `Plantupia_Android_Play_<version>.aab`

This Markdown file is the *maintainable working source*, not a claim that these PDF/build artifacts have been generated or that a company handoff has already occurred. Keep any proprietary logo assets, unapproved brand PDFs, real user private photography, host credentials, commercial terms, and personal employment details **out of a public GitHub repository**. A final client-only document may include approved proprietary annexes in an access-controlled destination.

---

## 10 · Open questions, without blocking honest current progress

| Question | Current answer | Who confirms | Delivery impact |
| --- | --- | --- | --- |
| Is the selected Nav A or Study 03 care-tab B better in real tasks? | Not field-tested. Native A currently built. | Product owner after integrated QA | Do **not** silently redesign all root navigation. |
| Are final logo and font packages licensed and approved? | Direction provided; actual bundled files not verified. | Brand/company owner | Brand finalization before release. |
| Is latest Plantupia code actually deployed on correct isolated Oracle QA endpoint? | CI yes; live staging evidence not attached. | Hosting/engineering | Blocks truthful server/device testing. |
| Can the paid client test a separate Play package? | Builder's own Play Console intended; package/signing/FCM pending. | Developer account owner | Blocks independent Play distribution, **not** branch code work. |
| Are public AI requests allowed and affordable for this deployment? | Provider adapter exists, live QA not certified. | Product/company owner | Gate assistant feature or mark disabled. |
| Are company-installed spaces, staff roles, zones and professional aftercare contracted features? | No verified company SOP/roster. | Authorized company stakeholder | Do not invent operational screens or entitlements. |
| Who approves the final APK and documents? | Not recorded in source. | Client project contact | Record name/date/version after actual review. |

### Maintenance rule
Every relevant change must update: **the PR commit → affected screen ledger row → decision/rationale if behavior changes → actual QA evidence → release notes once approved**. Use this document as a navigable executive story; keep the repo/source files and test results as the auditable source of truth.

**Document footer —** v0.1 / 2026-10-17 / prepared for integrated user-and-client review, **not an announcement of launch**.

### 2026-10-18 maintenance increment — Integrity Sprint 01

Audit F06/F07/F12 now have scoped native implementation and JVM regression coverage: immutable/account-scoped mutation callbacks, independent pending check-in failure/retry states, and strict successful-response contracts with retained Nova no-content compatibility. F01 was deterministically reproduced; shared authentication remains unchanged and open. See [versioned sprint evidence and proposed F01 repair](./PLANTUPIA_INTEGRITY_SPRINT_01_2026-10-18.md). Source/CI increments do not mark any screen device-verified or owner-accepted.


### 2026-10-18 maintenance increment — Integrity Sprint 02

F01 now has a shared native session-generation/refresh coordinator, required-owner credential/profile writes and deterministic cross-account/background regression coverage. This supersedes the preceding investigation-only status without changing backend authentication, deletion/consent policy or Play configuration. See [Sprint 02 evidence and compatibility review](./PLANTUPIA_INTEGRITY_SPRINT_02_2026-10-18.md). Device/hosted QA and whole-app release approval remain open.

### 2026-10-18 maintenance increment — Integrity Sprint 03

F05 now has durable account/plant-scoped native submission identities and atomic Django replay/conflict enforcement for private care and consented public questions. Retries preserve observation time and exact photo bytes; recovered public questions require review and explicit consent. See [Sprint 03 contract, migration/compatibility review and device QA](./PLANTUPIA_INTEGRITY_SPRINT_03_2026-10-18.md). Migration 0004 was tested only in disposable environments. Legacy clients without IDs remain outside the new guarantee; hosted rollout and device/release gates remain open.
