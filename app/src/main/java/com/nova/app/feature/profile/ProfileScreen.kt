package com.nova.app.feature.profile

import com.nova.app.feature.plants.PlantupiaActionRow
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import com.nova.app.ui.components.NovaAvatar
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.SettingsActivity
import com.nova.app.SocialGraphActivity
import com.nova.app.feature.plants.PlantupiaProfileSpaces
import com.nova.app.feature.plants.PlantupiaMembershipSheet
import com.nova.app.feature.plants.PlantupiaShopOpenSignal
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaShapes
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
import com.nova.app.ui.icons.NovaIcon
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
    onPlantsClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val context = LocalContext.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    var showCarePlans by remember(userId) { mutableStateOf(false) }
    // Private records and the social identity are distinct destinations, not one feed.
    var publicProfile by rememberSaveable(userId) { mutableStateOf(false) }

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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    if (!largeText) Text(
                        text = "My Plantupia",
                        style = PlantupiaText.editorialHeading,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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

            if (largeText) Text(
                text = "My Plantupia",
                style = PlantupiaText.editorialHeading,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )

            Spacer(modifier = Modifier.height(NovaSpacing.lg))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "Private world", true to "Circle profile").forEach { (isPublic, label) ->
                    val selected = publicProfile == isPublic
                    Surface(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .selectable(selected = selected, role = Role.Tab, onClick = { publicProfile = isPublic }),
                        color = if (selected) PlantupiaColors.forest else PlantupiaColors.paper,
                        shape = PlantupiaShapes.card,
                        border = if (selected) null else BorderStroke(1.dp, PlantupiaColors.border),
                    ) {
                        Box(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = PlantupiaText.body, fontWeight = FontWeight.SemiBold,
                                color = if (selected) androidx.compose.ui.graphics.Color.White else PlantupiaColors.forest)
                        }
                    }
                }
            }
            Spacer(Modifier.height(NovaSpacing.lg))
            if (!publicProfile) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val name = displayName.ifBlank { username }.ifBlank { "Your living world" }
                    if (maxWidth / LocalDensity.current.fontScale >= 220.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            NovaAvatar(source = avatarUrl, fallbackText = name, size = 56.dp,
                                plantupiaUserId = userId, plantupiaAvatarVariant = avatarVariant)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(name, style = PlantupiaText.pageHeading)
                                Text("A world that grows with you.", style = PlantupiaText.supporting)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NovaAvatar(source = avatarUrl, fallbackText = name, size = 56.dp,
                                plantupiaUserId = userId, plantupiaAvatarVariant = avatarVariant)
                            Text(name, style = PlantupiaText.pageHeading)
                            Text("A world that grows with you.", style = PlantupiaText.supporting)
                        }
                    }
                }
                Spacer(Modifier.height(NovaSpacing.lg))
                PlantupiaProfileSpaces(accountId = userId, onOpenSpaces = onPlantsClick)

            Spacer(modifier = Modifier.height(NovaSpacing.lg))
            PlantupiaProfileSectionHeading(
                eyebrow = "Services",
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            PlantupiaProfileActionCard(
                icon = NovaIconAsset.CareCalendar,
                title = "Plans and service",
                onClick = { showCarePlans = true },
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            PlantupiaProfileActionCard(
                icon = NovaIconAsset.Shop,
                title = "Orders",
                enabled = userId > 0L,
                onClick = { PlantupiaShopOpenSignal.openOrders(userId) },
            )

            } else {
            PlantupiaProfileSectionHeading(
                eyebrow = "Public profile",
                description = "Your plants and care records stay private.",
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
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
                showLabel = false,
                framed = false,
            )
            Spacer(modifier = Modifier.height(NovaSpacing.md))
            PlantupiaProfileSocialStats(
                moments = postsCount,
                followers = followersCount,
                following = followingCount,
                onFollowersClick = { openSocialGraph(MODE_FOLLOWERS) },
                onFollowingClick = { openSocialGraph(MODE_FOLLOWING) },
            )

            Spacer(modifier = Modifier.height(NovaSpacing.md))
            NovaSecondaryButton(
                text = "Edit public profile",
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NovaSpacing.xl))
            PlantupiaProfileSectionHeading(eyebrow = "Shared moments")
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            NovaPagedProfilePostsGrid(
                username = username,
                owner = profileContentOwner,
                onPostClick = onPostClick,
                emptyTitle = "Share your first moment",
                emptyMessage = "Your shared photos and posts appear here.",
                plantupiaMomentsOnly = true,
            )
            }
            Spacer(modifier = Modifier.height(NovaSpacing.xl))
        }
    }
    if (showCarePlans) PlantupiaMembershipSheet(onDismiss = { showCarePlans = false })
}

@Composable
private fun PlantupiaProfileActionCard(
    icon: NovaIconAsset,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    PlantupiaActionRow(icon = icon, title = title, enabled = enabled,
        detail = if (enabled) null else "Unavailable", onClick = onClick)
}
