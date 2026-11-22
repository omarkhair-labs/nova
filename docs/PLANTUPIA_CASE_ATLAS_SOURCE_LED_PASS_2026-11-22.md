# Plantupia — Source-led Case Atlas Pass (2026-11-22)

**Area:** Existing website, apps/web, branch design/plantupia-web-refinement-20261122. This pass changes production-intended source but deploys only to an authenticated Preview pending rights and approval. It is not a separate design Lab.

## Visual diagnosis

Full viewport screenshots of the six live case study openings showed the same failure: large text-only first-screen presentations (especially Pixels, Audi and Villa), with source images well below the fold. The very different proposal materials were presented as near-identical initial layouts.

## Editorial solution

One project-specific source photograph appears *inside* the opening of each case study, with the project title, concise proposal context and a visible "DESIGN PROPOSAL / SOURCE" caption. Existing full-length option galleries, photo sequences, technical tables, care descriptions and source notes remain in place. A lead preview is an opening editorial frame; detailed gallery images preserve the un-cropped source documents.

- **Mazal:** restaurant façade / nine directions, original comparison slider intact
- **EM Sherif:** navy metal planter as the constant / seven planting personalities
- **El Hallab:** full terrace planter edge / five choices plus source comparison matrix
- **Pixels Mall:** commercial architectural landscape / eight-frame spatial sequence, five proposal principles
- **Audi Cairo:** branded showroom interior / six proposal chapters, plant palette, care programme
- **Villa 7:** covered terrace / ten areas, twenty-four alternatives and selection sheet logic

The CSS shares only stable layout mechanics; per-case photo aspect ratios, cropping/containment and spatial emphasis differ. Mobile puts source imagery immediately after the heading, before explanatory text. One first-screen lead image receives fetchPriority high; below-the-fold image galleries load lazily.

## Meaningful project-to-project continuity

A CaseTrail concludes each study. It shows a source-backed image of the *next* study plus a specific conceptual transition (façade -> fixed vessel -> terrace line -> landscape architecture -> showroom -> residential space -> return to façade). A small exit returns to the full six-project archive, plus one context-appropriate reading in either Studio or Living Systems.

## Source-language correction

The six Audi proposal photos were examined directly in a montage. The same boilerplate description previously repeated under all six frames is replaced by specific editorial observations on the actual light, spatial planting, containers, exhibition surfaces, hospitality corner and brand wall. Labels and proposal page references remain source-derived, while image readings are clearly editorial, not a quoted client claim or installation proof.

Pixels Mall had an identical "descriptive reading" disclaimer under every one of eight frames; it was consolidated into one note in the opening text. The source note that these are proposals, not verified built work, remains.

## QA evidence

Desktop and mobile case-opening screenshots were captured (1440 x 900, 390 x 844) before and after the change.
- All 6 x 2 = 12 case routes: HTTP 200.
- Lead image and next-case image naturalWidth > 0 for all 12 after allowing asynchronous lazy images to load.
- Exactly one H1 each; no horizontal document overflow.
- Links correctly follow the project archive sequence including Villa 7 returning to Mazal.
- Mazal range input still responds; Audi has six chapters; Villa 7 has ten areas.
- Zero page JavaScript exceptions observed in that test.
- TypeScript check passed locally.
- Production domain and public image rights remain unchanged.

## Still open

1. Source-image rights and company publication approval.
2. Full page-by-page editorial/factual signoff of 9 + 7 + 5 + 8 + 6 + 24 proposal frames and related technical terms.
3. Deeper responsive motion and full-page accessibility/reading pace on Studio and Living Systems.
4. Live published commerce catalog and authorized working contact submission.
5. On-device performance/LCP beyond synthetic local tests.

Do not call this an accepted public launch or a finished product replatform.
