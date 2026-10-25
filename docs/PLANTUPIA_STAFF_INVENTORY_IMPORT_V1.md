# Staff inventory and care-guide import v1

Issue #244 · existing branch `feature/plantupia-native-experience-20261012` · draft PR #241.
This is a Django Admin operational workflow. Android continues using the existing
authenticated Care Guide, handover, care, check-in and history contracts.
No hosted data, deployment, QR issuance or customer entitlement is changed by upload.

## Operator rehearsal: disposable local/CI database only

1. Install `backend/requirements.txt`, configure an empty local test database and
   separate filesystem media with development settings. Apply migrations there,
   including `plants/0008_alter_careguideversion_options_and_more`. Never point this
   rehearsal at Oracle, production or the existing reviewed `PLT-OFFICE-001 2.`.
2. Sign into protected Django Admin as authorized staff. Create a staff-owned Space
   and a DRAFT Installation for that exact Space. Import links appear on Installations,
   its draft detail, Plant units and Care guide versions.
3. Open **Import inventory and care-guide drafts**, select the installation and
   download its **version 1 XLSX**. The destination's UUIDs are embedded in Metadata.
   The downloadable example contains only **FICTIONAL QA / NOT APPROVED** content
   and `example.test` references. Replace those rows with authorized records for
   real operations; never present this fixture as company botanical advice.
4. Upload the workbook. Inspect the exact installation/Space names and UUIDs, every
   row's create/update/skip/conflict, before/after values, provenance and warnings.
   Preview writes no inventory, species, guides, receipt or Admin audit entry.
   Errors prevent approval. Correct the file and upload again.
5. Check **I reviewed every row…**. If any verified species is supplied, separately
   confirm independent review of the identity evidence. Choose **Approve and apply
   preview**. The complete import commits atomically or rolls back. New guides are
   unreviewed/unpublished, with no invented reviewer/date. No care event, follow-up,
   membership, invitation, public post or plant photo is created.
6. Inspect plants and manifest. For botanical approval, open each draft in **Care
   guide versions**, examine the actual source and content/rights/context, correct
   unpublished content if necessary, then select only reviewed versions and run
   **Confirm source/content review and publish selected versions**. The actor and
   actual timestamp come from the authenticated staff action. Technical permission
   does not establish botanical qualifications; company approval remains human work.
   **Withdraw** hides guidance while preserving immutable content. Subsequent advice
   requires a new version, even after withdrawal.
7. Review the complete installation and use the existing **mark as ready** action.
   Imports now refuse that batch. In explicitly authorized isolated QA, use the
   existing recipient-bound issue/preview/claim workflow. Verify original plant
   UUIDs and placements in the existing Android QA app. A verified identity plus
   published guide shows source/version/reviewer/date; an unknown identity or
   unpublished/withdrawn guide stays honestly unverified. The stranger and former
   staff must lose access after customer-owned handover.
8. Rehearse an actual care observation, optional human-selected check-in, outcome
   and Living History using the same plant. Verify viewing a guide creates no care
   record and no automatic schedule. Test bad files, double-confirm/reupload,
   expired/stale preview, CSRF, wrong account and a separate independent manual-add
   account. Preserve the existing Android tests and privacy boundaries.

For physical-phone local USB QA, follow the existing
[G/H/I QA contract](PLANTUPIA_PHASE_G_H_I_INTEGRATION_2026-10-25.md).
It requires the USB-localhost artifact and `adb reverse`; emulator loopback is a
different artifact. No physical/hosted acceptance is implied by CI or these steps.
Any later hosted QA rollout requires a separate supervised deployment decision,
isolation/backup/schema review and a **new disposable DRAFT installation**; existing
Oracle QA inventory must remain untouched. This implementation does not deploy it.

## Exact schema

Sheet and column names/order are case sensitive. All cells are literal text except
`CareGuides.version`, which may also be an Excel integer. Empty trailing formatting
is ignored; unsupported nonempty columns/sheets are rejected. Text is UTF-8 and
preserves Arabic/English. Leading/trailing whitespace is trimmed.

| Sheet | Exact ordered columns | Rules |
| --- | --- | --- |
| Metadata | `field,value` | Exactly `template_version=plantupia-inventory-v1`, `batch_id=<selected UUID>`, `space_id=<selected UUID>` |
| Species | `key,scientific_name` | Both required; lowercase ASCII slug key, max 100; scientific name max 180. Existing keys must match exactly. This defines reusable taxonomy, never verifies a plant on its own. |
| CareGuides | `species_key,version,source_url,source_reference,instructions,context_notes` | First five required; positive integer version, valid HTTPS URL <=500, reference <=240, instructions/context <=8000 each. Species must be supplied or already curated. No remote URL fetch. Existing versions must match exactly; conflicting content needs a new version. |
| PlantUnits | `source_key,label,unit_type,species_label,verified_species_key,identity_reference,placement_note` | First three required; key <=80, label <=120, `individual` or `group`, species label/placement <=180, evidence reference <=500. Verified key and evidence reference must be supplied together, and independently confirmed by staff. Blank key means unverified identity. |

Species/CareGuides may contain headers only. PlantUnits must contain at least one
plant. Optional text cells can be blank. There are no accepted `approved`,
`published`, `reviewer`, `reviewed_at`, date, plant UUID, tenant/Space override, image
URL, calendar, diagnosis or sensor columns. Photos are uploaded separately through
the existing Plant Admin; all existing photos and care/history remain intact.
Plant-specific pot/light/context can be recorded in the bounded placement note;
this version does not add a separate arbitrary per-plant context document.

The optional CSV is **inventory only**. Download it for the selected batch. Its
first line is `plantupia-inventory-v1,<batch UUID>,<space UUID>`, followed by the
exact PlantUnits header and rows. UTF-8 with or without BOM, standard comma/quote
escaping and multiline quoted text are supported. A single CSV cannot import
unrelated Species/CareGuides sheets; curate those first or use XLSX.

Limits: 2 MiB uploaded bytes; 16 MiB expanded XLSX; 80 unique ZIP entries;
exactly four sheets; 500 data rows per sheet; 8000 characters per text cell.
Legacy XLS, XLSM, encrypted archives, macros, embedded objects, external data,
defined names, formulas, hyperlinks/external relationships, error/boolean/date
cells and formula-like text starting `= + - @` are rejected. Paste values as text
and remove Excel's automatic hyperlinks. ZIP parts are inspected without extraction;
XML uses defusedxml; openpyxl reads literal cells without evaluating formulas or
following links. Producer-supplied sheet dimensions cannot hide extra rows.

## Identity, approval and concurrency

- The only plant match is **selected batch + exact case-sensitive source_key**.
  Labels, species names and UUIDs are never used to find another plant. Existing
  mappings preserve UUIDs. Distinct batches may use the same source key without
  sharing a plant. Existing cross-space manifests and archived-unit repurposing
  are conflicts. Biological replacements need a new source key and UUID.
- A changed row updates only the allowed plant fields. Blank optional fields clear
  their previous values, including botanical verification/provenance. Omitted rows,
  photos, events and history are retained; this is not a destructive synchronization.
- Preview is signed, actor/destination/content/state bound and expires after 30
  minutes. It lives in the staff browser, is not encrypted, and must be treated as
  private customer content. No raw payload/filename is stored in an import record,
  audit message or application log. Responses use `private, no-store`.
- Approval rechecks current permissions, owner and DRAFT status and locks batch,
  Space, existing plants and referenced species/guides. Same-batch imports and
  inventory review serialize. Existing taxonomy locks serialize with publication.
  Referenced inventory/guide changes invalidate approval until a fresh preview.
  Unrelated destination records cannot be overwritten by a stale workbook.
- A unique batch + normalized-content digest receipt is committed with all rows.
  Formatting, row order and filename do not change that digest. Same-content retry
  returns the original summary without mutation, including after later manual edits;
  an old file is **not** an undo operation. READY/CLAIMED imports are always refused,
  including replay. Different concurrent previews cannot silently overwrite each
  other. Concurrent first creation of shared taxonomy in different batches may
  reject one import atomically; upload a fresh preview and retry that batch.
- This is an application/Admin transaction contract. Privileged direct SQL or
  `QuerySet.update`, and broader inherited Admin amendment paths, are not a complete
  immutable commercial audit system. Staff organization policy and controlled
  reviewed-inventory amendments remain rollout gates.

## Permissions

Importer requires active staff status plus all of: `plants.import_inventory`,
`change_installationbatch`, `view_space`, `add_plantunit`, `change_plantunit`,
`add_installationmanifestrow`, `change_installationmanifestrow`,
`add_botanicalspecies`, `view_botanicalspecies`, `add_careguideversion`,
`view_careguideversion` (all under `plants`). Conservative default: ordinary staff
may import only DRAFT installations **both prepared by them and owned by them**;
superusers may choose any staff-owned DRAFT destination. No implicit cross-company
role inference is added. Publishing additionally requires
`plants.publish_careguideversion` and `plants.change_careguideversion`.
The routes are protected Django Admin routes with session authentication and CSRF;
there is no customer REST import endpoint. Use dedicated staff roles/groups rather
than handing operational users superuser credentials.

## Verification and boundaries

`backend/plants/test_inventory_import.py` covers parser limits/malicious input,
row conflicts, read-only preview, CSRF/auth/scope, tampered/expired approvals,
explicit review, rollback, stable identity/idempotency, READY/CLAIMED refusal,
publication/withdrawal immutability and import -> review -> claim -> existing
private Care Guide contract. Four PostgreSQL-only simultaneous transaction cases
cover identical imports, conflicting previews, review vs import and cross-batch
taxonomy creation. They intentionally skip on SQLite; full CI uses PostgreSQL.
Run `python manage.py check`, `python manage.py makemigrations --check`, full Django
tests, all existing architecture guards and Android tests/build/lint.

No company spreadsheet, approved botanical corpus or approval credentials were
supplied. Content/source rights and identity evidence need actual company review.
There is no image import, automatic watering/calendar, pre-account invitation or
new maintenance/support promise. Published guide consumption and customer care
remain the existing Android experience. PR #241 stays draft; no merge, production
change, live Oracle mutation or deployment is authorized by this runbook.
