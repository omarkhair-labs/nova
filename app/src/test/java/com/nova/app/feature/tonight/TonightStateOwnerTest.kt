package com.nova.app.feature.tonight

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.tonight.data.TonightRepository
import com.nova.app.feature.tonight.domain.model.TonightSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class TonightStateOwnerTest {
    @Test
    fun `load stores active Tonight snapshot with requested offset`() = runBlocking {
        val expected = snapshot(isTonight = true, peopleCount = 3, momentsCount = 5)
        val repository = FakeTonightRepository(
            results = mutableListOf(ApiResult.Success(expected)),
        )
        val owner = TonightStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(utcOffsetMinutes = 180, showSpinner = true)

        assertEquals(expected, owner.state.snapshot)
        assertEquals(listOf(180), repository.offsets)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `daytime snapshot remains a valid non error state`() = runBlocking {
        val expected = snapshot(isTonight = false)
        val owner = TonightStateOwner(
            FakeTonightRepository(
                results = mutableListOf(ApiResult.Success(expected)),
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadNow(utcOffsetMinutes = -300, showSpinner = true)

        assertEquals(false, owner.state.snapshot?.isTonight)
        assertEquals(-300, owner.state.snapshot?.utcOffsetMinutes)
        assertNull(owner.state.error)
        assertFalse(owner.state.loading)
    }

    @Test
    fun `401 expires session without leaking inline error`() = runBlocking {
        val owner = TonightStateOwner(
            FakeTonightRepository(
                results = mutableListOf(ApiResult.Failure("expired", 401)),
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadNow(utcOffsetMinutes = 120, showSpinner = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `network failure releases spinner and keeps message retryable`() = runBlocking {
        val owner = TonightStateOwner(
            FakeTonightRepository(
                results = mutableListOf(ApiResult.Failure("offline", 503)),
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadNow(utcOffsetMinutes = 0, showSpinner = true)

        assertEquals("offline", owner.state.error)
        assertFalse(owner.state.loading)
        assertNull(owner.state.snapshot)
    }
}


private class FakeTonightRepository(
    private val results: MutableList<ApiResult<TonightSnapshot>>,
) : TonightRepository {
    val offsets = mutableListOf<Int>()

    override suspend fun tonight(utcOffsetMinutes: Int): ApiResult<TonightSnapshot> {
        offsets += utcOffsetMinutes
        return results.removeFirst()
    }
}


private fun snapshot(
    isTonight: Boolean,
    peopleCount: Int = 0,
    momentsCount: Int = 0,
) = TonightSnapshot(
    isTonight = isTonight,
    localHour = if (isTonight) 22 else 12,
    utcOffsetMinutes = 180,
    startsAt = "2026-08-22T15:00:00Z",
    endsAt = "2026-08-23T03:00:00Z",
    peopleCount = peopleCount,
    momentsCount = momentsCount,
    myMomentsCount = 0,
    people = emptyList(),
)
