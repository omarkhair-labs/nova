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
import com.nova.app.core.social.NovaProfileRepostsRepository
import kotlinx.coroutines.launch


@Composable
fun NovaPagedProfileRepostsGrid(
    username: String,
    active: Boolean,
    onPostClick: (NovaPost) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaProfileRepostsRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var posts by remember(username) { mutableStateOf<List<NovaPost>>(emptyList()) }
    var nextCursor by remember(username) { mutableStateOf<String?>(null) }
    var loaded by remember(username) { mutableStateOf(false) }
    var loading by remember(username) { mutableStateOf(false) }
    var loadingMore by remember(username) { mutableStateOf(false) }
    var error by remember(username) { mutableStateOf<String?>(null) }

    fun loadFirstPage() {
        if (loading || username.isBlank()) return
        scope.launch {
            loading = true
            error = null
            when (val result = repository.reposts(username)) {
                is ApiResult.Success -> {
                    posts = result.value.posts
                    nextCursor = result.value.nextCursor
                    loaded = true
                }
                is ApiResult.Failure -> error = result.message
            }
            loading = false
        }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (loadingMore || username.isBlank()) return
        scope.launch {
            loadingMore = true
            error = null
            when (val result = repository.reposts(username, cursor)) {
                is ApiResult.Success -> {
                    val existingIds = posts.mapTo(mutableSetOf()) { it.id }
                    posts = posts + result.value.posts.filterNot { it.id in existingIds }
                    nextCursor = result.value.nextCursor
                }
                is ApiResult.Failure -> error = result.message
            }
            loadingMore = false
        }
    }

    LaunchedEffect(username, active) {
        if (active && !loaded && !loading) loadFirstPage()
    }

    if (!active) return

    Column {
        NovaProfilePostsGrid(
            posts = posts,
            isLoading = loading,
            errorMessage = error,
            onRetry = ::loadFirstPage,
            onPostClick = onPostClick,
            emptyTitle = "No reposts yet",
            emptyMessage = "Posts you repost will show up here.",
            sectionTitle = "",
        )

        if (posts.isNotEmpty() && nextCursor != null) {
            Spacer(modifier = Modifier.height(12.dp))
            NovaSecondaryButton(
                text = if (loadingMore) "Loading more…" else "Load more reposts",
                onClick = { if (!loadingMore) loadMore() },
            )
        }
    }
}
