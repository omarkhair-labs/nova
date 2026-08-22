package com.nova.app.feature.memories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.app.appContainer
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import java.util.TimeZone


@Composable
fun MemoriesRail(
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.memoryRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { MemoryStateOwner(repository, scope) }
    val state = owner.state
    var showMemory by remember { mutableStateOf(false) }

    LaunchedEffect(owner) {
        owner.load(
            utcOffsetMinutes = railUtcOffsetMinutes(),
            weeksAgo = 0,
            showSpinner = true,
        )
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    Surface(
        onClick = { if (state.memory != null) showMemory = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if ((state.memory?.stats?.highlights ?: 0) > 0) Color(0xFF12151D) else NovaSurface,
        border = BorderStroke(
            1.dp,
            if ((state.memory?.stats?.highlights ?: 0) > 0) NovaAccent.copy(alpha = 0.26f) else NovaBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = NovaAccentSoft,
            ) {
                Text(
                    text = "✦",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    color = NovaAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        state.loading && state.memory == null -> "Remembering last week…"
                        state.error != null && state.memory == null -> "Couldn't build your week"
                        (state.memory?.stats?.highlights ?: 0) > 0 -> "Your week is ready"
                        else -> "Last week was quiet"
                    },
                    color = if ((state.memory?.stats?.highlights ?: 0) > 0) Color.White else NovaInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        state.error != null && state.memory == null -> "Tap after Nova reconnects."
                        state.memory == null -> "Pulse · Rooms · Posts"
                        else -> railSummary(state.memory!!.stats.highlights, state.memory!!.stats.people, state.memory!!.stats.nights)
                    },
                    color = if ((state.memory?.stats?.highlights ?: 0) > 0) Color(0xFFB6BCC9) else NovaMuted,
                    fontSize = 9.sp,
                )
            }
            if (state.loading && state.memory == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = NovaAccent,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = if (state.memory != null) "Open ›" else "",
                    color = NovaAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (state.error != null && state.memory == null) {
        Spacer(modifier = Modifier.width(1.dp))
    }

    if (showMemory) {
        Dialog(
            onDismissRequest = { showMemory = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            MemoryScreen(
                onBack = { showMemory = false },
                onPersonClick = { username ->
                    showMemory = false
                    onPersonClick(username)
                },
                onSessionExpired = {
                    showMemory = false
                    onSessionExpired()
                },
            )
        }
    }
}


private fun railSummary(highlights: Int, people: Int, nights: Int): String = when {
    highlights == 0 -> "A quiet week, still yours."
    people > 0 && nights > 0 -> "$highlights moments · $people people · $nights nights"
    people > 0 -> "$highlights moments · $people people"
    else -> "$highlights moments worth keeping"
}


private fun railUtcOffsetMinutes(): Int =
    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
