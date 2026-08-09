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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.network.NovaPost
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaProfilePostsGrid
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.launch

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
) {
    val context = LocalContext.current
    val messagingRepository = remember(context) {
        NovaMessagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var isOpeningMessage by remember(person?.username) { mutableStateOf(false) }
    var messageError by remember(person?.username) { mutableStateOf<String?>(null) }

    fun openMessage(selectedPerson: NovaPerson) {
        if (isOpeningMessage) return
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
                        SocialStat(person.followersCount.toString(), "followers")
                        SocialStat(person.followingCount.toString(), "following")
                        SocialStat(person.postsCount.toString(), "posts")
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
                }

                NovaSecondaryButton(
                    text = if (isOpeningMessage) "Opening chat…" else "Message",
                    onClick = { if (!isOpeningMessage) openMessage(person) },
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (person.isFollowing) {
                    NovaSecondaryButton(
                        text = if (isLoading) "Updating…" else "Following",
                        onClick = { if (!isLoading) onFollowToggle(person) },
                    )
                } else {
                    NovaPrimaryButton(
                        text = if (isLoading) "Updating…" else "Follow",
                        onClick = { if (!isLoading) onFollowToggle(person) },
                        enabled = !isLoading,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                NovaProfilePostsGrid(
                    posts = profilePosts,
                    isLoading = postsLoading,
                    errorMessage = postsError,
                    onRetry = onRetryPosts,
                    onPostClick = onPostClick,
                    emptyTitle = "No posts yet",
                    emptyMessage = "When @${person.username} shares something, it will appear here.",
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SocialStat(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
