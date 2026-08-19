package com.nova.app.feature.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nova.app.feature.messages.details.ConversationDetailsDialog
import com.nova.app.feature.messages.details.ConversationDetailsTab


@Composable
fun ConversationScreenV9(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    themeLabel: String,
    onOpenTheme: () -> Unit,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    var showDetails by remember(conversationId) { mutableStateOf(false) }
    var detailsStartTab by remember(conversationId) { mutableStateOf(ConversationDetailsTab.Details) }
    val identityEndPadding = if (username == "group") 61.dp else 110.dp

    Box(Modifier.fillMaxSize()) {
        ConversationScreenV8(
            conversationId = conversationId,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            onBack = onBack,
            onConversationRead = onConversationRead,
            onSessionExpired = onSessionExpired,
        )

        Surface(
            onClick = {
                detailsStartTab = ConversationDetailsTab.Details
                showDetails = true
            },
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 58.dp, end = identityEndPadding)
                .height(52.dp),
        ) {
            Box(Modifier.fillMaxSize())
        }
    }

    if (showDetails) {
        ConversationDetailsDialog(
            conversationId = conversationId,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            initialTab = detailsStartTab,
            themeLabel = themeLabel,
            onOpenTheme = onOpenTheme,
            onDismiss = { showDetails = false },
            onSessionExpired = onSessionExpired,
        )
    }
}
