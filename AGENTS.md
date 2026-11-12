# Repository instructions — Nova → Plantupia

This is the existing Nova product being transformed into Plantupia **in place**. It is **not** an independent app or a greenfield rebuild.

For any Plantupia-related task, read:
1. [Project context, decision provenance and status](docs/PLANTUPIA_CONTEXT_INDEX.md)
2. [Implementation contract](docs/PLANTUPIA_PRODUCT_CONVERSION.md)
3. [Research index on the separate public research branch](https://github.com/omarkhair70-droid/Plantupia/blob/research/foundation-source-atlas-20261007/docs/README.md)

The linked research was conducted *before* a public website + client Android product direction was selected. Do **not** mistake earlier hypotheses for commitments. Explicitly distinguish research, known company/product direction, implementation proposals, and verified code.

## Hard rules
- Work on reviewed feature branches/PRs; do not silently push to `master`, merge, run migrations against Oracle, or publish Google Play.
- Keep the **existing** Play `applicationId = "com.omarkhair70.nova"` for the intended app-listing update. Do not rename the Android application/package identity without explicitly revisiting that plan.
- Preserve the existing Nova Android/Kotlin, Django/PostgreSQL, social, media, messaging, realtime, safety and auth engines where appropriate; do not replace with a new Flutter/Supabase app.
- Source user account + customer space access from the server. Plant and customer IDs must not grant access on their own.
- A personal plant name, botanical species, and internal inventory label are different concepts.
- Private customer spaces, care history, addresses and photos are **never** published to Circle, Posts, Stories or Reels automatically. Public publishing requires an explicit audience and consent with server-side enforcement.
- Never manufacture healthy/unhealthy percentages or inferred diagnoses without evidence. Separate observation, measurement, report and inference.
- Do not require physical serial/QR to log in. An internal UUID is sufficient for initial records; QR is optional and needs a use-case.
- Do not ship a full ERP, an AI diagnosis engine, carbon claims, or IoT hardware integrations just because they appeared as research or offer possibilities.
- Do not commit private meetings, chats, user/client personal information, unpublished company materials, internal brand-PDF copies, Oracle/Play credentials, .env files, or signed artifacts to this **public** repository.
- Run existing architecture tests, Django migration/check/security tests and Android tests; report precisely what passed, failed, or has not been tested.
- A PR alone does not publish to Oracle or Google Play. Existing auto-deploy configuration needs explicit verification; no release change is implied by a green CI.

## Product priority
1. Commercial client with a Plantupia-installed or Plantupia-managed living space.
2. Track living spaces and installed plant/planting units, personal naming, event/photo history and meaningful care over time.
3. Recontextualize Nova community engines around plant experience and help: Posts, Stories, replies, media, messaging; preserve report/block/moderation.
4. Company internal UI starts with small Django Admin; web creative site is a separate surface sharing the brand narrative, not a copy of the Android app.
5. iOS is out of the current implementation scope.

If a future agent cannot access the linked public research or privately supplied approved brand assets, say so and ask; **do not invent their contents**.
