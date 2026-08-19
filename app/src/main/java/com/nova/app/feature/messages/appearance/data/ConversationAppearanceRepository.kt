package com.nova.app.feature.messages.appearance.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.appearance.model.ConversationPreference


interface ConversationAppearanceRepository {
    suspend fun preference(conversationId: Long): ApiResult<ConversationPreference>

    suspend fun setTheme(
        conversationId: Long,
        themeKey: String,
    ): ApiResult<ConversationPreference>
}
