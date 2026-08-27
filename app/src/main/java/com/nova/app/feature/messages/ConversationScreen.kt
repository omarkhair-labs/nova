package com.nova.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.appearance.ConversationAppearanceViewModel
import com.nova.app.feature.messages.conversation.ConversationContent
import com.nova.app.feature.messages.details.ConversationDetailsDialog
import com.nova.app.feature.messages.details.ConversationDetailsTab
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.LocalNovaColorOverride
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder


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
    val appearanceStoreOwner = remember(conversationId) { ConversationAppearanceStoreOwner() }
    val appearanceViewModel: ConversationAppearanceViewModel = viewModel(
        viewModelStoreOwner = appearanceStoreOwner,
        key = "conversation-appearance-$conversationId",
        factory = ConversationAppearanceViewModel.factory(
            conversationId = conversationId,
            repository = context.appContainer.conversationAppearanceRepository,
            resolveThemeKey = { NovaChatThemes.resolve(it).key },
        ),
    )
    val appearanceState = appearanceViewModel.state

    DisposableEffect(appearanceStoreOwner) {
        onDispose { appearanceStoreOwner.viewModelStore.clear() }
    }

    LaunchedEffect(appearanceState.sessionExpiryVersion) {
        if (appearanceState.sessionExpiryVersion > 0) onSessionExpired()
    }

    var showGroupInfo by remember(conversationId) { mutableStateOf(false) }
    var showDetails by remember(conversationId) { mutableStateOf(false) }
    var detailsStartTab by remember(conversationId) { mutableStateOf(ConversationDetailsTab.Details) }
    var liveDisplayName by remember(conversationId, displayName) { mutableStateOf(displayName) }
    var liveAvatarUrl by remember(conversationId, avatarUrl) { mutableStateOf(avatarUrl) }
    var liveMembersCount by remember(conversationId, membersCount) { mutableIntStateOf(membersCount) }
    val identityEndPadding = if (username == "group") 70.dp else 124.dp

    val palette = NovaChatThemes.resolve(appearanceState.themeKey)
    CompositionLocalProvider(
        LocalNovaColorOverride provides palette.colorOverride(),
        LocalNovaChatPalette provides palette,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground),
        ) {
            ConversationContent(
                conversationId = conversationId,
                username = username,
                displayName = liveDisplayName,
                avatarUrl = liveAvatarUrl,
                onBack = onBack,
                onConversationRead = onConversationRead,
                onSessionExpired = onSessionExpired,
            )

            Surface(
                onClick = {
                    detailsStartTab = ConversationDetailsTab.Details
                    showDetails = true
                },
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 58.dp, end = identityEndPadding)
                    .height(52.dp),
            ) {
                Box(Modifier.fillMaxSize())
            }

            if (isGroup) {
                ConversationCallAction(
                    icon = NovaIconAsset.Info,
                    contentDescription = "Open group info",
                    onClick = { showGroupInfo = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 12.dp),
                )
            } else {
                ConversationCallAction(
                    icon = NovaIconAsset.CallAudio,
                    contentDescription = "Start voice call",
                    onClick = onAudioCall,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 61.dp),
                )
                ConversationCallAction(
                    icon = NovaIconAsset.CallVideo,
                    contentDescription = "Start video call",
                    onClick = onVideoCall,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 12.dp),
                )
            }
        }

        if (showDetails) {
            ConversationDetailsDialog(
                conversationId = conversationId,
                username = username,
                displayName = liveDisplayName,
                avatarUrl = liveAvatarUrl,
                initialTab = detailsStartTab,
                themeLabel = palette.label,
                onOpenTheme = appearanceViewModel::openPicker,
                onDismiss = { showDetails = false },
                onSessionExpired = onSessionExpired,
            )
        }

        if (appearanceState.pickerOpen) {
            NovaChatThemePicker(
                selectedKey = appearanceState.themeKey,
                savingKey = appearanceState.savingThemeKey,
                errorMessage = appearanceState.errorMessage,
                onSelect = { appearanceViewModel.selectTheme(it.key) },
                onDismiss = appearanceViewModel::dismissPicker,
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


private class ConversationAppearanceStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}


@Composable
private fun ConversationCallAction(
    icon: NovaIconAsset,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = NovaBackground,
        border = BorderStroke(1.dp, NovaBorder),
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            NovaIcon(
                asset = icon,
                contentDescription = contentDescription,
                tint = NovaAccent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
