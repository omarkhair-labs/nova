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
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.messaging.NovaGroupDetail
import com.nova.app.core.messaging.NovaGroupMessagingRepository
import com.nova.app.core.messaging.NovaGroupMember
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.social.NovaSocialPagingRepository
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
fun GroupInfoDialog(
    conversationId: Long,
    onDismiss: () -> Unit,
    onGroupLeft: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaGroupMessagingRepository(context.applicationContext)
    }
    val currentUsername = remember(context) {
        NovaSessionStore(context.applicationContext).load()?.cachedUser?.username.orEmpty()
    }
    val scope = rememberCoroutineScope()

    var detail by remember(conversationId) { mutableStateOf<NovaGroupDetail?>(null) }
    var loading by remember(conversationId) { mutableStateOf(true) }
    var busyUsername by remember(conversationId) { mutableStateOf<String?>(null) }
    var leaving by remember(conversationId) { mutableStateOf(false) }
    var deleting by remember(conversationId) { mutableStateOf(false) }
    var showAddMembers by remember(conversationId) { mutableStateOf(false) }
    var error by remember(conversationId) { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            when (val result = repository.detail(conversationId)) {
                is ApiResult.Success -> detail = result.value
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            loading = false
        }
    }

    fun remove(member: NovaGroupMember) {
        if (busyUsername != null || leaving || deleting) return
        scope.launch {
            busyUsername = member.user.username
            error = null
            when (val result = repository.removeMember(conversationId, member.user.username)) {
                is ApiResult.Success -> {
                    detail = result.value
                    if (result.value == null) onGroupLeft()
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            busyUsername = null
        }
    }

    fun leave() {
        if (leaving || deleting || busyUsername != null) return
        scope.launch {
            leaving = true
            error = null
            when (val result = repository.leaveGroup(conversationId)) {
                is ApiResult.Success -> onGroupLeft()
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                    leaving = false
                }
            }
        }
    }

    fun deleteGroup() {
        if (leaving || deleting || busyUsername != null) return
        scope.launch {
            deleting = true
            error = null
            when (val result = repository.deleteGroup(conversationId)) {
                is ApiResult.Success -> onGroupLeft()
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                    deleting = false
                }
            }
        }
    }

    LaunchedEffect(conversationId) { load() }

    val currentDetail = detail
    val role = currentDetail?.conversation?.currentUserRole.orEmpty()
    val canManage = role == "owner" || role == "admin"

    AlertDialog(
        onDismissRequest = {
            if (!leaving && !deleting && busyUsername == null) onDismiss()
        },
        title = {
            Text(currentDetail?.conversation?.displayName ?: "Group info")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    loading && currentDetail == null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                        }
                    }
                    currentDetail == null -> {
                        Text(
                            text = error ?: "Nova couldn't open this group.",
                            color = NovaMuted,
                            fontSize = 12.sp,
                        )
                    }
                    else -> {
                        Text(
                            text = "${currentDetail.conversation.membersCount} members · ${role.ifBlank { "member" }}",
                            color = NovaMuted,
                            fontSize = 12.sp,
                        )

                        if (canManage) {
                            Surface(
                                onClick = { showAddMembers = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(15.dp),
                                color = NovaAccentSoft,
                                border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.25f)),
                            ) {
                                Text(
                                    text = "+ Add people",
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                    color = NovaAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(currentDetail.members, key = { it.user.id }) { member ->
                                val isMe = member.user.username == currentUsername
                                val canRemove = canManage && !isMe && member.role != "owner" &&
                                    !(role == "admin" && member.role == "admin")
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = NovaSurface,
                                    border = BorderStroke(1.dp, NovaBorder),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        NovaAvatar(
                                            source = member.user.avatarUrl,
                                            fallbackText = member.user.name.ifBlank { member.user.username },
                                            size = 38.dp,
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = member.user.name.ifBlank { member.user.username },
                                                color = NovaInk,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "@${member.user.username} · ${member.role}${if (isMe) " · you" else ""}",
                                                color = NovaMuted,
                                                fontSize = 10.sp,
                                            )
                                        }
                                        if (canRemove) {
                                            TextButton(
                                                onClick = { remove(member) },
                                                enabled = busyUsername == null && !leaving && !deleting,
                                            ) {
                                                Text(
                                                    if (busyUsername == member.user.username) "Removing…" else "Remove",
                                                    color = NovaMuted,
                                                    fontSize = 10.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!error.isNullOrBlank()) {
                            Text(error.orEmpty(), color = NovaMuted, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        TextButton(
                            onClick = ::leave,
                            enabled = !leaving && !deleting && busyUsername == null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (leaving) "Leaving…" else "Leave group")
                        }
                        if (role == "owner") {
                            TextButton(
                                onClick = ::deleteGroup,
                                enabled = !leaving && !deleting && busyUsername == null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (deleting) "Deleting…" else "Delete group")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !leaving && !deleting && busyUsername == null,
            ) {
                Text("Done")
            }
        },
    )

    if (showAddMembers && currentDetail != null) {
        AddGroupMembersDialog(
            conversationId = conversationId,
            existingUsernames = currentDetail.members.map { it.user.username }.toSet(),
            repository = repository,
            onDismiss = { showAddMembers = false },
            onUpdated = {
                detail = it
                showAddMembers = false
            },
            onSessionExpired = onSessionExpired,
        )
    }
}


@Composable
private fun AddGroupMembersDialog(
    conversationId: Long,
    existingUsernames: Set<String>,
    repository: NovaGroupMessagingRepository,
    onDismiss: () -> Unit,
    onUpdated: (NovaGroupDetail) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val socialRepository = remember(context) {
        NovaSocialPagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var adding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        delay(220)
        loading = true
        when (val result = socialRepository.people(query.trim())) {
            is ApiResult.Success -> {
                people = result.value.people.filterNot { it.username in existingUsernames }
                error = null
            }
            is ApiResult.Failure -> {
                if (result.statusCode == 401) onSessionExpired() else error = result.message
            }
        }
        loading = false
    }

    fun add() {
        if (adding || selected.isEmpty()) return
        scope.launch {
            adding = true
            error = null
            when (val result = repository.addMembers(conversationId, selected.toList())) {
                is ApiResult.Success -> onUpdated(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                    adding = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!adding) onDismiss() },
        title = { Text("Add people") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    ),
                )
                if (selected.isNotEmpty()) {
                    Text("${selected.size} selected", color = NovaAccent, fontSize = 11.sp)
                }
                when {
                    loading && people.isEmpty() -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) { CircularProgressIndicator(color = NovaAccent) }
                    }
                    people.isEmpty() -> Text(
                        if (query.isBlank()) "No more people to add." else "No matches.",
                        color = NovaMuted,
                        fontSize = 12.sp,
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        items(people, key = { it.id }) { person ->
                            val chosen = person.username in selected
                            Surface(
                                onClick = {
                                    selected = if (chosen) selected - person.username else selected + person.username
                                },
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
                if (!error.isNullOrBlank()) Text(error.orEmpty(), color = NovaMuted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = ::add, enabled = !adding && selected.isNotEmpty()) {
                Text(if (adding) "Adding…" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !adding) { Text("Cancel") }
        },
    )
}
