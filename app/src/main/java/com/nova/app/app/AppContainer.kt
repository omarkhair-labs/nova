package com.nova.app.app

import android.content.Context
import com.nova.app.NovaApplication
import com.nova.app.core.auth.NovaAuthRepository
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.calls.NovaCallRepository
import com.nova.app.core.calls.NovaCallSignalingClient
import com.nova.app.core.calls.NovaCallWebRtcAdapter
import com.nova.app.core.feed.NovaFeedRepository
import com.nova.app.core.messaging.NovaConversationDraftStore
import com.nova.app.core.messaging.NovaConversationRealtimeClient
import com.nova.app.core.messaging.NovaInboxPagingRepository
import com.nova.app.core.messaging.NovaMessagingApiClient
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.core.reels.NovaProfileReelsRepository
import com.nova.app.core.reels.NovaReelWatchRepository
import com.nova.app.core.reels.NovaReelsRepository
import com.nova.app.core.sharing.NovaSharingRepository
import com.nova.app.core.social.NovaSocialPagingRepository
import com.nova.app.core.social.NovaSocialRepository
import com.nova.app.core.stories.NovaStoriesRepository
import com.nova.app.feature.calls.data.CallRepository
import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaIceConfig
import com.nova.app.feature.calls.signaling.CallSignaling
import com.nova.app.feature.calls.webrtc.CallWebRtcEngine
import com.nova.app.feature.calls.webrtc.CallWebRtcListener
import com.nova.app.feature.feed.data.FeedRepository
import com.nova.app.feature.messages.appearance.data.ConversationAppearanceRepository
import com.nova.app.feature.messages.appearance.data.remote.ConversationAppearanceRemoteRepository
import com.nova.app.feature.messages.conversation.ConversationDraftStore
import com.nova.app.feature.messages.conversation.ConversationRealtime
import com.nova.app.feature.messages.data.InboxRepository
import com.nova.app.feature.messages.data.MessagesRepository
import com.nova.app.feature.messages.details.data.ConversationToolsRepository
import com.nova.app.feature.messages.details.data.remote.ConversationToolsRemoteRepository
import com.nova.app.feature.messages.group.data.GroupManagementRepository
import com.nova.app.feature.messages.group.data.GroupMembershipRepository
import com.nova.app.feature.messages.group.data.GroupPeopleRepository
import com.nova.app.feature.messages.group.data.remote.GroupManagementRemoteRepository
import com.nova.app.feature.messages.group.data.remote.GroupMembershipRemoteRepository
import com.nova.app.feature.messages.group.data.remote.GroupPeoplePagingRepository
import com.nova.app.feature.notifications.data.NotificationsRepository
import com.nova.app.feature.notifications.data.remote.CoreNotificationsRepositoryAdapter
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.reels.data.ProfileReelsRepository
import com.nova.app.feature.reels.data.ReelWatchRepository
import com.nova.app.feature.reels.data.ReelsRepository
import com.nova.app.feature.sharing.data.SharingRepository
import com.nova.app.feature.stories.data.StoriesRepository
import com.nova.app.navigation.AppNavigationBridge


/** Lightweight application dependency owner; no framework or service locator globals. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val api = NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL)
    private val messagingApi = NovaMessagingApiClient(NovaMessagingRepository.PRODUCTION_API_URL)
    private val sessionStore = NovaSessionStore(appContext)

    val authRepository = NovaAuthRepository(appContext, api)
    val feedRepository = NovaFeedRepository(appContext, api)
    val feedDataRepository: FeedRepository = feedRepository
    val postDataRepository: PostRepository = feedRepository
    val messagingRepository: MessagesRepository = NovaMessagingRepository(appContext, messagingApi, api)
    val inboxRepository: InboxRepository = NovaInboxPagingRepository(appContext)
    val conversationToolsRepository: ConversationToolsRepository = ConversationToolsRemoteRepository(appContext)
    val conversationAppearanceRepository: ConversationAppearanceRepository = ConversationAppearanceRemoteRepository(appContext)
    val groupManagementRepository: GroupManagementRepository = GroupManagementRemoteRepository(appContext)
    val groupMembershipRepository: GroupMembershipRepository = GroupMembershipRemoteRepository(appContext)
    val groupPeopleRepository: GroupPeopleRepository = GroupPeoplePagingRepository(appContext)
    val callRepository: CallRepository = NovaCallRepository(appContext)
    val socialRepository = NovaSocialRepository(appContext, api)
    val peopleRepository: PeopleRepository = socialRepository
    val peoplePagingRepository: PeoplePagingRepository = NovaSocialPagingRepository(appContext)
    val storiesRepository: StoriesRepository = NovaStoriesRepository(appContext)
    val reelsRepository: ReelsRepository = NovaReelsRepository(appContext)
    val profileReelsRepository: ProfileReelsRepository = NovaProfileReelsRepository(appContext)
    val reelWatchRepository: ReelWatchRepository = NovaReelWatchRepository(appContext)
    val sharingRepository: SharingRepository = NovaSharingRepository(appContext)
    val notificationsRepository: NotificationsRepository = CoreNotificationsRepositoryAdapter(appContext)
    val appNavigator = AppNavigationBridge()

    fun callSignaling(callId: String): CallSignaling =
        NovaCallSignalingClient(appContext, callId, callRepository)

    fun callWebRtcEngine(
        kind: NovaCallKind,
        iceConfig: NovaIceConfig,
        listener: CallWebRtcListener,
    ): CallWebRtcEngine = NovaCallWebRtcAdapter(appContext, kind, iceConfig, listener)

    fun conversationRealtime(conversationId: Long): ConversationRealtime =
        NovaConversationRealtimeClient(appContext, conversationId, messagingRepository)

    fun conversationDraftStore(): ConversationDraftStore = NovaConversationDraftStore(appContext)

    fun currentCachedUserId(): Long? = sessionStore.load()?.cachedUser?.id

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
