# Plantupia — One real customer checkout & stock-hold contract (2026-11-10)

This is **not** a new QA checkout or a second customer Shop. It builds directly on PR #287 real product review and PR #288 real `CommerceSKU`-backed Shop/Cart.

## Data & operational behavior

- A customer has one `CommerceCartLine` cart of exact SKU quantities and live warehouse availability.
- `POST /commerce/orders/` creates one **genuine delivery quote request** using the current cart fingerprint and the customer-entered address and confirmed map pin. The order's `CommerceOrderLine` records immutable SKU, description, exact quantity and price snapshots. A retry with the same `client_id` and content returns the same real order. **No stock reservation, payment or delivery** at this stage.
- Authorized Company staff sees this request in the B2C **Actual Orders** inbox, confirms delivery serviceability, enters a real exact shipping fee (including explicit zero only when free), or declines. This increments `shipping_quote_version` and grants 72-hour validity; no fictional shipping quote is invented.
- The customer sees the item subtotal, the actual quoted EGP shipping fee and the final exact total. `POST /commerce/orders/{id}/accept-quote/` with the current `quote_version` validates exact SKU prices/publication again and reserves counted physical stock **by warehouse location**, atomically. Other customers' accepted quotes cannot hold the same units.
- The order becomes `confirmed`, not `paid` and not `delivered`. Payment preference is COD or manual transfer; payment_status remains `unpaid` until independently evidenced and reconciled in a later release cut.
- Customer `POST /commerce/orders/{id}/cancel/` before preparation cancels the request and releases all active SKU/location holds. Repeat calls are idempotent. Stock count corrections may not reduce on-hand units below active reserved units.

## Real customer API

- `GET/POST /commerce/cart/`: persistent real SKU selection. `can_request_shipping_quote` when cart is valid. Checkout is not yet promoted to the shipped Android UI.
- `GET/POST /commerce/orders/`: customer-scoped real order history and quote submission.
- `GET /commerce/orders/{id}/`: owner-scoped exact quote and status.
- `POST /commerce/orders/{id}/accept-quote/`: confirm exact quote and reserve stock.
- `POST /commerce/orders/{id}/cancel/`: cancel/release unfulfilled order.

All paths above are under the existing `/api/v1/plantupia/` API prefix.

## Company

- `/company/commerce/orders/`: real inbox, quote-pending, quoted, confirmed counts.
- `/company/commerce/orders/{uuid}/`: customer address, confirmed coordinates, exact line snapshots and stock holds.
- `/company/commerce/orders/{uuid}/shipping/`: staff-only exact EGP quote or decline.
- Actual orders replace QA order requests as the **primary B2C navigation**; historical QA records stay only in Testing Tools for migration continuity.

## Guarantees and remaining Production blockers

- No user is charged by a payment preference, and quoted/confirmed orders are never represented as a courier dispatch.
- No product is customer-listed merely because its data is approved. `storefront_published_at` is NULL until an explicit, audited publication action in the final coordinated release.
- **Still required before Production:** actual payment-provider or COD/manual transfer reconciliation with evidence, proof-based dispatch and delivery, cancellation/return/refund after fulfillment, inventory fulfillment decrement/release with auditing, shipping zones/coverage policies, operator roles, notifications and signed commercial/legal acceptance. Android **must be cut over only once** to the new Shop, Cart, Checkout and My Orders; old demo routes are not production shopping.
- Database migrations 0023/0024/0025 are additive. Preview #253 must be backed up and isolated before deployment. Run full Backend/Android CI and real-device end-to-end flows including parallel checkout on PostgreSQL.

## Definition of done for this backend slice

Two real customers request quotes for the same SKU and the same pooled stock. The company quotes both. The first customer accepts and exact warehouse stock is held; the second customer receives a truthful out-of-stock response without any partial reservation. Merchant receives one genuine order in Company, no fake DemoOrder, no payment or dispatch inferred. Customer cancellation releases exact stock and repeat cancellation cannot double-release.
