# Nova V2 — Google Play closed-test release checklist

Release target: **Nova 2.0.0**  
Application ID: `com.omarkhair70.nova`  
Version code: `20000`  
Target SDK: `36`

This file is an operational checklist for the first Nova V2 Play upload. Do not commit Play Console passwords, upload-key passwords, tester credentials, or other secrets to this repository.

## 1. Main store listing

### App name

`Nova`

### Short description

`Share moments, message friends, join groups, and call on Nova.`

### Full description

Nova is a social space for sharing moments and staying connected with the people you care about.

Create a profile, discover people, share photo and video Stories, publish posts, and repost moments you want others to see. Keep conversations moving with direct messages and group chats, or switch to a voice or video call when text is not enough.

Nova includes privacy and safety controls designed for real social use. Accounts can be private, follow requests can be approved or declined, Stories can be shared with Close Friends, and users can block or report accounts when needed.

Key features:
- Posts, comments, likes, sharing, and reposts
- Photo and video Stories
- Private accounts and follow requests
- Close Friends Story audience
- Direct messages and group chats
- Voice and video calls
- Profile Posts and Reposts tabs
- Blocking, reporting, password security, and account deletion

Nova V2 is focused on familiar, simple social interactions without unnecessary complexity.

## 2. Store graphics

Prepare these before submitting the store listing:

- Play Store app icon: **512 × 512**, 32-bit PNG with alpha, max 1 MB.
- Feature graphic: **1024 × 500**, JPEG or 24-bit PNG without alpha.
- At least **2 phone screenshots** are required to publish the listing.
- Recommended Nova set: **4 portrait screenshots at 1080 × 1920** so the listing is eligible for more large-format recommendation surfaces.

Recommended screenshot sequence:

1. Home feed + Stories rail.
2. Story viewer or post detail showing the visual social experience.
3. Messages inbox + a direct/group conversation.
4. Profile showing Posts / Reposts tabs and privacy-focused identity layout.

Use real in-app UI. Avoid adding ranking claims, prices, misleading badges, or references that imply affiliation with another social app.

## 3. Public policy URLs

After this PR is deployed to Nova production:

- Privacy Policy: `https://nova-production-4f6b.up.railway.app/privacy/`
- Account deletion: `https://nova-production-4f6b.up.railway.app/account-deletion/`

Both URLs must return successfully from a logged-out browser before Play submission.

## 4. App content declarations

### Ads

Current Nova V2: **No ads**.

If ads are added later, update Play Console and the privacy/data-safety disclosures before shipping that build.

### App access

Nova requires login for most product functionality. Create or maintain a dedicated review account that can access the normal product experience.

Enter the review username/password **only in Play Console App access**. Do not put credentials in this repository, release notes, screenshots, or public documentation.

Reviewer instructions should be concise, for example:

> Sign in with the provided Nova review account. The account can access Home, People, Stories, Messages, Groups, Settings, and direct voice/video call entry points. No OTP or external approval is required after login.

### Target audience

Recommended product position for current Nova: **not directed to children under 13**. Select the age groups that match the intended launch audience in Play Console. If Nova is intentionally offered to children, stop and re-review Families policy, user-generated-content safeguards, SDKs, store content, and privacy wording before submission.

### Content rating

Complete the questionnaire based on the real product. Nova includes user-generated social content, messaging, sharing, and communication between users. Do not answer based only on first-party sample content.

## 5. Data Safety draft audit

Before submission, verify the final Play Data Safety form against the release build and every SDK.

Nova V2 currently processes categories that likely include:

- Account/personal information: email, username, display name, profile image.
- User-generated photos/videos: profile media, posts, Stories, and group images where applicable.
- Messages: direct and group conversation content.
- App interactions: follows, requests, Close Friends, likes, comments, reposts, shares, blocks, and reports.
- Device/app identifiers used for push delivery, such as Firebase messaging tokens.
- Call session/signaling information needed to connect and operate calls.

Current product statements that should remain true for this release:

- Nova does not sell personal data.
- Nova V2 does not contain third-party advertising.
- Production traffic is sent over encrypted network transport.
- Users can request account deletion.
- In-app account deletion removes identifying profile/content/social data as implemented by Nova; shared message history may remain for other participants without the deleted profile identity.

Call audio/video is not intentionally recorded or stored by Nova. Review Google's Data Safety definition of ephemeral processing when completing any audio/video field; answer based on the actual release architecture rather than assumptions.

## 6. Foreground service declaration — phone calls

Nova declares phone-call foreground-service capability for its VoIP call experience.

Suggested functionality description:

> Nova provides user-initiated and incoming 1:1 VoIP voice and video calls. Phone-call foreground execution keeps an active call available while the user temporarily backgrounds the app and supports the expected Android calling experience.

Suggested user impact if deferred/interrupted:

> If the call task is deferred or interrupted, an incoming or active Nova call may not connect, may lose continuity when the app is backgrounded, or may end unexpectedly. Immediate execution is required only while the user is actively receiving or participating in a call.

Video evidence to record for Play Console:

1. Open a direct Nova conversation.
2. Start a voice or video call using the visible call action.
3. Accept the call on a second test account/device.
4. Show the active call.
5. Background Nova briefly and return to the active call.
6. End the call normally.

Keep the demonstration focused on the feature that triggers the foreground call behavior.

## 7. Full-screen intent declaration

Nova uses full-screen intent for the permitted high-priority use case of **receiving phone/video calls**.

Suggested declaration summary:

> Nova's calling feature can present an incoming voice or video call that requires immediate user attention, including when the app is not currently in the foreground. The full-screen intent is used for incoming call presentation, not for marketing, ordinary messages, or promotional notifications.

## 8. Account deletion verification

Before Play submission verify both paths:

1. In app: `Profile → Settings → Security → Delete account`.
2. Logged-out browser: open the public Account Deletion URL and confirm the external request instructions are visible.

## 9. Final build gates

Before generating the signed upload bundle:

- `master` contains the release-prep PR.
- Nova CI is fully green.
- `bundleRelease` is green.
- Production backend has deployed successfully.
- Privacy and deletion URLs work over HTTPS without login.
- One focused physical smoke test passes on the release code: login, Home, one Story, one comment, one DM/group message, one direct call entry, Settings legal links.

## 10. Generate the signed AAB

Use Android Studio:

`Build → Generate Signed App Bundle or APK → Android App Bundle`

Use the existing Google Play upload key / keystore for this Play app. Do not create a different upload identity unless you know the existing Play App Signing setup requires it.

The file uploaded to Google Play must be the signed `.aab`, not an APK.

After upload, review Play Console's automated pre-review warnings before sending the closed-test release for review.
