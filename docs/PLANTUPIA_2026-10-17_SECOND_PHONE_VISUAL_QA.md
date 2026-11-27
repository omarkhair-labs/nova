# Plantupia — Second Real-Device Visual QA (2026-10-17, 19:49–19:51 device time)

**Evidence:** ten user-provided Android screenshots, deliberately **not** uploaded to the repository because they contain private account names, personal notes, and user-chosen media. Observed *after* the previous Plantupia QA APK handoff. Device screenshots do not independently expose a build SHA, backend deployment SHA, or confirm successful persistence across a fresh restart.

**Owner request:** inspect the screenshots against the research, references, briefs, product behavior and Android UI/UX. This is a new physical QA iteration, not an automatic sign-off.

## Evidence and severity

| Priority | Actual observation in capture | UX / product brief law | Corrective native change | Acceptance evidence still needed |
| --- | --- | --- | --- | --- |
| **P0** | `Concerning change` (third bottom save action) compressed to **one letter per line**, leaving a huge white region; other 2 buttons remain horizontal | Basic responsive typography/readability; Pass 07 Quiet Care must not be broken by device widths/font scaling | Three safe, full-width save actions in vertical stack with real touch targets; existing observation/care/issue API handlers kept intact | Screenshot at 320/360/390/430dp and 1×/1.5×/2× font; keyboard; all three saves produce correct private event type |
| **P1** | Ask Circle direct intent shows existing plant detail with photo, AI “Ask about this plant” and repeated “Plantupia Circle / Ask the Circle” intro, then old check-in/history/settings after question composer | **Optional Human Help is a deliberate public step**, not another paragraph in the private care view | Suppress redundant Circle panel editorial header in context; direct question intent presents question and prior linked questions, not private check-in/history/admin. Hide AI/assistant CTA in that mode | Create → Ask plant → review exact photo/title/text → 2nd account sees only chosen post → Back returns to space |
| **P1** | Default Plant Detail appends huge automatic Circle publishing form and “Separate public post” with duplicated dividers after history | Quiet Care → Field Notes → *optional* Human Help. Preserve privacy and Nova media while not forcing everyone to see a public form | Collapse community/sharing behind explicit section button, while preserving comment, outcome and separate public post links | Default first-screen focus remains care and history; explicit expand/collapse preserves form behavior; no unconsented posting |
| **P1** | Huge vertical rhythm on phone: paragraphs explaining the same private/public distinctions, big tall buttons, and “Check Back” far below care | Research-first visual system uses the actual plant/record/action as hierarchy, not a repeated brochure | Condensed Circle panels + fewer dividers/text; further whole-screen spacing QA still pending | First useful action and next real record are visible with minimal scrolling at user device font scaling |
| **P1** | Me still displays wide private-spaces/photo panel plus older Nova Posts/Reels/Reposts below a public graph | Native social engine remains but whole app should share one intentional visual identity | Earlier sage Plantupia Me container completed; **larger Profile composition/social IA pass still open** | Own vs stranger profile, privacy, DM/Reels/Stories and device visual review |
| **Functional positive** | Captures show real `Care recorded`, dated note, uploaded photo, real 2026-10-20 human-chosen follow-up, check-in actions and question with 0 replies | Private dated evidence is more honest than synthetic health scores | Preserve server-backed care, follow-up, outcome and history APIs; do not replace with prototypes | Create new record, background/kill/restart, verify same record + scope; another account cannot see private records |
| **Content context** | Test photos appear to be books and cover imagery, not actual plants | The app must show uploaded media honestly, never generate sample plants or claim a book depicts a real plant | Reword cover accessibility text as “Photo uploaded for this plant”, not “Actual plant photo” | Test with genuine plant photo and with arbitrary test image; explicit public photo review works as intended |

## Source hierarchy

- [Original Product Bible](./PLANTUPIA_PRODUCT_BIBLE_AND_HANDOFF_V01_2026-10-17.md) and source atlas/human-place research in `omarkhair70-droid/Plantupia`, branch `research/foundation-source-atlas-20261007`.
- [Pass 05 behavior teardown: Focus Friend / Day One / Are.na](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/07-award-winners-behavioral-teardown-pass-05-2026-10-14.md).
- [Pass 07 integration: Place → Quiet Care → Field Notes → Optional Human Help](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/design/09-pass07-integrated-product-synthesis-contract-2026-10-14.md).
- [First phone audit](./PLANTUPIA_REAL_DEVICE_VISUAL_BRIEF_AUDIT_2026-10-17.md) and [Circle decision map](./PLANTUPIA_CIRCLE_BRIEF_TO_NATIVE_DECISIONS_2026-10-17.md).

## Native correction on branch (NOT YET accepted)

- `PlantupiaClientSection.kt`: save buttons in column (Observation / Care / Concern) with `fillMaxWidth` and >=48dp target; no overflowing 3-item Row.
- `PlantupiaCommunityPanels.kt`: `compactHeader` option for contextual question form; 0-reply outcome copy does not imply anyone already gave help.
- `PlantupiaClientSection.kt`: direct QUESTION intent renders the public question flow alone after the selected plant; unrelated Check Back, living private history and management are only in normal plant mode. Optional community form is collapsed until chosen, not an obligatory tail.
- Keep existing Django auth, server-backed care, signed/public photo preview consent, original Nova social editing/post ID and optional assistant route intact.
- Important P0: actual QA server role / `can_add_plant` mismatch from the first screenshot audit is still **unverified remotely**. Do not circumvent Django permissions or claim the service fixed because one account had earlier care data.
- Final next gate: pass exact-head Kotlin/Android instrumentation/lint + Django CI; extract one **new** SHA-matched QA APK; check re-signing problem vs prior QA installed package, and get real on-phone screenshots with accessible text.

**No approval, production rollout, Play Console activity, or customer permission inferred from this document.**
