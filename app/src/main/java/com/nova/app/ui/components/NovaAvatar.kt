package com.nova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft


@Composable
fun NovaAvatar(
    source: String,
    fallbackText: String,
    size: Dp = 86.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(NovaAccentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = fallbackText.firstOrNull()?.uppercase() ?: "N",
            color = NovaAccent,
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.Bold,
        )

        if (source.isNotBlank()) {
            AsyncImage(
                model = source,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
