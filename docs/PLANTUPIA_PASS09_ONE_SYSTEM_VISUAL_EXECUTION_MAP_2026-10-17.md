# Plantupia — Pass 09 / One Integrated Native Visual System + Execution Map
**Date:** 2026-10-17 | **Status:** Active research-to-native execution ledger; historical planned items below are not all current (see implementation updates). No owner/device acceptance | **Source:** Product Bible, Pass04–08, native QA captures and user's current direction.

## Final direction (do not reopen three HTML design routes)
**One coherent Android-native Plantupia**. Material language: warm paper/cream, forest/sage, quiet editorial serif for true plant/place identity, readable UI sans, real photographs when users supply them, authored botanical–architectural vector motifs, clear six-action care pictograms, restrained meaningful motion. Maintain existing Nova Android/Django functionality and privacy; visually integrate rather than recolor. Do not copy winner artwork or Reddit identity characters.

**New builder requirement:** an account **without a photo must have a distinctive designed identity, not just an initial or number** (Reddit-inspired product behavior, NOT a clone of Reddit's Snoo). Apply to own profile AND person cards, feeds, questions/replies, DM list, followers, story attribution, people search and notifications. Account identity represents a *person*, never falsely depicts a known face or assumes their species, gender, race, age, mood or personality.

### Profile avatar system — deliverable specification
- Native Compose-rendered **original human-centered illustrated avatars** with a restrained botanical/architectural visual accent (e.g. abstract portrait/silhouette and subtle leaf/shadow/frame), not a pot or plant pretending to be the user.
- Calibrated family with **consistent proportions, strokes, radii and contrasts**; e.g. 6–8 authored portraits × 4–6 palette combinations × botanical/background accents. Specific art must be designed and reviewed; these numbers are a candidate kit size, not existing assets.
- Each account with no photo gets **one consistent variant**, keyed to an immutable account ID (prefer existing server identity) or a persisted explicit preference. Do not generate new appearance randomly every recomposition/reinstall or use name's first letter/zero.
- Optional **Choose avatar / Change avatar / Upload photo** control. Uploaded image always takes precedence; never silently replace someone else's photo. Resolve fallback if upload fails; do not send anonymous person's photo to remote generation.
- A single visual owner / composable for own profile, *other public profiles*, posts/comments, Circle questions, people discovery, Stories, Inbox/DM, followers and notifications. Do NOT globally alter Nova production appearance; use Plantupia theme/context and keep Nova original listing untouched.
- Support avatar at 24/32/48/64/96dp, light and dark media backdrop, offline, TalkBack `Profile illustration for [display name]`, long/multilingual names, image loader errors, 1.5–2× font. No animation required for static avatars: optional **small one-time response on first choice**, low motion and reduced-motion safe.
- If stable immutable account ID isn't available in existing view models, **investigate data source and write a role/identity-neutral persistence contract first**; don't improvise secrets or change backend schema silently.
- Never infer/assign illustrated physical traits from a user name or plant collection. Manual selection is allowed; deterministic defaults must remain abstract.

## Source evidence and what actually exists
- [Pass08 whole-product award/competition review](./PLANTUPIA_PASS08_WHOLE_PRODUCT_DESIGN_RESEARCH_EVALUATION_2026-10-17.md) and source repo `omarkhair70-droid/Plantupia` Pass04–07: authored illustration supports identity/empty states; real photos are evidence and never decorated into fake observations. 'Human & Living Places' is the adopted Me direction.
- `app/src/main/java/com/nova/app/ui/components/NovaAvatar.kt`: actual fallback is `fallbackText.trim().firstOrNull()?.uppercase()`; this is the technical cause of letter/"0" displays. A fallback brand mark appears only when no text; no default character system.
- `feature/profile/PlantupiaMeIdentity.kt`: new sage identity but calls legacy `NovaAvatar` and uses mixed `NovaType` tokens. The rest of `ProfileScreen.kt` appends actual private-spaces summary then Nova social graph/posts — needs one composition.
- `feature/plants/PlantupiaVisualComponents.kt`: six authored Canvas care pictograms and selected-state spring already exist. **Refine and retain**, don't rewrite as if absent.
- Real user photo/history, check-in and Circle zero-reply question have appeared in phone QA. Backend owner vs viewer/manager authorization and QA `can_add_plant` runtime mismatch remain unverified; no claim of full integration.

## Six workstreams — exact build order / acceptance
| ID | Workstream / user-visible outcome | Reuse vs create | Main source owners | Evidence to close |
|---|---|---|---|---|
| **W0/P0** | QA safety and product correctness: no vertically broken buttons, creation permission truth, check-in/photo/save, public preview, stable QA signing | Keep Django ACL and current native source; fix actual deployed mismatch | `PlantupiaClientSection.kt`, `PlantupiaRepository.kt`, Django plant APIs, CI/release | Exact-head CI, real 320/360/390/430dp on phone, 1×/1.5×/2× text, owner/viewer + second account, same data after restart; confirm QA backend SHA/DB scope before real data |
| **W1/P1** | **Unified visual grammar**: one type scale and icon-text rules, visual hierarchy, button sizes, forest/cream/sage roles, loading/error/empty/success | Existing `PlantupiaDesignTokens` + Nova technical systems; no unlicensed font bundle | `PlantupiaDesignTokens.kt`, `NovaType`, Home/Me/Circle/Create/Plant UI | Same types/styles in the six principal screens; Arabic/English wrapping, TalkBack/contrast, no giant repeated slogans |
| **W2/P1** | **Illustrated people and integrated Me**: no letter or number as sole no-photo identity; human + their living places, then opt-in public social presence | Create custom character family + shared native component; retain actual avatar upload and Nova posts/DM | `NovaAvatar.kt` (do **not** change globally for Nova), new Plantupia avatar component, `PlantupiaMeIdentity.kt`, `ProfileScreen.kt`, public profile/people/comment/message call sites | Two accounts no-photo get distinct **stable** avatars across app/restart; photo override; no private places on visitor profile; Stories/DM/profile taps and returns work |
| **W3/P1** | **Real plant visuals + illustrated states**: real place and plant picture continuity, useful field notes/photo dates, truthful no-cover, 3–5 authored empty graphics | Reuse user uploads, Care Canvas kit, plant/care timeline; add *sparse* botanical–architectural line illustrations for zero-content only | `PlantupiaClientSection.kt`, inventory, history, Create onboarding/profile empty states | No fake pictures, no false 'before and after', photo crop legibility, 0/1/7/100 assets, real 2-date comparison when enough evidence |
| **W4/P1** | **Coherent social and motion**: Circle question/author/selected photo, public preview, Stories/Reels/Inbox identity, native tap transitions, save/result affordances | Reuse Nova media/social engine and actual Circle APIs; add restrained motion to actual state changes | `OrbitScreen.kt`, `PlantupiaCommunityPanels.kt`, `CreateHubScreen.kt`, Nav/DM/media | Clear public/private choice; user-chosen preview; real comments; reduced motion, Back, keyboard, retry/timeouts and no fake saved confirmations |
| **W5/P2** | Final product assessment, technical peer audit, ready-for-owner-approval **one APK**, then release requirements | Codex read-only audit then bounded creative/technical fixes; separate Play work after approval | [Codex handoff](./PLANTUPIA_CODEX_AUDIT_HANDOFF_CONTRACT.md), screen ledger, CI artifacts, Play checklist | One cohesive real-phone review; documented failures/retests; stable QA signing; no production Nova/Oracle changes; later separate Plantupia application ID/AAB approval |

**Scheduling rule:** W0 blocking integrity is continuous; W1 → W2 (priority new requirement) → W3 → W4 are integrated UI increments, not separate visual products; W5 only after the first integrated phone pass. For user-facing phone tests, all visible surfaces must use the same identity and no new skin-only screen may be accepted alone.

## Screen-by-screen practical implementation inventory
| Surface | Specific remaining work |
|---|---|
| Welcome/onboarding | 1–2 lightweight original illustrated empty/explanation moments and first private place/plant creation; no carousel of generic marketing claims |
| Home / selected space | true place identity/photo, check-in/actions above fold, accurate counts and metadata, real list when 100+; no duplicate poetic title |
| Plant / care / history | responsive care action/save grid, photo evidence/date, quick care, history photo continuity, clean outcome, collapsible settings/community |
| Create | quick native Private vs Public picker; person avatar in header, real plant chooser; consent screen only on public path |
| Circle | question-first author, illustrated no-photo avatar, actual 0-reply state and prior outcomes; genuine public media secondary |
| Me / public person | human illustrated avatar if missing photo, actual places on SELF ONLY, beautiful human-to-place cue, community activity without towering Nova collage |
| Posts, Comments, Inbox, People, Stories, Reels | person identity consistent, existing Nova engines/routes still working; preserve audience and social privacy |
| Empty/loading/errors | semantic original vector illustrations when the state earns them; don't fill every UI gap with art |

## Explicit rules from research — when each medium appears
- **Word:** permission, saving state, exact action, source/uncertainty, destructive/public choices. Short, readable.
- **Icon + word:** care, tabs, unfamiliar actions, share/record context. Consistent pictograms, correct semantics.
- **Icon-only:** well-known compact button *with* accessibility label and touch target.
- **Real photo:** actual observed living plant/place/person/content. No generated plants posing as evidence.
- **Commissioned illustration:** person with no avatar, empty world, minimal onboarding, optional fieldbook accent. Do not use to fake an installed plant or verified condition.
- **Motion:** after a genuine press, navigation or confirmed backend save, with no distracting loops, reduced motion and interrupted transitions respected.

## Original Pass 09 gaps at planning time (historical snapshot; see latest increments below)
- **Not implemented yet:** illustrated default avatars across app; approved artwork asset kit; complete shared typography/font licensing; product-wide motion grammar; final Profile hierarchy.
- **Coded, not completely verified:** care/history/check-in/public question private/outcome flow; community engine from Nova; launch of assistant adapter.
- **Unproven / gated:** live QA backend isolation, role-based Add Plant fix, actual two-account media privacy, AI provider quality, final separate Android Play app, company-run visits, stable QA signing.
- **Outside current scope:** automated plant species/illness diagnosis, verified sensor readings, IoT watering, fabricated before/after.

## Codex review request
Codex may challenge iconography, illustration composition, user identity and information architecture *with evidence*, offer an original stronger design solution, and execute small reversible trials **on Android Native** with permissions preserved. Not three new HTML websites or a full research restart. Deliver a ranked audit and then approved fixes.

**Exit condition:** signed-in person can find actual space/plant, record what they did, see a dated story, optionally ask humans, navigate public/profile/inbox with **recognizable human identity even without photo**, with no broken small screens; owner accepts after physical phone review.

## 2026-10-17 W2 first native illustrated-person increment — CODED, NOT DEVICE ACCEPTED

- New `PlantupiaAvatar.kt` draws an original abstract person centered within botanical/architectural artwork (8 figure variants, 5 color palettes, 3 accents), using the existing **immutable numeric API person ID**. Names never assign looks. An actual uploaded photo takes priority, and a failed/offline image falls back to the same native illustration.
- Integrated in owner Me, visited public profile, People rows, Edit Profile preview and one-to-one Inbox; group imagery retains Nova semantics. No schema, migrations, unlicensed fonts, permission change or production asset substitution.
- Added pure JVM deterministic-style tests. **Still incomplete:** posts/comments/Stories/Reels/notifications, user-selectable avatar variant persisted to server, complete profile hierarchy, real-device screen sizes/TalkBack and cross-account visual signoff. No claim of an APK or owner acceptance.

**Shared Nova UI architecture seam (CI follow-up):** Keep `NovaAvatar` as the actual Me/People call-site owner. These surfaces opt into the authored illustration via optional `plantupiaUserId`; all unmodified NovaAvatar callers retain the original behavior. No guard weakened or bypassed. Verify the native build and tests at the new exact SHA.

**W2 social identity integration continuation:** Reused the same opt-in NovaAvatar seam for actual Post author, Comment/Reply author, Stories author/viewer and self Add Story rail (including a missing real photo). IDs come from Nova post and story models; group chat identity and anonymous text-story composer remain intentionally separate. Android/build and phone proof still required.

## 2026-10-17 W1/W2 profile cohesion and private-data isolation increment — CODED, NOT DEVICE ACCEPTED

- Owner Me and visited Person now share **one public-safe Plantupia person card**, with paper/forest/cream visual roles and the same deterministic illustrated person/photo fallback. Public Person continues through the original `NovaProfileIdentity` seam but the opted-in Plantupia branch uses `PlantupiaMeIdentity`; vanilla Nova remains unchanged.
- **Privacy guard:** `PlantupiaProfileSpaces` now requires the authenticated numeric account ID, skips nonpositive/unknown account, and keys its repository/loading/retry/requests by ID to avoid displaying a prior session's private-space cards after account switching. Visitors never invoke this private component.
- Added JVM ID-policy tests; server authorization continues to be the source of truth. No backend endpoint/schema change.
- This is a first cohesive profile/identity pass, **not** a signoff on overall screens, fonts, approved illustration, manual avatar picker, or two-account/physical QA. Current exact-head CI is required.
