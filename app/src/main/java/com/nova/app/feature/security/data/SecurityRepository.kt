package com.nova.app.feature.security.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.auth.domain.model.NovaUser
import com.nova.app.feature.people.domain.model.NovaPerson


data class SecuritySession(
    val id: String,
    val deviceName: String,
    val platform: String,
    val ipAddress: String,
    val lastSeenAt: String,
    val isCurrent: Boolean,
)


interface SecurityRepository {
    suspend fun requestPasswordReset(email: String): ApiResult<String>

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): ApiResult<String>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): ApiResult<NovaUser>

    suspend fun revokeOtherSessions(currentPassword: String): ApiResult<NovaUser>

    suspend fun sessions(): ApiResult<List<SecuritySession>> =
        ApiResult.Failure("Session history is unavailable.")

    suspend fun revokeSession(sessionId: String): ApiResult<Unit> =
        ApiResult.Failure("Session revocation is unavailable.")

    suspend fun deleteAccount(currentPassword: String): ApiResult<String>
}


interface BlockedAccountsRepository {
    suspend fun blockedAccounts(): ApiResult<List<NovaPerson>>

    suspend fun unblock(username: String): ApiResult<Unit>
}
