package com.nova.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaElevation
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaType


@Composable
fun NovaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    busyText: String = text,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = NovaAccent,
            contentColor = Color.White,
            disabledContainerColor = NovaAccent.copy(alpha = 0.36f),
            disabledContentColor = Color.White.copy(alpha = 0.85f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = NovaElevation.flat),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (busy) {
                NovaPresenceIndicator(
                    modifier = Modifier.size(18.dp),
                    monochrome = true,
                    monochromeColor = Color.White,
                )
            }
            Text(
                text = if (busy) busyText else text,
                style = NovaType.button,
            )
        }
    }
}


@Composable
fun NovaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, NovaBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaInk),
    ) {
        Text(
            text = text,
            style = NovaType.button,
        )
    }
}
