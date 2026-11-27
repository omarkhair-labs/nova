package com.nova.app.feature.plants

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Real mutually exclusive bodies inside ONE persisted PlantUnit detail context. */
internal enum class PlantupiaDetailChapter(val title: String, val purpose: String) {
    OVERVIEW("Overview", "Identity and next real step"),
    CARE("Care", "Reviewed guidance and explicit care recording"),
    HISTORY("History", "Only real recorded moments"),
}

internal fun plantupiaOpeningChapter(observationIntent: Boolean): PlantupiaDetailChapter =
    if (observationIntent) PlantupiaDetailChapter.CARE else PlantupiaDetailChapter.OVERVIEW

/** Switching chapter never implicitly writes a care event or loses the parent draft. */
internal fun plantupiaNextChapter(
    current: PlantupiaDetailChapter,
    selected: PlantupiaDetailChapter,
): PlantupiaDetailChapter = if (current == selected) current else selected

@Composable
internal fun PlantupiaDetailChapters(
    selected: PlantupiaDetailChapter,
    onSelect: (PlantupiaDetailChapter) -> Unit,
) {
    val chapters = PlantupiaDetailChapter.entries
    val labelStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Measure at the selected weight so selecting a tab never changes layout.
    // TextMeasurer respects the user's font scaling and actual font metrics.
    val widestLabel = with(density) {
        chapters.maxOf { chapter ->
            measurer.measure(chapter.title, style = labelStyle, softWrap = false, maxLines = 1).size.width
        }.toDp()
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val availableLabelWidth = (maxWidth - 6.dp * (chapters.size - 1)) / chapters.size - 10.dp
        if (availableLabelWidth >= widestLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chapters.forEach { chapter ->
                    PlantupiaDetailChapterTab(chapter, selected == chapter, onSelect, labelStyle, Modifier.weight(1f))
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chapters.forEach { chapter ->
                    PlantupiaDetailChapterTab(chapter, selected == chapter, onSelect, labelStyle, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PlantupiaDetailChapterTab(
    chapter: PlantupiaDetailChapter,
    active: Boolean,
    onSelect: (PlantupiaDetailChapter) -> Unit,
    labelStyle: TextStyle,
    modifier: Modifier,
) {
    Surface(
        onClick = { onSelect(chapter) },
        modifier = modifier.heightIn(min = 48.dp).semantics {
            role = Role.Tab
            selected = active
        },
        color = if (active) PlantupiaColors.softSage else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(chapter.title, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 5.dp),
                color = if (active) PlantupiaColors.forest else PlantupiaColors.secondaryText,
                style = labelStyle.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.fillMaxWidth().height(2.dp)
                .background(if (active) PlantupiaColors.forest else PlantupiaColors.border))
        }
    }
}
