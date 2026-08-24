package com.nova.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.privacy.data.PrivacyRepository
import com.nova.app.feature.privacy.domain.model.NovaNotificationPreferences
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaTheme
import kotlinx.coroutines.launch


class NotificationPreferencesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaTheme {
                NotificationPreferencesScreen(
                    repository = applicationContext.appContainer.privacyRepository,
                    onBack = { finish() },
                    onSessionExpired = {
                        startActivity(
                            Intent(this, MainActivity::class.java).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
                            )
                        )
                        finish()
                    },
                )
            }
        }
    }
}


@Composable
private fun NotificationPreferencesScreen(
    repository: PrivacyRepository,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var preferences by remember { mutableStateOf<NovaNotificationPreferences?>(null) }
    var busyKey by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        when (val result = repository.notificationPreferences()) {
            is ApiResult.Success -> preferences = result.value
            is ApiResult.Failure -> if (result.statusCode == 401) onSessionExpired() else error = result.message
        }
    }

    fun update(key: String, enabled: Boolean) {
        if (busyKey != null) return
        scope.launch {
            busyKey = key
            error = null
            when (val result = repository.updateNotificationPreference(key, enabled)) {
                is ApiResult.Success -> preferences = result.value
                is ApiResult.Failure -> if (result.statusCode == 401) onSessionExpired() else error = result.message
            }
            busyKey = null
        }
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
            title = "Notifications",
            subtitle = "Choose which Nova moments can interrupt you.",
            onBack = onBack,
        )
        Spacer(Modifier.height(22.dp))
        if (preferences == null && error == null) {
            Row(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = NovaAccent)
            }
        } else {
            Text("What's new", color = NovaMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            NotificationToggle("Likes, comments & shares", "People interacting with your content.", "likes_comments_shares", preferences?.likesCommentsShares == true, busyKey, ::update)
            NotificationToggle("Mentions & tags", "When someone mentions you.", "mentions_tags", preferences?.mentionsTags == true, busyKey, ::update)
            NotificationToggle("Followers", "Follow requests and new followers.", "followers", preferences?.followers == true, busyKey, ::update)
            NotificationToggle("Messages", "New messages and reactions.", "messages", preferences?.messages == true, busyKey, ::update)
            Spacer(Modifier.height(18.dp))
            Text("Live & activity", color = NovaMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            NotificationToggle("Live sessions", "Calls and live invitations.", "live_sessions", preferences?.liveSessions == true, busyKey, ::update)
            NotificationToggle("Reels & stories", "Updates from people you follow.", "reels_stories", preferences?.reelsStories == true, busyKey, ::update)
            NotificationToggle("Events & spaces", "Rooms, plans and reminders.", "events_spaces", preferences?.eventsSpaces == true, busyKey, ::update)
            NotificationToggle("Product updates", "Important changes to Nova.", "product_updates", preferences?.productUpdates == true, busyKey, ::update)
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}


@Composable
private fun NotificationToggle(
    title: String,
    subtitle: String,
    key: String,
    checked: Boolean,
    busyKey: String?,
    onChange: (String, Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = NovaInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = NovaMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onChange(key, it) },
                enabled = busyKey == null,
                colors = SwitchDefaults.colors(checkedTrackColor = NovaAccent),
            )
        }
    }
}
