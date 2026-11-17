# Plantupia / نقد التجربة الحية: Shop × Care × Submission
**النوع:** Research / Product & Interaction Audit — مسودة للنقاش، **ليست Direction Approved أو أمرًا بالتنفيذ**.
**التاريخ:** 2026-11-17
**Baseline:** `feature/plantupia-unified-experience-20261028`, SHA `02c4e6901320b4c78f101601ea998abd3f6c6e4a`، PR #253.
**الهدف:** استعادة وحدة التصميم والسلوك والحركة، لا إضافة API أو «شاشة خضراء» أخرى. نراجع الوضع الحقيقي ثم نصمم الرحلة من البداية إلى نهايتها ونقبلها قبل كتابة UI جديد.

## 0 / تعريف المشكلة من منظور العميل

نقلنا الـShop من QA الوهمي إلى مسار تجاري حقيقي، ونجحت الوظائف التقنية الأساسية. لكن المستخدم لا يتعامل مع `commerce/orders/` أو `CommerceSKU`: هو يكتشف منتجًا، يفهمه، يقارن الخيارات، يحتفظ باختياره، ثم ينتظر ردًا واضحًا يفهم ماذا حدث وما سيحدث. وما نراه في الكود اليوم أقرب إلى مجموعة `StorePanel` متتالية من كونه تجربة مؤلَّفة كاملة.

المشكلة كذلك في Care Guide وPlant Detail وموقع التواصل: كل وحدة أضافت مفاهيم وواجهات وتدرجات بصرية مستقلة. التوحيد ليس مجرد palette أو rounded-corners؛ بل **تسلسل معنى وقرار وحدث ونتيجة**. يجب الحفاظ على الفروق بين الحقيقة المسجلة، الإرشاد العام المراجع، نية الشراء، طلب الشحن، التأكيد، والدفع والتسليم المثبتين.

## 1 / مصادر جرى فتحها بالفعل (لا اعتماد على ذاكرة عامة)

**الكود:**
- `app/src/main/java/com/nova/app/feature/plants/PlantupiaStoreShop.kt` — `screen` states, product/cart/order flow, feedback, checkout.
- `app/src/main/java/com/nova/app/feature/plants/PlantupiaStorePresentation.kt` — `StoreCatalog`, `StoreProductDetails`, `StorePanel`.
- `app/src/main/java/com/nova/app/feature/plants/PlantupiaDemoJourney.kt` — historical QA Shop categories, product content, `PlantupiaCatalogPhoto` crop policy.
- `app/src/main/java/com/nova/app/feature/plants/PlantupiaBusinessInquiryScreen.kt` — B2B feedback state.
- `app/src/main/java/com/nova/app/feature/plants/PlantupiaCareGuidePanel.kt`, `PlantupiaCareGuideVisual.kt`, `PlantupiaClientSection.kt`, `PlantupiaPrimaryRoutes.kt` — care/topic/plant route composition.
- `backend/plants/commerce_models.py`, `commerce_storefront.py`, `commerce_orders.py`, `commerce_publication.py` — product truth and staged order semantics.
- `apps/web/app/ContactStudio.tsx`, `SpaceBrief.tsx`, `api/website/enquiries/route.ts` — submission and legacy copy-only outline.
- `docs/PLANTUPIA_V2_NATIVE_SCREEN_BLUEPRINTS_2026-11-03.md`, `PLANTUPIA_BOTANICAL_LIBRARY_CARE_EXPERIENCE_CONTRACT_V02_2026-11-03.md`, `PLANTUPIA_PASS08_WHOLE_PRODUCT_DESIGN_RESEARCH_EVALUATION_2026-10-17.md`, `PLANTUPIA_PASS09_ONE_SYSTEM_VISUAL_EXECUTION_MAP_2026-10-17.md` — decisions, early research, and explicitly promised behaviors.

**External mechanism references, not source for Plantupia content/art:**
- Nielsen Norman Group — [persistent add-to-cart feedback](https://www.nngroup.com/articles/cart-feedback/), [product-page usability](https://www.nngroup.com/articles/ecommerce-product-pages/).
- Baymard Institute — [added-to-cart response patterns](https://baymard.com/ecommerce-design-examples/added-to-cart-confirmation), [image-gallery thumbnails](https://baymard.com/research-articles/always-use-thumbnails-additional-images), [checkout ergonomics](https://baymard.com/research-articles/current-state-of-checkout-ux), [order confirmation](https://baymard.com/research-articles/order-confirmation-page).
- [Bloomscape](https://bloomscape.com/shop/) for category/product material, plants and care context; [The Sill](https://www.thesill.com/) for living-plant commerce plus care after sale; [Aesop](https://www.aesop.com/) for restraint and product/editorial hierarchy; [Planta](https://play.google.com/store/apps/details?id=com.stromming.planta) for plant-specific care timing and the distinction between guidance and current action.
- Android Developers — [Compose shared-element navigation](https://developer.android.com/develop/ui/compose/animation/shared-elements) and [predictive Back](https://developer.android.com/develop/ui/compose/system/predictive-back-setup).

No evidence these reference services' backend flows are identical to Plantupia's; no artwork or UX copied. No personal/mobile visual UAT in this pass. Design hypotheses and code-verified observations are labeled separately.

## 2 / Source-proven gaps, not aesthetic guesses

| Evidence | Present state | Why it matters |
|---|---|---|
| `StoreCatalog` / `StorePanel` | Uniform card for every product family: photo/name/category/from price/out-of-stock/text | No object-specific visual/hierarchical identity; hard to judge dimensions, variation and function |
| `StoreProductDetails` | Single long `StorePanel`, fixed-height gallery with previous/next text buttons, SKU radio list, stock count and add button | Weak photo exploration, option selection and primary purchase focus; no visually authored transitions |
| `PlantupiaCatalogPhoto` | Crop at 148dp card/245dp detail with `ContentScale.Crop` | Potential loss of full product body; image treatment needs measured product-first policy, not one crop |
| `PlantupiaStoreShop.updateQuantity` | Backend updates cart and sets `feedback = "Cart updated."` | No visible item receipt, no mini-cart, cart count, persistent success state or next-action choice |
| `PlantupiaStoreShop` | Top repeat buttons Products / Cart / My orders | Contradicts older design blueprint: compact cart count and My orders in header rather than repeated giant tabs |
| `PlantupiaShopLayoutPolicy` | 1 column below 330dp/1.3 font; 2 otherwise; incremental 8 | Technical responsive policy exists, but not a product-stage visual narrative |
| `PlantupiaDemoJourney.kt` old QA | Care subfilters Tools / Soil & Substrates / Plant Nutrition; manufacturer evidence card | Preserved old QA code, **not in real commercial store** |
| `CommerceProduct`, `sku_json` | `kind = plant/planter/care`, SKU option_label/price, photos; no commercial subcategory/manufacturer-label or use/cautions fields | Restoring the Care subfilters *alone* cannot work honestly: missing commercial data contract |
| Native `checkout` success | After sending genuine delivery quote request, clears form and switches to My Orders | Technically truthful but not a distinct receipt/moment; user may miss that this is **quote requested**, not order confirmed/paid |
| B2B `PlantupiaBusinessInquiryScreen` | Text `Business request submitted` and refresh | Outcome has little hierarchy and no authored post-submit route |
| `CareGuidePanel` | Collapsible 92dp opener → nested topic tiles → an expanded reading below the same row; additional legacy/provenance drawers | Every layer is individually functional; together they create a prolonged accordion maze in Plant Detail |
| `PlantupiaClientSection` | Overview/Care/History anchored LazyRow, then Record care, guide, inspection, AI, Team, settings, Circle | Main card stacks more duties than a single clear "open a plant" intention |
| `ContactStudio` | On ACK sets reference/feedback and clears message, but keeps form visible and disables submit | No distinct, accessible thank-you scene containing reference, meaning and next steps |
| `SpaceBrief` | Old copy-only client-side outline explicitly says nothing sent | Legacy study must not be mistaken for the active ContactStudio enquiry route |

**Important:** Inline Care Guide opening is *not* literally a separate navigation screen in current code. Perceived page fragmentation results from nested expansion, long vertical flow, repeated cards, and disconnected information hierarchy. Do not misdiagnose navigation based only on user impression.

## 3 / Principles — a coherent Plantupia purchase, not a cosmetic Shopify clone

**Proposed thesis (requires owner review):** A living object is chosen for a place, then enters a relationship over time. Shop should feel like an architectural/botanical collection whose items have commercial truth. It can borrow universal commerce clarity while inventing Plantupia's composition, use of field notes, object portraits and meaningful motion.

- **One visual language** across Shop, Plant Detail, Care, and form-result; deliberate hierarchy, measured whitespace, no endless identical full-bleed cards and giant repeated titles.
- **Product-specific truth:** Plants: actual size, verified source/variety, placement/light if reviewed, optional contextual image; Planters: actual dimensions/material/finish/compatibility if supplied; Care products: function, usage limitations, trusted manufacturer evidence and safety. No automatic guess from product titles.
- **One deliberate CTA per phase:** discover → inspect → select → add → review → quote → accept/track; the heading/buttons must clearly communicate the current stage.
- **Motion is causal:** only animate saved/add/submit as success *after server acknowledgement*. Loading, error, offline/retry, double taps and reduced motion are first-class states.
- **The app is not the website:** the cinematic website may have editorial chapter transitions; Android needs tactile, short and trustworthy microinteractions. Share brand/material language, not layouts.
- **Never conflate:** general guide vs personalized plant, viewing guide vs recording care, add-to-cart vs reserved stock, shipping quote vs confirmed order, transfer reference vs verified payment, RFQ vs installation contract, message received vs customer support response.

## 4 / Proposed first storyboard, end-to-end (behavior not implementation)

1. **Entry / Shop**: direct first useful content, contextual hero only if genuinely helpful, discoverable All / Plants / Pots / Care, clear cart quantity and accessible Orders. Preserve browsing position when returning.
2. **Discovery / item card**: actual full object in a consistent media field, name, concise reviewed differentiator, starting price & variant summary, explicit low-stock/unknown state; tap opens real product. No fictional plant-health scoring or beautiful-but-fake before/after.
3. **Product / inspection**: photo-first artwork. Swipe + thumbnail/accessibility control + full-screen zoom and proper `ContentScale.Fit` where preserving whole product matters. Product-specific information hierarchy; a clear chosen SKU, current price and availability.
4. **Variant choice**: design based on real structured attributes; do not derive color/material/size from `option_label` silently. Price and visual update only with proper SKU/photo association, clear unavailable alternatives. Keep selected option on return.
5. **Primary Add**: show intent/pending, prevent duplicate taps, await server truth. After ACK, increment persistent cart counter, change add state, show contextual compact confirmation / mini-cart with chosen item, variant, quantity, `View cart` and `Continue exploring`. Error keeps selection and clearly offers retry.
6. **Cart**: photo & exact selected SKU; quantity editor with stock feedback and recalculated server price; removed item has reversible UI intent if operation supports safe undo; explicit subtotal and "Delivery quote comes next; payment not collected".
7. **Delivery details**: staged recipient/contact + real MapLibre pin + delivery notes + payment preference. Field-level validation next to field, persistent draft across back/navigation. Do not imply cost finalized yet.
8. **Quote request submitted**: dedicated receipt scene tied to genuine `commerce/orders/` ID: **"Delivery quote requested"**, reference, exact item subtotal, what the team will review, CTA `View request` / `Back to shop`. Never say "Order placed / Paid".
9. **Quote received**: My Orders exposes real fee + total + expiry/version; customer confirms exact quote, then new separate **"Order confirmed / Stock reserved"** state; payment remains independent. Follow-up honest timeline.
10. **Delivery & continuing care**: staff-evidenced shipment/delivery, actual order status, physically delivered plant units only where verified, link from order to My Plant identity and first real care action; no synthetic QR ownership generated from SKU.
11. **B2B**: separate quotation request receipt with ID and nonbinding next steps; never force it through the B2C cart or declare a project created.
12. **Empty/error/offline**: product absent, all sold out, no image, API 401/409/429/503, stale cart, revision conflict, network timeout after mutation, screen reader/large fonts, account swap. Must be storyboarded to parity, not handled by one generic toast.

## 5 / Care: don't design a decorative encyclopedia

**Care Guide should become an authored, source-aware field reading within Plant Detail.** The primary plant screen should answer: Which actual plant? What is known/verified? What might need checking now? What did I actually do? What changed over time?

- Entry surface (real plant image/name/placement/identity) + one current genuine action if due + small "Guide for this type" entry.
- When opening a guide, user remains anchored to the same plant. Prefer one focused reading surface that reveals quick summary, topic choice and the selected full source-backed text, rather than expanding arbitrary nested cards inside an already long record screen.
- Water/light/soil/humidity/nutrition/etc are **not** sensors or prescriptions. Only show reviewed nonempty topics, identity basis, context caveat, source/reviewer/version, and unavailable/missing source states.
- Connect a **verified approved inspection** to a meaningful `Check now` action; reading a topic must not log a care event. An actual action recorded by the user gets its own separate verified success and Living History entry.
- If a user-selected species is unverified, convey source level without shaming them. If no guide published, give an honest and useful next step (personal observation/check-in or company help).
- Preserve photo/observed history, current source version, group/individual plant identities, membership permissions, and documented human confirmation.

## 6 / Web enquiry: a real ending, not a notification under the button

After the Next.js → Django endpoint responds with a real `reference`:
- Form gives way to a **distinct confirmation composition** (not a generic success toast) acknowledging the message was received, with reference, an accurate statement that it lives in private Company inbox, and next actions "Explore spaces" / "Return to the experience" / "Send another enquiry".
- The visual transition should be restrained and follow real ACK. No automatic unannounced redirect, no invented email confirmation, no promise of response time, no duplicate submission.
- Keep form input if ambiguous timeout/error and preserve same idempotency key; if confirmed, render receipt rather than empty message field + disabled button.
- Focus main confirmation heading and announce status accessibly. Honour reduced motion.
- **Separate** the historical copy-only `SpaceBrief` study from the current `ContactStudio` live enquiry; no surprise two-form world.

## 7 / Visual and motion exploration (candidate, not a signed-off design)

A visual language of **actual product portrait, quiet archival field notes, architectural grid and light**. Avoid generic plant emojis, mock luxury, over-rounded card piles, fake pot imagery and aggressive sale UI. Use an asymmetrical/bounded collection layout while retaining scanability, readable product hierarchy and legible mobile views.

- *Discovery* = calm catalogue / object studies; labels are small, prices truthful; imagery as primary evidence.
- *Detail* = a gallery/selection table with one continuous layout; selected SKU and add action visually remain together. Hero may have an authored material boundary rather than a giant generic `StorePanel`.
- *Add* = subtle continuity of object into a small cart tray/badge **only after acknowledgement**; haptic if appropriate, no unnecessary wait or forced redirect.
- *Care* = a field manual *for a particular plant*, consistent with the physical object's visual identity, not nine colorful cards competing with one another.
- *Outcome* = a signed/dated receipt-like object, not an empty form plus "success" sentence. Website may be more editorial; app more tactile.
- **Prototyping gate:** critique 3 alternatives for each meaningful interaction on actual 360/390dp & desktop, choose the best one in context, then unify all. Do not create 3 production forks.

## 8 / Data contract and technical requirements before art implementation

- Extend **canonical commercial** `CommerceProduct` / SKU data model (and importer/editor/API/review readiness) with source-verified care subcategory and manufacturer evidence where actual business wants to sell such items. Keep old QA model separate. Migration is **proposal only**, not executed.
- For rich variants (material/size/finish) evaluate explicit structured attributes; a human-typed `option_label` isn't reliable semantic data. Do not fabricate unavailable options.
- New backend fields must obey content approval/publication, media rights, missing metadata, and existing order snapshots. Distinct SKU + stock reservations remain source truth.
- Add UX state models for `Adding/Persisted/Failed`, `QuoteRequested/Quoted/Confirmed`, `MessageSending/Acknowledged/UnknownFailure`. Do not confuse a pending network call with success.
- Keep user browsing state across product/detail/cart and Android Back, process death where practical; respect account switching and refresh.
- Real device screenshots, touch/keyboard/TalkBack test matrix; media loading states, 320/360/390/430dp, 200% fonts and dynamic theming/RTL.

## 9 / What is approved now?

Only **investigation and critique**. Not yet approved: a particular composition, full visual grammar, animation timing, business data migration, modified checkout permissions or any production release.

**First owner review should be one chapter at a time**: actual Shop Home and collection → product detail → mini-cart/add → cart and quote → receipt → Plant Detail/Care Guide → web enquiry result. For each: read actual code + visual screenshots, current vs reference vs original proposal, draw interactive storyboards, test real behavior, revise until accepted. Then and only then code the integrated native/website experience on a safe review branch.

## 10 / Acceptance is not "unit tests passed"

- Full working journey with two customers and staff on isolated QA Postgres, verified deployment SHA and actual phone.
- Exact customer truth preserved for photos/SKU/cart/quote/stock/payments/QR/guide versions.
- User understands current state and next action in under one scan; all errors/success meaningful and specific.
- Test honest loading, interruption, 409 quote revision, 401 account expired, missing media, partial data, no botanical guide, slow network and repeated taps.
- Visual critique from physical Android screenshots plus desktop/mobile web, no invented screenshot-based claims.

No code/runtime mutations performed in this research branch.
