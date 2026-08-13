package com.nova.app.ui.components

import androidx.compose.runtime.Composable
import com.nova.app.core.network.NovaPost


/**
 * Backwards-compatible profile content entry point.
 *
 * V4 keeps the existing screen-level contract but upgrades the shared profile
 * content surface so Reposts can include both Reel reposts and Post reposts.
 */
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
    NovaProfileContentTabsV4(
        username = username,
        initialPosts = initialPosts,
        postsLoading = isLoading,
        postsError = errorMessage,
        onRetryPosts = onRetry,
        onPostClick = onPostClick,
        postsEmptyTitle = emptyTitle,
        postsEmptyMessage = emptyMessage,
    )
}
