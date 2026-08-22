package com.nova.app.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import com.nova.app.core.network.ApiResult
import com.nova.app.core.notifications.NovaNotificationRepository
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.feature.memories.MemoriesRail
import com.nova.app.feature.notifications.NotificationsScreen
import com.nova.app.feature.orbit.OrbitRail
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.pulse.PulseRail
import com.nova.app.feature.rooms.RoomsRail
import com.nova.app.feature.stories.StoriesRail
import com.nova.app.feature.tonight.TonightSurface
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaErrorState
import com.nova.app.ui.components.NovaInlineLoading
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.components.NovaTab
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
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    displayName: String,
    username: String,
    avatarUrl: String,
    posts: List<NovaPost>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    errorMessage: String?,
    deletingPostId: Long?,
    likingPostId: Long?,
    onCreatePost: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onDeletePost: (NovaPost) -> Unit,
    onLikeToggle: (NovaPost) -> Unit,
    onCommentsClick: (NovaPost) -> Unit,
    onResolvePost: suspend (Long) -> ApiResult<NovaPost>,
    onPersonClick: (String) -> Unit,
    onPeopleClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val context = LocalContext.current
    val notificationRepository = remember(context) {
        NovaNotificationRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val firstName = displayName.trim().substringBefore(' ').ifBlank { username }

    var showActivity by remember { mutableStateOf(false) }
    var notificationUnreadCount by remember { mutableStateOf(0) }

    fun refreshUnreadCount() {
        scope.launch {
            when (val result = notificationRepository.notifications()) {
                is ApiResult.Success -> notificationUnreadCount = result.value.unreadCount
                is ApiResult.Failure -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshUnreadCount()
    }

    val pendingPushTarget = NovaPushOpenSignal.pendingTarget
    LaunchedEffect(pendingPushTarget) {
        val target = pendingPushTarget ?: return@LaunchedEffect

        when (target.kind) {
            "follow" -> {
                NovaPushOpenSignal.consume()
                if (target.actorUsername.isNotBlank()) {
                    onPersonClick(target.actorUsername)
                } else {
                    showActivity = true
                }
            }

            "like", "comment" -> {
                val postId = target.postId
                if (postId == null) {
                    NovaPushOpenSignal.consume()
                    showActivity = true
                } else {
                    when (val result = onResolvePost(postId)) {
                        is ApiResult.Success -> {
                            NovaPushOpenSignal.consume()
                            onCommentsClick(result.value)
                        }

                        is ApiResult.Failure -> {
                            NovaPushOpenSignal.consume()
                            showActivity = true
                        }
                    }
                }
            }

            else -> {
                NovaPushOpenSignal.consume()
                showActivity = true
            }
        }
    }

    if (showActivity) {
        BackHandler {
            showActivity = false
            refreshUnreadCount()
        }
        NotificationsScreen(
            onBack = {
                showActivity = false
                refreshUnreadCount()
            },
            onPersonClick = { selectedUsername ->
                showActivity = false
                onPersonClick(selectedUsername)
            },
            onPostClick = { post ->
                showActivity = false
                onCommentsClick(post)
            },
            onUnreadCountChanged = { notificationUnreadCount = it },
            onSessionExpired = { showActivity = false },
        )
        return
    }

    LaunchedEffect(listState, hasMore, isLoading, isLoadingMore, posts.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalItems) ->
                val closeToEnd = totalItems > 0 && lastVisible >= totalItems - 4
                if (
                    closeToEnd &&
                    hasMore &&
                    posts.isNotEmpty() &&
                    !isLoading &&
                    !isLoadingMore
                ) {
                    onLoadMore()
                }
            }
    }

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Home,
                onHomeClick = {},
                onPeopleClick = onPeopleClick,
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading && posts.isNotEmpty(),
            onRefresh = {
                if (!isLoadingMore) onRefresh()
                refreshUnreadCount()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    bottom = NovaSpacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(NovaSpacing.lg),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "nova",
                                color = NovaInk,
                                style = NovaType.pageTitle,
                            )
                            Text(
                                text = "Good to see you, $firstName.",
                                color = NovaMuted,
                                style = NovaType.meta,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { showActivity = true },
                                shape = RoundedCornerShape(16.dp),
                                color = if (notificationUnreadCount > 0) NovaAccentSoft else NovaSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (notificationUnreadCount > 0) NovaAccent.copy(alpha = 0.42f) else NovaBorder,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = NovaSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Activity",
                                        color = NovaInk,
                                        style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                    if (notificationUnreadCount > 0) {
                                        Spacer(modifier = Modifier.width(NovaSpacing.sm))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = NovaAccent,
                                        ) {
                                            Text(
                                                text = if (notificationUnreadCount > 99) "99+" else notificationUnreadCount.toString(),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                color = NovaBackground,
                                                style = NovaType.badge,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(NovaSpacing.sm))

                            Surface(
                                onClick = onProfileClick,
                                shape = RoundedCornerShape(24.dp),
                                color = NovaAccentSoft,
                            ) {
                                NovaAvatar(
                                    source = avatarUrl,
                                    fallbackText = displayName.ifBlank { username },
                                    size = 44.dp,
                                    modifier = Modifier.padding(2.dp),
                                )
                            }
                        }
                    }
                }

                item {
                    TonightSurface(
                        onPersonClick = onPersonClick,
                        onSessionExpired = {},
                    )
                }

                item {
                    PulseRail(
                        displayName = displayName,
                        username = username,
                        avatarUrl = avatarUrl,
                        onSessionExpired = {},
                    )
                }

                item {
                    StoriesRail(
                        displayName = displayName,
                        username = username,
                        avatarUrl = avatarUrl,
                        onSessionExpired = {},
                    )
                }

                item {
                    OrbitRail(
                        onPersonClick = onPersonClick,
                        onSessionExpired = {},
                    )
                }

                item {
                    RoomsRail(
                        onPersonClick = onPersonClick,
                        onSessionExpired = {},
                    )
                }

                item {
                    MemoriesRail(
                        onPersonClick = onPersonClick,
                        onSessionExpired = {},
                    )
                }

                item {
                    NovaCard(
                        onClick = onCreatePost,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
                        ) {
                            NovaAvatar(
                                source = avatarUrl,
                                fallbackText = displayName.ifBlank { username },
                                size = 40.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share a moment",
                                    color = NovaInk,
                                    style = NovaType.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                                )
                                Text(
                                    text = "Photo + caption",
                                    color = NovaMuted,
                                    style = NovaType.micro,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = NovaAccent,
                            ) {
                                Text(
                                    text = "+",
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                    color = NovaBackground,
                                    style = NovaType.title,
                                )
                            }
                        }
                    }
                }

                if (isLoading && posts.isEmpty()) {
                    item {
                        NovaLoadingState(
                            message = "Loading your feed…",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else if (errorMessage != null && posts.isEmpty()) {
                    item {
                        NovaErrorState(
                            title = "Couldn't load your feed",
                            message = errorMessage,
                            onRetry = onRetry,
                        )
                    }
                } else if (posts.isEmpty()) {
                    item {
                        NovaEmptyState(
                            title = "Your feed is ready",
                            message = "Post your first moment or find someone in People to start building your feed.",
                            actionLabel = "Find people",
                            onAction = onPeopleClick,
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Your feed",
                                color = NovaInk,
                                style = NovaType.title,
                            )
                            Text(
                                text = if (isLoading) "Refreshing…" else "Latest",
                                color = NovaMuted,
                                style = NovaType.micro,
                            )
                        }
                    }

                    items(
                        items = posts,
                        key = { it.id },
                    ) { post ->
                        NovaPostCard(
                            post = post,
                            isDeleting = deletingPostId == post.id,
                            isLiking = likingPostId == post.id,
                            onAuthorClick = {
                                if (post.isMine) {
                                    onProfileClick()
                                } else {
                                    onPersonClick(post.author.username)
                                }
                            },
                            onLikeToggle = { onLikeToggle(post) },
                            onCommentsClick = { onCommentsClick(post) },
                            onDelete = { onDeletePost(post) },
                        )
                    }

                    if (hasMore && isLoadingMore) {
                        item {
                            NovaInlineLoading(message = "Loading more moments…")
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            NovaInlineRetry(
                                message = errorMessage,
                                onRetry = if (hasMore) onLoadMore else onRefresh,
                            )
                        }
                    }
                }
            }
        }
    }
}
