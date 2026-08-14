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
import androidx.compose.runtime.LaunchedEffect
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
import com.nova.app.core.messaging.NovaGroupMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.social.NovaSocialPagingRepository
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun NewGroupDialog(
    onDismiss: () -> Unit,
    onConversationReady: (NovaConversation) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val socialRepository = remember(context) {
        NovaSocialPagingRepository(context.applicationContext)
    }
    val groupRepository = remember(context) {
        NovaGroupMessagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loadingPeople by remember { mutableStateOf(true) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        delay(220)
        loadingPeople = true
        error = null
        when (val result = socialRepository.people(query.trim())) {
            is ApiResult.Success -> {
                people = result.value.people
                loadingPeople = false
            }
            is ApiResult.Failure -> {
                loadingPeople = false
                if (result.statusCode == 401) onSessionExpired() else error = result.message
            }
        }
    }

    fun createGroup() {
        if (creating || title.isBlank() || selected.size < 2) return
        scope.launch {
            creating = true
            error = null
            when (val result = groupRepository.createGroup(title, selected.toList())) {
                is ApiResult.Success -> {
                    creating = false
                    onConversationReady(result.value)
                    onDismiss()
                }
                is ApiResult.Failure -> {
                    creating = false
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text("New group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Name the group, then choose at least two people.",
                    color = NovaMuted,
                    fontSize = 12.sp,
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
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

                if (selected.isNotEmpty()) {
                    Text(
                        text = "${selected.size} selected",
                        color = NovaAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
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
                    loadingPeople && people.isEmpty() -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                        }
                    }
                    people.isEmpty() -> {
                        Text(
                            text = if (query.isBlank()) "No people available yet." else "No people match that search.",
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
                            items(people, key = { it.id }) { person ->
                                val chosen = person.username in selected
                                Surface(
                                    onClick = {
                                        if (!creating) {
                                            selected = if (chosen) {
                                                selected - person.username
                                            } else {
                                                selected + person.username
                                            }
                                        }
                                    },
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

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error.orEmpty(),
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = ::createGroup,
                enabled = !creating && title.isNotBlank() && selected.size >= 2,
            ) {
                Text(if (creating) "Creating…" else "Create group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) {
                Text("Cancel")
            }
        },
    )
}
