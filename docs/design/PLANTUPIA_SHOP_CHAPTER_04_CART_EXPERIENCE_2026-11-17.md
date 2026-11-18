# 04 / Cart Experience — Reviewing the Saved Collection

**Date:** 2026-11-17 (Africa/Cairo).
**Status:** Implemented on a stacked Draft QA branch. Exact-HEAD CI and physical native interaction/visual sign-off remain required.
**Base:** `refine/plantupia-cart-confirmation-20261117`, PR #318; before that Chapters #317 and #316. Nothing here authorizes production publication or release.
**System:** The real Plantupia customer commerce cart (`CommerceCartLine` + `CommerceSKU`), not DemoOrder/QA Store.

## The brief: a cart should be a readable record of the user's choices

Previously the cart was a series of generic `StorePanel` cards containing text for the name, SKU, line total and unlabelled +/- controls, then another repeated card for subtotal. It had **no product thumbnail, weak hierarchy for the selected option, no meaningful specific mutation feedback, and no undo**.

The customer needs to recognize the *object*, confirm the *exact option*, understand *quantity and price*, and control changes while clearly knowing that a cart is saved — **not ordered, paid or reserved**.

## Reference study — findings vs decisions

| Source | Observed UX mechanism / research | Plantupia translation |
|---|---|---|
| [NN/g: Decision Making in the Ecommerce Shopping Cart](https://www.nngroup.com/articles/shopping-cart/) | An overly small image makes it hard to recognize the product; variant, quantity, price and image should support re-evaluation; image/name can link to product details | Show a recognizable **actual product-family photo**, chosen variant, SKU, price and line total; tapping image/name opens the real Product Detail |
| [Baymard: Use Buttons or Buttons Plus an Open Text Field for Updating Cart Quantity](https://baymard.com/research-articles/auto-update-users-quantity-changes) | Adequately sized +/- quantity controls reduce errors; customers must be able to remove; a confirmed removal can offer Undo | 48dp+ controls; minus at one removes and there is an explicit Remove control; after server-confirmed removal offer an actual Restore mutation (which can fail if stock changed) |
| [Baymard Mobile Cart Benchmark](https://baymard.com/mcommerce-usability/benchmark/mobile-page-types/cart) | Cart is a place to evaluate and edit items before committing to checkout | Present clear object review before delivery details, avoiding promotional clutter |
| [Baymard Checkout UX Best Practices](https://baymard.com/research-articles/current-state-of-checkout-ux) | Quantity editing, fees and errors must be understandable before proceeding | Server-owned saved subtotal is clearly distinct from shipping charge; availability blocks entry to delivery details |
| [NN/g: Adding an Item to a Shopping Cart](https://www.nngroup.com/articles/cart-feedback/) | Feedback should be persistent enough to distinguish saved action from failure | Inline pending/error/resolution and a precise saved-state acknowledgement. No speculative success |

**Boundaries:** These are behavioral reference mechanisms, not evidence that competitors use the same commercial backend, nor visual assets to imitate. No source images were copied. The selected SKU's photo is *not* claimed to be variant-specific: `sku_json()` exposes **product-level** photos, and the cart returns this object in `line.product`.

## Data truth — exact backend contract

`backend/plants/commerce_storefront.py::cart_json` returns:

- `results[]` with `product = sku_json(sku,stock)`, `quantity`, `available`, `line_total`, `notice`;
- `product` includes `id` (exact SKU), `product_id`, name, category, `variant_label`, code, current unit price, available units, `has_image`, uploaded photo IDs + descriptions;
- root `subtotal`, `all_items_available`, `can_request_shipping_quote`, `fingerprint`, `currency`.

`set_cart_quantity` accepts one **absolute desired quantity** between 0 and 10, is customer-authenticated and stock-checked, and returns the whole cart. Quantity 0 removes the row. Repeating the exact same desired quantity is safe against duplicate increments. But a timeout may leave uncertainty; success is only shown when the *returned cart* explicitly contains the exact desired SKU/quantity, or its absence after removal. The server's `all_items_available` and `can_request_shipping_quote` are the only authority for delivery eligibility.

A real cart is not a payment. Shipping requires a separate explicit quote and then customer confirmation, and only the latter reserves stock.

## Authored visual and interaction storyboard

1. **Enter** — a calm saved collection heading with number of real product families and total units; no repeated Sales CTA or fictitious promotion.
2. **Identify** — each object occupies a photo-and-information composition with order number, actual family photo in a neutral field, reviewed title, option, SKU, unit price and line total. The image and title navigate to the same real Product Detail. Real media: exact product ID + cover or approved gallery ID from the cart response; show labelled no-photo study if absent. Do not infer an option-specific photo.
3. **Review** — availability/stock warning is adjacent to the affected line. At 320dp and 200% type the quantity controls live beneath the object portrait rather than crammed beside product title.
4. **Adjust** — 48dp controls: - decreases or removes the final unit; + increases only if current stock, line availability and 10-unit cap permit. Explicit Remove remains available. Current quantity is displayed as text. All controls are disabled while a POST is in flight.
5. **Confirm** — the input is a desired absolute quantity, not an optimistic local increment. After server ACK verifies exact SKU and quantity, the line and subtotal update from the server; show a concise saved confirmation. A mismatch or network ambiguity is not a success: block repeat mutations and request re-fetch.
6. **Remove** — only a verified response **without** that SKU removes its visual row. A soft neutral note says it was removed and offers **Restore saved selection** with exact prior quantity; this is another backend request, not a simulated rollback. Restoration may be declined by stock change or publication.
7. **Empty** — an editorial but helpful state with a route back to the real collection; do not show a checkout path.
8. **Subtotal** — saved subtotal alone, no fabricated delivery fee, taxes, offers or discount. `Continue to delivery details` moves to *Chapter 05's delivery quote request*, not payment; disabled if backend cannot quote or lines are unreadable.
9. **Failure and stale response** — source errors and mismatch shown near the cart, with explicit `Refresh saved cart` before further mutation. No stale view misleadingly claims updated quantity.
10. **Cross-account privacy** — removal, pending, error and receipt cleared on identity change; no other customer's photo or cart row may remain visible.
11. **Media/responsiveness** — whole object in a recognizably sized neutral media area, controlled image fetch, no invented product or brand photos. Verify network/thumbnail decoding cost.
12. **Accessibility** — accessible quantity controls, line source/price, focusable photo/name, live status updates, scrollable 320/360/390/430dp and 200% font tests.

## Code and tests in this branch

- `PlantupiaCartModel.kt`: exact typed line model, image selection from existing product-level media contract, safe quantity policy, removal candidate, backend ACK verification.
- `PlantupiaCartExperience.kt`: full Cart UI: image-first cards, product deep-link, option/price, quantity, server-owned subtotal, accurate state, removal/restore, image-less, empty, error and delivery eligibility.
- `PlantupiaStoreShop.kt`: replace old card stack, hook real saved-cart mutations to verified ACKs, pending SKU feedback and restored exact qty; retain Product Detail Chapter 03 behavior and backend routes unchanged.
- `PlantupiaCartModelTest.kt`: unit contract including exact SKU/quantity/remove, duplicate SKU corruption, gallery-only media, absent photo, stock, invalid row and restore snapshot.
- `PlantupiaCartExperienceInstrumentedTest.kt`: Compose interaction tests for object/option/photo placeholder, quantity/remove/restore, pending, out-of-stock and quote gating. Fixtures use labelled fictional QA items and trigger **no backend writes**.

## Out of scope, not forgotten

- Chapter 05 — full delivery address/map/payment preference design and delivery quote request receipt.
- Chapter 06 — exact quote acceptance, Order timeline, delivery and finance proof.
- Real product manufacturer/material/dimension and SKU-specific image mapping fields; needs separately approved business data and backend contract.
- Real stock publication or QA inventory imports; do not enable publication to test design.
- Automated remote execution on the user's phone, promotion to `master` or Google Play.

## Closure gate

- [x] Review current Cart code and Django `cart_json`/`set_cart_quantity` truth.
- [x] Compare with NN/g/Baymard research and account for cart product thumbnails.
- [x] Build real Photo × Variant × Quantity × Price cart with full-object image treatment.
- [x] Add separate, real server-acknowledged remove/restore interactions.
- [x] Add JVM and Compose fixture tests.
- [ ] Exact HEAD CI: Kotlin/Android build, unit tests, lint and backend checks successful.
- [ ] Real device QA: 320/360/390/430dp at 1×/2× font, TalkBack, portrait/landscape, image/no image, slow media, empty/multiple, 1/10/stock-cap, removal and restore.
- [ ] Isolated QA account A/B plus staff state, post-timeout re-fetch, quantity conflict, expired sign-in, network 401/409/429/503.
- [ ] Verify image decoder/refetch cost and ensure no cross-session bitmap leakage.
- [ ] User review of actual rendered screens and flow acceptance. Keep stacked Draft until prerequisite #316–#318 are accepted.
- [ ] Merge sequentially in QA; no direct production or merchant data operations.

**Definition of done:** user can see **what they saved**, **what it costs before delivery**, **how to revise it**, and **what the next step does** without a false order/payment promise.
