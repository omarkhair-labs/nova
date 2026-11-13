# Plantupia Web — Approved Cinematic Direction / Navigation Decision 02
Date: 2026-11-13. PR #300, Draft, English-only.

## Owner review
- **APPROVED:** the cinematic architectural environment with sunlight, materiality, foliage and scene hotspots in the art-direction mockup. This is **visual direction approval**, not approval of a finished 3D asset or deployed interface.
- **REJECTED:** the conventional full header with Vision / Spaces / Process / Projects / Studio, a prominent Let's Create CTA and multiple competing navigation groups.
- **PREFERRED:** Plantupia signature left; centered **+** with purposeful behavior; a concise three-word cluster on the side; scene hotspots retained.
- **DESKTOP FIRST:** treat the desktop composition as its own authored experience. Phones may receive a simplified usable fallback; do not simply shrink the desktop world.

## Navigation logic, prototype hypothesis
- `+` **Experience Index**: a deliberate visual interruption and way into the site, not a hamburger/dropdown. Proposed groups: THE EXPERIENCE, OUR WORK, LIVING SYSTEMS. These names are test labels only; unfinished routes must not pretend to exist.
- `SPACE / LIFE / TIME` on the right: **three ways of reading the current scene**, not corporate section links. The middle control should actually change information and the selected state.
- Hotspots: in-scene points disclose information about light, planting and materials; their roles are distinct from index/scene modes.
- Do not stack three controls that do the same thing (right-side icon dock, top perspective labels and bottom modes) in the finished website. Decide a clear hierarchy after testing.
- The footer/sequence belongs to real scene transformations, not decorative navigation.

## Prototype — deliberately isolated from the public homepage
Route `/study/navigation/` in PR #300 tests control placement, centered +, modal index, three modes and clickable markers. The CSS architectural atmosphere is expressly a **temporary navigation backdrop**; it must **not** be compared to or replace the approved cinematic visual.

The mockup's photographic render currently includes baked-in text and icon imagery and cannot be used as a production interactive background. We need a clean render or independently rebuilt 3D/filmic scene before the first real cinematic interaction.

## Immediate gate before Scene 01 implementation
1. Inspect `+` index open/close, Escape, keyboard focus, English labels and visible selection.
2. Critique whether SPACE / LIFE / TIME are the most meaningful three controls.
3. Confirm the navigation does not compete with the scene and hotspots.
4. Only after that, storyboard **one** before/after scene: architecture-only → with thoughtful planting. No fabricated time-lapse or growth data.
5. Do not add service pages, contact funnels, cart or a full animation engine here.

No `plantupia.com` DNS or production product changes, no merge to #253 and no release promotion.
