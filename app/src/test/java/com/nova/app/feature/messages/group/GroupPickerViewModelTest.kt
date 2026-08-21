package com.nova.app.feature.messages.group

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.group.data.GroupMembershipRepository
import com.nova.app.feature.messages.group.data.GroupPeopleRepository
import com.nova.app.feature.messages.group.model.GroupDetail
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class GroupPickerViewModelTest {
    @Test
    fun addMembersFiltersExistingPeopleAndPublishesUpdateAfterSubmit() = runBlocking {
        val people = FakeGroupPeopleRepository(
            ApiResult.Success(listOf(person(1, "alice"), person(2, "bob"))),
        )
        val membership = FakeGroupMembershipRepository().apply {
            addMembersResult = ApiResult.Success(groupDetail())
        }
        val viewModel = AddGroupMembersViewModel(
            conversationId = 42L,
            existingUsernames = setOf("alice"),
            membershipRepository = membership,
            peopleRepository = people,
            debounceMillis = 0L,
            workScope = this,
        )
        yield()

        assertEquals(listOf("bob"), viewModel.state.people.map { it.username })
        viewModel.toggleSelection("bob")
        viewModel.add()
        yield()

        assertEquals(listOf(AddCall(42L, listOf("bob"))), membership.addCalls)
        assertEquals(1, viewModel.state.updatedVersion)
        assertTrue(viewModel.state.adding)
    }

    @Test
    fun addMembersTerminal401RaisesSessionEffectAndUnlocksSubmission() = runBlocking {
        val membership = FakeGroupMembershipRepository().apply {
            addMembersResult = ApiResult.Failure("expired", 401)
        }
        val viewModel = AddGroupMembersViewModel(
            conversationId = 42L,
            existingUsernames = emptySet(),
            membershipRepository = membership,
            peopleRepository = FakeGroupPeopleRepository(ApiResult.Success(listOf(person(2, "bob")))),
            debounceMillis = 0L,
            workScope = this,
        )
        yield()
        viewModel.toggleSelection("bob")
        viewModel.add()
        yield()

        assertEquals(1, viewModel.state.sessionExpiryVersion)
        assertFalse(viewModel.state.adding)
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun newGroupPreservesCapsSelectionRequirementAndPublishesCreatedConversation() = runBlocking {
        val membership = FakeGroupMembershipRepository().apply {
            createResult = ApiResult.Success(conversation())
        }
        val people = FakeGroupPeopleRepository(
            ApiResult.Success(listOf(person(1, "alice"), person(2, "bob"))),
        )
        val viewModel = NewGroupViewModel(
            membershipRepository = membership,
            peopleRepository = people,
            debounceMillis = 0L,
            workScope = this,
        )
        yield()

        viewModel.updateTitle("x".repeat(90))
        viewModel.updateQuery("q".repeat(50))
        yield()
        assertEquals(80, viewModel.state.title.length)
        assertEquals(40, viewModel.state.query.length)

        viewModel.toggleSelection("alice")
        viewModel.createGroup()
        yield()
        assertTrue(membership.createCalls.isEmpty())

        viewModel.toggleSelection("bob")
        viewModel.createGroup()
        yield()

        assertEquals(1, membership.createCalls.size)
        assertEquals(80, membership.createCalls.single().title.length)
        assertEquals(setOf("alice", "bob"), membership.createCalls.single().usernames.toSet())
        assertEquals(1, viewModel.state.conversationReadyVersion)
        assertEquals(42L, viewModel.state.readyConversation?.id)
    }

    @Test
    fun newGroupPeople401ProducesTerminalEffectWithoutInlineError() = runBlocking {
        val viewModel = NewGroupViewModel(
            membershipRepository = FakeGroupMembershipRepository(),
            peopleRepository = FakeGroupPeopleRepository(ApiResult.Failure("expired", 401)),
            debounceMillis = 0L,
            workScope = this,
        )
        yield()

        assertEquals(1, viewModel.state.sessionExpiryVersion)
        assertNull(viewModel.state.errorMessage)
        assertFalse(viewModel.state.loadingPeople)
    }

    private class FakeGroupPeopleRepository(
        var result: ApiResult<List<NovaPerson>>,
    ) : GroupPeopleRepository {
        val queries = mutableListOf<String>()

        override suspend fun people(query: String): ApiResult<List<NovaPerson>> {
            queries += query
            return result
        }
    }

    private class FakeGroupMembershipRepository : GroupMembershipRepository {
        var createResult: ApiResult<NovaConversation> = ApiResult.Failure("unused")
        var addMembersResult: ApiResult<GroupDetail> = ApiResult.Failure("unused")
        val createCalls = mutableListOf<CreateCall>()
        val addCalls = mutableListOf<AddCall>()

        override suspend fun createGroup(title: String, usernames: List<String>): ApiResult<NovaConversation> {
            createCalls += CreateCall(title, usernames)
            return createResult
        }

        override suspend fun detail(conversationId: Long): ApiResult<GroupDetail> = ApiResult.Failure("unused")

        override suspend fun addMembers(conversationId: Long, usernames: List<String>): ApiResult<GroupDetail> {
            addCalls += AddCall(conversationId, usernames)
            return addMembersResult
        }

        override suspend fun removeMember(conversationId: Long, username: String): ApiResult<GroupDetail?> =
            ApiResult.Failure("unused")

        override suspend fun leaveGroup(conversationId: Long): ApiResult<Boolean> = ApiResult.Failure("unused")

        override suspend fun deleteGroup(conversationId: Long): ApiResult<Unit> = ApiResult.Failure("unused")
    }

    private data class CreateCall(val title: String, val usernames: List<String>)
    private data class AddCall(val conversationId: Long, val usernames: List<String>)

    private companion object {
        fun person(id: Long, username: String) = NovaPerson(
            id = id,
            username = username,
            name = username.replaceFirstChar { it.uppercase() },
            avatarUrl = "",
            followersCount = 0,
            followingCount = 0,
            postsCount = 0,
            isFollowing = false,
        )

        fun conversation() = NovaConversation(
            id = 42L,
            otherUser = NovaPostAuthor(0L, "group", "", ""),
            lastMessage = null,
            unreadCount = 0,
            createdAt = "",
            updatedAt = "",
            kind = "group",
            title = "Study group",
            membersCount = 3,
        )

        fun groupDetail() = GroupDetail(conversation = conversation(), members = emptyList())
    }
}
