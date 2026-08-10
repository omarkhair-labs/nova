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
import com.nova.app.core.messaging.NovaConversation
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
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
fun NewMessageDialog(
    onDismiss: () -> Unit,
    onConversationReady: (NovaConversation) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val socialRepository = remember(context) {
        NovaSocialRepository(context.applicationContext)
    }
    val messagingRepository = remember(context) {
        NovaMessagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var openingUsername by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        delay(220)
        isLoading = true
        errorMessage = null
        when (val result = socialRepository.people(query.trim())) {
            is ApiResult.Success -> {
                people = result.value
                isLoading = false
            }

            is ApiResult.Failure -> {
                isLoading = false
                if (result.statusCode == 401) {
                    onSessionExpired()
                } else {
                    errorMessage = result.message
                }
            }
        }
    }

    fun openConversation(person: NovaPerson) {
        if (openingUsername != null) return
        scope.launch {
            openingUsername = person.username
            errorMessage = null
            when (val result = messagingRepository.openConversation(person.username)) {
                is ApiResult.Success -> {
                    openingUsername = null
                    onConversationReady(result.value)
                    onDismiss()
                }

                is ApiResult.Failure -> {
                    openingUsername = null
                    if (result.statusCode == 401) {
                        onSessionExpired()
                    } else {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (openingUsername == null) onDismiss() },
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
                    value = query,
                    onValueChange = { query = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Name or @username", color = NovaMuted) },
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
                    isLoading && people.isEmpty() -> {
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

                    errorMessage != null && people.isEmpty() -> {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = NovaMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    }

                    people.isEmpty() -> {
                        Text(
                            text = if (query.isBlank()) {
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
                            items(people, key = { it.id }) { person ->
                                val isOpening = openingUsername == person.username
                                Surface(
                                    onClick = { if (openingUsername == null) openConversation(person) },
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

                if (errorMessage != null && people.isNotEmpty()) {
                    Text(
                        text = errorMessage.orEmpty(),
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
                enabled = openingUsername == null,
            ) {
                Text("Close")
            }
        },
    )
}
