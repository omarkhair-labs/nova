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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.nova.app.feature.messages.ConversationScreen
import com.nova.app.feature.messages.MessagesScreen
import com.nova.app.feature.messages.NewGroupDialog
import com.nova.app.feature.messages.NewMessageDialog
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import com.nova.app.ui.components.NovaActiveCallPill
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaInk
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
                kind = intent.getStringExtra(NovaMessagingNavigator.EXTRA_KIND).orEmpty().ifBlank { "direct" },
                membersCount = intent.getIntExtra(NovaMessagingNavigator.EXTRA_MEMBERS_COUNT, 2),
                currentUserRole = intent.getStringExtra(NovaMessagingNavigator.EXTRA_CURRENT_USER_ROLE).orEmpty(),
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


/**
 * MainActivity-owned messaging root used by the V5 primary navigator.
 *
 * The same inbox, create flows, conversation UI and call actions are reused here
 * instead of launching a second root Activity. Deep-link conversations can still
 * use MessagesActivity directly.
 */
@Composable
fun NovaMessagesRootContent(
    onRootRequested: (NovaRootTab) -> Unit,
    onSessionExpired: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NovaBackground,
    ) {
        MessagingActivityContent(
            initialConversation = null,
            onFinish = { onRootRequested(NovaRootTab.Home) },
            onRootRequested = onRootRequested,
            onSessionExpired = onSessionExpired,
        )
    }
}


@Composable
private fun MessagingActivityContent(
    initialConversation: InitialConversation?,
    onFinish: () -> Unit,
    onRootRequested: ((NovaRootTab) -> Unit)? = null,
    onSessionExpired: () -> Unit = onFinish,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaMessagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var activeConversation by remember { mutableStateOf(initialConversation) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showNewMessage by remember { mutableStateOf(false) }
    var showNewGroup by remember { mutableStateOf(false) }
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
        showCreateMenu = false
        showNewMessage = false
        showNewGroup = false
        activeConversation = InitialConversation(
            id = selected.id,
            username = if (selected.isGroup) "group" else selected.otherUser.username,
            displayName = selected.displayName,
            avatarUrl = if (selected.isGroup) {
                selected.membersPreview.firstOrNull()?.avatarUrl.orEmpty()
            } else {
                selected.otherUser.avatarUrl
            },
            kind = selected.kind,
            membersCount = selected.membersCount,
            currentUserRole = selected.currentUserRole,
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

    fun groupClosed() {
        NovaMessagesSignal.requestInboxRefresh()
        if (openedDirectly) {
            onFinish()
        } else {
            activeConversation = null
            refreshUnreadCount()
        }
    }

    fun finishToRoot(tab: NovaRootTab) {
        val rootHandler = onRootRequested
        if (rootHandler != null) {
            rootHandler(tab)
        } else {
            NovaRootNavigationSignal.request(tab)
            onFinish()
        }
    }

    fun startCall(conversation: InitialConversation, kind: NovaCallKind) {
        if (conversation.kind == "group") return
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

        context.startActivity(callIntent)
    }

    BackHandler {
        when {
            showNewMessage -> showNewMessage = false
            showNewGroup -> showNewGroup = false
            showCreateMenu -> showCreateMenu = false
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
                onSessionExpired = onSessionExpired,
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 90.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Surface(
                    onClick = { showCreateMenu = !showCreateMenu },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = NovaAccent,
                    shadowElevation = 7.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (showCreateMenu) "×" else "+",
                            color = NovaBackground,
                            fontSize = if (showCreateMenu) 25.sp else 28.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                DropdownMenu(
                    expanded = showCreateMenu,
                    onDismissRequest = { showCreateMenu = false },
                    modifier = Modifier.width(196.dp),
                    shape = RoundedCornerShape(18.dp),
                    containerColor = NovaBackground,
                    shadowElevation = 8.dp,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "New message",
                                color = NovaInk,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        leadingIcon = {
                            Text("✉", color = NovaAccent, fontSize = 16.sp)
                        },
                        onClick = {
                            showCreateMenu = false
                            showNewMessage = true
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "New group",
                                color = NovaInk,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        leadingIcon = {
                            Text("＋", color = NovaAccent, fontSize = 17.sp)
                        },
                        onClick = {
                            showCreateMenu = false
                            showNewGroup = true
                        },
                    )
                }
            }
        }

        if (showNewMessage) {
            NewMessageDialog(
                onDismiss = { showNewMessage = false },
                onConversationReady = ::selectConversation,
                onSessionExpired = {
                    showNewMessage = false
                    onSessionExpired()
                },
            )
        }

        if (showNewGroup) {
            NewGroupDialog(
                onDismiss = { showNewGroup = false },
                onConversationReady = ::selectConversation,
                onSessionExpired = {
                    showNewGroup = false
                    onSessionExpired()
                },
            )
        }
    } else {
        ConversationScreen(
            conversationId = conversation.id,
            username = conversation.username,
            displayName = conversation.displayName,
            avatarUrl = conversation.avatarUrl,
            isGroup = conversation.kind == "group",
            membersCount = conversation.membersCount,
            onBack = ::backFromConversation,
            onConversationRead = ::refreshUnreadCount,
            onSessionExpired = onSessionExpired,
            onGroupLeft = ::groupClosed,
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
    val kind: String = "direct",
    val membersCount: Int = 2,
    val currentUserRole: String = "",
)
