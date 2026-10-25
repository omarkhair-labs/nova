# PLANTUPIA / AS-IS — Complete Android User Flow & Information Architecture
**20 October 2026 · source-traced from PR #241 · Current Android product only · not the new CEO/QR proposal**

> **Purpose:** show *what the present app actually does and where each tap leads*. This is the **AS-IS** application map for the CEO's critique, not a concept for how it might work in a future Plantupia installation business. Source-coded is not equivalent to device-tested or production-live.

## Legend and confidence

- **Solid route:** action and destination exist in the native app's current code.
- **Conditional route:** only opens with a real authorized space/plant, user action or explicit consent.
- **Secondary inherited Nova engine:** present in the Android app but not necessarily endorsed as a core Plantupia value proposition.
- **Back/Retry/Empty/Loading/Denied** are real states, not separate product features.
- **NOT CURRENT:** installation QR, batch claim, delivered plant provisioning, verified species-care instructions, professional service visits, subscriptions, automatic watering, diagnosis and IoT are **not** part of this AS-IS map.

## A. Complete app-level navigation and entry

~~~mermaid
flowchart TB
  Launch["Launch Android Plantupia (Nova native package)"] --> Auth{"Valid cached or new session?"}
  Auth -- "No session / invalid" --> Welcome["Welcome"]
  Welcome --> Login["Log in"] --> AuthOK{"Authenticated?"}
  Welcome --> Register["Create account"] --> AuthOK
  Welcome --> Terms["Terms / Privacy"]
  AuthOK -- "No" --> Login
  AuthOK -- "Yes" --> Home
  Auth -- "Yes: restored or cached" --> Home["MY SPACE / Home"]

  Home <--> Circle["CIRCLE / community"]
  Home <--> Create["CREATE / private or public actions"]
  Home <--> Me["ME / own profile"]
  Home --> People["Discover People / search and follow"]
  Circle --> People
  Home --> Activity["Activity / notifications"]
  Home --> Inbox["Inbox / messages"]
  Circle --> Inbox
  Me --> Inbox
  Home --> PlantSpace["My spaces and plants (embedded in Home)"]
  Home --> CommunityPeek["Optional shared moments in Home"]
  CommunityPeek --> PostDetail["Public post detail"]
  Activity --> PostDetail
  Activity --> Person["Person profile"]
  People --> Person
  Circle --> PostDetail
  Create --> CreatePost["Share public experience"]
  Create --> PrivateIntent["Request private care/add plant/add space OR public plant question"]
  PrivateIntent --> PlantSpace
  PostDetail --> Comments["Comments and replies"]
  PostDetail --> Person
  Me --> Edit["Edit profile"]
  Me --> Settings["Settings / security / account"]
  Me --> SocialGraph["Followers / Following"]
  Person --> SocialGraph
  Person --> Inbox
  Person --> Calls["Audio / video call actions"]
  Person --> Safety["Report / block / privacy gated actions"]
  Home --> Legacy["Optional inherited Stories / Pulse / Tonight / Rooms / Memories"]
  Create --> Legacy
~~~

**Navigation truth:** There are **four root tabs**: Home/My Space, Circle, Create, Me. Inbox, People, Settings, notifications, edit profile, post comments and person pages are contextual entries or secondary activities; they are not additional bottom tabs. The legacy \`NovaRoute.ProfileSetup\` exists in source but current register/login routes go to Home after success. Edit/Profile and People crash issues are **fixes coded, device recurrence not yet independently verified**.

## B. Private living-place → plant → care → recorded outcome

~~~mermaid
flowchart TB
  H["MY SPACE / Today"] --> Access{"Can load authorized spaces and plants?"}
  Access -- "Loading/offline/error" --> Retry["Honest loading/error + retry (not fake empty records)"] --> Access
  Access -- "Yes" --> CheckIns["Due check-ins across allowed spaces"]
  Access --> SelectSpace["Select owned/shared space"]
  SelectSpace --> HasPlants{"Plants in this space?"}
  HasPlants -- "No plants" --> AddPlant["Create plant in a writable space"]
  HasPlants -- "Have active plants" --> Inventory["Search, expand, open real plant"]
  HasPlants -- "Only archived plants" --> Archive["Archived plant records / read-only history"]
  AddPlant --> NewPlant["Real name; optional species, placement, uploaded photo"]
  NewPlant -- "Save succeeded" --> Inventory
  NewPlant -- "Failed" --> RetrySave["Show error; no fictional record; retry"]

  Inventory --> Open["PLANT DETAIL: real identity, place, original photo"]
  Archive --> Open
  CheckIns --> Open
  Open --> Permit{"Active + allowed to record care?"}
  Permit -- "No" --> ReadOnly["Inspect existing private record; no write"]
  Permit -- "Yes" --> Composer["Record actual care / observation / concern"]
  Composer --> Action["Care kind or 6 authored actions; optional note and real photo"]
  Action --> ReviewDate{"Choose a check-in date?"}
  ReviewDate -- "No" --> Save["Save real dated event in Django"]
  ReviewDate -- "Yes" --> OptReminder["Optional local reminder (device permissions apply)"] --> Save
  Save -- "Failure/lost response" --> OriginalRetry["Retry the same submitted entry (duplicate-safe journal)"] --> Save
  Save -- "Success" --> History["Private living history: events + original photos"]
  History --> Filter["Filter observations / care / concerns / outcomes"]
  History --> Compare{"Two real photos on different dates?"}
  Compare -- "Yes" --> Two["Two Moments photo comparison; open originals"]
  Compare -- "No" --> One["Normal one/zero-photo history; never invent a pair"]
  Open --> Reviews["Pending human check-ins"]
  Reviews --> Outcome["Observe actual result, save outcome and close"]
  Reviews --> Reschedule["Choose later date, no fictional watering"]
  Reviews --> Reminder["Enable / cancel optional reminder"]
  Outcome --> History
  Reschedule --> Reviews
  Open --> Manage["Plant details & settings: rename / edit shared context / archive / restore"]
  Manage --> History
  Open --> AskAI["Optional assistant (source coded; live provider QA open)"]
~~~

**Ownership and privacy:** a \`SpaceMember\` has owner/manager/carer/viewer roles; the server enforces who may list, add, edit, record and see private history. \`viewer\` and archived plant paths are not care-editors. A space may exist without photos; a plant may exist without photos; neither implies an imaginary installation. User-chosen *check-in* != biological watering schedule. A deleted/offline photo has a truthful loading/failure state. The care journal is isolated from public community content.

## C. Plant question vs social/community, deliberately different privacy branches

~~~mermaid
flowchart TB
  Start["Circle / Create / Plant detail"]
  Start --> Q["Ask about one real plant (write-authorized)"]
  Q --> Input["Choose specific question/title and a public photo"]
  Input --> Preview["Preview exact public text and photo"]
  Preview --> Consent{"Explicit consent to public publication?"}
  Consent -- "No / cancel" --> Edit["Return to private draft"]
  Edit --> Input
  Consent -- "Yes" --> Publish["Server-safe public Circle question"]
  Publish --> PublicFeed["Circle plant questions / public post"]
  PublicFeed --> Reply["Replies / comments from people"]
  Reply --> OwnerOutcome["Owner may later write separate private outcome"]
  OwnerOutcome --> PrivateHistory["Private plant history — never auto-shared"]

  Start --> Share["Share an experience / public post + real media"]
  Share --> Posted["Public post in community feed"]
  Posted --> Social["Like / comment / repost / author profile"]
  Social --> Visit["Visited Person profile"]
  Visit --> Follow["Follow/unfollow"]
  Visit --> Msg["Message / calls, subject to permission"]
  Visit --> Report["Report / block / privacy"]
  Start --> Other["Optional Stories (24h), Pulse, Gatherings, Memories"]
  Start --> People["Discover people / search"]
  People --> Visit
~~~

**Current nuance:** Reels as an independent infinite-feed entry was removed from Plantupia Create; the retained Nova video/story/player engine is *not* equivalent to a user-facing primary Reels tab. Me and visited Person profiles now expose one Shared Moments section rather than the inherited Posts/Reels/Reposts tab strip; code-built, not yet accepted on every device.

## D. Me / People / support and secondary navigation

~~~mermaid
flowchart LR
  Me["ME: Identity, avatar/photo, bio"] --> Private["Self-only private space/plant summary"]
  Me --> Shared["Shared Moments + counts"]
  Me --> Edit["Edit profile: identity/photo/avatar variant/bio/location/link/interests"]
  Me --> Followers["Followers / following"]
  Me --> Inbox["Inbox"]
  Me --> Settings["Settings"]
  People["PEOPLE: search, discover, filter"] --> Person["Visit Person (privacy-gated)"]
  Person --> Public["Visible shared moments; counts if permitted"]
  Person --> Follow["Follow/unfollow"]
  Person --> Message["Message; audio/video call options"]
  Person --> Safe["Block / report / share profile"]
  Inbox --> Chat["Existing individual/group messaging & media"]
  Settings --> Control["Privacy / notifications / security / support / account actions"]
~~~

The owner profile can show private spaces. A visited person's profile cannot show their private space inventory. Other inherited Nova settings/social/messaging subflows need a dedicated real-device navigation and permission sweep; this map identifies their **entry and module**, not an E2E certification of every inherited screen.

## E. Every major screen and a trace to actual code

| Area/entry | Action/path present in source | Principal source | What remains unproved |
| --- | --- | --- | --- |
| Launch/auth | Session resume → Home, Welcome, login, sign up, Terms and Privacy | \`NovaApp.kt\`, \`AppViewModel.kt\` | Real-device account recovery and host/API reachability |
| Four bottom tabs | Home, Orbit/Circle, Create, Profile/Me | \`NovaRoute.kt\`, \`NovaApp.kt\`, \`NovaBottomBar.kt\` | Final visual/accessibility QA |
| My Space | Authorized space switch, plants, first-space/first-plant forms, due check-ins | \`HomeScreen.kt\`, \`PlantupiaClientSection.kt\`, \`PlantupiaSelfServiceForms.kt\` | 0/1/7/100 plants on phone, service permission parity |
| Plant detail | Owner/manager/carer/viewer conditions; plant image, name, species, placement | \`PlantupiaClientSection.kt\`, Django \`plants\` app | Real phone permissions, slow network, genuine photos |
| Record care | Six care actions, observation/concern, note/photo, selected check-in day, optional reminder | \`PlantupiaClientSection.kt\`, \`PlantupiaCareReminderScheduler.kt\` | Actual push/local delivery and on-device save |
| Check-in/outcome | Record outcome and close, move a date, cancel/re-enable reminder | \`PlantupiaClientSection.kt\`, Django event endpoints | Linked event consistency over multiple devices |
| Living History | Event pagination/filters, photo timeline/compare, retry, zoom | \`PlantupiaHistoryTimeline.kt\`, \`PlantupiaPhotoIndex.kt\`, \`PlantupiaPhotoViewer.kt\` | Visual framing, gesture/slow media device QA |
| Plant management | Rename/personal name, shared context edit, archive/restore | \`PlantupiaClientSection.kt\`, Django roles | Actual role/device validation |
| Optional assistant | Question sheet | \`PlantupiaAssistantSheet.kt\`, \`backend/plants/assistant.py\` | Provider on staging/production NOT proven live |
| Circle | Public plant Q&A, feed, posts/comments, people and inherited activity/story views | \`OrbitScreen.kt\`, \`PlantupiaCommunityPanels.kt\` | Genuine two-user consent/reply QA |
| Create | Private: care/plant/space; public: question/post/Story; secondary Pulse/Gathering/Memory | \`CreateHubScreen.kt\`, \`PlantupiaCreateIntent.kt\` | All nested composer/editor media and Back |
| People | Search/discovery, profile visit/follow with account privacy | \`PeopleScreen.kt\`, \`PersonScreen.kt\` | Device crash reported earlier; latest fix unverified on user's device |
| Own profile | Private places + public Shared Moments, stats, Edit Profile, Inbox, Settings | \`ProfileScreen.kt\`, \`PlantupiaProfileSpaces.kt\` | Phone render and profile save regression |
| Person profile | Conditional Shared Moments + follow/messages/calls/block/report | \`PersonScreen.kt\` | Permission and person switch tests |
| Inbox/secondary | Messages/group chat, social activities; Stories/Pulse/Tonight/Rooms/Memories | \`MessagesActivity.kt\`, \`CreateHubScreen.kt\`, \`HomeScreen.kt\` | Inherited Nova product fit and full device QA |
| Error/state routes | Offline/loading/retry, read-only, expired session, unclaimed photos, private/public boundaries | Multiple modules; owner/repository/backend | Full offline/restart+authorization acceptance |

## F. Important genuine GAPS: **do not draw these as already live**

1. **CEO suggestion / future business flow:** provisioned delivered inventory → handover QR → claim/assign plants → receive verified care program: **not implemented** in Android or listed \`backend/plants/urls.py\`. See [separate TO-BE customer/service blueprint](./PLANTUPIA_CUSTOMER_JOURNEY_AND_QR_HANDOVER_SERVICE_BLUEPRINT_2026-10-20.md).
2. **Approved species-specific care instructions / watering recommendations:** no curated verified species-care corpus, plant-specific initial care instruction/approval or completed rule engine in the inspected code. Existing human check-in choices do not substitute.
3. **Plantupia staffed maintenance, client entitlements, warranty, billing, technician assignments:** not approved or shipped; Django Admin/role model is not a full commercial service lifecycle.
4. **Operational certainty:** source code + successful CI in earlier versions != verified network environment, two-account data permissions or physical-device acceptance of the latest screens. Source-coded crash mitigations for Edit Profile/Discover People were previously made; real device recurrence is still unverified.
5. **Photos/illustrations:** asset source integration exists but visual art-direction approval is a separate step.

## G. CEO readout — one sentence

> **Today the real Android app goes from account → private space → real plant → self-reported care and human-chosen follow-up → recorded history/outcome, with optional consented Circle sharing, people, messages and profile. The purchase/delivery/QR activation and source-approved per-species care instructions are important NEW bridges—not features we should pretend already exist.**

### Source paths checked in this flow

- \`app/src/main/java/com/nova/app/NovaApp.kt\` — route switch and real callback wiring.
- \`app/src/main/java/com/nova/app/navigation/NovaRoute.kt\` — route inventory.
- \`app/src/main/java/com/nova/app/feature/home/HomeScreen.kt\`
- \`app/src/main/java/com/nova/app/feature/create/CreateHubScreen.kt\`
- \`app/src/main/java/com/nova/app/feature/orbit/OrbitScreen.kt\`
- \`app/src/main/java/com/nova/app/feature/plants/PlantupiaClientSection.kt\`
- \`app/src/main/java/com/nova/app/feature/plants/PlantupiaCreateIntent.kt\`
- \`app/src/main/java/com/nova/app/feature/plants/PlantupiaCommunityPanels.kt\`
- \`app/src/main/java/com/nova/app/feature/profile/ProfileScreen.kt\`
- \`app/src/main/java/com/nova/app/feature/people/PersonScreen.kt\`
- \`backend/plants/models.py\`, \`backend/plants/urls.py\`
- \`docs/PLANTUPIA_PRODUCT_BIBLE_AND_HANDOFF_V01_2026-10-17.md\` and \`docs/PLANTUPIA_SCREEN_CLOSURE_LEDGER.md\`.

**Document status:** AS-IS source-traced, not a client-approved UX audit, not evidence of a real customer testing it.
