# Plantupia Android — 2026-10-16 company delivery & separate Play-app triage

**Goal:** Deliver a real installable, independently testable Plantupia Android build to the paying client within several days if the required external dependencies are available. Distinguish *client-ready private test* from *public Play release*. **Do not publish anything, merge PRs, change Nova's application ID/signing, or deploy Oracle production under this document.**

## 0. Exact present-day baseline (checked 2026-10-16)

- Production-preserving original project: `omarkhair-labs/nova`; `PR #240` (base `master`, branch `feature/plantupia-product-conversion-20261010`), HEAD `bf26a052c4dfe197e87bcb23a7cb1166fa659d69`, Draft, 89 changed files, 91 commits.
- Latest integrated Native work: `PR #241`, stacked upon #240, branch `feature/plantupia-native-experience-20261012`, baseline HEAD `d73edf2de966be376fea4faa56def9e43b7608b6`, Draft, 65 changed files, 112 commits.
- [Android build and Django tests on exact pre-triage head: SUCCESS](https://github.com/omarkhair-labs/nova/actions/runs/38647785290). Builds debug / QA APK / release APK and bundle; **success does not show live network, device, third-party provider, or signing works**.
- CI artifact [`plantupia-qa-pr240-apk`](https://github.com/omarkhair-labs/nova/actions/runs/38647785290) exists, approx. 44 MB, **expires 2026-10-21 22:37 UTC**. Artifact title is historical and incorrectly suggests PR #240 only even though it is built at #241 SHA. Download from GitHub Actions while valid. It is a side-by-side **debug-type QA** install, not the final separately signed Play AAB.
- `app/build.gradle.kts`: default `applicationId="com.omarkhair70.nova"`, version `2.1.13 (20113)`, `targetSdk=36`, `qa` build type with `.qa` suffix.
- `NovaServerConfig.kt`: QA host `240.zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io`; default builds use existing Nova production host. **DO NOT install/use a production-pointing build against real customer accounts during QA.** A previous [signoff audit](./PLANTUPIA_V1_SIGNOFF.md) recorded HTML 404 from a Plantupia endpoint on a device. External HTTP GET to QA host was not verifiable from this conversation's network; this is an **unverified prerequisite**, not a declaration that it is down.
- `backend/plants`: migrations, permissions, real authenticated API, upload/photo sanitization, events, follow-ups, saved outcomes, public questions and server-side AI adapters are implemented, with Django tests. But no evidence of a healthy *deployed isolated* Postgres/Redis/media stack with #241 routes and two real account journeys.
- `PLANTUPIA_AI_ENABLED` is disabled unless configured server-side; [AI setup](./PLANTUPIA_FREE_AI_SETUP.md) prescribes Groq Free Plan, per-request consent, Redis throttling, QA-only secret. Test mocks do **not** prove a live AI answer.
- CI QA Firebase configuration is a **placeholder** generated during build for `com.omarkhair70.nova.qa`; FCM/push and new-app credentials have not been proven.
- Last research directions [Pass 07](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/09-pass07-integrated-product-synthesis-contract-2026-10-14.md), original interactive Pass 05/07 artifacts and Study 03 A/B are **comparative prototypes**, not approved final native screens. Core priorities are Place, actual Care, durable Field Notes/history, optional Nova human help.

## 1. New product-distribution decision requiring implementation before *any separate Play entry*

**Newly explicit user intent (2026-10-16): Plantupia is to be a SECOND independent Google Play app, not a rename/update of the already existing Nova listing.**

This supersedes only the previous same-package *publishing assumption* for this delivery. It does **not** grant permission to change `master`, the installed Nova package `com.omarkhair70.nova`, existing signing, Oracle production, or private company ownership.

A previous, **bootstrap-only** [`omarkhair70-droid/plantupia-product`](https://github.com/omarkhair70-droid/plantupia-product) repository uses `com.plantupia.app`, but its README explicitly states it is not rebranded, production-tested or on Play. **Do not revert to this old bootstrap and throw away #240/#241**. Proposed safe technical route after owner/Play-account confirmation: cut a release-preparation branch **from fully integrated #241** and assign an approved separate, permanently chosen Plantupia application ID; configure matching brand/app label/manifest/deep links, Firebase Android client/FCM, separate server origin and new-app upload signing while retaining the protected Nova source and app ID. Produce an **Android App Bundle (.aab)** for Play; QA APK alone is not a publishable new-app release.

The owner must establish whether publishing from their Play Console account or the company's, and approve package ID ownership and release signing. Prior identity `com.plantupia.app` is a *candidate*, not evidence of company approval or reserved listing.

**Google policy as verified 2026-10-16:**
- [Internal testing](https://support.google.com/googleplay/android-developer/answer/9845334) distributes through Play to up to 100 designated testers, suitable for client demonstration and feedback. Availability may take several hours.
- [Internal app sharing](https://support.google.com/googleplay/android-developer/answer/9844679) can share uploaded test bundles/APKs as separate quick links; it is **not** an internal-testing track release and doesn't satisfy closed-testing requirements.
- [New personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465) created after 2023-11-13 require 12 opted-in testers for **14 consecutive days** of closed testing before applying for **Production** access. Do not promise public Play visibility within a few days if that account restriction applies.
- [Google Play target API](https://developer.android.com/google/play/requirements/target-sdk): as of 2026-08-31 API 36 required for Android mobile new app/updates. **Already met in Gradle**, barring an app-specific Play Console warning.
- [Release/upload guidance](https://support.google.com/googleplay/android-developer/answer/9859348) and [update identity/signing rules](https://support.google.com/googleplay/android-developer/answer/9859350): app listing, package, release AAB, appropriate Play App Signing/upload key, unique version code and valid approvals must be coherent. Store review timing is not controlled by GitHub.

## 2. Release blockers (P0 — this delivery, NOT wishlist)
| ID | Blocker | Exact acceptance test / needed evidence | Owner |
| --- | --- | --- | --- |
| P0-01 | **Isolated real QA backend** | Deploy/test #241 migrations on disposable/staging PostgreSQL; authenticated spaces/plant API returns JSON, unauthorized returns 401/403 not HTML 404; health endpoint works; media object stored/retrievable under correct account; Redis only on QA. Confirm no production auto-deploy/DB attachment before deployment. | Engineering + host owner |
| P0-02 | **Separate application identity** | Approved `applicationId`, account owner, distinct Firebase project/client config and signing; successful install **alongside** Nova on one device; separate AAB builds; no route or signing regression. | User/Play owner + engineering |
| P0-03 | **Critical real-user paths on two accounts** | New owner: sign in → create Space → add actual plant/photo → record observation/care → choose date → reschedule → actual outcome → history/restart. Viewer/carer/manager/stranger isolation; create question with explicit public photo preview → another user comments → private outcome; no shared private site/media. Check 0/1/7/100 inventories. | Engineering / real QA |
| P0-04 | **Social/media/auth/device smoke** | Native Story view/upload, Reels playback, Create post, Inbox/DM/back, session expiry/login recovery, camera/photo/video permissions, offline retry, background, rotation, notification permission; no silent fake success. | Engineering / device tester |
| P0-05 | **AI honestly demonstrated or clearly gated** | Either configure real QA-only provider (credential from approved account, no code secret, opt-in, quota, failure tests) and prove actual answers, **or explicitly disable/label AI unavailable in delivered review scope**. Do not imply working AI if disabled. | User authorizes provider + engineering |
| P0-06 | **Company handoff / Play track** | One cohesive QA build, dated pass/fail list, actual current feature set and known exclusions, install/demo instructions, preview/production privacy policy URLs as applicable, verified support/account deletion/permissions metadata; separately signed AAB and tester access if Play path is authorized and ready. | Engineering + Play owner |
| P0-07 | **Exact-head build proof** | After each final code increment, exact-HEAD Android unit/lint/QA and release build, Backend tests/migrations pass; install and film critical paths. PR stays Draft until explicit signoff. | Engineering |

### Practical P1 — only after P0 works
Native Pass 07-informed visual review: first-viewport actual attention + photographed space (only when real photo exists), thumb-accessible care, short factual type, photo time history, Story/Inbox from Nova in coherent design language; 360/390/430 dp, TalkBack, mixed RTL authored content, real approved mark, app icon, fonts/permissions. Close defects that would embarrass company demo; defer unsolicited large-scale redesign.

### C1 / company B1 / separate website — NOT hidden blockers to a truthful company pilot
Unapproved staff/invite UI, space archiving/zones, guessed botanical reference corpus, AI vision/species diagnosis, IoT, automatic watering, operational company SLAs and public creative website are **not shipped** in a timeboxed Android preview. Track individually as implemented / expressly deferred / company-gated. Never fake functionality or data to fill a screen. Official company scope determines whether any are contractual blocking deliverables *before representing the work as fully finished*.

## 3. Short, dependency-driven delivery sprint (tentative, NOT a promise)
- **2026-10-16 / Day 1:** Lock *separate-app* Play account/package ownership, inspect QA host/DB isolation, prove one API call from two different accounts; freeze existing research/coded features and rank P0 failures. Keep existing user research and screens; no next unlimited Pass.
- **Day 2:** Repair staging-auth/media, complete highest-risk user journeys and permission QA, activate safe real AI only if QA key and privacy approval exist. Run on physical Android for functional failures.
- **Day 3:** Close crash/UX/regression on Home/Plant/Care/Create/Circle/Profile/Inbox/Stories and 0/1/7/100 states; run accessibility/English and genuine photo audits, same-sha CI.
- **Day 4:** Build one full integrated independent Plantupia APK and approved upload-signed AAB; internal share/Test track as account allows; send company install/Play test link, short video and pass/fail evidence. If a P0 is unresolved, disclose it and provide an **honestly scoped prototype**, not call it launch-ready.
- **Public Production:** separate decision/time horizon, subject to Play account rules, review, commercial/IP permission and full policy/business approval.

## 4. No uncontrolled deployment / signoff boundaries
- GitHub CI artifact exists but the public repo/issue does not contain APK bytes or credentials; only an authorized user can use the GitHub download controls.
- Never upload an AAB signed with the original Nova credentials to a new, unapproved listing; never overwrite Nova's package or storage. New separate package demands intentional identity/certificate/Firebase work.
- Do not configure company secrets or expose private company PDF, customer photos or internal agreements in this public repo.
- A separate app *still* needs an isolated API account/customer-data privacy contract before the company uses it; separate Android package alone does not isolate servers.
- **State must be: CODED / CI PASSED / STAGING TESTED / DEVICE TESTED / CLIENT REVIEWED / PLAY TRACK AVAILABLE / PRODUCTION APPROVED**, no shortcut.
