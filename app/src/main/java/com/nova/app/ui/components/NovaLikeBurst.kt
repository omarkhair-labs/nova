package com.nova.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaMotion
import kotlinx.coroutines.delay


/**
 * Shared lightweight favorite feedback for Nova content surfaces.
 *
 * `trigger` should increment for every accepted double-tap gesture. The motion is
 * deliberately visual-only: callers keep ownership of the actual Like mutation,
 * so replaying the animation can never toggle/unlike content accidentally.
 */
@Composable
fun NovaLikeBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        visible = false
        // Yield one frame so repeated double taps restart the entrance motion.
        delay(16)
        visible = true
        delay(NovaMotion.emphasized.toLong())
        visible = false
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(NovaMotion.fast)) + scaleIn(
                initialScale = 0.34f,
                animationSpec = tween(NovaMotion.standard, easing = FastOutSlowInEasing),
            ),
            exit = fadeOut(tween(NovaMotion.standard)) + scaleOut(
                targetScale = 1.18f,
                animationSpec = tween(NovaMotion.standard, easing = FastOutSlowInEasing),
            ),
        ) {
            NovaIcon(
                asset = NovaIconAsset.LikeFilled,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .shadow(14.dp, clip = false),
                tint = Color.White,
            )
        }
    }
}
