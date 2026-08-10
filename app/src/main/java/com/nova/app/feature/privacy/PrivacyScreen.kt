package com.nova.app.feature.privacy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.privacy.NovaFollowRequest
import com.nova.app.core.privacy.NovaPrivacyRepository
import com.nova.app.core.privacy.NovaPrivacySummary
import com.nova.app.core.social.NovaSocialPagingRepository
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val privacyRepository = remember(context) { NovaPrivacyRepository(context.applicationContext) }
    val socialRepository = remember(context) { NovaSocialPagingRepository(context.applicationContext) }
    val username = remember(context) {
        NovaSessionStore(context.applicationContext).load()?.cachedUser?.username.orEmpty()
    }
    val scope = rememberCoroutineScope()

    var summary by remember { mutableStateOf<NovaPrivacySummary?>(null) }
    var requests by remember { mutableStateOf<List<NovaFollowRequest>>(emptyList()) }
    var closeFriends by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var followers by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var followerCursor by remember { mutableStateOf<String?>(null) }
    var followerQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var loadingFollowers by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var privacyBusy by remember { mutableStateOf(false) }
    var requestBusyId by remember { mutableStateOf<Long?>(null) }
    var closeFriendBusyId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun handleFailure(result: ApiResult.Failure) {
        if (result.statusCode == 401) onSessionExpired() else error = result.message
    }

    fun loadSummaryBundle() {
        scope.launch {
            loading = true
            error = null
            when (val result = privacyRepository.summary()) {
                is ApiResult.Success -> summary = result.value
                is ApiResult.Failure -> handleFailure(result)
            }
            when (val result = privacyRepository.followRequests()) {
                is ApiResult.Success -> requests = result.value
                is ApiResult.Failure -> handleFailure(result)
            }
            when (val result = privacyRepository.closeFriends()) {
                is ApiResult.Success -> closeFriends = result.value
                is ApiResult.Failure -> handleFailure(result)
            }
            loading = false
        }
    }

    fun loadFollowers(reset: Boolean) {
        if (username.isBlank()) return
        if (!reset && (loadingMore || followerCursor == null)) return
        scope.launch {
            if (reset) loadingFollowers = true else loadingMore = true
            error = null
            when (
                val result = socialRepository.followers(
                    username = username,
                    query = followerQuery.trim(),
                    cursor = if (reset) null else followerCursor,
                )
            ) {
                is ApiResult.Success -> {
                    followers = if (reset) {
                        result.value.people
                    } else {
                        val ids = followers.mapTo(mutableSetOf()) { it.id }
                        followers + result.value.people.filterNot { it.id in ids }
                    }
                    followerCursor = result.value.nextCursor
                }
                is ApiResult.Failure -> handleFailure(result)
            }
            loadingFollowers = false
            loadingMore = false
        }
    }

    fun togglePrivate(enabled: Boolean) {
        if (privacyBusy) return
        scope.launch {
            privacyBusy = true
            error = null
            feedback = null
            when (val result = privacyRepository.setPrivate(enabled)) {
                is ApiResult.Success -> {
                    summary = result.value
                    if (result.value.acceptedPendingRequests > 0) {
                        feedback = "${result.value.acceptedPendingRequests} pending follow requests were accepted."
                        requests = emptyList()
                        loadFollowers(reset = true)
                    } else {
                        feedback = if (enabled) "Your account is now private." else "Your account is now public."
                    }
                }
                is ApiResult.Failure -> handleFailure(result)
            }
            privacyBusy = false
        }
    }

    fun decideFollowRequest(item: NovaFollowRequest, accept: Boolean) {
        if (requestBusyId != null) return
        scope.launch {
            requestBusyId = item.id
            error = null
            val result = if (accept) {
                privacyRepository.acceptFollowRequest(item.id)
            } else {
                privacyRepository.declineFollowRequest(item.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    requests = requests.filterNot { it.id == item.id }
                    summary = summary?.copy(
                        pendingFollowRequests = (summary?.pendingFollowRequests ?: 1).minus(1).coerceAtLeast(0)
                    )
                    feedback = if (accept) {
                        "@${item.requester.username} can now follow you."
                    } else {
                        "Follow request declined."
                    }
                    if (accept) loadFollowers(reset = true)
                }
                is ApiResult.Failure -> handleFailure(result)
            }
            requestBusyId = null
        }
    }

    fun toggleCloseFriend(person: NovaPerson) {
        if (closeFriendBusyId != null) return
        val currentlyClose = closeFriends.any { it.id == person.id }
        scope.launch {
            closeFriendBusyId = person.id
            error = null
            when (val result = privacyRepository.setCloseFriend(person.username, !currentlyClose)) {
                is ApiResult.Success -> {
                    closeFriends = if (currentlyClose) {
                        closeFriends.filterNot { it.id == person.id }
                    } else {
                        closeFriends + person
                    }
                    summary = summary?.copy(closeFriendsCount = closeFriends.size)
                    feedback = if (currentlyClose) {
                        "Removed @${person.username} from Close Friends."
                    } else {
                        "Added @${person.username} to Close Friends."
                    }
                }
                is ApiResult.Failure -> handleFailure(result)
            }
            closeFriendBusyId = null
        }
    }

    LaunchedEffect(Unit) {
        loadSummaryBundle()
        loadFollowers(reset = true)
    }

    LaunchedEffect(followerQuery) {
        delay(280)
        loadFollowers(reset = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        NovaHeader(
            title = "Privacy",
            subtitle = "Control who can follow you and who sees your closer moments.",
            onBack = onBack,
        )

        Spacer(Modifier.height(22.dp))

        if (loading && summary == null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = NovaAccent)
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Private account",
                            color = NovaInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (summary?.isPrivate == true) {
                                "Only people you approve can see your posts, followers and following."
                            } else {
                                "Anyone on Nova can follow you and see your posts."
                            },
                            color = NovaMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    Switch(
                        checked = summary?.isPrivate == true,
                        onCheckedChange = { togglePrivate(it) },
                        enabled = !privacyBusy,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaBackground, checkedTrackColor = NovaAccent),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            SectionTitle(
                title = "Follow requests",
                count = summary?.pendingFollowRequests ?: requests.size,
                subtitle = "People waiting for approval to follow your private account.",
            )
            Spacer(Modifier.height(10.dp))

            if (requests.isEmpty()) {
                EmptyCard("No pending requests", "New requests will appear here when your account is private.")
            } else {
                requests.forEach { item ->
                    PersonDecisionCard(
                        person = item.requester,
                        busy = requestBusyId == item.id,
                        onPrimary = { decideFollowRequest(item, true) },
                        onSecondary = { decideFollowRequest(item, false) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle(
                title = "Close Friends",
                count = closeFriends.size,
                subtitle = "Choose from people who already follow you. Close Friends Stories are only visible to them.",
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = followerQuery,
                onValueChange = { followerQuery = it.take(50) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your followers", color = NovaMuted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaAccent,
                    unfocusedBorderColor = NovaBorder,
                    cursorColor = NovaAccent,
                ),
            )
            Spacer(Modifier.height(10.dp))

            if (loadingFollowers && followers.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                }
            } else if (followers.isEmpty()) {
                EmptyCard(
                    if (followerQuery.isBlank()) "No followers yet" else "No matching followers",
                    if (followerQuery.isBlank()) {
                        "When people follow you, you can add them to Close Friends here."
                    } else {
                        "Try a different name or username."
                    },
                )
            } else {
                followers.forEach { person ->
                    val selected = closeFriends.any { it.id == person.id }
                    CloseFriendRow(
                        person = person,
                        selected = selected,
                        busy = closeFriendBusyId == person.id,
                        onToggle = { toggleCloseFriend(person) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (followerCursor != null) {
                    NovaSecondaryButton(
                        text = if (loadingMore) "Loading more…" else "Load more followers",
                        onClick = { if (!loadingMore) loadFollowers(reset = false) },
                    )
                }
            }
        }

        feedback?.let {
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NovaAccentSoft,
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(13.dp),
                    color = NovaAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(13.dp),
                    color = NovaMuted,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}


@Composable
private fun SectionTitle(title: String, count: Int, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NovaInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = NovaMuted, fontSize = 11.sp, lineHeight = 17.sp)
        }
        Surface(shape = RoundedCornerShape(12.dp), color = NovaAccentSoft) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = NovaAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


@Composable
private fun PersonDecisionCard(
    person: NovaPerson,
    busy: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NovaAvatar(
                    source = person.avatarUrl,
                    fallbackText = person.name.ifBlank { person.username },
                    size = 42.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(person.name.ifBlank { person.username }, color = NovaInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("@${person.username}", color = NovaMuted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { if (!busy) onPrimary() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NovaAccent,
                ) {
                    Text(
                        text = if (busy) "Updating…" else "Accept",
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = NovaBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Surface(
                    onClick = { if (!busy) onSecondary() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        text = "Decline",
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = NovaMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}


@Composable
private fun CloseFriendRow(
    person: NovaPerson,
    selected: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NovaAvatar(
                source = person.avatarUrl,
                fallbackText = person.name.ifBlank { person.username },
                size = 42.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(person.name.ifBlank { person.username }, color = NovaInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("@${person.username}", color = NovaMuted, fontSize = 10.sp)
            }
            Surface(
                onClick = { if (!busy) onToggle() },
                shape = RoundedCornerShape(13.dp),
                color = if (selected) NovaAccentSoft else NovaBackground,
                border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
            ) {
                Text(
                    text = if (busy) "…" else if (selected) "Close friend" else "Add",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    color = if (selected) NovaAccent else NovaInk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}


@Composable
private fun EmptyCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = NovaInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(body, color = NovaMuted, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
        }
    }
}
