# 03 / Add to Cart — The Saved Selection
**Date:** 2026-11-17 (Africa/Cairo)
**Status:** Implemented draft on an isolated stacked QA branch. Exact-head CI + real-device acceptance are required before marking closed.
**Base:** `refine/plantupia-product-object-study-20261117` / PR #317. Its parent is Shop Discovery PR #316. Both remain unpublished QA review work.
**Canonical product:** one customer Shop built from `CommerceSKU`, never the fictional DemoOrder/QA shop.

## A. The actual problem
The old `PlantupiaStoreShop.updateQuantity()` issued `POST commerce/cart/`, replaced the cart JSON and showed "Cart updated." high above the product CTA. This lacked an obvious persisted receipt, exact SKU/quantity feedback, and a meaningful next decision. A customer might click again, or mistake a successful saved cart for a paid or stock-reserved order.

A **saved cart** is neither a **stock hold**, a **quote**, a **confirmed order**, nor a **payment**. The backend's `set_cart_quantity` writes a desired absolute quantity; repeat submissions of the same desired quantity should not add duplicate items.

## B. Research references (mechanism, not visual templates)
- [Baymard — Added-to-Cart Confirmation examples](https://baymard.com/ecommerce-design-examples/added-to-cart-confirmation): an acknowledged addition should expose enough context and support continued browsing or reviewing cart. Study mobile examples, not their brand copy or visual composition.
- [Nielsen Norman Group — Adding an Item to a Shopping Cart: Provide Clear, Persistent Feedback](https://www.nngroup.com/articles/cart-feedback/): weak feedback invites repeated clicks and uncertainty; don't rely only on a fleeting status label.
- [Nielsen Norman Group — Indicators, Validations and Notifications](https://www.nngroup.com/articles/indicators-validations-notifications/): keep action-dependent response near the action, and match prominence to significance.
- [W3C WAI — WCAG 4.1.3 Status Messages](https://www.w3.org/WAI/WCAG21/Understanding/status-messages.html): status feedback must be exposed to assistive tech without demanding a forced focus change.
- Source of truth is the Plantupia repository and its reviewed commercial backend; references are **not evidence** of merchant capacity, ready inventory or customer approval.

## C. The authored interaction storyboard

| Moment | Real system event | Required sensory and semantic behavior |
|---|---|---|
| 00 / Ready | Product page has a real `CommerceSKU`, cart is loaded | Button shows Add to cart / Add one more (n); selected option/price/stock stay visible |
| 01 / Touch | Customer presses once | Disable repeat taps, preserve selected SKU, show spinner and "Saving your selection…" at the same CTA |
| 02 / Commit | Absolute `POST commerce/cart/` returns | Read response as the current customer's persisted cart, not a local optimistic count |
| 03 / Verify | Response `results` contains **same exact SKU ID** and **requested final quantity** | Only now create acknowledgement/receipt. If mismatch, show uncertainty; do not display a "Saved" confirmation |
| 04 / Reveal | Verified ACK | Cart count transitions from server state; a native compact modal sheet rises with product name, chosen option, exact persisted quantity, item total and cart subtotal, plus actual product-family photo when present |
| 05 / Stay | User chooses Continue exploring / dismisses | Sheet closes, product detail and selected SKU remain; don't force an intermediary page |
| 06 / Review | User chooses View your cart | Close sheet and enter the **same real commercial cart**, which refetches saved state; never go to fake checkout |
| 07 / Server reject | HTTP validation/stock/permission/network failure | No success panel. An inline error appears beside Add and offers Check saved cart, since an interrupted mutation might have committed |
| 08 / Retry | User checks cart or repeats exact desired state | Absolute quantity is idempotent by design. Avoid accidental double-add |
| 09 / Account swap | Login identity changes | No previous user's receipt, cart count, product or error exposed to the new user |
| 10 / Out of stock | SKU has no available stock or cap reached | Existing Add remains disabled with honest stock/cap explanation |

## D. Visual grammar
- An **acknowledged field slip** in existing Plantupia forest/paper palette, not a pop-up with celebratory confetti.
- Photo is *real catalog family media* and can be absent. It never implies a photo specific to a Variant.
- Product name + chosen option + quantity + totals are the primary hierarchy.
- A thin forest/soft sage boundary separates the receipt from an important note: "No stock is reserved yet. Delivery is quoted and confirmed separately."
- One primary **View your cart**, one secondary **Continue exploring**; native back/outside dismiss preserves context.
- Small 190ms crossfade on persisted cart count. Bottom-sheet motion is tied to a verified server result; Compose respects system animation scale. No artificial delay.
- Pending, success and errors are programmatic status information. Provide TalkBack legibility and 48dp+ touch targets.

## E. Source responsibilities
- `PlantupiaCartAcknowledgement.kt`: typed receipt parser from returned cart JSON + modal Compose receipt. Return `null` unless exact SKU, exact quantity and valid product identity are present.
- `PlantupiaStoreShop.kt`: pending/error/receipt state attached to `updateQuantity` with `fromProduct=true` only. Normal cart plus/minus retains existing behavior, without unsolicited sheet. Changing accounts clears receipts.
- `PlantupiaProductObjectStudy.kt`: add button pending text/spinner, explicit near-CTA ambiguity message and Check saved cart CTA; no variant selection during a pending request.
- `PlantupiaShopDiscovery.kt`: soft count transition only when authenticated `cart` data actually changes.
- `PlantupiaCartAcknowledgementTest.kt`: exact SKU / target qty, malformed response, missing image, gallery-only, price/totals.
- `PlantupiaCartAcknowledgementInstrumentedTest.kt`: actual saved-state sheet and both navigation callbacks on a disposable no-network in-memory fixture.

## F. Non-negotiable edge cases
1. Cannot say "Added" before authenticated server ACK. Never auto-accept stock reservations.
2. The returned cart may be missing the line, or contain a different quantity: treat as unconfirmed, offer viewing the cart.
3. UI cannot infer that an HTTP timeout means backend rolled back. Don't re-send a different quantity or silently switch selected SKU.
4. A successful product add must not cause a success sheet when editing the cart's existing +/- controls.
5. No one customer's receipt after account change, and no auto-opening of an old receipt after process restore.
6. Product can be out of stock; keep Add eligibility from real stock count and quantity caps, re-evaluated server-side.
7. A real stock hold only comes after a later shipping quote and exact customer acceptance. No shipping fee promise or payment claim here.

## G. Acceptance before closing Chapter 03
- [x] Inspect backend contract, Shop mutation code and preceding Chapters 01/02.
- [x] Review contemporary cart feedback, accessibility and navigation references.
- [x] Implement ACK-only mini cart, pending and error states on one stacked QA branch.
- [x] Pure test scenarios for persisted receipt validation and Compose test for sheet choices.
- [ ] Exact-head `Nova CI` green across Android compile/unit/lint/QA assemble and backend test jobs.
- [ ] Device 320/360/390/430dp and 200% fonts: captured input/output frames, sheet focus/Back, touch, TalkBack, orientation, line wrapping.
- [ ] Two authorized QA customers, slow connection, fail/retry, double tap and interrupted/committed timeout against an isolated backend.
- [ ] Inspect repeated photo requests/cache and empty/failed media performance.
- [ ] Owner evaluates actual native screenshots; improve composition and micro-motion until accepted.
- [ ] Merge sequentially **#316 → #317 → this PR** into #253 QA, only after their independent acceptance. Never merge directly to master or enable merchant storefront publication.

This is not a checkout, a fake DemoOrder, or a production release.
