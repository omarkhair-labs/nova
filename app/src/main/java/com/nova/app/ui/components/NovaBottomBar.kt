package com.nova.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import com.nova.app.navigation.rootNavigationPlan
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaElevation
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType


@Composable
fun NovaBottomBar(
    selected: NovaTab,
    onHomeClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMessagesClick: (() -> Unit)? = null,
    onReelsClick: (() -> Unit)? = null,
    messagesUnreadCount: Int? = null,
) {
    val context = LocalContext.current
    val resolvedUnreadCount = messagesUnreadCount ?: NovaMessagesSignal.unreadCount
    val rootRequestVersion = NovaRootNavigationSignal.requestVersion

    fun dispatchRoot(requested: NovaRootTab) {
        if (selected == NovaTab.Messages || selected == NovaTab.Reels) {
            when (requested) {
                NovaRootTab.Home -> onHomeClick()
                NovaRootTab.People -> onPeopleClick()
                NovaRootTab.Profile -> onProfileClick()
            }
            return
        }

        val currentRoot = when (selected) {
            NovaTab.Home -> NovaRootTab.Home
            NovaTab.People -> NovaRootTab.People
            NovaTab.Profile -> NovaRootTab.Profile
            NovaTab.Messages, NovaTab.Reels -> return
        }

        rootNavigationPlan(currentRoot, requested).forEach { step ->
            when (step) {
                NovaRootTab.Home -> onHomeClick()
                NovaRootTab.People -> onPeopleClick()
                NovaRootTab.Profile -> onProfileClick()
            }
        }
    }

    LaunchedEffect(rootRequestVersion, selected) {
        val requested = NovaRootNavigationSignal.pendingTab ?: return@LaunchedEffect
        if (selected == NovaTab.Messages || selected == NovaTab.Reels) return@LaunchedEffect

        dispatchRoot(requested)
        NovaRootNavigationSignal.consume(requested)
    }

    Surface(
        color = NovaSurface,
        shadowElevation = NovaElevation.floating,
        tonalElevation = NovaElevation.flat,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 5.dp, vertical = NovaSpacing.sm),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaTabItem(
                label = "Home",
                icon = Icons.Filled.Home,
                selected = selected == NovaTab.Home,
                onClick = { dispatchRoot(NovaRootTab.Home) },
            )
            NovaTabItem(
                label = "People",
                icon = Icons.Filled.Search,
                selected = selected == NovaTab.People,
                onClick = { dispatchRoot(NovaRootTab.People) },
            )
            NovaTabItem(
                label = "Reels",
                icon = Icons.Filled.PlayArrow,
                selected = selected == NovaTab.Reels,
                onClick = {
                    if (onReelsClick != null) {
                        onReelsClick()
                    } else if (selected != NovaTab.Reels) {
                        NovaReelsNavigator.open(
                            context = context,
                            replaceCurrentActivity = selected == NovaTab.Messages,
                        )
                    }
                },
            )
            NovaTabItem(
                label = "Messages",
                icon = Icons.Filled.Email,
                selected = selected == NovaTab.Messages,
                badgeCount = resolvedUnreadCount,
                onClick = {
                    if (onMessagesClick != null) {
                        onMessagesClick()
                    } else if (selected != NovaTab.Messages) {
                        NovaMessagingNavigator.openInbox(
                            context = context,
                            replaceCurrentActivity = selected == NovaTab.Reels,
                        )
                    }
                },
            )
            NovaTabItem(
                label = "You",
                icon = Icons.Filled.Person,
                selected = selected == NovaTab.Profile,
                onClick = { dispatchRoot(NovaRootTab.Profile) },
            )
        }
    }
}


@Composable
private fun NovaTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) NovaAccent.copy(alpha = 0.10f) else Color.Transparent,
        label = "nova-tab-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) NovaAccent else NovaMuted,
        label = "nova-tab-content",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        label = "nova-tab-icon-scale",
    )

    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
                if (badgeCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = NovaAccent,
                    ) {
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
    People,
    Reels,
    Messages,
    Profile,
}
