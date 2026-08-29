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
import org.junit.Assert.assertTrue
import org.junit.Test


class PulseViewerStateOwnerTest {
    @Test
    fun `chain load replaces initial pulse with visible ordered chain`() = runBlocking {
        val initial = pulse(2, replyToId = 1, chainRootId = 1)
        val expected = listOf(pulse(1), initial, pulse(3, replyToId = 2, chainRootId = 1))
        val owner = PulseViewerStateOwner(
            initial,
            FakePulseViewerRepository(chainResults = mutableListOf(ApiResult.Success(expected))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadChainNow(initial.id)

        assertEquals(expected, owner.state.chain)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `chain refresh preserves playback identity when signed urls rotate`() = runBlocking {
        val initial = pulse(
            id = 1,
            mediaUrl = "https://media.example/pulse.mp4?signature=old",
            thumbnailUrl = "https://media.example/pulse.jpg?signature=old",
        )
        val refreshed = initial.copy(
            mediaUrl = "https://media.example/pulse.mp4?signature=new",
            thumbnailUrl = "https://media.example/pulse.jpg?signature=new",
            viewersCount = 12,
        )
        val owner = PulseViewerStateOwner(
            initial,
            FakePulseViewerRepository(chainResults = mutableListOf(ApiResult.Success(listOf(refreshed)))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadChainNow(initial.id, showSpinner = false)

        assertEquals(initial.mediaUrl, owner.state.chain.single().mediaUrl)
        assertEquals(initial.thumbnailUrl, owner.state.chain.single().thumbnailUrl)
        assertEquals(12, owner.state.chain.single().viewersCount)
        assertFalse(owner.state.loading)
    }

    @Test
    fun `record view updates engagement without rotating playback url`() = runBlocking {
        val initial = pulse(
            id = 1,
            mediaUrl = "https://media.example/pulse.mp4?signature=old",
        )
        val viewed = initial.copy(
            mediaUrl = "https://media.example/pulse.mp4?signature=new",
            viewersCount = 9,
        )
        val owner = PulseViewerStateOwner(
            initial,
            FakePulseViewerRepository(viewResults = mutableListOf(ApiResult.Success(viewed))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.recordView(initial.id)

        assertEquals(initial.mediaUrl, owner.state.chain.single().mediaUrl)
        assertEquals(9, owner.state.chain.single().viewersCount)
        assertTrue(owner.state.error == null)
    }

    @Test
    fun `chain 401 expires session without surfacing inline error`() = runBlocking {
        val initial = pulse(1)
        val owner = PulseViewerStateOwner(
            initial,
            FakePulseViewerRepository(chainResults = mutableListOf(ApiResult.Failure("expired", 401))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadChainNow(initial.id)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `text reply appends live moment and emits completion`() = runBlocking {
        val initial = pulse(1)
        val reply = pulse(2, mine = true, replyToId = 1, chainRootId = 1)
        val owner = PulseViewerStateOwner(
            initial,
            FakePulseViewerRepository(textReplyResults = mutableListOf(ApiResult.Success(reply))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.replyTextNow(initial.id, "On my way", "followers")

        assertEquals(listOf(initial, reply), owner.state.chain)
        assertEquals(1, owner.state.replyCreatedVersion)
        assertFalse(owner.state.replying)
        assertNull(owner.state.error)
    }

    @Test
    fun `reply failure releases busy state and keeps current chain`() = runBlocking {
        val initial = pulse(1)
        val owner = PulseViewerStateOwner(
            initial,
            FakePulseViewerRepository(textReplyResults = mutableListOf(ApiResult.Failure("offline", 503))),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.replyTextNow(initial.id, "hello", "followers")

        assertEquals(listOf(initial), owner.state.chain)
        assertFalse(owner.state.replying)
        assertEquals("offline", owner.state.error)
    }
}


private class FakePulseViewerRepository(
    private val chainResults: MutableList<ApiResult<List<NovaPulse>>> = mutableListOf(),
    private val textReplyResults: MutableList<ApiResult<NovaPulse>> = mutableListOf(),
    private val mediaReplyResults: MutableList<ApiResult<NovaPulse>> = mutableListOf(),
    private val viewResults: MutableList<ApiResult<NovaPulse>> = mutableListOf(),
) : PulseRepository {
    override suspend fun pulses(): ApiResult<List<NovaPulse>> = ApiResult.Success(emptyList())

    override suspend fun createTextPulse(note: String, audience: String): ApiResult<NovaPulse> =
        ApiResult.Failure("unused")

    override suspend fun createMediaPulse(
        mediaUri: Uri,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = ApiResult.Failure("unused")

    override suspend fun pulseChain(pulseId: Long): ApiResult<List<NovaPulse>> =
        chainResults.removeFirst()

    override suspend fun replyTextPulse(
        pulseId: Long,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = textReplyResults.removeFirst()

    override suspend fun replyMediaPulse(
        pulseId: Long,
        mediaUri: Uri,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = mediaReplyResults.removeFirst()

    override suspend fun deletePulse(pulseId: Long): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun recordView(pulseId: Long): ApiResult<NovaPulse> =
        viewResults.removeFirst()
}


private fun pulse(
    id: Long,
    mine: Boolean = false,
    replyToId: Long? = null,
    chainRootId: Long? = null,
    mediaUrl: String = "",
    thumbnailUrl: String = "",
) = NovaPulse(
    id = id,
    author = NovaPulseAuthor(
        id = id + 100,
        username = "person$id",
        name = "Person $id",
        avatarUrl = "",
    ),
    mediaUrl = mediaUrl,
    thumbnailUrl = thumbnailUrl,
    mediaType = if (mediaUrl.isBlank()) "text" else "video",
    audience = "followers",
    note = "Pulse $id",
    createdAt = "2026-08-22T12:00:0${id}Z",
    expiresAt = "2026-08-23T00:00:0${id}Z",
    isMine = mine,
    replyToId = replyToId,
    chainRootId = chainRootId,
)
