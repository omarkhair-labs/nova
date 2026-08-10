package com.nova.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.NovaPost
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun NovaProfilePostsGrid(
    posts: List<NovaPost>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onPostClick: (NovaPost) -> Unit,
    emptyTitle: String = "No posts yet",
    emptyMessage: String = "Shared moments will show up here.",
    sectionTitle: String? = "Posts",
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!sectionTitle.isNullOrBlank()) {
            Text(
                text = sectionTitle,
                color = NovaInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        when {
            isLoading && posts.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                        Text(
                            text = "Loading posts…",
                            color = NovaMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            errorMessage != null && posts.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Couldn't load posts",
                            color = NovaInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = errorMessage,
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

            posts.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = emptyTitle,
                            color = NovaInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = emptyMessage,
                            color = NovaMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            else -> {
                posts.chunked(3).forEach { rowPosts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        rowPosts.forEach { post ->
                            Surface(
                                onClick = { onPostClick(post) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = NovaSurface,
                            ) {
                                NovaMediaImage(
                                    source = post.imageUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentDescription = "Post by ${post.author.username}",
                                )
                            }
                        }

                        repeat(3 - rowPosts.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
