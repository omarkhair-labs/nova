# Plantupia / Original-source image gap and curated expansion — 2026-11-22

Status: **review-only, not a public rollout**. This note accompanies existing apps/web on `design/plantupia-web-refinement-20261122`.

## Actual gap
- Earlier original nine-PDF audit counted 333 embedded image objects, 183 large unique photos, 165 first-pass web candidates and 82 wide candidates. Candidate counts do **not** mean individually approved or independent projects.
- Current private asset set: **93 WebP**, of which **90 are literally referenced** in functioning website code. Three not referenced: `residence.webp`, `terrace-evergreen.webp`, `terrace-lavender.webp`.
- The *larger PDF image source* is not part of this 93-file set. Literal usage alone cannot quantify visually repeated frames or authorizations.

## Original source audit
- **Pixels Mall / MT Architects**: source PDF p4–p30 provides 27 large visual pages. The site uses principal chapters; many additional pages are same-place planting variants. Eight extra photographs were selected for a private review curation: pp8, 11, 14, 17, 20, 22, 25 and 27.
- **Audi Cairo**: six principal chapter images are visible. Additional valuable source angles p6, p8, p11, p13 and p15 were selected for private review; international Audi showroom *references* on p4 must not be shown as Plantupia work.
- **Hospitality Greenery Catalogue**: many outdoor/indoor species and vessel variations reuse one location or camera position; this is ideal for comparative planting/material reading, **not** for inflated completed-project counts. Selected pp9, 10, 12, 14, 15, 16, 22, 25, 26 and 36. Careful: p17 onwards include Sandwichroom frontage despite generic headers, so don't relabel it as a verified office.
- **Villa 7**: all 24 main outdoor/indoor proposal alternatives are already represented. The p27–p29 selection sheets repeat thumbnails; p30–31 are explicitly references.
- **Mazal**: all nine facade planting directions represented. Additional summary thumbnails are repetitions.
- **EM Sherif**: all seven variants of the navy metal planter represented; summary is repeated imagery.
- **El Hallab**: five terrace directions represented; unused `terrace-evergreen` and `terrace-lavender` are alternates of the same boundary, not two independent projects.
- **Pricing catalogue**: source photos help SKU/catalog research only. Internal pricing and supplier/client specifics do not establish publication rights or a live purchasable catalogue.

## 23-photo internal review curation
An offline bundle was created from the original user-provided PDFs with **23 WebP source images**, a CSV/JSON source-page manifest and comparative contact sheets. The **bundle is not uploaded to GitHub or Vercel**, and is not a public approval. Curated new files are named `archive-{source}-p{page}.webp`.

## Visual integration map — next accepted private-review changes
1. **Pixels / Studio:** p20 threshold, p25 frontage and p27 stair edge offer new visual readings of planting against architecture.
2. **Audi / case study:** p6/p8/p11/p13/p15 offer secondary camera views within the existing six chapters, not new projects.
3. **Hospitality / Spaces:** p14 silver foliage vs p15/p16 flowers on the **same outdoor frontage**, as a genuine one-place comparison.
4. **Material library / Shop:** p22/p25/p26 indoor plant/vessel characters and p36 metal planter line. No SKU prices or stock until published backend works.
5. **Living Systems:** support physical-care discussion with source-informed plant/vessel choices but do not fabricate completed visits, sensors or actual installed plants.

## Release and truth guardrails
Only company-approved and properly attributed source images go into public production. Client proposals and catalogue illustrations remain clearly labeled as such. No source image will be committed into this public repo to avoid exposing private archive rights. This note captures **what and why**; the next private preview must separately receive the files and pass image/viewport tests.
