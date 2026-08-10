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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.social.NovaSocialPagingRepository
import com.nova.app.core.social.NovaSocialRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun SocialConnectionsScreen(
    username: String,
    mode: String,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val pagingRepository = remember(context) {
        NovaSocialPagingRepository(context.applicationContext)
    }
    val socialRepository = remember(context) {
        NovaSocialRepository(context.applicationContext)
    }
    val currentUserId = remember(context) {
        NovaSessionStore(context.applicationContext).load()?.cachedUser?.id
    }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var people by remember(username, mode) { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var nextCursor by remember(username, mode) { mutableStateOf<String?>(null) }
    var isLoading by remember(username, mode) { mutableStateOf(true) }
    var isLoadingMore by remember(username, mode) { mutableStateOf(false) }
    var updatingUsername by remember(username, mode) { mutableStateOf<String?>(null) }
    var errorMessage by remember(username, mode) { mutableStateOf<String?>(null) }
    var requestVersion by remember(username, mode) { mutableStateOf(0) }

    val normalizedMode = if (mode == MODE_FOLLOWING) MODE_FOLLOWING else MODE_FOLLOWERS
    val title = if (normalizedMode == MODE_FOLLOWING) "Following" else "Followers"

    fun load(reset: Boolean) {
        if (!reset && (isLoadingMore || nextCursor == null)) return
        requestVersion += 1
        val version = requestVersion
        val cursor = if (reset) null else nextCursor
        scope.launch {
            if (reset) isLoading = true else isLoadingMore = true
            errorMessage = null
            val result = if (normalizedMode == MODE_FOLLOWING) {
                pagingRepository.following(username, query, cursor)
            } else {
                pagingRepository.followers(username, query, cursor)
            }
            when (result) {
                is ApiResult.Success -> {
                    if (version == requestVersion) {
                        people = if (reset) {
                            result.value.people
                        } else {
                            val existingIds = people.mapTo(mutableSetOf()) { it.id }
                            people + result.value.people.filterNot { it.id in existingIds }
                        }
                        nextCursor = result.value.nextCursor
                        isLoading = false
                        isLoadingMore = false
                    }
                }
                is ApiResult.Failure -> {
                    if (version == requestVersion) {
                        isLoading = false
                        isLoadingMore = false
                        if (result.statusCode == 401) {
                            onSessionExpired()
                        } else {
                            errorMessage = result.message
                        }
                    }
                }
            }
        }
    }

    fun toggleFollow(person: NovaPerson) {
        if (updatingUsername != null || person.id == currentUserId) return
        scope.launch {
            updatingUsername = person.username
            errorMessage = null
            when (
                val result = socialRepository.setFollowing(
                    username = person.username,
                    follow = !person.isFollowing,
                )
            ) {
                is ApiResult.Success -> {
                    people = people.map { existing ->
                        if (existing.id == result.value.id) result.value else existing
                    }
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired()
                    else errorMessage = result.message
                }
            }
            updatingUsername = null
        }
    }

    LaunchedEffect(username, normalizedMode, query) {
        delay(240)
        load(reset = true)
    }

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
            value = query,
            onValueChange = { query = it.take(40) },
            label = "Search",
            placeholder = "Name or @username",
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading && people.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                }
            }

            errorMessage != null && people.isEmpty() -> {
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
                            text = errorMessage.orEmpty(),
                            color = NovaMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        NovaSecondaryButton(
                            text = "Try again",
                            onClick = { load(reset = true) },
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }

            people.isEmpty() -> {
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
                            text = if (query.isBlank()) "No $title yet" else "No matches",
                            color = NovaInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (query.isBlank()) {
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
                    text = "${people.size} loaded",
                    color = NovaMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(people, key = { it.id }) { person ->
                        ConnectionRow(
                            person = person,
                            isSelf = person.id == currentUserId,
                            isUpdating = updatingUsername == person.username,
                            onFollowToggle = { toggleFollow(person) },
                        )
                    }
                    if (errorMessage != null) {
                        item {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = NovaMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                        }
                    }
                    if (nextCursor != null) {
                        item {
                            NovaSecondaryButton(
                                text = if (isLoadingMore) "Loading more…" else "Load more",
                                onClick = { if (!isLoadingMore) load(reset = false) },
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
                    text = "@${person.username}",
                    color = NovaMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            if (!isSelf) {
                if (person.isFollowing) {
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
                } else {
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

const val MODE_FOLLOWERS = "followers"
const val MODE_FOLLOWING = "following"
