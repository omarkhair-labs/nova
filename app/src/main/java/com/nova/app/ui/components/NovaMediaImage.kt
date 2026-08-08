package com.nova.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL


@Composable
fun NovaMediaImage(
    source: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "Nova photo",
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = source,
    ) {
        value = if (source.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val stream = when {
                        source.startsWith("content://") || source.startsWith("file://") -> {
                            context.contentResolver.openInputStream(Uri.parse(source))
                        }

                        source.startsWith("http://") || source.startsWith("https://") -> {
                            URL(source).openStream()
                        }

                        else -> null
                    }
                    stream?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier.background(NovaAccentSoft),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (source.isNotBlank()) {
            CircularProgressIndicator(color = NovaAccent)
        }
    }
}
