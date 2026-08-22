package com.nova.app.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaAccent

/** Shared unread marker for Activity, messages and other ordinary Nova lists. */
@Composable
fun NovaUnreadDot(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = NovaAccent,
    ) {
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier
                .then(modifier)
                .size(8.dp),
        )
    }
}
