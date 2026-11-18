# Issue #246 — connected CEO QA journey

27 October 2026. Existing `feature/plantupia-native-experience-20261012`, draft
PR #241. This extends the original Android app, Django backend and `/company/`.
It preserves Issue #244 and follows #245's company-first inspection requirement.
Historical roadmap statements that subscriptions/support/shop are deferred describe
their earlier inspection dates; this explicitly authorized **QA-only** slice adds
them without introducing real billing, delivery or company service promises.

## What is connected

1. Ordinary company operator creates an installation and imports the existing
   destination-bound XLSX. Source-linked care versions remain unpublished. A separate
   permitted reviewer explicitly publishes the exact content.
2. Operator prepares a plan for a **verified plant and its published guide**, with
   an inspection action, every-N-days cadence (7 = weekly), start date, IANA timezone,
   placement context and instructions. The actual source reviewer can review the
   proposal while inventory is staff-owned. Approval is separate from creation and
   customer opt-in. Approved contents are immutable; retirement preserves history.
3. Existing exact-manifest READY review and recipient-bound one-time QR/manual code
   issue are reused. Native authenticated preview/confirm claims the original UUIDs.
4. My Space/plant detail show approved inspection proposals. A carer opts in or
   declines, records an actual observation, skips or chooses another date. Completion
   creates an ordinary private `PlantEvent`, visible in existing Living History.
   Skip/reschedule/decline append plan decisions without inventing an act of care.
   Today reads persisted next occurrences; relaunch does not recreate tasks.
5. Ask AI opens configurable concept plans before sending any question. QA activation
   stores an audited, expiring entitlement without payment. Real inference uses the
   existing provider adapters only after object access, entitlement, per-request
   context consent, key/configuration and Redis checks. Unconfigured service is
   explicitly unavailable; no synthetic model response is substituted.
6. Ask Plantupia team requires QA Expert Care access. Customer explicitly shares the
   typed request and plant label, optionally its recorded placement. A separate staff
   account claims the real queue entry, types a real reply and the customer refreshes
   the actual persisted thread. This is separate from Circle and ordinary social DMs.
7. Contextual Shop opens the small fictional QA catalog, item/quantity, persisted
   cart, reviewed no-charge confirmation and My Orders. The company queue sees the
   same order. Explicit staff confirmation can prepare a **fresh DRAFT**, never
   automatic verification, delivery or handover.

The four Android roots remain My Space / Circle / Create / Me. No second consumer
app, fifth root, separate domain database or replacement messaging engine was added.
The `.qa` build is the existing side-by-side testing variant, not another product.

## Actual contracts and boundaries

| Capability | Contract |
| --- | --- |
| Inspection | Per actual plant; no unverified species inference; guide must be published and match verified identity. Withdrawal/identity change/retirement pauses actions. Date recurrence uses the plan's local calendar day, with no flood of missed tasks. |
| Responsibility | One opt-in enrollment per plan + authenticated carer, with revision/next date. Viewer cannot record. No automatic local notification is activated; existing human check-in/WorkManager behavior remains intact. |
| QA access | `DemoOffer` stores provisional EGP monthly concept price, bounded AI/support quota and 1–72-hour test duration. Defaults are AI Care 79 and Expert Care 199, **concept only**. Explicit activation/revocation/expiry are persisted. No payment receipt, subscription billing or free production unlock exists. |
| AI | Actual existing OpenAI/Groq adapters; 1,000-character question, 8 bounded turns, 850 output tokens, offer limit capped at 35/account/day and 200 server-wide/day by default. A provider project spending limit is still necessary. Redis is required even in local inference mode. No autonomous writes to care/history. |
| Support privacy | Only the customer's shared snapshot/text is available. Staff never gain current customer-owned plant photos/history/Space access from a ticket. Sender/time/status are genuine persisted records. No fabricated names, presence or response SLA. |
| Messaging reuse | A `ServiceRequest` owns a real Nova `Conversation`/`Message` stream, with no generic DM participants/group memberships. Dedicated ticket authorization controls it. It cannot appear in or be written through ordinary Inbox/DM/group routes to bypass entitlement. Existing social messaging remains unchanged. |
| Expiry | New AI/team writes need current access. Customer can still read their already-shared ticket and real replies after expiry; staff can finish an existing request. New requests/customer follow-ups are blocked. |
| Orders | Server decimal prices and name/kind snapshots, positive bounded quantities, row-locked test stock, actor-bound immutable receipt and exact cart/price fingerprint. No shipping/tracking, payment integration or stock guarantee. Order is not a delivered plant. |
| Retry | Server actor + UUID receipts serialize transactions. Native app-private AtomicFile intents are immutable and scoped by backend origin + account, outside backups. Pending actions are retried explicitly from My Space; no automatic background purchase/support send. |
| Media | Optional catalog upload reuses the existing sanitizer; active catalog photos load through authenticated bounded reads. No generated product photos are seeded. Support text/label/optional placement are the bounded sharing interface; ticket photo uploads and history attachment selection are not included. |

All new native APIs and company workspaces require
`PLANTUPIA_CEO_DEMO_ENABLED=1` **and** the existing restricted company-dashboard gate.
Default is OFF. Development accepts SQLite or loopback PostgreSQL; hosted access
requires the existing narrowly constrained, separately approved QA guard. A generic
production flag or remote development database cannot turn this on. Nothing here
deploys to or seeds the existing Oracle Preview #241.

## Disposable local setup — one database for app and dashboard

Use a fresh private folder outside Git and Python 3.12. From `backend/`:

```powershell
$ceoQaRoot = Join-Path $env:LOCALAPPDATA ('PlantupiaCEOQA/' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $ceoQaRoot | Out-Null
$env:DJANGO_ENV = 'development'
$env:DJANGO_DEBUG = '1'
$env:DJANGO_SECRET_KEY = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$env:DATABASE_URL = 'sqlite:///' + (Join-Path $ceoQaRoot 'qa.sqlite3').Replace([char]92,[char]47)
$env:DJANGO_ALLOWED_HOSTS = 'localhost,127.0.0.1,10.0.2.2'
$env:REDIS_URL = ''
$env:AWS_ENDPOINT_URL = ''
$env:AWS_ACCESS_KEY_ID = ''
$env:AWS_SECRET_ACCESS_KEY = ''
$env:AWS_S3_BUCKET_NAME = ''
$env:FIREBASE_SERVICE_ACCOUNT_JSON = ''
$env:PLANTUPIA_AI_ENABLED = '0'
$env:PLANTUPIA_AI_API_KEY = ''
$env:PLANTUPIA_COMPANY_DASHBOARD_ENABLED = '1'
$env:PLANTUPIA_HANDOVER_QA_ENABLED = '1'
$env:PLANTUPIA_CEO_DEMO_ENABLED = '1'
$env:PLANTUPIA_LOCAL_QA_MEDIA_ROOT = Join-Path $ceoQaRoot 'media'
python -m pip install -r requirements.txt
python manage.py migrate
python manage.py collectstatic --noinput
python manage.py seed_local_plantupia_ceo_qa --credentials-file (Join-Path $ceoQaRoot 'private-credentials.json')
python manage.py runserver 127.0.0.1:8766
```

The command composes the guarded existing company fixture: empty local SQLite only,
ordinary operator/reviewer/customer/stranger accounts and no superuser. It adds two
concept offers, three **FICTIONAL QA** products, an empty CEO installation and a
destination-bound `fictional-ceo-inventory.xlsx` beside the private credentials.
That workbook reuses the three existing RHS-linked synthetic examples, explicitly
not Plantupia-approved. Nothing is imported, published, enrolled, entitled, ordered
or handed over by this new command. Passwords are random and never printed.

Open `http://127.0.0.1:8766/company/login/` using the **operator email**. Use the new
CEO installation and its workbook, or create a new installation and populate its
fresh template with the same fictional rows. Do not reuse a destination-bound file
for a different batch/Space. The strict #244 import schema remains unchanged:
inspection plans are separate reviewed forms, not silently inferred spreadsheet data.

After import, sign in as reviewer to inspect/publish source versions. As operator,
prepare daily leaf and every-three-day soil **inspection** proposals. Reviewer
approves those exact proposals. Operator reviews READY and issues to the exact
fixture customer username with the explicit QA ownership policy. Keep QR/code private.

## Android rehearsal and installable artifacts

Use the exact successful final-head CI artifact
`plantupia-pr241-physical-phone-usb-localhost-apk`, which targets device localhost
8765. With this local server on 8766, connect a phone or owned emulator by ADB:

```powershell
adb devices
adb reverse tcp:8765 tcp:8766
adb install -r <extracted-USB-QA-APK>
```

Open the existing QA app. Sign in as fixture customer, enter/scan the one-time code,
preview and confirm. Verify the original identities, source/version/reviewer,
inspection opt-in/completion/reschedule, then Living History and relaunch. Test AI
paywall → explicit no-charge demo access → honest unconfigured fallback; upgrade
to QA Expert Care, explicitly share a request, then reply from the separate staff
browser session and refresh the customer thread. Shop → quantity → cart → explicit
no-charge order → My Orders → reload → company order queue. Wrong account sees none
of these private records. Revoke QA access in `/company/demo-access/`; new AI/team
writes must return to the paywall. Core care, past shared requests and catalog remain.

Do not use the hosted-QA artifact or emulator-only 10.0.2.2 artifact for a physical
phone. USB/ADB and the local server must remain connected; this is not a wireless
or hosted deployment. Artifact retention is seven days. The old phone helper seeds
a different fixture; use these commands for the new connected dashboard rehearsal.

For explicitly opted-in local instrumentation, build with
`-PplantupiaQaInstrumentation=true -PplantupiaQaApiUrl=http://127.0.0.1:8766/api/v1/`
and target the matching QA instrumentation APK. The new real-backend journey test
refuses a production package/origin and requires a private `files/ceo-local-qa.json`
fixture in the QA app. Run stage 1, reply in the actual staff browser, then stage 2.
Never commit, print or include that credential/invitation file in screenshots.
Ordinary CI only builds instrumentation; no device-run claim follows from that.

## Actual inference configuration and remaining gates

No provider key is invented or embedded. Keep the assistant unavailable until the
owner securely configures a QA-only key, supported model, `PLANTUPIA_AI_ENABLED=1`
and QA Redis in that server environment, following [the existing provider setup](PLANTUPIA_FREE_AI_SETUP.md).
The current entitlement/configuration checks precede any real inference request.
Use a synthetic nonprivate question for the first real call. No fake answer or
fabricated paid transaction is part of the demonstration. Configuration is not
equivalent to a verified live response; final evidence must distinguish them.

Real billing/Play policy implementation, commercial prices/catalog/rights,
qualified botanical approval, staffed service operations, richer recurrence,
support photo attachments, push delivery and formal accessibility acceptance remain
separate work. Current QA does not authorize production, real customers, Oracle
data changes, Play publishing, PR merge or a hosted rollout. Keep PR #241 draft.
Final commit/CI IDs, device/browser evidence and exact known gaps belong in the
delivery report; this source document is not owner acceptance.
