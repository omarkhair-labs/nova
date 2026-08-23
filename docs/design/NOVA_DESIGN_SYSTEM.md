# Nova Design System

Status: DS-1 and DS-2 merged; DS-3 final convergence in progress

Nova's product identity is **the social app that remembers your life with your people**. The design system makes every ordinary surface feel like the same product while preserving deliberate feature moods such as Tonight, immersive media and chat themes.

This is a code-owned system, not a redesign. Product behavior, feature ownership and navigation contracts remain intact while repeated visual decisions migrate behind semantic roles and shared presentation primitives.

## Principles

1. **One Nova, multiple moods.** Core navigation, page chrome, feedback, typography, spacing, shapes, icon language and interaction hierarchy are shared. Feature moods are explicit theme variants, not styling drift.
2. **Semantic roles over raw values.** Prefer `NovaType`, `NovaSpacing`, Material-theme shape roles and named palettes over arbitrary local constants.
3. **Shared presentation, feature-owned state.** Repeated buttons, fields, headers, cards, status and feedback are shared; networking, repositories and state machines stay with their features.
4. **Accessibility is part of the system.** Contrast, touch targets, readable hierarchy, content descriptions, state communication and motion restraint are design requirements.
5. **Migration is incremental.** Closed-test stability wins over broad rewrites. CI gets stricter only after a surface has a clean migrated baseline.

## Foundations

Android foundations live in `app/src/main/java/com/nova/app/ui/theme/`:

- `Color.kt` — semantic base color roles and controlled palette override seam.
- `Type.kt` — semantic Nova typography roles mapped into Material 3 `Typography`.
- `Shape.kt` — global rounded shape scale mapped into Material 3 `Shapes`.
- `Space.kt` — 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32dp spacing rhythm.
- `Elevation.kt` — flat / raised / floating roles.
- `Motion.kt` — fast / standard / emphasized duration roles.
- `Theme.kt` — composes the Material 3 theme used by Nova hosts.

Material 3 remains Nova's Compose rendering foundation. Nova owns the semantic brand layer above it.

### Color

Ordinary screens use semantic background, surface, ink, muted, border, accent, accent-soft and danger roles. Raw brand colors should not be introduced into migrated ordinary screens.

Feature-specific palettes are valid only for deliberate modes such as chat appearance, Tonight and immersive media. Those palettes must be centralized in a named owner.

### Typography

Prefer `NovaType` roles such as:

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

### Shape, elevation and motion

The global shape scale is 10 / 14 / 18 / 22 / 28dp. Circles remain valid for avatars and circular actions.

Nova is primarily border-and-surface driven. Elevation is intentionally sparse: flat 0dp, raised 1dp, floating 6dp.

Motion durations are fast 120ms, standard 220ms and emphasized 320ms. Motion communicates state/hierarchy rather than decorating screens.

## Shared component boundary

Shared UI lives in `app/src/main/java/com/nova/app/ui/components/` and stays split by responsibility. A generic `NovaComponents.kt` dumping ground is forbidden.

Stable seams now include:

- `NovaPrimaryButton` / `NovaSecondaryButton`
- `NovaTextField`
- `NovaHeader`
- `NovaBottomBar`
- `NovaIconButton` / `NovaBackButton`
- `NovaAvatar`
- `NovaCard`
- `NovaUnreadDot`
- `NovaLoadingState` / `NovaInlineLoading`
- `NovaEmptyState`
- `NovaErrorState` / `NovaInlineRetry`
- `NovaActiveCallPill`
- shared media/profile primitives

`NovaEmptyState` may expose one feature-owned action when an empty surface has a clear next step. The feature owns copy and behavior; the design system owns presentation.

## Iconography

Ordinary Nova chrome uses the app-owned `NovaIconAsset` catalog backed by bundled Material Symbols Rounded vectors. Calls retain their existing app-owned communication icon seam until separately migrated.

Rules:

1. reusable interactive icons live behind `ui/icons`;
2. ordinary chrome uses one rounded symbol language;
3. Unicode/text glyphs are not used as reusable action icons once a vector exists;
4. feature code does not own reusable icons;
5. meaningful icon actions have content descriptions.

## Navigation and page chrome

Navigation behavior remains owned by Nova's navigation architecture. The design system owns only presentation.

Ordinary screens converge on one bottom-navigation language, one back/icon-button language and semantic title/subtitle roles. Immersive media and calls may intentionally use different chrome.

## Feedback, containers and status

A feature owns its state machine and message copy. Repeated presentation uses shared primitives:

- `NovaLoadingState` — full-page/section loading;
- `NovaInlineLoading` — secondary/pagination loading;
- `NovaEmptyState` — empty state, optionally with one next action;
- `NovaErrorState` — recoverable full-section error;
- `NovaInlineRetry` — compact subsection/pagination failure;
- `NovaCard` — standard bordered ordinary-screen container;
- `NovaUnreadDot` — shared unread marker.

Activity/Notifications, Settings and Profile are migrated consumers. The final DS-3 convergence moves Home feed support states and People discovery onto the same vocabulary.

## Feature moods

### Chat

`NovaChatPalette` is the strongest existing model: semantic background, surface, bubble, text, accent, border and composer roles are centralized per appearance theme.

### Tonight

Tonight remains visually distinct, but DS-4 moves its dark/live colors behind a named Tonight palette rather than raw values spread through composables.

### Pulse and media

Pulse and immersive media may remain dark. DS-4 centralizes their dark-media roles and shared motion rather than forcing them into the ordinary light surface palette.

## Rollout

### DS-1 — Foundations — merged

- semantic typography, spacing, shape, elevation and motion roles;
- Material 3 theme wiring;
- Header, Buttons, TextField and BottomBar migration;
- CI ownership/enforcement.

### DS-2 — Icons and page chrome — merged, ordinary convergence ongoing

- app-owned `NovaIconAsset` catalog;
- Material Symbols Rounded vectors;
- shared `NovaIconButton` / `NovaBackButton`;
- BottomBar, Settings, Profile action and shared Header migration;
- removal of migrated action glyphs and legacy icon imports.

### DS-3 — Feedback and containers — final convergence in progress

Merged work:
- shared loading/empty/error/retry primitives;
- Activity/Notifications feedback and chrome migration;
- `NovaCard` and `NovaUnreadDot`;
- Settings and Profile semantic type/spacing/container migration.

Final candidate:
- Home feed loading/error/empty/pagination and composer container;
- People discovery loading/empty/person cards and core typography/spacing;
- CI baselines protecting those surfaces.

### DS-4 — Alive feature convergence — next

- centralize Tonight palette;
- centralize Pulse/media palette;
- migrate Pulse, Orbit, Rooms, Memories and Tonight to shared typography/spacing/shape/feedback roles while preserving product identity;
- put `NovaMotion` into real animation call sites;
- progressively reject new raw visual constants in migrated feature areas.

## Enforcement strategy

CI becomes stricter in the same order as migration. It protects foundations, migrated chrome, feedback, containers and migrated screens without attempting a fragile global ban over intentional legacy code.

The end state prevents regressions such as:

- raw brand colors in migrated ordinary screens;
- reusable action icons bypassing `ui/icons`;
- arbitrary text scales replacing semantic type roles;
- duplicate ordinary page headers/back controls;
- feature-local copies of standard loading/error/empty UI;
- repeated ordinary card/status primitives;
- restoration of a generic shared-component dumping ground.

## Direction references

Nova follows Material 3 as its Compose implementation foundation and layers a product-specific semantic system above it. Its governance model is informed by mature public design systems such as Pinterest Gestalt, which treats accessibility, motion, brand expression, color, design tokens, elevation, iconography, layout, messaging and typography as first-class foundations.
