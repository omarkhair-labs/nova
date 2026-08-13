package com.nova.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPost
import com.nova.app.core.social.NovaSocialPagingRepository
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.launch


private enum class ProfileContentTabV4 { Posts, Reels, Reposts }
private const val PROFILE_V4_FIRST_PAGE_SIZE = 24


/**
 * V4 profile content surface.
 *
 * Reposts are a content type, not just Post reposts: the tab now presents both
 * Reel reposts and Post reposts while keeping authored Reels in their own tab.
 */
@Composable
fun NovaProfileContentTabsV4(
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
    var selected by remember(username) { mutableStateOf(ProfileContentTabV4.Posts) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ProfileV4Tab(
                label = "Posts",
                selected = selected == ProfileContentTabV4.Posts,
                modifier = Modifier.weight(1f),
            ) { selected = ProfileContentTabV4.Posts }
            ProfileV4Tab(
                label = "Reels",
                selected = selected == ProfileContentTabV4.Reels,
                modifier = Modifier.weight(1f),
            ) { selected = ProfileContentTabV4.Reels }
            ProfileV4Tab(
                label = "Reposts",
                selected = selected == ProfileContentTabV4.Reposts,
                modifier = Modifier.weight(1f),
            ) { selected = ProfileContentTabV4.Reposts }
        }

        Spacer(Modifier.height(14.dp))

        when (selected) {
            ProfileContentTabV4.Posts -> ProfilePostsV4(
                username = username,
                initialPosts = initialPosts,
                isLoading = postsLoading,
                errorMessage = postsError,
                onRetry = onRetryPosts,
                onPostClick = onPostClick,
                emptyTitle = postsEmptyTitle,
                emptyMessage = postsEmptyMessage,
            )

            ProfileContentTabV4.Reels -> NovaProfileReelsGrid(
                username = username,
                isOwnProfile = isOwnProfile,
            )

            ProfileContentTabV4.Reposts -> ProfileRepostsV4(
                username = username,
                isOwnProfile = isOwnProfile,
                onPostClick = onPostClick,
            )
        }
    }
}


@Composable
private fun ProfileV4Tab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = if (selected) NovaAccentSoft else NovaSurface,
        border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 10.dp),
            color = if (selected) NovaAccent else NovaMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}


@Composable
private fun ProfilePostsV4(
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
    val repository = remember(context) { NovaSocialPagingRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var posts by remember(username) { mutableStateOf(initialPosts) }
    var nextCursor by remember(username) {
        mutableStateOf(initialPosts.takeIf { it.size >= PROFILE_V4_FIRST_PAGE_SIZE }?.lastOrNull()?.id?.toString())
    }
    var loadedOlder by remember(username) { mutableStateOf(false) }
    var loadingMore by remember(username) { mutableStateOf(false) }
    var pagingError by remember(username) { mutableStateOf<String?>(null) }

    LaunchedEffect(username, initialPosts) {
        if (!loadedOlder) {
            posts = initialPosts
            nextCursor = initialPosts
                .takeIf { it.size >= PROFILE_V4_FIRST_PAGE_SIZE }
                ?.lastOrNull()
                ?.id
                ?.toString()
        } else {
            val freshIds = initialPosts.mapTo(mutableSetOf()) { it.id }
            posts = initialPosts + posts.filterNot { it.id in freshIds }
        }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (loadingMore || username.isBlank()) return
        scope.launch {
            loadingMore = true
            pagingError = null
            when (val result = repository.profilePosts(username, cursor)) {
                is ApiResult.Success -> {
                    val ids = posts.mapTo(mutableSetOf()) { it.id }
                    posts = posts + result.value.posts.filterNot { it.id in ids }
                    nextCursor = result.value.nextCursor
                    loadedOlder = true
                }
                is ApiResult.Failure -> pagingError = result.message
            }
            loadingMore = false
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
            Spacer(Modifier.height(12.dp))
            NovaSecondaryButton(
                text = if (loadingMore) "Loading more…" else "Load more posts",
                onClick = { if (!loadingMore) loadMore() },
            )
        }
    }
}


@Composable
private fun ProfileRepostsV4(
    username: String,
    isOwnProfile: Boolean,
    onPostClick: (NovaPost) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { NovaSocialPagingRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var posts by remember(username) { mutableStateOf<List<NovaPost>>(emptyList()) }
    var nextCursor by remember(username) { mutableStateOf<String?>(null) }
    var loading by remember(username) { mutableStateOf(true) }
    var loadingMore by remember(username) { mutableStateOf(false) }
    var error by remember(username) { mutableStateOf<String?>(null) }

    suspend fun loadFirstPage() {
        loading = true
        error = null
        when (val result = repository.profileReposts(username)) {
            is ApiResult.Success -> {
                posts = result.value.posts
                nextCursor = result.value.nextCursor
            }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    LaunchedEffect(username) {
        if (username.isBlank()) loading = false else loadFirstPage()
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (loadingMore || username.isBlank()) return
        scope.launch {
            loadingMore = true
            error = null
            when (val result = repository.profileReposts(username, cursor)) {
                is ApiResult.Success -> {
                    val ids = posts.mapTo(mutableSetOf()) { it.id }
                    posts = posts + result.value.posts.filterNot { it.id in ids }
                    nextCursor = result.value.nextCursor
                }
                is ApiResult.Failure -> error = result.message
            }
            loadingMore = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        NovaProfileRepostedReelsGrid(
            username = username,
            isOwnProfile = isOwnProfile,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Reposted Posts",
                color = NovaInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            NovaProfilePostsGrid(
                posts = posts,
                isLoading = loading,
                errorMessage = error,
                onRetry = { scope.launch { loadFirstPage() } },
                onPostClick = onPostClick,
                emptyTitle = "No Reposted Posts",
                emptyMessage = if (isOwnProfile) {
                    "Posts you repost will show up here."
                } else {
                    "@$username hasn't reposted any Posts yet."
                },
                sectionTitle = null,
                loadingLabel = "Loading Reposted Posts…",
                errorTitle = "Couldn't load Reposted Posts",
            )
            if (posts.isNotEmpty() && nextCursor != null) {
                NovaSecondaryButton(
                    text = if (loadingMore) "Loading more…" else "Load more Reposted Posts",
                    onClick = { if (!loadingMore) loadMore() },
                )
            }
        }
    }
}
