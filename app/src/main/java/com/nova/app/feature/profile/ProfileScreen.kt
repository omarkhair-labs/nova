package com.nova.app.feature.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.SettingsActivity
import com.nova.app.SocialGraphActivity
import com.nova.app.core.network.NovaPost
import com.nova.app.feature.people.MODE_FOLLOWERS
import com.nova.app.feature.people.MODE_FOLLOWING
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaPagedProfilePostsGrid
import com.nova.app.ui.components.NovaPagedProfileRepostsGrid
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface

@Suppress("UNUSED_PARAMETER")
@Composable
fun ProfileScreen(
    displayName: String,
    username: String,
    email: String,
    avatarUrl: String,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    profilePosts: List<NovaPost>,
    postsLoading: Boolean,
    postsError: String?,
    onRetryPosts: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
    onHomeClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var selectedContent by remember(username) { mutableStateOf(ProfileContentTab.POSTS) }

    fun openSocialGraph(mode: String) {
        context.startActivity(
            Intent(context, SocialGraphActivity::class.java)
                .putExtra(SocialGraphActivity.EXTRA_USERNAME, username)
                .putExtra(SocialGraphActivity.EXTRA_MODE, mode)
        )
    }

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Profile,
                onHomeClick = onHomeClick,
                onPeopleClick = onPeopleClick,
                onProfileClick = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground)
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "You",
                        color = NovaInk,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Your profile on Nova",
                        color = NovaMuted,
                        fontSize = 13.sp,
                    )
                }

                Surface(
                    onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = NovaInk,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                NovaAvatar(
                    source = avatarUrl,
                    fallbackText = displayName.ifBlank { username },
                    size = 92.dp,
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName.ifBlank { username },
                        color = NovaInk,
                        fontSize = 23.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@$username",
                        color = NovaMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = email,
                            color = NovaMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 17.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ProfileStat(
                        value = postsCount.toString(),
                        label = "Posts",
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStat(
                        value = followersCount.toString(),
                        label = "Followers",
                        modifier = Modifier.weight(1f),
                        onClick = { openSocialGraph(MODE_FOLLOWERS) },
                    )
                    ProfileStat(
                        value = followingCount.toString(),
                        label = "Following",
                        modifier = Modifier.weight(1f),
                        onClick = { openSocialGraph(MODE_FOLLOWING) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            NovaSecondaryButton(
                text = "Edit profile",
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            ProfileContentTabs(
                selected = selectedContent,
                onSelected = { selectedContent = it },
            )

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedContent) {
                ProfileContentTab.POSTS -> {
                    NovaPagedProfilePostsGrid(
                        username = username,
                        initialPosts = profilePosts,
                        isLoading = postsLoading,
                        errorMessage = postsError,
                        onRetry = onRetryPosts,
                        onPostClick = onPostClick,
                        emptyTitle = "Share your first moment",
                        emptyMessage = "Your posts will build a visual history here.",
                        sectionTitle = "",
                    )
                }
                ProfileContentTab.REPOSTS -> {
                    NovaPagedProfileRepostsGrid(
                        username = username,
                        active = true,
                        onPostClick = onPostClick,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private enum class ProfileContentTab {
    POSTS,
    REPOSTS,
}

@Composable
private fun ProfileContentTabs(
    selected: ProfileContentTab,
    onSelected: (ProfileContentTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ProfileContentTabButton(
            symbol = "▦",
            label = "Posts",
            selected = selected == ProfileContentTab.POSTS,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(ProfileContentTab.POSTS) },
        )
        ProfileContentTabButton(
            symbol = "↻",
            label = "Reposts",
            selected = selected == ProfileContentTab.REPOSTS,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(ProfileContentTab.REPOSTS) },
        )
    }
}

@Composable
private fun ProfileContentTabButton(
    symbol: String,
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = NovaBackground,
    ) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = symbol,
                color = if (selected) NovaInk else NovaMuted,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                color = if (selected) NovaInk else NovaMuted,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Spacer(modifier = Modifier.height(7.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = if (selected) NovaAccent else NovaBorder,
            ) {}
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = NovaInk,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = NovaMuted,
            fontSize = 11.sp,
        )
    }
}
