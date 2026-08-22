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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.ApiResult
import com.nova.app.core.notifications.NovaNotificationRepository
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.feature.notifications.NotificationsScreen
import com.nova.app.feature.orbit.OrbitRail
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.pulse.PulseRail
import com.nova.app.feature.rooms.RoomsRail
import com.nova.app.feature.stories.StoriesRail
import com.nova.app.feature.tonight.TonightSurface
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
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
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                                fontSize = 30.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Good to see you, $firstName.",
                                color = NovaMuted,
                                fontSize = 12.sp,
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
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Activity",
                                        color = NovaInk,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (notificationUnreadCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = NovaAccent,
                                        ) {
                                            Text(
                                                text = if (notificationUnreadCount > 99) "99+" else notificationUnreadCount.toString(),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                color = NovaBackground,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

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
                    Surface(
                        onClick = onCreatePost,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Photo + caption",
                                    color = NovaMuted,
                                    fontSize = 11.sp,
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
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                if (isLoading && posts.isEmpty()) {
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
                                text = "Loading your feed…",
                                color = NovaMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else if (errorMessage != null && posts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Couldn't load your feed",
                                    color = NovaInk,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    text = errorMessage,
                                    color = NovaMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                NovaSecondaryButton(
                                    text = "Try again",
                                    onClick = onRetry,
                                )
                            }
                        }
                    }
                } else if (posts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = NovaAccentSoft,
                                ) {
                                    Text(
                                        text = "✦",
                                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 11.dp),
                                        color = NovaAccent,
                                        fontSize = 23.sp,
                                    )
                                }
                                Spacer(modifier = Modifier.height(15.dp))
                                Text(
                                    text = "Your feed is ready",
                                    color = NovaInk,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    text = "Post your first moment or find someone in People to start building your feed.",
                                    color = NovaMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                NovaSecondaryButton(
                                    text = "Find people",
                                    onClick = onPeopleClick,
                                )
                            }
                        }
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
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (isLoading) "Refreshing…" else "Latest",
                                color = NovaMuted,
                                fontSize = 11.sp,
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(color = NovaAccent)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Loading more moments…",
                                    color = NovaMuted,
                                    fontSize = 12.sp,
                                )
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
                                        text = errorMessage,
                                        color = NovaMuted,
                                        fontSize = 12.sp,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    NovaSecondaryButton(
                                        text = "Try again",
                                        onClick = if (hasMore) onLoadMore else onRefresh,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}