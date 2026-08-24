package com.nova.app.feature.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType


@Composable
fun PeopleScreen(
    state: PeopleUiState,
    onQueryChange: (String) -> Unit,
    onPersonClick: (String) -> Unit,
    onFollowToggle: (NovaPerson) -> Unit,
    onLoadMore: () -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    androidx.compose.material3.Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = null,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
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
                .padding(horizontal = NovaSpacing.xl),
        ) {
            Text(
                text = "People",
                color = NovaInk,
                style = NovaType.pageTitle,
                modifier = Modifier.padding(top = NovaSpacing.lg),
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            Text(
                text = "Find someone worth following, then let the connection grow from there.",
                color = NovaMuted,
                style = NovaType.bodyCompact,
            )

            Spacer(modifier = Modifier.height(NovaSpacing.xl))
            NovaTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = "Search",
                placeholder = "Name or @username",
            )
            Spacer(modifier = Modifier.height(NovaSpacing.lg))

            if (state.people.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.query.isBlank()) "Discover" else "Results",
                        color = NovaInk,
                        style = NovaType.label,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
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
                            style = NovaType.micro,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NovaSpacing.md))
            }

            val visibleError = state.pagingError ?: state.followError
            when {
                state.firstPageLoading && state.people.isEmpty() -> {
                    NovaLoadingState(
                        message = "Finding people…",
                        modifier = Modifier.weight(1f),
                    )
                }

                visibleError != null && state.people.isEmpty() -> {
                    NovaEmptyState(
                        title = "Couldn't load people",
                        message = visibleError,
                        modifier = Modifier.weight(1f),
                    )
                }

                state.people.isEmpty() -> {
                    NovaEmptyState(
                        title = if (state.query.isBlank()) "No one to show yet" else "No matches",
                        message = if (state.query.isBlank()) {
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
                        verticalArrangement = Arrangement.spacedBy(NovaSpacing.md),
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

                        if (visibleError != null) {
                            item {
                                Text(
                                    text = visibleError,
                                    color = NovaMuted,
                                    style = NovaType.meta,
                                    modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = NovaSpacing.xs),
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
                        item { Spacer(modifier = Modifier.height(NovaSpacing.lg)) }
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
    NovaCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = NovaBackground,
        borderColor = NovaBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovaSpacing.xs, vertical = NovaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
        ) {
            NovaAvatar(
                source = person.avatarUrl,
                fallbackText = person.name.ifBlank { person.username },
                size = 46.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name.ifBlank { person.username },
                    color = NovaInk,
                    style = NovaType.label,
                    maxLines = 1,
                )
                Text(
                    text = "@${person.username}",
                    color = NovaMuted,
                    style = NovaType.meta,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(NovaSpacing.xs))
                Text(
                    text = buildString {
                        if (privacy.isPrivate) append("Private · ")
                        append("${person.followersCount} ${if (person.followersCount == 1) "follower" else "followers"}")
                    },
                    color = NovaMuted,
                    style = NovaType.micro,
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
                            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
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
                            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }

                else -> {
                    OutlinedButton(
                        onClick = onFollowToggle,
                        enabled = !isUpdating,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.55f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 15.dp,
                            vertical = 7.dp,
                        ),
                    ) {
                        Text(
                            text = if (isUpdating) "…" else "Follow",
                            color = NovaAccent,
                            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }
    }
}
