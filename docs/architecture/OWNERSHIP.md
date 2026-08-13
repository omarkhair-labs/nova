# Current ownership and entry paths

Snapshot: `dcbd534f6ba6656832d08b55d94097a302c8e99b`.

This file records current behavior. A row with several current owners identifies
consolidation work; it does not imply that one of those paths may be removed
without tests.

## Android runtime map

```text
NovaApplication
`- MainActivity (launcher; adjustResize; edge-to-edge)
   |- NovaPrimaryHost
   |  |- NovaApp (auth + Home / People / Profile Nav3 child stack)
   |  |- ReelsScreen overlay
   |  `- NovaMessagesRootContent overlay
   |     `- MessagingActivityContent
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
| Android process bootstrap | `NovaApplication` | app presence and process-level setup | `app/NovaApplication` + `AppContainer` |
| launcher/window/update/push bootstrap | `MainActivity` | edge-to-edge, `adjustResize`, update controller, notification permission, initial/new intents | thin `MainActivity` + `AppViewModel` |
| five primary destinations | `NovaPrimaryHost`, `NovaPrimaryNavigationDispatcher` | social roots stay alive under Reels/Messages overlays | `NovaAppHost` + typed `AppNavigator` |
| nested social roots | `NovaApp`, `NovaRootNavigationSignal`, `rootNavigationPlan` | secondary-to-secondary resets through Home | typed child destinations/policy |
| push/deep-link parsing | `MainActivity.routePushIntent`, `NovaPushOpenSignal`, special navigators | exact push kinds/data keys and fallback behavior | `DeepLinkRouter` |
| session expiry | `NovaApp`, `NovaPrimaryHost`, Activities, many screens/repositories | logout/clear state and return to authentication on terminal 401 | one session-expiry coordinator |
| dependency construction | Activities and route Composables | repositories are created from application context | lightweight `AppContainer` and factories |
| unread sync | `MainActivity`, Messages content, `NovaMessagesSignal` | inbox count refresh at startup/resume/read/back | Messages state owner |
| global call pill | `MainActivity`, `MessagesActivity`, `ReelsActivity` | active call remains reachable | app host / shared special-entry shell |

## Primary navigation and fallback policy

`NovaPrimaryNavigationDispatcher` consumes navigation only while MainActivity is
resumed and has an attached handler. When it does not consume:

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

## Activity and inset ownership

| Activity | Exported | Entry | Window policy at snapshot | Content inset owners |
|---|---:|---|---|---|
| `MainActivity` | yes | launcher and notification `onNewIntent` | edge-to-edge + manifest `adjustResize` | route content owns status/navigation insets; V8 composer and outer conversation both currently touch IME |
| `MessagesActivity` | no | inbox fallback or direct conversation | edge-to-edge + manifest `adjustResize` | same Messages/Conversation content as primary overlay; duplicate IME ownership remains |
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
| Messages inbox | `MessagingActivityContent`, `MessagesScreen` | `NovaMessagingRepository`, signals, UI state | `feature/messages/inbox` |
| Conversation | `ConversationScreen` -> V9 -> V8 | messaging repository, realtime, draft/preferences, UI state | `feature/messages/conversation` + state owner |
| Composer/media/voice | outer `ConversationScreen` + V8 composer | V8 coroutine/state/recorder logic | `feature/messages/composer` with one IME owner |
| Message details/search/media/theme/groups | V9 dialog, group dialogs, theme picker | V9 tools repository + messaging repositories | responsibility-specific Messages packages |
| Calls | `CallActivity` | `NovaCallController`, signaling, WebRTC, Telecom, notifications/history | `feature/calls` boundaries with explicit state machine |
| notifications/sharing | notification screen/share dialog | notification, push, messaging/social repositories | `feature/notifications`, `feature/sharing` |
| privacy/settings/security | special Activities and feature screens | privacy/auth/social repositories and UI callbacks | corresponding feature packages |

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
`- 35 behavior/regression test modules
```

All domain packages remain inside `accounts` until package boundaries are stable.
Moving model app ownership, table identity, or migrations is outside the current
plan.

## Current construction and error pattern

The prevailing Android pattern is:

```text
route Composable
|- remember(context) { Repository(applicationContext) }
|- mutable UI and domain state
|- coroutines and network/realtime calls
|- status-code/session interpretation
|- navigation mutation
`- rendering
```

There are no ViewModel implementations at this snapshot. Terminal 401 handling
is repeated across `NovaApp`, route screens, Activities, and repositories; this
is a characterization target before Phase 1 centralizes ownership.
