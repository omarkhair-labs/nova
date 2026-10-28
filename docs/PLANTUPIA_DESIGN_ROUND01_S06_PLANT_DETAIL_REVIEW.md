# Plantupia design workshop — Round 01 / S06 Plant Detail

**Date:** 2026-10-28  
**Status:** DESIGN REVIEW ONLY (neither variant approved)  
**Native baseline:** Nova draft PR #241 at `9eb039d2d879b43c0e418fe90b8db4580930f45f`; CI run `40663078245` passed.  
**No application code, deployment, billing, permission or real plant record changes are authorized.**

## Owner-origin problem

The owner's physical QA screenshot shows a dated, persisted observation in Living History, followed by giant `Plant details & settings` and `Community & sharing` accordions at the bottom of a long page. Care actions, help and history feel disconnected. The goal is a coherent plant-focused information architecture, not relocating a few buttons.

## Two candidate layouts

| | A: Guided overview | B: Organized workspace |
| --- | --- | --- |
| Shared first viewport | Real plant identity/photo/fallback, actual next check, Record care, AI, Team | Same |
| Below actions | Recent activity, Care Guide summary, contextual Circle sharing | Overview / Care / History in-page tabs |
| Full private history | Dedicated contextual destination | History pane and an optional full timeline |
| Advantage | Fast daily visit; fewer controls | Clear compartments for larger history |
| Risk | Endless scroll if uncontrolled | Navigation depth, duplication, selected-tab state |

**Review recommendation, not approval:** consider A as the default for a short daily action, but test B with a long-lived plant and real record volume before deciding. A carefully bounded hybrid is possible only if owner feedback demonstrates the need. Do not implement both navigation patterns at once.

## Shared non-negotiable contracts

1. Only authenticated individual plant data and actual uploaded photo; honest no-photo state otherwise. Unknown species is valid.
2. An approved opted-in inspection or human-chosen check-in, never automatic watering, fake health status or sensors.
3. Record care is primary and permissioned; actual note/photo/follow-up persists in private history, with draft recovery.
4. AI and team support remain separate entitled functions and separate from social Inbox/Groups. AI plant context sharing is per question.
5. Private dated history is not automatically public; Ask Circle requires explicit preview and approval.
6. Settings, access and archive/rename live in an authorized overflow menu, not a giant bottom accordion.
7. Five roots remain Today / Plants / Create / Shop / Circle; full details return to the correct Plants destination.
8. Base plants/care/history remain useful without a paid subscription.

## Shared test states and review gates

Interactive A/B prototype includes owner with one existing QA observation, first plant/empty history, read-only, no photo/unknown species, and offline/unavailable. Its mock destinations open Record, AI, Team, Care Guide, History, Circle and Settings. **It does not call a backend or perform live AI.**

Required after design approval: 320/390/430dp and tablet, 2x text, IME and navigation safe insets, 0/1/7/100 histories, correct A/B account isolation, request/revocation/permission states, actual Next Check semantics, explicit public consent, correct Back/relaunch/navigation and physical phone owner acceptance.

## Open decisions — all unapproved

- `S06-D01`: A, B or hybrid?
- `S06-D02`: Which actions must be visible in first viewport?
- `S06-D03`: Contextual Ask Circle placement?
- `S06-D04`: Full History separate destination or in-place tab?
- `S06-D05`: Empty/unknown/read-only/offline state copy?
- `S06-D06`: Nested detail header vs global account header?

## Owner workshop evidence

The owner received a standalone HTML A/B review, a three-page PDF, screen captures and full editable Markdown decision/testing notes. These binary review files are supplied through the conversation workshop package, **not committed to this docs-only branch**. Original screenshot came from an older installed QA build; concept image does not prove real plant identity.

**Next:** owner tries both design candidates, chooses and explains preference. Only after explicit owner approval do we lock S06, move to Today/My Plants, and eventually write the Codex implementation handoff.