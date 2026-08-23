# Nova Design System

Status: **DS-1 through DS-4 complete**

Nova's product identity is **the social app that remembers your life with your people**. The code-owned design system now gives the product one shared visual grammar while preserving deliberate feature moods such as Tonight, immersive Pulse/media, Memories and chat themes.

This consolidation is intentionally **not the final visual redesign**. It establishes the system that a later identity pass can safely reshape from a small number of semantic owners instead of changing hundreds of local UI constants.

## Principles

1. **One Nova, multiple moods.** Core navigation, page chrome, feedback, typography, spacing, shapes, icon language and interaction hierarchy are shared. Deliberate moods are named palettes, not styling drift.
2. **Semantic roles over raw values.** Use `NovaType`, `NovaSpacing`, Material-theme shapes, `NovaMotion` and named feature palettes instead of arbitrary local values.
3. **Shared presentation, feature-owned state.** Repeated UI presentation is shared; repositories, networking and state machines remain feature-owned.
4. **Accessibility is part of the system.** Contrast, touch targets, readable hierarchy, content descriptions, clear state communication and restrained motion are system requirements.
5. **Immersive exceptions stay explicit.** Calls, media viewers, Memory Film and other immersive surfaces may use feature chrome when their interaction model requires it; that is not permission for ordinary-screen drift.

## Foundations

Android foundations live under `app/src/main/java/com/nova/app/ui/theme/`:

- `Color.kt` — semantic base roles and controlled palette override seam;
- `Type.kt` — semantic Nova typography mapped into Material 3 `Typography`;
- `Shape.kt` — global rounded shape scale mapped into Material 3 `Shapes`;
- `Space.kt` — 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32dp rhythm;
- `Elevation.kt` — flat / raised / floating roles;
- `Motion.kt` — fast / standard / emphasized durations;
- `Theme.kt` — composes the Material 3 implementation foundation.

Material 3 remains the rendering engine. Nova owns the semantic product layer above it.

### Color

Ordinary screens use semantic background, surface, ink, muted, border, accent, accent-soft and danger roles. Migrated ordinary surfaces must not introduce raw brand colors.

Deliberate moods have named owners:

- `NovaChatPalette` — chat appearances;
- `TonightTheme.live` — live-night atmosphere;
- `PulseTheme.media` — dark Pulse cards and immersive viewer;
- `MemoryTheme.ready` — reflective/ready memory presentation.

Changing a future brand direction should happen in these owners first, not inside individual composables.

### Typography

Use `NovaType` roles: `display`, `pageTitle`, `screenTitle`, `sectionTitle`, `title`, `body`, `bodyCompact`, `subtitle`, `button`, `label`, `meta`, `micro`, navigation labels and `badge`.

### Shape, elevation and motion

The global shape scale is 10 / 14 / 18 / 22 / 28dp. Circles remain valid for avatars and circular actions.

Nova is border-and-surface driven. Elevation stays sparse: flat 0dp, raised 1dp, floating 6dp.

Motion roles are fast 120ms, standard 220ms and emphasized 320ms. `NovaMotion.standard` now drives real size/state transitions in the alive-feature layer; future animation work should reuse these roles rather than add arbitrary timings.

## Shared component boundary

Shared UI lives under `app/src/main/java/com/nova/app/ui/components/` and remains split by responsibility. A generic `NovaComponents.kt` dumping ground is forbidden.

Stable seams include:

- `NovaPrimaryButton` / `NovaSecondaryButton`;
- `NovaTextField`;
- `NovaHeader`;
- `NovaBottomBar`;
- `NovaIconButton` / `NovaBackButton`;
- `NovaAvatar`;
- `NovaCard`;
- `NovaUnreadDot`;
- `NovaLoadingState` / `NovaInlineLoading`;
- `NovaEmptyState`;
- `NovaErrorState` / `NovaInlineRetry`;
- `NovaActiveCallPill`;
- shared media/profile primitives.

`NovaEmptyState` may expose one feature-owned next action. Features own copy and behavior; the design system owns repeated presentation.

## Iconography and page chrome

Ordinary Nova chrome uses the app-owned `NovaIconAsset` catalog backed by bundled Material Symbols Rounded vectors.

Rules:

1. reusable interactive icons live behind `ui/icons`;
2. ordinary chrome uses one rounded symbol language;
3. Unicode/text glyphs are not reusable ordinary action icons once an app-owned vector exists;
4. meaningful icon actions have content descriptions;
5. immersive/content symbols may remain feature-owned where they communicate media or event meaning rather than ordinary navigation.

Navigation behavior remains owned by the navigation architecture. The design system owns its presentation. Ordinary screens converge on the shared bottom bar, back/icon-button language and semantic title hierarchy.

## Feedback, containers and status

A feature owns its state machine and message copy. Repeated presentation uses:

- `NovaLoadingState` — full-page/section loading;
- `NovaInlineLoading` — secondary/pagination loading;
- `NovaEmptyState` — empty state, optionally with one next action;
- `NovaErrorState` — recoverable full-section error;
- `NovaInlineRetry` — compact subsection/pagination failure;
- `NovaCard` — standard bordered ordinary-screen container;
- `NovaUnreadDot` — shared unread marker.

Migrated consumers include Activity/Notifications, Settings, Profile, Home, People and the ordinary Rooms list surface.

## Alive feature convergence

### Tonight

The night presentation is centralized in `TonightTheme.live`. `TonightSurface` consumes that palette plus semantic type, spacing, shape and motion roles. The 6 PM / 6 AM product behavior remains feature-owned and unchanged.

### Pulse / media

`PulseTheme.media` owns dark media background, ink, muted, overlay and panel-border roles shared by Pulse cards and the full-screen viewer. Pulse rail/viewer use semantic type/shape/spacing and Nova motion without changing the 12-hour/chain behavior.

### Orbit

Orbit keeps its social-motion content language while its cards, typography, spacing, shapes and state-size motion use the Nova system. Event symbols remain content semantics rather than ordinary navigation icons.

### Rooms

Rooms rail uses shared cards and semantic roles. The All Rooms screen uses `NovaBackButton`, shared loading/empty/retry states and `NovaCard` while conversation/group ownership remains unchanged. The deeper Room interaction surface is intentionally left feature-owned for the later visual identity pass rather than refactored during closed testing.

### Memories

`MemoryTheme.ready` owns the deliberate dark reflective state used by Memories entry surfaces. `MemoriesRail` uses the named palette, shared cards, semantic roles and motion. Memory Film/rendering remains an immersive feature boundary and is not structurally rewritten by the design-system consolidation.

## Completed rollout

### DS-1 — Foundations

- semantic typography, spacing, shape, elevation and motion roles;
- Material 3 theme wiring;
- shared Header, Buttons, TextField and BottomBar migration;
- foundation ownership enforcement.

### DS-2 — Icons and page chrome

- app-owned `NovaIconAsset` catalog;
- Material Symbols Rounded vectors;
- shared `NovaIconButton` / `NovaBackButton`;
- BottomBar, Settings, Profile action and shared Header convergence;
- CI protection against migrated legacy icon/chrome drift.

### DS-3 — Feedback and containers

- loading/empty/error/retry primitives;
- `NovaCard` and `NovaUnreadDot`;
- Activity/Notifications, Settings, Profile, Home and People convergence;
- CI baselines protecting migrated ordinary surfaces.

### DS-4 — Alive feature convergence

- named Tonight, Pulse/media and Memories palettes;
- real `NovaMotion` adoption;
- Orbit, Rooms and Memories entry-surface convergence;
- CI gate for alive-feature palette/system ownership.

## Enforcement

Nova CI protects the clean baselines in the order they were migrated. It rejects regressions such as:

- raw feature colors returning to migrated Tonight/Pulse/Memories entry surfaces;
- migrated alive surfaces bypassing semantic typography/shapes/motion;
- ordinary Rooms screen restoring its legacy text back control or local feedback copies;
- reusable chrome bypassing the app-owned icon catalog;
- shared component ownership collapsing into a generic dumping ground.

The gate is deliberately scoped instead of banning every literal across the entire codebase: media dimensions, one-off product geometry and deep immersive screens may still need specific values. The later visual identity pass can decide which of those should become additional semantic roles.

## What comes next

**The design-system plumbing is complete. The next phase is visual identity, not another refactor phase.**

The next discussion should answer what Nova should unmistakably look and feel like: brand palette, visual signature, density, typography personality, icon personality, navigation expression, motion character, card language, media treatment and how Tonight/Pulse/Rooms/Memories should feel related while remaining distinct.

Once that direction is chosen, the existing semantic owners let us apply it coherently rather than redesigning screen-by-screen.

## Direction references

Nova uses Material 3 as its Compose implementation foundation and layers a product-specific semantic system above it. Its governance model is informed by mature public design systems such as Pinterest Gestalt, which treats accessibility, motion, brand expression, color, design tokens, elevation, iconography, layout, messaging and typography as first-class foundations.
