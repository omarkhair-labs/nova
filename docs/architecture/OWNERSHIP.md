# Current ownership and entry paths

Snapshot: Phase 7 final architecture state (#178), based on fresh master
`1d52e6e7225eee0d458418828e660611a4f0fa62` after #177 / Nova CI #542 full green.

This file records **current ownership**, not the historical sequence used to get
here. The PR-by-PR migration history and rollback points live in `PROGRESS.md`;
protected contracts and the Definition of Done remain governed by
`NOVA_ARCHITECTURE_AUDIT_AND_MASTER_PROMPT.md` and the route/WebSocket
characterization tests.

## Android runtime map

```text
NovaApplication
|- AppContainer (application-scoped construction + AppNavigationBridge)
`- MainActivity (launcher; edge-to-edge; adjustResize)
   |- NovaAppHost + AppViewModel
   |  |- NovaApp (auth + Home / People / Profile child navigation)
   |  |- ReelsScreen overlay
   |  `- MessagesRootRoute overlay
   |     `- MessagesRoute
   |        |- MessagesScreen
   |        `- ConversationScreen
   |           `- feature/messages/conversation/ConversationContent
   |- NovaActiveCallPill
   `- NovaUpdateReadyBanner

Special Activities
|- MessagesActivity
|- ReelsActivity
|- CallActivity
|- SettingsActivity
|- PrivacyActivity
|- AccountSecurityActivity
`- SocialGraphActivity
```

The Home/People/Profile social tree deliberately remains alive beneath the
Messages and Reels overlays. Direct conversation and profile-Reel entry keep the
special-Activity fallbacks. Main, Messages and Reels retain their existing
`adjustResize` parity. Those behaviors remain protected until device evidence
establishes a replacement.

## Android shell ownership

| Concern | Current owner | Protected responsibility |
|---|---|---|
| process/bootstrap | `NovaApplication`, `AppContainer` | app presence, shared construction, application-scoped navigation bridge |
| launcher/window/push/update | `MainActivity`, `AppViewModel` | edge-to-edge/window policy, update controller, notification permission, initial/new intents, session restore |
| primary navigation | `NovaAppHost`, typed `AppDestination`/`AppNavigator`, `AppNavigationBridge` | preserve root state and existing fallback rules |
| child social navigation | `NovaApp`, `NovaRootNavigationSignal`, `rootNavigationPlan` | existing Home/People/Profile transition semantics |
| push/deep-link parsing | `DeepLinkRouter` + shell bridge | exact push kinds/data keys and special-Activity fallbacks |
| terminal session handling | `AppViewModel` with feature effect bridges | clear session/global state and return to auth on terminal session expiry |
| unread sync | Messages state owners + shell refresh signals | startup/resume/read/back unread behavior |
| active-call reachability | app/special-Activity hosts + `NovaActiveCallPill` | active call remains reachable across surfaces |

## Android feature/data ownership

| Feature | Stable owner(s) | Production boundary |
|---|---|---|
| Auth | `feature/auth` models/parser/remote data source plus `NovaAuthRepository` session orchestration | shared network exposes primitives; Auth owns register/login/me/profile wire decoding |
| Feed / Posts / Comments | `feature/feed` state/data contracts and `feature/posts` models/parser/remote transport | `NovaFeedRepository` preserves feed cache/auth orchestration while Posts owns endpoint/DTO/parser behavior |
| People / Profile / Social graph | `feature/people` models/parser/repositories/state owners | People owns direct and paging transport; Privacy state carried across the boundary stays feature-owned |
| Stories | `feature/stories` models/repository/state owners | Stories production repository keeps media/auth behavior behind the stable feature contract |
| Reels | `feature/reels` models/contracts/state owners | feed/profile/watch/comment production implementations consume feature-owned models directly |
| Messages / Groups | `feature/messages` inbox/conversation/details/appearance/group models/contracts/state owners | AppContainer owns concrete construction; UI retains platform rendering/picker/player/recorder concerns only where intentional |
| Calls | `feature/calls` models, `CallStateOwner`, REST/signaling/WebRTC contracts | `core/calls` contains Android/transport adapters that inherently touch REST/WebSocket/WebRTC/Telecom/notification APIs |
| Sharing | `feature/sharing` contract/state owner | production implementation uses stable Messages/People/Post seams |
| Notifications | `feature/notifications` models/repository/state owner | follow-request dependency remains Privacy-owned |
| Privacy | `feature/privacy` models/contracts/state owner | one production Privacy transport implements the narrow Privacy/follow-request views |
| Settings | Settings UI host + `AppContainer` cached-user/auth seams | no Settings-owned auth/session repository construction |
| Security / Blocked accounts | `feature/security` contracts/state owners | production account-security/blocked repositories sit behind stable feature interfaces |

## Shared Android network boundary — Phase 5 exit

`core/network/NovaApiClient.kt` is no longer a feature endpoint/DTO/parser owner.
Its allowed responsibility is shared transport infrastructure: HTTP execution,
auth/error primitives, the deliberate refresh primitive, media URL resolution,
and genuinely generic multipart/upload mechanics where shared by more than one
feature.

Feature wire ownership is direct:

```text
feature/auth   -> Auth parser + AuthRemoteDataSource
feature/people -> People parser + PeopleRemoteDataSource / paging
feature/posts  -> Post/comment parser + PostsRemoteDataSource
```

`NovaUser`, `AuthSession`, `NovaPerson`, `NovaPost`, `NovaComment`, and
`NovaPostAuthor` are feature-owned records. The former core compatibility model
aliases and duplicate parsers are deleted. Architecture gates prevent feature
DTO/endpoint ownership from returning to `NovaApiClient`.

## Backend composition — Phase 7 final state

All backend domains remain inside the **existing `accounts` Django app**. The
consolidation changes Python implementation ownership only; it does **not** change
Django app labels, model/table identity, migration history, public REST
paths/names/status semantics, WebSocket paths/events/payloads, environment
variables, or deployment entry points.

```text
nova_backend/
|- settings.py                     # DRF auth points directly to accounts.auth.jwt_auth
|- urls.py
`- asgi.py

accounts/
|- urls.py                         # composition-only -> api.urls
|- routing.py                      # composition-only -> messaging/calls routing
|- api/urls.py                     # domain URL composition
|- auth/                           # auth/account-security/JWT + me
|- social/                         # People/social graph HTTP ownership
|- privacy/                        # privacy/follow requests/Close Friends
|- trust_safety/                   # block/report/account deletion
|- posts/                          # Posts + post comments
|- notifications/                  # notification/push-device HTTP ownership
|- sharing/                        # repost/feed/message sharing ownership
|- stories/                        # Stories HTTP ownership
|- reels/                          # Reels/profile/ranking/comments ownership
|- messaging/                      # conversations/messages/groups/presence/realtime/tools
|- calls/                          # call REST/ICE/signaling/reliability/history/realtime
|- models.py                       # authoritative core accounts-app models
|- messaging_models.py             # accounts-app sidecar model registration
|- privacy_models.py               # accounts-app sidecar model registration
|- story_models.py                 # accounts-app sidecar model registration
|- reels_models.py                 # accounts-app sidecar model registration
|- sharing_models.py               # accounts-app sidecar model registration
|- comment_reply_models.py         # accounts-app sidecar model registration
|- migrations/                     # unchanged accounts migration identity
|- test_core_api.py                # historical core API behavior suite
`- tests/                          # architecture/exit/final enforcement
```

`accounts/api/urls.py` includes domain packages directly. `accounts/routing.py`
composes Messaging and Calls WebSocket pattern lists directly. The temporary root
Stories/Reels/Calls URL/routing adapters were removed in #177.

`AccountsConfig.ready()` imports call-history registration from
`accounts.calls.call_history` directly. Phase 7 removes the remaining 21 root
runtime compatibility modules after migrating historical test/config references
to their canonical owners. `realtime_auth.py` and DRF authentication settings now
point directly to `accounts.auth.jwt_auth`. The permanent Phase 7 scanner rejects
restoration of those root files or any `accounts.<legacy>` reference anywhere
under `backend/` while allowing legitimate same-domain relative imports.

## Backend model/migration identity

The authoritative Django app remains `accounts`. Domain packages may contain
import-only `models.py` facades for local relative imports, but they must not
define replacement Django models. Existing sidecar model modules and
`accounts/migrations/` retain their identities. `makemigrations --check` and the
backend regression suite remain mandatory merge gates.

## Protected backend route/realtime inventory

Executable characterization fixes the public surface at:

- exactly **72 named `/api/v1` routes** with their current reverse/resolve paths;
- public privacy/account-deletion/child-safety routes unchanged; and
- exactly these ordered WebSocket regexes:
  - `^ws/presence/$`
  - `^ws/conversations/(?P<conversation_id>\d+)/$`
  - `^ws/calls/(?P<call_id>[0-9a-fA-F-]+)/$`

Domain-ownership tests additionally assert that REST views and Messaging/Calls
consumers resolve to the canonical package owners rather than compatibility
modules.

## Backend test ownership

The old `backend/accounts/tests.py` name blocked creation of an `accounts.tests`
package. #177 renamed that behavior suite to `test_core_api.py` without changing
its contents and created `accounts/tests/` for architecture/exit organization.
The Phase 6 exit gate prevents restoration of the old module collision and the
superseded root route adapters. Phase 7 adds permanent root-shim/reference
absence enforcement across the complete backend tree.

## CI, release, and physical-smoke boundary

Hosted Nova CI is the automated merge gate: Django configuration, migrations,
release-script validation, backend regression tests, Android architecture gates,
whitespace, JVM tests, lint, debug APK, instrumentation APK, release APK and AAB
must all pass on the exact #178 head before merge.

The Google Play workflow remains unchanged. #178 does not modify
`app/build.gradle.kts`, does not bump version `2.1.4` / versionCode `20104`, and
does not authorize or dispatch a Play publish.

The physical Samsung checklist remains separately blocked by the differently
signed package already installed on the authorized SM-A266B. No uninstall is
authorized. That unresolved physical check is recorded rather than bypassed.

## Completion marker

#178 is the final consolidation PR. It may merge only after the complete hosted
gate above is green on this final architecture/documentation state. Once that
merge lands on `master`, the governed consolidation status is **REFACTOR DONE**;
there is no additional architecture-consolidation PR in the fixed budget.
