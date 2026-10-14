# PLANTUPIA — Native Screen-by-Screen Closure Ledger (2026-10-13)

**Status: ACTIVE BUILD LEDGER — user-facing final review is deferred until the complete integrated APK.**
Branch: `feature/plantupia-native-experience-20261012`, stacked on `feature/plantupia-product-conversion-20261010` (PR #241 on #240).
Android source is Kotlin/Jetpack Compose in `omarkhair-labs/nova`. Nova's network, authentication, social/messaging/media/player/publishing engine is an asset to preserve; legacy Nova *user-facing content* is not approved final Plantupia. Do not merge, migrate Production DB, change Play signing/package or promote stores from this ledger.

## What each status means

- **PLANTUPIA-CODED:** custom Plantupia functionality exists in the branch, but no final UX or on-device acceptance implied.
- **HYBRID:** a Plantupia surface reuses a visible Nova screen or partially restyled feature.
- **NOVA-INHERITED:** functional engine/screen exists, but its Plantupia role, wording and design still require review.
- **GAP:** a necessary capability has no complete proven implementation.
- **ACCEPTED:** reserved for the final integrated user review, after green current-commit CI and physical QA with authenticated staged data, including errors/privacy. Interim groups can be tagged BUILT or INTERNALLY TESTED, never owner-accepted.

All entries below are **not accepted** pending the listed evidence. Legacy names in internal Kotlin classes may stay where removing them risks regression; app-facing names must be coherent. Do not substitute the HTML demo for runtime evidence.

## Product architecture — user-facing screen ledger

| ID | Destination / screen contract | Status at audit | Implementation / dependency | Primary closure work |
| --- | --- | --- | --- | --- |
| A00 | Startup, saved-session resume, offline & expiry | NOVA-INHERITED | `NovaApp.kt`, `MainActivity.kt`, auth repositories | session restore, brand, expired/offline QA |
| A01 | Welcome | HYBRID | `feature/welcome/WelcomeScreen.kt` | approved brand assets, concise benefit, sign-in path |
| A02 | Login, signup, account errors | HYBRID | `feature/auth/AuthScreens.kt` | Plantupia copy, recovery and keyboard/accessibility |
| A03 | Profile setup | NOVA-INHERITED | `feature/onboarding/ProfileSetupScreen.kt` | lead into first actual space and plant |
| A04 | First-run create space → first plant | PLANTUPIA-CODED | `PlantupiaSelfServiceForms.kt`, `PlantupiaCreateIntent.kt` | empty user, save failure/retry, photo, return to created plant |
| S01 | Living Home / Today | HYBRID | `HomeScreen.kt`, `PlantupiaClientSection.kt`, `PlantupiaTodayPanel.kt` | plant/attention first, selected-space focus, no inherited feed dominance |
| S02 | Space switcher | PLANTUPIA-CODED, AWAITING QA | `PlantupiaSpaceSwitcher.kt`, `PlantupiaSpaceFocus.kt` | chosen authorized site, saved rotation state, scoped hero/recent/inventory; test device |
| S03 | Create a private space | PLANTUPIA-CODED | `PlantupiaSelfServiceForms.kt`, Django spaces | shorten form, explicit ownership, zero-data UX |
| S04 | Space overview / plants | HYBRID | `PlantupiaClientSection.kt` | dedicated active space; nonduplicate hierarchy, camera-first |
| S05 | Plants inventory, search, many units | PLANTUPIA-CODED | `PlantupiaSpaceBrowser.kt` | scoped search; 1/7/100+ QA, list performance |
| S06 | Space metadata editing; space archive, zones and site observations separately | PARTIAL C1 CODED (edit), ARCHIVE/ZONES GAP | `PlantupiaSpaceDetailsEditor.kt`, scoped Django `SpaceDetailView` PATCH; no space archive model yet | real owner/manager change + authorization/device QA; **do not** mark space archive or zones done |
| S07 | Space members/role management UI | GAP | backend `SpaceMember` read permission exists | user/admin workflows, invites and revocation require approval |
| P01 | Add a plant | PLANTUPIA-CODED | forms, `PlantupiaRepository`, Django API | create with/without photo, rollback, open new plant |
| P02 | Plant profile/identity | PLANTUPIA-CODED | `PlantupiaClientSection.kt` selected plant | action hierarchy, typography/photo, offline/retry |
| P03 | Private personal nickname | PLANTUPIA-CODED | nickname endpoint | separate botanical vs user nickname, edit/retry |
| P04 | Shared context edit; reversible plant archive/restore; replacement separate | C1 CODED (edit + archive) / QA OPEN; REPLACEMENT GAP | `PlantDetailView`, `PlantUpdateSerializer` using existing `PlantUnit.is_active`, `PlantupiaClientSection.kt` archive controls; private administrative event retains history | Verify archived/history permissions, old reminders, on-device persistence. **Replacement/new biological identity is not implemented** |
| C01 | Daily check-ins overview | PLANTUPIA-CODED | `PlantupiaTodayPanel.kt` | date/rules, empty/due/upcoming, UX QA |
| C02 | Record actual care action | PLANTUPIA-CODED | care tiles/events/typed history | icon-based compact composer, no watering inference |
| C03 | Record observation/photo | PLANTUPIA-CODED | events and photo upload | image retry/upload states, permissions |
| C04 | Report concern | PLANTUPIA-CODED | event kind `issue` | easy next action without fake diagnosis |
| C05 | Choose follow-up/complete outcome | PLANTUPIA-CODED | follow-up DB fields and transactions | own dates, pending pagination, no duplicate outcome |
| C06 | Living chronology and photo comparison | PLANTUPIA-CODED | history paging, `PlantupiaPhotoViewer.kt` | visible timestamps, photo quality/zoom, long histories |
| C07 | Opt-in local check-in reminders / cancel | PLANTUPIA-CODED, PARTIAL | `PlantupiaCareReminderScheduler.kt`, WorkManager, `PlantupiaClientSection.kt`; server-created event UUID, user opt-in, approx 9 AM, calendar-date choice, generic private notification and optional re-enable from pending review | exact-device delivery, denied permission, battery/doze, full server-side rescheduling and cross-device outcome consistency unverified |
| C08 | Sourced plant care guide | GAP | not an approved curated corpus | cite real sources; avoid invented biology |
| AI01 | General & context-scoped care assistant | PLANTUPIA-CODED, DISABLED | `PlantupiaAssistantSheet.kt`, `backend/plants/assistant.py` | isolated QA provider key, actual reply/device QA, policy confirmation |
| AI02 | AI photos, cited resources, answer-to-care action | GAP | text only, no RAG | optional C1, explicit user save |
| M01 | Circle plant Q&A + pagination | HYBRID | `OrbitScreen.kt`, `PlantupiaCommunityPanels.kt` | distinct useful discovery, all states, end-to-end |
| M02 | Public question and precise preview/consent | HYBRID | plant discussion + Nova Post engine | post and verify account-separated private/public |
| M03 | Public post detail, comments, reporting | NOVA-INHERITED | post detail/comments/privacy | visual and permission QA, return to plant/outcome |
| M04 | People/relationships/followers | NOVA-INHERITED | `PeopleScreen`, `PersonScreen`, social graph | role in living community, Plantupia visual language |
| M05 | Generic Create post/publishing queue | NOVA-INHERITED | `CreatePostScreen`, background media pipeline | Plantupia audience, publishing/abort/retry |
| M06 | Stories 24h, viewer, privacy | NOVA-INHERITED | `StoriesRail.kt`, repository | preserve media viewer; story↔plant is NOT implemented |
| M07 | Reels/video | NOVA-INHERITED | `ReelsScreen.kt`, player/upload | decide role and brand; Android performance |
| M08 | Optional Pulse, Memories, Rooms, Tonight | NOVA-INHERITED | respective feature packages | inspect value; keep secondary or remove from navigation, never delete underlying data |
| U01 | My profile and private spaces | HYBRID | `ProfileScreen.kt`, `PlantupiaProfileSpaces.kt` | public/private separation, visual QA |
| U02 | Edit profile, followers, other profiles | NOVA-INHERITED | profile/social packages | user copy, private memberships hidden |
| U03 | Inbox/conversation/groups/voice/video | NOVA-INHERITED | `MessagesRootRoute`, `ConversationScreen`, `CallActivity` | keep engine; Plantupia surface, calls QA |
| U04 | Activity notifications + privacy/security | NOVA-INHERITED | notifications/settings/privacy/security | brand copy, care-vs-social activity, permission truth |
| U05 | Legal, support and account lifecycle | HYBRID | welcome/auth/settings and policy | approved URLs, data rights, contact channels |
| B01 | Company installation → support/service journey | GAP / COMPANY-GATED | Django Admin is not an approved customer service contract | need real roster, responsibilities, SLA & entitlement data |
| WEB | Public Plantupia creative website | SEPARATE PRODUCT | research repo scope | do not conflate with Android release |

## Backend / invisible engine ledger

| Layer | Exists | Next evidence gate |
| --- | --- | --- |
| Kotlin Compose/navigation | original Nova + four Plantupia primary tabs | RTL/back/gesture/deep-link/device matrix |
| Auth, refresh and accounts | original Nova engine | QA session refresh + two accounts |
| Django Plantupia models & migrations | Space, SpaceMember, PlantUnit, nickname, event, discussion, followups | isolated QA DB backup/migration check; NEVER Production without approval |
| Access control | owner/manager/carer/viewer filters, scoped event/photo and public post | real account separation and ID guessing tests |
| Media/story/reels pipeline | original Nova storage, upload, playback, background publish | genuine plant photos; large media; metadata sanitation and privacy |
| Messages/social/realtime | original Nova relationships, group conversation and calls | Android background, privacy, network failure QA |
| AI provider adapters | optional OpenAI Responses / Groq Chat Completions; disabled without secret | Groq **Free Plan** secure key on isolated QA; no claims of infinite free tokens |
| No sensors | **deliberate scope boundary** | human-observed conditions only; no fake soil readings |
| Rate limits/Redis/cache | Django optional Redis throttling; safety caps | QA Redis in production-mode backend, quota and error tests |
| CI | Android tests/lint/APK/AAB, Django tests/migration checks | green CI on exact HEAD + physical run + acceptable privacy |
| Distribution | QA applicationId suffix `.qa`; original Play ID reserved | no signing, Production rollout or Play changes without explicit permission |
| Local care reminders | WorkManager one-off scheduling after real saved Django event, explicit per-event opt-in, cancel button, user-id-scoped generic text, logout cancels all | no exact delivery promise; verify background limits, multiple accounts, app restarts, Android 13+ permission |

## Release language — new stakeholder requirement (2026-10-14)

**Decision: English-first user interface for the integrated Plantupia release.** A stakeholder has asked for the Android app to be in English. This supersedes the earlier Arabic-first prototype copy for the *shipping UI*, but does not change data contracts, navigation architecture, backend hosting, or the planned final-only user review.

Language acceptance gates:
1. **All product-owned UI strings in English:** Welcome, authentication, first-use, Home, space switcher, plants, care/check-ins, notifications, photo viewer, AI, Circle, Create, public preview, Profile, Inbox, Stories/Reels where exposed, Settings, privacy and error/offline/permission states. No English heading with an unexplained Arabic subtitle.
2. **User-authored data is never auto-translated:** user-entered plant nicknames, space names, care notes, posts and messages remain in their original language and support Unicode/RTL input. Existing stored records are untouched.
3. **English-aware layout:** LTR navigation labels, correctly mirrored directional arrows/back, meaningful locale-aware date/time/number presentation (not forced US formats), accessible TalkBack labels and clear 12/24h dates as appropriate.
4. **Android resource strategy:** centralize new/modified app-visible strings in `strings.xml`/feature resource files where practical; avoid introducing new hardcoded Arabic strings. Preserve protected navigation/security architecture tests and update their contracts deliberately, never by bypassing safeguards.
5. **AI provider:** assistant replies in the user's requested language (default product English); no system assumption that English only input is valid. Server errors and third-party outages present a consistent English UI; no invented answer on failure.
6. **Privacy/device:** English user-facing notification channel names and generic privacy-safe content; no translation of private records by the background worker; permission rationale stays clear and optional.
7. **Validation:** check every primary journey and all empty/loading/error/read-only states on a device, including mixed-direction user-generated text. Do not mark language work complete from grep/CI alone.

**Evidence from sampled source files (not full-app exhaustive):** `PlantupiaClientSection.kt` still includes many Arabic literals, as do `PlantupiaTodayPanel.kt`, `PlantupiaSelfServiceForms.kt`, `PlantupiaCommunityPanels.kt`, `CreateHubScreen.kt`, `OrbitScreen.kt`, `ProfileScreen.kt`, `WelcomeScreen.kt`, `SettingsActivity.kt`, and `PlantupiaCareReminderScheduler.kt`. The existing `app/src/main/res/values/strings.xml` is tiny; broad cleanup remains. Inherited Nova screens require a separate app-facing copy check.

**Scope discipline:** do not call this a new complete translation/i18n feature, a completed English build, or an approved localization design. First implement English V1 consistently; other interface locales are optional future work.

## English-first implementation progress (2026-10-14)

- English-first copy has now been **implemented** in the Plantupia-native Home, space switcher, add space/plant forms, plant record/care/check-ins, Today, living history/photo viewer, AI sheet, Circle/public consent preview, Create hub, Profile's private spaces, Welcome, and key Settings strings.
- Care-action labels and new authored action descriptions are English. **Existing notes/nicknames/observations remain in their original language in storage**, untouched by this UI change; new care actions record English labels only when a user explicitly selects them.
- Main Plantupia tab labels and create accessibility label are English. The Compose app host supplies LTR for product navigation, including devices with RTL system settings. User-authored content continues to support Unicode text input.
- English-only generic local notification channel labels/body, session unlock wording and incomplete-assistant error.
- A CI check, `scripts/check_plantupia_english.py`, guards these **specified newly converted source entrypoints** and the Plantupia feature module against Arabic-script hardcoded copy. It is **not proof the inherited Nova engine's entire surface is localized**.
- **Still open:** all inherited Nova-authored visible UI (messages, post detail, followers, stories/reels, secondary tools, auth/legal/privacy system activities); network-sourced messages from Django/Nova; full user-journey language and accessibility checks, app-specific locale behavior outside Compose; approved brand typeface and final device QA.
- C0/C1 implementation, true backend QA and free-tier AI activation remain separate gates. No final review until one integrated APK.

## Acceptance: what "close this screen" requires

1. **Product contract:** understandable task; place within physical living experience; no phantom features.
2. **Visual:** approved Plantupia identity, Compose layouts, real photos, mobile typography, parity analysis against design reference (not HTML copy).
3. **Interactions:** taps/nav/Android Back, keyboard, motion, content, states (loading, empty, failed, populated, read-only), including 1/7/100+ plants where relevant.
4. **Data & permission:** actual authenticated isolated QA, public/private boundaries, no unauthorized views, no fabricated customer/plant/sensor/AI statuses.
5. **Quality:** tests for policy and state, green latest-head CI, screenshots and real-device walkthrough.
6. **Owner review (FINAL ONLY):** user reviews the complete cohesive Plantupia APK across all groups at the end, after internal engineering/CI checks. No per-screen user approval or repeated stop-and-wait checkpoints. Only then mark user-approved screens ACCEPTED.

Build the following connected groups continuously, with developer-side testing/CI and fixes during implementation. **Defer the user's visual review and final QA walkthrough until all groups have an integrated candidate**:
- G0 visual system, app shell, typography/icons/status-bars.
- **G1 Living Home → Space navigation → inventory → Plant Profile** (CURRENT; first UX batch).
- G2 care/check-ins/history/photos/consented reminders.
- G3 real AI/optional expert guide.
- G4 Circle/Create/public-post privacy/comments.
- G5 Profile/People/Inbox/calls.
- G6 Stories/Reels/optional social tools.
- G7 notifications/settings/legal/auth plus full E2E, QA and handoff.

### G1 fixture and tests (must not imply fixtures are customers)
- Empty new user: no spaces, clear create flow.
- One owner space: 1 real plant; create/photo/nickname.
- Owner multiple spaces: pick the correct space and keep unrelated plants out of hero/list, while due personal tasks remain discoverable.
- Read-only shared space: can browse but cannot add a new plant or record care.
- 100+ inventory: search, paging, date ordering, stable back behavior.
- Upload error, backend 404 and stale selection: truthful retry, never "empty account."
- Real device: RTL, top status-bar contrast, keyboard, camera/media and low-network visual state.

### Progress log
- 2026-10-13: code/PR-wide inventory performed. **No screen marked accepted.**
- 2026-10-13 G1 iteration: selected-space navigation and scoped photo hero/inventory/recent moments; first plant creation defaults to the focused writable space and navigates to its real profile after save; moved recording care ahead of long timelines. Kotlin policy tests added. **All remain CODED, NOT ACCEPTED** pending exact-head CI and real-device QA.
- CI diagnostic: initial G1 run found a trailing space in this ledger (not an Android compiler issue); this update strips it. Continue following Android/Django runs for actual build results.
- Next: continue G1 implementation and then G2–G7 without pausing for user screen reviews. Collect internal test evidence and design notes as work proceeds; review the complete integrated APK together only at the end.

- 2026-10-13 workflow decision from user: **final app-wide review only**, not one approval after every screen. CI, permission checks and regression fixes remain continuous internal engineering responsibilities; Production/Play safety gates still apply.

- 2026-10-13 G2 increment (still not ACCEPTED): optional Android WorkManager check-in notifications for *user-chosen dates* only; event ID + owner ID scoped, one-time generic content, cancel without erasing the actual Django care record, session logout clears all local work. No moisture sensor, no automated watering instruction and no notification if the event POST failed. Free AI remains server-configurable but not live QA verified.
- 2026-10-13 G0 increment: readable dark status-bar glyphs on the cream/native account and messaging surfaces; Plantupia labels for activity/DM notification channels, settings copy and onboarding without claiming a company installation is compulsory. Must check dark video/overlay exceptions during final E2E.

- 2026-10-13 G2 follow-up increment: user chooses an arbitrary future check-in date with Android native calendar (existing 7/14-day shortcuts remain); explicit consent can be added **after** a previously saved review. Same Django data and exact review event ID are used; dates are validated and a past custom date blocks save. Tests cover optional/future/past/custom date precedence. A WorkManager failure cannot block logout or crash local reminder cancellation: its synchronous privacy gate clears independently.

- 2026-10-14: stakeholder language requirement changed to **English-first release UI**. Recorded as G0/G7 cross-cutting work; implementation has **not** yet been performed, and final user review remains deferred until integrated APK.

- 2026-10-14 C1 increment: role-guarded shared plant name/species/placement editor and private change event implemented, with Django tests pending latest-head CI. Personal nickname stays separate. Retirement/replacement and site management remain open.

## 2026-10-15 — reconciliation after research-to-native handoff
- The [finite integrated roadmap](./PLANTUPIA_FINITE_INTEGRATED_DELIVERY_ROADMAP_2026-10-15.md) now traces the full research-to-native history and defines closure gates G0–G6, including a separate website and conditional B1 company delivery.
- Plant archive/restore and real space metadata edits **are coded**, not device accepted. Restoring an archived plant preserves event history; it is not the same as replacing a biological individual. Space archive and space zones are **still gaps**.
- For original native head `18cadf3e32486484735e7438d4b02d4f0eec420d`, [Android and backend run 38641919445](https://github.com/omarkhair-labs/nova/actions/runs/38641919445) completed successfully. Later documentation commits require their own head status check; branch CI does not represent real-device or QA-backend proof.
- No screen is marked OWNER ACCEPTED; final visual review remains the single complete QA APK.
