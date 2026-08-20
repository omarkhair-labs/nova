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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.feature.people.domain.model.NovaPerson
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


@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val appContainer = context.appContainer
    val scope = rememberCoroutineScope()
    val sessionExpiredCallback = rememberUpdatedState(onSessionExpired)
    val owner = remember(appContainer, scope) {
        PrivacyStateOwner(
            username = appContainer.currentCachedUsername(),
            privacyRepository = appContainer.privacyRepository,
            followRequestRepository = appContainer.followRequestRepository,
            peoplePagingRepository = appContainer.peoplePagingRepository,
            scope = scope,
            onSessionExpired = { sessionExpiredCallback.value() },
        )
    }
    val state = owner.state

    LaunchedEffect(owner) {
        owner.start()
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

        if (state.loading && state.summary == null) {
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
                            text = if (state.summary?.isPrivate == true) {
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
                        checked = state.summary?.isPrivate == true,
                        onCheckedChange = owner::togglePrivate,
                        enabled = !state.privacyBusy,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaBackground, checkedTrackColor = NovaAccent),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            SectionTitle(
                title = "Follow requests",
                count = state.summary?.pendingFollowRequests ?: state.requests.size,
                subtitle = "People waiting for approval to follow your private account.",
            )
            Spacer(Modifier.height(10.dp))

            if (state.requests.isEmpty()) {
                EmptyCard("No pending requests", "New requests will appear here when your account is private.")
            } else {
                state.requests.forEach { item ->
                    PersonDecisionCard(
                        person = item.requester,
                        busy = state.requestBusyId == item.id,
                        onPrimary = { owner.decideFollowRequest(item, true) },
                        onSecondary = { owner.decideFollowRequest(item, false) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle(
                title = "Close Friends",
                count = state.closeFriends.size,
                subtitle = "Choose from people who already follow you. Close Friends Stories are only visible to them.",
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.followerQuery,
                onValueChange = owner::setFollowerQuery,
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

            if (state.loadingFollowers && state.followers.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                }
            } else if (state.followers.isEmpty()) {
                EmptyCard(
                    if (state.followerQuery.isBlank()) "No followers yet" else "No matching followers",
                    if (state.followerQuery.isBlank()) {
                        "When people follow you, you can add them to Close Friends here."
                    } else {
                        "Try a different name or username."
                    },
                )
            } else {
                state.followers.forEach { person ->
                    val selected = state.closeFriends.any { it.id == person.id }
                    CloseFriendRow(
                        person = person,
                        selected = selected,
                        busy = state.closeFriendBusyId == person.id,
                        onToggle = { owner.toggleCloseFriend(person) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (state.followerCursor != null) {
                    NovaSecondaryButton(
                        text = if (state.loadingMore) "Loading more…" else "Load more followers",
                        onClick = { if (!state.loadingMore) owner.loadFollowers(reset = false) },
                    )
                }
            }
        }

        state.feedback?.let {
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
        state.error?.let {
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
