# Plantupia / Chapter 05–06 — One-step checkout at a known delivery price

**Status:** Draft implementation on PR #322, stacked on PR #321; QA only, no production approval.

## Product decision

The normal customer should choose objects, enter the delivery address, see **items + approved delivery fee = final total**, and place one confirmed order. The company receives the order to prepare and deliver; it does not quote each normal order. No external courier API, live driver map, automatic payment processor or instant physical stock deduction is required.

### How delivery rates work

Company authorized staff can set an active, non-negative EGP fee for each city using the existing Company dashboard: **Customer orders → Delivery prices**. A missing or disabled city rate is **unknown**, never zero. Actual fees are not seeded or fabricated. The checkout reads an authoritative fee against the current persisted cart; changing a rate increases its revision. Already placed orders retain their saved shipping fee and total.

City input is deliberately a simple exact, case-insensitive key: free-text spelling variants can differ. This is a bounded first implementation, not a claim of carrier routing or zone coverage. Before launch, the company must verify and approve names, coverage and rates; future controlled city selection can remove spelling ambiguity.

### One normal order

1. Customer's authenticated cart has current SKU/price/availability.
2. Customer enters contact/address and confirms a map pin (preserving existing QA data contract).
3. Android requests `GET commerce/delivery-price/?city=...&cart_fingerprint=...`.
4. When an active city fee exists, review shows the exact subtotal, delivery fee and total, and `Place Order`.
5. `POST commerce/orders/direct/` includes existing idempotent cart/customer payload plus the displayed rate revision. Django locks the user and city-rate record; independently recomputes the cart, checks item prices/current inventory, creates a confirmed order at the saved fee, and records `order_placed`.
6. Inventory is **held against overselling**, not physically deducted. The actual stock movement is recorded by the existing fulfillment operation at real dispatch. Payment is not inferred or charged.
7. Customer sees the confirmed order reference, persisted items, total, and evidence-backed tracking. Staff sees a normal confirmed order ready for preparation.

If the city has no active rate, the customer can still send the older, clearly labelled **delivery confirmation request**. That request is not a paid, confirmed or reserved sale; an explicit customer agreement follows any proposed total. A failed price lookup never silently becomes the fallback — it requires a retry.

## Invariants and safety

- One canonical `CommerceOrder` and `CommerceCartLine`; no demo store or duplicate record system.
- All amounts and inventory availability come from the server. Clients cannot submit a fee directly.
- Stale rate revision or changed cart rejects purchase; the customer must review the new total.
- Customer + client-id retries are idempotent, same body only.
- Company price management requires staff permissions and writes an audit entry.
- No freight carrier, arrival time, GPS, payment confirmation, or delivery proof is invented.
- Staff still operates fulfillment, returns and payment verification through existing restricted workspaces.

## QA acceptance

- Missing rate: backend returns no fee; direct order rejects; customer sees honest exception path.
- Published city rate: customer sees fee and total; `Place Order` creates exactly one confirmed order and one `order_placed` event.
- New revision after price display: old checkout submission is rejected; no old price is silently accepted.
- Retry on network uncertainty: no duplicate orders or reserved units.
- The active stock hold prevents overselling; physical stock count does not decrease until actual dispatch.
- Zero EGP is valid *only* when explicitly entered by authorized staff, never as a missing-rate default.
- Other customers cannot access or mutate the order; unauthorized users cannot edit rates.
- Run Django tests, Android unit/build and emulator tests; inspect real device at narrow and large-text sizes, with two distinct customer accounts and staff.

## Release gate

Do not merge or deploy until CI is green, Company rates have been reviewed with real coverage/prices by Plantupia, QA migrations and persistence have been checked, and customer-to-staff-to-delivery journeys pass on real Android devices. No real commerce data has been changed by this PR.
