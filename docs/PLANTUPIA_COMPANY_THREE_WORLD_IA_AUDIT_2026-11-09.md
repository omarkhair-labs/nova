# Plantupia Company — Three-world Information Architecture & Operational Audit
Date: 2026-11-09
Scope: Isolated preview code based on PR #253 at \`f650137\`. This is **an interaction/IA slice**, not a commercial go-live, payment or backend-model redesign.

## Product thesis

The Company opens as **three clear work contexts** rather than a permanent list of unrelated backend modules:
1. **B2B / Projects** — RFQ enquiry, installations, physical verified plant units, reviewed QR handover.
2. **B2C / Commerce** — Products, Orders, Operations (sellable stock).
3. **Living Care** — approved plant guidance, inspection plans, service conversations.

The entrance uses three distinct, illustrated **native HTML \`details\` cards**, each with a single workspace action and a short actionable indicator. The icon is static; the card content reveals with subtle motion. The full sidebar is absent on the entrance; individual spaces get scoped secondary navigation and a return to the three-space entrance. Accessible summary/keyboard behavior is native. \`prefers-reduced-motion\` disables animated reveals.

Bilingual-ready *information hierarchy* is introduced through English primary labels with correct short Arabic labels marked \`lang=ar dir=rtl\`. This is **not** a completed Arabic translation: all workflows, validation errors, longer prose and full RTL responsive layout still require a dedicated locale pass, shared terminology and native-device sign-off. Do not claim a full bilingual release.

## Current application to real user-job mapping

| User job | Working route(s) | Verified implementation boundary |
| --- | --- | --- |
| Orient / select space | \`/company/\` | Entry only; no new commercial records |
| B2B project operations | \`/company/projects/\` | Installed-site draft/review/claim counts from stored installations |
| Ask/answer a business RFQ | \`/company/projects/rfqs/\` | Short nonbinding QA enquiry & reply; no versioned contracted quotes/PO |
| Prepare a physical installation | \`/company/installations/\` | Staff draft -> review -> READY, not inferred from retail order |
| Record installed plant evidence | \`/company/inventory/\`, installation import | Physical PlantUnit identity distinct from CommerceSKU; XLSX/CSV and reviewed preview/apply |
| Issue reviewed, recipient-specific QR | \`/company/handovers/\` | Only after approved installation and proper recipient review |
| B2C grouped entry | \`/company/commerce/\` | Products, Orders, Operations are separate jobs |
| Create commercial product / SKU | \`/company/commerce/products/\` | Internal draft, independent cover photo, SKU variants and prices; not an approved/published retail offer |
| Receive QA order | \`/company/commerce/qa/requests/\` | Customer scoped QA intake, statuses, address and pin, no payment/dispatch |
| Record stock count | \`/company/commerce/stock/\` | Stock location + append-only count ledger; no reservation/fulfillment |
| Open legacy sample catalog | \`/company/catalog/\` | \`CatalogProduct\`, synthetic-only QA |
| Import demo catalog ZIP | \`/company/catalog/import/\` | Feature gated by \`PLANTUPIA_QA_CATALOG_ZIP_IMPORT_ENABLED\`; ZIP includes \`products.xlsx\` + \`images/\`; only legacy \`CatalogProduct\`, not \`CommerceProduct\`. Limit 100 products / 20MB ZIP |
| Review legacy sample orders | \`/company/orders/\` | \`DemoOrder\`, no-charge QA; separate from new \`CommerceOrderRequest\` |
| Care library | \`/company/care/\` | Botanical species, references, draft/review/publication; not automatic diagnosis |
| Inspection plan | \`/company/plans/\` | Staff proposals and reviews, QA bounded |
| Service conversation | \`/company/support/\` | Ticket assignment, customer messages and staff responses |
| QA access/entitlements | \`/company/demo-access/\` | Test plans; not paid recurring billing |

## Deliberate information architecture decisions

- **B2C top level = Products / Orders / Operations.** The old QA catalog and demo orders remain accessible under *Testing tools*, but are not presented as equal production workflows. No data migration or destructive merging is part of this slice.
- Product creation has a single commercial editor route; existing SKU and stock forms remain where users expect them. A later slice will give product detail a single photo/variants/inventory workspace.
- Orders surface their current QA status honestly. There is **no hidden payment collection**: COD and manual transfer are preferences, not settlement.
- B2B installation inventory and retail warehouse stock are **different things**; no implicit SKU -> individual physical plant conversion.
- All records still use their server-enforced staff permissions, customer ownership, audit, idempotency and recipient-bound handover. Do not replace these with front-end-only access checks.
- Keep \`/company/catalog/import/\` intact until **commercial** bulk import is implemented, tested, reconciled and released. It cannot be rebranded as the new commercial importer.

## Necessary follow-through — full product, not an MVP claim

**Catalog unification:** \`CommerceProduct\` remains canonical for real commercial draft, SKU and stock. Implement reliable XLSX/CSV + ZIP image import into these models with image gallery, SKU/variant mapping, row-level actionable preview, duplicate/update strategy, resumable review and explicit publication. Treat the previous fictitious importer as test fixture only.

**Commercial fulfillment:** order confirmation, actual reserved and released inventory, shipping serviceability/quotes and delivery proof, COD/transfer reconciliation, return/refund, then reviewed physical installation/QR when necessary. Never infer money movement or delivery from QA status.

**B2B lifecycle:** organization/contact identity, scope/sites, RFQ attachments, quote versions, approvals/contract, staffing/schedule, project deliveries and living aftercare.

**People and language:** team/workspace roles in the portal, notification center and work queue, full Arabic RTL and English language settings, Arabic copy review, screen reader and keyboard testing.

**Visual sign-off:** 320/390/430 px and wide desktop, reduced motion, keyboard-only links, content 200% zoom, Chrome/Firefox/Safari as applicable, realistic product/photos, empty states, and real-device Android->Company end-to-end evidence.

## Scope / acceptance for this specific navigation PR

- Three entry \`details\` open through keyboard/pointer and link to real existing workspaces.
- No permanent full sidebar on home; secondary navigation shows only the active world.
- B2C home presents Products / Orders / Stock & Operations, with original QA tools nested but available.
- Care receives its own real home, not a fourth disconnected backend.
- Existing company permissions and state transitions unchanged; no changes to production, database or migrations.
- Existing automated backend tests plus new navigation tests pass before considering merging into Preview #253.
