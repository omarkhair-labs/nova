package com.nova.app.feature.security

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaUser
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.security.data.BlockedAccountsRepository
import com.nova.app.feature.security.data.SecurityRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class SecurityStateOwnersTest {
    @Test
    fun `recovery normalizes inputs and initial reset request advances to code`() = runBlocking {
        val repository = FakeSecurityRepository(
            requestResults = mutableListOf(ApiResult.Success("sent")),
        )
        val owner = PasswordRecoveryStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.setEmail("  User@Example.com  ")
        owner.setCode("a12b345678")
        owner.requestResetCodeNow()

        assertEquals("User@Example.com", owner.state.email)
        assertEquals("123456", owner.state.code)
        assertEquals(PasswordRecoveryStage.Code, owner.state.stage)
        assertEquals("sent", owner.state.info)
        assertNull(owner.state.error)
        assertFalse(owner.state.loading)
        assertEquals(listOf("User@Example.com"), repository.requestCalls)
    }

    @Test
    fun `recovery resend failure keeps previous info and remains on code stage`() = runBlocking {
        val repository = FakeSecurityRepository(
            requestResults = mutableListOf(
                ApiResult.Success("first code sent"),
                ApiResult.Failure("offline", 503),
            ),
        )
        val owner = PasswordRecoveryStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))
        owner.setEmail("a@b.com")
        owner.requestResetCodeNow()

        owner.requestResetCodeNow()

        assertEquals(PasswordRecoveryStage.Code, owner.state.stage)
        assertEquals("first code sent", owner.state.info)
        assertEquals("offline", owner.state.error)
        assertFalse(owner.state.loading)
    }

    @Test
    fun `recovery reset success uses current fields and reaches done`() = runBlocking {
        val repository = FakeSecurityRepository(
            resetResults = mutableListOf(ApiResult.Success("changed")),
        )
        val owner = PasswordRecoveryStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))
        owner.setEmail("user@example.com")
        owner.setCode("12x3456")
        owner.setNewPassword("abcdefgh")
        owner.setConfirmPassword("abcdefgh")

        owner.resetPasswordNow()

        assertEquals(listOf(ResetCall("user@example.com", "123456", "abcdefgh")), repository.resetCalls)
        assertEquals(PasswordRecoveryStage.Done, owner.state.stage)
        assertEquals("changed", owner.state.info)
        assertNull(owner.state.error)
        assertFalse(owner.state.loading)
    }

    @Test
    fun `password change success keeps exact feedback and updates password drafts`() = runBlocking {
        val repository = FakeSecurityRepository(
            changeResults = mutableListOf(ApiResult.Success(user())),
        )
        val owner = AccountSecurityStateOwner(
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
            onAccountDeleted = {},
        )
        owner.setCurrentPassword("old-password")
        owner.setNewPassword("new-password")
        owner.setConfirmPassword("new-password")

        owner.changePasswordNow()

        assertEquals(listOf(ChangeCall("old-password", "new-password")), repository.changeCalls)
        assertEquals("new-password", owner.state.currentPassword)
        assertEquals("", owner.state.newPassword)
        assertEquals("", owner.state.confirmPassword)
        assertEquals("Password changed. Other sessions were signed out.", owner.state.info)
        assertNull(owner.state.error)
        assertNull(owner.state.loadingAction)
    }

    @Test
    fun `account security keeps one global action lock`() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val job = Job()
        val repository = FakeSecurityRepository(
            changeResults = mutableListOf(ApiResult.Success(user())),
            changeBlocker = release,
        )
        val owner = AccountSecurityStateOwner(
            repository = repository,
            scope = CoroutineScope(job + Dispatchers.Unconfined),
            onAccountDeleted = {},
        )
        owner.setCurrentPassword("old-password")
        owner.setNewPassword("new-password")
        owner.setConfirmPassword("new-password")

        owner.changePassword()
        assertEquals("change", owner.state.loadingAction)
        owner.revokeOtherSessions()
        owner.requestDeleteConfirmation()

        assertTrue(repository.revokeCalls.isEmpty())
        assertFalse(owner.state.showDeleteConfirm)

        release.complete(Unit)
        assertNull(owner.state.loadingAction)
        job.cancel()
    }

    @Test
    fun `delete confirmation preserves validation failure and success effects`() = runBlocking {
        val deleted = mutableListOf<String>()
        val failureRepository = FakeSecurityRepository(
            deleteResults = mutableListOf(ApiResult.Failure("wrong password", 400)),
        )
        val failureOwner = AccountSecurityStateOwner(
            repository = failureRepository,
            scope = CoroutineScope(Dispatchers.Unconfined),
            onAccountDeleted = { deleted += "deleted" },
        )

        failureOwner.requestDeleteConfirmation()
        assertEquals("Enter your current password first.", failureOwner.state.error)
        assertFalse(failureOwner.state.showDeleteConfirm)

        failureOwner.setCurrentPassword("secret")
        failureOwner.requestDeleteConfirmation()
        assertTrue(failureOwner.state.showDeleteConfirm)
        failureOwner.confirmDeleteNow()
        assertEquals("wrong password", failureOwner.state.error)
        assertFalse(failureOwner.state.showDeleteConfirm)
        assertNull(failureOwner.state.loadingAction)
        assertTrue(deleted.isEmpty())

        val successOwner = AccountSecurityStateOwner(
            repository = FakeSecurityRepository(
                deleteResults = mutableListOf(ApiResult.Success("deleted")),
            ),
            scope = CoroutineScope(Dispatchers.Unconfined),
            onAccountDeleted = { deleted += "deleted" },
        )
        successOwner.setCurrentPassword("secret")
        successOwner.requestDeleteConfirmation()
        successOwner.confirmDeleteNow()
        assertEquals(listOf("deleted"), deleted)
        assertEquals("delete", successOwner.state.loadingAction)
        assertTrue(successOwner.state.showDeleteConfirm)
    }

    @Test
    fun `blocked load keeps non401 inline and terminal 401 loading semantics`() = runBlocking {
        val terminal = mutableListOf<String>()
        val non401 = BlockedAccountsStateOwner(
            repository = FakeBlockedAccountsRepository(
                loadResults = mutableListOf(ApiResult.Failure("offline", 503)),
            ),
            scope = CoroutineScope(Dispatchers.Unconfined),
            onSessionExpired = { terminal += "expired" },
        )
        non401.loadNow()
        assertEquals("offline", non401.state.errorMessage)
        assertFalse(non401.state.isLoading)
        assertTrue(terminal.isEmpty())

        val expired = BlockedAccountsStateOwner(
            repository = FakeBlockedAccountsRepository(
                loadResults = mutableListOf(ApiResult.Failure("expired", 401)),
            ),
            scope = CoroutineScope(Dispatchers.Unconfined),
            onSessionExpired = { terminal += "expired" },
        )
        expired.loadNow()
        assertEquals(listOf("expired"), terminal)
        assertTrue(expired.state.isLoading)
        assertNull(expired.state.errorMessage)
    }

    @Test
    fun `blocked unblock removes matching id and uses one global username lock`() = runBlocking {
        val alice = person(1, "alice")
        val bob = person(2, "bob")
        val release = CompletableDeferred<Unit>()
        val job = Job()
        val repository = FakeBlockedAccountsRepository(
            loadResults = mutableListOf(ApiResult.Success(listOf(alice, bob))),
            unblockResults = mutableListOf(ApiResult.Success(Unit)),
            unblockBlocker = release,
        )
        val owner = BlockedAccountsStateOwner(
            repository = repository,
            scope = CoroutineScope(job + Dispatchers.Unconfined),
            onSessionExpired = {},
        )
        owner.loadNow()

        owner.unblock(alice)
        assertEquals("alice", owner.state.unblockingUsername)
        owner.unblock(bob)
        assertEquals(listOf("alice"), repository.unblockCalls)

        release.complete(Unit)
        assertEquals(listOf(2L), owner.state.blocked.map { it.id })
        assertNull(owner.state.unblockingUsername)
        job.cancel()
    }

    @Test
    fun `blocked unblock terminal 401 keeps busy username for activity exit`() = runBlocking {
        val terminal = mutableListOf<String>()
        val alice = person(1, "alice")
        val repository = FakeBlockedAccountsRepository(
            loadResults = mutableListOf(ApiResult.Success(listOf(alice))),
            unblockResults = mutableListOf(ApiResult.Failure("expired", 401)),
        )
        val owner = BlockedAccountsStateOwner(
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
            onSessionExpired = { terminal += "expired" },
        )
        owner.loadNow()

        owner.unblockNow(alice)

        assertEquals(listOf("expired"), terminal)
        assertEquals("alice", owner.state.unblockingUsername)
        assertEquals(listOf(1L), owner.state.blocked.map { it.id })
        assertNull(owner.state.errorMessage)
    }
}


private data class ResetCall(val email: String, val code: String, val newPassword: String)
private data class ChangeCall(val currentPassword: String, val newPassword: String)


private class FakeSecurityRepository(
    private val requestResults: MutableList<ApiResult<String>> = mutableListOf(),
    private val resetResults: MutableList<ApiResult<String>> = mutableListOf(),
    private val changeResults: MutableList<ApiResult<NovaUser>> = mutableListOf(),
    private val revokeResults: MutableList<ApiResult<NovaUser>> = mutableListOf(),
    private val deleteResults: MutableList<ApiResult<String>> = mutableListOf(),
    private val changeBlocker: CompletableDeferred<Unit>? = null,
) : SecurityRepository {
    val requestCalls = mutableListOf<String>()
    val resetCalls = mutableListOf<ResetCall>()
    val changeCalls = mutableListOf<ChangeCall>()
    val revokeCalls = mutableListOf<String>()
    val deleteCalls = mutableListOf<String>()

    override suspend fun requestPasswordReset(email: String): ApiResult<String> {
        requestCalls += email
        return requestResults.removeFirstOrNull() ?: ApiResult.Success("sent")
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<String> {
        resetCalls += ResetCall(email, code, newPassword)
        return resetResults.removeFirstOrNull() ?: ApiResult.Success("changed")
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<NovaUser> {
        changeCalls += ChangeCall(currentPassword, newPassword)
        changeBlocker?.await()
        return changeResults.removeFirstOrNull() ?: ApiResult.Success(user())
    }

    override suspend fun revokeOtherSessions(currentPassword: String): ApiResult<NovaUser> {
        revokeCalls += currentPassword
        return revokeResults.removeFirstOrNull() ?: ApiResult.Success(user())
    }

    override suspend fun deleteAccount(currentPassword: String): ApiResult<String> {
        deleteCalls += currentPassword
        return deleteResults.removeFirstOrNull() ?: ApiResult.Success("deleted")
    }
}


private class FakeBlockedAccountsRepository(
    private val loadResults: MutableList<ApiResult<List<NovaPerson>>> = mutableListOf(),
    private val unblockResults: MutableList<ApiResult<Unit>> = mutableListOf(),
    private val unblockBlocker: CompletableDeferred<Unit>? = null,
) : BlockedAccountsRepository {
    val unblockCalls = mutableListOf<String>()

    override suspend fun blockedAccounts(): ApiResult<List<NovaPerson>> =
        loadResults.removeFirstOrNull() ?: ApiResult.Success(emptyList())

    override suspend fun unblock(username: String): ApiResult<Unit> {
        unblockCalls += username
        unblockBlocker?.await()
        return unblockResults.removeFirstOrNull() ?: ApiResult.Success(Unit)
    }
}


private fun user() = NovaUser(
    id = 1,
    email = "user@example.com",
    username = "user",
    name = "User",
    avatarUrl = "",
)


private fun person(id: Long, username: String) = NovaPerson(
    id = id,
    username = username,
    name = username,
    avatarUrl = "",
    followersCount = 0,
    followingCount = 0,
    postsCount = 0,
    isFollowing = false,
)
