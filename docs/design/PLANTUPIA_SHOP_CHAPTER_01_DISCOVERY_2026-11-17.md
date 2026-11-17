# 01 / Shop Home — The Living Collection
**Status:** Implemented in isolated QA review branch; unit tests and exact-head CI/real-device acceptance required before declaring closed.  
**Working baseline:** #253 / `02c4e6901320b4c78f101601ea998abd3f6c6e4a`.  
**Scope:** Commercial-native customer Shop catalogue landing, discovery/filters, cart orientation, media preservation, loading/error/empty states. No product-detail or checkout redesign yet.

## Brief: what the user actually needs
The shop's original design was abandoned when the team replaced a fictional QA cart with commercial SKU, stock and quote-based orders. The new commerce backend works as a separate layer, but discovery was reduced to repeated paper-coloured cards, top-row tabs and technical messaging.

**Chapter 01 must be one complete design operation, not an arbitrary style patch:** user understands which collection they're in, what can be explored, where their cart is, what counts mean, and what happens on tapping an object. User can return to exactly the same category, query and scroll position without redoing their search. Content and publication are server-authorized. Product photography must not be cut to make it fit a generic crop.

## Source-of-truth references
1. [Plantupia V2 Native Screen Blueprint / Screen 10](../PLANTUPIA_V2_NATIVE_SCREEN_BLUEPRINTS_2026-11-03.md) — compact cart count and Orders, All/Plants/Pots/Care, responsive single/two-column, clear first useful product and honest empty states.
2. [Plantupia Botanical Library × Care Experience Contract V02](../PLANTUPIA_BOTANICAL_LIBRARY_CARE_EXPERIENCE_CONTRACT_V02_2026-11-03.md) — one authenticated commerce catalogue, source-reviewed plants/care, no fake self-verification or product prescription.
3. [Shop × Care × Submission critique](./PLANTUPIA_SHOP_CARE_SUBMISSION_REVIEW_AR_2026-11-17.md) — current problems and original cutover losses.
4. Actual source: `PlantupiaStoreShop.kt`, `PlantupiaStorePresentation.kt`, `PlantupiaShopLayoutPolicy.kt`, `PlantupiaDemoJourney.kt` and `backend/plants/commerce_models.py`.

## External mechanisms — study and translate, not copy
- [Baymard, Product Listing Page UX / August 2026](https://baymard.com/blog/product-listing-page-plp-ux): scannable product lists, filters visible and understandable, readable card details, mobile category decisions.
- [Baymard, Category Page / April 2026](https://baymard.com/blog/ecommerce-category-page): useful category orientation; avoid giant promotional hero that hides the first object.
- [NN/g, Ecommerce Homepages, Category Pages and Product Listing Pages](https://www.nngroup.com/articles/ecommerce-homepages-listing-pages/): both browsers and searchers need direct, recognizable ways to find products.
- [Bloomscape, Shop All Plants](https://bloomscape.com/shop/shop-all-plants/): object-forward plant photography, honest price and browse controls.
- [Bloomscape, Care Accessories](https://bloomscape.com/shop/plant-care-accessories/shop-all-care/): shop taxonomy derived from real merchandise.
- [The Sill](https://www.thesill.com/): retail plant context and care adjacency.
No copyrighted layouts/images/assets reproduced and no peer comparison benchmark claimed.

## The authored composition (real UI)
1. **Orientation / THE SHOP**: the living collection, contextual tagline, a deliberately narrow `Collection / Orders / Cart · N` row. This is not the old three wide page tabs. Count means customer-owned saved cart quantities, **never** reserved stock.
2. **Find**: direct query field, semantic category rail with All/Plants/Pots/Care and distinct family counts calculated *after* grouping SKUs by product ID.
3. **Object study**: quiet neutral media field; actual commercial image rendered with Fit, not Crop; if cover image is absent, use first authorized gallery photo; if all media absent, a labelled abstract registration grid says `Product photo pending` and does not invent the physical object.
4. **Catalog reading**: category, bounded object name, server-authorized price or lowest available variant price, availability and unambiguous explore action. Unavailable products may be viewed, but no fake Add button in the grid.
5. **Expand selection**: first 8 families with real counts; an explicit `Explore N more objects` action reveals more without losing prior cards.
6. **No-results/no-products**: distinct states, honest no-publication label and single reset. A failed cart fetch does not block a successfully loaded public customer collection.
7. **Leave and return**: product tap preserves search/category/scroll; after a logged-in account switch, product IDs/cart/order rows are cleared and API fetch is account-keyed.
8. **Business path**: accessible separate `For spaces, projects and wholesale`, not an invented shared retail checkout.

## Not in this chapter
- Gallery swipe/zoom, selected SKU, add-to-cart state/mini-cart, order/quote receipt, Shop nutrition metadata migration, Care Guide, web Submit confirmation. They need their own full briefs, tests and acceptance; no pretending these features are solved by Shop Home.
- No fake product photos, AI botanical facts, fabricated categories/filter facets, ratings, discounts, stock or reviews.
- No publication flag or launch authorization changed; QA record import remains separated from real commercial inventory.

## Behavior/acceptance storyboard
| Step | Action | UI truth | Verify |
|---|---|---|---|
| 01 | User opens Shop | Collection context, real objects, no repeated header tabs | Actual 320/360/390/430dp device capture |
| 02 | Searches object or SKU | Same backend families filtered, count updates, scroll remains navigable | query/missing/partial input and keyboard |
| 03 | Taps Plants/Pots/Care | One family count per real product; no fake subcategories | group-by against many SKU variants |
| 04 | Taps object | Product ID carried to canonical detail, then Back returns to query/category/scroll | real navigation + Android Back |
| 05 | Inspects card image | Whole product visible, no cropping; missing photo truthful | 4:5, 1:1, 3:2, gallery-only, 404 |
| 06 | Opens Cart | Cart badge matches server-saved quantity; failures don't fake zero | authenticated customer A/B, offline |
| 07 | Sees no published products | Helpful empty state, no QA merchandise leak | empty API, review-only SKUs |
| 08 | Paginates | 8 at a time, no duplicates or state loss | 0/1/8/9/100 families |
| 09 | Changes user | previous user's product list/cart not visible while loading | two-account login/logout |
| 10 | Reader/large font | single-column on narrow/large text; 48dp filters | TalkBack, 200% scale and contrast |
| 11 | Failure/slow network | real error and retry; no old products shown as current | 401, 429, offline, stale state |

## Implementation / actual source points
- New `PlantupiaShopDiscovery.kt`: category model/counts, truthful cart label, accessible category rail, visual no-photo treatment.
- `PlantupiaStoreShop.kt`: canonical collection header, compact navigation, real product family filtering, updated browse state and account-keyed requests, graceful failed-cart behavior.
- `PlantupiaStorePresentation.kt`: product media & content hierarchy and gallery-only image fallback.
- `PlantupiaDemoJourney.kt`: **opt-in** full-object scale for real store only; old QA gallery presentation unchanged.
- `PlantupiaShopLayoutPolicyTest.kt`: count and cart-state policy regressions.

## Closure gate (before moving to Chapter 02)
- [x] Read prior UX contract and current native/backend source.
- [x] Research 2026 e-commerce discovery/category reference mechanisms and actual plant retailer.
- [x] Implement in a single isolated QA branch without new commercial data structures.
- [x] Add targeted JVM unit regression assertions.
- [ ] Exact branch HEAD: Android build, tests, architecture checks and lint green.
- [ ] Actual native screenshots on Android at 320, 360, 390, 430dp; tap, Search, filters, Back, scroll, photo display reviewed.
- [ ] QA account A/B, offline/retry, account switch and catalog empty/nonempty state verified.
- [ ] Owner critiques visual composition from real screenshots; iterate if needed.
- [ ] Merge verified chapter to #253 QA only; master/production remain untouched.

**Do not mark Chapter 01 complete solely because CI is green.** This document preserves unfinished acceptance explicitly.
