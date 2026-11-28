# Plantupia / Flutter edition

A new native Flutter presentation candidate, explicitly requested on 28 November 2026. This branch does not replace the existing Kotlin app, website, company platform or backend. It uses the already reviewed #253 server contracts. Existing Play identity and production remain untouched.

Art direction: the website's authored editorial photography, dark arrival, large quiet typography, asymmetrical composition, fine rules and private plant records. No Living Lens, scan corners, segmentation or pseudo-health scoring. The introductory architectural photo is the same publicly referenced Pexels image used on the site; it is not a Plantupia project. It is never substituted for a customer's photograph. Typeface and typographic wordmark are review fallbacks, not approved brand artwork.

Scope of this candidate: account login, Today, plant collection and details, reviewed care guide, recorded history, explicit private text care/observation with an idempotent request, published catalogue and persistent cart, saved orders, existing server assistant with per-request consent, read-only Circle and team records. No mock success, duplicated backend or automatic sharing. Camera/gallery upload, QR handover, account registration/recovery, complete checkout, social publishing/messaging/calls, notifications, persistent secure sessions and full localization still need implementation and parity acceptance before any replacement. Sessions are memory-only for this review; users sign in after restarting. Read-only media access is restricted; unavailable images have an explicit fallback.

Build: Flutter stable (see CI), generated Android shell with applicationId com.omarkhair70.nova.flutterqa. No production signing or Play release. Set PLANTUPIA_QA_API to the authorized HTTPS QA API origin ending /api/v1/ at compile time; no production fallback. The build workflow generates only Android platform scaffolding, then analyses, tests, and builds a debug APK. Visual fixture tests use fictional named records only and never reach customer data.

```sh
flutter pub get
flutter analyze
flutter test
flutter build apk --debug --dart-define=PLANTUPIA_QA_API=<approved-QA-api-origin>
```

The actual SDK-generated Android folder is needed before the last command; use the isolated scaffold step in the workflow. This is an engineering/art-direction candidate, not a finished migrated product.
