# Plantupia #253 — Coolify QA environment recovery and verification (2026-10-30)

**Scope:** existing Oracle Coolify `nova-backend` PR preview, not Nova production. This document tracks diagnostics seen in screenshots and real source, not a claim that server variables were changed. No production merge or resource recreation is authorized.

## Verified inherited state
- Original #241 preview had working Company Platform, staff QA assets, a separate `plantupia-preview-db` PostgreSQL host, and `plantupia-preview-redis` service. Do not discard or seed over these assets.
- Code in unified PR #253 includes every ancestor commit from #240 and #241; the PR now targets master but stays Draft.
- `backend/Dockerfile` executes **`python manage.py migrate --noinput && python manage.py collectstatic --noinput && daphne -b 0.0.0.0 -p ${PORT:-8000} ...`** on container start. A missing setting prevents Daphne from starting; Coolify may show a successful image build while HTTP health is unhealthy.
- On #253 the logs first showed `DJANGO_SECRET_KEY must be set in production` and, after fixing that, `Railway bucket credentials are required in production`. “Railway” is stale error wording: `django-storages` is S3-compatible, not Railway-specific.
- Unlike infrastructure, **preview environment variables and storage wiring are not part of Git commits** and are not copied by retargeting a GitHub PR. Preview #241 running does not prove #253 receives the same variables.
- Existing `pilot_enabled()` allowed #241 using the verified QA DB and host. Unified #253 is now included as an allowed QA hostname **only with the same strict DB identity and both QA flags**. This is not a replacement database or a license for production handover.

## Check only configuration NAMES first, not secrets

On Coolify's **Preview deployment environment** for `nova-backend`, compare #253 to the known-working #241 runtime. Do not paste credential VALUES into chat or logs.

| Variable | Desired condition |
|---|---|
| `DJANGO_ENV` | `production` (production-grade hardening even on hosted QA preview) |
| `DJANGO_DEBUG` | `0` |
| `DJANGO_SECRET_KEY` | present, nondefault; prefer preview-only secret. Changing a key logs out existing sessions. |
| `DJANGO_ALLOWED_HOSTS` | include exact `253...`, optionally `241...`, plus `localhost,127.0.0.1`; no `https://`, no wildcard |
| `DJANGO_CSRF_TRUSTED_ORIGINS` | `https://253...` and optionally `https://241...` (no malformed slash/double-dot) |
| `DATABASE_URL` | **existing verified `plantupia-preview-db`**, host `imfiwc6cb0pkuyuzu1y1rxsu`; NOT nova production. Verify host/DB identity without exposing password. |
| `REDIS_URL` | existing `plantupia-preview-redis`, not Nova production |
| `AWS_ENDPOINT_URL` | existing working QA-appropriate S3-compatible endpoint, present |
| `AWS_ACCESS_KEY_ID` | existing scoped QA-compatible credential, present |
| `AWS_SECRET_ACCESS_KEY` | corresponding secret, present, runtime only |
| `AWS_S3_BUCKET_NAME` | existing QA-approved bucket or partition with permissions; do not create a new bucket without checking the established arrangement |
| `AWS_DEFAULT_REGION` / `AWS_S3_URL_STYLE` | retain prior known-working values if present |
| `PLANTUPIA_HANDOVER_QA_ENABLED` | `1` on QA preview only |
| `PLANTUPIA_PREVIEW_HOSTED_QA_ENABLED` | `1` on verified hosted QA only |
| `PLANTUPIA_COMPANY_DASHBOARD_ENABLED` | `1` on verified QA only |
| `PLANTUPIA_CEO_DEMO_ENABLED` | `1` for synthetic shop QA and staff demo flows |
| `PLANTUPIA_QA_CATALOG_ZIP_IMPORT_ENABLED` | `1` only after DB/media isolation and safe import readiness |

### Correct order of operations
1. **No re-provisioning.** Inspect existing #241 runtime variable names, preview-only variable configuration and resource links. Confirm exact QA database and media scope before redeploy. Shared preview PostgreSQL is shared state: any #253 migrations can affect #241.
2. Back up the existing preview PostgreSQL DB and record the running deployment revision. Avoid running migrate, reset, flush or seed commands on Nova production.
3. Restore/fix missing runtime S3 values for #253 using the existing approved setup. **Do not use fake placeholders**, expose secrets, enable DEBUG, or remove the storage guard.
4. Deploy **#253 preview only** and inspect its *own* runtime logs. The HTTP probe is `GET http://localhost:8000/api/v1/health/` expected 200 with JSON `{"status":"ok","service":"nova-api"}`. The QA customer/company routes are protected; health success alone does not prove those routes are enabled.
5. Verify: `/company/login/` (staff), `/company/catalog/` (authenticated staff), `/company/catalog/import/` (requires explicit opt-in + permission), images persist after redeploy, and correct QA customer accounts see the products. If a 404 follows healthy runtime, inspect the five preview-only Plantupia flags and user's staff permissions—not the Docker port.
6. QA APK `23d3d6e` is currently configured to request the **#241** preview URL. A **new APK targeted to #253** is required for Android-to-same-backend verification. Do not confuse a successful web upload into #253 with products appearing on an APK still pointed at #241.

### Exact launcher behavior
The #253 source has an additive database migration `0012_catalog_product_identity_and_gallery`, and its catalog importer is deliberately disabled by default. Import ZIP package with 12 synthetic products and 12 images **only after verified DB/media and staff rights**; validate first, then confirm. Real commercial sale, payment and delivery remain disabled.

### Unresolved proof (not source-confirmed)
- No direct connected Coolify API/env access in this GitHub-only source review; cannot assert current #253 variable names, verified DB host, storage credentials or latest live container health.
- Existing bucket's storage prefix/scope and persistence must be checked in Coolify, not inferred from the Django code.
- Test the #253 state on an actual phone and QA accounts after backend health and flags succeed.
