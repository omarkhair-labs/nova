package com.nova.app.feature.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaErrorState
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaInlineLoading
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType


@Composable
fun SocialConnectionsScreen(
    username: String,
    mode: String,
    state: SocialConnectionsUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onFollowToggle: (NovaPerson) -> Unit,
    onPersonClick: (String) -> Unit,
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
            .padding(horizontal = NovaSpacing.xl, vertical = NovaSpacing.lg),
    ) {
        NovaHeader(
            title = title,
            subtitle = "@$username",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(NovaSpacing.lg))
        NovaTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = "Search",
            placeholder = "Name or @username",
        )
        Spacer(modifier = Modifier.height(NovaSpacing.lg))

        when {
            state.isLoading && state.people.isEmpty() -> {
                NovaLoadingState(
                    message = "Loading $title…",
                    modifier = Modifier.weight(1f),
                )
            }

            state.errorMessage != null && state.people.isEmpty() -> {
                NovaErrorState(
                    title = "Couldn't load $title",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.people.isEmpty() -> {
                NovaEmptyState(
                    title = if (state.query.isBlank()) "No $title yet" else "No matches",
                    message = if (state.query.isBlank()) {
                        if (normalizedMode == MODE_FOLLOWING) {
                            "Accounts followed by @$username will appear here."
                        } else {
                            "People following @$username will appear here."
                        }
                    } else {
                        "Try a different name or username."
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            else -> {
                Text(
                    text = "${state.people.size} loaded",
                    color = NovaMuted,
                    style = NovaType.micro,
                    modifier = Modifier.padding(bottom = NovaSpacing.md),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
                ) {
                    items(state.people, key = { it.id }) { person ->
                        NovaPersonRow(
                            person = person,
                            privacy = state.privacyByUserId[person.id]
                                ?: NovaPersonPrivacyState(false, false, true),
                            isSelf = person.id == state.currentUserId,
                            isUpdating = state.updatingUsername == person.username,
                            onClick = { onPersonClick(person.username) },
                            onFollowToggle = { onFollowToggle(person) },
                            showFollowerCount = false,
                        )
                    }
                    if (state.errorMessage != null) {
                        item {
                            NovaInlineRetry(
                                message = state.errorMessage,
                                onRetry = onRetry,
                            )
                        }
                    } else if (state.nextCursor != null) {
                        item {
                            LaunchedEffect(state.nextCursor) { onLoadMore() }
                            NovaInlineLoading(message = "Loading more $title…")
                        }
                    }
                    item { Spacer(modifier = Modifier.height(NovaSpacing.sm)) }
                }
            }
        }
    }
}
