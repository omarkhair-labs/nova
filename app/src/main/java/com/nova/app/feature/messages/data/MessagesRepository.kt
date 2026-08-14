package com.nova.app.feature.messages.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.domain.model.NovaConversationList
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaMessagePage
import com.nova.app.feature.messages.domain.model.NovaMessageReaction
import java.io.File


/** Data boundary used by Messages state owners and realtime coordination. */
interface MessagesRepository {
    suspend fun conversations(query: String = ""): ApiResult<NovaConversationList>

    suspend fun openConversation(username: String): ApiResult<NovaConversation>

    suspend fun messages(
        conversationId: Long,
        cursor: String? = null,
    ): ApiResult<NovaMessagePage>

    suspend fun sendMessage(
        conversationId: Long,
        body: String,
        clientId: String,
        replyToId: Long? = null,
        imageUri: Uri? = null,
        audioFile: File? = null,
        audioDurationMs: Long? = null,
    ): ApiResult<NovaMessage>

    suspend fun editMessage(messageId: Long, body: String): ApiResult<NovaMessage>

    suspend fun deleteMessage(messageId: Long): ApiResult<String>

    suspend fun setReaction(
        messageId: Long,
        emoji: String?,
    ): ApiResult<List<NovaMessageReaction>>

    suspend fun markRead(conversationId: Long): ApiResult<Int>

    suspend fun realtimeAccessToken(): ApiResult<String>
}
