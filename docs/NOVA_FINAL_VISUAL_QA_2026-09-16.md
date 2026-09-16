# Nova Final Visual QA — 2026-09-16

## Goal
Close the remaining visual-system gaps after the Nova identity rollout without changing product semantics, APIs, navigation, or persistence.

## Findings addressed
- People discovery still exposed generic Material progress indicators during refresh and pagination.
- Follow/unfollow actions still used a generic spinner instead of Nova presence motion.
- Followers/Following screens had bespoke loading and retry presentation instead of the shared Nova feedback system.
- Secondary buttons had no shared branded busy state, which encouraged text-only loading treatments.
- Create used an orbit-ring avatar as generic decoration even though the core Nova identity is interaction/presence rather than astronomy.

## Changes
- Use `NovaPresenceIndicator`, `NovaLoadingState`, `NovaInlineLoading`, `NovaInlineRetry`, and `NovaErrorState` across People and social-graph feedback states.
- Add optional `enabled`, `busy`, and `busyText` support to `NovaSecondaryButton`, matching the primary-button behavior.
- Replace the decorative orbit-ring treatment in Create with a quiet avatar surface.
- Keep the single-ring Home avatar treatment as an intentional live-presence seam (`showLivePoint = true`), not as the primary Nova symbol; the Shared Void brand mark remains the Home brand mark.
- Keep all existing discovery filters, follow behavior, pagination, navigation, publishing, Orbit product behavior, and backend behavior intact.

## QA contract
CI must pass the Android architecture gates, unit tests, lint, debug/instrumentation/release builds, release bundle, and backend suite before merge.
