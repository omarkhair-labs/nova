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
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.feature.messages.group.AddGroupMembersDialog
import com.nova.app.feature.messages.group.GroupInfoViewModel
import com.nova.app.feature.messages.group.model.GroupMember
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun GroupInfoDialog(
    conversationId: Long,
    onDismiss: () -> Unit,
    onGroupUpdated: (title: String, avatarUrl: String, membersCount: Int) -> Unit = { _, _, _ -> },
    onGroupLeft: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val currentUsername = remember(context) {
        NovaSessionStore(context.applicationContext).load()?.cachedUser?.username.orEmpty()
    }
    val storeOwner = remember(conversationId) { GroupInfoStoreOwner() }
    val groupInfoViewModel: GroupInfoViewModel = viewModel(
        viewModelStoreOwner = storeOwner,
        key = "group-info-$conversationId",
        factory = GroupInfoViewModel.factory(
            conversationId = conversationId,
            managementRepository = context.appContainer.groupManagementRepository,
            membershipRepository = context.appContainer.groupMembershipRepository,
            currentUsername = currentUsername,
        ),
    )
    val state = groupInfoViewModel.state

    DisposableEffect(storeOwner) {
        onDispose { storeOwner.viewModelStore.clear() }
    }

    LaunchedEffect(state.groupUpdatedVersion) {
        if (state.groupUpdatedVersion > 0) {
            state.detail?.let { detail ->
                onGroupUpdated(detail.title, detail.avatarUrl, detail.membersCount)
            }
        }
    }
    LaunchedEffect(state.groupLeftVersion) {
        if (state.groupLeftVersion > 0) onGroupLeft()
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) groupInfoViewModel.updateAvatar(uri)
    }

    val currentDetail = state.detail
    val role = currentDetail?.currentUserRole.orEmpty()
    val canManageAppearance = role == "owner" || role == "admin"
    val isOwner = role == "owner"
    val blocked = state.blocked

    AlertDialog(
        onDismissRequest = { if (!blocked) onDismiss() },
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when {
                    state.loading && currentDetail == null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                        }
                    }
                    currentDetail == null -> {
                        Text(
                            text = state.errorMessage ?: "Nova couldn't open this group.",
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
                                if (state.busyAction == "avatar") {
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
                                    onClick = groupInfoViewModel::toggleTitleEditing,
                                )
                            }
                            if (currentDetail.avatarUrl.isNotBlank()) {
                                TextButton(
                                    onClick = groupInfoViewModel::removeAvatar,
                                    enabled = !blocked,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Remove group photo", color = NovaMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        if (state.editingTitle && canManageAppearance) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = state.titleDraft,
                                    onValueChange = groupInfoViewModel::updateTitleDraft,
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
                                    onClick = groupInfoViewModel::rename,
                                    shape = CircleShape,
                                    color = NovaAccent,
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (state.busyAction == "rename") {
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
                                onClick = groupInfoViewModel::openAddMembers,
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
                                val isMe = member.user.username == state.currentUsername
                                val canRemove = canManageAppearance && !isMe && member.role != "owner" &&
                                    !(role == "admin" && member.role == "admin")
                                GroupMemberRow(
                                    member = member,
                                    isMe = isMe,
                                    isOwner = isOwner,
                                    canRemove = canRemove,
                                    busyAction = state.busyAction,
                                    enabled = !blocked || state.busyAction?.contains(member.user.username) == true,
                                    onMakeAdmin = { groupInfoViewModel.changeRole(member, "admin") },
                                    onRemoveAdmin = { groupInfoViewModel.changeRole(member, "member") },
                                    onRemove = { groupInfoViewModel.removeMember(member) },
                                )
                            }
                        }

                        if (!state.errorMessage.isNullOrBlank()) {
                            Text(state.errorMessage.orEmpty(), color = NovaMuted, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(1.dp))
                        TextButton(
                            onClick = groupInfoViewModel::leave,
                            enabled = !blocked,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.leaving) "Leaving…" else "Leave group")
                        }
                        if (isOwner) {
                            TextButton(
                                onClick = groupInfoViewModel::deleteGroup,
                                enabled = !blocked,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (state.deleting) "Deleting…" else "Delete group")
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

    if (state.showAddMembers && currentDetail != null) {
        AddGroupMembersDialog(
            conversationId = conversationId,
            existingUsernames = currentDetail.members.map { it.user.username }.toSet(),
            onDismiss = groupInfoViewModel::dismissAddMembers,
            onUpdated = {
                groupInfoViewModel.dismissAddMembers()
                groupInfoViewModel.reload()
            },
            onSessionExpired = onSessionExpired,
        )
    }
}


private class GroupInfoStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
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
    member: GroupMember,
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
