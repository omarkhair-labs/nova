package com.nova.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.feature.reels.ProfileReelsGridStateOwner
import com.nova.app.feature.reels.ProfileReelsSource
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun NovaProfileReelsGrid(
    username: String,
    isOwnProfile: Boolean,
) {
    val context = LocalContext.current
    val repository = context.appContainer.profileReelsRepository
    val scope = rememberCoroutineScope()
    val owner = remember(username, repository, scope) {
        ProfileReelsGridStateOwner(
            username = username,
            source = ProfileReelsSource.Authored,
            repository = repository,
            scope = scope,
        )
    }
    val state = owner.state
    val reels = state.reels
    val nextCursor = state.nextCursor
    val isLoading = state.loading
    val isLoadingMore = state.loadingMore
    val error = state.error

    LaunchedEffect(owner) { owner.loadFirstPage() }

    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            isLoading && reels.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 46.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Loading Reels…",
                        color = NovaMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            reels.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (error != null) "Couldn't load Reels" else "No Reels yet",
                            color = NovaInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = error ?: if (isOwnProfile) {
                                "Reels you share will show up here."
                            } else {
                                "@$username hasn't shared any Reels yet."
                            },
                            color = NovaMuted,
                            fontSize = 12.sp,
                        )
                        if (error != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                onClick = owner::loadFirstPage,
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, NovaBorder),
                            ) {
                                Text(
                                    text = "Try again",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = NovaInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                reels.chunked(3).forEachIndexed { rowIndex, rowReels ->
                    if (rowIndex > 0) Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        rowReels.forEach { reel ->
                            NovaProfileReelThumbnail(
                                reel = reel,
                                showRepostAuthor = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    NovaReelsNavigator.openProfile(
                                        context = context,
                                        username = username,
                                        initialReelId = reel.id,
                                    )
                                },
                            )
                        }
                        repeat(3 - rowReels.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = error.orEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }

                if (nextCursor != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaSecondaryButton(
                        text = if (isLoadingMore) "Loading more…" else "Load more Reels",
                        onClick = { if (!isLoadingMore) owner.loadMore() },
                    )
                }
            }
        }
    }
}
