package com.nova.app.feature.home

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
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface

@Composable
fun HomeScreen(
    displayName: String,
    username: String,
    onProfileClick: () -> Unit,
) {
    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Home,
                onHomeClick = {},
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground)
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "nova",
                        color = NovaInk,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Good to see you, ${displayName.substringBefore(' ')}.",
                        color = NovaMuted,
                        fontSize = 13.sp,
                    )
                }

                Surface(
                    onClick = onProfileClick,
                    shape = RoundedCornerShape(18.dp),
                    color = NovaAccentSoft,
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "N",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        color = NovaAccent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = NovaSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder),
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "Your space is ready",
                        color = NovaInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "@$username · This is your home. Posts, people and moments will live here.",
                        color = NovaMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = NovaSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NovaAccentSoft,
                    ) {
                        Text(
                            text = "✦",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            color = NovaAccent,
                            fontSize = 25.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "A quiet feed, for now.",
                        color = NovaInk,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The next build will bring real people and posts into this space.",
                        color = NovaMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
    }
}
