package com.nova.app.app

import android.content.Context
import com.nova.app.NovaApplication
import com.nova.app.core.auth.NovaAuthRepository
import com.nova.app.core.feed.NovaFeedRepository
import com.nova.app.core.messaging.NovaMessagingApiClient
import com.nova.app.core.messaging.NovaInboxPagingRepository
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.social.NovaSocialRepository
import com.nova.app.feature.messages.data.InboxRepository
import com.nova.app.feature.messages.data.MessagesRepository
import com.nova.app.navigation.AppNavigationBridge


/** Lightweight application dependency owner; no framework or service locator globals. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val api = NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL)
    private val messagingApi = NovaMessagingApiClient(NovaMessagingRepository.PRODUCTION_API_URL)

    val authRepository = NovaAuthRepository(appContext, api)
    val feedRepository = NovaFeedRepository(appContext, api)
    val messagingRepository: MessagesRepository = NovaMessagingRepository(appContext, messagingApi, api)
    val inboxRepository: InboxRepository = NovaInboxPagingRepository(appContext)
    val socialRepository = NovaSocialRepository(appContext, api)
    val appNavigator = AppNavigationBridge()
}


val Context.appContainer: AppContainer
    get() = (applicationContext as NovaApplication).appContainer
