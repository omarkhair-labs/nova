package com.nova.app.feature.messages.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.domain.model.NovaConversationPage


/** Paged inbox/search boundary used by the inbox state owner. */
interface InboxRepository {
    suspend fun conversations(
        query: String = "",
        cursor: String? = null,
    ): ApiResult<NovaConversationPage>
}
