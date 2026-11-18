# Plantupia customer Shop source-of-truth — commercial SKU & cart

This change builds on commercial catalog review PR #287. It does not create a new QA experience. It adds the database and service foundation of **the one future customer Shop**.

## Existing identity
- `CommerceProduct` + `CommerceSKU`: the sole intended production product/variant identity.
- `CommerceProductPhoto`: reviewed, authenticated product photographs.
- `CommerceStockMovement`: warehouse count ledger, *not* reserved inventory.
- `CommerceCartLine`: customer's persistent selection of exact commercial SKUs. One row per customer + SKU; 1–10 units/line, up to 20 distinct lines, quantity 0 removes.

## Publication distinction
An employee's product-data approval (`catalog_review_status=approved`) never silently lists the product. `storefront_published_at` is a separate, default-NULL gate. This PR **does not provide a publish button** or enable Checkout. A future coordinated Shop release must explicitly decide and audit which real products are published after the rest of the order system works. Editing product or SKU invalidates approval and clears any existing publication timestamp.

## API prepared for one future Android Shop
- `GET /api/v1/plantupia/commerce/catalog/` — real reviewed and published SKU rows; a `product_id` may have multiple SKUs. Includes real prices, variant identity, counted availability and photo metadata.
- `GET /api/v1/plantupia/commerce/catalog/{product_uuid}/image/?photo_id={photo_uuid}` — customer-authenticated bounded image response, only for published products.
- `GET /api/v1/plantupia/commerce/cart/` — user-scoped saved SKU lines, server-calculated subtotal/fingerprint and availability, always `checkout_ready=false`.
- `POST /api/v1/plantupia/commerce/cart/` with `{ "sku_id": "<UUID>", "quantity": 1 }` — set quantity, 0 deletes, reject unavailable/stale/invalid SKUs.

All use existing authenticated customer access; staff cannot use customer Shop. No order, payment, stock movement, dispatch, installment plan, QR or installed physical `PlantUnit` is created.

These endpoints are the intended replacements for the old synthetic `/catalog/` and `/cart/` endpoints. Those old API routes are temporarily untouched for compatibility with installed older APKs; **do not expose them as parallel purchase flows in the final Android UI**. App cutover must use one Shop and one Cart, while checkout remains unavailable until real orders and stock reservations are implemented.

## What follows this slice
1. Actual order objects/lines, snapshot prices, idempotent checkout and **atomic reservation by exact SKU** with location-level allocation; cart must be revalidated at checkout.
2. Reuse the already-developed delivery address + confirmed MapLibre pin in that one checkout flow, including an honest shipping quote.
3. Staff-operated actual Orders inbox with authentic payment status, courier/merchant fulfillment, cancellation and returns.
4. Cut Android over to `commerce/catalog` and `commerce/cart` *at the same time as checkout*. Legacy demo routes remain read-only historical/QA compatibility outside normal Shop navigation.
5. Real phone camera, map, Arabic/English and end-to-end Company acceptance.

## Safety before any merge into Preview #253
Migrations `0023` + `0024` are additive. CI must pass for the exact head SHA. Confirm isolated Preview database backup/migrations, persistent media, existing product data and release-branch integration. **No production promotion, no real payment collection and no release APK until the full one-Shop cutover is tested.**
