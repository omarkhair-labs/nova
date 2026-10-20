# Integrity Sprint 03 — Reliable saves (F05)

Scope: private care/observation/issue/outcome creation and explicitly consented public Circle question creation. Starting HEAD: `87bb28d723d4dcc92e4cd72c9556eb8bcdfb7891`; existing feature branch and draft PR #241. Android Native + existing Django only. This is implementation evidence, not device acceptance or release approval.

## Failure, decision and test-first evidence

A committed mutation with a lost acknowledgement previously became a new event or public post when retried. Nova's `Post.client_publish_id` establishes a useful author + UUID pattern, but its existing replay lookup accepts changed captions and does not provide Plantupia's strict content/scope conflict contract. Its behavior and other social publishing engines are preserved.

Decision F05 / 2026-10-18 / scoped user authorization: persist the logical submission before sending; bind its identity to actor, operation, plant and exact semantic content on the server. Alternatives rejected: fresh UUID per retry, content-only deduplication (blocks intentional repeated observations), fallback to an old endpoint on 404 (old servers can ignore the field), and reusing the looser Post contract. Falsification: any uncertain retry creates another record/photo/public question/creation effect; another actor accesses the receipt; changed content reuses its ID.

Test-first commit `88a7617fd588d3e35ef4ece56576817b1e92ede9`: local plants suite reported 53 tests, seven failures and one PostgreSQL-only skip before the fix. Tests demonstrated duplicate creation and missing conflict enforcement. Subsequent cases add real database contention, rollback/photo reuse and durable rejection checks.

## Backend contract and migration

- Migration `plants/0004_plantsubmission.py` adds a `PlantSubmission` receipt uniquely keyed by authenticated actor + UUID. It stores operation, plant UUID, canonical request fingerprint, immutable result and status. Actor deletion cascades as existing actor-owned data does; no deletion-policy change or new foreign-key protection is introduced.
- New POST routes `plantupia/plants/{plant}/event-submissions/` and `question-submissions/` require `client_submission_id`. Native never falls back to the old creation route. An older backend fails safely with 404 and the device retains the submission.
- Receipt reservation, event/follow-up closure or Post + PlantDiscussion creation and response snapshot commit in one transaction. PostgreSQL's actor/UUID unique constraint makes concurrent contenders wait for the winner. Identical replay returns the original body (201 first creation, 200 replay); conflicting plant, operation, content or photo returns 409.
- Fingerprints normalize JSON/multipart fields, trimmed text and observation timestamps to UTC, and hash exact uploaded photo bytes. Different filenames do not change identity. Public photos retain the existing EXIF/GPS sanitizer. Uploaded URLs cannot replace owned private photo bytes.
- Current authentication, plant access and record permissions are checked on every replay. Public consent remains required on each public request. Another actor or a removed membership cannot recover the receipt. Mutable follow-up/archive checks run for new creation after receipt lookup, allowing the original committed outcome to be acknowledged later.
- Definitive creation validation failures become durable bound 400 receipts with `code=submission_rejected` and `request_id` equal to the UUID. The device releases its frozen slot only for that bound rejection. Unbound 400, 403/404/409, 5xx, cancellation, malformed success and lost responses retain identity. A rejection cannot race a changed request that commits behind the client.
- Deterministic photo keys hash actor + submission + actual stored bytes and fit the existing 100-character image columns. Only the transaction winner writes. A database rollback after writing a blob reuses that object on retry. PostgreSQL CI exposed the initial long-path defect that SQLite did not enforce; the compact-key correction preserves Nova's schema.
- Existing event/question routes remain compatible and honor IDs when supplied. **Old clients omitting IDs retain their previous non-idempotent behavior.** Backend deployment alone does not upgrade them. No Post/Story/Reel/DM contract, Circle resolution consent or release configuration changed.

No production or hosted QA migration was applied. Before an authorized deployment, verify QA database isolation, backups and server version; apply 0004 before exposing the new routes. A native-first rollout stays safe but cannot save until the server supports the protocol. Only disposable test databases are migrated in this sprint.

## Native ownership and recovery

`PlantupiaSubmissionJournal` writes an atomic manifest and owned photo copy under app-private `noBackupFilesDir/plantupia-submissions`. Namespace: API environment + positive authenticated account ID + plant + slot (care, specific follow-up outcome, public question). A shared process lock coordinates journal/repository instances. File sync and atomic replacement precede sending. Original UUID, timestamp, fields and photo hash survive instance/process recreation. Unreadable journals fail closed rather than inventing a second public identity.

Uncertain failures freeze that logical submission; changed content cannot silently start another. Care exposes “Retry original entry” and refreshes selected history without resetting an unrelated unsaved composer. Circle exposes “Review saved question”, restores the exact saved text/photo into the existing preview and resets consent. No pending public question sends automatically. A valid acknowledgement is retained durably until current UI confirmation; only then can that slot begin a new logical submission.

Sprint 02 login-generation checks surround photo preparation, disk reads, networking and callbacks. Logout/account B cannot inspect or send A's pending record. A's journal remains app-private for manual recovery after a later authenticated A login; old responses cannot change B's UI or credentials. Reopening the same plant invalidates old detail loads. Shared networking adds only optional typed error-code/request-ID metadata, tested for JSON and multipart. Shared auth, normal callers and existing response handling remain compatible.

Creation reminders use the committed event UUID as unique WorkManager identity, KEEP policy, durable marker and existing-work query. Replay cannot duplicate or re-enable an already cancelled/delivered reminder. Explicit rescheduling retains its behavior. New creation work carries login generation, preventing delivery into a replacement login of the same account. Reminders remain approximate local notifications. No Circle push fan-out was added: a test-only database notification receiver verifies creation effects happen once.

## Verification

- Isolated local SQLite: configuration and migration-drift checks pass; 59 plants tests pass with two PostgreSQL contention skips. No hosted services or real accounts contacted.
- Full PostgreSQL backend CI at the compact-key commit passed, including both contention tests. Final exact-HEAD CI count/run URL is recorded on PR #241 after completion.
- Native regressions: ID/time/photo retention across recreated instances; account/plant/environment isolation; simultaneous journal preparation; changed content/corrupt files; bound rejection versus uncertain failure; lost JSON and unusable multipart acknowledgements; expired picker access; renewed public consent; logout/B/new-A recovery; queued reminder login ownership. A real TCP fixture closes after the complete multipart upload before returning any acknowledgement.
- Final full Android suite, release lint, APK builds and all 20 architecture/English guards are required. Network guard additionally protects mandatory-ID endpoints and journal durability; no prior guard is weakened. Final outcomes and commit SHAs are recorded in the draft PR.

## Required physical and isolated-server QA

1. Lose the response after JSON care commits; retry twice. Verify one event UUID/time, pending review and reminder. Repeat with photo, issue and outcome saves, completed-follow-up replay and archive/restore between attempts.
2. Commit a consented multipart question, drop acknowledgement, Back, force-stop and restart. Review exact saved text/photo, re-consent and retry twice: one Post, discussion, sanitized object and creation effect. Refuse consent: no request. Change frozen text/photo: conflict guidance, no new post.
3. Start A's upload, switch to B before completion and reopen a shared plant: no A note/photo/result/banner/notification in B. Recover A only after fresh A authentication. Repeat A→logout and A→new-A with queued local reminders.
4. Kill before send, during upload, after commit/before response, after acknowledgement/before UI confirmation and after confirmation. Restart/retry, compare row/object counts. Revoke picker access; recovery uses the owned copy. Test actual storage-full/corrupt-file errors and denied notification permission.
5. Two clients submit identical and conflicting requests simultaneously: 201+200 original results or 201+409 conflict, one file/effect, actor isolation and current permissions/private image authorization. Test equivalent JSON/multipart and different filenames.
6. Older server returns an error without legacy fallback; after an authorized isolated upgrade, retry the same saved ID. Smoke-test login/profile and inherited Nova Posts/Stories/Reels/DM media. Verify Back/switch, TalkBack and long drafts around recovery controls.

Not run: physical device/process kill, actual WorkManager delivery, S3/object-storage crash semantics, real push delivery or hosted migrations. JVM recreation reloads disk with a new instance; it is not Android process-kill evidence. PostgreSQL contention uses real concurrent connections; SQLite is not concurrency proof.

Receipts intentionally have no expiry: deleting them without a retention contract can re-enable duplicates. They add private snapshot storage proportional to submissions. Pending photos/notes remain until confirmed/rejected, app-data removal or uninstall; logout does not destroy another account's pending save. Include this retention in operational/privacy review without changing deletion policy here. Device loss/uninstall loses local recovery IDs. An unretried rollback after blob write can leave an orphan; retry reuses it, while separately audited garbage collection remains future work. Directory-fsync/power-loss and non-local storage failure behavior require target-platform validation. The journal/coordinator assume the current single Android process; future separate processes need IPC coordination.

## Implementation commits

- `88a7617fd588d3e35ef4ece56576817b1e92ede9` — failing backend replay/conflict regressions.
- `a0928f2d1cccf69f88b2ae4eacbeb2a1c4a0cc7c` — atomic receipt protocol, migration, media ownership and expanded regressions.
- `e664f5f21e0be4ef83993ca2c7ed43bd96a1a22a` — PostgreSQL photo-column compatibility fix and length assertions.
- `05ee392bf190db77903c12750d5c1c46d26cbd68` — durable native journal and isolation/concurrency tests.
- `20b35947b8018d5cbc47a5db778c11fb1a66efbd` — repository/UI recovery, once-only creation reminders, transport metadata and actual disconnect/session regressions.

Final exact-HEAD check outcomes and run URL are maintained in the Sprint 03 verification section of draft PR #241. The app remains unapproved for release.
