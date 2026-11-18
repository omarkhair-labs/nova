package com.nova.app.feature.plants

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.plants.data.PlantupiaCareEvent

/** A human-controlled flip between actual uploaded records, NOT inferred growth. */
@Composable
internal fun PlantupiaPhotoTimeLens(
    pair: PlantupiaLoadedPhotoPair,
    hasOlderPages: Boolean,
    onOpenPhoto: (PlantupiaCareEvent) -> Unit,
) {
    var showLater by rememberSaveable(pair.earlier.id, pair.later.id) {
        mutableStateOf(true)
    }
    val selected = if (showLater) pair.later else pair.earlier
    Surface(
        color = PlantupiaColors.paper,
        border = BorderStroke(1.dp, PlantupiaColors.border),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("TWO MOMENTS / ONE LIVING PLANT",
                style = PlantupiaText.sectionLabel)
            Text("See it at another point in time",
                style = PlantupiaText.body.copy(
                    fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                ))
            Text("Two photographs you actually recorded. Switch dates to examine them; " +
                "this is not a measured growth or health comparison.",
                style = PlantupiaText.supporting)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(false, true).forEach { later ->
                    val entry = if (later) pair.later else pair.earlier
                    val active = showLater == later
                    Surface(
                        onClick = { showLater = later },
                        color = if (active) PlantupiaColors.forest else PlantupiaColors.softSage,
                        border = BorderStroke(1.dp, if (active) PlantupiaColors.forest
                            else PlantupiaColors.border),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                            .semantics {
                                role = Role.Tab
                                selected = active
                                contentDescription = (if (later) "Later recorded photo, "
                                    else "Earlier recorded photo, ") + entry.observedAt.take(10)
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(if (later) "LATER" else "EARLIER",
                                color = if (active) PlantupiaColors.paper
                                    else PlantupiaColors.forest,
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(entry.observedAt.take(10),
                                color = if (active) PlantupiaColors.paper
                                    else PlantupiaColors.forest,
                                fontSize = 12.sp)
                        }
                    }
                }
            }
            Crossfade(targetState = selected.id, animationSpec = tween(180),
                label = "actual-saved-photo-time-lens") { id ->
                val visible = if (id == pair.later.id) pair.later else pair.earlier
                PlantupiaEvidencePhoto(
                    photoUrl = visible.photoUrl,
                    photoDescription = "Actual recorded plant photograph from " +
                        visible.observedAt.take(10) + ". Tap to inspect.",
                    modifier = Modifier.fillMaxWidth().height(248.dp),
                    cornerRadius = 11.dp,
                    framing = PlantupiaPhotoFraming.FULL_RECORD,
                    onOpenPhoto = { onOpenPhoto(visible) },
                )
            }
            if (hasOlderPages) {
                Text("Only photos loaded so far. Earlier pages may add older moments.",
                    style = PlantupiaText.supporting)
            }
        }
    }
}
