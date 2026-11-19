package com.nova.app.feature.messages

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nova.app.app.appContainer
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.feature.messages.inbox.InboxViewModel
import com.nova.app.feature.messages.inbox.InboxFilter
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaEvidencePhoto
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.plants.PlantupiaTeamSheet
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.PlantupiaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaErrorState
import com.nova.app.ui.components.NovaInlineLoading
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.flow.distinctUntilChanged


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onConversationClick: (NovaConversation) -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUnreadCountChanged: (Int) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val inboxViewModel: InboxViewModel = viewModel(
        factory = InboxViewModel.factory(context.appContainer.inboxRepository),
    )
    val state = inboxViewModel.state
    val query = state.query
    val conversations = state.conversations
    val unreadCount = state.unreadCount
    val nextCursor = state.nextCursor
    val isLoading = state.isLoading
    val isLoadingMore = state.isLoadingMore
    val errorMessage = state.errorMessage
    val inboxRefreshVersion = NovaMessagesSignal.inboxRefreshVersion
    val initialInboxRefreshVersion = remember { inboxRefreshVersion }
    val listState = rememberLazyListState()
    val plantRepository = remember(context) { PlantupiaRepository(context.applicationContext) }
    var teamPlants by remember { mutableStateOf<List<PlantupiaPlant>>(emptyList()) }
    var teamPlantsLoading by remember { mutableStateOf(true) }
    var teamPlantsError by remember { mutableStateOf<String?>(null) }
    var teamPlantsRefresh by remember { mutableIntStateOf(0) }
    var teamPickerOpen by remember { mutableStateOf(false) }
    var selectedTeamPlant by remember { mutableStateOf<PlantupiaPlant?>(null) }

    LaunchedEffect(plantRepository, teamPlantsRefresh) {
        teamPlantsLoading = true
        teamPlantsError = null
        when (val result = plantRepository.plants()) {
            is ApiResult.Success -> if (plantRepository.isCurrentSession()) {
                teamPlants = result.value.filter { it.active }.sortedBy { it.name.lowercase() }
            }
            is ApiResult.Failure -> if (plantRepository.isCurrentSession()) {
                teamPlants = emptyList()
                teamPlantsError = result.message
            }
        }
        teamPlantsLoading = false
    }

    LaunchedEffect(inboxRefreshVersion) {
        if (inboxRefreshVersion != initialInboxRefreshVersion) {
            inboxViewModel.refresh()
        }
    }

    LaunchedEffect(state.unreadUpdateVersion) {
        if (state.unreadUpdateVersion > 0) {
            onUnreadCountChanged(state.unreadCount)
        }
    }

    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) {
            onSessionExpired()
        }
    }

    LaunchedEffect(listState, nextCursor, isLoading, isLoadingMore, conversations.size) {
        snapshotFlow {
            val layout = listState.layoutInfo
            (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalItems) ->
                if (
                    nextCursor != null &&
                    conversations.isNotEmpty() &&
                    !isLoading &&
                    !isLoadingMore &&
                    totalItems > 0 &&
                    lastVisible >= totalItems - 4
                ) {
                    inboxViewModel.loadMore()
                }
            }
    }

    Scaffold(
        containerColor = PlantupiaColors.cream,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Inbox,
                messagesUnreadCount = unreadCount,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
                onInboxClick = {},
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PlantupiaColors.cream)
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
            ) {
                Text("MESSAGES / PRIVATE & COMMUNITY", style = PlantupiaText.sectionLabel)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Conversations",
                    style = PlantupiaText.pageHeading,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = when {
                        unreadCount > 0 -> "$unreadCount unread ${if (unreadCount == 1) "conversation" else "conversations"}. Team support stays private and separate."
                        else -> "Private Plantupia support first. People and groups stay together below."
                    },
                    style = PlantupiaText.supporting,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            PlantupiaTeamEntry(
                plants = teamPlants,
                loading = teamPlantsLoading,
                error = teamPlantsError,
                onOpen = {
                    when (teamPlants.size) {
                        0 -> if (teamPlantsError != null) teamPlantsRefresh += 1
                        1 -> selectedTeamPlant = teamPlants.first()
                        else -> teamPickerOpen = true
                    }
                },
                onRetry = { teamPlantsRefresh += 1 },
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("YOUR CONVERSATIONS", style = PlantupiaText.sectionLabel)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InboxFilter.entries.forEach { filter ->
                    InboxFilterTab(
                        filter = filter,
                        selected = state.filter == filter,
                        onClick = { inboxViewModel.onFilterChanged(filter) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            InboxSearchField(
                value = query,
                onValueChange = inboxViewModel::onQueryChanged,
                onClear = { inboxViewModel.onQueryChanged("") },
            )

            Spacer(modifier = Modifier.height(18.dp))

            when {
                isLoading && conversations.isEmpty() -> {
                    NovaLoadingState(
                        message = "Loading conversations…",
                        modifier = Modifier.weight(1f),
                    )
                }

                errorMessage != null && conversations.isEmpty() -> {
                    NovaErrorState(
                        title = "Couldn't load messages",
                        message = errorMessage.orEmpty(),
                        onRetry = inboxViewModel::retry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 48.dp),
                    )
                }

                conversations.isEmpty() -> {
                    NovaEmptyState(
                        title = when {
                            query.isNotBlank() -> "No matches"
                            state.filter == InboxFilter.Unread -> "You're caught up"
                            state.filter == InboxFilter.Mentions -> "No mentions yet"
                            else -> "No conversations yet"
                        },
                        message = when {
                            query.isNotBlank() -> "Try another name, username, or group name."
                            state.filter == InboxFilter.Unread -> "New unread conversations will appear here."
                            state.filter == InboxFilter.Mentions -> "Conversations that mention your username will appear here."
                            else -> "People you message and groups you join will appear here. Plantupia Team stays separate above."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 48.dp),
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (query.isBlank()) "RECENT" else "RESULTS",
                            color = NovaInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${conversations.size} ${if (conversations.size == 1) "conversation" else "conversations"}",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(conversations, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation) },
                            )
                        }
                        if (errorMessage != null) {
                            item {
                                NovaInlineRetry(
                                    message = errorMessage.orEmpty(),
                                    onRetry = {
                                        if (nextCursor != null) inboxViewModel.loadMore() else inboxViewModel.retry()
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        }
                        if (isLoadingMore) {
                            item {
                                NovaInlineLoading(message = "Loading older conversations…")
                            }
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }

    if (teamPickerOpen) {
        PlantupiaTeamPlantPicker(
            plants = teamPlants,
            onChoose = { plant ->
                teamPickerOpen = false
                selectedTeamPlant = plant
            },
            onDismiss = { teamPickerOpen = false },
        )
    }
    selectedTeamPlant?.let { plant ->
        PlantupiaTeamSheet(
            plant = plant,
            repository = plantRepository,
            onDismiss = { selectedTeamPlant = null },
        )
    }
}



@Composable
private fun PlantupiaTeamEntry(
    plants: List<PlantupiaPlant>,
    loading: Boolean,
    error: String?,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
) {
    val actionable = !loading && (plants.isNotEmpty() || error != null)
    Surface(
        onClick = onOpen,
        enabled = actionable,
        modifier = Modifier.fillMaxWidth(),
        shape = PlantupiaShapes.card,
        color = PlantupiaColors.forest,
        border = BorderStroke(1.dp, PlantupiaColors.forest),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = PlantupiaColors.cream,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NovaIcon(
                            asset = NovaIconAsset.Messages,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = PlantupiaColors.forest,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "PLANTUPIA TEAM",
                        style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.sage),
                    )
                    Text(
                        "Private support for your plants",
                        style = PlantupiaText.editorialHeading.copy(color = PlantupiaColors.cream),
                    )
                }
            }
            Text(
                "Requests, staff replies and the plant context you explicitly choose to share. This is separate from community messages.",
                style = PlantupiaText.supporting.copy(color = PlantupiaColors.cream.copy(alpha = 0.82f)),
            )
            HorizontalDivider(color = PlantupiaColors.cream.copy(alpha = 0.22f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        loading -> "Checking your plants..."
                        error != null -> "Team access couldn't load"
                        plants.isEmpty() -> "Claim or add a plant to start a private request"
                        plants.size == 1 -> "Open support for ${plants.first().name}"
                        else -> "Choose from ${plants.size} plants"
                    },
                    modifier = Modifier.weight(1f),
                    style = PlantupiaText.supporting.copy(color = PlantupiaColors.cream),
                )
                if (error != null) {
                    Surface(onClick = onRetry, color = PlantupiaColors.cream, shape = PlantupiaShapes.badge) {
                        Text("Retry", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = PlantupiaColors.forest, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else if (plants.isNotEmpty()) {
                    Text("Open \u2192", style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.cream))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantupiaTeamPlantPicker(
    plants: List<PlantupiaPlant>,
    onChoose: (PlantupiaPlant) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PlantupiaColors.paper,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("PLANTUPIA TEAM / PRIVATE SUPPORT", style = PlantupiaText.sectionLabel)
            Text("Which plant is this about?", style = PlantupiaText.pageHeading)
            Text(
                "Support stays attached to the plant it belongs to. Private history and photos are not shared unless a support request explicitly includes them.",
                style = PlantupiaText.supporting,
            )
            Spacer(Modifier.height(4.dp))
            plants.forEach { plant ->
                Surface(
                    onClick = { onChoose(plant) },
                    modifier = Modifier.fillMaxWidth(),
                    color = PlantupiaColors.cream,
                    shape = PlantupiaShapes.inset,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PlantupiaEvidencePhoto(
                            photoUrl = plant.coverPhoto,
                            photoDescription = plant.name,
                            modifier = Modifier.size(56.dp),
                            compact = true,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(plant.name, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                            val detail = listOf(plant.species, plant.placement).filter { it.isNotBlank() }.joinToString(" \u00B7 ")
                            if (detail.isNotBlank()) Text(detail, style = PlantupiaText.supporting, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text("Open \u2192", style = PlantupiaText.sectionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxFilterTab(
    filter: InboxFilter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PlantupiaColors.forest else PlantupiaColors.paper,
        border = BorderStroke(1.dp, if (selected) PlantupiaColors.forest else PlantupiaColors.border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = filter.label,
                color = if (selected) PlantupiaColors.cream else PlantupiaColors.forestText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}


@Composable
private fun InboxSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        color = PlantupiaColors.paper,
        border = BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaIcon(
                asset = NovaIconAsset.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PlantupiaColors.secondaryText,
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = PlantupiaColors.forestText,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(PlantupiaColors.forest),
                decorationBox = { innerField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
                            Text(
                                text = "Search people, groups or conversations",
                                color = PlantupiaColors.secondaryText,
                                fontSize = 13.sp,
                            )
                        }
                        innerField()
                    }
                },
            )
            if (value.isNotBlank()) {
                Surface(
                    onClick = onClear,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NovaIcon(
                            asset = NovaIconAsset.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(18.dp),
                            tint = PlantupiaColors.forest,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ConversationRow(
    conversation: NovaConversation,
    onClick: () -> Unit,
) {
    val last = conversation.lastMessage
    val hasUnread = conversation.unreadCount > 0
    val preview = when {
        last == null -> if (conversation.isGroup) "Start the group conversation" else "Start the conversation"
        last.isMine -> "You: ${last.body}"
        conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }}: ${last.body}"
        else -> last.body
    }.ifBlank {
        when {
            last?.imageUrl?.isNotBlank() == true -> when {
                last.isMine -> "You sent a photo"
                conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }} sent a photo"
                else -> "Sent a photo"
            }
            last?.audioUrl?.isNotBlank() == true -> when {
                last.isMine -> "You sent a voice note"
                conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }} sent a voice note"
                else -> "Sent a voice note"
            }
            else -> "Open conversation"
        }
    }
    val avatarSource = if (conversation.isGroup) {
        conversation.membersPreview.firstOrNull()?.avatarUrl.orEmpty()
    } else {
        conversation.otherUser.avatarUrl
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (hasUnread) {
                    "${conversation.unreadCount} unread"
                } else {
                    "Read"
                }
            },
        shape = RoundedCornerShape(0.dp),
        color = if (hasUnread) PlantupiaColors.softSage else PlantupiaColors.cream,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (conversation.isGroup) {
                    NovaAvatar(
                        source = avatarSource,
                        fallbackText = conversation.displayName,
                        size = 50.dp,
                    )
                } else {
                    PlantupiaAvatar(
                        userId = conversation.otherUser.id,
                        displayName = conversation.displayName,
                        photoUrl = conversation.otherUser.avatarUrl,
                        size = 50.dp,
                        chosenVariant = conversation.otherUser.plantupiaAvatarVariant,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = conversation.displayName,
                            color = NovaInk,
                            fontSize = 15.sp,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (conversation.isGroup) {
                            NovaIcon(
                                asset = NovaIconAsset.Group,
                                contentDescription = "Group conversation",
                                modifier = Modifier.size(16.dp),
                                tint = NovaMuted,
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        if (hasUnread) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = PlantupiaColors.forest,
                            ) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    color = PlantupiaColors.cream,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = preview,
                        color = if (hasUnread) NovaInk else NovaMuted,
                        fontSize = 13.sp,
                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (conversation.displaySubtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = conversation.displaySubtitle,
                            color = NovaMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            HorizontalDivider(color = PlantupiaColors.border.copy(alpha = 0.82f))
        }
    }
}
