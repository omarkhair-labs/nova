package com.nova.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPost
import com.nova.app.core.social.NovaSocialPagingRepository
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import kotlinx.coroutines.launch


private enum class ProfileContentTab {
    Posts,
    Reels,
    Reposts,
}


@Composable
fun NovaProfileContentTabs(
    username: String,
    initialPosts: List<NovaPost>,
    postsLoading: Boolean,
    postsError: String?,
    onRetryPosts: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
    postsEmptyTitle: String,
    postsEmptyMessage: String,
) {
    val context = LocalContext.current
    val isOwnProfile = remember(context, username) {
        NovaSessionStore(context.applicationContext).load()?.cachedUser?.username == username
    }
    var selectedTab by remember(username) { mutableStateOf(ProfileContentTab.Posts) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(49.dp),
        ) {
            ProfileTabButton(
                selected = selectedTab == ProfileContentTab.Posts,
                contentDescription = "Posts",
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = ProfileContentTab.Posts },
            ) { color ->
                PostsGridIcon(color = color)
            }
            ProfileTabButton(
                selected = selectedTab == ProfileContentTab.Reels,
                contentDescription = "Reels",
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = ProfileContentTab.Reels },
            ) { color ->
                ReelsIcon(color = color)
            }
            ProfileTabButton(
                selected = selectedTab == ProfileContentTab.Reposts,
                contentDescription = "Reposts",
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = ProfileContentTab.Reposts },
            ) { color ->
                RepostIcon(color = color)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NovaBorder),
        )
        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            ProfileContentTab.Posts -> {
                NovaPagedPostsTabGrid(
                    username = username,
                    initialPosts = initialPosts,
                    isLoading = postsLoading,
                    errorMessage = postsError,
                    onRetry = onRetryPosts,
                    onPostClick = onPostClick,
                    emptyTitle = postsEmptyTitle,
                    emptyMessage = postsEmptyMessage,
                )
            }

            ProfileContentTab.Reels -> {
                NovaProfileReelsGrid(
                    username = username,
                    isOwnProfile = isOwnProfile,
                )
            }

            ProfileContentTab.Reposts -> {
                NovaPagedProfileRepostsGrid(
                    username = username,
                    onPostClick = onPostClick,
                    isOwnProfile = isOwnProfile,
                )
            }
        }
    }
}


@Composable
private fun ProfileTabButton(
    selected: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val color = if (selected) NovaInk else NovaMuted
    Column(
        modifier = modifier
            .height(49.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            icon(color)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) NovaInk else Color.Transparent),
        )
    }
}


@Composable
private fun PostsGridIcon(color: Color) {
    Canvas(modifier = Modifier.size(21.dp)) {
        val gap = 2.2.dp.toPx()
        val stroke = 1.35.dp.toPx()
        val cell = (size.width - gap * 2f) / 3f
        repeat(3) { row ->
            repeat(3) { column ->
                drawRect(
                    color = color,
                    topLeft = Offset(
                        x = column * (cell + gap),
                        y = row * (cell + gap),
                    ),
                    size = Size(cell, cell),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
}


@Composable
private fun ReelsIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = 1.45.dp.toPx()
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.10f
        val bottom = size.height * 0.90f

        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.8.dp.toPx()),
            style = Stroke(width = stroke),
        )

        val center = Offset(size.width * 0.52f, size.height * 0.53f)
        val triangle = 4.2.dp.toPx()
        val first = Offset(center.x - triangle * 0.55f, center.y - triangle)
        val second = Offset(center.x - triangle * 0.55f, center.y + triangle)
        val third = Offset(center.x + triangle, center.y)
        drawLine(color, first, second, strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, second, third, strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, third, first, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}


@Composable
private fun RepostIcon(color: Color) {
    Canvas(modifier = Modifier.size(23.dp)) {
        val stroke = 1.8.dp.toPx()
        val arrow = 4.dp.toPx()
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.34f
        val bottom = size.height * 0.68f

        drawLine(
            color = color,
            start = Offset(left, top),
            end = Offset(right, top),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(right, top),
            end = Offset(right - arrow, top - arrow),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(right, top),
            end = Offset(right - arrow, top + arrow),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        drawLine(
            color = color,
            start = Offset(right, bottom),
            end = Offset(left, bottom),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(left, bottom),
            end = Offset(left + arrow, bottom - arrow),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(left, bottom),
            end = Offset(left + arrow, bottom + arrow),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}


@Composable
private fun NovaPagedPostsTabGrid(
    username: String,
    initialPosts: List<NovaPost>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
    emptyTitle: String,
    emptyMessage: String,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaSocialPagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var posts by remember(username) { mutableStateOf(initialPosts) }
    var nextCursor by remember(username) {
        mutableStateOf(initialPosts.takeIf { it.size >= FIRST_PAGE_SIZE }?.lastOrNull()?.id?.toString())
    }
    var hasLoadedMore by remember(username) { mutableStateOf(false) }
    var isLoadingMore by remember(username) { mutableStateOf(false) }
    var pagingError by remember(username) { mutableStateOf<String?>(null) }

    LaunchedEffect(username, initialPosts) {
        if (!hasLoadedMore) {
            posts = initialPosts
            nextCursor = initialPosts
                .takeIf { it.size >= FIRST_PAGE_SIZE }
                ?.lastOrNull()
                ?.id
                ?.toString()
        } else {
            val initialById = initialPosts.associateBy { it.id }
            val retainedOlder = posts.filterNot { it.id in initialById }
            posts = initialPosts + retainedOlder
        }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (isLoadingMore || username.isBlank()) return
        scope.launch {
            isLoadingMore = true
            pagingError = null
            when (val result = repository.profilePosts(username, cursor)) {
                is ApiResult.Success -> {
                    val existingIds = posts.mapTo(mutableSetOf()) { it.id }
                    posts = posts + result.value.posts.filterNot { it.id in existingIds }
                    nextCursor = result.value.nextCursor
                    hasLoadedMore = true
                }
                is ApiResult.Failure -> pagingError = result.message
            }
            isLoadingMore = false
        }
    }

    Column {
        NovaProfilePostsGrid(
            posts = posts,
            isLoading = isLoading && posts.isEmpty(),
            errorMessage = if (posts.isEmpty()) errorMessage else pagingError ?: errorMessage,
            onRetry = onRetry,
            onPostClick = onPostClick,
            emptyTitle = emptyTitle,
            emptyMessage = emptyMessage,
            sectionTitle = null,
        )

        if (posts.isNotEmpty() && nextCursor != null) {
            Spacer(modifier = Modifier.height(12.dp))
            NovaSecondaryButton(
                text = if (isLoadingMore) "Loading more…" else "Load more posts",
                onClick = { if (!isLoadingMore) loadMore() },
            )
        }
    }
}


@Composable
private fun NovaPagedProfileRepostsGrid(
    username: String,
    onPostClick: (NovaPost) -> Unit,
    isOwnProfile: Boolean,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaSocialPagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var reposts by remember(username) { mutableStateOf<List<NovaPost>>(emptyList()) }
    var nextCursor by remember(username) { mutableStateOf<String?>(null) }
    var isLoading by remember(username) { mutableStateOf(true) }
    var isLoadingMore by remember(username) { mutableStateOf(false) }
    var error by remember(username) { mutableStateOf<String?>(null) }

    suspend fun loadFirstPage() {
        isLoading = true
        error = null
        when (val result = repository.profileReposts(username)) {
            is ApiResult.Success -> {
                reposts = result.value.posts
                nextCursor = result.value.nextCursor
            }
            is ApiResult.Failure -> error = result.message
        }
        isLoading = false
    }

    LaunchedEffect(username) {
        if (username.isNotBlank()) {
            loadFirstPage()
        } else {
            isLoading = false
        }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (isLoadingMore || username.isBlank()) return
        scope.launch {
            isLoadingMore = true
            error = null
            when (val result = repository.profileReposts(username, cursor)) {
                is ApiResult.Success -> {
                    val existing = reposts.mapTo(mutableSetOf()) { it.id }
                    reposts = reposts + result.value.posts.filterNot { it.id in existing }
                    nextCursor = result.value.nextCursor
                }
                is ApiResult.Failure -> error = result.message
            }
            isLoadingMore = false
        }
    }

    Column {
        NovaProfilePostsGrid(
            posts = reposts,
            isLoading = isLoading,
            errorMessage = error,
            onRetry = { scope.launch { loadFirstPage() } },
            onPostClick = onPostClick,
            emptyTitle = "No reposts yet",
            emptyMessage = if (isOwnProfile) {
                "Posts you repost will show up here."
            } else {
                "@$username hasn't reposted anything yet."
            },
            sectionTitle = null,
            loadingLabel = "Loading reposts…",
            errorTitle = "Couldn't load reposts",
        )

        if (reposts.isNotEmpty() && nextCursor != null) {
            Spacer(modifier = Modifier.height(12.dp))
            NovaSecondaryButton(
                text = if (isLoadingMore) "Loading more…" else "Load more reposts",
                onClick = { if (!isLoadingMore) loadMore() },
            )
        }
    }
}

private const val FIRST_PAGE_SIZE = 24
