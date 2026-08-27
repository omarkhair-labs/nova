package com.nova.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaMotion


/** Nova-owned high-contrast action for immersive photo/video surfaces. */
@Composable
fun NovaImmersiveAction(
    icon: NovaIconAsset,
    contentDescription: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    busy: Boolean = false,
    activeColor: Color = NovaAccent,
) {
    val tint = if (active) activeColor else Color.White
    val scale = animateFloatAsState(
        targetValue = if (active) 1.08f else 1f,
        animationSpec = tween(NovaMotion.fast),
        label = "immersiveActionScale",
    ).value

    Column(
        modifier = modifier.semantics {
            role = Role.Button
            if (busy) stateDescription = "Updating"
            if (active) stateDescription = "Active"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            enabled = !busy,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.42f),
            border = BorderStroke(
                width = 1.dp,
                color = if (active) activeColor.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.16f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    NovaIcon(
                        asset = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(23.dp).scale(scale),
                        tint = tint,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
