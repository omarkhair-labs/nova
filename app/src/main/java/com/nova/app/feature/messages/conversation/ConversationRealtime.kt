package com.nova.app.feature.messages.conversation

import com.nova.app.core.messaging.NovaConversationPresence
import com.nova.app.core.messaging.NovaMessageDeletedEvent
import com.nova.app.core.messaging.NovaMessageReactionEvent
import com.nova.app.core.messaging.NovaMessageUpdatedEvent
import com.nova.app.core.messaging.NovaRealtimeEvent
import com.nova.app.core.messaging.NovaRealtimeStatus
import kotlinx.coroutines.CoroutineScope


/** Realtime boundary consumed by the conversation state owner. */
interface ConversationRealtime {
    fun start(
        scope: CoroutineScope,
        onEvent: (NovaRealtimeEvent) -> Unit,
        onStatus: (NovaRealtimeStatus) -> Unit,
        onSessionExpired: () -> Unit,
        onPresence: (NovaConversationPresence) -> Unit = {},
        onReaction: (NovaMessageReactionEvent) -> Unit = {},
        onMessageUpdated: (NovaMessageUpdatedEvent) -> Unit = {},
        onMessageDeleted: (NovaMessageDeletedEvent) -> Unit = {},
    )

    fun stop()

    fun sendTyping(isTyping: Boolean)
}
