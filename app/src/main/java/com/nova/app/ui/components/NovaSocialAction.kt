package com.nova.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaMotion
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaType


@Composable
fun NovaSocialAction(
    icon: NovaIconAsset,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    active: Boolean = false,
    busy: Boolean = false,
    activeColor: Color = NovaAccent,
) {
    val tint by animateColorAsState(
        targetValue = if (active) activeColor else NovaMuted,
        animationSpec = tween(NovaMotion.fast),
        label = "social-action-color",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = tween(NovaMotion.standard),
        label = "social-action-scale",
    )

    Surface(
        onClick = { if (!busy) onClick() },
        modifier = modifier
            .widthIn(min = 48.dp)
            .semantics { role = Role.Button },
        color = Color.Transparent,
        contentColor = tint,
    ) {
        Row(
            modifier = Modifier.size(width = if (count == null) 48.dp else 56.dp, height = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(31.dp),
                        color = tint.copy(alpha = 0.45f),
                        strokeWidth = 1.5.dp,
                    )
                }
                NovaIcon(
                    asset = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(21.dp).scale(scale),
                    tint = tint,
                )
            }
            count?.let {
                Text(
                    text = compactSocialCount(it),
                    color = tint,
                    style = NovaType.meta,
                )
            }
        }
    }
}


internal fun compactSocialCount(count: Int): String = when {
    count < 1_000 -> count.coerceAtLeast(0).toString()
    count < 1_000_000 -> "${count / 100 / 10.0}K".removeSuffix(".0K") + if (count / 100 % 10 == 0) "K" else ""
    else -> "${count / 100_000 / 10.0}M".removeSuffix(".0M") + if (count / 100_000 % 10 == 0) "M" else ""
}
