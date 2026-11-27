# Vercel Protected Preview — 2026-11-19

## Canonical current deployment

- Deployment ID: `dpl_4kNjzezDA38YFxFJXFiJ6BrCbWNd`
- Preview URL: https://plantupia-public-preview-g3bwkztpc-omarkhair70-droids-projects.vercel.app
- Target: Preview, not Production.
- Status: READY.
- Protection: Vercel Authentication / SSO enabled for deployment URLs.
- Project: `plantupia-public-preview`.
- Project root remains `apps/web`.
- No billing, Production alias, master merge, or Production deployment was changed.

This is the canonical protected review deployment after the whole-site refinement, legacy cleanup, development-console polish and micro art-direction pass.

## Source state

Git branch:
`design/plantupia-arrival-private-20261118`

Runtime source commits:
- `b7c603593d3bef4fb6c1bc26920e5b8a379439f1` — whole-site refinement and legacy cleanup.
- `c76dcaadf3cca14a7c3a3761e938692eb81e9d1b` — final stable React key fix and site icon.
- `d39a5651d1bfce6c979a20d03f81ceddcdcc17dc` — micro art-direction pass for line wrapping, mobile case-study rhythm and source metadata readability.

Relevant earlier visual-curation commits:
- `fe9414c` — flagship case studies become distinct visual journeys.
- `2a09593` — visual-first curation for Shop, Mazal, EM Sherif and El Hallab.
- `000f15b` — previous canonical protected-preview record.

The Preview staging copy contains 96 allowlisted private review assets under `public/private-review-assets`.

## Whole-site refinement rule

An element stays only if it reveals evidence, explains a relationship, supports a decision, carries useful intent, establishes status/provenance, or moves the visitor into a meaningful next action.

Key changes:
- Project Archive: all six proposal studies are visible as six bodies of work; filter and active-project UI removed.
- Hospitality: Arrival / Seating / Terrace are one visible spatial journey; tabs removed.
- Offices & HQs: three composition logics remain visible together; only the field-note interaction remains because it carries selected intent into contact.
- Home and About: the company introduction now has different jobs and different copy; About no longer links to itself.
- Home / Spaces / Studio / Living Systems / About: repeated generic ChapterThreshold endings removed where the page already resolves its own argument.
- Spaces: duplicate project-archive CTA removed.
- Shop: category/search/sort controls render only when published commerce is available; the material/specification world still leads the transactional layer.
- Language pass: generic filler such as “thoughtful” and “considered” was removed where it was model-written description rather than source terminology.
- Source terminology such as “Premium Custom Planters” remains where it is explicitly part of the supplied proposal.
- 14 unused earlier-generation component/CSS files were removed after confirming they had no live-route callers.
- Spaces private image layers now use stable environment keys; the duplicate React-key warning is removed.
- `app/icon.svg` uses the existing circular `◕` wordmark language and established green/ivory palette; the browser no longer falls back to a missing `/favicon.ico`.

The reasoning for every retained interaction and page structure is documented in:
`apps/web/REFINEMENT_RATIONALE_2026-11-19.md`

## Current visual system

### Home
- Source-led hero and curated project field.
- World gateway remains because it maps the practice.
- Company introduction follows the evidence instead of duplicating About.
- Shop remains an alternate object/material path.
- No generic next-chapter panel after the page already establishes its routes.

### Spaces
- Environment chooser changes the active source study and questions.
- Case studies remain visible as evidence.
- Field notes remain interactive because selected concerns carry into the project conversation.
- Exactly one full-archive CTA remains in the main flow.

### Sector pages
- Hospitality: three spatial moments visible in sequence.
- Commercial: Pixels architectural framework → Audi brand/interior study → care; five sourced design principles retain progressive disclosure.
- Offices & HQs: catalogue evidence is explicit because no named office commission exists in the reviewed archive; three composition logics are static, field notes remain actionable.

### Project Archive + Case Studies
- Archive: six source-led studies visible without filtering.
- Pixels Mall: eight-frame architectural journey.
- Audi Cairo: six showroom chapters.
- Villa 7: ten areas containing all 24 proposal directions.
- Mazal: nine facade directions visible before the compare tool.
- EM Sherif: seven planting characters around one constant vessel.
- El Hallab: five terrace directions before the source comparison matrix.

### Shop
- Condition → Vessel → Plant → Finish before commerce.
- Augusta remains vessel-in-space plus object study.
- Plant collection is treated as a material palette.
- Fiberglass planter collection is treated as a specification layer.
- Empty/unavailable commerce does not expose dead filter controls.

### Studio
- Four acts remain interactive because they are the process argument: Read → Compose → Introduce → Return.
- Proposal evidence changes with the active act.
- Repeated generic chapter-ending UI is removed.

### Living Systems
- Physical care and digital continuity remain distinct.
- Timeline interaction demonstrates confirmed / recorded / planned / recorded states.
- No fake sensor reading, diagnosis or automatic proof of completed care is represented.
- The page ends in the care conversation rather than a generic chapter transition.

### About + Contact
- About names the practice, business focus and evidence; it no longer repeats the Home introduction verbatim or self-links.
- Contact keeps only action-oriented modes and fields.
- Preview contact submission remains disabled.

## Staging guards

- Private review is enabled only for Preview or local review conditions.
- `/api/private-arrival/[asset]` remains allowlisted.
- On Preview, allowlisted private-review requests redirect only to embedded protected review assets.
- `Start a Space` is forcibly unavailable whenever `VERCEL_ENV=preview`.
- Deployed contact copy states:
  - “Sending is disabled, so nothing will be submitted or stored.”
  - “Website submissions are not enabled on this preview. Nothing is sent or stored.”
- No real enquiry was sent during verification.

## Verification

### Local whole-site regression
- TypeScript passed after the structural deletion and after the final key/icon fix.
- `git diff --check` passed.
- 17 live routes tested at 320 / 390 / 768 / 1440 = 68 route/viewport combinations.
- All tested routes returned HTTP 200.
- Zero horizontal overflow across the tested combinations.
- Zero broken loaded images across the tested combinations.
- Semantic interaction suite passed for:
  - Spaces field-note brief carry-forward;
  - single Spaces archive CTA;
  - Project Archive six-study/no-selector structure;
  - Hospitality three-moment/no-selector structure;
  - Offices static composition logic + actionable field notes;
  - Mazal comparison options 04 and 09;
  - Studio act navigation;
  - Living Systems planned-vs-recorded state;
  - Shop availability-aware controls;
  - About no-self-link introduction.
- Fresh hydrated Spaces browser session after the final key/icon fix captured an empty set for console warnings/errors, page errors and HTTP responses >= 400.

### Whole-site protected deployment verification
The immediate protected whole-site refactor deployment built from `b7c6035` was checked route-by-route before the final delta:
- all 17 deployed routes returned their current refinement-pass content;
- unauthenticated access returned HTTP 302 to Vercel Authentication;
- both disabled-submission statements were present;
- representative private assets from Home, Shop, Mazal, EM Sherif, El Hallab, Pixels, Audi and Villa returned HTTP 200.

### Final canonical delta verification
The final `c76dcaa` delta is limited to the stable React key and site icon:
- staging source contains the stable `key={keys[i]}` image-layer identity;
- staging contains `app/icon.svg`;
- staging TypeScript passed with `VERCEL_ENV=preview`;
- Vercel build completed READY and generated all expected routes plus static `/icon.svg`;
- deployed `/spaces` contains the current refined content;
- deployed `/start-a-space` contains the disabled-submission statement;
- deployed `/icon.svg` returned HTTP 200 with command exit code 0;
- unauthenticated `/spaces` returned HTTP 302 to Vercel Authentication.

### Micro art-direction delta verification
The `d39a565` delta changes presentation only: balanced display wrapping, prettier copy wrapping, mobile Spaces case-study rhythm and small source-label readability.
- local regression loaded all 17 live routes at 390 and 1440 (34 combinations);
- every route returned HTTP 200 with zero horizontal overflow, broken loaded images, browser console warnings/errors or page errors;
- staging contains the full-width Spaces case-study rule below 520 px, 9.5 px Studio narrow-screen sheet metadata and the shared balanced-heading rule;
- staging TypeScript passed with `VERCEL_ENV=preview`;
- Vercel build completed READY;
- deployed Spaces, Studio and Shop returned their current refined content;
- deployed Start a Space retains disabled submission;
- deployed icon returned HTTP 200 with exit code 0;
- unauthenticated Spaces access returns HTTP 302 to Vercel Authentication.

## Preview cleanup

Removed after the micro art-direction replacement reached READY and passed verification:
- `plantupia-public-preview-f3k3k7vqj-omarkhair70-droids-projects.vercel.app`

Removed after the preceding final replacement reached READY and passed verification:
- `plantupia-public-preview-55dwz0n04-omarkhair70-droids-projects.vercel.app`

Removed after the preceding whole-site replacement passed full route verification:
- `plantupia-public-preview-qgvrxbi89-omarkhair70-droids-projects.vercel.app`

Removed during previous verified replacement rounds:
- `plantupia-public-preview-eelstwlqr-omarkhair70-droids-projects.vercel.app`
- `plantupia-public-preview-bar96mcn1-omarkhair70-droids-projects.vercel.app`

Earlier unsafe intermediate deployments that exposed live enquiry conditions had already been removed:
- `dpl_3MCCbgnBqqYagSZp3DTuWANSELdy`
- `dpl_EG6gswjzX7mLw9CWEtT4S7kFARdC`

## Release transition

This document records the authenticated review deployment history. The final curated experience subsequently passed staged-Production acceptance and was promoted to Production on 2026-11-19. Canonical release evidence is now in `PRODUCTION_RELEASE_2026-11-19.md`.
