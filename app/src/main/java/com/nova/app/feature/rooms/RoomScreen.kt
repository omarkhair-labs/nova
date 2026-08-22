package com.nova.app.feature.rooms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.MessagesRouteArgs
import com.nova.app.feature.messages.MessagesRouteFactory
import com.nova.app.feature.rooms.domain.model.RoomDetail
import com.nova.app.feature.rooms.domain.model.RoomItem
import com.nova.app.feature.rooms.domain.model.RoomMember
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun RoomScreen(
    conversationId: Long,
    onBack: () -> Unit,
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.roomRepository
    val scope = rememberCoroutineScope()
    val owner = remember(conversationId, repository, scope) {
        RoomStateOwner(conversationId, repository, scope)
    }
    val state = owner.state
    var editDescription by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    LaunchedEffect(owner) {
        owner.load(showSpinner = true)
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NovaBackground,
    ) {
        when {
            state.loading && state.detail == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                }
            }

            state.detail == null -> {
                RoomLoadError(
                    message = state.error ?: "Nova couldn't open this Room.",
                    onBack = onBack,
                    onRetry = { owner.load(showSpinner = true) },
                )
            }

            else -> {
                val detail = state.detail
                val canEdit = detail.conversation.currentUserRole in setOf("owner", "admin")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovaBackground)
                        .statusBarsPadding(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 14.dp,
                        bottom = 34.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        RoomHeader(
                            detail = detail,
                            onBack = onBack,
                            onChat = {
                                val conversation = detail.conversation
                                context.startActivity(
                                    MessagesRouteFactory.conversationIntent(
                                        context,
                                        MessagesRouteArgs(
                                            id = conversation.id,
                                            username = "",
                                            displayName = conversation.title,
                                            avatarUrl = conversation.avatarUrl,
                                            kind = "group",
                                            membersCount = conversation.membersCount,
                                            currentUserRole = conversation.currentUserRole,
                                        ),
                                    )
                                )
                            },
                        )
                    }

                    item {
                        DescriptionCard(
                            description = detail.description,
                            canEdit = canEdit,
                            onEdit = { editDescription = true },
                        )
                    }

                    if (detail.members.isNotEmpty()) {
                        item {
                            MembersRail(
                                members = detail.members,
                                onPersonClick = onPersonClick,
                            )
                        }
                    }

                    item {
                        RoomSectionRail(
                            detail = detail,
                            selectedKind = state.selectedKind,
                            onSelect = owner::selectKind,
                        )
                    }

                    if (state.pinned.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pinned",
                                color = NovaInk,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(state.pinned, key = { "pinned-${it.id}" }) { item ->
                            RoomItemCard(item = item)
                        }
                    }

                    when {
                        state.loading && state.items.isEmpty() -> item {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = NovaAccent,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }

                        state.items.isEmpty() -> item {
                            EmptyRoomSection(kind = state.selectedKind)
                        }

                        else -> {
                            item {
                                Text(
                                    text = if (state.selectedKind == null) "Room timeline" else sectionTitle(state.selectedKind),
                                    color = NovaInk,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            items(state.items, key = { it.id }) { item ->
                                RoomItemCard(item = item)
                            }
                        }
                    }

                    if (state.nextBefore != null) {
                        item {
                            Surface(
                                onClick = owner::loadMore,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = NovaSurface,
                                border = BorderStroke(1.dp, NovaBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (state.loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = NovaAccent,
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = if (state.loadingMore) "Loading…" else "Load older",
                                        color = NovaAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    if (!state.error.isNullOrBlank()) {
                        item {
                            Surface(
                                onClick = { owner.load(showSpinner = false) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = NovaAccentSoft,
                            ) {
                                Text(
                                    text = "${state.error} · Tap to retry",
                                    modifier = Modifier.padding(12.dp),
                                    color = NovaMuted,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                }

                if (editDescription) {
                    RoomDescriptionDialog(
                        initial = detail.description,
                        saving = state.savingDescription,
                        onDismiss = { if (!state.savingDescription) editDescription = false },
                        onSave = { value ->
                            owner.updateDescription(value)
                            editDescription = false
                        },
                    )
                }
            }
        }
    }
}


@Composable
private fun RoomHeader(
    detail: RoomDetail,
    onBack: () -> Unit,
    onChat: () -> Unit,
) {
    val room = detail.conversation
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Text(
                text = "‹",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                color = NovaInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        NovaAvatar(
            source = room.avatarUrl,
            fallbackText = room.title,
            size = 48.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.title,
                color = NovaInk,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${room.membersCount} people · ${room.currentUserRole.ifBlank { "member" }}",
                color = NovaMuted,
                fontSize = 9.sp,
            )
        }
        Surface(
            onClick = onChat,
            shape = RoundedCornerShape(17.dp),
            color = NovaAccent,
        ) {
            Text(
                text = "Chat",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                color = NovaBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


@Composable
private fun DescriptionCard(
    description: String,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    Surface(
        onClick = if (canEdit) onEdit else {},
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (description.isBlank()) "This Room doesn't have a description yet." else description,
                    color = if (description.isBlank()) NovaMuted else NovaInk,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
            if (canEdit) {
                Spacer(modifier = Modifier.width(10.dp))
                Text("Edit", color = NovaAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun MembersRail(
    members: List<RoomMember>,
    onPersonClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "People",
            color = NovaInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(members, key = { it.person.id }) { member ->
                Surface(
                    onClick = { onPersonClick(member.person.username) },
                    modifier = Modifier.width(104.dp),
                    shape = RoundedCornerShape(19.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NovaAvatar(
                            source = member.person.avatarUrl,
                            fallbackText = member.person.name.ifBlank { member.person.username },
                            size = 38.dp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = member.person.name.ifBlank { member.person.username },
                            color = NovaInk,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (member.role != "member") {
                            Text(
                                text = member.role,
                                color = NovaAccent,
                                fontSize = 8.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun RoomSectionRail(
    detail: RoomDetail,
    selectedKind: String?,
    onSelect: (String?) -> Unit,
) {
    val sections = detail.sections
    val values = listOf(
        Triple("All", null, sections.all),
        Triple("Notes", "note", sections.note),
        Triple("Photos", "photo", sections.photo),
        Triple("Videos", "video", sections.video),
        Triple("Music", "music", sections.music),
        Triple("Plans", "plan", sections.plan),
        Triple("Saved", "saved", sections.saved),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(values, key = { it.second ?: "all" }) { (label, kind, count) ->
            val selected = selectedKind == kind
            Surface(
                onClick = { onSelect(kind) },
                shape = RoundedCornerShape(15.dp),
                color = if (selected) NovaAccent else NovaSurface,
                border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
            ) {
                Text(
                    text = "$label $count",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    color = if (selected) NovaBackground else NovaInk,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}


@Composable
private fun RoomItemCard(item: RoomItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = NovaSurface,
        border = BorderStroke(
            1.dp,
            if (item.pinned) NovaAccent.copy(alpha = 0.35f) else NovaBorder,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (item.mediaUrl.isNotBlank() && item.kind == "photo") {
                NovaMediaImage(
                    source = item.mediaUrl,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentDescription = "Room photo",
                )
            }
            if (item.kind == "video") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFF10131B)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", color = Color.White, fontSize = 30.sp)
                }
            }
            Column(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = if (item.mediaUrl.isBlank() || item.kind != "photo") 14.dp else 2.dp,
                    bottom = 14.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = itemKindLabel(item.kind),
                        color = NovaAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (item.pinned) {
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("PINNED", color = NovaMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    item.createdBy?.let { creator ->
                        Text(
                            text = "@${creator.username}",
                            color = NovaMuted,
                            fontSize = 8.sp,
                        )
                    }
                }
                if (item.title.isNotBlank()) {
                    Text(
                        text = item.title,
                        color = NovaInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (item.body.isNotBlank()) {
                    Text(
                        text = item.body,
                        color = NovaInk,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
                if (item.url.isNotBlank()) {
                    Text(
                        text = item.url,
                        color = NovaAccent,
                        fontSize = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                item.scheduledFor?.let { scheduled ->
                    Text(
                        text = "Planned · $scheduled",
                        color = NovaMuted,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}


@Composable
private fun EmptyRoomSection(kind: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("✦", color = NovaAccent, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (kind == null) "This Room is waiting for its first shared thing." else "Nothing in ${sectionTitle(kind).lowercase()} yet.",
                color = NovaMuted,
                fontSize = 11.sp,
            )
        }
    }
}


@Composable
private fun RoomDescriptionDialog(
    initial: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Room description") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 240) value = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("What is this Room about?", color = NovaMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaAccent,
                    unfocusedBorderColor = NovaBorder,
                    cursorColor = NovaAccent,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = !saving) {
                Text(if (saving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Cancel")
            }
        },
    )
}


@Composable
private fun RoomLoadError(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = NovaMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onBack, shape = RoundedCornerShape(16.dp), color = NovaSurface) {
                Text("Back", modifier = Modifier.padding(12.dp), color = NovaInk)
            }
            Surface(onClick = onRetry, shape = RoundedCornerShape(16.dp), color = NovaAccent) {
                Text("Retry", modifier = Modifier.padding(12.dp), color = NovaBackground)
            }
        }
    }
}


private fun itemKindLabel(kind: String): String = when (kind) {
    "note" -> "NOTE"
    "photo" -> "PHOTO"
    "video" -> "VIDEO"
    "music" -> "MUSIC"
    "plan" -> "PLAN"
    "saved" -> "SAVED"
    else -> kind.uppercase()
}


private fun sectionTitle(kind: String?): String = when (kind) {
    "note" -> "Notes"
    "photo" -> "Photos"
    "video" -> "Videos"
    "music" -> "Music"
    "plan" -> "Plans"
    "saved" -> "Saved"
    else -> "Room timeline"
}
