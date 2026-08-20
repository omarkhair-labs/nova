package com.nova.app.feature.sharing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun NovaShareDialog(
    title: String,
    postId: Long? = null,
    profileUsername: String? = null,
    reelId: Long? = null,
    onExternalShare: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    require(listOf(postId != null, profileUsername != null, reelId != null).count { it } == 1) {
        "NovaShareDialog requires exactly one share target."
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val target = remember(postId, profileUsername, reelId) {
        when {
            postId != null -> SharingTarget.Post(postId)
            reelId != null -> SharingTarget.Reel(reelId)
            else -> SharingTarget.Profile(profileUsername.orEmpty())
        }
    }
    val owner = remember(context, target, scope) {
        val container = context.appContainer
        SharingStateOwner(
            target = target,
            messagesRepository = container.messagingRepository,
            peopleRepository = container.peopleRepository,
            sharingRepository = container.sharingRepository,
            scope = scope,
        )
    }
    LaunchedEffect(owner) {
        owner.start()
    }

    val state = owner.state
    val busy = state.busy
    val canAddToStory = owner.canAddToStory

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = NovaInk, fontWeight = FontWeight.Bold)
                Text("Share inside Nova", color = NovaMuted, fontSize = 12.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canAddToStory) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StoryAudienceAction(
                            title = if (state.addingToStory) "Adding…" else "Your Story",
                            subtitle = "Followers",
                            symbol = "✦",
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            onClick = { owner.addToStory("followers") },
                        )
                        StoryAudienceAction(
                            title = if (state.addingToStory) "Adding…" else "Close Friends",
                            subtitle = "Selected people",
                            symbol = "★",
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            onClick = { owner.addToStory("close_friends") },
                        )
                    }
                }

                if (onExternalShare != null) {
                    Surface(
                        onClick = {
                            if (!busy) {
                                onDismiss()
                                onExternalShare()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NovaAccentSoft,
                        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.25f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Text("↗", color = NovaAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Column {
                                Text("Share outside Nova", color = NovaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Use Android's share menu", color = NovaMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = owner::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search chats or people", color = NovaMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                    ),
                )

                if (state.loadingPeople || state.loadingConversations) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                    }
                } else if (state.people.isEmpty() && state.conversations.isEmpty()) {
                    Text(
                        if (state.query.isBlank()) "No chats or people available to share with yet."
                        else "No chats or people match that search.",
                        color = NovaMuted,
                        fontSize = 12.sp,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.conversations.isNotEmpty()) {
                            item(key = "chats-label") {
                                Text("Recent chats", color = NovaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            items(state.conversations, key = { "conversation-${it.id}" }) { conversation ->
                                ShareConversationRow(
                                    conversation = conversation,
                                    busy = state.busyConversationId == conversation.id,
                                    enabled = !busy,
                                    onSend = { owner.sendToConversation(conversation) },
                                )
                            }
                        }
                        if (state.people.isNotEmpty()) {
                            item(key = "people-label") {
                                Text(
                                    "People",
                                    color = NovaMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = if (state.conversations.isEmpty()) 0.dp else 4.dp),
                                )
                            }
                            items(state.people, key = { "person-${it.id}" }) { person ->
                                SharePersonRow(
                                    person = person,
                                    busy = state.busyUsername == person.username,
                                    enabled = !busy,
                                    onSend = { owner.sendToPerson(person) },
                                )
                            }
                        }
                    }
                }

                state.message?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = NovaAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                state.error?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = NovaMuted, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Done") }
        },
    )
}


@Composable
private fun ShareConversationRow(
    conversation: NovaConversation,
    busy: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    val avatar = if (conversation.isGroup) {
        conversation.membersPreview.firstOrNull()?.avatarUrl.orEmpty()
    } else {
        conversation.otherUser.avatarUrl
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NovaAvatar(source = avatar, fallbackText = conversation.displayName, size = 38.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(conversation.displayName, color = NovaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(conversation.displaySubtitle, color = NovaMuted, fontSize = 10.sp)
            }
            TextButton(onClick = onSend, enabled = enabled) {
                Text(if (busy) "Sending…" else "Send", color = NovaAccent)
            }
        }
    }
}


@Composable
private fun SharePersonRow(
    person: NovaPerson,
    busy: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NovaAvatar(
                source = person.avatarUrl,
                fallbackText = person.name.ifBlank { person.username },
                size = 38.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    person.name.ifBlank { person.username },
                    color = NovaInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("@${person.username}", color = NovaMuted, fontSize = 10.sp)
            }
            TextButton(onClick = onSend, enabled = enabled) {
                Text(if (busy) "Sending…" else "Send", color = NovaAccent)
            }
        }
    }
}


@Composable
private fun StoryAudienceAction(
    title: String,
    subtitle: String,
    symbol: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = NovaAccentSoft,
        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.28f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(symbol, color = NovaAccent, fontSize = 18.sp)
            Text(title, color = NovaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = NovaMuted, fontSize = 9.sp)
        }
    }
}
