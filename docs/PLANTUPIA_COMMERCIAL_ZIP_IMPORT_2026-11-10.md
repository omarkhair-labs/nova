# Plantupia — Commercial Catalogue Excel/ZIP Bulk Import (2026-11-10)

## New commercial workflow
Entry: /company/commerce/products/import/ . Not the older fictional QA importer.
Staff downloads the generated XLSX template, supplies a ZIP containing products.xlsx and images/, validates it without writes, and confirms the same selected ZIP using the in-page asynchronous confirmation.

ZIP layout:
    products.xlsx
    images/fern-cover.webp
    images/fern-detail.jpg

Worksheet name: Products. Exactly these headers, in order:
    product_key,sku,name,kind,description,variant,price,currency,cover_image,cover_alt,gallery_images,species_key

- Each row is one SKU; multiple variant rows may share the same product_key and identical product fields.
- product_key: stable lowercase slug of 3–64 chars. sku: unique uppercase code.
- kind: plant, planter or care. Currency: EGP, positive price with at most two decimal places.
- cover_image: images/name.jpg or blank. cover_alt: truthful photo description if cover exists.
- gallery_images: up to eight semicolon-separated pairs, each image path and its description separated by |.
- species_key: optional existing BotanicalSpecies key for a plant. It does not verify a physical installed PlantUnit.
- ZIP max 50MB, expanded data max 150MB, up to 600 SKU rows, individual input images <=3MB.

## Transaction and data boundaries
Preview checks product key/SKU collisions and parses/normalizes photos without writing DB or media.
Confirm checks the digest of the re-uploaded bytes, fully revalidates, creates CommerceProduct draft per key, CommerceSKU per row and optional ordered CommerceProductPhoto records.
All pictures are converted to JPEG stripping EXIF/GPS. A database transaction protects product/SKU creation, with best-effort storage cleanup after error.
Unexpected crashes during external storage writes still require media/orphan audits.
Import is create-only; it does not overwrite existing products, prices, stock counts or orders.
Imports default to qa_listed=False. Staff can deliberately opt a reviewed product into isolated QA visibility in the editor, which is NOT live publication.
Neither ZIP preview nor import creates stock, checkout, payment, shipping, reservations or customer PlantUnit identities.
Original CatalogProduct/DemoOrder QA tooling remains separate and accessible under Testing tools.
Photos are served through authenticated Company URLs and should use persistent object storage before release.

## Verification and missing full-product cuts
Run makemigrations --check, commerce bulk import tests, QA order/RFQ tests, whole Backend/Android CI.
Exercise multi-variant previews, real photo display, corrupt ZIP, duplicate keys/SKUs, rollback, user roles and QA listing gate.
Review browser UX at 320/390/430px and desktop and actual Arabic RTL locale (short Arabic labels are not a complete translation).
Verify Preview #253 database/media storage isolation before deploying a migration.
Still needed: commercial publication approvals, stock reservation, courier and payment reconciliation, full B2B contracts/quotes, full Arabic/English localization and live customer/device acceptance.
