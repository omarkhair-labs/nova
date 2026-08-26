package com.nova.app.feature.sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType


@Composable
fun NovaShareDialog(
    title: String,
    postId: Long? = null,
    profileUsername: String? = null,
    reelId: Long? = null,
    onExternalShare: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    require(listOf(postId != null, profileUsername != null, reelId != null).count { it } == 1) {
        "NovaShareDialog requires exactly one share target."
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val target = remember(postId, profileUsername, reelId) {
        when {
            postId != null -> SharingTarget.Post(postId)
            reelId != null -> SharingTarget.Reel(reelId)
            else -> SharingTarget.Profile(profileUsername.orEmpty())
        }
    }
    val owner = remember(context, target, scope) {
        val container = context.appContainer
        SharingStateOwner(
            target = target,
            messagesRepository = container.messagingRepository,
            peopleRepository = container.peopleRepository,
            sharingRepository = container.sharingRepository,
            scope = scope,
        )
    }
    LaunchedEffect(owner) {
        owner.start()
    }

    val state = owner.state
    val busy = state.busy
    val canAddToStory = owner.canAddToStory
    val addedToFollowersStory = "followers" in state.addedStoryAudiences
    val addedToCloseFriendsStory = "close_friends" in state.addedStoryAudiences

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = NovaBackground,
        contentColor = NovaInk,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = NovaSpacing.sm).size(width = 38.dp, height = 4.dp),
                shape = CircleShape,
                color = NovaBorder,
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = NovaSpacing.xl)
                .padding(bottom = NovaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovaSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = NovaInk, style = NovaType.sectionTitle)
                    Text("Choose where this moment goes.", color = NovaMuted, style = NovaType.meta)
                }
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Done") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
            ) {
                if (canAddToStory) {
                    StoryAudienceAction(
                        title = when {
                            state.addingToStoryAudience == "followers" -> "Adding…"
                            addedToFollowersStory -> "Added ✓"
                            else -> "Your Story"
                        },
                        subtitle = if (addedToFollowersStory) "Story updated" else "Followers",
                        icon = NovaIconAsset.Create,
                        modifier = Modifier.weight(1f),
                        enabled = !busy && !addedToFollowersStory,
                        onClick = { owner.addToStory("followers") },
                    )
                }
                if (onExternalShare != null) {
                    Surface(
                        onClick = {
                            if (!busy) {
                                onDismiss()
                                onExternalShare()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = NovaAccentSoft,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
                        ) {
                            NovaIcon(
                                asset = NovaIconAsset.Share,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = NovaAccent,
                            )
                            Column {
                                Text("Share outside Nova", color = NovaInk, style = NovaType.meta)
                                Text("Use Android's share menu", color = NovaMuted, style = NovaType.micro)
                            }
                        }
                    }
                }
            }

            if (canAddToStory) {
                Surface(
                    onClick = { owner.addToStory("close_friends") },
                    enabled = !busy && !addedToCloseFriendsStory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (addedToCloseFriendsStory) NovaAccentSoft else MaterialTheme.colorScheme.background,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
                    ) {
                        NovaIcon(
                            asset = NovaIconAsset.Orbit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = NovaAccent,
                        )
                        Column {
                            Text(
                                if (state.addingToStoryAudience == "close_friends") {
                                    "Adding to Close Friends Story…"
                                } else if (addedToCloseFriendsStory) {
                                    "Added to Close Friends Story ✓"
                                } else {
                                    "Add to Close Friends Story"
                                },
                                color = NovaInk,
                                style = NovaType.meta,
                            )
                            if (addedToCloseFriendsStory) {
                                Text("Story updated", color = NovaMuted, style = NovaType.micro)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = NovaBorder.copy(alpha = 0.7f))

            OutlinedTextField(
                value = state.query,
                onValueChange = owner::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search chats or people", color = NovaMuted) },
                leadingIcon = {
                    NovaIcon(
                        asset = NovaIconAsset.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = NovaMuted,
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaAccent,
                    unfocusedBorderColor = NovaBorder,
                    cursorColor = NovaAccent,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                ),
            )

            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 360.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                when {
                    state.loadingPeople || state.loadingConversations -> CircularProgressIndicator(
                        modifier = Modifier.padding(top = NovaSpacing.xxl),
                        color = NovaAccent,
                    )

                    state.people.isEmpty() && state.conversations.isEmpty() -> Text(
                        text = if (state.query.isBlank()) {
                            "Your recent conversations will appear here."
                        } else {
                            "No chats or people match that search."
                        },
                        modifier = Modifier.padding(vertical = NovaSpacing.xxl),
                        color = NovaMuted,
                        style = NovaType.bodyCompact,
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(NovaSpacing.xs),
                    ) {
                        if (state.conversations.isNotEmpty()) {
                            item(key = "chats-label") {
                                Text("Recent chats", color = NovaMuted, style = NovaType.micro)
                            }
                            items(state.conversations, key = { "conversation-${it.id}" }) { conversation ->
                                ShareConversationRow(
                                    conversation = conversation,
                                    busy = state.busyConversationId == conversation.id,
                                    sent = conversation.id in state.sentConversationIds,
                                    enabled = !busy,
                                    onSend = { owner.sendToConversation(conversation) },
                                )
                            }
                        }
                        if (state.people.isNotEmpty()) {
                            item(key = "people-label") {
                                Text(
                                    "People",
                                    modifier = Modifier.padding(top = NovaSpacing.sm),
                                    color = NovaMuted,
                                    style = NovaType.micro,
                                )
                            }
                            items(state.people, key = { "person-${it.id}" }) { person ->
                                SharePersonRow(
                                    person = person,
                                    busy = state.busyUsername == person.username,
                                    sent = person.username.lowercase() in state.sentUsernames,
                                    enabled = !busy,
                                    onSend = { owner.sendToPerson(person) },
                                )
                            }
                        }
                    }
                }
            }

            state.message?.takeIf { it.isNotBlank() }?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = NovaAccentSoft,
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(NovaSpacing.md),
                        color = NovaAccent,
                        style = NovaType.meta.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            state.error?.takeIf { it.isNotBlank() }?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(it, modifier = Modifier.weight(1f), color = NovaMuted, style = NovaType.meta)
                        TextButton(onClick = owner::start, enabled = !busy) { Text("Retry") }
                    }
                }
            }
        }
    }
}


@Composable
private fun ShareConversationRow(
    conversation: NovaConversation,
    busy: Boolean,
    sent: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    val avatar = if (conversation.isGroup) {
        conversation.membersPreview.firstOrNull()?.avatarUrl.orEmpty()
    } else {
        conversation.otherUser.avatarUrl
    }
    ShareDestinationRow(
        avatar = avatar,
        fallbackText = conversation.displayName,
        title = conversation.displayName,
        subtitle = conversation.displaySubtitle,
        busy = busy,
        sent = sent,
        enabled = enabled,
        onSend = onSend,
    )
}


@Composable
private fun SharePersonRow(
    person: NovaPerson,
    busy: Boolean,
    sent: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    ShareDestinationRow(
        avatar = person.avatarUrl,
        fallbackText = person.name.ifBlank { person.username },
        title = person.name.ifBlank { person.username },
        subtitle = "@${person.username}",
        busy = busy,
        sent = sent,
        enabled = enabled,
        onSend = onSend,
    )
}


@Composable
private fun ShareDestinationRow(
    avatar: String,
    fallbackText: String,
    title: String,
    subtitle: String,
    busy: Boolean,
    sent: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = NovaSpacing.sm, vertical = NovaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
    ) {
        NovaAvatar(source = avatar, fallbackText = fallbackText, size = 42.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NovaInk, style = NovaType.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = NovaMuted, style = NovaType.micro, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = onSend, enabled = enabled && !sent) {
            Text(
                when {
                    busy -> "Sending…"
                    sent -> "Sent"
                    else -> "Send"
                },
                color = if (sent) NovaMuted else NovaAccent,
            )
        }
    }
}


@Composable
private fun StoryAudienceAction(
    title: String,
    subtitle: String,
    icon: NovaIconAsset,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = NovaAccentSoft,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
        ) {
            NovaIcon(
                asset = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = NovaAccent,
            )
            Column {
                Text(title, color = NovaInk, style = NovaType.meta)
                Text(subtitle, color = NovaMuted, style = NovaType.micro)
            }
        }
    }
}
