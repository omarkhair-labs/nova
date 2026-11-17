# Chapter 05 — Delivery / The Place and the Request

**Status:** Authored implementation on an isolated stacked QA review branch. CI/physical Android acceptance pending; not production.
**Base:** PR #319 (Cart Experience), itself dependent on #318 / #317 / #316. The existing quote-and-reservation backend remains the source of truth. No new payment gateways or courier integrations.
**Date:** 17 November 2026, Cairo.

## 01 / What exists today and what must change

The old `PlantupiaStoreShop.kt` Checkout already worked as a single long `StorePanel` page: `PlantupiaDeliveryAddressFields` (recipient, phone, city, street, floor, landmark, **actual MapLibre pin**), two equal payment-preference buttons, saved items subtotal, then `POST commerce/orders/` via the existing `demoMutation("single-shop-shipping-quote",...)`. On success it cleared the fields and instantly switched to My Orders, showing no readable acknowledged receipt.

**Keep technical truth, redesign the relationship between parts.** The user must understand: Where? What items and price are known? Which preference? What is actually submitted? What happens next? They should not confuse the request with checkout payment, an approved shipping fee, a confirmed order or reserved stock.

## 02 / Exact contracts re-opened

- `app/src/main/java/com/nova/app/feature/plants/PlantupiaStoreShop.kt`: native state, original request and transition, original MapLibre picker call.
- `PlantupiaDeliveryAddressFields.kt`: existing validation, map confirmation and phone normalization; `PlantupiaDeliveryPinPicker.kt`: explicit map interaction/no device geolocation assumption.
- `backend/plants/commerce_orders_api.py::RealOrderQuoteInput`: `client_id` UUID, 64-hex `cart_fingerprint`, recipient/phone/city/address, decimal map pin, optional building/landmark/`customer_note`, `payment_preference=cod|manual_transfer`.
- `backend/plants/commerce_orders.py::request_shipping_quote`: idempotency by stable client ID + payload fingerprint, cart fingerprint/availability check, order status `quote_pending`; **no stock reservation or payment**.
- `PlantupiaRepository.demoMutation`: existing account-scoped pending-retry journal injects `client_id` automatically. The UI MUST NOT inject its own or reset the journal on a network timeout.
- `order_json`: `id`, `status`, `customer`, `subtotal`, `payment_preference`, `shipping_fee=null`, `stock_reserved=false`, `charged=false`.

## 03 / Research references and applied mechanisms

- [Baymard Checkout UX 2026](https://baymard.com/research-articles/current-state-of-checkout-ux): review must preserve visibility of what user entered; avoid accordion stages which hide key delivery/payment information after progression.
- [Baymard Checkout Flow UX, May 2026](https://baymard.com/blog/checkout-flow-ux-optimization): review only facts already known; unknown shipping fees must remain explicitly unknown, not estimated as if a final total.
- [Baymard Payment UX, June 2026](https://baymard.com/blog/payment-ux): a multi-step checkout should make it clear that choosing a payment preference does not accidentally place an order.
- [Baymard Ecommerce UX Audit, July 2026](https://baymard.com/blog/ecommerce-ux-audit): coherent review of shipping/contact/payment before submission.
- [Android Developers / Compose](https://developer.android.com/develop/ui/compose/components/progress): show progress only for real in-flight work, not artificial waiting.
- Source artwork or layouts are **not** copied; only research mechanisms interpreted for Plantupia's existing architectural, botanical editorial language.

## 04 / Proposed vs built storyboard

| Moment | Behavior | Decision / original source |
|---|---|---|
| Arrival | "Where should these objects go?" establishes the quote intent, with a two-stage orientation rail | Contextual narrative, not unrelated form cards |
| Address | Existing fields and native map picker retained; no implicit location access | A customer's chosen pin is not verification of address truth |
| Address changes | Editing city or street invalidates *confirmation* of old pin; user confirms the actual point again | Avoid mismatched street vs old map coordinates |
| Preference | Vertical radio-choice objects with honest COD / manual transfer explanations | No payment captured or proof of bank transfer at this stage |
| Human note | Add optional `customer_note` supported by existing backend; max 500 chars | No unnecessary new API field |
| Continue | Validate 2+ char name/city, format of phone, 5+ char address, valid confirmed coordinate, real quotable cart and 64-hex fingerprint | Specific missing data list; no request when invalid |
| Review | Show exact SKU names/variants/quantities and *saved items subtotal only*, plus recipient/address/pin/preference/note | Real data; shipping not available until company supplies it |
| Edit | Return to address without erasing entered fields or selected pin | No restart or hidden state |
| Submit | Existing idempotent mutation call with generated original journal client ID; spinner; prevent double tap | Not an optimistic save/checkout |
| Server ACK | Verify UUID, `status=quote_pending`, same submitted recipient/phone/city/subtotal/payment, `shipping_fee=null`, not charged/reserved | Otherwise show uncertainty; do not claim sent |
| Receipt | New intentional state `Delivery quote requested`, real reference, subtotal, recipient, unknown shipping and explicit next actions | No automatic redirect or made-up email/SMS/response time |
| Interrupted submit | Preserve fields and review state, explain request may have arrived; suggest checking Your requests before re-submit | Stable client-side journal handles identical retry |
| Cart changes | Server re-fetch and fingerprint are reviewed again, stale quote submit is blocked | No hidden stale cart confirmation |
| Account swap | Reset recipient/phone/map/draft/receipt along with old catalog/order state | Prevent private address leaking between customers |
| Back | Review → details; details → cart; received receipt → focused request | Preserve causal continuity |

## 05 / Code and tests

- New `PlantupiaDeliveryQuotePolicy.kt`: source-of-truth draft, field issues, readiness, unchanged backend payload, verified order receipt.
- New `PlantupiaDeliveryQuoteJourney.kt`: contextual form, phase rail, payment radio choices, optional note, review scene and real acknowledgement scene.
- Updated `PlantupiaStoreShop.kt`: retains existing MapLibre and `demoMutation`; converts submission to review → request ACK; invalidates stale pin on address edits; clears private details on account switch.
- New `PlantupiaDeliveryQuotePolicyTest.kt`: validation, exact backend shape, readable/quotable saved cart, receipt negative tests, payment and idempotency semantics.
- New `PlantupiaDeliveryQuoteJourneyInstrumentedTest.kt`: native form, preference, review/unknown shipping, invalid no-submit and receipt paths on explicit fictional QA data only.
- No backend changes, no warehouse writes, no payments, no real orders submitted by these tests.

## 06 / Acceptance gates

- [x] Re-read original native form, MapLibre picker, idempotency journal and backend order quote/API model.
- [x] Research 2026 checkout/address/payment/review UI principles and preserve exact truth.
- [x] Implement staged delivery address → review → ACK receipt in one canonical Android store.
- [x] Add QA tests for validation/ACK/visual semantics.
- [ ] **Exact branch HEAD** GitHub CI success (Android compile, JVM, lint, QA APK assembly, instrumentation assembly, backend).
- [ ] Physical Android screenshots and gestures at 320/360/390/430 dp, 200% fonts, TalkBack, landscape and MapLibre picker orientation changes.
- [ ] Signed-in isolated QA customer A/B, address mutation, map offline/permission denied, 401, 409 quote/cart mismatch, double submit, interrupted request/client journal replay, real order result verification.
- [ ] Check form input focus/scroll and keyboard resize, especially a long optional note; no content hidden behind IME.
- [ ] Owner visual feedback from real device; iterate composition/motion. Chapter 06 order handling follows only after this is accepted.
- [ ] Sequential QA merge #316 → #317 → #318 → #319 → this branch; no master/Play/published store change.

**Not a claim of production readiness.** No real delivery promise, transfer, stock hold or payment in this chapter.
