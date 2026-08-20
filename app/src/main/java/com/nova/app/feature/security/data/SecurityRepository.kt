package com.nova.app.feature.security.data

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaUser
import com.nova.app.feature.people.domain.model.NovaPerson


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

    suspend fun deleteAccount(currentPassword: String): ApiResult<String>
}


interface BlockedAccountsRepository {
    suspend fun blockedAccounts(): ApiResult<List<NovaPerson>>

    suspend fun unblock(username: String): ApiResult<Unit>
}
