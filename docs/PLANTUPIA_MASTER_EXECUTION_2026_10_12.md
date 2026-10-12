# Plantupia — Full Product & Native Execution Brief

**Prepared:** 2026-10-12
**Branch:** `experiment/plantupia-care-create-native-20261012` based on the unchanged draft Plantupia PR #240 (`bf26a05`).
**Status:** Native source committed; **isolated GitHub Actions run 38195855956 SUCCESS** on commit `1844651d51f59f72923da637142763cee8a42a45` (Backend tests + Android unit tests + lint + QA APK + release/App Bundle). **Real-device QA, production/preview deployment isolation and live AI activation still NOT verified.**
**Truth:** No production, Play or main branch deployment is authorized. *Do not merge or preview-deploy until backend environment/database/media isolation has been verified.*

## 1. Why this approach exists

The user chose **Nova as fertile ground**: native Kotlin + Jetpack Compose experience, icons, transitions, media production, posts, feeds, Stories, Reels, messages, notifications, permissions, real server, auth and delivery infrastructure. It was never merely a convenient set of backend dependencies. An HTML rendering of Plantupia must therefore function as *art-direction and interaction research*, not as a substitute frontend or literal Compose copy.

Plantupia's research instead determines *which* native engines are useful, *where* and *with what privacy*: **living space ↔ plant individual ↔ human observation/action ↔ revisited outcome**, alongside voluntary social expression and person-to-person messages. Company operations must be grounded in real company processes.

**Absolute company fact:** Plantupia has **no sensors in its plants**; no telemetry, assumed moisture measurements, hardware onboarding or smart-device UI belongs in this product.

Source working canon:
- [Plantupia working law](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/WORKING_LAW.md)
- [Product translation](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/synthesis/02-product-translation.md)
- [Cross-world critique](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/exploration/05-cross-world-critique.md)
- [Master product spec](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/MASTER_PRODUCT_SPEC_V01_2026-10-12.md)
- [Information architecture](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/INFORMATION_ARCHITECTURE_V01_2026-10-12.md)
- [Full-product Review 03](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/FULL_PRODUCT_VISUAL_REVIEW_03_2026-10-12.md) — user reviewed, **not formally frozen**.

## 2. Benchmark matrix (behavior, not copying visual assets)

| Source | What is externally demonstrable | What we adopt/avoid in Plantupia |
|---|---|---|
| [Greg Support](https://greg.app/support/) | personalized watering recommendation, reminder, caregiver-marked watering; question/community | **Adopt** lightweight task visibility; **avoid** claiming a generic due date indicates actual moisture |
| [Greg users/answers](https://greg.app/question/1o4tzx/greggs-recommending-to-water-my-plants-way-to-often-i-keep-having-the-same-problem-with-the-watering) | users can challenge/snooze inaccurate watering predictions; conditions remain dynamic | **Adopt** action log and voluntary check-in; never autoprescribe water from timer alone |
| [PictureThis app listing](https://apps.apple.com/eg/app/picturethis-plant-identifier/id1252497129) | photo identification and disease-assistance marketing, external claims | **Consider** optical ID as later optional capability; **avoid** claiming disease certainty or an unlicensed API integration |
| [FYTA documentation](https://fyta.de/en/pages/app-download) | profiles and care guidance usable **without** device; advanced device telemetry in their own hardware ecosystem | **Adopt** no-hardware onboarding and credible guides; **exclude** hardware telemetry because Plantupia has no sensors |
| [Pl@ntNet licensing](https://my.plantnet.org/terms_of_use) | paid commercial terms apply past limits; photo-based species suggestions and possible attribution | **Gate** image species ID behind verified license, API key, confidence UX, company costs |
| [Material 3 Navigation](https://developer.android.com/develop/ui/compose/components/navigation-bar) | 3–5 destinations suitable for compact navigation | **Implement testable five-intent** Home/Care/Create/Circle/Self native navigation |
| [Material 3 bottom sheet](https://developer.android.com/develop/ui/compose/components/bottom-sheets) | native sheet patterns with dismiss, safe insets | **Implement** accessible touch-first Create sheet; no permanently huge green banner |
| [OpenAI GPT-5.4 Mini](https://developers.openai.com/api/docs/models/gpt-5.4-mini) | hosted text/vision model billed per-token | **Optional** server-side text-first AI; no embedded key; no training/personal file upload needed |

**Interpretation, not a proven market claim:** Plantupia's differentiator could be **continuity of one real living plant in its real place**, not another feed or a copy of a competitor's green interface. Test this with real owners and authorized company client/maintenance staff.

## 3. Design brief: unify the complete product

- **Art direction:** editorial/spatial identity and real human/plant photography; natural hierarchy instead of wall-to-wall explanations and leaf clip-art. Deliberate visual difference between *private retained history* and *public ephemeral expression*.
- **Native components:** reuse Nova image pickers, galleries, icon set, story composer/viewer, inbox, feeds, follow state, notifications, error/loading components, session handling and Compose motion.
- **Primary nav decision in this experimental branch:** five recognisable destinations `مساحتي | ♡ الرعاية | + | الدائرة | أنا`. The (+) opens a real Compose **ModalBottomSheet**; one tap reveals private care, add plant, public question, AI, Story and old CreateHub. No compulsory swipe-only discovery. This is **an implementation candidate**, not a silently approved global design decision.
- **Privacy:** private events do not become posts/Stories and AI chat does not become a message to another person.
- **No fabricated content:** company photos, actual site/office details, staff visits, sensor metrics, plant-health scores and guaranteed AI diagnoses must have real sources or remain absent.
- **Trust:** no AI-generated database writes; follow-up dates mean *review observations*, not *water now*.

## 4. Implementation ledger: code **written**, NOT equivalent to finished/verified

| Area | Current branch change | What must be QA-tested |
|---|---|---|
| Native navigation | Care root added to typed Nova Nav3 routes and primary bottom bar | all original roots/Back behavior, focus, 360dp, badges, story full-screen |
| Care screen | `PlantupiaCareHub.kt` calls authenticated real plant list, renders due and upcoming human-selected follow-ups, photo cards, observation/history shortcuts | 0/1/7/100 plants, loading/error, wrong user, stale data, return/deep navigation |
| Quick Create | real `ModalBottomSheet` with separated private/public options, existing CreateHub retained, direct Story entry | screen reader, IME, swipe/back, story/public media, panel size, RTL |
| Add plant | Quick Create intent opens existing `PlantupiaSelfServiceForms` with real Django creation | empty-space create-first flow, photo permission/upload/error |
| Care/plant deep link | open a **selected** plant by ID, not random first item, via existing `PlantupiaClientSection` | different spaces/multiple plants, unknown/stale ID; scroll/Back |
| AI Backend | `POST /api/v1/plantupia/assistant/ask/`, authenticated, rate-limited, opt-in context, no training, no key in APK | with/without key, provider 503/502, cross-account UUID, quota, cost and prompt quality |
| AI Native | Compose temporary chat, context checkbox, server requests, errors and no fake answers | actual preview backend and API key, Arabic/English, network, contextual return |
| Community / Stories / Inbox / Profile | all original Nova engines preserved; only entry points reused | real messages, moderation, media upload, Story playback, notification deep links |
| Backend models | existing Spaces, PlantUnit, PlantEvent and discussions remain; **no new database migration in this branch** | isolated preview database/media checks, migration baseline |
| Website / B2B system / iOS | no changes; separate agreed deliverables and company gates | cannot label complete due to app UX work |

## 5. System contracts

### Real care
- A `PlantEvent` exists only for an authenticated user action on an authorized plant.
- A follow-up is a future user-chosen check-in, **not** a fixed soil moisture prediction.
- Care opens exact plant ID, preserves history and permissions, uses backend's real `next_follow_up_on`.

### Quick Create
- Private care → request private observation → select plant → native private record.
- Add → real Space/Plant creation; no scan, QR or sensor gate.
- Public plant question → explicit choice/permission → existing Circle engine.
- Story → existing Nova Story interface, 24h by engine; should never automatically ingest a private plant record.
- More → original rich full CreateHub with video/reels/media/social formats.

### AI
- New environment setting: `PLANTUPIA_AI_API_KEY` **server-side only**; `PLANTUPIA_AI_MODEL` default `gpt-5.4-mini`; `PLANTUPIA_AI_BURST_LIMIT` default `8/min`; `PLANTUPIA_AI_DAILY_LIMIT` default `50/day`.
- When not configured, return **503 explicitly**, don't replace with faked intelligence.
- Auth and role scope applied to plant before any context access; context includes only opted-in name, species user label, placement description, last 5 human notes (not photos, account IDs or full locations).
- No provider conversation storage (`store:false`); ephemeral app UI session only, no persisted chats in current scope.
- Agent is text-only; no photo vision or plant species API yet. Neither veterinary/medical diagnoses nor company service contracts are asserted.

## 6. Not done / critical acceptance gate

**NOT CLOSED** until all of:
1. Kotlin Gradle `testQaUnitTest` (where configured), lint and `assembleQa` pass.
2. Django `manage.py check`, `makemigrations --check` and complete backend unit tests pass (in isolated CI).
3. Show real screenshots from compiled native APK on 360/390/430dp; review comparison to Nova originals.
4. QA with two real test accounts, photos, space authorization, care followup outcome, Stories/video, Inbox, notification routing and return.
5. Verify Coolify PR preview **database + media + env** separation from real Nova production **before** any preview deployment. Existing backend Dockerfile automatically applies migrations.
6. Configure AI only in protected isolated test env, cap paid usage, verify responses; never paste keys in screenshots or repo.
7. Company operational permission/brand/assets and privacy review before live customer feature enablement.
8. Confirm Android app/store identity migration plan from `com.omarkhair70.nova`; no Play upload without explicit user action.

### No false completion claim
Writing a branch is not delivering a verified APK. The user asked for the entire rich experience; this branch implements a coherent **native Care + Create + AI integration step** on top of existing Nova engines. Visual art direction, actual full app E2E testing, licensed plant identification/guide, company-specific customer workflows, production-readiness and live AI activation remain **incomplete**. Treat them as genuine gates, not invisible TODOs.

## 7. Next safe execution

- Review/compile branch in isolated CI **only after preventing an automatic Coolify Preview deployment from accidentally inheriting production DB**. Do not open a PR blindly if PR webhooks auto-deploy.
- Fix compilation/tests, generate signed/unsigned QA install artifact separate from original Nova; run device QA with actual image handling.
- Audit screen-by-screen experience and adjust all connected native components, not only navigation.
- Source company-approved service, brand/photography facts before B2B and company-specific AI answers; design website as a separate workstream.

**Status snapshot**: Source committed; no production or release action; CI and APK test outstanding at time of writing.


## 8. Automated verification performed 2026-10-12

- **CI run:** https://github.com/omarkhair-labs/nova/actions/runs/38195855956
- **Backend job:** SUCCESS (Django checks, no pending migrations, all backend tests including AI privacy and forbidden user context).
- **Android job:** SUCCESS (design and network architecture checks, Kotlin unit tests, release lint, debug and separate QA APK, instrumentation APK assembly, release APK and bundle assembly).
- **QA artifact:** GitHub artifact `plantupia-qa-pr240-apk`, id `11723278215`, size of raw `app-qa.apk` 69,837,276 bytes (QA application id remains separate from original Nova).
- **Artifact integrity checked locally:** ZIP archive validated; `AndroidManifest.xml` and 21 dex files present; SHA256 `d7b1eb9daa6078c48452494c35bd9340b492dc0d5b9372078d0e4f17784aad13`.
- **Not tested:** real Android install, authentication against real isolated preview, functional Stories/Inbox and data persistence on-device, actual AI inference (the existing PR #240 backend preview does not contain the new AI endpoint or its API key), approved visual/brand review.
- **Deployment safety:** no new preview, no database migration and no Play deployment performed. Before test-enabling AI in Coolify, verify preview DB/media/environment isolation.

A successful CI **does not** close the full product or authorize release. Product owner still needs to review integrated native feel against the HTML design study and original Nova.
