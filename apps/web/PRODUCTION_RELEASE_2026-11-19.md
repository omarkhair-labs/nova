# Plantupia Production Release — 2026-11-19

## Live production

- Public URL: https://plantupia-public-preview.vercel.app
- Vercel project: `plantupia-public-preview`
- Production deployment: `dpl_F8oHRivT23nZtxevKHvpCWKjdXAK`
- Immutable deployment URL: https://plantupia-public-preview-it1qh4ijs-omarkhair70-droids-projects.vercel.app
- Framework: Next.js 16.4.6
- Region: `iad1`
- Release-prep source commit: `23b5d388457c89ad5b411cc720d86a479a459652`
- Final micro art-direction implementation commit: `d39a5651d1bfce6c979a20d03f81ceddcdcc17dc`

The deployment was created with Production environment variables but with domain assignment skipped. The exact built deployment was tested first and then promoted without rebuilding.

## Release artifact

The 96 curated proposal/catalogue assets remain outside Git.

The committed release tool:
`apps/web/scripts/prepare-vercel-release-stage.ps1`

creates a deployment staging tree and copies the allowlisted curated assets into:
`apps/web/public/private-review-assets`

The app source enables the curated source-led experience in `VERCEL_ENV=production`. The private-arrival asset handler redirects allowlisted Preview/Production requests only to the embedded release assets.

## Search/indexing

Production metadata now permits indexing; development and Preview remain noindex.

Production also exposes:
- `/robots.txt` with `Allow: /`
- `/sitemap.xml` with 17 public route URLs

Study/research routes are not added to the public sitemap.

## Staged-production acceptance

Before promotion, the immutable production deployment passed:

- 17/17 public routes returned the expected current curated content.
- All 17 routes were free of `noindex` in the staged Production output.
- Production contact UI detected the configured company-inbox environment and did not show Preview-disabled copy.
- No real enquiry was submitted during release verification.
- `robots.txt` allowed crawling.
- `sitemap.xml` contained exactly 17 `<loc>` entries.
- Representative assets returned HTTP 200:
  - Arrival
  - Home / Pixels
  - Shop / Augusta
  - Mazal
  - EM Sherif
  - El Hallab
  - Pixels
  - Audi
  - Villa 7
- Acceptance script ended with `STAGED_PRODUCTION_ACCEPTANCE=PASS`.

## Post-promotion acceptance

After promotion:

- both Vercel promote aliases reported `completed`;
- `plantupia-public-preview.vercel.app` is publicly reachable with HTTP 200;
- the QA alias remains Vercel-authentication protected;
- all 17 public production routes returned HTTP 200;
- Home contains the curated Green Technology / biophilic integration experience and is indexable;
- Contact remains enabled through the Production configuration;
- `robots.txt` allows crawling;
- sitemap still contains 17 routes;
- a representative private-arrival asset returned HTTP 200;
- Vercel confirmed the public Production alias resolves to `dpl_F8oHRivT23nZtxevKHvpCWKjdXAK`.

## Observability

Vercel runtime log scan for the promoted deployment, error/fatal levels, last 30 minutes:
- no matching logs found.

Configured project log drains:
- none.

Runtime logs are therefore the current release-error observation source.

## Rollback

Previous READY Production deployment:
- `dpl_2Xea1ZN1TvkbyWvwU3skps4SZzzL`
- source commit `627f483e561ffdf72103f2739c776d23f9c52247`
- branch `feature/plantupia-public-living-website-20261113`

It remains a Vercel rollback candidate.

## Domain status

No custom Plantupia domain is currently attached to this Vercel project.

Current project domains:
- `plantupia-public-preview.vercel.app` — Production public URL
- `plantupia-website-qa.vercel.app` — QA/branch alias, authentication protected

A custom-domain/DNS migration is therefore a separate follow-up, not part of this release.
