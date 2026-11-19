# Plantupia App Design Closure — Circle
Date: 2026-11-19
Branch: `design/plantupia-app-closure-20261119`

## Product decision

Circle is not an orbit visualization.

The existing relationship/activity backend remains valid, but the user-facing metaphor is now Plantupia:
- questions from real plants;
- people;
- intentionally shared moments;
- activity from those relationships.

The pass removes the visual/social-network metaphor that remained from Nova without deleting compatible backend event data.

## Information architecture

Circle keeps two authored modes:

### Plant questions

A question starts from a real plant.

The existing privacy rule remains:
- choose a plant;
- review exactly what becomes public;
- private care/history does not become community content automatically.

The source-backed `PlantupiaCirclePanel` remains the question feed.

### People & moments

This mode contains:
- shared stories/moments;
- people discovery entry;
- real relationship/activity events.

The user-facing activity filters are now:
- All
- Shared moments
- People

There is no Pulse tab.

Old pulse-backed events are not deleted. If an existing backend event arrives through that contract, it is presented generically as a short shared update rather than exposing a separate Pulse product metaphor.

## Removed Nova/Orbit presentation

Removed from the Circle screen:
- orbit constellation card;
- orbit rings around the current user;
- four floating person nodes;
- dark sci-fi person-detail dialog;
- `LIVE IN YOUR ORBIT`;
- `Waking your orbit...`;
- `Looking deeper in your orbit...`;
- Pulse as a top-level filter;
- `People in your circle` constellation-led heading.

A person's profile remains the canonical detail destination instead of a duplicate dark activity dialog.

## Plantupia visual language

The Circle root now uses the Plantupia cream background.

The existing editorial intro remains:
- `THE CIRCLE · HUMAN STORIES`
- `Better together`
- private-record boundary copy.

Quick destinations are authored as two small Plantupia cards:
- Shared moments — what people chose to publish
- People — find and follow intentionally

Section navigation uses Plantupia forest / paper / cream states.

The Ask-the-Circle card uses:
- Plantupia soft sage;
- Plantupia editorial typography;
- copy that explicitly says the owner reviews what becomes public.

Circle activity rows use:
- Plantupia color tokens;
- real actor;
- compact source/action text;
- optional real post/short-update preview;
- profile/content destinations already owned by the app.

## Shared event language

The shared Orbit event helpers remain internal compatibility functions but no longer emit Orbit/Pulse language.

Examples:
- like -> `liked a shared moment`
- comment -> `joined a conversation`
- repost -> `reshared a moment`
- pulse_reply -> `replied to a short update`
- unknown -> `shared activity`

Comment context uses normal quoted text with valid UTF-8.

Default event icon no longer falls back to the Orbit icon.

## What was not deleted

This slice does not delete:
- the existing Pulse repository/API;
- Pulse publishing/viewer code used elsewhere;
- Orbit repository/API event compatibility;
- Stories;
- Posts;
- people/follow relationships.

Those may be evaluated separately during Create/app-shell closure.

The goal here is user-facing Circle coherence without a risky data migration.

## Verification

- Kotlin compile after the Circle refactor: BUILD SUCCESSFUL.
- `OrbitStateOwnerTest` passed after adding explicit event-language regression coverage.
- Changed Circle/Orbit/intro files contain zero U+FFFD replacement characters.
- `git diff --check` passes.
- Android instrumentation source compilation is run before commit to guard the removed composable structure.

A local ignored dummy `app/google-services.json` is used only when required by the Google Services Gradle plugin and is removed before commit.
