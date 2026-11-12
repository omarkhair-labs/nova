# Plantupia — One Production Product / Closure Map
Date: 2026-11-10
Reference: existing isolated Preview #253 branch. This is a **production-convergence plan**, not a new MVP or a second QA release.

## Non-negotiable product contract

Plantupia has one customer-facing Shop. Product source of truth is `CommerceProduct` + `CommerceSKU` + `CommerceProductPhoto`. Counted stock is `CommerceStockMovement`; installed physical plant identity is `PlantUnit` only after a reviewed installation/handover. The app shows one Cart, one Checkout (address + user-confirmed map pin + eligible payment option), one My Orders timeline. Company B2C operates the same products, orders, warehouse and delivery records. B2B reuses product identity but keeps quotation/project operations separate. Care and QR remain associated with the correct physical plant, not fabricated from an SKU.

`CatalogProduct`, `DemoCartLine`, `DemoOrder` and `CommerceOrderRequest` are currently QA artifacts. Preserve existing QA/history data as needed, but do **not** let these artifacts define a second live customer checkout.

## Verified implementation boundary, as of this branch base

| Work area | Confirmed today | Production exit requirement |
| --- | --- | --- |
| Real catalog | Commercial products, variants, photo gallery and Excel+ZIP import; no real publishing state | Verified source data, explicit approval, consistent customer read API, searchable variants, authorized durable media, data lifecycle |
| Shop | Android reads fictional `/catalog/` with synthetic QA product IDs | Android reads actual published `CommerceSKU` identity; remove fictional catalog from normal customer navigation |
| Cart | `/cart/` stores `DemoCartLine` against `CatalogProduct` | Signed-in persistent cart uses real SKU IDs, per-line quantity, active price/availability and proper retry behavior |
| Address and maps | Android `PlantupiaDeliveryPinPicker` and `PlantupiaCommerceQaJourney` exist | Checkout is entered FROM Cart; verified user address + manual pin, optional device location, consistent validation |
| Orders | `DemoOrder` and noncommercial `CommerceOrderRequest` split | One active commercial order service/API, SKU/price snapshots, no false collection/dispatch |
| Stock | Physical stock count ledger and nonnegative adjustments | Atomic reservation/release against available stock and multiple concurrent checkouts; cancellation, return and fulfillment accounting |
| Money | COD/manual transfer preferences only; no captured payment | Define offered options and settlement responsibilities with Plantupia; reconciliation, invoices/receipts, no success without evidence, refund handling |
| Delivery | QA progress labels and staff-entered shipping quote | Serviceable address/zone and fee, carrier or staff dispatch evidence, proof of delivery, exceptions, return workflow |
| Company operations | Grouped B2B/B2C/Care, product importer, QA order inbox | One actual Orders inbox, roles, stock/delivery operations, actionable notifications and logs |
| B2B | QA RFQ, installation workflow, real plant inventory, reviewed recipient-bound QR | RFQ→versioned quote→acceptance→project→installation→handover→aftercare, named accountable owners |
| Plant care | Species/guide/history/photos and private service requests exist in code | Real camera and gallery permission UX, failure recovery, complete care/support acceptance test |
| UX/Language | Three world entrances, Arabic labels, English screens; Android QA overflow observed | Full Arabic RTL and English locale, copy, readability at 320/360/390/430dp + high font scale, accessibility, operator acceptance |
| Release | Preview #253 and CI exist | Backup/restore, migrations, isolated Preview and persistent media, monitoring, authorization tests, exact APK/API build version, pilot/production approval |

## Release cuts (dependency order, not separate end-user products)

1. **Catalog source of truth.** Implement staff product-data review and change invalidation; imported products start draft. Do not label internally approved data as a live offer until the unified Shop exposes it. The source-of-truth schema must remain stable through checkout.
2. **Replace (not augment) customer Shop.** On a coordinated cut, switch the existing customer catalog/product detail and photo display to reviewed commercial products and SKU IDs. Do not keep a visible "demo orders" route as a second way to purchase.
3. **Cart + real checkout + inventory.** Move persisted cart to exact SKU identities, compute server-authoritative totals, create one real customer order with idempotent retry and atomic reservation, make address/pin inputs part of that single checkout, and release stock upon cancellation. Reject oversells under concurrency.
4. **Merchant order fulfillment and money truth.** Company reviews actual orders, calculates shipping under written policy, handles COD/manual transfer with verified settlement, records carrier/tracking and delivery proof, handles failed delivery/cancellation/return/refund. Never infer a payment from a preference or a delivery from a status click.
5. **B2B + Care linked operations.** Complete quotation/contract/project stages, physical plant identity, reviewed QR handover and living care; test camera/photo and real device lifecycle.
6. **Bilingual operations + production evidence.** Arabic and English complete flows, UI/accessibility, customer/operator tests on actual devices, media persistence, backup and restoration, crash monitoring, security, release runbook and signed operational acceptance.

## Definition of done for one end-to-end order

Employee imports a real product + photos via XLSX/ZIP, reviews the content and price, counts stock; customer sees that exact SKU in the SINGLE Shop, selects variants and quantity, sees same SKU in persisted Cart, enters address + confirmed map pin, receives a server-authoritative shipping/total quote or explicit pending state, chooses an available payment method, submits once; backend atomically reserves stock and creates exactly one order on retries; Company sees the SAME order in its inbox, performs documented fulfillment/settlement, customer sees matching timeline; cancellation releases reserved stock, and return/refund state follows actual evidence. No historic QA/demo order is exposed as a parallel production order.

A UI "success" screenshot, green CI, or healthy Preview is not acceptance by itself.

## Decisions requiring Plantupia's real business input before money/delivery go live

- Real product feed and ownership/accuracy of photos, price/variants, availability.
- Delivery coverage, shipping fee rule, stock locations, fulfillment ownership/carrier, failed-delivery policy.
- Which actual payment methods can be accepted on launch, who reconciles manual transfers/COD, receipt/invoicing and refunds.
- Responsible staff roles and how they approve publication and order changes.

These are business inputs, **not** permission to stall engineering on the canonical catalog, native Shop, cart/order identity and reservation tests.

## This PR's deliberately narrow scope

Adds a staff-checked **catalog data review state** to existing `CommerceProduct`, missing-photo/description/SKU readiness checklist, audit actor/time and invalidation on subsequent product/SKU edits. **No second storefront, no new customer API, no actual sales, no shipment and no production deployment.** It is the first small code cut in a coordinated migration, not a claim that Plantupia is ready to launch.
