# Plantupia — Ahmed & Company Website Sign-off Register
**Date:** 23 Nov 2026  |  **Status:** Not signed; company decisions required.
**Purpose:** One place to collect the actual acceptance decisions before changing any public domain. A nice preview and a READY build do not constitute approval.

## Review 1 — Creative / content
| Item | Decision to record | Evidence needed | Responsible reviewer | Decision/date |
| --- | --- | --- | --- | --- |
| Brand and site story | Accurate final description of Plantupia's Green Technology services, About, contact wording and intended audience | Final approved copy | Plantupia/CEO or delegated reviewer | Pending |
| English-first presentation | Keep English launch; add Arabic later only if separately approved | Audience/language decision | Plantupia | Pending |
| Site photographs | Approve each original private proposal/catalogue asset, client name/logo and reproduction rights | Per-asset source and written permission | Plantupia rights/content owner | Pending |
| Six project studies | Confirm 'proposal/alternative' vs completed installation; approve technical claims, captions, source p# | Six case-review sheets | Project owners and creative | Pending |
| Homepage / navigation | Approve final image order, first impression, + index, readable desktop/mobile and disabled reduced motion effects | Protected preview walkthrough | Ahmed / Plantupia | Pending |

## Review 2 — Product / real operations
| Area | Acceptance proof | Scope boundary | Reviewer | Decision/date |
| --- | --- | --- | --- | --- |
| Shop launch | Real reviewed/published SKU name, photograph, price and stock load from intended backend; withdrawal hides item | Web displays collection and enquiry, **not web checkout** | Commerce/product owner | Pending |
| Order channel | Authenticated customer app correctly receives buying flow and computes shipping/fees | No guest order/reservation created from web | Mobile/backend owner | Pending |
| Enquiries | Real project/care/general message received in Company Inbox, retry dedupes, staff roles enforced, visitor has confirmation/reference | Preview sending stays disabled until controlled test and approval | Company operations + backend | Pending |
| Care language | Actual contract coverage for installation, smart irrigation, scheduled visits, warranty and QR/plant records verified | Illustrative timeline must not imply real telemetry or completed visits | Operations + legal/company reviewer | Pending |
| Privacy / response owner | Approved retention policy, anti-spam controls, response team and secure customer-data procedure | Never store visitor details just to make a demo | Plantupia + company operations | Pending |

## Review 3 — Technical gates
- [x] Web code head dd76a6e tested: TypeScript, production build, 41/41 security/route smoke, 14/14 Chrome visitor tests (390 and 1440). https://github.com/omarkhair-labs/nova/actions/runs/45099253019
- [ ] Protected preview visually approved on desktop, real Android phone, real iPhone if available, 320/390/768/1440px and short landscape.
- [ ] Reduced motion, keyboard focus, dialogues, links, image loading/error states and accessibility pass.
- [ ] Measure first visit LCP/INP/CLS and verify no expensive scroll handlers, image decode surprises or late shifts.
- [ ] Verify published SKU API and Company Inbox against authorized real backend + migrations, staff permissions, rate limiting and backups.
- [ ] Approve actual hostname, metadata/OG/sitemap and contact/legal details; confirm who can modify DNS.
- [ ] Generate production approval manifest listing approvedBy, approvedAt, approvedAssets with actual rights evidence, and include only approved files.
- [ ] Build with production configuration without promoting domain; smoke the *exact* artifact and record rollback.
- [ ] Company representative signs a clear Go/No-Go on the exact build; then release owner performs approved alias promotion.

## Proof currently available
- Earlier browser QA: 17 routes × 2 viewports; six-project 12-case study checks; Pixels motion and scroll checks; successful protected preview.
- Source web closure run: https://github.com/omarkhair-labs/nova/actions/runs/45098010722 — typecheck, production build and **41/41 public publication-gate smoke checks** green.
- Latest Chromium browser acceptance completed successfully: **14/14** cases at phone and desktop widths, plus 41/41 non-publication release smoke. These tests do not replace physical-device/accessibility/performance signoff.
- Current last photographed protected preview: https://plantupia-public-preview-du00ehney-omarkhair70-droids-projects.vercel.app ; newer source-only CI changes are not assumed present at that URL.
- Main review concept/closure map: docs/PLANTUPIA_WEB_CLOSURE_AND_AHMED_REVIEW_2026-11-23.md.

## Meeting record / fill during actual review
**Reviewer:** Pending

**Date:** Pending

**Creative direction:** Pending approval

**Approved assets and exact source names:** Pending

**Services and case claims:** Pending

**Shop, customer-app channel and Company Inbox owners:** Pending

**Official domain + release owner:** Pending

**Decision:** NO PUBLIC PROMOTION until explicit signoff and green final gates.

**Agreed modifications with owner, priority and verification:** Pending
