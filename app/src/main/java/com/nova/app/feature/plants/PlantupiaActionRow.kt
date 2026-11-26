package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset

/** A real destination; effects and access checks belong to the caller. */
@Composable
internal fun PlantupiaActionRow(
    icon: NovaIconAsset,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    enabled: Boolean = true,
    accent: Boolean = false,
) {
    val background = if (accent) PlantupiaColors.forest else PlantupiaColors.paper
    val ink = if (accent) PlantupiaColors.paper else PlantupiaColors.forestText
    val detailInk = if (accent) PlantupiaColors.cream else PlantupiaColors.secondaryText
    val direction = LocalLayoutDirection.current
    Surface(onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        color = background, shape = PlantupiaShapes.inset,
        border = if (accent) null else BorderStroke(1.dp, PlantupiaColors.border)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaIcon(icon, null, modifier = Modifier.size(24.dp),
                tint = if (enabled) ink else ink.copy(alpha = .45f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = PlantupiaText.itemTitle.copy(color = if (enabled) ink else detailInk))
                if (!detail.isNullOrBlank()) Text(detail, style = PlantupiaText.supporting.copy(color = detailInk))
            }
            NovaIcon(NovaIconAsset.Back, null,
                modifier = Modifier.size(18.dp).rotate(if (direction == LayoutDirection.Ltr) 180f else 0f),
                tint = if (enabled) ink else ink.copy(alpha = .45f))
        }
    }
}
