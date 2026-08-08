package com.nova.app.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
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
    onHomeClick: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Profile,
                onHomeClick = onHomeClick,
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
                Surface(
                    modifier = Modifier.size(86.dp),
                    shape = CircleShape,
                    color = NovaAccentSoft,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "N",
                            color = NovaAccent,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

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

            Spacer(modifier = Modifier.height(28.dp))

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
                    ProfileStat(value = "0", label = "posts")
                    ProfileStat(value = "0", label = "people")
                    ProfileStat(value = "0", label = "following")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Your profile is live",
                        color = NovaInk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Photos, bio, links and real social stats come next.",
                        color = NovaMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            NovaSecondaryButton(
                text = "Log out",
                onClick = onLogout,
            )
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
