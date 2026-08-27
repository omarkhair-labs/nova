# Nova Product Polish Roadmap

Status: **governing execution roadmap for the post-convergence polish phase**

Established: **2026-08-25**

This roadmap is the execution companion to `docs/design/NOVA_PRODUCT_POLISH_BRIEF.md`.

The brief defines **what Nova must become and what quality means**. This roadmap defines **how the remaining product-quality work is sequenced, reviewed and closed without falling into another shallow mega-pass**.

This roadmap does not replace architecture records, `NOVA_DESIGN_SYSTEM.md`, `NOVA_VISUAL_IDENTITY.md`, or the product-polish brief. It sits below them.

---

## 1. Overall finish line

The goal is not to finish a checklist of named screens.

The goal is to reach a point where Nova can be used end-to-end on a real Android device without the user encountering an obviously unfinished, generic, visually weak, inconsistent or functionally broken seam in a normal journey.

The polish phase is complete only when Nova feels:

- visually intentional;
- interaction-complete;
- media-reliable;
- internally consistent;
- recognizably Nova;
- mature across both flagship and secondary surfaces;
- free of obvious legacy leftovers in normal use;
- validated on a real device beyond compile/tests.

A roadmap row reaching `DEVICE VERIFIED` does not automatically prove the whole application is complete. A final whole-product coherence sweep is mandatory.

---

## 2. Two traps this roadmap explicitly prevents

### Trap A — treating flow boundaries as blinders

Flows exist to give an implementation agent enough focus to go deep. They are **not permission to ignore an obvious product defect merely because it lives one screen outside the named flow**.

When implementing a flow, the agent must inspect:

- the entry into the flow;
- the complete flow itself;
- equivalent shared interactions;
- immediate exit/handoff surfaces;
- shared primitives touched by the flow;
- obvious neighboring seams visible during normal use.

If a small, clear, low-risk adjacent defect is discovered and fixing it is necessary to avoid leaving an embarrassing or inconsistent seam, it may be fixed in the same PR when doing so does not explode scope or violate ownership boundaries.

If the discovered issue is substantial or belongs to another major product domain, **do not ignore it and do not silently expand the PR**. Record it explicitly for the appropriate later flow or final coherence pass.

Every flow therefore ends with a **seam scan**, not merely a test of the named screen.

### Trap B — believing the named flows exhaust the product

The flow list is a planning structure, not a proof that every meaningful Nova surface has been named.

Agents must remain alert for:

- legacy screens reachable from modern screens;
- old icons or old action rows;
- forgotten dialogs/sheets;
- loading/error/empty states that were never redesigned;
- permission states;
- secondary menus;
- one-off detail screens;
- old navigation transitions;
- broken deep links/handoffs;
- stale copy;
- inconsistent system-bar/inset behavior;
- small but highly visible bugs that make the product feel unfinished.

The final coherence pass must actively search for these leftovers. It is not optional cleanup.

---

## 3. Competitive-reference rule: maturity, never cloning

Nova should not become Instagram, TikTok, X, Threads, Snapchat, WhatsApp, Telegram or any other existing product with Nova colors applied on top.

However, avoiding cloning does **not** mean avoiding competitive learning.

For high-frequency social interactions, agents may study mature products and platform conventions to understand the expected quality bar for:

- gesture responsiveness;
- media playback behavior;
- comments and replies;
- share/repost flows;
- composer ergonomics;
- keyboard behavior;
- action-state clarity;
- upload/progress/failure handling;
- navigation continuity;
- accessibility and touch targets;
- animation timing and feedback;
- information density.

Use those products as **maturity benchmarks and interaction references**, not as visual templates.

The correct question is:

> What makes this interaction feel solved and mature in leading products, and what is the strongest Nova-specific solution consistent with our own identity and architecture?

The wrong question is:

> How do we copy Instagram/TikTok/X here?

Nova must preserve and strengthen its own relationship/live/memory identity — especially Orbit, Tonight, Memories and its calm/live contrast — while meeting or exceeding the interaction maturity users already expect from established social products.

---

## 4. Working model

Each implementation unit follows this loop:

**Audit → plan → implement → focused tests → PR review → real-device use → fixes → seam scan → DEVICE VERIFIED**

Codex owns detailed implementation planning inside the flow after reading the current repository and governing records.

The product owner/ChatGPT side owns:

- product intent;
- quality bar;
- prioritization;
- interpretation of real-device observations;
- review of whether a solution feels genuinely finished rather than merely technically valid.

Do not over-specify arbitrary pixel values when stronger implementation judgment is possible. Be strict on outcomes and truthfulness, flexible on solution details.

---

## 5. Status model

Each major flow uses only these statuses:

- `PLANNED`
- `IN PROGRESS`
- `PR READY`
- `DEVICE REVIEW`
- `DEVICE VERIFIED`
- `REOPENED`

A flow may not reach `DEVICE VERIFIED` solely from CI, screenshots or unit tests.

If real use exposes a meaningful defect, mark the flow `REOPENED` rather than pretending the earlier PR settled it.

---

## 6. Planning horizon

Expected planning horizon: **approximately one focused week of agent-driven work**, with real-device review between substantial flows.

This is an estimate, not a deadline and not an instruction to stop after seven days.

If the work goes unusually smoothly, the phase may compress into roughly 3–4 intense days. If media, backend, realtime or Android lifecycle defects expose deeper root causes, it may take longer than a week.

Quality gates decide completion, not elapsed time.

---

## 7. Flow roadmap

### Flow 1 — Social core

Status: `DEVICE REVIEW` — PR #206 was reopened after the 2026-08-26 Samsung findings; bounded fixes are implemented, but the corrected paths require another physical-device pass

**Home/Feed → Post presentation → Like/social actions → Repost/Share → Post Detail → Comments/Replies/Composer**

Primary goals:

- make the highest-frequency social loop feel premium and distinctly Nova;
- establish/refine the core Nova social-action icon language;
- fix comments/IME/composer quality;
- make Share/Repost a real coherent flow;
- remove weak generic Post chrome and excessive cardification;
- verify loading/empty/error/optimistic states;
- run an adjacent seam scan before closing.

PR #206 now includes cache-first, per-account feed startup with a background authoritative refresh; field-scoped Like/Repost reconciliation across Feed and Post Detail; and adaptive, single-line social-count actions. Focused regression coverage includes interleaved Like/Repost outcomes, cached-feed hydration, account switching, duplicate startup prevention and compact-count formatting. Real-device judgment remains open for startup transitions, action-row density and interaction feel on Samsung hardware.

The 2026-08-26 device pass reopened Flow 1 because returning-session bootstrap waited for remote session validation before Home could consume the per-user cache, foregrounding the retained app had no feed reconciliation trigger, and Add-to-Story success was only transient sheet copy. PR #206 now hydrates the locally persisted session user before authoritative validation, reconciles Feed on app foreground with cache-preserving request deduplication, and keeps successful Story audiences visibly marked `Added ✓` while the share sheet remains open. A larger cross-surface Story refresh belongs to Flow 3. The observed lead-post split — one Post below Tonight with remaining Posts after Pulse/Orbit/Rooms/Memories — remains a recorded Home/Create composition seam; changing it is deliberately deferred rather than guessed inside this bounded fix.

Do not redesign the full application in this PR.

### Flow 2 — Create + media pipeline

Status: `PR READY` — implementation and CI-equivalent local gates are complete; Samsung device confirmation remains required

**Create → media selection → validation → upload → progress → publish → first frame/thumbnail → playback**

Primary goals:

- root-cause inconsistent video acceptance;
- root-cause black-video/audio-only playback;
- root-cause short buffering/restart loops;
- make validation/errors understandable;
- harden Post/Reel media lifecycle;
- ensure media reliability is not hidden behind visual polish.

The 2026-08-26 Samsung review confirmed a Pulse video path with a black surface and audio-only playback plus an apparent short restart loop despite a longer source. Static tracing proved that the previous picker accepted arbitrary `video/*`, uploaded the original bytes unchanged, validated only type and size, generated no first-frame thumbnail, and used the default `SurfaceView` player inside a Compose dialog without visible playback errors or retry. The backend did not trim or transcode the upload; the player looped at the endpoint Media3 interpreted from the unnormalised source. The exact source codec/profile/timestamp defect versus Samsung decoder contribution cannot be proven without the original device file and decoder logs, so that attribution remains a device-review question rather than a code claim.

Flow 2 now normalises accepted video to H.264/AAC MP4, rejects transforms that lose duration, extracts a real first-frame JPEG, and uses thumbnail-backed `TextureView` playback with surfaced failure and retry. Post and Reel publishing use account-scoped, idempotent WorkManager jobs with queued/preparing/uploading/published/failed state, real preparation progress, bounded automatic retry, safe manual retry, and session checks that prevent account-switch uploads. Normal Posts now have an image-or-video API contract, persistent video metadata, feed/profile/detail playback, and media-preserving DM, Story, and Orbit handoffs. Pulse and Reels share the same preparation and playback foundations. No foreground-service, manifest, release, signing, WebRTC, TURN, Memory Film, or Play-version contract was changed.

Samsung review still must confirm the original Pulse clip plays picture and audio for its full duration before looping, first-frame thumbnails appear across Pulse/Post/Reel surfaces, full-screen handoff and retry behavior feel correct, and queued publication survives the relevant app/background/network transitions. These paths are not marked `DEVICE VERIFIED` by local or hosted CI.

Product boundaries recorded with that review: Stories remain a 24-hour personal/narrative format and are not being moved to Home; Pulse remains a distinct approximately 12-hour “right now” text/photo/video format whose accepted videos play their full duration before looping; Reels remain a distinct immersive format; normal video Posts remain persistent feed content; and Create remains a rich Nova hub. Broad Home/Create composition cleanup is deferred until stronger product evidence exists.

### Flow 3 — Reels + Stories + immersive media

Status: `PR READY` — implementation is complete; hosted CI and Samsung device review remain the validation boundary

Primary goals:

- mature full-screen media composition;
- gestures, Like/Comment/Share states and animation;
- comment-over-video concept where appropriate;
- Story viewer/composer as a Nova-owned experience;
- playback/loading/failure continuity;
- cross-surface consistency with Flow 1 actions without copying another app.

Flow 3 replaces the remaining Reel text-symbol action columns with Nova-owned semantic icons, accessible 48dp controls, active/busy states, restrained Like haptics/motion, thumbnail-backed buffering/error continuity, and pooled shared playback in both the root and Profile Reel viewers. Existing VerticalPager behavior, watch tracking, comments-over-video sheet, optimistic Like/Repost ownership and internal/external sharing contracts remain intact.

Story media creation now provides an immersive real photo/video preview and truthful Followers/Close Friends selection. Video Stories reuse the Flow 2 H.264/AAC normalizer, duration-integrity check, first-frame thumbnail, account-scoped idempotent WorkManager publication and retry state. The viewer now renders through the shared TextureView player/error/retry surface, pauses for viewer/mutation/IME overlays, and advances video only at the actual playback endpoint; photo and text timing remains intentionally bounded. Existing reactions, replies, viewers, delete, shared Post and shared Reel semantics are preserved.

The Create hub keeps its rich multi-format composition while presenting Reel once, alongside the other real creation formats, and truthfully describes video Posts. Pulse remains a separate approximately 12-hour format and continues to use the shared Flow 2 preparation/player foundation; only exposed legacy play, reaction, reply and close glyphs were aligned with Nova's icon catalog. Stories were not moved to Home, and no broad Home/Create redesign or Flow 4 work was included.

Samsung device review still must confirm Reel gesture/action density, pooled playback and overlay pause/resume; Story picker preview, background publication/retry, first-frame thumbnail, full-duration video advance, IME/back/system-bar behavior, reactions/replies/viewers; and Pulse icon/playback continuity. Flow 3 is not `DEVICE VERIFIED`.

### Flow 4 — Profile + Edit Profile + People/Discovery

Status: `PR READY` — implementation is complete; hosted CI and Samsung device review remain the validation boundary

Primary goals:

- stronger profile identity/hierarchy;
- fix saved profile link not appearing;
- polish grids/tabs/counts/actions;
- own/other-profile continuity;
- search/discovery/connections quality;
- verify all persisted profile metadata actually renders truthfully.

Flow 4 now gives self and other-person profiles one mature identity grammar while preserving their different ownership and safety actions. Persisted links are rendered and handed only to valid HTTP(S) destinations; saved profile themes have a restrained identity-level effect; `showOrbit` controls the Orbit treatment; and verified, location, link and private-account states use Nova-owned iconography. Edit Profile previews the actual saved identity choices, keeps its action above the IME, blocks back while saving and surfaces link/server validation without exposing the private email field. Existing Posts/Reels/Reposts paging, follow/private gating, Message, call, share, report and block contracts remain intact.

People discovery retains its confirmed server-backed People, Nearby, Interests, Verified and New semantics and now explains those semantics in place. Active searches are revision-owned so stale responses cannot replace a newer query or filter; semantic filter changes never present the previous filter's rows. Recent profile handoffs remain usable even when the account is outside the current result page, row treatment is denser and shared across discovery and connections, verified/private/follow-request truth is explicit, and follow mutations reconcile from the server without allowing a second busy mutation to change an unsent row. Followers/Following now have calm loading/error/empty/paging states, automatic bottom paging and a real handoff back into the selected Nova profile.

Samsung review still must confirm long-link and large-font layout, theme/Orbit visibility, external browser handoff, Edit Profile IME/back/save behavior, discovery filter transitions, recent searches, automatic paging and Follow/Requested/Following touch feedback. Flow 4 is not `DEVICE VERIFIED`.

### Flow 5 — Orbit + Tonight + Activity/Notifications

Status: `PR READY` — implementation is complete; hosted CI and Samsung device review remain the validation boundary

Primary goals:

- make Nova’s relationship/live concepts genuinely distinctive;
- reduce repetitive oversized event-card treatment;
- clarify activity density and grouping;
- preserve real presence semantics without fake activity;
- strengthen handoffs into profiles/messages/content;
- ensure unique Nova identity becomes deeper, not more generic.

Orbit keeps its constellation and relationship-first identity while replacing repetitive oversized activity cards with a denser semantic event stream. Real Post events open the exact Post, actor/profile handoffs remain direct, available Post/Pulse context media is rendered, filters retain their server-backed meanings and cursor paging now reaches a truthful filtered-empty state before stopping. Pulse is the only event that produces a live Orbit marker; ordinary relationship activity no longer implies fabricated presence. Because Nova has no specific-Pulse destination route, Pulse activity truthfully hands off to its actor rather than inventing a dead content target.

Tonight keeps its existing local-time window, identity hero and Rooms handoffs. The live surface now exposes the real server-returned nearby Pulse moments with their image or first-frame thumbnail previews, Nova-owned Moon/Play iconography and no new presence claim. The backend contract already supplied `thumbnail_url`; Flow 5 preserves that field in the Android model/parser and consumes it instead of adding a parallel or fake media source.

Activity keeps Follow Requests prominent and actionable while rendering normal notifications as a compact row-and-divider stream with semantic icons, actor hierarchy, relative timestamps and a non-colour unread description. Successful mark-all-read now reconciles visible rows immediately; cursor-page unread counts cannot overwrite the global badge; paging deduplicates repeated items; duplicate in-flight refreshes are suppressed; a resolved follow request cannot be resurrected by a stale request response; and only the notification whose Post is resolving shows opening progress. Existing pull-to-refresh and Follow/Post/Reel/Person destinations remain intact, with invalid or deleted Reel metadata falling back to the real actor profile instead of a dead tap.

Samsung review still must confirm constellation and event-row density, real Pulse-only live markers, filtered paging and Post/profile handoffs; Tonight image/video thumbnail treatment and local-window transitions; and Activity large-font layout, unread contrast/announcement, pull-to-refresh, follow-request decision feedback, paging and Post/Reel/Profile navigation. Flow 5 is not `DEVICE VERIFIED`.

### Flow 6 — Inbox + Messaging + Calls

Status: `PLANNED`

Primary goals:

- polished conversation/composer/media/replies/reactions;
- message and call-event visual language;
- IME/inset quality;
- attachment flow;
- real call lifecycle/reconnection/audio continuity regression checks;
- preserve TURN/Redis/realtime infrastructure and recent reliability work.

### Flow 7 — Rooms

Status: `PLANNED`

Primary goals:

- make Rooms feel like shared social spaces rather than stacked settings cards;
- member/admin identity;
- detail/timeline/add-to-room/plans/media/saved surfaces;
- Discover/Following/public join/follow states;
- live/chat handoffs;
- permissions remain obvious and truthful.

### Flow 8 — Memories

Status: `PLANNED`

Primary goals:

- weekly recap and authored-memory hierarchy;
- Memory Film storyboard/render/progress/cancel/reattach/preview/share;
- real empty/small-memory behavior;
- protect recent route/thread/work-identity/FGS fixes;
- ensure the feature feels intentional even with minimal content.

### Flow 9 — Settings / Account / Privacy / Security / secondary utility surfaces

Status: `PLANNED`

Primary goals:

- clean and confident secondary UI;
- icon/row/switch/destructive-action consistency;
- Privacy, Notifications, Security, Login Activity, Blocked Accounts, Data & Storage, Help/About;
- accessibility and low cognitive load;
- no unnecessary visual spectacle.

---

## 8. Cross-app lanes that run through every flow

The following are not postponed until a final pass. They must be considered whenever relevant:

- Nova-owned iconography;
- motion and tactile feedback;
- keyboard/IME/insets;
- loading/empty/error/retry;
- accessibility/RTL/font scaling;
- media lifecycle;
- optimistic state and rollback;
- navigation continuity;
- trust/safety and privacy truthfulness;
- real-device behavior;
- obvious adjacent legacy seams.

A final pass still audits them globally after the focused flows are complete.

---

## 9. Opportunistic-fix policy

A Codex flow PR may include an adjacent issue outside the literal flow only when all of the following are true:

1. the issue is discovered during normal inspection/testing of the current flow;
2. it is obvious to a user or directly harms cross-surface consistency;
3. the root cause is understood;
4. the fix is bounded and low-risk;
5. it does not require a separate major product decision;
6. it does not turn the PR into another app-wide mega-pass;
7. the PR explicitly lists the opportunistic fix.

Otherwise, record the issue for its correct flow/final pass.

This policy exists so focused work does not leave embarrassing small leftovers, while still protecting depth and reviewability.

---

## 10. Mandatory seam scan for every flow

Before requesting `DEVICE VERIFIED`, inspect at least:

- screen immediately before the flow;
- screen immediately after the flow;
- all sheets/dialogs opened from the flow;
- empty/loading/error variants;
- back navigation;
- system bars and keyboard where applicable;
- shared icons/actions used elsewhere;
- content created by the flow when viewed in its destination;
- own-user and other-user variants where applicable.

Any glaring inconsistency must either be fixed if bounded or explicitly recorded.

---

## 11. Final whole-product coherence pass

Status: `PLANNED`

This pass starts only after the major flows are individually stable.

It must actively traverse the product rather than simply reread PR summaries.

Required checks:

- app-wide icon catalog and all high-frequency action states;
- app-wide visual language and remaining legacy components;
- over-cardification and density;
- navigation transitions and handoffs;
- sheets/dialogs/menus that escaped focused flows;
- loading/error/empty states;
- keyboard/insets;
- media previews/playback;
- accessibility/RTL/font scaling;
- dark/immersive surfaces;
- system bars/safe areas;
- real Samsung smoke run;
- obvious dead/duplicated controls;
- stale copy and raw backend errors;
- regression of recent Memory Film, TURN/realtime, auth/privacy or release-sensitive behavior.

The final question is not “did every roadmap flow get a PR?”

The final question is:

> Can a normal user wander through Nova and still find an obvious part that feels old, generic, broken, strangely inconsistent or unfinished?

If yes, the polish phase is not closed.

---

## 12. Handoff rule for Codex prompts

Each Codex prompt should:

- require reading `NOVA_PRODUCT_POLISH_BRIEF.md` and this roadmap completely;
- name one primary flow;
- allow the bounded opportunistic-fix policy above;
- require a seam scan;
- require root-cause handling for functional defects;
- forbid another indiscriminate full-app rewrite;
- require one focused PR;
- require explicit list of deferred issues discovered during the audit;
- avoid prescribing arbitrary pixel solutions;
- remind the agent that competitive products are maturity references, not templates to clone.

The detailed implementation plan belongs to the agent after it understands the current code. The product finish line belongs to this brief/roadmap and real-device review.
