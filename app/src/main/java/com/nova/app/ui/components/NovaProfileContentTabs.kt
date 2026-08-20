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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.profile.ProfileContentTab
import com.nova.app.feature.profile.ProfileContentUiState
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


/** Stable profile content surface for authored Posts, Reels, and reposted content. */
@Composable
fun NovaProfileContentTabs(
    username: String,
    state: ProfileContentUiState,
    onTabSelected: (ProfileContentTab) -> Unit,
    onRetryPosts: () -> Unit,
    onLoadMorePosts: () -> Unit,
    onRetryReposts: () -> Unit,
    onLoadMoreReposts: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
    postsEmptyTitle: String,
    postsEmptyMessage: String,
) {
    val context = LocalContext.current
    val isOwnProfile = remember(context, username) {
        NovaSessionStore(context.applicationContext).load()?.cachedUser?.username == username
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ProfileTab(
                label = "Posts",
                selected = state.selectedTab == ProfileContentTab.Posts,
                modifier = Modifier.weight(1f),
            ) {
                if (state.selectedTab != ProfileContentTab.Posts) {
                    onTabSelected(ProfileContentTab.Posts)
                }
            }
            ProfileTab(
                label = "Reels",
                selected = state.selectedTab == ProfileContentTab.Reels,
                modifier = Modifier.weight(1f),
            ) {
                if (state.selectedTab != ProfileContentTab.Reels) {
                    onTabSelected(ProfileContentTab.Reels)
                }
            }
            ProfileTab(
                label = "Reposts",
                selected = state.selectedTab == ProfileContentTab.Reposts,
                modifier = Modifier.weight(1f),
            ) {
                if (state.selectedTab != ProfileContentTab.Reposts) {
                    onTabSelected(ProfileContentTab.Reposts)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when (state.selectedTab) {
            ProfileContentTab.Posts -> ProfilePosts(
                state = state,
                onRetry = onRetryPosts,
                onLoadMore = onLoadMorePosts,
                onPostClick = onPostClick,
                emptyTitle = postsEmptyTitle,
                emptyMessage = postsEmptyMessage,
            )

            ProfileContentTab.Reels -> NovaProfileReelsGrid(
                username = username,
                isOwnProfile = isOwnProfile,
            )

            ProfileContentTab.Reposts -> ProfileReposts(
                username = username,
                state = state,
                isOwnProfile = isOwnProfile,
                onRetry = onRetryReposts,
                onLoadMore = onLoadMoreReposts,
                onPostClick = onPostClick,
            )
        }
    }
}


@Composable
private fun ProfileTab(
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
private fun ProfilePosts(
    state: ProfileContentUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
    emptyTitle: String,
    emptyMessage: String,
) {
    Column {
        NovaProfilePostsGrid(
            posts = state.posts,
            isLoading = state.postsLoading && state.posts.isEmpty(),
            errorMessage = if (state.posts.isEmpty()) {
                state.postsError
            } else {
                state.postsPagingError ?: state.postsError
            },
            onRetry = onRetry,
            onPostClick = onPostClick,
            emptyTitle = emptyTitle,
            emptyMessage = emptyMessage,
            sectionTitle = null,
        )
        if (state.posts.isNotEmpty() && state.postsNextCursor != null) {
            Spacer(Modifier.height(12.dp))
            NovaSecondaryButton(
                text = if (state.postsLoadingMore) "Loading more…" else "Load more posts",
                onClick = { if (!state.postsLoadingMore) onLoadMore() },
            )
        }
    }
}


@Composable
private fun ProfileReposts(
    username: String,
    state: ProfileContentUiState,
    isOwnProfile: Boolean,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
) {
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
                posts = state.reposts,
                isLoading = state.repostsLoading,
                errorMessage = state.repostsError,
                onRetry = onRetry,
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
            if (state.reposts.isNotEmpty() && state.repostsNextCursor != null) {
                NovaSecondaryButton(
                    text = if (state.repostsLoadingMore) "Loading more…" else "Load more Reposted Posts",
                    onClick = { if (!state.repostsLoadingMore) onLoadMore() },
                )
            }
        }
    }
}
