# Plantupia — Production release gate (2026-11-10)

This is a **release contract**, not a status claim or permission to deploy. Preserve the single Android Shop and all existing plant-care, installations and QR workflows. No synthetic orders, no direct publication DB edits, no staging-to-production shortcuts.

## Current stacked changes

- #287 reviewed source catalog and SKU checks; content approval does not publish.
- #288 canonical real CommerceSKU + persistent cart.
- #289 genuine delivery quote -> exact customer acceptance -> atomic warehouse stock hold, without collecting money or dispatch.
- #290 one Android Shop, original native MapLibre picker and shared address form.
- #291 distinct audited merchant publish/unpublish, **disabled by default**.
- #292 owner-scoped status history for genuinely recorded order events; no fictional courier tracking.

## Non-negotiable remaining implementation

1. **Fulfillment & stock**: authorize preparation, pick/pack, actual location-based count decrement and reservation consumption; avoid double-decrement and include operator receipts; preserve cancellation and damaged/partial shipment behavior.
2. **Payment truth**: formally approved COD process and manual InstaPay/bank reconciliation against official merchant accounts. Define evidence, reviewer, time and double-confirmation where applicable. Never display Paid solely from a customer's transfer screenshot or payment preference. Real gateway integration is a separate approval.
3. **Delivery**: verify serviceable zones/charges, dispatch reference and courier/operator proof of pickup; distinguish carrier status from proof of delivery. No auto-delivered states.
4. **Returns & refunds**: merchant policy, time windows, request/approval flow, physical receipt/condition, restock decision and verified refund receipt. No imaginary automatic bank refund.
5. **B2B**: complete RFQ → quote/revision → customer approval → project/order/installation → physical inventory + QR handover, alongside wholesale-only requests.
6. **Product truth**: actual photos, alternatives, price, stock counts, and responsible staff review for each public SKU; explicitly publish only after release gate is approved. Historic QA data remains separate.
7. **Company operations**: role scopes, review history, owner/next action, customer support, stock/financial reconciliation and audit retention.
8. **Customer experience**: Arabic and English checks, order identifier, real events, support/return entry point, checkout validation, pin permission and offline/error/retry behavior.
9. **Notifications & legal**: user-consented reliable order update notifications, company policies and customer-facing shipping/returns/payment terms; no unapproved promises.
10. **Privacy & reliability**: auth/ownership tests, idempotent updates, concurrency on PostgreSQL, upload protection, backups, media bucket isolation, secrets, server errors and data retention.

## Joint acceptance test — no substitutions

- Verify migration chain and backup on isolated Preview #253; never run unverified migrations on shared Nova/Production data.
- Import one real product and verify photo storage and actual SKU/price; reject publication before review and while release gate disabled.
- Publish only in a deliberately authorized test environment, then verify exactly one customer Shop/catalog and no historical demo fallback.
- Customer A browses, adds correct SKU, changes quantity, selects original map pin, enters phone/building and requests shipping quote.
- Company sees the *same* order and customer-confirmed coordinates, replies with an exact EGP delivery fee; customer sees and accepts the matching revision.
- Customer B competes for the same inventory; no oversell or partial holds. Customer A cancellation releases exactly once.
- Confirm the recorded timeline agrees in Android, API and Company; no fabricated payment, dispatch or delivery labels.
- Validate 320/360/390dp Android layouts, larger system fonts, Arabic text, keyboard, map permissions, no connection, expired quote, duplicated taps and logout/login as a second user.
- Test manual paid/dispatch/return/refund only **after** those future evidence-backed workflows are implemented and independently reviewed.
- Run all backend and Android CI, install actual fresh APK on phone, perform operator UAT and obtain written merchant approval before coordinated deployment.
- Document rollback and migrate/back up database + isolated media safely. Do not merge these stacked PRs or flip any publication/payment switch before acceptance.

## Release decision

**NOT APPROVED FOR PRODUCTION** until the missing operational workflows are implemented and every required test passes. Green CI for one PR is necessary but not sufficient. Capture exact build SHA, deployed Preview SHA, test evidence, operator sign-off and rollback plan at release time.
