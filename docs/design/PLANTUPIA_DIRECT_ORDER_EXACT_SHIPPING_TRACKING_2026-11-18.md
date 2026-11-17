# Plantupia — Direct Orders, Exact Shipping, Honest Tracking

**Date:** 2026-11-18 (Cairo)
**Status:** Product contract + first tracking UI slice on isolated QA review branch; direct checkout and third-party courier integrations **NOT IMPLEMENTED OR ENABLED**.
**Base:** PR #320 / `refine/plantupia-delivery-quote-journey-20261117` (stacked on #319/#318/#317/#316).
**No shipping price, travel time, courier GPS, merchant API credentials, payment or stock state has been invented.**

## 0. User correction / why this changes the earlier plan

A customer's everyday purchase must not become an internal staff quotation workflow by default. The user specifically wants the **exact known delivery price displayed before purchase**, a normal **Place Order** decision, and a genuinely useful Amazon-class **Track Order** view. The company employee should fulfil and handle exceptions, not mediate every ordinary checkout.

The previous Chapter 05 stages — address, pin, payment preference, quote request, manual delivery quote, customer approval — are real and technically coherent but should become a **fallback only** when an approved automatic rate is unavailable. The active QA PR #320 remains draft; do not release that manually gated path as the default.

## 1. What already works in the actual repo

`backend/plants/commerce_orders.py`:
- `request_shipping_quote` creates `CommerceOrder(status=quote_pending)` without collecting money or reserving stock.
- Staff `quote_shipping` sets `shipping_fee`, `shipping_quote_version`, three-day quote validity and `status=quoted`.
- Customer `accept_shipping_quote` validates current quote/version, stocks and price snapshots, atomically reserves counted stock, marks `status=confirmed`.
- `CommerceRealOrderEvent` persists `quote_requested`, `shipping_quoted`, `quote_accepted`, `preparing`, `dispatched`, `delivered`, cancellation/returns.
- `commerce_fulfillment.py` requires operator-attested handover, a carrier name and tracking reference, and independently retained proof for delivery.
- `order_json` returns customer-owned event timeline, carrier/handover evidence, totals, payment truth and delivery status.
- Android `StoreOrderCard` previously hid all events behind a small text toggle and mixed order state with sale/payment/return controls.

**Absent from current commercial implementation:** customer-facing automatic rate calculator backed by a merchant-approved tariff; direct `place-order` transaction with that verified rate and stock hold; carrier-created shipment ID/webhook events; live driver coordinates or an ETA algorithm sourced from carrier; push delivery notifications tied to carrier updates.

## 2. External sources actually consulted

- [Bosta official Developer Docs](https://docs.bosta.co/) — merchant/API integration and account/API key.
- [Bosta rate/pricing site](https://bosta.co/en-eg/pricing) — commercial pricing depends on shipping/account terms; do not use a generic public number as the company's tariff.
- [ShipBlu official API Tutorial](https://docs.shipblu.com/tutorial) — covered governorates/cities, creation and tracking statuses (`CREATED`, `PICKED_UP`, `IN_TRANSIT`, `EN_ROUTE`, `OUT_FOR_DELIVERY`), and carrier labels.
- [ShipBlu official API Reference](https://docs.shipblu.com/api-reference) — sandbox requires merchant credentials and requesting access via `tech@shipblu.com`; API key from merchant account.
- [Amazon Map Tracking explanation](https://www.aboutamazon.com/news/retail/track-amazon-package) — location-on-map appears for eligible Amazon deliveries near arrival, not a property of every tracking number.
- [Amazon Shipping: shipment scan and GPS where available](https://shipping.amazon.com/insights-and-news/how-to-track-your-package) — status tracking and driver GPS are different data products.

These are **implementation references**, not signed carrier terms, merchant authorization, proof of live API access or permission to reproduce another app's design.

## 3. Target flow — NORMAL order (only when shipping can be authoritatively priced)

```text
Shop -> Cart -> Address & validated Map pin
 -> authenticated server-side exact shipping rate (source + version + expiry)
 -> Review: products EGP X + delivery EGP Y + applicable approved fees EGP Z = EGP TOTAL
 -> one explicit PLACE ORDER request
 -> atomic recheck of product publication, unit prices, SKU stock and quote validity
 -> confirmed CommerceOrder + counted stock reservation once, idempotent confirmation ID
 -> purchase receipt with a real order ID
 -> company picks/packs -> carrier handover -> carrier scans -> delivery proof
 -> real My Plant activation if a living object is physically delivered
```

No staff action just to calculate a **known** normal delivery price. Staff still physically prepares and hands over orders, manages failed delivery/returns/refunds and audits payments.

## 4. Exact shipping rate contract (must be implemented before direct checkout)

Use a **versioned pricing source** with a specific validity window; either a Plantupia-approved commercial zone/tier table or a signed merchant carrier account/API. Never quote from a web search result or infer a fixed EGP fee from a governorate label.

**Input (authoritative):**
- source/pickup warehouse or origin + supported delivery **zone ID** resolved from an authoritative city/zone model; a user-entered city string alone is not enough
- confirmed delivery pin, and serviceability/coverage validation; the pin does not itself define an arbitrary carrier service area
- cart fingerprint and exact published SKUs, quantities, size/weight/handling class actually verified per product; fragile live plants/oversize pottery must use correct shipping class
- delivery service level (if offered) and approved COD/collection fees, fuel or remote-area surcharge and tax where contractually applicable
- merchant tariff version or live carrier quote ID, timestamp and expiry

**Output (authoritative):**
```json
{
  "available": true,
  "rate_source": "merchant-approved-tariff-or-live-provider",
  "service_code": "CONTRACT_DEFINED",
  "zone_id": "CANONICAL_ZONE_ID",
  "cart_fingerprint": "<exact current fingerprint>",
  "shipping_amount_egp": "<server decimal amount>",
  "other_required_fees_egp": "<only if evidenced>",
  "items_subtotal_egp": "<server snapshot>",
  "final_total_egp": "<server decimal sum>",
  "rate_version_or_quote_id": "<version-bound identifier>",
  "valid_until": "<server timestamp>",
  "estimated_delivery_window": null
}
```
The strings above are **schema illustrations**, NOT values, contract facts or a current API response. If coverage, product dimensions, carrier quote or pricing source is absent, server returns `available=false` with a reason — no fake `0 EGP` or guessed number.

**Backend transaction guard:** `POST /commerce/direct-orders/` (proposed only) must validate customer authorization, idempotency key scoped to this user, fixed rate/version/fingerprint, warehouse/stock and exact catalog prices, then create order, snapshot shipping/fees and reserve units inside one database transaction. On rate expiry/cart change/stock race return explicit 409 and refreshed review, not silent price increase. COD remains unpaid until actual collection/evidence; transfer preference remains unverified until independent financial reconciliation. Existing manual `quote_pending` path remains for exceptionally unpriceable cases.

**The commercial inputs still required:** rate card for Plantupia's actual courier(s), supported governorates/cities/zones, origin warehouses, parcel handling/weights/dimensions for each SKU, surcharge policy, service time SLA, merchant API contract and credentials (if provider-led). Do not enable a price until merchant approves these data.

## 5. Tracking semantics — no pretend courier

There are THREE layers with progressively stricter truth requirements:

**A. In-app order progress: IMPLEMENTABLE NOW.** Use `CommerceRealOrderEvent` and `CommerceFulfillmentReceipt` only:
- **Quote requested** / **Quote ready** before customer confirms. This is not yet a placed order.
- **Order confirmed** when an actual acceptance event exists.
- **Preparing** when operator preparation event exists.
- **Handed to delivery team** only when actual dispatch event **and** carrier/tracking reference exist.
- **Delivery recorded** only when a delivery event and retained proof flag exist.
- Display recorded timestamps from event history. No fabricated "out for delivery" scan, driver pin or delivery date.

**B. Carrier status tracking: requires an approved integration.** When order is dispatched, create shipment through carrier credentials or assign verified existing shipment ID; keep idempotent shipment creation and reconciliation. Accept verified signature/authenticated webhooks or authenticated polling, map carrier statuses to internal event taxonomy (picked up, at hub, on route, out for delivery, delivered, attempted, delayed, returning), retain raw carrier event ID/time/source and ignore duplicate/out-of-order events. Shipment states can have multiple parcels and partial deliveries. Expose the status to the owning customer only and never overwrite merchant fulfillment proof with an unverified notification.

**C. Live driver map/ETA: optional only where real service exists.** Show dynamic map exclusively during a valid delivery window and while a verified carrier fleet feed or authorized in-house courier device provides recent GPS coordinates; disclose last-updated time, restrict access to the intended order/customer, expire old positions and turn off sharing after delivered. Do not show precise private driver location or arrival countdown from an unverified server timestamp. No GPS feed means no moving pin. An actual carrier's public tracking page can be a separate source-linked fallback if verified, not a fabricated URL.

## 6. UX, motion, states and accessibility

1. `Order placed` screen should show order reference and an exact receipt **only for successfully placed direct orders**.
2. `Your Orders` should surface an always-visible compact progress timeline in the actual Plantupia visual language: editorial order title, recorded steps, quiet markers, explicit source-backed timestamps, actual carrier and tracking reference, honest next action.
3. Shipment status updates transition gently on **new recorded events**, not on local timers; no generic confetti/phantom animated courier.
4. "Arriving today", two-hour ETA, "N stops away", a route map and moving vehicle appear **only if the carrier supplies that data**.
5. On stale/offline errors, show last verified update and its age, with refresh; don't turn an API timeout into "delivered".
6. Support Android Back, large fonts, talkback/live-region announcements, and empty events/no carrier/multi-package/attempted/delayed flows when the backend actually models them.
7. Live-plant handover can lead to My Plant and Care Guide only after actual recorded individual plant delivery and ownership evidence.

## 7. What this branch actually implements now

- `PlantupiaOrderTracking.kt`: verified-only milestones for existing real-order events, explicit carrier handover and source-based proof; no fabricated ETA or location.
- `PlantupiaStorePresentation.kt`: replace legacy hidden order history and duplicate carrier text inside `StoreOrderCard` with always-visible Plantupia tracking component while retaining quote confirmation/payment/returns.
- `PlantupiaOrderTrackingTest.kt`: source evidence gates, pending quote != order, cancellation, handover, delivered proof and no fake GPS.
- `PlantupiaOrderTrackingInstrumentedTest.kt`: Compose QA fixture for visible progress, accessible event-history toggle and no fake courier map.

**Not implemented on this branch:** automatic shipping price calculation, new direct order endpoint, courier connection, external shipping webhooks, GPS, ETA, push notifications or automatic fulfillment. Those require approved business data/credentials and focused backend/Android test slices. This PR is not permission to expose draft catalog merchandise or auto-confirm stock.

## 8. Remaining engineering slices and acceptance

1. **Shipping master data:** company tariff/warehouse/zone/parcel records + validation and access control; tests using labelled fictional QA rates *only*.
2. **Rate quote backend:** exact versioned fee calculation, taxes/surcharge/coverage, cart fingerprint, expiry, stock gates, wrong-zone/dimension rejection, QA feature flag default OFF. Never return a fake exact price.
3. **Direct order transaction:** idempotent Place Order endpoint with atomic stock reservations and fee snapshot; concurrent customers, expiry, price change and network-retry tests. Keep existing manual request fallback.
4. **Checkout:** real Android server-rate preview and exact final review + Place Order + genuine receipt; conditional branch to exception quote when rate unavailable.
5. **Carrier adapter:** merchant sandbox keys, label creation, signed webhook/authorized polling, event mapping, duplicate scans, non-delivery and refund states.
6. **Tracking details:** statuses and optional ETA, notifications, operator controls; GPS optional only with a real authenticated recent feed.
7. **Full acceptance:** two customers/one staff account in isolated QA, real carrier sandbox and approved tariff, stock concurrency, slow/offline network, live photos and 320/360/390/430dp screenshots, 200% text, privacy and location rules.
8. Then sequentially merge stacked QA chapters; **no production deployment, payments, published rates or courier claims** until commercial sign-off and data proof.

## 9. Research/reference links

- [Bosta API docs](https://docs.bosta.co/)
- [Bosta commercial pricing](https://bosta.co/en-eg/pricing)
- [ShipBlu API reference](https://docs.shipblu.com/api-reference)
- [ShipBlu API tutorial](https://docs.shipblu.com/tutorial)
- [Amazon official guide for map tracking](https://www.aboutamazon.com/news/retail/track-amazon-package)

**Definition of Done:** customers can see the **actual price before deciding**, place a genuine server-confirmed order without obligatory staff quotes where the merchant has a verified service price, then see exactly what happened to their shipment and when. Everything else is an exception, conditional service, or documented unknown.
