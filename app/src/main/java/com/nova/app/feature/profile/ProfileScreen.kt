package com.nova.app.feature.profile

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
                    Text(text = "PLANTUPIA / ME", style = PlantupiaText.sectionLabel)
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

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Private places, care and orders first. Public identity and shared moments stay separate.",
                style = PlantupiaText.supporting,
            )

            Spacer(modifier = Modifier.height(NovaSpacing.xl))
            PlantupiaProfileSpaces(accountId = userId, onOpenSpaces = onPlantsClick)

            Spacer(modifier = Modifier.height(NovaSpacing.lg))
            PlantupiaProfileSectionHeading(
                eyebrow = "SERVICE / YOUR PLANTUPIA",
                description = "Care access and purchases belong to your private account, not your public profile.",
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            PlantupiaProfileActionCard(
                icon = NovaIconAsset.CareCalendar,
                eyebrow = "CARE & ACCESS",
                title = "Plans and service",
                body = "See the Plantupia care access attached to your account.",
                action = "Open care access",
                onClick = { showCarePlans = true },
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            PlantupiaProfileActionCard(
                icon = NovaIconAsset.Shop,
                eyebrow = "ORDERS",
                title = "Purchases and status",
                body = "Open orders, receipts and the latest status attached to this account.",
                action = "Open my orders",
                enabled = userId > 0L,
                onClick = { PlantupiaShopOpenSignal.openOrders(userId) },
            )

            Spacer(modifier = Modifier.height(NovaSpacing.xl))
            PlantupiaProfileSectionHeading(
                eyebrow = "PUBLIC / WHAT YOU SHARE",
                description = "Only these profile details and moments are public. Your plants, places and care records stay private.",
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
            )
            Spacer(modifier = Modifier.height(NovaSpacing.md))
            NovaSecondaryButton(
                text = "Edit public profile",
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(NovaSpacing.xl))
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

@Composable
private fun PlantupiaProfileActionCard(
    icon: NovaIconAsset,
    eyebrow: String,
    title: String,
    body: String,
    action: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        color = if (enabled) PlantupiaColors.paper else PlantupiaColors.cream,
        shape = PlantupiaShapes.card,
        border = BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = PlantupiaColors.softSage,
                shape = PlantupiaShapes.inset,
            ) {
                Box(Modifier.padding(11.dp)) {
                    NovaIcon(
                        asset = icon,
                        contentDescription = null,
                        tint = PlantupiaColors.forest,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(eyebrow, style = PlantupiaText.sectionLabel)
                Text(title, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                Text(body, style = PlantupiaText.supporting)
                Text(
                    if (enabled) "$action \u2192" else "Unavailable",
                    style = PlantupiaText.sectionLabel,
                )
            }
        }
    }
}
