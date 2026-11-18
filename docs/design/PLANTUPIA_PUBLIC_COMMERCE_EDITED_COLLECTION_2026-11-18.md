# Plantupia / Public Commerce · Edited Collection
**18 Nov 2026 · QA / stacked on One Plant PR #324 · not a live sales launch**

## Product decision
The company website already has a fully authored six-chapter story; Shop is a small additional room, not a forced seventh cinematic chapter. The visitor sees a subtle introduction after the company story and a small sidepath within the World Index. The direct route `/shop` is an editorial, curated product collection, not a disposable blank commerce dashboard.

Real-world businesses often have one operational product catalogue with multiple sales channels. A company public website can expose **reviewed and explicitly published** products without reimplementing customer login, cart, delivery pricing and payment from a mobile API. The Android customer app remains the authenticated order channel; it is **not correct** to call the website an integrated web checkout until server-authorized website-specific cart/session/address/order idempotency is designed.

## Backend integration / no duplicative shop data
- Existing `CommerceProduct` and `CommerceSKU` are the **single source** for website and Android.
- Publication requires catalog review status `approved` plus `storefront_published_at` set by authorized Company operation. A draft is never public, even if it has a photo and price.
- Public read-only endpoint: `GET /api/v1/plantupia/commerce/public/catalog/`. Max 36 published EGP SKUs; product name, reviewed description, kind, variant, price and current in-stock Boolean. No exact warehouse count, customer, cart, staff, private planted inventory, fake QA/SKU or commercial fulfillment mutation.
- Image endpoint: `GET /api/v1/plantupia/commerce/public/products/<uuid>/cover/`. One published product cover only, max 4 MB, allowed verified JPEG/PNG/WebP content; authless intentionally only for published products. On withdrawal the endpoint returns 404. No private media paths or storage credentials exposed.
- The existing authenticated `commerce/catalog/`, cart and `commerce/orders/direct/` remain unchanged. Do not forward session tokens from browser, impersonate a user or create guest orders in this slice.
- The website loads the public catalog server-side using `PLANTUPIA_PUBLIC_COMMERCE_BACKEND` if configured, otherwise the existing `PLANTUPIA_WEBSITE_INBOX_BACKEND`. Only a validated HTTPS origin is accepted, no cookies, credentials or browser access to that origin. The Next.js cover route proxies only published UUID covers. If backend not configured, unreachable or malformed, the page **explicitly says collection unavailable**, never displays invented product tiles/prices.

## Visual composition / product design
- An authored botanical geometry is prominently marked **Concept drawing, not a product**. Do not use it as a purported purchased product photo.
- Palette: deep Plantupia forest ink, warm uncoated paper, muted botanical sage, hairline borders and real source photos; original geometry and typography, not a generic Shopify/Baymard card clone.
- Asymmetric first viewport, edited-collection narrative, compact category chips `All objects / Living plants / Planters / Care objects`, real SKU dossiers with variants and current available/unavailable state, product CTA to the existing authenticated website enquiry channel.
- A `ShopBridge` is placed between the company story and existing Chapter Threshold. The six original chapter numbers, routes, state scenes and scroll interactions are unmodified.
- One product CTA to `/start-a-space?topic=general&item=<name>` prepopulates a **visitor-editable question** about that product. This sends through already-reviewed website inbox only if that service is configured. It does **not** reserve stock or create an order.
- No public product records? Show honest editorial empty state. No placeholders masquerading as real stock. The original backend sample products are test fixtures, never real offers.

## QA gates
1. Django tests: anonymous catalog initially empty; review alone never publishes; after explicit publication exact variants/EGP prices are visible; exact stock, staff/customer and draft records remain private; POST forbidden; image from actual approved file only, 404 after withdrawal.
2. Next.js `npm run typecheck`, `npm run build`, route smoke on `/shop`, homepage bridge and World Index sidepath. Production build must work with **no backend env** and tell the visitor it cannot verify a catalog.
3. Verify live deployed Vercel branch and check site visually at 320 / 390 / 768 / 1440 widths, image failures, keyboard and 200% text, JS disabled links, state changes, reduced-motion and accessible buttons.
4. With a company-approved **disposable QA** product and stock fixture only: independently verify backend public catalog, image proxy, no leaks, product contact enquiry reaching authorized Company Inbox.
5. Before public release: real catalog assets, publication review, payment/consumer terms, shipping geography and merchant channel approval. Vercel preview URL is **not** the official site and backend must not be accidentally deployed to production.

## Deployment separation
- GitHub QA branch / Vercel `plantupia-public-preview` serves the **Next.js site**. Vercel deployment alone does not publish Django API or run schema migrations.
- Django Company Platform/Commerce API deployment must have an identified owner, a protected QA target and real migrations reviewed and applied; do not switch Coolify production by guessing an endpoint or reusing Nova production credentials.
- Native APK from GitHub Actions `assembleQa` is separate. CI emits **three different artifacts**; the USB-localhost and emulator-localhost builds require the corresponding local server setup. Do not call an offline APK a connected customer preview.

## Real-world references / mechanisms
- Shopify official Admin/Storefront publishing: product publication is a channel-level choice, not implied by draft creation — https://help.shopify.com/en/manual/products/details/product-status and https://help.shopify.com/en/manual/online-store/themes/customizing-themes/add-product-recommendations
- NN/g Progressive Disclosure: keep the visitor's current choice and truthful action visible — https://www.nngroup.com/articles/progressive-disclosure/
- Baymard checkout research: price and delivery must be clear before confirming payment, hence no partial/false checkout on this web surface — https://baymard.com/blog/checkout-usability
