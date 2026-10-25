## Issue #244 staff import continuation — 25 October 2026

The Django Admin company-side XLSX/CSV import is now implemented on the same
branch. Read [the operator/schema runbook](PLANTUPIA_STAFF_INVENTORY_IMPORT_V1.md)
and [implementation evidence](PLANTUPIA_ISSUE_244_DELIVERY_2026-10-25.md).
It imports only into authorized staff-owned DRAFT batches after read-only preview
and explicit approval. New guides remain unreviewed drafts until a separate staff
publication action; verified plant identity requires independent evidence and
confirmation. Migration 0008 is additive and must not be applied to live data by
this task. No Android rebuild/redesign, Oracle data mutation, merge or deployment.
Next step is supervised disposable-data QA of upload -> preview -> approve ->
source review/publish -> READY -> existing recipient-bound claim -> Care Guide.
Compare the final pushed SHA and CI evidence in the delivered report before QA;
older counts below remain historical. Company botanical approval is still open.

## Implementation continuation — 25 October 2026

Native delivery activation and Care Guide structure are now implemented. Read [the G/H/I integration contract](./PLANTUPIA_PHASE_G_H_I_INTEGRATION_2026-10-25.md) before interpreting the historical baseline and proposed demo below. Implementation code HEAD is `14652d7e` (resolve full SHA in Git); final delivery documentation commits and exact tested CI/artifact links are recorded on draft PR #241 and in the delivery report. Always compare the current remote HEAD before continuing.

Completed code: protected isolated-QA Admin issue/reissue/revoke, exact-manifest checks, revoked-access preview safety, production-disable safeguard, native Welcome/My Space QR/manual entry, encrypted code persistence through authentication/restart, explicit preview/claim and original-space refresh, private version/source/review/placement Care Guide or unverified state. The existing manual add, care, idempotent photo/event submissions, check-ins, outcomes, history and four roots remain the shared product.

New migration: `plants/0007_botanicalspecies_plantunit_verified_species_and_more.py`; new models `BotanicalSpecies`, `CareGuideVersion`, nullable plant `verified_species`. New authenticated route: `GET /api/v1/plantupia/plants/{uuid}/care-guide/`. Native flow uses existing G2 handover POST endpoints. The QA flag stays off by default and is rejected in production. No hosted migration, merge, Play action or deployment.

Local evidence at this continuation: the 77-test plants run passed with three SQLite locking skips; the 16-test focused handover/guide run passed with one PostgreSQL-only skip; 443 Android JVM tests passed with zero failures/skips; all 20 architecture/design/language guards passed. Two more PostgreSQL race cases and the QR codec regression were subsequently added. Exact final-head CI and actual emulator execution must be read from the final delivery evidence, not inferred from these earlier local counts.

**Unresolved core limitation:** there is no secure pre-account or install-to-App-Link redemption mechanism. A first-time customer registers before staff issues/reissues a code to that existing account. Android preserves registration intent but does not override recipient binding. No approved botanical content or commercial maintenance/support operation was supplied. Those are not presented as delivered capabilities.

**Next concrete implementation action:** read final delivery evidence and reproduce the documented local seven-plant fixture, then close any remaining Android runtime matrix cases. Before extending the zero-account delivery scenario, obtain an approved verified-recipient enrollment/channel contract and implement/test that bounded authentication extension; never remove recipient checks. Keep PR draft and production unchanged.

---

# PLANTUPIA — CANONICAL NEXT CHAT / CODEX / CEO DEMO HANDOFF
**2026-10-25 · living handoff — update when Codex completes implementation · Draft PR #241**

## 1. Read this first in every new chat

Repository: https://github.com/omarkhair-labs/nova

Working branch: `feature/plantupia-native-experience-20261012`

Draft PR: https://github.com/omarkhair-labs/nova/pull/241

**Verified pre-Codex baseline as of 25 Oct 2026**: `5f6fce212193247e3bbe14d6d5bb1bce1d0f91e6`. Run https://github.com/omarkhair-labs/nova/actions/runs/40177720935 succeeded: Backend tests and Android build. The QA APK artifact was emitted. **This is NOT proof of an Android QR screen or a complete end-to-end user test.**

This handoff is to preserve exact continuity if the ChatGPT conversation runs out. **First check the current remote PR HEAD and CI; the SHA above is a known baseline, not necessarily the latest HEAD after Codex.** Do not assume Codex ran merely because a task was sent.

## 2. Source of truth (read rather than restart)

1. `docs/PLANTUPIA_CANONICAL_TO_BE_END_TO_END_FLOW_V01_2026-10-25.md`
2. `docs/PLANTUPIA_AS_IS_ANDROID_FULL_USER_FLOW_2026-10-20.md`
3. `docs/PLANTUPIA_AS_IS_VS_CEO_GAP_DECISIONS_2026-10-20.md`
4. `docs/PLANTUPIA_CUSTOMER_JOURNEY_AND_QR_HANDOVER_SERVICE_BLUEPRINT_2026-10-20.md`
5. `docs/PLANTUPIA_G1_STAFF_INSTALLATION_STAGING_2026-10-25.md`
6. `docs/PLANTUPIA_G2_QA_GATED_HANDOVER_API_2026-10-25.md`
7. `docs/PLANTUPIA_PRODUCT_BIBLE_AND_HANDOFF_V01_2026-10-17.md`
8. `docs/PLANTUPIA_SCREEN_CLOSURE_LEDGER.md`
9. Prior research: `omarkhair70-droid/Plantupia` branch `research/foundation-source-atlas-20261007`; competitor studies include Planta, Greg, PictureThis, PlantIn, FYTA, One Big Green, Naava, ORTH, ReNile, Day One, Are.na.

A long, detailed **Codex mission was drafted in ChatGPT** covering Phases A–I (audit, staff handover, Android scan/claim, sourced care guide, end-to-end journey, UX finishing, tests, integration and delivery). The exact chat writing block is not by itself a file in this repo. This handoff records its goals and acceptance checks; confirm Codex actually receives the complete A–I task.

## 3. What is ACTUALLY implemented in the baseline

- Android Native + Django. Four bottom destinations: **My Space, Circle, Create, Me**; contextual People, Inbox, post/detail and Settings; retain relevant Nova social/video/chat/stories infrastructure.
- Existing manual onboarding: create private Space -> add genuine PlantUnit -> inspect -> record actual care/observation/photo -> choose human check-in -> later outcome -> Living History and optionally explicitly consented public Circle question.
- Private permissions: staff/users, owner versus manager/carer/viewer; private spaces invisible to strangers; account and Android test suites.
- G1 backend: staff-owned `InstallationBatch` & `InstallationManifestRow`, per-plant stable UUID and unique source row, Django Admin inventory ready review.
- G2 backend: `HandoverInvitation` with hashed expiring/revocable code, **existing intended recipient account**, explicit ownership mode, authenticated `POST /api/v1/plantupia/handovers/preview/` and `/claim/`, replay and privacy tests. **`PLANTUPIA_HANDOVER_QA_ENABLED=0` by default; no production activation.**
- Last verified CI for baseline succeeded. No claim that current APK includes customer QR/activation UI.

## 4. Current missing items and crucial design pitfalls for Codex

1. **Cold start: scan QR before app install and before account creation.** G2 currently binds invitation to an **existing account** (`intended_recipient` FK). That DOES NOT implement the CEO's zero-account scenario. Design a secure, company-approved identity verification/invitation redemption handoff for new account creation; do not simply remove recipient checking or let possession of QR claim any private space. Pending token must survive install/login safely without exposing inventory.
2. Android Native **Received plants** entry, manual code + functional QR scanner (or truthful unsupported fallback), authorization preview, explicit confirm, account-scoped token resume, success navigation and plant refresh.
3. Botanical guide is NOT approved/implemented: source/version/reviewer; unknown species and unverified guide must be honestly stated. Scheduling an inspection is not automatic watering, and no sensor telemetry exists.
4. A handover must not duplicate plant UUIDs or grant former staff access to a customer-owned private space. Managed-company site role needs contractual confirmation. Parallel issue/claim, retry and revocation require PostgreSQL integration tests.
5. G1 READY manifest can still potentially be modified via other privileged existing administrative model paths; consider a reviewed snapshot / audit before production.
6. Company service visits, 24/7 support, warranties, subscription, IoT, automatic watering are **not confirmed company promises**.
7. Edit Profile / Discover People crash fixes are code-level, not automatically verified on the user's actual Android phone.
8. No real customer data, passwords, tokens, production deploy, PR merge or app replacement without permission. Isolated QA only; keep existing manual route functional.

## 5. CEO demo — exact ideal 10–15 minute script (ONLY IF IMPLEMENTED AND VERIFIED)

**Prerequisites**: dedicated QA server with migration and specifically enabled isolated handover feature; two unrelated QA accounts plus authorized staff, a synthetic named site, **7 distinct test plant UUIDs** with real test labels/placements, safe approved/explicitly unverified care guide examples, generated QR or code, compatible Android QA APK and active API connectivity. Distinguish simulated installation from real customer proof.

1. **Company-side prep (staff on protected admin)**: create or view prepared installation `QA-DELIVERY-001`, Space `Test Office`, 7 plant units + IDs. Show inventory and staff review; ready status; no customer access yet.
2. **Recipient invitation**: choose approved pilot ownership mode, identify recipient or verify new recipient through a safe enrollment mechanism, issue one-time expiring activation invite. Show QR/card or manual code securely (never include token in public screenshots/logs).
3. **Uninstalled-new-user path**: scan QR by phone camera, install/open app, register or sign in, safely resume; if only pre-existing-account flow works, **say that openly** and demonstrate that narrower scope instead.
4. **Preview**: show limited `Test Office · 7 plants`, explicit **Confirm** and **Not my delivery**. Do not expose private inventory before authorization.
5. **Claim**: confirm once, receive server confirmation, open **My Space** with correct actual seven plants and unchanged UUIDs; demonstrate refresh/relaunch and no duplication.
6. **Care guide**: select plant, show placement, identity and source/reviewed guidance **only if genuinely approved**; otherwise explain unknown/unverified state. Do not claim auto-watering.
7. **Real care record**: inspect (e.g., soil is damp -> chose NOT to water), add actual observation/photo, choose next check-in, save. Show server acknowledgement and event history.
8. **Later action**: reschedule/check-in/record honest outcome, open **Living History**, show original dated entries; compare only real dated photos when available.
9. **Optional community**: explicitly preview and consent to a public question; demonstrate that private site/history never appears publicly, without accidentally sharing personal location.
10. **Manual independent route**: a user who never bought Plantupia plants creates own Space, Plant and care event; no company invite or QR required.
11. **Negative security**: wrong account denied, revoked/expired code denied, double click idempotent, network failure no fake success, revoked permissions remain revoked.
12. **Other app surfaces**: Home, Circle, Create, Me, Discover People, Edit Profile, messages and back navigation still function without crashes.

The demo is an acceptance script, NOT an assertion that all of these steps work today.

## 6. What we will genuinely have delivered *after Codex completes all A–I and QA passes*

**A coherent Android customer experience**, not merely QR UI: staff prepares real inventory -> authorized invite -> app entry -> verified account/confirm -> unchanged delivered plants visible -> credible care understanding -> real human care/history/outcomes -> optional consented community. An independent user continues manual add/care. Backend permissions, retries and QA build must be evidenced.

That would meet the CEO's **core product scenario in an isolated demonstrated pilot** *if* signup/QR, guided care and the entire integration are implemented and validated. It would **not alone prove** that Plantupia's actual nursery/order system integrates automatically, their personnel can sustain support visits, their botanist has approved all species care, or the app is deployed and adopted by real customers. Those require company source records, operating decisions and production evidence.

## 7. Exact next-chat instruction

> Continue Plantupia from `docs/PLANTUPIA_CANONICAL_NEXT_CHAT_AND_CEO_DEMO_HANDOFF_2026-10-25.md` on branch `feature/plantupia-native-experience-20261012`, draft PR #241. Inspect the latest HEAD/CI and Codex changes first. Compare implementation with the CEO's 12-step demo script, fix missing account/QR/Android/guide integration and regressions, preserve existing native product, run appropriate CI and prepare a verified QA walkthrough. Don't restart research or invent unapproved company services. Report code, tests, device QA and company-dependent gaps separately.

## 8. Handoff update protocol for Codex / next agent

Before stopping, update this document with exact current HEAD, CI run/artifacts, screen list, API migrations, device QA status, feature flag controls, demo readiness and **first concrete remaining step**. Keep PR draft; do not silently deploy or merge.
