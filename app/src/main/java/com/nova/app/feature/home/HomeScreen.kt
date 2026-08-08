package com.nova.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.NovaPost
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun HomeScreen(
    displayName: String,
    username: String,
    avatarUrl: String,
    posts: List<NovaPost>,
    isLoading: Boolean,
    errorMessage: String?,
    deletingPostId: Long?,
    likingPostId: Long?,
    onCreatePost: () -> Unit,
    onRetry: () -> Unit,
    onDeletePost: (NovaPost) -> Unit,
    onLikeToggle: (NovaPost) -> Unit,
    onCommentsClick: (NovaPost) -> Unit,
    onPersonClick: (String) -> Unit,
    onPeopleClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Home,
                onHomeClick = {},
                onPeopleClick = onPeopleClick,
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground)
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "nova",
                            color = NovaInk,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Good to see you, ${displayName.substringBefore(' ')}.",
                            color = NovaMuted,
                            fontSize = 13.sp,
                        )
                    }

                    Surface(
                        onClick = onProfileClick,
                        shape = RoundedCornerShape(24.dp),
                        color = NovaAccentSoft,
                    ) {
                        NovaAvatar(
                            source = avatarUrl,
                            fallbackText = displayName.ifBlank { username },
                            size = 46.dp,
                            modifier = Modifier.padding(2.dp),
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Share a moment",
                            color = NovaInk,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Post a photo from your phone. People who follow you can like it and join the conversation.",
                            color = NovaMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        NovaPrimaryButton(
                            text = "Create post",
                            onClick = onCreatePost,
                        )
                    }
                }
            }

            if (isLoading && posts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading your feed…",
                            color = NovaMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else if (errorMessage != null && posts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Couldn't load your feed",
                                color = NovaInk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = errorMessage,
                                color = NovaMuted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            NovaSecondaryButton(
                                text = "Try again",
                                onClick = onRetry,
                            )
                        }
                    }
                }
            } else if (posts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 34.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = NovaAccentSoft,
                            ) {
                                Text(
                                    text = "✦",
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                    color = NovaAccent,
                                    fontSize = 25.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your feed is ready.",
                                color = NovaInk,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = "Create your first post or follow someone in People to start filling this space.",
                                color = NovaMuted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            NovaSecondaryButton(
                                text = "Find people",
                                onClick = onPeopleClick,
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Your feed",
                        color = NovaInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(
                    items = posts,
                    key = { it.id },
                ) { post ->
                    NovaPostCard(
                        post = post,
                        isDeleting = deletingPostId == post.id,
                        isLiking = likingPostId == post.id,
                        onAuthorClick = {
                            if (post.isMine) {
                                onProfileClick()
                            } else {
                                onPersonClick(post.author.username)
                            }
                        },
                        onLikeToggle = { onLikeToggle(post) },
                        onCommentsClick = { onCommentsClick(post) },
                        onDelete = { onDeletePost(post) },
                    )
                }

                if (errorMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.padding(14.dp),
                                color = NovaMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
