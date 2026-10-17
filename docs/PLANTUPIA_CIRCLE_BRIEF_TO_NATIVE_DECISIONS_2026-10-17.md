# Plantupia / Circle — research-to-native design decision 2026-10-17

**Status:** CODED CANDIDATE, NOT DEVICE/CLIENT ACCEPTED. **Branch:** `feature/plantupia-native-experience-20261012`, stacked draft PR #241. **Input:** actual 9-screen device review + inherited research / briefs; no invented user interview or benchmark success.

## Why this pass exists

On the owner's real QA Android screenshots, Circle felt like a renamed Nova feed with oversized post media, repetitive editorial slogans and buried action. But the **brief never demanded a generic plant feed**: Circle should let people encounter a question tied to actual plant context, respond through Nova's original social/comments engine, and allow the owner to **separately document an observed outcome** in private plant history.

### Reference → behavioral law → actual code decision

| Source / research provenance | Specific principle (do not copy visual artwork) | Implemented native change | Observable QA acceptance still owed |
| --- | --- | --- | --- |
| [Pass 05, Are.na knowledge links](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/07-award-winners-behavioral-teardown-pass-05-2026-10-14.md) | Question as an accountable knowledge object: human author/source, actual selected public photo, replies, **separately authored** outcome | `QuestionCard` in `PlantupiaCommunityPanels.kt` now foregrounds topic/caption/author, shows a smaller optional source photo and actual comment count; outcome badge only if server says resolved | Tap source question → Nova actual post/comment replies; resolution state and real outcome via owner; device and two-account test |
| [Pass 05, Day One](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/07-award-winners-behavioral-teardown-pass-05-2026-10-14.md) | Dated records and actual evidence are valuable, but a public post is not a care journal | Preserve `PlantupiaQuestionsPanel` linked private outcome action; do not add artificial dated Circle history or fake a second photo | Real saved private outcome remains private and recoverable in the selected plant's history |
| [Pass 05, Focus Friend](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/07-award-winners-behavioral-teardown-pass-05-2026-10-14.md) | Reduce attention tax / return people to the plant, not maximize scrolling | Remove duplicated headline and descriptive paragraph, reduce card vertical photo dominance to a compact 96dp optional thumbnail | Time to open one actual question / reply on 360/390/430dp; no nested scroll confusion |
| [Pass 07 integrated contract](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/09-pass07-integrated-product-synthesis-contract-2026-10-14.md) | PLACE → QUIET CARE → FIELD NOTES → **OPTIONAL HUMAN HELP**; private source data must never travel automatically with a public question | Shorter root Circle header and a small `Ask the Circle` card placed **before** the public question list; routes through `PlantupiaCreateIntent.QUESTION` to a real plant; preview and explicit consent retained | Real user selects plant/photo/text; sees public preview; private space name/notes/location absent from stranger-visible payload |
| [Nova integration visual contract](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/product/NOVA_INTEGRATION_VISUAL_CONTRACT_V01_2026-10-12.md) | Keep actual Stories, user identity, comments, Inbox and people discovery, without preserving old visual hierarchy | Keep `OrbitScreen` section switch `Plant questions / People & moments`, contextual Inbox/unread and discovery, Nova real post ID destination; visually condense title | Circle → public questions → people/media → Inbox/Story → return works on physical Android |
| [9-device-screenshot audit](./PLANTUPIA_REAL_DEVICE_VISUAL_BRIEF_AUDIT_2026-10-17.md) | Old single huge `yy` post visually looked like generic media feed; actual book covers were user test content, not invented media | Compact text-led card, optional genuinely authored photo, no stock/AI substitution; honest 0/1/100 reply labels | Genuine plants, Arabic/multilingual question lengths, no-image and failed-photo states |

## Choice and explicit alternatives

**Chosen candidate:** compact *question-first* public knowledge objects (title, authored context, modest photo as evidence, actual comments, resolved status if known). This is **not** proof that text-only cards perform better: photo may need increased prominence for certain biological questions after observed QA.

**Alternatives not silently removed:**
- Photography-dominant feed: useful when identification is essential; rejected as universal treatment, but images still open at real post detail.
- Full separate social application: unnecessary; Nova provides real person-to-person engines already.
- AI-generated summary of human replies: **not implemented**; risks reinterpreting advice and claiming evidence not in source.
- Automatically sharing private plant history: explicitly forbidden until the actual person independently chooses and previews what is published.

## Honest implementation boundary

- App routes to `postId`, Nova manages comments; no invented recommendation rankings, accepted answer status, botanical expertise badge or health inference. `resolved` is a server flag—not an outcome quote auto-published to Circle.
- `circleReplyLabel` renders 0/1/multiple replies truthfully with JVM tests. A malformed negative value clamps to zero, not a fabricated reply.
- Public question photo is **chosen by the user and reviewed**; private photo and placement are not reused automatically. Server photo EXIF handling must be verified in a real QA integration.
- Circle refresh pagination isolation remains, with earlier regression tests; no unbounded feed and no fabricated item count.
- No React/WebView/prototype replacement, PostgreSQL/Oracle mutation, Nova production edit, Play rollout or claim of design approval.

**Next:** exact-commit Android + Django CI, integrated QA APK, real two-account Circle preview/comments/outcome and visual comparison with original screenshots, then update this record with observed findings and accepted/rejected status.
