package com.nova.app.feature.messages.details

import com.nova.app.feature.messages.details.model.ConversationMessageContext
import com.nova.app.feature.messages.details.model.ConversationToolMessage


enum class ConversationDetailsTab { Details, Search, Media }


data class ConversationDetailsUiState(
    val tab: ConversationDetailsTab = ConversationDetailsTab.Details,
    val query: String = "",
    val searchResults: List<ConversationToolMessage> = emptyList(),
    val searchLoading: Boolean = false,
    val searchError: String? = null,
    val mediaType: String = "all",
    val mediaItems: List<ConversationToolMessage> = emptyList(),
    val mediaCursor: String? = null,
    val mediaLoading: Boolean = false,
    val mediaError: String? = null,
    val muted: Boolean = false,
    val muteLoading: Boolean = true,
    val muteSaving: Boolean = false,
    val muteError: String? = null,
    val contextTargetId: Long? = null,
    val messageContext: ConversationMessageContext? = null,
    val contextLoading: Boolean = false,
    val contextError: String? = null,
    val sessionExpiryVersion: Int = 0,
)
