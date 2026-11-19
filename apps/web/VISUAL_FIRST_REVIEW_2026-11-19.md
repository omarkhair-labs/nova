# Visual-first correction — 2026-11-19

Trigger: review screenshots showed the private website still felt too diagram-led and table-led despite a large Plantupia visual source library. The Green Fortune Plantwire reference was re-opened as a design reference, not a template to copy. The useful principle is project/product imagery as the primary structure, with concise text and technical explanation secondary.

Changes:
- SiteIndex / World Index: removed the overlaid architectural SVG study in private review; chapter preview now leads with Plantupia proposal/catalogue imagery and adds a globally visible Case Studies / 06 link.
- Chapter transitions: private review replaces passage drawings with large Plantupia proposal images. Public/default fallback can still retain the authored drawing system until release direction is final.
- Spaces: all six private environment hero images now come from Plantupia proposal/catalogue material. Workplaces, Industry and Public are labelled as studies, not verified sector installations. No external editorial image is used for the six private environment frames.
- Spaces: added a prominent six-project Selected Case Studies image section with direct links to every case study. Project Archive remains available for filtering, but is no longer the only discovery route.
- Case studies: added image-led visual archives showing every existing case-study visual at once before the text-heavy controls:
  Mazal 9; EM Sherif 7; El Hallab 5; Pixels Mall 5; Audi Cairo 6; Villa 7 24 = 56 visible proposal frames.
- Villa 7: 24-frame visual archive now precedes the area selection UI; the long selection sheet is collapsed into secondary detail.
- Studio: private review uses the four actual proposal studies as the sticky visual instead of the large illustrative floor-plan SVG.
- Offices & HQs: the conceptual arrangement SVG is removed from the rendered private sector page; catalogue imagery leads the arrangement section instead.
- EM Sherif: planter specification remains as text; the large conceptual planter diagram is no longer rendered before the seven real proposal visuals.

Verification:
- TypeScript exit 0 and git diff --check clean.
- Six case-study visual archive counts verified on mobile: 9 / 7 / 5 / 5 / 6 / 24; HTTP 200, zero page errors, no horizontal overflow.
- Private Spaces image sources verified: six of six point to /api/private-arrival Plantupia assets.
- Responsive screenshots captured at 390 and 1440 for Spaces selected work, Villa visual archive, Studio source-led reading, and Offices catalogue-led arrangement.
- Further visual-library expansion should be curated from the remaining extracted source material, avoiding duplicates, pricing pages, references and low-value diagrams rather than uploading all extracted figures blindly.
- No production deployment or publication-rights claim.