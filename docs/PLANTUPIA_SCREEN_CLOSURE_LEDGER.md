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
| A04 | First-run create space → first plant | PLANTUPIA-CODED / NATIVE NAV PASS | `PlantupiaSelfServiceForms.kt`, `PlantupiaCreateIntent.kt`; editor heading auto-scroll on Create → Home handoff | empty user, actual upload/save error/retry, post-create return on device |
| S01 | Living Home / Today | HYBRID / PASS 07 ORDER CODED | `HomeScreen.kt`, `PlantupiaClientSection.kt`, `PlantupiaTodayPanel.kt`; real selected-space hero now appears before optional AI/create controls; Today explicitly cross-space | on-device 360/390dp, owner/manager/viewer, empty/account-error, real photos, keyboard and gesture audit; no inherited feed dominance |
| S02 | Space switcher | PLANTUPIA-CODED, AWAITING QA | `PlantupiaSpaceSwitcher.kt`, `PlantupiaSpaceFocus.kt` | chosen authorized site, saved rotation state, scoped hero/recent/inventory; test device |
| S03 | Create a private space | PLANTUPIA-CODED | `PlantupiaSelfServiceForms.kt`, Django spaces | shorten form, explicit ownership, zero-data UX |
| S04 | Space overview / plants | HYBRID / MEDIA PROVENANCE CODED | `PlantupiaClientSection.kt`; private space metadata editor positioned next to selected authentic hero; plant-cover photo explicitly labeled as plant-in-space, not a fabricated architectural site photo | real staged 0/1/7/100 device screenshots, responsive layout, missing photo/empty/edit failures |
| S05 | Plants inventory, search, many units | PLANTUPIA-CODED | `PlantupiaSpaceBrowser.kt` | scoped search; 1/7/100+ QA, list performance |
| S06 | Space metadata editing; space archive, zones and site observations separately | PARTIAL C1 CODED (edit), ARCHIVE/ZONES GAP | `PlantupiaSpaceDetailsEditor.kt`, scoped Django `SpaceDetailView` PATCH; no space archive model yet | real owner/manager change + authorization/device QA; **do not** mark space archive or zones done |
| S07 | Space members/role management UI | GAP | backend `SpaceMember` read permission exists | user/admin workflows, invites and revocation require approval |
| P01 | Add a plant | PLANTUPIA-CODED | forms, `PlantupiaRepository`, Django API | create with/without photo, rollback, open new plant |
| P02 | Plant profile/identity | PLANTUPIA-CODED / DETAIL-FIRST REORDER | `PlantupiaClientSection.kt`; authentic selected name/species/placement now precede compact real photo, then one direct care action, check-ins and chronological evidence; maintenance/rename/archive are tucked behind optional details | real 360/390dp device evidence, Back/focus/scroll, archived/viewer behavior, full photo, offline/retry |
| P03 | Private personal nickname | PLANTUPIA-CODED | nickname endpoint | separate botanical vs user nickname, edit/retry |
| P04 | Shared context edit; reversible plant archive/restore; replacement separate | C1 CODED (edit + archive) / QA OPEN; REPLACEMENT GAP | `PlantDetailView`, `PlantUpdateSerializer` using existing `PlantUnit.is_active`, `PlantupiaClientSection.kt` archive controls; private administrative event retains history | Verify archived/history permissions, old reminders, on-device persistence. **Replacement/new biological identity is not implemented** |
| C01 | Daily check-ins overview | PLANTUPIA-CODED / LARGE INVENTORY PASS | `PlantupiaTodayPanel.kt`; cross-space label and progressively reveal all actual due check-ins, 0/1/100 tests | date/rules, state reset, on-device UX QA |
| C02 | Record actual care action | PLANTUPIA-CODED / POST-SAVE NAV GUARD | real private care/observation/issue composer; delayed save does not reopen prior plant after Android Back or overwrite another plant's error | photo+note upload, opt-in notification, rapid Back/save, offline/retry on device |
| C03 | Record observation/photo | PLANTUPIA-CODED | events and photo upload | image retry/upload states, permissions |
| C04 | Report concern | PLANTUPIA-CODED | event kind `issue` | easy next action without fake diagnosis |
| C05 | Choose follow-up/complete outcome | PLANTUPIA-CODED | follow-up DB fields and transactions | own dates, pending pagination, no duplicate outcome |
| C06 | Living chronology and photo comparison | PLANTUPIA-CODED / REQUEST-ISOLATED | paginated history, photo viewer and question outcome refresh; `PlantupiaDetailRequestPolicy.kt` + JVM tests reject late responses for prior plant/reopened same plant | device/network slow-response QA, older pages, zoom, 1 photo truth |
| C07 | Opt-in local check-in reminders / cancel | PLANTUPIA-CODED, PARTIAL | `PlantupiaCareReminderScheduler.kt`, WorkManager, `PlantupiaClientSection.kt`; server-created event UUID, user opt-in, approx 9 AM, calendar-date choice, generic private notification and optional re-enable from pending review | exact-device delivery, denied permission, battery/doze, full server-side rescheduling and cross-device outcome consistency unverified |
| C08 | Sourced plant care guide | GAP | not an approved curated corpus | cite real sources; avoid invented biology |
| AI01 | General & context-scoped care assistant | PLANTUPIA-CODED, DISABLED | `PlantupiaAssistantSheet.kt`, `backend/plants/assistant.py` | isolated QA provider key, actual reply/device QA, policy confirmation |
| AI02 | AI photos, cited resources, answer-to-care action | GAP | text only, no RAG | optional C1, explicit user save |
| M01 | Circle plant Q&A + pagination | HYBRID / QUESTION-FIRST NATIVE PASS | `OrbitScreen.kt`, `PlantupiaCommunityPanels.kt`, `PlantupiaCirclePresentationTest.kt`; compact header, question-first 96dp optional photo, truthful author/reply/outcome labels, Ask from actual plant before list; preserves Nova posts/comments and private/public consent | 360/390/430dp, actual post/replies, two-account consent/EXIF, reader/RTL, refresh and native Back/Inbox/Story QA |
| M02 | Public question and precise preview/consent | HYBRID | plant discussion + Nova Post engine | post and verify account-separated private/public |
| M03 | Public post detail, comments, reporting | NOVA-INHERITED | post detail/comments/privacy | visual and permission QA, return to plant/outcome |
| M04 | People/relationships/followers | NOVA-INHERITED | `PeopleScreen`, `PersonScreen`, social graph | role in living community, Plantupia visual language |
| M05 | Generic Create post/publishing queue | NOVA-INHERITED / ENTRYPOINT NAV IMPROVED | `CreateHubScreen.kt`, `CreatePostScreen`; optional live social editors scroll into view when chosen even after repeat tap | real public post publishing/abort/retry and private-vs-public QA |
| M06 | Stories 24h, viewer, privacy | NOVA-INHERITED / CREATE ENTRYPOINT IMPROVED | `CreateHubScreen.kt` reveals the real existing `StoriesRail` instead of leaving the loaded composer below the fold | user journey camera/gallery/expiry and publishing tests; story↔plant is NOT implemented |
| M07 | Reels/video | NOVA-INHERITED | `ReelsScreen.kt`, player/upload | decide role and brand; Android performance |
| M08 | Optional Pulse, Memories, Rooms, Tonight | NOVA-INHERITED | respective feature packages | inspect value; keep secondary or remove from navigation, never delete underlying data |
| U01 | My profile and private spaces | HYBRID / EXPLICIT REFRESH CODED | `ProfileScreen.kt`, `PlantupiaProfileSpaces.kt`; authenticated owner-only private card has manual real reload after new plant/space | public/private two-account check; actual card photo, count and error/retry device QA |
| U02 | Edit profile, followers, other profiles | NOVA-INHERITED | profile/social packages | user copy, private memberships hidden |
| U03 | Inbox/conversation/groups/voice/video | NOVA-INHERITED / QUERY-FILTER ISOLATION CODED | `MessagesRootRoute`, `InboxViewModel.kt` + JVM tests, `ConversationScreen`, `CallActivity`; changing search/filter drops old rows/cursor immediately and revokes prior requests | end-to-end search/mentions, groups/calls/voice/video, auth/keyboard/device QA |
| U04 | Activity notifications + privacy/security | NOVA-INHERITED | notifications/settings/privacy/security | brand copy, care-vs-social activity, permission truth |
| U05 | Legal, support and account lifecycle | HYBRID | welcome/auth/settings and policy | approved URLs, data rights, contact channels |
| B01 | Company installation → support/service journey | GAP / COMPANY-GATED | Django Admin is not an approved customer service contract | need real roster, responsibilities, SLA & entitlement data |
| WEB | Public Plantupia creative website | SEPARATE PRODUCT | research repo scope | do not conflate with Android release |


### 2026-10-17 actual Native screen closure increment (not client signoff)

- **Home composition / Pass 07:** the selected authenticated space switcher and actual plant-photo hero now sit before the optional AI button and create editors. The next human-chosen check-ins remain cross-space so choosing an individual space cannot silently hide due work. No fake space covers or synthetic activity.
- **First-time / Create handoff:** Create → Home editor retains one stateful form instance; the new space/plant editor heading requests native bring-into-view after opening, rather than being stranded below the photo-led hero. Still requires physical Android layout/keyboard verification.
- **Care overview:** previous 3-item cap no longer strands further real due check-ins; 8-at-a-time expansion and JVM tests for 0/1/2/100. Actual notification schedule unchanged.
- **Truthful display:** hero's count now describes 0, 1, or multiple active plants in the selected space without claiming fabricated observations or care; unit tests cover 0/1/7/100.
- **Proof:** code pushed only on stacked draft PR #241. Exact-final-HEAD Android/backend CI and staged/on-device QA must be checked separately; this update is *code-complete incremental work*, not screen approval or store readiness.

### 2026-10-17 Native screen closure increment 2: Spaces → Plant Profile → Care/History

- **S04 site context:** move the owner/manager space editor beside the selected site hero rather than after secondary Create/AI. A plant's uploaded cover image is labeled `PLANT PHOTO · IN THIS SPACE`; no fake architectural space photography. No-cover space retains abstract background and honest active count.
- **P02 no-cover profile:** show a small native no-cover state, not a broken image or sample photo. Dated observation imagery is still kept separately in private history.
- **P02/C06 request isolation:** every plant-detail open increments an ephemeral generation; opening a different plant, reopening the same plant, or Back invalidates prior network results. Clear previous private history synchronously. Guard initial events, pending check-ins, subsequent history pages, and Circle question outcome refresh before updating visible state. JVM tests reject old plant, same-plant prior generation, and null selection.
- **C02 save navigation:** capture chosen check-in/reminder parameters for the actual save; if user navigates away before the API returns, keep the authoritative server save/optional scheduled local reminder but **do not forcibly reopen the old plant**, alter new plant notes, or show another plant's save error.
- **Still unverified:** physical-device transitions, TalkBack focus, poor/slow network, actual Oracle-hosted QA service revision/media permissions and full preview-user signoff. No merged changes, Oracle migration or Play uploads.

### 2026-10-17 Native screen closure increment 3: Create and Circle

- **Create:** existing `StoriesRail`, `PulseRail`, `RoomsRail`, `MemoriesRail` are maintained; when a secondary format opens, the real section at index 5 scrolls into view. A repeated tap on the same format triggers navigation again (not a no-op). Public-post and private-plant actions remain separate.
- **Circle:** refreshing the real public question feed clears old rows/cursor and revokes any outstanding "load older" requests; stale request results and old errors are ignored, never appended to a new page or shown following a failed refresh. `PlantupiaCirclePagePolicyTest` covers current/stale/multiple refresh revisions.
- **Consent:** the plant question preview, explicit checkbox, text/photo checks, and server-side public consent validation stay intact; this increment does not attach private care records, locations or photos automatically.
- **QA still pending:** native scroll/focus behavior with the keyboard and nested media rails, two-account public post/reply and moderation, media upload, screen reader behavior, slow-connection refresh, and exact HEAD CI. The screen is not owner-accepted.

### 2026-10-17 Native screen closure increment 4: Profile and Inbox + product rationale

- **U01 owner Profile:** the authenticated private-space summary can be refreshed explicitly after creating or changing genuine plants/spaces; loaded photos and counts remain authorized API data, never inserted into another person's public profile. Physical device, second-account and session-change QA pending.
- **U03 Inbox:** a new search/filter immediately hides prior-filter conversations and pagination; previously in-flight responses cannot repopulate the new filter. JVM regressions cover filter failure, changed query before load and delayed responses. Nova's conversations/media/calls engine remains unmodified.
- **Client product handoff source:** [Plantupia Product Bible & Handoff v0.1](./PLANTUPIA_PRODUCT_BIBLE_AND_HANDOFF_V01_2026-10-17.md) now contains product thesis, seven source-derived principles, exploration worlds, decision/evidence log, Pass 04–07 translation, art direction, IA, APIs, privacy, staged evidence gates, separate app-store destination and deliverable annexes. Status **WORKING**, not an already-approved client PDF or final APK.
- **Physical device verification remains open** before any “accepted” stage. The CI outcome belongs to a named exact commit only.

### 2026-10-17 Native design consolidation: Create intent-first page

- **Owner device review triggered a true visual/product intervention**, not just a green palette. See [nine-capture brief audit](./PLANTUPIA_REAL_DEVICE_VISUAL_BRIEF_AUDIT_2026-10-17.md).
- **Profile/Home:** new own-profile Plantupia identity separate from inherited violet Nova theme, shorter branded English-first Home header and truthful singular plant counts. Account/profile, private places, public social content, notifications and DM engine preserved.
- **Create:** prior stacked explanatory hero + optional user-photo decoration + long marketing introduction replaced by native compact Private (record care / add plant / new space) and Public (plant question / post / Story / video) intent groups, with Nova's actual existing editor routes retained. Secondary Pulse, Gathering and Memory still use actual native rails. Existing page is still the center `+` destination until the full-navigation device A/B gate.
- **QA semantic distinction:** the `can_add_plant` field now tracks whether the QA API returned an authorization answer. Missing field shows server-revision warning, explicit denied access shows owner/manager role guidance; **client-side write remains disabled** until server authority is present. A live server-response check is still required; don't infer the owner has actually been granted or denied.
- **Tests / acceptance:** source changed only on draft PR #241. Final exact-HEAD CI, 360/390/430dp physical screenshots, screen-reader focus, two-account public/private test and actual Add Plant create remain outstanding until proven.
- **Later Codex audit (not started):** [Codex scoped audit contract](./PLANTUPIA_CODEX_AUDIT_HANDOFF_CONTRACT.md) preserves research hierarchy, avoids autonomous product redesign, and requires review before accepted code changes.

### 2026-10-17 Native design consolidation: Plant Detail → Care → Check-in → History

- **Plant-first hierarchy:** the selected **real** plant name, optional species and placement lead the detail before its authentic/empty photo; the photo hero is less vertically dominant (160dp). The primary label is `Record care or observation`, not an undefined “moment”.
- **Living time/evidence:** selected human check-ins and real living-history timeline follow the care composer with no intervening rename/species/archive administrative cards. Original paginated history, actual photo viewer, opt-in reminder and outcome flows remain.
- **Secondary management:** personal nickname, shared plant context and archive/restore actions now sit in a collapsible `Plant details & settings` area **after** the real history, preserving existing API permissions and functions. This is a UI hierarchy change only; no data migration.
- **Create fix:** the compact private/public Create refactor initially failed Kotlin compile due to an accidentally removed `remember` import; restored explicitly on commit `38189395`. Do **not** cite the pre-fix build as green.
- **Navigation scroll:** `HomeScreen` already scrolls to the Plantupia living-space item in `onOpenPlant` via the actual owning `LazyColumn`. Keep it and test device jump; don't duplicate a conflicting local scroll call.
- **Upcoming real QA:** long names in Arabic/English, photo zoom, keyboard on care textarea, human-selected dates, fast plant switching, viewer/archived roles, visual 360/390/430dp and Add Plant stale server-permission mismatch. No full-screen sign-off or QA APK from these commits yet.

### 2026-10-17 Native design consolidation: Circle → question → replies → private observed outcome

- **Behavior from briefs and references, not decoration:** [Circle research-to-Compose decision map](./PLANTUPIA_CIRCLE_BRIEF_TO_NATIVE_DECISIONS_2026-10-17.md) traces [Pass 05 Are.na / Day One / Focus Friend](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/07-award-winners-behavioral-teardown-pass-05-2026-10-14.md), [Pass 07](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/09-pass07-integrated-product-synthesis-contract-2026-10-14.md) and [Nova integration visual brief](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/NOVA_INTEGRATION_VISUAL_CONTRACT_V01_2026-10-12.md).
- **Native changes:** concise Circle header, small Ask-from-your-plant action preceding questions, authored title + chosen caption + small optional source photo, **actual** server comment-count grammar and server-confirmed outcome state. No suggestion, expert badge or fictional data added.
- **Preserved:** original Nova post/comment navigation, people/Stories/Inbox engine, Plantupia preview/photo explicit public consent and separate private observed outcome, account-scoped server privacy, paginated Circle refresh.
- **Regression:** new `PlantupiaCirclePresentationTest` validates 0/1/2/100 and negative malformed reply counts. No physical/on-device or two-account signoff yet.

### 2026-10-17 Integration pass: first real action, owned-space recovery, authorization-safe creation

- **Briefs → native:** Pass 05 Focus Friend / Quiet Care (real human task before marketing), Pass 07 (place as context, care as immediate attention, dated evidence as proof, optional AI/social), and the original Nova Integration visual contract guided the change. This is **not** a device-accepted layout or a fake user-study result.
- **Home:** after true scheduled check-ins, `Record care` appears **only** if an active recordable plant actually exists. `+ Add plant` opens the real existing Android/Django form; without an owner/manager-writable space, its sanctioned path starts by creating a private space first. Removed the repeated giant “Your plants, in their place” intro; reduced space hero height, kept authorized user photo/space metadata and honest empty state, moved optional assistant below real creation action.
- **Data meaning:** media accessibility label now says `Photo uploaded for [plant]`, not that the app verified the image depicts an actual plant (e.g., the earlier user test contained book photos). No synthetic flora or care events inserted.
- **Auth-safe private/public intent:** `actionablePlantChoices()` selects only active `canRecord` plants for Record Care or Ask Circle. A read-only plant cannot silently open a dead composer from an action intent; selection offers an owned private plant path and displays a precise explanation. A plant can still be opened normally as read-only when no write intent exists.
- **Tests:** `PlantupiaCreateIntentTest` now verifies active+authorized selection, ignoring read-only and archived records; previous `PlantupiaSpacePermissionPolicyTest` differentiates a preview API missing the `can_add_plant` flag from a role that actually denies adding.
- **P0 NOT RESOLVED REMOTELY:** the account screenshot's Add Plant button disabled despite existing spaces requires checking the actual QA `GET /api/v1/plantupia/spaces/` payload and deployed Django revision/owner vs manager membership. Don't default `can_add_plant` to true, force enable UI, or claim a confirmed staging-backend fix without the network evidence.
- **QA:** inspect 360/390/430dp first useful viewport with 0/1/7/100 records and due/no-due states. Complete a real approved New Space → Add Plant → Care + date → History → Ask preview → actual reply/outcome → Inbox/Story flow and check Back/keyboard and two-account privacy.

### 2026-10-17 Pass 08 / whole-product brand and competitive reset — RESEARCH ONLY

- [Pass 08 cross-product research and honest self-evaluation](./PLANTUPIA_PASS08_WHOLE_PRODUCT_DESIGN_RESEARCH_EVALUATION_2026-10-17.md) re-read the original award-reference atlases and master capability matrix; updated official competitor listings and Apple/Google awards; proposed coherent photography, typography, pictogram, illustration, motion and Profile visual rules.
- **Actual state:** researched proposals, three Me/Profile alternatives, provisional rubric and required tests — **no approval, generated visual assets, font licensing, native UI commits or APK in this research increment**. Previous QA fixes still await exact-device acceptance.
- **Differentiation warning:** Planta and Greg already offer journals, reminders and social community. Current defensible Plantupia thesis is place + real authored care + dated evidence + optional consented question/outcome. Effects on plant health or user's relationship are NOT yet established.
- **Builder's Pass08 clarification:** the original research and native art direction are ALREADY chosen. **No three separate HTML designs or mandatory 3-way A/B**; refine the one integrated Plantupia system, with a real-human + living-places Profile, authentic photographs, a coherent authored illustration/functional icon family, and causal native motion. Historical P-B/P-C ideas are optional inspiration only.
- **Creative reviewer:** [deferred Codex handoff](./PLANTUPIA_CODEX_AUDIT_HANDOFF_CONTRACT.md) must consider Pass08 and can challenge specific weak UX decisions with reversible evidence-based proof, not unilaterally restart the product.
- **Design gate:** evaluate actual first useful action, 0/1/7/100 plant inventory, no-image states, real photos, brand fonts/licensing, Arabic/English fallback, profile self/stranger privacy, media+Inbox, 320–430dp with font scaling, reduced motion and material interaction. Do not confuse concept art with verified client installations.

### 2026-10-17 Pass 09 / integrated avatar identity and art system execution plan — PLANNED, NOT YET CODED

- [Pass 09 one-system Android plan](./PLANTUPIA_PASS09_ONE_SYSTEM_VISUAL_EXECUTION_MAP_2026-10-17.md) consolidates *everything remaining* into W0–W5: QA truth; visual grammar; unique illustrated no-photo **human** avatars and Me; original botanical/architectural empties and real plant photos; Circle/social/motion; later Codex and release.
- **New actual gap:** `NovaAvatar.kt` currently emits an initial (including a possible “0”), which does **not** satisfy user's designed illustrated person identity. The future implementation must be Plantupia-scoped, stable per immutable account, consistent in own/other profiles, posts, comments, Stories and messages, and overridden by user upload. Original icon/character art still needs to be authored; don't call it delivered.
- No 3 HTML alternative product designs, no new app shell/research restart and no invented social/photo/health evidence. Same private/public boundary and a single integrated user-facing QA device review.

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

### 2026-10-17 W2 illustrated avatar initial native integration — NOT ACCEPTED

- **CODED:** new original `PlantupiaAvatar`, deterministic by API numeric account ID, overrides with real authorized profile photo, with visual fallback when photo is missing/failed. Applied to self/public identity, People, Edit Profile preview, direct Inbox.
- **PARTIAL:** remaining posts/comments/Stories/Reels/DM header/notifications, avatar selection visible across devices, unified public profile art direction.
- **QA GATE:** Android JVM/lint/build on exact SHA; actual two-account continuity, large text, 24–96dp, offline and failed image cases. No owner acceptance, signed APK, Play or production changes.

- **W2 social extension CODED:** authentic public Post author + comment/reply author, Story rail and Story viewer/person list use the exact same account-ID illustration fallback. Create-Story without real image shows the actual owner's illustrated identity; separate text-story symbol is not a person. Prior audience/media/reply behavior stays inherited. **NOT device accepted**; backend and Android CI must pass at final HEAD.

### 2026-10-17 W1/W2 cohesive people surfaces / private-profile gate (NOT ACCEPTED)

- **CODED:** one Plantupia public metadata identity card for owner Me, public visitor profile, and Edit Profile preview; saved Nova public social posts/follows and profile links retained. Private spaces are still only included on owner `ProfileScreen`.
- **CODED:** private space summaries gated on positive authenticated account ID; repository, load state, retry and request coroutine keyed per account to avoid old UI data persisting across account switches.
- **PENDING:** two-account permission/device QA; small screens, accessibility and real photo variants. The separate QA backend's Add Plant role mismatch is **still unverified**, and a successful build is not a substitute for that check.

- **W3 initial zero-content art CODED, NOT DEVICE ACCEPTED:** `PlantupiaEmptyPlaceArt` in the owner's empty/missing-photo private-spaces summary replaces a generic brand placeholder. It is explicitly decorative and never used as a real plant/space photo. Final appearance and coherence with onboarding and Care zero states still require physical review.

### 2026-10-17 W0/P0 Add Plant access recovery & narrow-screen creation (CODED; HOSTED QA PENDING)
- **CODED:** Create -> Add Plant with visible but non-writable spaces now reveals an ACCESS state rather than silently opening New Space. The explanation distinguishes server missing `can_add_plant` from verified read-only. A functional **Refresh access** action fetches the actual authenticated space list again.
- **CODED:** main Add plant/New space controls stack at full width with >=48dp minimum height for small devices/large text; backend role checks remain authoritative and no denied write is forced.
- **UNIT TESTS:** creation intent with read-only vs writable space and mixed missing/known permission. **NOT VERIFIED:** deployed preview API version, role response, isolated QA DB/media, on-device text scaling and real Add Plant success. These still require environment + two-account QA.

### 2026-10-17 W2 selected Avatar backend preference — SOURCE-CODED, CI/DEVICE GATED
- Django User model now has a separate `plantupia_avatar_variant` small integer (`-1` deterministic ID-based default or 0..7 authored alternatives). The preference is publicly represented on Me/Person/Post author APIs and validated on authenticated profile editing. Uploaded user image remains the override; Nova `profile_theme` remains independent.
- Migration `0037` and Django authenticated/API validation tests cover save/restore/out-of-range. No production migration run or real account mutation. Android reading, selection, and all social author projections remain a separate gating part of this increment until tested on QA.

- **W2 Stories propagation CODED (CI/device pending):** Stories API author payload now includes the public saved `plantupia_avatar_variant`; Android story author model/parser and story rail/viewer lists use it. Current user's add-story affordance and messaging/conversation peer payloads still require preference-aware wiring. No social ACL or published media visibility changed.

- **2026-10-17 W2 Home + Activity person identity CODED:** logged-in user's Home top-right profile shortcut and social post composer, activity/notification actors, pending follow requester rows now reuse the same selected Plantupia character via NovaAvatar's opt-in identity seam; real photos still override. No change to notifications, follows, feed authorization or payloads. Await exact-head CI + real-device validation.

- **2026-10-17 W2 Reels person identity CODED:** Reels API `_author_payload` now exposes the same public avatar preference. Native Reel author model/parser, live Reels feed, profile Reel player and threaded comment/reply avatars render the saved original character variant through the existing shared `NovaAvatar` seam. Social/media/watch behavior untouched. Needs exact-head Android + Django CI and two-account real-phone confirmation.

### 2026-10-17 W0/W1 Care action text-scaling pass — CODED, PHONE QA PENDING
- **CODED:** existing six native illustrated care actions now use a responsive Compose width+font-scale 2/3-column layout rather than forcing three narrow cells on a small device or 1.5–2× text. Captions center with each corresponding pictogram; backend event semantics and save/permission flows unchanged.
- **UNIT TESTS:** 320/360/390/430 content widths, regular and enlarged font scales, no zero-width assumptions. **GATE:** physical Samsung at 1×, 1.5×, 2× font, long localized names, TalkBack, back/save and screenshot review. Do not mark a screen owner-accepted from compiler success alone.

### 2026-10-17 W0 History unavailable-versus-empty truth — CODED, QA PENDING
- **CODED:** a failed initial/private history request must never display the authored blank-fieldbook “No field notes yet” empty state. UI now distinguishes loading, unavailable, genuine empty, and recorded states.
- **CODED:** a scoped **Retry loading history** action re-fetches only the selected plant’s authenticated history, with the existing request generation check. It does not reset/erase an unsaved care note, attached preview image, check-in choice or switch plants. Actual owner data remains in Django.
- **JVM tests:** state boundaries 0/1/100 records, loading overriding a prior failure, and failed fetch overriding apparent emptiness. **PENDING:** exact-head green CI, offline/reconnect on device, rapid Back/switch during retry, cross-account authorization. No real history data was fabricated or deleted.

### 2026-10-18 Integrity Sprint 01 — F06 mutation isolation

- Private mutation submissions snapshot their values before launch. Care, outcome, reschedule, nickname, shared metadata, archive, space edits and question callbacks use account/lifetime/request guards; Back or another selection cannot apply old history, errors, navigation or busy-state completion.
- Same-account confirmed server saves remain valid after Back; reminder side effects never attach to a different login. Plantupia repositories are bound to their originating login. Shared Nova authentication is unchanged; F01 is a separate investigation.
- Linked-post resolution checks the originating detail again after its own network request. Question panels reset their callback lifetime when the same plant is reopened.
- Focused local JVM regression run: 12 tests passed (mutation runner + existing detail policy). Final sprint CI and physical device cases remain pending; no deployment or hosted migration performed.

### 2026-10-18 Integrity Sprint 01 — F07 pending check-in states

- Pending reviews have independent Loading / Unavailable / Empty / Loaded states. Only a successful empty response renders the no-check-ins message. History can finish independently of pending loading.
- Retry loading check-ins requests only the current plant's pending endpoint and keeps history, selected plant, care note/photo/action/date untouched. Older-page failures retain loaded reviews and expose their own retry error.
- Local JVM run: 17 focused tests passed, including five pending-state/load regressions and the F06/request-isolation tests. Local Django check, migration-drift check and 43 plants tests passed using disposable SQLite/storage; PostgreSQL CI and device evidence remain separate gates.
