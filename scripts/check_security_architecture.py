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

# Exit gate: all three live Security surfaces must render stable owner state and
# use AppContainer-owned contracts rather than constructing concrete repositories
# or launching request coroutines themselves.
for required in (
    "import com.nova.app.app.appContainer",
    "import com.nova.app.feature.security.PasswordRecoveryStateOwner",
    "import com.nova.app.feature.security.PasswordRecoveryStage",
    "import com.nova.app.feature.security.AccountSecurityStateOwner",
    "PasswordRecoveryStateOwner(",
    "repository = container.securityRepository",
    "when (state.stage)",
    "onValueChange = owner::setEmail",
    "onValueChange = owner::setCode",
    "onClick = owner::requestResetCode",
    "onClick = owner::resetPassword",
    "AccountSecurityStateOwner(",
    "onAccountDeleted = { currentOnAccountDeleted() }",
    "onValueChange = owner::setCurrentPassword",
    "onValueChange = owner::setNewPassword",
    "onValueChange = owner::setConfirmPassword",
    "onClick = owner::changePassword",
    "onClick = owner::revokeOtherSessions",
    "onClick = owner::requestDeleteConfirmation",
    "onClick = owner::confirmDelete",
):
    if required not in account_screens:
        errors.append(f"live Account Security/Recovery owner wiring is missing seam: {required}")

for forbidden in (
    "NovaAccountSecurityRepository",
    "import com.nova.app.core.network.ApiResult",
    "mutableStateOf",
    "scope.launch",
    "repository.requestPasswordReset",
    "repository.resetPassword",
    "repository.changePassword",
    "repository.revokeOtherSessions",
    "repository.deleteAccount",
):
    if forbidden in account_screens:
        errors.append(f"Security exit gate rejects route-owned account-security orchestration: {forbidden}")

for required in (
    "import com.nova.app.app.appContainer",
    "import com.nova.app.feature.security.BlockedAccountsStateOwner",
    "BlockedAccountsStateOwner(",
    "repository = container.blockedAccountsRepository",
    "onSessionExpired = { currentOnSessionExpired() }",
    "LaunchedEffect(Unit) { owner.load() }",
    'NovaSecondaryButton(text = "Try again", onClick = owner::load)',
    "owner.unblock(person)",
    "items(state.blocked, key = { it.id })",
):
    if required not in blocked_screen:
        errors.append(f"live Blocked Accounts owner wiring is missing seam: {required}")

for forbidden in (
    "NovaBlockedAccountsRepository",
    "import com.nova.app.core.network.ApiResult",
    "import com.nova.app.core.network.NovaPerson",
    "mutableStateOf",
    "scope.launch",
    "repository.blockedAccounts",
    "repository.unblock",
):
    if forbidden in blocked_screen:
        errors.append(f"Security exit gate rejects route-owned blocked-account orchestration: {forbidden}")

# Preserve special-Activity modes and recovery entry after the live switch.
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
