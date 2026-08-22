package com.nova.app.feature.rooms

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.rooms.data.RoomRepository
import com.nova.app.feature.rooms.domain.model.RoomConversation
import com.nova.app.feature.rooms.domain.model.RoomDetail
import com.nova.app.feature.rooms.domain.model.RoomItem
import com.nova.app.feature.rooms.domain.model.RoomItemPage
import com.nova.app.feature.rooms.domain.model.RoomSections
import com.nova.app.feature.rooms.domain.model.RoomSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class RoomsStateOwnerTest {
    @Test
    fun `Rooms list load owns summaries and releases spinner`() = runBlocking {
        val expected = listOf(summary(1), summary(2))
        val repository = FakeRoomRepository(roomsResult = ApiResult.Success(expected))
        val owner = RoomsStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(showSpinner = true)

        assertEquals(expected, owner.state.rooms)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `Rooms list 401 emits session expiry without inline error`() = runBlocking {
        val repository = FakeRoomRepository(
            roomsResult = ApiResult.Failure("expired", 401),
        )
        val owner = RoomsStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(showSpinner = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `Room load combines detail and first timeline page`() = runBlocking {
        val detail = detail(7)
        val firstPage = RoomItemPage(
            pinned = listOf(item(90, pinned = true)),
            items = listOf(item(30), item(20)),
            nextBefore = 20,
        )
        val repository = FakeRoomRepository(
            detailResult = ApiResult.Success(detail),
            itemResults = mutableListOf(ApiResult.Success(firstPage)),
        )
        val owner = RoomStateOwner(7, repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(showSpinner = true)

        assertEquals(detail, owner.state.detail)
        assertEquals(listOf(90L), owner.state.pinned.map { it.id })
        assertEquals(listOf(30L, 20L), owner.state.items.map { it.id })
        assertEquals(20L, owner.state.nextBefore)
        assertFalse(owner.state.loading)
    }

    @Test
    fun `Room section selection requests only selected section`() = runBlocking {
        val repository = FakeRoomRepository(
            itemResults = mutableListOf(
                ApiResult.Success(RoomItemPage(emptyList(), listOf(item(8, kind = "photo")), null)),
            ),
        )
        val owner = RoomStateOwner(7, repository, CoroutineScope(Dispatchers.Unconfined))

        owner.selectKind("photo")

        assertEquals("photo", owner.state.selectedKind)
        assertEquals(listOf("photo"), repository.itemKinds)
        assertEquals(listOf(8L), owner.state.items.map { it.id })
    }

    @Test
    fun `Room paging appends unique items and advances before cursor`() = runBlocking {
        val repository = FakeRoomRepository(
            itemResults = mutableListOf(
                ApiResult.Success(RoomItemPage(emptyList(), listOf(item(30), item(20)), 20)),
                ApiResult.Success(RoomItemPage(emptyList(), listOf(item(20), item(10)), null)),
            ),
        )
        val owner = RoomStateOwner(7, repository, CoroutineScope(Dispatchers.Unconfined))
        owner.selectKind(null)

        owner.loadMoreNow(20)

        assertEquals(listOf(30L, 20L, 10L), owner.state.items.map { it.id })
        assertNull(owner.state.nextBefore)
        assertFalse(owner.state.loadingMore)
        assertEquals(listOf(null, 20L), repository.itemBefore)
    }

    @Test
    fun `Room description update replaces detail`() = runBlocking {
        val original = detail(7, description = "Old")
        val changed = detail(7, description = "New")
        val repository = FakeRoomRepository(
            detailResult = ApiResult.Success(original),
            updateResult = ApiResult.Success(changed),
        )
        val owner = RoomStateOwner(7, repository, CoroutineScope(Dispatchers.Unconfined))
        owner.loadNow()

        owner.updateDescriptionNow("New")

        assertEquals("New", owner.state.detail?.description)
        assertFalse(owner.state.savingDescription)
        assertEquals(listOf("New"), repository.descriptions)
    }
}


private class FakeRoomRepository(
    private val roomsResult: ApiResult<List<RoomSummary>> = ApiResult.Success(emptyList()),
    private val detailResult: ApiResult<RoomDetail> = ApiResult.Success(detail(7)),
    private val itemResults: MutableList<ApiResult<RoomItemPage>> = mutableListOf(
        ApiResult.Success(RoomItemPage(emptyList(), emptyList(), null)),
    ),
    private val updateResult: ApiResult<RoomDetail> = detailResult,
) : RoomRepository {
    val itemKinds = mutableListOf<String?>()
    val itemBefore = mutableListOf<Long?>()
    val descriptions = mutableListOf<String>()

    override suspend fun rooms(): ApiResult<List<RoomSummary>> = roomsResult

    override suspend fun room(conversationId: Long): ApiResult<RoomDetail> = detailResult

    override suspend fun items(
        conversationId: Long,
        kind: String?,
        before: Long?,
        limit: Int,
    ): ApiResult<RoomItemPage> {
        itemKinds += kind
        itemBefore += before
        return itemResults.removeFirst()
    }

    override suspend fun updateDescription(
        conversationId: Long,
        description: String,
    ): ApiResult<RoomDetail> {
        descriptions += description
        return updateResult
    }
}


private fun summary(id: Long) = RoomSummary(
    conversation = conversation(id),
    description = "Room $id",
)


private fun detail(id: Long, description: String = "Room") = RoomDetail(
    conversation = conversation(id),
    description = description,
    sections = RoomSections(all = 2, note = 2),
    members = emptyList(),
)


private fun conversation(id: Long) = RoomConversation(
    id = id,
    title = "Room $id",
    avatarUrl = "",
    membersCount = 3,
    currentUserRole = "member",
    unreadCount = 0,
    updatedAt = "2026-08-22T12:00:00Z",
)


private fun item(
    id: Long,
    kind: String = "note",
    pinned: Boolean = false,
) = RoomItem(
    id = id,
    kind = kind,
    createdBy = null,
    title = "",
    body = "Item $id",
    url = "",
    mediaUrl = "",
    scheduledFor = null,
    pinned = pinned,
    createdAt = "2026-08-22T12:00:00Z",
    updatedAt = "2026-08-22T12:00:00Z",
)
