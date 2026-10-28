# Plantupia QA Catalog ZIP Import — owner test gate

This change is **not deployed**. It lives in an independent Draft PR based on
Nova PR #241's `77856e7` checkpoint and must not be merged until source-side
Codex visual/device QA and review complete.

## What it adds
- New, staff-permission-guarded `/company/catalog/import/` page
- One ZIP containing `products.xlsx` with exact headers, and `images/<name>.webp`
- Preview-only validation (never writes), and a **separate explicit confirmed upload**
  (the file must be selected again)
- Bounded ZIP size, decompression limits, photo type/pixel validation, metadata
  stripping, valid decimal prices, demo-only SKU/name checks, duplicate protection,
  media cleanup on errors and a transactional DB insert
- Completely separate from customer orders, payment, shipping, QR and care plans

## Safety, defaults and rollback
- The importer is **off by default**, even when the existing QA demo flag is on.
- Requires the existing company/demo gates, staff `plants.add_catalogproduct`
  permission, CSRF-protected POST, plus:
  `PLANTUPIA_QA_CATALOG_ZIP_IMPORT_ENABLED=1`
- The flag must be enabled on **isolated Plantupia Preview #241 only** once
  its independent database and persistent media storage are reviewed.
- No production release, Nova production env, real customer catalogue or billing.
- If not explicitly enabled, requests to the new route return 404.
- Existing products are not overwritten. If a name already exists, the whole ZIP
  is rejected. Product identity currently uses `CatalogProduct.name` because
  the existing model has no SKU; the import SKU is recorded in company audit.
- After validation, the whole pack may be submitted using the separate confirm mode.
  Confirmation creates 12 records and sanitized photographs in the current
  configured storage; immediate image API checks and a later redeploy regression
  are mandatory before considering media persistence verified.
- PostgreSQL rollback cannot itself undo files already written to external object
  storage; the importer explicitly deletes files on failure, and this guard must
  be exercised in QA. An unexpected server crash during upload remains a residual
  orphan-file risk; do not use for commercial bulk imports.

## Required packaging
```
plantupia-shop-12-products-QA.zip
  products.xlsx
  images/monstera-deliciosa.webp
  images/snake-plant.webp
  ...
```

`Products` worksheet header order:
`sku,name,kind,description,proposed_price,currency,qa_stock,active,image,demo_only`.

Images in the owner's initial ZIP are **generated visual concepts**, not real
Plantupia merchandise photography or proof of a botanical specimen. Prices and
stock values are fictional. Existing `CatalogProduct` supports one photo per
product. Any real product deployment needs a separate authorized catalogue,
correct licencing/photography and payment/fulfilment work.

## Test gate
Run `python manage.py test plants.test_catalog_bulk_import --noinput` in CI.
Do not use hosted production migrations or seed commands to test this feature.
