package com.nova.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder


/** Stable conversation entry point shared by direct and group messaging. */
@Composable
fun ConversationScreen(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    isGroup: Boolean = false,
    membersCount: Int = 2,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
    onGroupLeft: () -> Unit = onBack,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
) {
    var showGroupInfo by remember(conversationId) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        ConversationScreenV9(
            conversationId = conversationId,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            onBack = onBack,
            onConversationRead = onConversationRead,
            onSessionExpired = onSessionExpired,
        )

        if (isGroup) {
            ConversationCallAction(
                icon = Icons.Filled.Info,
                contentDescription = "Open group info",
                onClick = { showGroupInfo = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 12.dp),
            )
        } else {
            ConversationCallAction(
                icon = Icons.Filled.Call,
                contentDescription = "Start voice call",
                onClick = onAudioCall,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 105.dp),
            )
            ConversationCallAction(
                icon = Icons.Filled.Videocam,
                contentDescription = "Start video call",
                onClick = onVideoCall,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 12.dp),
            )
        }
    }

    if (showGroupInfo && isGroup) {
        GroupInfoDialog(
            conversationId = conversationId,
            onDismiss = { showGroupInfo = false },
            onGroupLeft = {
                showGroupInfo = false
                onGroupLeft()
            },
            onSessionExpired = onSessionExpired,
        )
    }
}


@Composable
private fun ConversationCallAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = NovaBackground,
        border = BorderStroke(1.dp, NovaBorder),
        modifier = modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = NovaAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
