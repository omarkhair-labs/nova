# Plantupia — connected QR Handover + company-authored Shop delivery contract (v0.1)

Date: 2026-10-28
Branch: `feature/plantupia-unified-experience-20261028` / Draft PR #253, based on final Codex PR #241 commit `9eb039d`
Status: **IMPLEMENTED AS QA CANDIDATE; NO COMMERCIAL LAUNCH OR OWNER DEVICE SIGN-OFF**

## 1. Handover: what changed, and what cannot change

- Existing Welcome, login/register continuation, encrypted expiring QR intent, camera scanner, manual code, read-only recipient preview, server-side exact account ownership verification and atomic claim are preserved.
- Revised handover first viewport now explains what scan/review does and separates *review details* from *claim*. Confirmation requires a checked recipient/account acknowledgment before any claim request.
- After the server confirms ownership, **the original actual PlantUnit identities** appear within My Plants, not fabricated new customer inventory. No automatic plant creation occurs from a retail order.
- Customers with no Plantupia shipment have a direct, authenticated path to My Plants to add their own private plant and space.
- Only staff can prepare and approve inventory and issue recipient-bound QR; non-plant orders never become plant handovers.

## 2. Company Platform → Shop product family → exact SKU → customer cart

An existing product in `CatalogProduct` is still an individually purchasable **QA SKU**, with an immutable UUID. New additive metadata:
- `sku`: unique code across nonblank values.
- `family_key`: same string across products offering choices for the same family.
- `variant_label`: human-understandable choice, e.g. “Small · Terracotta” or “Medium”.
- `image_description`: cover photograph description for accessibility.
- `CatalogProductPhoto`: ordered approved extra photographs (maximum 8 via staff portal), each with accurate alt text. The existing cover `image` remains supported.

Staff flow: **Company → Catalog → Edit product** for SKU/family/option/cover image; **Manage photos** adds/removes sanitized actual photos. The company operator must have catalog change permission. The ZIP importer accepts validated synthetic rows and now saves the source SKU, but `family_key` and `variant_label` must be edited afterward; an imported ZIP is **not** real retail approval. Uploaded gallery images are delivered via the existing authenticated `catalog/<product_id>/image/?photo_id=<uuid>` endpoint; a photograph is scoped to its own SKU and only active product images can be queried.

Customer flow: **Shop** search+categories → one listing per family → **Product detail** with cover/extra photos, verified description, options (each option maps to a separate product UUID), option-specific price and QA stock → **Cart** persisted by product identity with real authenticated catalogue thumbnails and qty adjustment → **Review QA order** with explicit no-charge acknowledgment → **Order receipt / My Orders / Details** → staff order view with SKU/option snapshot.

A customer's selected `sku_snapshot` and `variant_snapshot` are frozen onto DemoOrderLine. Editing or renaming a product afterward **never relabels** the existing order. Existing server-side product locks, cart fingerprint, idempotent mutations and quantity checks remain. Each real plant eventually needs a separately verified individual PlantUnit identity and recipient-bound handover, regardless of SKU/quantity.

## 3. Honest scope of current implementation

**Implemented in Draft code:** data fields/migration; staff catalogue editing, media sanitation/gallery form and authorization; API SKU/family/variant/gallery fields; family-aware discovery and variant selector; authenticated gallery loading; persisted cart/review/QA order/line snapshots and order detail; QR consent and actual transition to My Plants; test additions.

**NOT implemented, authorized or implied:** licensed real retail product media not supplied by the owner/company; verified commercial price list; live warehouse stock reservation; delivery addresses/carrier tracking; payment provider/refund obligations; conversion of QA orders into *completed* shipments; real commercial end-to-end order → recipient delivery → approved installation → QR. The current order states are only `qa_requested` and `draft_prepared`, and the latter is a staff-owned **DRAFT**, never proof of real fulfilment.

The UI/API still clearly identify QA-only catalogue and zero-charge orders. Do not hide legal/operational limitations by rewriting QA copy as if real delivery existed.

## 4. Acceptance gates before anyone calls this 'final Shop'

1. Exact-head Django migrations check and tests; staff/unauthorized gallery uploads; API photos scoped to SKU; SKU uniqueness; selected option and immutable order snapshots.
2. Exact-head Android unit/lint/architecture/APK/AAB CI; no compilation warnings/error; real responsive checks 320, 390, 430 and tablet dp, 2x text, TalkBack and keyboard. Gallery controls must scroll horizontally rather than overflow.
3. First-time Welcome → QR scan/manual → preview → explicit claim → My Plants, and independent personal plant entry, each on an actual device and with different accounts.
4. Full customer path with two variants of one family, distinct company-authorized photos and prices, cart quantity edits, correct order snapshot in app and company console.
5. Actual brand-approved SKU list, image provenance, model variants, item availability source and fulfilment SOP signed by the owner.
6. Only after genuine commercial business/policy integrations are contracted and end-to-end verified should Shop become an actual production checkout. Separately evaluate billing policy for digital Care plans.

## 5. Regression isolation

All code is incremental to PR #241. PR #253 is the **sole integrated candidate**, not a new app or replacement for the existing Nova messaging, Circle feed, care history, notifications or handover. PRs #247/#248/#250/#251/#252 are review sources; don't independently merge their overlapping changes.

Native build/review and physical user acceptance must occur before merge. No code in this documentation authorizes production Oracle writes, Coolify deployment, Google Play release or real payments.
