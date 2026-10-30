# Plantupia context index — research → product → implementation

**Status:** working cross-project handoff, 2026-10-11. This document is **not** a complete transcript of private planning, meetings, uploads or chats. It provides a safe, traceable starting point for anyone working in this **public** repository.

## 1. The source-of-truth hierarchy

| Source | Where | What it proves | What it does not prove |
| --- | --- | --- | --- |
| Original Nova code | this repository, baseline `master` | existing Kotlin/Compose Android app, Django/PostgreSQL, auth, posts, stories, realtime media, chats and CI | that every reused feature is production-ready for the Plantupia UX |
| Plantupia research | [research repository, active discovery branch](https://github.com/omarkhair70-droid/Plantupia/tree/research/foundation-source-atlas-20261007/docs) | source atlases, 7 meanings, stress tests, 4 alternative worlds, critique, project-lifecycle hypotheses | confirmed on-the-ground company workflows or all later product decisions |
| Conversion contract | [`docs/PLANTUPIA_PRODUCT_CONVERSION.md`](./PLANTUPIA_PRODUCT_CONVERSION.md) | selected implementation direction, provisional brand tokens, technical constraints, remaining release gates | exhaustive private meetings, definitive design or approval of current screens |
| PR #240 | [Plantupia conversion PR](https://github.com/omarkhair-labs/nova/pull/240) | actual file changes and CI history | proof that the app is already deployed or that visual/user acceptance is complete |
| Private company materials | supplied outside public repo | brand logo assets, client-space details, content approval and commercial terms when separately verified | content another contributor can automatically access by reading GitHub |

When sources conflict, do not silently overwrite history. A *newer confirmed product decision* can supersede a *2026-10-07 research-stage assumption*; keep provenance.

## 2. What the original research actually says

Read the original materials, not only this condensed index:

- [Working law](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/WORKING_LAW.md): `Reality → Market → Source research → Reference decomposition → Meaning extraction → Product translation → Exploration → Prototype evidence → Critique/QA → Recheck → Thesis → Production`. The concept chooses the technology.
- [Source Atlas: Plant × Space](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/research/02-source-atlas-plant-space.md)
- [Source Atlas: Human × Space × Greenery](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/research/03-source-atlas-human-space.md)
- [Source Atlas: Human × Plant](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/research/04-source-atlas-human-plant.md)
- [Source Atlas: Living Systems](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/research/05-source-atlas-living-systems.md)
- [Meaning extraction](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/synthesis/01-meaning-extraction.md)
- [Digital/product translation](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/synthesis/02-product-translation.md)
- [Three-scenario stress test](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/synthesis/03-core-stress-test.md)
- [Cross-world critique](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/exploration/05-cross-world-critique.md)
- [Commercial project lifecycle, explicitly hypothetical](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/exploration/06-project-lifecycle-map-v0.md)
- [Master continuation](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/continuation/PLANTUPIA_MASTER_CONTINUATION_2026-10-07.md)

**Seven provisional meanings (not immutable rules):**
`Relational Life` / `Living Time` / `Retained History` / `Care Creates Relationship` / `Living Agency` / `Legibility` / `Accountable Living`.

**Working laws:** context before advice; trajectory before snapshot; memory before intelligence; automate burden but protect human involvement; represent living uncertainty; show meaning instead of vanity measurements; ground claims in evidence.

Four research worlds were **alternatives**, not four committed apps: Living Operations; Plant Literacy; Living Object; Living Materials/Specifier Infrastructure. The cross-world critique favors preserving meaningful design/installation/care history over building a generic plant field-service/CRM suite. Living Layer — continuity between design intent, installation, changes and care — remains a working hypothesis, not a validated platform requirement.

Three stress-test scenarios from research: a Plantupia-managed Cairo office; a household with seven plants outside Egypt; an interactive living object. These are thought experiments, **not verified live customers**.

Reference families: interiorscaping and green operations (e.g. Naava / One Big Green), consumer plant care and tracking (e.g. Planta / FYTA / Greg), spatial/biophilic architecture, furniture-as-greenery, field workflows, calm technology, biological/behavioral research. Check **current original** references before deriving UI: we use relationships/principles, not copying competitor screen styles.

## 3. Product direction selected after the research stage

These are **current working product decisions**, not claims about what 2026-10-07 research had already approved:

- **Public creative website:** Plantupia's spaces, commercial installation work, architectural intentions, real photography, case narratives and a credible contact/proposal journey. It must be art-directed rather than a generic eco SaaS template. A space-photo visualizer remains a **proposed experiment**; assess realistic inventory and business value before building AR/3D. The website is a separate delivery surface and is **not implemented by this Android PR**.
- **Android client app:** first user is a buyer/client with Plantupia-installed or Plantupia-managed plants and spaces. This user sees their physical `Space`, individual or group `PlantUnit`, personally assigned nickname, real images, observations, acts of care, chronological history and meaningful changes over time. A B2B manager of 100 plants requires grouping; a home owner may have fewer; do not assume one fixed UI scale.
- **Small internal system:** Django Admin for clients, spaces, plants, membership/care/history. Not a large ERP, fleet CRM, sensor dashboard or workforce management rewrite.
- **Social layer:** Nova's existing posts, comments, photos, videos, stories, profiles, messages and notification/safety infrastructure are reusable. Plantupia Circle adds **specific plant context**, voluntary public help, and optional recorded follow-up outcomes, rather than a clone of a generic social feed. A private observation is never a public Story unless explicitly shared. A video community is distinct from plant ownership records.
- **Identifiers:** backend internal IDs are necessary. A scanned physical serial/QR is **not** necessary to use the app, and no serial-number gate should be invented. Species, installation inventory label and user's personal name are separate fields. Replacing a dead individual plant may require a new living identity while the location/planter persists.
- **AI/IoT:** may be useful later but are not V1 prerequisites. Peer help may complement AI; it is not guaranteed to replace professional horticulture expertise, moderation or operating expense.
- **Platform:** existing original Nova Android app on Google Play, package `com.omarkhair70.nova`, is the intended update path. Current task **excludes iOS**. Existing Oracle hosting is reported to auto-deploy; verify exact branch, staging and database migration gates before touching it.

## 4. UX thesis — how the features become one product

The user should experience:
`Plantupia installation / managed space → client access → living inventory → personal names → observed events with photos → optional public question through existing Nova Post engine → existing replies/messages → follow-up outcome retained in the plant's history.`

What this changes: a plant photo is not just a feed asset; it can become an observation in context, and a public question need not lose the result in disconnected comments.

**Permission split:**
- `Private`: space identity, internal location, installed assets, observations, care history, private images and access to records.
- `Public by explicit consent`: user-authored question/topic/caption and selected photo/video with selected Nova audience, governed by existing block/report/privacy rules.
- `Evidence`: observations, measured values, third-party reports and inferred suggestions are clearly distinct. No made-up plant-health percentage and no guarantee of biological diagnosis from a single image.

## 5. Existing code / review checkpoint

Read the latest PR diff and CI, not a stale checklist. As of the founding implementation branch:
- Django `backend/plants/` models and admin: `Space`, `SpaceMember`, `PlantUnit`, `PlantNickname`, `PlantEvent`, and Nova-linked `PlantDiscussion`.
- Kotlin `feature/plants/` and `feature/plants/data/`: authorized data retrieval, client sections, photos/events, questions and Circle UI.
- Shared color tokens, welcome/Home/Circle/Create/profile/video/memory wording and provisional geometric brand glyph.
- Nova's existing engines remain. These do **not** prove complete brand replacement in every edge screen.
- **Do not** equate CI green with a visually approved, complete, secure production release.

## 6. Open gates and explicit non-claims

- **Real company source data needed:** approved logo and vector artwork, photo/video rights, actual installations, plant inventory and species, client and site/zone mapping, field service rules, chosen brand copy. Never fill the gaps with mock customers presented as real.
- **UX/creative validation:** use actual brand assets, review the complete navigation and every Nova residual surface, test on devices, test RTL/English, contrast and accessibility, evaluate the community-first vs space-first entry.
- **Engineering validation:** test end-to-end post/care photo permissions, image formats, migrations against a restored Oracle-hosted data snapshot, existing storage URLs, saved-session refresh, question reporting/moderation and low-connectivity UX; verify Play signing/package ID and release history.
- **Stories linked to plants:** distinguish *the retained existing Nova story engine* from *a completed direct Story→Plant relation*; do not label the latter shipped without code and tests.
- **Website/photo visualizer:** not silently included in the Android PR; a distinct public website and experiment scope.
- **Commercial/legal:** code licensing, future team handoff, pricing, private meetings, staffing and ownership negotiations belong in private approved agreements — not public GitHub.
- **Deploy:** the user reports Oracle auto-deployment of the existing app's server. Determine the exact auto-deploy branch and migration behavior before merging; a PR itself does not deploy. Google Play Internal Testing is a separate release/promotion step.
- **Do not claim** that a public repo has absorbed the chat transcript or attached local/private PDFs; anyone outside the conversation must receive an approved private brief separately.

## 7. How to work from here

For every product change:
1. Identify whether it is grounded in **public research**, a **later working decision**, or a **new hypothesis**.
2. Point to the specific source and implementation target, with constraints and privacy impacts.
3. Reuse Nova systems unless a concrete technical reason justifies change.
4. Add/maintain tests, document what remains, and wait for approval before touching production.
5. If missing a private brand asset or private meeting decision, stop and ask rather than invent.

This index should be updated when later decisions are made. It does **not** automatically sync with ChatGPT conversations.

## 8. Later feature-selection decision (2026-10-11)

The user explicitly rejected treating Nova's pre-existing Reels, Stories, Pulse, Chat and Rooms as compulsory Plantupia screens just because the code is available. **Retain technical engines; justify each product surface from the real living-space use case.**

Current primary roles: `Space → living unit → real observation/care/issue → retained history → optional public Circle question → authentic follow-up`. A published media moment is a *separate* action, not a private journal entry.

Secondary roles: people discovery, messaging, temporary Stories, Reels, Pulse and Gatherings can be accessed where appropriate, but should not dominate the client living-space Home or the main Create hierarchy without evidence. Do not invent species, client sites, care outcomes, AI status, sensor readings or fabricated photography to make a UI appear finished.

The work after commit `87865db` began supporting searchable client-scale inventory, older history pages, context-first Circle cards and explicit public publish labels. Verify the **current** PR head and test run before claiming any of these fully QA-accepted. Additional assets, staging/backend and device review remain release gates.
