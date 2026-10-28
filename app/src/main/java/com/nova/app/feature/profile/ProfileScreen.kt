package com.nova.app.feature.profile

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.SettingsActivity
import com.nova.app.SocialGraphActivity
import com.nova.app.feature.plants.PlantupiaProfileSpaces
import com.nova.app.feature.plants.PlantupiaMembershipSheet
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.ui.components.NovaUnreadDot
import androidx.compose.foundation.layout.Box
import com.nova.app.feature.people.MODE_FOLLOWERS
import com.nova.app.feature.people.MODE_FOLLOWING
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaIconButton
import com.nova.app.ui.components.NovaPagedProfilePostsGrid
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaSpacing

@Composable
fun ProfileScreen(
    displayName: String,
    userId: Long = 0L,
    avatarVariant: Int = -1,
    username: String,
    avatarUrl: String,
    bio: String,
    location: String,
    link: String,
    interests: List<String>,
    profileTheme: String,
    showOrbit: Boolean,
    isVerified: Boolean,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    profileContentOwner: ProfileContentStateOwner,
    onPostClick: (NovaPost) -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val context = LocalContext.current
    var showCarePlans by remember(userId) { mutableStateOf(false) }

    fun openSocialGraph(mode: String) {
        context.startActivity(
            Intent(context, SocialGraphActivity::class.java)
                .putExtra(SocialGraphActivity.EXTRA_USERNAME, username)
                .putExtra(SocialGraphActivity.EXTRA_MODE, mode)
        )
    }

    Scaffold(
        containerColor = PlantupiaColors.cream,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Profile,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
                onProfileClick = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PlantupiaColors.cream)
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
                    Text(text = "PLANTUPIA / ME", style = PlantupiaText.sectionLabel)
                    Text(text = "Your profile", style = PlantupiaText.editorialHeading)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        NovaIconButton(
                            asset = NovaIconAsset.Inbox,
                            contentDescription = "Messages",
                            onClick = { NovaMessagingNavigator.openInbox(context = context) },
                            iconSize = 20.dp,
                        )
                        if (NovaMessagesSignal.unreadCount > 0) {
                            NovaUnreadDot(modifier = Modifier.align(Alignment.TopEnd))
                        }
                    }
                    NovaIconButton(
                        asset = NovaIconAsset.Settings,
                        contentDescription = "Settings",
                        onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                        iconSize = 20.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(NovaSpacing.xl))
            PlantupiaMeIdentity(
                displayName = displayName,
                userId = userId,
                avatarVariant = avatarVariant,
                username = username,
                avatarUrl = avatarUrl,
                bio = bio,
                location = location,
                link = link,
                interests = interests,
                isVerified = isVerified,
            )

            Spacer(modifier = Modifier.height(NovaSpacing.md))
            NovaSecondaryButton(
                text = "Edit profile",
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NovaSpacing.lg))
            // The personal, authenticated view only. Plantupia sites and
            // care records must never be injected into public Nova posts
            // or another user's PersonScreen.
            PlantupiaProfileSpaces(accountId = userId, onOpenSpaces = onHomeClick)
            Spacer(modifier = Modifier.height(NovaSpacing.md))
            OutlinedButton(
                onClick = { showCarePlans = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Plantupia Care · Plans and access") }

            Spacer(modifier = Modifier.height(NovaSpacing.lg))
            PlantupiaProfileSectionHeading(
                eyebrow = "SHARED MOMENTS",
                description = "What you choose to make public, separate from your private plants and care.",
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            PlantupiaProfileSocialStats(
                moments = postsCount,
                followers = followersCount,
                following = followingCount,
                onFollowersClick = { openSocialGraph(MODE_FOLLOWERS) },
                onFollowingClick = { openSocialGraph(MODE_FOLLOWING) },
            )

            Spacer(modifier = Modifier.height(NovaSpacing.md))
            NovaPagedProfilePostsGrid(
                username = username,
                owner = profileContentOwner,
                onPostClick = onPostClick,
                emptyTitle = "Share your first moment",
                emptyMessage = "Your intentionally shared moments will appear here.",
                plantupiaMomentsOnly = true,
            )
            Spacer(modifier = Modifier.height(NovaSpacing.xl))
        }
    }
    if (showCarePlans) PlantupiaMembershipSheet(onDismiss = { showCarePlans = false })
}
