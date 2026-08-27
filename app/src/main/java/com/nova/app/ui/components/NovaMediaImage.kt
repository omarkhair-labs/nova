package com.nova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaMuted


@Composable
fun NovaMediaImage(
    source: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "Nova photo",
    contentScale: ContentScale = ContentScale.Crop,
) {
    var isLoading by remember(source) { mutableStateOf(source.isNotBlank()) }
    var hasFailed by remember(source) { mutableStateOf(false) }

    Box(
        modifier = modifier.background(NovaAccentSoft),
        contentAlignment = Alignment.Center,
    ) {
        if (source.isNotBlank()) {
            AsyncImage(
                model = source,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onLoading = {
                    isLoading = true
                    hasFailed = false
                },
                onSuccess = {
                    isLoading = false
                    hasFailed = false
                },
                onError = {
                    isLoading = false
                    hasFailed = true
                },
            )
        }

        when {
            isLoading -> CircularProgressIndicator(color = NovaAccent)
            hasFailed -> Text(
                text = "Photo unavailable",
                color = NovaMuted,
                fontSize = 11.sp,
            )
        }
    }
}
