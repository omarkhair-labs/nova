package com.nova.app.feature.messages.details.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.details.model.ConversationMediaPage
import com.nova.app.feature.messages.details.model.ConversationMessageContext
import com.nova.app.feature.messages.details.model.ConversationToolMessage


interface ConversationToolsRepository {
    suspend fun searchMessages(
        conversationId: Long,
        query: String,
    ): ApiResult<List<ConversationToolMessage>>

    suspend fun messageContext(
        conversationId: Long,
        messageId: Long,
    ): ApiResult<ConversationMessageContext>

    suspend fun sharedMedia(
        conversationId: Long,
        type: String = "all",
        cursor: String? = null,
    ): ApiResult<ConversationMediaPage>

    suspend fun isMuted(conversationId: Long): ApiResult<Boolean>

    suspend fun setMuted(conversationId: Long, muted: Boolean): ApiResult<Boolean>
}
