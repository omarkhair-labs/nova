package com.nova.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nova.app.core.calls.NovaCallKind
import com.nova.app.core.calls.NovaCallPerson
import com.nova.app.core.messaging.NovaConversation
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.ConversationScreenV11
import com.nova.app.feature.messages.MessagesScreen
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
                MessagingActivityContent(
                    initialConversation = initialConversation,
                    onFinish = { finish() },
                )
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
    val openedDirectly = remember { initialConversation != null }

    fun refreshUnreadCount() {
        scope.launch {
            when (val result = repository.conversations()) {
                is ApiResult.Success -> NovaMessagesSignal.updateUnreadCount(result.value.unreadCount)
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun backFromConversation() {
        if (openedDirectly) {
            onFinish()
        } else {
            activeConversation = null
            refreshUnreadCount()
        }
    }

    fun startCall(conversation: InitialConversation, kind: NovaCallKind) {
        context.startActivity(
            CallActivity.outgoingIntent(
                context = context,
                conversationId = conversation.id,
                kind = kind,
                peer = NovaCallPerson(
                    id = 0L,
                    username = conversation.username,
                    name = conversation.displayName,
                    avatarUrl = conversation.avatarUrl,
                ),
            )
        )
    }

    BackHandler {
        if (activeConversation != null) {
            backFromConversation()
        } else {
            onFinish()
        }
    }

    val conversation = activeConversation
    if (conversation == null) {
        MessagesScreen(
            onConversationClick = { selected: NovaConversation ->
                activeConversation = InitialConversation(
                    id = selected.id,
                    username = selected.otherUser.username,
                    displayName = selected.otherUser.name.ifBlank { selected.otherUser.username },
                    avatarUrl = selected.otherUser.avatarUrl,
                )
            },
            onHomeClick = onFinish,
            onPeopleClick = onFinish,
            onProfileClick = onFinish,
            onUnreadCountChanged = NovaMessagesSignal::updateUnreadCount,
            onSessionExpired = onFinish,
        )
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
