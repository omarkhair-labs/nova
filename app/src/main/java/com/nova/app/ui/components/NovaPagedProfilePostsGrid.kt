package com.nova.app.ui.components

import androidx.compose.runtime.Composable
import com.nova.app.core.network.NovaPost


/**
 * Backwards-compatible profile content entry point.
 *
 * Existing profile screens call this component for their post grid. V2 profile
 * closeout upgrades that shared surface to the familiar Posts / Reposts tabs so
 * both your profile and other visible profiles stay consistent without
 * duplicating screen-level state or navigation wiring.
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
    NovaProfileContentTabs(
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
