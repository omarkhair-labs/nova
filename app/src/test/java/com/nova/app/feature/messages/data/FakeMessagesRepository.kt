package com.nova.app.feature.messages.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.domain.model.NovaConversationList
import com.nova.app.feature.messages.domain.model.NovaConversationPage
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaMessagePage
import com.nova.app.feature.messages.domain.model.NovaMessageReaction
import java.io.File


class FakeMessagesRepository : MessagesRepository {
    data class MessagesCall(val conversationId: Long, val cursor: String?)

    data class SendMessageCall(
        val conversationId: Long,
        val body: String,
        val clientId: String,
        val replyToId: Long?,
        val imageUri: Uri?,
        val audioFile: File?,
        val audioDurationMs: Long?,
    )

    data class EditMessageCall(val messageId: Long, val body: String)
    data class ReactionCall(val messageId: Long, val emoji: String?)

    var conversationsResult: ApiResult<NovaConversationList> = notConfigured()
    var openConversationResult: ApiResult<NovaConversation> = notConfigured()
    var messagesResult: ApiResult<NovaMessagePage> = notConfigured()
    var sendMessageResult: ApiResult<NovaMessage> = notConfigured()
    var editMessageResult: ApiResult<NovaMessage> = notConfigured()
    var deleteMessageResult: ApiResult<String> = notConfigured()
    var setReactionResult: ApiResult<List<NovaMessageReaction>> = notConfigured()
    var markReadResult: ApiResult<Int> = notConfigured()
    var realtimeAccessTokenResult: ApiResult<String> = notConfigured()

    val conversationQueries = mutableListOf<String>()
    val openedUsernames = mutableListOf<String>()
    val messagesCalls = mutableListOf<MessagesCall>()
    val sendMessageCalls = mutableListOf<SendMessageCall>()
    val editMessageCalls = mutableListOf<EditMessageCall>()
    val deletedMessageIds = mutableListOf<Long>()
    val reactionCalls = mutableListOf<ReactionCall>()
    val markedReadConversationIds = mutableListOf<Long>()
    var realtimeAccessTokenCalls: Int = 0

    override suspend fun conversations(query: String): ApiResult<NovaConversationList> {
        conversationQueries += query
        return conversationsResult
    }

    override suspend fun openConversation(username: String): ApiResult<NovaConversation> {
        openedUsernames += username
        return openConversationResult
    }

    override suspend fun messages(
        conversationId: Long,
        cursor: String?,
    ): ApiResult<NovaMessagePage> {
        messagesCalls += MessagesCall(conversationId, cursor)
        return messagesResult
    }

    override suspend fun sendMessage(
        conversationId: Long,
        body: String,
        clientId: String,
        replyToId: Long?,
        imageUri: Uri?,
        audioFile: File?,
        audioDurationMs: Long?,
    ): ApiResult<NovaMessage> {
        sendMessageCalls += SendMessageCall(
            conversationId = conversationId,
            body = body,
            clientId = clientId,
            replyToId = replyToId,
            imageUri = imageUri,
            audioFile = audioFile,
            audioDurationMs = audioDurationMs,
        )
        return sendMessageResult
    }

    override suspend fun editMessage(messageId: Long, body: String): ApiResult<NovaMessage> {
        editMessageCalls += EditMessageCall(messageId, body)
        return editMessageResult
    }

    override suspend fun deleteMessage(messageId: Long): ApiResult<String> {
        deletedMessageIds += messageId
        return deleteMessageResult
    }

    override suspend fun setReaction(
        messageId: Long,
        emoji: String?,
    ): ApiResult<List<NovaMessageReaction>> {
        reactionCalls += ReactionCall(messageId, emoji)
        return setReactionResult
    }

    override suspend fun markRead(conversationId: Long): ApiResult<Int> {
        markedReadConversationIds += conversationId
        return markReadResult
    }

    override suspend fun realtimeAccessToken(): ApiResult<String> {
        realtimeAccessTokenCalls += 1
        return realtimeAccessTokenResult
    }

    private companion object {
        fun notConfigured(): ApiResult.Failure = ApiResult.Failure("Fake result was not configured")
    }
}


class FakeInboxRepository : InboxRepository {
    data class ConversationsCall(val query: String, val cursor: String?)

    var conversationsResult: ApiResult<NovaConversationPage> =
        ApiResult.Failure("Fake result was not configured")
    val calls = mutableListOf<ConversationsCall>()

    override suspend fun conversations(
        query: String,
        cursor: String?,
    ): ApiResult<NovaConversationPage> {
        calls += ConversationsCall(query, cursor)
        return conversationsResult
    }
}
