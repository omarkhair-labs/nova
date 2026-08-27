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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
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
    onFilterChange: (String) -> Unit,
    onPersonClick: (String) -> Unit,
    onFollowToggle: (NovaPerson) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val context = LocalContext.current
    val recentPreferences = remember(context) {
        context.getSharedPreferences("nova_people_recent", android.content.Context.MODE_PRIVATE)
    }
    var recentUsernames by remember {
        mutableStateOf(recentPreferences.getString("usernames", "").orEmpty().split('|').filter(String::isNotBlank))
    }

    fun recordAndOpen(username: String) {
        recentUsernames = (listOf(username) + recentUsernames.filterNot { it == username }).take(6)
        recentPreferences.edit().putString("usernames", recentUsernames.joinToString("|")).apply()
        onPersonClick(username)
    }
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
            val recentPeople = recentUsernames.map { username ->
                username to state.people.firstOrNull { it.username == username }
            }
            if (state.query.isBlank() && recentPeople.isNotEmpty()) {
                Spacer(modifier = Modifier.height(NovaSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent searches", color = NovaInk, style = NovaType.label)
                    TextButton(
                        onClick = {
                            recentUsernames = emptyList()
                            recentPreferences.edit().remove("usernames").apply()
                        },
                    ) {
                        Text("Clear", color = NovaMuted, style = NovaType.micro)
                    }
                }
                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
                ) {
                    recentPeople.forEach { (username, person) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                onClick = { recordAndOpen(username) },
                                shape = RoundedCornerShape(999.dp),
                                color = NovaBackground,
                            ) {
                                NovaAvatar(
                                    source = person?.avatarUrl.orEmpty(),
                                    fallbackText = person?.name?.ifBlank { username } ?: username,
                                    size = 46.dp,
                                )
                            }
                            Text(
                                person?.name?.ifBlank { username } ?: "@$username",
                                color = NovaInk,
                                style = NovaType.micro,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(NovaSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
            ) {
                listOf(
                    "people" to "People",
                    "nearby" to "Nearby",
                    "interests" to "Interests",
                    "verified" to "Verified",
                    "new" to "New",
                ).forEach { (value, label) ->
                    Surface(
                        onClick = { onFilterChange(value) },
                        shape = RoundedCornerShape(999.dp),
                        color = if (state.filter == value) NovaAccent else NovaBackground,
                        border = BorderStroke(1.dp, if (state.filter == value) NovaAccent else NovaBorder),
                    ) {
                        Text(
                            text = label,
                            color = if (state.filter == value) androidx.compose.ui.graphics.Color.White else NovaInk,
                            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            Text(
                text = discoveryFilterDescription(state.filter),
                color = NovaMuted,
                style = NovaType.micro,
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
                        actionLabel = "Try again",
                        onAction = onRetry,
                    )
                }

                state.people.isEmpty() -> {
                    NovaEmptyState(
                        title = if (state.query.isBlank()) "No one to show yet" else "No matches",
                        message = peopleEmptyMessage(state.filter, state.query),
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
                            NovaPersonRow(
                                person = person,
                                privacy = privacy,
                                isUpdating = state.followingUsername == person.username ||
                                    state.cancelingUsername == person.username,
                                onClick = { recordAndOpen(person.username) },
                                onFollowToggle = { onFollowToggle(person) },
                            )
                        }

                        if (state.followError != null) {
                            item {
                                Text(
                                    text = state.followError,
                                    color = NovaMuted,
                                    style = NovaType.meta,
                                    modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = NovaSpacing.xs),
                                )
                            }
                        }

                        if (state.pagingError != null && state.people.isNotEmpty()) {
                            item {
                                NovaSecondaryButton(
                                    text = "Retry loading people",
                                    onClick = onRetry,
                                )
                            }
                        } else if (state.nextCursor != null) {
                            item(key = "people-next-${state.nextCursor}") {
                                LaunchedEffect(state.nextCursor) { onLoadMore() }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(NovaSpacing.md),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = NovaAccent,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.height(18.dp),
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(NovaSpacing.lg)) }
                    }
                }
            }
        }
    }
}


internal fun discoveryFilterDescription(filter: String): String = when (filter) {
    "nearby" -> "People who share the location saved on your profile."
    "interests" -> "People with interests that overlap your saved profile interests."
    "verified" -> "Verified Nova accounts."
    "new" -> "Accounts that joined Nova most recently."
    else -> "People you can discover across Nova."
}


internal fun peopleEmptyMessage(filter: String, query: String): String = when {
    query.isNotBlank() -> "Try a different name or username."
    filter == "nearby" -> "Nearby uses the location saved on your profile. Add one or try another filter."
    filter == "interests" -> "Add interests to your profile or try another filter."
    filter == "verified" -> "No verified accounts are available here yet."
    filter == "new" -> "New accounts will appear here as people join Nova."
    else -> "Your discovery space will grow as more people join Nova."
}
