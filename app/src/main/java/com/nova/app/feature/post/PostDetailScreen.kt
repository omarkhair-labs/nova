package com.nova.app.feature.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.ui.AspectRatioFrameLayout
import com.nova.app.feature.home.NovaPostCard
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaVideoPlayer
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun PostDetailScreen(
    post: NovaPost?,
    isLoading: Boolean,
    isLiking: Boolean,
    isReposting: Boolean,
    isDeleting: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onLikeToggle: (NovaPost) -> Unit,
    onRepostToggle: (NovaPost) -> Unit,
    onCommentsClick: (NovaPost) -> Unit,
    onDelete: (NovaPost) -> Unit,
) {
    var mediaOpen by remember(post?.id) { mutableStateOf(false) }
    if (mediaOpen && post != null) {
        NovaFullscreenPostMedia(post = post, onDismiss = { mediaOpen = false })
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
            title = "Post",
            subtitle = "A moment on Nova.",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(22.dp))

        when {
            post == null && isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                    Text(
                        text = "Opening post…",
                        color = NovaMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            post == null -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Couldn't open this post",
                            color = NovaInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = errorMessage ?: "It may have been deleted.",
                            color = NovaMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        NovaSecondaryButton(
                            text = "Try again",
                            onClick = onRetry,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }

            else -> {
                NovaPostCard(
                    post = post,
                    isDeleting = isDeleting,
                    isLiking = isLiking,
                    isReposting = isReposting,
                    actionErrorMessage = errorMessage,
                    onAuthorClick = { onAuthorClick(post.author.username) },
                    onReposterClick = onAuthorClick,
                    onOpenPost = { mediaOpen = true },
                    onLikeToggle = { onLikeToggle(post) },
                    onCommentsClick = { onCommentsClick(post) },
                    onRepostToggle = { onRepostToggle(post) },
                    onDelete = { onDelete(post) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
private fun NovaFullscreenPostMedia(
    post: NovaPost,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (post.mediaType == "video") {
                NovaVideoPlayer(
                    source = post.mediaUrl,
                    thumbnailSource = post.thumbnailUrl,
                    modifier = Modifier.fillMaxSize(),
                    autoplay = true,
                    repeat = false,
                    useController = true,
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    description = "Video post by ${post.author.username}",
                )
            } else {
                var scale by remember(post.id) { mutableFloatStateOf(1f) }
                var offsetX by remember(post.id) { mutableFloatStateOf(0f) }
                var offsetY by remember(post.id) { mutableFloatStateOf(0f) }
                NovaMediaImage(
                    source = post.mediaUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(post.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offsetX = if (scale == 1f) 0f else offsetX + pan.x
                                offsetY = if (scale == 1f) 0f else offsetY + pan.y
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        ),
                    contentDescription = "Post by ${post.author.username}",
                    contentScale = ContentScale.Fit,
                )
            }

            Surface(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .minimumInteractiveComponentSize(),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.62f),
            ) {
                Text(
                    text = "Close",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (post.caption.isNotBlank()) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.68f),
                ) {
                    Text(
                        text = post.caption,
                        modifier = Modifier.navigationBarsPadding().padding(18.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}
