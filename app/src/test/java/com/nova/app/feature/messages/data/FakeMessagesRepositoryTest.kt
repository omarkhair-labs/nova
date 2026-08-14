package com.nova.app.feature.messages.data

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.messages.domain.model.NovaConversationList
import com.nova.app.feature.messages.domain.model.NovaMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test


class FakeMessagesRepositoryTest {
    @Test
    fun configuredResultAndInboxQueryAreDeterministic() = runBlocking {
        val expected = ApiResult.Success(NovaConversationList(emptyList(), unreadCount = 3))
        val fake = FakeMessagesRepository().apply {
            conversationsResult = expected
        }

        val actual = fake.conversations("nova team")

        assertSame(expected, actual)
        assertEquals(listOf("nova team"), fake.conversationQueries)
    }

    @Test
    fun sendCapturesEveryComposerArgument() = runBlocking {
        val expected = ApiResult.Success(message())
        val fake = FakeMessagesRepository().apply {
            sendMessageResult = expected
        }

        val actual = fake.sendMessage(
            conversationId = 91L,
            body = "Hello",
            clientId = "client-91",
            replyToId = 42L,
            audioDurationMs = 1_500L,
        )

        assertSame(expected, actual)
        assertEquals(
            FakeMessagesRepository.SendMessageCall(
                conversationId = 91L,
                body = "Hello",
                clientId = "client-91",
                replyToId = 42L,
                imageUri = null,
                audioFile = null,
                audioDurationMs = 1_500L,
            ),
            fake.sendMessageCalls.single(),
        )
    }

    @Test
    fun realtimeTokenCallsAreObservable() = runBlocking {
        val expected = ApiResult.Success("access-token")
        val fake = FakeMessagesRepository().apply {
            realtimeAccessTokenResult = expected
        }

        assertSame(expected, fake.realtimeAccessToken())
        assertEquals(1, fake.realtimeAccessTokenCalls)
    }

    private fun message() = NovaMessage(
        id = 91L,
        clientId = "client-91",
        sender = NovaPostAuthor(7L, "alice", "Alice", ""),
        body = "Hello",
        imageUrl = "",
        replyTo = null,
        reactions = emptyList(),
        createdAt = "2026-08-14T00:00:00Z",
        deliveredAt = null,
        readAt = null,
        isMine = true,
    )
}
