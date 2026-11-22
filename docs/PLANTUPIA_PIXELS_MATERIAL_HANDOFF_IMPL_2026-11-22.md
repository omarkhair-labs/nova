# Plantupia — Pixels material handoff / first implementation
Date: 2026-11-22
Branch: design/plantupia-web-refinement-20261122

## Scope and intent
Applied the previously researched HOLD → RECEIVE → SETTLE motion grammar only to the existing Pixels Mall p08/p11 proposal comparison. No global scroll libraries, page-wide filters, independent labs, or public-production release.

## Change
- Both source photos remain registered and stacked, but p08 is a permanently fully opaque foundation (z-index 0). Only the p11 foreground photo changes alpha (z-index 1). This prevents the double-fade that showed the colored plate mid-transition.
- The foreground uses a 0.56s ease-out opacity transition. Both directions work without swapping the DOM image order, so the back-and-forth experience is reversible and doesn't show an empty frame.
- Both images remain source-backed / proposal-labeled. The original options, captions and source-page metadata remain unchanged.
- A selected option whose photo has not finished decoding waits for that photo rather than blanking the frame. A request counter ensures the latest clicked selection wins when quickly tapping back and forth. Failed decoding keeps the last visible photo.
- Reduced-motion setting disables the visual transition. Keyboard activation and semantic pressed states remain active.

## Local QA
- Baseline confirmed old image opacity transitions both at 0.38s with z-index auto on a solid greenish-grey backing.
- Automated test on existing page /spaces/projects/pixels-mall at 1440×900 and 390×844, with normal and reduced motion (4 combinations). In all cases HTTP 200, both source photos loaded, p08 stayed opacity=1 throughout, foreground transitioned to 1 and back to 0 with z-index=1, no horizontal document overflow or browser errors.
- Repeated fast taps selected the latest choice; Enter key changed to the first choice.
- Reduced Motion computed transition duration was 0s; normal was 0.56s. Visual QA screenshots were captured before/at forward and reverse transition. They are private source photo QA, not public image files.
- These tests verify interaction correctness and visible layering. They are NOT a measured guarantee of 60fps, device-level LCP or performance across every browser.

## Rollout decision
Treat this as an authored visual candidate, not a universal motion default. Collect an actual visual reaction to forward/reverse Pixels handoff; then consider applying the same grammar to Hospitality (same-place plant variants), followed by Home and Spaces (different locations must not be falsely morph-matched). Never reintroduce an automatic scroll-velocity overlay. Production remains unchanged.
