# Plantupia — 23-source-photo integration / protected preview only
Date: 2026-11-22
Branch: design/plantupia-web-refinement-20261122

This is an applied extension to the **same website** rather than a new Creative Lab. It uses image data from the original Plantupia PDFs found in the authorized PC's project files; originals do not enter Git.

## Source verification and treatment
- 23 original PDF image xrefs were verified against their source pages and extracted as WebP with source filenames intact.
- Pixels Mall / MT Architects: eight added proposal images, pages 8, 11, 14, 17, 20, 22, 25 and 27.
- Audi Cairo / Living Interiors: five companion views, pages 6, 8, 11, 13 and 15, connected to the matching existing chapters.
- Hospitality Greenery Catalogue: six outdoor planting variations, pages 9, 10, 12, 14, 15, 16, split into two meaningful same-scene comparisons. Four material/vessel studies (pages 22, 25, 26 and 36) are shown in the Shop as catalogue illustrations and **not** as for-sale SKUs.
- Studio's first source frame was updated to Pixels architectural threshold, p20; this reuses one of the 23, not a twenty-fourth file.
- Proposal visuals are not evidence of built installations, approval of a planting design, commercial stock, final client choice or measured care outcomes.

## Implementation details
- Protected assets: extracted under D:\02_QA-and-Builds\Plantupia\2026-11-22-curated-extracted and copied into the existing authenticated Vercel Preview's private-review-assets folder.
- Not checked into source control. Private asset route uses an explicit allowlist of the 23 names.
- The existing 96-asset preview bundle plus the 23 additions now includes 119 files.
- Pixels comparison is explicitly toggleable, preserving the original five major spatial frames and next-case trail. Six further source frames are presented as architectural field notes.
- Audi second-angle frames live beside their corresponding chapter (no new invented chapter names).
- Hospitality has two separately controlled actual site-variant comparisons.
- Shop adds four non-transactional material photos in an authored sequence, not a fake product grid.

## Local QA
At 1440x900 and 390x844: Pixels, Audi, Hospitality, Shop and Studio each responded HTTP 200 (10 checks). All 23 distinct images loaded (plus Studio's one deliberate reuse), no JavaScript page exceptions, and the two interactive comparisons changed successfully. A newly introduced horizontal overflow in Shop was found and fixed; document width then equaled viewport on both breakpoints.

## Release boundary
No production alias promotion or public image rights approval is implied. The product feed and live enquiry acceptance remain separately gated. Cloud Preview must be verified after deployment.
