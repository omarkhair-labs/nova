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
fun NovaProfileRepostedReelsGrid(
    username: String,
    isOwnProfile: Boolean,
) {
    val context = LocalContext.current
    val repository = context.appContainer.profileReelsRepository
    val scope = rememberCoroutineScope()
    val owner = remember(username, repository, scope) {
        ProfileReelsGridStateOwner(
            username = username,
            source = ProfileReelsSource.Reposted,
            repository = repository,
            scope = scope,
        )
    }
    val state = owner.state
    val reels = state.reels
    val nextCursor = state.nextCursor
    val loading = state.loading
    val loadingMore = state.loadingMore
    val error = state.error

    LaunchedEffect(owner) { owner.loadFirstPage() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Reposted Reels",
            color = NovaInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )

        when {
            loading && reels.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(82.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                }
            }
            error != null && reels.isEmpty() -> {
                Surface(
                    onClick = owner::loadFirstPage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        text = "${error.orEmpty()} · Tap to retry",
                        modifier = Modifier.padding(13.dp),
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }
            }
            reels.isEmpty() -> {
                Text(
                    text = if (isOwnProfile) {
                        "Reels you repost will show up here."
                    } else {
                        "@$username hasn't reposted any Reels yet."
                    },
                    color = NovaMuted,
                    fontSize = 11.sp,
                )
            }
            else -> {
                reels.chunked(3).forEach { rowReels ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        rowReels.forEach { reel ->
                            NovaProfileReelThumbnail(
                                reel = reel,
                                showRepostAuthor = true,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    // Open through the original author's authored-Reels source so
                                    // the viewer can resolve the selected Reel deterministically.
                                    NovaReelsNavigator.openProfile(
                                        context = context,
                                        username = reel.author.username,
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
                    Text(error.orEmpty(), color = NovaMuted, fontSize = 10.sp)
                }
                if (nextCursor != null) {
                    NovaSecondaryButton(
                        text = if (loadingMore) "Loading more…" else "Load more Reposted Reels",
                        onClick = { if (!loadingMore) owner.loadMore() },
                    )
                }
            }
        }
    }
}
