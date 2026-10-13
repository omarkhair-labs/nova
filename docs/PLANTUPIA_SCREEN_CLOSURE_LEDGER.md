# PLANTUPIA — Native Screen-by-Screen Closure Ledger (2026-10-13)

**Status: ACTIVE WORKING AUDIT — no screen is signed off yet.**
Branch: `feature/plantupia-native-experience-20261012`, stacked on `feature/plantupia-product-conversion-20261010` (PR #241 on #240).
Android source is Kotlin/Jetpack Compose in `omarkhair-labs/nova`. Nova's network, authentication, social/messaging/media/player/publishing engine is an asset to preserve; legacy Nova *user-facing content* is not approved final Plantupia. Do not merge, migrate Production DB, change Play signing/package or promote stores from this ledger.

## What each status means

- **PLANTUPIA-CODED:** custom Plantupia functionality exists in the branch, but no final UX or on-device acceptance implied.
- **HYBRID:** a Plantupia surface reuses a visible Nova screen or partially restyled feature.
- **NOVA-INHERITED:** functional engine/screen exists, but its Plantupia role, wording and design still require review.
- **GAP:** a necessary capability has no complete proven implementation.
- **ACCEPTED:** forbidden without visual/product review AND passing current-commit CI AND physical QA with authenticated staged data, including errors/privacy.

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
| S02 | Space switcher | GAP (previously implicit expansion) | `PlantupiaClientSection.kt` | switcher must respect allowed spaces, retain selection, no cross-site mixing |
| S03 | Create a private space | PLANTUPIA-CODED | `PlantupiaSelfServiceForms.kt`, Django spaces | shorten form, explicit ownership, zero-data UX |
| S04 | Space overview / plants | HYBRID | `PlantupiaClientSection.kt` | dedicated active space; nonduplicate hierarchy, camera-first |
| S05 | Plants inventory, search, many units | PLANTUPIA-CODED | `PlantupiaSpaceBrowser.kt` | scoped search; 1/7/100+ QA, list performance |
| S06 | Space edit/archive, zones, site observations | GAP | no approved mature contract | C1; explicit decision, no phantom controls |
| S07 | Space members/role management UI | GAP | backend `SpaceMember` read permission exists | user/admin workflows, invites and revocation require approval |
| P01 | Add a plant | PLANTUPIA-CODED | forms, `PlantupiaRepository`, Django API | create with/without photo, rollback, open new plant |
| P02 | Plant profile/identity | PLANTUPIA-CODED | `PlantupiaClientSection.kt` selected plant | action hierarchy, typography/photo, offline/retry |
| P03 | Private personal nickname | PLANTUPIA-CODED | nickname endpoint | separate botanical vs user nickname, edit/retry |
| P04 | Edit place/species, retire/replace | GAP | no proved end-to-end UI | C1 user confirmation, retained past history |
| C01 | Daily check-ins overview | PLANTUPIA-CODED | `PlantupiaTodayPanel.kt` | date/rules, empty/due/upcoming, UX QA |
| C02 | Record actual care action | PLANTUPIA-CODED | care tiles/events/typed history | icon-based compact composer, no watering inference |
| C03 | Record observation/photo | PLANTUPIA-CODED | events and photo upload | image retry/upload states, permissions |
| C04 | Report concern | PLANTUPIA-CODED | event kind `issue` | easy next action without fake diagnosis |
| C05 | Choose follow-up/complete outcome | PLANTUPIA-CODED | follow-up DB fields and transactions | own dates, pending pagination, no duplicate outcome |
| C06 | Living chronology and photo comparison | PLANTUPIA-CODED | history paging, `PlantupiaPhotoViewer.kt` | visible timestamps, photo quality/zoom, long histories |
| C07 | Local reminders / reschedule / cancel | GAP | existing Nova notifications do not implement care reminders | Android consented scheduling, privacy, boot/reinstall/sign-out behavior |
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

## Acceptance: what "close this screen" requires

1. **Product contract:** understandable task; place within physical living experience; no phantom features.
2. **Visual:** approved Plantupia identity, Compose layouts, real photos, mobile typography, parity analysis against design reference (not HTML copy).
3. **Interactions:** taps/nav/Android Back, keyboard, motion, content, states (loading, empty, failed, populated, read-only), including 1/7/100+ plants where relevant.
4. **Data & permission:** actual authenticated isolated QA, public/private boundaries, no unauthorized views, no fabricated customer/plant/sensor/AI statuses.
5. **Quality:** tests for policy and state, green latest-head CI, screenshots and real-device walkthrough.
6. **Owner review:** approve the screen/related journey; only then update this ledger's status to **ACCEPTED**.

Screens are reviewed as **connected closure groups**, rather than polishing a single fragment: 
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
- Next unit: G1 space switcher and selected-space filtering, followed by an actual QA APK visual comparison with the user's captured screenshots.
