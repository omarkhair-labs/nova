package com.nova.app.feature.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.feature.notifications.domain.model.NovaNotification
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaErrorState
import com.nova.app.ui.components.NovaInlineLoading
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.components.NovaUnreadDot
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val appContainer = context.appContainer
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val unreadCallback = rememberUpdatedState(onUnreadCountChanged)
    val sessionExpiredCallback = rememberUpdatedState(onSessionExpired)
    val postClickCallback = rememberUpdatedState(onPostClick)
    val owner = remember(appContainer, scope) {
        NotificationsStateOwner(
            notificationsRepository = appContainer.notificationsRepository,
            followRequestRepository = appContainer.followRequestRepository,
            postRepository = appContainer.postDataRepository,
            scope = scope,
            onUnreadCountChanged = { unreadCallback.value(it) },
            onSessionExpired = { sessionExpiredCallback.value() },
            onPostOpened = { postClickCallback.value(it) },
        )
    }
    val state = owner.state

    LaunchedEffect(owner) {
        owner.start()
    }

    LaunchedEffect(
        listState,
        state.nextCursor,
        state.isLoading,
        state.isLoadingMore,
        state.notifications.size,
    ) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalItems) ->
                if (owner.shouldLoadMore(lastVisible, totalItems)) {
                    owner.loadActivity(reset = false)
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { owner.loadActivity(reset = true) },
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
                start = NovaSpacing.xl,
                end = NovaSpacing.xl,
                top = NovaSpacing.lg,
                bottom = NovaSpacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NovaBackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(NovaSpacing.md))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = NovaSpacing.lg),
                    ) {
                        Text(
                            text = "Activity",
                            color = NovaInk,
                            style = NovaType.screenTitle,
                        )
                        Text(
                            text = "Follow requests and moments from people you connect with.",
                            color = NovaMuted,
                            style = NovaType.meta,
                        )
                    }
                }
            }

            if (state.followRequests.isNotEmpty()) {
                item(key = "follow-requests-title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = NovaSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Follow requests",
                            color = NovaInk,
                            style = NovaType.title,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = state.followRequests.size.toString(),
                            color = NovaAccent,
                            style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                items(
                    items = state.followRequests,
                    key = { "follow-request-${it.id}" },
                ) { request ->
                    FollowRequestRow(
                        request = request,
                        busy = state.requestBusyId == request.id,
                        enabled = state.requestBusyId == null,
                        onPersonClick = { onPersonClick(request.requester.username) },
                        onAccept = { owner.decideFollowRequest(request, accept = true) },
                        onDecline = { owner.decideFollowRequest(request, accept = false) },
                    )
                }
                if (!state.requestError.isNullOrBlank()) {
                    item(key = "follow-request-error") {
                        NovaInlineRetry(
                            message = state.requestError.orEmpty(),
                            onRetry = owner::loadFollowRequests,
                        )
                    }
                }
            } else if (state.requestsLoading) {
                item(key = "follow-requests-loading") {
                    NovaInlineLoading(message = "Checking follow requests…")
                }
            } else if (!state.requestError.isNullOrBlank()) {
                item(key = "follow-requests-error") {
                    NovaInlineRetry(
                        message = state.requestError.orEmpty(),
                        onRetry = owner::loadFollowRequests,
                    )
                }
            }

            if (state.isLoading && state.notifications.isEmpty()) {
                item {
                    if (state.followRequests.isEmpty()) {
                        NovaLoadingState(
                            message = "Loading activity…",
                            modifier = Modifier.height(220.dp),
                        )
                    } else {
                        NovaInlineLoading(message = "Loading recent activity…")
                    }
                }
            } else if (state.errorMessage != null && state.notifications.isEmpty()) {
                item {
                    if (state.followRequests.isEmpty()) {
                        NovaErrorState(
                            title = "Couldn't load Activity",
                            message = state.errorMessage.orEmpty(),
                            onRetry = { owner.loadActivity(reset = true) },
                        )
                    } else {
                        NovaInlineRetry(
                            message = state.errorMessage.orEmpty(),
                            onRetry = { owner.loadActivity(reset = true) },
                        )
                    }
                }
            } else if (state.notifications.isEmpty() && state.followRequests.isEmpty() && !state.requestsLoading) {
                item {
                    NovaEmptyState(
                        title = "Quiet for now.",
                        message = "Follow requests, follows, likes, comments, replies and Reel reposts will show up here.",
                    )
                }
            } else if (state.notifications.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent",
                        color = NovaInk,
                        style = NovaType.title,
                        modifier = Modifier.padding(top = NovaSpacing.lg, bottom = NovaSpacing.sm),
                    )
                }

                items(
                    items = state.notifications,
                    key = { it.id },
                ) { notification ->
                    NotificationRow(
                        notification = notification,
                        isOpening = state.openingNotificationId == notification.id,
                        onClick = {
                            when (val target = owner.openTarget(notification)) {
                                is NotificationOpenTarget.Person -> onPersonClick(target.username)
                                is NotificationOpenTarget.Post -> owner.openPost(notification.id, target.postId)
                                is NotificationOpenTarget.Reel -> NovaReelsNavigator.openProfile(
                                    context = context,
                                    username = target.username,
                                    initialReelId = target.reelId,
                                )
                            }
                        },
                    )
                }

                if (state.isLoadingMore) {
                    item {
                        NovaInlineLoading(message = "Loading more activity…")
                    }
                }

                if (state.errorMessage != null) {
                    item {
                        NovaInlineRetry(
                            message = state.errorMessage.orEmpty(),
                            onRetry = {
                                if (state.nextCursor != null) {
                                    owner.loadActivity(reset = false)
                                } else {
                                    owner.loadActivity(reset = true)
                                }
                            },
                        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = NovaSpacing.sm),
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
                    enabled = enabled,
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
                    enabled = enabled,
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
                            CircularProgressIndicator(
                                modifier = Modifier.width(13.dp).height(13.dp),
                                color = NovaBackground,
                                strokeWidth = 2.dp,
                            )
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
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (notification.isRead) "Read" else "Unread"
            },
        shape = RoundedCornerShape(0.dp),
        color = if (notification.isRead) Color.Transparent else NovaAccentSoft.copy(alpha = 0.58f),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 68.dp)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaAvatar(
                    source = notification.actor.avatarUrl,
                    fallbackText = notification.actor.name.ifBlank { notification.actor.username },
                    size = 42.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notificationText(notification),
                        color = NovaInk,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = relativeTime(notification.createdAt),
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (isOpening) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    NovaIcon(
                        asset = notificationIcon(notification.kind),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (notification.isRead) NovaMuted else NovaAccent,
                    )
                }
                if (!notification.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    NovaUnreadDot()
                }
            }
            HorizontalDivider(color = NovaBorder.copy(alpha = 0.7f))
        }
    }
}


private fun notificationText(notification: NovaNotification) = buildAnnotatedString {
    val name = notification.actor.name.ifBlank { "@${notification.actor.username}" }
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
        append(name)
    }
    append(" ")
    append(when (notification.kind) {
        "follow" -> "started following you"
        "like" -> "liked your post"
        "comment" -> {
            val preview = notification.commentPreview.trim()
            if (preview.isBlank()) {
                "commented on your post"
            } else {
                "commented: “${preview.take(90)}${if (preview.length > 90) "…" else ""}”"
            }
        }
        "comment_reply" -> "replied to your comment"
        "reel_like" -> "liked your Reel"
        "reel_comment" -> "commented on your Reel"
        "reel_repost" -> "reposted your Reel"
        "reel_reply" -> "replied to your Reel comment"
        else -> "interacted with you"
    })
}


private fun notificationIcon(kind: String): NovaIconAsset = when (kind) {
    "like", "reel_like" -> NovaIconAsset.LikeFilled
    "comment", "comment_reply", "reel_comment", "reel_reply" -> NovaIconAsset.Comment
    "reel_repost" -> NovaIconAsset.Repost
    "follow" -> NovaIconAsset.Profile
    else -> NovaIconAsset.Notifications
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
