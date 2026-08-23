# Nova Visual Identity — Quiet Orbit

Status: **approved visual target for implementation**

This document sits on top of the completed DS-1 through DS-4 plumbing. It is not a new design system and must not create parallel foundations.

The approved visual direction is the final **Quiet Orbit hybrid** home mockup selected after the post-DS4 identity research. The implementation target is a warm, calm, content-first social surface with a stronger living-night identity in Tonight/Pulse, an orbit relationship signature, compact Rooms, and warmer Memories.

## Recognition stack

Nova should remain recognizable with the wordmark hidden through the repeated combination of:

1. warm canvas;
2. Nova violet signal;
3. orbit relationship motif;
4. rounded-line Nova-owned icons;
5. soft spatial motion;
6. content-first media.

The orbit motif is semantic, not wallpaper. Use it for relationship, live presence, selection and progress.

## Approved base palette

| Role | Value | Contract |
|---|---|---|
| canvas | `#FAFAF8` | ordinary application background |
| surface | `#FFFFFF` | cards, chrome and raised controls |
| ink | existing Nova ink | primary text/icons |
| muted | existing Nova muted | secondary information |
| signal | `#6554E8` | primary Nova action/selection/identity signal |
| signal soft | existing Nova accent-soft | quiet emphasis |
| live | `#58C9D8` | realtime/live state only; never a second general primary |
| Tonight base | owned by `TonightTheme.live` | dark cinematic live-night mode |

User photos and videos remain unfiltered. Brand color frames media; it does not tint it.

## Home hierarchy

The approved target order is:

1. identity header;
2. greeting and orbit status;
3. Tonight hero;
4. feed content;
5. Pulse realtime strip;
6. Your Orbit relationship strip;
7. Rooms;
8. Memories;
9. bottom navigation.

The production implementation may keep asynchronous/empty/error states in their existing locations, but populated-state hierarchy should converge on this order.

## Identity header

- `nova` is visually stronger than ordinary page chrome and uses Nova signal violet.
- Search, Activity and profile are compact icon/avatar actions rather than a text Activity pill.
- Profile avatar carries a restrained orbit ring/live point.
- Greeting is time-aware: Good morning / afternoon / evening + first name.
- Supporting copy: `Your orbit is awake.`

## Tonight hero

Tonight is the strongest identity moment on Home.

- wide cinematic dark card;
- 28dp family radius through the existing Material/Nova shape scale;
- deep navy/violet background, never generic black;
- large day/night headline;
- `Live` may use the dedicated cyan live signal;
- orbit rings organize active people visually;
- avatars remain individually meaningful/clickable where behavior exists;
- no permanent glow loops or decorative bouncing;
- error/retry and 6 PM / 6 AM behavior remain feature-owned.

The target feeling is **alive, intimate and nocturnal**, not gaming/neon.

## Feed

Feed content stays calm and practical:

- white surface against warm canvas;
- media supplies the majority of color;
- social actions remain compact and legible;
- no purple wash over user media;
- ordinary feed geometry remains quieter than Tonight/Pulse.

## Pulse

Pulse is realtime and media-forward but compact on Home:

- dark/immersive treatment can appear inside media tiles;
- violet indicates Nova interaction/state;
- cyan is reserved for live/realtime cues;
- Pulse must not turn the whole Home page dark.

## Your Orbit

This strip is where the relationship signature is most explicit:

- circular avatars;
- partial orbit rings/status points;
- calm labels and short realtime context;
- do not communicate state by ring color alone.

## Rooms

Rooms are compact, social and actionable:

- small room cards/chips;
- member/activity context;
- clear Join/Remind/live semantics only when real behavior supports them;
- same radius/type/icon language as ordinary Nova.

## Memories

Memories are the warm counterweight to live surfaces:

- subtly warmer framing;
- tactile/photo-stack feeling is allowed;
- no automatic vintage filter on user content;
- typography/timing/framing provide emotion rather than sepia overlays everywhere.

## Navigation target

The approved mockup shows:

`Home / Orbit / Create / Inbox / Profile`

This is a **behavioral navigation change**, not merely a label restyle. It must not be faked by relabeling the current `Home / People / Reels / Messages / You` routes. Navigation changes land only when route behavior and presentation change together.

Until that PR, the current navigation semantics remain truthful.

## Motion

Nova motion grammar is:

**approach → orbit → settle**

- enter: restrained translation/fade;
- selection: short low-overshoot response;
- expand: preserve spatial origin where possible;
- live: slow breathing/orbit signal only where status requires it;
- dismiss: slightly faster than enter;
- reduced motion: state/fade replacement with no unnecessary travel.

Existing `NovaMotion` roles remain the owner; future identity PRs may extend semantic roles instead of adding screen-local timings.

## PR rollout

1. **Home identity shell** — header/greeting, orbit primitive, Tonight hero and page hierarchy groundwork.
2. **Navigation truth** — Home/Orbit/Create/Inbox/Profile behavior + matching presentation.
3. **Home media/social convergence** — feed, Pulse, Orbit, Rooms and Memories populated-state geometry.
4. **Core screens** — Profile, People/Search, Notifications, Settings, Messages.
5. **Deep alive surfaces** — Tonight, Pulse, Orbit, Rooms and Memories full experiences.
6. **Motion + visual QA** — reduced motion, RTL/font scaling, canonical screenshot/golden coverage.

## Non-negotiables

- do not create Design System V2;
- do not bypass semantic owners with raw screen colors;
- do not make labels imply behavior the app does not perform;
- do not turn the orbit motif into decorative wallpaper;
- do not filter user media to manufacture brand identity;
- do not merge visual PRs without the existing full Nova CI gate.
