package com.nova.app.feature.messages.group

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
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun AddGroupMembersDialog(
    conversationId: Long,
    existingUsernames: Set<String>,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val storeOwner = remember(conversationId, existingUsernames) { AddGroupMembersStoreOwner() }
    val viewModel: AddGroupMembersViewModel = viewModel(
        viewModelStoreOwner = storeOwner,
        key = "add-group-members-$conversationId",
        factory = AddGroupMembersViewModel.factory(
            conversationId = conversationId,
            existingUsernames = existingUsernames,
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
    LaunchedEffect(state.updatedVersion) {
        if (state.updatedVersion > 0) onUpdated()
    }

    AlertDialog(
        onDismissRequest = { if (!state.adding) onDismiss() },
        title = { Text("Add people") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    ),
                )
                if (state.selected.isNotEmpty()) {
                    Text("${state.selected.size} selected", color = NovaAccent, fontSize = 11.sp)
                }
                when {
                    state.loading && state.people.isEmpty() -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) { CircularProgressIndicator(color = NovaAccent) }
                    }
                    state.people.isEmpty() -> Text(
                        if (state.query.isBlank()) "No more people to add." else "No matches.",
                        color = NovaMuted,
                        fontSize = 12.sp,
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        items(state.people, key = { it.id }) { person ->
                            val chosen = person.username in state.selected
                            Surface(
                                onClick = { viewModel.toggleSelection(person.username) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(15.dp),
                                color = if (chosen) NovaAccentSoft else NovaSurface,
                                border = BorderStroke(1.dp, NovaBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                ) {
                                    NovaAvatar(
                                        source = person.avatarUrl,
                                        fallbackText = person.name.ifBlank { person.username },
                                        size = 36.dp,
                                    )
                                    Text(
                                        text = person.name.ifBlank { person.username },
                                        modifier = Modifier.weight(1f),
                                        color = NovaInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(if (chosen) "Selected" else "Add", color = NovaAccent, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                if (!state.errorMessage.isNullOrBlank()) {
                    Text(state.errorMessage.orEmpty(), color = NovaMuted, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::add, enabled = !state.adding && state.selected.isNotEmpty()) {
                Text(if (state.adding) "Adding…" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.adding) { Text("Cancel") }
        },
    )
}


private class AddGroupMembersStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
