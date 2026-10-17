package com.nova.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.ui.components.PlantupiaAvatar
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType

/**
 * The owner's Plantupia identity: human first, then their private living places.
 * Does not use Nova's legacy profile-theme-colored orbit slab. The public social
 * posts/follow graph remain on the same screen, explicitly separated below.
 *
 * This is not used for visiting another person's public PersonScreen.
 */
@Composable
internal fun PlantupiaMeIdentity(
    displayName: String,
    userId: Long = 0L,
    username: String,
    avatarUrl: String,
    bio: String,
    location: String,
    link: String,
    interests: List<String>,
    isVerified: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    val verifiedUrl = remember(link) { normalizedProfileExternalUrl(link) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PlantupiaColors.softSage,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlantupiaAvatar(
                    userId = userId,
                    displayName = displayName.ifBlank { username }.ifBlank { "Plantupia" },
                    photoUrl = avatarUrl,
                    size = 64.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = displayName.ifBlank { username }.ifBlank { "Your profile" },
                            color = NovaInk,
                            style = NovaType.sectionTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isVerified) {
                            NovaIcon(
                                asset = NovaIconAsset.Verified,
                                contentDescription = "Verified account",
                                modifier = Modifier.size(17.dp),
                                tint = PlantupiaColors.forest,
                            )
                        }
                    }
                    Text(
                        text = "@${username}",
                        color = NovaMuted,
                        style = NovaType.bodyCompact,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (bio.isNotBlank()) {
                Text(text = bio, style = NovaType.bodyCompact, color = NovaInk)
            }
            if (location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NovaIcon(
                        asset = NovaIconAsset.Location,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NovaMuted,
                    )
                    Text(text = location, color = NovaMuted, style = NovaType.meta, maxLines = 2)
                }
            }
            if (verifiedUrl != null) {
                androidx.compose.material3.TextButton(
                    onClick = { runCatching { uriHandler.openUri(verifiedUrl) } },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    NovaIcon(
                        asset = NovaIconAsset.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PlantupiaColors.forest,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = link.trim(),
                        style = NovaType.bodyCompact.copy(fontWeight = FontWeight.Medium),
                        color = PlantupiaColors.forest,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (interests.isNotEmpty()) {
                Text(
                    text = interests.joinToString(" · "),
                    color = PlantupiaColors.forest,
                    style = NovaType.micro,
                    maxLines = 2,
                )
            }
        }
    }
}
