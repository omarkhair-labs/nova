package com.nova.app.ui.components

import androidx.compose.runtime.Composable
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.profile.ProfileContentStateOwner


/** Stable profile content entry point shared by self and person profiles. */
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
