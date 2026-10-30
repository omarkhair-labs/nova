**Later implementation:** [25 October integration and QA contract](./PLANTUPIA_PHASE_G_H_I_INTEGRATION_2026-10-25.md) adds protected QA Admin issue/reissue/revoke, native QR/manual login-resume/preview/confirm/original-space navigation and Care Guide structure. Claimed previews now recheck current access, extra unmanifested plants block claim and Django production refuses the QA flag. The backend-only G2 snapshot below is historical; no hosted rollout or company approval is implied.

# Plantupia G2 — QA-gated secure delivery handover (backend)

**25 Oct 2026. Draft PR #241, isolated engineering prototype, NOT deployed and NOT production-authorized.**

## What's implemented in source

1. `InstallationBatch` G1 can move **DRAFT → READY** after staff inventory review, then **READY → CLAIMED** through G2.
2. `HandoverInvitation` binds **one reviewed batch**, **one existing intended account**, **one explicitly selected ownership mode**, issuing staff identity, expiration, revocation and claimed timestamp. Only `SHA-256(random 256-bit secret)` is stored. Never log, embed in static docs or store the raw code.
3. `issue_pilot_invitation(...)` is a controlled internal Python service method for an approved staff actor in QA; it yields the secret once for out-of-band delivery. There is **no public token-creation API or QR drawing service**.
4. Two **authenticated POST** API endpoints use the raw code in request JSON body (not URL query params):
   - `POST /api/v1/plantupia/handovers/preview/` with `{"invitation":"<opaque secret>"}`: *only intended signed-in recipient* sees minimal site label, exact plant count, approved ownership mode.
   - `POST /api/v1/plantupia/handovers/claim/` with `{"invitation":"<opaque secret>","confirm":true}`: explicit authorization, transactional role/owner update, unchanged plant identities, consumed token and `CLAIMED` state.
5. Both endpoints and issuance **return 503 by default**. They are active only when `PLANTUPIA_HANDOVER_QA_ENABLED=1` is **explicitly set in a dedicated non-production QA environment**, after its real accounts and ownership rules have been reviewed. Do NOT enable the flag in existing production Oracle deployment.
6. Repeated valid confirmation **for the same already-authorized account** returns its original space; another account is denied; expired/revoked/reissued invitations are rejected. Preview and claim enforce recipient binding and account-scoped attempt limits.
7. On **customer-owned sale** mode, the prepared staff-owned `Space.owner` changes to the intended customer and all prior membership assignments are dropped, preventing accidental continued staff access. On **company-maintained** mode, the staff owner remains and that customer receives a `SpaceMember.CARER` role. No permission is inferred from public social relationships.

## Security and operational boundaries

- Customer-owned vs company-managed is **a pilot switch, not proof of the company's contractual ownership policy**. Company must approve which mode applies for any real handover.
- This is a backend contract **only**: no Android scanner, deep link, install/resume flow or downloadable new QR APK has been implemented. That is G2 Android work.
- G1 staging must be executed by authorized staff with truthful plant records and verifiable source keys; don't invent customer installations or botanical advice.
- Tokens are delivered through an explicitly authorized secure channel. Never use raw secrets in URLs that may be logged by analytics or third parties, GitHub files, screenshots or crash reports. A future Android QR implementation must decide how to safely pass the opaque code without auto-claiming.
- Preview gives the intended recipient only a compact summary; no private inventory is exposed to any stranger. One invitation cannot grant ownership on scanning; explicit authenticated confirmation is required.
- Staff provisioning and *after-review modifications to the underlying Space/Plant admin records* need a further immutable manifest snapshot/audit gate before the production launch. Transactional locking currently protects the claim itself.
- Each API is rate-limited per signed-in account via a best-effort Django cache counter, not a substitute for ingress/API WAF throttling.
- Empty or invalid secrets, no authentication, intended-account mismatches, repeat and stale credentials must fail safely without leaking full inventory.
- No app ownership, real paid services, real botanist validation, or customer migration is implied by feature-complete tests.

## Acceptance checks — backend QA

- Off by default (issuance, preview, claim).
- Authentication and intended-recipient binding; stranger sees 404 without private labels.
- Seven existing `PlantUnit` IDs are unchanged from source manifest through activation, none duplicated.
- Customer-owned branch transfers access; former staff cannot see the customer's private inventory.
- Managed branch keeps staff ownership but gives limited `carer` permission; removing role prevents idempotent replay from regaining access.
- Expired, revoked, superseded invitation and missing explicit `confirm` cannot change membership.
- Validate Django migration consistency and full Android + Django regression suite on PR, followed by isolated backend device QA.
- Before any real rollout, test two simultaneous claim transactions on PostgreSQL and delivery change/amendment audits.

## Next gated delivery

**G2 Android**: first-run "Received plants" versus "Add my own plants"; in-app code capture/scanner; token retained only in private account-scoped memory/short-lived secure state until explicit confirmation; API preview → user-confirmed claim → refresh My Space. Never show a fake inventory, and never use a QR token as a permanent per-plant identifier.

**G3 care**: company-reviewed plant guidance version, botanical source/citation, placement-specific checklist and unknown-species state.

Reference: [G1](./PLANTUPIA_G1_STAFF_INSTALLATION_STAGING_2026-10-25.md), [Canonical TO-BE](./PLANTUPIA_CANONICAL_TO_BE_END_TO_END_FLOW_V01_2026-10-25.md).
