package com.nova.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.calls.NovaCallKind
import com.nova.app.core.calls.NovaCallPerson
import com.nova.app.core.messaging.NovaConversation
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.ConversationScreenV11
import com.nova.app.feature.messages.MessagesScreen
import com.nova.app.feature.messages.NewMessageDialog
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import com.nova.app.ui.components.NovaActiveCallPill
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaTheme
import kotlinx.coroutines.launch


class MessagesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialConversation = intent.getLongExtra(
            NovaMessagingNavigator.EXTRA_CONVERSATION_ID,
            -1L,
        ).takeIf { it > 0L }?.let { conversationId ->
            InitialConversation(
                id = conversationId,
                username = intent.getStringExtra(NovaMessagingNavigator.EXTRA_USERNAME).orEmpty(),
                displayName = intent.getStringExtra(NovaMessagingNavigator.EXTRA_DISPLAY_NAME).orEmpty(),
                avatarUrl = intent.getStringExtra(NovaMessagingNavigator.EXTRA_AVATAR_URL).orEmpty(),
            )
        }

        setContent {
            NovaTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MessagingActivityContent(
                        initialConversation = initialConversation,
                        onFinish = { finish() },
                    )
                    NovaActiveCallPill(
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}


@Composable
private fun MessagingActivityContent(
    initialConversation: InitialConversation?,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaMessagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var activeConversation by remember {
        mutableStateOf(initialConversation)
    }
    var showNewMessage by remember { mutableStateOf(false) }
    val openedDirectly = remember { initialConversation != null }

    fun refreshUnreadCount() {
        scope.launch {
            when (val result = repository.conversations()) {
                is ApiResult.Success -> NovaMessagesSignal.updateUnreadCount(result.value.unreadCount)
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun selectConversation(selected: NovaConversation) {
        showNewMessage = false
        activeConversation = InitialConversation(
            id = selected.id,
            username = selected.otherUser.username,
            displayName = selected.otherUser.name.ifBlank { selected.otherUser.username },
            avatarUrl = selected.otherUser.avatarUrl,
        )
    }

    fun backFromConversation() {
        if (openedDirectly) {
            onFinish()
        } else {
            activeConversation = null
            refreshUnreadCount()
        }
    }

    fun finishToRoot(tab: NovaRootTab) {
        NovaRootNavigationSignal.request(tab)
        onFinish()
    }

    fun startCall(conversation: InitialConversation, kind: NovaCallKind) {
        val callIntent = CallActivity.outgoingIntent(
            context = context,
            conversationId = conversation.id,
            kind = kind,
            peer = NovaCallPerson(
                id = 0L,
                username = conversation.username,
                name = conversation.displayName,
                avatarUrl = conversation.avatarUrl,
            ),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Calls live in a dedicated task. Minimizing the call therefore reveals
        // the existing Nova conversation instead of sending the whole app away.
        context.startActivity(callIntent)
    }

    BackHandler {
        when {
            showNewMessage -> showNewMessage = false
            activeConversation != null -> backFromConversation()
            else -> onFinish()
        }
    }

    val conversation = activeConversation
    if (conversation == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            MessagesScreen(
                onConversationClick = ::selectConversation,
                onHomeClick = { finishToRoot(NovaRootTab.Home) },
                onPeopleClick = { finishToRoot(NovaRootTab.People) },
                onProfileClick = { finishToRoot(NovaRootTab.Profile) },
                onUnreadCountChanged = NovaMessagesSignal::updateUnreadCount,
                onSessionExpired = onFinish,
            )

            Surface(
                onClick = { showNewMessage = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 88.dp),
                shape = RoundedCornerShape(18.dp),
                color = NovaAccent,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = "+  New message",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = NovaBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (showNewMessage) {
            NewMessageDialog(
                onDismiss = { showNewMessage = false },
                onConversationReady = ::selectConversation,
                onSessionExpired = {
                    showNewMessage = false
                    onFinish()
                },
            )
        }
    } else {
        ConversationScreenV11(
            conversationId = conversation.id,
            username = conversation.username,
            displayName = conversation.displayName,
            avatarUrl = conversation.avatarUrl,
            onBack = ::backFromConversation,
            onConversationRead = ::refreshUnreadCount,
            onSessionExpired = onFinish,
            onAudioCall = { startCall(conversation, NovaCallKind.Audio) },
            onVideoCall = { startCall(conversation, NovaCallKind.Video) },
        )
    }
}


private data class InitialConversation(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
)
