# Nova Google Play release automation

Nova can build, sign and publish Android App Bundles from GitHub Actions. Tagged releases are intentionally safe by default: `android-v*` tags publish only to the Google Play **internal** track with in-app update priority `2`.

## One-time Google Play setup

1. Enable the Google Play Developer API in a Google Cloud project.
2. Create a service account for Nova release automation.
3. In Google Play Console > Users and permissions, invite that service-account email and grant only the Nova app permissions needed to upload releases to the tracks you intend to use.
4. Download the service-account JSON key and keep it private.

The app must already exist in Google Play before the Publishing API can upload releases. Nova satisfies this because its first release was uploaded through Play Console.

## Required GitHub Actions secrets

Configure these repository secrets:

- `NOVA_ANDROID_KEYSTORE_B64` — base64 of the existing Nova upload keystore used for Google Play.
- `NOVA_RELEASE_STORE_PASSWORD` — upload-keystore store password.
- `NOVA_RELEASE_KEY_ALIAS` — upload key alias.
- `NOVA_RELEASE_KEY_PASSWORD` — upload key password.
- `NOVA_GOOGLE_SERVICES_JSON_B64` — base64 of Nova's production `app/google-services.json`.
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_B64` — base64 of the Google Play service-account JSON key.

Do not create a new upload key for this workflow. Use the same upload key already registered for Nova in Google Play.

### Base64 on Windows PowerShell

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\nova-upload.jks")) | Set-Clipboard
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\google-services.json")) | Set-Clipboard
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\play-service-account.json")) | Set-Clipboard
```

Paste each clipboard value into the matching GitHub secret.

## Releasing to Internal Testing

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` and merge that change to `master`.
2. Tag the exact `master` commit, for example:

```bash
git switch master
git pull --ff-only origin master
git tag android-v2.0.2
git push origin android-v2.0.2
```

The `Google Play Release` workflow then:

1. verifies that the tagged commit belongs to `master`;
2. restores the production Firebase config and upload keystore from GitHub Secrets;
3. builds a signed release AAB;
4. verifies that the bundle is signed;
5. uploads it with the Google Play Developer Publishing API;
6. assigns it to the `internal` track with update priority `2`;
7. commits the Play edit.

## Manual releases

GitHub Actions > **Google Play Release** > **Run workflow** supports:

- `internal`
- `alpha`
- `beta`
- `production`

It also accepts an in-app update priority from `0` through `5`.

Production publishing is blocked unless `confirm_production` is exactly `PUBLISH`. A production run uses a completed release, so treat it as a deliberate full production publish rather than a staged rollout.

## Nova in-app update behavior

Nova uses Google Play's official in-app update library.

- priority `0-3`: Flexible update when Google Play allows it;
- priority `4-5`: Immediate update when Google Play allows it;
- if only one Play flow is allowed for the current device/release, Nova falls back to that flow;
- Flexible updates show an in-app **Restart** banner after the download completes;
- Nova resumes an already-running Immediate update when the app returns to the foreground;
- canceled Flexible prompts cool down for 24 hours for the same version;
- canceled Immediate prompts cool down for 4 hours for the same version.

In-app updates are a Google Play feature. Test them using a build installed from a Play testing track with an account that has access to that track; a normal Android Studio debug install is not a representative test of Play update availability.
