package com.nova.app.feature.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType


@Composable
internal fun NovaPersonRow(
    person: NovaPerson,
    privacy: NovaPersonPrivacyState,
    isUpdating: Boolean,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isSelf: Boolean = false,
    onClick: (() -> Unit)? = null,
    showFollowerCount: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NovaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .sizeIn(minHeight = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
            ) {
                NovaAvatar(
                    source = person.avatarUrl,
                    fallbackText = person.name.ifBlank { person.username },
                    size = 48.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = person.name.ifBlank { person.username },
                            color = NovaInk,
                            style = NovaType.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (person.isVerified) {
                            NovaIcon(
                                asset = NovaIconAsset.Verified,
                                contentDescription = "Verified account",
                                modifier = Modifier.size(15.dp),
                                tint = NovaAccent,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = "@${person.username}",
                            color = NovaMuted,
                            style = NovaType.meta,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (privacy.isPrivate) {
                            NovaIcon(
                                asset = NovaIconAsset.Lock,
                                contentDescription = "Private account",
                                modifier = Modifier.size(13.dp),
                                tint = NovaMuted,
                            )
                        }
                    }
                    if (showFollowerCount) {
                        Spacer(modifier = Modifier.height(NovaSpacing.xxs))
                        Text(
                            text = "${person.followersCount} ${if (person.followersCount == 1) "follower" else "followers"}",
                            color = NovaMuted,
                            style = NovaType.micro,
                        )
                    }
                }
            }

            if (!isSelf) {
                val active = person.isFollowing
                val requested = privacy.followRequested && !active
                val label = when {
                    isUpdating -> "Updating"
                    active -> "Following"
                    requested -> "Requested"
                    else -> "Follow"
                }
                if (!active && !requested) {
                    Button(
                        onClick = onFollowToggle,
                        enabled = !isUpdating,
                        modifier = Modifier.sizeIn(minWidth = 88.dp, minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NovaAccent,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        PersonRowActionContent(label, isUpdating, Color.White)
                    }
                } else {
                    OutlinedButton(
                        onClick = onFollowToggle,
                        enabled = !isUpdating,
                        modifier = Modifier.sizeIn(minWidth = 88.dp, minHeight = 48.dp),
                        border = BorderStroke(1.dp, NovaBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        PersonRowActionContent(
                            label = label,
                            isUpdating = isUpdating,
                            color = if (requested) NovaMuted else NovaInk,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = NovaBorder.copy(alpha = 0.72f))
    }
}


@Composable
private fun PersonRowActionContent(
    label: String,
    isUpdating: Boolean,
    color: Color,
) {
    if (isUpdating) {
        CircularProgressIndicator(
            modifier = Modifier.size(15.dp),
            color = color,
            strokeWidth = 2.dp,
        )
    } else {
        Text(
            text = label,
            color = color,
            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
