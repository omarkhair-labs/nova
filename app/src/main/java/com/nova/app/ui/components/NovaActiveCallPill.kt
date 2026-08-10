package com.nova.app.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.CallActivity
import com.nova.app.core.calls.NovaActiveCallSignal
import com.nova.app.core.calls.NovaActiveCallSummary
import com.nova.app.core.calls.NovaCallKind
import com.nova.app.core.calls.NovaCallStatus
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay


@Composable
fun NovaActiveCallPill(
    modifier: Modifier = Modifier,
) {
    val active by NovaActiveCallSignal.state.collectAsState()
    val call = active ?: return
    val context = LocalContext.current
    val detail = activeCallDetail(call)

    Surface(
        onClick = {
            context.startActivity(
                CallActivity.existingCallIntent(
                    context = context,
                    callId = call.callId,
                    action = CallActivity.ACTION_OPEN_CALL,
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },
        shape = RoundedCornerShape(22.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
        shadowElevation = 8.dp,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 8.dp, start = 14.dp, end = 14.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = NovaAccentSoft,
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (call.kind == NovaCallKind.Video) {
                            Icons.Filled.Videocam
                        } else {
                            Icons.Filled.Call
                        },
                        contentDescription = null,
                        tint = NovaAccent,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (call.kind == NovaCallKind.Video) "Video" else "Voice"} call · ${call.peerName}",
                    color = NovaInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    color = NovaMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            Text(
                text = "Return",
                color = NovaAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


@Composable
private fun activeCallDetail(call: NovaActiveCallSummary): String {
    if (call.status != NovaCallStatus.Active) {
        return when (call.status) {
            NovaCallStatus.Ringing -> "Calling…"
            else -> "Tap to return"
        }
    }

    val startedAt = call.answeredAtEpochMs ?: return "Connected · Tap to return"
    var now by remember(call.callId, startedAt) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(call.callId, startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val elapsedSeconds = ((now - startedAt).coerceAtLeast(0L) / 1_000L)
    return "${formatDuration(elapsedSeconds)} · Tap to return"
}


private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
