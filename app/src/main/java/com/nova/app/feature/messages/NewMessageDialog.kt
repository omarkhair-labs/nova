package com.nova.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun NewMessageDialog(
    onDismiss: () -> Unit,
    onConversationReady: (NovaConversation) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val storeOwner = remember { NewMessageStoreOwner() }
    val viewModel: NewMessageViewModel = viewModel(
        viewModelStoreOwner = storeOwner,
        key = "new-message",
        factory = NewMessageViewModel.factory(
            messagesRepository = context.appContainer.messagingRepository,
            peopleSearch = context.appContainer.socialRepository::people,
        ),
    )
    val state = viewModel.state

    DisposableEffect(storeOwner) {
        onDispose { storeOwner.viewModelStore.clear() }
    }

    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.conversationReadyVersion) {
        if (state.conversationReadyVersion > 0) {
            state.readyConversation?.let { conversation ->
                onConversationReady(conversation)
                onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (state.openingUsername == null) onDismiss() },
        icon = {
            NovaIcon(
                asset = NovaIconAsset.Messages,
                contentDescription = null,
                tint = NovaAccent,
            )
        },
        title = { Text("New message") },
        text = {
            Column {
                Text(
                    text = "Search Nova and open a conversation directly.",
                    color = NovaMuted,
                    fontSize = 12.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Name or @username", color = NovaMuted) },
                    leadingIcon = {
                        NovaIcon(
                            asset = NovaIconAsset.Search,
                            contentDescription = null,
                            tint = NovaMuted,
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                        focusedContainerColor = NovaSurface,
                        unfocusedContainerColor = NovaSurface,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    state.isLoading && state.people.isEmpty() -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                        }
                    }

                    state.errorMessage != null && state.people.isEmpty() -> {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = NovaMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    }

                    state.people.isEmpty() -> {
                        Text(
                            text = if (state.query.isBlank()) {
                                "No one to message yet."
                            } else {
                                "No people match that search."
                            },
                            color = NovaMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 330.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.people, key = { it.id }) { person ->
                                val isOpening = state.openingUsername == person.username
                                Surface(
                                    onClick = {
                                        if (state.openingUsername == null) viewModel.openConversation(person)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isOpening) NovaAccentSoft else NovaSurface,
                                    border = BorderStroke(1.dp, NovaBorder),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        NovaAvatar(
                                            source = person.avatarUrl,
                                            fallbackText = person.name.ifBlank { person.username },
                                            size = 42.dp,
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = person.name.ifBlank { person.username },
                                                color = NovaInk,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "@${person.username}",
                                                color = NovaMuted,
                                                fontSize = 11.sp,
                                            )
                                        }
                                        Text(
                                            text = if (isOpening) "Opening…" else "Message",
                                            color = NovaAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.errorMessage != null && state.people.isNotEmpty()) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = NovaMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = state.openingUsername == null,
            ) {
                Text("Close")
            }
        },
    )
}


private class NewMessageStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
