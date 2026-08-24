package com.nova.app.feature.tonight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nova.app.feature.rooms.RoomTonightSection
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType


@Composable
fun TonightScreen(
    onPersonClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val palette = TonightTheme.live
    Scaffold(
        containerColor = palette.background,
        bottomBar = {
            NovaBottomBar(
                selected = null,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
                onProfileClick = onProfileClick,
                containerColor = palette.surface,
                inactiveContentColor = palette.muted,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = NovaSpacing.lg,
                end = NovaSpacing.lg,
                top = NovaSpacing.lg,
                bottom = NovaSpacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(NovaSpacing.lg),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.xs)) {
                    Text("Tonight", color = palette.ink, style = NovaType.pageTitle)
                    Text("Live with your orbit", color = palette.muted, style = NovaType.bodyCompact)
                }
            }
            item {
                TonightSurface(
                    onPersonClick = onPersonClick,
                    onSessionExpired = onSessionExpired,
                )
            }
            item {
                RoomTonightSection(
                    onPersonClick = onPersonClick,
                    onSessionExpired = onSessionExpired,
                )
            }
        }
    }
}
