# Plantupia — Native material scroll / first whole-site pass
Date: 2026-11-22
Branch: design/plantupia-web-refinement-20261122
Status: candidate refinement to the existing site. Preview only; public production unchanged.

## Desired sensation
Comfortable continuous visual reading during scrolling, **not slower scrolling**. Wheel, touch, keyboard and trackpad motion stay native. Selected architectural photos move very slightly within established frames. Captions, navigation, product data and precise comparison geometry stay stable.

## Implementation
- CSS native View Timelines with progressive enhancement; no external smooth-scroll library, JS scroll handler or altered wheel behavior.
- A stationary photo frame defines each timeline and the child image animates. Independent CSS translate and scale properties preserve existing transform-based hover/focus behavior.
- Desktop source photographs: ±8px, scale 1.045. Mobile: ±5px, scale 1.04. Editorial media frames: desktop ±5px and mobile ±3px, without scale.
- Reduced-motion mode disables the motion entirely; browsers without View Timeline support retain the original static layout.
- Scope: Home selected work, Spaces source case cards, Project Archive, six source photo leads and next-case images, ShopBridge and material journal. No content hiding or new menus.
- Studio already has its own section reading behavior, so its staged image does not receive an extra competing animation. Pixels/Mazal/Hospitality precise alternative comparisons remain spatially registered and unchanged.

## Local verification
- 6 representative routes × 2 widths (1440px / 390px) × 2 motion settings = 24 browser cases.
- A fixed 470px wheel gesture produced 470px document scroll on the checked routes; no interception or added wheel delay.
- HTTP 200 throughout and no horizontal overflow or page JavaScript errors.
- Distinct source image translate values before/after scrolling confirmed actual view-linked motion in Spaces, Projects archive, Pixels case and Shop material area.
- On Reduced Motion, computed animation-name and translate returned none. No content disappeared.
- Home and Shop contain subtle editorial frame motion. Studio intentionally remains unchanged.
- This is a functional/browser correctness check, **not a 60fps or Core Web Vitals certification on a mid-tier phone**.

## Technical references
- MDN https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/animation-timeline/view
- Chrome https://developer.chrome.com/docs/modern-web-guidance/guides/ui-behaviors/scroll-entry-exit-effects
- web.dev https://web.dev/articles/rendering-performance/

## Release boundaries
Only authenticated Preview may carry protected source imagery. Public production stays on the previous verified version and separate publication approval, inbox and product-commerce checks remain in effect.

## Editorial next test
Judge the feeling while scrolling Home -> Spaces -> a case page. If the effect feels decorative, reduce its magnitude. If it feels faint, refine the image staging, **not** the user's native scroll speed. Keep the previously fixed Rapid Index overlay disabled.
