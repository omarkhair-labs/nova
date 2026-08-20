package com.nova.app.feature.people

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun SocialConnectionsScreen(
    username: String,
    mode: String,
    state: SocialConnectionsUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onFollowToggle: (NovaPerson) -> Unit,
    onBack: () -> Unit,
) {
    val normalizedMode = if (mode == MODE_FOLLOWING) MODE_FOLLOWING else MODE_FOLLOWERS
    val title = if (normalizedMode == MODE_FOLLOWING) "Following" else "Followers"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        NovaHeader(
            title = title,
            subtitle = "@$username",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(18.dp))
        NovaTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = "Search",
            placeholder = "Name or @username",
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading && state.people.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                }
            }

            state.errorMessage != null && state.people.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Couldn't load $title",
                            color = NovaInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.errorMessage,
                            color = NovaMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        NovaSecondaryButton(
                            text = "Try again",
                            onClick = onRetry,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }

            state.people.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (state.query.isBlank()) "No $title yet" else "No matches",
                            color = NovaInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (state.query.isBlank()) {
                                if (normalizedMode == MODE_FOLLOWING) {
                                    "Accounts followed by @$username will appear here."
                                } else {
                                    "People following @$username will appear here."
                                }
                            } else {
                                "Try a different name or username."
                            },
                            color = NovaMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = "${state.people.size} loaded",
                    color = NovaMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(state.people, key = { it.id }) { person ->
                        ConnectionRow(
                            person = person,
                            privacy = state.privacyByUserId[person.id]
                                ?: NovaPersonPrivacyState(false, false, true),
                            isSelf = person.id == state.currentUserId,
                            isUpdating = state.updatingUsername == person.username,
                            onFollowToggle = { onFollowToggle(person) },
                        )
                    }
                    if (state.errorMessage != null) {
                        item {
                            Text(
                                text = state.errorMessage,
                                color = NovaMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                        }
                    }
                    if (state.nextCursor != null) {
                        item {
                            NovaSecondaryButton(
                                text = if (state.isLoadingMore) "Loading more…" else "Load more",
                                onClick = { if (!state.isLoadingMore) onLoadMore() },
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}


@Composable
private fun ConnectionRow(
    person: NovaPerson,
    privacy: NovaPersonPrivacyState,
    isSelf: Boolean,
    isUpdating: Boolean,
    onFollowToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NovaAvatar(
                source = person.avatarUrl,
                fallbackText = person.name.ifBlank { person.username },
                size = 50.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name.ifBlank { person.username },
                    color = NovaInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = buildString {
                        append("@${person.username}")
                        if (privacy.isPrivate) append(" · 🔒")
                    },
                    color = NovaMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            if (!isSelf) {
                when {
                    person.isFollowing -> {
                        OutlinedButton(
                            onClick = onFollowToggle,
                            enabled = !isUpdating,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Text(
                                text = if (isUpdating) "…" else "Following",
                                color = NovaInk,
                                fontSize = 10.sp,
                            )
                        }
                    }

                    privacy.followRequested -> {
                        OutlinedButton(
                            onClick = onFollowToggle,
                            enabled = !isUpdating,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Text(
                                text = if (isUpdating) "…" else "Requested",
                                color = NovaMuted,
                                fontSize = 10.sp,
                            )
                        }
                    }

                    else -> {
                        Button(
                            onClick = onFollowToggle,
                            enabled = !isUpdating,
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NovaAccent,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = if (isUpdating) "…" else "Follow",
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
