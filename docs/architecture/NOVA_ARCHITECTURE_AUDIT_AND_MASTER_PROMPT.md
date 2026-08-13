# Nova Architecture Consolidation

Repository: `omarkhair-labs/nova`

Audit baseline: `c47e0d271ff698b5043d0c9f199688ecf0ef54b1`
(Android 2.1.3 / 20103)

Execution baseline: `dcbd534f6ba6656832d08b55d94097a302c8e99b`, which differs
from the audit baseline only by the dedicated Blacksmith runner migration in
PR #95.

## Executive decision

Nova is in a temporary feature freeze for staged architectural consolidation.
This is not a V6 release and not a rewrite. Preserve observable behavior while
replacing version-stacked implementations, oversized UI owners, implicit
navigation, and mixed responsibilities with stable feature boundaries.

The order is mandatory:

1. Record current contracts and add missing regression tests.
2. Consolidate the application shell and entry paths.
3. Refactor Android one feature at a time, starting with Messages.
4. Split the shared network layer only after feature ownership is clear.
5. Reorganize the Django backend by domain without changing public contracts.
6. Remove version-labelled implementations only after proven equivalence.

Every change is a small, sequential PR. Never build a dependent PR on an
unvalidated PR, and never merge before required CI is green.

## Audit findings

At the audit baseline the repository contains 299 tracked blobs, 124 Kotlin
files, 115 Python files, 115 Android production Kotlin files, eight Android JVM
test files, one Android instrumented test file, and 35 backend test modules.
The backend has materially stronger behavioral coverage than Android.

Confirmed risks:

- Android compilation/lint cannot prove keyboard resizing, back behavior,
  primary overlays, or notification/deep-link parity.
- `MainActivity`, `NovaPrimaryHost`, `NovaApp`, signals, dispatchers, and special
  Activities share root-navigation ownership implicitly.
- the live conversation path is `ConversationScreen -> ConversationScreenV9 ->
  ConversationScreenV8`, with multiple UI and inset owners;
- route-level Composables construct repositories, launch asynchronous work,
  interpret authentication failures, and mutate navigation;
- `NovaApiClient` and feature repositories concentrate unrelated DTOs/parsers;
- the Django `accounts` app owns most product domains;
- live production names still encode V4/V8/V9 history; and
- tracked IDE state and a custom `androidx.compose...` namespace need explicit
  decisions.

Large mixed-responsibility owners at the baseline include
`ConversationScreenV8.kt`, `NovaApp.kt`, `StoriesRail.kt`,
`ConversationScreenV9.kt`, `ReelsScreen.kt`, `NovaCallController.kt`,
`CallActivity.kt`, `NovaMessagingRepository.kt`, `GroupInfoDialog.kt`, and
`NotificationsScreen.kt`. File size is evidence, not the rule: split only by
ownership and independent reasons to change.

## Protected contracts

Unless a dedicated approved PR explicitly changes one, preserve:

- Android application ID `com.omarkhair70.nova`, signing identity, and version;
- the Google Play workflow and its existing automatic/manual target rules;
- REST paths, HTTP methods, request/response semantics, and status behavior;
- WebSocket paths, event types, and payload semantics;
- Django app labels, database table identity, and all migration history;
- authentication, refresh, session-expiry, and logout behavior;
- notification kinds, push data keys, and intent extras;
- message optimistic send, retry, idempotency, drafts, edit/delete, replies,
  reactions, photos, voice notes, paging, unread state, delivery/read receipts,
  realtime reconciliation, typing/presence, sharing, groups, themes, search,
  context/media, and call-history integrity;
- calls, signaling, negotiation IDs, WebRTC, STUN/TURN, Telecom, notifications,
  REST fallback, and history behavior;
- visible product behavior; and
- backend deployment and environment contracts.

Historical Django migration filenames are immutable and retain their version
numbers.

## Target Android architecture

Keep one Gradle app module initially. Establish package and API boundaries
before considering module extraction.

```text
com.nova.app/
|- app/          application, activity, app host, app state/viewmodel/container
|- navigation/   typed destinations, navigator, deep-link router, root policy
|- core/
|  |- network/   HTTP engine, auth interceptor, ApiResult only
|  |- session/   session storage and expiry coordination
|  |- realtime/  generic socket lifecycle primitives
|  |- media/     shared upload/image/audio utilities
|  |- ui/        genuinely reusable components and tokens
|  `- testing/   fakes and fixtures
`- feature/
   |- auth/      |- feed/       |- people/        |- profile/
   |- posts/     |- stories/    |- reels/         |- messages/
   |- calls/     |- notifications/               |- privacy/
   |- sharing/   `- settings/
```

Substantial features use a predictable shape: navigation, data/remote,
data/local, domain/model, optional reusable use cases, route screens,
lifecycle-aware state owners, stateless content, and responsibility-specific
subfeatures.

Android ownership rules:

- a Screen is a route-level entry that wires state to UI;
- a ViewModel (or equivalent explicit lifecycle-aware owner) owns asynchronous
  orchestration and durable route state;
- stateless Composables render values and emit events;
- one domain repository owns data access, never UI state;
- DTOs and wire parsing stay in feature `data/remote` packages;
- navigation uses typed destinations/navigator interfaces;
- session expiry has one coordinator;
- every window/system/IME inset has one named owner;
- special Activities reuse the same feature route and state owner; and
- avoid generic `Utils`, `Manager`, and `Components` dumping grounds.

Introduce a lightweight explicit `AppContainer` before considering a DI
framework. Construct API/session/repository dependencies once and expose
interfaces or factories to state owners.

## Target backend architecture

First form domain packages inside the existing `accounts` Django app. Do not
split Django apps or move model ownership until migration impact is separately
planned and approved.

```text
backend/accounts/
|- api/
|- auth/
|- social/
|- posts/
|- notifications/
|- messaging/   models, serializers, selectors, services, views, realtime,
|               routing, urls
|- calls/
|- stories/
|- reels/
|- privacy/
|- sharing/
|- trust_safety/
`- tests/       organized by behavior and domain
```

Backend ownership rules:

- preserve app labels, table names, migrations, URLs, and WebSocket routes;
- domain `urls.py` modules are included by the existing root;
- selectors own read/query logic;
- services own mutation/orchestration;
- serializers/views own transport validation;
- version-labelled live modules move through compatibility imports and tests;
  and
- a true Django-app split is out of scope until package boundaries stabilize.

## Execution roadmap and exit gates

### Phase -1 — Blacksmith runner migration

Move every active Linux workflow job to `blacksmith-2vcpu-ubuntu-2404` in a
dedicated PR. Preserve every trigger, permission, secret, concurrency rule,
build step, release target, and publishing behavior. Prove named Blacksmith
runners actually pick up CI; do not dispatch Google Play to test the change.

Exit: PR CI is green on Blacksmith and the runner-only PR is merged.

### Phase 0 — freeze and baseline (PR 1–2)

- declare the feature freeze and record this plan;
- document current ownership, entry paths, REST routes, and WebSocket routes;
- characterize root policy, push/deep-link routing, session expiry, and
  Messages navigation/IME ownership;
- add device/Compose checks where the production code has a reliable seam; and
- maintain a manual Samsung smoke checklist for behavior automation cannot yet
  prove.

Exit: automated assertions are mutation-sensitive for the protected seams;
unautomated device behavior is explicitly recorded rather than inferred from a
build.

### Phase 1 — application shell consolidation (PR 3–4)

- replace signal/dispatcher ambiguity with typed destinations and one navigator;
- make one understandable app-host boundary while keeping the social tree alive;
- move global bootstrap/session state to `AppViewModel`;
- centralize intent parsing in `DeepLinkRouter`;
- share route factories across normal and special entries; and
- document system-bar/IME ownership for every Activity.

Exit: normal tabs, notifications, deep links, back, and logout are equivalent.

### Phase 2 — Messages consolidation (PR 5–10)

In order: extract models; add repository interfaces/fakes; add
`ConversationViewModel`; extract stateless list/row/date/unread UI; extract
composer/voice/attachments with one inset owner; extract details/search/media/
theme/groups; switch `ConversationScreen` to the new owners; then delete V8/V9,
V9 tools, and the backend V9 live module once behavior is proven. Rename tests
by behavior.

Exit: all protected messaging behaviors remain green and there is one
conversation screen and one composer inset owner.

### Phase 3 — Calls isolation (PR 11–14)

Separate Activity/UI, state machine, signaling transport, WebRTC engine,
Telecom bridge, notification actions, and history persistence. Do not change
transport algorithms. Add lifecycle tests for outgoing/incoming, answer,
decline, cancel, reconnect, end, background/foreground, and notification paths.

Exit: backend call suites and Android state-machine tests are green.

### Phase 4 — social content features (PR 15–22)

Process one feature at a time: feed/posts/comments; people/profile/social graph;
Stories; Reels; sharing/notifications; privacy/settings/security. For each:
characterize, introduce state ownership, split stateless UI, isolate repository
models, and delete superseded code.

### Phase 5 — network and shared UI cleanup (PR 23–25)

Reduce `NovaApiClient` to HTTP/auth/error primitives, move DTOs/parsers to
features, replace `NovaComponents.kt` with narrow owners, remove equivalent V4
profile components, decide the icon-alias namespace, and remove unintentional
tracked IDE state.

### Phase 6 — backend domain packages (PR 26–32)

Introduce domain URL modules, then move serializers/views/selectors/services/
realtime with compatibility imports. Preserve model/migration identity. Organize
tests by domain and behavior without renaming migrations.

### Phase 7 — final enforcement (PR 33)

Delete unreachable legacy code and shims with import proof, add architecture
checks for forbidden dependencies/naming, update contributor guidance, and run
complete CI, release builds, the Closed-testing smoke matrix, and a production-
path audit.

## PR safety policy

Every consolidation PR has one purpose; lists added/moved/deleted owners;
identifies protected behavior and evidence; avoids unrelated product work;
passes backend tests, Android JVM tests, lint, debug APK, release APK, and release
AAB; reports automated versus physical verification; and provides a rollback
point. Architecture-only PRs do not bump versions or publish releases.

Stop for a decision when behavior is ambiguous, implementations conflict, a
database/public contract would change, call transport behavior would change, a
credential or external permission is unavailable, a destructive action is
required, a pre-existing CI failure cannot be separated, or a physical result is
required before the next design is safe.

## Definition of done

- no live production V-number names remain except immutable history;
- each feature's route, state, domain model, and data access are discoverable;
- route Composables do not perform repository/network orchestration;
- root navigation, deep links, and session expiry each have one owner;
- Messages has one screen and one composer inset owner;
- Calls has a testable state machine separate from Activity UI;
- shared network contains primitives, not all feature DTOs;
- backend domain packages are clear while migrations/contracts remain stable;
- critical Android paths have meaningful automated and device coverage; and
- CI, Closed testing, and the Samsung smoke matrix remain green.
