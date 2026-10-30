package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaIconButton
import com.nova.app.ui.components.NovaUnreadDot
import com.nova.app.ui.icons.NovaIconAsset

/** The same authenticated identity and contextual destinations on each customer root. */
@Composable
internal fun PlantupiaAccountHeader(
    displayName: String, username: String, avatarUrl: String,
    userId: Long, avatarVariant: Int, onProfile: () -> Unit,
    onNotifications: () -> Unit = { NovaRootNavigationSignal.requestActivity() },
) {
    val context = LocalContext.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            PlantupiaBrandGlyph(Modifier.size(26.dp))
            if (!largeText) Text("Plantupia", fontFamily = FontFamily.Serif, fontSize = 20.sp, color = PlantupiaColors.forest)
        }
        Box {
        NovaIconButton(asset = NovaIconAsset.Inbox, contentDescription = "Social inbox",
            onClick = { NovaMessagingNavigator.openInbox(context) }, size = 48.dp,
            contentColor = PlantupiaColors.forest)
        if (NovaMessagesSignal.unreadCount > 0) NovaUnreadDot(Modifier.align(Alignment.TopEnd))
        }
        Box {
        NovaIconButton(asset = NovaIconAsset.Notifications, contentDescription = "Notifications",
            onClick = onNotifications, size = 48.dp, contentColor = PlantupiaColors.forest)
        if (LocalPlantupiaNoticeCount.current > 0) NovaUnreadDot(Modifier.align(Alignment.TopEnd))
        }
        Surface(onClick = onProfile, shape = CircleShape, color = PlantupiaColors.paper,
            modifier = Modifier.size(48.dp).semantics { contentDescription = "Open your profile" }) {
            Box(Modifier.padding(4.dp)) {
                NovaAvatar(source = avatarUrl, fallbackText = displayName.ifBlank { username }, size = 40.dp,
                    plantupiaUserId = userId.takeIf { it > 0 }, plantupiaAvatarVariant = avatarVariant)
            }
        }
    }
}
