# Plantupia App Design Closure — Activity
Date: 2026-11-19
Branch: `design/plantupia-app-closure-20261119`

## Product role

The old Notifications surface mixed care, support, commerce and social events under a generic notification screen, and still exposed QA/Nova language.

The refined surface is **Plantupia Activity**.

Its job is not to duplicate every destination. Its job is to answer:
**What changed, and where does that change belong?**

## Information model

The existing backend notification kinds and navigation targets remain the source of truth.

Activity presents five views:
- All
- Care
- Team
- Orders
- Circle

### Care

Care does not render a second full calendar inside Activity anymore.

The Care view now contains one authored destination card:
- explains that planned dates and recorded Living History moments remain distinct;
- opens the existing account-scoped Care Schedule through `PlantupiaCareCalendarOpenSignal`.

This keeps one calendar and one schedule truth.

### Team

`plant_team` notices remain private-support events.

Copy now reads as a Plantupia Team reply, and the existing target still opens the exact support ticket/plant relationship through `PlantupiaSupportPushSignal`.

### Orders

The internal navigation target was renamed from `QaOrder` to `Order`.

The notification no longer says a separate QA order draft was prepared. User-facing Activity copy is:
`Plantupia updated your order`.

The same persisted account order opens through `PlantupiaShopOpenSignal`.

### Circle

People and intentionally shared content remain community activity:
- follows;
- likes;
- comments/replies;
- shared-video reactions.

Social events keep their existing person/post/video targets.

## Visual language

Header:
- `PLANTUPIA / ACTIVITY`
- `What changed`
- explanatory copy that care, team, order and Circle updates open their owning destination.

Generic Material filter chips were replaced with Plantupia category tabs:
- forest selected state;
- paper/cream unselected state;
- Plantupia shapes and typography.

Rows now identify their source before the message:
- `TEAM / PRIVATE SUPPORT`
- `ORDER / STATUS`
- `CIRCLE / PEOPLE`
- `CIRCLE / SHARED MOMENT`
- `CIRCLE / SHARED VIDEO`

Team/order notices use a quiet system tile rather than pretending a staff/system event is an ordinary community avatar.

Unread state keeps the existing unread truth but uses the Plantupia soft-sage surface.

Follow-request cards were moved onto Plantupia paper/forest/cream styling.

## Preferences cleanup

Notification preferences no longer use Nova/QA language.

Changes include:
- `Activity preferences`
- `Choose which Plantupia updates can interrupt you.`
- `Shared moments`
- `Calls & live`
- `Shared videos & stories`
- `Plantupia Team`
- `Order updates`

The persisted preference keys and backend behavior are unchanged.

## Truth and navigation preserved

No new event store, fake count or duplicate support/order state was created.

Preserved:
- unread counts and mark-read behavior;
- paging;
- follow requests;
- support-ticket routing;
- order routing;
- person/post/video routing;
- notification preference keys;
- pull-to-refresh.

## Verification

- U+FFFD scan on changed Activity/Preferences/test files: zero.
- `git diff --check`: pass.
- `:app:compileDebugKotlin`: BUILD SUCCESSFUL.
- `NotificationsStateOwnerTest`: BUILD SUCCESSFUL.
- `:app:compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL.
- Instrumented assertions were updated for the authored Activity copy and Order notification language.
- The only Android-test compile warning was an existing deprecated soft-input constant in another test.

A local ignored dummy `app/google-services.json` was used only to satisfy the Google Services Gradle plugin during local test compilation and is removed before commit.
