# Current ownership and entry paths

Snapshot: Phase 2 PR 18, based on `088749b3f8ddde3772ba5aa7efd7d622b6f9117e`.

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
| dependency construction | `AppContainer` for shell/auth/feed/social/messages, conversation tools/appearance, managed-group transport, plus conversation realtime/draft factories; remaining feature routes still construct specialized repositories | repositories and transports use application context | expand the explicit container feature by feature |
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
| Message details/search/media | stable `ConversationDetailsDialog` | `ConversationDetailsViewModel`/`ConversationDetailsUiState`, `ConversationToolsRepository`, and stable details data/model packages; dialog owns unchanged media-player/full-photo platform UI | `feature/messages/details` |
| Conversation theme | `ConversationScreen`, `NovaChatThemePicker` | `ConversationAppearanceViewModel`/`ConversationAppearanceUiState` own load/save/optimistic rollback/picker state and terminal-401 effects; stable appearance repository owns HTTP/auth/local fallback/legacy-backend compatibility; palette/color rendering remains UI-owned | `feature/messages/appearance` |
| Group management | `ConversationScreen`, `GroupInfoDialog` | stable group models plus `GroupManagementRepository`/`GroupManagementRemoteRepository` own managed detail/rename/avatar/remove-avatar/role transport; `AppContainer` owns that interface; historical membership transport and UI orchestration remain | `feature/messages/group` |
| Calls | `CallActivity` | `NovaCallController`, signaling, WebRTC, Telecom, notifications/history | `feature/calls` boundaries with explicit state machine |
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
orchestration moves. The membership-side repository is intentionally unchanged
in this PR.

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
cross-feature cleanup. The inbox, conversation core, composer, details data/
state/UI, appearance data/lifecycle ownership, shared group model ownership, and
management-side group transport now have focused stable owners. Membership
transport, group UI/state orchestration, and the V8/V9 route/chrome layering
remain on the Phase 2 extraction path.
