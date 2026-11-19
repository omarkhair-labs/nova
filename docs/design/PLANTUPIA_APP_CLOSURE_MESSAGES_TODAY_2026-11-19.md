# Plantupia App Design Closure — Messages, Team, Today & Schedule
Date: 2026-11-19
Branch: `design/plantupia-app-closure-20261119`

## Why this pass exists

The Android product already has substantial real functionality. The design problem is no longer "add more screens." It is to close the places where the old Nova interaction model still exposes a generic social/QA surface instead of a clearly authored Plantupia product.

This pass starts with two high-signal gaps:
1. Messages did not expose the existing Plantupia Team relationship even though authenticated support tickets, staff replies and push routing already exist.
2. Today / Schedule had truthful data and working calendar logic, but the visual hierarchy still read like a functional utility list rather than a finished Plantupia care surface.

No new mock backend or duplicate state store is introduced.

## Messages + Plantupia Team

### Product decision

Messages now has two deliberately separate relationship types:

- **Plantupia Team** — private, plant-scoped support requests and real staff replies.
- **Your conversations** — direct/community messages and groups.

They share one destination because they are both conversations, but they do not share the same data model or privacy assumptions.

### Team entry

The first surface in Messages is now **PLANTUPIA TEAM / Private support for your plants**.

It uses the existing authenticated `PlantupiaRepository.plants()` data:
- one active plant → opens that plant's existing `PlantupiaTeamSheet` directly;
- multiple active plants → asks which plant the request belongs to, then opens the same real support surface;
- no active plants → explains that a plant must be added/claimed before a private plant support request exists;
- plant-loading failure → honest retry state.

The team surface does not invent:
- response times;
- unread support counts;
- fake staff presence;
- a second support inbox;
- access to private plant history/photos beyond the existing explicit consent model.

### Existing support truth reused

`PlantupiaTeamSheet` remains the source of truth for:
- service-request tickets;
- actual staff replies;
- ticket status;
- private reply composer;
- explicit context sharing;
- optional placement sharing;
- human-access gate.

Push notifications still route authorized `plantupia_support_reply` targets through the existing plant-scoped support flow.

### Message visual language

The screen is now authored as Plantupia:
- `MESSAGES / PRIVATE & COMMUNITY` eyebrow;
- `Conversations` editorial heading;
- cream/forest/paper Plantupia surfaces;
- explicit `YOUR CONVERSATIONS` section;
- search copy names people, groups and conversations;
- empty state explains that community conversations live below while Team support remains separate above;
- loading copy encoding residue was removed.

The existing inbox paging, filters, unread state, search and community conversation rows are preserved.

## Today / personal rhythm

### Product decision

A personal check-in is a date the person chose. It is not:
- a watering instruction;
- a diagnosis;
- a sensor event;
- proof that care was completed.

The visual language now reinforces that distinction.

### New hierarchy

The old compact `YOUR CHECK-INS` list becomes a **PERSONAL RHYTHM** surface.

When something is due:
- the due count is visible;
- the first due plant becomes a proper editorial focus card with its real uploaded photo;
- the copy says the date was chosen by the person and instructs them to open the plant before recording anything;
- additional due plants remain incrementally revealable;
- the next future check-in is visible separately as `UP NEXT`.

When nothing is due:
- the next scheduled personal check-in is the hero;
- the date reads as human language such as Today / Tomorrow / Mon 23 Nov;
- the UI explicitly says nothing is marked done until the person records what actually happened.

No synthetic schedule appears when no real follow-up date exists.

## Care Schedule

### Existing data model preserved

The calendar still merges three truthful sources without collapsing them:
- personal user-chosen follow-ups;
- reviewed/approved care plan dates;
- recorded Living History moments.

Week/month behavior, selected date, server calendar history and plant-opening behavior remain unchanged.

### Visual closure

The first fold now reads as:
- `CARE SCHEDULE`;
- current week or month;
- explicit explanation that planned dates and recorded moments are distinct;
- previous/next period control;
- Today;
- Week / Month mode;
- visible Planned vs Recorded legend.

The selected day now has its own hierarchy:
- `SELECTED DAY`;
- human date;
- count summary for recorded and planned entries;
- honest empty-day copy.

Entry status language is explicit:
- `RECORDED`
- `DUE`
- `PLANNED`

The calendar still uses compact marks inside date cells, but substantive status is no longer dependent on those marks.

## Verification

### Kotlin compilation

`:app:compileDebugKotlin` passed after Messages + Team + Today changes.

A second incremental compile passed after the Schedule redesign.

The build used the local Android SDK directly. The repository intentionally does not contain a production `google-services.json`.

### Unit tests

A local ignored test-only `app/google-services.json` was generated solely to allow the Google Services Gradle plugin to complete. It is covered by the repository's existing `.gitignore` rule and is not staged or committed.

Targeted test run passed:
- `PlantupiaTodayPanelTest`
- `PlantupiaCareCalendarModelTest`
- `InboxViewModelTest`
- `MessagesNavigationPolicyTest`

The Today test suite now also verifies human date labels without changing schedule truth:
- Today
- Yesterday
- overdue date
- Tomorrow
- future weekday/date
- malformed-value passthrough

### Diff hygiene

`git diff --check` passes.

## Next closure sequence

After this slice:
1. Notifications — remove QA/Nova residue and make Care / Team / Orders / Circle a coherent Plantupia activity surface.
2. Circle + People — remove remaining Orbit metaphor where it does not serve Plantupia.
3. Profile — restructure as the person's Plantupia life rather than a stack of entry buttons.
4. AI positioning — decide root-nav weight versus contextual access.
5. Create — align creation actions with the final information architecture.
6. Loading / empty / offline / error states and final app-shell polish.
