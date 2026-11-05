# Plantupia — B2B Projects + B2C Commerce Product / Engineering Contract V1
Date: 2026-11-05
Status: design and implementation contract only, not a production or commercial release
Base code: PR #272 head `63cc266` on parent #253 (QA).
Owner intent: notes from 5 November meeting — order appears with full detail in company platform, staff confirms before fulfillment, plant-specific preparation and recipient QR after review. Separate Management/Plants from B2C Commerce; allow business RFQs.

## 0. Ground truth and release boundary
The existing product already has:
- staff/company overview, installation drafts, source inventory, care-guide review and recipient-bound one-use handover;
- fictional QA catalog with images, category/filter, cart, immutable QA order line snapshots, QA order queue;
- *separate* staff action converting a QA order containing plants into an unverified DRAFT installation, not a delivery or customer plant;
- source-reviewed care content and app QA under a separately reviewed rollout.

It does **not** have actual card/payment provider integration, COD settlement, InstaPay reconciliation, shipping addresses, actual commercial stock reservations, real sales invoices, returns, enterprise accounts, RFQs, binding quotes or B2B purchase orders. Existing `CatalogForm` requires "FICTIONAL QA" name, and `DemoOrder` explicitly says no charge or delivery. Do not remove these fences in a design PR.

## 1. Product structure
### B2C — Shop / direct customer order
Home > Shop > Catalog > Product Detail/Variant > Cart > Address + Delivery Quote > Review Total > Payment Selection > Place Order > Track order > Receive.

Category: Plants / Pots / Care (Tools, Soil & Substrates, Plant Nutrition) / Accessories if sourced; search + real stock filtering; featured curated selections only if recorded as such.

Product: owner-approved photo (or an honest fallback), name, variant SKU, description, price/currency, availability, physical delivery constraints, botanical reference type distinctly NOT verified specimen identity, included/warranty/returns terms, nutrition label source if relevant.

Cart: quantity limits, real availability, editable line items, persistent cart, delivery charge, promotions only if published, full total, currency and line-item price snapshot.

Checkout: authenticated customer, delivery name/contact/address by explicit consent, delivery serviceability, delivery fee/estimate, fulfillment method, payment method. Distinguish `order_submitted` from `confirmed` from `paid`. Do not claim paid or initiate dispatch from a screenshot. Support a guest flow only after separate security/ownership architecture approval.

### B2B — Projects / sourcing / installations
Same storefront can have a "For businesses / Request a quote" entry point, but RFQ is **not** a B2C purchase and cannot silently use consumer cart checkout.
Organization sign-up/verification > Request quotation with company name and contact, plant/product/installation lines, quantities and site/branches, locations, service scope and attachments (with malware scanning) > company triage > availability/technical/site assessment > versioned quote > internal authorization > customer acceptance > purchase order/contract > agreed deposit/invoice terms > procurement/stock allocation > delivery/installation > inspected actual plant inventory > reviewed handover to specifically authorized recipient > aftercare.

Large institutional buyers can upload a procurement request as an attachment, but parsing is not an acceptance of requirements. Use quote revisions, explicit validity, taxes, delivery/service lines, scope assumptions, payment milestones, audit of human approval. B2B can include products without installations, and installations can be created without an e-commerce sale.

## 2. Company Platform information architecture
Overview
- KPIs sourced from persisted approved records, no fake revenue or fake order totals
- actionable activity feed and missing-data queue
B2B · Projects
- Organizations & contacts (proposed)
- RFQs and quotation revisions (proposed)
- Contract / purchase orders (proposed)
- Installations (existing)
- Installed plant inventory (existing: physical PlantUnit and manifest)
- QR Handovers (existing: recipient-bound only after review)
B2C · Commerce
- Product Catalog (existing QA)
- Customer Orders (existing QA)
- Payment reconciliation (proposed)
- Fulfillment / Shipping (proposed)
- Returns / refunds (proposed)
Shared
- Care Library and source-approved content (existing)
- Service/support requests (existing QA)
- Stock ledger/warehouses (proposed; **not** the physical installed-plant manifest)
- Team / roles / audit, financial reporting (proposed)

Presentation rules: show **existing working** links separately from **planned** surfaces. Do not make invented B2B RFQ and payments pages that look operational. Nav grouping itself may be implemented early without changing authorized endpoints.

## 3. Transaction and stock models (planned, ADDITIVE only)
- `Organization`, `OrganizationMember` with independently granted role (requester, buyer, approver, viewer).
- `RFQ`, `RFQLine`, `RFQAttachment`, `RFQRevision`, `Quote`, `QuoteLine`, `QuoteApproval`, `BusinessPurchaseOrder` with explicit requester + approver audit.
- `ProductFamily`, `SellableSku`, `Warehouse`, `StockMovement`, `StockReservation` with warehouse scoped reservations (no negative on-hand; release on cancellation/expiry).
- `CommercialOrder`, `CommercialOrderLineSnapshot`, `OrderAddressSnapshot`, `OrderAdjustment`, `OrderStatusEvent`.
- `PaymentInstruction`, `PaymentEvidence`, `PaymentReceipt`, `PaymentReconciliation` recording verified settlement only, private storage, reviewer and immutable audit.
- `Fulfillment`, `Shipment`, `DispatchEvent`, `DeliveryProof`, `ReturnRequest`, `RefundRecord`.
- `OrderInstallationLink` may link accepted/fulfilled plant-bearing sales to *fresh staff-prepared* InstallationBatch; does not turn every plant SKU into a verified PlantUnit.
- Keep existing `DemoOrder` and fictional QA data as separate domain/test fixtures; don't silently migrate or relabel them as production orders.

## 4. State machines and confirmation points
B2C commercial order:
`draft_cart → submitted → staff_review → confirmed → preparing → ready_to_dispatch → dispatched → delivered`; alternatives `rejected`, `cancelled`, `return_requested`, `returned` with permission + event audit. No transitions to paid or fulfilled without evidence.

Separate payment status (never infer from the order):
`unpaid → instructions_issued → customer_claimed_transfer → verification_pending → verified_paid`; alternatives `failed`, `rejected`, `refunded`, `partially_refunded`. A submitted receipt/screenshot is not proof funds arrived. Track amounts and partial payments/shipments.

COD:
- option only where the company actually services the delivery destination;
- `payment_method=COD`, `payment_status=unpaid` until authorized courier/company settlement verification;
- company confirms order + stock + shipping fee before dispatch;
- track refusal, returns and collected amount, with reconciliation separated from delivery scan.

InstaPay/manual bank transfer:
- at checkout choose "Manual bank transfer (InstaPay)" only if company supplies **approved beneficiary details** and collection policy;
- display a unique order reference, amount, beneficiary display name; optional evidence reference supplied by customer;
- keep `verification_pending` until staff independently checks bank statement/settlement. Never mark paid from receipt upload alone;
- no automatic payment success, no claim of merchant API, no storage of bank credentials or private financial identifiers in logs;
- large B2B quotes may need bank transfer / milestones rather than the consumer manual method. Confirm transfer limits and beneficiary details at launch with payment operator.

B2B RFQ:
`draft → submitted → acknowledged → discovery → pricing → quote_sent → revision_requested | accepted | declined | expired`; accepted quotes can create purchase orders only after approvals and agreed commercial terms. Each revision immutable.

Installation/handover:
`order confirmed` DOES NOT produce valid QR.
Only verified physical installation inventory with an explicitly reviewed manifest can progress to ready. Staff issues short-lived one-use recipient-bound QR; customer authenticated claim establishes ownership/access according to selected mode. No generic buyer QR, no cross-account leak, no auto plant-care history inferred.

## 5. Screen-level UX
B2C app: Shop home (products first), category/filter, Product Details, Variant selection, Cart, Delivery address, Review, Payment method, Confirmation with truthful status, My Orders timeline, Support/Return. Show error/loading/empty/stock-changed states, accessible touch targets / Arabic and English / 320–430dp, font scale 200%, offline retry and duplicate submission protection.

B2B app/site: For Business CTA, structured RFQ wizard (Organization > Scope/Products > Delivery & Installation > Files > Review/Submit), status list, quote versions, acceptance. Company desktop: two complementary dashboards; B2B approval queue and B2C order queue, with consistent details panel, activity log and status reason.

Company product editor: SKU/family/variants, authentic image/gallery, product categorization and media rights, description and bilingual copy, real stock/availability, price/version, publish/review gate, inactive/sold out. Product inventory NOT the same table as installed plant inventory.

Order detail: order number, channel (B2C/B2B), buyer, full line snapshots (SKU/quantity/unit price), total, discount/tax/delivery breakdown as actually applicable, shipping address, fulfillment, chosen payment and verified payment status, audit timeline, notes, allowed staff actions with confirmation. Restrict PII to authorized staff.

Design: Plantupia editorial botanical palette, quiet typography, prominent status hierarchy and action bar; no nested scatter of cards. On wide desktop use master/detail split, on mobile use stacked sections. Use existing Company CSS tokens rather than introducing an unrelated framework.

## 6. Security / integrity
Tenant- and user-scoped authorization on every read/write, audited role changes, CSRF, exact record scoping, dedupe client UUID / idempotent checkout, server-computed totals, locked inventory reservation, no staff/customer-private mixing, no public media secrets, malware scan RFQ attachments, EXIF removal as already implemented for appropriate photos, fraud verification for manual transfers, audit logs append only, no data export without access enforcement.

NEVER share production secrets, replace real media buckets, reset preview DB, or merge to master by default.

## 7. Delivery slices and acceptance evidence
P0 — Navigation & truth: grouped Company workspace linking existing authorized pages, B2B/B2C blueprint, no changes to production/payment/DB. Django template regression/accessibility checks.
P1 — Source catalog and real inventory: approved SKU, variants, real photos, pricing and stock ledger, server-side publication/authorization, import permission and audit; order QA remains isolated.
P2 — B2C transaction: delivery address and shipping fees, real order state machine, COD and manual-transfer reconciliation, checkout and staff order queue, notifications, receipt/review events, customer tracking, returns.
P3 — B2B RFQ: organization scope, RFQ intake and secure attachments, quote revisions and approvals, PO handoff and agreed milestones.
P4 — Fulfillment + real plant linkage: pick/pack/delivery proof, inventory staff review, recipient QR, company services and aftercare, payment settlement audit.
P5 — security, QA signoff, controlled preview release, explicit owner decision on launch.

Each slice gates on exact commit CI, mobile real-device flow, two customers plus staff tests, payment adversarial/replay tests, delivery cancellation/refund tests, no cross-account leakage. Safe QA only until commercial facts and legal/payment terms are approved.

## 8. Decisions the business must supply before collecting actual money
- Official merchant beneficiary / settlement account and who can verify incoming funds.
- Which Egyptian delivery areas, courier, delivery pricing, liability, live plants packaging/returns.
- Tax/invoicing policy, registered commercial entity, COD cap, cash handling, cancellations.
- Product SKUs, approved photos/price/availability and customer support terms.
- B2B quotation validity, PO signing authority and installment/deposit policy.

No artificial order data, no fake live inventory, no payment gateway assumed.
