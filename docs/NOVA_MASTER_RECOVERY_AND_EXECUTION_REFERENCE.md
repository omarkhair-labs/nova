# NOVA — MASTER RECOVERY, PRODUCT VISION & EXECUTION REFERENCE

**Purpose:** Permanent continuation file for Nova so a new ChatGPT/Codex session can recover the product vision, approved references, product grammar, visual system, working method, protected architecture, roadmap history, CI strategy and current handoff without reconstructing the old chat from scratch.

**Repository:** `omarkhair-labs/nova`  
**Reference branch:** `docs/nova-master-reference`  
**Prepared:** 2026-08-28

---

## 0. How a new chat must use this file

This is a continuity file, not a frozen implementation snapshot.

At the start of a new Nova session:

1. Read this file completely.
2. Inspect live GitHub and current `master`.
3. Read the governing repository docs listed below.
4. Compare live state with the checkpoint recorded here.
5. If GitHub has advanced, live repository state wins for implementation state.
6. Preserve the product decisions, visual direction and workflow rules here unless a later explicit decision supersedes them.
7. Do not restart finished architecture or design-system phases.
8. Do not reopen completed flow work unless there is concrete regression evidence.

Recommended opening instruction:

> Treat `docs/NOVA_MASTER_RECOVERY_AND_EXECUTION_REFERENCE.md` as the continuity source for Nova. Verify live GitHub first, then continue only unfinished roadmap work. Do not redesign Nova, restart architecture, invent backend capability, or touch protected release/Memory Film/WebRTC/Firebase seams without evidence.

---

# 1. Current live checkpoint

At the Final Whole-Product Coherence checkpoint:

- Repository: `omarkhair-labs/nova`
- Default branch: `master`
- Current master:
  `7ab0a7e7ac65d62a41f0fd5ce001c16e1a58d4e0`
- This is the merge commit of PR #216 / Flow 9 Settings, Account, Privacy and Security.
- PR #214 / Flow 8 Memories: MERGED.
- PR #216 / Flow 9: MERGED.
- Flows 1–9 are merged.
- Final Whole-Product Coherence is `PR READY`; hosted CI and Samsung validation remain pending.

Important immediate history:

PR #214 merged Flow 8 Memories at `4286ad5ab319c2aec9a80d3d618722b9b0bd4c57`. PR #215 then updated this recovery reference. PR #216 merged Flow 9 at `7ab0a7e7ac65d62a41f0fd5ce001c16e1a58d4e0`. Live GitHub and repository state continue to outrank older checkpoint prose elsewhere in this document.

Current roadmap:

- Flows 1–9 — merged; their documented Samsung/device checks remain open where applicable.
- Final Whole-Product Coherence Pass — `PR READY` with bounded visible seam closures and focused local validation.
- Real Samsung/device closure — next product-validation phase after hosted review/CI.
- Release/Play — separate and not authorized by product-polish work.

Do not invent a Flow 10 unless a genuinely separate product domain is discovered.

---

# 2. Product north star

Nova is not being turned into another Instagram clone.

Approved product target:

> **A social app that remembers your life with your people.**

Approved identity:

> **Quiet Orbit**

Recognition stack:

- warm canvas;
- Nova violet signal;
- relationship/orbit motif;
- rounded Nova-owned icon language;
- soft spatial motion;
- content-first media;
- calm precision;
- real moments of life rather than dashboard chrome.

Useful identity formula:

> **warm space + violet signal + people/content + orbit relationships + quiet precision + moments of life**

Nova should feel coherent, premium and alive, but not sterile, corporate, neon, gaming-like, or over-designed.

---

# 3. Quiet Orbit visual identity

## Core roles

- Canvas: `#FAFAF8`
- Surface: `#FFFFFF`
- Nova Signal violet: `#6554E8`
- Live signal cyan: `#58C9D8`

Rules:

- Violet is Nova's main product signal.
- Cyan is reserved for genuine realtime/live semantics.
- Cyan is not a second generic primary color.
- User photos/videos remain truthful and unfiltered.
- Do not apply brand filters to user media.
- Do not make every immersive surface dark.

## Orbit language

Orbit/ring motifs are semantic, not decoration.

Use for:
- relationships;
- selection;
- progress;
- people-centered connection;
- real live/relationship meaning.

Do not scatter orbit rings as wallpaper.

## Feature moods

**Home / ordinary social:** warm, calm, content-first.  
**Tonight:** intimate, alive, nocturnal; not neon gaming UI.  
**Pulse:** immediate, right-now, socially alive.  
**Orbit:** relationship-first and distinctive; not generic event cards.  
**Memories:** warmer, reflective, tactile, authored; not analytics.  
**Settings/utility:** quiet, trustworthy, low-cognitive-load; no spectacle.

---

# 4. Existing design system is the foundation

Do NOT create `DesignSystemV2`.

Reuse/extend current semantic owners:

- `NovaType`
- `NovaSpacing`
- Nova/Material shapes
- `NovaMotion`
- elevation roles
- `NovaIconAsset`
- `NovaCard`
- `NovaAvatar`
- `NovaBottomBar`
- shared loading/empty/error/retry components
- `TonightTheme`
- `PulseTheme`
- `MemoryTheme`
- chat appearance palettes

If the visual pack exposes a genuinely reusable semantic need, extend the existing owner minimally.

Do not scatter:
- raw brand colors;
- arbitrary typography;
- duplicate spacing/radius constants;
- screen-local parallel visual systems.

---

# 5. Approved visual reference set

The Full Visual Pack was treated as one coordinated authority:

1. **Nova — Final Design System & All Screens**
2. **Nova — Core Social**
3. **Nova — Live & Orbit**
4. **Nova — Rooms & Messaging**
5. **Nova — Memories & Creation**
6. **Nova — Discovery & Profiles**
7. **Nova — Account, Notifications & Entry**

The pack governs:

- visual hierarchy;
- page composition;
- navigation presentation;
- spacing/density;
- component appearance;
- grouping/surface treatment;
- Orbit identity;
- Live/Tonight mood;
- Memories mood;
- typography hierarchy;
- icon personality;
- profile/social presentation;
- interaction-affordance appearance.

The pack does NOT govern production data.

Never hardcode sample:
- people;
- photos;
- counts;
- timestamps;
- captions;
- fake live state;
- fake backend behavior

just to resemble a board.

---

# 6. Authority order for decisions

When things conflict:

1. Existing production behavior, public contracts, security rules and protected architecture.
2. Current approved Nova architecture/design docs.
3. Approved Full Visual Pack for presentation.
4. Existing unprotected implementation detail.

Consequences:

- old styling/geometry may change;
- real behavior must not silently break;
- screenshots never justify breaking REST/WebSocket/auth/media contracts;
- if a mockup implies a capability that does not exist, adapt the presentation truthfully;
- never fabricate a backend capability;
- if a real capability exists, wire the design to it instead of faking it.

---

# 7. Competitive references — maturity, not cloning

Leading products may be studied for interaction maturity:

- gesture responsiveness;
- playback behavior;
- comments/replies;
- share/repost flows;
- composer ergonomics;
- keyboard handling;
- optimistic state;
- upload/progress/failure;
- navigation continuity;
- touch targets;
- motion/haptics;
- information density.

Products in this reference class include Instagram, TikTok, X/Threads, Snapchat, WhatsApp and Telegram.

Correct question:

> What makes this interaction solved and mature, and what is the strongest Nova-specific solution consistent with Quiet Orbit and our real architecture?

Wrong question:

> How do we copy another app here?

Orbit, Tonight, Pulse, Rooms and Memories must become stronger Nova concepts, not generic copies.

---

# 8. Product content grammar — formats must stay distinct

## Post
Persistent feed content:
- photo/video/caption;
- likes/comments;
- share/repost;
- persistent feed/profile presence.

## Story
Approx. 24-hour personal/narrative format:
- photo/video/text;
- Followers / Close Friends;
- reactions/replies;
- belongs to Create/Stories ecosystem.

**Do not move Stories to a generic Home story rail by default.**

## Pulse
Approx. 12-hour right-now format:
- text;
- photo;
- video;
- Live;
- Music;
- Talks;
- Vibes / present-tense conversational moments.

Pulse is not a four-second mini-Reel.

No intentional four-second cap was established.

## Reel
Short-form vertical immersive video.
Distinct from Post, Story and Pulse.

## Room
A shared-place interaction layer, not a second group chat.

Messaging owns:
- group membership;
- roles;
- chat;
- realtime messaging.

Rooms owns:
- shared-place identity;
- public discovery/following;
- Room items;
- sections;
- plans/reminders;
- Room media;
- Room Tonight composition.

## Memory
Retrospective layer over life that already happened:
- weekly recap;
- people;
- Rooms;
- nights;
- highlights;
- authored drafts;
- Memory Film.

Memories is not a feed and not a dashboard.

## Create
A rich truthful central creation hub.

Do not reduce Create to a bare launcher and do not add dead/fake actions.

---

# 9. Home intent and navigation truth

Home statement:

> **Home = life happening in front of you.**

Home should feel like the present social world, not a directory of every Nova feature.

Implications:

- content-first feed;
- Tonight can add contextual live/night identity;
- Orbit can add relationship context;
- Rooms/Memories can appear as compact contextual surfaces;
- do not permanently duplicate full Pulse/Rooms/Memories/Create as giant Home sections;
- Stories remain in the Story/Create ecosystem;
- Create remains a full destination;
- Home should not become a dashboard.

Approved primary navigation target:

> **Home / Orbit / Create / Inbox / Profile**

Also preserve reachability of:

- People/Search as Discovery;
- Reels;
- direct conversations;
- Calls;
- deep links;
- push routing.

---

# 10. Product-polish roadmap model

The roadmap exists to prevent:

### Trap A — shallow mega-pass
Touching everything without solving interactions deeply.

### Trap B — tunnel vision
Ignoring an obvious seam just because it sits one screen outside the named flow.

Flow loop:

> **Audit → plan → implement → focused validation → PR → hosted CI → review → real-device use → fixes → seam scan → DEVICE VERIFIED**

Flow boundaries are focus, not blinders.

A small adjacent issue may be fixed in-flow only when:
- found during normal flow inspection;
- obvious to a user;
- root cause understood;
- bounded/low-risk;
- no major new product decision;
- does not explode scope.

Otherwise record it for the correct later flow or final coherence.

---

# 11. ChatGPT / Codex responsibility split

## ChatGPT/product-review side

Owns:
- product intent;
- quality bar;
- audit and architecture reading;
- scope boundaries;
- visual/interaction direction;
- live GitHub state;
- CI log inspection;
- deciding real regression vs stale test/gate vs unrelated baseline;
- deciding whether adjacent fixes belong;
- merge timing;
- next-flow brief;
- interpretation of Samsung observations.

## Codex implementation side

Owns:
- detailed code tracing;
- implementation inside authorized flow;
- isolated worktree changes;
- focused tests;
- smallest useful compile;
- relevant architecture gate;
- logical commits;
- push;
- PR preparation;
- concise implementation report.

Codex should not burn quota babysitting CI.

---

# 12. CI-first / quota-efficient execution method

Preferred loop:

> **ChatGPT audit/scope → Codex implementation → focused local check → commit/push → STOP → hosted GitHub CI → ChatGPT live review → targeted concrete fix → Samsung QA**

## Run locally when useful

- code inspection;
- feature architecture scanner;
- targeted state-owner/unit tests if behavior changed;
- smallest meaningful Kotlin compile;
- `git diff --check`;
- clean status;
- protected-file scan if relevant.

## Do not routinely duplicate hosted CI locally

Avoid repeated:
- full Android suite;
- full backend suite;
- `lintRelease`;
- `assembleRelease`;
- `bundleRelease`;
- instrumentation build;
- emulator/device suite;
- entire CI after tiny edits.

Hosted CI is the full merge gate.

Quality is required. Waste is not.

## CI failure handling

1. Read relevant logs first.
2. Understand the failure cluster.
3. Decide whether it is:
   - real product regression;
   - stale test expectation;
   - stale architecture/design-system gate;
   - unrelated baseline issue.
4. Fix the correct layer once.
5. Run focused validation.
6. Push one coherent fix.
7. Let hosted CI run naturally.

Do not:
- spam no-op commits;
- manually dispatch duplicate CI;
- poll unchanged CI continuously.

---

# 13. Local worktree / Firebase safety

The user's local checkout:

`D:\projects\Nova`

has had a separate Nova Dev setup on:

`chore/nova-dev-variant`

Known local-dev elements include:

- debug `applicationIdSuffix ".dev"`;
- debug app name `Nova Dev`;
- real debug Firebase file:
  `D:\projects\Nova\app\src\debug\google-services.json`

### This Firebase file is protected.

Never:
- commit it;
- copy it into Codex branches;
- inspect/expose it;
- delete it;
- overwrite it;
- stash/drop it casually.

Codex work should use an isolated worktree.

If the isolated worktree lacks local SDK/Firebase setup:

- supply Android SDK through process environment;
- if Google Services generation is unavoidable, use only the repo's non-secret CI fixture temporarily;
- remove it before commit;
- verify it is absent from status.

### Destructive Git rule

Never `reset --hard`, clean, restore, checkout-over, stash/drop or discard a worktree containing authorized uncommitted work before inspecting and preserving it.

This rule matters especially after a Codex quota interruption.

---

# 14. Protected release / Play boundaries

Product polish does not authorize a new release.

Do not casually change:

- `app/build.gradle.kts`;
- `versionCode`;
- `versionName`;
- signing configuration;
- release credentials;
- Play tracks;
- publishing scripts.

Do not publish to Google Play as part of a normal polish flow.

Release/publishing is not the Definition of Done for product-polish PRs.

---

# 15. Protected calls/realtime boundaries

Do not touch WebRTC/TURN/Redis merely because Messaging/Calls/Rooms UI is being polished.

Protect:
- signaling;
- negotiation IDs;
- reconnect behavior;
- audio routing;
- Bluetooth/earpiece/speaker;
- Telecom;
- PiP;
- background/foreground lifecycle.

Only modify these with direct evidence of a defect in that layer.

---

# 16. Memory Film — protected high-risk area

Memory Film already had difficult lifecycle/render fixes.

Protected historical commit prefixes:

- route alias fix — `42efc092`
- Transformer main-thread fix — `0a5aa7f`
- demo-duration revert — `8aa9c178`
- WorkManager identity + REPLACE — `9baf507`
- `render_version2` identity — `6a073c4`
- immediate FGS notification — `318ba1e99`

Protect:
- `MemoryFilmWorker`;
- mediaProcessing FGS behavior;
- immediate foreground notification;
- work identity;
- enqueue/replace semantics;
- reattach behavior;
- render-version identity;
- Media3 exporter pipeline;
- final MP4 ownership;
- cancellation semantics.

Flow 8 can polish the UI around these behaviors. It must not casually rewrite the engine for cosmetic reasons.

---

# 17. Flow history

## Flow 1 — Social Core
PR #206  
Merge: `86c3c88f57df44060c38849018d50ddada57c061`

Key work:
- account-scoped feed cache;
- returning-session local hydration;
- foreground reconciliation;
- optimistic Like/Repost reconciliation;
- double-tap Like only;
- comments drafts/reply/IME/retry;
- share sheet;
- Add-to-Story audience feedback.

Important history:
returning sessions previously waited for remote `/me` before Home could hydrate cache; foregrounding lacked reconciliation.

Do not claim Flow 1 created the lead-Post-under-Tonight composition; it pre-existed.

## Flow 2 — Create + Media
PR #207  
Merge: `f23925bafb33e287fc4e3ddf94e2fc3c3f0b368b`

Key work:
- H.264/AAC MP4 normalization;
- duration-integrity validation;
- first-frame JPEG extraction;
- thumbnail-backed TextureView playback;
- playback error/retry;
- account-scoped idempotent WorkManager publication;
- streaming file upload instead of full-video byte array.

Historical root cause:
arbitrary `video/*` was previously uploaded unchanged with weak validation/playback error visibility.

Important uncertainty:
exact Samsung source codec/timestamp cause of the historical black/audio-only Pulse clip was not proven. No deliberate four-second trim was found.

## Flow 3 — Reels + Stories
PR #209  
Merge: `1b9ae8b67bf08d1538ba5f7193c89fa46e4a7a11`

Reels:
- pooled/shared player;
- Nova icon action rail;
- 48dp controls;
- motion/haptics;
- buffering/error continuity.

Stories:
- real photo/video preview;
- durable publish;
- Flow 2 preparation;
- first-frame thumbnail;
- video advances at actual playback end;
- IME pauses timing;
- viewer error/retry;
- reactions/replies/viewers preserved.

Boundary:
Stories were not moved to Home.

## Flow 4 — Profile + People
PR #210  
Merge: `b13f4f49edb4793435552cb36b554e7bf2efd3c3`

Key work:
- mature own/other profile grammar;
- persisted links;
- valid HTTP(S) handoff;
- theme/Orbit profile identity;
- private/verified/location states;
- Edit Profile save/IME/back;
- discovery filter semantics;
- stale-search protection;
- denser People/Connections rows;
- follow/request/following reconciliation;
- paging and profile handoff.

## Flow 5 — Orbit + Tonight + Activity
PR #211  
Merge: `147f5f32ffbf60ef95129f78774ac339cf5e14eb`

Orbit:
- relationship-first identity;
- denser event stream;
- real Post handoff;
- real Pulse context;
- no fake generic presence.

Tonight:
- local-time window preserved;
- real nearby Pulse moments;
- real image/thumbnail usage;
- Rooms handoff;
- no invented presence.

Activity:
- Follow Requests remain prominent;
- compact semantic rows;
- relative time;
- unread state beyond color;
- mark-all-read reconciliation.

## Flow 6 — Inbox + Messaging + Calls
PR #212  
Merge: `6c34c189405d712a14918d378733add8515a1cf5`

Inbox:
- real All/Unread/Mentions;
- Nova icons;
- denser conversation rows;
- automatic paging;
- mature creation surfaces.

Conversation:
- Nova call/info/reply/media/voice controls;
- 48dp actions;
- cleaner reactions/replies/swipes;
- pending/retry preserved;
- shared Post/Profile/Reel handoffs preserved.

Calls:
- Nova communication icon catalog;
- reconnecting distinct from failure;
- signaling/media/audio router/Telecom/PiP left intact.

CI lesson:
stale tests expected:
`🎤 Voice message`
`📷 Photo`

New correct text:
`Voice message`
`Photo`

with real Nova icons.

Correct fix was updating stale tests, not restoring emoji.

## Flow 7 — Rooms
PR #213  
Merge: `6553d60c12998bf36e3502511a8bf22b118b493f`

Flow 7A:
- mature discovery identity;
- public/private truth;
- topics/roles/member counts;
- Join vs Follow;
- loading/error/empty states;
- stale-refresh protection so old list responses cannot undo Join/Follow.

Flow 7B:
- removed whole-video `readBytes()`;
- reused `NovaVideoPreparer`;
- H.264/AAC normalization;
- duration verification;
- first-frame extraction;
- streaming multipart upload;
- real selected-media preview;
- real published-video playback;
- preparation/publishing progress;
- bounded automatic paging;
- safer HTTP(S) links;
- local-readable plan times;
- creator/profile handoff;
- 48dp actions;
- improved members/admin identity.

Ownership remains:
Messaging = group membership/roles/chat/realtime.
Rooms = Room identity/discovery/items/sections/plans/reminders/media/Tonight composition.

CI history:
#685 failed → gate alignment commit `ac0e326...` → #686 success → merge → #687 post-merge success.

---

# 18. Flow 8 — Memories (merged)

Status: merged via PR #214 at `4286ad5ab319c2aec9a80d3d618722b9b0bd4c57`. The notes below preserve the governing implementation brief and protected boundaries.

Preserve strong existing foundation:

- completed Monday-to-Monday weekly read model;
- local UTC offset;
- bounded `weeksAgo`;
- chronological highlights;
- Nights grouping;
- people/rooms/stats;
- privacy/block filtering;
- authored Memory drafts;
- Memory Film plan;
- WorkManager render;
- progress;
- cancel;
- reattach;
- rendered MP4 preview;
- Android share.

Known older UI seams to audit included:

- `✦`
- `Open ›`
- `▶`
- `Film ›`
- back `‹`
- `☾`
- video highlight dark placeholder + play glyph;
- Film scene placeholder;
- generic `AlertDialog` draft composer;
- raw `OutlinedTextField`;
- weak selected-media presentation.

Required outcome:

Your Week should feel:
- reflective;
- personal;
- warm;
- chronological;
- people/Room connected;
- intentional even when quiet.

Do not turn it into analytics cards.

Authored Memory composer should become Nova-owned and keyboard-safe while preserving:
- recap/film kind;
- title max;
- note max;
- create/update/delete;
- autosave behavior unless a real bug exists;
- photo/video selection.

Memory Film product UI may be modernized, but render/lifecycle infrastructure remains protected.

---

# 19. Flow 9 — Settings / Account / Privacy / Security (merged)

Status: merged via PR #216 at `7ab0a7e7ac65d62a41f0fd5ce001c16e1a58d4e0`. The notes below remain the governing capability-truth and safety record.

Principle:

> secondary surfaces should feel calm, obvious, trustworthy and low-cognitive-load.

Known Settings seams historically included:

- Account row with `onClick = null`;
- Appearance row without a real feature;
- Language row without real switching;
- Data & storage routing to Android App Details;
- About Nova using native `android.app.AlertDialog`.

Rule:

> **No fake settings.**

Do not invent:
- dark mode;
- themes;
- localization;
- cache controls;
- storage controls;
- account capabilities

unless they actually exist.

Notification Preferences real capabilities include:
- likes/comments/shares;
- mentions/tags;
- followers;
- messages;
- live sessions;
- reels/stories;
- events/spaces;
- product updates.

Privacy must preserve:
- private account;
- follow requests received/sent;
- Close Friends;
- Story audience;
- follower search/paging;
- session expiry.

Security must preserve:
- password reset;
- password change;
- signed-in devices/sessions;
- revoke other sessions;
- app lock;
- account deletion;
- blocked accounts;
- session-expiry handling.

Do not rewrite auth/session transport for visual reasons.

---

# 20. Samsung / real-device verification

A flow is not `DEVICE VERIFIED` because CI is green.

Real-device review matters for:

- system bars/insets;
- IME;
- font scaling;
- long text;
- touch feel;
- playback;
- device-recorded video;
- background/foreground;
- WorkManager;
- audio routes;
- Bluetooth;
- PiP;
- rotation;
- network changes;
- real encoding;
- performance/jank.

Real Samsung findings may reopen a flow.

Do not explain away a real device defect because tests pass.

---

# 21. State completeness

Changed surfaces must consider relevant:

- loading;
- refreshing;
- pagination;
- empty;
- error;
- retry;
- network failure;
- disabled;
- selected;
- pressed;
- unread;
- live;
- sending/uploading;
- pending;
- failure/retry;
- permission denied;
- private;
- blocked;
- session expiry;
- keyboard/input;
- long text;
- large fonts;
- RTL;
- reduced motion where relevant.

Do not communicate state by color alone.

---

# 22. Visual quality rules

Across Nova:

- violet has one consistent meaning;
- cyan only for real live/realtime;
- ordinary canvas remains warm;
- dark immersive surfaces only where mood justifies;
- Memories stays warmer;
- Orbit remains semantic;
- rounded-line Nova icons remain consistent;
- avoid Unicode/glyph actions when a Nova icon exists;
- avoid over-cardification;
- keep information density mature;
- deep screens must feel like the same product;
- user media stays truthful;
- final coherence should leave no major legacy-looking surface.

---

# 23. Code quality / architecture rules

Production code only.

No:
- demo implementation;
- placeholder navigation;
- hardcoded mock users;
- fake live counts;
- fake backend state;
- TODO-based completion;
- dead buttons;
- duplicate repositories;
- parallel architecture;
- `V2`/`New`/`Final`/`Temp` forks to avoid integration;
- giant generic UI dumping-ground;
- unrelated refactors;
- opportunistic backend cleanup;
- mass renaming without product value.

Prefer:
- feature-local state ownership;
- shared repeated presentation;
- small composables;
- current repository contracts;
- narrow architecture-aligned changes.

---

# 24. Do-not-claim list

Do not claim:

- exact Samsung Pulse black-video root cause was proven;
- intentional four-second Pulse cap existed;
- Pulse <60 sec was fully device-verified;
- Stories belong on Home;
- Flow 1 created lead-Post-under-Tonight composition;
- DEVICE VERIFIED from CI alone;
- production deployment is current without verification;
- release/Play state is current without live inspection.

---

# 25. Major polish merge history

- PR #206 / Flow 1 → `86c3c88f57df44060c38849018d50ddada57c061`
- PR #207 / Flow 2 → `f23925bafb33e287fc4e3ddf94e2fc3c3f0b368b`
- PR #209 / Flow 3 → `1b9ae8b67bf08d1538ba5f7193c89fa46e4a7a11`
- PR #210 / Flow 4 → `b13f4f49edb4793435552cb36b554e7bf2efd3c3`
- PR #211 / Flow 5 → `147f5f32ffbf60ef95129f78774ac339cf5e14eb`
- PR #212 / Flow 6 → `6c34c189405d712a14918d378733add8515a1cf5`
- PR #213 / Flow 7 → `6553d60c12998bf36e3502511a8bf22b118b493f`
- PR #214 / Flow 8 → `4286ad5ab319c2aec9a80d3d618722b9b0bd4c57`
- PR #216 / Flow 9 → `7ab0a7e7ac65d62a41f0fd5ce001c16e1a58d4e0`

Always verify live GitHub before acting.

---

# 26. Governing repository sources

New implementation sessions should read current versions of:

## Product/design
- `docs/design/NOVA_PRODUCT_POLISH_BRIEF.md`
- `docs/design/NOVA_PRODUCT_POLISH_ROADMAP.md`
- `docs/design/NOVA_VISUAL_IDENTITY.md`
- `docs/design/NOVA_DESIGN_SYSTEM.md`

## Architecture
- relevant files under `docs/architecture/`
- `docs/architecture/PRODUCT_MEMORIES.md`
- active-flow boundary docs
- current ownership/progress/route records if present

## Enforcement
Relevant scripts such as:
- `scripts/check_memories_architecture.py`
- `scripts/check_settings_architecture.py`
- `scripts/check_privacy_architecture.py`
- `scripts/check_security_architecture.py`
- `scripts/check_rooms_architecture.py`
- relevant shared UI/design-system gates.

Architecture gates are living contracts.

If product code intentionally evolves, a stale gate may need to be updated to protect the new correct seam.

Do not weaken a gate merely to make CI green.

---

# 27. Lessons from CI failures

A failing test/gate does not automatically mean the product change is wrong.

Flow 6 example:
old tests expected emoji fallback labels after product UI moved to real Nova icons.

Correct action:
update stale expectations.

Flow 7 example:
a Rooms/design-system gate encoded a previous seam and needed alignment with intended Flow 7 icon/Room ownership.

Correct action:
protect the new intended seam.

Rule:

> First classify the failure: real regression, stale test, stale architecture characterization, or unrelated baseline. Then fix the correct layer.

---

# 28. Branch / stacked PR rule

Later flows may temporarily stack on previous branches.

Once previous flow merges:

- retarget later PR to `master`;
- verify diff contains only the later flow;
- do not duplicate previous-flow commits;
- rebase only when safe;
- preserve all local uncommitted work before any rebase/reset.

Never destructively reset over in-progress Codex work.

---

# 29. Next execution plan

At this checkpoint:

1. Flows 1–9 are merged on master at `7ab0a7e7ac65d62a41f0fd5ce001c16e1a58d4e0`.
2. Final Whole-Product Coherence is `PR READY` as one bounded PR against master.
3. Hosted GitHub CI remains the full merge gate.
4. After hosted review/merge, perform the real Samsung/device closure recorded in the roadmap and smoke checklist.
5. Release/Play remains a separate later concern.

Do not start a new architecture phase.

---

# 30. Final Whole-Product Coherence Pass

Status: `PR READY`, with focused local gates and Kotlin compilation complete. Hosted CI and Samsung/device validation are still required; this status is not `DEVICE VERIFIED`.

After Flows 8 and 9, Nova was traversed like a normal user. The bounded closure replaced fake profile-Reel glyph tiles with real thumbnail-backed tiles and Nova icons; corrected stale glyph/version copy and Create icon semantics; made affected busy/success states explicit; made Welcome/auth actions safer under constrained height; and raised the affected shared/high-frequency action targets to the 48dp baseline. Existing state owners, navigation and protected media/realtime/release contracts were deliberately not reopened.

Audit:

- icon catalog and action states;
- leftover legacy components;
- over-cardification;
- navigation transitions;
- sheets/dialogs/menus;
- loading/error/empty;
- IME/insets;
- media preview/playback;
- accessibility;
- RTL;
- font scaling;
- immersive/dark surfaces;
- system bars/safe areas;
- dead/duplicated controls;
- stale copy;
- raw backend errors;
- Memory Film regression;
- auth/privacy regression;
- calls/realtime regression;
- release-sensitive files unchanged.

Final question:

> Can a normal user wander through Nova and still find an obvious part that feels old, generic, broken, inconsistent or unfinished?

If yes, polish is not closed.

---

# 31. Definition of Done

Polish closes only when:

- Quiet Orbit is coherent app-wide;
- primary navigation truth is coherent;
- Home/Orbit/Create/Inbox/Profile work truthfully;
- People and Reels remain reachable;
- Feed/Post/Comments work;
- Stories work;
- Pulse/Orbit/Tonight work;
- Rooms work;
- Memories/Memory Film work;
- Messaging/Calls work;
- Profile/People work;
- Settings/Privacy/Security work;
- deep links/push remain compatible;
- no fake backend state was added;
- no parallel Design System V2 exists;
- no accidental release was triggered;
- targeted regression evidence exists for changed behavior;
- hosted CI is green;
- device-sensitive paths receive real-device review;
- final coherence finds no obvious unfinished seam.

---

# 32. Recovery prompt for a future new chat

Paste this with the file:

> We are continuing Nova from an existing long-running product-polish program.
>
> Read `docs/NOVA_MASTER_RECOVERY_AND_EXECUTION_REFERENCE.md` completely.
>
> Repository: `omarkhair-labs/nova`
>
> Do not restart architecture.
> Do not redesign Nova from scratch.
> Do not discard Quiet Orbit.
> Do not invent backend capabilities.
> Do not touch protected release/Play, Nova Dev Firebase, Memory Film, WebRTC/TURN/Redis seams without concrete evidence.
>
> First inspect live GitHub:
> - current master SHA
> - recent merged PRs
> - open PRs
> - latest CI
>
> Then read the current product-polish brief, roadmap, visual identity, design system and architecture docs.
>
> Compare live state with the checkpoint in the reference file.
>
> Continue only genuinely unfinished roadmap work.
>
> Use the established workflow:
> ChatGPT audit/scope → Codex isolated implementation → focused local validation → commit/push → hosted CI → ChatGPT review/fix → Samsung verification.
>
> Do not waste Codex quota duplicating full CI locally or polling CI.
>
> Report the recovered live state before the next major product change.

---

# 33. Short memory version

> Nova = Quiet Orbit.
> Social app that remembers your life with your people.
> Warm canvas + violet signal + semantic orbit + rounded Nova icons + content-first media.
> Home = life happening in front of you.
> Post persistent. Story ~24h narrative/Create ecosystem. Pulse ~12h right-now. Reel immersive short video. Room shared place, not second group chat. Memories retrospective. Create rich truthful hub.
> Primary nav = Home / Orbit / Create / Inbox / Profile; People and Reels remain reachable.
> Competitive products are maturity references, never templates.
> ChatGPT audits/scopes/reviews CI; Codex implements in isolated worktree with focused checks. Hosted CI is the full gate. Samsung proves device-sensitive behavior.
> Protect Nova Dev Firebase, release/Play, Memory Film FGS/export identity, WebRTC/TURN/Redis.
> Flows 1–9 merged. Final Whole-Product Coherence is PR READY. Next after hosted review = real Samsung/device closure; release/Play remains separate.

---

# 34. Reference provenance

This file consolidates the working decisions produced across the Nova architecture/design-system convergence and the later product-polish flows.

Important historical reference set included the Full Visual Implementation Master Task and the approved Quiet Orbit visual direction.

The reference philosophy is:

> production truth and protected contracts outrank mockups; approved design direction governs presentation; fake capability is never acceptable.

This file should be updated at major Nova milestones rather than reconstructed again from chat memory.

**Checkpoint in this version:**  
`master = 7ab0a7e7ac65d62a41f0fd5ce001c16e1a58d4e0`

Flows 1–9 merged through Flow 9 / PR #216.

Final Whole-Product Coherence: PR READY, not device verified.

next product-validation item: real Samsung/device closure after hosted review
