package com.nova.app.feature.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.NovaPost
import com.nova.app.feature.home.NovaPostCard
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaSecondaryButton
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
    isDeleting: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onLikeToggle: (NovaPost) -> Unit,
    onCommentsClick: (NovaPost) -> Unit,
    onDelete: (NovaPost) -> Unit,
) {
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
                    onAuthorClick = { onAuthorClick(post.author.username) },
                    onLikeToggle = { onLikeToggle(post) },
                    onCommentsClick = { onCommentsClick(post) },
                    onDelete = { onDelete(post) },
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = NovaMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
