# Nova Product Polish Brief

Status: **governing product-polish brief after Full Visual Convergence**

Date established: **2026-08-25**

This document defines the next product-quality phase for Nova after the merged Full Visual Convergence work, especially PR #205. It is intentionally broader than a screenshot-matching task and intentionally narrower than another architecture rewrite.

The purpose of this phase is to take Nova from a broad, coherent, technically mature alpha to a **recognizable, premium, intentional social product** whose high-frequency interactions, media behavior, visual language and real-device reliability all feel finished.

This brief is not permission to discard the existing architecture or design system. It sits on top of the current architecture records, `NOVA_DESIGN_SYSTEM.md`, `NOVA_VISUAL_IDENTITY.md`, and the approved implementation matrix. Those remain governing constraints for ownership, safety, contracts and semantic design tokens.

---

## 1. Why this phase exists

PR #205 successfully translated a very large approved visual/product scope into production code. It delivered a real shared visual system, the Quiet Orbit identity direction, expanded product surfaces, deeper Settings/Privacy/Rooms/Memories behavior and broad convergence across the application.

That work is a **foundation, not the visual finish line**.

Real-device use after the merge showed a clear difference between:

- broad implementation coverage;
- architectural/design-system maturity;
- final product taste;
- interaction polish;
- media reliability;
- real-device quality.

Nova now often feels like a real product with a team and a coherent system behind it. It does **not yet consistently feel like a world-class, distinctive social product in every flow**.

The next phase must therefore optimize for depth, not another app-wide breadth pass.

Do not repeat a huge convergence sweep merely because many screens can be changed at once. The goal is not maximum changed-file count. The goal is that each important flow is genuinely excellent before moving to the next one.

---

## 2. Interpretation rule: examples are concepts, not isolated tickets

This is a core product-owner rule for the entire phase:

> **Examples supplied by the product owner are evidence of a broader interaction, visual or product problem. They are not necessarily isolated implementation requests. Infer the underlying principle, audit all equivalent surfaces, and fix the product family rather than only the literal example.**

Examples:

- If the owner says double-tapping a Post/Reel to Like feels strange, do not only patch one gesture callback. Audit the complete Like interaction language across Posts, Reels, media viewers and any equivalent surface: gesture recognition, animation, haptics if appropriate, optimistic state, count updates, active/inactive icon states and failure rollback.
- If the owner says a Like, Comment or Share icon looks weak, do not replace that one glyph. Audit Nova iconography globally and establish a consistent app-owned visual language across social actions, navigation, Stories, Reels, Messages, Rooms, Create, Settings and states.
- If the owner points at the Comments keyboard, do not only add bottom padding. Review the whole comment-composer interaction: keyboard/insets, focus, send action, draft state, loading/failure, reply context, media/GIF opportunities if real product support exists, and equivalent behavior in Post and Reel comment surfaces.
- If the owner says the Post share screen feels weak, treat Share/Repost as a product flow: entry animation, recipient selection, repost/external share distinction, confirmation, feedback, errors and iconography.
- If the owner says a Reel looks wrong after upload, inspect the whole media lifecycle: picker/validation, upload, processing, first frame, playback, buffering, looping, audio/video sync, controls, failures, retries and published presentation.

Do not hide behind the wording of one example when the same weakness exists elsewhere.

---

## 3. Governing constraints

Before changing code, read current `master` and the current governing records, especially:

- `docs/architecture/NOVA_ARCHITECTURE_AUDIT_AND_MASTER_PROMPT.md`
- `docs/architecture/PROGRESS.md`
- `docs/architecture/OWNERSHIP.md`
- `docs/architecture/ROUTE_INVENTORY.md`
- `docs/architecture/SAMSUNG_SMOKE_CHECKLIST.md`
- feature/product architecture records under `docs/architecture/`
- `docs/design/NOVA_DESIGN_SYSTEM.md`
- `docs/design/NOVA_VISUAL_IDENTITY.md`
- `docs/design/APPROVED_VISUAL_IMPLEMENTATION_MATRIX.md`
- recent merged architecture/design PRs, especially PRs #197, #202, #203, #204 and #205

Preserve the consolidated ownership boundaries. Do not create Design System V2, shadow repositories, route-local duplicates, fake product controls, speculative API contracts or another parallel architecture.

Do not change Play signing, release workflows, `applicationId`, versioning or Google Play behavior as part of visual polish unless a task explicitly requires it.

Do not intentionally regress recent production fixes, including Memory Film foreground rendering/reattachment, foreground-service notification behavior, media-processing declaration behavior, WebRTC/TURN support, Redis-backed realtime behavior or existing trust/safety enforcement.

---

## 4. Product quality target

The target is not simply “clean Material 3.” Nova already has Material/Compose foundations.

The target is:

> A first-time user should feel that Nova is a deliberate, modern social product with its own point of view, not an AI-generated collection of rounded Material cards.

A designer should be able to inspect ten different Nova screens and recognize one product language without every screen being a clone.

A normal user should experience:

- strong media presentation;
- obvious hierarchy;
- consistent controls;
- responsive interactions;
- believable motion;
- useful density;
- clear loading/error/empty states;
- reliable upload and playback;
- no inexplicable dead controls or hidden saved data;
- no visible mismatch between “premium shell” and “generic inner flow.”

The visual bar is **recognizable, confident and restrained**, not decorative excess.

---

## 5. What should be preserved — and what is not sacred

### Preserve when it genuinely works

- warm canvas;
- Nova violet as the primary signal;
- restrained live cyan only for realtime/live semantics;
- orbit motif when it communicates relationship, live presence, selection or progress;
- content-first media;
- Quiet Orbit’s calm/live contrast;
- strong Tonight identity moments;
- the consolidated design-system ownership and semantic tokens;
- truthful navigation and product behavior.

### Not sacred

No screen is protected merely because it came from PR #205 or an approved mockup.

Home, Orbit, Story Viewer, Profile, Create, Feed, Rooms and every other visible surface may be improved when real-device use reveals a better product solution.

The existing Home/Orbit direction may be useful evidence of Nova identity, but it is **not a template to copy everywhere** and it is not automatically the final visual bar.

The product owner has explicitly not approved the current visual implementation as “finished.”

---

## 6. Cross-app visual principles

### 6.1 Reduce over-cardification

Current Nova overuses a recurring pattern:

- white surface;
- thin border;
- large rounded rectangle;
- repeated vertical stack.

This creates coherence but also makes many unrelated surfaces feel generated from the same template.

Reduce unnecessary containers. Use the right combination of:

- whitespace;
- hierarchy;
- separators;
- media edges;
- grouped surfaces;
- typography;
- contextual backgrounds;
- immersive surfaces;
- chips only where a chip is semantically right.

A component should earn its container.

Do not solve this by removing all cards. Solve it by making container choice intentional.

### 6.2 Content and media dominance

Photos and videos should feel like the content, not like attachments trapped inside UI chrome.

Review:

- cropping and aspect-ratio strategy;
- edge treatment;
- corner radius;
- transitions into full-screen media;
- first-frame behavior;
- poster/thumbnail behavior;
- loading placeholders;
- failure visuals;
- text/media balance;
- action placement relative to media.

Never tint or filter user media merely to manufacture brand identity.

### 6.3 Hierarchy over decoration

Improve screens first through:

- composition;
- typography;
- spacing;
- density;
- alignment;
- grouping;
- state clarity.

Do not use gradients, glow, giant radii, unnecessary shadows or animation to compensate for weak hierarchy.

### 6.4 Nova-owned iconography

Perform an app-wide iconography audit and establish one strong Nova icon language.

The goal is not simply “all icons have the same color.” Review:

- optical weight;
- stroke width;
- corner/terminal treatment;
- filled vs outline logic;
- active/inactive states;
- selected/unselected states;
- 16/20/24/32dp optical balance;
- alignment inside touch targets;
- badge placement;
- semantic clarity;
- animation/state transitions where useful.

High-frequency icons deserve particular attention:

- Like;
- Comment;
- Repost;
- Share;
- Save;
- More;
- Search;
- Notifications;
- Back;
- Send;
- Add/create;
- Camera/media;
- Voice/video call;
- Story/Play;
- navigation destinations.

Do not leave mixed legacy icon families on high-frequency screens after declaring the icon pass complete.

### 6.5 Motion and tactile feedback

Motion should support meaning, not merely prove animation exists.

Audit:

- pressed states;
- selection;
- Like/reaction feedback;
- double-tap feedback;
- navigation transitions;
- media opening/closing;
- bottom sheets;
- Share/Repost transitions;
- comment/reply insertion;
- optimistic state updates;
- progress states;
- failure rollback;
- reduced-motion behavior.

Use existing `NovaMotion` ownership rather than screen-local random timings.

### 6.6 Density and repetition

Repeated events should not each consume oversized cards by default.

Examples include Orbit/Activity follow events and repeated social notifications. Group, compress or redesign when that improves scanability without hiding important information.

---

## 7. Social core: Home, Feed and Posts

This is the highest-priority product loop.

### 7.1 Home

Current Home has some of Nova’s strongest identity work, including the time-aware greeting, Tonight hero and Orbit language. Do not assume that means the full Home is finished.

Audit:

- top chrome and identity strength;
- Tonight hierarchy and real usefulness;
- transition from hero identity into ordinary feed;
- information density;
- scroll rhythm;
- repeated rails;
- whether Feed feels secondary to decorative product chrome;
- whether returning users can reach content quickly;
- empty/low-community states;
- loading and refresh behavior.

Home should feel alive even when community density is modest, without fabricating activity.

### 7.2 Post card / feed item

The current Post presentation still feels weaker and more generic than the best Home identity surfaces.

Reconsider the full Post composition:

- author header;
- avatar and identity metadata;
- timestamp/context;
- text hierarchy;
- photo/video geometry;
- repost attribution;
- multi-line caption treatment;
- overflow actions;
- action row;
- counts;
- comments preview;
- save/share affordance;
- spacing between consecutive posts;
- visual treatment of own content vs normal content only where useful.

The action row in particular must not look like leftover utility icons under a redesigned shell.

### 7.3 Like interaction

Treat Like as one product interaction across all media families.

Audit:

- tap;
- double tap;
- animation;
- icon morph/fill;
- count change;
- optimistic behavior;
- network failure rollback;
- repeated rapid taps;
- haptic feedback where appropriate;
- accessibility semantics.

Double-tap must feel intentional and satisfying rather than accidental or visually awkward.

### 7.4 Repost

Repost should communicate clearly:

- who reposted;
- what original content is;
- whether the user is reposting instantly or composing context;
- undo/remove behavior;
- counts and state;
- how reposted content appears in Feed and Profile.

Avoid tiny cryptic glyphs or weak attribution.

### 7.5 Share

Redesign Share as a coherent flow rather than merely a system sheet entry.

Audit:

- entry affordance;
- Nova-internal sharing;
- conversation/Room recipient selection if supported;
- external Android share;
- repost distinction;
- recent recipients;
- search;
- confirmation/feedback;
- failure handling;
- dismissal;
- keyboard behavior when search is involved;
- consistent icons.

Do not add fake recipient or messaging behavior.

---

## 8. Comments and replies

Comments are a high-frequency social surface and should be treated as a real product, not a utility page.

### Known real-device concern

The current comments screen can present a poor keyboard/composer experience where the keyboard occupies a large portion of the screen while the actual composer/state feels missing, displaced or weak.

Do not solve this only with inset padding.

Audit and polish:

- persistent composer placement above IME;
- focus behavior;
- keyboard opening/closing;
- send action;
- disabled/loading state;
- optimistic insertion;
- network failure and retry;
- draft preservation during temporary navigation;
- reply targeting/context;
- nested replies;
- comment/reply likes;
- counts;
- deletion/moderation affordances where authorized;
- author distinction;
- timestamps;
- long text;
- emoji;
- RTL;
- font scaling;
- empty state;
- loading state;
- media/GIF support if product/backend support is real or can be implemented end-to-end safely.

If richer comment media such as GIFs is introduced, it must be a truthful end-to-end capability with moderation and loading behavior, not a decorative control.

Comments on Posts and comments on Reels should share a coherent interaction language while respecting different presentation contexts.

For Reels, evaluate whether comments should appear as a sheet/overlay that preserves video context rather than forcing a generic disconnected page.

---

## 9. Reels and video product quality

Reels must be treated as a complete media product, not just a dark screen with playback.

### Confirmed real-device defects to investigate

Real-device testing reported all of the following and they must be reproduced/understood rather than dismissed as cosmetic:

1. selecting one ordinary video during Create/Upload can be rejected while another seemingly similar random video is accepted, with no clear explanation;
2. an accepted uploaded video can later display a **black visual frame while audio plays**;
3. playback can run for roughly several seconds, show buffering/loading, restart from the beginning and repeat;
4. acceptance/validation behavior is not sufficiently understandable to the user.

Treat these as product-quality defects in the upload/transcode/playback pipeline.

Audit:

- Android picker inputs;
- MIME/container/codec handling;
- file-size/duration validation;
- rotation/orientation metadata;
- server upload responses;
- storage headers/content type;
- thumbnail/poster generation;
- supported codecs;
- Media3/ExoPlayer configuration;
- buffering strategy;
- range requests/CDN/storage behavior;
- lifecycle/recomposition effects;
- player reuse;
- looping semantics;
- audio/video track selection;
- first frame;
- failure UI;
- retry behavior.

Do not merely increase a size limit unless evidence shows that is the defect.

### Reel visual/interaction pass

Review:

- full-screen composition;
- author/caption placement;
- readable overlays over bright/dark media;
- Like/Comment/Repost/Share/Save icon language;
- gesture language;
- comment entry;
- share entry;
- buffering indicators;
- pause/play affordance;
- scrubbing only if appropriate;
- transition between Reels;
- published-state consistency with uploaded preview;
- safe areas and system bars.

The owner’s complaints about one Reel control should be interpreted as a reason to audit all Reel controls and equivalent Post controls.

---

## 10. Stories

The current Story Viewer is functional but **must not be treated as an approved final visual bar**.

Audit the concept as a whole:

- media framing;
- progress bars;
- author identity;
- close/back behavior;
- reply composer;
- keyboard interaction;
- quick reactions;
- View original post card when applicable;
- share/repost behavior;
- text readability;
- taps/swipes/hold-to-pause;
- transition between stories;
- failure/loading;
- own-story actions;
- story creation flow;
- photo/video/text consistency.

The aim is an immersive but unmistakably Nova viewer, not a generic clone with Nova colors.

---

## 11. Create / composers

The current Create hub is clear and functional but risks feeling like a stack of white rounded system cards rather than a creative social entry surface.

Audit:

- Post;
- Story;
- Pulse;
- Room;
- Memory;
- Reel;
- hierarchy between frequent and secondary creation actions;
- media-first vs text-first entry;
- whether the user should see every creator equally at all times;
- animation/transition from Create into the chosen composer;
- drafts/progress/errors;
- account/avatar context.

### Post composer

Review:

- image/video selection truthfulness;
- preview quality;
- replacement/removal;
- caption composition;
- character count behavior;
- upload progress;
- retry;
- disabled state;
- success transition into real posted content;
- keyboard handling;
- accessibility.

A large empty upload card should not be accepted merely because it is clean.

---

## 12. Profile and edit profile

The Profile foundation is coherent but can still feel generic.

Audit:

- identity hierarchy;
- avatar/orbit treatment;
- bio;
- handle;
- location;
- interests;
- external link;
- counts;
- edit action;
- post/reel/repost tabs;
- media grid density;
- own vs other-user profile consistency;
- follow/message/call actions on other profiles.

### Confirmed functional defect

Real-device testing showed that a link entered in Edit Profile can be saved but **does not appear on the Profile surface**.

Trace the entire path:

- field state;
- validation;
- request payload;
- backend persistence;
- public/private profile response DTO;
- parser/domain model;
- UI rendering;
- clickable link behavior;
- safe URL handling.

Add regression coverage where practical.

Do not declare Edit Profile complete while persisted fields disappear from the visible profile.

---

## 13. Orbit, activity and notifications

Orbit is one of Nova’s strongest unique concepts, but repeated activity presentation currently becomes visually heavy.

Audit:

- orbit visualization and real relationship meaning;
- filters;
- active/recent semantics;
- repeated follow/like/repost events;
- event density;
- grouping;
- avatar repetition;
- action affordances;
- unread/read distinction;
- navigation into the relevant object;
- relationship between Orbit and Notifications.

Avoid one oversized rounded card for every tiny social event when a more scannable composition is possible.

Notifications/Activity should feel like a social information surface, not a form-generated list.

---

## 14. Rooms

Rooms have meaningful product depth and should visually communicate a shared place rather than a generic settings/detail page.

Audit:

- Room identity/header;
- member strip;
- owner/admin distinctions;
- description editing;
- Add to Room;
- Notes;
- Photos;
- Videos;
- Music;
- Plans;
- Saved;
- filtering tabs;
- timeline items;
- empty sections;
- live/chat entry;
- public discover/follow/join states;
- admin controls;
- Room cards in rails/lists.

Look for more expressive but restrained spatial composition, especially where the current screen is a stack of bordered containers/chips.

Do not sacrifice clarity of permissions or membership state for visual novelty.

---

## 15. Messages, Inbox and call events

Messaging is functional but must reach the same product-quality bar as the strongest Home identity surfaces.

Audit:

- conversation header;
- message bubbles;
- timestamps/read state;
- replies;
- reactions;
- media;
- voice messages;
- composer;
- keyboard/insets;
- attachment flow;
- group identity;
- search;
- unread/mentions;
- call entry;
- call event cards in message history.

Call-history events should feel like deliberate message objects and clearly distinguish:

- completed call and duration;
- no answer;
- canceled;
- missed/incoming/outgoing semantics where available.

### Call reliability regression requirement

Recent infrastructure work added/validated TURN relay support and Redis/realtime configuration because real calls could connect and then lose audio intermittently. Product polish must not break this.

When touching calls/WebRTC, validate real connection lifecycle and audio continuity, not only UI state.

---

## 16. Memories

Memories/Memory Film recently exposed several real defects that have been fixed and should now be treated as regression contracts:

- Android/backend film-plan route mismatch produced a raw HTML 404;
- Media3 Transformer was accessed from the wrong thread;
- completed WorkManager identity could reattach stale output after plan changes;
- fast foreground rendering could complete before the user-visible FGS notification appeared;
- the foreground notification was adjusted to request immediate display for a real user-initiated media-processing task.

Do not regress these behaviors.

Further polish should evaluate:

- weekly recap emotional hierarchy;
- selection/storyboard clarity;
- render progress;
- cancel/retry;
- leaving and returning during render;
- ready preview;
- share/save;
- empty/quiet weeks;
- transitions between weeks;
- distinction between generated recap, authored Memory and Memory Film.

The feature should feel intentional even when only one scene exists.

---

## 17. Settings, privacy and secondary surfaces

The large settings expansion is valuable, but secondary product surfaces still need the same quality discipline.

Audit:

- Settings hierarchy;
- section density;
- row icon consistency;
- switches;
- destructive actions;
- Privacy;
- Notifications preferences;
- Security;
- Login activity;
- Blocked accounts;
- Data & Storage;
- language/appearance status;
- Help/About.

Do not redesign these into flashy social surfaces. The goal here is clarity, confidence, consistency and low cognitive load.

Two-factor authentication and social OAuth remain separate product/security/external-configuration decisions unless their governing blockers have genuinely been resolved.

---

## 18. Loading, empty, error and retry states

Every polished flow must include its non-happy states.

Audit:

- initial loading;
- pagination loading;
- refresh;
- empty community;
- empty search;
- permission denial;
- network loss;
- 401/session expiry;
- upload failure;
- media decode/playback failure;
- server validation error;
- retry;
- optimistic rollback;
- partial content availability.

Never expose raw HTML server errors to the user.

Avoid generic full-screen spinners when a contextual skeleton or local progress state is better.

Errors should explain what happened and what the user can do next without leaking sensitive internals.

---

## 19. Keyboard, insets and Android-native behavior

Keyboard/IME quality is a cross-app requirement, not a per-screen patch.

Test all text-entry surfaces:

- Comments;
- Messages;
- Story replies;
- Share search;
- Post caption;
- Reel caption;
- Pulse composer;
- profile editing;
- Room notes/plans;
- search.

Validate:

- composer remains visible;
- no content is permanently obscured;
- focus behavior is stable;
- Back dismisses in the expected order;
- navigation does not trap the IME;
- system bars/insets remain correct;
- orientation/font scaling do not break layouts.

Use proper Compose/Android inset handling rather than magic offsets.

---

## 20. Accessibility and internationalization

Product polish is incomplete without:

- meaningful content descriptions;
- touch targets;
- state not communicated by color alone;
- large font behavior;
- contrast;
- reduced motion;
- RTL review;
- Arabic/English layout sanity;
- truncation behavior;
- keyboard navigation where relevant;
- TalkBack-friendly semantics for custom controls.

Do not break the warm/quiet visual identity to satisfy accessibility; design it correctly within that identity.

---

## 21. Engineering quality is part of product quality

Visual work may reveal structural defects. Fix them at the correct owner rather than adding UI workarounds.

Examples:

- if Reel playback is broken because storage/range/codec behavior is wrong, fix that contract rather than hiding buffering;
- if Profile link data is lost in a DTO mapper, fix the mapper rather than duplicating the link locally;
- if a keyboard bug is caused by navigation/window-inset ownership, fix that ownership;
- if equivalent social actions have divergent implementations, prefer a stable shared primitive when semantics are genuinely shared.

Do not over-generalize unrelated features merely to make code look abstract.

Preserve narrow ownership and existing architecture gates.

---

## 22. Real-device QA is mandatory

Compile success and unit tests are necessary but **not sufficient**.

Definition of Done for a polished flow requires evidence across the real user journey.

At minimum, validate relevant paths on the project’s real Samsung Android device profile and through the existing Samsung smoke checklist where applicable.

For media flows use real files with variation:

- image;
- short video;
- longer video;
- different aspect ratios;
- different codecs/containers when available;
- reasonably large files;
- rotation metadata;
- background/foreground transitions;
- slow or interrupted network where practical.

For social flows test both:

- empty/small data;
- populated data;
- own content;
- other-user content;
- optimistic success;
- failure/retry.

When a bug is discovered during real-device testing, capture the root cause and add regression coverage where practical before closing the flow.

---

## 23. What “world-class icons” means in this project

The icon goal is deliberately ambitious because icons appear in almost every touchpoint.

A completed iconography pass must produce:

- a clearly Nova-owned catalog;
- consistent family/style;
- predictable active/fill behavior;
- good optical centering;
- consistent touch targets;
- clear semantics at a glance;
- no random mixture of Material defaults, legacy custom glyphs and unrelated visual weights;
- consistent use in Post, Reel, Story, Comments, Messages, Rooms, navigation and Settings;
- intentional special cases only where a feature genuinely needs a distinct symbol.

The implementation may use existing vector foundations, custom vectors or carefully selected system/material primitives where they visually belong. The requirement is product coherence, not ideological rejection of Material icons.

---

## 24. Product taste rules

Avoid these failure modes:

- “everything is a card”;
- “everything is 28dp rounded because that is the token”;
- giant empty space presented as premium minimalism;
- decorative controls with no real behavior;
- fake live state;
- unreadable text over media;
- weak gray metadata everywhere;
- icon-only controls whose meaning is unclear;
- motion for motion’s sake;
- duplicating Instagram/TikTok mechanically instead of solving Nova’s own product problem;
- redesigning a strong unique Nova concept into a generic social template;
- treating a mockup screenshot as more authoritative than real product usability.

Prefer:

- clear hierarchy;
- strong media;
- thoughtful density;
- calm surfaces punctuated by meaningful identity moments;
- simple controls with excellent states;
- distinctive relationship/live semantics;
- fewer but better visual primitives;
- interaction feedback that feels immediate and alive.

---

## 25. Execution strategy — do not perform another mega-pass

The next phase should be implemented flow-by-flow.

Each flow can still be substantial and autonomous. The point is to give the agent enough room to solve the problem deeply while preventing a 100+ file sweep where quality becomes uneven.

Recommended order:

### Flow 1 — Social core

**Home/Feed → Post → Like/Repost/Share → Comments**

Includes the cross-app iconography foundation needed for these actions, keyboard/composer quality and real social-state QA.

### Flow 2 — Create + media pipeline

**Create → Post/Reel selection/upload → publish → playback**

Prioritize the confirmed video rejection/black-frame/buffering defects before cosmetic polish.

### Flow 3 — Reels + Stories

Immersive media presentation, gestures, actions, comments/share surfaces and story viewer/composer.

### Flow 4 — Profile + people

Own/other profile, edit profile, link bug, grids, connections and people discovery/search.

### Flow 5 — Orbit + activity

Orbit visualization, event density, notifications/activity and relationship-state polish.

### Flow 6 — Inbox + messaging + calls

Conversation quality, composer/media/reactions/call events and call reliability regression checks.

### Flow 7 — Rooms

Room identity, detail/timeline, members, Add-to-Room, public discovery/join/follow and live/chat handoffs.

### Flow 8 — Memories

Weekly recap, Memory Film, drafts, progress/reattachment, preview/share and recent FGS/media-processing regression contracts.

### Flow 9 — Settings/account polish

Secondary surfaces, consistency, accessibility and final cross-app cleanup.

### Final pass — coherence and QA

Only after focused flows are complete:

- app-wide visual coherence audit;
- icon catalog audit;
- motion audit;
- accessibility/RTL/font scaling;
- Samsung real-device smoke;
- screenshot/visual regression coverage where useful;
- remove temporary/legacy visual seams proven unused.

The order may change if an audit proves a more severe user-facing defect should be fixed first, but the flow boundary principle should remain.

---

## 26. Agent autonomy: strict on outcomes, flexible on solutions

Do not over-specify pixel values from this brief unless they are existing approved design-system contracts.

The agent should be free to choose the strongest implementation after inspecting current code and state ownership.

Good requirement:

> Comments must preserve a visible, stable composer above the keyboard, maintain conversation context, handle send/loading/failure correctly and share a coherent language between Posts and Reels.

Bad requirement:

> Make the composer 56dp tall with 18dp radius and 12dp margin.

Good requirement:

> Establish one coherent Nova social-action icon family with deliberate active states across Post and Reel.

Bad requirement:

> Replace the Like icon with a specific arbitrary vector without auditing its family.

Be strict about the user experience and acceptance criteria. Leave room for engineering/design judgment in how to achieve them.

---

## 27. Required workflow for each Codex implementation task

For every flow:

1. Fetch and inspect current `master`; do not work from an old checkpoint.
2. Read this brief plus the governing architecture/design records relevant to the flow.
3. Inspect the live implementation, state owners, repositories, APIs and tests before proposing changes.
4. Identify confirmed defects separately from visual/product opportunities.
5. Trace each confirmed defect to root cause before applying a workaround.
6. Audit equivalent surfaces using the “examples are concepts” rule.
7. Preserve protected architecture/security/behavior contracts.
8. Implement one coherent flow deeply.
9. Add/update focused regression tests where practical.
10. Run relevant local/hosted checks.
11. Perform or prepare explicit real-device QA steps.
12. Record what changed, what was tested and any genuine remaining blocker.
13. Open one focused PR. Do not silently expand into an unrelated app-wide rewrite.

---

## 28. Definition of Done for a flow

A flow is not done because:

- it compiles;
- screenshots look cleaner;
- colors use tokens;
- the happy path works once;
- a single literal complaint was patched.

A flow is done when all applicable criteria are true:

- high-frequency user journey works end-to-end;
- confirmed bugs in scope have root-cause fixes;
- equivalent surfaces were audited;
- visual hierarchy is deliberate;
- iconography is coherent;
- interaction feedback is polished;
- keyboard/insets are correct;
- loading/empty/error/retry states are usable;
- optimistic updates and rollback behave correctly;
- accessibility and common scaling/RTL concerns are checked;
- real-device behavior is validated where applicable;
- no architecture/security contract is weakened;
- no fake feature or decorative dead control is introduced;
- regression coverage exists where practical;
- the result feels designed, not merely tokenized.

---

## 29. Current known product observations from real-device review

This section is intentionally explicit so future agents do not reinterpret PR #205’s completion matrix as proof of final quality.

Observed strengths:

- product-wide consistency is substantially improved;
- Nova now visibly has a design system and coherent navigation;
- Home/Tonight/Orbit introduce a recognizable relationship/live identity;
- Settings and secondary functionality are much more complete;
- Rooms/Memories/Pulse/Privacy product breadth is real rather than decorative;
- the app feels significantly more mature than before the convergence work.

Observed weaknesses/opportunities:

- the final visual result is less striking than the “Full Visual Convergence” name implies;
- many screens rely too heavily on the same white rounded-card formula;
- Feed/Post social chrome still looks comparatively generic;
- Like/Comment/Repost/Share icon/action language needs a real global pass;
- Share/Repost presentation does not yet feel premium;
- Story Viewer is functional but not an untouchable visual reference;
- Comments need stronger product design and keyboard/composer behavior;
- Orbit activity/event lists can feel repetitive and oversized;
- Profile is coherent but still generic in places;
- Create is clear but can feel like a system menu rather than a creative social surface;
- Messages/call-history presentation is usable but below the strongest Home identity moments;
- the app should preserve architectural sophistication while raising taste, interaction quality and media reliability to the same level.

Confirmed/credible defects requiring investigation or regression protection:

- inconsistent video selection/upload acceptance;
- accepted video can render black while audio plays;
- video playback can buffer/restart in a short loop;
- saved profile link may not appear on Profile;
- Comments keyboard/composer UX issue;
- call audio continuity historically unstable under some network conditions; TURN/realtime improvements must be regression-tested;
- recent Memory Film route/thread/work-identity/FGS-notification issues were fixed and must stay fixed.

---

## 30. First implementation task after this brief

The preferred first execution task is:

> **Product Polish Flow 1: Home/Feed → Post → Like/Repost/Share → Comments**

The implementing agent must first audit the current production paths and then implement the flow end-to-end to this brief’s Definition of Done.

Do not start by editing every screen in Nova.

Do not treat existing screenshots as immutable requirements.

Do not declare completion until the social core feels like one deliberate product and the critical interactions have real QA coverage.

---

## 31. Suggested Codex handoff prompt

Use a short task prompt because this document is the persistent context:

> You are taking over Nova Product Polish on the current `master` branch. Read `docs/design/NOVA_PRODUCT_POLISH_BRIEF.md` completely, then read the governing architecture/design records it references and inspect the current production implementation. PR #205 is the foundation, not the finish line. Execute **Flow 1 only: Home/Feed → Post → Like/Repost/Share → Comments**. Treat product-owner examples as evidence of broader product principles and audit equivalent surfaces, but do not expand into unrelated flows. Preserve protected architecture/security contracts. Fix root causes, not cosmetic symptoms. Implement to the brief’s Definition of Done, add focused regression coverage, run the relevant validation, record real-device QA requirements, and open one focused PR. Do not touch Play version/signing/release behavior unless this flow genuinely requires it.

---

## 32. Completion boundary for the overall polish phase

This phase is complete only when the major flows above have been individually polished and then audited together.

There is no target number of commits, PRs or changed files.

The finish line is product quality:

**Nova should feel architecturally mature, visually intentional, interaction-complete and reliable on a real Android device — with a recognizable identity that survives beyond the Home hero.**
