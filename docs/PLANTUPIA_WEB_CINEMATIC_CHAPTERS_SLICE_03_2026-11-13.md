# Plantupia — Cinematic Chapters / Slice 03
Date: 2026-11-13
Workstream: Creative public website only, Draft PR #300 (branch `feature/plantupia-public-living-website-20261113`).
Release: Preview only. Do not merge PR #300 to PR #253 / master, change app/backend, or promote a domain.

## Why this slice exists

The first connected website already had an impressive conceptual entrance, a real chapter threshold, functioning Spaces/Studio/Living Systems controls, and `/start-a-space`. But the three deeper chapters used completely different hand-built CSS/SVG-like illustrations from the photorealistic editorial opening. The experience consequently read as separate experiments.

This slice removes the cartoon spatial, studio and systems illustrations from rendered React **and deletes their obsolete CSS**, replacing them with a coherent editorial photographic foundation while preserving purposeful interaction per page.

## Shared art direction now in code

- Architectural light, forest darkness, warm chalk, moss/sage cues, photo grain/grading and precise typographic folios.
- Full-bleed editorial photograph with layered interpretive information; distinct content interaction rather than generic full-bleed stock backgrounds.
- Shared plus (`SiteIndex`) and one route-specific chapter control system.
- Explicit source and `not Plantupia work` disclosure on the photo scene itself, not hidden in a footer.
- Fallback to locally hosted concept images if external Pexels photography cannot load.
- Responsive CSS, focus indicators, reduced-motion controls; full manual visual/keyboard/touch QA is **not** completed.

## Spaces / `/spaces`

Three real **independent** photo contexts replace the fake rendered illustration:
1. Dwelling — skylit architectural interior, Öznur Yüksel / Pexels — `28577085`.
2. Workplace — office atrium with greenery, Tatianaa Co / Pexels — `35566906`.
3. Gathering — interior restaurant/hospitality, Lachlan Ross / Pexels — `6510384`.

The scene responds to each place selection. Within the current place, **three actual design-prompt buttons** reveal concise questions such as daylight, movement, access, maintenance, rhythm and care responsibility. These are educational prompts, not site surveys or assessments of the pictured locations.

Outcome path remains real: `/spaces` → `/start-a-space` → shareable client-side outline, not a fake enquiry send.

## The Studio / `/studio`

One fixed editorial photograph of a planted architectural atrium (David Allen / Pexels `33708096`) carries a layered studio workbench. The four already-working stages now alter visible diagram material:

- Observe: architectural axes and framing.
- Compose: light plane and two material forms.
- Introduce: symbolic, abstract living-material location on the frame.
- Return: explicit continuity line and end marker.

This is an illustrative reading, **not** a genuine Plantupia client's plans, built result or actual installation record. The composition is deliberately a designed overlay on an independently sourced reference photo.

## Living Systems / `/living-systems`

An editorial photograph with hanging greenery (Rahime Gül / Pexels `40883631`) supports a quieter three-part schematic:

1. Place / installation context.
2. Care / human action and attention.
3. Memory / recorded observation through time.

The actively selected moment is visible in a narrative timeline. No sensor data, growth curve, air-quality metrics or false care completion is represented. A clear `NOT A LIVE DASHBOARD` disclosure stays visible.

## Source and rights

All new photos used here are editorial references from Pexels (at the public licensed photo pages below), not verified Plantupia project images:
- https://www.pexels.com/photo/modern-architectural-interior-with-skylight-28577085/
- https://www.pexels.com/photo/modern-office-atrium-with-greenery-35566906/
- https://www.pexels.com/photo/interior-of-restaurant-with-hanging-green-potted-plants-6510384/
- https://www.pexels.com/photo/indoor-atrium-with-lush-greenery-and-modern-architecture-33708096/
- https://www.pexels.com/photo/indoor-atrium-with-hanging-greenery-and-skylight-40883631/
- License: https://www.pexels.com/license/

For a company launch, assess the photos' license/third-party rights and better yet replace with approved authentic Plantupia site imagery. These are *distinct* locations and not physically accurate before/after sequences.

## Acceptance and remaining work

Automated verification:
- Web TypeScript check + production build.
- HTTP route smoke checks for all three scenes and their truthful labels.
- Verify `wr-space-art-` old illustration is absent from rendered `/spaces`.
- CI checks on `/start-a-space` and opening remain unchanged.

**Not yet verified via a real authenticated browser:** native desktop image load/crop at 1440/1920+, 360px mobile layout, readability of caption over photography, keyboard focus on the new Spaces prompt buttons, Vercel protection, Web Vitals, network-failure fallback, hover/touch behavior. Build success is not visual signoff.

**Next art direction work:** choose actual approved Plantupia material sources; refine photo composition as one coherent architectural world; introduce genuine company evidence (after approval); give the Studio more authentic blueprint/material layers; connect customer Care Guide and Botanical Library only through reviewed data; never make a fake interactive simulation of a working backend.

The wider business goal remains: **wonder → understanding → trust → useful action → continuing relationship**.
