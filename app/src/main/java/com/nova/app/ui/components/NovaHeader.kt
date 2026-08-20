package com.nova.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaSurface


@Composable
fun NovaHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Text(
                    text = "‹",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = NovaInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(26.dp))
        }

        Text(
            text = title,
            color = NovaInk,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            color = NovaMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}
