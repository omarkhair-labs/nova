package com.nova.app.feature.plants

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A visual metaphor only: the answer belongs to the human observer, never a sensor. */
internal data class InspectionChoiceStyle(val chapter: String, val background: Color, val ink: Color)
internal fun inspectionChoiceStyle(value: String): InspectionChoiceStyle = when (value) {
    "dry" -> InspectionChoiceStyle("soil", Color(0xFFF1E1D1), Color(0xFF795642))
    "moist" -> InspectionChoiceStyle("water", Color(0xFFE5F0F5), Color(0xFF28677B))
    "leaves_ok" -> InspectionChoiceStyle("inspection", Color(0xFFE4EFE1), Color(0xFF537446))
    "leaves_changed" -> InspectionChoiceStyle("warnings", Color(0xFFF7E5DB), Color(0xFF995B41))
    "drainage_ok" -> InspectionChoiceStyle("water", Color(0xFFE5F0F5), Color(0xFF28677B))
    "drainage_concern" -> InspectionChoiceStyle("warnings", Color(0xFFF7E5DB), Color(0xFF995B41))
    "light_ok" -> InspectionChoiceStyle("light", Color(0xFFFFEFCD), Color(0xFF966019))
    "light_changed" -> InspectionChoiceStyle("repotting", Color(0xFFF1E3D7), Color(0xFF76543F))
    else -> InspectionChoiceStyle("inspection", PlantupiaColors.softSage, PlantupiaColors.forest)
}

@Composable
internal fun PlantupiaInspectionChoiceGrid(
    choices: List<Pair<String, String>>,
    selected: String?,
    enabled: Boolean,
    onChoose: (String) -> Unit,
) {
    BoxWithConstraints {
        val columns = if (maxWidth >= 300.dp && LocalDensity.current.fontScale < 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            choices.chunked(columns).forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    group.forEach { (value, label) ->
                        val art = inspectionChoiceStyle(value)
                        val chosen = selected == value
                        val bg by animateColorAsState(
                            targetValue = if (chosen) art.ink else art.background,
                            label = "inspection-choice-color-$value",
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (chosen) 1.012f else 1f,
                            animationSpec = spring(dampingRatio = .83f, stiffness = 540f),
                            label = "inspection-choice-scale-$value",
                        )
                        val fg = if (chosen) PlantupiaColors.paper else art.ink
                        Surface(
                            onClick = { onChoose(value) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f).heightIn(min = 93.dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .semantics { this.selected = chosen; contentDescription = label },
                            color = bg,
                            border = BorderStroke(if (chosen) 2.dp else 1.dp,
                                fg.copy(alpha = if (chosen) .78f else .17f)),
                            shape = RoundedCornerShape(17.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(13.dp),
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CareChapterIllustration(art.chapter, fg, Modifier.size(35.dp))
                                Text(
                                    label,
                                    modifier = Modifier.weight(1f),
                                    style = PlantupiaText.body.copy(
                                        color = fg,
                                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                )
                            }
                        }
                    }
                    if (group.size < columns) repeat(columns-group.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
