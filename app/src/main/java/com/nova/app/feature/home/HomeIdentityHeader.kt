package com.nova.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaIconButton
import com.nova.app.ui.components.NovaOrbitRing
import com.nova.app.ui.components.NovaUnreadDot
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import java.time.LocalTime

@Composable
internal fun HomeIdentityHeader(
    firstName: String,
    displayName: String,
    username: String,
    avatarUrl: String,
    unreadCount: Int,
    onSearchClick: () -> Unit,
    onActivityClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NovaSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "nova",
                color = NovaAccent,
                style = NovaType.display,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaIconButton(
                    asset = NovaIconAsset.Search,
                    contentDescription = "Search people",
                    onClick = onSearchClick,
                    size = 48.dp,
                    iconSize = 24.dp,
                    containerColor = Color.Transparent,
                    borderColor = Color.Transparent,
                    contentColor = NovaInk,
                )

                Box {
                    NovaIconButton(
                        asset = NovaIconAsset.Notifications,
                        contentDescription = "Activity",
                        onClick = onActivityClick,
                        size = 48.dp,
                        iconSize = 23.dp,
                        containerColor = Color.Transparent,
                        borderColor = Color.Transparent,
                        contentColor = NovaInk,
                    )
                    if (unreadCount > 0) {
                        NovaUnreadDot(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp),
                        )
                    }
                }

                Surface(
                    onClick = onProfileClick,
                    color = NovaSurface,
                    shape = androidx.compose.foundation.shape.CircleShape,
                ) {
                    NovaOrbitRing(
                        modifier = Modifier.size(48.dp),
                        rings = 1,
                        showLivePoint = true,
                    ) {
                        NovaAvatar(
                            source = avatarUrl,
                            fallbackText = displayName.ifBlank { username },
                            size = 38.dp,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.xxs)) {
            Text(
                text = "${currentGreeting()}, $firstName",
                color = NovaInk,
                style = NovaType.screenTitle,
            )
            Text(
                text = "Your orbit is awake.",
                color = NovaMuted,
                style = NovaType.subtitle,
            )
        }
    }
}

private fun currentGreeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
