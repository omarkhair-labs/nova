package com.nova.app.app

import android.content.Context
import com.nova.app.NovaApplication
import com.nova.app.core.auth.NovaAuthRepository
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.feed.NovaFeedRepository
import com.nova.app.core.messaging.NovaConversationDraftStore
import com.nova.app.core.messaging.NovaConversationRealtimeClient
import com.nova.app.core.messaging.NovaInboxPagingRepository
import com.nova.app.core.messaging.NovaMessagingApiClient
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.core.social.NovaSocialRepository
import com.nova.app.feature.messages.conversation.ConversationDraftStore
import com.nova.app.feature.messages.conversation.ConversationRealtime
import com.nova.app.feature.messages.data.InboxRepository
import com.nova.app.feature.messages.data.MessagesRepository
import com.nova.app.navigation.AppNavigationBridge


/** Lightweight application dependency owner; no framework or service locator globals. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val api = NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL)
    private val messagingApi = NovaMessagingApiClient(NovaMessagingRepository.PRODUCTION_API_URL)
    private val sessionStore = NovaSessionStore(appContext)

    val authRepository = NovaAuthRepository(appContext, api)
    val feedRepository = NovaFeedRepository(appContext, api)
    val messagingRepository: MessagesRepository = NovaMessagingRepository(appContext, messagingApi, api)
    val inboxRepository: InboxRepository = NovaInboxPagingRepository(appContext)
    val socialRepository = NovaSocialRepository(appContext, api)
    val appNavigator = AppNavigationBridge()

    fun conversationRealtime(conversationId: Long): ConversationRealtime =
        NovaConversationRealtimeClient(appContext, conversationId, messagingRepository)

    fun conversationDraftStore(): ConversationDraftStore = NovaConversationDraftStore(appContext)

    fun currentMessageAuthor(): NovaPostAuthor = sessionStore.load()?.cachedUser?.let { user ->
        NovaPostAuthor(
            id = user.id,
            username = user.username,
            name = user.name,
            avatarUrl = user.avatarUrl,
        )
    } ?: NovaPostAuthor(
        id = 0L,
        username = "me",
        name = "",
        avatarUrl = "",
    )
}


val Context.appContainer: AppContainer
    get() = (applicationContext as NovaApplication).appContainer
