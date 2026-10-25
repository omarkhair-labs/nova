# Plantupia — integrated customer activation, care and QA

25 October 2026 · draft PR #241 · required branch `feature/plantupia-native-experience-20261012`.
This is the current implementation supplement to the canonical flow, Product Bible and screen ledger. Historical G1/G2 and October 20 maps describe their inspection dates. They do not describe the new native increment.

## Implemented product path

```mermaid
flowchart TD
  Staff[Protected Django Admin: exact synthetic inventory review] --> Issue[QA-only issue: explicit recipient and ownership mode]
  Issue --> Code[Native Welcome or My Space: QR or manual opaque code]
  Code --> Auth{Signed in?}
  Auth -- No --> Pending[Encrypted short-lived pending intent]
  Pending --> Login[Existing login or registration]
  Login --> Review[Review delivery using authenticated preview]
  Auth -- Yes --> Review
  Review --> Confirm[Explicit confirm: transactional claim]
  Confirm --> Connected[Server-confirmed result]
  Connected --> Refresh[Refresh authorized inventory and select original space]
  Own[Independent owner: existing create space and add plant] --> Refresh
  Refresh --> Detail[Existing native Plant Detail]
  Detail --> Guide{Published guide for verified botanical identity?}
  Guide -- Yes --> Sourced[Version, source, reviewer, review date, recorded placement]
  Guide -- No --> Unknown[Care guidance not yet verified]
  Sourced --> Care[Existing private observation or actual care]
  Unknown --> Care
  Care --> Date[Optional human-selected check-in]
  Date --> Outcome[Return and record actual outcome]
  Outcome --> History[Same plant UUID: Living History and linked follow-up]
  Detail --> Public[Optional separate Circle preview and explicit consent]
  Review --> Recover[Wrong account / expiry / revocation / offline / disabled: honest recovery]
```

The four existing roots remain **My Space / Circle / Create / Me**. People, inbox, photos, Stories, Reels and messaging retain their existing routes and engines. Activation introduces no new root tab. Both customer paths reuse the existing care, idempotent event submissions, photo journal, follow-up, outcome and history code.

## Actual changes and boundaries

- Native `PlantupiaHandoverScreen`: one full-screen native flow with code, preview, explicit confirm and connected states. Welcome and signed-in My Space expose the contextual entry. Back closes it without claiming. “Not my delivery” removes the pending secret.
- QR uses JourneyApps ZXing Android Embedded 4.3.0 with the Activity Result `ScanContract`, QR format only, no saved barcode image or audible feedback. It decodes locally without a Google Play module download, supports the current minSdk 26, and uses existing camera permission. Manual entry remains available on denial, cancellation or camera failure. [Maintainer documentation](https://github.com/journeyapps/zxing-android-embedded#usage-with-scancontract).
- Pending codes use Android Keystore AES-GCM and AtomicFile in `noBackupFilesDir`, expire locally after 24 hours, and bind to the first authenticated account. Bound codes cannot resume on another account or while signed out. No token in URLs, saved instance state, logs, analytics, public payloads or permanent admin fields. Code UI uses secure-window protection and a password field. A process restart requires a fresh preview and explicit confirmation; it never auto-claims.
- Same-account confirmed retry returns the original space. An uncertain/offline response retains the code and requires retry; no locally fabricated inventory or success. Account/login generation guards reject stale callbacks. Successful activation refreshes server-authorized data and focuses the original space, including after a failed initial refresh is retried.
- Protected Django Admin issues an invitation once in a `no-store` response, explicitly requires recipient, QA ownership mode and QA acknowledgment, and exposes non-secret status/revocation. Reissue revokes unused predecessors. Claim/issue reject a space containing plants outside the reviewed manifest. Claimed previews recheck current access. Handover is now also refused when Django identifies the environment as production, even if the QA flag is mistakenly set.
- `BotanicalSpecies` is a curated identity, separate from user-entered `species_label`. Nullable `PlantUnit.verified_species` represents unknown identity. `CareGuideVersion` stores species/version, source URL/reference, instructions, contextual notes, staff reviewer, actual review date and publication state. Published content edits require a new version through Admin; a publication can be withdrawn. No actual botanical content is seeded or approved by this mission.
- The private Care Guide panel lives in the existing Plant Detail view. It displays the latest published version for the curated identity, sources/review provenance and recorded placement, or an accurate unverified/unavailable/loading/retry state. Reading it never records care. No inferred environmental measurements, health score, diagnosis, recurring watering schedule or invented maintenance role.
- Local QA can compile a loopback/emulator-only API override into the `.qa` variant with `-PplantupiaQaApiUrl=http://10.0.2.2:8765/api/v1/`. It cannot modify release/debug production routing or target an arbitrary hosted server. Without the property the existing QA preview origin is preserved. QA and production package identities remain unchanged.

## Backend inventory

Existing preserved models: `Space`, `SpaceMember`, `PlantUnit`, `PlantNickname`, `PlantEvent`, `PlantDiscussion`, `PlantSubmission`, `InstallationBatch`, `InstallationManifestRow`, `HandoverInvitation`.

New models: `BotanicalSpecies`, `CareGuideVersion`. New nullable plant FK: `verified_species`.

New schema: `plants/0007_botanicalspecies_plantunit_verified_species_and_more.py`. This is additive. No migrations are applied to a hosted or production database. Rollout needs isolated environment/schema verification and an approved deployment.

Authenticated routes under `/api/v1/plantupia/`:

| Endpoint | Purpose |
| --- | --- |
| `POST handovers/preview/` | Existing G2 recipient-bound minimal summary; body `invitation` |
| `POST handovers/claim/` | Existing G2 atomic claim; body `invitation`, `confirm: true` |
| `GET plants/{uuid}/care-guide/` | New private guide/provenance/placement or explicit `unverified` state |
| `GET/POST spaces/`, `GET/POST plants/` | Existing shared manual entry and authorized inventory |
| `POST plants/{uuid}/event-submissions/` | Existing immutable account/plant/submission care and outcome identity |
| `GET plants/{uuid}/events/` | Existing Living History and `pending_followups=1` |

Staff-only Admin route: `/admin/plants/handoverinvitation/issue/`; no public invitation issuance endpoint. Revocation is an Admin action with the same batch-before-invitation lock order as claim and reissue.

## Demonstrate with synthetic data only

1. Use a fresh disposable **local SQLite** database and filesystem media. Set `DJANGO_ENV=development`, `DATABASE_URL` to that new local SQLite path, `DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1,10.0.2.2`, and `PLANTUPIA_HANDOVER_QA_ENABLED=1`. Never use production credentials/storage.
2. From `backend/`, install existing requirements, run `python manage.py migrate`, then `python manage.py seed_local_plantupia_qa --credentials-file <new-private-local-file-outside-repo>`. The command refuses production, PostgreSQL, disabled handover, existing users/spaces and an existing credentials file. It creates only explicitly synthetic staff/customer/stranger/independent accounts and seven distinct original plant UUIDs. Credentials and the short-lived code go only to the requested local file.
3. Run this backend on `127.0.0.1:8765`. Build/install `.qa` with the loopback property above for the emulator. Firebase config can use the existing CI dummy configuration; it does not prove push delivery. Standard CI QA APKs retain the preexisting QA preview address, whose deployment/schema/isolation are not established by this build.
4. Open **Connect my Plantupia plants** at Welcome. Enter the private local code or a QR encoding exactly that raw opaque code. URLs/App Links are intentionally not accepted. Scan is not claim.
5. Continue to existing login with the synthetic intended customer. The pending code resumes. Select **Review my delivery**; confirm the synthetic space and seven-plant count. Select **Confirm and connect my plants**, wait for server confirmation, then **Open My Space**.
6. Confirm `Synthetic plant 01` through `07`, original UUIDs and placement, with no duplicate rows. Open one. Open Care Guide: unknown identity correctly shows unverified guidance. A source-backed guide can be tested only with clearly synthetic test content or genuinely approved content entered by an authorized reviewer; do not invent biological advice.
7. Use the existing care form to record an observation, choose a future check-in and save. Confirm the actual record in Living History. Open its pending check-in, record an outcome, and verify the parent link and completed state. Viewing a guide and opening the form must not create records.
8. Reopen the same delivery with the same account; explicitly confirm and reopen. Verify seven original plants still exist. Repeat on the synthetic stranger account: preview/claim/private guide/history must fail without revealing the space/inventory. Test expiry, revoke and reissue via the isolated staff Admin.
9. Test an independent user without a code: existing registration/login → Create → private space → add plant → observation/care → check-in → outcome → Living History. No purchase or company service is required.
10. Test offline/online recovery, Back, rotation, process restart, logout/switch, camera denial, narrow screen and enlarged text. Open all four roots, People and inbox. Media playback, real push and remote storage require their own actual-media runtime cases; a code test is not acceptance of those capabilities.

## QA evidence and honest acceptance boundary

The actual emulator walkthrough reached all seven native inventory labels, unverified and explicitly synthetic published-guide states, a server-saved care record, a human-chosen check-in, app restart, outcome and linked Living History. Circle and Me also opened. A subsequent Edit Profile smoke test exposed pre-existing invalid `MNaN,NaN` vector backgrounds in two avatar hero assets and two art-lab vectors. Their original SVG rectangle omitted x/y (SVG defaults to zero); the four backgrounds now use `M0,0`, preserving the original artwork. A regression using the actual Compose path parser was observed failing before correction; a native test loads all sixteen hero/micro portraits. Final rerun evidence and remaining navigation/media cases belong in the delivery report, not a blanket device-acceptance claim.

PostgreSQL simultaneous same-account claim testing also exposed a joined `FOR UPDATE` result disappearing while another transaction changed the space owner. Issuance/claim now lock the base batch/space rows separately and invitation locking targets its base row. All three PostgreSQL claim-race cases passed in the subsequent full backend CI. Final source changes still require their own complete relevant CI.

Automated additions: handover parser/intent/account/expiry/error tests; real Android Keystore and signed-out login/registration continuation instrumentation; backend seven-identity claim→guide→care→lost-ack retry→follow-up→outcome→private history test; exact-manifest/production-disable/revoked-preview/Admin tests; guide publication/version/source/reviewer/authorization tests; simultaneous claim test using real row locks on PostgreSQL CI (intentionally skipped on SQLite).

Final command results, exact tested SHA, CI run links, downloadable APK and any actual emulator walkthrough are recorded in the delivery report and updated draft PR. Never infer final-head CI success from the previous G2 run or an earlier implementation commit. Instrumentation compilation and instrumentation execution are different evidence.

## Company approvals / unresolved release work

- Actual delivery manifest process, recipient verification/channel, ownership sale vs managed policy and staff access. The two G2 modes remain simulated QA choices.
- G2 targets **existing accounts**. A first-time customer must register before staff issues/reissues their account-bound code. Pre-account email invitation and install-to-App-Link routing are not implemented. The Android flow preserves registration intent but never bypasses a different intended recipient.
- Approved botanical identities, reviewers, content/source rights and local care guidance. Staff permission is technical authority, not proof of botanical credentials. No expert badge or substantive plant advice is fabricated.
- Approved HTTPS/App Links domain, customer distribution, independent hosted QA DB/media isolation, migrations, device/media/camera testing and final visual/accessibility acceptance.
- No contracted support, maintenance bookings, subscription, warranty, technician assignment, SLA, IoT, automatic watering or biological replacement workflow is offered. Those remain proposed/company-gated operations.
- The staging manifest is protected by the handover service and Admin, but upstream privileged direct ORM/DB amendments are not a complete immutable commercial inventory audit trail. Reviewed inventory amendments and staff-scoped organization permissions remain a rollout gate.
- Local pending codes are lost on uninstall/data clearing and cannot outlive server expiry. A bound intent is not available to another login account. There is no offline claim or automatic queued confirmation.
- Production, master, release signing and Google Play remain unaffected. PR stays **draft** and must not merge or deploy without explicit approval.
