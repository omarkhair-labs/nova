# Plantupia website / integrated local redevelopment review — 2026-11-19

This document records local review status only. It is not client approval, publication authorization, production deployment or a claim that proposal visualizations are completed installations.

## Current local scope
1. Home: source-led imagery, Company Introduction, Shop bridge, chapter transition, and meeting-aligned Green Technology / Biophilic Integration positioning. Technology and sustainability are expressed as Site Conditions / Living Material / Continuing Care rather than generic tech decoration.
2. Spaces Explorer: six environments retained; primary B2B hierarchy Hospitality, Commercial, Offices & HQs; secondary Residential, Industry, Public Spaces.
3. Dedicated sector paths: Hospitality, Commercial, Offices & HQs.
4. Project Archive: six source-led proposal records.
5. Interactive case studies: Mazal, EM Sherif, El Hallab terrace, Pixels Mall, Audi Cairo, Villa 7.
6. Studio: four-act architectural process linked to real proposal evidence.
7. Living Systems: physical-care evidence separated from illustrative digital continuity.
8. About: source-led company framing and primary B2B sectors without invented history/team claims.
9. Shop: independent redesign draft remains separate; route included in smoke test only.
10. Start a Space: full preview UX explorable while actual submission stays disabled when inbox is unavailable.

## Source discipline
Supplied PDFs remain outside git. Private extracted assets remain outside the repository and are served only by the loopback/private asset route in source. Proposal visuals are labelled as proposals/visualizations; installation status and publication permission are not inferred. Expanded El Hallab deck mentioning The Sandwich Room remains excluded from the confirmed El Hallab terrace case pending relationship clarification. No named office commission was fabricated.

## Interaction evidence before integrated build
- Spaces: 24/24 environment/viewport passes.
- Sector pages: Hospitality 12/12 moments; Commercial 20/20 principle checks; Offices 12/12 arrangement checks.
- Project Archive: 24/24 project/image selections + 16/16 filters.
- Six case studies: 224 responsive option/scene checks total, plus integrated archive/case/enquiry journeys.
- Studio: 16/16 stage/viewport checks after hydration fix.
- Living Systems: 16/16 entry/viewport checks.
- About: four responsive passes.
- Start a Space: four responsive preview-UX passes.
- Home meeting-positioning pass: 3 scenes x 4 viewports = 12 checks.

## Integrated optimized build
Diagnostic copy: D:/Plantupia-QA-2026-11-18/full-review-build-20261119
The diagnostic copy changes development guards only inside the copy to PLANTUPIA_LOCAL_BUILD_TEST=1. Original source guards remain unchanged. Build binds only to 127.0.0.1:3026 and uses the local private asset directory. It is not a release candidate and was not uploaded.

Next.js 16.4.6 webpack optimized build:
- Build exit 0.
- Compilation successful in 10.7s.
- TypeScript successful.
- Static generation successful.
- All new private routes present.

Integrated smoke evidence:
- 17 routes x 2 widths (390 and 1440) = 34/34 route/viewport checks.
- 0 page errors.
- 0 horizontal overflows.
- 0 broken local/private visible assets.
- External editorial references are not treated as local build-health dependencies because third-party loading can vary.
- Evidence: temp/full-review-build/smoke-results.json.

Integrated click journeys:
- Normal motion: Home -> Spaces -> Project Archive -> Mazal, and Studio -> Living Systems -> About -> Start a Space passed.
- Reduced motion: same journey passed.
- 0 page errors.
- Final Start a Space submit remained disabled in preview.
- Evidence: temp/full-review-build/journeys.json.

## Still not complete / blockers
- Final artistic/client acceptance of the whole integrated experience.
- Publication permission for supplied proposal/catalogue imagery.
- Cross-browser/device accessibility and performance audit beyond current Chromium/responsive checks.
- Real inbox submission testing requires explicitly enabled backend credentials; none was sent.
- Vercel protected Preview remains unresolved from the earlier staging-root/deployment-limit work. No billing, project protection or production settings were changed.
- Shop remains its own redesign track and has not been declared production-ready.
- No merge to master, no production deployment, no push or commit is claimed by this review.
