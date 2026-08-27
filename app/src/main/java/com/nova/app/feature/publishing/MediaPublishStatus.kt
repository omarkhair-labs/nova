package com.nova.app.feature.publishing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType


@Composable
fun MediaPublishStatus(
    items: List<MediaPublishItem>,
    modifier: Modifier = Modifier,
    onRetry: (MediaPublishItem) -> Unit,
    onCancel: (MediaPublishItem) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
        items.forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                color = if (item.stage == MediaPublishWorker.STAGE_FAILED) NovaSurface else NovaAccentSoft,
                border = BorderStroke(1.dp, if (item.stage == MediaPublishWorker.STAGE_FAILED) NovaBorder else NovaAccent.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${targetLabel(item.target)} · ${stageLabel(item.stage)}",
                                color = NovaInk,
                                style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                            )
                            item.error?.let {
                                Text(text = it, color = NovaMuted, style = NovaType.micro)
                            }
                        }
                        when {
                            item.canRetry -> Surface(
                                onClick = { onRetry(item) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                color = NovaAccent,
                                shape = androidx.compose.material3.MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    text = "Retry",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = NovaSurface,
                                    style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                            item.canCancel -> Surface(
                                onClick = { onCancel(item) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                color = NovaSurface,
                                shape = androidx.compose.material3.MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, NovaBorder),
                            ) {
                                Text(
                                    text = "Cancel",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = NovaMuted,
                                    style = NovaType.micro,
                                )
                            }
                        }
                    }
                    if (item.stage in ActivePublishStages) {
                        if (item.progress != null) {
                            LinearProgressIndicator(
                                progress = { item.progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = NovaAccent,
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = NovaAccent,
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun targetLabel(target: MediaPublishTarget): String = when (target) {
    MediaPublishTarget.POST -> "Post"
    MediaPublishTarget.REEL -> "Reel"
    MediaPublishTarget.STORY -> "Story"
}


private fun stageLabel(stage: String): String = when (stage) {
    MediaPublishWorker.STAGE_QUEUED -> "Queued for connection"
    MediaPublishWorker.STAGE_PREPARING -> "Preparing media"
    MediaPublishWorker.STAGE_UPLOADING -> "Uploading"
    MediaPublishWorker.STAGE_PUBLISHED -> "Published ✓"
    MediaPublishWorker.STAGE_FAILED -> "Needs attention"
    else -> "Queued"
}


private val ActivePublishStages = setOf(
    MediaPublishWorker.STAGE_QUEUED,
    MediaPublishWorker.STAGE_PREPARING,
    MediaPublishWorker.STAGE_UPLOADING,
)
