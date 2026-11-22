# Plantupia / Production release readiness — 2026-11-22

Status: Production build staged, NOT publicly promoted. This is the real apps/web website, not a new lab.

## Verified deployment state
- Vercel project: plantupia-public-preview.
- Default project domain still maps to older production deployment dpl_F8oHRivT23nZtxevKHvpCWKjdXAK.
- Production-configuration candidate: dpl_8GSmEH95xK6wXcDSBoJoMJtYZzqi, READY, built with --prod --skip-domain, zero aliases.
- Authenticated stage smoke: Home, Spaces, Shop and Contact returned 200. Proposal pages and protected review images returned 404 intentionally without publication approval. Robots disallows indexing. Contact form explicitly disabled.
- A separate authenticated Preview displays 96 source review assets. REVIEW USE IS NOT PUBLICATION AUTHORIZATION.
- Earlier 16 routes × 2 viewport audit found only Home horizontal overflow. Fixed and retested at 390 and 1440.

## New release gates
1. PLANTUPIA_PUBLICATION_APPROVED=1 is required before publicly serving proposal assets, indexing pages, opening case-study/sector pages and enabling message submission in production.
2. With approval enabled, the build checks scripts/check-production-release.mjs. It fails without apps/web/production-publication-approval.json, recorded approver/date/approvedAssets, all allowlisted private asset files, and enabled inbox credentials.
3. No private-review assets were packaged into the unapproved production staging candidate.
4. Node version pinned to Vercel project's 26.x major to avoid automatic major upgrade.

## Launch blockers NOT bypassed
- Source-image and final company-copy publication permission: meeting notes dated 2026-11-18 establish client-supplied review materials, not explicit public publication rights.
- Photo manifest: record approved source names and evidence. A technical manifest cannot itself prove copyright licensing.
- Enquiry inbox: relevant Vercel production variable names exist, but a controlled end-to-end actual-company inbox test is not verified.
- Shop catalogue and order path: verify live published SKU, pricing, product images, stock and the handoff to the app; never display seeded QA items.
- End-to-end UX and visual signoff on production-configured public candidate.
- Company domain: only project .vercel.app domains identified; official company-owned custom domain remains unverified.

## Release protocol
1. Obtain written approval of company copy, image rights and accepted storefront/care claims.
2. Build a complete production-publication-approval.json with approvedBy, approvedAt and approvedAssets and include the authorized files in the production bundle.
3. Verify the actual inbox and live product feed in a controlled environment.
4. Set PLANTUPIA_PUBLICATION_APPROVED=1 for Vercel Production only.
5. Deploy with --prod --skip-domain, retest every route, image, query/checkout/contact flow and access controls.
6. Promote that tested production build to the intended domain, not an unchecked Preview.

Vercel READY or successful typecheck does not constitute public release approval.
