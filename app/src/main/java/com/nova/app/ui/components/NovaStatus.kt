package com.nova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaAccent

/** Shared unread marker for Activity, messages and other ordinary Nova lists. */
@Composable
fun NovaUnreadDot(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(NovaAccent, CircleShape),
    )
}
