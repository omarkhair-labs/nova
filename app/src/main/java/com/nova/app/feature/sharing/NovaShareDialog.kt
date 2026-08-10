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
    val sharingRepository = remember(context) {
        NovaSharingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var loadingPeople by remember { mutableStateOf(true) }
    var busyUsername by remember { mutableStateOf<String?>(null) }
    var addingToStory by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        delay(220)
        loadingPeople = true
        when (val result = socialRepository.people(query.trim())) {
            is ApiResult.Success -> {
                people = result.value
                loadingPeople = false
            }
            is ApiResult.Failure -> {
                people = emptyList()
                loadingPeople = false
                error = result.message
            }
        }
    }

    fun sendTo(person: NovaPerson) {
        if (busyUsername != null || addingToStory) return
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

    fun addToStory() {
        val targetPost = postId ?: return
        if (busyUsername != null || addingToStory) return
        scope.launch {
            addingToStory = true
            error = null
            message = null
            when (val result = sharingRepository.addPostToStory(targetPost)) {
                is ApiResult.Success -> message = "Added to your Story"
                is ApiResult.Failure -> error = result.message
            }
            addingToStory = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (busyUsername == null && !addingToStory) onDismiss()
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
                    Surface(
                        onClick = ::addToStory,
                        enabled = busyUsername == null && !addingToStory,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = NovaAccentSoft,
                        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.28f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("✦", color = NovaAccent, fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (addingToStory) "Adding…" else "Add to your Story",
                                    color = NovaInk,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Keeps a live link to the original post",
                                    color = NovaMuted,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it.take(60)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search people", color = NovaMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                    ),
                )

                when {
                    loadingPeople -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                        }
                    }
                    people.isEmpty() -> {
                        Text(
                            text = if (query.isBlank()) "No people available to share with yet." else "No people match that search.",
                            color = NovaMuted,
                            fontSize = 12.sp,
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 270.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(people, key = { it.id }) { person ->
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
                                        TextButton(
                                            onClick = { sendTo(person) },
                                            enabled = busyUsername == null && !addingToStory,
                                        ) {
                                            Text(
                                                if (busyUsername == person.username) "Sending…" else "Send",
                                                color = NovaAccent,
                                            )
                                        }
                                    }
                                }
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
                enabled = busyUsername == null && !addingToStory,
            ) {
                Text("Done")
            }
        },
    )
}
