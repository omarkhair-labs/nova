package com.nova.app.feature.notifications

import com.nova.app.feature.plants.PlantupiaActionRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextButton
import com.nova.app.feature.plants.PlantupiaCareCalendarOpenSignal
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.feature.plants.PlantupiaSupportPushSignal
import com.nova.app.feature.plants.PlantupiaShopOpenSignal
import com.nova.app.feature.plants.LocalPlantupiaNoticeCountChanged
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.feature.notifications.domain.model.NovaNotification
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaErrorState
import com.nova.app.ui.components.NovaInlineLoading
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.components.NovaPresenceIndicator
import com.nova.app.ui.components.NovaUnreadDot
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Duration
import java.time.Instant


@Composable
fun NotificationsScreen(
    accountId: Long = 0L,
    onBack: () -> Unit,
    onPersonClick: (String) -> Unit,
    onPostClick: (NovaPost) -> Unit,
    onUnreadCountChanged: (Int) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val appContainer = context.appContainer
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val updateBadge = LocalPlantupiaNoticeCountChanged.current
    val unreadCallback = rememberUpdatedState<(Int) -> Unit> { count -> updateBadge(count); onUnreadCountChanged(count) }
    val sessionExpiredCallback = rememberUpdatedState(onSessionExpired)
    val postClickCallback = rememberUpdatedState(onPostClick)
    val owner = remember(appContainer, scope) {
        NotificationsStateOwner(
            notificationsRepository = appContainer.notificationsRepository,
            followRequestRepository = appContainer.followRequestRepository,
            postRepository = appContainer.postDataRepository,
            scope = scope,
            onUnreadCountChanged = { unreadCallback.value(it) },
            onSessionExpired = { sessionExpiredCallback.value() },
            onPostOpened = { postClickCallback.value(it) },
        )
    }
    val state = owner.state
    val category = state.category
    val visibleNotifications = state.notifications.filter {
        when (category) { "Team" -> it.kind == "plant_team"; "Orders" -> it.kind == "plant_order"
            "Circle" -> it.kind !in setOf("plant_team", "plant_order"); "Care" -> false; else -> true }
    }

    LaunchedEffect(owner) {
        owner.start()
    }

    LaunchedEffect(
        category,
        listState,
        state.nextCursor,
        state.isLoading,
        state.isLoadingMore,
        state.notifications.size,
    ) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalItems) ->
                if (category in setOf("All", "Circle") && owner.shouldLoadMore(lastVisible, totalItems)) {
                    owner.loadActivity(reset = false)
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { owner.loadActivity(reset = true) },
        modifier = Modifier
            .fillMaxSize()
            .background(PlantupiaColors.cream)
            .statusBarsPadding(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(PlantupiaColors.cream),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = NovaSpacing.xl,
                end = NovaSpacing.xl,
                top = NovaSpacing.lg,
                bottom = NovaSpacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NovaBackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(NovaSpacing.md))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = NovaSpacing.lg),
                    ) {
                        Text("Activity", style = PlantupiaText.pageHeading)
                    }
                }
            }

            item(key = "notification-categories") {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("All", "Care", "Team", "Orders", "Circle").forEach { name ->
                        ActivityCategoryTab(
                            label = name,
                            selected = category == name,
                            onClick = { owner.selectCategory(name) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (category != "Care") {
                        TextButton(onClick = owner::markAllRead) { Text("Mark read", color = PlantupiaColors.forest) }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { context.startActivity(android.content.Intent(context, com.nova.app.NotificationPreferencesActivity::class.java)) }) {
                        Text("Preferences", color = PlantupiaColors.forest)
                    }
                }
            }
            if (category == "Care") item(key = "care-schedule-entry") {
                PlantupiaActionRow(
                    icon = NovaIconAsset.CareCalendar,
                    title = "Care schedule",
                    detail = "Planned and recorded care",
                    modifier = Modifier.padding(vertical = 8.dp),
                    onClick = { PlantupiaCareCalendarOpenSignal.open(accountId) },
                )
            }
            if (category in setOf("All", "Circle") && state.followRequests.isNotEmpty()) {
                item(key = "follow-requests-title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = NovaSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Follow requests",
                            style = PlantupiaText.editorialHeading,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            color = PlantupiaColors.softSage,
                            shape = PlantupiaShapes.badge,
                        ) {
                            Text(
                                state.followRequests.size.toString(),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = PlantupiaText.sectionLabel,
                            )
                        }
                    }
                }
                items(
                    items = state.followRequests,
                    key = { "follow-request-${it.id}" },
                ) { request ->
                    FollowRequestRow(
                        request = request,
                        busy = state.requestBusyId == request.id,
                        enabled = state.requestBusyId == null,
                        onPersonClick = { onPersonClick(request.requester.username) },
                        onAccept = { owner.decideFollowRequest(request, accept = true) },
                        onDecline = { owner.decideFollowRequest(request, accept = false) },
                    )
                }
                if (!state.requestError.isNullOrBlank()) {
                    item(key = "follow-request-error") {
                        NovaInlineRetry(
                            message = state.requestError.orEmpty(),
                            onRetry = owner::loadFollowRequests,
                        )
                    }
                }
            } else if (category in setOf("All", "Circle") && state.requestsLoading) {
                item(key = "follow-requests-loading") {
                    NovaInlineLoading(message = "Checking follow requests...")
                }
            } else if (category in setOf("All", "Circle") && !state.requestError.isNullOrBlank()) {
                item(key = "follow-requests-error") {
                    NovaInlineRetry(
                        message = state.requestError.orEmpty(),
                        onRetry = owner::loadFollowRequests,
                    )
                }
            }

            if (category != "Care" && state.isLoading && state.notifications.isEmpty()) {
                item {
                    if (state.followRequests.isEmpty()) {
                        NovaLoadingState(
                            message = "Loading activity...",
                            modifier = Modifier.height(220.dp),
                        )
                    } else {
                        NovaInlineLoading(message = "Loading recent activity...")
                    }
                }
            } else if (category != "Care" && state.errorMessage != null && state.notifications.isEmpty()) {
                item {
                    if (state.followRequests.isEmpty()) {
                        NovaErrorState(
                            title = "Couldn't load activity",
                            message = state.errorMessage.orEmpty(),
                            onRetry = { owner.loadActivity(reset = true) },
                        )
                    } else {
                        NovaInlineRetry(
                            message = state.errorMessage.orEmpty(),
                            onRetry = { owner.loadActivity(reset = true) },
                        )
                    }
                }
            } else if (category != "Care" && visibleNotifications.isEmpty() && !state.requestsLoading) {
                item {
                    NovaEmptyState(
                        title = "No new activity",
                        message = "No updates in this category. Your other activity keeps its own unread state.",
                    )
                }
            } else if (visibleNotifications.isNotEmpty()) {
                item {
                    Text(
                        text = activitySectionTitle(category),
                        color = PlantupiaColors.forestText,
                        style = PlantupiaText.editorialHeading,
                        modifier = Modifier.padding(top = NovaSpacing.lg, bottom = NovaSpacing.sm),
                    )
                }

                items(
                    items = visibleNotifications,
                    key = { it.id },
                ) { notification ->
                    NotificationRow(
                        notification = notification,
                        isOpening = state.openingNotificationId == notification.id,
                        onClick = {
                            owner.markRead(notification) {
                            when (val target = owner.openTarget(notification)) {
                                is NotificationOpenTarget.Team -> if (accountId > 0) {
                                    PlantupiaSupportPushSignal.offer(accountId, target.ticketId, target.plantId)
                                    NovaRootNavigationSignal.request(NovaRootTab.Plants)
                                }
                                is NotificationOpenTarget.Order -> if (accountId > 0) PlantupiaShopOpenSignal.open(accountId, target.orderId)
                                NotificationOpenTarget.Unavailable -> Unit
                                is NotificationOpenTarget.Person -> onPersonClick(target.username)
                                is NotificationOpenTarget.Post -> owner.openPost(notification.id, target.postId)
                                is NotificationOpenTarget.Reel -> NovaReelsNavigator.openProfile(
                                    context = context,
                                    username = target.username,
                                    initialReelId = target.reelId,
                                )
                            }
                            }
                        },
                    )
                }

                if (state.isLoadingMore) {
                    item {
                        NovaInlineLoading(message = "Loading more activity...")
                    }
                }

                if (state.errorMessage != null) {
                    item {
                        NovaInlineRetry(
                            message = state.errorMessage.orEmpty(),
                            onRetry = {
                                if (state.nextCursor != null) {
                                    owner.loadActivity(reset = false)
                                } else {
                                    owner.loadActivity(reset = true)
                                }
                            },
                        )
                    }
                }
            }
            if (category in setOf("Team", "Orders") && state.nextCursor != null) item(key = "older-filtered-updates") {
                TextButton(enabled = !state.isLoadingMore, onClick = { owner.loadActivity(reset = false) }) { Text("Load older updates") }
            }
        }
    }
}


@Composable
private fun ActivityCategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = PlantupiaShapes.badge,
        color = if (selected) PlantupiaColors.forest else PlantupiaColors.paper,
        border = BorderStroke(1.dp, if (selected) PlantupiaColors.forest else PlantupiaColors.border),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = PlantupiaText.body.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (selected) PlantupiaColors.cream else PlantupiaColors.forestText,
            ),
        )
    }
}

@Composable
private fun ActivityDestinationCard(
    eyebrow: String,
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        color = PlantupiaColors.paper,
        shape = PlantupiaShapes.card,
        border = BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(eyebrow, style = PlantupiaText.sectionLabel)
            Text(title, style = PlantupiaText.editorialHeading)
            Text(body, style = PlantupiaText.supporting)
            Spacer(Modifier.height(2.dp))
            Text(action, style = PlantupiaText.sectionLabel)
        }
    }
}

private fun activitySectionTitle(category: String): String = when (category) {
    "Team" -> "Team replies"
    "Orders" -> "Order updates"
    "Circle" -> "From your Circle"
    else -> "Recent activity"
}


@Composable
private fun FollowRequestRow(
    request: NovaFollowRequest,
    busy: Boolean,
    enabled: Boolean,
    onPersonClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = NovaSpacing.sm),
        shape = PlantupiaShapes.card,
        color = PlantupiaColors.paper,
        border = BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onPersonClick, shape = RoundedCornerShape(18.dp), color = PlantupiaColors.paper) {
                    NovaAvatar(
                        source = request.requester.avatarUrl,
                        fallbackText = request.requester.name.ifBlank { request.requester.username },
                        size = 46.dp,
                        plantupiaUserId = request.requester.id.takeIf { it > 0L },
                        plantupiaAvatarVariant = request.requester.plantupiaAvatarVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.requester.name.ifBlank { request.requester.username },
                        color = PlantupiaColors.forestText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("@${request.requester.username} wants to follow you", color = PlantupiaColors.secondaryText, fontSize = 11.sp)
                    Text(relativeTime(request.createdAt), color = PlantupiaColors.secondaryText, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = { if (enabled) onDecline() },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = PlantupiaColors.cream,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                ) {
                    Text(
                        text = "Decline",
                        modifier = Modifier.padding(vertical = 9.dp),
                        color = PlantupiaColors.secondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Surface(
                    onClick = { if (enabled) onAccept() },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = PlantupiaColors.forest,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (busy) {
                            NovaPresenceIndicator(
                                modifier = Modifier.size(14.dp),
                                monochrome = true,
                                monochromeColor = PlantupiaColors.cream,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (busy) "Saving..." else "Accept",
                            color = PlantupiaColors.cream,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NotificationRow(
    notification: NovaNotification,
    isOpening: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (notification.isRead) "Read" else "Unread"
            },
        shape = RoundedCornerShape(0.dp),
        color = if (notification.isRead) PlantupiaColors.cream else PlantupiaColors.softSage,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 68.dp)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (notification.kind in setOf("plant_team", "plant_order")) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = PlantupiaColors.paper,
                        border = BorderStroke(1.dp, PlantupiaColors.border),
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            NovaIcon(
                                asset = if (notification.kind == "plant_team") NovaIconAsset.Messages else NovaIconAsset.Shop,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = PlantupiaColors.forest,
                            )
                        }
                    }
                } else {
                    NovaAvatar(
                        source = notification.actor.avatarUrl,
                        fallbackText = notification.actor.name.ifBlank { notification.actor.username },
                        size = 42.dp,
                        plantupiaUserId = notification.actor.id.takeIf { it > 0L },
                        plantupiaAvatarVariant = notification.actor.plantupiaAvatarVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(notificationSourceLabel(notification.kind), style = PlantupiaText.sectionLabel)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notificationText(notification),
                        style = PlantupiaText.body,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = relativeTime(notification.createdAt),
                        style = PlantupiaText.supporting,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (isOpening) {
                    NovaPresenceIndicator(
                        modifier = Modifier.size(18.dp),
                        monochrome = true,
                        monochromeColor = PlantupiaColors.forest,
                    )
                } else {
                    NovaIcon(
                        asset = notificationIcon(notification.kind),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (notification.isRead) PlantupiaColors.secondaryText else PlantupiaColors.forest,
                    )
                }
                if (!notification.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    NovaUnreadDot()
                }
            }
            HorizontalDivider(color = PlantupiaColors.border.copy(alpha = 0.82f))
        }
    }
}


private fun notificationSourceLabel(kind: String): String = when (kind) {
    "plant_team" -> "TEAM / PRIVATE SUPPORT"
    "plant_order" -> "ORDER / STATUS"
    "follow" -> "CIRCLE / PEOPLE"
    "like", "comment", "comment_reply" -> "CIRCLE / SHARED MOMENT"
    "reel_like", "reel_comment", "reel_repost", "reel_reply" -> "CIRCLE / SHARED VIDEO"
    else -> "ACTIVITY"
}


private fun notificationText(notification: NovaNotification) = buildAnnotatedString {
    val name = notification.actor.name.ifBlank { "@${notification.actor.username}" }
    when (notification.kind) {
        "plant_team" -> {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("Plantupia Team") }
            append(" replied to your private support request")
        }
        "plant_order" -> {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("Plantupia") }
            append(" updated your order")
        }
        else -> {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(name) }
            append(" ")
            append(when (notification.kind) {
                "follow" -> "started following you"
                "like" -> "liked your shared moment"
                "comment" -> {
                    val preview = notification.commentPreview.trim()
                    if (preview.isBlank()) "commented on your shared moment"
                    else "commented: \"${preview.take(90)}${if (preview.length > 90) "..." else ""}\""
                }
                "comment_reply" -> "replied to your comment"
                "reel_like" -> "liked your shared video"
                "reel_comment" -> "commented on your shared video"
                "reel_repost" -> "reshared your video"
                "reel_reply" -> "replied to your video comment"
                else -> "interacted with you"
            })
        }
    }
}



private fun notificationIcon(kind: String): NovaIconAsset = when (kind) {
    "like", "reel_like" -> NovaIconAsset.LikeFilled
    "comment", "comment_reply", "reel_comment", "reel_reply" -> NovaIconAsset.Comment
    "reel_repost" -> NovaIconAsset.Repost
    "follow" -> NovaIconAsset.Profile
    else -> NovaIconAsset.Notifications
}


private fun relativeTime(raw: String): String {
    return runCatching {
        val then = Instant.parse(raw)
        val seconds = Duration.between(then, Instant.now()).seconds.coerceAtLeast(0)
        when {
            seconds < 60 -> "Just now"
            seconds < 3_600 -> "${seconds / 60}m"
            seconds < 86_400 -> "${seconds / 3_600}h"
            seconds < 604_800 -> "${seconds / 86_400}d"
            else -> "${seconds / 604_800}w"
        }
    }.getOrDefault("Recently")
}
