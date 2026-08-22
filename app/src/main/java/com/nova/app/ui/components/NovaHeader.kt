package com.nova.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType


@Composable
fun NovaHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            NovaBackButton(onClick = onBack)
            Spacer(modifier = Modifier.height(26.dp))
        }

        Text(
            text = title,
            color = NovaInk,
            style = NovaType.pageTitle,
        )

        Spacer(modifier = Modifier.height(NovaSpacing.sm))

        Text(
            text = subtitle,
            color = NovaMuted,
            style = NovaType.subtitle,
        )
    }
}
