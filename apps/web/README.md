# Plantupia public experience

The courtyard map is the creative website review for [issue #301](https://github.com/omarkhair-labs/nova/issues/301). The visitor enters a scene, can read its planting or return in time, and can go anywhere from the map.

## Review locally

Use Node.js 22 and run the following from `apps/web`.

```sh
npm install --no-audit --no-fund
npm run typecheck
npm run build
npm run start -- -p 3017 -H 127.0.0.1
```

Open [the local review](http://127.0.0.1:3017). In another terminal, `npm run smoke` checks the routes, contextual brief links and image assets.

## Destinations

- `/` is the courtyard entrance. Its signs change the viewing position and lead into the Approach and Aftercare scenes.
- `/spaces` opens six environments and links to their individual study routes. Each has place notes and preserves the selected environment in the project brief.
- `/studio` moves an annotation in the courtyard as the visitor chooses light, movement or care.
- `/living-systems` follows four illustrative care moments with native URL anchors. Browser Back revisits the preceding moment.
- `/about#selected-works` explains the company and the status of its project archive.
- `/start-a-space` is a copy-only project, care and general brief workspace. It accepts `space` and `topic` context.
- `/study/scene` redirects to the courtyard. `/study/navigation` preserves the original study URL.

[Reference briefs and spatial decisions](../../docs/PLANTUPIA_WEB_SPATIAL_EXPERIENCE_2026-11-15.md) · [Previous courtyard pass](../../docs/PLANTUPIA_WEB_COURTYARD_REVISION_2026-11-15.md)
