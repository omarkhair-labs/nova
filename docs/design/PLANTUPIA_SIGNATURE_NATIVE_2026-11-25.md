# Plantupia Signature Experience — native implementation entry
Status: first native design candidate, not final approval. Baseline: app-closure 6075bff (28 commits ahead of consolidated QA #253 at 425800c).
No prototype-only routes, WebView, new backend models, release workflow, billing or deployment changes.

- Add plant: progressive type, name/place, optional real photo. Change type returns to reviewed library; unknown remains explicit. Existing create request and callbacks retained.
- Specimen identity: dedicated identity section preserves staff/QA/self-selected/unknown text; primary action appears before cover photo. No image substitution.
- First arrival: dark acknowledgement with real name only through the existing newlyAddedPlantId branch (returned plant ID must be found in refreshed authorized list). Existing care and dismiss behavior.
- Guide: compact chapter index, expanded illustrated field plate for actual selected reviewed topic, complete instructions/context/source untouched. Illustrations are not botanical measurements.
- History: actual recorded date is carried with the visible image in crossfade, existing earlier/later buttons, pair and full-record framing retained.
- Handover: scan/review/acknowledged connection presentation explicitly distinct, confirmation remains server-owned; no receipt based on mere preview or order.

Acceptance: exact-head Kotlin/Android build and tests; physical 320/360/390/430dp, large text, TalkBack, reduced motion, account switch, camera permission, real upload and persisted plant/care/history; two accounts and actual QR preview/claim/retry/revoke on isolated QA. Source verification is not runtime acceptance.

Next native increments (one protected integrated product, not parallel prototypes): reconcile art assets and official type/wordmark; finish entry/auth/profile and settings; Today, space inventory, all creation modes, care composer and calendar; AI/support/membership/Circle/media/messages/activity; single canonical Shop/cart/checkout/orders and handover; audited role-based Company B2B/B2C/Care and independent website integration. Require accepted real-data screen and error states and exact-head regression evidence before production. No invented release date or production authorization.
