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
import com.nova.app.core.messaging.NovaConversation
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.sharing.NovaSharingRepository
import com.nova.app.core.social.NovaSocialRepository
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
fun NovaShareDialog(
    title: String,
    postId: Long? = null,
    profileUsername: String? = null,
    onDismiss: () -> Unit,
) {
    require((postId != null) xor (profileUsername != null)) {
        "NovaShareDialog requires exactly one share target."
    }

    val context = LocalContext.current
    val socialRepository = remember(context) {
        NovaSocialRepository(context.applicationContext)
    }
    val messagingRepository = remember(context) {
        NovaMessagingRepository(context.applicationContext)
    }
    val sharingRepository = remember(context) {
        NovaSharingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var groups by remember { mutableStateOf<List<NovaConversation>>(emptyList()) }
    var loadingPeople by remember { mutableStateOf(true) }
    var loadingGroups by remember { mutableStateOf(true) }
    var busyUsername by remember { mutableStateOf<String?>(null) }
    var busyConversationId by remember { mutableStateOf<Long?>(null) }
    var addingToStory by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val busy = busyUsername != null || busyConversationId != null || addingToStory

    LaunchedEffect(query) {
        delay(220)
        loadingPeople = true
        loadingGroups = true
        error = null

        when (val result = socialRepository.people(query.trim())) {
            is ApiResult.Success -> people = result.value
            is ApiResult.Failure -> {
                people = emptyList()
                error = result.message
            }
        }
        loadingPeople = false

        when (val result = messagingRepository.conversations(query.trim())) {
            is ApiResult.Success -> groups = result.value.conversations.filter { it.isGroup }
            is ApiResult.Failure -> {
                groups = emptyList()
                if (error == null) error = result.message
            }
        }
        loadingGroups = false
    }

    fun sendTo(person: NovaPerson) {
        if (busy) return
        scope.launch {
            busyUsername = person.username
            error = null
            message = null
            val result = if (postId != null) {
                sharingRepository.sharePost(person.username, postId)
            } else {
                sharingRepository.shareProfile(person.username, profileUsername.orEmpty())
            }
            when (result) {
                is ApiResult.Success -> message = "Sent to @${person.username}"
                is ApiResult.Failure -> error = result.message
            }
            busyUsername = null
        }
    }

    fun sendToGroup(group: NovaConversation) {
        if (busy || !group.isGroup) return
        scope.launch {
            busyConversationId = group.id
            error = null
            message = null
            val result = if (postId != null) {
                sharingRepository.sharePostToConversation(group.id, postId)
            } else {
                sharingRepository.shareProfileToConversation(group.id, profileUsername.orEmpty())
            }
            when (result) {
                is ApiResult.Success -> message = "Sent to ${group.displayName}"
                is ApiResult.Failure -> error = result.message
            }
            busyConversationId = null
        }
    }

    fun addToStory(audience: String) {
        val targetPost = postId ?: return
        if (busy) return
        scope.launch {
            addingToStory = true
            error = null
            message = null
            when (val result = sharingRepository.addPostToStory(targetPost, audience = audience)) {
                is ApiResult.Success -> {
                    message = if (audience == "close_friends") {
                        "Added to your Close Friends Story"
                    } else {
                        "Added to your Story"
                    }
                }
                is ApiResult.Failure -> error = result.message
            }
            addingToStory = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!busy) onDismiss()
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = NovaInk, fontWeight = FontWeight.Bold)
                Text(
                    text = "Share inside Nova",
                    color = NovaMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (postId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StoryAudienceAction(
                            title = if (addingToStory) "Adding…" else "Your Story",
                            subtitle = "Followers",
                            symbol = "✦",
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            onClick = { addToStory("followers") },
                        )
                        StoryAudienceAction(
                            title = if (addingToStory) "Adding…" else "Close Friends",
                            subtitle = "Selected people",
                            symbol = "★",
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            onClick = { addToStory("close_friends") },
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it.take(60)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search people or groups", color = NovaMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                    ),
                )

                if (loadingPeople || loadingGroups) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                    }
                } else if (people.isEmpty() && groups.isEmpty()) {
                    Text(
                        text = if (query.isBlank()) {
                            "No people or groups available to share with yet."
                        } else {
                            "No people or groups match that search."
                        },
                        color = NovaMuted,
                        fontSize = 12.sp,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (groups.isNotEmpty()) {
                            item(key = "groups-label") {
                                Text(
                                    text = "Groups",
                                    color = NovaMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            items(groups, key = { "group-${it.id}" }) { group ->
                                ShareGroupRow(
                                    group = group,
                                    busy = busyConversationId == group.id,
                                    enabled = !busy,
                                    onSend = { sendToGroup(group) },
                                )
                            }
                        }

                        if (people.isNotEmpty()) {
                            item(key = "people-label") {
                                Text(
                                    text = "People",
                                    color = NovaMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = if (groups.isEmpty()) 0.dp else 4.dp),
                                )
                            }
                            items(people, key = { "person-${it.id}" }) { person ->
                                SharePersonRow(
                                    person = person,
                                    busy = busyUsername == person.username,
                                    enabled = !busy,
                                    onSend = { sendTo(person) },
                                )
                            }
                        }
                    }
                }

                if (!message.isNullOrBlank()) {
                    Text(
                        text = message.orEmpty(),
                        color = NovaAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error.orEmpty(),
                        color = NovaMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
            ) {
                Text("Done")
            }
        },
    )
}


@Composable
private fun ShareGroupRow(
    group: NovaConversation,
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
                source = group.membersPreview.firstOrNull()?.avatarUrl.orEmpty(),
                fallbackText = group.displayName,
                size = 38.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.displayName,
                    color = NovaInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = group.displaySubtitle,
                    color = NovaMuted,
                    fontSize = 10.sp,
                )
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
                    text = person.name.ifBlank { person.username },
                    color = NovaInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "@${person.username}",
                    color = NovaMuted,
                    fontSize = 10.sp,
                )
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
            Text(
                text = title,
                color = NovaInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = subtitle, color = NovaMuted, fontSize = 9.sp)
        }
    }
}
