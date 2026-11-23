# Plantupia / Stable-Link Integration Baseline — 23 Nov 2026

**Purpose:** carry the latest *website* design into the existing working `plantupia-public-preview.vercel.app` deployment without breaking the original Company Inbox, published catalog contract, authenticated customer-app handoff or image rights. This is a technical baseline, **not** public-release approval.

## Exact source and deployment identities
- Stable Vercel alias: `https://plantupia-public-preview.vercel.app/` -> production deployment `dpl_F8oHRivT23nZtxevKHvpCWKjdXAK`, created 19 Nov 2026, READY.
- Latest manually photo-bundled visual Preview previously inspected: `https://plantupia-public-preview-du00ehney-omarkhair70-droids-projects.vercel.app/` -> `dpl_GxcdcxuS5NyP53ufcQt9QjGEVA1A`, created 22 Nov, READY. It is **not** the latest GitHub source.
- Both deployment records belong to the same Vercel project, `plantupia-public-preview`, not independent products.
- Source of the design and regression changes: `omarkhair-labs/nova`, `design/plantupia-web-refinement-20261122` under `apps/web`.
- Repository default is **`master`**, not `main`. Comparison with the design branch shows 1,647 commits ahead and no reverse commits. Do not blindly merge this entire monorepo branch into master to ship only the website.

## Precisely what was preserved
1. **Old live Inbox contract:** `POST /api/website/enquiries` on the older production deployment checked `PLANTUPIA_WEBSITE_INBOX_ENABLED`, HTTPS backend origin, a server-only token and same-origin requests. It did **not** require public approval for new private project photography.
2. **Regression found in new visual code:** the revised website routed both the contact page button and its POST handler through `publicationApproved`. Consequently it could silently disable previously active production messaging until unrelated image rights were signed.
3. **Fix applied:** production Inbox availability is once again controlled by **its original independent operational switches**, while protected project photos remain behind `PLANTUPIA_PUBLICATION_APPROVED` and Preview inquiry submission stays off. Both contact UI and POST handler share `websiteInboxOperational`. Existing server-side validation, size limit, same-origin HTTPS, idempotent submission ID and backend credentials are retained.
4. **Shop unchanged in principle:** both old and new source implement `getPublicCatalog` reading only reviewed/published SKUs from `/api/v1/plantupia/commerce/public/catalog/`, with optional `PLANTUPIA_PUBLIC_COMMERCE_BACKEND` and fallback to the website Inbox backend. Empty/unavailable still show honest UI; no fake web checkout or stock reservation. Current Vercel environment variable *names* include separate Inbox backend/token/enable controls in Production and Preview; no dedicated Commerce backend variable was listed, so verify the actual same-backend catalog before release.
5. **Public Shop copy corrected:** on an unapproved public build without the private material library, the page no longer falsely says that review-only source photography appears below. Links to Studio/status remain valid.

## Current regression coverage
- `apps/web/scripts/check-release-modes.mjs`: six synthetic, non-network release modes prove photos and Inbox are independent. Never sends real customer data.
- `apps/web/scripts/closure-smoke.mjs`: 41 route/navigation/publication checks in non-publication mode.
- `apps/web/tests/closure-ui.spec.mjs`: 14 desktop/phone Chrome visitor acceptance tests.
- GitHub Actions workflow `plantupia-public-web.yml` now executes all three alongside Next.js typecheck/build. Record the **latest exact-commit** green run; older green runs do not cover later source edits.

## Only safe route to the ONE fixed production URL
1. Review the latest source changes for `apps/web`, and record a website-only integration baseline; do not wholesale merge the Android/backend monorepo into `master`.
2. Reconstruct an exact photo-bundled candidate from *authorized* review/source assets, retaining only company-approved public imagery and editorial copy. The Git repo alone does not contain all protected preview images.
3. Stage production configuration with **no domain alias promotion**. Preserve existing Inbox env names and scoped values, and verify product feed + contact UI in the exact staged artifact. Do not invent credentials or replace them with dummy values.
4. Run visual/keyboard/mobile/performance tests plus synthetic and **authorized real** enquiries, including the company staff Inbox receipt, access control and safe retry. Only send test messages with agreed company authorization; no external messages just for a build smoke.
5. Confirm domain ownership, publication rights/manifest and exact product claims. Record Ahmed/Plantupia Go/No-Go.
6. Promote the verified production-configured deployment to the **same fixed alias**, with prior production deployment ID recorded for rollback. One public website, Preview URLs for QA only.

**Current boundary:** code regression is fixed and testable, but a public alias switch is NOT authorized by the available company publication evidence. The old public alias has intentionally been left untouched.

## Verified checkpoint after the integration guard
- Website code head `2c3bf2d7b2b41221380f6a6f7d824e89648257fe`: [all GitHub Actions green](https://github.com/omarkhair-labs/nova/actions/runs/45110424694).
- **6/6** synthetic release-gate modes passed, including production Inbox enabled with photo-publication approval disabled.
- **41/41** public-route, security and publication checks passed.
- **14/14** Chrome visitor acceptance checks passed on mobile and desktop widths.
- The stable public alias was rechecked afterwards and still points to `dpl_F8oHRivT23nZtxevKHvpCWKjdXAK` (READY). No site promotion, backend configuration mutation or real external enquiry was performed.
- These test results verify new-code regression safety under synthetic configurations. They do not establish that the separate operational backend is currently accepting real enquiries, nor that the 23 Nov source contains all approved source-photo files needed for a new public release.

## Follow-on checkpoint — isolated real HTTP contract / 23 November 2026
- Added `apps/web/scripts/isolated-company-integration.mjs`. The actual built Next.js website is started with a *disposable local HTTPS* Company API. This does not contact real Plantupia production, store visitor data, or send an email.
- The mock uses the original public `POST /api/website/enquiries` route: independently enabled production Inbox despite `PLANTUPIA_PUBLICATION_APPROVED=0`, successful 201+reference, idempotent replay, cross-origin 403, and continued 404s for unpublished case images.
- The mock also serves a valid published-SKU catalog response and a seeded QA fixture. The website accepts the valid item and filters the QA fixture; real production SKU publication was **not** asserted.
- Actual GitHub Actions run for **website code commit `2cf3debfacff8aa16f3519ec1ab9be3a5eca2dd3`** completed successfully: [view run 45120490802](https://github.com/omarkhair-labs/nova/actions/runs/45120490802). Checks: **6/6** release-mode cases, **8/8** isolated HTTPS integration checkpoints, **41/41** non-publication smoke checks, **14/14** browser acceptance checks, along with TypeScript and production build.
- Still open: approved source image files not bundled in Git, explicit publication approval, actual-company Inbox message receipt/roles, real published SKU feed and app channel, physical device performance/accessibility, exact production-candidate retest, and company-approved public alias promotion. There is NO new Vercel release based on this code yet.
