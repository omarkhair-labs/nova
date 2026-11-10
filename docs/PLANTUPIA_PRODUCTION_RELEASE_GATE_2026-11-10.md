# Plantupia — coordinated Production release gate
Date: 2026-11-10
Status: **NOT APPROVED FOR PRODUCTION**. This is an acceptance contract, not a rollout record.

## Product identity
One existing Plantupia Android app, one real `CommerceSKU` source of truth, one cart and checkout, the same MapLibre delivery pin and address form, one Company Platform. Preserve existing care, installations, QR handovers, botanical library, history and AI. Never revive or present the legacy DemoOrder / QA RFQ as a live sale.

## Stacked PR dependency chain (do not merge out of order)

| PR | Implemented in draft code | Does **not** prove |
|---|---|---|
| #287 | Product data review gate | Public listing |
| #288 | Canonical SKU & persisted customer cart | Payment/checkout |
| #289 | Real quote/order + atomic warehouse holds | Delivery/payment |
| #290 | Single Android Shop, shared address + original map | Phone/Preview acceptance |
| #291 | Explicit staff product publication, default OFF | Go-live permission |
| #292 | Genuine order timeline in customer + Company | Courier tracking integration |
| #293 | Operator-attested preparation/dispatch/POD and one-time stock consumption | Independent physical proof |
| #294 | Customer transfer reference distinct from finance-reviewed COD/bank settlement | Automatic bank verification |
| #295 | Return request, physical receipt, separate external refund evidence | Partial-return/restock automation |
| #296 | Real B2B wholesale/project enquiry, quote and customer follow-up | Signed PO/installation contract |
| #297 | Separate Commerce, Finance and B2B staff groups | Automatic staff assignments |

## Required external commercial decisions before launch
1. Official merchant payment account(s), COD custody/accounting SOP, refund method and bank-statement reconciliation process approved by company owner/finance.
2. Delivery coverage/serviceability rules, actual fee setting, assigned carriers/team, POD retention and support escalation for lost/partial delivery.
3. Shipping, cancellation, return, damage, warranty, privacy and customer-service policies approved and visible in Arabic and English.
4. Real published products with verified SKU/variant, authentic photos/storage, EGP prices, counted warehouse stock, availability and responsible content reviewer. The publication switch stays disabled until the other launch gates pass.
5. Named Company operators, finance reviewers and B2B quote reviewers; provision permission groups separately and explicitly assign identities. Validate least privilege in live Preview.
6. B2B binding purchase orders/contracts, project/site breakdown, multi-site scheduling, material reservation/installation budget and QR handover mapping — nonbinding RFQ alone is not project execution.
7. Invoice/tax/accounting compliance; exception handling for partial shipments, damaged product condition, physical restock approval, partial returns/refunds and chargebacks.
8. Order notification delivery (with consent, delivery/retry/reconciliation), support contact and escalation channels; a stored timeline is not a push notification.

## Technical release gates
- Verify the exact 0023–0030 migration chain against isolated Preview #253 on PostgreSQL and an isolated verified media bucket; back up database and storage and test rollback.
- No production merges or Coolify deploys without current green Backend/Android tests, one reviewed stacked diff and explicit release manager approval.
- Audit actual actors, no cross-account access, immutable events, correct SKU/variant price snapshots, forged request retries, manual finance permissions, duplicate dispatch/refund/return operations and stock concurrency using PostgreSQL (SQLite tests alone insufficient).
- Android real-device matrix: 320/360/390 dp, larger system font, Arabic/English, TalkBack/accessibility, keyboard, map permissions, unavailable map tiles/GPS, weak network, authentication changes and repeated taps.
- Confirm the original MapLibre pin, full address/phone/building fields, exact quote revision consent and same order/company record. Never show a paid or delivered status without corresponding operational evidence.
- Confirm actual B2B RFQ remains nonbinding and never silently creates a B2C payment/order, installation, physical plant or QR invitation.
- Verify real staff role provisioning, ingress security, upload/image access, object storage isolation, backup/restore, secret rotation, support access and monitoring.
- Install a freshly built QA APK produced from the **same** tested/deployed Preview commit. Record the commit SHA, workflow ID, deployed image digest and device/operator sign-off.

## End-to-end acceptance
- A and B add the same scarce physical SKU; the company quotes shipping; only one can reserve remaining stock at exact total; cancellation returns the hold once.
- Approved goods move to preparation, then operator-attested carrier handover consumes reserved warehouse stock once; carrier reference and POD status propagate to the same customer My Orders timeline.
- COD remains unpaid until evidenced delivery and authorized finance reconciliation. Customer InstaPay reference remains only a claim until matched against independent company records. Never invent merchant bank instructions.
- Customer requests return post-delivery; approval, physical receipt, restock decision and externally verified refund remain separate with unique events and distinct financial permissions.
- Business customer submits wholesale or installation scope; Company responds with EGP quote; quote acceptance does not consume SKU stock or create a contract/QR installation.

## Status declaration
**Draft code exists, but not yet a production release.** Green CI is necessary and does not verify live integrations, customer policies, real financial evidence, serviceability or merchant acceptance. Explicitly capture all open blockers and completed UAT evidence before promoting the stacked PR chain.
