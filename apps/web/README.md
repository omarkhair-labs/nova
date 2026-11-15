# Plantupia public website

**Where life takes root** is the creative website review for [issue #301](https://github.com/omarkhair-labs/nova/issues/301). Enter a living courtyard from its shadowed threshold, step closer to the planting and return with the person who looks after it.

## Run the review build

Use Node.js 22, matching web CI. Run these commands from `apps/web`.

```sh
npm install --no-audit --no-fund
npm run typecheck
npm run build
npm run start -- -p 3017 -H 127.0.0.1
```

Open [the local preview](http://127.0.0.1:3017). With the server running, run `npm run smoke` in another terminal to check the routes, contextual links and image sizes. Use `npm run dev` during editing.

## Explore the site

- `/` offers three views of the same courtyard and direct routes into the rest of the experience.
- The shared Index previews a destination on focus and desktop hover. Every destination is also linked in the footer.
- `/spaces` previews six environments. The individual study routes explain place-specific design and care questions and carry the chosen space into the brief.
- `/studio` reads one courtyard through light, movement and access for care.
- `/living-systems` follows that planting from handover through observation, follow-up and retained history.
- `/about#selected-works` describes the company and availability of project evidence.
- `/start-a-space` prepares local drafts for projects, care or general messages. It accepts the existing `space` and `topic` query parameters.
- `/study/scene` redirects to the homepage. `/study/navigation` is a historical study reachable by its original URL.

[Courtyard revision and critique](../../docs/PLANTUPIA_WEB_COURTYARD_REVISION_2026-11-15.md) · [First draft research and asset sources](../../docs/PLANTUPIA_WEB_ROOM_FOR_LIFE_2026-11-15.md)
