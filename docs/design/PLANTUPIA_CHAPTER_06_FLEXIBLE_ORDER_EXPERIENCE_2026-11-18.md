# Plantupia — Chapter 06: A Natural Order, a Living Receipt
**Date:** 2026-11-18 (Cairo)
**Scope:** Product/UX clarification and execution brief; no live implementation, company tariffs, shipping contracts, or production permission.
**Context:** Supersedes the assumption that automatic shipping-rate calculation, instant stock deduction, or a courier API are mandatory for the normal checkout. Retains the UX quality and technical protections in Chapters 01–05, PRs #316–#320, and the first tracking UI in PR #321.

## 1. Product decision: useful simplicity over excessive automation

The customer should be able to choose a plant, review a visually coherent cart, submit a natural order in one clear action, receive a persistent receipt/reference, and understand what is happening next. The company should be able to process the order through a humane, editable operational workflow. **Do not convert every purchase into a formal quotation chain or a complicated logistics workflow.**

Real-time stock reservation, continuously calculated carrier rates, third-party courier integration, live GPS, ETAs, automated payments, and instantaneous event webhooks are optional capabilities—not prerequisites to a credible UX or a mandate to build now. Design the minimum honest workflow first, reusing the existing commerce records and permission boundaries. Never invent a carrier, a payment, an approval or an actual stock allocation.

## 2. The default customer path

1. **Shop:** Explore products in the authored collection (Chapters 01–04) with real product images, real SKU options and a clear Cart.
2. **Delivery:** Provide recipient, phone and usable address. Map pin is available where helpful, but should not create unnecessary friction or force location permission when the location is adequately specified and company policy allows an address-only order.
3. **Review:** See exact product subtotal and the delivery charge **if the company has supplied an approved price for this address/order**. A simple staff-maintained fee per service zone or merchant-defined flat rate is sufficient; no carrier API or extremely granular calculation is needed by default.
4. **Place Order:** One submission. Show an accurate "Order received" receipt with persistent reference when server has actually stored it. If goods/amount are not yet approved, **do not say payment received, stock reserved, shipping booked or order fully confirmed.**
5. **My Orders:** Persistent readable timeline showing the latest recorded order state, a real timestamp when available, total/fees as confirmed, and the one useful next action.
6. **After delivery:** When the company has recorded actual delivery and plant ownership, eligible living plant items enter My Plants and can open Care Guide/History. Non-plant items never do.

**When shipping fee is known:** present it and the final amount before the one-tap submission (without a staff quotation loop).

**When shipping fee is not known:** do not guess or block ordinary intake needlessly. Label delivery fee as "To be confirmed", show products subtotal and any known components, allow an explicitly provisional order/request to be recorded. Staff can follow up with a real fee. Customer must review/accept any new total **before being charged or treated as having agreed to it**; permit cancellation. Never imply an unknown total is a confirmed final amount.

## 3. Company workflow: human-friendly, adaptable

A new request appears in the existing /company/ order inbox, with recipient/address/products, requested quantities, payment preference, recorded message and shipping-price state.

Staff actions can be simple: **Review → Confirm → Prepare → Out for delivery → Delivered**, plus **Contact customer / Needs attention / Cancelled** where genuinely needed. Staff can record approved delivery fee or availability before confirmation. The company need not automatically reserve/deduct stock on every button press; an inventory allocation/deduction can occur at a deliberate company-defined later step. However, do not mark an item definitively available or confirm a quantity the company cannot fulfil. Preserve consistent order identities, idempotent submission, access checks, and a truthful quantity/stock ledger if used.

Use the existing database and event models wherever feasible. Do **not** introduce a second fake shop, duplicate order database, or public Django Admin workaround. Technical state transitions should match real business decisions rather than a rigid, over-engineered carrier or warehouse flow.

## 4. Tracking: Amazon-like clarity, not imitation of Amazon infrastructure

What the user means by useful tracking is easy visibility into what happened, what is happening now, and what happens next, with clear dates and order reference—**not a compulsory live courier map**.

The initial tracking experience is powered by company-recorded events and can function even if delivery is done by Plantupia's own staff. Show carrier name/tracking number **only when actually recorded**, and never fabricate statuses, stops, GPS, ETA, or notifications. A third-party integration may be added if the company actually uses and approves one.

Visually: compact order header → primary current status → calm vertical progress/history with real timestamps → order items/photos → delivery/contact details → next action. Avoid dense operational jargon, payment/legal controls in the tracking hero, and multiple screens to learn one status. Returning to My Orders must preserve the user's place.

## 5. Chapter 06 moments to design and implement

- **Receipt:** After a successful server write, replace the form with an intentional standalone confirmation moment: received status, reference, visual order summary, and "Track this order" / "Continue exploring". Never merely disable Submit or jump to a generic list.
- **Timeline:** Distinguish "Received, waiting for staff review", "Confirmed", "Preparing", "On the way", "Delivered" using actual authoritative events. Not every order needs the same number of intermediate states; don't invent steps.
- **Exceptions:** Staff may report unavailable items, changed delivery fee or a delivery issue. Present a single understandable decision, accurate revised amounts, and optional contact; no implicit payment or forced acceptance.
- **Connectivity:** On slow network or uncertain ACK, retain reference/input and verify saved order before retry. No duplicate orders from multi-taps. A failed request must not show success.
- **Accessibility/feel:** Real product thumbnail, meaningful copy, accessible taps, large text, focus/back behavior, Arabic/English support as relevant, and consistent Plantupia art direction.

## 6. Acceptance scenarios on isolated QA

1. **Known flat/zone fee:** Staff configures a fictional labelled QA fee; customer sees approved amount and submits once; same order visible in Company and My Orders; staff updates status; customer refresh sees authentic event.
2. **Unknown fee exception:** Customer sees "To be confirmed" before submitting a provisional request; staff supplies fee; customer receives revised total and explicit accept/cancel, without invented payment.
3. **Uncertain availability:** Product can be requested but isn't falsely guaranteed; staff confirms/adjusts or contacts customer, with no phantom immediate physical stock deduction.
4. **Manual delivery:** Staff records dispatch/delivery; timeline updates correctly without courier API, GPS or invented ETA. Eligible plant appears in My Plants only after legitimate delivery provenance.
5. **Reliability:** Repeat tap / interrupted response yields no duplicate logical order; customer A cannot inspect customer B's order; staff cannot access routes beyond role permissions.
6. **Device:** Physical Android small-screen and 200% text review, local QA authenticated customer ↔ staff journey, before owner accepts any Chapter as visually closed.

## 7. Implementation boundary and sequence

- Preserve existing Draft PR #253 integrated base and Chapters #316–#321. This text is a product correction and design handoff, **not an assertion the flexible order-recording path already exists in code**.
- Inspect current `commerce_orders.py`, `commerce_storefront.py`, Company order workspaces, `PlantupiaStorePresentation.kt`, and QA tests to choose the smallest compatible state/API evolution.
- Implement **first** the Order Receipt + Company-driven Tracking experience (Chapter 06) and resolve whether default Place Order can create a genuine `received/pending_review` order without prior staff shipping quote.
- Then simplify checkout's default path, retain fee confirmation only for genuinely unknown cases, and add approved simple fee configuration if needed.
- Do not modify real merchant tariffs, warehouse inventory, customer data, Play production, master or live deployments.
- Beyond Chapter 06, resume agreed sequence: 07 Business Requests → 08 Plant Detail → 09 Care Guide → 10 Care & History → 11 Website Submission → 12 Integrated Acceptance, each with evidence and design review.

**Design principle:** effortless for customer, useful for operator, truthful in every state.

## 8. Implementation progress — first honest request/receipt slice (2026-11-18)

**Committed on QA PR #321 ancestry, not production:**
- `PlantupiaDeliveryQuoteJourney.kt`: authored post-submission request receipt, real reference, server-confirmed item snapshot, subtotal, recipient, delivery status, human next steps and meaningful navigation. Checkout text now says **send order request** with a clearly unconfirmed delivery amount; it does not falsely label it paid or finally purchased.
- `PlantupiaDeliveryQuotePolicy.kt`: strengthen ACK to require line-level correspondence between requested SKU, quantity, unit price and subtotal and the backend-persisted CommerceOrder snapshots. No optimistic success if line data is missing/mismatched.
- `PlantupiaOrderTracking.kt`, `PlantupiaStorePresentation.kt`, `PlantupiaStoreShop.kt`: truth-led `Request received` state, readable company-event history and natural Your orders navigation; no fabricated carrier/ETA.
- Kotlin/JVM and Compose instrumentation fixtures updated to assert the receipt, exact lines, truthful not-yet-confirmed state, buttons and unsafe ACK rejection.

**Important remaining boundary:** The backend still stores the first submission as `quote_pending`, requires a subsequent staff shipping quote plus customer acceptance before `confirmed`, and reserves stock at acceptance. This first slice improves the current real UX; it **does not yet implement one-tap direct confirmed checkout, a merchant-approved known-fee path, optional in-house handover without a carrier reference, or stock workflow flexibility**. Do not portray it as if those exist. Next bounded technical slice must simplify that default company/customer handshake while preserving consent to the actual amount, idempotency and meaningful stock accounting.

**Verification:** exact-head GitHub CI and physical-device visual QA are required; commit writes alone are not a test pass. Keep the stacked PR draft until verified and reviewed by owner.
