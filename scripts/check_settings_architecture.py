#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = ROOT / "app/src/main/java/com/nova/app/SettingsActivity.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
AUTH_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/auth/NovaAuthRepository.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Settings architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


settings = read(SETTINGS)
container = read(CONTAINER)
auth_repository = read(AUTH_REPOSITORY)

# Settings is a thin Activity/UI shell over shared AppContainer session/auth ownership.
for required in (
    "import com.nova.app.app.appContainer",
    "val appContainer = applicationContext.appContainer",
    "val username = appContainer.currentCachedUsername()",
    "appContainer.authRepository.logout()",
):
    if required not in settings:
        errors.append(f"Settings live shell is missing AppContainer ownership seam: {required}")

for forbidden in (
    "import com.nova.app.core.auth.NovaSessionStore",
    "import com.nova.app.core.auth.NovaAuthRepository",
    "NovaSessionStore(applicationContext)",
    "NovaAuthRepository(applicationContext)",
    "rememberCoroutineScope",
    "scope.launch",
):
    if forbidden in settings:
        errors.append(f"Settings must not restore route-owned auth/session/async ownership: {forbidden}")

# Preserve exact navigation, external URLs, task reset, and visible Settings contract.
for required in (
    'private const val PRIVACY_POLICY_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/privacy/"',
    'private const val ACCOUNT_DELETION_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/account-deletion/"',
    "Intent(this, PrivacyActivity::class.java)",
    "Intent(this, AccountSecurityActivity::class.java)",
    "AccountSecurityActivity.EXTRA_MODE",
    "AccountSecurityActivity.MODE_SECURITY",
    "AccountSecurityActivity.MODE_BLOCKED",
    "Intent(Intent.ACTION_VIEW, Uri.parse(url))",
    "Intent(this, MainActivity::class.java).addFlags(",
    "Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK",
    'title = "Privacy"',
    'title = "Security"',
    'title = "Blocked accounts"',
    'title = "Privacy policy"',
    'title = "Account deletion help"',
    'text = "Log out"',
    "AboutNovaDialog(",
    'title = "Android app settings"',
):
    if required not in settings:
        errors.append(f"Settings protected navigation/UI seam changed or disappeared: {required}")

# AppContainer keeps the already-established cached-username seam and shared auth repository,
# now wired through the feature-owned Auth remote boundary.
for required in (
    "import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource",
    "val authRepository = NovaAuthRepository(appContext, AuthRemoteDataSource(api))",
    "fun currentCachedUsername(): String = sessionStore.load()?.cachedUser?.username.orEmpty()",
):
    if required not in container:
        errors.append(f"AppContainer is missing Settings dependency seam: {required}")

# Logout behavior remains centralized in the existing repository implementation.
for required in (
    "fun logout() {",
    "NovaPendingRegistrationPhoto.clear()",
    "NovaPushRegistration.logout(",
    "sessionStore.clear()",
):
    if required not in auth_repository:
        errors.append(f"Settings logout behavior changed or disappeared: {required}")

if errors:
    print("Settings architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Settings architecture check passed.")
