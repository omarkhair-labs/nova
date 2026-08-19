# Current ownership and entry paths

Snapshot: Phase 3 Calls closing enforcement, based on the #123 consolidation branch from `b194d170d1f1a36a8dd6bd80ee5be3815d3115f4`.

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
| dependency construction | `AppContainer` for shell/auth/feed/social/messages and stable Calls repository/signaling/WebRTC construction, plus conversation tools/appearance, group management/membership/people lookup, and conversation realtime/draft factories; remaining non-consolidated feature routes may still construct specialized repositories | repositories and transports use application context; consolidated feature UI consumes stable interfaces/state owners | expand the explicit container feature by feature |
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
| `SocialGraphActivity` | no | explicit internal intent with username/mode | edge-to-edge | social graph screen owns status/navigation padding |

The current manifest gives `adjustResize` to Main, Messages, and Reels. The
2.1.3 fix added MainActivity because normal Messages now lives there. Do not
remove that parity before a device test establishes a replacement.

## Feature ownership table

| Feature | Route/UI owner(s) now | Data/control owner(s) now | Consolidation destination |
|---|---|---|---|
| auth/onboarding | `NovaApp`, auth/welcome/onboarding screens | `NovaAuthRepository`, `NovaSessionStore`, `NovaApiClient` | `feature/auth` + `core/session` |
| feed/posts/comments | `NovaApp`, `HomeScreen`, post screens/cards | API client and post/comment calls orchestrated by UI | `feature/feed`, `feature/posts` |
| people/profile/social graph | `NovaApp`, People/Person/Profile screens, V4 profile components, `SocialGraphActivity` | social repositories + UI orchestration | `feature/people`, `feature/profile` |
| Stories | `StoriesRail` | stories repository plus UI-owned orchestration | `feature/stories` |
| Reels | `ReelsScreen`, `ProfileReelsViewerScreen`, `ReelsActivity` | reels repositories, playback pool/safety, UI orchestration | `feature/reels` |
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
boundaries and one live `CallStateOwner`, while Android/transport-specific
implementations remain focused core adapters. `AppViewModel` owns global session
restore/current-user state, terminal session logout, and durable primary-overlay
state. Feature state owners still report terminal session effects to routes;
central session-expiry ownership is a later cross-feature cleanup. Platform-only
UI responsibilities such as MediaPlayer, picker/permission launchers, recorder
state, and the composer's sole IME/navigation-bar inset consumption remain
intentionally with focused UI owners.
