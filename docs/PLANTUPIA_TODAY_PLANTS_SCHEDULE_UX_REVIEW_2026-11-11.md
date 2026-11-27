# Plantupia — Today / My Plants / Care Schedule — UX review pass
Date: 2026-11-11
Based on: QA phone screenshots (Today + My Plants) and #253 exact head `cab2a4f4`.
Status: **Draft UX change only. Not approved for production or deployed.**

## The design rule
- **Today** = the next truthful action, not a dashboard of all features.
- **My Plants** = the customer's actual spaces and private plants.
- **Schedule** = dates across personal follow-ups, reviewed inspections and recorded observations, using one existing source of truth.
- Keep brand paper/forest/sage, original Android Compose/navigation, real photo/privacy semantics.
- No additional bottom tab, second calendar, fabricated watering forecasts, silent status updates, or new persistent schedule.

## Direct observations from the provided QA screenshots
1. Three global utilities (inbox, notifications, profile) already occupy the top bar. The previous extra floating calendar icon sat beside an uppercase MY PLANTS label on a **separate row** before the actual page title, producing accidental hierarchy.
2. Today: a large generic green masthead, then three approved inspection cards, then a separate personal check-in section with another calendar button and repeated empty-state sentences. The actual next step is below the fold.
3. A failed / unavailable hero-photo placeholder displayed text directly below the overlaid living-space title: `Photo unavailable` collided with `Plantupia CEO Pilot`.
4. In the calendar source, two header descriptions, ISO date navigation, a large week/month grid, and a long explanation at the bottom competed with the selected day's records.

## Changes in this review branch
- Keep the existing three authenticated header actions and bottom navigation unchanged.
- **My Plants:** remove the extra route-level MY PLANTS/calendar row. Place the **existing** care-calendar icon beside the main My plants heading and Add plant action; for narrow or large-text screens it becomes a labeled Schedule row.
- **Today:** move an accessible **Schedule** action to the top masthead, alongside the date. Reduce the masthead's generic prose. The personal check-in panel no longer repeats the calendar button or displays an empty explanatory block when the user has no chosen personal dates.
- **Approved inspections:** retain genuine reviewed plans; initially reveal the most urgent two and provide `See all` for the rest. Do not delete any approved plan or change its actionable flow.
- **Personal check-ins:** retain one real due list with an expandable remainder; reduce photo size and card height; no invented check-in when none is scheduled.
- **My Plants space media:** the existing photo placeholder becomes icon-only behind the living-space title; real owner photo and caption semantics are preserved.
- **Schedule:** shorter heading, human-readable week labels, localized weekday labels, accessible per-day count+recorded semantics, subtler buttons, animated selection tint and week/month height transition, one honest empty state; the same existing API data and Plant Detail destination remain authoritative.

## Non-negotiable acceptance before merging into #253
- Exact branch Android unit + lint + assemble QA and release; no Kotlin compilation errors.
- On real Android phones 320/360/390/430dp with 1.0x/1.3x/1.5x font: title, Schedule, Add plant, month cells, and global header utilities do not overlap; reachable tap targets at least 48dp.
- Today -> Schedule, My Plants -> Schedule, Schedule -> Plant Detail -> back; selected day/month/week navigation, different months and dates around New Year.
- Cases: 0 plants; plants but no personal check-ins; approved inspection due; 1/3/20 inspections; owner photos loaded/missing/failed; archived spaces; cross-account isolation.
- Week/month animation should honor reduced device animation settings; TalkBack should announce date and counts rather than ambiguous punctuation.
- Re-verify Android QA against **isolated, backed-up** Preview #253 exact head and its migration 0031; do not deploy or merge to master or change Production automatically.
- Only after the real screenshots are approved should we consider tighter Today aggregation of inspection and personal check-in urgency on a shared factual model, rather than counting one and calling everything complete.

## Design references (research, not copied UI)
- Android Jetpack Compose Design Systems & Accessibility: https://developer.android.com/develop/ui/compose/designsystems
- Compose Semantics: https://developer.android.com/develop/ui/compose/accessibility/semantics
- NN/g Progressive Disclosure: https://www.nngroup.com/articles/progressive-disclosure/

## 18:06 follow-up QA photos (five Android screenshots)
- **Schedule, month:** grid still showed 6 heavy rows of outlined tiles with tiny "1 ·" labels. Calendar toolbar was scrolled out of view after the mode switch; a user could not see which month the grid belonged to. Move weekday labels to one separate ruler, keep 48dp month targets, show only real event markers, and reset scroll to the toolbar on mode change / opening the screen.
- **Schedule, history:** the device displayed `Recorded care history is unavailable: The Plantupia server returned an invalid response`. Root cause traced to Android `calendarHistory()` calling generic `plantupiaRows()`, which requires an `id` for each list item. The existing backend endpoint intentionally returns `date / plant_id / plant_name / kind / count / state` **aggregates without an ID**. Use a dedicated strict aggregate parser and unit-test valid and malformed responses; do not add invented IDs or hide real failures.
- **New Space:** a cramped horizontal `Create and add a plant` + `Cancel` action row wrapped Cancel into `Canc / el`. Stack the full-width primary action and the secondary cancellation target; preserve the save-then-open-plant behavior.
- **Create:** editorial intro is fine but three repetitions of privacy language push actions down. Keep one accurate privacy statement and concise category headings while preserving explicit public-share choices.
- **Today:** inspection cards and generic words still dominate first fold. Keep due-first ordering and real actions, compress card footprint and format stored dates as human-readable English abbreviations.
- Follow-up screenshots omitted a full My Plants first fold; do not claim visual sign-off for that screen until a matching screenshot exists.

**Deployment distinction:** The QA APK reflects the PR #299 branch. No backend fix or schema change is required for the history parser mismatch; existing Preview response format is valid. No Production changes or automatic deployment were made.
