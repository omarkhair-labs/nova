package com.nova.app.feature.orbit

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.orbit.data.OrbitRepository
import com.nova.app.feature.orbit.domain.model.OrbitEvent
import com.nova.app.feature.orbit.domain.model.OrbitPage
import com.nova.app.feature.orbit.domain.model.OrbitPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class OrbitStateOwnerTest {
    @Test
    fun `initial load owns page and next cursor`() = runBlocking {
        val expected = listOf(event("like:1"), event("comment:2"))
        val owner = OrbitStateOwner(
            FakeOrbitRepository(
                results = mutableListOf(ApiResult.Success(OrbitPage(expected, "next"))),
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadNow(showSpinner = true)

        assertEquals(expected, owner.state.events)
        assertEquals("next", owner.state.nextCursor)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `401 expires session without inline error`() = runBlocking {
        val owner = OrbitStateOwner(
            FakeOrbitRepository(
                results = mutableListOf(ApiResult.Failure("expired", 401)),
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadNow(showSpinner = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `load more appends unique movement and advances cursor`() = runBlocking {
        val first = OrbitPage(listOf(event("like:1"), event("comment:2")), "cursor-1")
        val second = OrbitPage(listOf(event("comment:2"), event("repost:3")), null)
        val repository = FakeOrbitRepository(
            results = mutableListOf(ApiResult.Success(first), ApiResult.Success(second)),
        )
        val owner = OrbitStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))
        owner.loadNow()

        owner.loadMoreNow("cursor-1")

        assertEquals(listOf("like:1", "comment:2", "repost:3"), owner.state.events.map { it.id })
        assertEquals(null, owner.state.nextCursor)
        assertFalse(owner.state.loadingMore)
        assertEquals(listOf(null, "cursor-1"), repository.cursors)
    }

    @Test
    fun `load more failure keeps existing movement and releases busy state`() = runBlocking {
        val first = OrbitPage(listOf(event("like:1")), "cursor-1")
        val repository = FakeOrbitRepository(
            results = mutableListOf(
                ApiResult.Success(first),
                ApiResult.Failure("offline", 503),
            ),
        )
        val owner = OrbitStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))
        owner.loadNow()

        owner.loadMoreNow("cursor-1")

        assertEquals(listOf("like:1"), owner.state.events.map { it.id })
        assertEquals("offline", owner.state.error)
        assertFalse(owner.state.loadingMore)
    }
}


private class FakeOrbitRepository(
    private val results: MutableList<ApiResult<OrbitPage>>,
) : OrbitRepository {
    val cursors = mutableListOf<String?>()

    override suspend fun orbit(cursor: String?): ApiResult<OrbitPage> {
        cursors += cursor
        return results.removeFirst()
    }
}


private fun event(id: String) = OrbitEvent(
    id = id,
    kind = id.substringBefore(':'),
    actor = OrbitPerson(
        id = 7,
        username = "friend",
        name = "Friend",
        avatarUrl = "",
    ),
    createdAt = "2026-08-22T12:00:00+00:00",
    post = null,
    person = null,
    pulse = null,
    commentPreview = "",
)
