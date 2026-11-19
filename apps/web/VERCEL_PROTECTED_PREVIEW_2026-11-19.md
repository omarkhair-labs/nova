# Vercel Protected Preview — 2026-11-19

## Canonical current deployment

- Deployment ID: `dpl_EdUK12LtLRN8m3KqbYcCgETpsy3z`
- Preview URL: https://plantupia-public-preview-qgvrxbi89-omarkhair70-droids-projects.vercel.app
- Target: Preview, not Production.
- Status: READY.
- Protection: Vercel Authentication / SSO enabled for deployment URLs.
- Project: `plantupia-public-preview`.
- Project root remains `apps/web`.
- No billing, Production alias, master merge, or Production deployment was changed.

This is the canonical protected visual-review deployment for the 2026-11-19 curation pass.

## Source state

Git branch:
`design/plantupia-arrival-private-20261118`

Relevant visual-curation commits:
- `fe9414c` — flagship case studies become distinct visual journeys.
- `2a09593` — complete visual-first curation for Shop, Mazal, EM Sherif and El Hallab.

The Preview staging copy contains 96 allowlisted private review assets under `public/private-review-assets`.

## Staging guards

- Private review is enabled only for Preview or local review conditions.
- `/api/private-arrival/[asset]` remains allowlisted.
- On Preview, allowlisted private-review requests redirect only to embedded protected review assets.
- `Start a Space` is forcibly unavailable whenever `VERCEL_ENV=preview`.
- The deployed page explicitly states that sending is disabled and nothing is submitted or stored.
- No real enquiry was sent during verification.

## Current visual system

### Home
- Curated opening scenes from Pixels Mall, Audi Cairo and Villa 7.
- Eight-image visual field linked to real proposal sources.
- External editorial imagery is not used to lead the protected review.

### Shop
- Three spatial-condition scenes before commerce.
- Material library structured as Condition → Vessel → Plant → Finish.
- Augusta shown as vessel-in-space plus object study.
- Plant collection treated as a material palette.
- Fiberglass planter collection treated as a specification layer.
- Live catalogue remains intact after the editorial material library.
- Catalogue studies are explicitly distinguished from live SKU photography.

### Case studies
- Pixels Mall: eight-frame architectural journey.
- Audi Cairo: six showroom chapters in one continuous visual sequence.
- Villa 7: ten spatial chapters containing all 24 proposal directions.
- Mazal: all nine facade directions visible first, grouped by three families; two-direction compare remains as a secondary tool.
- EM Sherif: all seven planting characters visible together around one constant vessel.
- El Hallab: all five terrace directions visible before the source comparison matrix.

## Verification

Local visual acceptance:
- 320 / 390 / 768 / 1440 px.
- Zero horizontal overflow in all tested Shop and case-study pages.
- Zero broken private-review images in all tested visual fields.
- TypeScript passes.
- Mazal comparison controls were exercised with options 04 and 09.
- El Hallab matrix preserves five options and seven source comparison measures.
- Shop preserves the live catalogue after the private material library.

Deployed verification:
- `/shop` contains the current material-library markup.
- All six case-study routes return their current visual-first content.
- `/start-a-space` contains Preview mode and the disabled-submission state.
- Sample private assets for Shop, Mazal, El Hallab, Pixels, Audi and Villa return HTTP 200 through authenticated Preview access.
- Unauthenticated access to the deployment redirects to Vercel Authentication (HTTP 302).

## Preview cleanup

Removed after the canonical replacement reached READY and passed verification:
- `plantupia-public-preview-eelstwlqr-omarkhair70-droids-projects.vercel.app`
- `plantupia-public-preview-bar96mcn1-omarkhair70-droids-projects.vercel.app`

Earlier unsafe intermediate deployments that exposed live enquiry conditions had already been removed:
- `dpl_3MCCbgnBqqYagSZp3DTuWANSELdy`
- `dpl_EG6gswjzX7mLw9CWEtT4S7kFARdC`

## Release gate

This deployment is for authenticated review only. Publication permission for supplied proposal and catalogue imagery remains a release gate before any Production publication.
