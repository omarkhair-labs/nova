# Vercel Protected Preview — 2026-11-19

Final corrected deployment:
- Deployment ID: dpl_J4V4UWpEwxwBwzPMAuGx6znhYP1h
- Preview URL: https://plantupia-public-preview-bar96mcn1-omarkhair70-droids-projects.vercel.app
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
## Visual-first update
The protected preview was redeployed after the visual-first correction: six direct case-study cards in Spaces, 56 case-study visual frames exposed through image archives, Plantupia imagery replacing external editorial images in private Spaces, image-led Studio/Offices review, and drawings removed from the private World Index/chapter passages. Previous protected preview dpl_7p6Ktj7n6DxPPcajf6tdptoUYJBw was removed after the replacement reached READY.

## Home + Shop production-set update
The protected preview now includes the first curated production set: 15 newly extracted source assets (8 Home / 7 Shop), three curated opening scenes on Home, an eight-image Selected Work field, and a Shop material world built from Hospitality catalogue SPACE / OBJECT / PLANTS / VESSELS studies before the live catalogue. Preview staging contains 96 private review assets total. Home and Shop were verified at 390 and 1440 locally with zero page errors/overflow/broken review assets. Deployed responses were verified to contain the new visual sections, direct review assets return HTTP 200, live catalogue markup remains present, and Start a Space remains locked in Preview mode. Previous READY preview dpl_7VMF7twoU7BywynTi3ULDMnj55WQ was removed after replacement.
