package com.nova.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import com.nova.app.navigation.rootNavigationPlan
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface

@Composable
fun NovaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NovaAccent,
            contentColor = Color.White,
            disabledContainerColor = NovaAccent.copy(alpha = 0.36f),
            disabledContentColor = Color.White.copy(alpha = 0.85f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun NovaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NovaBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaInk),
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun NovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(placeholder, color = NovaMuted)
            }
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NovaAccent,
            unfocusedBorderColor = NovaBorder,
            focusedLabelColor = NovaAccent,
            cursorColor = NovaAccent,
            focusedContainerColor = NovaSurface,
            unfocusedContainerColor = NovaSurface,
        ),
    )
}

@Composable
fun NovaHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Text(
                    text = "‹",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = NovaInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(26.dp))
        }

        Text(
            text = title,
            color = NovaInk,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            color = NovaMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}

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
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 5.dp, vertical = 8.dp),
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
                icon = Icons.Filled.People,
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
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
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
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
