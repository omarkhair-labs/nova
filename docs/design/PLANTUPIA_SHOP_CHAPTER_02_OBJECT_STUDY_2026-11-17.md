# 02 / Product Detail — Object Study

**Date:** 2026-11-17 (Cairo).
**Status:** Implemented on an isolated, stacked QA branch; pending exact-HEAD CI and physical visual/interaction acceptance.
**Parent chapter:** Shop Home PR #316 (`refine/plantupia-shop-discovery-authored-20261117`), unmerged and not yet accepted.
**Scope:** Product identity, full-object gallery, photo inspection, selected SKU, selected price, availability, accessible choices. No Add-to-Cart confirmation/mini-cart or checkout changes.

## Working brief
The original detail screen in `PlantupiaStorePresentation.kt` was a single generic `StorePanel`: a 245dp cropped photo, two text next/previous buttons, repeated material radio rows, and a purchase button. Gallery, product story, SKU choice and purchase decision competed on the same vertical rhythm. The new chapter separates the physical object from the exact sellable SKU **without inventing data**.

**Design proposition:** The object is introduced by its *actual photograph*, product identity and description; the customer can inspect the object, then knowingly choose a commercial option. A plant is not a pot, nor is care nutrition a plant. Content follows the real model rather than fake item-specific claims.

## Original and external research
- Original user/project review: [Shop × Care × Submission review PR #315](https://github.com/omarkhair-labs/nova/pull/315), historical V2 native blueprint, and Shop Home [PR #316](https://github.com/omarkhair-labs/nova/pull/316).
- [Baymard / Product Page UX Best Practices 2026](https://baymard.com/research-articles/current-state-ecommerce-product-page-ux) — available variants must be unambiguous, with buttons for choices and true price context. Reference for interaction clarity, not visual imitation.
- [Baymard / Always Use Thumbnails for Product Images, updated 2026-11-10](https://baymard.com/research-articles/always-use-thumbnails-additional-images) — expose accessible gallery navigation; touch swiping needs an explicit alternative.
- [Baymard / Mobile App UX Trends](https://baymard.com/research-articles/mobile-app-ux-trends) and [Image Resolution and Zoom](https://baymard.com/research-articles/ensure-sufficient-image-resolution-and-zoom) — native product photo must be inspectable; don't crop the useful object.
- [The Sill / Planters](https://www.thesill.com/collections/planters), [The Sill / Plant shop](https://www.thesill.com/collections/live-plants) — plant products and containers have *different* physical attributes; Plantupia may only show them when the backend has approved data.
- **Research limits:** External pages are reference mechanisms. No source images/assets/styles, competitor copy, invented source citations, live product IDs or benchmark scores were copied.

## Truth contract from actual code
The canonical server `backend/plants/commerce_storefront.py::sku_json` supplies `product_id`, `name`, `description`, `kind`, `sku`, `variant_label`, `price`, `available_units`, `in_stock`, product `has_image`, `image_description`, product-level `photos [{id,alt_text}]`, optional `reference_species`. Nothing in this payload identifies **which photo belongs to a selected SKU**. Product photos must remain explicitly family-level regardless of chosen SKU, and no color swatch or size chart may be inferred from the text label.

A plant `reference_species.scientific_name` is a **reference**, not proof that this specific sold/delivered organism has been verified or handover-claimed.

An Add to Cart action updates a persistent `CommerceCartLine`, does not reserve stock. Successful shipping quote request is not paid order. The complete add/ack receipt is Chapter 03 and **must** remain separate.

## Experience storyboard
1. **Open:** Tapping a commercial collection object opens its full Product Detail while preserving collection search/category/scroll in Chapter 01.
2. **Inspect:** Display the whole uploaded product family image in a neutral framed field using `ContentScale.Fit`; no forced crop, generic lifestyle filler, simulated shadow or AI illustration.
3. **Gallery:** Visible index `PHOTO N / Total`, horizontal thumbnails (selected border), Previous/Next controls, horizontal swipe, stable index when changing SKU, bounded index when gallery changes.
4. **Inspect photo:** Fullscreen native dialog, pinch/pan zoom with max/min bounds, explicit accessible Zoom+/Zoom− and Close, Android Back dismisses without losing selection. Respect OS reduced-motion settings.
5. **No photo:** Distinct labelled registration-grid placeholder says the photograph is pending. No fake depiction of the plant or the package. No zoom control.
6. **Identify:** Product kind, approved name and description. For a plant, show a scientific species reference only when returned by API and marked as a reference, not verification.
7. **Choose SKU:** Explicit touch radio rows with option label, exact current EGP price, availability. OOS is selectable for information but cannot be added. Changes update selected price, SKU, available units and button state immediately. Selected option survives process-safe state; if removed from catalog, fallback to a valid current option.
8. **Purchase boundary:** Existing Add action remains guarded by authenticated cart availability, current server stock, busy state and 10-item cap. Show a clear non-reservation note. No auto-adding or fake successful animation.
9. **Return:** Product→Collection returns to previous saved discovery context. Switching account must clear any old product/cart state (from Chapter 01).
10. **Accessibility:** Radio selection, photo alt text, explicit navigation alternatives, no gesture-only controls. Test 320/360/390/430dp and 200% fonts on actual Android and font scaling.
11. **Resilience:** Gallery photo fetch 401/404/network error shows a retry; real media is never silently replaced with unrelated image. SKU list may refresh or go empty; selection stays bound only to a currently returned ID.

## Code changes
- `PlantupiaProductDetailPolicy.kt` — source-verified product photo list (dedupe/skip empty media), stable saved SKU fallback, bounded swipe, stock copy and add guard.
- `PlantupiaProductObjectStudy.kt` — canonical Product Detail visual and interaction: gallery, selected SKU, details, photo inspection, purchase boundary.
- `PlantupiaStorePresentation.kt` — remove obsolete second Product Detail implementation; retain shared Store functions.
- `PlantupiaDemoJourney.kt::PlantupiaCatalogPhoto` — opt-in `displayHeight` only; old callers unchanged.
- `PlantupiaProductDetailPolicyTest.kt` — pure tests for source preservation, media, stock, variant and gesture contracts.

## Explicitly not claimed complete
- Product-specific media association (requires explicit backend schema and approved mappings).
- Materials, planter dimensions, manufacturer evidence, tool/nutrition usage; backend currently does not expose validated fields. A beautiful guessed label is a regression.
- Real account/device screenshot signoff or truly enabled merchant inventory.
- Chapter 03 / Add to Cart feedback and server acknowledgement. Do not merge those states into this Product Detail implementation.
- Exact product photo zoom performance on a real phone. UI present in source; physical visual tests pending.

## Closure checklist
- [x] Review current Shop and canonical SKU/media JSON.
- [x] Review original product UX brief and current relevant retail/gallery research.
- [x] Implement one canonical Product Detail, not a new Demo Shop.
- [x] Source-based policy tests added.
- [ ] Exact-head Android CI succeeds: compile, unit tests, lint, QA assembly, instrumentation APK assembly.
- [ ] Native visual/gesture QA: 320/360/390/430dp, large font, single/no/multiple/gallery-only photos, long titles, no image/no stock, Back, pinch zoom, Next/Previous and screen reader.
- [ ] Customer A/B and last-price/stock mutation checks with isolated approved test SKUs.
- [ ] Owner review of actual device screenshots and final art direction.
- [ ] Integrate reviewed Chapter 02 into Chapter 01, then #253 QA; no master/production promotion.

**Until every acceptance gate is demonstrated, this is an implemented draft, not a finished release.**
