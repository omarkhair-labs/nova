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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.social.NovaSocialPagingRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PeopleScreen(
    people: List<NovaPerson>,
    isLoading: Boolean,
    errorMessage: String?,
    followingUsername: String?,
    onSearch: (String) -> Unit,
    onPersonClick: (String) -> Unit,
    onFollowToggle: (NovaPerson) -> Unit,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaSocialPagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var pagedPeople by remember { mutableStateOf<List<NovaPerson>>(people) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var firstPageLoading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var pagingError by remember { mutableStateOf<String?>(null) }
    var requestVersion by remember { mutableStateOf(0) }

    fun loadPage(reset: Boolean, showSpinner: Boolean = true) {
        if (!reset && (loadingMore || nextCursor == null)) return
        requestVersion += 1
        val version = requestVersion
        val cursor = if (reset) null else nextCursor
        scope.launch {
            if (reset) {
                if (showSpinner) firstPageLoading = true
            } else {
                loadingMore = true
            }
            pagingError = null
            when (val result = repository.people(query = query, cursor = cursor)) {
                is ApiResult.Success -> {
                    if (version == requestVersion) {
                        pagedPeople = if (reset) {
                            result.value.people
                        } else {
                            val existingIds = pagedPeople.mapTo(mutableSetOf()) { it.id }
                            pagedPeople + result.value.people.filterNot { it.id in existingIds }
                        }
                        nextCursor = result.value.nextCursor
                        firstPageLoading = false
                        loadingMore = false
                    }
                }
                is ApiResult.Failure -> {
                    if (version == requestVersion) {
                        firstPageLoading = false
                        loadingMore = false
                        pagingError = result.message
                        if (result.statusCode == 401) {
                            // Keep the existing app-level session-expiry path as the source of truth.
                            onSearch(query)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(query) {
        delay(280)
        loadPage(reset = true, showSpinner = pagedPeople.isEmpty())
    }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            loadPage(reset = true, showSpinner = false)
        }
    }

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
                value = query,
                onValueChange = { query = it.take(40) },
                label = "Search",
                placeholder = "Name or @username",
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (pagedPeople.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (query.isBlank()) "Discover" else "Results",
                        color = NovaInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (firstPageLoading && pagedPeople.isNotEmpty()) {
                            CircularProgressIndicator(
                                color = NovaAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(14.dp),
                            )
                        }
                        Text(
                            text = "${pagedPeople.size} loaded",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val visibleError = pagingError ?: errorMessage
            when {
                firstPageLoading && pagedPeople.isEmpty() -> {
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

                visibleError != null && pagedPeople.isEmpty() -> {
                    EmptyPeopleCard(
                        title = "Couldn't load people",
                        subtitle = visibleError,
                        modifier = Modifier.weight(1f),
                    )
                }

                pagedPeople.isEmpty() -> {
                    EmptyPeopleCard(
                        title = if (query.isBlank()) "No one to show yet" else "No matches",
                        subtitle = if (query.isBlank()) {
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
                        items(pagedPeople, key = { it.id }) { person ->
                            PersonRow(
                                person = person,
                                isUpdating = followingUsername == person.username,
                                onClick = { onPersonClick(person.username) },
                                onFollowToggle = {
                                    val wasFollowing = person.isFollowing
                                    pagedPeople = pagedPeople.map { existing ->
                                        if (existing.id == person.id) {
                                            existing.copy(
                                                isFollowing = !wasFollowing,
                                                followersCount = (existing.followersCount + if (wasFollowing) -1 else 1)
                                                    .coerceAtLeast(0),
                                            )
                                        } else {
                                            existing
                                        }
                                    }
                                    onFollowToggle(person)
                                },
                            )
                        }

                        if (visibleError != null) {
                            item {
                                Text(
                                    text = visibleError,
                                    color = NovaMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                )
                            }
                        }

                        if (nextCursor != null) {
                            item {
                                NovaSecondaryButton(
                                    text = if (loadingMore) "Loading more…" else "Load more people",
                                    onClick = { if (!loadingMore) loadPage(reset = false) },
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
                    text = "${person.followersCount} ${if (person.followersCount == 1) "follower" else "followers"}",
                    color = NovaMuted,
                    fontSize = 11.sp,
                )
            }

            if (person.isFollowing) {
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
            } else {
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
