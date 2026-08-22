package com.nova.app.feature.memories

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.memories.data.MemoryRepository
import com.nova.app.feature.memories.domain.model.MemoryStats
import com.nova.app.feature.memories.domain.model.WeeklyMemory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class MemoryStateOwnerTest {
    @Test
    fun `load stores requested completed week`() = runBlocking {
        val expected = memory(weeksAgo = 2)
        val repository = FakeMemoryRepository(
            results = mutableListOf(ApiResult.Success(expected)),
        )
        val owner = MemoryStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(utcOffsetMinutes = 180, weeksAgo = 2, showSpinner = true)

        assertEquals(expected, owner.state.memory)
        assertEquals(2, owner.state.weeksAgo)
        assertEquals(listOf(180 to 2), repository.requests)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `week index is bounded before transport`() = runBlocking {
        val repository = FakeMemoryRepository(
            results = mutableListOf(ApiResult.Success(memory(weeksAgo = 51))),
        )
        val owner = MemoryStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(utcOffsetMinutes = 0, weeksAgo = 80, showSpinner = true)

        assertEquals(listOf(0 to 51), repository.requests)
        assertEquals(51, owner.state.weeksAgo)
    }

    @Test
    fun `401 emits terminal session expiry without inline error`() = runBlocking {
        val repository = FakeMemoryRepository(
            results = mutableListOf(ApiResult.Failure("expired", 401)),
        )
        val owner = MemoryStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(utcOffsetMinutes = 120, weeksAgo = 0, showSpinner = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `network failure releases spinner and remains retryable`() = runBlocking {
        val repository = FakeMemoryRepository(
            results = mutableListOf(ApiResult.Failure("offline", 503)),
        )
        val owner = MemoryStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(utcOffsetMinutes = 0, weeksAgo = 0, showSpinner = true)

        assertEquals("offline", owner.state.error)
        assertFalse(owner.state.loading)
        assertNull(owner.state.memory)
    }
}


private class FakeMemoryRepository(
    private val results: MutableList<ApiResult<WeeklyMemory>>,
) : MemoryRepository {
    val requests = mutableListOf<Pair<Int, Int>>()

    override suspend fun week(
        utcOffsetMinutes: Int,
        weeksAgo: Int,
    ): ApiResult<WeeklyMemory> {
        requests += utcOffsetMinutes to weeksAgo
        return results.removeFirst()
    }
}


private fun memory(weeksAgo: Int) = WeeklyMemory(
    startsAt = "2026-08-10T00:00:00Z",
    endsAt = "2026-08-17T00:00:00Z",
    utcOffsetMinutes = 0,
    weeksAgo = weeksAgo,
    generatedAt = "2026-08-22T17:00:00Z",
    stats = MemoryStats(
        pulses = 1,
        posts = 1,
        roomItems = 1,
        rooms = 1,
        people = 1,
        nights = 2,
        highlights = 3,
    ),
    highlights = emptyList(),
    people = emptyList(),
    rooms = emptyList(),
)
