# Plantupia — Integrated QA / Acceptance Audit
**Date:** 2026-11-17 (Cairo)  
**Scope:** Public website + native Android + Django company workspace + commerce + care/QR + deployment boundaries.  
**Reviewed integration commit:** `caba69e8c0aa6d05e6729657876b5620fe68e399` on `feature/plantupia-unified-experience-20261028` (**PR #253, Draft**).  
**Public website deployment commit:** `822581c739c3c0eae4f9dd157921734bd2fe1619`, preview-only, `feature/plantupia-public-living-website-20261113`.  
**Release classification:** QA/Design preview, **NOT an authorized commercial production release**.

## 1. Evidence vocabulary

- **LIVE / READ-ONLY PASS:** observed on the currently reachable deployment via authorized HTTP extraction, without signing in or changing user data.
- **CI PASS:** tests/build/migration checks succeeded on a named commit; no claim about a real user device or deployment.
- **CODE PRESENT:** source route/UI/test exists, but authenticated live or device acceptance was not completed.
- **GATED / UNKNOWN:** deliberately disabled by default, or runtime configuration/operator evidence not read.
- **BLOCKED:** cannot be called complete without the required external evidence or approval.

Never infer production readiness merely from the existence of a route, a passing unit test, a draft PR, an API response or a QA screenshot.

## 2. Source of truth / branch convergence

- `#253` is the only consolidated native + backend + company + website **QA branch**, remains Draft and unmerged into `master`.
- `#300` was merged into `#253` as **`caba69e8`**. Its changed scope was 63 files exclusively within `apps/web/`, Plantupia web docs and web CI. No Android, commerce DB migration or backend source was replaced by this integration.
- Website iteration PRs **#303 and #309–#313** are merged into website branch and now part of `#253` via #300. Android/company changes **#305–#308** are already in #253.
- Independent courtyard experiment `#302` is not the canonical website and remains a separate draft. Numerous older stacked Plantupia PRs remain open as historical integration/review branches: do not count or merge them blindly as missing features.
- Vercel keeps deploying the **website-specific branch**, not the entire #253 Android/backend branch. All six canonical web routes exist in the same Vercel project.
- The sole QA preview address is **https://plantupia-website-qa.vercel.app**. The old `plantupia-public-preview.vercel.app` alias now returns HTTP **307** redirect to this address. Unversioned build URLs and legacy prototype routes are **not** the public source of truth.

## 3. Live checks actually completed

| Surface | Observation | Classification |
|---|---|---|
| Vercel canonical deployment | READY, SHA `822581c7`, fixed QA alias | LIVE PASS |
| `/`, `/spaces`, `/studio`, `/living-systems`, `/about`, `/start-a-space` | All six returned HTTP 200; rendered titles and index markup are present | LIVE PASS (HTTP/SSR only) |
| World Index and Passage | All six have server-rendered plus/index strings; five narrative passage components are present across relevant chapters | LIVE PASS (markup); interaction NOT retested |
| SEO/release | Pages declare `noindex` | LIVE PASS |
| Website enquiry form | Form renders with enabled send control in the configured preview | LIVE PASS (rendering only) |
| `GET /api/website/enquiries` | HTTP 405 as expected for POST-only handler | LIVE PASS |
| Django `GET /api/v1/health/` | JSON `{"status":"ok","service":"nova-api"}` | LIVE PASS (process reachable, not a deployment SHA) |
| Django staff `/company/login/` | Staff sign-in page reachable | LIVE PASS |
| Django `/company/` and `/company/website/inbox/` | Authentication required for anonymous browser | LIVE PASS (security boundary) |
| Customer `GET /api/v1/plantupia/commerce/catalog/` | Anonymous request receives HTTP 401 | LIVE PASS (authentication boundary; no real customer catalogue assertion) |
| Website enquiry actual delivery | Earlier user-performed message was observed in staff inbox; not repeated after latest integration | HISTORICAL MANUAL QA, not current fresh E2E |
| Company logged-in UI and Android device | No authorized session or physical-device browser acceptance in this audit | NOT TESTED |

**Limitation:** automated interactive visual browser run did not execute due connector credits. HTTP fetch and CI do **not** prove hover, keyboard/Escape, mobile cropping or hydration behavior.

## 4. Current CI — exact SHA evidence

- Before combining branches, `0888d0e5` (#253) passed `Nova CI`: Backend tests and Android build (unit tests, lint, debug/QA/emulator/USB artifacts, instrumentation APK assembly, release bundle assembly).
- Website head `822581c7` passed `Plantupia Public Web CI` (typecheck, Next.js build, HTTP route smoke) **and** `Nova CI` (Backend and Android).
- Consolidated commit `caba69e8`: `Plantupia Public Web CI` **PASS** (run 44154278562). `Nova CI` run **44154278325 was still in progress at audit capture**; verify its latest conclusion before declaring the merged exact commit green.
- CI passes do not mean tests were run on a physical phone or on the deployed Coolify container.

Run links:  
https://github.com/omarkhair-labs/nova/actions/runs/44154278562  
https://github.com/omarkhair-labs/nova/actions/runs/44154278325

## 5. Capabilities by product surface

| Capability | Source/code | Current acceptance |
|---|---|---|
| Cinematic home, World Index (+), chapter transition | `apps/web/app/CinematicExperience.tsx`, `SiteIndex.tsx`, `ChapterThreshold.tsx` | CI PASS; HTTP PASS; actual touch/hover/Escape/browser visuals pending |
| Six public company chapters | `apps/web/app/{page,spaces,studio,living-systems,about,start-a-space}` | All six HTTP 200 |
| Spaces field notes → editable enquiry | `SpaceExplorer.tsx`, `ContactStudio.tsx` | CI PASS; interactive cross-page browser QA pending |
| Website form → private staff inbox | Next `api/website/enquiries/route.ts` → Django `website_inbox.py` | Backend validation/role/idempotency tests PASS; one earlier actual delivery; current authenticated end-to-end not rerun |
| Catalogue grouped SKUs, photos, stock ledger, Excel+ZIP create-only import | `commerce_company.py`, `commerce_bulk_import.py` | Code + CI PASS; real warehouse/media persistence and live duplicate/import QA pending |
| Single Android Shop and saved SKU cart | `PlantupiaStoreShop.kt`, `commerce_storefront.py` | CI PASS, deployed Android real account not accepted in this audit |
| Shipping quote → explicit customer acceptance → atomic stock hold | `commerce_orders.py`, API, Company `commerce_order_company.py` | Backend acceptance tests PASS, no live real-actor E2E confirmation |
| Picking/dispatch/delivery proof, manual transfer/COD evidence | `commerce_fulfillment.py`, `commerce_payments.py` | Staff-attested code and tests; **no courier integration or automatic bank reconciliation** |
| Returns and external refund evidence | `commerce_returns.py` | Code + CI PASS, no actual refund provider/bank workflow |
| B2B inquiry, nonbinding EGP quote, project installation staging | `commerce_business_real*.py` | Code and tests; no signed contract/merchant handoff |
| QR handover, plant identity, Care Guide, Living History, Today, Circle | `plants/urls.py`, Android Plantupia feature tree | Existing integrated code/CI; real-device/two-account/on-site acceptance pending |
| Android signed update / Play Store / iOS | CI assembles artifacts; no production signing/Play approval proof | GATED; iOS outside current scope |

## 6. Flags, independent states and deliberate closures

**Default-disabled in source unless explicit runtime env is set:**
`PLANTUPIA_COMPANY_DASHBOARD_ENABLED=0`,
`PLANTUPIA_CEO_DEMO_ENABLED=0`,
`PLANTUPIA_STOREFRONT_PUBLICATION_ENABLED=0`,
`PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED=0`,
`PLANTUPIA_QA_CATALOG_ZIP_IMPORT_ENABLED=0`,
`PLANTUPIA_HANDOVER_QA_ENABLED=0` (for opt-in QA paths).
These are *source defaults*, **not readings of Coolify environment values**.

Website preview has configured Vercel keys `PLANTUPIA_WEBSITE_INBOX_ENABLED`, `..._TOKEN`, `..._BACKEND`; form rendering confirms intake enabled from the Next.js side. No secret values were retrieved or disclosed. The Django endpoint independently checks matching enabled state and token.

**Catalog states are separate:** ZIP import creates drafts; `qa_listed` is not live publication; product-data approval is not live publication; publication requires both approval and the deliberate `PLANTUPIA_STOREFRONT_PUBLICATION_ENABLED` switch. Only published reviewed SKUs show in the real customer catalogue. No stored SKU automatically establishes an installed customer `PlantUnit`.

**Commercial boundary:** genuine order/quote/reservation code is present, but it does not process gateway payments, contact a courier, verify banks automatically, or create a signed B2B contract. COD/transfer claims and independently verified records must never be conflated.

## 7. Known sample import and operational issue

The uploaded QA B02 workbook was inspected directly:
**12 synthetic products, 25 SKUs, 238 fictional opening-stock units; all rows reference warehouse code `WH-001`**. Labels explicitly state `FICTIONAL QA` and photos are AI concept illustrations, **not actual merchant inventory**.

`commerce_bulk_import.py` rejects existing `catalog_key`, SKU **or exact product name**. It is **create-only**: cannot reimport to update existing goods or add stock to the existing SKU by resending the original ZIP. A nonzero `initial_stock` needs a pre-existing **active** warehouse code and permissions for stock movements.

Previous staff screenshot displayed `Unknown or inactive stock location: WH-001`; this audit did not see an authenticated later state. Do not claim the pack imported or the warehouse was created. Use counted stock operations for existing SKUs; do not delete real products or alter merchant stock without separate evidence/authorization.

## 8. Risks and remaining checks (prioritized)

### P0 — Required before any production release
1. Exact-head **combined Nova CI** result after web merge, review all failing steps if any; new app build must be matched to deployed backend and QA image digest.
2. Fresh real Android-device end-to-end (320/360/390dp, large fonts, Arabic/English, TalkBack, weak network, login/logout/account switch, camera/map/location). Test separate customer A, customer B and staff with no cross-account leakage.
3. Real PostgreSQL QA migrations 0023–0032 + media object-store persistence, verified backups and restore, rate limiting backing store, current Coolify running commit/digest.
4. Authorized staff roles and live commerce checkout chain: draft → review → publish *only when approved* → customer cart → exact company quote → accept → hold → operator dispatch proof → finance → returns; separate B2B and physical plant evidence.
5. Merchant/legal release approval: authentic products/photos/prices/counts, geographic delivery pricing, payment/COD reconciliation, refund/return/privacy/support, invoicing and staff responsibility. No Google Play or official-domain promotion yet.

### P1 — Must verify for QA quality
1. Authenticated company catalogue/inbox at desktop and 360px; role-based view/change denied tests and actual status saves.
2. Interactive world index (+): click, hover, focus, Escape, dialog focus return, back/forward, Touch, reduced motion, iOS/Safari, no overlays or horizontal overflow at 1440/810/390/360 and short landscape.
3. Real website enquiry from correct public branch to correct isolated company environment; receipt/audit/duplicate submit/rate-limit and privacy agreement, without sending synthetic personal data to production.
4. Check active `WH-001` location and media after redeploy, and inspect catalog photo framing on staff screens. Synthetic stock must remain QA-only.
5. Validate object storage upload persistence, API authentication and authorization for both customers and staff.
6. Check the old `/study/navigation` test route remains discoverable by direct URL, though excluded from World Index; decide whether to redirect/archive for launch.

### P2 — Consistency and maintainability
1. `apps/web/README.md` and earlier standalone slice docs still describe a copy-only form, old chapter count, or earlier prototypes. Current implementation is the authority; update documentation, do not silently trust stale descriptions.
2. `commerce_storefront.py` still returns `checkout_ready: false` and an old catalog notice about orders not being activated despite a separate live quote-order API. Verify whether `checkout_ready` means online payment readiness or current ability to request quotes; align the label and contract without accidentally enabling payments.
3. Many older Draft PRs still open; categorize **merged into #253 / superseded / active independent** before archiving. Do not auto-merge or delete legacy branches.
4. Public site uses disclosed stock editorial reference photos, not authorized Plantupia client work. Arabic production version, real work archive, approved brand/legal copy and media licenses remain blocked.
5. Avoid reintroducing QA `CatalogProduct` / `DemoOrder` as a second customer Shop. Preserve historical test records separately.

## 9. Canonical acceptance order

1. Confirm post-integration CI and #253 running backend revision.
2. Confirm isolated Postgres/media/Redis, backups, flags and staff provisioning (read-only first).
3. Browser acceptance: website all chapters, + dialog, field notes, enquiry receipt, company inbox and catalogue.
4. Android QA APK from exact head, two customers and staff, full real SKU/order/QR/care journey, network/device and security cases.
5. Real data/legal/commercial sign-off.
6. Only then plan a separate approved production release, official domain and Play Store rollout.

**No assertion that “everything is working” until these gates have evidence.** This audit consolidates source/code, CI and observed runtime; it does not silently turn QA into production.
