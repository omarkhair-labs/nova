# Plantupia public website

**Room for life** is the creative website review for [issue #301](https://github.com/omarkhair-labs/nova/issues/301). It introduces Plantupia’s planting practice, six types of built space, a courtyard design reading, an illustrative aftercare sequence and a local project brief.

## Run the review build

Use Node.js 22, matching web CI. Run these commands from `apps/web`.

```sh
npm install --no-audit --no-fund
npm run typecheck
npm run build
npm run start -- -p 3017 -H 127.0.0.1
```

Open [the local preview](http://127.0.0.1:3017). With the server running, run `npm run smoke` in another terminal to check the routes, contextual links and image assets. Use `npm run dev` during editing.

## Explore the site

- `/` introduces the practice and offers direct links into six environments.
- `/spaces` is the directory. The six `/spaces/[kind]` studies describe place-specific design and care questions and carry the chosen space into the brief.
- `/studio` reads one courtyard through light, movement and care.
- `/living-systems` follows an illustrative planting from handover through observation, care and retained history.
- `/about#selected-works` describes the company and availability of project evidence.
- `/start-a-space` prepares visitor-owned drafts for projects, care or general messages. It accepts the existing `space` and `topic` query parameters.
- `/study/scene` redirects to the homepage. `/study/navigation` is a historical study reachable by its original URL.

[Creative rationale and asset ledger](../../docs/PLANTUPIA_WEB_ROOM_FOR_LIFE_2026-11-15.md) · [Issue and release scope](https://github.com/omarkhair-labs/nova/issues/301)
