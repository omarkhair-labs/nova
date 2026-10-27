# Plantupia — CEO pilot: one connected customer journey

**Scope:** isolated Oracle PR #241 preview and side-by-side Android QA build only. Never merge into/publish Nova production as a side effect of this pilot.

## One-sentence product
Plantupia delivers plants with a verified care plan; after a recipient-bound QR handover, customers see their own plants and today's care, and can optionally ask AI/the team or shop.

## Source Atlas (existing implementation; do not duplicate)
- `backend/plants/inventory_import.py`: strict four-sheet XLSX / preview / authorized atomic import. Sheets: Metadata, Species, CareGuides, PlantUnits. The Metadata batch_id and space_id must match a **staff-owned DRAFT installation**. This importer does not ingest photos or care inspection plans.
- `backend/plants/company_views.py`, `company_urls.py`: installations, plants, botanical guides, review, import/template, recipient handovers.
- `backend/plants/handovers.py`: recipient-specific, one-time handover; no public bulk plant claiming.
- `backend/plants/demo_models.py`, `demo_services.py`, `demo_api.py`: proposal/reviewer-approved inspection plans, access offers, AI entitlement, human support, catalog, cart and **no-charge QA** orders.
- `backend/plants/assistant.py`: Groq gpt-oss-20b; Oracle QA direct model and call_provider verified after explicit HTTP User-Agent.
- `app/src/main/java/com/nova/app/feature/plants/PlantupiaDemoJourney.kt`: customer care, optional access, shop/cart/orders; UI preview simplified in commit 5a986a2. Care history and existing social functionality stay untouched.

## End-to-end CEO rehearsal (acceptance sequence)
1. **Company account:** authorized operator opens an installation DRAFT and downloads its unique template. Never use local-only database seed commands on hosted Oracle.
2. **Inventory:** copy three researched pilot species/guide/unit records into the matching installation template; inspect import preview and explicitly approve. Source links and drafts are real, but botanical approval is not implicit.
3. **Actual photos:** operator photographs each tagged, physical installed plant (portrait hero crop, natural background, no fake transparency); associates photos via existing media UI. Wikimedia reference photos are *reference only*, never proof of a customer's exact specimen.
4. **Botanical review:** independently validate plant identity, publish approved guide version and reviewer-approve context-aware *inspection* proposals. For watering, a due date says 'check soil', not 'water regardless of moisture'.
5. **Recipient:** create/login with a separate non-staff customer account; authorized operator issues an exact-recipient QR invite; customer claims plants and sees only that installation.
6. **My Plants / Today:** all three real names/photos render; due soil/leaf checks display; completing a check adds history and computes the next due date; relaunch preserves state.
7. **Plantupia Care:** care and history work without a subscription. Optional AI Care / Expert Care shows concise benefits, clear provisional prices and a single no-charge preview notice. Customer activates, asks live Groq about a particular owned plant, and receives a contextual answer only after explicit opt-in.
8. **Human help:** Expert Care creates a consented support ticket; staff respond via the existing Nova-backed private support conversation; customer sees the actual reply after relaunch.
9. **Shop:** list/photographs/prices → product detail → cart → **no-charge QA order** → My Orders → staff order panel. No live payments, fulfillment or inventory reservation is implied.

## Visual direction
- **Navigation:** Today / My Plants / Shop / Community; assistance contextual inside plant, not a new parallel app. Preserve the current four native roots where possible.
- **Today:** one high-priority task, then a short queue; prominent 'Check soil' / 'Done' actions.
- **Plant:** actual photo as hero; name, location, next check; 'Care' / 'History' / 'Ask Plantupia' obvious.
- **Subscription:** two calm tier cards; concise price, 1–2 differences, one CTA; keep preview/no-charge disclosure legible but not repeated in every paragraph.
- **Shop:** generous real photography, compact names/prices, a clear primary add-to-cart button; no internal terms like FICTIONAL QA in consumer titles (the preview notice persists).
- **Typography:** one editorial headline size, simple sans-serif UI. Warm off-white / forest green / muted sage. No synthetic gradients, fake specimens, sticky technical debug copy or dense stacked cards.
- **Empty/error:** human labels ('Switch to a customer account', 'No orders yet') and one recovery action. Staff accounts cannot perform customer purchases or activate customer care.

## Photo and horticultural sources used in pilot kit
- RHS Monstera: https://www.rhs.org.uk/plants/swiss-cheese-plants
- RHS Spider plants: https://www.rhs.org.uk/plants/spider-plants/growing-guide
- RHS Sansevieria: https://www.rhs.org.uk/plants/sansevieria/growing-guide
- Verified photo references/license metadata are in the separately prepared pilot photo shotlist; photographs of actual delivered specimens remain to be captured.
- Product/UX pattern reference: Planta's task schedule https://apps.apple.com/us/app/planta-plant-garden-care/id1410126781 ; Bloomscape product listings https://bloomscape.com/shop/ ; Bloom's photo-first plant collection https://bloomsprout.app/ . **Borrow principles, not screenshots/assets/layouts verbatim.**

## Current QA status (2026-10-27)
- Oracle Preview #241 migrations through plants 0011, isolated QA database host verified.
- Groq API live chat succeeded; app-native `call_provider` succeeded.
- Two QA offers and three **fictional preview-only** catalog entries created in hosted QA.
- No three-plant researched batch has been imported yet; identities, reviewer approvals, handover QR, real specimen photos and customer-native end-to-end session remain **unverified**.
- New Native UI surface edit is in PR #241; release requires green CI + new hosted QA build & device walkthrough.

## Exit criteria
CEO can complete the above in a single narrated screen recording on the same QA environment and a real Android device; evidence verifies imported plant IDs, authored review state, QR claim, persisted care event, live AI entitlement, actual staff reply, and cart/order. All QA charges must remain disabled.
