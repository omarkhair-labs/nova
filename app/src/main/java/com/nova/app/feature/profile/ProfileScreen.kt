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
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.SettingsActivity
import com.nova.app.SocialGraphActivity
import com.nova.app.feature.plants.PlantupiaProfileSpaces
import com.nova.app.feature.plants.PlantupiaBrandGlyph
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
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    displayName: String,
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
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        PlantupiaBrandGlyph(modifier = Modifier.size(25.dp))
                        Text("plantupia", color = NovaInk,
                            style = NovaType.sectionTitle.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                            ))
                    }
                    Text("أنت، ومساحتك.", color = NovaInk, style = NovaType.pageTitle)
                    Text("حياتك الخاصة هنا، ووجودك مع الناس باختيارك.",
                        color = NovaMuted, style = NovaType.meta)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        NovaIconButton(
                            asset = NovaIconAsset.Inbox,
                            contentDescription = "الرسائل",
                            onClick = { NovaMessagingNavigator.openInbox(context = context) },
                            iconSize = 20.dp,
                        )
                        if (NovaMessagesSignal.unreadCount > 0) {
                            NovaUnreadDot(modifier = Modifier.align(Alignment.TopEnd))
                        }
                    }
                    NovaIconButton(
                        asset = NovaIconAsset.Settings,
                        contentDescription = "الإعدادات",
                        onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                        iconSize = 20.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(NovaSpacing.xl))
            NovaProfileIdentity(
                displayName = displayName,
                username = username,
                avatarUrl = avatarUrl,
                bio = bio,
                location = location,
                link = link,
                interests = interests,
                profileTheme = profileTheme,
                showOrbit = showOrbit,
                isVerified = isVerified,
            )

            Spacer(modifier = Modifier.height(NovaSpacing.xl))
            // The personal, authenticated view only. Plantupia sites and
            // care records must never be injected into public Nova posts
            // or another user's PersonScreen.
            PlantupiaProfileSpaces(onOpenSpaces = onHomeClick)

            Spacer(modifier = Modifier.height(NovaSpacing.xl))
            Text("اللي بتشاركه مع الناس", color = NovaInk, style = NovaType.sectionTitle)
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileStat(
                    postsCount.toString(),
                    "Moments",
                    Modifier.weight(1f),
                    onClick = { profileContentOwner.selectTab(ProfileContentTab.Posts) },
                )
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

            Spacer(modifier = Modifier.height(NovaSpacing.md))
            NovaSecondaryButton(
                text = "عدّل بروفايلك العام",
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NovaSpacing.xxl))
            NovaPagedProfilePostsGrid(
                username = username,
                owner = profileContentOwner,
                onPostClick = onPostClick,
                emptyTitle = "شارك أول لحظة",
                emptyMessage = "منشوراتك العامة هتظهر هنا. صور نباتاتك الخاصة هتفضل في مساحتها.",
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
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Color(0xFFFFFEFA),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(value, color = Color(0xFF153B2E),
                style = NovaType.title.copy(fontWeight = FontWeight.Bold))
            Text(
                when (label) {
                    "Moments" -> "منشورات"
                    "Followers" -> "متابعون"
                    "Following" -> "بتتابع"
                    else -> label
                },
                color = Color(0xFF64796C), style = NovaType.micro
            )
        }
    }
}
