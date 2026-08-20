package com.nova.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.nova.app.app.appContainer
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.profile.ProfileContentStateOwner


/** Stable profile content entry point when the route already owns the content owner. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun NovaPagedProfilePostsGrid(
    username: String,
    owner: ProfileContentStateOwner,
    onPostClick: (NovaPost) -> Unit,
    emptyTitle: String = "No posts yet",
    emptyMessage: String = "Shared moments will show up here.",
    sectionTitle: String? = "Posts",
) {
    val state = owner.state
    NovaProfileContentTabs(
        username = username,
        state = state,
        onTabSelected = owner::selectTab,
        onRetryPosts = owner::loadPosts,
        onLoadMorePosts = owner::loadMorePosts,
        onRetryReposts = owner::retryReposts,
        onLoadMoreReposts = owner::loadMoreReposts,
        onPostClick = onPostClick,
        postsEmptyTitle = emptyTitle,
        postsEmptyMessage = emptyMessage,
    )
}


/**
 * Compatibility entry for PersonScreen's existing first-page owner.
 * Paging/reposts still move behind ProfileContentStateOwner and stable AppContainer contracts.
 */
@Suppress("UNUSED_PARAMETER")
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
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val owner = remember(username, container, scope) {
        ProfileContentStateOwner(
            username = username,
            postRepository = container.postDataRepository,
            pagingRepository = container.peoplePagingRepository,
            scope = scope,
        ).also {
            it.synchronizeExternalPosts(
                posts = initialPosts,
                isLoading = isLoading,
                errorMessage = errorMessage,
            )
        }
    }

    LaunchedEffect(username, initialPosts, isLoading, errorMessage) {
        owner.synchronizeExternalPosts(
            posts = initialPosts,
            isLoading = isLoading,
            errorMessage = errorMessage,
        )
    }

    val state = owner.state
    NovaProfileContentTabs(
        username = username,
        state = state,
        onTabSelected = owner::selectTab,
        onRetryPosts = onRetry,
        onLoadMorePosts = owner::loadMorePosts,
        onRetryReposts = owner::retryReposts,
        onLoadMoreReposts = owner::loadMoreReposts,
        onPostClick = onPostClick,
        postsEmptyTitle = emptyTitle,
        postsEmptyMessage = emptyMessage,
    )
}
