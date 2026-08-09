package com.nova.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder


@Composable
fun ConversationScreenV11(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
) {
    // Apply the IME inset once at the outer conversation boundary. The V8 composer
    // also knows about IME insets; consuming it here prevents the bottom bar from
    // growing by the full keyboard height and leaving a large blank gap.
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

        // Keep only three compact actions visible in the header: voice, search
        // (provided by V9), and video. Video intentionally occupies the old V8
        // refresh slot; realtime already keeps the conversation current and this
        // gives the identity / last-seen block enough room on narrow phones.
        CallHeaderButton(
            text = "☎",
            contentDescription = "Start voice call",
            onClick = onAudioCall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 105.dp),
        )
        CallHeaderButton(
            text = "▣",
            contentDescription = "Start video call",
            onClick = onVideoCall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 12.dp),
        )
    }
}


@Composable
private fun CallHeaderButton(
    text: String,
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
            Text(
                text = text,
                color = NovaAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
