package com.nova.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.core.messaging.NovaConversationPreferenceRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.ui.theme.LocalNovaColorOverride
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import kotlinx.coroutines.launch


/** Stable conversation entry point shared by direct and group messaging. */
@Composable
fun ConversationScreen(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    isGroup: Boolean = false,
    membersCount: Int = 2,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
    onGroupLeft: () -> Unit = onBack,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
) {
    val context = LocalContext.current
    val preferenceRepository = remember(context) {
        NovaConversationPreferenceRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var showGroupInfo by remember(conversationId) { mutableStateOf(false) }
    var showThemePicker by remember(conversationId) { mutableStateOf(false) }
    var liveDisplayName by remember(conversationId, displayName) { mutableStateOf(displayName) }
    var liveAvatarUrl by remember(conversationId, avatarUrl) { mutableStateOf(avatarUrl) }
    var liveMembersCount by remember(conversationId, membersCount) { mutableIntStateOf(membersCount) }
    var themeKey by remember(conversationId) { mutableStateOf("nova") }
    var savingThemeKey by remember(conversationId) { mutableStateOf<String?>(null) }
    var themeError by remember(conversationId) { mutableStateOf<String?>(null) }

    LaunchedEffect(conversationId) {
        when (val result = preferenceRepository.preference(conversationId)) {
            is ApiResult.Success -> {
                themeKey = NovaChatThemes.resolve(result.value.themeKey).key
                themeError = null
            }
            is ApiResult.Failure -> {
                if (result.statusCode == 401) onSessionExpired()
                else themeError = result.message
            }
        }
    }

    fun selectTheme(selected: NovaChatPalette) {
        if (savingThemeKey != null || selected.key == themeKey) return
        val previousKey = themeKey
        themeKey = selected.key
        savingThemeKey = selected.key
        themeError = null
        scope.launch {
            when (val result = preferenceRepository.setTheme(conversationId, selected.key)) {
                is ApiResult.Success -> {
                    themeKey = NovaChatThemes.resolve(result.value.themeKey).key
                    themeError = null
                }
                is ApiResult.Failure -> {
                    themeKey = previousKey
                    if (result.statusCode == 401) onSessionExpired()
                    else themeError = result.message
                }
            }
            savingThemeKey = null
        }
    }

    val palette = NovaChatThemes.resolve(themeKey)
    CompositionLocalProvider(
        LocalNovaColorOverride provides palette.colorOverride(),
        LocalNovaChatPalette provides palette,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground),
        ) {
            ConversationScreenV9(
                conversationId = conversationId,
                username = username,
                displayName = liveDisplayName,
                avatarUrl = liveAvatarUrl,
                toolsEndPadding = if (isGroup) 61.dp else 110.dp,
                themeLabel = palette.label,
                onOpenTheme = { showThemePicker = true },
                onBack = onBack,
                onConversationRead = onConversationRead,
                onSessionExpired = onSessionExpired,
            )

            if (isGroup) {
                ConversationCallAction(
                    icon = Icons.Filled.Info,
                    contentDescription = "Open group info",
                    onClick = { showGroupInfo = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 12.dp),
                )
            } else {
                ConversationCallAction(
                    icon = Icons.Filled.Call,
                    contentDescription = "Start voice call",
                    onClick = onAudioCall,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 61.dp),
                )
                ConversationCallAction(
                    icon = Icons.Filled.Videocam,
                    contentDescription = "Start video call",
                    onClick = onVideoCall,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 12.dp),
                )
            }
        }

        if (showThemePicker) {
            NovaChatThemePicker(
                selectedKey = themeKey,
                savingKey = savingThemeKey,
                errorMessage = themeError,
                onSelect = ::selectTheme,
                onDismiss = { if (savingThemeKey == null) showThemePicker = false },
            )
        }

        if (showGroupInfo && isGroup) {
            GroupInfoDialog(
                conversationId = conversationId,
                onDismiss = { showGroupInfo = false },
                onGroupUpdated = { title, groupAvatarUrl, updatedMembersCount ->
                    liveDisplayName = title
                    liveAvatarUrl = groupAvatarUrl
                    liveMembersCount = updatedMembersCount
                },
                onGroupLeft = {
                    showGroupInfo = false
                    onGroupLeft()
                },
                onSessionExpired = onSessionExpired,
            )
        }
    }
}


@Composable
private fun ConversationCallAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = NovaBackground,
        border = BorderStroke(1.dp, NovaBorder),
        modifier = modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = NovaAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
