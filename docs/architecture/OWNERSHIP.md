# Current ownership and entry paths

Snapshot: Phase 4 Reels data-boundary work, based on the #133 merge `e0b465d3c8d2cfdb3b276d1a32eb35f9358790db` with #134 as the active Reels boundary PR.

This file records current behavior. A row with several current owners identifies
consolidation work; it does not imply that one of those paths may be removed
without tests.

## Android runtime map

```text
NovaApplication
|- AppContainer (shared API/repositories + AppNavigationBridge)
`- MainActivity (launcher; adjustResize; edge-to-edge)
   |- NovaAppHost + AppViewModel
   |  |- NovaApp (auth + Home / People / Profile Nav3 child stack)
   |  |- ReelsScreen overlay
   |  `- MessagesRootRoute overlay
   |     `- MessagesRoute
   |        |- MessagesScreen
   |        `- ConversationScreen
   |           `- feature/messages/conversation/ConversationContent
   |- NovaActiveCallPill
   `- NovaUpdateReadyBanner

Special Activities
|- MessagesActivity (inbox fallback and direct notification conversation entry)
|- ReelsActivity (root fallback and profile-Reel entry)
|- CallActivity (call window, permissions, PiP, and Compose host)
|- SettingsActivity
|- PrivacyActivity
|- AccountSecurityActivity
`- SocialGraphActivity
```

The Home/People/Profile social tree deliberately remains composed under the
Messages and Reels overlays. That state-preservation behavior is protected.

## Shell ownership table

| Concern | Current owner(s) | Current contract | Intended stable owner |
|---|---|---|---|
| Android process bootstrap | `NovaApplication`, `AppContainer` | app presence, process setup, and one construction point for shared API/repositories | `NovaApplication` + `AppContainer` |
| launcher/window/update/push bootstrap | `MainActivity`, `AppViewModel` | edge-to-edge, `adjustResize`, update controller, notification permission, initial/new intents, and session restore | thin `MainActivity` + `AppViewModel` |
| five primary destinations | `NovaAppHost`, typed `AppDestination`/`AppNavigator`, application-scoped `AppNavigationBridge` | social roots stay alive under Reels/Messages overlays | `NovaAppHost` + `AppViewModel` |
| nested social roots | `NovaApp`, `NovaRootNavigationSignal`, `rootNavigationPlan` | secondary-to-secondary resets through Home | typed child destinations/policy |
| push/deep-link parsing | `MainActivity.routePushIntent`, `NovaPushOpenSignal`, special navigators | exact push kinds/data keys and fallback behavior | `DeepLinkRouter` |
| session expiry | `AppViewModel` coordinates logout and global state; feature state owners report terminal 401 effects | logout/clear state and return to authentication on terminal 401 | `AppViewModel` until a core session package is extracted |
| dependency construction | `AppContainer` for shell/auth/feed/people/messages plus stable Calls repository/signaling/WebRTC construction, stable feed/posts and People contract views, stable Stories repository construction, stable Reels feed/profile/watch contract views, conversation tools/appearance, group management/membership/people lookup, and conversation realtime/draft factories; live Reels routes still construct the legacy concrete repositories until the next equivalence slice | repositories and transports use application context; consolidated feature UI consumes stable interfaces/state owners | expand the explicit container feature by feature |
| unread sync | `MainActivity`, `InboxViewModel`, `NovaMessagesSignal` | inbox count refresh at startup/resume/read/back | Messages state owner |
| global call pill | `MainActivity`, `MessagesActivity`, `ReelsActivity` | active call remains reachable | app host / shared special-entry shell |

## Primary navigation and fallback policy

`AppNavigator` is the root-navigation consumer contract. The `AppContainer`
owns one `AppNavigationBridge`; there is no object-level dispatcher state. The
bridge consumes navigation only while MainActivity is resumed and has an
attached `AppViewModel` handler. When it does not consume:

- `NovaMessagingNavigator.openInbox` starts `MessagesActivity`;
- `NovaReelsNavigator.open` starts `ReelsActivity`; and
- callers may finish the current Activity when `replaceCurrentActivity=true`.

Direct conversation and profile-Reel routes always use special Activities:

- `NovaMessagingNavigator.openConversation` -> `MessagesActivity` with the
  `nova_conversation_*` extras;
- `NovaReelsNavigator.openProfile` -> `ReelsActivity` with `profile_username`
  and `initial_reel_id`.

`MainActivity.routePushIntent` treats valid message and Reel-activity pushes as
special routes. Other targets are offered to `NovaPushOpenSignal` for the social
tree. Empty intents are ignored by the signal.

## Shared route factories

- `MessagesRouteFactory` owns the unchanged `nova_conversation_*` intent
  contract. `NovaMessagingNavigator` builds it, `MessagesActivity` parses it,
  and both special and primary entries render `MessagesRoute`.
- `ReelsRouteFactory` owns the unchanged `profile_username` and
  `initial_reel_id` contract. `NovaReelsNavigator` builds it, and both
  `NovaAppHost` and `ReelsActivity` render `ReelsRoute`.
- `DeepLinkRouter` remains the single push-intent parser in `MainActivity`.

## Activity and inset ownership

| Activity | Exported | Entry | Window policy at snapshot | Content inset owners |
|---|---:|---|---|---|
| `MainActivity` | yes | launcher and notification `onNewIntent` | edge-to-edge + manifest `adjustResize` | route content owns status/navigation insets; `ConversationComposer` alone consumes navigation-bar/IME insets |
| `MessagesActivity` | no | inbox fallback or direct conversation | edge-to-edge + manifest `adjustResize` | same Messages/Conversation content as primary overlay; composer alone consumes bottom/IME insets |
| `ReelsActivity` | no | root fallback or profile Reel | edge-to-edge + manifest `adjustResize` | Reels/profile viewer status-bar padding; sheets own bottom insets |
| `CallActivity` | no | call intents/notifications | edge-to-edge, resizeable, PiP, separate call task affinity; delegates call state/lifecycle to `CallStateOwner` | call UI owns status/navigation padding; Activity owns permission launcher and PiP/window policy |
| `SettingsActivity` | no | explicit internal intent | edge-to-edge | screen status/system-bottom padding |
| `PrivacyActivity` | no | explicit internal intent | edge-to-edge | privacy screen owns status/navigation padding |
| `AccountSecurityActivity` | no | explicit internal intent | edge-to-edge | security page owns status/navigation/IME padding |
| `SocialGraphActivity` | no | explicit internal intent with username/mode; hosts `SocialConnectionsStateOwner` and terminal-session effect bridging | edge-to-edge | social graph screen owns status/navigation padding |

The current manifest gives `adjustResize` to Main, Messages, and Reels. The
2.1.3 fix added MainActivity because normal Messages now lives there. Do not
remove that parity before a device test establishes a replacement.

## Feature ownership table

| Feature | Route/UI owner(s) now | Data/control owner(s) now | Consolidation destination |
|---|---|---|---|
| auth/onboarding | `NovaApp`, auth/welcome/onboarding screens | `NovaAuthRepository`, `NovaSessionStore`, `NovaApiClient` | `feature/auth` + `core/session` |
| feed/posts/comments | `NovaApp` as navigation/session-effect bridge; `HomeScreen`, `NovaPostCard`, `PostDetailScreen`, `PostCommentsScreen` render state and emit callbacks | `FeedStateOwner`, `PostDetailStateOwner`, `PostCommentsStateOwner`; stable `FeedRepository`/`PostRepository` and `feature/posts/domain/model/PostModels.kt`; `NovaFeedRepository`/`NovaApiClient` remain concrete transport/parser adapters | stable `feature/feed` + `feature/posts`; downstream compatibility imports removed in each later feature slice |
| people/profile/social graph | `NovaApp` as People/Person effect/navigation bridge; `PeopleScreen` and `SocialConnectionsScreen` render stable state/callbacks; `PersonScreen` still owns privacy/safety/message UI residuals; Profile self-screen remains separate; `SocialGraphActivity` hosts the graph owner | stable `PeopleRepository`/`PeoplePagingRepository`, `PeopleStateOwner`, `PersonStateOwner`, `SocialConnectionsStateOwner`; core social repositories remain production adapters | `feature/people` is the stable People state/data owner; profile-specific and cross-feature privacy/sharing/message residuals remain focused follow-up |
| Stories | `StoriesRail` owns picker/composer/dialog rendering, image timer, ExoPlayer/video progress, navigation, and insets only | `feature/stories/domain/model/StoryModels.kt`, stable `StoriesRepository`, `StoriesStateOwner`, `StoryViewerStateOwner`; `NovaStoriesRepository` is the production HTTP/auth/media implementation exposed through `AppContainer` | stable `feature/stories`; feature exit gate satisfied in #133 |
| Reels | `ReelsScreen`, `ProfileReelsViewerScreen`, `ThreadedReelCommentsSheet`, `ReelsActivity` still own live async/UI/playback orchestration | stable `feature/reels` models and `ReelsRepository`/`ProfileReelsRepository`/`ReelWatchRepository` contracts are exposed through `AppContainer` via explicit core adapters in #134; legacy core repositories/records remain live until the next slice | stable `feature/reels`; state-owner migration follows the data boundary |
| Messages inbox | `MessagesRoute`, `MessagesScreen` | `InboxViewModel`/`InboxUiState`, feature-owned domain models, `InboxRepository`, and refresh signals | `feature/messages/inbox` |
| New direct message | `NewMessageDialog` | dialog-scoped `NewMessageViewModel` owns people search/open-conversation state and terminal effects using AppContainer dependencies | `feature/messages` stable state owner |
| Conversation | `ConversationScreen` -> `conversation/ConversationContent` | `ConversationViewModel`/`ConversationUiState` own server behavior; `ConversationScreen` owns details/theme/group/call overlays; stable list/rows/composer render state/callbacks | `feature/messages/conversation` + stateless header/content |
| Composer/media/voice | `ConversationComposer` | `ConversationComposerState` owns recorder, permission/picker, ephemeral attachment/voice drafts, cleanup, and the sole IME/navigation-bar padding; `ConversationViewModel` owns textual draft persistence and optimistic sends | `feature/messages/conversation` composer boundary |
| Message details/search/media | stable `ConversationDetailsDialog` | `ConversationDetailsViewModel`/`ConversationDetailsUiState`, `ConversationToolsRepository`, and stable details data/model packages; dialog owns unchanged media-player/full-photo platform UI | `feature/messages/details` |
| Conversation theme | `ConversationScreen`, `NovaChatThemePicker` | `ConversationAppearanceViewModel`/`ConversationAppearanceUiState` own load/save/optimistic rollback/picker state and terminal-401 effects; stable appearance repository owns HTTP/auth/local fallback/legacy-backend compatibility; palette/color rendering remains UI-owned | `feature/messages/appearance` |
| Group management | `ConversationScreen`, `GroupInfoDialog`, stable add-members/new-group dialogs | stable group models and `GroupManagementRepository`/`GroupMembershipRepository`/`GroupPeopleRepository`; `GroupInfoViewModel`, `AddGroupMembersViewModel`, `NewGroupViewModel` own async state and terminal effects | `feature/messages/group` |
| Calls | `CallActivity` as Android window/permission/PiP/Compose host | `feature/calls/CallStateOwner`, stable call domain models, `CallRepository`, `CallSignaling`, `CallWebRtcEngine`; `core/calls` retains production REST/signaling/WebRTC/Telecom/notification adapters | stable `feature/calls` ownership with platform adapters behind explicit boundaries |
| notifications/sharing | notification screen/share dialog | notification, push, messaging/social repositories | `feature/notifications`, `feature/sharing` |
| privacy/settings/security | special Activities and feature screens | privacy/auth/social repositories and UI callbacks | corresponding feature packages |

## Phase 4 feed/posts/comments dependency boundary

`feature/posts/domain/model/PostModels.kt` is the actual owner of `NovaPost`,
`NovaPostPage`, `NovaComment`, and `NovaCommentMutation`. `NovaApiClient` keeps the
same JSON parsing and HTTP endpoints but no longer declares those records.
`NovaPostAuthor` intentionally remains a shared `core.network` identity record
because messaging and other social features also consume it; shared DTO
ownership is handled in the later network cleanup rather than being forced into
this feature.

`feature/feed/data/FeedRepository.kt` and `feature/posts/data/PostRepository.kt`
are the stable data contracts. `NovaFeedRepository` is the existing production
implementation and preserves the same authentication refresh behavior, feed
cache, multipart post upload, 10 MB image limit, REST paths, and error mapping.
`AppContainer` exposes stable contract views of that production instance.

`FeedStateOwner` owns feed first-page/loading-more state, the existing page-merge
semantics, post create/delete/like state, shared post synchronization, content
invalidation, and session/profile-refresh effects. `PostDetailStateOwner` owns
route-local post load/like/delete state. `PostCommentsStateOwner` owns post and
comment loading plus top-level comment and reply mutations. Reply failures keep
the legacy local-error behavior, including terminal HTTP errors, and reply
mutations intentionally do not update the shared post/content version because
the pre-consolidation screen did not do so.

`HomeScreen` no longer constructs `NovaFeedRepository`; push-target post
resolution is supplied as a callback while the exact consume/success/fallback
behavior remains unchanged. `PostCommentsScreen` no longer constructs a
repository or launches comment-reply requests; it owns only ephemeral composer
UI state and emits callbacks to `PostCommentsStateOwner`.

`scripts/check_feed_posts_architecture.py` prevents the completed
`NovaApp`/feed/home/post/posts slice from importing the deprecated core post
model aliases, prevents `NovaApiClient` from redeclaring post/comment models,
and rejects feed repository ownership from `HomeScreen`/`PostCommentsScreen`.
`core/network/PostModelCompatibility.kt` is a temporary deprecated bridge only
for downstream features whose Phase 4 slice has not run yet; it does not restore
core ownership and is removed as those consumers migrate.

## Phase 4 People/Profile/Social-graph dependency boundary

`feature/people/domain/model/PeopleModels.kt` owns `NovaPerson`, `NovaPersonPage`,
and `NovaProfilePostPage`. `feature/people/data/PeopleRepository.kt` owns the
stable direct-person/follow/safety contract and the followers/following/profile
paging contract. `NovaSocialRepository` and `NovaSocialPagingRepository` remain
the production transport/auth/parser adapters, while `AppContainer` exposes the
stable interfaces.

`PeopleStateOwner` owns discovery query state, the existing 280 ms debounce,
paging/cursor state, stale-request suppression, privacy metadata, existing-ID
page deduplication, optimistic follow/request state, and session/profile/feed
effects. It intentionally preserves the old split-owner quirks: duplicates that
occur only inside an incoming page remain duplicated; normal-follow optimistic
UI can happen before the global in-flight follow guard rejects a second request;
a request-cancel 401 remains a local error; and a normal follow error remains
visible across the no-spinner paging refresh that follows it.

`PersonStateOwner` owns route-local person loading, profile-post loading, normal
follow transport/state, terminal-session effects, and current-user/feed refresh
effects. `NovaApp` now bridges those effects to navigation/session/feed behavior
instead of launching those requests directly.

`SocialConnectionsStateOwner` owns followers/following mode normalization, the
existing 240 ms search debounce, paging/privacy state, stale-request suppression,
self-follow protection, privacy-aware follow/request cancellation, and terminal
401 effects. `SocialGraphActivity` hosts that owner and retains the special
Activity clear-task behavior on session expiry. `PeopleScreen` and
`SocialConnectionsScreen` render state and emit callbacks; neither constructs a
social repository or launches network coroutines.

`PersonScreen` is deliberately not claimed as fully stateless. It still owns
privacy-state loading, private-request cancellation, block/report flows, profile
sharing, and message-opening UI/platform orchestration. Those responsibilities
cross the later Privacy/Sharing/Messages boundaries and remain explicit residuals
rather than being forced into the People owner during this slice.

`scripts/check_people_architecture.py` enforces stable People model/repository
ownership, production adapter conformance, `AppContainer` construction, the three
state-owner seams, render-only People/social-connections screens, and removal of
the former `NovaApp` People orchestration. Temporary core person/social-page
aliases remain only until a focused residual audit proves all remaining consumers
have migrated.

## Phase 4 Stories dependency boundary

`feature/stories/domain/model/StoryModels.kt` is the single owner of the current
Story author/shared-post/shared-Reel/story/group/viewer record graph.
`feature/stories/data/StoriesRepository.kt` is the stable data contract.
`NovaStoriesRepository` keeps the existing production HTTP, authentication,
refresh/session clearing, JSON parsing, media URL resolution, multipart upload,
validation, timeout, and error-mapping behavior, but in #133 it implements the
stable contract directly and parses into feature-owned records. The temporary
`CoreStoriesRepositoryAdapter` and its field-for-field mapping test are deleted.

`StoriesStateOwner` owns rail loading/error state, media/text create state,
completion versions, sibling reload behavior, and terminal-session effects.
`StoryViewerStateOwner` owns ordered viewer navigation, local viewed/reaction
state, the shared mutation lock, reply state, delete completion, viewers-dialog
loading/errors, and terminal-session effects. Existing first-unseen selection,
401 distinctions, reply persistence, and reaction-toggle behavior remain
characterized by JVM tests.

`StoriesRail` now owns only UI/platform responsibilities: document picker and
composer visibility, image-frame timing, video progress/ExoPlayer lifecycle,
shared Post/Reel navigation, dialogs, and system/IME inset behavior. Superseded
V2 implementation helper identifiers are removed in #133 without changing those
behaviors. The visible `Aa · Nova V3` Text Story copy remains untouched because
this cleanup does not rewrite user-visible product copy.

`scripts/check_stories_architecture.py` is the Stories exit gate. It requires the
stable models/contract/state-owner/live wiring, requires the production
repository to implement the stable contract directly, rejects duplicate core
Story declarations and adapter restoration, scans main/test Kotlin sources for
legacy core Story model imports, rejects direct repository/network orchestration
from `StoriesRail`, and rejects the superseded live helper identifiers.

## Phase 4 Reels dependency boundary

`feature/reels/domain/model/ReelModels.kt` defines the stable Reel author, Reel,
page, threaded-comment, and comment-mutation graph introduced by #134.
`feature/reels/data/ReelsRepository.kt` deliberately keeps three contracts:
`ReelsRepository` for feed/create/like/repost/comments/delete operations,
`ProfileReelsRepository` for authored/reposted profile paging, and
`ReelWatchRepository` for watch telemetry. Keeping those contracts separate
matches the three existing production responsibilities rather than creating a
single oversized Reels repository.

The first boundary intentionally does not rewrite `core/reels`. The existing
`NovaReelsRepository`, `NovaProfileReelsRepository`, and
`NovaReelWatchRepository` remain the production HTTP/auth/media implementations,
and the existing core Reel records remain temporary transport-side records.
`CoreReelsRepositoryAdapter`, `CoreProfileReelsRepositoryAdapter`, and
`CoreReelWatchRepositoryAdapter` expose those implementations through the stable
contracts. Explicit mapping preserves every Reel field, page order/duplicates and
cursor, plus nested comment/reply structure. `AppContainer` owns all three stable
contract views.

Live `ReelsScreen`, `ProfileReelsViewerScreen`, and
`ThreadedReelCommentsSheet` still construct/use the concrete core repositories
and own their existing async state in #134. `ReelPlaybackCoordinator`, player
pool/ExoPlayer behavior, picker, overlays, sharing, navigation, and the special
`ReelsActivity` entry path are not changed. This is intentional: the next
Reels slice moves live async state behind feature-owned state owners only after
this boundary passes CI.

`scripts/check_reels_architecture.py` requires the stable model/contracts/adapters
and AppContainer seams, while also asserting that the first boundary has not
silently switched or removed the current live transport/state ownership. The
next state-owner PR must evolve that gate as live consumers migrate.

## Phase 2 Messages dependency boundary

`feature/messages/domain/model/MessagingModels.kt` owns the nine live
conversation, message, reply, reaction, share, list, and page models. The core
conversation model slice no longer relies on V-number compatibility types.

`feature/messages/data/MessagesRepository.kt` defines the existing conversation
mutation, read-marker, and realtime-token data operations;
`feature/messages/data/InboxRepository.kt` owns paged inbox/search loading.
`NovaMessagingRepository` and `NovaInboxPagingRepository` are the production
implementations. `AppContainer` owns both interfaces, and
`NovaConversationRealtimeClient` depends on `MessagesRepository`.

`feature/messages/inbox/InboxViewModel.kt` owns inbox query/debounce/paging,
ordered ID deduplication, unread state, loading/errors, stale-response
suppression, and terminal-session effects. `MessagesScreen` renders that state
and forwards intents.

`feature/messages/conversation/ConversationViewModel.kt` owns message-page
loading, earlier-page deduplication, optimistic send/retry identity, edit/delete/
reaction mutations, text-draft debounce, unread/read effects, presence/typing,
delivery/read receipts, and realtime reconciliation. Production realtime/draft
implementations are constructed by `AppContainer`.

`ConversationMessageList.kt` and `ConversationMessageRow.kt` own stateless list
and row rendering around callback-owned navigation/mutations.
`ConversationComposer.kt` owns photo picking, microphone permission, recorder
platform state, recording limits, temporary cleanup, previews, reply/edit
context, send eligibility, and the sole conversation `imePadding` responsibility.

`feature/messages/details` owns stable search/context/shared-media/mute models,
repository contract/implementation, lifecycle state, dialog rendering,
MediaPlayer lifecycle, and full-photo UI. Characterization preserves the current
key-based behavior where selecting an already-active media filter or pressing
the existing load-more control does not issue an additional media request.

Phase 2 PR 15 introduced `feature/messages/appearance/model`, `data`, and
`data/remote`. `ConversationAppearanceRepository` exposes the current preference
read and theme write contract; `ConversationAppearanceRemoteRepository` owns the
existing authenticated REST calls, token refresh/session clearing, local
`nova_conversation_themes` SharedPreferences fallback, theme-key normalization,
and legacy-backend fallback that re-sends `muted` with `theme_key`.

Phase 2 PR 16 adds `ConversationAppearanceViewModel` and
`ConversationAppearanceUiState`. They own the initial preference load, existing
`NovaChatThemes.resolve` key semantics (injected from UI so Compose `Color` stays
out of the state owner), picker visibility, save-in-flight lock, optimistic
selected key, previous-key rollback, inline non-401 error, and terminal-401
effect. The view model is scoped to a `ConversationScreen`-owned
`ViewModelStore` that is cleared when the route leaves composition, preserving
the prior `remember(conversationId)` lifetime rather than retaining state at
Activity scope. `ConversationScreen` consumes
`AppContainer.conversationAppearanceRepository` directly and the temporary
`NovaConversationPreferenceRepository` compatibility aliases are removed.

Phase 2 PR 17 introduces `feature/messages/group/model/GroupModels.kt` as the
stable owner for the shared group records used across management, membership,
and the group-info UI: `GroupMember`, `GroupDetail`, and `ManagedGroupDetail`.
`NovaGroupMessagingRepository.kt` and `NovaGroupManagementRepository.kt` no
longer declare those records themselves. Temporary deprecated aliases in
`core/messaging/GroupModelCompatibility.kt` preserve every existing consumer
while transport ownership is moved in the next slices. This PR changes no group
validation, REST path/method/body, avatar upload, membership semantics, auth,
error mapping, or UI behavior.

Phase 2 PR 18 adds `feature/messages/group/data/GroupManagementRepository.kt`
and `data/remote/GroupManagementRemoteRepository.kt`. The remote implementation
is the moved management-side transport: exact `group/manage/` detail/rename/
avatar/remove-avatar calls, role endpoint, title/role validation, `Uri` and
ContentResolver media boundary, image-only and 10 MB checks, multipart field and
timeouts, response parsing, auth refresh/session clearing, error mapping, and
Coolify URL. `AppContainer` owns the stable interface. The old
`NovaGroupManagementRepository` symbol is temporarily a constructor-compatible
typealias so `GroupInfoDialog` remains behaviorally untouched until state/UI
orchestration moves.

Phase 2 PR 19 adds `feature/messages/group/data/GroupMembershipRepository.kt`
and `data/remote/GroupMembershipRemoteRepository.kt`. The remote implementation
is the moved membership-side transport: exact group create/detail/add/remove/
leave/delete paths and methods, title/member normalization and validation,
`deleted=true` null-detail behavior, cached-current-user leave semantics,
conversation/member parsing, auth refresh/session clearing, error mapping,
timeouts, URL encoding, relative media resolution, and the Coolify production
URL. `AppContainer` owns the stable interface. The old
`NovaGroupMessagingRepository` symbol remains temporarily as a
constructor-compatible typealias so current group dialogs and add-member flows
continue unchanged until their state/UI orchestration moves.

Phase 2 PR 20 adds `GroupInfoViewModel` and `GroupInfoUiState` under stable
`feature/messages/group` ownership. They own the initial managed-detail load,
title draft/edit state, rename/avatar/role/remove/leave/delete in-flight locks,
inline non-401 errors, terminal-401 effects, group-updated/group-left effects,
and the existing reload-after-remove/add behavior. The view model is scoped to a
`GroupInfoDialog`-owned `ViewModelStore` that is cleared when the dialog leaves
composition, preserving the prior `remember(conversationId)` lifetime instead of
retaining group dialog state at Activity scope. Permission derivation and photo
picker rendering remain UI-owned. `AddGroupMembersDialog` intentionally keeps
its current search/selection/add orchestration for the next slice.

Phase 2 PR 21 moves add-members and new-group people search, selection,
submission, loading/error state, and terminal effects into stable
`AddGroupMembersViewModel` and `NewGroupViewModel` ownership. It adds
`GroupPeopleRepository` backed by the existing first-page people paging behavior
and gives `AppContainer` construction ownership. The historical
`NovaGroupManagementRepository` and `NovaGroupMessagingRepository` compatibility
aliases are deleted after the live group UI stops consuming them.

Phase 2 PR 22 moves direct-message people search, opening lock, error/session
state, and `openConversation(username)` orchestration into dialog-scoped
`NewMessageViewModel`. `NewMessageDialog` renders the stable state and consumes
AppContainer-owned dependencies; the existing 220 ms debounce, 40-character cap,
and callback order remain characterized.

Phase 2 PR 23 collapses the live `ConversationScreen -> V9 -> V8` chain into one
stable `ConversationScreen -> conversation/ConversationContent` path. The former
V8 file is a structural rename into stable conversation ownership; the former V9
identity/details click layer moves into `ConversationScreen` with the same
padding, hit area, initial tab, and z-order relative to call/info actions.
`ConversationScreenV8.kt` and `ConversationScreenV9.kt` are deleted. The first
hosted Android compile exposed a same-package last-seen helper-name collision;
renaming only those private helpers resolved it, and the replacement CI run
passed all hosted gates without changing parsing behavior.

Final Phase 2 enforcement deletes `GroupModelCompatibility.kt` after all live
consumers use stable group models. `scripts/check_messages_architecture.py` is a
CI boundary check that rejects the historical V8/V9/group compatibility symbols
and asserts the focused stable Messages owners are present.

## Phase 3 Calls dependency boundary

`feature/calls/domain/model/CallModels.kt` owns the live call kind/status/person/
session and ICE records. `feature/calls/data/CallRepository.kt` owns REST/ICE/auth
access, while `feature/calls/signaling/CallSignaling.kt` owns signaling status and
event records plus the transport contract. `feature/calls/webrtc` owns the WebRTC
contract and audio-quality records.

`feature/calls/CallStateOwner.kt` is the live call lifecycle/state owner. It owns
launch state, permission handoff, accept/decline/end, signaling negotiation and
recovery, media-quality recovery, duration state, and the call UI state machine.
`CallActivity` is intentionally limited to Android window/show-when-locked,
runtime-permission launcher, PiP, Compose rendering, intent parsing, and the
existing `start()`/`release()` lifecycle timing.

`AppContainer` owns production `CallRepository` construction and the signaling/
WebRTC factories. `core/calls` retains the production adapters that inherently
touch Android or transport implementation details: REST parsing, OkHttp WebSocket,
WebRTC engine, Telecom/audio routing, call notifications, active-call signal, and
action fallback dispatch. Those adapters import stable feature call records
directly; the old core model/audio-quality aliases and unreachable
`NovaCallController` are deleted.

`scripts/check_calls_architecture.py` enforces the stable Calls owners, rejects
the deleted controller/compatibility files and imports, and verifies that
`CallActivity` delegates live state to `CallStateOwner`. This structural
boundary does not alter REST/WS payloads, signaling replay/reconnect/negotiation
IDs, ICE/TURN/SDP behavior, Telecom callbacks, notification intents/actions,
ring timeout, or call-history semantics.

## Backend ownership map

```text
nova_backend/
|- settings.py / urls.py / asgi.py
`- project-level health, public pages, and API includes

accounts/
|- models.py plus extracted messaging/calls/stories/reels model files
|- urls.py plus reels_urls.py
|- routing.py
|- auth/social/posts/notifications base views
|- messaging, groups, realtime, paging, V9 tools
|- calls, reliability, signaling/realtime
|- stories, Reels, sharing, privacy, trust/safety, push/presence
`- behavior/regression test modules
```

All domain packages remain inside `accounts` until package boundaries are stable.
Moving model app ownership, table identity, or migrations is outside the current
plan. Backend V9-tool naming is not renamed during the Android Messages screen
consolidation because backend packaging is a later phase and public behavior must
remain unchanged.

## Current construction and error pattern

The shell now uses:

```text
NovaApplication -> AppContainer -> shared API/repositories
MainActivity -> NovaAppHost -> AppViewModel -> AppState
feature entry -> AppNavigator -> active host or special-Activity fallback
```

Most non-consolidated feature routes may still use the earlier pattern, which
later phases replace one responsibility at a time:

```text
route Composable
|- remember(context) { Repository(applicationContext) }
|- mutable UI and domain state
|- coroutines and network/realtime calls
|- status-code/session interpretation
|- navigation mutation
`- rendering
```

Messages no longer relies on that route-owned network orchestration pattern for
its inbox, direct-message opening, conversation core, details, appearance, or
group workflows. Calls likewise has stable domain/data/signaling/WebRTC
boundaries and one live `CallStateOwner`. Feed/posts/comments now has stable
model/data/state ownership and its UI no longer constructs the feed repository.
People discovery, Person route loading/following, and followers/following now
have stable feature-owned data/state boundaries; `NovaApp` and
`SocialGraphActivity` are effect/navigation hosts for those flows. `PersonScreen`
still has explicit cross-feature privacy/safety/message orchestration residuals.
Stories has one feature-owned Story model graph, stable data/state owners, and
live UI that delegates network/session behavior while retaining only
picker/playback/timer/navigation/dialog/inset platform responsibilities. Reels
now has a stable feature data/model boundary and AppContainer contract views, but
its live screens intentionally still use the route-owned repository/state pattern
until the next Reels equivalence slice. Android/transport-specific implementations
remain focused core adapters. `AppViewModel` owns global session restore/current-
user state, terminal session logout, and durable primary-overlay state. Feature
state owners still report terminal session effects to routes; central session-
expiry ownership is a later cross-feature cleanup. Platform-only UI
responsibilities such as MediaPlayer, picker/permission launchers, recorder
state, and the composer's sole IME/navigation-bar inset consumption remain
intentionally with focused UI owners.
