# Vercel Protected Preview — 2026-11-19

Final corrected deployment:
- Deployment ID: dpl_7p6Ktj7n6DxPPcajf6tdptoUYJBw
- Preview URL: https://plantupia-public-preview-bl6e8tril-omarkhair70-droids-projects.vercel.app
- Target: Preview (target=null), not Production.
- Vercel Authentication / SSO protection is enabled for deployment URLs.
- Project root remains apps/web; no project setting, billing, production alias, or master merge changed.

Staging discipline:
- Deployment source was a separate review staging directory preserving monorepo path apps/web.
- 81 private review assets were embedded only in the review staging copy, under public/private-review-assets.
- Staging-only guards enable private review when VERCEL_ENV=preview; original source production guards were not weakened.
- /api/private-arrival remains allowlisted and on preview redirects only allowlisted requests to the embedded protected asset path.
- The first two newly-created review deployments were removed after discovering that Vercel project inbox environment variables made Start a Space live there.
- Final staging explicitly forces Start a Space available=false whenever VERCEL_ENV=preview. TypeScript passed before deployment.

Verification:
- Vercel build READY; Next.js 16.4.6; all expected routes built.
- Direct embedded review asset arrival.webp returns HTTP 200.
- /shop returns HTTP 200.
- /spaces/projects/mazal returns HTTP 200 and preloads the private proposal assets.
- Home response contains GREEN TECHNOLOGY / BIOPHILIC INTEGRATION and private-review markup.
- Start a Space response contains Preview mode + Sending is disabled and server-rendered submit button disabled.
- Mazal response contains Nine ways and private proposal image references.
- No real enquiry was sent during verification.
- Unsafe intermediate deployments removed: dpl_3MCCbgnBqqYagSZp3DTuWANSELdy and dpl_EG6gswjzX7mLw9CWEtT4S7kFARdC.
- Older prior GitHub preview and historical ERROR deployment remain untouched.

This preview is for authenticated review. Publication permission for supplied proposal/catalogue imagery remains a release gate.