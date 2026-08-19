package com.nova.app.core.messaging

import com.nova.app.feature.messages.details.data.remote.ConversationToolsRemoteRepository
import com.nova.app.feature.messages.details.model.ConversationMediaPage
import com.nova.app.feature.messages.details.model.ConversationMessageContext
import com.nova.app.feature.messages.details.model.ConversationToolMessage


@Deprecated("Use ConversationToolsRepository through AppContainer.")
typealias NovaMessagingV9ToolsRepository = ConversationToolsRemoteRepository

@Deprecated("Use ConversationToolMessage.")
typealias NovaV9MessageItem = ConversationToolMessage

@Deprecated("Use ConversationMediaPage.")
typealias NovaV9MediaPage = ConversationMediaPage

@Deprecated("Use ConversationMessageContext.")
typealias NovaV9MessageContext = ConversationMessageContext
