package com.nova.app.feature.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.notifications.data.NotificationsRepository
import com.nova.app.feature.notifications.domain.model.NovaNotification
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.privacy.data.FollowRequestRepository
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class NotificationsUiState(
    val notifications: List<NovaNotification> = emptyList(),
    val followRequests: List<NovaFollowRequest> = emptyList(),
    val nextCursor: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val requestsLoading: Boolean = true,
    val requestBusyId: Long? = null,
    val openingPostId: Long? = null,
    val errorMessage: String? = null,
    val requestError: String? = null,
) {
    val isRefreshing: Boolean
        get() = (isLoading || requestsLoading) && (notifications.isNotEmpty() || followRequests.isNotEmpty())
}


sealed interface NotificationOpenTarget {
    data class Person(val username: String) : NotificationOpenTarget
    data class Post(val postId: Long) : NotificationOpenTarget
    data class Reel(val reelId: Long, val username: String) : NotificationOpenTarget
    data object None : NotificationOpenTarget
}


/** Owns Activity-page async state. Rendering and Reel/Person navigation remain UI-owned. */
class NotificationsStateOwner(
    private val notificationsRepository: NotificationsRepository,
    private val followRequestRepository: FollowRequestRepository,
    private val postRepository: PostRepository,
    private val scope: CoroutineScope,
    private val onUnreadCountChanged: (Int) -> Unit,
    private val onSessionExpired: () -> Unit,
    private val onPostOpened: (NovaPost) -> Unit,
) {
    var state by mutableStateOf(NotificationsUiState())
        private set

    fun start() {
        loadActivity(reset = true)
    }

    fun loadFollowRequests() {
        scope.launch { loadFollowRequestsNow() }
    }

    internal suspend fun loadFollowRequestsNow() {
        state = state.copy(
            requestsLoading = true,
            requestError = null,
        )
        when (val result = followRequestRepository.followRequests()) {
            is ApiResult.Success -> state = state.copy(followRequests = result.value)
            is ApiResult.Failure -> {
                if (result.statusCode == 401) {
                    onSessionExpired()
                } else {
                    state = state.copy(requestError = result.message)
                }
            }
        }
        state = state.copy(requestsLoading = false)
    }

    fun loadActivity(reset: Boolean) {
        if (state.isLoadingMore || (reset && state.isLoading && state.notifications.isNotEmpty())) return
        val cursor = if (reset) null else state.nextCursor ?: return

        if (reset) loadFollowRequests()

        scope.launch {
            loadActivityRequestNow(reset = reset, cursor = cursor)
        }
    }

    internal suspend fun loadActivityRequestNow(reset: Boolean, cursor: String?) {
        state = if (reset) {
            state.copy(isLoading = true, errorMessage = null)
        } else {
            state.copy(isLoadingMore = true, errorMessage = null)
        }

        when (val result = notificationsRepository.notifications(cursor)) {
            is ApiResult.Success -> {
                val page = result.value
                val nextNotifications = if (reset) {
                    page.notifications
                } else {
                    val existingIds = state.notifications.mapTo(mutableSetOf()) { it.id }
                    state.notifications + page.notifications.filterNot { it.id in existingIds }
                }
                state = state.copy(
                    notifications = nextNotifications,
                    nextCursor = page.nextCursor,
                    isLoading = false,
                    isLoadingMore = false,
                )
                onUnreadCountChanged(page.unreadCount)

                if (reset && page.unreadCount > 0) {
                    when (val readResult = notificationsRepository.markAllRead()) {
                        is ApiResult.Success -> onUnreadCountChanged(readResult.value)
                        is ApiResult.Failure -> {
                            if (readResult.statusCode == 401) onSessionExpired()
                        }
                    }
                }
            }

            is ApiResult.Failure -> {
                state = state.copy(
                    isLoading = false,
                    isLoadingMore = false,
                )
                if (result.statusCode == 401) {
                    onSessionExpired()
                } else {
                    state = state.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun decideFollowRequest(request: NovaFollowRequest, accept: Boolean) {
        scope.launch { decideFollowRequestNow(request, accept) }
    }

    internal suspend fun decideFollowRequestNow(request: NovaFollowRequest, accept: Boolean) {
        if (state.requestBusyId != null) return
        state = state.copy(
            requestBusyId = request.id,
            requestError = null,
        )
        val result = if (accept) {
            followRequestRepository.acceptFollowRequest(request.id)
        } else {
            followRequestRepository.declineFollowRequest(request.id)
        }
        when (result) {
            is ApiResult.Success -> state = state.copy(
                followRequests = state.followRequests.filterNot { it.id == request.id },
            )

            is ApiResult.Failure -> {
                if (result.statusCode == 401) {
                    onSessionExpired()
                } else {
                    state = state.copy(requestError = result.message)
                }
            }
        }
        state = state.copy(requestBusyId = null)
    }

    fun openPost(postId: Long) {
        scope.launch { openPostNow(postId) }
    }

    internal suspend fun openPostNow(postId: Long) {
        if (state.openingPostId != null) return
        state = state.copy(
            openingPostId = postId,
            errorMessage = null,
        )
        when (val result = postRepository.post(postId)) {
            is ApiResult.Success -> {
                state = state.copy(openingPostId = null)
                onPostOpened(result.value)
            }

            is ApiResult.Failure -> {
                state = state.copy(openingPostId = null)
                if (result.statusCode == 401) {
                    onSessionExpired()
                } else {
                    state = state.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun shouldLoadMore(lastVisible: Int, totalItems: Int): Boolean {
        return state.nextCursor != null &&
            !state.isLoading &&
            !state.isLoadingMore &&
            state.notifications.isNotEmpty() &&
            totalItems > 0 &&
            lastVisible >= totalItems - LOAD_MORE_THRESHOLD
    }

    fun openTarget(notification: NovaNotification): NotificationOpenTarget {
        return when (notification.kind) {
            "follow" -> NotificationOpenTarget.Person(notification.actor.username)
            "like", "comment", "comment_reply" -> {
                notification.postId?.let(NotificationOpenTarget::Post)
                    ?: NotificationOpenTarget.Person(notification.actor.username)
            }
            "reel_like", "reel_comment", "reel_repost", "reel_reply" -> {
                val reelId = notification.reelId
                if (reelId != null && notification.reelAuthorUsername.isNotBlank()) {
                    if (reelId <= 0L) {
                        NotificationOpenTarget.None
                    } else {
                        NotificationOpenTarget.Reel(
                            reelId = reelId,
                            username = notification.reelAuthorUsername.trim().lowercase(),
                        )
                    }
                } else {
                    NotificationOpenTarget.Person(notification.actor.username)
                }
            }
            else -> NotificationOpenTarget.Person(notification.actor.username)
        }
    }

    companion object {
        internal const val LOAD_MORE_THRESHOLD = 4
    }
}
