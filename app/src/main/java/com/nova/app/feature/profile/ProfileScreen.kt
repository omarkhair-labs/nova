package com.nova.app.feature.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.SettingsActivity
import com.nova.app.SocialGraphActivity
import com.nova.app.feature.people.MODE_FOLLOWERS
import com.nova.app.feature.people.MODE_FOLLOWING
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaIconButton
import com.nova.app.ui.components.NovaPagedProfilePostsGrid
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType

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
    profileContentOwner: ProfileContentStateOwner,
    onPostClick: (NovaPost) -> Unit,
    onHomeClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current

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
                .padding(horizontal = NovaSpacing.xl, vertical = NovaSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = "You", color = NovaInk, style = NovaType.pageTitle)
                    Text(text = "Your profile on Nova", color = NovaMuted, style = NovaType.meta)
                }
                NovaIconButton(
                    asset = NovaIconAsset.Settings,
                    contentDescription = "Settings",
                    onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                    iconSize = 20.dp,
                )
            }

            Spacer(modifier = Modifier.height(NovaSpacing.xxl))
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
                        style = NovaType.sectionTitle,
                    )
                    Spacer(modifier = Modifier.height(NovaSpacing.xs))
                    Text(
                        text = "@$username",
                        color = NovaMuted,
                        style = NovaType.bodyCompact.copy(fontWeight = FontWeight.Medium),
                    )
                    if (email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(NovaSpacing.xs))
                        Text(text = email, color = NovaMuted, style = NovaType.meta, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(NovaSpacing.xxl))
            NovaCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = NovaSpacing.lg),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ProfileStat(postsCount.toString(), "Posts", Modifier.weight(1f))
                    ProfileStat(
                        followersCount.toString(),
                        "Followers",
                        Modifier.weight(1f),
                        onClick = { openSocialGraph(MODE_FOLLOWERS) },
                    )
                    ProfileStat(
                        followingCount.toString(),
                        "Following",
                        Modifier.weight(1f),
                        onClick = { openSocialGraph(MODE_FOLLOWING) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NovaSpacing.md))
            NovaSecondaryButton(
                text = "Edit profile",
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NovaSpacing.xxl))
            NovaPagedProfilePostsGrid(
                username = username,
                owner = profileContentOwner,
                onPostClick = onPostClick,
                emptyTitle = "Share your first moment",
                emptyMessage = "Your posts will build a visual history here.",
            )
            Spacer(modifier = Modifier.height(NovaSpacing.xl))
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
            .padding(vertical = NovaSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = NovaInk,
            style = NovaType.title.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(NovaSpacing.xxs))
        Text(text = label, color = NovaMuted, style = NovaType.micro)
    }
}
