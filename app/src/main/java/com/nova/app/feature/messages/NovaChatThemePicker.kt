package com.nova.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun NovaChatThemePicker(
    selectedKey: String,
    savingKey: String?,
    errorMessage: String?,
    onSelect: (NovaChatPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NovaBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Text(
                            "‹",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp),
                            color = NovaInk,
                            fontSize = 27.sp,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Chat theme",
                            color = NovaInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Choose the mood for this conversation. Only your view changes.",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(
                            errorMessage,
                            modifier = Modifier.padding(12.dp),
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(NovaChatThemes.All, key = { it.key }) { palette ->
                        NovaThemePreviewCard(
                            palette = palette,
                            selected = palette.key == selectedKey,
                            saving = palette.key == savingKey,
                            enabled = savingKey == null,
                            onClick = { onSelect(palette) },
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NovaThemePreviewCard(
    palette: NovaChatPalette,
    selected: Boolean,
    saving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = { if (enabled && !selected) onClick() },
        shape = RoundedCornerShape(24.dp),
        color = palette.background,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) palette.accent else palette.border,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        palette.label,
                        color = palette.ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        when (palette.key) {
                            "nova" -> "Clean and familiar"
                            "midnight" -> "Deep, calm and cinematic"
                            "aurora" -> "Cool violet and blue"
                            "ocean" -> "Fresh teal and sea tones"
                            "rose" -> "Soft rose with warm contrast"
                            "ember" -> "Warm coral and amber"
                            else -> "Nova conversation theme"
                        },
                        color = palette.muted,
                        fontSize = 11.sp,
                    )
                }
                when {
                    saving -> CircularProgressIndicator(
                        color = palette.accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                    selected -> Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.accent,
                    ) {
                        Text(
                            "Selected",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = palette.outgoingText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 5.dp),
                            color = palette.incomingBubble,
                            border = BorderStroke(1.dp, palette.border),
                        ) {
                            Text(
                                "This feels like us.",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                color = palette.incomingText,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 5.dp, 16.dp),
                            color = palette.outgoingBubble,
                        ) {
                            Text(
                                "Keep this one ✦",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                color = palette.outgoingText,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(palette.accent, palette.outgoingBubble, palette.accentSoft).forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(9.dp)
                            .background(color, CircleShape),
                    )
                }
            }
        }
    }
}
