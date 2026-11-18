package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A person's explicit record intent, never inferred from a guide or a due date. */
internal enum class PlantupiaCareEntryMode(val eventKind: String, val label: String, val saveLabel: String) {
    CARE("care", "Care action", "Save care"),
    NOTE("observation", "Observation", "Save observation"),
    CONCERN("issue", "Concern", "Record concern"),
}

internal fun plantupiaCanSaveCareEntry(
    mode: PlantupiaCareEntryMode,
    careActionSelected: Boolean,
    noteOrPhotoPresent: Boolean,
    validFollowUpDate: Boolean,
): Boolean = validFollowUpDate && when (mode) {
    PlantupiaCareEntryMode.CARE -> careActionSelected
    PlantupiaCareEntryMode.NOTE, PlantupiaCareEntryMode.CONCERN -> noteOrPhotoPresent
}

/** Larger text and narrow phones get three full-width choices, never clipped labels. */
internal fun careEntryModesNeedStack(widthDp: Float, fontScale: Float): Boolean =
    widthDp < 330f || fontScale >= 1.3f

@Composable
internal fun PlantupiaCareEntryModePicker(
    selectedMode: PlantupiaCareEntryMode,
    enabled: Boolean,
    onSelect: (PlantupiaCareEntryMode) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = careEntryModesNeedStack(
            maxWidth.value, LocalDensity.current.fontScale,
        )
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                PlantupiaCareEntryMode.entries.forEach { mode ->
                    PlantupiaCareEntryModeChoice(
                        mode = mode, active = selectedMode == mode, enabled = enabled,
                        modifier = Modifier.fillMaxWidth(), onClick = { onSelect(mode) },
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                PlantupiaCareEntryMode.entries.forEach { mode ->
                    PlantupiaCareEntryModeChoice(
                        mode = mode, active = selectedMode == mode, enabled = enabled,
                        modifier = Modifier.weight(1f), onClick = { onSelect(mode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantupiaCareEntryModeChoice(
    mode: PlantupiaCareEntryMode,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp).semantics {
            role = Role.RadioButton
            selected = active
            contentDescription = mode.label + " record type"
        },
        shape = RoundedCornerShape(10.dp),
        color = if (active) PlantupiaColors.softSage else PlantupiaColors.paper,
        border = BorderStroke(1.dp, if (active)
            PlantupiaColors.forest else PlantupiaColors.border),
    ) {
        Text(
            text = mode.label,
            color = PlantupiaColors.forest,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
        )
    }
}
