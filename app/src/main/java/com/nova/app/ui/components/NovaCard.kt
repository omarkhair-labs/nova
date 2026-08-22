package com.nova.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaSurface

/** Standard bordered surface for ordinary Nova cards and grouped sections. */
@Composable
fun NovaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = NovaSurface,
    borderColor: Color = NovaBorder,
    content: @Composable () -> Unit,
) {
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            content = content,
        )
    }
}
