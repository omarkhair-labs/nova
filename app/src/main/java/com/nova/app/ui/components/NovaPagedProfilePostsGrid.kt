package com.nova.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPost
import com.nova.app.core.social.NovaSocialPagingRepository
import kotlinx.coroutines.launch


@Composable
fun NovaPagedProfilePostsGrid(
    username: String,
    initialPosts: List<NovaPost>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
    emptyTitle: String = "No posts yet",
    emptyMessage: String = "Shared moments will show up here.",
    sectionTitle: String? = "Posts",
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
            sectionTitle = sectionTitle,
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

private const val FIRST_PAGE_SIZE = 24
