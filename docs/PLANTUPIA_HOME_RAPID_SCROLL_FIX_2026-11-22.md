# Plantupia / Rapid Index scroll flashing and performance QA
Date: 2026-11-22
Scope: Existing public-web home experience, not a standalone lab or new feature.

## Reproduction
User-provided screenshot showed a sudden fullscreen eight-row image list, labelled SELECTED WORK / RAPID INDEX, replacing the regular work presentation.
The original code in CinematicExperience used global scroll velocity: if speed exceeded 1250 px/s, data-rapid became true. CSS then covered the viewport with a fixed Rapid Index and blurred/scaled the underlying project grid. A timeout of about 520–720 ms reverted it.
A local Playwright run at 1440px reproduced rapid=true, overlay visible during fast scroll, rapid=false and hidden after a stop.
This behavior was intentional code, but was unpredictable and disruptive in practice.

## Resolution
- Removed scroll-velocity state updates and full-screen overlay rules.
- Removed the duplicated eight-image Rapid Index markup. Selected Work still has three project stories and eight selectable archive images.
- Reduced Selected Work image DOM count from 19 to 11 without removing user-selectable images.
- Restricted subtle pointer response to the visible Selected Work section and at most one update every 90 ms.
- Replaced expensive large-image filter/scale entry animation with a short opacity-only fade.
- Preserved the + World Index, project choices, real archive, Mazal comparison, source labeling, publication approvals gate, and production protection.

## Verification
- TypeScript local exit 0.
- Playwright at 1440×900 and 390×844: homepage 200, 3 real project chapters, 11 images, no Rapid Index DOM, all fast-scroll samples clean, selectable image changes and loads successfully, no horizontal overflow, no browser JavaScript errors.
- Vercel Preview (authenticated) READY, cloud build passed including TypeScript.
- Preview URL: https://plantupia-public-preview-4x01d67ob-omarkhair70-droids-projects.vercel.app
- Authenticated cloud requests for /, /spaces/projects, /spaces/projects/mazal, /start-a-space and two photo paths all 200.
- Vercel's existing project production domain was NOT re-pointed. The prior timestamped preview 3jm3y39e8 remains immutable and still displays the old behavior.

## Limits
This is a confirmed regression fix, not a measured guarantee of universally higher FPS or faster network performance on every computer. Other site animations and device/browser constraints still require separate profiling if slowness remains after this fix.
