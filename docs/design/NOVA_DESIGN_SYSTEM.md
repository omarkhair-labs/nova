# Nova Design System

Status: foundation rollout in progress

Nova's product identity is **the social app that remembers your life with your people**. The design system exists to make every surface feel like the same product while allowing deliberate feature moods such as Tonight, media viewers, and chat themes.

This is a code-owned system, not a visual redesign. Existing behavior and product contracts stay intact while duplicated styling migrates behind shared roles and components.

## Principles

1. **One Nova, multiple moods.** Core navigation, page chrome, feedback, typography, spacing, shapes, icon language, and interaction hierarchy are shared. Feature moods are explicit theme variants, not local styling drift.
2. **Semantic roles over raw values.** Screens should ask for `NovaType.sectionTitle`, `NovaSpacing.lg`, or a shared shape role instead of choosing an arbitrary font size, gap, or radius.
3. **Shared components own repeated interaction.** Buttons, fields, headers, navigation, avatars, badges, dialogs, sheets, loading, empty, error, and system-feedback patterns should have narrow shared owners when they repeat.
4. **Feature ownership remains feature ownership.** Shared UI does not absorb domain state or networking. It only owns reusable presentation and interaction primitives.
5. **Accessibility is part of the system.** Contrast, touch targets, readable hierarchy, content descriptions, state communication, and motion restraint are design-system requirements.
6. **Migration is incremental.** Closed-testing stability is more important than rewriting every screen at once. New foundations land first, then existing surfaces migrate in bounded PRs.

## Foundation source of truth

Android UI foundations live under `app/src/main/java/com/nova/app/ui/theme/`.

- `Color.kt` — semantic base color roles and controlled palette override seam.
- `Type.kt` — semantic Nova type roles mapped into Material 3 `Typography`.
- `Shape.kt` — global rounded shape scale mapped into Material 3 `Shapes`.
- `Space.kt` — shared spacing rhythm.
- `Elevation.kt` — shared lift roles.
- `Motion.kt` — shared motion-duration roles.
- `Theme.kt` — composes the Material 3 theme used by Nova hosts.

Material 3 remains the rendering foundation. Nova owns the brand decisions layered on top of it.

## Current foundation roles

### Color

Use semantic roles from `Color.kt`: background, surface, ink, muted, border, accent, accent-soft, and danger. Do not encode brand colors directly in ordinary screen code.

Feature palettes are allowed only when the feature has a deliberate presentation mode. Examples include:

- chat appearance themes;
- Tonight dark/live presentation;
- full-screen photo/video presentation.

Those palettes should be centralized in their feature theme owner rather than scattered through individual composables.

### Typography

Prefer semantic `NovaType` roles:

- `display`
- `pageTitle`
- `screenTitle`
- `sectionTitle`
- `title`
- `body`
- `bodyCompact`
- `subtitle`
- `button`
- `label`
- `meta`
- `micro`
- `navigationLabel` / `navigationLabelSelected`
- `badge`

The Material 3 type scale maps to these same roles so Material components inherit Nova hierarchy.

### Shape

The global shape scale is:

- extra-small: 10dp
- small: 14dp
- medium: 18dp
- large: 22dp
- extra-large: 28dp

Circle/full shapes remain valid where semantically appropriate, such as avatars and circular icon actions.

### Spacing

The shared spacing rhythm is 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32dp. Existing one-off measurements migrate only when a shared role is equivalent; this rollout does not intentionally change layout geometry just to satisfy a token rule.

### Elevation

Nova is primarily border-and-surface driven. Use elevation sparingly:

- flat: 0dp
- raised: 1dp
- floating: 6dp

### Motion

Use semantic duration roles instead of arbitrary timing values:

- fast: 120ms
- standard: 220ms
- emphasized: 320ms

Motion must communicate state or hierarchy rather than exist as decoration.

## Shared component boundary

Shared UI lives under `app/src/main/java/com/nova/app/ui/components/` and must remain split by responsibility. A generic dumping-ground file is forbidden.

Existing stable seams include:

- `NovaPrimaryButton` / `NovaSecondaryButton`
- `NovaTextField`
- `NovaHeader`
- `NovaBottomBar`
- `NovaAvatar`
- `NovaActiveCallPill`
- shared media/profile primitives

The next consolidation wave will add or normalize the following only where repeated usage proves the seam:

- page header/back chrome;
- icon button and app-owned icon catalog;
- badge/status presentation;
- loading, empty, error, success, and retry messaging;
- confirmation dialogs and bottom sheets;
- shared card/container roles.

## Iconography policy

Nova currently has an app-owned communication icon seam plus Material icon usage and some text/Unicode action glyphs. This is transitional.

Target rules:

1. interactive icons are referenced through `ui/icons`;
2. one stroke/fill language is chosen per icon family;
3. Unicode/text glyphs are not used as action icons once an app-owned vector exists;
4. feature code does not become the owner of reusable icons;
5. content descriptions are mandatory for meaningful icon actions.

The icon migration is intentionally a separate PR so this foundations PR does not visually replace navigation or action icons during closed testing.

## Navigation and page chrome policy

Navigation behavior remains owned by the existing navigation architecture. The design system owns only visual chrome.

Target consistency:

- one bottom-navigation visual language;
- one standard page-header/back pattern for ordinary screens;
- explicit exceptions only for immersive media, calls, and other full-screen experiences;
- unread badges and selected states use shared tokens/components.

## Feedback and system messaging policy

Loading, retry, empty, success, warning, and error states should communicate with a small consistent set of patterns. A feature may own its message copy and state machine, but it should render repeated feedback through shared presentation primitives.

This includes Notifications/Activity. Notifications remain feature-owned data/state; their visual rows, unread state, loading/retry/empty presentation should consume shared Nova roles as migration proceeds.

## Feature moods

Feature-specific presentation is valid when it is deliberate and centralized.

### Chat

`NovaChatPalette` is the current strongest example: each chat theme defines semantic background, surface, bubble, text, accent, border, and composer roles and can provide a controlled Nova color override.

### Tonight

Tonight may remain dark and visually distinct, but its palette should move behind a named Tonight palette instead of raw colors spread through composables.

### Pulse and media

Pulse/media dark surfaces may remain visually distinct, but dark media roles should be explicit and reusable rather than local constants repeated between viewer/card implementations.

## Rollout plan

### DS-1 — Foundations

- establish spacing, shape, elevation, motion, and semantic typography roles;
- wire shape and typography into `NovaTheme`;
- migrate the stable shared Header, Button, TextField, and BottomBar seams without intentional visual redesign;
- add architecture enforcement for the foundation owners.

### DS-2 — Icons and page chrome

- establish `NovaIcons` app-owned catalog and aliases;
- migrate bottom navigation, settings, headers/back actions, notifications, calls, and common actions;
- remove action-oriented Unicode glyphs as replacements become available;
- normalize ordinary-screen page chrome.

### DS-3 — Feedback and containers

- create shared loading/empty/error/retry/status primitives;
- establish repeated card/container roles;
- migrate Notifications, Settings, People/Profile, Home support states, and dialogs/sheets.

### DS-4 — Alive feature convergence

- centralize Tonight and media palettes;
- migrate Pulse, Orbit, Rooms, Memories, and Tonight to shared typography/spacing/shape/feedback roles while preserving each feature's product identity;
- tighten CI to reject new raw visual constants outside approved theme/palette owners.

## Enforcement strategy

CI enforcement becomes stricter in the same order as migration. Foundations are enforced immediately. Rules that would fail large amounts of intentional legacy code are not turned on until the corresponding migration wave establishes a clean baseline.

The end state should prevent regressions such as:

- raw brand colors in ordinary screen code;
- reusable interactive icons bypassing `ui/icons`;
- arbitrary text scales replacing semantic type roles;
- duplicate page headers/back controls;
- feature-local copies of standard loading/error/empty UI;
- restoration of generic shared-component dumping grounds.

## References used for direction

Nova follows Material 3 as its Compose implementation foundation while using a product-specific semantic layer. The organization of additional foundations is informed by mature public design systems such as Pinterest Gestalt, which treats accessibility, motion, brand expression, color, design tokens, elevation, iconography, layout, messaging, and typography as first-class foundations.
