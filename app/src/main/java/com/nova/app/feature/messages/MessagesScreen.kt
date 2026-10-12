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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.feature.messages.inbox.InboxViewModel
import com.nova.app.feature.messages.inbox.InboxFilter
import com.nova.app.ui.components.NovaAvatar
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
        containerColor = NovaBackground,
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
                .background(NovaBackground)
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
            ) {
                Text(
                    text = "الرسائل",
                    color = NovaInk,
                    style = com.nova.app.ui.theme.NovaType.pageTitle,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        unreadCount > 0 -> "عندك $unreadCount محادثات غير مقروءة"
                        conversations.isNotEmpty() -> "كمل كلامك من آخر مرة."
                        else -> "محادثاتك الخاصة مع الناس في مكان واحد."
                    },
                    color = NovaMuted,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                        message = "جارٍ تحميل المحادثات…",
                        modifier = Modifier.weight(1f),
                    )
                }

                errorMessage != null && conversations.isEmpty() -> {
                    NovaErrorState(
                        title = "تعذّر تحميل الرسائل",
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
                            query.isNotBlank() -> "مفيش نتائج"
                            state.filter == InboxFilter.Unread -> "مفيش رسائل غير مقروءة"
                            state.filter == InboxFilter.Mentions -> "مفيش إشارات ليك لسه"
                            else -> "مفيش محادثات لسه"
                        },
                        message = when {
                            query.isNotBlank() -> "جرّب اسم شخص أو مجموعة تانية."
                            state.filter == InboxFilter.Unread -> "أول ما توصلك رسالة جديدة هتظهر هنا."
                            state.filter == InboxFilter.Mentions -> "المحادثات اللي حد أشار لك فيها هتظهر هنا."
                            else -> "محادثاتك مع الناس هتظهر هنا أول ما تبدأ الكلام."
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
                            text = if (query.isBlank()) "آخر المحادثات" else "نتائج البحث",
                            color = NovaInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${conversations.size} محادثة",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                NovaInlineLoading(message = "بنحمّل محادثات أقدم…")
                            }
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
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
        color = if (selected) NovaAccent else NovaSurface,
        border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = filter.label,
                color = if (selected) NovaBackground else NovaInk,
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
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaIcon(
                asset = NovaIconAsset.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NovaMuted,
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = NovaInk,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(NovaAccent),
                decorationBox = { innerField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
                            Text(
                                text = "ابحث في المحادثات والمجموعات",
                                color = NovaMuted,
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
                            contentDescription = "امسح البحث",
                            modifier = Modifier.size(18.dp),
                            tint = NovaAccent,
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
        last == null -> if (conversation.isGroup) "ابدأ الكلام في المجموعة" else "ابدأ المحادثة"
        last.isMine -> "أنت: ${last.body}"
        conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }}: ${last.body}"
        else -> last.body
    }.ifBlank {
        when {
            last?.imageUrl?.isNotBlank() == true -> when {
                last.isMine -> "بعتّ صورة"
                conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }} بعت صورة"
                else -> "صورة"
            }
            last?.audioUrl?.isNotBlank() == true -> when {
                last.isMine -> "بعتّ تسجيل صوتي"
                conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }} بعت تسجيل صوتي"
                else -> "تسجيل صوتي"
            }
            else -> "افتح المحادثة"
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
                    "${conversation.unreadCount} غير مقروءة"
                } else {
                    "مقروءة"
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = if (hasUnread) NovaAccentSoft.copy(alpha = 0.70f) else NovaSurface,
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
                NovaAvatar(
                    source = avatarSource,
                    fallbackText = conversation.displayName,
                    size = 50.dp,
                )

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
                                contentDescription = "محادثة جماعية",
                                modifier = Modifier.size(16.dp),
                                tint = NovaMuted,
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        if (hasUnread) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = NovaAccent,
                            ) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    color = NovaBackground,
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
        }
    }
}
