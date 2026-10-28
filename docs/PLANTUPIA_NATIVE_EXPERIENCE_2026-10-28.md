# Native experience refinement - 28 October 2026

## Scope and starting evidence

Continue `omarkhair-labs/nova`, draft PR #241, on the existing feature branch.
The repository was fast-forwarded from the preserved CEO implementation to
`22adb76fb27021b5a37a7d302e716af6a82f36bc`. Its Android/backend CI passed in
[run 40640042815](https://github.com/omarkhair-labs/nova/actions/runs/40640042815).
The independent `plantupia-app` repository is outside this task.

The owner's latest request explicitly supersedes the previous four-root navigation
constraint. Primary destinations are **Today / Plants / Create / Shop / Circle**.
Profile is contextual through the authenticated avatar; social Inbox, AI and team
support retain their existing engines and appropriate context.

## Audit and changes

| Finding at the starting commit | Implemented direction |
| --- | --- |
| Root replacement discarded the selected plant, filters and unsaved care input | Retain unique primary Nav3 entries and their saveable state; isolate it by authenticated account. Profile returns to its caller. |
| Today displayed a second copy of plant detail; one-plant creation could resolve in Today | Resolve a requested plant only through the authenticated Plants inventory. Create and Today use that same route. |
| Plants/Shop omitted the avatar header; crowded header/navigation rows risked narrow-screen overflow | Shared authenticated header, weighted five-place navigation, selected-tab semantics and a labeled central Create action. |
| An observation heading and an inventory row could share the same plant name | Keep distinct, accessible inventory actions and full wrapped names. Tests select the actual interactive record. |
| Detail inherited the inventory scroll offset; closing a draft discarded input | Open detail at its header, return to the inventory position, retain drafts across tabs, and ask before explicit draft discard. |
| Chat used a fixed message height inside another scrolling column | Shared bounded conversation sheet, flexible message viewport and composer outside the message scroller; IME send and accessible message input. |
| Human conversation said it was waiting even after a staff reply | Render actual persisted status and sender, retain the draft, reload authorized threads and keep past replies readable after expiry. |
| Support push had no durable notice and no recipient binding in its open payload | Reuse Nova Notification storage, add account-scoped resource hints and validate the original recipient before private navigation. |
| Social Activity implicitly marked unseen updates read | Explicit individual/all read actions, preserving other unread categories and the existing pagination/auth contracts. |
| Order preparation had no customer update | One deduplicated notice for an actual staff DRAFT conversion; no alert for one's own checkout, fake shipment or delivery. |
| Notification settings omitted private categories | Add Team/QA-order push preferences to the existing preferences API. Messages preference still also gates team alerts. |
| The five-root change had made the actual social feed unreachable | Circle's Shared moments opens the existing authenticated feed/posts/stories owner; its contextual back action returns to Circle. |
| Consuming a one-shot plant/ticket/order hint cancelled its own awaited operation | Launch the authorized operation in the destination's lifetime scope after consuming the hint; account/generation checks still gate every result. A real emulator ticket-open failure reproduced this before the fix. |
| General AI questions discarded prior dialogue unless private context was selected | Retain bounded general conversation; exclude prior context-bearing questions and answers without renewed per-question consent. |
| Changing plant or recording a separate inspection could erase a care draft | Confirm discarding before opening another plant; reload records after inspections/outcomes while retaining the separate observation draft. |
| Today check-in heading crowded its status on 320dp | Stack the editorial heading and actual due status; preserve full readable text. |

The existing global Nova palette is already Plantupia's forest/cream/sage palette.
It is preserved. Shared account headers, conversation surfaces, editorial roles,
feedback, forms and icons extend those foundations instead of replacing social,
media, auth, safety, creation or messaging systems.

## Notification truth

- **Care:** the center reads persisted opted-in inspections and human-selected
  check-ins. It never creates a second recurrence/reminder scheduler or enables
  OS alerts automatically. Existing explicit local check-in WorkManager delivery
  remains the delivery mechanism. Inspection schedules currently surface in Today
  and the Care category; they do not silently schedule OS alarms.
- **Team:** only an actual authenticated staff message creates a deduplicated
  Nova notification. Its generic push contains recipient/ticket/plant IDs, never
  message text, plant name, placement, photo or history. Opening still fetches the
  ticket through customer authorization. Expiry never exposes it to another account.
- **Orders:** only a staff preparation transition creates a notice. It means a
  fresh unverified QA DRAFT, not payment, delivery or live inventory fulfillment.
- **Circle:** original follow requests, posts/reels/person routing, block filters,
  pagination and social push behavior remain in their existing owners.
- Muting push does not destroy actual messages, orders or their in-app history.
  Android permission/channel settings and configured Firebase delivery are separate
  gates. Payload/unit/intent tests do not establish real FCM delivery.

Migration `accounts.0038_plantupia_private_activity` is additive. It extends the
existing notification/preferences records; it creates no second consumer database.
Only disposable local/CI databases may be migrated in this task. Never run migrations,
seeding, deployments, Play publication or merges against production as a side effect.

## Verification contract

The focused starting regression suite passed 53 tests (four SQLite-only skips).
After adding notification contracts, the 68-test focused backend run succeeded
(64 passed, four PostgreSQL-only checks skipped on SQLite). The initial navigation and integrated conversation/notification code passed
Android compilation and JVM tests. These are intermediate results, not final-head
or visual acceptance. Final results belong in the delivery evidence.

Use the existing [CEO local setup](PLANTUPIA_CEO_DEMO_246.md), with ordinary separate
operator/reviewer/customer/stranger accounts and one shared disposable backend.
Preserve imported original plant UUIDs and existing care/support/order history.
The opted-in native integration tests refuse production packages/origins and require
the private local fixture; credentials and one-time handover codes never belong in
Git, screenshots or the delivery package.

Review Today -> correct plant -> inspection/history; draft -> another tab -> same
draft; AI paywall/actual unavailable response and per-question consent; actual staff
reply -> authenticated notification/ticket; catalog/category/details/quantity/cart
-> explicit no-charge order -> reopening -> separate staff DRAFT; Create/Circle/
Profile/Settings; back, relaunch, keyboard and 320/390/430dp/tablet/large text.

No live payment, shipping, botanical diagnosis, staff response SLA, real specimen
photo or provider response may be invented. Local UI/HTTP evidence, exact-head CI,
real device/FCM/live-model evidence and owner acceptance must be reported separately.

Checkpoint `77856e76f3c85ae97e0bedbb4032ab942891c531` passed all 453 backend
tests against CI PostgreSQL/Redis, Android unit tests, release lint, all architecture
guards, debug/QA/release APK and release AAB builds in
[run 40649571941](https://github.com/omarkhair-labs/nova/actions/runs/40649571941).
Later fixes require their own successful final-head CI; this checkpoint is not a
substitute for that evidence.

Local Android integration uses the actual disposable Django server and actual
staff/customer/stranger credentials supplied outside Git. The new test class covers
tab/draft/activity restoration, real Circle/Profile/Settings, private support open
intents, categorized notices, unavailable AI without history writes, no-charge
checkout and order reopening, account isolation and manual first-plant creation.
The interrupted-save case requires an explicit `offline=owned-local` argument and
host marker coordination; it never disables the device's or host's network.
The revocation case runs only after a real local staff revocation. Preconditions
that are not satisfied are skips, not proof of a successful flow.

An owned headless AVD initially displayed an Android **System UI** ANR after cold
boot. The local fixture helper may select Wait on that exact OS dialog. It does
not suppress application ANRs. Emulator cold boot on this host and physical-device
startup remain distinct acceptance gates. Native screenshots are captured after
Compose idle and native frame presentation, not inferred from test selectors.

The conversation PDF mentioned in the handoff was not delivered as an accessible
file or link. Only the supplied text attachment was available. No contents of that
missing historical PDF have been inferred; repository contracts and current owner
instructions govern this change.
