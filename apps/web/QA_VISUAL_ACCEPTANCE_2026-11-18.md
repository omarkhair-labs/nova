# Plantupia — Commerce visual acceptance, pass 01
**Date:** 2026-11-18  
**Review status:** In progress; no public-release or final visual sign-off.  
**Baseline:** QA Draft PR #253, commit `425800c113b44017bdf086766805e7cb2897bea2`.  
**Surface inspected:** public website `/shop` only — not the native Android Shop, Cart, Checkout or Orders.

## What was actually checked
- A local, already-built Next.js preview on port 3017 returned HTTP 200.
- A browser automation run used Puppeteer's real responsive viewport settings at CSS widths **320, 360, 390, 430, 768 and 1440 px**.
- Browser document width equaled viewport width on all six sizes: no **body-level** horizontal overflow.
- No uncaught page JavaScript exceptions in those runs.
- The filter strip's overflowing items live inside a deliberately horizontally scrollable area, not a broken page width.
- Mobile entry composition, collection, catalog-unavailable state, and continuity section were visually inspected from local captures.
- An earlier Chrome CLI `--window-size=390` capture was misleading due to its minimum inner viewport; it is **not** acceptance evidence. Use the Puppeteer viewport captures instead.

## Changes in this first review slice
- Enlarged the web brand and contact click areas to at least **44px high**, with a slightly more readable mobile contact label.
- Increased the mobile chapter-menu trigger from **43px to 44px**.
- Tightened public collection copy and made the empty/unavailable state messages customer-facing without changing their factual distinction.
- **No change** to public SKU gating, product imagery, shipping, price calculation, cart/order behavior, authentication, or private customer data.

## Verification status
| Check | Evidence | Result |
| --- | --- | --- |
| Existing website, six widths | `D:\\Plantupia-QA-2026-11-18\\temp\\shop-audit.json` | Pass for HTTP 200, document width, JS errors |
| Existing menu and filter interaction | Pre-change Puppeteer smoke at six widths | Pass |
| Web TypeScript after copy/CSS edits | `npm.cmd run typecheck` | Pass |
| Rebuilt runtime after edits | The running site is `next start`, serving the **previous** build | **Pending** — do not claim new touch sizes visible yet |
| Real public product photo/variants | Local QA catalog is unavailable | **Pending** — no fictional SKU substitutes |
| Native Android screen acceptance | No device connected to ADB; emulator not started with under 1 GB free RAM | **Pending** |
| Backend first-photo upload | New QA route not deployed to connected backend in prior release audit | **Pending** |

## Next acceptance passes
1. Build/serve this web slice in an isolated QA environment and repeat the six-width smoke with tap-target geometry **after** the new bundle loads.
2. Test collection with **real approved** SKUs and images in a connected QA backend; review crop, aspect ratio, product name, variant, price and no-photo fallback.
3. Inspect latest QA Android APK on a device/emulator at 320/360/390/430dp plus large font (200%), across Collection → Product Detail → Cart → Checkout → Orders; capture visual evidence, talkback/focus and keyboard clearance.
4. Test operational states (empty, loading, offline, delivery quote and error/retry) without faking payment, stock, shipping events or plant-care records.
5. Capture a before/after screen register; only mark each chapter visually accepted after an actual device review.

## Product and editorial guardrails
This is an authored Plantupia commerce room, not a generic store clone. Preserve the separation between public discovery and authenticated app checkout. Do not fabricate photos, catalog records, stock, care outcomes or company-approved delivery prices. Keep production, `master` and billing unchanged.
