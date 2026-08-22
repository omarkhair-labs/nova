# Nova Design System

Status: DS-1 and DS-2 merged; DS-3 feedback rollout in progress

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
- `NovaIconButton` / `NovaBackButton`
- `NovaLoadingState` / `NovaInlineLoading`
- `NovaEmptyState`
- `NovaErrorState` / `NovaInlineRetry`
- `NovaAvatar`
- `NovaActiveCallPill`
- shared media/profile primitives

The remaining consolidation waves add or normalize shared seams only where repeated usage proves the need:

- badge/status presentation;
- confirmation dialogs and bottom sheets;
- shared card/container roles;
- explicit feature palettes for deliberate presentation modes.

## Iconography policy

Ordinary Nova chrome now uses an app-owned icon catalog backed by bundled Material Symbols Rounded vectors. Calls keep their existing app-owned communication icon seam while they migrate independently.

Rules:

1. reusable interactive icons are referenced through `ui/icons`;
2. ordinary chrome uses one rounded symbol language;
3. Unicode/text glyphs are not used as action icons once an app-owned vector exists;
4. feature code does not become the owner of reusable icons;
5. content descriptions are mandatory for meaningful icon actions.

## Navigation and page chrome policy

Navigation behavior remains owned by the existing navigation architecture. The design system owns only visual chrome.

Target consistency:

- one bottom-navigation visual language;
- one standard page-header/back pattern for ordinary screens;
- explicit exceptions only for immersive media, calls, and other full-screen experiences;
- unread badges and selected states use shared tokens/components.

DS-2 established the app-owned bottom-navigation icons plus shared `NovaIconButton` and `NovaBackButton`. Activity/Notifications is the first large legacy surface being migrated onto that ordinary-screen chrome.

## Feedback and system messaging policy

Loading, retry, empty, success, warning, and error states communicate through a small consistent set of patterns. A feature owns its message copy and state machine; the design system owns repeated presentation.

Current shared feedback primitives:

- `NovaLoadingState` — full-page/section loading;
- `NovaInlineLoading` — secondary or pagination loading;
- `NovaEmptyState` — ordinary empty state;
- `NovaErrorState` — recoverable full-section error with retry;
- `NovaInlineRetry` — compact subsection/pagination failure.

Notifications/Activity is the first migrated consumer because it exercises loading, follow-request loading/error, full-page error, empty, pagination loading/error, and ordinary page chrome in one real feature.

## Feature moods

Feature-specific presentation is valid when it is deliberate and centralized.

### Chat

`NovaChatPalette` is the current strongest example: each chat theme defines semantic background, surface, bubble, text, accent, border, and composer roles and can provide a controlled Nova color override.

### Tonight

Tonight may remain dark and visually distinct, but its palette should move behind a named Tonight palette instead of raw colors spread through composables.

### Pulse and media

Pulse/media dark surfaces may remain visually distinct, but dark media roles should be explicit and reusable rather than local constants repeated between viewer/card implementations.

## Rollout plan

### DS-1 — Foundations — merged

- established spacing, shape, elevation, motion, and semantic typography roles;
- wired shape and typography into `NovaTheme`;
- migrated the stable Header, Button, TextField, and BottomBar seams;
- added architecture enforcement for foundation owners.

### DS-2 — Icons and page chrome — merged / convergence continuing

- established the app-owned `NovaIconAsset` catalog;
- migrated bottom navigation, Settings, Profile settings action, and shared header/back chrome;
- removed action-oriented Unicode glyphs from migrated surfaces;
- established shared `NovaIconButton` and `NovaBackButton`.

Ordinary legacy screens continue converging as they are touched; Activity/Notifications is included in DS-3 because it is also the first comprehensive feedback migration.

### DS-3 — Feedback and containers — in progress

- establish shared loading/empty/error/retry primitives;
- migrate Notifications/Activity as the first complete consumer;
- establish repeated card/container/status roles;
- migrate Settings, People/Profile, Home support states, and dialogs/sheets in bounded follow-up PRs.

### DS-4 — Alive feature convergence

- centralize Tonight and media palettes;
- migrate Pulse, Orbit, Rooms, Memories, and Tonight to shared typography/spacing/shape/feedback roles while preserving each feature's product identity;
- tighten CI to reject new raw visual constants outside approved theme/palette owners.

## Enforcement strategy

CI enforcement becomes stricter in the same order as migration. Foundations and migrated chrome are enforced now. Feedback primitives and Activity/Notifications are enforced as soon as their migration lands. Rules that would fail large amounts of intentional legacy code are not turned on until the corresponding migration wave establishes a clean baseline.

The end state should prevent regressions such as:

- raw brand colors in ordinary screen code;
- reusable interactive icons bypassing `ui/icons`;
- arbitrary text scales replacing semantic type roles;
- duplicate page headers/back controls;
- feature-local copies of standard loading/error/empty UI;
- restoration of generic shared-component dumping grounds.

## References used for direction

Nova follows Material 3 as its Compose implementation foundation while using a product-specific semantic layer. The organization of additional foundations is informed by mature public design systems such as Pinterest Gestalt, which treats accessibility, motion, brand expression, color, design tokens, elevation, iconography, layout, messaging, and typography as first-class foundations.
