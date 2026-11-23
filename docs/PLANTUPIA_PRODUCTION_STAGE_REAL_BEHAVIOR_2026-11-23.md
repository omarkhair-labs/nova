# Plantupia — Production-configured comparison and release boundary
Date: 23 November 2026 | Status: staged and verified, NOT promoted to the fixed domain

## One existing site, two deliberately separate deployment modes
- Same Vercel project: `plantupia-public-preview` (`prj_yM9FKSN5mLkVD1I3WVjATmwCceDI`).
- Fixed domain `https://plantupia-public-preview.vercel.app` still routes to the old READY production deployment `dpl_F8oHRivT23nZtxevKHvpCWKjdXAK` created 19 November. It was NOT changed.
- New authenticated visual Preview: `dpl_4J1kGpmwR4DMD6vif3AyxGVm1Qe7`, `https://plantupia-public-preview-lt55aa9bo-omarkhair70-droids-projects.vercel.app` built 23 November, READY. Prepared from website branch at commit `ce7df66`; 119 private-review source files staged locally, including 116 WebP source images present in Vercel deployment tree. GET `/`, `/spaces`, `/spaces/projects/pixels-mall`, `/studio`, `/shop`, `/start-a-space` and Pixels image P08/P11 returned HTTP 200 under authenticated Vercel CLI. Unauthenticated request returned 302 authentication redirect; `POST /api/website/enquiries` returned 503, intentionally disabled for Preview. **This URL is not the public launch.**
- Production-configured staging candidate `dpl_DCdnEcaFs1KQxmGT2MWgtJaqP7qJ`, `https://plantupia-public-preview-lfzjj1g5i-omarkhair70-droids-projects.vercel.app`, built with `vercel deploy --prod --skip-domain` on the SAME Vercel project, READY and with zero aliases. The source bundle deliberately excludes all private-review image files; public-source permissions are not yet signed. `/`, `/spaces`, `/studio`, `/shop`, `/start-a-space` returned 200; unpublished `/spaces/projects/pixels-mall` and private Pixels P08 returned 404, as intended without publication approval.

## A verified real difference between the fixed live domain and the production-staged replacement
- Authenticated GET of the old stable deployment `/shop` rendered **23 product cards**. Exact names were parsed from its real HTML, and EVERY card carried an explicit `FICTIONAL QA` or `FICTIONAL QA B02` prefix (plants, vessels and maintenance accessories). Those are test fixture records, NOT evidence of 23 genuine published products.
- Authenticated GET of the production-configured staged `/shop` rendered **0 public product cards** and the honest `No products are published yet.` status. The newer `getPublicCatalog()` deliberately filters QA fixtures. This is a correct production safety behavior, not a broken network connection. A truly approved published SKU must be inserted and confirmed by company operations before demonstrating a real store.
- Authenticated GET of `/start-a-space` on BOTH old stable and new production-configured staging rendered the same operationally enabled submit state (no disabled HTML attribute, informational text says the company inbox will receive the message). This confirms production environment flags are correctly inherited and the new independent Inbox release gate preserved old UI availability.
- This inspection made **NO real POST to the company backend**, therefore company-side receipt, rights, deduplication under live conditions and staff permissions are not re-certified by this comparison. Prior historical tests must not be confused with a fresh live receipt.
- In the protected visual Preview, the Contact button remains disabled and private project photos can be viewed by authorized reviewers. Distinct Preview vs Production behavior is intentional.

## Automated tests for the same latest web code
- Verified GitHub workflow run: https://github.com/omarkhair-labs/nova/actions/runs/45120490802 (web source commit `2cf3debfacff8aa16f3519ec1ab9be3a5eca2dd3`; later doc-only commits contain no web code modifications).
- TypeScript + Next production build passed.
- `6/6` independent release modes passed.
- `8/8` actual Next HTTP route paths exercised against disposable local HTTPS company-backend mock: submit, replay idempotency, origin guard, published item acceptance, QA fixture filter.
- `41/41` chapter/publication smoke and `14/14` Chrome phone/desktop acceptance tests passed.

## What remains before promoting to the SAME stable URL
1. Plantupia provides or authorizes exactly which source-proposal photos and client names may be publicly visible, with a signed asset manifest. Copy/images cannot be assumed approved merely because available for private review.
2. A company owner explicitly publishes a *real* reviewed SKU in backend, so website shows genuine product name/photo/price/stock. Keep `FICTIONAL QA` samples hidden.
3. Controlled company-authorized message test: confirm actual receipt/reference in staff Inbox and actual authorized roles; record outcome without putting test customer data in release logs.
4. Assemble a production-configured build that includes **only approved images and real products**, verify exact artifact across mobile/desktop, performance/privacy/SEO/accessibility, then company Go/No-Go.
5. Promote the tested production deployment using the existing fixed Vercel alias; retain original deployment ID as rollback reference. Do not publish the currently photo-incomplete staging candidate or the review Preview as the final site.

**Decision:** Do not promote today. The fixed domain is still the existing working site. Latest code is tested, latest design is available for protected review, and the operational Inbox switch survives in production staging. The only legitimate unresolved product content is real approved SKU publication and private image/public-copy rights, plus re-confirming live Inbox receipt.
