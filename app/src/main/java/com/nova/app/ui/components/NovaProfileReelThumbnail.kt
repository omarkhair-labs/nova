package com.nova.app.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.reels.domain.model.NovaReel
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset


private val ReelThumbnailBackground = Color(0xFF090B10)
private val ReelThumbnailInk = Color(0xFFF6F7FA)
private val ReelThumbnailMuted = Color(0xFFB8BDC8)


/** Shared truthful Reel tile for authored and reposted profile grids. */
@Composable
internal fun NovaProfileReelThumbnail(
    reel: NovaReel,
    showRepostAuthor: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.72f),
        shape = RoundedCornerShape(7.dp),
        color = ReelThumbnailBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(ReelThumbnailBackground)) {
            if (reel.thumbnailUrl.isNotBlank()) {
                NovaMediaImage(
                    source = reel.thumbnailUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "Reel preview by ${reel.author.displayName}",
                    failureLabel = "Preview unavailable",
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.Center).size(40.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.52f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NovaIcon(
                        asset = NovaIconAsset.Play,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 7.dp, vertical = 6.dp),
            ) {
                if (showRepostAuthor) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NovaIcon(
                            asset = NovaIconAsset.Repost,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ReelThumbnailInk,
                        )
                        Text(
                            text = "@${reel.author.username}",
                            color = ReelThumbnailInk,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else if (reel.caption.isNotBlank()) {
                    Text(
                        text = reel.caption,
                        color = ReelThumbnailInk,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    NovaIcon(
                        asset = NovaIconAsset.Like,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = ReelThumbnailMuted,
                    )
                    Text(compactSocialCount(reel.likesCount), color = ReelThumbnailMuted, fontSize = 9.sp)
                    Spacer(modifier = Modifier.size(3.dp))
                    NovaIcon(
                        asset = NovaIconAsset.Comment,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = ReelThumbnailMuted,
                    )
                    Text(compactSocialCount(reel.commentsCount), color = ReelThumbnailMuted, fontSize = 9.sp)
                }
            }
        }
    }
}
