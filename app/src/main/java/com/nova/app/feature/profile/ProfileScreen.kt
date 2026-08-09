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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.AccountSecurityActivity
import com.nova.app.core.network.NovaPost
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaProfilePostsGrid
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "You",
                color = NovaInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                NovaAvatar(
                    source = avatarUrl,
                    fallbackText = displayName.ifBlank { username },
                    size = 86.dp,
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = NovaInk,
                        fontSize = 22.sp,
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
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = email,
                            color = NovaMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            NovaSecondaryButton(
                text = "Edit profile",
                onClick = onEditProfile,
            )

            Spacer(modifier = Modifier.height(10.dp))

            NovaSecondaryButton(
                text = "Security",
                onClick = {
                    context.startActivity(
                        Intent(context, AccountSecurityActivity::class.java)
                            .putExtra(
                                AccountSecurityActivity.EXTRA_MODE,
                                AccountSecurityActivity.MODE_SECURITY,
                            )
                    )
                },
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ProfileStat(value = postsCount.toString(), label = "posts")
                    ProfileStat(value = followersCount.toString(), label = "followers")
                    ProfileStat(value = followingCount.toString(), label = "following")
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            NovaProfilePostsGrid(
                posts = profilePosts,
                isLoading = postsLoading,
                errorMessage = postsError,
                onRetry = onRetryPosts,
                onPostClick = onPostClick,
                emptyTitle = "Share your first moment",
                emptyMessage = "Your posts will build a visual history here.",
            )

            Spacer(modifier = Modifier.height(28.dp))

            NovaSecondaryButton(
                text = "Log out",
                onClick = onLogout,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = NovaInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = NovaMuted,
            fontSize = 12.sp,
        )
    }
}
