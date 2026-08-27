package com.nova.app.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaOrbitRing
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import java.net.URI


internal data class NovaProfileThemePalette(
    val accent: Color,
    val soft: Color,
)


internal fun novaProfileThemePalette(theme: String): NovaProfileThemePalette = when (theme.lowercase()) {
    "cyan" -> NovaProfileThemePalette(Color(0xFF287D88), Color(0xFFE7F5F6))
    "orange" -> NovaProfileThemePalette(Color(0xFFAC5A2D), Color(0xFFFFF0E7))
    "pink" -> NovaProfileThemePalette(Color(0xFFA54E78), Color(0xFFFBEAF2))
    "slate" -> NovaProfileThemePalette(Color(0xFF5C6875), Color(0xFFEDF0F3))
    "ink" -> NovaProfileThemePalette(Color(0xFF39343E), Color(0xFFF0EEF1))
    "black" -> NovaProfileThemePalette(Color(0xFF18171C), Color(0xFFECEBED))
    else -> NovaProfileThemePalette(Color(0xFF6554E8), Color(0xFFF0EDFF))
}


internal fun normalizedProfileExternalUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    return runCatching {
        val uri = URI(candidate)
        if (uri.scheme.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            null
        } else {
            uri.toASCIIString()
        }
    }.getOrNull()
}


@Composable
internal fun NovaProfileIdentity(
    displayName: String,
    username: String,
    avatarUrl: String,
    bio: String,
    location: String,
    link: String,
    interests: List<String>,
    profileTheme: String,
    showOrbit: Boolean,
    isVerified: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = remember(profileTheme) { novaProfileThemePalette(profileTheme) }
    val externalUrl = remember(link) { normalizedProfileExternalUrl(link) }
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.soft,
        shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = NovaSpacing.xl, vertical = NovaSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showOrbit) {
                NovaOrbitRing(
                    modifier = Modifier.size(116.dp),
                    color = palette.accent,
                    rings = 2,
                    showLivePoint = false,
                ) {
                    NovaAvatar(
                        source = avatarUrl,
                        fallbackText = displayName.ifBlank { username },
                        size = 88.dp,
                    )
                }
            } else {
                Surface(shape = CircleShape, color = Color.Transparent) {
                    NovaAvatar(
                        source = avatarUrl,
                        fallbackText = displayName.ifBlank { username },
                        size = 88.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(NovaSpacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.xs),
            ) {
                Text(
                    text = displayName.ifBlank { username },
                    color = NovaInk,
                    style = NovaType.sectionTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                if (isVerified) {
                    NovaIcon(
                        asset = NovaIconAsset.Verified,
                        contentDescription = "Verified account",
                        modifier = Modifier.size(18.dp),
                        tint = palette.accent,
                    )
                }
            }
            Text(
                text = "@$username",
                color = NovaMuted,
                style = NovaType.bodyCompact.copy(fontWeight = FontWeight.Medium),
            )

            if (bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                Text(
                    text = bio,
                    color = NovaInk,
                    style = NovaType.bodyCompact,
                    textAlign = TextAlign.Center,
                )
            }

            if (location.isNotBlank()) {
                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                ProfileMetadataRow(
                    icon = NovaIconAsset.Location,
                    text = location,
                    tint = NovaMuted,
                )
            }

            if (link.isNotBlank()) {
                Spacer(modifier = Modifier.height(NovaSpacing.xs))
                ProfileMetadataRow(
                    icon = NovaIconAsset.Link,
                    text = link.trim(),
                    tint = if (externalUrl != null) palette.accent else NovaMuted,
                    modifier = if (externalUrl != null) {
                        Modifier
                            .sizeIn(minHeight = 48.dp)
                            .semantics { role = Role.Button }
                            .clickable { runCatching { uriHandler.openUri(externalUrl) } }
                    } else {
                        Modifier
                    },
                )
            }

            if (interests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                Text(
                    text = interests.joinToString("  ·  "),
                    color = palette.accent,
                    style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}


@Composable
private fun ProfileMetadataRow(
    icon: NovaIconAsset,
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = NovaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovaSpacing.xs),
    ) {
        NovaIcon(
            asset = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint,
        )
        Text(
            text = text,
            color = tint,
            style = NovaType.meta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
