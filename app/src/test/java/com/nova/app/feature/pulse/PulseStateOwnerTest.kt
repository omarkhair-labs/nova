package com.nova.app.feature.pulse

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.pulse.data.PulseRepository
import com.nova.app.feature.pulse.domain.model.NovaPulse
import com.nova.app.feature.pulse.domain.model.NovaPulseAuthor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class PulseStateOwnerTest {
    @Test
    fun `load success replaces live pulses and stops spinner`() = runBlocking {
        val expected = listOf(pulse(1), pulse(2))
        val owner = PulseStateOwner(
            FakePulseRepository(loadResults = mutableListOf(ApiResult.Success(expected))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadNow(showSpinner = true)

        assertEquals(expected, owner.state.pulses)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `load 401 emits session expiry without inline error`() = runBlocking {
        val owner = PulseStateOwner(
            FakePulseRepository(loadResults = mutableListOf(ApiResult.Failure("expired", 401))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadNow(showSpinner = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `text create inserts new pulse immediately`() = runBlocking {
        val created = pulse(9, mine = true)
        val owner = PulseStateOwner(
            FakePulseRepository(textResults = mutableListOf(ApiResult.Success(created))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.createTextNow("right now", "followers")

        assertEquals(listOf(created), owner.state.pulses)
        assertEquals(1, owner.state.createdVersion)
        assertFalse(owner.state.uploading)
    }

    @Test
    fun `delete removes pulse locally without refetch`() = runBlocking {
        val repository = FakePulseRepository(
            loadResults = mutableListOf(ApiResult.Success(listOf(pulse(1), pulse(2, mine = true)))),
            deleteResults = mutableListOf(ApiResult.Success(Unit)),
        )
        val owner = PulseStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))
        owner.loadNow()

        owner.deleteNow(2)

        assertEquals(listOf(1L), owner.state.pulses.map { it.id })
        assertEquals(null, owner.state.deletingPulseId)
        assertEquals(1, repository.deleteCalls)
    }
}


private class FakePulseRepository(
    private val loadResults: MutableList<ApiResult<List<NovaPulse>>> = mutableListOf(),
    private val textResults: MutableList<ApiResult<NovaPulse>> = mutableListOf(),
    private val mediaResults: MutableList<ApiResult<NovaPulse>> = mutableListOf(),
    private val deleteResults: MutableList<ApiResult<Unit>> = mutableListOf(),
) : PulseRepository {
    var deleteCalls = 0
        private set

    override suspend fun pulses(): ApiResult<List<NovaPulse>> = loadResults.removeFirst()

    override suspend fun createTextPulse(note: String, audience: String): ApiResult<NovaPulse> =
        textResults.removeFirst()

    override suspend fun createMediaPulse(
        mediaUri: Uri,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = mediaResults.removeFirst()

    override suspend fun pulseChain(pulseId: Long): ApiResult<List<NovaPulse>> =
        ApiResult.Success(emptyList())

    override suspend fun replyTextPulse(
        pulseId: Long,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = ApiResult.Failure("unused")

    override suspend fun replyMediaPulse(
        pulseId: Long,
        mediaUri: Uri,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = ApiResult.Failure("unused")

    override suspend fun deletePulse(pulseId: Long): ApiResult<Unit> {
        deleteCalls += 1
        return deleteResults.removeFirst()
    }
}


private fun pulse(id: Long, mine: Boolean = false) = NovaPulse(
    id = id,
    author = NovaPulseAuthor(
        id = 7,
        username = "person$id",
        name = "Person $id",
        avatarUrl = "",
    ),
    mediaUrl = "",
    mediaType = "text",
    audience = "followers",
    note = "Pulse $id",
    createdAt = "created",
    expiresAt = "expires",
    isMine = mine,
)
