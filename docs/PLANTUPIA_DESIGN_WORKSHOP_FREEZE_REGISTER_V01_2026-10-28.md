# Plantupia — Product Design Workshop & Freeze Register v0.1

**2026-10-28 · DRAFT FOR OWNER REVIEW · NO IMPLEMENTATION APPROVAL**

This is the in-repository entry point for the owner-reviewed *Plantupia Design Workshop v0.1*. The downloadable workshop package in the owner conversation includes a 31-page PDF, an interactive offline HTML board with **28 screen states**, this editable decision map, device evidence references and an asset manifest. Do not assume the attached artifacts are automatically hosted in this repository. Once the design is signed off, publish the approved drawings/assets with exact asset provenance.

**Scope:** `omarkhair-labs/nova` existing Android + Django, draft PR #241. Independent `plantupia-app` is outside scope. This docs-only branch does not merge, deploy, modify production data, create a live payment capability or authorize Codex implementation.

## 1. One product, not independent screen features

Two entry paths:
- **Delivery:** Plantupia staff record authentic installation/unit identities, approve required content, validate actual delivery, issue recipient-bound handover, and the authorized customer claims the original plants.
- **Independent plants:** customers create their own private space and plants without QR or purchase.

Separate commerce loop:
- Discover → Product → Cart → Review → Order.
- After *real* fulfillment, a verified company installation and customer-bound Handover may connect a plant order to My Plants. Ordering alone NEVER grants plant access. Every physical specimen retains an individual identity/history; non-plant merchandise never produces a PlantUnit.

Continuity:
- My Plants → Care Guide → Record real observation/care → Living History → actual next check → return to Today → optional AI, team or Circle.

## 2. Navigation / original platform

**Primary:** Today / Plants / Create / Shop / Circle. **Contextual:** Inbox, Activity/Notifications, Avatar/Profile. Do not add root tabs for Cart, Orders, Group Chats, Membership or Handover.

Reuse original Nova Feed/Posts/Comments, Stories, Reels/media, social Inbox/Groups/People and platform notification settings. Keep team support separate from social DMs. Public Circle questions require separate preview and explicit permission; private plant notes/photos/history are NEVER implicitly published.

Care, plant records/history, and manual spaces remain free independent of QA offer activation.

## 3. Screen registry (28 concepts; nothing automatically frozen)

| Group | IDs | Screens |
| --- | --- | --- |
| Entry | S00–S03 | Welcome; Login/session; Scan/enter Handover; authorized preview/claim |
| Care | S04–S11 | Today; Plants/Spaces; Plant Overview; Care Guide; Record Care; Living History; AI Conversation; Team Support |
| Shop | S12–S19 | Shop Home; Search/Filters; Product; Cart; Order Review; Confirmation; My Orders; Order Detail / delivery bridge |
| Membership | S20–S21 | Plantupia Care plans; My Membership |
| Social | S22–S24 | Circle; Create; Inbox & Groups |
| Account | S25–S27 | Profile/Settings; Notifications/Activity; Settings/Privacy |

**Candidate Plant Detail IA (S06, NOT owner-approved yet):**
Authentic plant identity and photo/fallback → actual next check → primary Record care → secondary AI / Team → Overview / Care / History. Latest real activity summarized in Overview; History is its own logical panel, not an endless nested accordion. Permissions/name/archive in contextual overflow menu. Ask Circle is a deliberate public share route, not an extra large Community accordion at page bottom.

**Candidate Shop IA (S12-S19, NOT owner-approved yet):**
Photo-first Shop discovery, product images correspond to actual merchandise, first-screen search and category selection, cart icon, reusable compact cart, order review, no-charge QA confirmation, clear order list accessible via Profile/confirmation/notifications. No fake delivery stages. Future SKU, galleries, variants, live fulfilment and payment are explicitly separate schema/business decisions.

**Candidate memberships (S20-21, NOT owner-approved yet):**
Free / AI Care / Expert Care with discoverable member screen in Profile and contextual upgrades at restricted actions. Existing DemoOffer/Entitlement is time-limited QA access, NOT recurring billing. Preserve provider/account/privacy/quotas and historical ticket read access.

## 4. Existing evidence and observed design gaps

- Owner phone captures on 2026-10-28: nested Shop header/monogram, repeated QA copy, mismatched ceramic planter image, product Add-to-cart CTA partially obscured by 5-tab bottom navigation, cart requiring compact photo/quantity editing, internal order status strings.
- Later Plant Detail screenshot: Check Back, actual Living History observation and two giant bottom accordions (Plant details & settings; Community & sharing) are visually disconnected from the plant's primary care action.
- These are screenshots from one installed QA build, not proof of the state of newer commits.
- Previous generated sample product photos and earlier UI illustrations are *concept-only*; never use them as actual customer specimen or real inventory photographs.

## 5. Existing code boundaries

- **PR #241:** native/Django implementation, real account privacy, handover, care, messages and QA order systems; ongoing source/device QA. Current base of this docs branch taken from its precise commit on 2026-10-28.
- **PR #247:** separate draft, QA ZIP catalog import with feature gate; CI passed in the earlier check, not proof of deployment/media durability.
- **PR #248:** separate draft, screenshot-driven first-pass Shop/Cart/Care styling, NOT final system-wide design.
- **CatalogProduct:** existing single image, concept price/qa stock. Real SKU, variants, gallery and commercial stock require additive design/modeling.
- **DemoOrder:** after no-charge QA checkout, an explicitly authorized operator can prepare a fresh staff-owned InstallationBatch DRAFT from ordered plant lines; this is NOT an approved, delivered or claimed installation.
- **Handover:** recipient-bound issued token, authorized preview, atomic claim. No automatic link from orders.

## 6. Freeze protocol — screen by screen

**PROPOSED → REVIEWED → OWNER APPROVED → TECH READY → CODEX HANDOFF → BUILT → DEVICE ACCEPTED**.

To move a screen into APPROVED, capture: design file/frame ID, what appears in first viewport, primary/secondary actions, exact exit/back flow, server-owned data, roles/privacy, empty/loading/error/offline variants, text-size/keyboard/safe-inset behavior, asset provenance, owner decision and date. TECH READY additionally requires explicit API/contracts and QA criteria.

**No user-visible layout in this v0.1 is marked final.** The aligned principles above are constraints, not visual design sign-off. Codex should not implement this document by itself or blindly cherry-pick PRs #247/#248.

## 7. Open decisions

1. S06 layout: Overview/Care/History tabs vs smaller contextual sections (avoid double Community).
2. Account-level Placement of AI/team/membership and cross-links.
3. Shop exact browse/search/filter content, sticky CTA, variants and gallery data.
4. My Orders access via Profile, confirmation and notifications.
5. Company catalog image rights & object-store durability; 12 generated QA concepts aren't real merchandise.
6. Order lifecycle and future commercial fulfilment, recipient confirmation and refunds policy.
7. Nova Reels/Rooms/Pulse exposure and hierarchy.
8. Handoff between Create (private vs public) and Circle.
9. Global typography, icon set, imagery/crops, touch dimensions and motion.
10. Full QA gates (320/390/430dp/tablet/2x font/IME/device/A-B account tests).

## 8. Implementation-ready gate and source links

Source of truth for current native code: PR #241. Supplemental QA import PR #247 and preliminary layout PR #248 must be rebased/reviewed against the exact final PR #241 commit **only after** design freeze.

No production merge/deploy, backend data mutation, payment integration or real plant addition is authorized by this document.

## 9. How to use the owner's offline design workshop

Open `Plantupia_Interactive_Design_Board_v01.html` from the workshop package locally in Chrome/Edge; click one of the 28 screen IDs. The center shows a **first-viewport concept wireframe**, the right pane shows purpose/data/states/flows, and phone actions preview cross-screen navigation. The 31-page PDF prints all screen contracts beside concept frames and includes the owner's actual screenshots as evidence.

The full structured source is `PLANTUPIA_DESIGN_MASTER_v01.md` in the owner package. **Do not call the JPG concept assets verified Plantupia products.**

### Change record

- v0.1, 2026-10-28: initial comprehensive living map / review board, no implementation freeze.

## 10. Final native technical checkpoint — verified 2026-10-28

**GitHub verified:** draft PR #241 head `9eb039d2d879b43c0e418fe90b8db4580930f45f`. Workflow `40663078245` has `status=completed` and `conclusion=success`; its Android build and Backend tests jobs each concluded successfully.

**Codex delivery report (owner-provided, not a separate independent retest):** 453 backend tests, 450 local Android tests, release lint, 20 architecture guards, completed APK/AAB builds. Reported emulator/device-sized checks: 320/390/430/800dp, 2x text, platform keyboard, private routing, interrupted saves and revocation. Report says evidence gallery contains 65 verified captures; individual files and captures have not all been independently inspected in this design workshop.

**Exact artifact distinction:** GH Actions run `40663078245` produced `plantupia-qa-pr241-apk` (hosted-preview QA), `plantupia-pr241-physical-phone-usb-localhost-apk` (requires connected local backend plus ADB reverse), and `plantupia-pr241-local-emulator-only-apk`. The owner's Codex delivery message links to a Windows-local USB APK, bundle and Markdown report. A Windows `C:\\Users\\...` path is not an accessible public download URL.

**Not completed by successful CI:** physical Samsung acceptance of the *final exact commit*, real Firebase push delivery, live configured AI provider conversation, approved and licensed catalogue photographs, persistent hosted media, payment and commercial fulfilment, and owner approval of redesigned 28 screen states.

**Design baseline decision:** use `9eb039d` for all forthcoming code-to-wireframe gap comparisons. PR #247 remains a separate import draft; PR #248 is a provisional style draft; PR #249 documents design only. No cherry-pick/merge/redeploy until screen decisions and integration tests are explicitly approved.

**Next design review:** S06 Plant Detail (first viewport, care/AI/team positioning, history/Community/Settings) → Today/My Plants → Shop/Cart/Orders/Handover → Membership → Circle/Nova Messaging. Capture exact original-device evidence and approved frame IDs before issuing implementation instructions.

## 11. Owner-authorized implementation wave — 2026-10-28

The owner requested to **begin converting the agreed visual/product decisions to native code**, preserving the original Nova engine and the complete company-authored Care Guide. This authorizes a *reversible design/implementation increment*, not production launch.

All implementation branches start **independently** at Codex's successful `9eb039d2d879b43c0e418fe90b8db4580930f45f`. No branch is merged or deployed by this record.

| Draft PR | Owner decision addressed | Scope and acceptance gate |
| --- | --- | --- |
| #250 | Plant Detail should not be a list of disconnected accordions; approved Care Guide stays intact | Record Care / AI / team / Circle / settings repositioned; guide retains full authored instructions/context/source; latest 2 history entries with full timeline expansion. Kotlin CI, 320/390/430, 2x text, native/back/relaunch/privacy QA **pending**. |
| #251 | Shop should be one browse destination with contextual cart, not three equal top tabs | Catalogue search, persisted cart photos/quantity, separate no-charge Review screen and honest receipts. Backend remains strictly QA-only; no real checkout/fulfillment. Kotlin CI and physical QA **pending**. |
| #252 | Memberships must be discoverable without first hitting an AI paywall | Native Profile entry and current QA entitlement/offers/expiry/limits using existing authenticated `demo/access/` API with affirmative no-charge consent. No recurring billing. Kotlin CI and device QA **pending**. |

These are the **first three of many system-connected increments**, not complete implementation of every 28-state wireframe. Remaining work: Today/My Plants/Welcome/QR physical device pass, connected Shop data schema and real approved photographs, recipient verified order-to-handover fulfillment, final Create/Circle/Inbox/Groups/Profile visual polish, notification FCM verification and full journey acceptance.

**Handoff gate:** do not cherry-pick PR #248 or merge #247, #250–#252 without evaluating conflicts, evidence, accessibility and schema invariants against exactly one current native base. Preserve no-payment QA limitations.
