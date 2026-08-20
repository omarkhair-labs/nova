package com.nova.app.feature.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.security.data.BlockedAccountsRepository
import com.nova.app.feature.security.data.SecurityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


enum class PasswordRecoveryStage { Email, Code, Done }


data class PasswordRecoveryUiState(
    val stage: PasswordRecoveryStage = PasswordRecoveryStage.Email,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)


class PasswordRecoveryStateOwner(
    private val repository: SecurityRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(PasswordRecoveryUiState())
        private set

    fun setEmail(value: String) {
        state = state.copy(email = value.trim())
    }

    fun setCode(value: String) {
        state = state.copy(code = value.filter(Char::isDigit).take(6))
    }

    fun setNewPassword(value: String) {
        state = state.copy(newPassword = value)
    }

    fun setConfirmPassword(value: String) {
        state = state.copy(confirmPassword = value)
    }

    fun requestResetCode() {
        if (state.loading) return
        scope.launch { requestResetCodeNow() }
    }

    internal suspend fun requestResetCodeNow() {
        if (state.loading) return
        val startingStage = state.stage
        state = state.copy(
            loading = true,
            error = null,
            info = if (startingStage == PasswordRecoveryStage.Email) null else state.info,
        )
        when (val result = repository.requestPasswordReset(state.email)) {
            is ApiResult.Success -> {
                state = state.copy(
                    info = result.value,
                    stage = if (startingStage == PasswordRecoveryStage.Email) {
                        PasswordRecoveryStage.Code
                    } else {
                        state.stage
                    },
                )
            }
            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(loading = false)
    }

    fun resetPassword() {
        if (state.loading) return
        scope.launch { resetPasswordNow() }
    }

    internal suspend fun resetPasswordNow() {
        if (state.loading) return
        state = state.copy(loading = true, error = null, info = null)
        when (
            val result = repository.resetPassword(
                email = state.email,
                code = state.code,
                newPassword = state.newPassword,
            )
        ) {
            is ApiResult.Success -> state = state.copy(
                info = result.value,
                stage = PasswordRecoveryStage.Done,
            )
            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(loading = false)
    }
}


data class AccountSecurityUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val loadingAction: String? = null,
    val error: String? = null,
    val info: String? = null,
    val showDeleteConfirm: Boolean = false,
)


class AccountSecurityStateOwner(
    private val repository: SecurityRepository,
    private val scope: CoroutineScope,
    private val onAccountDeleted: () -> Unit,
) {
    var state by mutableStateOf(AccountSecurityUiState())
        private set

    fun setCurrentPassword(value: String) {
        state = state.copy(currentPassword = value)
    }

    fun setNewPassword(value: String) {
        state = state.copy(newPassword = value)
    }

    fun setConfirmPassword(value: String) {
        state = state.copy(confirmPassword = value)
    }

    fun changePassword() {
        if (state.loadingAction != null) return
        scope.launch { changePasswordNow() }
    }

    internal suspend fun changePasswordNow() {
        if (state.loadingAction != null) return
        state = state.copy(loadingAction = "change", error = null, info = null)
        when (
            val result = repository.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword,
            )
        ) {
            is ApiResult.Success -> {
                state = state.copy(
                    info = "Password changed. Other sessions were signed out.",
                    currentPassword = state.newPassword,
                    newPassword = "",
                    confirmPassword = "",
                )
            }
            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(loadingAction = null)
    }

    fun revokeOtherSessions() {
        if (state.loadingAction != null || state.currentPassword.isBlank()) return
        scope.launch { revokeOtherSessionsNow() }
    }

    internal suspend fun revokeOtherSessionsNow() {
        if (state.loadingAction != null || state.currentPassword.isBlank()) return
        state = state.copy(loadingAction = "revoke", error = null, info = null)
        when (val result = repository.revokeOtherSessions(state.currentPassword)) {
            is ApiResult.Success -> state = state.copy(info = "Other Nova sessions were signed out.")
            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(loadingAction = null)
    }

    fun requestDeleteConfirmation() {
        if (state.loadingAction != null) return
        if (state.currentPassword.isBlank()) {
            state = state.copy(error = "Enter your current password first.")
        } else {
            state = state.copy(error = null, showDeleteConfirm = true)
        }
    }

    fun dismissDeleteConfirmation() {
        if (state.loadingAction == null) {
            state = state.copy(showDeleteConfirm = false)
        }
    }

    fun confirmDelete() {
        if (state.loadingAction != null) return
        scope.launch { confirmDeleteNow() }
    }

    internal suspend fun confirmDeleteNow() {
        if (state.loadingAction != null) return
        state = state.copy(loadingAction = "delete", error = null, info = null)
        when (val result = repository.deleteAccount(state.currentPassword)) {
            is ApiResult.Success -> onAccountDeleted()
            is ApiResult.Failure -> {
                state = state.copy(
                    error = result.message,
                    showDeleteConfirm = false,
                    loadingAction = null,
                )
            }
        }
    }
}


data class BlockedAccountsUiState(
    val blocked: List<NovaPerson> = emptyList(),
    val isLoading: Boolean = true,
    val unblockingUsername: String? = null,
    val errorMessage: String? = null,
)


class BlockedAccountsStateOwner(
    private val repository: BlockedAccountsRepository,
    private val scope: CoroutineScope,
    private val onSessionExpired: () -> Unit,
) {
    var state by mutableStateOf(BlockedAccountsUiState())
        private set

    fun load() {
        scope.launch { loadNow() }
    }

    internal suspend fun loadNow() {
        state = state.copy(isLoading = true, errorMessage = null)
        when (val result = repository.blockedAccounts()) {
            is ApiResult.Success -> state = state.copy(blocked = result.value)
            is ApiResult.Failure -> {
                if (result.statusCode == 401) {
                    onSessionExpired()
                    return
                }
                state = state.copy(errorMessage = result.message)
            }
        }
        state = state.copy(isLoading = false)
    }

    fun unblock(person: NovaPerson) {
        if (state.unblockingUsername != null) return
        scope.launch { unblockNow(person) }
    }

    internal suspend fun unblockNow(person: NovaPerson) {
        if (state.unblockingUsername != null) return
        state = state.copy(unblockingUsername = person.username, errorMessage = null)
        when (val result = repository.unblock(person.username)) {
            is ApiResult.Success -> state = state.copy(
                blocked = state.blocked.filterNot { it.id == person.id },
            )
            is ApiResult.Failure -> {
                if (result.statusCode == 401) {
                    onSessionExpired()
                    return
                }
                state = state.copy(errorMessage = result.message)
            }
        }
        state = state.copy(unblockingUsername = null)
    }
}
