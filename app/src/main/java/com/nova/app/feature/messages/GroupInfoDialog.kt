package com.nova.app.feature.messages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.nova.app.core.messaging.NovaGroupManagementRepository
import com.nova.app.core.messaging.NovaGroupMessagingRepository
import com.nova.app.core.messaging.NovaGroupMember
import com.nova.app.core.messaging.NovaManagedGroupDetail
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
    onGroupUpdated: (title: String, avatarUrl: String, membersCount: Int) -> Unit = { _, _, _ -> },
    onGroupLeft: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val messagingRepository = remember(context) {
        NovaGroupMessagingRepository(context.applicationContext)
    }
    val managementRepository = remember(context) {
        NovaGroupManagementRepository(context.applicationContext)
    }
    val currentUsername = remember(context) {
        NovaSessionStore(context.applicationContext).load()?.cachedUser?.username.orEmpty()
    }
    val scope = rememberCoroutineScope()

    var detail by remember(conversationId) { mutableStateOf<NovaManagedGroupDetail?>(null) }
    var loading by remember(conversationId) { mutableStateOf(true) }
    var busyAction by remember(conversationId) { mutableStateOf<String?>(null) }
    var leaving by remember(conversationId) { mutableStateOf(false) }
    var deleting by remember(conversationId) { mutableStateOf(false) }
    var showAddMembers by remember(conversationId) { mutableStateOf(false) }
    var editingTitle by remember(conversationId) { mutableStateOf(false) }
    var titleDraft by remember(conversationId) { mutableStateOf("") }
    var error by remember(conversationId) { mutableStateOf<String?>(null) }

    fun applyDetail(updated: NovaManagedGroupDetail) {
        detail = updated
        titleDraft = updated.title
        onGroupUpdated(updated.title, updated.avatarUrl, updated.membersCount)
    }

    fun load() {
        scope.launch {
            loading = true
            error = null
            when (val result = managementRepository.detail(conversationId)) {
                is ApiResult.Success -> applyDetail(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            loading = false
        }
    }

    fun rename() {
        if (busyAction != null) return
        val clean = titleDraft.trim()
        if (clean.isBlank()) {
            error = "Give the group a name."
            return
        }
        scope.launch {
            busyAction = "rename"
            error = null
            when (val result = managementRepository.rename(conversationId, clean)) {
                is ApiResult.Success -> {
                    applyDetail(result.value)
                    editingTitle = false
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            busyAction = null
        }
    }

    fun updateAvatar(uri: android.net.Uri) {
        if (busyAction != null) return
        scope.launch {
            busyAction = "avatar"
            error = null
            when (val result = managementRepository.updateAvatar(conversationId, uri)) {
                is ApiResult.Success -> applyDetail(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            busyAction = null
        }
    }

    fun removeAvatar() {
        if (busyAction != null) return
        scope.launch {
            busyAction = "avatar"
            error = null
            when (val result = managementRepository.removeAvatar(conversationId)) {
                is ApiResult.Success -> applyDetail(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            busyAction = null
        }
    }

    fun changeRole(member: NovaGroupMember, role: String) {
        if (busyAction != null) return
        scope.launch {
            busyAction = "role:${member.user.username}"
            error = null
            when (
                val result = managementRepository.setRole(
                    conversationId = conversationId,
                    username = member.user.username,
                    role = role,
                )
            ) {
                is ApiResult.Success -> applyDetail(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            busyAction = null
        }
    }

    fun remove(member: NovaGroupMember) {
        if (busyAction != null || leaving || deleting) return
        scope.launch {
            busyAction = "remove:${member.user.username}"
            error = null
            when (val result = messagingRepository.removeMember(conversationId, member.user.username)) {
                is ApiResult.Success -> {
                    if (result.value == null) {
                        onGroupLeft()
                    } else {
                        load()
                    }
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            busyAction = null
        }
    }

    fun leave() {
        if (leaving || deleting || busyAction != null) return
        scope.launch {
            leaving = true
            error = null
            when (val result = messagingRepository.leaveGroup(conversationId)) {
                is ApiResult.Success -> onGroupLeft()
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                    leaving = false
                }
            }
        }
    }

    fun deleteGroup() {
        if (leaving || deleting || busyAction != null) return
        scope.launch {
            deleting = true
            error = null
            when (val result = messagingRepository.deleteGroup(conversationId)) {
                is ApiResult.Success -> onGroupLeft()
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                    deleting = false
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) updateAvatar(uri)
    }

    LaunchedEffect(conversationId) { load() }

    val currentDetail = detail
    val role = currentDetail?.currentUserRole.orEmpty()
    val canManageAppearance = role == "owner" || role == "admin"
    val isOwner = role == "owner"
    val blocked = leaving || deleting || busyAction != null

    AlertDialog(
        onDismissRequest = { if (!blocked) onDismiss() },
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when {
                    loading && currentDetail == null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                NovaAvatar(
                                    source = currentDetail.avatarUrl,
                                    fallbackText = currentDetail.title,
                                    size = 64.dp,
                                )
                                if (busyAction == "avatar") {
                                    Surface(
                                        modifier = Modifier.size(64.dp),
                                        shape = CircleShape,
                                        color = NovaSurface.copy(alpha = 0.72f),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = NovaAccent,
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentDetail.title,
                                    color = NovaInk,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${currentDetail.membersCount} members · ${role.ifBlank { "member" }}",
                                    color = NovaMuted,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        if (canManageAppearance) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                GroupActionChip(
                                    text = if (currentDetail.avatarUrl.isBlank()) "Add photo" else "Change photo",
                                    enabled = !blocked,
                                    modifier = Modifier.weight(1f),
                                    onClick = { photoPicker.launch("image/*") },
                                )
                                GroupActionChip(
                                    text = "Rename",
                                    enabled = !blocked,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        titleDraft = currentDetail.title
                                        editingTitle = !editingTitle
                                    },
                                )
                            }
                            if (currentDetail.avatarUrl.isNotBlank()) {
                                TextButton(
                                    onClick = ::removeAvatar,
                                    enabled = !blocked,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Remove group photo", color = NovaMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        if (editingTitle && canManageAppearance) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = titleDraft,
                                    onValueChange = { titleDraft = it.take(80) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    placeholder = { Text("Group name") },
                                    shape = RoundedCornerShape(15.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NovaAccent,
                                        unfocusedBorderColor = NovaBorder,
                                        cursorColor = NovaAccent,
                                    ),
                                )
                                Surface(
                                    onClick = ::rename,
                                    shape = CircleShape,
                                    color = NovaAccent,
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (busyAction == "rename") {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = NovaSurface,
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text("✓", color = NovaSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (canManageAppearance) {
                            Surface(
                                onClick = { if (!blocked) showAddMembers = true },
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

                        Text(
                            text = "Members",
                            color = NovaInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            items(currentDetail.members, key = { it.user.id }) { member ->
                                val isMe = member.user.username == currentUsername
                                val canRemove = canManageAppearance && !isMe && member.role != "owner" &&
                                    !(role == "admin" && member.role == "admin")
                                GroupMemberRow(
                                    member = member,
                                    isMe = isMe,
                                    isOwner = isOwner,
                                    canRemove = canRemove,
                                    busyAction = busyAction,
                                    enabled = !blocked || busyAction?.contains(member.user.username) == true,
                                    onMakeAdmin = { changeRole(member, "admin") },
                                    onRemoveAdmin = { changeRole(member, "member") },
                                    onRemove = { remove(member) },
                                )
                            }
                        }

                        if (!error.isNullOrBlank()) {
                            Text(error.orEmpty(), color = NovaMuted, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(1.dp))
                        TextButton(
                            onClick = ::leave,
                            enabled = !blocked,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (leaving) "Leaving…" else "Leave group")
                        }
                        if (isOwner) {
                            TextButton(
                                onClick = ::deleteGroup,
                                enabled = !blocked,
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
            TextButton(onClick = onDismiss, enabled = !blocked) {
                Text("Done")
            }
        },
    )

    if (showAddMembers && currentDetail != null) {
        AddGroupMembersDialog(
            conversationId = conversationId,
            existingUsernames = currentDetail.members.map { it.user.username }.toSet(),
            repository = messagingRepository,
            onDismiss = { showAddMembers = false },
            onUpdated = {
                showAddMembers = false
                load()
            },
            onSessionExpired = onSessionExpired,
        )
    }
}


@Composable
private fun GroupActionChip(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) NovaInk else NovaMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}


@Composable
private fun GroupMemberRow(
    member: NovaGroupMember,
    isMe: Boolean,
    isOwner: Boolean,
    canRemove: Boolean,
    busyAction: String?,
    enabled: Boolean,
    onMakeAdmin: () -> Unit,
    onRemoveAdmin: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
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
                        text = "@${member.user.username}${if (isMe) " · you" else ""}",
                        color = NovaMuted,
                        fontSize = 10.sp,
                    )
                }
                if (member.role != "member") {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = NovaAccentSoft,
                    ) {
                        Text(
                            text = member.role.replaceFirstChar { it.uppercase() },
                            color = NovaAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (!isMe && member.role != "owner" && (isOwner || canRemove)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isOwner) {
                        TextButton(
                            onClick = if (member.role == "admin") onRemoveAdmin else onMakeAdmin,
                            enabled = enabled && busyAction == null,
                        ) {
                            Text(
                                if (busyAction == "role:${member.user.username}") "Updating…"
                                else if (member.role == "admin") "Remove admin"
                                else "Make admin",
                                fontSize = 10.sp,
                            )
                        }
                    }
                    if (canRemove) {
                        TextButton(
                            onClick = onRemove,
                            enabled = enabled && busyAction == null,
                        ) {
                            Text(
                                if (busyAction == "remove:${member.user.username}") "Removing…" else "Remove",
                                color = NovaMuted,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AddGroupMembersDialog(
    conversationId: Long,
    existingUsernames: Set<String>,
    repository: NovaGroupMessagingRepository,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit,
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
                is ApiResult.Success -> onUpdated()
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
