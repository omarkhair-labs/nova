# Plantupia — whole-product finish line
**18 October 2026 · Working roadmap, not a design decree · Android Native / Compose + Django**

This document is an execution ledger for the *complete integrated app*, not a license to declare one choice permanent, substitute HTML for native QA, or mark anything owner-accepted. Source-of-truth for individual coded screens remains [screen closure ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md). The owner's actual device feedback supersedes speculative design assertions. Record alternatives, reasons and consequences.

## Scope and definition of done
A person can create an account; create/select an authorized real space and plant; record an actual action and photograph without duplicate or lost saves; see a dated result; opt into a public question or a personal public post; open another person's truly public identity; message them; navigate media without misleading content; edit their profile; manage privacy and account lifecycle. All on the same authenticated QA backend, tested with two separate accounts on Android. Empty, error, offline, long, photo/no-photo and larger-font states are first-class. CI green is a technical gate, not physical or editorial acceptance.

## Priority lanes (iterate continuously, not sequential design silos)

| Lane | Concrete closure scope | Proof / gate |
| --- | --- | --- |
| **P0 — hosted contract + data integrity** | Prove isolated QA host revision, actual response containing `can_add_plant`, approved Postgres/Redis/media boundaries, Avatar `accounts.0037` and submission `plants.0004`, and `event-submissions` / `question-submissions`. Preserve on-device pending journal; never clear app data to reset. Test replay and duplicate avoidance with two identities. | Reproducible authenticated HTTP evidence and one real phone create -> save -> retry -> history journey. No production migrations or deploys without separate authorization. |
| **P1 — meaning / IA / navigation** | Review every feature as Primary, Contextual, Optional, or Outside current app. Four primary destinations currently My Space / Circle / Create / Me. Confirm what Create should offer, and when Search / Inbox / Messages / Media appear. | Each section has a job, one sensible first action and unambiguous public/private behavior. |
| **P1 — one art / UX system** | Editorial typography, natural but not gender-coded colors, use of photographs, botanical + architectural motifs, original illustrations, motion and haptics, contrast / icon geometry / states. Review original avatar variants *as artwork*, not just successful Compose drawing. | Side-by-side device captures on 320/360/390/430dp, standard and 2x text. Cohesive app, not repeated green cards. No manufactured visual evidence or unlicensed assets. |
| **P1 — human identity** | Edit Profile, Me, visited Person, avatar chooser, photo override, badges, optional public metadata, People, Search, Posts, Comments, Story, Reel, Inbox, DM, Notifications. Hide private places from other users. | Two accounts choose different characters; same identity everywhere across logout/restart; same profile presentation from self/edit/visitor; no full-height Nova profile-theme interface. |
| **P1 — living place / actual care** | Home, space selector, large/zero inventory, add/edit space, add/edit/retire plant, care and issue, observations, private journal/photo provenance, follow-up dates, history and image comparison. | One clear action above the fold; real successful write/read and role/access QA, no duplicated moments, verified failure/retry states. |
| **P1 — Circle and media** | Questions with human author/replies/outcomes and separate public consent; Posts/Comments/Create; Story; Reels/video; real uploads and reporting. Decide content role and discovery before altering navigation. | A public contribution is created intentionally and isolated from private records; Media does not promise verified plant facts. |
| **P1 — communications / account** | Inbox, text/group DM, push, optionally call context, notifications, auth/restore, settings, privacy, deletion/report/block, English-first copy. | Real two-account delivery + blocked/privacy scenarios; permissions and account deletion checked. |
| **P2 — polish/release** | Onboarding and authored zero states, reduced motion, accessibility, 100+ data, media performance, QA-signing continuity, AAB/Play brand/signing/package decision. | Final integrated APK + exact-head Android/Django CI + real-phone walkthrough + explicit owner review and release consent. |

## Product choices to examine rather than blindly preserve or delete

| Capability | Working recommendation | Why / evidence needed | Current safety boundary |
| --- | --- | --- | --- |
| Reels | **Provisional: contextual, not the core infinite feed.** Consider short living stories, processes, place diaries, transformations with truthful dates/context, or an optional media shelf. | Test whether actual public videos help care, culture, discovery or learning. Compare retention/creation effort and navigation. | Keep Nova upload/player/data while reviewing UI. Don't relabel generic videos as verified transformations. |
| Stories | Optional short-lived human/place moments; linked to actual public content only when user explicitly selects it. | Separate spontaneous social expression from private observation log. | Existing Nova 24h pipeline remains; don't claim plant->Story linking is implemented. |
| Calls | **Contextual** in people/contact surface; do not make Audio+Video permanent primary buttons for strangers. Could ultimately become opt-in support after a DM or within a real expert session. | Test perceived privacy, consent, actual demand and notification reliability. | Keep existing engine/routes. This sprint collapses the buttons under a deliberate affordance; no backend permission rules changed. |
| Me / visited Profile | The person first; honest public contributions and optional connection; *self-only* private places. | Current owner screen duplicates private-space/media/social blocks; visitor still feels like a Nova social hub. | Do not show site membership/photos in visitor profile or infer public installations. |
| Edit Profile | Show the actual public identity preview; configure avatar/photo, name/handle/bio, optional location/link/interests, privacy where contract is real. | Old Nova theme colors and `showOrbit` editing controls are not a Plantupia product identity. | Preserve legacy theme/visibility values on save pending a deliberate schema decision. |
| Circle | Real questions, contextual answers, evidence, optional human relationships; meaningful author identity. | Reduce explanation cards; improve before-publish summary and reply journey. | Never expose private plant journals automatically. |
| Illustrated characters | Distinct visual personalities expressed by geometry, clothing, posture, palette, references to places/nature; no assumed sex or demographic identity. | Study neutral visual systems, styles, cultural breadth and legibility at 24/40/64px. | Keep original stable ID and photo priority; do not imply AI portrait is a real person. |
| Business/IoT/AI | Test as separately bounded domain flows with real operations/data; not imaginary badges, fabricated sensors or diagnosis. | The company scenario remains important research; needs roles, services, sensors and approved evidence. | Do not present future capability as live or make business staff flows public by default. |

## Art and external reference passes
Research precedes and follows each meaningful implementation. Use first-hand reference screens where possible; identify *behavior*, *composition*, *content hierarchy*, *failure modes*, and *what cannot be copied*. Reference shortlist:
- [Apple 2026 Design Awards](https://developer.apple.com/design/awards/) — Moonlitt: specific relationship between time and visual explanation, not a UI skin.
- [Apple 2025 Design Awards](https://developer.apple.com/design/awards/2025/) — CapWords: camera/action feedback; iA Writer: reduced UI noise; Vocabulary: consistent illustration rhythm.
- [Are.na](https://www.are.na/) — people, collections and intentional relationships instead of engagement-driven metrics.
- [Android accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility) — 48dp targets, contrast, font scaling, TalkBack.
- [Android adaptive navigation](https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation) — screen sizes, not literal imitation of iOS.
- The project's Source Atlas / Pass04–Pass09 / Product Bible are *inputs to challenge*, not rules frozen by a previous assistant.

Visual briefs needed: (A) person identity across 10+ placements; (B) photographic real plant/place evidence, date and attribution; (C) empty, interrupted and returned journey illustrations; (D) one visual language for public moments, Circle and media; (E) active/passive/disabled/success state and motion. Mood must allow different genders, styles and personalities, not a soft feminine plant boutique.

## Acceptance matrix, no premature DONE labels
- **Coded** = source present, behavior tests may exist.
- **CI passed** = exact SHA green Android/backend gates.
- **QA verified** = authenticated isolated server + actual device, offline/restart, second account and correct permissions.
- **Visual reviewed** = integrated screen captures, variants, references compared; owner sees whole app.
- **Accepted** = the owner explicitly approves the final cohesive behavior and look.

**Journey A:** first launch -> signup -> empty living place -> create site -> add real plant/photo -> first record -> see history.

**Journey B:** owner offline/timeout/lost acknowledgement -> retry same receipt (no data loss/duplicate) -> role restrictions -> account switch -> return.

**Journey C:** care -> voluntary public question -> visitor sees public author -> reply -> outcome; private journal not exposed.

**Journey D:** no-photo avatar -> Edit Profile -> Me -> People -> visitor Person -> Post/Comment/Story/Reel -> Inbox; identity continuity and privacy.

**Journey E:** DM -> optional call -> decline/blocked state -> notification -> return. No fabricated calls/messages.

**Journey F:** 0/1/7/100+ plants, small display, large fonts, TalkBack, long text, missing/misleading photo, no network, Android Back and process restart.

### First 2026-10-18 implemented slice (pending exact-HEAD CI and physical QA)
- `EditProfileScreen.kt`: public preview/field hierarchy and gender-neutral illustration copy; remove orphaned legacy theme/orbit editors while preserving their persisted payload fields on save.
- `PersonScreen.kt`: closer Plantupia surface colors; audio/video controls are discoverable on intent, not priority actions. Call engine remains intact.
- **Not yet done:** a fully redesigned Me/Person media composition; role-based contact settings; new avatar artwork; all app-wide motion/graphic work; server/QA confirmation; media product decisions and final acceptance.

## Change/decision discipline
For every uncertain choice record **observation -> reference/brief -> alternatives -> decision for this increment -> native implementation -> measured QA -> revise**. Do not make previous Markdown, a single award/reference, or assistant preference a permanent rule. Changes affecting production, user data, new package/signing/Play or deleting existing media require explicit separate approval.
