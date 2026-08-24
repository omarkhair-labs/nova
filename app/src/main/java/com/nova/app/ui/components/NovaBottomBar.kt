package com.nova.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaElevation
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType


/** Presentation owner for Home / Orbit / Create / Inbox / Profile. */
@Composable
fun NovaBottomBar(
    selected: NovaTab?,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onInboxClick: (() -> Unit)? = null,
    messagesUnreadCount: Int? = null,
    containerColor: Color = NovaSurface,
    inactiveContentColor: Color = NovaMuted,
) {
    val context = LocalContext.current
    val resolvedUnreadCount = messagesUnreadCount ?: NovaMessagesSignal.unreadCount

    Surface(
        color = containerColor,
        shadowElevation = NovaElevation.floating,
        tonalElevation = NovaElevation.flat,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = NovaSpacing.sm, vertical = NovaSpacing.xs),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaTabItem(
                label = "Home",
                icon = NovaIconAsset.Home,
                selected = selected == NovaTab.Home,
                inactiveContentColor = inactiveContentColor,
                onClick = onHomeClick,
            )
            NovaTabItem(
                label = "Orbit",
                icon = NovaIconAsset.Orbit,
                selected = selected == NovaTab.Orbit,
                inactiveContentColor = inactiveContentColor,
                onClick = onOrbitClick,
            )
            NovaCreateTabItem(
                selected = selected == NovaTab.Create,
                onClick = onCreateClick,
            )
            NovaTabItem(
                label = "Inbox",
                icon = NovaIconAsset.Inbox,
                selected = selected == NovaTab.Inbox,
                badgeCount = resolvedUnreadCount,
                inactiveContentColor = inactiveContentColor,
                onClick = {
                    if (onInboxClick != null) {
                        onInboxClick()
                    } else if (selected != NovaTab.Inbox) {
                        NovaMessagingNavigator.openInbox(context = context)
                    }
                },
            )
            NovaTabItem(
                label = "Profile",
                icon = NovaIconAsset.Profile,
                selected = selected == NovaTab.Profile,
                inactiveContentColor = inactiveContentColor,
                onClick = onProfileClick,
            )
        }
    }
}


@Composable
private fun NovaCreateTabItem(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        label = "nova-create-tab-scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else NovaAccent,
        label = "nova-create-tab-color",
    )

    Box(
        modifier = Modifier.widthIn(min = 58.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            color = containerColor,
            shadowElevation = NovaElevation.raised,
        ) {
            Box(contentAlignment = Alignment.Center) {
                NovaIcon(
                    asset = NovaIconAsset.Create,
                    contentDescription = "Create",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
        }
    }
}


@Composable
private fun NovaTabItem(
    label: String,
    icon: NovaIconAsset,
    selected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    inactiveContentColor: Color = NovaMuted,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) NovaAccent else inactiveContentColor,
        label = "nova-tab-content",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        label = "nova-tab-icon-scale",
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 58.dp),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                NovaIcon(
                    asset = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier
                        .size(21.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
                if (badgeCount > 0) {
                    Surface(shape = CircleShape, color = NovaAccent) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            modifier = Modifier.padding(horizontal = NovaSpacing.xs, vertical = 1.dp),
                            color = Color.White,
                            style = NovaType.badge,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(NovaSpacing.xxs))
            Text(
                text = label,
                color = contentColor,
                style = if (selected) NovaType.navigationLabelSelected else NovaType.navigationLabel,
            )
        }
    }
}


enum class NovaTab {
    Home,
    Orbit,
    Create,
    Inbox,
    Profile,
}
