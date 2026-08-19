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
import com.nova.app.feature.messages.group.NewGroupViewModel
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun NewGroupDialog(
    onDismiss: () -> Unit,
    onConversationReady: (NovaConversation) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val storeOwner = remember { NewGroupStoreOwner() }
    val viewModel: NewGroupViewModel = viewModel(
        viewModelStoreOwner = storeOwner,
        key = "new-group",
        factory = NewGroupViewModel.factory(
            membershipRepository = context.appContainer.groupMembershipRepository,
            peopleRepository = context.appContainer.groupPeopleRepository,
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
        onDismissRequest = { if (!state.creating) onDismiss() },
        title = { Text("New group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Name the group, then choose at least two people.",
                    color = NovaMuted,
                    fontSize = 12.sp,
                )

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Group name", color = NovaMuted) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                        focusedContainerColor = NovaSurface,
                        unfocusedContainerColor = NovaSurface,
                    ),
                )

                if (state.selected.isNotEmpty()) {
                    Text(
                        text = "${state.selected.size} selected",
                        color = NovaAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search people", color = NovaMuted) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                        focusedContainerColor = NovaSurface,
                        unfocusedContainerColor = NovaSurface,
                    ),
                )

                when {
                    state.loadingPeople && state.people.isEmpty() -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                        }
                    }
                    state.people.isEmpty() -> {
                        Text(
                            text = if (state.query.isBlank()) "No people available yet." else "No people match that search.",
                            color = NovaMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.people, key = { it.id }) { person ->
                                val chosen = person.username in state.selected
                                Surface(
                                    onClick = { viewModel.toggleSelection(person.username) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (chosen) NovaAccentSoft else NovaSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (chosen) NovaAccent.copy(alpha = 0.4f) else NovaBorder,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        NovaAvatar(
                                            source = person.avatarUrl,
                                            fallbackText = person.name.ifBlank { person.username },
                                            size = 40.dp,
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
                                            text = if (chosen) "Selected" else "Add",
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

                if (!state.errorMessage.isNullOrBlank()) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::createGroup,
                enabled = state.canCreate,
            ) {
                Text(if (state.creating) "Creating…" else "Create group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.creating) {
                Text("Cancel")
            }
        },
    )
}


private class NewGroupStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
