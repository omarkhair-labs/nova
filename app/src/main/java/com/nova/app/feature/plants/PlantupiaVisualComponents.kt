package com.nova.app.feature.plants

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.nova.app.feature.plants.data.PlantupiaCareAction
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset

private val ink = Color(0xFF123B2E)
private val muted = Color(0xFF4A5A50)
private val paper = Color(0xFFFFFDF7)
private val soft = Color(0xFFE9EDE3)
private val border = Color(0xFFD5DED1)

/** Real private care actions with authored Android Canvas pictograms. */
@Composable
internal fun PlantupiaCareActionTile(
    action: PlantupiaCareAction,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) ink else paper,
        label = "plantupia-care-background",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) paper else ink,
        label = "plantupia-care-foreground",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 480f),
        label = "plantupia-care-selection",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .semantics { this.selected = selected }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(17.dp),
        color = background,
        border = BorderStroke(1.dp, if (selected) ink else border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 116.dp)
                .padding(horizontal = 7.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Canvas(modifier = Modifier.size(40.dp)) { careIcon(action, foreground) }
            Text(
                text = action.label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = foreground,
                // A full human action must stay legible even on narrow
                // editors and enlarged Android font sizes. Rows can grow.
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                minLines = 2,
            )
        }
    }
}

private fun DrawScope.careIcon(action: PlantupiaCareAction, color: Color) {
    val s = size.minDimension
    val stroke = s * .057f
    fun pt(x: Float, y: Float) = Offset(s * x, s * y)
    fun segment(a: Float, b: Float, c: Float, d: Float) {
        drawLine(color, pt(a, b), pt(c, d), stroke, StrokeCap.Round)
    }
    when (action) {
        PlantupiaCareAction.WATERED -> {
            val drop = Path().apply {
                moveTo(s * .5f, s * .11f)
                cubicTo(s * .37f, s * .36f, s * .22f, s * .51f, s * .22f, s * .65f)
                cubicTo(s * .22f, s * .83f, s * .35f, s * .9f, s * .5f, s * .9f)
                cubicTo(s * .65f, s * .9f, s * .78f, s * .83f, s * .78f, s * .65f)
                cubicTo(s * .78f, s * .51f, s * .63f, s * .36f, s * .5f, s * .11f)
                close()
            }
            drawPath(drop, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
            segment(.35f, .67f, .40f, .76f)
        }
        PlantupiaCareAction.CHECKED_SOIL -> {
            segment(.12f, .38f, .88f, .38f)
            segment(.2f, .57f, .8f, .57f)
            segment(.28f, .77f, .72f, .77f)
            segment(.5f, .13f, .5f, .26f)
            segment(.43f, .2f, .5f, .27f)
            segment(.57f, .2f, .5f, .27f)
            drawCircle(color, stroke * .65f, pt(.33f, .48f))
            drawCircle(color, stroke * .65f, pt(.66f, .67f))
        }
        PlantupiaCareAction.PRUNED -> {
            drawCircle(color, s * .1f, pt(.27f, .75f), style = Stroke(stroke))
            drawCircle(color, s * .1f, pt(.56f, .75f), style = Stroke(stroke))
            segment(.34f, .68f, .78f, .20f)
            segment(.49f, .68f, .18f, .25f)
        }
        PlantupiaCareAction.MOVED -> {
            segment(.18f, .3f, .8f, .3f)
            segment(.66f, .17f, .8f, .3f)
            segment(.66f, .43f, .8f, .3f)
            segment(.82f, .69f, .2f, .69f)
            segment(.34f, .56f, .2f, .69f)
            segment(.34f, .82f, .2f, .69f)
        }
        PlantupiaCareAction.FERTILIZED -> {
            segment(.2f, .6f, .8f, .6f)
            segment(.27f, .6f, .35f, .85f)
            segment(.73f, .6f, .65f, .85f)
            segment(.35f, .85f, .65f, .85f)
            drawCircle(color, s * .04f, pt(.32f, .32f))
            drawCircle(color, s * .04f, pt(.52f, .18f))
            drawCircle(color, s * .04f, pt(.7f, .34f))
            drawCircle(color, s * .03f, pt(.47f, .45f))
        }
        PlantupiaCareAction.REPOTTED -> {
            segment(.19f, .44f, .81f, .44f)
            segment(.25f, .44f, .34f, .85f)
            segment(.75f, .44f, .66f, .85f)
            segment(.34f, .85f, .66f, .85f)
            segment(.5f, .44f, .5f, .19f)
            segment(.5f, .32f, .34f, .18f)
            segment(.5f, .27f, .66f, .12f)
        }
    }
}

/** Native photo-led plant row: only user-owned data supplied by the signed-in API. */
@Composable
internal fun PlantupiaPlantInventoryCard(
    plant: PlantupiaPlant,
    eventLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, border),
        color = paper,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantupiaEvidencePhoto(
                photoUrl = plant.coverPhoto,
                photoDescription = "Uploaded photo of " + plant.name,
                modifier = Modifier.size(width = 96.dp, height = 106.dp),
                cornerRadius = 13.dp,
                compact = true,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    plant.name, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                    // Keep distinct inventory identities readable at 360dp and large text.
                    softWrap = true,
                )
                val context = listOf(plant.species, plant.placement).filter(String::isNotBlank).joinToString(" · ")
                if (context.isNotBlank()) {
                    Text(context, color = muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    if (plant.lastEventAt.isNotBlank()) eventLabel + " · " + plant.lastEventAt.take(10)
                    else "Start recording this plant's life",
                    color = muted, fontSize = 11.sp, maxLines = 1,
                )
                if (plant.nextFollowUpOn.isNotBlank()) {
                    Text(
                        "Chosen check-in · " + plant.nextFollowUpOn,
                        color = ink, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                    )
                }
            }
        }
    }
}


/**
 * A genuine dated record, not an AI health score. Photo, note, and source are
 * taken from the authenticated PlantEvent response. Used by the real history UI.
 */
@Composable
internal fun PlantupiaHistoryEventCard(
    event: PlantupiaCareEvent,
    title: String,
    evidenceLabel: String,
    onPhotoClick: () -> Unit,
    linkedOutcomes: List<PlantupiaCareEvent> = emptyList(),
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = paper,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = soft) {
                    NovaIcon(
                        asset = when (event.kind) {
                            "care" -> NovaIconAsset.Check
                            "issue" -> NovaIconAsset.Info
                            "outcome" -> NovaIconAsset.Refresh
                            else -> NovaIconAsset.Photo
                        },
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(19.dp),
                        tint = ink,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(event.observedAt.take(10), color = muted, fontSize = 11.sp)
                }
            }
            if (event.photoUrl.isNotBlank()) {
                PlantupiaEvidencePhoto(
                    photoUrl = event.photoUrl,
                    photoDescription = "Actual photo saved with the " + title + " event",
                    modifier = Modifier.fillMaxWidth().height(198.dp),
                    cornerRadius = 13.dp,
                    framing = PlantupiaPhotoFraming.FULL_RECORD,
                    onOpenPhoto = onPhotoClick,
                )
            }
            if (event.note.isNotBlank()) {
                Text(event.note, color = ink, fontSize = 13.sp, lineHeight = 20.sp)
            } else {
                Text("Care event recorded", color = muted, fontSize = 12.sp)
            }
            Text(
                "Evidence: " + evidenceLabel,
                color = muted, fontSize = 11.sp,
            )
            if (event.followUpOn.isNotBlank()) {
                Surface(color = soft, shape = RoundedCornerShape(9.dp)) {
                    Text(
                        if (event.followUpCompletedAt.isNotBlank()) "Check-in completed · " + event.followUpOn
                        else "Check again on " + event.followUpOn,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = ink, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                    )
                }
            }
            if (linkedOutcomes.isNotEmpty()) {
                Surface(color = soft, shape = RoundedCornerShape(13.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Observed check-in outcome",
                            color = ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        )
                        linkedOutcomes.forEach { outcome ->
                            Text(outcome.observedAt.take(10), color = muted, fontSize = 11.sp)
                            Text(
                                outcome.note.ifBlank { "Check-in outcome recorded without details." },
                                color = ink, fontSize = 13.sp, lineHeight = 19.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
