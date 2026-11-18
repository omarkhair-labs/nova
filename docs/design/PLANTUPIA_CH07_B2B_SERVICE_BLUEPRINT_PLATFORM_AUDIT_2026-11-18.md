# Plantupia · Chapter 07 / Service blueprint × Company Platform audit
**Working research + bounded QA implementation · 18 November 2026 · Draft / not visually approved**

## What is being redesigned
Existing Plantupia is not a green-field ERP. Preserve one native Android app, the same authenticated B2B commercial inquiry, the Company Platform, and existing installation / QR / care truth. Do not create a second customer B2B database, a new checkout, or a mock workflow just to make screenshots. Chapter 07 must improve *both sides of the same service*: how a business customer describes a need and how Plantupia employees receive, qualify, quote, follow up, install and hand over verified physical plants.

**Cross-cutting scope for Chapters 07–12:** examine the Company Platform as a coherent operating tool, including B2C, B2B, Care/Support and shared Website Inbox. A beautiful screen is not acceptable if it requires avoidable navigation, repeats manual entry or invents an operation. Conversely, evidence/permissions/financial and physical handover gates may not be removed merely to reduce clicks.

## Audited source of truth (QA PR #322 ancestor)
- `app/src/main/java/com/nova/app/feature/plants/PlantupiaBusinessInquiryScreen.kt`: identical form structure for wholesale and installation, quantity required and defaulted to "1"; generic one-line success; list of earlier requests under the same form.
- `backend/plants/commerce_business_real_api.py`: authenticated `BusinessInquiryInput`; mandatory positive quantity, minimum 20-character scope, optional email and product SKU.
- `backend/plants/commerce_business_real.py`: `CommerceBusinessInquiry` creation and idempotency, statuses `received/quoted/accepted/declined`, manually authored quote with a revision. Accepted is only permission for further commercial conversation.
- `backend/plants/commerce_business_real_company.py`: real business inbox, status filter, quote and decline endpoints; received-only pending counter; action to create linked installation from accepted quotation.
- `backend/plants/templates/plants/company/commerce_business_inquiries.html` and `commerce_business_inquiry.html`: rows display contact, city, kind and quantity, but not next recommended staff task.
- `backend/plants/commerce_business_project.py`: staff explicitly creates one linked draft installation and Space, only after the customer accepts an installation quote. It does not populate physical plants or issue QR.
- `backend/plants/company_operations.py` / `operations_projects.html`: B2B home has independent enquiry, QA RFQ, installation, plant inventory, QR tools; user must infer the project's next stage.
- `backend/plants/templates/plants/company/base.html`: role-scoped B2B/B2C/Care navigation with Website Inbox as shared channel. Preserving these distinctions is good, but global work attention and cross-record context require refinement.
- `backend/plants/commerce_models.py`: B2B enquiry has no owner/next-contact/attachment/site-visit fields and no binding signed-contract evidence. These are genuine missing contracts; never fake them in presentation.

### AS-IS service blueprint: business installation
1. Customer selects 'Installation', fills generic organisation/contact/city/**required quantity**/free-text scope, sends authenticated request.
2. API creates inquiry, status `received`; Android replaces neither form nor location of focus, merely appends a generic feedback sentence and refreshes list.
3. Staff navigates B2B home → Business requests → a record; decides to quote or decline; writes one exact EGP amount + 15–800 character note.
4. Customer receives a nonbinding quoted record and explicitly accepts follow-up; the inquiry becomes `accepted`. No binding contract, stock hold or confirmed sale.
5. Authorized staff goes back to accepted record and explicitly creates one draft installation. Staff still needs physical plant manifest, review and recipient-bound QR handover.
6. Employee moves between Installations, Installed Plant Inventory and QR tools; all exist for valid reasons but actions lack a continuous, project-contextual guide.

### Measurable friction hypotheses (not measured user-study findings)
- **P0 clarity:** installation projects may have no meaningful item count at first contact; forcing `quantity = 1` risks fictitious precision. Requires data-contract review before changing validation.
- **P0 outcome:** successful business request has no independent verifiable receipt composition; generic feedback obscures the reference and next action.
- **P1 employee action:** the inbox count only covers received enquiries, not accepted quotations requiring follow-up. Quoted enquiries correctly wait on the customer and should not appear as needing staff quotation.
- **P1 project continuity:** Workspaces show distinct tools (legitimate RBAC boundaries) but don't sufficiently answer 'what is the next task for this particular request?'
- **P1 model gap:** no employee owner/next activity, contract artifact or visit record; can't claim there is a scheduler, assigner or signed PO.
- **P2 presentation:** overlapping QA names and nonbinding disclaimers repeat in ordinary operating views; keep truthful warnings, consolidate them into contextual disclosures instead of making every row hard to scan.
- **P2 usability:** mobile form and desktop staff workspace need independent treatment. Do not copy website editorial animations into a task-first staff admin.

## External research — mechanisms, not borrowed product art
| Evidence | Verified operating approach | Relevant lesson / limitation |
| --- | --- | --- |
| Ambius commercial interior landscape | consultation/site understanding → design / proposal → approval → physical installation / service | Plantupia installation is a scoped service; an early 'unit count' is not always the first useful question. No evidence these companies share Plantupia's database or contract process. |
| Planteria Group | consultation / site visit → design and planting solution → agreed work → installation → maintenance | The service is longer than a form; keep one project reference across commercial and physical phases. |
| Odoo Sales 19 quotations | quote with price and terms; quotation acceptance can produce a sales order | Inspiration for explicit quotation content and stage transition, **not** permission to infer Plantupia has signed or accepted a binding PO. |
| Odoo Project 19 | distinct, customizable project task stages and dependencies | Next actions should reflect actual handoff and physical dependencies, not arbitrary UI tabs. |
| HubSpot CRM pipeline rules / follow-ups | stage-dependent required information and owner-assigned next tasks | Use contextual actions and follow-up visibility; do not auto-create an unrecorded staff task. |
| Shopify order management | status/fulfillment work queues and per-order timeline | B2C staff should see real work needing attention instead of translating raw statuses, while finance evidence stays separate. |
| Baymard form UX | eliminate irrelevant inputs, distinguish required vs optional fields, facilitate error correction | Use the smallest honest form; do not hide fields that matter for safety or fulfillment. |
| NN/g service blueprint | customer, frontstage, backstage and support lanes; compare as-is with to-be | Evaluate customer and employee friction together; validate with real staff observations later. |

**Exact source URLs:**
- https://www.ambius.co.uk/interior/interior-landscaping/
- https://www.planteriagroup.com/office-plants/
- https://www.odoo.com/documentation/19.0/applications/sales/sales/sales_quotations.html
- https://www.odoo.com/documentation/19.0/applications/services/project/tasks/task_stages_statuses.html
- https://knowledge.hubspot.com/object-settings/set-up-and-customize-pipelines
- https://knowledge.hubspot.com/tasks/create-tasks
- https://help.shopify.com/en/manual/fulfillment/managing-orders/order-status
- https://baymard.com/research-articles/checkout-flow-average-form-fields
- https://www.nngroup.com/articles/ux-mapping-cheat-sheet/

## TO-BE candidate / service, not a signed-off layout
**Customer:** choose Wholesale vs Design & Installation → minimum accurate brief (company, contact, locality, what is needed) → optional context and estimated quantities when known → review → server ACK with immutable reference → follow status/quote → explicit follow-up acceptance.

**Employee:** B2B 'needs attention' queue distinguishes `received` (review and prepare quote), `quoted` (customer decision pending), `accepted` (commercial follow-up / if installation and permissions, prepare a linked draft project), `declined` (read-only history); the *one request record* shows stage, source, scope, latest quote and one contextual next step. Quote decision remains human. Linked installation carries through to real manifest, approval, handover and care.

**Backstage:** no fabricated contract, automatic stock deduction, phantom scheduled site visits, AI quote, customer reply, GPS or plant IDs. Keep actual RBAC and immutable snapshots, idempotent mutations, real audit, evidence-led handover.

### Company Platform IA comparison — under review, not frozen
**A · Keep each tool silo as equal primary navigation (current):** safe explicit permissions; more context switching and duplicated mental work.
**B · One universal CRM / single status for every record:** superficially simple; conflates B2C orders, B2B project offers, installations and support, and risks security/financial mistakes.
**C · Recommended candidate: role-aware three operations areas, plus cross-cutting attention and record continuity.**
- **B2C · Commerce:** Catalog/Publication, Stock, Orders, Pricing/Delivery, Returns & Finance according to permissions. User first sees *what needs action*.
- **B2B · Projects:** Enquiries/Quotes, active projects, verified manifest and handover. One linked job context; actions take the employee to existing specialized tools, not a second datastore.
- **Living Care:** reviewed species / guides, care follow-ups, private service support. Staff sees only permissioned data, not private customer history by default.
- **Shared:** Website Inbox, staff, notifications/roles, settings, audit. Site submissions can be triaged to appropriate place **only when explicitly mapped**, without duplicate manual entry.
- One consistent visual/navigation grammar: dense but legible editorial hierarchy; contextual metadata, one strongest task action, gentle state feedback, desktop efficient tables, mobile narrow screens, keyboard/touch/accessibility. No generic AI dashboard and no decorative metrics.

## Chapter 07 implementation gate
**Safe now (determinable from existing truth):**
1. Server-authored incoming business request receipt (verified ID/kind/company/contact) and customer next step; no unrelated process claim.
2. Staff business inbox grouped by actual statuses with contextual next actions. Read-only `quoted` and `declined` remain visible but are not positioned as staff quotation work.
3. Staff detail context panel states next legitimate action and current waiting party; accepted installation can link to its existing draft (with permission).
4. Permission/identity tests for views and genuine transitions.

**Must be designed/researched before data migrations:**
- Is project quantity optional/unknown? Separate `target units` from wholesale ordered quantity; permit no estimate for installation without silently storing `1`.
- Should quotation contain line items/scope/inclusions/exclusions/expiry and signed terms? What constitutes a legally binding purchase order? Human and legal approval required.
- Company task owner, notes, reminder and due-date semantics; do not make the UI claim automatic customer communications.
- Who conducts site survey, verifies quantities, approves plant schedule, uploads floorplan, signs installation proof? No speculative integrations.
- Color/typography/component system for both B2B and B2C workspace from actual QA screenshots and staff use; current review is a source-code audit, not visual device acceptance.

### Acceptance for this chapter
- Customer success visible only after validated API acknowledgment; persisted record ID; retry ambiguity never displays fake completion; app account/session isolation maintained.
- No numeric placeholders invented as actual site quantity in any newly written record. The current mandatory quantity contract remains marked open until approved schema change.
- `received` staff knows 'quote'; `quoted` staff knows 'wait'; `accepted` staff knows 'follow up / create draft only with permission'; `declined` no action.
- Existing B2B authorization, quote version and idempotency tests remain green; unauthorized staff cannot leak record details or create installations.
- Company workspaces remain B2C/B2B/Care separate, and QA-only legacy tools do not become production workflow.
- CI + emulator/physical device walkthrough + visual review of actual screenshots before calling the design 'approved' or releasing anything.

**The new B2B changes are stacked on the still-draft QA PR #322. Nothing here merges into master or deploys production.**

## Company Platform / actual shell redesign slice — 18 November 2026

**Problem verified in source:** Previously `base.html` replaced the entire sidebar according to B2B/B2C/Care section. The Company overview had three strong destinations, but inner pages didn't preserve that mental map. The Website inbox section fell back to a generic hub-like menu despite being shared work. The B2C pricing editor was grouped under the Orders active nav marker. Early QA tools competed with real workflows. The existing look had a 244px dark sidebar, a large decorative sidebar note, repeated page heading and numerous large workspace cards.

**Architecture choice (implemented in Draft):** *One stable workspace switcher*, then *only relevant local tools*. Users may switch directly between permitted workspaces without backing out to the hub. On each inner page the compact breadcrumb shows Workspaces / Current workspace / Current page or record. A business reviewer without installation rights sees the B2B request list, not misleading inventory/project actions. Website Inbox has its own shared state rather than pretending to be a B2B/B2C section. Existing role checks in `service.require()` stay the authorization source of truth; menu visibility alone is never access control.

**Visual and interaction direction (code candidate, needs screenshots):**
- Quiet, dark green *navigation rail*, not full-screen marketing cards. Cream highlight for the active workspace, a restrained line marker for the actual page. Real text labels, small bilingual supporting names; keyboard-focused and accessible `aria-current` and `nav` landmarks.
- Primary navigation has **three persistent areas** when staff permission allows: B2B Projects, B2C Commerce, Care & Support. Website Inbox is shared, not repeated inside every section. Admin/QA-era views live behind a clearly secondary disclosure.
- Current-area tools directly underneath: **B2B** overview, requests/quotes, installations, verified plants, QR; **B2C** overview, products/publication, orders/fulfillment, stock, delivery prices; **Care** overview, library, customer support. Nonexistent integrations have no fake navigation items.
- A real business request/page starts with *actual next recorded action*, not status trivia. The company home and B2B/B2C pages surface actionable records before utilities. Status counts are real, derived and role scoped.
- At desktop the shell has a ~264px rail; at tablet/mobile workspaces become compact always-visible choices and local actions wrap below, rather than an inaccessible hover or third level of nested menus. Reduce motion appropriately.
- Company workspace should optimize for repeated daily tasks: controlled density, clear tables, consistent form labels, quiet empty states, real metadata. Brand/editorial is present but subordinate to legibility and speed.
- Don't claim the redesign is visually accepted until actual QA at ~360, 390, 768 and 1440 CSS px plus 200% text, screen reader/focus order, staff-role views and screenshots.

**Reference mechanisms checked:** Shopify Admin navigation/search and order roles: https://help.shopify.com/en/manual/shopify-admin/shopify-admin-overview ; HubSpot's separate CRM/Sales workspaces: https://knowledge.hubspot.com/help-and-resources/a-guide-to-hubspots-navigation ; NN/g menu and local navigation: https://www.nngroup.com/articles/menu-design/ and https://www.nngroup.com/articles/local-navigation/ ; breadcrumbs: https://www.nngroup.com/articles/breadcrumbs/ ; progressive disclosure: https://www.nngroup.com/articles/progressive-disclosure/ . No vendor design copied, no user UI study yet.

### Implementation checks and outstanding user choices
- Changed `company_views.page` + `templates/plants/company/base.html` + `company.css`, with role-aware switcher, context labels, real QA/production environment text, breadcrumbs, and semantic active state. Added regression tests for staff vs finance-only navigation and shared inbox.
- B2B Projects home now leads with the genuine actionable enquiry queue; existing install/project records remain visible but QR/physical inventory stay independent.
- B2C Commerce home now leads with actual confirmed order preparation and exceptional requests; products, shipping rates and stock remain separate verified operations.
- **Unresolved design decisions:** Whether desktop staff should be able to collapse the rail; compact list vs detail split-view for records; default grouping when a person has B2C + B2B + Care rights; what notification counts are most urgent; whether company will assign owners/due dates. These require observational QA, not arbitrary inventing of data.
- **Release gate:** no migration or Production changes. Test results, real device/admin viewport review, staff feedback and owner sign-off remain distinct from merely having code on a Draft PR.
