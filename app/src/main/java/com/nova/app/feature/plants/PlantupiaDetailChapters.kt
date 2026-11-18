package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PlantupiaDetailChapter.entries.forEach { chapter ->
            val active = selected == chapter
            Surface(
                onClick = { onSelect(chapter) },
                modifier = Modifier.weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = active
                    },
                color = if (active) PlantupiaColors.forest else Color(0xFFE8EDE3),
                border = BorderStroke(1.dp,
                    if (active) PlantupiaColors.forest else PlantupiaColors.border),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = chapter.title,
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 5.dp),
                    color = if (active) Color.White else PlantupiaColors.forest,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
