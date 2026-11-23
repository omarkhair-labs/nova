# PLANTUPIA — THE POCKET FIELD
## Mobile Edition / Creative direction, visual references and release brief
Version: 2026-11-23 / Owner: Plantupia web experience / Branch: design/plantupia-web-refinement-20261122

### The commission
The phone is not a reduced desktop. It is the closest distance between a visitor's hand and Plantupia's materials: leaf, vessel, courtyard, weathered architectural surfaces, time and care. A visitor should feel like they are opening a field journal, not a conventional service website. This is an authored **mobile edition** of the same real Plantupia site, with the same real proposal imagery, company material, commerce API and enquiry API.

### One sentence
**A living place, held in the hand.**

### What must be felt
- **First second:** a photograph with presence; a decisive editorial headline, not a giant list of CTA chips or a looping gimmick.
- **First choice:** two equally understandable doors — see the work, explore the places — without guessing what a plus icon means. Existing World Index remains and becomes legible.
- **Mid-journey:** images read at their own aspect ratio, with calm technical captions. Changes between chapters are intentional, not gratuitous cinematic transitions.
- **Last movement:** a real action — contact the team about a project, choose an environment or explore genuine published products. Nothing asks for fabricated image permissions or a hidden approval.
- **Overall rhythm:** one continuous Plantupia voice across 16 page types. Large typographic gestures are followed by quiet metadata; dense text is broken by real evidence, never decorative placeholder art.

### Distinctive mobile art direction
- **Palette:** shaded forest (#183a2c), leaf mineral (#566e59), sunlit paper (#f0efe7), warm stone (#e6e5db), soft rule (#aebbaa). Reserve colour for editorial contrast, not status badges.
- **Type:** expressive, tightly tracked serif for stories and captions in a restrained grotesk / monospace for evidence. Avoid microscopic sub-10px explanatory prose; minimum practical reading size 13-16px.
- **Image:** company proposal images in original proportion (contain and editorial white space). Full bleed is reserved for atmospheric home/sector scenes where crop is knowingly acceptable. Never swap actual imagery with AI placeholders.
- **Composition:** paper edges, running folio numbers, asymmetric but measured frames, alternating full-bleed and inset material panels, vertical whitespace with purpose. No fake iPhone chrome.
- **Movement:** 180-500ms material crossfades. Touch scroll remains native. No scroll-jacking, no auto-playing music/video, no blocking intro, no forced onboarding, no parallax that enlarges source evidence or causes sideways page scroll. Reduced-motion respects user preference.
- **Navigation:** the World Index is an atlas, not a hamburger. A legible INDEX trigger; an image-led list and a clear route back. In case studies, a compact chapter/reading strip makes section jumps visible and available without hiding the article. All targets genuinely exist.
- **Conversion:** one careful invitation to talk, no unsolicited modal, no premature product purchase where backend publishes no products.

### Reference library — principles, not copies
The following are external inspirations. "Award" labels apply only where an award source verifies that recognition:
1. BAT Architecture — Awwwards Honorable Mention. Reference: photograph-led mobile portfolio with a restrained way to change the collection. https://www.awwwards.com/sites/bat-architecture
   Transfer: coherent architectural image framing and quiet index, not the same look.
2. Yurdaer Architecture — Awwwards Honorable Mention. Reference: project lists and detail pages can have different editorial roles while sharing identity. https://www.awwwards.com/sites/yurdaer-architecture
   Transfer: project body as narrative, not repeated templates.
3. Unanime — Rezo Zero case study. Reference: generous mobile typography, strong first impression, hand-held category imagery. https://www.rezo-zero.com/projects/unanime/
   Transfer: confidence in negative space and clear mobile entry.
4. WE—ON / projects — Awwwards inspiration. Reference: information-dense buildings receive readable metadata after imagery. https://www.awwwards.com/inspiration/we-on-design-buildings-clean-and-minimal-website-navigation
   Transfer: sequence photography → description → site facts, not UI mimicry.
5. Hammerli & Caccia / Maloja — Awwwards inspiration. Reference: material detail photography and stacked facts on a mobile case. https://www.awwwards.com/inspiration/projects-hammerli-caccia-architects
   Transfer: unobtrusive evidence labels and true source hierarchy.
6. Arqui9 / N4 Studio — 25 July 2025 Awwwards Site of the Day (credited by the studio). https://www.n4.studio/au/feed/arqui9-wins-awwwards-site-of-the-day
   Transfer: cinematic media must retain mobile performance.
7. Awwwards Mobile Excellence Guidelines: responsive readability, tap target sizing, image compression, form simplification, native mobile usability. https://www.awwwards.com/mobile-excellence-guidelines.pdf

These references are **not** a claim that Plantupia has won awards or that any design here is a copy of an awarded piece.

### Journey map and bespoke treatment
| Visit | Screen / route | Role in story | Unique phone behavior |
|---|---|---|---|
| 01 | / | Arrival / first impression | full-bleed photographic field, three scenes, real next doors; touch controls + optional horizontal stage swipe |
| 02 | World Index | Global atlas | chapter portraits + titles, clear CLOSE, original + symbol and INDEX label, 44px+ touch zones |
| 03 | /spaces | Places | one-hand horizontal sector selector, large true-photo frame, live explanatory text |
| 04 | /spaces/hospitality | From arrival to atmosphere | three framed moments, photographic rhythm, service sequence |
| 05 | /spaces/commercial | Designed destination | Pixels architecture image, clear design-principle accordions, Audi sequence |
| 06 | /spaces/offices | Working life | catalogue-based compositions, real field note selection and contact hand-off |
| 07 | /spaces/projects | Evidence archive | edge-to-edge sources, project sectors, six real case entrances, no fabricated completion labels |
| 08 | /spaces/projects/mazal | Nine facade directions | source wall of nine, registered two-way comparison, readable per-family passage |
| 09 | /spaces/projects/em-sherif | Seven planting characters | one constant navy vessel, all seven visible, rhythm of captions and care |
| 10 | /spaces/projects/el-hallab | Five terrace moods | five visual directions then native-horizontal comparison table |
| 11 | /spaces/projects/pixels-mall | Built-environment landscape | panoramas without forced crops, source-led design principles |
| 12 | /spaces/projects/audi-cairo | Living showroom | six separate spatial moments, palette and continuing care |
| 13 | /spaces/projects/villa-7 | Residence | 24 source directions grouped by space, visible interior index, full photographs |
| 14 | /studio | The practice | four process chapters, selectable authentic steps, source photography |
| 15 | /living-systems | After installation | four time/record states, planned vs recorded distinction, mobile-native timeline |
| 16 | /about | The company | photo first, practice and B2B sectors, archive entry |
| 17 | /shop | The collection | product catalogue only when real SKUs exist; visual material library otherwise, full-width detail sheet |
| 18 | /start-a-space | The conversation | choose project/care/general, readable single-column enquiry, no false send success |

### Interaction decisions
- Every case study has a **mobile reading index** linking genuine section IDs. This is a navigation affordance, not a static graphic.
- Photo panoramas retain intact composition. Gallery groups can scroll horizontally inside their own footprint only where a comparable group exists; no horizontal document overflow.
- On /shop, product filters must operate on source-backed live catalogue only, no QA fixture. Empty commerce has editorial alternative content, never fake SKUs.
- On /start-a-space, restore input values when a send fails; don't disable the send control unless backend unavailable; don't add new publishing consent gates.
- Keep the native browser Back button, normal anchor links, visible focus, semantic buttons and descriptive alt text.
- Never silently relabel proposals as completed installations.

### Engineering constraints
- Desktop remains independent and unaffected where mobile visuals change.
- Maintain the public preview route mapping and company assets available from the existing Vercel release. Preview may proxy approved existing public media, but this is a temporary release bridge, not asset ownership.
- Keep true back-end credentials server-side; do not expose secrets to client code.
- No external design library, cookie banner, consent approval, new auth gate or mandatory PWA install introduced by this brief.
- System motion preferences and touch accessibility take priority over visual effects.
- Targets: 320, 360, 390, 430, 768, 1024 and 1440 widths; 44px minimum interactive targets; no root horizontal overflow; all main routes HTTP 200; route navigation and forms tested; original proposal imagery loads on preview.
- Release is only accepted after source build and browser acceptance pass, plus live preview route and asset smoke tests. A CI pass is not a substitute for artistic review on an actual phone.

### Delivery evidence
Commit SHA, one reproducible Vercel preview URL, browser acceptance run link and failures corrected are part of this brief. This is the decision record so that future chats do not invent additional approval steps or rebuild the same page from scratch.
