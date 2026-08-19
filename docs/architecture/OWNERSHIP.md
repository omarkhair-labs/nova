# Current ownership and entry paths

Snapshot: Phase 2 PR 13, based on `e73787ece1820d51cfea9e4426f7084765bf8757`.

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
   |           `- ConversationScreenV9
   |              `- ConversationScreenV8
   |- NovaActiveCallPill
   `- NovaUpdateReadyBanner

Special Activities
|- MessagesActivity (inbox fallback and direct notification conversation entry)
|- ReelsActivity (root fallback and profile-Reel entry)
|- CallActivity (call lifecycle, PiP, Telecom/WebRTC UI)
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
| dependency construction | `AppContainer` for shell/auth/feed/social/messages, conversation tools, plus conversation realtime/draft factories; remaining feature routes still construct specialized repositories | repositories and transports use application context | expand the explicit container feature by feature |
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
| `CallActivity` | no | call intents/notifications | edge-to-edge, resizeable, PiP, separate call task affinity | call UI owns status/navigation padding |
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
| Conversation | `ConversationScreen` -> V9 -> V8 | `ConversationViewModel`/`ConversationUiState` own server behavior; stateless list/rows and the focused composer render their state and callbacks | `feature/messages/conversation` + stateless header |
| Composer/media/voice | `ConversationComposer` | `ConversationComposerState` owns recorder, permission/picker, ephemeral attachment/voice drafts, cleanup, and the sole IME/navigation-bar padding; `ConversationViewModel` owns textual draft persistence and optimistic sends | `feature/messages/conversation` composer boundary |
| Message details/search/media/theme/groups | V9 dialog, group dialogs, theme picker | `ConversationDetailsViewModel`/`ConversationDetailsUiState` own tab/query/search/media/context/mute async state and terminal-401 effects; `ConversationToolsRepository` owns data; V9 still owns details rendering and media-player platform UI; theme/group orchestration remains outside this state owner | responsibility-specific Messages packages |
| Calls | `CallActivity` | `NovaCallController`, signaling, WebRTC, Telecom, notifications/history | `feature/calls` boundaries with explicit state machine |
| notifications/sharing | notification screen/share dialog | notification, push, messaging/social repositories | `feature/notifications`, `feature/sharing` |
| privacy/settings/security | special Activities and feature screens | privacy/auth/social repositories and UI callbacks | corresponding feature packages |

## Phase 2 Messages dependency boundary

`feature/messages/domain/model/MessagingModels.kt` owns the nine live
conversation, message, reply, reaction, share, list, and page models. Thirteen
production/test consumers import that package directly; no compatibility
typealiases remain in `core/messaging` for the core conversation model slice.

`feature/messages/data/MessagesRepository.kt` defines the existing conversation
mutation, read-marker, and realtime-token data operations;
`feature/messages/data/InboxRepository.kt` owns paged inbox/search loading.
`NovaMessagingRepository` and `NovaInboxPagingRepository` are the production
implementations. `AppContainer` owns both interfaces, and
`NovaConversationRealtimeClient` depends on `MessagesRepository`. Deterministic
test fakes capture every argument and return configured `ApiResult` values. The
Messages interface temporarily retains Android media inputs for exact composer
parity; a later data cleanup can replace those Android media types only with
separate characterization.

`feature/messages/inbox/InboxViewModel.kt` is the lifecycle-aware owner for the
inbox query, 260 ms debounce, page cursor, ordered ID deduplication, unread
state, loading/error flags, stale-response suppression, and terminal-session
effect. `MessagesScreen` renders that state and forwards UI intents; it retains
the existing refresh signal and route callbacks without constructing a
repository or launching data coroutines.

`feature/messages/conversation/ConversationViewModel.kt` owns message-page
loading, ordered earlier-page deduplication, optimistic send/retry identity,
edit/delete/reaction mutations, local text-draft debounce, unread/read effects,
presence/typing state, delivery/read receipts, and realtime update/delete/
reaction reconciliation. `ConversationRealtime` and `ConversationDraftStore`
make those behaviors deterministic in JVM tests; the production realtime client
and draft store implement the boundaries, and `AppContainer` constructs them.
V8 retains route wiring, header/delete/photo overlays, and shared-item
navigation around the extracted list and composer components.

`ConversationMessageList.kt` renders loading/empty/paged list state and delegates
all intents through callbacks. Its pure `messageRowContext` retains the exact
date-divider, sender-compaction, group-name, and unread-anchor decisions.
`ConversationMessageRow.kt` owns message/pending bubbles, reply/share cards,
reaction/actions, voice playback, receipt labels, swipe-to-reply, and full-screen
photos. Shared post/profile/Reel navigation is callback-owned by the route.

`ConversationComposer.kt` owns photo picking, microphone permission, the
`ConversationVoiceRecorder` boundary, recording timers and 1-second/5-minute
rules, temporary-file cleanup, attachment previews, reply/edit context, send
eligibility, and composer rendering. Its bottom-bar column is the only
conversation `imePadding` owner; the outer route no longer shifts the header or
overlays when the IME opens. `ConversationViewModel` remains the owner of text
drafts and server mutations.

`feature/messages/details/model` and `feature/messages/details/data` own stable
conversation search/context/shared-media/mute models and contracts.
`ConversationToolsRemoteRepository` owns the unchanged HTTP/auth/refresh/error/
parsing implementation and `AppContainer` owns one instance. The live V9 dialog
uses that interface directly with no compatibility aliases.

Phase 2 PR 13 adds `ConversationDetailsViewModel` and
`ConversationDetailsUiState`. They own the exact 320 ms/200-character search
policy, search/media/context/mute loading and errors, tab and target state,
terminal-401 effect version, and dialog-scoped cancellation. The view model is
scoped to a dialog-owned `ViewModelStore`, which is cleared when the dialog is
removed so reopening resets state and cancels work as the previous
`LaunchedEffect` implementation did. Characterization deliberately preserves
the current key-based behavior where selecting an already-active media filter
or pressing the existing load-more control does not issue an additional media
request. Fixing that product behavior is not part of architecture consolidation.

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
plan.

## Current construction and error pattern

The shell now uses:

```text
NovaApplication -> AppContainer -> shared API/repositories
MainActivity -> NovaAppHost -> AppViewModel -> AppState
feature entry -> AppNavigator -> active host or special-Activity fallback
```

Most feature routes still use the earlier pattern, which later phases replace
one responsibility at a time:

```text
route Composable
|- remember(context) { Repository(applicationContext) }
|- mutable UI and domain state
|- coroutines and network/realtime calls
|- status-code/session interpretation
|- navigation mutation
`- rendering
```

`AppViewModel` owns global session restore/current-user state, terminal session
logout, and durable primary-overlay state. Feature state owners still report
terminal session effects to routes; central session-expiry ownership is a later
cross-feature cleanup. The inbox, core conversation data path, message
rendering, composer platform work, conversation-tools data implementation, and
details async orchestration now have focused owners. Historical V9 details
rendering/media playback, themes, groups, and V8 route/chrome layering remain on
the Phase 2 extraction path.
