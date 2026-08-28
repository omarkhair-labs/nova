# Samsung physical-device smoke checklist

This checklist is required because JVM tests, lint, APK/AAB assembly, and even
instrumented intent tests do not prove Samsung keyboard/window behavior.

Record device model, Android/One UI version, navigation mode (gestures or
buttons), build commit, and result for each run. Use a non-production test
account and the Closed-testing build only when a phase explicitly requires a
device release.

## Baseline metadata

- [ ] Device model:
- [ ] Android version:
- [ ] One UI version:
- [ ] Navigation mode:
- [ ] Build commit/version:
- [ ] Tester/date:

Latest non-destructive connection attempt (Phase 2 PR 8): Samsung SM-A266B,
Android 16, One UI 8.5 (`ro.build.version.oneui=80500`), gesture navigation
(`navigation_mode=2`). The device was authorized, but Gradle executed zero tests
because the installed `com.omarkhair70.nova` signature differs from the local
debug key (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). The installed app was not
removed, so no checklist item or build-commit result is claimed from this
attempt.

## Shell and primary navigation

- [ ] Cold launch shows the expected authenticated or signed-out destination.
- [ ] Home -> People -> Profile -> Home follows the current root policy.
- [ ] People -> Profile and Profile -> People do not accumulate stale child
      destinations; Back behavior matches 2.1.3.
- [ ] Opening Messages keeps social state alive underneath; returning restores
      the prior social state.
- [ ] Opening Reels keeps social state alive underneath; returning restores the
      prior social state and Reel audio is paused.
- [ ] Android Back closes Reels overlay before leaving the app.
- [ ] Android Back from Messages inbox returns to Home/current special-entry
      owner as expected.
- [ ] Logout/session expiry from social, Messages, and Reels returns to the same
      signed-out experience without stale overlays.

## Notifications and special entries

- [ ] Direct-message notification opens the intended direct conversation.
- [ ] Group-message notification opens the intended group and title.
- [ ] Invalid/missing conversation data falls back safely to in-app routing.
- [ ] Reel like/comment/repost/reply notification opens the intended profile
      Reel when reel ID and author are valid.
- [ ] Other notification kinds open the expected social/post/profile target.
- [ ] Reopening a notification through `onNewIntent` does not duplicate or hide
      the current root.
- [ ] Back from a directly opened conversation finishes the special Activity;
      inbox-opened conversation Back returns to the inbox.

## Messages keyboard, header, and composer

Run for both the MainActivity Messages overlay and a notification-opened
MessagesActivity conversation.

- [ ] Tap composer: keyboard appears and composer stays immediately above it.
- [ ] Header remains visible while keyboard opens/closes.
- [ ] Latest message remains reachable and is not hidden behind composer/IME.
- [ ] Gesture navigation and three-button navigation do not create a double
      bottom gap.
- [ ] Emoji/system keyboard switching does not jump or overlap the composer.
- [ ] Attach-photo flow returns with composer/header layout intact.
- [ ] Start/cancel/send a voice note; recorder state and bottom inset recover.
- [ ] Reply/edit context card stays above composer and remains dismissible.
- [ ] Back closes keyboard/context overlays before leaving conversation.
- [ ] Conversation -> inbox -> conversation does not retain the wrong draft,
      reply, edit, search, theme, or group-details state.

## Messaging behavior sampling

- [ ] Send text once; optimistic row reconciles without duplication.
- [ ] Exercise retry using a temporary offline transition; client ID remains
      idempotent.
- [ ] Edit/delete/reply/react to messages and verify realtime peer updates.
- [ ] Send/view photo and voice note.
- [ ] Confirm unread divider, delivery/read receipts, paging, typing, presence,
      shared post/profile/Reel, group roles, theme, search/context/media.
- [ ] Existing call-history messages remain immutable and correctly rendered.

## Calls regression sampling

- [ ] Incoming/outgoing audio and video answer, decline, cancel, end.
- [ ] Background/foreground and PiP preserve the active call.
- [ ] Notification answer/decline/end actions work.
- [ ] Network interruption/reconnect does not create duplicate negotiation.
- [ ] Audio route, microphone, camera, Telecom registration, TURN/STUN, and call
      history match the baseline.

## Multipart publishing and Pulse closure

Root-cause record (2026-08-28): authenticated production logs showed Edit
Profile `PUT /me/`, Post `POST /posts/`, Story `POST /stories/`, and Pulse
`POST /pulses/` reached Django with the multipart content type but with empty
fields/files. All four paths shared manual `HttpURLConnection` multipart with
`setChunkedStreamingMode`, which sends `Transfer-Encoding: chunked` without a
`Content-Length`. The exact server validation bodies and response byte lengths
matched empty request data. The corrected client uses one known-length OkHttp
`MultipartBody` and streams file-backed video/thumbnail bodies. This is proved
by focused transport tests, but still requires a deployed Samsung pass.

- [ ] Save Profile theme/metadata without choosing a new avatar; the existing
      username is submitted and the saved theme is visible after re-entry.
- [ ] Publish an image Post; progress completes, the composer closes, and one
      durable Post appears without duplication.
- [ ] Publish an image Story, then a representative device-recorded video
      Story; confirm first frame, picture, audio, duration and retry behavior.
- [ ] Pick a Pulse video; preview starts with audible sound, the 48dp mute
      control announces and toggles its state, and no competing viewer audio is
      audible behind a reply composer.
- [ ] Publish Pulse image and video media; queued/preparing/uploading/published
      state remains visible after the composer closes and the resulting Pulse
      reconciles once without duplication.
- [ ] Interrupt Pulse network access during preparation/upload; confirm bounded
      retry, actionable failure, manual retry, and cancel only while cancellation
      is valid.
- [ ] Background and foreground Nova during a queued Pulse publish; persisted
      picker access survives and publication remains scoped to the initiating
      account.
- [ ] Switch accounts while a Pulse publish is pending; no media is published
      into the other account and no other-account status leaks into its UI.
- [ ] Play the original failing Samsung Pulse clip full-screen; confirm a real
      first frame, picture plus audio, full source duration before repeat, and
      correct pause/resume after composer, background and full-screen handoff.

Backend deployment prerequisite: apply
`accounts.0036_pulse_publish_identity` before judging durable Pulse retries.
Do not mark these checks passed from JVM tests, architecture gates, or CI.

## Result

- [ ] All required checks passed.
- [ ] Failures recorded with exact entry path, screenshots/video, logs, and
      whether the same behavior occurs on Android 2.1.3.
