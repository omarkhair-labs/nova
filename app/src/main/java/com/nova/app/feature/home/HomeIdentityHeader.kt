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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.feature.plants.PlantupiaBrandGlyph
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.ui.components.NovaIconButton
import com.nova.app.ui.components.NovaUnreadDot
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import java.time.LocalTime

@Composable
internal fun HomeIdentityHeader(
    firstName: String,
    displayName: String,
    username: String,
    avatarUrl: String,
    userId: Long = 0L,
    avatarVariant: Int = -1,
    unreadCount: Int,
    onSearchClick: () -> Unit,
    onActivityClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NovaSpacing.md)) {
        com.nova.app.feature.plants.PlantupiaAccountHeader(
            displayName, username, avatarUrl, userId, avatarVariant, onProfileClick,
            onNotifications = onActivityClick,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (firstName.isNotBlank()) "${currentGreeting()}, $firstName" else currentGreeting(),
                style = PlantupiaText.sectionLabel,
            )

        }
    }
}

private fun currentGreeting(): String = when (LocalTime.now().hour) {
    in 0..4 -> "Hello"
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
