# Issue #244 implementation evidence

Implementation commit: `c394496847e71b898a612bf75dffb8b27f8cd3c6`, based on preserved
PR #241 head `4e8216e043def84423b6e2110e17d4c40b13a6c1`.
Work remains on `feature/plantupia-native-experience-20261012`, PR #241 draft.
The follow-up documentation commit does not change application behavior.

Staff now download a destination-bound XLSX/CSV template, upload for a read-only
row preview, explicitly approve an atomic DRAFT-only import, and independently
review/publish source-backed guide drafts. Plant identity verification requires an
evidence reference and separate staff confirmation. Customer consumption reuses
the existing Android API and Care Guide panel; no Android product was rebuilt.

Changed implementation files:

- `backend/plants/inventory_import.py`: bounded literal-cell parsing, schema and
  destination validation, conflict/skip/create/update plan, signed expiring
  actor/state approval, parent-first row locks, atomic apply and content receipts.
- `backend/plants/admin.py` and four `templates/admin/plants/` files: protected
  discovery/template/upload/preview/approval, authentic review and publication.
- `backend/plants/models.py` and migration
  `0008_alter_careguideversion_options_and_more.py`: nullable unreviewed drafts,
  publication review constraint, immutable publication history, identity evidence,
  import receipts and granular permissions. Existing published versions are frozen
  on migration; existing plants/UUIDs and guides are preserved.
- `backend/plants/views.py`: customer species correction also clears obsolete
  imported identity-review metadata. Existing response contract is unchanged.
- `backend/requirements.txt`: openpyxl and defusedxml, no deployment configuration.
- `backend/plants/test_inventory_import.py`: 20 scenario tests including five
  PostgreSQL-only concurrency cases. All other tests/guards remain intact.

Final review hardening additionally serializes model validation/save with guide
publication, preventing a stale draft editor from changing published content.
Embedded workbook images/charts are rejected with the unsupported-content error,
so photo input cannot be silently discarded. A separate commit contains this fix
and its fifth PostgreSQL race regression; consult the delivered final SHA/run.

Local evidence before push:

- Complete Plantupia suite: 115 tests passed, seven PostgreSQL-only skips. This run
  started before two additional concurrency tests and the final review constraint;
  it is supporting regression evidence, not final-head PostgreSQL proof.
- Latest focused import suite: 19 tests passed, four intentional SQLite concurrency
  skips. The same tests run without those skips in PostgreSQL CI.
- All 20 existing architecture/design/language guards passed.
- Django configuration and migration-drift checks passed. Whitespace check passed.
- Android JVM tests and release lint were started locally; their final outcome and
  complete final-head CI must be read from the delivered evidence, not assumed here.

The existing CI workflow runs the complete Django suite on PostgreSQL 17/Redis,
Django check/migration checks, all 20 architecture guards, Android JVM tests,
release lint, debug/QA/emulator/USB QA/instrumentation/release APK builds and release
App Bundle. Check the exact SHA in [branch Actions](https://github.com/omarkhair-labs/nova/actions?query=branch%3Afeature%2Fplantupia-native-experience-20261012).
No CI conclusion is claimed in this pre-push document. The delivered report will
record the final pushed SHA, run IDs, job conclusions and remaining QA gates.

Runbook, exact schema/permissions, replay/conflict semantics and supervised QA:
[Staff inventory import v1](PLANTUPIA_STAFF_INVENTORY_IMPORT_V1.md).

Remaining limitations: no actual company spreadsheet/content/identity approval was
supplied; images use separate upload; CSV imports inventory only; no watering or
care calendar engine; no reviewed-inventory reopening/amendment workflow; inherited
privileged SQL/ORM/Admin paths do not form a complete commercial immutable audit
system. Existing-account recipient binding remains. Hosted/physical visual QA and
company acceptance remain separate. The existing Oracle QA installation/data,
production Nova, PR #240, merge, deployment, Play and release signing were untouched.
