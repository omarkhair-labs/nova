package com.nova.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.ApiResult
import com.nova.app.core.reels.NovaProfileReelsRepository
import com.nova.app.core.reels.NovaReel
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.launch


private val RepostedReelBackground = Color(0xFF090B10)
private val RepostedReelInk = Color(0xFFF6F7FA)
private val RepostedReelMuted = Color(0xFFB8BDC8)


@Composable
fun NovaProfileRepostedReelsGrid(
    username: String,
    isOwnProfile: Boolean,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaProfileReelsRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var reels by remember(username) { mutableStateOf<List<NovaReel>>(emptyList()) }
    var nextCursor by remember(username) { mutableStateOf<String?>(null) }
    var loading by remember(username) { mutableStateOf(true) }
    var loadingMore by remember(username) { mutableStateOf(false) }
    var error by remember(username) { mutableStateOf<String?>(null) }

    suspend fun loadFirstPage() {
        loading = true
        error = null
        when (val result = repository.repostedReels(username)) {
            is ApiResult.Success -> {
                reels = result.value.reels
                nextCursor = result.value.nextCursor
            }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (loadingMore || username.isBlank()) return
        scope.launch {
            loadingMore = true
            error = null
            when (val result = repository.repostedReels(username, cursor)) {
                is ApiResult.Success -> {
                    val ids = reels.mapTo(mutableSetOf()) { it.id }
                    reels = reels + result.value.reels.filterNot { it.id in ids }
                    nextCursor = result.value.nextCursor
                }
                is ApiResult.Failure -> error = result.message
            }
            loadingMore = false
        }
    }

    LaunchedEffect(username) {
        if (username.isBlank()) loading = false else loadFirstPage()
    }

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
                    onClick = { scope.launch { loadFirstPage() } },
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
                            RepostedReelCard(
                                reel = reel,
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
                        onClick = { if (!loadingMore) loadMore() },
                    )
                }
            }
        }
    }
}


@Composable
private fun RepostedReelCard(
    reel: NovaReel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.72f),
        shape = RoundedCornerShape(7.dp),
        color = RepostedReelBackground,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(RepostedReelBackground),
        ) {
            Text(
                text = "▶",
                modifier = Modifier.align(Alignment.Center),
                color = RepostedReelInk.copy(alpha = 0.88f),
                fontSize = 22.sp,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.52f))
                    .padding(horizontal = 7.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "↻ @${reel.author.username}",
                    color = RepostedReelInk,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (reel.caption.isNotBlank()) {
                    Text(
                        text = reel.caption,
                        color = RepostedReelMuted,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
