# Plantupia courtyard map

Third creative pass for [issue #301](https://github.com/omarkhair-labs/nova/issues/301). The previous pass found a continuous visual subject. It still put an ordinary site frame and stacked sections around that subject. This pass begins with the place and makes its parts the way to reach the rest of the experience.

## Reference briefs

### Penderecki’s Garden — navigation through a place

The garden places distinct chapters in named physical settings and offers chapter links alongside entry and previous/next controls. The visitor can choose an order and still understand where they are. Its gardens and soundtrack are biographical material. An equivalent Plantupia soundtrack would need its own material and purpose. The design test here is whether a visitor can open a plan in any scene, see their current position and go directly to a different place. The live feature previously loaded the invitation in Chromium, but this pass’s full headless run stalled during loading. The published chapter links and the Awwwards selector exhibit support the structural reading. [Live garden](https://www.pendereckisgarden.pl/en) · [Awwwards Site of the Day and chapter selector](https://www.awwwards.com/sites/pendereckis-garden)

### Nomadic Tribe — the encounter is the interaction

Makemepulse gives its four chapters different encounters in a world suited to this particular story. A conventional block layout would change how a visitor notices the environment. Plantupia applies that principle to one actual decision at a time. The in-image doorway changes viewpoint, the light/movement/care controls move attention in the courtyard and the time controls change which visit the reader can see. Watching a forced transition or mastering controls would slow the visit to a company site. The live WebGL experience timed out in the headless run. The studio case study and awards record document the chapter construction and recognition. [Studio case study](https://www.makemepulse.com/case-study/nomadic-tribe) · [Awwwards record](https://www.awwwards.com/sites/nomadic-tribe)

### Of the Oak — one object stays legible as the view changes

The guide uses one tree to connect seasons, meditations and the species around it. The introduction exposes a skip action and the guide has direct public paths. Plantupia keeps the olive as a point of orientation, first seen through the arch, then considered spatially and revisited in care. No scientific scan or field record is borrowed. The interaction test is whether entering Approach on the same image reads as a continuation, and whether the record describes the same bed, passage and tree. The original guide and studio case study were accessible as documents. The interactive live pass did not finish in headless Chromium. [Original field guide](https://oftheoak.co.uk/) · [Lusion project](https://lusion.co/projects/of_the_oak/)

### Bruno Simon — destinations are encountered in space

The current portfolio invites the visitor to drive between actual destinations and exposes an Unstuck action. The earlier portfolio won the 2019 CSS Design Awards Website of the Year. A driving interface makes sense for a developer demonstrating 3D and physics. Requiring a visitor to steer around a courtyard would make contact and orientation harder. Plantupia uses the world's signs as regular links, whose physical positions have meaning. The test is whether any destination opens in one activation from the map with touch or keyboard. The live DOM exposed the action labels and portfolio instruction. The headless 3D scene did not finish loading. [Live portfolio](https://bruno-simon.com/) · [CSS Design Awards winners](https://cssdesignawards.com/blog/website-of-the-year-2019-winners/330/)

### Kaizen, Open Garden and the preceding Plantupia studies

Kaizen lets different kinds of interaction coexist within one authored story. Open Garden uses environmental transitions to let visitors look around a subject. The dedicated Weleda deployment failed to resolve and Kaizen displayed its loading state during the browser pass. The available case studies show the design mechanisms. Plantupia already tested an isometric atlas, a plus-menu and a cinematic opening. The most transferable decisions were free chapter entry, controlled movement and a relationship worth moving for. Neither an ordinary service directory presented as a dark modal nor a photographic carousel gives that relationship spatial structure. [Kaizen case study](https://www.makemepulse.com/case-study/kaizen) · [Weleda case study](https://www.makemepulse.com/case-study/weleda-the-open-garden) · [Living Atlas research](./PLANTUPIA_WEB_LIVING_ATLAS_RESEARCH_AND_INTERACTION_V02_2026-11-13.md) · [Previous courtyard pass](./PLANTUPIA_WEB_COURTYARD_REVISION_2026-11-15.md)

## Experience specification

- Start at the threshold. A sign inside the frame moves the visitor toward the tree instead of scrolling down to another module. Returning changes scale from architecture to the person and plant.
- Keep a map within reach. Its illustrated plan holds the drafting table, the central olive, growing places, an open terrace and a place to write. Its six positions open addressable routes. The mobile map rotates the plan and keeps labels readable.
- Give each destination its own action. Spaces changes the visible environment on focus or hover and opens it on activation. Individual places have a fold-out observation. Approach places each condition at its own point inside the courtyard. Aftercare changes the visit through the URL and supports Back. Contact is a worktable for a message the visitor keeps.
- Preserve direct paths and recovery. Native links and a JavaScript-disabled directory provide access. The map restores focus when it closes. Reduced motion presents destinations without movement. There is no requirement to pan, drag or finish a chapter.
- Keep the evidence boundary. The map is an imagined limestone model generated for this pass using the built-in ImageGen tool. The prompts described a ninety-degree tabletop model with a central olive, a lower threshold and adjoining rooms. It is published as `apps/web/public/field/courtyard-map.webp`. The map, photographs and visits depict concepts. A project archive or company contact channel requires approved material and destination.

The review is isolated to draft PR #302 and the website feature branch from issue #301.

## Courtyard continuity

The threshold, planting and return views have native fragment addresses. Browser Back and Forward follow the visited views. The map leads back inside the courtyard from Approach and to the return view from Aftercare. On the courtyard itself, opening the map keeps the current viewing position. The map and view controls preserve direct links and work without a JavaScript history layer.

Approach places three keyboard- and touch-operable controls over the shadow, the passage and the planting bed. The markers are placed in the photograph's coordinates and move with its cover crop. At phone widths the picture fades into the note so the controls sit on the photograph and remain clear of the text. The optical targets follow the image but still behave like native radios and links.

## The contact worktable

The heading, fields and review occupy a single sheet on the table. A project note includes the chosen space, location and timing. A care note can include a location and a planting or site reference. Each topic keeps its own in-page message. Name, email, phone and organisation are optional reply details shared across topics. An entered email is checked by the browser and the message is required.

Review uses the same sheet and lets the visitor edit without losing the draft. Copy writes to the clipboard, with text selection as a fallback. The form stays within the browser and no message or reply details are sent or stored by Plantupia. The map sits above the sheet on phones so it remains outside the form’s touch targets.
