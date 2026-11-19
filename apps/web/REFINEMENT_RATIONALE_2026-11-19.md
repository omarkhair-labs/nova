# Plantupia — Whole-Site Refinement Rationale
Date: 2026-11-19

## The rule

The site is not trying to look minimal for its own sake. It is trying to be authored.

Every visible element must do at least one job:
1. reveal source material,
2. explain a spatial or operational relationship,
3. help a person choose or compare something,
4. carry useful intent into a conversation,
5. establish provenance or status,
6. move the visitor into the next meaningful part of the practice.

If an element only hides content that could be read directly, repeats an action already available nearby, or exists because an earlier template expected it, it should be removed.

## Visual grammar, not one template

The site keeps a shared grammar: serif display typography, restrained green/neutral palette, source captions, fine rules, generous but purposeful pauses, and explicit proposal status.

The layouts are not forced to match. Pixels is an architectural sequence. Audi is a chaptered showroom journey. Villa 7 is a spatial atlas of 24 choices. Mazal is a facade wall with a secondary compare tool. EM Sherif holds one vessel constant. El Hallab shows five terrace directions before technical comparison.

Consistency comes from editorial judgement and evidence treatment, not from repeating the same card, tab or grid.

## Interaction rule

Interaction stays only when it creates information or changes a decision.

Keep:
- global Site Index: navigation across the whole world;
- Spaces environment chooser: changes the environment, source study and field questions;
- Spaces / Offices field notes: selected considerations carry into the contact brief;
- Mazal compare: isolates two facade directions after all nine are visible;
- Studio stage navigation: moves through a real four-act design argument;
- Living Systems timeline: distinguishes confirmed, observed, planned and recorded states;
- Shop filters/search/sort: only when a live published catalogue exists;
- Contact modes and form: direct action.

Remove:
- archive filters for only six studies;
- active-project selectors that hide the other five studies;
- Hospitality tabs that hide a three-part guest journey;
- Offices arrangement tabs that hide three composition ideas;
- generic next-chapter panels repeated after pages that already have a clear ending;
- duplicate archive links and self-referential About CTAs;
- commerce controls when there are no live catalogue items.

## Page rationales

### Home / The Experience
Purpose: establish the thesis before explaining the company.
The opening demonstrates space, living material and continuity. The curated visual field proves that the language belongs to actual supplied proposal material. The world gateway maps the practice. The company introduction then names what Plantupia does. Shop is a quiet alternate path, not chapter seven.
Removed the generic next-chapter passage because the Home already contains the world gateway.

### Spaces
Purpose: translate the practice into environments and questions.
The environment chooser is justified because it changes the image, reading and field notes. The six case studies are visible as evidence. The field-note interaction is justified because it carries selected concerns into the project conversation.
A second archive CTA after the case-study archive link was removed as duplicate intent.

### Hospitality
Purpose: show that hospitality planting is a guest journey, not a menu of plants.
Arrival / Mazal → Seating / EM Sherif → Terrace / El Hallab are now visible in sequence. There are no tabs because the three conditions should be compared by scrolling through the same argument.
The process follows the visual journey so operation comes after spatial intent, not before it.

### Commercial
Purpose: explain planting at architectural and brand scale.
Pixels establishes the architectural framework. The five proposal principles are technical design intentions, so progressive disclosure remains defensible here. Audi then shows how the same practice changes at showroom/brand scale. Care follows as an operational layer.
Do not turn this into another project archive; the case studies already own that depth.

### Offices & HQs
Purpose: explain a workplace composition system without inventing a named commission.
Catalogue evidence is used openly because no named office commission is present in the reviewed archive. Individual vessels, grouped planting and linear boundaries are all visible together; they are composition logics, not product tabs.
The field-note interaction stays because it produces a useful brief.

### Project Archive
Purpose: let six proposal studies exist as six bodies of work.
All six are visible in one authored wall. Filters and an active-project panel were removed because filtering six items creates more interface than information.
Context counts remain as orientation, and proposal status remains explicit. Offices appears separately as a service context because there is no reviewed named office project.

### Six Case Studies
Purpose: let the source material determine page behaviour.
No case-study UI exists only for consistency. Every project has a different visual logic derived from its proposal: sequence, chapters, spatial areas, comparison wall, constant vessel or terrace directions.
Source notes distinguish proposal visualization from completed work.

### Shop
Purpose: move from spatial condition to material specification to commerce.
The private material library explains Condition → Vessel → Plant → Finish before transaction. The live catalogue is a separate transactional layer.
When published commerce is unavailable, filters, search, sort and zero-count categories disappear. Empty controls have no job.

### Studio
Purpose: make the design process legible as four acts: Read → Compose → Introduce → Return.
The stage navigation is retained because it moves through a real argument, not a gallery. Proposal evidence changes with the stage. The final proof section shows that the four acts recur in the supplied work without claiming every project follows one rigid workflow.
The generic chapter passage was removed because the page already resolves its own argument.

### Living Systems
Purpose: distinguish physical care from truthful digital continuity.
Physical care is grounded in proposal headings. The interactive timeline then demonstrates the product distinction between confirmed handover, recorded observation, planned check-in and recorded outcome.
The interaction is necessary because state and time are the content. It explicitly avoids fake sensors, diagnosis or automatic proof of completed care.
The generic chapter passage was removed; the page now ends with the actual care conversation.

### About
Purpose: explain the practice, business focus and evidence without repeating Home.
The About introduction now has its own role and no longer links to itself. Primary B2B sectors, the three-part practice and the archive are the evidence structure.
Home introduces the relationship. About names the practice.

### Contact
Purpose: turn intent into a real project/care/general enquiry.
No decorative interaction is needed. Modes change the form context; fields collect only information needed for a response. On protected Preview, sending remains visibly disabled and nothing is stored.

## Reference checks

### Snøhetta / Projects
Reference observation: Snøhetta uses Grid/List/Map/Search plus filters for discipline, typology, location, status and sort across a very large archive. It also exposes project status such as Design Proposal.
Decision for Plantupia: retain clear status/provenance, but do not copy a heavy filtering interface for six studies.

### Green Fortune / Plantwire + Maintenance
Reference observation: Plantwire is explained through the spatial condition it solves, project-specific customization, technical system and maintenance/service relationship in one product story.
Decision for Plantupia: spatial intent, technical specification and care can belong in one authored narrative; they do not need separate UI modes simply because they are different data types.

### Atelier Vierkant / Vasologue + 2026 material studies
Reference observation: the vessel is treated in relation to planting, architecture, landscape, material, colour, texture and customization rather than as an isolated object.
Decision for Plantupia: Shop's vessel/finish layer should read as architectural specification before it reads as a product grid.

## Legacy cleanup

The following unused earlier-generation components and their CSS were removed after confirming they had no live-route callers:
- PrivateArrival / ArrivalOpening
- ProposalImageIndex
- WorldRoute
- LivingAtlas
- SpaceBrief
- ChapterThreshold

The removal is architectural cleanup, not a feature reduction: current routes use the newer authored components.

## Acceptance criteria for this pass

- no broken images or horizontal overflow at 390 and 1440 in the refined route set;
- TypeScript passes after structural deletion;
- no dead catalogue controls when commerce is unavailable;
- no archive UI that hides six already-small studies;
- no repeated generic chapter ending where the page already resolves itself;
- no duplicate CTA to the same destination in the same flow;
- every retained interaction has a written reason above;
- Production remains untouched until publication and imagery gates are explicitly cleared.

## Verified result

Whole-site route regression:
- 17 live routes tested at 320 / 390 / 768 / 1440 px = 68 route/viewport combinations.
- All tested routes returned HTTP 200.
- Zero horizontal overflow in the tested combinations.
- Zero broken loaded images in the tested combinations.

Semantic interaction regression:
- Spaces field note changes the carried contact brief; exactly one archive CTA remains in the main flow.
- Project Archive exposes all 6 studies with no main selector buttons.
- Hospitality exposes all 3 spatial moments with no main selector buttons.
- Offices exposes all 3 composition logics; the 3 field-note buttons remain and carry selected intent to contact.
- Mazal compare was exercised with options 04 and 09.
- Studio navigation changes the active process reading to 03 / ACCESS.
- Living Systems distinguishes the planned Follow-up state from recorded states after hydration.
- Shop hides category/search/sort controls when the published catalogue is unavailable.
- About contains no self-referential CTA in its introduction and points to the Studio process instead.

Code verification:
- TypeScript passes after the legacy component deletion.
- git diff --check passes.
- The local visual review uses the same private-review asset source as the protected Preview workflow.
