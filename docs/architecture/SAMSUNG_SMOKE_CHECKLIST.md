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

## Result

- [ ] All required checks passed.
- [ ] Failures recorded with exact entry path, screenshots/video, logs, and
      whether the same behavior occurs on Android 2.1.3.
