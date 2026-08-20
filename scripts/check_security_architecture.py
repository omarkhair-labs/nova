#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/security/data/SecurityRepository.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/security/SecurityStateOwners.kt"
OWNER_TEST = ROOT / "app/src/test/java/com/nova/app/feature/security/SecurityStateOwnersTest.kt"
CORE_SECURITY = ROOT / "app/src/main/java/com/nova/app/core/auth/NovaAccountSecurityRepository.kt"
CORE_BLOCKED = ROOT / "app/src/main/java/com/nova/app/core/social/NovaBlockedAccountsRepository.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
ACCOUNT_SCREENS = ROOT / "app/src/main/java/com/nova/app/feature/auth/AccountSecurityScreens.kt"
BLOCKED_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/auth/BlockedAccountsScreen.kt"
ACTIVITY = ROOT / "app/src/main/java/com/nova/app/AccountSecurityActivity.kt"
AUTH_SCREENS = ROOT / "app/src/main/java/com/nova/app/feature/auth/AuthScreens.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Security architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


contract = read(CONTRACT)
owner = read(OWNER)
owner_test = read(OWNER_TEST)
core_security = read(CORE_SECURITY)
core_blocked = read(CORE_BLOCKED)
container = read(CONTAINER)
account_screens = read(ACCOUNT_SCREENS)
blocked_screen = read(BLOCKED_SCREEN)
activity = read(ACTIVITY)
auth_screens = read(AUTH_SCREENS)

for required in (
    "interface SecurityRepository",
    "suspend fun requestPasswordReset(email: String): ApiResult<String>",
    "suspend fun resetPassword(",
    "suspend fun changePassword(",
    "suspend fun revokeOtherSessions(currentPassword: String): ApiResult<NovaUser>",
    "suspend fun deleteAccount(currentPassword: String): ApiResult<String>",
    "interface BlockedAccountsRepository",
    "suspend fun blockedAccounts(): ApiResult<List<NovaPerson>>",
    "suspend fun unblock(username: String): ApiResult<Unit>",
):
    if required not in contract:
        errors.append(f"stable Security contract is missing seam: {required}")

for required in (
    "import com.nova.app.feature.security.data.SecurityRepository",
    ") : SecurityRepository {",
    "override suspend fun requestPasswordReset(email: String): ApiResult<String>",
    "override suspend fun resetPassword(",
    "override suspend fun changePassword(",
    "override suspend fun revokeOtherSessions(currentPassword: String): ApiResult<NovaUser>",
    "override suspend fun deleteAccount(currentPassword: String): ApiResult<String>",
):
    if required not in core_security:
        errors.append(f"production account-security repository is missing direct stable seam: {required}")

for required in (
    "import com.nova.app.feature.people.domain.model.NovaPerson",
    "import com.nova.app.feature.security.data.BlockedAccountsRepository",
    ") : BlockedAccountsRepository {",
    "override suspend fun blockedAccounts(): ApiResult<List<NovaPerson>>",
    "override suspend fun unblock(username: String): ApiResult<Unit>",
):
    if required not in core_blocked:
        errors.append(f"production blocked-accounts repository is missing direct stable seam: {required}")

for required in (
    "val securityRepository: SecurityRepository = NovaAccountSecurityRepository(appContext)",
    "val blockedAccountsRepository: BlockedAccountsRepository = NovaBlockedAccountsRepository(appContext)",
):
    if required not in container:
        errors.append(f"AppContainer is missing Security construction seam: {required}")

# Preserve exact account-security/recovery transport and session behavior.
for required in (
    'path = "auth/password/reset/request/"',
    'path = "auth/password/reset/confirm/"',
    'path = "auth/password/change/"',
    'path = "auth/sessions/revoke-others/"',
    'path = "auth/account/delete/"',
    '.put("current_password", currentPassword)',
    '.put("new_password", newPassword)',
    '.put("email", email.trim().lowercase())',
    '.put("code", code.trim())',
    "connectTimeout = 12_000",
    "readTimeout = 15_000",
    "authApi.refresh(stored.refreshToken)",
    "sessionStore.updateAccessToken(accessToken)",
    "sessionStore.save(session)",
    "NovaPushRegistration.activate(appContext)",
    "if (response.statusCode == 401) sessionStore.clear()",
):
    if required not in core_security:
        errors.append(f"Security transport/session seam changed or disappeared: {required}")

# Preserve exact blocked-account transport/auth behavior.
for required in (
    'requestJson("auth/blocks/", bearerToken = token)',
    'path = "people/${encode(username.trim().lowercase())}/block/"',
    'method = "DELETE"',
    "connectTimeout = 10_000",
    "readTimeout = 10_000",
    "authApi.refresh(stored.refreshToken)",
    "sessionStore.updateAccessToken(refreshed.value)",
    "if (retried.statusCode == 401) sessionStore.clear()",
):
    if required not in core_blocked:
        errors.append(f"Blocked-accounts transport/session seam changed or disappeared: {required}")

for required in (
    "enum class PasswordRecoveryStage { Email, Code, Done }",
    "data class PasswordRecoveryUiState(",
    "class PasswordRecoveryStateOwner(",
    "data class AccountSecurityUiState(",
    "class AccountSecurityStateOwner(",
    "data class BlockedAccountsUiState(",
    "class BlockedAccountsStateOwner(",
    "value.filter(Char::isDigit).take(6)",
    'loadingAction = "change"',
    'loadingAction = "revoke"',
    'loadingAction = "delete"',
    'error = "Enter your current password first."',
    "if (result.statusCode == 401) {",
    "onSessionExpired()",
    "blocked = state.blocked.filterNot { it.id == person.id }",
):
    if required not in owner:
        errors.append(f"Security state characterization is missing seam: {required}")

for test_name in (
    "recovery normalizes inputs and initial reset request advances to code",
    "recovery resend failure keeps previous info and remains on code stage",
    "recovery reset success uses current fields and reaches done",
    "password change success keeps exact feedback and updates password drafts",
    "account security keeps one global action lock",
    "delete confirmation preserves validation failure and success effects",
    "blocked load keeps non401 inline and terminal 401 loading semantics",
    "blocked unblock removes matching id and uses one global username lock",
    "blocked unblock terminal 401 keeps busy username for activity exit",
):
    if test_name not in owner_test:
        errors.append(f"Security state characterization is missing test: {test_name}")

# Boundary/characterization PR intentionally leaves live screens pre-switch.
for required in (
    "import com.nova.app.core.auth.NovaAccountSecurityRepository",
    "NovaAccountSecurityRepository(context.applicationContext)",
    "rememberCoroutineScope()",
    "mutableStateOf",
):
    if required not in account_screens:
        errors.append(f"Security boundary PR must leave AccountSecurity screens pre-switch: {required}")

for required in (
    "import com.nova.app.core.social.NovaBlockedAccountsRepository",
    "NovaBlockedAccountsRepository(context.applicationContext)",
    "rememberCoroutineScope()",
    "mutableStateOf",
    "LaunchedEffect(Unit) { load() }",
):
    if required not in blocked_screen:
        errors.append(f"Security boundary PR must leave BlockedAccounts screen pre-switch: {required}")

# Preserve special-Activity modes and recovery entry while ownership is introduced.
for required in (
    'const val EXTRA_MODE = "account_security_mode"',
    'const val MODE_RECOVERY = "recovery"',
    'const val MODE_SECURITY = "security"',
    'const val MODE_BLOCKED = "blocked"',
    "MODE_RECOVERY -> PasswordRecoveryScreen(onBack = { finish() })",
    "MODE_BLOCKED -> BlockedAccountsScreen(",
    "else -> AccountSecurityScreen(",
    "Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK",
):
    if required not in activity:
        errors.append(f"AccountSecurityActivity protected seam changed or disappeared: {required}")

for required in (
    'secondaryActionText = "Forgot password?"',
    "Intent(context, AccountSecurityActivity::class.java)",
    "AccountSecurityActivity.MODE_RECOVERY",
):
    if required not in auth_screens:
        errors.append(f"Password-recovery entry seam changed or disappeared: {required}")

if errors:
    print("Security architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Security architecture check passed.")
