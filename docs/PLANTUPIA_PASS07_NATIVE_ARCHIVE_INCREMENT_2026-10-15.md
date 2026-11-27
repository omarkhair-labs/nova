# Plantupia Native — C1 reversible plant archive increment
**2026-10-15 | Implementation evidence, not product sign-off**

Source: Pass 07 native execution contract and `PLANTUPIA_MASTER_ROADMAP_REFERENCE_AUDIT_2026-10-14.md`.
Target: existing `feature/plantupia-native-experience-20261012`, PR #241 (Draft). No production, master, Google Play, signature or credential changes.

## Contract and implementation

- Reuse existing `PlantUnit.is_active`; **no database migration, hard delete, fake care event or new sensor fields**.
- PATCH existing owner/manager-only `/plantupia/plants/<uuid>/` with boolean `is_active` to archive or restore. 404 to unrelated users/viewers/carers; metadata and photos retained. Archive/restore is auditable in private PlantEvent history as a labelled **administrative record**, not observed watering or diagnosis. Idempotent repeat does not create another event.
- Active inventory / Today never include archived records. Existing Plantupia plant list still returns authorized archived records so owner, manager and authorized space members can access the living record; a separate native expandable searchable archived section prevents their silent disappearance.
- Django hides archived pending check-ins from the pending endpoint and `next_follow_up_on` inventory field, **without claiming the underlying human-scheduled event was completed**. After restore, original recorded dates are visible for human review, not automatically rescheduled.
- Django rejects new care/outcome events, follow-up reschedules and new public questions while archived, retaining prior private history, photos and existing public posts. Public sharing retains explicit consent.
- Android Compose offers archive/restore only to users with server-returned owner/manager space-edit authority, with explicit confirmation, server ACK refresh and error state. It does not switch UI state optimistically after a rejected request.
- Newly consented Android check-in reminders now store their plant association locally; archive cancels only that plant's associated reminders. Known previously loaded legacy pending event IDs are also cancelled. **Older previously scheduled local reminders without plant metadata beyond the currently loaded pending page require physical-device regression/migration QA**; this cannot be asserted fully closed based on CI alone.

## Tests added

- Django: owner+manager versus viewer/carer/stranger archive authority; audit idempotence; archive→restore; retained event, timestamp and pending history; block new care/questions/follow-up schedule while inactive.
- JVM: isolated archived inventory search/filter/scale (1–100 records); local reminder metadata selection scoped to target plant.
- CI gate: `Android build` (unit/lint/debug/QA/release assembly), `Backend tests` and migrations check **must be verified again on latest HEAD** after commits complete.

## Real QA still required before calling C1 closed

1. Two-account Android QA with real images and membership revocation; check archived history appears to authorized members and never publishes automatically.
2. Android 13+ opt-in reminder cancellation, boot/background/timezone, legacy reminders scheduled **before** this increment; verify archived plant receives no stale local alert and other plants remain scheduled.
3. Server-isolated QA Django and PostgreSQL/media storage, 0/1/7/100 plant experience, offline/retry, touch targets, TalkBack and font scaling.
4. Full native Media3 Stories/Reels → DM/keyboard/back smoke; real AI provider request with QA-only secret and cost/privacy audit.

**Status:** Increment authored in code and automated tests. Do not label live device or hosted QA verified. Final owner visual review remains one integrated APK, not per-screen checkpoints.
