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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.privacy.NovaPersonPrivacyState
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun PeopleScreen(
    state: PeopleUiState,
    onQueryChange: (String) -> Unit,
    onPersonClick: (String) -> Unit,
    onFollowToggle: (NovaPerson) -> Unit,
    onLoadMore: () -> Unit,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    androidx.compose.material3.Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.People,
                onHomeClick = onHomeClick,
                onPeopleClick = {},
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground)
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "People",
                color = NovaInk,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Find someone worth following, then let the connection grow from there.",
                color = NovaMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(18.dp))

            NovaTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = "Search",
                placeholder = "Name or @username",
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.people.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.query.isBlank()) "Discover" else "Results",
                        color = NovaInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.firstPageLoading && state.people.isNotEmpty()) {
                            CircularProgressIndicator(
                                color = NovaAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(14.dp),
                            )
                        }
                        Text(
                            text = "${state.people.size} loaded",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            when {
                state.firstPageLoading && state.people.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NovaAccent)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Finding people…",
                                color = NovaMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                state.pagingError != null && state.people.isEmpty() -> {
                    EmptyPeopleCard(
                        title = "Couldn't load people",
                        subtitle = state.pagingError,
                        modifier = Modifier.weight(1f),
                    )
                }

                state.people.isEmpty() -> {
                    EmptyPeopleCard(
                        title = if (state.query.isBlank()) "No one to show yet" else "No matches",
                        subtitle = if (state.query.isBlank()) {
                            "Your discovery space will grow as more people join Nova."
                        } else {
                            "Try a different name or username."
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.people, key = { it.id }) { person ->
                            val privacy = state.privacyByUserId[person.id]
                                ?: NovaPersonPrivacyState(false, false, true)
                            PersonRow(
                                person = person,
                                privacy = privacy,
                                isUpdating = state.followingUsername == person.username ||
                                    state.cancelingUsername == person.username,
                                onClick = { onPersonClick(person.username) },
                                onFollowToggle = { onFollowToggle(person) },
                            )
                        }

                        if (state.pagingError != null) {
                            item {
                                Text(
                                    text = state.pagingError,
                                    color = NovaMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                )
                            }
                        }

                        if (state.nextCursor != null) {
                            item {
                                NovaSecondaryButton(
                                    text = if (state.loadingMore) "Loading more…" else "Load more people",
                                    onClick = { if (!state.loadingMore) onLoadMore() },
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(14.dp)) }
                    }
                }
            }
        }
    }
}


@Composable
private fun PersonRow(
    person: NovaPerson,
    privacy: NovaPersonPrivacyState,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onFollowToggle: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = NovaSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NovaAvatar(
                source = person.avatarUrl,
                fallbackText = person.name.ifBlank { person.username },
                size = 52.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name.ifBlank { person.username },
                    color = NovaInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "@${person.username}",
                    color = NovaMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        if (privacy.isPrivate) append("🔒 Private · ")
                        append("${person.followersCount} ${if (person.followersCount == 1) "follower" else "followers"}")
                    },
                    color = NovaMuted,
                    fontSize = 11.sp,
                )
            }

            when {
                person.isFollowing -> {
                    OutlinedButton(
                        onClick = onFollowToggle,
                        enabled = !isUpdating,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NovaBorder),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 13.dp,
                            vertical = 7.dp,
                        ),
                    ) {
                        Text(
                            text = if (isUpdating) "…" else "Following",
                            color = NovaInk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                privacy.followRequested -> {
                    OutlinedButton(
                        onClick = onFollowToggle,
                        enabled = !isUpdating,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NovaBorder),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 13.dp,
                            vertical = 7.dp,
                        ),
                    ) {
                        Text(
                            text = if (isUpdating) "…" else "Requested",
                            color = NovaMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                else -> {
                    Button(
                        onClick = onFollowToggle,
                        enabled = !isUpdating,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NovaAccent,
                            contentColor = Color.White,
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 15.dp,
                            vertical = 7.dp,
                        ),
                    ) {
                        Text(
                            text = if (isUpdating) "…" else "Follow",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun EmptyPeopleCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NovaSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "◎",
                    color = NovaAccent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    color = NovaInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = NovaMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
