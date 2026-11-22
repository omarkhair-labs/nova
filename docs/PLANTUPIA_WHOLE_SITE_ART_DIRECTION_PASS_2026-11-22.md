# Plantupia / Whole-site editorial and art-direction pass
Date: 2026-11-22
Status: Applied to the existing website source, not a new lab or approved public launch.

## One continuous visual thesis
Plantupia is Green Technology and biophilic integration. The digital encounter connects a real place, deliberate planting, material choice, technical specification and continuing care. The source proposals carry this relationship already. The site lets people read it visually without false claims or surprise modes.

Working experience: place → planting decision → designed material → continuity → a real enquiry or published product. This is an organizing grammar, not a new company tagline.

## Reference research translated into the site
- Green Fortune presents Plantwall and Plantwire as technically different ways of bringing greenery into architecture. Its maintenance page links good initial installation with sustainable future care. Ref: https://www.greenfortune.com/products/plantwall ; https://www.greenfortune.com/products/plantwire ; https://www.greenfortune.com/maintenance
- Immersive Garden illustrates the impact of deeply composed motion and media, but usability/accessibility deserve equal attention; an award review rated usability below creativity. Ref: https://www.awwwards.com/sites/immersive-garden-website ; https://rickcreates.com/r/2025-01-07-immersive-garden
- Plantupia's archive is the primary visual source: Pixels Mall, Audi Cairo, Mazal, Villa 7, El Hallab, EM Sherif and the Hospitality Catalogue. Source images remain design proposals/catalogue illustrations, not verified completed installations.

## Applied changes
1. **Home / entry:** all three private-review scene images load early, first at high priority. The longer 0.9-second clip-path wipe and 1.8-second scale became a 0.48-second crossfade and 0.7-second subtle settle. Three independent stages, original photos, copy and World Index remain.
2. **Spaces overview:** all six environments and the original source switcher remain. Desktop now reads as an editorial sector index beside a large relevant photo, not six controls stacked above it; mobile keeps stacked source-aware layout. Sector image transitions use 0.42 seconds. A source-appropriate path leads into plant/vessel studies.
3. **Shop:** the route exists; it is not an end-to-end live store yet. There is no dedicated PLANTUPIA_PUBLIC_COMMERCE_BACKEND environment variable. The code falls back to a website inbox backend but receives no valid published product feed on the current Preview. The first path now leads visitors to source-backed material studies instead of promising unavailable prices. The empty state is honest and product price disclaimer only displays for available published products. No fictional stock was created.
4. **Shop / naming:** shop-office-greenery.webp actually shows the café/restaurant frontage bearing Sandwichroom branding, not an executive office. Visitor-facing image label corrected to Café frontage planting, under its existing Hospitality Catalogue page 17 source credit.
5. **Navigation:** the real + World Index links, three visual chapter links and ShopBridge use Next.js internal navigation. Six chapter routes and Shop as additional entrance are preserved.
6. **Living Systems:** connects continuing care back to the plant/vessel reading, without implying a live app sensor or verified intervention.
7. **About, footers and language:** fixed missing About H1 while keeping home intro H2. Hard-coded Design preview footers replaced with evergreen company descriptions; contact submission remains guarded.
8. **Earlier QA:** unexpected auto Rapid Index removed, retaining all select-able project images and reducing duplicated image DOM count.

## Site by site: remaining artistic work and constraints
- **Home:** scene handoff refined. Need real-device LCP and motion profiling, including first-visit image load.
- **Spaces overview:** source-driven visual composition applied. Keep six sector meanings coherent.
- **Commercial/Hospitality/Offices:** real source studies and narrative content exist. Each still deserves image-choice, reading-order and full-page mobile critique; do not present proposals as completed work.
- **Six project studies:** all routes already exist with alternatives. Need individual critical art direction of their composition, text hierarchy and visual sequencing, not repeated identical card layouts.
- **Studio:** four connected design stages exist. Need focused authored pacing, source consistency and responsive reading.
- **Living Systems:** physical care proposals and an illustrative ownership/history timeline remain distinct; never fabricate telemetry or service outcomes.
- **Shop:** editorial material library is real. Published SKU feed, stock and approved product photographs are a separate actual backend/company dependency.
- **About:** semantic heading corrected. Verified and authorized public portfolio content remains incomplete; do not invent a company track record.
- **Contact:** actual enquiry pathway exists but submissions are disabled on private previews. An authorized end-to-end inbox test is required before public launch.

## Local verification this pass
- Home / Spaces / Shop interactive passes at 390px, 900px and 1440px: all 200, responsive sector switching and published material anchors work, no document horizontal overflow or browser errors.
- Whole-site route audit after internal navigation: 17 routes x 2 widths (34): all HTTP 200 and no overflow. + Index opens; Shop navigation, browser Back, Home to Spaces link work.
- About missing H1 found and fixed; checked H1 on About and H2 on home.
- TypeScript passes. Final cloud preview and on-deployment image checks needed after last edits.

## Release boundary
Public production alias stays unchanged. Private proposal images and copy await explicit company publication approval. Real published products and contact flow must be verified before calling this a completed production commerce website. Vercel READY alone is not approval.
