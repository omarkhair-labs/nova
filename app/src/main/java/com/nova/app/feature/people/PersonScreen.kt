package com.nova.app.feature.people

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.SocialGraphActivity
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.network.NovaPost
import com.nova.app.core.privacy.NovaPersonPrivacyState
import com.nova.app.core.privacy.NovaPrivacyRepository
import com.nova.app.core.social.NovaSocialRepository
import com.nova.app.feature.sharing.NovaShareDialog
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaPagedProfilePostsGrid
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.launch


private data class ReportReason(val value: String, val label: String)

private val reportReasons = listOf(
    ReportReason("spam", "Spam"),
    ReportReason("harassment", "Harassment"),
    ReportReason("impersonation", "Impersonation"),
    ReportReason("sexual_content", "Sexual content"),
    ReportReason("violence", "Violence"),
    ReportReason("other", "Other"),
)


@Composable
fun PersonScreen(
    person: NovaPerson?,
    isLoading: Boolean,
    errorMessage: String?,
    profilePosts: List<NovaPost>,
    postsLoading: Boolean,
    postsError: String?,
    onRetryPosts: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
    onBack: () -> Unit,
    onFollowToggle: (NovaPerson) -> Unit,
    onBlocked: (NovaPerson) -> Unit,
) {
    val context = LocalContext.current
    val messagingRepository = remember(context) {
        NovaMessagingRepository(context.applicationContext)
    }
    val socialRepository = remember(context) {
        NovaSocialRepository(context.applicationContext)
    }
    val privacyRepository = remember(context) {
        NovaPrivacyRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var isOpeningMessage by remember(person?.username) { mutableStateOf(false) }
    var messageError by remember(person?.username) { mutableStateOf<String?>(null) }
    var safetyMessage by remember(person?.username) { mutableStateOf<String?>(null) }
    var isSafetyLoading by remember(person?.username) { mutableStateOf(false) }
    var showBlockConfirm by remember(person?.username) { mutableStateOf(false) }
    var showReportDialog by remember(person?.username) { mutableStateOf(false) }
    var showShareProfile by remember(person?.username) { mutableStateOf(false) }
    var reportReason by remember(person?.username) { mutableStateOf("harassment") }
    var reportDetails by remember(person?.username) { mutableStateOf("") }
    var privacyState by remember(person?.username) {
        mutableStateOf(NovaPersonPrivacyState(isPrivate = false, followRequested = false, canViewContent = true))
    }
    var privacyLoading by remember(person?.username) { mutableStateOf(false) }
    var requestCanceling by remember(person?.username) { mutableStateOf(false) }

    LaunchedEffect(person?.username) {
        val selected = person ?: return@LaunchedEffect
        privacyLoading = true
        when (val result = privacyRepository.personState(selected.username)) {
            is ApiResult.Success -> privacyState = result.value
            is ApiResult.Failure -> messageError = result.message
        }
        privacyLoading = false
    }

    fun openMessage(selectedPerson: NovaPerson) {
        if (isOpeningMessage || isSafetyLoading) return
        scope.launch {
            isOpeningMessage = true
            messageError = null
            when (val result = messagingRepository.openConversation(selectedPerson.username)) {
                is ApiResult.Success -> {
                    isOpeningMessage = false
                    NovaMessagingNavigator.openConversation(context, result.value)
                }

                is ApiResult.Failure -> {
                    isOpeningMessage = false
                    messageError = result.message
                }
            }
        }
    }

    fun openSocialGraph(selectedPerson: NovaPerson, mode: String) {
        if (!privacyState.canViewContent) return
        context.startActivity(
            Intent(context, SocialGraphActivity::class.java)
                .putExtra(SocialGraphActivity.EXTRA_USERNAME, selectedPerson.username)
                .putExtra(SocialGraphActivity.EXTRA_MODE, mode)
        )
    }

    fun cancelFollowRequest(selectedPerson: NovaPerson) {
        if (requestCanceling || isLoading) return
        scope.launch {
            requestCanceling = true
            messageError = null
            when (val result = socialRepository.setFollowing(selectedPerson.username, false)) {
                is ApiResult.Success -> {
                    privacyState = privacyState.copy(
                        followRequested = false,
                        canViewContent = !privacyState.isPrivate,
                    )
                    safetyMessage = "Follow request canceled."
                }
                is ApiResult.Failure -> messageError = result.message
            }
            requestCanceling = false
        }
    }

    fun toggleFollow(selectedPerson: NovaPerson) {
        if (requestCanceling || isLoading) return
        when {
            privacyState.followRequested && !selectedPerson.isFollowing -> cancelFollowRequest(selectedPerson)
            else -> {
                if (!selectedPerson.isFollowing && privacyState.isPrivate) {
                    privacyState = privacyState.copy(followRequested = true, canViewContent = false)
                    safetyMessage = "Follow request sent."
                } else if (selectedPerson.isFollowing && privacyState.isPrivate) {
                    privacyState = privacyState.copy(followRequested = false, canViewContent = false)
                }
                onFollowToggle(selectedPerson)
            }
        }
    }

    fun blockPerson(selectedPerson: NovaPerson) {
        if (isSafetyLoading) return
        scope.launch {
            isSafetyLoading = true
            messageError = null
            safetyMessage = null
            when (val result = socialRepository.setBlocked(selectedPerson.username, true)) {
                is ApiResult.Success -> {
                    isSafetyLoading = false
                    showBlockConfirm = false
                    onBlocked(selectedPerson)
                }
                is ApiResult.Failure -> {
                    isSafetyLoading = false
                    messageError = result.message
                }
            }
        }
    }

    fun submitReport(selectedPerson: NovaPerson) {
        if (isSafetyLoading) return
        scope.launch {
            isSafetyLoading = true
            messageError = null
            safetyMessage = null
            when (
                val result = socialRepository.report(
                    username = selectedPerson.username,
                    reason = reportReason,
                    details = reportDetails.trim(),
                )
            ) {
                is ApiResult.Success -> {
                    isSafetyLoading = false
                    showReportDialog = false
                    reportDetails = ""
                    safetyMessage = result.value
                }
                is ApiResult.Failure -> {
                    isSafetyLoading = false
                    messageError = result.message
                }
            }
        }
    }

    if (showBlockConfirm && person != null) {
        AlertDialog(
            onDismissRequest = { if (!isSafetyLoading) showBlockConfirm = false },
            title = { Text("Block @${person.username}?") },
            text = {
                Text(
                    "You won't be able to find each other, message, call, or see each other's activity. Following between you will be removed.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { blockPerson(person) },
                    enabled = !isSafetyLoading,
                ) {
                    Text(
                        if (isSafetyLoading) "Blocking…" else "Block",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockConfirm = false },
                    enabled = !isSafetyLoading,
                ) { Text("Cancel") }
            },
        )
    }

    if (showReportDialog && person != null) {
        AlertDialog(
            onDismissRequest = { if (!isSafetyLoading) showReportDialog = false },
            title = { Text("Report @${person.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose the reason that best describes the problem.",
                        color = NovaMuted,
                        fontSize = 12.sp,
                    )
                    reportReasons.forEach { reason ->
                        Surface(
                            onClick = { reportReason = reason.value },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (reportReason == reason.value) NovaAccentSoft else NovaSurface,
                            border = BorderStroke(
                                1.dp,
                                if (reportReason == reason.value) NovaAccent else NovaBorder,
                            ),
                        ) {
                            Text(
                                text = reason.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                color = NovaInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it.take(500) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Optional details", color = NovaMuted) },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaAccent,
                            unfocusedBorderColor = NovaBorder,
                            cursorColor = NovaAccent,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { submitReport(person) },
                    enabled = !isSafetyLoading,
                ) { Text(if (isSafetyLoading) "Sending…" else "Submit report") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReportDialog = false },
                    enabled = !isSafetyLoading,
                ) { Text("Cancel") }
            },
        )
    }

    if (showShareProfile && person != null) {
        NovaShareDialog(
            title = "Share @${person.username}",
            profileUsername = person.username,
            onDismiss = { showShareProfile = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        NovaHeader(
            title = "Profile",
            subtitle = "A real person on Nova.",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(24.dp))

        when {
            person == null && isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                    Text(
                        text = "Opening profile…",
                        color = NovaMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            person == null -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Couldn't open this profile",
                            color = NovaInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "Try again in a moment.",
                            color = NovaMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NovaAvatar(
                        source = person.avatarUrl,
                        fallbackText = person.name.ifBlank { person.username },
                        size = 112.dp,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = person.name.ifBlank { person.username },
                        color = NovaInk,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@${person.username}",
                        color = NovaMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (shouldShowPrivateProfileBadgeV4(privacyState)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(shape = RoundedCornerShape(14.dp), color = NovaAccentSoft) {
                            Text(
                                text = "🔒 Private account",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                color = NovaAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        SocialStat(
                            value = person.followersCount.toString(),
                            label = "followers",
                            modifier = Modifier.weight(1f),
                            onClick = if (privacyState.canViewContent) {
                                { openSocialGraph(person, MODE_FOLLOWERS) }
                            } else null,
                        )
                        SocialStat(
                            value = person.followingCount.toString(),
                            label = "following",
                            modifier = Modifier.weight(1f),
                            onClick = if (privacyState.canViewContent) {
                                { openSocialGraph(person, MODE_FOLLOWING) }
                            } else null,
                        )
                        SocialStat(
                            value = person.postsCount.toString(),
                            label = "posts",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val visibleError = messageError ?: errorMessage
                if (visibleError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Text(
                            text = visibleError,
                            modifier = Modifier.padding(14.dp),
                            color = NovaMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (!safetyMessage.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = NovaAccentSoft,
                    ) {
                        Text(
                            text = safetyMessage.orEmpty(),
                            modifier = Modifier.padding(14.dp),
                            color = NovaAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NovaSecondaryButton(
                        text = if (isOpeningMessage) "Opening chat…" else "Message",
                        onClick = { if (!isOpeningMessage) openMessage(person) },
                        modifier = Modifier.weight(1f),
                    )
                    NovaSecondaryButton(
                        text = "Share profile",
                        onClick = { showShareProfile = true },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when {
                    person.isFollowing -> {
                        NovaSecondaryButton(
                            text = if (isLoading) "Updating…" else "Following",
                            onClick = { toggleFollow(person) },
                        )
                    }
                    privacyState.followRequested -> {
                        NovaSecondaryButton(
                            text = if (requestCanceling) "Canceling…" else "Requested",
                            onClick = { toggleFollow(person) },
                        )
                    }
                    else -> {
                        NovaPrimaryButton(
                            text = if (isLoading || privacyLoading) "Updating…" else "Follow",
                            onClick = { toggleFollow(person) },
                            enabled = !isLoading && !privacyLoading,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NovaSecondaryButton(
                        text = "Report",
                        onClick = { if (!isSafetyLoading) showReportDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                    NovaSecondaryButton(
                        text = "Block",
                        onClick = { if (!isSafetyLoading) showBlockConfirm = true },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (privacyState.canViewContent) {
                    NovaPagedProfilePostsGrid(
                        username = person.username,
                        initialPosts = profilePosts,
                        isLoading = postsLoading,
                        errorMessage = postsError,
                        onRetry = onRetryPosts,
                        onPostClick = onPostClick,
                        emptyTitle = "No posts yet",
                        emptyMessage = "When @${person.username} shares something, it will appear here.",
                    )
                } else {
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
                            Text("🔒", fontSize = 28.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "This account is private",
                                color = NovaInk,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = if (privacyState.followRequested) {
                                    "Your follow request is waiting for approval."
                                } else {
                                    "Follow @${person.username} and wait for approval to see posts and connections."
                                },
                                color = NovaMuted,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SocialStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = NovaInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = NovaMuted,
            fontSize = 12.sp,
        )
    }
}
