package com.nova.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@Composable
fun NovaPostCard(
    post: NovaPost,
    isDeleting: Boolean,
    onAuthorClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Surface(
                    onClick = onAuthorClick,
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                ) {
                    NovaAvatar(
                        source = post.author.avatarUrl,
                        fallbackText = post.author.name.ifBlank { post.author.username },
                        size = 42.dp,
                    )
                }

                Surface(
                    onClick = onAuthorClick,
                    modifier = Modifier.weight(1f),
                    color = NovaSurface,
                ) {
                    Column {
                        Text(
                            text = post.author.name.ifBlank { post.author.username },
                            color = NovaInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "@${post.author.username} · ${friendlyDate(post.createdAt)}",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }
                }

                if (post.isMine) {
                    Surface(
                        onClick = { if (!isDeleting) onDelete() },
                        shape = RoundedCornerShape(14.dp),
                        color = NovaAccentSoft,
                    ) {
                        Text(
                            text = if (isDeleting) "…" else "Delete",
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            color = NovaAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            NovaMediaImage(
                source = post.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(0.dp)),
                contentDescription = "Post by ${post.author.username}",
            )

            if (post.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = post.caption,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    color = NovaInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun friendlyDate(raw: String): String {
    if (raw.isBlank()) return "now"
    return runCatching {
        val date = OffsetDateTime.parse(raw)
        date.format(DateTimeFormatter.ofPattern("MMM d"))
    }.getOrDefault("now")
}
