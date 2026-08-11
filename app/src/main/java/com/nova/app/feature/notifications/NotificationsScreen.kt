package com.nova.app.feature.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.feed.NovaFeedRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPost
import com.nova.app.core.notifications.NovaNotification
import com.nova.app.core.notifications.NovaNotificationRepository
import com.nova.app.core.privacy.NovaFollowRequest
import com.nova.app.core.privacy.NovaPrivacyRepository
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant


@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onPersonClick: (String) -> Unit,
    onPostClick: (NovaPost) -> Unit,
    onUnreadCountChanged: (Int) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaNotificationRepository(context.applicationContext)
    }
    val privacyRepository = remember(context) {
        NovaPrivacyRepository(context.applicationContext)
    }
    val feedRepository = remember(context) {
        NovaFeedRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var notifications by remember { mutableStateOf<List<NovaNotification>>(emptyList()) }
    var followRequests by remember { mutableStateOf<List<NovaFollowRequest>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var requestsLoading by remember { mutableStateOf(true) }
    var requestBusyId by remember { mutableStateOf<Long?>(null) }
    var openingPostId by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var requestError by remember { mutableStateOf<String?>(null) }

    fun loadFollowRequests() {
        scope.launch {
            requestsLoading = true
            requestError = null
            when (val result = privacyRepository.followRequests()) {
                is ApiResult.Success -> followRequests = result.value
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else requestError = result.message
                }
            }
            requestsLoading = false
        }
    }

    fun loadActivity(reset: Boolean) {
        if (isLoadingMore || (reset && isLoading && notifications.isNotEmpty())) return
        val cursor = if (reset) null else nextCursor ?: return

        if (reset) loadFollowRequests()

        scope.launch {
            if (reset) isLoading = true else isLoadingMore = true
            errorMessage = null

            when (val result = repository.notifications(cursor)) {
                is ApiResult.Success -> {
                    notifications = if (reset) {
                        result.value.notifications
                    } else {
                        val existingIds = notifications.mapTo(mutableSetOf()) { it.id }
                        notifications + result.value.notifications.filterNot { it.id in existingIds }
                    }
                    nextCursor = result.value.nextCursor
                    onUnreadCountChanged(result.value.unreadCount)
                    isLoading = false
                    isLoadingMore = false

                    if (reset && result.value.unreadCount > 0) {
                        when (val readResult = repository.markAllRead()) {
                            is ApiResult.Success -> onUnreadCountChanged(readResult.value)
                            is ApiResult.Failure -> {
                                if (readResult.statusCode == 401) onSessionExpired()
                            }
                        }
                    }
                }

                is ApiResult.Failure -> {
                    isLoading = false
                    isLoadingMore = false
                    if (result.statusCode == 401) {
                        onSessionExpired()
                    } else {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    fun decideFollowRequest(request: NovaFollowRequest, accept: Boolean) {
        if (requestBusyId != null) return
        scope.launch {
            requestBusyId = request.id
            requestError = null
            val result = if (accept) {
                privacyRepository.acceptFollowRequest(request.id)
            } else {
                privacyRepository.declineFollowRequest(request.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    followRequests = followRequests.filterNot { it.id == request.id }
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else requestError = result.message
                }
            }
            requestBusyId = null
        }
    }

    fun openPost(postId: Long) {
        if (openingPostId != null) return
        scope.launch {
            openingPostId = postId
            errorMessage = null
            when (val result = feedRepository.post(postId)) {
                is ApiResult.Success -> {
                    openingPostId = null
                    onPostClick(result.value)
                }

                is ApiResult.Failure -> {
                    openingPostId = null
                    if (result.statusCode == 401) {
                        onSessionExpired()
                    } else {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    fun openReel(notification: NovaNotification) {
        val reelId = notification.reelId ?: return
        val username = notification.reelAuthorUsername.trim().lowercase()
        if (reelId <= 0L || username.isBlank()) return
        NovaReelsNavigator.openProfile(
            context = context,
            username = username,
            initialReelId = reelId,
        )
    }

    LaunchedEffect(Unit) {
        loadActivity(reset = true)
    }

    LaunchedEffect(listState, nextCursor, isLoading, isLoadingMore, notifications.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalItems) ->
                if (
                    nextCursor != null &&
                    !isLoading &&
                    !isLoadingMore &&
                    notifications.isNotEmpty() &&
                    totalItems > 0 &&
                    lastVisible >= totalItems - 4
                ) {
                    loadActivity(reset = false)
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = (isLoading || requestsLoading) && (notifications.isNotEmpty() || followRequests.isNotEmpty()),
        onRefresh = { loadActivity(reset = true) },
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 30.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(18.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Text(
                            text = "Back",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            color = NovaInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activity",
                            color = NovaInk,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Follow requests, likes, comments, replies, reposts and new people around you.",
                            color = NovaMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            if (followRequests.isNotEmpty()) {
                item(key = "follow-requests-title") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Follow requests",
                            color = NovaInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = followRequests.size.toString(),
                            color = NovaAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                items(
                    items = followRequests,
                    key = { "follow-request-${it.id}" },
                ) { request ->
                    FollowRequestRow(
                        request = request,
                        busy = requestBusyId == request.id,
                        enabled = requestBusyId == null,
                        onPersonClick = { onPersonClick(request.requester.username) },
                        onAccept = { decideFollowRequest(request, accept = true) },
                        onDecline = { decideFollowRequest(request, accept = false) },
                    )
                }
                if (!requestError.isNullOrBlank()) {
                    item(key = "follow-request-error") {
                        Text(requestError.orEmpty(), color = NovaMuted, fontSize = 11.sp)
                    }
                }
            } else if (requestsLoading) {
                item(key = "follow-requests-loading") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.width(15.dp).height(15.dp), color = NovaAccent, strokeWidth = 2.dp)
                        Text("Checking follow requests…", color = NovaMuted, fontSize = 11.sp)
                    }
                }
            } else if (!requestError.isNullOrBlank()) {
                item(key = "follow-requests-error") {
                    Surface(
                        onClick = ::loadFollowRequests,
                        shape = RoundedCornerShape(15.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Text(
                            text = "${requestError.orEmpty()} · Try again",
                            modifier = Modifier.padding(12.dp),
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            if (isLoading && notifications.isEmpty() && followRequests.isEmpty() && requestsLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading activity…",
                            color = NovaMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else if (errorMessage != null && notifications.isEmpty() && followRequests.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Couldn't load Activity",
                                color = NovaInk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                color = NovaMuted,
                                fontSize = 13.sp,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            NovaSecondaryButton(
                                text = "Try again",
                                onClick = { loadActivity(reset = true) },
                            )
                        }
                    }
                }
            } else if (notifications.isEmpty() && followRequests.isEmpty() && !requestsLoading) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 38.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = NovaAccentSoft,
                            ) {
                                Text(
                                    text = "✦",
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                    color = NovaAccent,
                                    fontSize = 25.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Quiet for now.",
                                color = NovaInk,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = "Follow requests, follows, likes, comments, replies and Reel reposts will show up here.",
                                color = NovaMuted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                }
            } else if (notifications.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent",
                        color = NovaInk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(
                    items = notifications,
                    key = { it.id },
                ) { notification ->
                    NotificationRow(
                        notification = notification,
                        isOpening = openingPostId == notification.postId && openingPostId != null,
                        onClick = {
                            when (notification.kind) {
                                "follow" -> onPersonClick(notification.actor.username)
                                "like", "comment", "comment_reply" -> {
                                    notification.postId?.let(::openPost)
                                        ?: onPersonClick(notification.actor.username)
                                }
                                "reel_like", "reel_comment", "reel_repost", "reel_reply" -> {
                                    if (notification.reelId != null && notification.reelAuthorUsername.isNotBlank()) {
                                        openReel(notification)
                                    } else {
                                        onPersonClick(notification.actor.username)
                                    }
                                }
                                else -> onPersonClick(notification.actor.username)
                            }
                        },
                    )
                }

                if (isLoadingMore) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(78.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = NovaMuted,
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                NovaSecondaryButton(
                                    text = "Try again",
                                    onClick = {
                                        if (nextCursor != null) {
                                            loadActivity(reset = false)
                                        } else {
                                            loadActivity(reset = true)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun FollowRequestRow(
    request: NovaFollowRequest,
    busy: Boolean,
    enabled: Boolean,
    onPersonClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onPersonClick, shape = RoundedCornerShape(18.dp), color = NovaSurface) {
                    NovaAvatar(
                        source = request.requester.avatarUrl,
                        fallbackText = request.requester.name.ifBlank { request.requester.username },
                        size = 46.dp,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.requester.name.ifBlank { request.requester.username },
                        color = NovaInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("@${request.requester.username} wants to follow you", color = NovaMuted, fontSize = 11.sp)
                    Text(relativeTime(request.createdAt), color = NovaMuted, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = { if (enabled) onDecline() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NovaBackground,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        text = "Decline",
                        modifier = Modifier.padding(vertical = 9.dp),
                        color = NovaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Surface(
                    onClick = { if (enabled) onAccept() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NovaAccent,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.width(13.dp).height(13.dp), color = NovaBackground, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (busy) "Saving…" else "Accept",
                            color = NovaBackground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NotificationRow(
    notification: NovaNotification,
    isOpening: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (notification.isRead) NovaSurface else NovaAccentSoft,
        border = BorderStroke(1.dp, if (notification.isRead) NovaBorder else NovaAccent),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaAvatar(
                source = notification.actor.avatarUrl,
                fallbackText = notification.actor.name.ifBlank { notification.actor.username },
                size = 46.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notificationText(notification),
                    color = NovaInk,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = relativeTime(notification.createdAt),
                    color = NovaMuted,
                    fontSize = 11.sp,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isOpening) {
                CircularProgressIndicator(
                    modifier = Modifier.width(18.dp).height(18.dp),
                    color = NovaAccent,
                    strokeWidth = 2.dp,
                )
            } else if (!notification.isRead) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NovaAccent,
                ) {
                    Spacer(
                        modifier = Modifier
                            .width(8.dp)
                            .height(8.dp),
                    )
                }
            }
        }
    }
}


private fun notificationText(notification: NovaNotification): String {
    val name = notification.actor.name.ifBlank { "@${notification.actor.username}" }
    return when (notification.kind) {
        "follow" -> "$name started following you"
        "like" -> "$name liked your post"
        "comment" -> {
            val preview = notification.commentPreview.trim()
            if (preview.isBlank()) {
                "$name commented on your post"
            } else {
                "$name commented: “${preview.take(90)}${if (preview.length > 90) "…" else ""}”"
            }
        }
        "comment_reply" -> "$name replied to your comment"
        "reel_like" -> "$name liked your Reel"
        "reel_comment" -> "$name commented on your Reel"
        "reel_repost" -> "$name reposted your Reel"
        "reel_reply" -> "$name replied to your Reel comment"
        else -> "$name interacted with you"
    }
}


private fun relativeTime(raw: String): String {
    return runCatching {
        val then = Instant.parse(raw)
        val seconds = Duration.between(then, Instant.now()).seconds.coerceAtLeast(0)
        when {
            seconds < 60 -> "Just now"
            seconds < 3_600 -> "${seconds / 60}m"
            seconds < 86_400 -> "${seconds / 3_600}h"
            seconds < 604_800 -> "${seconds / 86_400}d"
            else -> "${seconds / 604_800}w"
        }
    }.getOrDefault("Recently")
}
