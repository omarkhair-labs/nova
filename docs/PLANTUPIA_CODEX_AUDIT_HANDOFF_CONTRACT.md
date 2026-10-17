# Plantupia — deferred Codex audit and final-integration contract

**Status: PREPARED, NOT DISPATCHED.** The product builder is still integrating the Android UI across screens. Do **not** start a whole-repository autonomous redesign yet.

## Purpose

After the builder completes the core design/product passes (My Space, Plant Detail, Care/History, Create, Circle, Me, Inbox), Codex may act as a **technical auditor and implementation partner**, not as an authority that reinterprets the brand or deletes prior research. The work is research-first, decision-traceable, cross-disciplinary product building: observed reality → research and references → synthesis and alternatives → design decisions and stress tests → native implementation → user-device evidence → iterative correction.

### Non-negotiable source order

1. The builder's newest explicit requirements and actual device-test findings.
2. [Product Bible and decision rationale](./PLANTUPIA_PRODUCT_BIBLE_AND_HANDOFF_V01_2026-10-17.md).
3. [Nine-screenshot integrated design audit](./PLANTUPIA_REAL_DEVICE_VISUAL_BRIEF_AUDIT_2026-10-17.md) and [screen closure ledger](./PLANTUPIA_SCREEN_CLOSURE_LEDGER.md).
4. Actual primary research repo `omarkhair70-droid/Plantupia`, branch `research/foundation-source-atlas-20261007`: `docs/synthesis/*`, `docs/product/*`, `docs/design/*`, Pass 04–07 research and reference teardown; treat early options as options, not approvals.
5. The real implementation, Django permissions, migrations, native shell, media features and automated test/code evidence on `omarkhair-labs/nova` stacked PR #240 → #241.
6. Only then unrelated competitor patterns, generic refactoring instincts or convenience APIs.

## Phase A — finish visual and UX product decisions first (builder/ChatGPT)

- Make the complete system coherent on-device: truthful place and plant identity, immediate care action, dated evidence, distinct optional public sharing, coherent Me/Circle/Create/Inbox.
- Reject generic “green recolor of Nova,” repetitive large marketing copy, abandoned media/DM capabilities, unexplained changes to navigation, fake plant imagery or sensor/health inference.
- Keep `Nova`'s functional Android Native engines. Preserve existing backend data, authentication, account-scoped permissions, social posts/video/stories/chat, photo handling and media pipelines.
- Complete at least one integrated QA APK visual review and record observed corrections.
- Investigate the real QA `can_add_plant` response/deployment mismatch before claiming the owner cannot add plants; do not bypass server permissions.
- Record each accepted/rejected interaction in the design ledger. Do not fabricate real user interviews or company approvals.

## Phase B — Codex READ-ONLY audit first (not yet authorized to rewrite)

Inspect:
- Architecture/module coupling, incorrect dependencies, giant Composables, state lifecycles, navigation/Back/keyboard/Compose scroll.
- Actual `Nova` leftovers and conflicting design tokens; UI localization (English-first chrome, user-created Arabic/RTL content), accessibility and 360/390/430dp.
- Django schema/API contract, role-scoped private spaces, image metadata/EXIF and selected photo consent, archived records and date semantics.
- Regression of authentication/session handling, media viewers, Stories/Reels, post publishing, comments, Inbox/DM/groups/calls.
- Runtime/staging deploy consistency and named environment vars **without reading or publishing secret values**; do not assert production and preview DBs are the same without evidence.
- Unit/integration tests, media permissions, memory/performance, network retry/cancellation, account switch and concurrent saves.
- Existing source documentation: identify decisions without implementation and functions without real QA, with exact file/test evidence.

Deliver a **ranked P0/P1/P2 report**: file/line, observed or reproducible failure, matching product law, scope of fix, tests, security/UX risk and expected sign-off. Avoid overhauls not traceable to a bug or accepted direction.

## Phase C — only after audit review, execute bounded changes

- Explicitly identify target feature branch / base; create separate scoped commits or a review PR where appropriate. Don't modify default branch, existing Nova listing/package, production migrations, Oracle production data, signing, secrets, account owners or Google Play without approval.
- Keep separate Plantupia app identity on the builder's Play Console as an **intended final distribution requirement** — not already configured.
- Implement fixes ordered by user impact and avoid autonomous product pivot; request design review when multiple UX alternatives change the journey.
- For every fix attach an exact regression test, GitHub CI at final commit, and (for visual changes) QA screenshots/video from actual Android test with privacy-respecting evidence.
- If backend/deploy access is missing, state that accurately. Never simulate a passing integration or manufacture a screenshot or client sign-off.
- Stop at the **one integrated APK review gate**, then make explicit and separate Play AAB/distribution preparations.

## Final deliverables

- Evidence-based audit and disposition table (fixed / deferred / needs owner decision).
- Code changes on reviewed feature branch with Android/Django tests and exact SHA.
- Clear known issues / risks / privacy state.
- Updated Product Bible, decision log, screen ledger and versioned QA evidence.
- One integrated QA APK for owner's physical phone and later distinct Plantupia Play AAB after owner approval.

**This contract is for an eventual Codex session. It is not a Codex job, an automated delegation or a claim that the current visual redesign has been completed.**
