package com.nova.app.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nova.app.feature.plants.PlantupiaCreateIntent
import com.nova.app.feature.plants.PlantupiaClientSection
import com.nova.app.feature.plants.PlantupiaCirclePanel
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.notifications.NotificationsScreen
import com.nova.app.feature.orbit.OrbitRail
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.pulse.PulseRail
import com.nova.app.feature.publishing.MediaPublishItem
import com.nova.app.feature.publishing.MediaPublishStatus
import com.nova.app.feature.rooms.RoomsRail
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
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    displayName: String,
    username: String,
    avatarUrl: String,
    userId: Long = 0L,
    avatarVariant: Int = -1,
    posts: List<NovaPost>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    errorMessage: String?,
    deletingPostId: Long?,
    likingPostIds: Set<Long>,
    repostingPostIds: Set<Long>,
    actionErrorPostId: Long?,
    actionErrorMessage: String?,
    publishingItems: List<MediaPublishItem>,
    onCreatePost: () -> Unit,
    onRetryPublish: (MediaPublishItem) -> Unit,
    onCancelPublish: (MediaPublishItem) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onDeletePost: (NovaPost) -> Unit,
    onLikeToggle: (NovaPost) -> Unit,
    onRepostToggle: (NovaPost) -> Unit,
    onPostClick: (NovaPost) -> Unit,
    onCommentsClick: (NovaPost) -> Unit,
    onResolvePost: suspend (Long) -> ApiResult<NovaPost>,
    onPersonClick: (String) -> Unit,
    onPeopleClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onPulseClick: () -> Unit,
    onTonightClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val context = LocalContext.current
    val notificationRepository = remember(context) {
        NovaNotificationRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val firstName = displayName.trim().substringBefore(' ').ifBlank { username }

    // Route on-demand creation back to the real living record, never a
    // generic disconnected Nova post composer.
    LaunchedEffect(PlantupiaCreateIntent.pending) {
        if (PlantupiaCreateIntent.pending != null) {
            // Publishing-status is an optional item before the living space.
            // Never land on that item when coming from "record" or "ask".
            listState.animateScrollToItem(if (publishingItems.isEmpty()) 1 else 2)
        }
    }

    var showActivity by remember { mutableStateOf(false) }
    var showCommunityFeed by remember { mutableStateOf(false) }
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

    LaunchedEffect(listState, showCommunityFeed, hasMore, isLoading, isLoadingMore, posts.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalItems) ->
                val closeToEnd = totalItems > 0 && lastVisible >= totalItems - 4
                if (
                    showCommunityFeed &&
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
        containerColor = PlantupiaColors.cream,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Home,
                onHomeClick = {},
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
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
                    .background(PlantupiaColors.cream),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = NovaSpacing.lg,
                    end = NovaSpacing.lg,
                    top = NovaSpacing.md,
                    bottom = NovaSpacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(NovaSpacing.md),
            ) {
                item {
                    HomeIdentityHeader(
                        firstName = firstName,
                        displayName = displayName,
                        username = username,
                        avatarUrl = avatarUrl,
                        userId = userId,
                        avatarVariant = avatarVariant,
                        unreadCount = notificationUnreadCount,
                        onSearchClick = onPeopleClick,
                        onActivityClick = { showActivity = true },
                        onProfileClick = onProfileClick,
                    )
                }

                if (publishingItems.isNotEmpty()) {
                    item(key = "media-publishing") {
                        MediaPublishStatus(
                            items = publishingItems,
                            onRetry = onRetryPublish,
                            onCancel = onCancelPublish,
                        )
                    }
                }

                item(key = "plantupia-living-space") {
                    PlantupiaClientSection(
                        accountId = userId,
                        onCommunityCreateClick = onCreatePost,
                        onOpenPlant = {
                            scope.launch {
                                listState.animateScrollToItem(
                                    if (publishingItems.isEmpty()) 1 else 2
                                )
                            }
                        },
                        onOpenLinkedPost = { postId, isCurrent ->
                            scope.launch {
                                when (val result = onResolvePost(postId)) {
                                    is ApiResult.Success -> if (isCurrent()) onPostClick(result.value)
                                    is ApiResult.Failure -> Unit
                                }
                            }
                        },
                    )
                }

                item(key = "home-community-toggle") {
                    // Community remains a deliberate choice, not an endless
                    // autoplay feed intruding into private living-space care.
                    Surface(
                        onClick = onOrbitClick,
                        modifier = Modifier.fillMaxWidth(),
                        color = PlantupiaColors.paper,
                        shape = PlantupiaShapes.inset,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlantupiaColors.border),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 17.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("THE CIRCLE", style = PlantupiaText.sectionLabel)
                                Text(
                                    "Explore the Circle",
                                    style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                                )
                                Text(
                                    "A community you enter when you choose.",
                                    style = PlantupiaText.supporting,
                                )
                            }
                            Text(
                                "→",
                                style = PlantupiaText.editorialHeading,
                            )
                        }
                    }
                }
                if (showCommunityFeed) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "FROM THE CIRCLE", style = PlantupiaText.sectionLabel)
                        Text(text = "Shared by people", style = PlantupiaText.supporting)
                    }
                }

                if (posts.isNotEmpty()) {
                    item(key = "home-lead-post-${posts.first().id}") {
                        val post = posts.first()
                        NovaPostCard(
                            post = post,
                            isDeleting = deletingPostId == post.id,
                            isLiking = post.id in likingPostIds,
                            isReposting = post.id in repostingPostIds,
                            actionErrorMessage = actionErrorMessage.takeIf { actionErrorPostId == post.id },
                            onAuthorClick = {
                                if (post.isMine) onProfileClick() else onPersonClick(post.author.username)
                            },
                            onReposterClick = { reposter ->
                                if (reposter == username) onProfileClick() else onPersonClick(reposter)
                            },
                            onOpenPost = { onPostClick(post) },
                            onLikeToggle = { onLikeToggle(post) },
                            onCommentsClick = { onCommentsClick(post) },
                            onRepostToggle = { onRepostToggle(post) },
                            onDelete = { onDeletePost(post) },
                        )
                    }
                } else if (isLoading) {
                    item {
                        NovaLoadingState(
                            message = "Loading your feed…",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else if (errorMessage != null) {
                    item {
                        NovaErrorState(
                            title = "Couldn't load your feed",
                            message = errorMessage,
                            onRetry = onRetry,
                        )
                    }
                } else {
                    item {
                        NovaEmptyState(
                            title = "Your circle is quiet",
                            message = "Share a plant moment or find people to begin the conversation.",
                            actionLabel = "Find people",
                            onAction = onPeopleClick,
                        )
                    }
                }

                // Optional social layer: rendered only when the user explicitly
                // opens their community feed, keeping the living space first.
                item {
                    TonightSurface(
                        onPersonClick = onPersonClick,
                        onSessionExpired = {},
                        onOpenTonight = onTonightClick,
                    )
                }

                item {
                    PulseRail(
                        displayName = displayName,
                        username = username,
                        avatarUrl = avatarUrl,
                        showCreateCard = false,
                        onOpenFeed = onPulseClick,
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
                    Surface(
                        onClick = onCreatePost,
                        modifier = Modifier.fillMaxWidth(),
                        color = PlantupiaColors.paper,
                        shape = PlantupiaShapes.inset,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlantupiaColors.border),
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
                                plantupiaUserId = userId.takeIf { it > 0L },
                                plantupiaAvatarVariant = avatarVariant,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share a living moment",
                                    style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                                )
                                Text(
                                    text = "Only what you intentionally make public",
                                    style = PlantupiaText.supporting,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PlantupiaColors.forest,
                            ) {
                                Text(
                                    text = "+",
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                    color = PlantupiaColors.paper,
                                    style = NovaType.title,
                                )
                            }
                        }
                    }
                }

                if (posts.isNotEmpty()) {
                    if (posts.size > 1) item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "More from your circle",
                                style = PlantupiaText.editorialHeading,
                            )
                            Text(
                                text = if (isLoading) "Refreshing…" else "Latest",
                                style = PlantupiaText.supporting,
                            )
                        }
                    }

                    items(
                        items = posts.drop(1),
                        key = { it.id },
                    ) { post ->
                        NovaPostCard(
                            post = post,
                            isDeleting = deletingPostId == post.id,
                            isLiking = post.id in likingPostIds,
                            isReposting = post.id in repostingPostIds,
                            actionErrorMessage = actionErrorMessage.takeIf { actionErrorPostId == post.id },
                            onAuthorClick = {
                                if (post.isMine) {
                                    onProfileClick()
                                } else {
                                    onPersonClick(post.author.username)
                                }
                            },
                            onReposterClick = { reposter ->
                                if (reposter == username) onProfileClick() else onPersonClick(reposter)
                            },
                            onOpenPost = { onPostClick(post) },
                            onLikeToggle = { onLikeToggle(post) },
                            onCommentsClick = { onCommentsClick(post) },
                            onRepostToggle = { onRepostToggle(post) },
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
}
