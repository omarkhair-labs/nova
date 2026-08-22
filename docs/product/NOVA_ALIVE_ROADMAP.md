# Nova Alive product roadmap

Status: product development after the completed architecture consolidation.

## Product idea

Nova should feel alive now, shared with your people, and worth returning to later:

```text
PULSE
  -> ORBIT
  -> TONIGHT
  -> ROOMS
  -> MEMORIES
```

- **Pulse** — "what I'm doing right now": short-lived photo, video, or text moments shared with followers or Close Friends.
- **Orbit** — activity and discoveries moving through your social circle.
- **Tonight** — a time-aware live surface that makes the app feel different while your people are active tonight.
- **Rooms** — persistent places for a group to chat, collect moments/media, make plans, and run live Tonight sessions.
- **Memories** — private recaps built from the moments people actually lived together, with a later AI-assisted film layer.

## Delivery sequence

The initial delivery budget is intentionally small-PR and dependency-aware:

1. Pulse backend foundation
2. Pulse Android experience
3. Pulse live replies / moment chains
4. Orbit backend activity graph
5. Orbit Android surface
6. Tonight backend live-state engine
7. Tonight Android experience
8. Rooms backend
9. Rooms Android experience
10. Rooms live / Tonight integration
11. Memories backend selection and grouping
12. Memories Android recap experience
13. Optional AI Memory Film rendering/selection layer

A later PR may start while an earlier PR's hosted CI is running, but dependent code must not merge ahead of a failing dependency.

## Pulse foundation contract

The first Pulse release establishes these server-owned rules:

- a Pulse lasts **12 hours**;
- supported content is photo, video, or text;
- audience is Followers or Close Friends;
- visibility inherits existing follow, block, active-account, and Close Friends policy;
- expiry is server controlled;
- existing Nova REST/WebSocket contracts remain unchanged; Pulse routes are additive;
- account deletion removes the user's Pulse content and uploaded Pulse media;
- Android version/signing and Google Play publishing are out of scope for the foundation PR.

The follow-up Pulse Android work should consume this contract through a dedicated `feature/pulse` ownership boundary rather than adding Pulse DTOs/endpoints to the shared network core.
