package com.nova.app.feature.messages.group

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.group.data.GroupManagementRepository
import com.nova.app.feature.messages.group.data.GroupMembershipRepository
import com.nova.app.feature.messages.group.model.GroupDetail
import com.nova.app.feature.messages.group.model.GroupMember
import com.nova.app.feature.messages.group.model.ManagedGroupDetail
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class GroupInfoViewModelTest {
    @Test
    fun initialLoadAppliesDetailAndPublishesUpdateVersion() = runBlocking {
        val management = FakeManagementRepository()
        val viewModel = viewModel(management, FakeMembershipRepository(), this)
        yield()

        assertEquals("Nova group", viewModel.state.detail?.title)
        assertEquals("Nova group", viewModel.state.titleDraft)
        assertEquals("me", viewModel.state.currentUsername)
        assertEquals(1, viewModel.state.groupUpdatedVersion)
        assertFalse(viewModel.state.loading)
        assertEquals(listOf(CONVERSATION_ID), management.detailCalls)
    }

    @Test
    fun blankRenameKeepsExistingValidationAndDoesNotWrite() = runBlocking {
        val management = FakeManagementRepository()
        val viewModel = viewModel(management, FakeMembershipRepository(), this)
        yield()

        viewModel.toggleTitleEditing()
        viewModel.updateTitleDraft("   ")
        viewModel.rename()
        yield()

        assertEquals("Give the group a name.", viewModel.state.errorMessage)
        assertTrue(management.renameCalls.isEmpty())
        assertTrue(viewModel.state.editingTitle)
    }

    @Test
    fun successfulRenameAppliesReturnedDetailAndClosesEditor() = runBlocking {
        val management = FakeManagementRepository().apply {
            renameResult = ApiResult.Success(detail(title = "New name", membersCount = 4))
        }
        val viewModel = viewModel(management, FakeMembershipRepository(), this)
        yield()

        viewModel.toggleTitleEditing()
        viewModel.updateTitleDraft("  New name  ")
        viewModel.rename()
        yield()

        assertEquals(listOf(RenameCall(CONVERSATION_ID, "New name")), management.renameCalls)
        assertEquals("New name", viewModel.state.detail?.title)
        assertEquals(4, viewModel.state.detail?.membersCount)
        assertEquals(2, viewModel.state.groupUpdatedVersion)
        assertFalse(viewModel.state.editingTitle)
        assertNull(viewModel.state.busyAction)
    }

    @Test
    fun deletedRemoveMemberSignalsGroupLeftWithoutReloadingManagementDetail() = runBlocking {
        val management = FakeManagementRepository()
        val membership = FakeMembershipRepository().apply {
            removeResult = ApiResult.Success(null)
        }
        val viewModel = viewModel(management, membership, this)
        yield()

        viewModel.removeMember(member)
        yield()

        assertEquals(listOf(RemoveCall(CONVERSATION_ID, "other")), membership.removeCalls)
        assertEquals(1, viewModel.state.groupLeftVersion)
        assertEquals(1, management.detailCalls.size)
        assertNull(viewModel.state.busyAction)
    }

    @Test
    fun terminal401PublishesSessionEffectWithoutInlineError() = runBlocking {
        val management = FakeManagementRepository().apply {
            detailResult = ApiResult.Failure("expired", 401)
        }
        val viewModel = viewModel(management, FakeMembershipRepository(), this)
        yield()

        assertEquals(1, viewModel.state.sessionExpiryVersion)
        assertNull(viewModel.state.errorMessage)
        assertFalse(viewModel.state.loading)
    }

    @Test
    fun failedLeaveRestoresEnabledStateAndShowsExistingInlineError() = runBlocking {
        val membership = FakeMembershipRepository().apply {
            leaveResult = ApiResult.Failure("could not leave", 500)
        }
        val viewModel = viewModel(FakeManagementRepository(), membership, this)
        yield()

        viewModel.leave()
        yield()

        assertEquals(listOf(CONVERSATION_ID), membership.leaveCalls)
        assertFalse(viewModel.state.leaving)
        assertFalse(viewModel.state.blocked)
        assertEquals("could not leave", viewModel.state.errorMessage)
    }

    private fun viewModel(
        management: GroupManagementRepository,
        membership: GroupMembershipRepository,
        scope: CoroutineScope,
    ) = GroupInfoViewModel(
        conversationId = CONVERSATION_ID,
        managementRepository = management,
        membershipRepository = membership,
        currentUsername = "me",
        workScope = scope,
    )

    private class FakeManagementRepository : GroupManagementRepository {
        var detailResult: ApiResult<ManagedGroupDetail> = ApiResult.Success(detail())
        var renameResult: ApiResult<ManagedGroupDetail> = ApiResult.Success(detail())
        var avatarResult: ApiResult<ManagedGroupDetail> = ApiResult.Success(detail())
        var roleResult: ApiResult<ManagedGroupDetail> = ApiResult.Success(detail())

        val detailCalls = mutableListOf<Long>()
        val renameCalls = mutableListOf<RenameCall>()

        override suspend fun detail(conversationId: Long): ApiResult<ManagedGroupDetail> {
            detailCalls += conversationId
            return detailResult
        }

        override suspend fun rename(conversationId: Long, title: String): ApiResult<ManagedGroupDetail> {
            renameCalls += RenameCall(conversationId, title)
            return renameResult
        }

        override suspend fun updateAvatar(conversationId: Long, uri: Uri): ApiResult<ManagedGroupDetail> = avatarResult

        override suspend fun removeAvatar(conversationId: Long): ApiResult<ManagedGroupDetail> = avatarResult

        override suspend fun setRole(
            conversationId: Long,
            username: String,
            role: String,
        ): ApiResult<ManagedGroupDetail> = roleResult
    }

    private class FakeMembershipRepository : GroupMembershipRepository {
        var removeResult: ApiResult<GroupDetail?> = ApiResult.Success(null)
        var leaveResult: ApiResult<Boolean> = ApiResult.Success(true)
        var deleteResult: ApiResult<Unit> = ApiResult.Success(Unit)

        val removeCalls = mutableListOf<RemoveCall>()
        val leaveCalls = mutableListOf<Long>()

        override suspend fun createGroup(title: String, usernames: List<String>): ApiResult<NovaConversation> =
            error("Unexpected createGroup call")

        override suspend fun detail(conversationId: Long): ApiResult<GroupDetail> =
            error("Unexpected detail call")

        override suspend fun addMembers(conversationId: Long, usernames: List<String>): ApiResult<GroupDetail> =
            error("Unexpected addMembers call")

        override suspend fun removeMember(conversationId: Long, username: String): ApiResult<GroupDetail?> {
            removeCalls += RemoveCall(conversationId, username)
            return removeResult
        }

        override suspend fun leaveGroup(conversationId: Long): ApiResult<Boolean> {
            leaveCalls += conversationId
            return leaveResult
        }

        override suspend fun deleteGroup(conversationId: Long): ApiResult<Unit> = deleteResult
    }

    private data class RenameCall(val conversationId: Long, val title: String)
    private data class RemoveCall(val conversationId: Long, val username: String)

    private companion object {
        const val CONVERSATION_ID = 42L

        val member = GroupMember(
            user = NovaPostAuthor(
                id = 2L,
                username = "other",
                name = "Other",
                avatarUrl = "",
            ),
            role = "member",
            joinedAt = "",
        )

        fun detail(
            title: String = "Nova group",
            membersCount: Int = 2,
        ) = ManagedGroupDetail(
            title = title,
            avatarUrl = "",
            membersCount = membersCount,
            currentUserRole = "owner",
            members = listOf(member),
        )
    }
}
