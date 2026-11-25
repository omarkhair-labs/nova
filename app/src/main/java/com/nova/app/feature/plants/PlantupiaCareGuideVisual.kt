package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.plants.data.PlantupiaCareTopic

/** Original graphic chapters, not sensor readings or guesses about the user's actual plant. */
internal data class CareChapterPalette(val background: Color, val ink: Color, val label: String)

internal fun careChapterPalette(topic: String): CareChapterPalette = when (topic) {
    "water" -> CareChapterPalette(Color(0xFFE7F2F5), Color(0xFF28677B), "Water")
    "light" -> CareChapterPalette(Color(0xFFFFEFCD), Color(0xFF966019), "Light")
    "soil" -> CareChapterPalette(Color(0xFFF1E3D7), Color(0xFF76543F), "Soil")
    "humidity" -> CareChapterPalette(Color(0xFFE3EFEB), Color(0xFF3A796C), "Humidity")
    "fertilizing" -> CareChapterPalette(Color(0xFFF0F1D7), Color(0xFF66783E), "Nutrition")
    "pruning" -> CareChapterPalette(Color(0xFFF2E8F0), Color(0xFF795574), "Pruning")
    "repotting" -> CareChapterPalette(Color(0xFFF3E3D9), Color(0xFF8C5847), "Repotting")
    "inspection" -> CareChapterPalette(Color(0xFFE8F0DD), Color(0xFF527044), "Observation")
    "warnings" -> CareChapterPalette(Color(0xFFF9E3D9), Color(0xFFA15237), "Cautions")
    else -> CareChapterPalette(PlantupiaColors.softSage, PlantupiaColors.forest, "Care")
}

/** Only reviewed topics returned by the server may become readable chapters. */
internal fun selectedReviewedCareTopic(
    topics: List<PlantupiaCareTopic>,
    selectedKey: String?,
): PlantupiaCareTopic? =
    topics.firstOrNull { it.topic == selectedKey } ?: topics.firstOrNull()

@Composable
internal fun PlantupiaCareChapterGallery(
    topics: List<PlantupiaCareTopic>,
    selectedTopic: String?,
    onSelect: (String) -> Unit,
    onOpenSource: (String) -> Unit,
) {
    if (topics.isEmpty()) return
    val chapter = selectedReviewedCareTopic(topics, selectedTopic)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("THE FIELD GUIDE / CHAPTER INDEX", style = PlantupiaText.sectionLabel)
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(topics.size) { index ->
                val item = topics[index]
                val palette = careChapterPalette(item.topic)
                val active = chapter?.topic == item.topic
                Surface(
                    onClick = { onSelect(item.topic) },
                    modifier = Modifier
                        .widthIn(min = 134.dp)
                        .heightIn(min = 100.dp)
                        .semantics {
                            selected = active
                            contentDescription = "Read reviewed " + palette.label + " chapter"
                        },
                    color = palette.background,
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(
                        if (active) 2.dp else 1.dp,
                        palette.ink.copy(alpha = if (active) 0.88f else 0.22f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        CareChapterIllustration(
                            item.topic, palette.ink, Modifier.size(36.dp).clearAndSetSemantics { },
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(palette.label, fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold, color = palette.ink)
                            Text(if (active) "Reading this chapter" else "Explore chapter",
                                style = PlantupiaText.supporting.copy(
                                    color = palette.ink, fontSize = 11.sp,
                                ))
                        }
                    }
                }
            }
        }
        // A single, full-length reading: distinct, illustrated chapter pickers
        // keep the nine native authored pictograms without nested expanding tiles.
        chapter?.let { item ->
            val palette = careChapterPalette(item.topic)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PlantupiaColors.paper,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.ink.copy(alpha = 0.28f)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Text(palette.label.uppercase(),
                        style = PlantupiaText.sectionLabel.copy(color = palette.ink))
                    Text(item.title, style = PlantupiaText.editorialHeading)
                    PlantupiaSignatureFieldPlate(item.topic)
                    Text(item.instructions, style = PlantupiaText.body)
                    if (item.contextNotes.isNotBlank()) {
                        Text("CONTEXT & CAUTIONS",
                            style = PlantupiaText.sectionLabel.copy(color = palette.ink))
                        Text(item.contextNotes, style = PlantupiaText.body)
                    }
                    if (item.sourceUrl.startsWith("https://")) {
                        TextButton(
                            onClick = { onOpenSource(item.sourceUrl) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Reviewed source · " + item.sourceReference,
                                color = palette.ink)
                        }
                    } else if (item.sourceReference.isNotBlank()) {
                        Text("Source: " + item.sourceReference,
                            style = PlantupiaText.supporting)
                    }
                }
            }
        }
    }
}

/** Simple self-authored pictograms that describe a *topic*, never a diagnosis or measured care value. */
@Composable
internal fun CareChapterIllustration(topic: String, ink: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val m = size.minDimension
        val sw = m * .045f
        fun p(x: Float, y: Float) = Offset(w * x, h * y)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(ink, p(x1, y1), p(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
        fun circle(x: Float, y: Float, radius: Float, filled: Boolean = false) =
            drawCircle(ink, radius * m, p(x, y), style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(sw))
        when (topic) {
            "water", "humidity" -> {
                val drop = Path().apply {
                    moveTo(.50f*w, .10f*h)
                    cubicTo(.37f*w, .34f*h, .22f*w, .52f*h, .22f*w, .68f*h)
                    cubicTo(.22f*w, .89f*h, .36f*w, .93f*h, .5f*w, .93f*h)
                    cubicTo(.64f*w, .93f*h, .78f*w, .89f*h, .78f*w, .68f*h)
                    cubicTo(.78f*w, .52f*h, .63f*w, .34f*h, .5f*w, .10f*h)
                    close()
                }
                drawPath(drop, ink, style = Stroke(sw))
                line(.34f,.70f,.40f,.79f)
                if (topic == "humidity") {
                    circle(.14f,.34f,.046f,true)
                    circle(.85f,.25f,.055f,true)
                }
            }
            "light" -> {
                circle(.50f,.50f,.21f)
                for (i in 0 until 8) {
                    val rad = i * Math.PI / 4
                    line(
                        (.50 + kotlin.math.cos(rad)*.32).toFloat(),
                        (.50 + kotlin.math.sin(rad)*.32).toFloat(),
                        (.50 + kotlin.math.cos(rad)*.44).toFloat(),
                        (.50 + kotlin.math.sin(rad)*.44).toFloat(),
                    )
                }
            }
            "soil" -> {
                line(.14f,.30f,.85f,.30f)
                line(.14f,.53f,.85f,.53f)
                line(.14f,.77f,.85f,.77f)
                circle(.34f,.41f,.036f,true); circle(.72f,.65f,.043f,true)
                circle(.53f,.84f,.033f,true)
            }
            "fertilizing" -> {
                circle(.30f,.28f,.055f,true); circle(.54f,.16f,.048f,true)
                circle(.73f,.36f,.042f,true)
                line(.18f,.55f,.82f,.55f)
                line(.23f,.55f,.33f,.87f)
                line(.77f,.55f,.67f,.87f)
                line(.33f,.87f,.67f,.87f)
            }
            "pruning" -> {
                circle(.31f,.75f,.12f)
                circle(.62f,.75f,.12f)
                line(.38f,.65f,.77f,.16f)
                line(.54f,.64f,.20f,.16f)
            }
            "repotting" -> {
                line(.17f,.51f,.83f,.51f)
                line(.24f,.51f,.36f,.88f)
                line(.76f,.51f,.64f,.88f)
                line(.36f,.88f,.64f,.88f)
                line(.5f,.50f,.5f,.20f)
                line(.5f,.36f,.30f,.22f)
                line(.50f,.31f,.70f,.15f)
            }
            "inspection" -> {
                circle(.44f,.40f,.24f)
                line(.62f,.61f,.88f,.88f)
                line(.38f,.48f,.46f,.28f)
            }
            "warnings" -> {
                val triangle = Path().apply {
                    moveTo(w*.5f,h*.12f); lineTo(w*.90f,h*.88f)
                    lineTo(w*.1f,h*.88f); close()
                }
                drawPath(triangle,ink,style=Stroke(sw))
                line(.5f,.40f,.5f,.63f)
                circle(.50f,.76f,.027f,true)
            }
            else -> {
                line(.5f,.20f,.5f,.77f)
                line(.5f,.47f,.3f,.31f)
                line(.5f,.40f,.68f,.24f)
                circle(.3f,.31f,.12f)
                circle(.68f,.24f,.12f)
            }
        }
    }
}
