# Plantupia Company Dashboard v1

Issue #244's standalone-dashboard clarification supersedes the earlier Admin-only
delivery boundary. **The independent staff portal is implemented at `/company/`.**
The existing importer and Django Admin fallback are preserved. Issue #244 remains
open for final verification and owner acceptance; this document does not close it.
PR #241 remains draft. No deployment or live Oracle operation is part of this work.

## Product and architecture

The dashboard is a separately branded web interface with its own login, navigation,
templates, responsive styles and progressive enhancement. It is served by the
existing Django process on the same origin: no extra frontend service, CORS token
storage, cross-origin auth or production topology change. Django sessions and CSRF
protect all forms. Static CSS/JS use the existing collectstatic/WhiteNoise pipeline;
CI now validates asset collection. All primary workflows stay within `/company/`
and require no visit to Django Admin.

| Workspace | Implemented persisted behavior |
| --- | --- |
| Staff access | Email/password login through existing account authentication, active staff + explicit company-access permission, bounded login attempts, POST/CSRF logout |
| Overview | Authorized Draft/Ready/Claimed installation counts, actual preparation and source-review queues, recent nonsecret invitation states |
| Installations | Scoped list/search/filter/pagination, create staff-owned Space + DRAFT batch atomically, draft metadata edit, exact manifest and plant context, import history, state-bound explicit READY review |
| Plant inventory | Authorized staff-owned plant cards/search/pagination; draft manual add/edit with source-key and UUID preservation; species-label correction clears obsolete verification; links from verified plants to the latest published guide |
| Import workspace | Existing destination-bound XLSX/CSV templates, read-only per-row diff/errors/warnings, separate identity confirmation, signed expiring approval, atomic/idempotent service and receipt history |
| Botanical library | Species creation, versioned sourced drafts, assignment to permitted active reviewers, actual reviewer/date on explicit state-bound publish, withdrawal and immutable published history |
| Handovers | Exact existing non-staff recipient username, explicit isolated-QA ownership policy, issue/reissue/revoke through existing services, locally rendered opaque-code SVG QR, manual reveal and nonsecret register |

The forest/cream/sage palette follows the existing conversion contract. Typography
uses local system serif/sans fallbacks; no proprietary assets or unlicensed font
files were imported. The typographic wordmark is provisional, not a claim of final
company logo approval. Desktop/tablet/mobile layouts include role-aware navigation,
semantic labels/headings, skip link, visible keyboard focus, empty/error/success
states, and native form submission with a visible pending state. No fabricated
health metrics, customer records, photos or commercial service promises are shown.

## Safety and permissions

`PLANTUPIA_COMPANY_DASHBOARD_ENABLED` defaults **OFF**. All company URLs, including
login, return 404 while disabled. Enabling requires an explicit opt-in and either
development with SQLite/loopback PostgreSQL or the existing narrowly constrained hosted
QA handover guard. A generic production environment cannot enable it with one flag.
An unverified remote database is refused even when someone labels its settings
development; the CI PostgreSQL loopback and disposable SQLite fixture are allowed.
Nothing in this branch enables the flag on Oracle or exposes new public writes.

All portal sessions require `is_active`, `is_staff`,
`plants.access_company_dashboard` and `plants.view_installationbatch`.
Every workspace/action additionally checks its specific model/import/publication
permissions on the server. Customers and permissionless staff cannot access it.
Non-superuser accounts with company-dashboard access are also refused entry by the
technical Admin site/login, preventing inherited unscoped ModelAdmin pages from
bypassing company record scoping. Existing technical staff without this new company
permission and explicit superusers retain their previous Admin compatibility.

`plants/company_roles.py` defines two QA presets, installed only by the guarded
local fixture command:

- **Operator:** prepare own installations/plants, import, curate/assign drafts,
  review inventory and perform authorized QA handover operations. No guide-publish,
  account-administration, permission-administration or superuser grant.
- **Care reviewer:** read the care library, create/edit draft versions and approve
  source content. No import or invitation issuance. An assigned version can be
  published only by its assigned reviewer or an explicit technical superuser.

Ordinary staff may manage only their own prepared, staff-owned installations.
After customer-owned claim, they retain a limited nonsecret delivery audit:
reference, original manifest count and permitted invitation state. Current private
Space/plant details, photos, imported evidence and care history are withheld.
The inventory endpoint excludes customer-owned spaces. Company-managed access
continues to follow the existing ownership policy. No organization model or
implicit employee/customer relationship is fabricated.

READY and guide approvals bind the staff actor, exact record/content/state and a
30-minute signed review token. Concurrent changes require fresh review. Batch then
Space/plant lock order matches the importer/handover services. Publication retains
species-before-guide locking. Invitation confirmation also binds current invitation
state: a duplicate submit cannot issue another code; reissue requires an explicit
checkbox and revokes unused predecessors. PostgreSQL tests exercise concurrent
confirmation. Claimed invitations cannot be revoked by this workflow.

Secret responses are private/no-store, never put raw codes into URLs, persistent
sessions, database fields, audit messages or third-party QR services. SVG generation
uses qrcode locally from the random ASCII token only. The manual code is masked
until explicitly revealed. Leaving clears the secret DOM; a restored back/forward
cache page redirects to the nonsecret register. The restrictive same-origin CSP,
same-origin referrer policy and frame blocking apply to portal responses. Audit
entries use object IDs and generic action text, not spreadsheet payloads or labels.

## Disposable local rehearsal

Do not use the existing Oracle QA database or its prepared installation. Start
with a **fresh local SQLite database**, separate media and no remote service keys.
From `backend/`, in a dedicated virtual environment:

```powershell
# Choose a NEW, private, empty local directory outside the repository.
$companyQaRoot = Join-Path $env:LOCALAPPDATA ('PlantupiaCompanyQA/' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $companyQaRoot | Out-Null
$env:DJANGO_ENV = 'development'
$env:DJANGO_DEBUG = '1'
$env:DJANGO_SECRET_KEY = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$env:DATABASE_URL = 'sqlite:///' + (Join-Path $companyQaRoot 'qa.sqlite3').Replace([char]92,[char]47)
$env:DJANGO_ALLOWED_HOSTS = 'localhost,127.0.0.1'
$env:REDIS_URL = ''
$env:AWS_ENDPOINT_URL = ''
$env:AWS_ACCESS_KEY_ID = ''
$env:AWS_SECRET_ACCESS_KEY = ''
$env:AWS_S3_BUCKET_NAME = ''
$env:FIREBASE_SERVICE_ACCOUNT_JSON = ''
$env:PLANTUPIA_AI_ENABLED = '0'
$env:PLANTUPIA_COMPANY_DASHBOARD_ENABLED = '1'
$env:PLANTUPIA_HANDOVER_QA_ENABLED = '1'
$env:PLANTUPIA_LOCAL_QA_MEDIA_ROOT = Join-Path $companyQaRoot 'media'
python -m pip install -r requirements.txt
python manage.py migrate
python manage.py collectstatic --noinput
python manage.py seed_local_plantupia_company_qa --credentials-file (Join-Path $companyQaRoot 'private-credentials.json')
python manage.py runserver 127.0.0.1:8766
```

The fixture command refuses non-development, non-SQLite, disabled flags, existing
users/spaces/batches, an existing credential file or an output path inside the
repository. It creates ordinary **non-superuser** operator/reviewer accounts,
recipient/stranger accounts, three explicitly fictional installations (two DRAFT,
one genuinely reviewed READY), six synthetic units and two unapproved source drafts.
Passwords are random and written only to the new private credentials file; output
prints its path, not its contents. No raw invitation is seeded. A failed seed rolls
back and removes its partial credential file.

1. Open `http://127.0.0.1:8766/company/login/`. Use the fixture **operator email**
   and password. Check real counts, queues and empty handover state.
2. Prepare a **new DRAFT installation** through the portal. Download its template.
   Use fictional QA content only; test formula/duplicate/destination errors.
3. Upload, review the side-by-side values and warnings, then explicitly approve.
   Verify stable source keys/UUIDs and unpublished guide drafts. Repeat import to
   prove no duplicates. Full schema/caps remain in the preserved
   [importer runbook](PLANTUPIA_STAFF_INVENTORY_IMPORT_V1.md).
4. In the care library, assign the draft to the fixture reviewer. The operator
   cannot publish. Sign out, sign in as reviewer, examine actual source/content,
   then explicitly confirm publication. Synthetic publication tests code only;
   it is not actual Plantupia botanical sign-off.
5. Return as operator. Review the exact inventory and mark READY. Confirm import
   and editing are locked. Issue to the exact fixture recipient username using
   the explicit QA policy/mode. Keep raw code/QR private. Test reissue and revoke.
6. With a compatible local Android QA build, or the existing authenticated API,
   preview and claim as recipient. Original UUIDs must remain. Verified/sourced
   guidance appears; unknown identities remain unverified. Former staff/stranger
   private guide/history requests must be denied. A staff delivery audit remains
   nonsecret and does not expose the customer's private living record.
7. Exercise desktop/tablet/mobile, keyboard, enlarged text, invalid forms, logout,
   customer rejection, expired/stale approvals and network retry. Remove only the
   disposable fixture directory when finished. Stop the owned local server.

For physical USB Android testing, use the existing USB-localhost QA APK and reverse
the chosen server port consistently; the default existing phone helper expects
8765, while this independent browser rehearsal uses 8766. Do not relabel an
emulator-targeted APK or silently use a hosted backend.

## Verification and separate remaining gaps

`test_company_dashboard.py` adds 21 portal scenarios, including one PostgreSQL-only
simultaneous issuance test. The existing importer adds five other PostgreSQL races.
The preserved Android API and importer/Admin tests remain in the full CI suite.
Configuration, migration drift, static asset collection and all 20 architecture
guards must pass at the final pushed SHA before handoff.

Actual local browser evidence used ordinary role accounts and real persisted data:
login -> installation creation -> destination-bound template download -> formula
rejection -> read-only preview -> explicit import -> reviewer assignment/login ->
publication -> READY -> one-time QR issue/reissue. A subsequent actual loopback HTTP
recipient claim preserved both original UUIDs, returned available/unverified guide
states, and denied stranger/former staff. Customer portal login was rejected.
Screenshots cover desktop and 390/834-pixel responsive views; 320-pixel layout was
also checked for page-wide overflow. Raw QR/codes/passwords were excluded from
screenshots and delivery evidence. Final CI IDs and exact SHAs belong in the final
delivery report, not inferred from this pre-push document.

**Dashboard gaps:** no multi-company organization/delegation model, employee account
management UI, standalone photo/asset-upload workspace, arbitrary source-workbook
column mapping, reviewed-inventory amendment workflow, automatic outbound invite
delivery or formal assistive-technology audit. Technical account/asset maintenance
retains restricted Django Admin compatibility. The primary delivery preparation,
import, care review and handover flow is independent of Admin.

**Importer/content gaps (preserved):** CSV is inventory-only, image columns are
unsupported, real company sources/content/qualifications still need approval;
no automatic watering/calendar/diagnosis or pre-account invitation was added.
Final logo/font approval, owner usability review, real-phone camera/QR acceptance,
hosted QA rollout/isolation and company acceptance remain separate gates. The issue
stays open; no merge, deployment, live Oracle mutation or production Nova change.
