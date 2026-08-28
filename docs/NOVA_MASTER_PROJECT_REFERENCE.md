# NOVA MASTER PROJECT REFERENCE
## Product vision · visual direction · architecture · flow history · workflow · QA · handoff

**Repository:** `omarkhair-labs/nova`  
**Primary Android application ID:** `com.omarkhair70.nova`  
**Purpose:** Permanent continuity pack for future ChatGPT/Codex sessions so Nova does not depend on one giant chat.

---

# 0. How to use this file

This is not a generic summary and not a new brief. It reconstructs the actual Nova working model through Flow 7.

When continuing Nova:

1. Read this file completely.
2. Inspect live `master`, open PRs and newest CI before changing anything.
3. Treat live repo/PR/CI as higher authority than this file.
4. Do not restart completed architecture/design phases.
5. Never claim `DEVICE VERIFIED` from CI alone.

**Source-of-truth order:** live repo/PR/CI → governing repo docs → feature architecture contracts → this file → older research/chat summaries → assumptions.

---

# 1. Live checkpoint

## Flow 7 is merged

Current verified `master`:

`6553d60c12998bf36e3502511a8bf22b118b493f`

Merge:

`Merge pull request #213 from omarkhair-labs/codex/product-polish-flow-7`

PR #213 final implementation commits:

- Flow 7A: `40f56d54da0832bc917eae8fa66cf7590088a309`
- Flow 7B: `da15b73c73b10418bda2adb61a50fc06480277f2`
- CI/design-gate alignment: `ac0e32641d40d9234b18e88716f4b3a99cb41730`

The initial Flow 7 hosted Android run failed at:

`Check alive feature design system`

with:

`Rooms screen bypassed shared ordinary-screen presentation: NovaCard(`

That was handled as a bounded Flow 7 design-system gate alignment rather than a product rollback.

**Post-merge Nova CI #687: SUCCESS.**

Therefore:

- Flow 1–7 implementation is merged.
- Flow 7 is still **NOT DEVICE VERIFIED**.
- Next planned implementation: Flow 8, then Flow 9.
- After Flow 8/9: Final Whole-Product Coherence Pass.

Flow 8 must start from the exact current merged `master` head, not an older Flow 7 branch.

---

# 2. What Nova is

Nova’s code-owned design-system statement:

> **Nova is the social app that remembers your life with your people.**

The current product-polish target:

> Take Nova from a broad, coherent, technically mature alpha into a recognizable, premium, intentional social product whose high-frequency interactions, media behavior, visual language and real-device reliability feel finished.

Nova should not feel like:

- an AI-generated stack of rounded Material cards;
- an Instagram/TikTok/Threads clone recolored purple;
- a feature checklist of disconnected surfaces;
- a premium shell hiding generic inner flows.

The target is:

- recognizable;
- calm but alive;
- content-first;
- relationship-centered;
- media-reliable;
- interaction-complete;
- internally coherent;
- mature on flagship and secondary surfaces;
- distinctive through repeated product grammar rather than visual noise.

---

# 3. Product content grammar

These formats are intentionally different and must not collapse into one another.

## Post

Persistent feed content. Text/caption, photo/video, comments, likes, repost/share, detail, profile/feed continuity.

## Story

Personal/narrative ephemeral content, approximately 24 hours.

Photo/video/text with Followers / Close Friends audience.

**Stories stay in the Create ecosystem. Do NOT add a permanent Stories rail to Home.**

## Pulse

Nova’s “right now” format, approximately 12 hours.

Can contain text/photo/video and contextual modes such as Live / Music / Talks / Vibes.

Pulse is conversational/live and temporal.

**Pulse is NOT a 4-second mini-video format.**

No intentional 4-second cap was found. Do not claim the historic playback issue came from an intentional short-duration limit.

## Reel

Short vertical immersive media with stronger media dominance and full-screen interaction.

## Room

A shared social place, not merely a group chat and not merely a settings/detail page.

Messaging owns group creation/membership/roles/chat/realtime.

Rooms owns identity/discovery/following/items/sections/plans/reminders/media/Tonight composition.

## Memory

Retrospective/reflective experience over Nova life that already happened.

## Memory Film

Rendered retrospective film built from a server film plan and Android render pipeline.

It is a protected immersive feature and must not be casually rewritten during visual polish.

## Create

Create is a **rich Nova creation hub**, not a bare plus-menu launcher.

---

# 4. Home hierarchy

Approved Quiet Orbit hierarchy:

1. identity header
2. greeting + orbit status
3. Tonight hero
4. feed
5. Pulse realtime strip
6. Your Orbit relationship strip
7. Rooms
8. Memories
9. bottom navigation

Rules:

- Home should feel like **life happening in front of you**.
- Feed content must not be buried under decorative product chrome.
- Stories do not become a permanent Home rail.
- Rails/cards are contextual entry points, not duplicate full features.
- Do not fabricate community activity or presence to make Home feel busy.
- Do not attribute the historic lead-Post placement under Tonight to Flow 1; that composition pre-existed that PR’s core ownership.

---

# 5. Approved visual direction — Quiet Orbit

Governing doc:

`docs/design/NOVA_VISUAL_IDENTITY.md`

Status:

**approved visual target for implementation**

Nova does **not** need Design System V2.

The selected identity is:

# Quiet Orbit

Mood:

- calm
- intimate
- alive
- slightly cosmic
- relationship-oriented
- not “space themed”
- not neon/gaming
- not visually loud

## Recognition stack

A Nova screen should remain recognizable with the wordmark hidden through:

1. **Warm Canvas**
2. **Nova Violet**
3. **Orbit relationship motif**
4. **Rounded-line Nova-owned icons**
5. **Soft spatial motion**
6. **Content-first media**

The orbit motif is semantic, not wallpaper.

Use it for:

- relationship
- live presence
- selection
- progress

## Base palette

- canvas: `#FAFAF8`
- surface: `#FFFFFF`
- Nova signal violet: `#6554E8`
- live cyan: `#58C9D8`
- dark/night foundations are feature-owned

Rules:

- Violet is the primary global chromatic signal.
- Cyan is rare and reserved for realtime/live semantics.
- User photos/videos are never tinted merely to manufacture brand identity.
- Ordinary product surfaces remain warm and content-first.

## Feature moods

- ordinary Nova — warm canvas / white surface / violet signal
- Tonight — strongest dark cinematic living-night identity
- Pulse/media — immersive dark media where semantically appropriate
- Orbit — relationship/orbit signature
- Rooms — shared-place social identity
- Memories — warmer reflective counterweight

One Nova, multiple named moods.

---

# 6. Visual research / reference model

A persistent research artifact exists in ChatGPT Library:

**`Nova Final Visual Identity — Post-DS4 Direction and Migration Plan`**

The research referenced:

- Material 3 / M3 Expressive — implementation substrate
- Pinterest Gestalt — design-system governance discipline
- Instagram / Threads — low-chrome content-first restraint
- TikTok — media behavior and motion as brand signatures
- Apple HIG / SF Symbols — precision, accessibility, icon geometry and predictable motion

Core lesson:

> Mature products do not win by stacking dozens of brand devices. They repeat a small number of stable signals.

Directions compared:

## Quiet Orbit

**Selected.**

Best continuity with existing Nova, strongest cross-feature flexibility, lower accessibility risk and highest practical ownability.

## Afterglow

Not selected app-wide.

Useful only as inspiration for Tonight / immersive Pulse.

Risk: neon/gaming feel, higher stimulation, higher contrast/overlay QA cost.

## Signal Paper

Not selected.

Warmer/editorial/tactile, but weaker continuity with the existing Nova system.

Final recognition chain:

> **Warm Canvas → Nova Violet → Orbit Motif → Rounded-Line Icons → Soft Spatial Motion → Content-First Media**

---

# 7. Design system contract

Governing doc:

`docs/design/NOVA_DESIGN_SYSTEM.md`

DS-1 through DS-4 are complete.

Material 3 remains the rendering engine. Nova owns the semantic product layer above it.

Principles:

1. One Nova, multiple moods.
2. Semantic roles over raw local values.
3. Shared presentation, feature-owned state.
4. Accessibility is part of the system.
5. Immersive exceptions stay explicit.

Use existing owners:

- `NovaType`
- `NovaSpacing`
- Material/Nova shapes
- `NovaMotion`
- semantic colors
- named feature palettes
- `NovaIconAsset`
- shared feedback/components

Stable presentation seams include:

- `NovaPrimaryButton`
- `NovaSecondaryButton`
- `NovaTextField`
- `NovaHeader`
- `NovaBottomBar`
- `NovaIconButton`
- `NovaBackButton`
- `NovaAvatar`
- `NovaCard`
- `NovaUnreadDot`
- `NovaLoadingState`
- `NovaInlineLoading`
- `NovaEmptyState`
- `NovaErrorState`
- `NovaInlineRetry`
- `NovaActiveCallPill`
- shared media/profile primitives

Do not create a generic component dumping ground.

## Icon rule

Ordinary reusable chrome uses Nova-owned vectors.

- no unnecessary `Icons.*` drift on migrated surfaces
- no Unicode/text glyph as reusable ordinary action icon when a Nova vector exists
- meaningful icon actions require accessibility semantics

## Foundation rhythm

- spacing: 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32dp
- shape family: 10 / 14 / 18 / 22 / 28dp
- sparse elevation
- semantic motion roles

Motion grammar:

> **approach → orbit → settle**

---

# 8. Product-polish rules

Governing doc:

`docs/design/NOVA_PRODUCT_POLISH_BRIEF.md`

## Examples are concepts, not isolated tickets

If the product owner identifies one weak Like icon, Share screen, comments keyboard state or Reel defect, audit the whole equivalent interaction family.

Do not patch one literal example while leaving the same weakness elsewhere.

## Reduce over-cardification

A container must earn its existence.

Do not turn every block into the same white surface + thin border + giant radius + stacked-card grammar.

Use hierarchy, whitespace, separators, media edges, typography and contextual surfaces intentionally.

## Content-first media

Photos/videos are the content, not attachments trapped inside chrome.

Protect:

- crop/aspect
- first frame/poster
- loading/error/retry
- full-screen continuity
- user media color integrity

## Competitive references

Use mature products to study solved interactions, not as visual templates.

Ask:

> What makes this interaction feel solved and mature, and what is the strongest Nova-specific solution consistent with Nova identity and architecture?

---

# 9. Root navigation truth

Approved target root language:

`Home / Orbit / Create / Inbox / Profile`

This is behavioral navigation, not relabeling.

Discovery/People remains a real secondary route.

Reels remains a real deep surface reached from Create/content entry points.

---

# 10. Architecture philosophy

Nova already completed a staged architecture consolidation.

It was:

- not a rewrite
- behavior-preserving
- ownership-oriented
- sequential
- protected by architecture gates

Core principle:

> Feature state/domain ownership stays feature-owned. Shared UI owns repeated presentation.

Do not reintroduce:

- route-local repositories
- route-local network parsing
- duplicate feature models
- shadow repositories
- compatibility shims already removed
- generic God files
- parallel design-system foundations

---

# 11. Application / release identity

Current repository configuration:

- namespace: `com.nova.app`
- applicationId: `com.omarkhair70.nova`
- minSdk: 26
- targetSdk: 36
- versionCode: `20108`
- versionName: `2.1.8`

Protected during ordinary polish:

- applicationId
- signing identity
- versionCode/versionName
- Play track behavior
- release publishing behavior
- release workflow
- Google Play publishing scripts

Product polish does not authorize Play publishing.

---

# 12. Flow history

## Flow 1 — Social core

Merged via PR #206.

Delivered:

- cache-first account-scoped feed startup
- returning-session local hydration before remote validation
- foreground feed reconciliation
- request dedupe preserving optimistic state
- cleaner content-first Post presentation
- media tap → Post Detail
- shared 48dp social actions
- optimistic Like/Repost reconciliation + rollback
- double-tap Like only
- no repost N+1
- internal Nova share + Android share
- comment drafts / reply / IME
- pending comments / retry / optimistic reactions
- Story share success remains visibly `Added ✓`

Still device-review dependent.

## Flow 2 — Create + media pipeline

Merged via PR #207.

Delivered:

- H.264/AAC MP4 normalization
- duration verification
- first-frame JPEG extraction
- bounded source copy
- streaming upload via source file
- account-scoped idempotent WorkManager publishing
- queued/preparing/uploading/published/failed states
- bounded retry + manual retry
- session/account guards
- thumbnail-backed shared player
- playback error/retry
- full-screen Post Detail
- media metadata continuity across Feed/Profile/DM/Story/Orbit
- backend validation/media contract work

Shared foundation:

`NovaVideoPreparer`

Known review debts:

- simultaneous idempotency race may still surface an IntegrityError even though DB uniqueness prevents duplicate persistence
- persisted picker permission lifecycle may accumulate
- long background Worker behavior still needs device validation
- exact historic Pulse black/audio-only root cause was not proven down to codec/profile/timestamp vs Samsung decoder contribution

Do not overclaim the root cause.

## Flow 3 — Reels + Stories + immersive media

Reels:

- Nova-owned action language
- 48dp controls
- active/busy states
- restrained Like feedback
- pooled/shared playback
- thumbnail/buffer/error/retry continuity
- double-tap Like only

Stories:

- durable WorkManager publish
- shared video normalization
- real media preview
- truthful Followers / Close Friends
- shared player
- IME pauses timer
- video advances on real playback end
- thumbnail/error/retry
- reactions/replies/viewers/delete/shared Post/Reel preserved

Create remains rich multi-format.

Stories were not moved to Home.

Pulse remains separate.

## Flow 4 — Profile + People

Merged PR #210.

Profile:

- mature self/other identity grammar
- persisted external links render truthfully
- safe HTTP(S) handoff
- theme identity treatment
- `showOrbit` truth
- verified/location/link/private states
- Edit Profile preview
- save/IME/back behavior
- existing Posts/Reels/Reposts/follow/message/call/share/report/block preserved

People:

- real People/Nearby/Interests/Verified/New semantics
- stale search protection
- filter revision ownership
- recent handoff continuity
- explicit private/verified/follow-request states
- follow reconciliation
- denser rows
- automatic paging

## Flow 5 — Orbit + Tonight + Activity

Merged PR #211.

Orbit:

- keeps relationship/constellation identity
- denser semantic event stream
- reduced oversized activity cards
- real Post/profile handoffs
- context media
- filters/paging
- Pulse-only live marker
- no fabricated generic presence

Tonight:

- keeps local-time window
- identity hero
- Rooms handoffs
- real nearby Pulse moments
- consumes real backend thumbnail metadata
- Nova-owned Moon/Play language

Activity:

- Follow Requests prominent
- ordinary notifications compact
- semantic icons
- relative time
- non-color unread description
- mark-all-read reconciliation
- unread race protection
- paging dedupe
- stale follow-request protection
- per-item Post opening progress

## Flow 6 — Inbox + Messaging + Calls

Merged PR #212.

Flow 6 merge commit:

`6c34c189405d712a14918d378733add8515a1cf5`

Post-merge CI #684: **SUCCESS**.

Inbox:

- real All / Unread / Mentions filters
- Nova icons
- flatter/denser rows
- automatic bottom paging
- shared loading/error/empty feedback
- New Message / New Group polish
- no invented presence

Conversation preserved:

- realtime reconciliation
- drafts
- typing
- unread anchor
- paging
- pending messages
- retry
- reply/edit/delete
- reactions
- photo/voice
- shared Post/Profile/Reel
- call history

Presentation:

- Nova call/info/reply/edit/delete/photo/voice/send controls
- touch targets
- cleaner swipe/reaction/actions
- Details/group consistency

Fallback text uses `Voice message` and `Photo` rather than embedding emoji; iconography belongs to UI presentation.

Calls keep protected:

- `CallStateOwner`
- signaling
- WebRTC
- negotiation/recovery
- audio routing
- Telecom
- PiP
- reconnect
- call history

No TURN/Redis/deployment/permission/FGS contract change.

## Flow 7 — Rooms

Merged PR #213.

Implementation:

- `40f56d54da0832bc917eae8fa66cf7590088a309` — strengthen shared place identity
- `da15b73c73b10418bda2adb61a50fc06480277f2` — complete shared media spaces
- `ac0e32641d40d9234b18e88716f4b3a99cb41730` — align Rooms design-system gate with Flow 7

Merge commit:

`6553d60c12998bf36e3502511a8bf22b118b493f`

Post-merge CI #687: **SUCCESS**.

Discovery/identity:

- My Rooms / Discover / Following
- public/private truth
- topics
- member count / role
- Join vs Follow
- loading/error/empty
- stale-response protection
- stale refresh cannot restore a joined Room or undo follow

Ownership:

Messaging owns group creation/membership/roles/chat/realtime.

Rooms owns discovery/following/profile metadata/sections/items/plans/reminders/media/Tonight composition.

Room detail:

- header
- member/admin identity
- description/topics
- pinned/sections
- creator profile
- safe HTTP(S) links
- local plan times
- reminders
- automatic bounded paging
- 48dp actions
- Room-to-Chat handoff

Media root-cause fix:

Old Room video used whole-file buffering.

Flow 7 removes whole-video `readBytes()` behavior and reuses `NovaVideoPreparer`:

- bounded source
- H.264/AAC
- duration verification
- first-frame extraction for preparation validation
- streaming multipart
- temp cleanup

No fake thumbnail backend field was invented.

Composer/playback:

- real selected photo/video preview
- preparation/publishing progress
- validation/failure/retry
- real shared Nova playback
- fake published-video black `▶` placeholder removed

Flow 7 still requires Samsung confirmation for discovery/detail density, large fonts/system bars, paging/reminders, handoffs and representative device-recorded video preparation/upload/playback/lifecycle.

---

# 13. Flow 8 — Memories plan

Status: planned, not started at this reference checkpoint.

Existing foundation:

- weekly read-model
- local timezone week boundary
- bounded older weeks
- people/rooms/stats
- chronological highlights
- authored drafts
- Memory Film plan
- WorkManager render
- progress
- cancel
- reattach
- MP4 preview
- share

Audit found older presentation seams:

- `✦`
- `Open ›`
- `▶`
- `Film ›`
- back `‹`
- `☾`
- fake video placeholders
- generic Memory Draft `AlertDialog`
- raw `OutlinedTextField`
- weak “Media selected · Change”
- no strong selected-media preview

Flow 8 should mature product UI without rebuilding Memory Film infrastructure.

---

# 14. Memory Film — protected regression zone

Do not casually rewrite:

- `MemoryFilmWorker`
- work unique identity
- retry/replace semantics
- foreground-service behavior
- immediate FGS notification
- `mediaProcessing` declaration
- reattachment
- render selection/version identity
- Media3 exporter pipeline
- final MP4 ownership

Protected history includes fixes around route alias/404, Transformer main-thread behavior, demo duration, WorkManager identity/REPLACE, render version, immediate FGS notification and mediaProcessing.

Historical short SHAs remembered from the fix lineage:

- `42efc092...`
- `0a5aa7f...`
- `8aa9c178...`
- `9baf507...`
- `6a073c4...`
- `318ba1e99...`

Re-read exact full SHAs from Git before relying on them.

Visual/product polish is allowed.

Infrastructure changes require a concrete reproducible defect.

---

# 15. Flow 9 — Settings / Account / Privacy / Security plan

Status: planned.

Settings audit found:

- Account row can look interactive while `onClick = null`
- Appearance can show `Warm off-white · Light` while non-interactive
- Language can show `English` while non-interactive
- Data & storage opens Android App Details
- About Nova uses native Android dialog

Rule:

**Do not create fake settings.**

If a capability does not exist, present it truthfully or remove misleading affordance.

Do not invent dark mode, appearance themes, localization, cache controls or storage controls merely because rows exist.

Notification preferences already have real backend capabilities:

- likes/comments/shares
- mentions/tags
- followers
- messages
- live sessions
- reels/stories
- events/spaces
- product updates

Privacy must preserve:

- private account
- received/sent follow requests
- Close Friends
- Story audience
- follower search/paging
- session expiry
- Profile/People privacy truth

Security must preserve:

- password reset/change
- signed-in devices/sessions
- revoke other sessions
- app lock
- account deletion
- blocked accounts
- session expiry

Blocked Accounts older seams include large cards, text `✓` empty state and text `…` busy state.

---

# 16. Final whole-product coherence pass

There is no automatic “Flow 10.”

After Flows 8/9 are stable, traverse the whole product.

Final question:

> Can a normal user wander through Nova and still find an obvious part that feels old, generic, broken, strangely inconsistent or unfinished?

Check:

- icon catalog
- action states
- legacy components
- over-cardification
- navigation/handoffs
- sheets/dialogs/menus
- loading/error/empty
- keyboard/insets
- media previews/playback
- accessibility
- RTL
- font scaling
- immersive surfaces
- system bars/safe areas
- Samsung smoke run
- dead/duplicate controls
- stale copy
- raw backend errors
- Memory Film regressions
- TURN/realtime regressions
- auth/privacy regressions
- release-sensitive behavior

---

# 17. Media pipeline rules

`NovaVideoPreparer` is the shared prepared-video path.

It:

- copies a picked source into bounded temp storage
- verifies a video track
- transforms to H.264/AAC
- reports progress
- validates output size
- verifies duration preservation
- extracts a first frame
- returns prepared files
- supports cleanup

Allowed duration loss is bounded approximately by:

`max(500ms, 5%)`

Avoid full-video byte arrays.

Prefer streaming/source-file upload.

Playback should use shared Nova surfaces with real buffering/error/retry and truthful poster/thumbnail behavior where supported.

Do not leave a black box + text `▶` as “video support.”

---

# 18. Rooms / Messaging ownership

Messaging owns:

- group creation
- membership
- roles
- chat
- realtime

Rooms owns:

- Room identity/profile
- public/private discovery
- Following
- Room items
- sections
- plans
- reminders
- media
- Room Tonight composition
- Room-specific presentation

Do not let Rooms become a second messaging stack.

Do not let Messaging absorb Room product identity.

---

# 19. Orbit / Tonight / presence truth

Do not invent activity/presence.

- Pulse is the valid live-marker source where implemented.
- ordinary relationship events do not imply fake live presence.
- Tonight follows real backend/time-window semantics.
- missing Pulse-specific route must fall back truthfully rather than create dead navigation.
- ring color alone must not be the only state signal.

---

# 20. Privacy / trust / safety

Visual polish must not weaken:

- blocked-account rules
- private-account rules
- Close Friends
- follow requests
- content visibility
- reporting/blocking
- group roles
- session expiry
- account deletion semantics

Do not hide destructive/security meaning for visual simplicity.

---

# 21. Release / Play protected boundary

Do not touch unless a dedicated task explicitly authorizes:

- release signing
- upload keys
- versionCode/versionName
- Play track
- Play publish
- release workflow
- product package identity

Manifest/release-sensitive foreground-service contracts for phone calls/media processing are not ordinary visual-polish scope.

---

# 22. Local Nova Dev checkout — do not damage

Separate local development checkout:

`D:\projects\Nova`

There is a real debug Firebase config for the `.dev` setup.

Hard rules:

- do not inspect/copy/expose the real debug Firebase file
- do not delete/overwrite/stash/drop it
- do not use `git reset --hard` in that dirty checkout
- Codex works in isolated worktree
- CI may use a repository-owned temporary fixture only
- temporary CI Firebase fixture must be removed before commit

---

# 23. Infra / realtime protected boundary

Nova has working external infrastructure around backend, database, Redis, realtime, TURN/coturn and WebRTC.

Rules:

- do not touch TURN/Redis/WebRTC because a UI flow happens to involve calls
- infrastructure changes require concrete evidence
- do not claim production deployment freshness unless verified
- never expose secrets

---

# 24. Working model that proved effective

## ChatGPT / product-owner side

Owns:

- product intent
- quality bar
- audit
- architecture boundary interpretation
- flow scope
- live GitHub/CI review
- failure diagnosis
- prompt construction
- real-regression-vs-stale-gate judgment
- merge judgment
- Samsung feedback interpretation

## Codex

Owns:

- bounded implementation in isolated worktree
- reading governing docs
- detailed code plan
- focused local validation only
- commit/push checkpoints
- no CI babysitting
- no merge
- no release action

## GitHub CI

Owns the expensive full mechanical merge gate.

## Samsung

Owns real-device proof:

- IME/window
- system bars
- touch feel
- device media
- audio routes
- PiP/background/foreground
- long-running work
- actual upload/playback
- large-font usability
- navigation feel

---

# 25. Execution loop

Preferred:

**ChatGPT audit/scope**
→ **Codex implementation**
→ optional focused test
→ `git diff --check`
→ status
→ commit
→ push
→ STOP
→ **GitHub hosted CI**
→ **ChatGPT live diff/log review**
→ targeted fix for concrete failure
→ hosted CI again
→ merge only when green
→ Samsung QA

Do NOT make Codex do by default:

- full Android unit suite
- lintRelease
- debug APK
- instrumentation APK
- release APK
- AAB
- full Django tests
- every architecture script
- repeated CI polling
- long waiting loops

Focused validation is allowed for the exact changed seam.

---

# 26. Stacked PR rule

Stacking is allowed to keep work moving.

Rules:

- child flow may branch from exact parent head
- after parent merges, retarget child PR to master
- ensure parent-only commits disappear from child diff
- preserve local work before rebase
- never hard-reset an in-progress worktree
- finish/commit the current checkpoint before fetching/rebasing when possible

Flow 7 followed this pattern.

---

# 27. Stale test / stale gate policy

A CI failure is not automatically a product bug.

Process:

1. inspect exact failing step/log
2. compare expectation to current product contract
3. if product code is wrong → fix product code
4. if test/gate encodes old intended behavior → update narrowly
5. never weaken a gate merely to force green

Flow 6 example:

Old tests expected:

- `🎤 Voice message`
- `📷 Photo`

New product intentionally moved iconography into Nova UI and kept fallback copy:

- `Voice message`
- `Photo`

Tests were updated.

Flow 7 gave another example: the alive design-system gate still encoded an older Rooms ordinary-screen assumption after the Flow 7 product contract changed. It was aligned in bounded commit `ac0e326...`, and the complete post-merge CI later passed.

---

# 28. Opportunistic-fix policy

Fix an adjacent issue in the same flow only if it:

1. was discovered during normal work
2. is user-visible/consistency-relevant
3. has an understood root cause
4. is bounded
5. is low-risk
6. requires no major new product decision
7. is explicitly reported

Otherwise defer it.

---

# 29. Mandatory seam scan

Before calling a flow stable, inspect:

- screen before entry
- screen after exit
- sheets/dialogs
- empty/loading/error
- back navigation
- system bars
- keyboard
- shared icons/actions
- created content at destination
- own-user/other-user variants where relevant

A flow ends with a seam scan, not merely “tests passed.”

---

# 30. Accessibility / quality

Cross-app requirements:

- meaningful touch targets
- content descriptions
- large-font behavior
- non-color-only state communication
- contrast
- RTL awareness
- keyboard/IME correctness
- safe areas/system bars
- restrained motion
- reduced-motion compatibility where applicable
- no raw backend errors as product copy
- no invisible busy state

Accessibility is part of the system, not optional cleanup.

---

# 31. Samsung device QA

Governing checklist:

`docs/architecture/SAMSUNG_SMOKE_CHECKLIST.md`

CI cannot prove Samsung window/IME/media/call behavior.

Record:

- device model
- Android version
- One UI version
- navigation mode
- build commit/version
- tester/date

Key physical checks:

### Social
session startup, feed cache/refresh, Like/Repost/Share, comments IME, Post media.

### Reels / Stories / Pulse
device-recorded video, duration, first frame, audio/video, buffering, retry, lifecycle.

### Profile / People
long links, large fonts, edit IME/back/save, paging/filter transitions.

### Orbit / Tonight / Activity
density, Pulse-only live markers, time windows, unread announcements, navigation.

### Messaging / Calls
composer above keyboard, drafts/reply/edit/search, pending/retry, voice/photo, two-device calls, audio routes, PiP, reconnect.

### Rooms
discovery/detail, paging, reminders, picker, H.264/AAC prep, duration, upload, playback, audio, retry, rotation/background recovery.

### Memories
quiet/normal/older week, draft picker, film render, background/reattach, cancel/retry, preview/share.

A flow is not `DEVICE VERIFIED` because CI is green.

---

# 32. Things not to overclaim

Do not claim:

- exact historic Pulse playback root cause
- intentional 4-second Pulse cap
- Pulse <60s physical-device verification
- Flow 1 created the lead-Post placement
- production deployment is current without live check
- any flow is DEVICE VERIFIED without physical pass
- Memory Film remains safe after changing protected worker/FGS without dedicated evidence
- TURN/call reliability without real two-peer evidence

---

# 33. Current next-action order

At this reference checkpoint:

1. Flow 7 is merged and post-merge CI is green.
2. Start Flow 8 from exact current `master`.
3. Flow 8 PR → hosted CI → ChatGPT review → merge.
4. Flow 9 from exact final Flow 8 state / stack if useful.
5. Flow 9 PR → hosted CI → review → merge.
6. Final Whole-Product Coherence Pass.
7. Samsung real-device sweep across affected/reopened flows.
8. Only then close the product-polish phase.

---

# 34. Master continuation prompt for a new chat

```text
Continue the existing Nova product-polish project.

Repository:
omarkhair-labs/nova

First read:
docs/NOVA_MASTER_PROJECT_REFERENCE.md

Treat it as continuity context, not as a substitute for live repository truth.

Before changing anything:

1. inspect current master
2. inspect open Nova PRs
3. inspect newest hosted Nova CI
4. read current governing repository docs named in the reference file
5. compare live state to the handoff checkpoint

Do not restart architecture/design work that is already complete.
Do not redesign Nova from scratch.
Do not create Design System V2.
Do not touch release/signing/Play unless explicitly authorized.
Do not touch TURN/Redis/WebRTC/Memory Film FGS without concrete evidence.
Do not inspect/copy/delete the real local Nova Dev Firebase config.

Use:
live repo/PR/CI > governing docs > architecture contracts > this reference > old summaries.

Resume only from the first genuinely unfinished checkpoint.

ChatGPT owns audit, scope, CI diagnosis and merge judgment.
Codex owns bounded implementation, focused checks, commit/push, then STOP.
Hosted GitHub CI owns full validation.
Samsung owns real-device verification.

Never claim DEVICE VERIFIED from CI alone.
```

---

# 35. Governing repository documents

## Design / product

- `docs/design/NOVA_PRODUCT_POLISH_BRIEF.md`
- `docs/design/NOVA_PRODUCT_POLISH_ROADMAP.md`
- `docs/design/NOVA_DESIGN_SYSTEM.md`
- `docs/design/NOVA_VISUAL_IDENTITY.md`
- `docs/design/APPROVED_VISUAL_IMPLEMENTATION_MATRIX.md`

## Architecture

- `docs/architecture/NOVA_ARCHITECTURE_AUDIT_AND_MASTER_PROMPT.md`
- `docs/architecture/PROGRESS.md`
- `docs/architecture/OWNERSHIP.md`
- `docs/architecture/ROUTE_INVENTORY.md`
- `docs/architecture/SAMSUNG_SMOKE_CHECKLIST.md`
- feature/product architecture docs under `docs/architecture/`

## Enforcement

- `scripts/check_*_architecture.py`
- shared UI / design-system gates
- `.github/workflows/ci.yml`

Never edit enforcement merely to hide a legitimate violation.

---

# 36. Prior research artifact

Persistent ChatGPT Library artifact:

**`Nova Final Visual Identity — Post-DS4 Direction and Migration Plan`**

It contains:

- post-DS4 system-vs-brand assessment
- benchmark research
- direction comparison
- palette reasoning
- icon/motion/media principles
- migration/governance thinking

Use it as research source.

Current repo docs override stale implementation details.

---

# 37. One-sentence project memory

> Nova is a warm, relationship-first social app that remembers life with your people; Quiet Orbit gives it a restrained recognizable identity, the product is being finished one deep flow at a time, architecture/release/realtime/Memory-Film boundaries are protected, Codex implements bounded checkpoints, GitHub CI validates them, and Samsung real-device behavior is the final proof.
